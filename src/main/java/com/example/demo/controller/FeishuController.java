package com.example.demo.controller;

import com.example.demo.dto.feishu.FeishuChallengeRequest;
import com.example.demo.dto.feishu.FeishuEventRequest;
import com.example.demo.service.FeishuService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/feishu")
public class FeishuController {
    
    private static final Logger logger = LoggerFactory.getLogger(FeishuController.class);
    
    private final FeishuService feishuService;
    
    @Autowired
    public FeishuController(FeishuService feishuService) {
        this.feishuService = feishuService;
    }
    
    /**
     * 飞书事件回调接口
     */
    @PostMapping("/webhook")
    public ResponseEntity<?> webhook(@RequestBody String requestBody) {
        try {
            // 判断是否是URL验证请求
            if (requestBody.contains("\"type\":\"url_verification\"")) {
                FeishuChallengeRequest challengeRequest = new com.fasterxml.jackson.databind.ObjectMapper()
                        .readValue(requestBody, FeishuChallengeRequest.class);
                
                String challenge = feishuService.handleChallenge(
                        challengeRequest.getChallenge(), 
                        challengeRequest.getToken()
                );
                
                Map<String, String> response = new HashMap<>();
                response.put("challenge", challenge);
                return ResponseEntity.ok(response);
            }
            
            // 处理消息事件
            FeishuEventRequest eventRequest = new com.fasterxml.jackson.databind.ObjectMapper()
                    .readValue(requestBody, FeishuEventRequest.class);
            
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
