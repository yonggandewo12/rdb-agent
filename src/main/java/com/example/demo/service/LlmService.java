package com.example.demo.service;

import java.util.List;
import java.util.Map;

public interface LlmService {
    
    /**
     * 调用大模型分析用户query，返回要执行的Redis操作指令
     * @param userQuery 用户输入的查询内容
     * @return 结构化的Redis操作指令（JSON格式）
     */
    String analyzeQuery(String userQuery);
    
    /**
     * 直接调用大模型回答用户的问题（不执行Redis操作）
     * @param userQuery 用户输入的查询内容
     * @return 自然语言回答
     */
    String directAnswer(String userQuery);
    
    /**
     * 执行Redis操作并生成自然语言回答
     * @param operation Redis操作结果
     * @param userQuery 用户原始查询
     * @return 自然语言回答
     */
    String generateResponse(Object operation, String userQuery);
    
    /**
     * 分析Redis监控报告，生成优化建议
     * @param slowQueries 慢查询列表
     * @param bigKeys 大key列表
     * @return 优化建议
     */
    String analyzeRedisReport(List<Map<String, Object>> slowQueries, List<Map<String, Object>> bigKeys);
}
