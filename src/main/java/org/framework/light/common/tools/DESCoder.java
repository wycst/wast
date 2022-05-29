package org.framework.light.common.tools;

import javax.crypto.*;
import javax.crypto.spec.DESKeySpec;
import java.security.Key;
import java.security.NoSuchAlgorithmException;

/**
 * DES安全编码组件
 */
public abstract class DESCoder {

    /**
     * 密钥算法
     * java 7只支持56位密钥
     * Bouncy Castle 支持64位密钥
     */
    public static final String KEY_ALGORITHM = "DES";

    /**
     * 加密/解密算法 /工作模式/填充方式
     */
    public static final String CIPHER_ALGORITHM = "DES/ECB/PKCS5Padding";

    protected DESCoder() throws NoSuchPaddingException, NoSuchAlgorithmException {
    }

    /**
     * 转换密钥
     *
     * @param key 二进制密钥
     * @return key 密钥
     * @throws Exception
     */
    private static Key toKey(byte[] key) throws Exception {
        //实例化DES密钥材料
        DESKeySpec dks = new DESKeySpec(key);
        //实例化密钥工厂
        SecretKeyFactory keyFactory = SecretKeyFactory.getInstance(KEY_ALGORITHM);
        //生产密钥
        SecretKey secretKey = keyFactory.generateSecret(dks);
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

    static Cipher cipher;

    static {
        try {
            cipher = Cipher.getInstance(CIPHER_ALGORITHM);
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        } catch (NoSuchPaddingException e) {
            e.printStackTrace();
        }
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
        // Cipher cipher = Cipher.getInstance(CIPHER_ALGORITHM);
        //初始化，设置为加密模式
        cipher.init(Cipher.ENCRYPT_MODE, k);
        //执行操作
        return cipher.doFinal(data);
    }

    /**
     * 生产密钥
     * java 7只支持56位 密钥
     * Bouncy Castle 支持64位密钥
     *
     * @return byte[] 二进制密钥
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

    public static byte[] generatorKey() throws Exception {
        return generatorKey(56);
    }

}