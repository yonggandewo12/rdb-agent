package com.example.demo.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * Redis数据源配置实体
 */
@Data
@TableName("t_redis_datasource")
public class RedisDatasource {

    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 分组ID，唯一标识
     */
    private String groupId;

    /**
     * 飞书群聊ID，用于匹配群聊消息
     */
    private String chatId;

    /**
     * 分组名称
     */
    private String groupName;

    /**
     * Redis地址
     */
    private String redisHost;

    /**
     * Redis端口
     */
    private Integer redisPort;

    /**
     * Redis密码
     */
    private String redisPassword;

    /**
     * Redis数据库号
     */
    private Integer redisDatabase;

    /**
     * 连接超时时间(ms)
     */
    private Integer timeout;

    /**
     * 最大连接数
     */
    private Integer maxActive;

    /**
     * 最大空闲连接数
     */
    private Integer maxIdle;

    /**
     * 最小空闲连接数
     */
    private Integer minIdle;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    private LocalDateTime updateTime;

    /**
     * 是否删除 0:未删除 1:已删除
     */
    @TableLogic
    private Integer isDeleted;
}
