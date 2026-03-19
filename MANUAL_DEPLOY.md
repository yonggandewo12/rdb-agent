# RDB-Agent v1.1.0 手动部署指南

## 📋 前置准备

### 1. 配置SSH访问（可选）
如果需要自动部署，请将以下公钥添加到服务器 `~/.ssh/authorized_keys`：

```
ssh-rsa AAAAB3NzaC1yc2EAAAADAQABAAABgQDBqRgTavbzhQN4CgkkFF2C6F25sr0AeQ/+rHJ2Wpbkyb2ps7ArsG3zvYQmMm0NcQfNc72zzRE8WrLB8UakBNElWc1YzL37FTHntEjN3m/xxdy45DUSjKpzxhM7N7lnj6FuV28M9yJ2c2EqV9YC0nwy0JTRCa7RW/S4ste98J21xE5vhQ2rZ7Kd96VUUtvjeR6K+F62XWyxytmzzND7YDytjIOtmGwMuhEAgWlew2J/Ping09W8bkiuYIiT130u47djmgL6d661kaXKL3E+zUlJ5AfEw81JXa1VJYGWAceOOJB1f5f4Kg9X0q9cp6cG5JZRUMWyPeMntwERUJn5Thf9QVD0qcd1GaZycQIcv2QGgwprT7ZuK69+whA8Xw4DCNKpotnkJXtSWjIn8nsAhCNo52JphxAgpYfS6oQpGYdIXP5D6/nLEGhiCcZAddwgK+uhBwJZPQ4F3hlYdkjUshUMZFwXzQxADtiK/jPNp2JvAAyP0x3McB9loMCTg6zM5WM= 17314823439@163.com
```

### 2. 准备部署文件
构建产物位置：`target/rdb-agent-0.0.1-SNAPSHOT-distribution.tar.gz`

---

## 🚀 部署步骤

### 步骤1：上传文件到服务器
```bash
# 在本地机器执行
scp target/rdb-agent-0.0.1-SNAPSHOT-distribution.tar.gz root@47.108.60.107:/root/rdb-agent/
```

### 步骤2：SSH登录服务器
```bash
ssh root@47.108.60.107
cd /root/rdb-agent
```

### 步骤3：停止旧服务
```bash
./bin/stop.sh
```

### 步骤4：备份旧版本（可选）
```bash
tar -czf rdb-agent-backup-$(date +%Y%m%d-%H%M%S).tar.gz rdb-agent-0.0.1-SNAPSHOT
```

### 步骤5：解压新版本
```bash
tar -xzf rdb-agent-0.0.1-SNAPSHOT-distribution.tar.gz
```

### 步骤6：初始化数据库
首先上传数据库schema文件到服务器，然后执行：
```bash
# 连接MySQL
mysql -h47.108.60.107 -udev -p1q2w3e4r rdb_agent

# 执行schema文件
source /path/to/schema.sql;
exit;
```

或者直接在本地执行schema文件（如果可以连接远程MySQL）。

> 升级提醒：老版本升级到 v1.1.0 需要先新增 `chat_id` 字段
> ```sql
> ALTER TABLE t_redis_datasource ADD COLUMN chat_id varchar(128) DEFAULT NULL COMMENT '飞书群聊ID' AFTER group_id;
> ALTER TABLE t_redis_datasource ADD UNIQUE KEY uk_chat_id (chat_id);
> ```

### 步骤7：更新配置（如需要）
```bash
vim config/application.yml
```

检查并更新以下配置：
- MySQL连接（已配置）
- Feishu应用凭证
- LLM API凭证
- 默认Redis连接

### 步骤8：启动服务
```bash
./bin/start.sh
```

### 步骤9：验证部署
```bash
# 查看日志
tail -f logs/application.log

# 测试健康检查
curl http://localhost:8999/
```

---

## 🎯 访问管理后台

部署成功后，访问：
- **管理后台URL**: `http://47.108.60.107:8999/`

### 首次使用流程：
1. 访问管理后台
2. 点击 "Add Datasource" 添加Redis数据源（记得填写对应的 Feishu `chat_id`）
3. 点击 "Add Task" 创建定时监控任务
4. 配置任务参数并启用

---

## 🔧 故障排查

### HikariCP类找不到
确保使用的是新的distribution.tar.gz包，而不是旧的jar文件。

### 端口冲突
检查8999端口是否被占用：
```bash
netstat -tlnp | grep 8999
```

### MySQL连接失败
检查MySQL服务状态和网络连接。

### 日志查看
```bash
tail -100f logs/application.log
```
