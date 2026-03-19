package com.example.demo.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.example.demo.entity.ScheduledTask;
import java.util.List;

public interface ScheduledTaskService extends IService<ScheduledTask> {

    /**
     * 查询所有启用的任务
     */
    List<ScheduledTask> listAllEnabled();

    /**
     * 新增任务
     */
    boolean addTask(ScheduledTask task);

    /**
     * 更新任务
     */
    boolean updateTask(ScheduledTask task);

    /**
     * 删除任务
     */
    boolean deleteTask(Long id);

    /**
     * 启停任务
     */
    boolean toggleTask(Long id, Integer enabled);

    /**
     * 立即执行一次任务
     */
    boolean runTaskNow(Long id);

    /**
     * 系统启动时初始化所有定时任务
     */
    void initAllTasks();
}
