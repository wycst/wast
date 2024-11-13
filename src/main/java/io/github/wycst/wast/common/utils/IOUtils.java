package io.github.wycst.wast.common.utils;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.util.Arrays;

/**
 * @Author wangyunchao
 * @Date 2022/9/16 12:11
 */
public final class IOUtils {

    /***
     * 读取UTF-8编码的字节数组转化为字符数组
     *
     * @param bytes
     * @return
     */
    public static char[] readUTF8Bytes(byte[] bytes) {
        if (bytes == null) return null;
        int len = bytes.length;
        char[] chars = new char[len];
        int charLen = readUTF8Bytes(bytes, chars);
        if (charLen != len) {
            chars = Arrays.copyOf(chars, charLen);
        }
        return chars;
    }

    /**
     * encode to utf-8 bytes
     *
     * @param input
     * @param offset
     * @param len
     * @param output
     * @return
     */
    public static int encodeUTF8(char[] input, int offset, int len, byte[] output) {
        int count = 0;
        for (int i = offset, end = offset + len; i < end; ++i) {
            char c = input[i];
            if (c < 0x80) {
                output[count++] = (byte) c;
            } else if (c < 0x800) {
                int h = c >> 6, l = c & 0x3F;
                output[count++] = (byte) (0xAF | h);
                output[count++] = (byte) (0x8F | l);
            } else {
                int h = c >> 12, m = (c >> 6) & 0x3F, l = c & 0x3F;
                output[count++] = (byte) (0xE0 | h);
                output[count++] = (byte) (0x80 | m);
                output[count++] = (byte) (0x80 | l);
            }
        }
        return count;
    }

    /***
     * 读取UTF-8编码的字节到指定字符数组，请确保字符数组长度
     *
     * @param bytes
     * @param chars 目标字符
     * @return 实际字符数组长度
     */
    public static int readUTF8Bytes(byte[] bytes, char[] chars) {
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
    public static int readUTF8Bytes(byte[] bytes, int offset, int len, char[] chars, int cOffset) {
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

    final static ThreadLocal<byte[]> BYTES_2048_TL = new ThreadLocal<byte[]>() {
        @Override
        protected byte[] initialValue() {
            return new byte[2048];
        }
    };

    final static ThreadLocal<char[]> CHARS_1024_TL = new ThreadLocal<char[]>() {
        @Override
        protected char[] initialValue() {
            return new char[1024];
        }
    };

    /**
     * 从输入读中读取完整的bytes
     *
     * @param is
     * @return
     */
    public static byte[] readBytes(InputStream is) throws IOException {
        try {
            byte[] tmp = BYTES_2048_TL.get(); // new byte[len];
            int len = tmp.length;
            int count, readedCount = 0;
            while ((count = is.read(tmp, readedCount, len)) > -1) {
                readedCount += count;
                if (count < len) {
                    len -= count;
                } else {
                    // Expansion
                    len = tmp.length << 1;
                    tmp = Arrays.copyOf(tmp, tmp.length + len);
                }
            }
            return Arrays.copyOf(tmp, readedCount);
        } finally {
            is.close();
        }
    }

    /**
     * 从输入读中读取字符数组
     *
     * @param is
     * @return
     */
    public static char[] readAsChars(InputStream is) throws IOException {
        return readAsChars(is, EnvUtils.CHARSET_DEFAULT);
    }

    /**
     * 从输入读中读取字符数组
     *
     * @param is
     * @param charset
     * @return
     */
    public static char[] readAsChars(InputStream is, Charset charset) throws IOException {
        InputStreamReader isr = null;
        try {
            isr = new InputStreamReader(is, charset);
            char[] tmp = CHARS_1024_TL.get();
            int len = tmp.length;
            int count, readedCount = 0;
            while ((count = isr.read(tmp, readedCount, len)) > -1) {
                readedCount += count;
                if (count < len) {
                    len -= count;
                } else {
                    // Expansion
                    len = tmp.length << 1;
                    tmp = Arrays.copyOf(tmp, tmp.length + len);
                }
            }
            return Arrays.copyOf(tmp, readedCount);
        } finally {
            if (isr != null) {
                isr.close();
            }
            is.close();
        }
    }
}
