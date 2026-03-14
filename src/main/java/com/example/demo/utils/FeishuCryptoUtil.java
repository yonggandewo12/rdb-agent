package com.example.demo.utils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.Base64Utils;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
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
    public static String decrypt(String encryptKey, String encrypt) throws Exception {
        if (encryptKey == null || encryptKey.trim().isEmpty()) {
            throw new IllegalArgumentException("encrypt_key cannot be empty");
        }
        if (encrypt == null || encrypt.trim().isEmpty()) {
            throw new IllegalArgumentException("encrypt content cannot be empty");
        }
        
        try {
            // 1. 处理encrypt_key：飞书提供的encrypt_key是base64编码，自动补全padding
            String base64Key = encryptKey;
            int padding = (4 - base64Key.length() % 4) % 4;
            if (padding > 0) {
                base64Key += "====".substring(0, padding);
            }
            byte[] aesKey = Base64Utils.decodeFromString(base64Key);
            logger.debug("AES key length: {} bytes", aesKey.length);
            
            // 2. 解码加密内容
            byte[] encryptedData = Base64Utils.decodeFromString(encrypt);
            if (encryptedData.length < IV_LENGTH) {
                throw new IllegalArgumentException("Invalid encrypted data: length " + encryptedData.length + " < 16 bytes");
            }
            
            // 3. 提取IV（前16字节）
            byte[] iv = Arrays.copyOfRange(encryptedData, 0, IV_LENGTH);
            IvParameterSpec ivParameterSpec = new IvParameterSpec(iv);
            
            // 4. 提取密文（16字节之后的内容）
            byte[] ciphertext = Arrays.copyOfRange(encryptedData, IV_LENGTH, encryptedData.length);
            
            // 5. AES解密
            SecretKeySpec secretKey = new SecretKeySpec(aesKey, ALGORITHM);
            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            cipher.init(Cipher.DECRYPT_MODE, secretKey, ivParameterSpec);
            byte[] decryptedData = cipher.doFinal(ciphertext);
            
            if (decryptedData.length < CONTENT_LENGTH_SIZE) {
                throw new IllegalArgumentException("Decrypted data too short: " + decryptedData.length + " < 4 bytes");
            }
            
            // 6. 解析内容长度：前4字节是大端序的内容长度
            int contentLength = ((decryptedData[0] & 0xFF) << 24)
                              | ((decryptedData[1] & 0xFF) << 16)
                              | ((decryptedData[2] & 0xFF) << 8)
                              |  (decryptedData[3] & 0xFF);
            
            logger.debug("Decrypted data length: {}, content length: {}", decryptedData.length, contentLength);
            
            // 7. 提取实际JSON内容：4字节之后的contentLength个字节
            if (contentLength < 0 || contentLength > decryptedData.length - CONTENT_LENGTH_SIZE) {
                throw new IllegalArgumentException("Invalid content length: " + contentLength + 
                    ", available: " + (decryptedData.length - CONTENT_LENGTH_SIZE));
            }
            
            String result = new String(decryptedData, CONTENT_LENGTH_SIZE, contentLength, StandardCharsets.UTF_8);
            logger.debug("Decrypt success, result length: {}", result.length());
            return result;
            
        } catch (Exception e) {
            logger.error("Failed to decrypt feishu event: {}", e.getMessage(), e);
            throw new Exception("Decrypt failed: " + e.getMessage(), e);
        }
    }
}
