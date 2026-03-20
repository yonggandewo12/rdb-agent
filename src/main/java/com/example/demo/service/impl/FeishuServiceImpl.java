package com.example.demo.service.impl;

import com.example.demo.constant.CommonConstant;
import com.example.demo.entity.RedisDatasource;
import com.example.demo.service.DynamicRedisService;
import com.example.demo.service.FeishuMessageService;
import com.example.demo.service.FeishuService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/**
 * 飞书事件服务实现类
 *
 * @author system
 * @date 2026-03-12
 */
@Service
public class FeishuServiceImpl implements FeishuService {
    
    private static final Logger logger = LoggerFactory.getLogger(FeishuServiceImpl.class);
    
    @Value("${feishu.verification-token}")
    private String verificationToken;
    
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final LlmServiceImpl llmService;
    private final FeishuMessageService feishuMessageService;
    private final DynamicRedisService dynamicRedisService;

    @Autowired
    public FeishuServiceImpl(LlmServiceImpl llmService, FeishuMessageService feishuMessageService,
                            DynamicRedisService dynamicRedisService) {
        this.llmService = llmService;
        this.feishuMessageService = feishuMessageService;
        this.dynamicRedisService = dynamicRedisService;
    }
    
    /**
     * 处理飞书URL验证请求
     *
     * @param challenge 挑战字符串
     * @param token 验证token
     * @return 挑战字符串
     */
    @Override
    public void handleChallenge(String challenge, String token) {
        if (!StringUtils.hasText(challenge) || !StringUtils.hasText(token)) {
            throw new IllegalArgumentException(CommonConstant.ErrorMessage.PARAM_NULL);
        }
        
        if (!verificationToken.equals(token)) {
            logger.error("Invalid verification token: {}", token);
            throw new RuntimeException("Invalid verification token");
        }
        
        logger.info("Feishu token verified successfully, token: {}", challenge);
    }
    
    /**
     * 处理飞书消息事件
     *
     * @param eventType 事件类型
     * @param content 消息内容
     * @param chatId 聊天ID
     * @param openId 用户openId
     * @param messageId 消息ID
     * @param chatType 聊天类型
     */
    @Override
    public void handleMessageEvent(String eventType, String content, String chatId, String openId, 
                                   String messageId, String chatType) {
        if (!StringUtils.hasText(eventType) || !StringUtils.hasText(content) 
                || !StringUtils.hasText(chatId) || !StringUtils.hasText(openId)
                || !StringUtils.hasText(messageId) || !StringUtils.hasText(chatType)) {
            logger.error(CommonConstant.ErrorMessage.PARAM_NULL);
            return;
        }
        
        try {
            // 解析消息内容
            JsonNode contentNode = objectMapper.readTree(content);
            String messageContent = contentNode.get("text").asText();
            
            // 去除@机器人的前缀
            String cleanContent = messageContent.replaceAll(CommonConstant.Regex.FEISHU_AT_PREFIX, "").trim();
            
            logger.info("Received Feishu message from user {} in chat {}: {}", 
                    openId, chatId, cleanContent);
            
            // 根据chatId查找对应的Redis数据源
            RedisDatasource datasource = dynamicRedisService.getDatasourceByChatId(chatId);
            if (datasource == null) {
                sendReply(chatType, chatId, openId, messageId, 
                        "未找到该群聊对应的Redis数据源配置，请先在管理后台配置！");
                return;
            }
            
            logger.info("Using Redis datasource: redis={}:{}", 
                    datasource.getRedisHost(), datasource.getRedisPort());
            
            // 1. 先尝试调用大模型分析，判断是不是Redis问题
            String llmResponse = llmService.analyzeQuery(cleanContent);
            if (!StringUtils.hasText(llmResponse)) {
                sendReply(chatType, chatId, openId, messageId, CommonConstant.ErrorMessage.LLM_CALL_FAILED);
                return;
            }
            
            // 2. 判断是JSON格式还是直接回答
            String trimmedResponse = llmResponse.trim();
            boolean isJson = trimmedResponse.startsWith("{") && trimmedResponse.endsWith("}");
            
            String finalResponse;
            if (isJson) {
                // 是JSON，执行Redis操作
                try {
                    Object result = llmService.executeRedisOperation(llmResponse, datasource, dynamicRedisService);
                    finalResponse = llmService.generateResponse(result, cleanContent);
                } catch (Exception e) {
                    logger.error("Failed to execute Redis operation", e);
                    finalResponse = "操作执行失败：" + e.getMessage();
                }
            } else {
                // 不是JSON，调用directAnswer获取更准确的回答
                finalResponse = llmService.directAnswer(cleanContent);
                if (!StringUtils.hasText(finalResponse)) {
                    finalResponse = llmResponse; // 如果directAnswer返回空，就用原来的回答
                }
            }
            
            // 3. 回复消息到对应的群组
            sendReply(chatType, chatId, openId, messageId, finalResponse);
            
        } catch (JsonProcessingException e) {
            logger.error("Failed to parse message content: {}", content, e);
            sendReply(chatType, chatId, openId, messageId, CommonConstant.ErrorMessage.MESSAGE_PARSE_FAILED);
        }
    }
    
    /**
     * 发送回复消息
     *
     * @param chatType 聊天类型
     * @param chatId 聊天ID
     * @param openId 用户openId
     * @param messageId 消息ID
     * @param content 回复内容
     */
    private void sendReply(String chatType, String chatId, String openId, String messageId, String content) {
        if (CommonConstant.Feishu.CHAT_TYPE_PRIVATE.equals(chatType)) {
            // 私聊直接发消息
            feishuMessageService.sendTextMessage(openId, content);
        } else {
            // 群聊回复消息
            feishuMessageService.replyGroupMessage(chatId, messageId, content);
        }
    }
}
