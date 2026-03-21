package com.example.demo.service;

import com.example.demo.config.DynamicRedisSourceManager;
import com.example.demo.entity.RedisDatasource;
import org.springframework.data.redis.connection.DataType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

@Service
public class DynamicRedisService {

    @Autowired
    private DynamicRedisSourceManager redisSourceManager;

    @Autowired
    private RedisDatasourceService redisDatasourceService;

    private StringRedisTemplate getRedisTemplate(RedisDatasource datasource) {
        if (datasource == null) {
            throw new IllegalArgumentException("Redis数据源不能为空");
        }
        return redisSourceManager.getRedisTemplate(datasource.getGroupId());
    }

    private StringRedisTemplate getRedisTemplate(String groupId) {
        return redisSourceManager.getRedisTemplate(groupId);
    }

    public String get(RedisDatasource datasource, String key) {
        StringRedisTemplate redisTemplate = getRedisTemplate(datasource);
        validateStringOperation(redisTemplate, key, null, "查询字符串值");
        return redisTemplate.opsForValue().get(key);
    }

    public String get(String groupId, String key) {
        StringRedisTemplate redisTemplate = getRedisTemplate(groupId);
        validateStringOperation(redisTemplate, key, null, "查询字符串值");
        return redisTemplate.opsForValue().get(key);
    }

    public void set(RedisDatasource datasource, String key, String value) {
        StringRedisTemplate redisTemplate = getRedisTemplate(datasource);
        validateStringOperation(redisTemplate, key, value, "设置字符串值");
        redisTemplate.opsForValue().set(key, value);
    }

    public void set(String groupId, String key, String value) {
        StringRedisTemplate redisTemplate = getRedisTemplate(groupId);
        validateStringOperation(redisTemplate, key, value, "设置字符串值");
        redisTemplate.opsForValue().set(key, value);
    }

    public void set(RedisDatasource datasource, String key, String value, long expireSeconds) {
        StringRedisTemplate redisTemplate = getRedisTemplate(datasource);
        validateStringOperation(redisTemplate, key, value, "设置字符串值");
        redisTemplate.opsForValue().set(key, value, expireSeconds, TimeUnit.SECONDS);
    }

    public void set(String groupId, String key, String value, long expireSeconds) {
        StringRedisTemplate redisTemplate = getRedisTemplate(groupId);
        validateStringOperation(redisTemplate, key, value, "设置字符串值");
        redisTemplate.opsForValue().set(key, value, expireSeconds, TimeUnit.SECONDS);
    }

    public Boolean delete(RedisDatasource datasource, String key) {
        validateKey(key);
        return getRedisTemplate(datasource).delete(key);
    }

    public Boolean delete(String groupId, String key) {
        validateKey(key);
        return getRedisTemplate(groupId).delete(key);
    }

    public Boolean exists(RedisDatasource datasource, String key) {
        validateKey(key);
        return getRedisTemplate(datasource).hasKey(key);
    }

    public Boolean exists(String groupId, String key) {
        validateKey(key);
        return getRedisTemplate(groupId).hasKey(key);
    }

    public Long increment(String groupId, String key, long delta) {
        return getRedisTemplate(groupId).opsForValue().increment(key, delta);
    }

    public Object hget(RedisDatasource datasource, String key, String hashKey) {
        StringRedisTemplate redisTemplate = getRedisTemplate(datasource);
        validateHashOperation(redisTemplate, key, null, "读取哈希字段");
        return redisTemplate.opsForHash().get(key, hashKey);
    }

    public Object hget(String groupId, String key, String hashKey) {
        StringRedisTemplate redisTemplate = getRedisTemplate(groupId);
        validateHashOperation(redisTemplate, key, null, "读取哈希字段");
        return redisTemplate.opsForHash().get(key, hashKey);
    }

    public void hset(RedisDatasource datasource, String key, String hashKey, Object value) {
        StringRedisTemplate redisTemplate = getRedisTemplate(datasource);
        validateHashOperation(redisTemplate, key, new Object[]{hashKey}, "写入哈希字段");
        redisTemplate.opsForHash().put(key, hashKey, value);
    }

    public void hset(String groupId, String key, String hashKey, Object value) {
        StringRedisTemplate redisTemplate = getRedisTemplate(groupId);
        validateHashOperation(redisTemplate, key, new Object[]{hashKey}, "写入哈希字段");
        redisTemplate.opsForHash().put(key, hashKey, value);
    }

    public Map<Object, Object> hgetAll(RedisDatasource datasource, String key) {
        StringRedisTemplate redisTemplate = getRedisTemplate(datasource);
        validateHashOperation(redisTemplate, key, null, "读取哈希全部字段");
        return redisTemplate.opsForHash().entries(key);
    }

    public Map<Object, Object> hgetAll(String groupId, String key) {
        StringRedisTemplate redisTemplate = getRedisTemplate(groupId);
        validateHashOperation(redisTemplate, key, null, "读取哈希全部字段");
        return redisTemplate.opsForHash().entries(key);
    }

    public List<Object> hmget(String groupId, String key, List<String> hashKeys) {
        return getRedisTemplate(groupId).opsForHash().multiGet(key, (List<Object>) (List<?>) hashKeys);
    }

    public void hmset(String groupId, String key, Map<String, Object> map) {
        getRedisTemplate(groupId).opsForHash().putAll(key, map);
    }

    public Long hdelete(RedisDatasource datasource, String key, Object... hashKeys) {
        StringRedisTemplate redisTemplate = getRedisTemplate(datasource);
        validateHashOperation(redisTemplate, key, hashKeys, "删除哈希字段");
        return redisTemplate.opsForHash().delete(key, hashKeys);
    }

    public Long hdelete(String groupId, String key, Object... hashKeys) {
        StringRedisTemplate redisTemplate = getRedisTemplate(groupId);
        validateHashOperation(redisTemplate, key, hashKeys, "删除哈希字段");
        return redisTemplate.opsForHash().delete(key, hashKeys);
    }

    public List<String> lrange(RedisDatasource datasource, String key, long start, long end) {
        StringRedisTemplate redisTemplate = getRedisTemplate(datasource);
        validateListOperation(redisTemplate, key, null, "查询列表元素");
        return redisTemplate.opsForList().range(key, start, end);
    }

    public List<String> lrange(String groupId, String key, long start, long end) {
        StringRedisTemplate redisTemplate = getRedisTemplate(groupId);
        validateListOperation(redisTemplate, key, null, "查询列表元素");
        return redisTemplate.opsForList().range(key, start, end);
    }

    public Long lpush(RedisDatasource datasource, String key, String value) {
        StringRedisTemplate redisTemplate = getRedisTemplate(datasource);
        validateListOperation(redisTemplate, key, new String[]{value}, "向列表左侧插入元素");
        return redisTemplate.opsForList().leftPush(key, value);
    }

    public Long lpush(String groupId, String key, String value) {
        StringRedisTemplate redisTemplate = getRedisTemplate(groupId);
        validateListOperation(redisTemplate, key, new String[]{value}, "向列表左侧插入元素");
        return redisTemplate.opsForList().leftPush(key, value);
    }

    public Long rpush(RedisDatasource datasource, String key, String value) {
        StringRedisTemplate redisTemplate = getRedisTemplate(datasource);
        validateListOperation(redisTemplate, key, new String[]{value}, "向列表右侧插入元素");
        return redisTemplate.opsForList().rightPush(key, value);
    }

    public Long rpush(String groupId, String key, String value) {
        StringRedisTemplate redisTemplate = getRedisTemplate(groupId);
        validateListOperation(redisTemplate, key, new String[]{value}, "向列表右侧插入元素");
        return redisTemplate.opsForList().rightPush(key, value);
    }

    public String lpop(String groupId, String key) {
        return getRedisTemplate(groupId).opsForList().leftPop(key);
    }

    public String rpop(String groupId, String key) {
        return getRedisTemplate(groupId).opsForList().rightPop(key);
    }

    public Long llen(String groupId, String key) {
        StringRedisTemplate redisTemplate = getRedisTemplate(groupId);
        validateListOperation(redisTemplate, key, null, "查询列表长度");
        return redisTemplate.opsForList().size(key);
    }

    public Long llen(RedisDatasource datasource, String key) {
        StringRedisTemplate redisTemplate = getRedisTemplate(datasource);
        validateListOperation(redisTemplate, key, null, "查询列表长度");
        return redisTemplate.opsForList().size(key);
    }

    public Set<String> smembers(RedisDatasource datasource, String key) {
        StringRedisTemplate redisTemplate = getRedisTemplate(datasource);
        validateSetOperation(redisTemplate, key, null, "查询集合成员");
        return redisTemplate.opsForSet().members(key);
    }

    public Set<String> smembers(String groupId, String key) {
        StringRedisTemplate redisTemplate = getRedisTemplate(groupId);
        validateSetOperation(redisTemplate, key, null, "查询集合成员");
        return redisTemplate.opsForSet().members(key);
    }

    public Long sadd(RedisDatasource datasource, String key, String... values) {
        StringRedisTemplate redisTemplate = getRedisTemplate(datasource);
        validateSetOperation(redisTemplate, key, values, "添加集合元素");
        return redisTemplate.opsForSet().add(key, values);
    }

    public Long sadd(String groupId, String key, String... values) {
        StringRedisTemplate redisTemplate = getRedisTemplate(groupId);
        validateSetOperation(redisTemplate, key, values, "添加集合元素");
        return redisTemplate.opsForSet().add(key, values);
    }

    public Long srem(RedisDatasource datasource, String key, String... values) {
        StringRedisTemplate redisTemplate = getRedisTemplate(datasource);
        validateSetOperation(redisTemplate, key, values, "移除集合元素");
        return redisTemplate.opsForSet().remove(key, values);
    }

    public Long srem(String groupId, String key, String... values) {
        StringRedisTemplate redisTemplate = getRedisTemplate(groupId);
        validateSetOperation(redisTemplate, key, values, "移除集合元素");
        return redisTemplate.opsForSet().remove(key, values);
    }

    public Boolean sismember(String groupId, String key, String value) {
        StringRedisTemplate redisTemplate = getRedisTemplate(groupId);
        validateSetOperation(redisTemplate, key, new String[]{value}, "检查集合成员");
        return redisTemplate.opsForSet().isMember(key, value);
    }

    public Long ttl(RedisDatasource datasource, String key) {
        validateKey(key);
        return getRedisTemplate(datasource).getExpire(key, TimeUnit.SECONDS);
    }

    public Long ttl(String groupId, String key) {
        validateKey(key);
        return getRedisTemplate(groupId).getExpire(key, TimeUnit.SECONDS);
    }

    public Boolean expire(String groupId, String key, long seconds) {
        return getRedisTemplate(groupId).expire(key, seconds, TimeUnit.SECONDS);
    }

    public RedisDatasource getDatasourceByChatId(String chatId) {
        return redisDatasourceService.getByChatId(chatId);
    }

    private void validateSetOperation(StringRedisTemplate redisTemplate, String key,
                                      String[] values, String operationName) {
        validateKey(key);
        validateStringValues(values, "集合操作");
        validateDataType(redisTemplate, key, DataType.SET, operationName);
    }

    private void validateStringOperation(StringRedisTemplate redisTemplate, String key,
                                         String value, String operationName) {
        validateKey(key);
        if (value != null && !StringUtils.hasText(value)) {
            throw new IllegalArgumentException("字符串操作缺少有效值");
        }
        if (!"设置字符串值".equals(operationName)) {
            validateDataType(redisTemplate, key, DataType.STRING, operationName);
        }
    }

    private void validateListOperation(StringRedisTemplate redisTemplate, String key,
                                       String[] values, String operationName) {
        validateKey(key);
        validateStringValues(values, "列表操作");
        validateDataType(redisTemplate, key, DataType.LIST, operationName);
    }

    private void validateHashOperation(StringRedisTemplate redisTemplate, String key,
                                       Object[] hashKeys, String operationName) {
        validateKey(key);
        if (hashKeys != null && hashKeys.length == 0) {
            throw new IllegalArgumentException("哈希操作缺少字段信息");
        }
        if (hashKeys != null) {
            for (Object hashKey : hashKeys) {
                if (hashKey == null || !StringUtils.hasText(String.valueOf(hashKey))) {
                    throw new IllegalArgumentException("哈希操作包含空字段，无法执行");
                }
            }
        }
        validateDataType(redisTemplate, key, DataType.HASH, operationName);
    }

    private void validateKey(String key) {
        if (!StringUtils.hasText(key)) {
            throw new IllegalArgumentException("Redis key不能为空");
        }
    }

    private void validateStringValues(String[] values, String operationType) {
        if (values != null && values.length == 0) {
            throw new IllegalArgumentException(operationType + "缺少元素值");
        }
        if (values != null) {
            for (String value : values) {
                if (!StringUtils.hasText(value)) {
                    throw new IllegalArgumentException(operationType + "包含空元素，无法执行");
                }
            }
        }
    }

    private void validateDataType(StringRedisTemplate redisTemplate, String key,
                                  DataType expectedType, String operationName) {
        DataType dataType = redisTemplate.type(key);
        if (dataType != null && dataType != DataType.NONE && dataType != expectedType) {
            throw new IllegalArgumentException(String.format("key %s 当前类型为 %s，不能执行%s", key,
                    dataType.code(), operationName));
        }
    }
}
