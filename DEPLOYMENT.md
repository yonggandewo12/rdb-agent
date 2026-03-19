# RDB-Agent v1.1.0 Deployment Guide

## 📦 Build Artifacts
- Location: `target/rdb-agent-0.0.1-SNAPSHOT-distribution.tar.gz`
- Size: ~43MB

## 🚀 Manual Deployment Steps

### 1. Upload to Server
```bash
scp target/rdb-agent-0.0.1-SNAPSHOT-distribution.tar.gz root@47.108.60.107:/root/rdb-agent/
```

### 2. SSH to Server
```bash
ssh root@47.108.60.107
cd /root/rdb-agent
```

### 3. Stop Old Service
```bash
./bin/stop.sh
```

### 4. Backup & Extract New Version
```bash
# Backup old version (optional)
tar -czf rdb-agent-backup-$(date +%Y%m%d-%H%M%S).tar.gz rdb-agent-0.0.1-SNAPSHOT

# Extract new version
tar -xzf rdb-agent-0.0.1-SNAPSHOT-distribution.tar.gz
```

### 5. Configure Database
First, run the database schema:
```bash
# Connect to MySQL
mysql -h47.108.60.107 -udev -p1q2w3e4r rdb_agent

# Execute schema file (upload it first or copy-paste)
source /path/to/schema.sql
```

> 升级提示：如果是从旧版本升级，请先执行以下SQL给 `t_redis_datasource` 表新增 `chat_id` 字段：
```sql
ALTER TABLE t_redis_datasource ADD COLUMN chat_id varchar(128) DEFAULT NULL COMMENT '飞书群聊ID' AFTER group_id;
ALTER TABLE t_redis_datasource ADD UNIQUE KEY uk_chat_id (chat_id);
```

### 6. Update Configuration
```bash
vim config/application.yml
```

Update these settings as needed:
- MySQL connection (already configured)
- Feishu app credentials
- LLM API credentials
- Default Redis connection

### 7. Start Service
```bash
./bin/start.sh
```

### 8. Verify Deployment
```bash
# Check logs
tail -f logs/application.log

# Test health
curl http://localhost:8999/
```

## 🎯 Access Admin Dashboard
- URL: `http://47.108.60.107:8999/`
- First, configure Redis datasources
- Ensure each datasource has the correct Feishu `chat_id`
- Then create scheduled monitoring tasks

## 📝 Database Schema
Location: `src/main/resources/db/schema.sql`

## 🔧 Troubleshooting
- HikariCP class not found: Make sure you're using the new distribution tar.gz
- Port conflicts: Check if port 8999 is available
- MySQL connection: Verify credentials and network access
