package com.example.demo.dto.feishu;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class FeishuChallengeRequest {
    
    @JsonProperty("challenge")
    private String challenge;
    
    @JsonProperty("token")
    private String token;
    
    @JsonProperty("type")
    private String type;
}
