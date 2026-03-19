package com.example.demo.controller;

import com.example.demo.entity.RedisDatasource;
import com.example.demo.entity.ScheduledTask;
import com.example.demo.service.RedisDatasourceService;
import com.example.demo.service.ScheduledTaskService;
import lombok.Data;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin")
public class AdminController {

    @Autowired
    private RedisDatasourceService redisDatasourceService;
    
    @Autowired
    private ScheduledTaskService scheduledTaskService;

    // ==================== Redis数据源管理 ====================
    
    @GetMapping("/datasources")
    public ResponseEntity<List<RedisDatasource>> listDatasources() {
        return ResponseEntity.ok(redisDatasourceService.listAllEnabled());
    }
    
    @PostMapping("/datasources")
    public ResponseEntity<Void> addDatasource(@RequestBody RedisDatasource datasource) {
        redisDatasourceService.save(datasource);
        redisDatasourceService.refreshCache(datasource.getGroupId());
        return ResponseEntity.ok().build();
    }
    
    @PutMapping("/datasources/{id}")
    public ResponseEntity<Void> updateDatasource(@PathVariable Long id, @RequestBody RedisDatasource datasource) {
        datasource.setId(id);
        redisDatasourceService.updateById(datasource);
        redisDatasourceService.refreshCache(datasource.getGroupId());
        return ResponseEntity.ok().build();
    }
    
    @DeleteMapping("/datasources/{id}")
    public ResponseEntity<Void> deleteDatasource(@PathVariable Long id) {
        RedisDatasource datasource = redisDatasourceService.getById(id);
        if (datasource != null) {
            redisDatasourceService.removeById(id);
            redisDatasourceService.refreshCache(datasource.getGroupId());
        }
        return ResponseEntity.ok().build();
    }
    
    @PostMapping("/datasources/{groupId}/test")
    public ResponseEntity<String> testDatasource(@PathVariable String groupId) {
        try {
            redisDatasourceService.getByGroupId(groupId);
            return ResponseEntity.ok("连接成功");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("连接失败: " + e.getMessage());
        }
    }

    // ==================== 定时任务管理 ====================
    
    @GetMapping("/tasks")
    public ResponseEntity<List<ScheduledTask>> listTasks() {
        return ResponseEntity.ok(scheduledTaskService.listAllEnabled());
    }
    
    @PostMapping("/tasks")
    public ResponseEntity<Void> addTask(@RequestBody ScheduledTask task) {
        task.setTaskId(java.util.UUID.randomUUID().toString().replace("-", ""));
        scheduledTaskService.addTask(task);
        return ResponseEntity.ok().build();
    }
    
    @PutMapping("/tasks/{id}")
    public ResponseEntity<Void> updateTask(@PathVariable Long id, @RequestBody ScheduledTask task) {
        task.setId(id);
        scheduledTaskService.updateTask(task);
        return ResponseEntity.ok().build();
    }
    
    @DeleteMapping("/tasks/{id}")
    public ResponseEntity<Void> deleteTask(@PathVariable Long id) {
        scheduledTaskService.deleteTask(id);
        return ResponseEntity.ok().build();
    }
    
    @PostMapping("/tasks/{id}/toggle")
    public ResponseEntity<Void> toggleTask(@PathVariable Long id, @RequestParam Integer enabled) {
        scheduledTaskService.toggleTask(id, enabled);
        return ResponseEntity.ok().build();
    }
    
    @PostMapping("/tasks/{id}/run")
    public ResponseEntity<Void> runTask(@PathVariable Long id) {
        scheduledTaskService.runTaskNow(id);
        return ResponseEntity.ok().build();
    }
}
