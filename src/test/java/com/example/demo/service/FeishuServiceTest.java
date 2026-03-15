package com.example.demo.service;

import com.example.demo.service.impl.FeishuServiceImpl;
import com.example.demo.service.impl.LlmServiceImpl;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.TestPropertySource;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@SpringBootTest(classes = FeishuServiceImpl.class)
@TestPropertySource(properties = "feishu.verification-token=test-token")
public class FeishuServiceTest {
    
    @MockBean
    private LlmServiceImpl llmService;
    
    @MockBean
    private FeishuMessageService feishuMessageService;
    
    @Autowired
    private FeishuService feishuService;
    
    @Test
    public void testHandleChallenge() {
        // 测试正确token不抛异常
        assertDoesNotThrow(() -> feishuService.handleChallenge("challenge123", "test-token"));
        
        // 测试错误token抛异常
        assertThrows(RuntimeException.class, () -> feishuService.handleChallenge("challenge123", "wrong-token"));
    }
    
    @Test
    public void testHandleMessageEvent() {
        String content = "{\"text\":\"@_user_1 查询key user:1\"}";
        
        when(llmService.analyzeQuery(anyString())).thenReturn("{\"operation\":\"get\",\"key\":\"user:1\"}");
        when(llmService.executeRedisOperation(anyString())).thenReturn("test-value");
        when(llmService.generateResponse(any(), anyString())).thenReturn("查询结果：test-value");
        
        feishuService.handleMessageEvent(
                "im.message.receive_v1",
                content,
                "test-chat-id",
                "test-open-id",
                "test-msg-id",
                "group"
        );
        
        verify(llmService, times(1)).analyzeQuery("查询key user:1");
        verify(llmService, times(1)).executeRedisOperation("{\"operation\":\"get\",\"key\":\"user:1\"}");
        verify(feishuMessageService, times(1)).replyGroupMessage(
                "test-chat-id",
                "test-msg-id",
                "查询结果：test-value"
        );
    }
    
    @Test
    public void testHandlePrivateMessage() {
        String content = "{\"text\":\"查询key user:1\"}";
        
        when(llmService.analyzeQuery(anyString())).thenReturn("{\"operation\":\"get\",\"key\":\"user:1\"}");
        when(llmService.executeRedisOperation(anyString())).thenReturn("test-value");
        when(llmService.generateResponse(any(), anyString())).thenReturn("查询结果：test-value");
        
        feishuService.handleMessageEvent(
                "im.message.receive_v1",
                content,
                "test-chat-id",
                "test-open-id",
                "test-msg-id",
                "private"
        );
        
        verify(feishuMessageService, times(1)).sendTextMessage(
                "test-open-id",
                "查询结果：test-value"
        );
    }
}
