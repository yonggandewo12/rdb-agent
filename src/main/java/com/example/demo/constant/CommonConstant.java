package com.example.demo.constant;

/**
 * 通用常量类
 *
 * @author system
 * @date 2026-03-12
 */
public class CommonConstant {
    
    private CommonConstant() {
        // 工具类禁止实例化
    }
    
    /**
     * 默认字符集
     */
    public static final String DEFAULT_CHARSET = "UTF-8";
    
    /**
     * JSON Content-Type
     */
    public static final String CONTENT_TYPE_JSON = "application/json";
    
    /**
     * 授权头前缀
     */
    public static final String AUTH_HEADER_PREFIX = "Bearer ";
    
    /**
     * 成功响应码
     */
    public static final int SUCCESS_CODE = 200;
    
    /**
     * 默认过期时间（秒）
     */
    public static final long DEFAULT_EXPIRE_SECONDS = 3600;
    
    /**
     * 飞书相关常量
     */
    public static class Feishu {
        private Feishu() {}
        
        /**
         * 租户AccessToken获取地址
         */
        public static final String TENANT_TOKEN_URL = "https://open.feishu.cn/open-apis/auth/v3/tenant_access_token/internal";
        
        /**
         * 消息发送地址
         */
        public static final String MESSAGE_SEND_URL = "https://open.feishu.cn/open-apis/im/v1/messages";

        public static final String CHAT_LIST_URL = "https://open.feishu.cn/open-apis/im/v1/chats";

        /**
         * 消息回复地址前缀
         */
        public static final String MESSAGE_REPLY_PREFIX = "https://open.feishu.cn/open-apis/im/v1/messages/";
        
        /**
         * 消息回复地址后缀
         */
        public static final String MESSAGE_REPLY_SUFFIX = "/reply";
        
        /**
         * 消息类型-文本
         */
        public static final String MSG_TYPE_TEXT = "text";
        
        /**
         * 接收ID类型-OpenID
         */
        public static final String RECEIVE_ID_TYPE_OPENID = "open_id";
        
        /**
         * 接收ID类型-群ID
         */
        public static final String RECEIVE_ID_TYPE_CHAT = "chat_id";
        
        /**
         * 事件类型-消息接收
         */
        public static final String EVENT_TYPE_MESSAGE_RECEIVE = "im.message.receive_v1";
        
        /**
         * 聊天类型-私聊
         */
        public static final String CHAT_TYPE_PRIVATE = "private";
        
        /**
         * 聊天类型-群聊
         */
        public static final String CHAT_TYPE_GROUP = "group";
    }
    
    /**
     * LLM相关常量
     */
    public static class Llm {
        private Llm() {}
        
        /**
         * 系统角色
         */
        public static final String ROLE_SYSTEM = "system";
        
        /**
         * 用户角色
         */
        public static final String ROLE_USER = "user";
        
        /**
         * 默认温度参数
         */
        public static final double DEFAULT_TEMPERATURE = 0.1;
        
        /**
         * Redis操作指令-查询
         */
        public static final String OP_GET = "get";
        
        /**
         * Redis操作指令-设置
         */
        public static final String OP_SET = "set";
        
        /**
         * Redis操作指令-删除
         */
        public static final String OP_DELETE = "delete";
        
        /**
         * Redis操作指令-存在性判断
         */
        public static final String OP_EXISTS = "exists";
        
        /**
         * Redis操作指令-剩余过期时间
         */
        public static final String OP_TTL = "ttl";
        
        /**
         * Redis操作指令-哈希查询
         */
        public static final String OP_HGET = "hget";
        
        /**
         * Redis操作指令-哈希全量查询
         */
        public static final String OP_HGETALL = "hgetall";
        
        /**
         * Redis操作指令-列表范围查询
         */
        public static final String OP_LRANGE = "lrange";
        
        /**
         * Redis操作指令-集合成员查询
         */
        public static final String OP_SMEMBERS = "smembers";

        /**
         * Redis操作指令-键查询
         */
        public static final String OP_KEYS = "keys";

        /**
         * Redis操作指令-列表长度查询
         */
        public static final String OP_LLEN = "llen";
        
        /**
         * Redis操作指令-哈希设置
         */
        public static final String OP_HSET = "hset";
        
        /**
         * Redis操作指令-哈希批量设置
         */
        public static final String OP_HMSET = "hmset";
        
        /**
         * Redis操作指令-哈希删除
         */
        public static final String OP_HDEL = "hdelete";
        
        /**
         * Redis操作指令-列表左侧插入
         */
        public static final String OP_LPUSH = "lpush";
        
        /**
         * Redis操作指令-列表右侧插入
         */
        public static final String OP_RPUSH = "rpush";
        
        /**
         * Redis操作指令-集合添加
         */
        public static final String OP_SADD = "sadd";
        
        /**
         * Redis操作指令-集合移除
         */
        public static final String OP_SREM = "srem";
    }
    
    /**
     * 正则表达式常量
     */
    public static class Regex {
        private Regex() {}
        
        /**
         * 飞书@机器人前缀匹配
         */
        public static final String FEISHU_AT_PREFIX = "@_user_1.*? ";
    }
    
    /**
     * 错误信息常量
     */
    public static class ErrorMessage {
        private ErrorMessage() {}
        
        /**
         * 参数为空
         */
        public static final String PARAM_NULL = "参数不能为空";
        
        /**
         * LLM调用失败
         */
        public static final String LLM_CALL_FAILED = "大模型调用失败，请稍后重试";
        
        /**
         * 消息解析失败
         */
        public static final String MESSAGE_PARSE_FAILED = "消息解析失败";
        
        /**
         * Token获取失败
         */
        public static final String TOKEN_GET_FAILED = "获取AccessToken失败";
        
        /**
         * 消息发送失败
         */
        public static final String MESSAGE_SEND_FAILED = "消息发送失败";
        
        /**
         * 不支持的操作
         */
        public static final String UNSUPPORTED_OPERATION = "不支持的操作类型：";
    }
}
