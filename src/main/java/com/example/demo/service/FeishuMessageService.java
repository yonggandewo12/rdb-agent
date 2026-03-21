package com.example.demo.service;

import java.util.List;
import java.util.Map;

public interface FeishuMessageService {
    List<Map<String, Object>> listVisibleChats(String keyword);
    
    /**
     * 发送文本消息给用户
     * @param openId 用户open_id
     * @param content 消息内容
     */
    void sendTextMessage(String openId, String content);
    
    /**
     * 回复消息到群聊
     * @param chatId 群聊id
     * @param messageId 要回复的消息id
     * @param content 回复内容
     */
    void replyGroupMessage(String chatId, String messageId, String content);
    
}
