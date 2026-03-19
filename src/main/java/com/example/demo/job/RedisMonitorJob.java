package com.example.demo.job;

import com.example.demo.config.DynamicRedisSourceManager;
import com.example.demo.entity.RedisDatasource;
import com.example.demo.entity.ScheduledTask;
import com.example.demo.service.RedisDatasourceService;
import com.example.demo.service.ScheduledTaskService;
import lombok.extern.slf4j.Slf4j;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import redis.clients.jedis.Jedis;

import java.time.LocalDateTime;
import java.util.*;

@Slf4j
@Component
public class RedisMonitorJob implements Job {

    @Autowired
    private ScheduledTaskService scheduledTaskService;
    
    @Autowired
    private RedisDatasourceService redisDatasourceService;
    
    @Autowired
    private DynamicRedisSourceManager redisSourceManager;
    
    @Autowired
    private FeishuReportService feishuReportService;

    @Override
    public void execute(JobExecutionContext context) throws JobExecutionException {
        String taskId = context.getJobDetail().getJobDataMap().getString("taskId");
        
        ScheduledTask task = scheduledTaskService.lambdaQuery()
                .eq(ScheduledTask::getTaskId, taskId)
                .one();
        
        if (task == null || task.getEnabled() != 1) {
            log.warn("任务不存在或已禁用: {}", taskId);
            return;
        }
        
        log.info("开始执行Redis监控任务: {}", task.getTaskName());
        
        Jedis jedis = null;
        try {
            RedisDatasource datasource = redisDatasourceService.getByGroupId(task.getGroupId());
            if (datasource == null) {
                log.error("Redis数据源不存在: {}", task.getGroupId());
                return;
            }
            
            jedis = new Jedis(datasource.getRedisHost(), datasource.getRedisPort());
            if (datasource.getRedisPassword() != null && !datasource.getRedisPassword().isEmpty()) {
                jedis.auth(datasource.getRedisPassword());
            }
            jedis.select(datasource.getRedisDatabase());
            
            Map<String, Object> reportData = new HashMap<>();
            reportData.put("taskName", task.getTaskName());
            reportData.put("groupId", task.getGroupId());
            reportData.put("redisHost", datasource.getRedisHost() + ":" + datasource.getRedisPort());
            reportData.put("slowQueryThreshold", task.getSlowQueryThreshold());
            reportData.put("bigKeyMemoryThreshold", task.getBigKeyMemoryThreshold());
            reportData.put("bigKeyCountThreshold", task.getBigKeyCountThreshold());
            
            List<Map<String, Object>> slowQueries = new ArrayList<>();
            List<Map<String, Object>> bigKeys = new ArrayList<>();
            
            String taskType = task.getTaskType();
            if ("slow_query".equals(taskType) || "all".equals(taskType)) {
                slowQueries = monitorSlowQueries(jedis, task.getSlowQueryThreshold());
            }
            
            if ("big_key".equals(taskType) || "all".equals(taskType)) {
                bigKeys = detectBigKeys(jedis, task.getBigKeyMemoryThreshold(), task.getBigKeyCountThreshold());
            }
            
            reportData.put("slowQueries", slowQueries);
            reportData.put("bigKeys", bigKeys);
            reportData.put("generateTime", LocalDateTime.now().toString());
            
            feishuReportService.generateAndSendReport(task.getNotifyChatId(), reportData);
            
            task.setLastRunTime(LocalDateTime.now());
            scheduledTaskService.updateById(task);
            
            log.info("Redis监控任务执行完成: {}, 慢查询: {}条, 大key: {}个", 
                    task.getTaskName(), slowQueries.size(), bigKeys.size());
            
        } catch (Exception e) {
            log.error("Redis监控任务执行失败: {}", task.getTaskName(), e);
        } finally {
            if (jedis != null) {
                jedis.close();
            }
        }
    }
    
    private List<Map<String, Object>> monitorSlowQueries(Jedis jedis, Long thresholdMicros) {
        List<Map<String, Object>> slowQueries = new ArrayList<>();
        
        try {
            List<redis.clients.jedis.resps.Slowlog> slowLogs = jedis.slowlogGet();
            
            for (redis.clients.jedis.resps.Slowlog entry : slowLogs) {
                long execTime = entry.getExecutionTime();
                if (execTime >= thresholdMicros / 1000) {
                    Map<String, Object> query = new HashMap<>();
                    query.put("id", entry.getId());
                    query.put("executionTime", execTime + "ms");
                    query.put("command", String.join(" ", entry.getArgs()));
                    query.put("timestamp", entry.getTimeStamp());
                    slowQueries.add(query);
                }
            }
        } catch (Exception e) {
            log.error("获取慢查询日志失败", e);
        }
        
        return slowQueries;
    }
    
    private List<Map<String, Object>> detectBigKeys(Jedis jedis, Long memoryThreshold, Long countThreshold) {
        List<Map<String, Object>> bigKeys = new ArrayList<>();
        
        try {
            Set<String> keys = jedis.keys("*");
            if (keys == null || keys.isEmpty()) {
                return bigKeys;
            }
            
            for (String key : keys) {
                try {
                    String type = jedis.type(key).toString();
                    long elementCount = getElementCount(jedis, key, type);
                    
                    long memoryUsage = 0;
                    if (type.equals("string")) {
                        String val = jedis.get(key);
                        memoryUsage = val != null ? val.length() : 0;
                    } else if (type.equals("hash")) {
                        Map<String, String> fields = jedis.hgetAll(key);
                        memoryUsage = fields != null ? fields.toString().length() : 0;
                    } else {
                        memoryUsage = elementCount;
                    }
                    
                    boolean isBig = (memoryUsage >= memoryThreshold) ||
                                   (elementCount >= countThreshold);
                    
                    if (isBig) {
                        Map<String, Object> keyInfo = new HashMap<>();
                        keyInfo.put("key", key);
                        keyInfo.put("type", type);
                        keyInfo.put("memory", formatSize(memoryUsage));
                        keyInfo.put("elements", elementCount);
                        bigKeys.add(keyInfo);
                    }
                } catch (Exception e) {
                    log.warn("检测key失败: {}", key, e);
                }
            }
            
            bigKeys.sort((a, b) -> {
                Long memA = getMemoryValue((String) a.get("memory"));
                Long memB = getMemoryValue((String) b.get("memory"));
                return memB.compareTo(memA);
            });
            
        } catch (Exception e) {
            log.error("检测大key失败", e);
        }
        
        return bigKeys;
    }
    
    private long getElementCount(Jedis jedis, String key, String type) {
        switch (type) {
            case "string":
                String value = jedis.get(key);
                return value != null ? value.length() : 0;
            case "list":
                return jedis.llen(key);
            case "set":
                return jedis.scard(key);
            case "zset":
                return jedis.zcard(key);
            case "hash":
                return jedis.hlen(key);
            default:
                return 0;
        }
    }
    
    private Long getMemoryValue(String size) {
        if (size == null) return 0L;
        try {
            if (size.endsWith("GB")) {
                return (long) (Double.parseDouble(size.replace(" GB", "")) * 1024 * 1024 * 1024);
            } else if (size.endsWith("MB")) {
                return (long) (Double.parseDouble(size.replace(" MB", "")) * 1024 * 1024);
            } else if (size.endsWith("KB")) {
                return (long) (Double.parseDouble(size.replace(" KB", "")) * 1024);
            } else if (size.endsWith(" B")) {
                return Long.parseLong(size.replace(" B", ""));
            }
        } catch (Exception e) {
        }
        return 0L;
    }
    
    private String formatSize(long bytes) {
        if (bytes < 1024) return bytes + " B";
        if (bytes < 1024 * 1024) return String.format("%.2f KB", bytes / 1024.0);
        if (bytes < 1024 * 1024 * 1024) return String.format("%.2f MB", bytes / (1024.0 * 1024));
        return String.format("%.2f GB", bytes / (1024.0 * 1024 * 1024));
    }
}
