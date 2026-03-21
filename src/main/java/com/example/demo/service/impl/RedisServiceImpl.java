package com.example.demo.service.impl;

import com.example.demo.service.RedisService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * Redis操作服务实现类
 *
 * @author system
 * @date 2026-03-12
 */
@Service
public class RedisServiceImpl implements RedisService {
    
    private final StringRedisTemplate redisTemplate;
    
    @Autowired
    public RedisServiceImpl(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }
    
    @Override
    public String get(String key) {
        if (!StringUtils.hasText(key)) {
            throw new IllegalArgumentException("Redis key cannot be empty");
        }
        return redisTemplate.opsForValue().get(key);
    }
    
    @Override
    public void set(String key, String value) {
        if (!StringUtils.hasText(key) || value == null) {
            throw new IllegalArgumentException("Redis key or value cannot be empty");
        }
        redisTemplate.opsForValue().set(key, value);
    }
    
    @Override
    public void set(String key, String value, long expireSeconds) {
        if (!StringUtils.hasText(key) || value == null || expireSeconds <= 0) {
            throw new IllegalArgumentException("Invalid parameter for set operation");
        }
        redisTemplate.opsForValue().set(key, value, expireSeconds, TimeUnit.SECONDS);
    }
    
    @Override
    public Boolean delete(String key) {
        if (!StringUtils.hasText(key)) {
            throw new IllegalArgumentException("Redis key cannot be empty");
        }
        return redisTemplate.delete(key);
    }
    
    @Override
    public Boolean exists(String key) {
        if (!StringUtils.hasText(key)) {
            throw new IllegalArgumentException("Redis key cannot be empty");
        }
        return redisTemplate.hasKey(key);
    }
    
    @Override
    public Long increment(String key, long delta) {
        if (!StringUtils.hasText(key)) {
            throw new IllegalArgumentException("Redis key cannot be empty");
        }
        return redisTemplate.opsForValue().increment(key, delta);
    }
    
    @Override
    public Object hget(String key, String hashKey) {
        if (!StringUtils.hasText(key) || !StringUtils.hasText(hashKey)) {
            throw new IllegalArgumentException("Redis key or hashKey cannot be empty");
        }
        return redisTemplate.opsForHash().get(key, hashKey);
    }
    
    @Override
    public void hset(String key, String hashKey, Object value) {
        if (!StringUtils.hasText(key) || !StringUtils.hasText(hashKey) || value == null) {
            throw new IllegalArgumentException("Invalid parameter for hset operation");
        }
        redisTemplate.opsForHash().put(key, hashKey, value);
    }
    
    @Override
    public Map<Object, Object> hgetAll(String key) {
        if (!StringUtils.hasText(key)) {
            throw new IllegalArgumentException("Redis key cannot be empty");
        }
        return redisTemplate.opsForHash().entries(key);
    }
    
    @Override
    @SuppressWarnings("unchecked")
    public List<Object> hmget(String key, List<String> hashKeys) {
        if (!StringUtils.hasText(key) || hashKeys == null || hashKeys.isEmpty()) {
            throw new IllegalArgumentException("Invalid parameter for hmget operation");
        }
        return redisTemplate.opsForHash().multiGet(key, (List<Object>) (List<?>) hashKeys);
    }
    
    @Override
    public void hmset(String key, Map<String, Object> map) {
        if (!StringUtils.hasText(key) || map == null || map.isEmpty()) {
            throw new IllegalArgumentException("Invalid parameter for hmset operation");
        }
        redisTemplate.opsForHash().putAll(key, map);
    }
    
    @Override
    public Long hdelete(String key, Object... hashKeys) {
        if (!StringUtils.hasText(key) || hashKeys == null || hashKeys.length == 0) {
            throw new IllegalArgumentException("Invalid parameter for hdelete operation");
        }
        return redisTemplate.opsForHash().delete(key, hashKeys);
    }
    
    @Override
    public List<String> lrange(String key, long start, long end) {
        if (!StringUtils.hasText(key)) {
            throw new IllegalArgumentException("Redis key cannot be empty");
        }
        return redisTemplate.opsForList().range(key, start, end);
    }
    
    @Override
    public Long lpush(String key, String value) {
        if (!StringUtils.hasText(key) || !StringUtils.hasText(value)) {
            throw new IllegalArgumentException("Redis key or value cannot be empty");
        }
        return redisTemplate.opsForList().leftPush(key, value);
    }
    
    @Override
    public Long rpush(String key, String value) {
        if (!StringUtils.hasText(key) || !StringUtils.hasText(value)) {
            throw new IllegalArgumentException("Redis key or value cannot be empty");
        }
        return redisTemplate.opsForList().rightPush(key, value);
    }
    
    @Override
    public String lpop(String key) {
        if (!StringUtils.hasText(key)) {
            throw new IllegalArgumentException("Redis key cannot be empty");
        }
        return redisTemplate.opsForList().leftPop(key);
    }
    
    @Override
    public String rpop(String key) {
        if (!StringUtils.hasText(key)) {
            throw new IllegalArgumentException("Redis key cannot be empty");
        }
        return redisTemplate.opsForList().rightPop(key);
    }
    
    @Override
    public Long llen(String key) {
        if (!StringUtils.hasText(key)) {
            throw new IllegalArgumentException("Redis key cannot be empty");
        }
        return redisTemplate.opsForList().size(key);
    }
    
    @Override
    public Set<String> smembers(String key) {
        if (!StringUtils.hasText(key)) {
            throw new IllegalArgumentException("Redis key cannot be empty");
        }
        return redisTemplate.opsForSet().members(key);
    }

    @Override
    public Set<String> keys(String pattern) {
        if (!StringUtils.hasText(pattern)) {
            throw new IllegalArgumentException("Redis pattern cannot be empty");
        }
        return redisTemplate.keys(pattern);
    }

    @Override
    public Long sadd(String key, String... values) {
        if (!StringUtils.hasText(key) || values == null || values.length == 0) {
            throw new IllegalArgumentException("Invalid parameter for sadd operation");
        }
        return redisTemplate.opsForSet().add(key, values);
    }
    
    @Override
    public Long srem(String key, String... values) {
        if (!StringUtils.hasText(key) || values == null || values.length == 0) {
            throw new IllegalArgumentException("Invalid parameter for srem operation");
        }
        return redisTemplate.opsForSet().remove(key, (Object[]) values);
    }
    
    @Override
    public Boolean sismember(String key, String value) {
        if (!StringUtils.hasText(key) || !StringUtils.hasText(value)) {
            throw new IllegalArgumentException("Redis key or value cannot be empty");
        }
        return redisTemplate.opsForSet().isMember(key, value);
    }
    
    @Override
    public Long ttl(String key) {
        if (!StringUtils.hasText(key)) {
            throw new IllegalArgumentException("Redis key cannot be empty");
        }
        return redisTemplate.getExpire(key, TimeUnit.SECONDS);
    }
    
    @Override
    public Boolean expire(String key, long seconds) {
        if (!StringUtils.hasText(key) || seconds <= 0) {
            throw new IllegalArgumentException("Invalid parameter for expire operation");
        }
        return redisTemplate.expire(key, seconds, TimeUnit.SECONDS);
    }
}
