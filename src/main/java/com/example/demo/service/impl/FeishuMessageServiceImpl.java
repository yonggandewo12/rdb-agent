package com.example.demo.service.impl;

import com.example.demo.constant.CommonConstant;
import com.example.demo.service.FeishuMessageService;
import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * 飞书消息发送服务实现类
 *
 * @author system
 * @date 2026-03-12
 */
@Service
public class FeishuMessageServiceImpl implements FeishuMessageService {
    
    private static final Logger logger = LoggerFactory.getLogger(FeishuMessageServiceImpl.class);
    
    /**
     * OkHttp客户端，配置超时时间
     */
    private static final OkHttpClient OK_HTTP_CLIENT = new OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(10, TimeUnit.SECONDS)
            .writeTimeout(10, TimeUnit.SECONDS)
            .build();
    
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    
    @Value("${feishu.app-id}")
    private String appId;
    
    @Value("${feishu.app-secret}")
    private String appSecret;
    
    /**
     * 获取租户AccessToken
     *
     * @return accessToken
     * @throws Exception 异常
     */
    private String getTenantAccessToken() throws Exception {
        Map<String, String> reqBody = new HashMap<>(4);
        reqBody.put("app_id", appId);
        reqBody.put("app_secret", appSecret);
        
        Request request = new Request.Builder()
                .url(CommonConstant.Feishu.TENANT_TOKEN_URL)
                .post(RequestBody.create(OBJECT_MAPPER.writeValueAsString(reqBody), 
                        MediaType.parse(CommonConstant.CONTENT_TYPE_JSON)))
                .build();
        
        try (Response response = OK_HTTP_CLIENT.newCall(request).execute()) {
            if (!response.isSuccessful() || response.body() == null) {
                logger.error("Get tenant access token failed, code: {}", response.code());
                throw new RuntimeException(CommonConstant.ErrorMessage.TOKEN_GET_FAILED);
            }
            Map<String, Object> respMap = OBJECT_MAPPER.readValue(response.body().string(), Map.class);
            return (String) respMap.get("tenant_access_token");
        }
    }

    @Override
    public String getChatIdByLinkToken(String linkToken) {
        if (!StringUtils.hasText(linkToken)) {
            throw new IllegalArgumentException("Link Token不能为空");
        }
        
        try {
            String accessToken = getTenantAccessToken();
            
            Map<String, String> reqBody = new HashMap<>(2);
            reqBody.put("link_token", linkToken);
            
            Request request = new Request.Builder()
                    .url("https://open.feishu.cn/open-apis/im/v1/chats/by_link_token")
                    .addHeader("Authorization", CommonConstant.AUTH_HEADER_PREFIX + accessToken)
                    .addHeader("Content-Type", CommonConstant.CONTENT_TYPE_JSON)
                    .post(RequestBody.create(OBJECT_MAPPER.writeValueAsString(reqBody), 
                            MediaType.parse(CommonConstant.CONTENT_TYPE_JSON)))
                    .build();
            
            try (Response response = OK_HTTP_CLIENT.newCall(request).execute()) {
                if (!response.isSuccessful() || response.body() == null) {
                    logger.error("Get chat id failed, code: {}", response.code());
                    throw new RuntimeException("获取ChatID失败，HTTP错误码：" + response.code());
                }
                String responseBody = response.body().string();
                Map<String, Object> respMap = OBJECT_MAPPER.readValue(responseBody, Map.class);
                
                Integer code = (Integer) respMap.get("code");
                if (code != null && code != 0) {
                    String msg = (String) respMap.get("msg");
                    logger.error("Get chat id failed, code: {}, msg: {}", code, msg);
                    throw new RuntimeException("获取ChatID失败：" + msg);
                }
                
                Map<String, Object> data = (Map<String, Object>) respMap.get("data");
                if (data == null) {
                    throw new RuntimeException("获取ChatID失败：返回数据为空");
                }
                
                Map<String, Object> chat = (Map<String, Object>) data.get("chat");
                if (chat == null) {
                    throw new RuntimeException("获取ChatID失败：未找到群聊信息");
                }
                
                return (String) chat.get("chat_id");
            }
            
        } catch (Exception e) {
            logger.error("Failed to get chat id by link token: {}", linkToken, e);
            throw new RuntimeException("获取ChatID失败：" + e.getMessage(), e);
        }
    }
    
    /**
     * 发送文本消息给用户
     *
     * @param openId 用户open_id
     * @param content 消息内容
     */
    @Override
    public void sendTextMessage(String openId, String content) {
        if (!StringUtils.hasText(openId) || !StringUtils.hasText(content)) {
            logger.error(CommonConstant.ErrorMessage.PARAM_NULL);
            return;
        }
        
        try {
            String token = getTenantAccessToken();
            
            Map<String, Object> reqBody = new HashMap<>(8);
            reqBody.put("receive_id", openId);
            reqBody.put("msg_type", CommonConstant.Feishu.MSG_TYPE_TEXT);
            
            Map<String, String> contentMap = new HashMap<>(4);
            contentMap.put("text", content);
            reqBody.put("content", OBJECT_MAPPER.writeValueAsString(contentMap));
            
            String url = CommonConstant.Feishu.MESSAGE_SEND_URL + "?receive_id_type=" 
                    + CommonConstant.Feishu.RECEIVE_ID_TYPE_OPENID;
            
            Request request = new Request.Builder()
                    .url(url)
                    .addHeader("Authorization", CommonConstant.AUTH_HEADER_PREFIX + token)
                    .post(RequestBody.create(OBJECT_MAPPER.writeValueAsString(reqBody), 
                            MediaType.parse(CommonConstant.CONTENT_TYPE_JSON)))
                    .build();
            
            try (Response response = OK_HTTP_CLIENT.newCall(request).execute()) {
                if (!response.isSuccessful()) {
                    logger.error("Send message failed, code: {}, message: {}", 
                            response.code(), response.message());
                }
            }
            
        } catch (Exception e) {
            logger.error("Failed to send feishu message", e);
        }
    }
    
    /**
     * 回复消息到群聊
     *
     * @param chatId 群聊id
     * @param messageId 要回复的消息id
     * @param content 回复内容
     */
    @Override
    public void replyGroupMessage(String chatId, String messageId, String content) {
        if (!StringUtils.hasText(chatId) || !StringUtils.hasText(messageId) 
                || !StringUtils.hasText(content)) {
            logger.error(CommonConstant.ErrorMessage.PARAM_NULL);
            return;
        }
        
        try {
            String token = getTenantAccessToken();
            
            Map<String, Object> reqBody = new HashMap<>(8);
            Map<String, String> contentMap = new HashMap<>(4);
            contentMap.put("text", content);
            reqBody.put("content", OBJECT_MAPPER.writeValueAsString(contentMap));
            reqBody.put("msg_type", CommonConstant.Feishu.MSG_TYPE_TEXT);
            
            String url = CommonConstant.Feishu.MESSAGE_REPLY_PREFIX + messageId 
                    + CommonConstant.Feishu.MESSAGE_REPLY_SUFFIX;
            
            Request request = new Request.Builder()
                    .url(url)
                    .addHeader("Authorization", CommonConstant.AUTH_HEADER_PREFIX + token)
                    .post(RequestBody.create(OBJECT_MAPPER.writeValueAsString(reqBody), 
                            MediaType.parse(CommonConstant.CONTENT_TYPE_JSON)))
                    .build();
            
            try (Response response = OK_HTTP_CLIENT.newCall(request).execute()) {
                if (!response.isSuccessful()) {
                    logger.error("Reply message failed, code: {}, message: {}", 
                            response.code(), response.message());
                }
            }
            
        } catch (Exception e) {
            logger.error("Failed to reply group message", e);
        }
    }
}
