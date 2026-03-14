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
 * 遵循飞书开放平台加密规范：https://open.feishu.cn/document/ukTMukTMukTM/uUTNz4SN1MjL1UzM
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
     * IV长度
     */
    private static final int IV_LENGTH = 16;
    
    /**
     * 解密飞书加密事件
     *
     * @param encryptKey 飞书后台配置的encrypt_key
     * @param encrypt 加密的内容
     * @return 解密后的明文
     * @throws Exception 解密异常
     */
    public static String decrypt(String encryptKey, String encrypt) throws Exception {
        if (encryptKey == null || encryptKey.trim().isEmpty()) {
            throw new IllegalArgumentException("encrypt_key cannot be empty");
        }
        
        try {
            // 1. 对encrypt_key做base64解码，得到AES密钥
            byte[] aesKey = Base64Utils.decodeFromString(encryptKey + "=");
            SecretKeySpec secretKey = new SecretKeySpec(aesKey, ALGORITHM);
            
            // 2. 对encrypt内容做base64解码
            byte[] encryptedData = Base64Utils.decodeFromString(encrypt);
            
            // 3. 提取IV（前16字节）
            byte[] iv = Arrays.copyOfRange(encryptedData, 0, IV_LENGTH);
            IvParameterSpec ivParameterSpec = new IvParameterSpec(iv);
            
            // 4. 提取密文（16字节之后的内容）
            byte[] ciphertext = Arrays.copyOfRange(encryptedData, IV_LENGTH, encryptedData.length);
            
            // 5. AES解密
            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            cipher.init(Cipher.DECRYPT_MODE, secretKey, ivParameterSpec);
            byte[] decryptedData = cipher.doFinal(ciphertext);
            
            // 6. 解析解密后的内容：前4字节是内容长度（大端序）
            int contentLength = byteArrayToInt(Arrays.copyOfRange(decryptedData, 0, 4));
            
            // 7. 提取实际的JSON内容
            return new String(Arrays.copyOfRange(decryptedData, 4, 4 + contentLength), StandardCharsets.UTF_8);
            
        } catch (Exception e) {
            logger.error("Failed to decrypt feishu event: {}", e.getMessage(), e);
            throw new Exception("Decrypt failed", e);
        }
    }
    
    /**
     * 字节数组转int（大端序）
     */
    private static int byteArrayToInt(byte[] bytes) {
        return ((bytes[0] & 0xFF) << 24) |
               ((bytes[1] & 0xFF) << 16) |
               ((bytes[2] & 0xFF) << 8) |
               (bytes[3] & 0xFF);
    }
}
