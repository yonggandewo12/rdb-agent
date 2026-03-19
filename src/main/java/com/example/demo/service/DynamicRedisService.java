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

    private StringRedisTemplate getRedisTemplate(String groupId) {
        return redisSourceManager.getRedisTemplate(groupId);
    }

    public String get(String groupId, String key) {
        return getRedisTemplate(groupId).opsForValue().get(key);
    }

    public void set(String groupId, String key, String value) {
        getRedisTemplate(groupId).opsForValue().set(key, value);
    }

    public void set(String groupId, String key, String value, long expireSeconds) {
        getRedisTemplate(groupId).opsForValue().set(key, value, expireSeconds, TimeUnit.SECONDS);
    }

    public Boolean delete(String groupId, String key) {
        return getRedisTemplate(groupId).delete(key);
    }

    public Boolean exists(String groupId, String key) {
        return getRedisTemplate(groupId).hasKey(key);
    }

    public Long increment(String groupId, String key, long delta) {
        return getRedisTemplate(groupId).opsForValue().increment(key, delta);
    }

    public Object hget(String groupId, String key, String hashKey) {
        return getRedisTemplate(groupId).opsForHash().get(key, hashKey);
    }

    public void hset(String groupId, String key, String hashKey, Object value) {
        getRedisTemplate(groupId).opsForHash().put(key, hashKey, value);
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

    public Long hdelete(String groupId, String key, Object... hashKeys) {
        return getRedisTemplate(groupId).opsForHash().delete(key, hashKeys);
    }

    public List<String> lrange(String groupId, String key, long start, long end) {
        return getRedisTemplate(groupId).opsForList().range(key, start, end);
    }

    public Long lpush(String groupId, String key, String value) {
        return getRedisTemplate(groupId).opsForList().leftPush(key, value);
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

    public Set<String> smembers(String groupId, String key) {
        return getRedisTemplate(groupId).opsForSet().members(key);
    }

    public Long sadd(String groupId, String key, String... values) {
        return getRedisTemplate(groupId).opsForSet().add(key, values);
    }

    public Long srem(String groupId, String key, String... values) {
        return getRedisTemplate(groupId).opsForSet().remove(key, values);
    }

    public Boolean sismember(String groupId, String key, String value) {
        return getRedisTemplate(groupId).opsForSet().isMember(key, value);
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
