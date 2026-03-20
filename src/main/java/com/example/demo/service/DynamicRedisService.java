package com.example.demo.service;

import com.example.demo.config.DynamicRedisSourceManager;
import com.example.demo.entity.RedisDatasource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

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
        return getRedisTemplate(datasource).opsForValue().get(key);
    }

    public String get(String groupId, String key) {
        return getRedisTemplate(groupId).opsForValue().get(key);
    }

    public void set(RedisDatasource datasource, String key, String value) {
        getRedisTemplate(datasource).opsForValue().set(key, value);
    }

    public void set(String groupId, String key, String value) {
        getRedisTemplate(groupId).opsForValue().set(key, value);
    }

    public void set(RedisDatasource datasource, String key, String value, long expireSeconds) {
        getRedisTemplate(datasource).opsForValue().set(key, value, expireSeconds, TimeUnit.SECONDS);
    }

    public void set(String groupId, String key, String value, long expireSeconds) {
        getRedisTemplate(groupId).opsForValue().set(key, value, expireSeconds, TimeUnit.SECONDS);
    }

    public Boolean delete(RedisDatasource datasource, String key) {
        return getRedisTemplate(datasource).delete(key);
    }

    public Boolean delete(String groupId, String key) {
        return getRedisTemplate(groupId).delete(key);
    }

    public Boolean exists(RedisDatasource datasource, String key) {
        return getRedisTemplate(datasource).hasKey(key);
    }

    public Boolean exists(String groupId, String key) {
        return getRedisTemplate(groupId).hasKey(key);
    }

    public Long increment(String groupId, String key, long delta) {
        return getRedisTemplate(groupId).opsForValue().increment(key, delta);
    }

    public Object hget(RedisDatasource datasource, String key, String hashKey) {
        return getRedisTemplate(datasource).opsForHash().get(key, hashKey);
    }

    public Object hget(String groupId, String key, String hashKey) {
        return getRedisTemplate(groupId).opsForHash().get(key, hashKey);
    }

    public void hset(RedisDatasource datasource, String key, String hashKey, Object value) {
        getRedisTemplate(datasource).opsForHash().put(key, hashKey, value);
    }

    public void hset(String groupId, String key, String hashKey, Object value) {
        getRedisTemplate(groupId).opsForHash().put(key, hashKey, value);
    }

    public Map<Object, Object> hgetAll(RedisDatasource datasource, String key) {
        return getRedisTemplate(datasource).opsForHash().entries(key);
    }

    public Map<Object, Object> hgetAll(String groupId, String key) {
        return getRedisTemplate(groupId).opsForHash().entries(key);
    }

    public List<Object> hmget(String groupId, String key, List<String> hashKeys) {
        return getRedisTemplate(groupId).opsForHash().multiGet(key, (List<Object>) (List<?>) hashKeys);
    }

    public void hmset(String groupId, String key, Map<String, Object> map) {
        getRedisTemplate(groupId).opsForHash().putAll(key, map);
    }

    public Long hdelete(RedisDatasource datasource, String key, Object... hashKeys) {
        return getRedisTemplate(datasource).opsForHash().delete(key, hashKeys);
    }

    public Long hdelete(String groupId, String key, Object... hashKeys) {
        return getRedisTemplate(groupId).opsForHash().delete(key, hashKeys);
    }

    public List<String> lrange(RedisDatasource datasource, String key, long start, long end) {
        return getRedisTemplate(datasource).opsForList().range(key, start, end);
    }

    public List<String> lrange(String groupId, String key, long start, long end) {
        return getRedisTemplate(groupId).opsForList().range(key, start, end);
    }

    public Long lpush(RedisDatasource datasource, String key, String value) {
        return getRedisTemplate(datasource).opsForList().leftPush(key, value);
    }

    public Long lpush(String groupId, String key, String value) {
        return getRedisTemplate(groupId).opsForList().leftPush(key, value);
    }

    public Long rpush(RedisDatasource datasource, String key, String value) {
        return getRedisTemplate(datasource).opsForList().rightPush(key, value);
    }

    public Long rpush(String groupId, String key, String value) {
        return getRedisTemplate(groupId).opsForList().rightPush(key, value);
    }

    public String lpop(String groupId, String key) {
        return getRedisTemplate(groupId).opsForList().leftPop(key);
    }

    public String rpop(String groupId, String key) {
        return getRedisTemplate(groupId).opsForList().rightPop(key);
    }

    public Long llen(String groupId, String key) {
        return getRedisTemplate(groupId).opsForList().size(key);
    }

    public Long llen(RedisDatasource datasource, String key) {
        return getRedisTemplate(datasource).opsForList().size(key);
    }

    public Set<String> smembers(RedisDatasource datasource, String key) {
        return getRedisTemplate(datasource).opsForSet().members(key);
    }

    public Set<String> smembers(String groupId, String key) {
        return getRedisTemplate(groupId).opsForSet().members(key);
    }

    public Long sadd(RedisDatasource datasource, String key, String... values) {
        return getRedisTemplate(datasource).opsForSet().add(key, values);
    }

    public Long sadd(String groupId, String key, String... values) {
        return getRedisTemplate(groupId).opsForSet().add(key, values);
    }

    public Long srem(RedisDatasource datasource, String key, String... values) {
        return getRedisTemplate(datasource).opsForSet().remove(key, values);
    }

    public Long srem(String groupId, String key, String... values) {
        return getRedisTemplate(groupId).opsForSet().remove(key, values);
    }

    public Boolean sismember(String groupId, String key, String value) {
        return getRedisTemplate(groupId).opsForSet().isMember(key, value);
    }

    public Long ttl(RedisDatasource datasource, String key) {
        return getRedisTemplate(datasource).getExpire(key, TimeUnit.SECONDS);
    }

    public Long ttl(String groupId, String key) {
        return getRedisTemplate(groupId).getExpire(key, TimeUnit.SECONDS);
    }

    public Boolean expire(String groupId, String key, long seconds) {
        return getRedisTemplate(groupId).expire(key, seconds, TimeUnit.SECONDS);
    }

    public RedisDatasource getDatasourceByChatId(String chatId) {
        return redisDatasourceService.getByChatId(chatId);
    }
}
