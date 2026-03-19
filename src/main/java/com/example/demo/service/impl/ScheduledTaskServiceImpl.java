package com.example.demo.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.demo.entity.ScheduledTask;
import com.example.demo.job.RedisMonitorJob;
import com.example.demo.mapper.ScheduledTaskMapper;
import com.example.demo.service.ScheduledTaskService;
import lombok.extern.slf4j.Slf4j;
import org.quartz.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.List;

@Slf4j
@Service
public class ScheduledTaskServiceImpl extends ServiceImpl<ScheduledTaskMapper, ScheduledTask> implements ScheduledTaskService {

    @Autowired
    private Scheduler scheduler;

    @PostConstruct
    @Override
    public void initAllTasks() {
        List<ScheduledTask> enabledTasks = listAllEnabled();
        for (ScheduledTask task : enabledTasks) {
            scheduleTask(task);
        }
        log.info("已初始化 {} 个定时任务", enabledTasks.size());
    }

    @Override
    public List<ScheduledTask> listAllEnabled() {
        LambdaQueryWrapper<ScheduledTask> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(ScheduledTask::getIsDeleted, 0);
        return list(queryWrapper);
    }

    @Override
    public boolean addTask(ScheduledTask task) {
        boolean saved = save(task);
        if (saved && task.getEnabled() == 1) {
            scheduleTask(task);
        }
        return saved;
    }

    @Override
    public boolean updateTask(ScheduledTask task) {
        removeTask(task.getId());
        boolean updated = updateById(task);
        if (updated && task.getEnabled() == 1) {
            scheduleTask(task);
        }
        return updated;
    }

    @Override
    public boolean deleteTask(Long id) {
        removeTask(id);
        return removeById(id);
    }

    @Override
    public boolean toggleTask(Long id, Integer enabled) {
        ScheduledTask task = getById(id);
        if (task == null) return false;
        
        if (enabled == 1) {
            scheduleTask(task);
        } else {
            removeTask(id);
        }
        
        task.setEnabled(enabled);
        return updateById(task);
    }

    @Override
    public boolean runTaskNow(Long id) {
        ScheduledTask task = getById(id);
        if (task == null) return false;
        
        try {
            JobKey jobKey = new JobKey(task.getTaskId(), task.getGroupId());
            scheduler.triggerJob(jobKey);
            log.info("手动触发任务: {}", task.getTaskName());
            return true;
        } catch (SchedulerException e) {
            log.error("手动触发任务失败: {}", task.getTaskName(), e);
            return false;
        }
    }

    private void scheduleTask(ScheduledTask task) {
        try {
            JobKey jobKey = new JobKey(task.getTaskId(), task.getGroupId());
            
            JobDetail jobDetail = JobBuilder.newJob(RedisMonitorJob.class)
                    .withIdentity(jobKey)
                    .usingJobData("taskId", task.getTaskId())
                    .storeDurably()
                    .build();

            CronTrigger trigger = TriggerBuilder.newTrigger()
                    .withIdentity(task.getTaskId() + "_trigger", task.getGroupId())
                    .withSchedule(CronScheduleBuilder.cronSchedule(task.getCronExpression()))
                    .build();

            scheduler.scheduleJob(jobDetail, trigger);
            log.info("调度定时任务: {}, cron: {}", task.getTaskName(), task.getCronExpression());
            
        } catch (SchedulerException e) {
            log.error("调度任务失败: {}", task.getTaskName(), e);
        }
    }

    private void removeTask(Long id) {
        ScheduledTask task = getById(id);
        if (task == null) return;
        
        try {
            JobKey jobKey = new JobKey(task.getTaskId(), task.getGroupId());
            scheduler.deleteJob(jobKey);
            log.info("移除定时任务: {}", task.getTaskName());
        } catch (SchedulerException e) {
            log.error("移除任务失败: {}", task.getTaskName(), e);
        }
    }
}
