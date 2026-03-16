package com.example.demo.dto.feishu;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class FeishuEventRequest {
    
    @JsonProperty("schema")
    private String schema;
    
    @JsonProperty("header")
    private Header header;
    
    @JsonProperty("event")
    private Event event;
    
    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Header {
        @JsonProperty("event_id")
        private String eventId;
        
        @JsonProperty("event_type")
        private String eventType;
        
        @JsonProperty("create_time")
        private String createTime;
        
        @JsonProperty("token")
        private String token;
        
        @JsonProperty("app_id")
        private String appId;
        
        @JsonProperty("tenant_key")
        private String tenantKey;
    }
    
    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Event {
        @JsonProperty("message")
        private Message message;
        
        @JsonProperty("sender")
        private Sender sender;
    }
    
    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Message {
        @JsonProperty("message_id")
        private String messageId;
        
        @JsonProperty("content")
        private String content;
        
        @JsonProperty("create_time")
        private String createTime;
        
        @JsonProperty("chat_id")
        private String chatId;
        
        @JsonProperty("chat_type")
        private String chatType;
        
        @JsonProperty("message_type")
        private String messageType;
    }
    
    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Sender {
        @JsonProperty("sender_id")
        private SenderId senderId;
        
        @JsonProperty("sender_type")
        private String senderType;
        
        @JsonProperty("tenant_key")
        private String tenantKey;
    }
    
    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class SenderId {
        @JsonProperty("union_id")
        private String unionId;
        
        @JsonProperty("user_id")
        private String userId;
        
        @JsonProperty("open_id")
        private String openId;
    }
}
