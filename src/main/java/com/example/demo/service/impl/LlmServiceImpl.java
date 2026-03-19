package com.example.demo.service.impl;

import com.example.demo.constant.CommonConstant;
import com.example.demo.service.DynamicRedisService;
import com.example.demo.service.LlmService;
import com.example.demo.service.RedisService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * 大模型服务实现类
 *
 * @author system
 * @date 2026-03-12
 */
@Service
public class LlmServiceImpl implements LlmService {
    
    private static final Logger logger = LoggerFactory.getLogger(LlmServiceImpl.class);
    
    /**
     * OkHttp客户端
     */
    private static final OkHttpClient OK_HTTP_CLIENT = new OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build();
    
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    
    /**
     * 大模型提示词模板
     */
    private static final String PROMPT_TEMPLATE = "你是一个Redis查询专家，请根据用户的问题，输出要执行的Redis操作，格式为JSON，只返回JSON不要其他内容。\n" +
            "支持的操作：get, set, delete, exists, hget, hgetall, hset, hmset, hdelete, lrange, lpush, rpush, llen, smembers, sadd, srem, ttl等。\n" +
            "示例1：用户问\"查询key为user:1的value\"，返回{\"operation\":\"get\",\"key\":\"user:1\"}\n" +
            "示例2：用户问\"查询哈希key为user:info的name字段\"，返回{\"operation\":\"hget\",\"key\":\"user:info\",\"field\":\"name\"}\n" +
            "示例3：用户问\"设置key为name的值为张三，过期时间60秒\"，返回{\"operation\":\"set\",\"key\":\"name\",\"value\":\"张三\",\"expire\":60}\n" +
            "示例4：用户问\"设置哈希user:info的age字段为25\"，返回{\"operation\":\"hset\",\"key\":\"user:info\",\"field\":\"age\",\"value\":\"25\"}\n" +
            "示例5：用户问\"向列表user:list右侧插入元素hello\"，返回{\"operation\":\"rpush\",\"key\":\"user:list\",\"value\":\"hello\"}\n" +
            "示例6：用户问\"向列表user:list左侧插入元素world\"，返回{\"operation\":\"lpush\",\"key\":\"user:list\",\"value\":\"world\"}\n" +
            "用户问题：%s";
    
    @Value("${llm.api-key}")
    private String apiKey;
    
    @Value("${llm.base-url}")
    private String baseUrl;
    
    @Value("${llm.model}")
    private String model;
    
    private final RedisService redisService;
    
    @Autowired
    public LlmServiceImpl(RedisService redisService) {
        this.redisService = redisService;
    }

    /**
     * 执行Redis操作（使用动态数据源）
     *
     * @param operationJson 操作指令JSON
     * @param groupId 数据源分组ID
     * @return 操作结果
     */
    public Object executeRedisOperation(String operationJson, String groupId, 
                                         DynamicRedisService dynamicRedisService) {
        if (!StringUtils.hasText(operationJson)) {
            return CommonConstant.ErrorMessage.PARAM_NULL;
        }
        
        try {
            JsonNode opNode = OBJECT_MAPPER.readTree(operationJson);
            String operation = opNode.get("operation").asText();
            String key = opNode.get("key").asText();
            
            switch (operation.toLowerCase()) {
                case CommonConstant.Llm.OP_GET:
                    return dynamicRedisService.get(groupId, key);
                case CommonConstant.Llm.OP_EXISTS:
                    return dynamicRedisService.exists(groupId, key);
                case CommonConstant.Llm.OP_TTL:
                    return dynamicRedisService.ttl(groupId, key);
                case CommonConstant.Llm.OP_HGET:
                    String field = opNode.get("field").asText();
                    return dynamicRedisService.hget(groupId, key, field);
                case CommonConstant.Llm.OP_HGETALL:
                    return dynamicRedisService.hgetAll(groupId, key);
                case CommonConstant.Llm.OP_LRANGE:
                    long start = opNode.has("start") ? opNode.get("start").asLong() : 0;
                    long end = opNode.has("end") ? opNode.get("end").asLong() : -1;
                    return dynamicRedisService.lrange(groupId, key, start, end);
                case CommonConstant.Llm.OP_SMEMBERS:
                    return dynamicRedisService.smembers(groupId, key);
                case CommonConstant.Llm.OP_LLEN:
                    return dynamicRedisService.llen(groupId, key);
                case CommonConstant.Llm.OP_SET:
                    String value = opNode.get("value").asText();
                    if (opNode.has("expire")) {
                        long expire = opNode.get("expire").asLong();
                        dynamicRedisService.set(groupId, key, value, expire);
                        return "设置成功";
                    } else {
                        dynamicRedisService.set(groupId, key, value);
                        return "设置成功";
                    }
                case CommonConstant.Llm.OP_DELETE:
                    return Boolean.TRUE.equals(dynamicRedisService.delete(groupId, key)) ? "删除成功" : "删除失败";
                case CommonConstant.Llm.OP_HSET:
                    String hField = opNode.get("field").asText();
                    Object hValue = opNode.get("value").asText();
                    dynamicRedisService.hset(groupId, key, hField, hValue);
                    return "哈希字段设置成功";
                case CommonConstant.Llm.OP_HDEL:
                    String[] delFields = opNode.get("fields").asText().split(",");
                    Long delCount = dynamicRedisService.hdelete(groupId, key, (Object[]) delFields);
                    return "成功删除哈希字段" + delCount + "个";
                case CommonConstant.Llm.OP_LPUSH:
                    String lValue = opNode.get("value").asText();
                    Long lpushCount = dynamicRedisService.lpush(groupId, key, lValue);
                    return "列表左侧插入成功，当前列表长度：" + lpushCount;
                case CommonConstant.Llm.OP_RPUSH:
                    String rValue = opNode.get("value").asText();
                    Long rpushCount = dynamicRedisService.rpush(groupId, key, rValue);
                    return "列表右侧插入成功，当前列表长度：" + rpushCount;
                case CommonConstant.Llm.OP_SADD:
                    String[] sValues = opNode.get("values").asText().split(",");
                    Long saddCount = dynamicRedisService.sadd(groupId, key, sValues);
                    return "集合添加成功，添加元素数量：" + saddCount;
                case CommonConstant.Llm.OP_SREM:
                    String[] sremValues = opNode.get("values").asText().split(",");
                    Long sremCount = dynamicRedisService.srem(groupId, key, sremValues);
                    return "集合移除成功，移除元素数量：" + sremCount;
                default:
                    return CommonConstant.ErrorMessage.UNSUPPORTED_OPERATION + operation;
            }
        } catch (JsonProcessingException e) {
            logger.error("Failed to parse operation json: {}", operationJson, e);
            return "操作解析失败";
        }
    }

    /**
     * 调用大模型分析用户query，返回要执行的Redis操作指令
     *
     * @param userQuery 用户输入的查询内容
     * @return 结构化的Redis操作指令（JSON格式）
     */
    @Override
    public String analyzeQuery(String userQuery) {
        if (!StringUtils.hasText(userQuery)) {
            throw new IllegalArgumentException(CommonConstant.ErrorMessage.PARAM_NULL);
        }
        
        try {
            // 构建提示词
            String prompt = String.format(PROMPT_TEMPLATE, userQuery);
            
            Map<String, Object> systemMessage = new HashMap<>(4);
            systemMessage.put("role", CommonConstant.Llm.ROLE_SYSTEM);
            systemMessage.put("content", "你是一个Redis操作专家，只输出JSON格式的操作指令");
            
            Map<String, Object> userMessage = new HashMap<>(4);
            userMessage.put("role", CommonConstant.Llm.ROLE_USER);
            userMessage.put("content", prompt);
            
            Map<String, Object> requestBody = new HashMap<>(8);
            requestBody.put("model", model);
            requestBody.put("messages", new Object[]{systemMessage, userMessage});
            requestBody.put("temperature", CommonConstant.Llm.DEFAULT_TEMPERATURE);
            
            Request request = new Request.Builder()
                    .url(baseUrl)
                    .addHeader("Authorization", CommonConstant.AUTH_HEADER_PREFIX + apiKey)
                    .addHeader("Content-Type", CommonConstant.CONTENT_TYPE_JSON)
                    .post(RequestBody.create(OBJECT_MAPPER.writeValueAsString(requestBody), 
                            MediaType.parse(CommonConstant.CONTENT_TYPE_JSON)))
                    .build();
            
            try (Response response = OK_HTTP_CLIENT.newCall(request).execute()) {
                if (!response.isSuccessful() || response.body() == null) {
                    logger.error("LLM API call failed, code: {}", response.code());
                    return null;
                }
                String responseBody = response.body().string();
                JsonNode jsonNode = OBJECT_MAPPER.readTree(responseBody);
                return jsonNode.get("choices").get(0).get("message").get("content").asText();
            }
            
        } catch (Exception e) {
            logger.error("Failed to analyze query with LLM", e);
            return null;
        }
    }
    
    /**
     * 执行Redis操作并生成自然语言回答
     *
     * @param operationResult Redis操作结果
     * @param userQuery 用户原始查询
     * @return 自然语言回答
     */
    @Override
    public String generateResponse(Object operationResult, String userQuery) {
        return "查询结果：\n```\n" + operationResult + "\n```";
    }
    
    /**
     * 执行Redis操作
     *
     * @param operationJson 操作指令JSON
     * @return 操作结果
     */
    public Object executeRedisOperation(String operationJson) {
        if (!StringUtils.hasText(operationJson)) {
            return CommonConstant.ErrorMessage.PARAM_NULL;
        }
        
        try {
            JsonNode opNode = OBJECT_MAPPER.readTree(operationJson);
            String operation = opNode.get("operation").asText();
            String key = opNode.get("key").asText();
            
            switch (operation.toLowerCase()) {
                case CommonConstant.Llm.OP_GET:
                    return redisService.get(key);
                case CommonConstant.Llm.OP_EXISTS:
                    return redisService.exists(key);
                case CommonConstant.Llm.OP_TTL:
                    return redisService.ttl(key);
                case CommonConstant.Llm.OP_HGET:
                    String field = opNode.get("field").asText();
                    return redisService.hget(key, field);
                case CommonConstant.Llm.OP_HGETALL:
                    return redisService.hgetAll(key);
                case CommonConstant.Llm.OP_LRANGE:
                    long start = opNode.has("start") ? opNode.get("start").asLong() : 0;
                    long end = opNode.has("end") ? opNode.get("end").asLong() : -1;
                    return redisService.lrange(key, start, end);
                case CommonConstant.Llm.OP_SMEMBERS:
                    return redisService.smembers(key);
                case CommonConstant.Llm.OP_LLEN:
                    return redisService.llen(key);
                case CommonConstant.Llm.OP_SET:
                    String value = opNode.get("value").asText();
                    if (opNode.has("expire")) {
                        long expire = opNode.get("expire").asLong();
                        redisService.set(key, value, expire);
                        return "设置成功";
                    } else {
                        redisService.set(key, value);
                        return "设置成功";
                    }
                case CommonConstant.Llm.OP_DELETE:
                    return Boolean.TRUE.equals(redisService.delete(key)) ? "删除成功" : "删除失败";
                case CommonConstant.Llm.OP_HSET:
                    String hField = opNode.get("field").asText();
                    Object hValue = opNode.get("value").asText();
                    redisService.hset(key, hField, hValue);
                    return "哈希字段设置成功";
                case CommonConstant.Llm.OP_HDEL:
                    String[] delFields = opNode.get("fields").asText().split(",");
                    Long delCount = redisService.hdelete(key, (Object[]) delFields);
                    return "成功删除哈希字段" + delCount + "个";
                case CommonConstant.Llm.OP_LPUSH:
                    String lValue = opNode.get("value").asText();
                    Long lpushCount = redisService.lpush(key, lValue);
                    return "列表左侧插入成功，当前列表长度：" + lpushCount;
                case CommonConstant.Llm.OP_RPUSH:
                    String rValue = opNode.get("value").asText();
                    Long rpushCount = redisService.rpush(key, rValue);
                    return "列表右侧插入成功，当前列表长度：" + rpushCount;
                case CommonConstant.Llm.OP_SADD:
                    String[] sValues = opNode.get("values").asText().split(",");
                    Long saddCount = redisService.sadd(key, sValues);
                    return "集合添加成功，添加元素数量：" + saddCount;
                case CommonConstant.Llm.OP_SREM:
                    String[] sremValues = opNode.get("values").asText().split(",");
                    Long sremCount = redisService.srem(key, sremValues);
                    return "集合移除成功，移除元素数量：" + sremCount;
                default:
                    return CommonConstant.ErrorMessage.UNSUPPORTED_OPERATION + operation;
            }
        } catch (JsonProcessingException e) {
            logger.error("Failed to parse operation json: {}", operationJson, e);
            return "操作解析失败";
        }
    }
    
    @Override
    public String analyzeRedisReport(java.util.List<java.util.Map<String, Object>> slowQueries, 
                                    java.util.List<java.util.Map<String, Object>> bigKeys) {
        try {
            String prompt = String.format(
                    "你是一位Redis性能优化专家。请分析以下Redis监控数据，生成简洁的优化建议：\n\n" +
                    "慢查询列表：%s\n\n" +
                    "大Key列表：%s\n\n" +
                    "请给出3-5条具体的优化建议，每条建议不超过50字。",
                    slowQueries != null ? slowQueries.toString() : "无",
                    bigKeys != null ? bigKeys.toString() : "无"
            );
            
            java.util.Map<String, Object> systemMessage = new java.util.HashMap<>(4);
            systemMessage.put("role", "system");
            systemMessage.put("content", "你是一位Redis性能优化专家，只输出优化建议，每条建议单独一行。");
            
            java.util.Map<String, Object> userMessage = new java.util.HashMap<>(4);
            userMessage.put("role", "user");
            userMessage.put("content", prompt);
            
            java.util.Map<String, Object> requestBody = new java.util.HashMap<>(8);
            requestBody.put("model", model);
            requestBody.put("messages", new Object[]{systemMessage, userMessage});
            requestBody.put("temperature", 0.3);
            
            Request request = new Request.Builder()
                    .url(baseUrl)
                    .addHeader("Authorization", CommonConstant.AUTH_HEADER_PREFIX + apiKey)
                    .addHeader("Content-Type", CommonConstant.CONTENT_TYPE_JSON)
                    .post(RequestBody.create(OBJECT_MAPPER.writeValueAsString(requestBody), 
                            MediaType.parse(CommonConstant.CONTENT_TYPE_JSON)))
                    .build();
            
            try (Response response = OK_HTTP_CLIENT.newCall(request).execute()) {
                if (!response.isSuccessful() || response.body() == null) {
                    logger.error("LLM API call failed, code: {}", response.code());
                    return "建议使用Redis调优最佳实践进行优化。";
                }
                String responseBody = response.body().string();
                JsonNode jsonNode = OBJECT_MAPPER.readTree(responseBody);
                return jsonNode.get("choices").get(0).get("message").get("content").asText();
            }
        } catch (Exception e) {
            logger.error("Failed to analyze redis report with LLM", e);
            return "建议使用Redis调优最佳实践进行优化。";
        }
    }
}
