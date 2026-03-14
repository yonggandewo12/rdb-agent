# RDB-Agent (Redis Agent)

## 📝 项目简介

RDB-Agent 是一款基于飞书机器人的**智能Redis查询代理工具**，用户可以通过自然语言对话的方式直接操作Redis数据库，无需记忆复杂的Redis命令，降低Redis使用门槛，提升开发和运维效率。

## ✨ 功能特性

- 🎯 **自然语言交互**：支持通过飞书机器人用自然语言查询和操作Redis
- 🔧 **全命令支持**：覆盖String/Hash/List/Set等常用Redis数据结构操作
- 🤖 **自动回复**：查询结果自动格式化为易读的自然语言回复到飞书
- 💬 **多场景适配**：同时支持飞书群聊@机器人和私聊两种使用方式
- 🔒 **安全可靠**：内置参数校验、异常处理、操作日志全链路可追溯
- 🚀 **开箱即用**：标准Spring Boot架构，最小配置即可快速部署上线
- 📦 **部署便捷**：提供assembly打包脚本、start/stop启停脚本，生产环境友好

## 🛠️ 技术栈

| 技术/组件 | 版本 | 说明 |
|---------|-----|-----|
| Spring Boot | 2.7.18 | 核心开发框架 |
| Spring Data Redis | 2.7.18 | Redis操作客户端 |
| 飞书开放平台API | - | 飞书消息发送/接收 |
| OpenAI API兼容 | - | 大模型自然语言处理 |
| OkHttp3 | 4.12.0 | 外部API调用客户端 |
| Lombok | 1.18.30 | 简化代码 |
| JDK | 1.8+ | 运行环境要求 |

## 🏗️ 架构设计

### 分层架构
```
com.example.demo
├── controller/       # 控制层：接收飞书事件回调
│   └── FeishuController.java
├── service/          # 服务层：核心业务逻辑
│   ├── FeishuService.java          # 飞书事件处理
│   ├── FeishuMessageService.java   # 飞书消息发送
│   ├── LlmService.java             # 大模型API调用
│   ├── RedisService.java           # Redis操作封装
│   └── impl/                       # 服务实现类
├── dto/              # 数据传输层：飞书事件请求DTO
│   └── feishu/
├── constant/         # 常量层：通用常量、错误信息等
│   └── CommonConstant.java
└── DemoApplication.java  # 启动类
```

### 工作流程
```
用户@飞书机器人提问 → 飞书平台回调事件 → FeishuController接收请求
       ↓
解析消息内容 → 调用LLM大模型生成结构化Redis操作指令
       ↓
执行Redis操作 → 将结果格式化为自然语言回答 → 回复到飞书对话界面
```

## 🚀 快速部署

### 环境要求
- JDK 1.8 或更高版本
- Redis 5.0+ 服务
- 飞书企业应用权限
- 支持OpenAI协议的大模型API服务

### 部署步骤

#### 1. 拉取代码
```bash
git clone git@github.com:yonggandewo12/rdb-agent.git
cd rdb-agent
```

#### 2. 修改配置文件
编辑 `src/main/resources/application.yml`，配置以下参数：
```yaml
# 服务端口
server:
  port: 8999

# Redis配置
spring:
  redis:
    host: 你的Redis地址
    port: 你的Redis端口
    password: 你的Redis密码
    database: 0

# 飞书应用配置
feishu:
  app-id: 飞书应用APP_ID
  app-secret: 飞书应用APP_SECRET
  verification-token: 事件订阅校验TOKEN
  encrypt-key: 加密密钥（可选）

# 大模型配置
llm:
  api-key: 大模型API密钥
  base-url: 大模型API地址（默认OpenAI格式）
  model: 模型名称（如gpt-3.5-turbo）
```

#### 3. 项目打包
```bash
mvn clean package -DskipTests
```

打包后产物在 `target/rdb-agent-0.0.1-SNAPSHOT-distribution.tar.gz`

#### 4. 服务启动
```bash
# 解压部署包
tar -zxvf rdb-agent-0.0.1-SNAPSHOT-distribution.tar.gz
cd rdb-agent-0.0.1-SNAPSHOT

# 启动服务
./bin/start.sh

# 停止服务
./bin/stop.sh
```

#### 5. 飞书后台配置
1. 登录飞书开放平台创建企业应用
2. 开启事件订阅，回调地址填写：`http://你的公网IP:8999/api/feishu/webhook`
3. 订阅事件：`接收消息v2` (im.message.receive_v1)
4. 申请权限：`获取用户发给机器人的单聊消息`、`获取群组中@机器人的消息`、`以应用身份发送消息`
5. 发布版本后即可使用

## 🎮 使用说明

### 支持的操作示例

| 飞书提问示例 | 说明 |
|-----------|-----|
| `@Redis助手 查询key为user:1的value` | 查询String类型值 |
| `@Redis助手 查询哈希user:info的name字段` | 查询Hash结构字段 |
| `@Redis助手 查询列表user:list的前10个元素` | 查询List范围 |
| `@Redis助手 设置key name为张三，过期时间60秒` | 设置值并指定过期时间 |
| `@Redis助手 查看key name是否存在` | 检查键是否存在 |
| `@Redis助手 查看key name的剩余过期时间` | 查询剩余TTL |
| `@Redis助手 删除key name` | 删除指定键 |
| `@Redis助手 查询集合tags的所有成员` | 查询Set集合成员 |
| `@Redis助手 查询列表user:list的长度` | 查询List长度 |
| `@Redis助手 查询哈希user:info的所有字段` | 获取Hash全部内容 |

### 注意事项
- 群聊使用时需要@机器人，私聊直接发送消息即可
- 复杂查询建议明确描述需求，避免歧义
- 敏感操作（如删除、修改）建议先在测试环境验证

## 🔧 开发调试

### 本地运行
```bash
# 直接启动
mvn spring-boot:run
```

### 远程调试
- 服务启动后自动开启远程调试端口：`9999`
- IDE配置远程调试连接 `服务器IP:9999` 即可断点调试

### 单元测试
```bash
# 运行全部测试
mvn test

# 运行指定测试类
mvn test -Dtest=RedisServiceTest
```

### 目录说明
```
rdb-agent-<版本号>/
├── bin/          # 启停脚本目录
│   ├── start.sh
│   └── stop.sh
├── lib/          # 依赖jar包目录
├── config/       # 配置文件目录
│   └── application.yml
└── logs/         # 日志目录
```

## 📄 许可证

本项目采用 MIT 许可证，详见 LICENSE 文件。

## 🤝 贡献

欢迎提交 Issue 和 Pull Request 贡献代码！
