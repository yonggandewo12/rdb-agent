package com.example.demo.service;

public interface LlmService {
    
    /**
     * 调用大模型分析用户query，返回要执行的Redis操作指令
     * @param userQuery 用户输入的查询内容
     * @return 结构化的Redis操作指令（JSON格式）
     */
    String analyzeQuery(String userQuery);
    
    /**
     * 执行Redis操作并生成自然语言回答
     * @param operation Redis操作结果
     * @param userQuery 用户原始查询
     * @return 自然语言回答
     */
    String generateResponse(Object operation, String userQuery);
}
