package com.example.demo.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 定时任务配置实体
 */
@Data
@TableName("t_scheduled_task")
public class ScheduledTask {

    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 任务ID，唯一标识
     */
    private String taskId;

    /**
     * 任务名称
     */
    private String taskName;

    /**
     * 关联的Redis分组ID
     */
    private String groupId;

    /**
     * CRON表达式
     */
    private String cronExpression;

    /**
     * 任务类型：slow_query(慢查询监控)/big_key(大key检测)/all(全部)
     */
    private String taskType;

    /**
     * 慢查询阈值(微秒)
     */
    private Long slowQueryThreshold;

    /**
     * 大key内存阈值(字节)
     */
    private Long bigKeyMemoryThreshold;

    /**
     * 大key元素数量阈值
     */
    private Long bigKeyCountThreshold;

    /**
     * 飞书通知群ID
     */
    private String notifyChatId;

    /**
     * 是否启用 0:禁用 1:启用
     */
    private Integer enabled;

    /**
     * 上次执行时间
     */
    private LocalDateTime lastRunTime;

    /**
     * 下次执行时间
     */
    private LocalDateTime nextRunTime;

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
