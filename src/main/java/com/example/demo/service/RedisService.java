package com.example.demo.service;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Redis操作服务接口
 *
 * @author system
 * @date 2026-03-12
 */
public interface RedisService {
    
    /**
     * 获取String类型值
     *
     * @param key 键
     * @return 值
     */
    String get(String key);
    
    /**
     * 设置String类型值
     *
     * @param key 键
     * @param value 值
     */
    void set(String key, String value);
    
    /**
     * 设置String类型值并设置过期时间
     *
     * @param key 键
     * @param value 值
     * @param expireSeconds 过期时间（秒）
     */
    void set(String key, String value, long expireSeconds);
    
    /**
     * 删除键
     *
     * @param key 键
     * @return 是否删除成功
     */
    Boolean delete(String key);
    
    /**
     * 判断键是否存在
     *
     * @param key 键
     * @return 是否存在
     */
    Boolean exists(String key);
    
    /**
     * 递增计数
     *
     * @param key 键
     * @param delta 增量
     * @return 递增后的值
     */
    Long increment(String key, long delta);
    
    /**
     * 获取哈希结构字段值
     *
     * @param key 键
     * @param hashKey 哈希字段
     * @return 字段值
     */
    Object hget(String key, String hashKey);
    
    /**
     * 设置哈希结构字段值
     *
     * @param key 键
     * @param hashKey 哈希字段
     * @param value 字段值
     */
    void hset(String key, String hashKey, Object value);
    
    /**
     * 获取哈希结构所有字段和值
     *
     * @param key 键
     * @return 字段值映射
     */
    Map<Object, Object> hgetAll(String key);
    
    /**
     * 批量获取哈希结构字段值
     *
     * @param key 键
     * @param hashKeys 哈希字段列表
     * @return 字段值列表
     */
    List<Object> hmget(String key, List<String> hashKeys);
    
    /**
     * 批量设置哈希结构字段值
     *
     * @param key 键
     * @param map 字段值映射
     */
    void hmset(String key, Map<String, Object> map);
    
    /**
     * 删除哈希结构字段
     *
     * @param key 键
     * @param hashKeys 要删除的字段
     * @return 删除的字段数量
     */
    Long hdelete(String key, Object... hashKeys);
    
    /**
     * 获取列表指定范围的元素
     *
     * @param key 键
     * @param start 起始索引
     * @param end 结束索引
     * @return 元素列表
     */
    List<String> lrange(String key, long start, long end);
    
    /**
     * 从列表左侧插入元素
     *
     * @param key 键
     * @param value 元素值
     * @return 列表长度
     */
    Long lpush(String key, String value);
    
    /**
     * 从列表右侧插入元素
     *
     * @param key 键
     * @param value 元素值
     * @return 列表长度
     */
    Long rpush(String key, String value);
    
    /**
     * 从列表左侧弹出元素
     *
     * @param key 键
     * @return 弹出的元素
     */
    String lpop(String key);
    
    /**
     * 从列表右侧弹出元素
     *
     * @param key 键
     * @return 弹出的元素
     */
    String rpop(String key);
    
    /**
     * 获取列表长度
     *
     * @param key 键
     * @return 列表长度
     */
    Long llen(String key);
    
    /**
     * 获取集合所有成员
     *
     * @param key 键
     * @return 成员集合
     */
    Set<String> smembers(String key);

    /**
     * 按模式查询匹配的键
     *
     * @param pattern 键模式
     * @return 键集合
     */
    Set<String> keys(String pattern);

    /**
     * 向集合添加元素
     *
     * @param key 键
     * @param values 元素值
     * @return 添加的元素数量
     */
    Long sadd(String key, String... values);
    
    /**
     * 从集合移除元素
     *
     * @param key 键
     * @param values 元素值
     * @return 移除的元素数量
     */
    Long srem(String key, String... values);
    
    /**
     * 判断元素是否在集合中
     *
     * @param key 键
     * @param value 元素值
     * @return 是否存在
     */
    Boolean sismember(String key, String value);
    
    /**
     * 获取键的剩余过期时间
     *
     * @param key 键
     * @return 剩余过期时间（秒），-1表示永久有效，-2表示键不存在
     */
    Long ttl(String key);
    
    /**
     * 设置键的过期时间
     *
     * @param key 键
     * @param seconds 过期时间（秒）
     * @return 是否设置成功
     */
    Boolean expire(String key, long seconds);
}
