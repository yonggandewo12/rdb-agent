package com.example.demo.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.demo.config.DynamicRedisSourceManager;
import com.example.demo.entity.RedisDatasource;
import com.example.demo.mapper.RedisDatasourceMapper;
import com.example.demo.service.RedisDatasourceService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import redis.clients.jedis.Jedis;

import java.util.List;

@Service
public class RedisDatasourceServiceImpl extends ServiceImpl<RedisDatasourceMapper, RedisDatasource> implements RedisDatasourceService {

    @Autowired
    private DynamicRedisSourceManager redisSourceManager;

    @Override
    public List<RedisDatasource> listAllEnabled() {
        LambdaQueryWrapper<RedisDatasource> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(RedisDatasource::getIsDeleted, 0);
        return list(queryWrapper);
    }

    @Override
    public RedisDatasource getByGroupId(String groupId) {
        LambdaQueryWrapper<RedisDatasource> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(RedisDatasource::getGroupId, groupId)
                .eq(RedisDatasource::getIsDeleted, 0);
        return getOne(queryWrapper);
    }

    @Override
    public RedisDatasource getByChatId(String chatId) {
        LambdaQueryWrapper<RedisDatasource> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(RedisDatasource::getChatId, chatId)
                .eq(RedisDatasource::getIsDeleted, 0);
        return getOne(queryWrapper);
    }

    @Override
    public void testConnection(RedisDatasource datasource) {
        if (datasource == null) {
            throw new IllegalArgumentException("Redis数据源不能为空");
        }

        String redisHost = datasource.getRedisHost();
        Integer redisPort = datasource.getRedisPort() != null ? datasource.getRedisPort() : 6379;
        int timeout = datasource.getTimeout() != null && datasource.getTimeout() > 0 ? datasource.getTimeout() : 3000;

        if (redisHost == null || redisHost.trim().isEmpty()) {
            throw new IllegalArgumentException("Redis地址不能为空");
        }

        try (Jedis jedis = new Jedis(redisHost, redisPort, timeout)) {
            if (datasource.getRedisPassword() != null && !datasource.getRedisPassword().isEmpty()) {
                jedis.auth(datasource.getRedisPassword());
            }
            if (datasource.getRedisDatabase() != null) {
                jedis.select(datasource.getRedisDatabase());
            }
            jedis.ping();
        } catch (Exception e) {
            throw new RuntimeException("Redis连接失败: " + e.getMessage(), e);
        }
    }

    @Override
    public void refreshCache(String groupId) {
        redisSourceManager.refresh(groupId);
    }
}
