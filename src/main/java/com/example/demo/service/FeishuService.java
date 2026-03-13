package com.example.demo.service;

public interface FeishuService {
    
    /**
     * 处理飞书URL验证请求
     */
    String handleChallenge(String challenge, String token);
    
    /**
     * 处理飞书消息事件
     */
    void handleMessageEvent(String eventType, String content, String chatId, String openId, String messageId, String chatType);
}
