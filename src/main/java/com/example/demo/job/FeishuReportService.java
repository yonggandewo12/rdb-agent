package com.example.demo.job;

import com.example.demo.service.LlmService;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Slf4j
@Service
public class FeishuReportService {

    @Value("${feishu.app-id}")
    private String appId;
    
    @Value("${feishu.app-secret}")
    private String appSecret;
    
    @Autowired
    private LlmService llmService;
    
    private static final MediaType JSON = MediaType.parse("application/json; charset=utf-8");
    private static final OkHttpClient CLIENT = new OkHttpClient.Builder()
            .connectTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
            .readTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
            .build();
    
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    
    public void generateAndSendReport(String chatId, Map<String, Object> reportData) {
        try {
            String tenantToken = getTenantAccessToken();
            
            // 生成报告内容
            String reportContent = generateReportContent(reportData);
            
            // 创建飞书云文档
            String docId = createFeishuDoc(tenantToken, reportData);
            
            // 写入报告内容到云文档
            writeContentToDoc(tenantToken, docId, reportContent);
            
            // 设置文档公开访问
            String docUrl = setDocPublicAccess(tenantToken, docId);
            
            // 发送通知到群聊
            sendGroupMessage(tenantToken, chatId, docUrl, reportData);
            
            log.info("报告生成并发送成功，文档链接: {}", docUrl);
            
        } catch (Exception e) {
            log.error("生成报告失败", e);
        }
    }
    
    private String getTenantAccessToken() throws Exception {
        String url = "https://open.feishu.cn/open-apis/auth/v3/tenant_access_token/internal";
        String json = String.format("{\"app_id\":\"%s\",\"app_secret\":\"%s\"}", appId, appSecret);
        
        RequestBody body = RequestBody.create(json, JSON);
        Request request = new Request.Builder().url(url).post(body).build();
        
        try (Response response = CLIENT.newCall(request).execute()) {
            String responseBody = response.body().string();
            if (response.isSuccessful()) {
                com.alibaba.fastjson.JSONObject result = 
                        com.alibaba.fastjson.JSONObject.parseObject(responseBody);
                return result.getString("tenant_access_token");
            }
            throw new RuntimeException("获取tenant_access_token失败: " + responseBody);
        }
    }
    
    private String generateReportContent(Map<String, Object> reportData) {
        String taskName = (String) reportData.get("taskName");
        String groupId = (String) reportData.get("groupId");
        String redisHost = (String) reportData.get("redisHost");
        String generateTime = (String) reportData.get("generateTime");
        
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> slowQueries = (List<Map<String, Object>>) reportData.get("slowQueries");
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> bigKeys = (List<Map<String, Object>>) reportData.get("bigKeys");
        
        StringBuilder content = new StringBuilder();
        content.append("# Redis监控报告\n\n");
        content.append("## 基本信息\n");
        content.append(String.format("- 任务名称: %s\n", taskName));
        content.append(String.format("- 分组ID: %s\n", groupId));
        content.append(String.format("- Redis实例: %s\n", redisHost));
        content.append(String.format("- 生成时间: %s\n\n", generateTime));
        
        // 慢查询分析
        content.append("## 慢查询分析\n");
        if (slowQueries == null || slowQueries.isEmpty()) {
            content.append("✅ 未发现慢查询\n\n");
        } else {
            content.append(String.format("⚠️ 发现 %d 条慢查询:\n\n", slowQueries.size()));
            for (int i = 0; i < Math.min(slowQueries.size(), 10); i++) {
                Map<String, Object> query = slowQueries.get(i);
                content.append(String.format("%d. 执行时间: %s, 命令: %s\n", 
                        i + 1, query.get("executionTime"), query.get("command")));
            }
            if (slowQueries.size() > 10) {
                content.append(String.format("\n... 共 %d 条慢查询\n", slowQueries.size()));
            }
            content.append("\n");
        }
        
        // 大key分析
        content.append("## 大Key分析\n");
        if (bigKeys == null || bigKeys.isEmpty()) {
            content.append("✅ 未发现大Key\n\n");
        } else {
            content.append(String.format("⚠️ 发现 %d 个大Key:\n\n", bigKeys.size()));
            for (int i = 0; i < Math.min(bigKeys.size(), 10); i++) {
                Map<String, Object> key = bigKeys.get(i);
                content.append(String.format("%d. Key: %s, 类型: %s, 大小: %s, 元素数: %s\n", 
                        i + 1, key.get("key"), key.get("type"), key.get("memory"), key.get("elements")));
            }
            if (bigKeys.size() > 10) {
                content.append(String.format("\n... 共 %d 个大Key\n", bigKeys.size()));
            }
            content.append("\n");
        }
        
        // 调用LLM生成分析建议
        try {
            String llmAdvice = llmService.analyzeRedisReport(slowQueries, bigKeys);
            content.append("## 优化建议\n");
            content.append(llmAdvice);
        } catch (Exception e) {
            log.warn("LLM分析失败，使用默认建议", e);
            content.append("## 优化建议\n");
            content.append("建议定期监控Redis性能，关注慢查询和大Key情况，及时进行优化。\n");
        }
        
        return content.toString();
    }
    
    private String createFeishuDoc(String token, Map<String, Object> reportData) throws Exception {
        String url = "https://open.feishu.cn/open-apis/docx/v1/documents";
        String taskName = (String) reportData.get("taskName");
        String title = String.format("Redis监控报告-%s-%s", taskName, 
                LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss")));
        
        String json = String.format("{\"title\":\"%s\"}", title);
        
        RequestBody body = RequestBody.create(json, JSON);
        Request request = new Request.Builder()
                .url(url)
                .post(body)
                .addHeader("Authorization", "Bearer " + token)
                .addHeader("Content-Type", "application/json; charset=utf-8")
                .build();
        
        try (Response response = CLIENT.newCall(request).execute()) {
            String responseBody = response.body().string();
            if (response.isSuccessful()) {
                com.alibaba.fastjson.JSONObject result = 
                        com.alibaba.fastjson.JSONObject.parseObject(responseBody);
                return result.getJSONObject("data")
                        .getJSONObject("document")
                        .getString("document_id");
            }
            throw new RuntimeException("创建云文档失败: " + responseBody);
        }
    }
    
    private void writeContentToDoc(String token, String docId, String content) throws Exception {
        String url = String.format("https://open.feishu.cn/open-apis/docx/v1/documents/%s/blocks/%s/children", docId, docId);
        
        String[] lines = content.split("\n");
        List<Map<String, Object>> blocks = new ArrayList<>();
        
        for (String line : lines) {
            Map<String, Object> block = new HashMap<>();
            
            if (line.startsWith("# ")) {
                block.put("block_type", 3);
                block.put("heading1", createTextElement(line.substring(2)));
            } else if (line.startsWith("## ")) {
                block.put("block_type", 4);
                block.put("heading2", createTextElement(line.substring(3)));
            } else if (line.startsWith("### ")) {
                block.put("block_type", 5);
                block.put("heading3", createTextElement(line.substring(4)));
            } else if (!line.trim().isEmpty()) {
                block.put("block_type", 2);
                block.put("text", createTextElement(line));
            }
            
            if (!block.isEmpty()) {
                blocks.add(block);
            }
        }
        
        String json = String.format("{\"children\":%s}", 
                com.alibaba.fastjson.JSONObject.toJSONString(blocks));
        
        RequestBody body = RequestBody.create(json, JSON);
        Request request = new Request.Builder()
                .url(url)
                .post(body)
                .addHeader("Authorization", "Bearer " + token)
                .addHeader("Content-Type", "application/json; charset=utf-8")
                .build();
        
        try (Response response = CLIENT.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                log.warn("写入文档内容失败: {}", response.body().string());
            }
        }
    }
    
    private Map<String, Object> createTextElement(String text) {
        Map<String, Object> element = new HashMap<>();
        element.put("elements", Arrays.asList(
                createTextRun(text)
        ));
        element.put("style", new HashMap<>());
        return element;
    }
    
    private Map<String, Object> createTextRun(String text) {
        Map<String, Object> textRun = new HashMap<>();
        textRun.put("content", text);
        return textRun;
    }
    
    private String setDocPublicAccess(String token, String docId) throws Exception {
        String url = String.format("https://open.feishu.cn/open-apis/drive/v1/permissions/%s/public?type=docx", docId);
        
        String json = "{\"external_access_entity\":\"open\",\"link_share_entity\":\"anyone_readable\"}";
        
        RequestBody body = RequestBody.create(json, JSON);
        Request request = new Request.Builder()
                .url(url)
                .patch(body)
                .addHeader("Authorization", "Bearer " + token)
                .addHeader("Content-Type", "application/json; charset=utf-8")
                .build();
        
        try (Response response = CLIENT.newCall(request).execute()) {
            // 返回文档链接
            return String.format("https://xxx.feishu.cn/docx/%s", docId);
        }
    }
    
    private void sendGroupMessage(String token, String chatId, String docUrl, Map<String, Object> reportData) throws Exception {
        String url = "https://open.feishu.cn/open-apis/im/v1/messages?receive_id_type=chat_id";
        
        String taskName = (String) reportData.get("taskName");
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> slowQueries = (List<Map<String, Object>>) reportData.get("slowQueries");
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> bigKeys = (List<Map<String, Object>>) reportData.get("bigKeys");
        
        String summary = String.format("【Redis监控报告】\n" +
                "📋 任务: %s\n" +
                "🐌 慢查询: %d条\n" +
                "🔴 大Key: %d个\n" +
                "📎 报告链接: %s", 
                taskName,
                slowQueries != null ? slowQueries.size() : 0,
                bigKeys != null ? bigKeys.size() : 0,
                docUrl);
        
        String json = String.format(
                "{\"receive_id\":\"%s\",\"msg_type\":\"text\",\"content\":{\"text\":\"%s\"}}",
                chatId, summary.replace("\n", "\\n").replace("\"", "\\\""));
        
        RequestBody body = RequestBody.create(json, JSON);
        Request request = new Request.Builder()
                .url(url)
                .post(body)
                .addHeader("Authorization", "Bearer " + token)
                .addHeader("Content-Type", "application/json; charset=utf-8")
                .build();
        
        try (Response response = CLIENT.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                log.warn("发送群消息失败: {}", response.body().string());
            }
        }
    }
}
