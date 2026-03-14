package com.example.demo.utils;

import org.apache.commons.codec.binary.Base64;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.Base64Utils;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.Security;
import java.util.Arrays;

/**
 * 飞书事件加密解密工具类
 * 严格遵循飞书官方加密规范：https://open.feishu.cn/document/ukTMukTMukTM/uUTNz4SN1MjL1UzM
 * 加密算法：AES-256-CBC，PKCS7填充
 *
 * @author system
 * @date 2026-03-14
 */
public class FeishuCryptoUtil {
    
    private static final Logger logger = LoggerFactory.getLogger(FeishuCryptoUtil.class);
    
    /**
     * AES加密算法
     */
    private static final String ALGORITHM = "AES";
    
    /**
     * AES加密模式/填充方式
     */
    private static final String TRANSFORMATION = "AES/CBC/PKCS5Padding";
    
    /**
     * IV长度（固定16字节）
     */
    private static final int IV_LENGTH = 16;
    
    /**
     * 内容长度字段长度（固定4字节，大端序）
     */
    private static final int CONTENT_LENGTH_SIZE = 4;
    
    /**
     * 解密飞书加密事件
     *
     * @param encryptKey 飞书后台配置的encrypt_key（不需要加末尾的=）
     * @param encrypt 加密的内容（飞书回调的encrypt字段）
     * @return 解密后的明文JSON
     * @throws Exception 解密异常
     */
    static {
        // 解决部分 JDK 加密权限限制
        Security.setProperty("crypto.policy", "unlimited");
    }

    /**
     * 解密飞书的 encrypt 字符串
     * @param encrypt 透传过来的加密字符串
     * @return 解密后的原始明文 JSON
     */
    public static String decrypt(String encrypt,String encryptedKey) throws Exception {
        // 1. 对密钥做 SHA256 处理（飞书固定规则）
        byte[] key = org.apache.commons.codec.digest.DigestUtils.sha256(encryptedKey);
        // 2. 初始化 AES-CBC 解密器
        SecretKeySpec secretKeySpec = new SecretKeySpec(key, "AES");
        // IV 固定取密钥前 16 位
        IvParameterSpec iv = new IvParameterSpec(key, 0, 16);

        Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
        cipher.init(Cipher.DECRYPT_MODE, secretKeySpec, iv);

        // 3. Base64 解码 + AES 解密
        byte[] encrypted = Base64.decodeBase64(encrypt);
        byte[] original = cipher.doFinal(encrypted);

        // 4. 转换为字符串（飞书格式：随机字符串+长度+明文JSON）
        String result = new String(original, StandardCharsets.UTF_8);
        // 5. 截取有效 JSON 数据（去掉飞书前缀）
        return extractJson(result);
    }

    /**
     * 从解密后的字符串中，提取出真正的业务 JSON
     */
    private static String extractJson(String str) {
        // 格式：16位随机字符串 + 4位数据长度 + JSON 正文
        return str.substring(16);
    }

    public static void main(String[] args) throws Exception {
        System.out.println(decrypt("wEs8roJ4q7eOXFEO137PaLr5c9Zpv3DwNsLhJ2Fv4rBmTHWyjjsfWh0F9jww8Q5wAIuIv/OfhII7j/B9c0Kbs2C2ogaOCfIBAobU0qmX99mddBC1KO8mRll4aL56um+QcI5TX067SQM4k45h+JNfqN1WYnXhvjFM+owit5lErHC5GRmU53BtmVxLcz3+tnDN","zhanghui444555"));
    }
}
