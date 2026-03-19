package com.example.demo.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.example.demo.entity.RedisDatasource;
import java.util.List;

public interface RedisDatasourceService extends IService<RedisDatasource> {

    /**
     * 查询所有启用的数据源
     */
    List<RedisDatasource> listAllEnabled();

    /**
     * 根据groupId查询数据源
     */
    RedisDatasource getByGroupId(String groupId);

    /**
     * 根据chatId查询数据源
     */
    RedisDatasource getByChatId(String chatId);

    /**
     * 刷新数据源缓存
     */
    void refreshCache(String groupId);
}
