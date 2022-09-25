package io.github.wycst.wast.common.utils;

import java.util.Arrays;

/**
 * @Author wangyunchao
 * @Date 2022/9/16 12:11
 */
public class IOUtils {

    /***
     * 读取UTF-8编码的字节数组转化为字符数组
     *
     * @param bytes
     * @return
     */
    public final static char[] readUTF8Bytes(byte[] bytes) {
        if (bytes == null) return null;
        int len = bytes.length;
        char[] chars = new char[len];
        int charLen = readUTF8Bytes(bytes, chars);
        if (charLen != len) {
            chars = Arrays.copyOf(chars, charLen);
        }
        return chars;
    }

    /***
     * 读取UTF-8编码的字节到指定字符数组，请确保字符数组长度
     *
     * @param bytes
     * @param chars 目标字符
     * @return 实际字符数组长度
     */
    public final static int readUTF8Bytes(byte[] bytes, char[] chars) {
        return readUTF8Bytes(bytes, 0, bytes.length, chars, 0);
    }

    /***
     * 读取UTF-8编码的字节到指定字符数组，请确保字符数组长度
     *
     * 一个字节 0000 0000-0000 007F | 0xxxxxxx
     * 二个字节 0000 0080-0000 07FF | 110xxxxx 10xxxxxx
     * 三个字节 0000 0800-0000 FFFF | 1110xxxx 10xxxxxx 10xxxxxx
     * 四个字节 0001 0000-0010 FFFF | 11110xxx 10xxxxxx 10xxxxxx 10xxxxxx
     *
     * @param bytes
     * @param offset    字节偏移位置
     * @param len       字节长度
     * @param chars     目标字符
     * @param cOffset   字符数组偏移位置
     * @return 字符数组长度
     */
    public final static int readUTF8Bytes(byte[] bytes, int offset, int len, char[] chars, int cOffset) {
        if (bytes == null) return 0;
        int charLen = cOffset;
        for (int j = offset, max = offset + len; j < max; ++j) {
            byte b = bytes[j];
            if (b >= 0) {
                chars[charLen++] = (char) b;
                continue;
            }
            // 读取字节b的前4位判断需要读取几个字节
            int s = b >> 4;
            switch (s) {
                case -1:
                    // 1111 4个字节
                    if (j < max - 3) {
                        // 第1个字节的后4位 + 第2个字节的后6位 + 第3个字节的后6位 + 第4个字节的后6位
                        byte b1 = bytes[++j];
                        byte b2 = bytes[++j];
                        byte b3 = bytes[++j];
                        int a = ((b & 0x7) << 18) | ((b1 & 0x3f) << 12) | ((b2 & 0x3f) << 6) | (b3 & 0x3f);
                        if (Character.isSupplementaryCodePoint(a)) {
                            chars[charLen++] = (char) ((a >>> 10)
                                    + (Character.MIN_HIGH_SURROGATE - (Character.MIN_SUPPLEMENTARY_CODE_POINT >>> 10)));
                            chars[charLen++] = (char) ((a & 0x3ff) + Character.MIN_LOW_SURROGATE);
                        } else {
                            chars[charLen++] = (char) a;
                        }
                        break;
                    } else {
                        throw new UnsupportedOperationException("utf-8 character error ");
                    }
                case -2:
                    // 1110 3个字节
                    if (j < max - 2) {
                        // 第1个字节的后4位 + 第2个字节的后6位 + 第3个字节的后6位
                        byte b1 = bytes[++j];
                        byte b2 = bytes[++j];
                        int a = ((b & 0xf) << 12) | ((b1 & 0x3f) << 6) | (b2 & 0x3f);
                        chars[charLen++] = (char) a;
                        break;
                    } else {
                        throw new UnsupportedOperationException("utf-8 character error ");
                    }
                case -3:
                    // 1101 和 1100都按2字节处理
                case -4:
                    // 1100 2个字节
                    if (j < max - 1) {
                        byte b1 = bytes[++j];
                        int a = ((b & 0x1f) << 6) | (b1 & 0x3f);
                        chars[charLen++] = (char) a;
                        break;
                    } else {
                        throw new UnsupportedOperationException("utf-8 character error ");
                    }
                default:
                    throw new UnsupportedOperationException("utf-8 character error ");
            }
        }
        return charLen;
    }

}
