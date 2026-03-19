package com.example.demo.config;

import com.example.demo.entity.RedisDatasource;
import com.example.demo.service.RedisDatasourceService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 动态Redis数据源管理器
 * 根据group_id动态获取对应的RedisTemplate
 */
@Component
public class DynamicRedisSourceManager {

    private final Map<String, StringRedisTemplate> redisTemplateMap = new ConcurrentHashMap<>();

    @Autowired
    private RedisDatasourceService redisDatasourceService;

    /**
     * 初始化加载所有Redis数据源
     */
    @PostConstruct
    public void init() {
        // 加载所有启用的Redis数据源
        redisDatasourceService.listAllEnabled().forEach(this::addRedisSource);
    }

    /**
     * 添加Redis数据源
     */
    public void addRedisSource(RedisDatasource datasource) {
        // 创建Redis配置
        RedisStandaloneConfiguration config = new RedisStandaloneConfiguration();
        config.setHostName(datasource.getRedisHost());
        config.setPort(datasource.getRedisPort());
        config.setDatabase(datasource.getRedisDatabase());
        if (datasource.getRedisPassword() != null && !datasource.getRedisPassword().isEmpty()) {
            config.setPassword(datasource.getRedisPassword());
        }

        // 创建连接工厂
        LettuceConnectionFactory factory = new LettuceConnectionFactory(config);
        factory.afterPropertiesSet();

        // 创建RedisTemplate
        StringRedisTemplate template = new StringRedisTemplate(factory);
        template.afterPropertiesSet();

        // 存入缓存
        redisTemplateMap.put(datasource.getGroupId(), template);
    }

    /**
     * 根据groupId获取RedisTemplate
     */
    public StringRedisTemplate getRedisTemplate(String groupId) {
        StringRedisTemplate template = redisTemplateMap.get(groupId);
        if (template == null) {
            // 缓存不存在，从数据库加载
            RedisDatasource datasource = redisDatasourceService.getByGroupId(groupId);
            if (datasource == null) {
                throw new IllegalArgumentException("Redis数据源不存在，groupId: " + groupId);
            }
            addRedisSource(datasource);
            template = redisTemplateMap.get(groupId);
        }
        return template;
    }

    /**
     * 刷新指定groupId的数据源配置
     */
    public void refresh(String groupId) {
        redisTemplateMap.remove(groupId);
        RedisDatasource datasource = redisDatasourceService.getByGroupId(groupId);
        if (datasource != null) {
            addRedisSource(datasource);
        }
    }

    /**
     * 删除指定groupId的数据源
     */
    public void remove(String groupId) {
        redisTemplateMap.remove(groupId);
    }
}
