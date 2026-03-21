package com.example.demo.service.impl;

import com.example.demo.constant.CommonConstant;
import com.example.demo.entity.RedisDatasource;
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
    private static final String PROMPT_TEMPLATE = "你是一个智能助手，主要负责帮助用户操作Redis。请按以下规则处理：\n" +
            "1. 如果问题与Redis完全无关，请直接回答用户的问题，不要输出JSON格式，直接返回自然语言回答。\n" +
            "2. 如果问题与Redis相关，请输出要执行的Redis操作，格式为JSON，只返回JSON不要其他内容。\n" +
            "支持的操作：get, set, delete, exists, hget, hgetall, hset, hmset, hdelete, lrange, lpush, rpush, llen, smembers, sadd, srem, ttl, keys等。\n" +
            "示例1：用户问\"查询key为user:1的value\"，返回{\"operation\":\"get\",\"key\":\"user:1\"}\n" +
            "示例2：用户问\"查询哈希key为user:info的name字段\"，返回{\"operation\":\"hget\",\"key\":\"user:info\",\"field\":\"name\"}\n" +
            "示例3：用户问\"设置key为name的值为张三，过期时间60秒\"，返回{\"operation\":\"set\",\"key\":\"name\",\"value\":\"张三\",\"expire\":60}\n" +
            "示例4：用户问\"设置哈希user:info的age字段为25\"，返回{\"operation\":\"hset\",\"key\":\"user:info\",\"field\":\"age\",\"value\":\"25\"}\n" +
            "注意：如果操作的是 list（如 lpush、rpush）或 set（如 sadd、srem），请统一使用 values 字段，格式为字符串数组，不要使用 value 字段。\n" +
            "示例5：用户问\"向列表user:list右侧插入元素hello\"，返回{\"operation\":\"rpush\",\"key\":\"user:list\",\"values\":[\"hello\"]}\n" +
            "示例6：用户问\"向列表user:list左侧插入元素world\"，返回{\"operation\":\"lpush\",\"key\":\"user:list\",\"values\":[\"world\"]}\n" +
            "示例7：用户问\"向列表user:list右侧插入元素a、b、c\"，返回{\"operation\":\"rpush\",\"key\":\"user:list\",\"values\":[\"a\",\"b\",\"c\"]}\n" +
            "示例8：用户问\"向集合tags添加元素java、redis\"，返回{\"operation\":\"sadd\",\"key\":\"tags\",\"values\":[\"java\",\"redis\"]}\n" +
            "示例9：用户问\"从集合tags移除元素redis\"，返回{\"operation\":\"srem\",\"key\":\"tags\",\"values\":[\"redis\"]}\n" +
            "示例10：用户问\"删除哈希user:info的name字段\"，返回{\"operation\":\"hdelete\",\"key\":\"user:info\",\"fields\":[\"name\"]}\n" +
            "示例11：用户问\"返回所有的keys\"，返回{\"operation\":\"keys\",\"pattern\":\"*\"}\n" +
            "示例12：用户问\"查看所有key\"，返回{\"operation\":\"keys\",\"pattern\":\"*\"}\n" +
            "示例13：用户问\"查看以user:开头的所有key\"，返回{\"operation\":\"keys\",\"pattern\":\"user:*\"}\n" +
            "示例14：用户问\"列出所有keys\"，返回{\"operation\":\"keys\",\"pattern\":\"*\"}\n" +
            "示例15：用户问\"返回所有key\"，返回{\"operation\":\"keys\",\"pattern\":\"*\"}\n" +
            "示例16：用户问\"列出所有的key\"，返回{\"operation\":\"keys\",\"pattern\":\"*\"}\n" +
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
     * @param datasource 已解析的数据源
     * @return 操作结果
     */
    public Object executeRedisOperation(String operationJson, RedisDatasource datasource,
                                         DynamicRedisService dynamicRedisService) {
        if (!StringUtils.hasText(operationJson)) {
            return CommonConstant.ErrorMessage.PARAM_NULL;
        }
        
        try {
            JsonNode opNode = OBJECT_MAPPER.readTree(operationJson);
            String operation = opNode.get("operation").asText();
            String key = opNode.has("key") ? opNode.get("key").asText() : null;
            
            switch (operation.toLowerCase()) {
                case CommonConstant.Llm.OP_GET:
                    return dynamicRedisService.get(datasource, key);
                case CommonConstant.Llm.OP_EXISTS:
                    return dynamicRedisService.exists(datasource, key);
                case CommonConstant.Llm.OP_TTL:
                    return dynamicRedisService.ttl(datasource, key);
                case CommonConstant.Llm.OP_HGET:
                    String field = opNode.get("field").asText();
                    return dynamicRedisService.hget(datasource, key, field);
                case CommonConstant.Llm.OP_HGETALL:
                    return dynamicRedisService.hgetAll(datasource, key);
                case CommonConstant.Llm.OP_LRANGE:
                    long start = opNode.has("start") ? opNode.get("start").asLong() : 0;
                    long end = opNode.has("end") ? opNode.get("end").asLong() : -1;
                    return dynamicRedisService.lrange(datasource, key, start, end);
                case CommonConstant.Llm.OP_SMEMBERS:
                    return dynamicRedisService.smembers(datasource, key);
                case CommonConstant.Llm.OP_KEYS:
                    String keysPattern = opNode.has("pattern") ? opNode.get("pattern").asText() : "*";
                    return dynamicRedisService.keys(datasource, keysPattern);
                case CommonConstant.Llm.OP_LLEN:
                    return dynamicRedisService.llen(datasource, key);
                case CommonConstant.Llm.OP_SET:
                    String value = opNode.get("value").asText();
                    if (opNode.has("expire")) {
                        long expire = opNode.get("expire").asLong();
                        dynamicRedisService.set(datasource, key, value, expire);
                        return "设置成功";
                    } else {
                        dynamicRedisService.set(datasource, key, value);
                        return "设置成功";
                    }
                case CommonConstant.Llm.OP_DELETE:
                    return Boolean.TRUE.equals(dynamicRedisService.delete(datasource, key)) ? "删除成功" : "删除失败";
                case CommonConstant.Llm.OP_HSET:
                    String hField = opNode.get("field").asText();
                    Object hValue = opNode.get("value").asText();
                    dynamicRedisService.hset(datasource, key, hField, hValue);
                    return "哈希字段设置成功";
                case CommonConstant.Llm.OP_HDEL:
                    String[] delFields = parseFields(opNode);
                    Long delCount = dynamicRedisService.hdelete(datasource, key, (Object[]) delFields);
                    return "成功删除哈希字段" + delCount + "个";
                case CommonConstant.Llm.OP_LPUSH:
                    String[] lValues = parseValues(opNode);
                    Long lpushCount = 0L;
                    for (String lValue : lValues) {
                        lpushCount = dynamicRedisService.lpush(datasource, key, lValue);
                    }
                    return "列表左侧插入成功，当前列表长度：" + lpushCount;
                case CommonConstant.Llm.OP_RPUSH:
                    String[] rValues = parseValues(opNode);
                    Long rpushCount = 0L;
                    for (String rValue : rValues) {
                        rpushCount = dynamicRedisService.rpush(datasource, key, rValue);
                    }
                    return "列表右侧插入成功，当前列表长度：" + rpushCount;
                case CommonConstant.Llm.OP_SADD:
                    String[] sValues = parseValues(opNode);
                    Long saddCount = dynamicRedisService.sadd(datasource, key, sValues);
                    return "集合添加成功，添加元素数量：" + saddCount;
                case CommonConstant.Llm.OP_SREM:
                    String[] sremValues = parseValues(opNode);
                    Long sremCount = dynamicRedisService.srem(datasource, key, sremValues);
                    return "集合移除成功，移除元素数量：" + sremCount;
                default:
                    return CommonConstant.ErrorMessage.UNSUPPORTED_OPERATION + operation;
            }
        } catch (JsonProcessingException e) {
            logger.error("Failed to parse operation json: {}", operationJson, e);
            return "操作解析失败";
        }
    }

    private String[] parseValues(JsonNode opNode) {
        if (opNode.has("values") && opNode.get("values").isArray()) {
            JsonNode valuesNode = opNode.get("values");
            String[] values = new String[valuesNode.size()];
            for (int i = 0; i < valuesNode.size(); i++) {
                values[i] = valuesNode.get(i).asText();
            }
            return values;
        }
        if (opNode.has("values")) {
            return opNode.get("values").asText().split(",");
        }
        if (opNode.has("value")) {
            return new String[]{opNode.get("value").asText()};
        }
        return new String[0];
    }

    private String[] parseFields(JsonNode opNode) {
        if (opNode.has("fields") && opNode.get("fields").isArray()) {
            JsonNode fieldsNode = opNode.get("fields");
            String[] fields = new String[fieldsNode.size()];
            for (int i = 0; i < fieldsNode.size(); i++) {
                fields[i] = fieldsNode.get(i).asText();
            }
            return fields;
        }
        if (opNode.has("fields")) {
            return opNode.get("fields").asText().split(",");
        }
        if (opNode.has("field")) {
            return new String[]{opNode.get("field").asText()};
        }
        return new String[0];
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
            systemMessage.put("content", "你是一个智能助手，请按规则处理用户问题。");
            
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

    @Override
    public String directAnswer(String userQuery) {
        if (!StringUtils.hasText(userQuery)) {
            throw new IllegalArgumentException(CommonConstant.ErrorMessage.PARAM_NULL);
        }
        
        try {
            Map<String, Object> systemMessage = new HashMap<>(4);
            systemMessage.put("role", CommonConstant.Llm.ROLE_SYSTEM);
            systemMessage.put("content", "你是一个友好的智能助手，请直接回答用户的问题。");
            
            Map<String, Object> userMessage = new HashMap<>(4);
            userMessage.put("role", CommonConstant.Llm.ROLE_USER);
            userMessage.put("content", userQuery);
            
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
                    return "抱歉，我遇到了一些问题，请稍后再试。";
                }
                String responseBody = response.body().string();
                JsonNode jsonNode = OBJECT_MAPPER.readTree(responseBody);
                return jsonNode.get("choices").get(0).get("message").get("content").asText();
            }
            
        } catch (Exception e) {
            logger.error("Failed to get direct answer from LLM", e);
            return "抱歉，我遇到了一些问题，请稍后再试。";
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
            String key = opNode.has("key") ? opNode.get("key").asText() : null;
            
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
                case CommonConstant.Llm.OP_KEYS:
                    String keysPattern = opNode.has("pattern") ? opNode.get("pattern").asText() : "*";
                    return redisService.keys(keysPattern);
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
                    String[] delFields = parseFields(opNode);
                    Long delCount = redisService.hdelete(key, (Object[]) delFields);
                    return "成功删除哈希字段" + delCount + "个";
                case CommonConstant.Llm.OP_LPUSH:
                    String[] lValues = parseValues(opNode);
                    Long lpushCount = 0L;
                    for (String lValue : lValues) {
                        lpushCount = redisService.lpush(key, lValue);
                    }
                    return "列表左侧插入成功，当前列表长度：" + lpushCount;
                case CommonConstant.Llm.OP_RPUSH:
                    String[] rValues = parseValues(opNode);
                    Long rpushCount = 0L;
                    for (String rValue : rValues) {
                        rpushCount = redisService.rpush(key, rValue);
                    }
                    return "列表右侧插入成功，当前列表长度：" + rpushCount;
                case CommonConstant.Llm.OP_SADD:
                    String[] sValues = parseValues(opNode);
                    Long saddCount = redisService.sadd(key, sValues);
                    return "集合添加成功，添加元素数量：" + saddCount;
                case CommonConstant.Llm.OP_SREM:
                    String[] sremValues = parseValues(opNode);
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
