package com.example.demo.controller;

import com.example.demo.dto.feishu.FeishuChallengeRequest;
import com.example.demo.dto.feishu.FeishuEventRequest;
import com.example.demo.service.FeishuService;
import com.example.demo.utils.FeishuCryptoUtil;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * 飞书事件回调控制器
 *
 * @author system
 * @date 2026-03-14
 */
@RestController
@RequestMapping("/api/feishu")
public class FeishuController {
    
    private static final Logger logger = LoggerFactory.getLogger(FeishuController.class);
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    
    @Value("${feishu.encrypt-key:}")
    private String encryptKey;
    
    @Value("${feishu.verification-token}")
    private String verificationToken;
    
    private final FeishuService feishuService;
    
    @Autowired
    public FeishuController(FeishuService feishuService) {
        this.feishuService = feishuService;
    }
    
    /**
     * 飞书事件回调接口
     * 支持明文和加密两种模式，自动检测处理
     */
    @PostMapping("/webhook")
    public ResponseEntity<?> webhook(@RequestBody String requestBody) {
        try {
            String plainRequestBody = requestBody;
            
            // 1. 检测是否是加密请求
            JsonNode rootNode = OBJECT_MAPPER.readTree(requestBody);
            if (rootNode.has("encrypt")) {
                String encryptContent = rootNode.get("encrypt").asText();
                if (!StringUtils.hasText(encryptKey)) {
                    logger.error("Received encrypted event but encrypt-key is not configured");
                    return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Encrypt key not configured");
                }
                
                // 解密加密内容
                plainRequestBody = FeishuCryptoUtil.decrypt(encryptKey, encryptContent);
                logger.debug("Decrypted feishu event: {}", plainRequestBody);
            }
            
            // 2. 处理解密后的明文请求
            JsonNode plainNode = OBJECT_MAPPER.readTree(plainRequestBody);
            
            // 3. 判断是否是URL验证请求
            if (plainNode.has("type") && "url_verification".equals(plainNode.get("type").asText())) {
                String challenge = plainNode.get("challenge").asText();
                String token = plainNode.get("token").asText();
                
                // 校验token
                if (!verificationToken.equals(token)) {
                    logger.error("Invalid challenge token: {}", token);
                    return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Invalid token");
                }
                
                Map<String, String> response = new HashMap<>();
                response.put("challenge", challenge);
                return ResponseEntity.ok(response);
            }
            
            // 4. 校验事件token（所有事件都需要校验，防止伪造请求）
            String eventToken = plainNode.get("header").get("token").asText();
            if (!verificationToken.equals(eventToken)) {
                logger.error("Invalid event token: {}", eventToken);
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Invalid token");
            }
            
            // 5. 处理消息事件
            FeishuEventRequest eventRequest = OBJECT_MAPPER.readValue(plainRequestBody, FeishuEventRequest.class);
            
            String eventType = eventRequest.getHeader().getEventType();
            logger.info("Received Feishu event: {}", eventType);
            
            // 只处理@机器人的消息事件
            if ("im.message.receive_v1".equals(eventType)) {
                FeishuEventRequest.Message message = eventRequest.getEvent().getMessage();
                FeishuEventRequest.Sender sender = eventRequest.getEvent().getSender();
                
                feishuService.handleMessageEvent(
                        eventType,
                        message.getContent(),
                        message.getChatId(),
                        sender.getSenderId().getOpenId(),
                        message.getMessageId(),
                        message.getChatType()
                );
            }
            
            return ResponseEntity.ok().build();
            
        } catch (Exception e) {
            logger.error("Failed to process feishu webhook request", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
}
