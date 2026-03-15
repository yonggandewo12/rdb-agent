package com.example.demo.controller;

import com.example.demo.service.FeishuService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(FeishuController.class)
@TestPropertySource(properties = "feishu.verification-token=test-token")
public class FeishuControllerTest {
    
    @Autowired
    private MockMvc mockMvc;
    
    @MockBean
    private FeishuService feishuService;
    
    @Test
    public void testChallengeRequest() throws Exception {
        String challengeJson = "{" +
                "\"type\":\"url_verification\"," +
                "\"challenge\":\"test-challenge-123\"," +
                "\"token\":\"test-token\"" +
                "}";
        
        mockMvc.perform(post("/api/feishu/webhook")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(challengeJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.challenge").value("test-challenge-123"));
    }
    
    @Test
    public void testMessageEvent() throws Exception {
        String eventJson = "{" +
                "\"schema\":\"2.0\"," +
                "\"header\":{" +
                "\"event_id\":\"test-event-id\"," +
                "\"event_type\":\"im.message.receive_v1\"," +
                "\"token\":\"test-token\"" +
                "}," +
                "\"event\":{" +
                "\"message\":{" +
                "\"message_id\":\"test-msg-id\"," +
                "\"content\":\"{\\\"text\\\":\\\"@_user_1 查询key test\\\"}\"," +
                "\"chat_id\":\"test-chat-id\"," +
                "\"chat_type\":\"group\"" +
                "}," +
                "\"sender\":{" +
                "\"sender_id\":{" +
                "\"open_id\":\"test-open-id\"" +
                "}" +
                "}" +
                "}" +
                "}";
        
        mockMvc.perform(post("/api/feishu/webhook")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(eventJson))
                .andExpect(status().isOk());
    }
}
