package org.framework.light.common.tools;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.security.Key;

/**
 * AES安全编码组件
 */
public abstract class AESCoder {

    /**
     * 密钥算法
     */
    public static final String KEY_ALGORITHM = "AES";

    /**
     * 加密/解密算法 /工作模式/填充方式
     */
    public static final String CIPHER_ALGORITHM = "AES/ECB/PKCS5Padding";

    static {
        try {
            generatorKey();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 转换密钥
     *
     * @param key 二进制密钥
     * @return key 密钥
     * @throws Exception
     */
    private static Key toKey(byte[] key) throws Exception {
        SecretKey secretKey = new SecretKeySpec(key, KEY_ALGORITHM);
        return secretKey;
    }

    /**
     * 解密
     *
     * @param data 待解密数据
     * @param key  密钥
     * @return byte[] 解密数据
     * @throws Exception
     */
    public static byte[] decrypt(byte[] data, byte[] key) throws Exception {
        //还原密钥
        Key k = toKey(key);
        //实例化 
        Cipher cipher = Cipher.getInstance(CIPHER_ALGORITHM);
        //初始化，设置为解密模式
        cipher.init(Cipher.DECRYPT_MODE, k);
        //执行操作
        return cipher.doFinal(data);
    }

    /**
     * 加密
     *
     * @param data 待加密数据
     * @param key  密钥
     * @return byte[] 加密数据
     * @throws Exception
     */
    public static byte[] encrypt(byte[] data, byte[] key) throws Exception {
        //还原密钥
        Key k = toKey(key);
        //实例化
        Cipher cipher = Cipher.getInstance(CIPHER_ALGORITHM);
        //初始化，设置为加密模式
        cipher.init(Cipher.ENCRYPT_MODE, k);
        //执行操作
        return cipher.doFinal(data);
    }

    /**
     * 生产密钥(256位)
     *
     * @return byte[] 二进制密钥
     * @throws Exception
     */
    public static byte[] generatorKey() throws Exception {
        return generatorKey(256);
    }

    /**
     * 生产密钥
     *
     * @param length [128 192 256]
     * @return 二进制密钥
     * @throws Exception
     */
    public static byte[] generatorKey(int length) throws Exception {
        /*
         * 实例化密钥生成器
         * 若要使用64位密钥注意替换
         * 讲下述代码中的
         * KeyGenerator.getInstance(KEY_ALGORITHM);
         * 替换为
         * KeyGenerator.getInstance(KEY_ALGORITHM，"BC");
         */
        KeyGenerator kg = KeyGenerator.getInstance(KEY_ALGORITHM);
        /*
         * 初始化密钥生成器
         * 若要使用64位密钥注意替换
         * 将下述的代码 kg.init(56);
         * 替换为 kg.init(64);
         */
        kg.init(length);
        //生成密钥
        SecretKey secretKey = kg.generateKey();
        //获得密钥的二进制编码形式
        return secretKey.getEncoded();
    }
}