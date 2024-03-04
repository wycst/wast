package io.github.wycst.wast.common.utils;

import java.io.IOException;
import java.io.InputStream;

/**
 * byte工具类
 *
 * @Author: wangy
 * @Description:
 */
public final class ByteUtils {

    /**
     * 指定每个字节为c,返回指定length长度的byte数组
     *
     * @param c
     * @param length
     * @return
     */
    public static byte[] memset(int c, int length) {
        byte[] buffer = new byte[length];
        for (int i = 0; i < length; i++) {
            buffer[i] = (byte) c;
        }
        return buffer;
    }

    /**
     * 内存拷贝
     *
     * @param src
     * @param start
     * @param len
     * @return
     */
    public static byte[] memcpy(byte[] src, int start, int len) {
        byte[] buffer = new byte[len];
        System.arraycopy(src, start, buffer, 0, len);
        return buffer;
    }

    /**
     * 将bytes数组以单字节字符输出为字符串
     *
     * @param bytes
     * @return
     */
    public static String toString(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0, len = bytes.length; (i < len) && (bytes[i] != 0); i++) {
            sb.append((char) bytes[i]);
        }
        return sb.toString();
    }

    /**
     * 将字符串字符以单字节转化为字节数组
     *
     * @param str
     * @param size
     * @return
     */
    public static byte[] toBytes(String str, int size) {
        int len = str == null ? 0 : str.length();
        byte[] bytes = new byte[size];
        if (len > 0) {
            for (int i = 0; i < len; i++) {
                bytes[i] = ((byte) str.charAt(i));
            }
        }
        return bytes;
    }

    /***
     * 将int值写入buf数组(大端模式)
     *
     * @param buf
     * @param offset
     * @param value
     */
    public static int writeInt(byte[] buf, int offset, int value) {
        buf[offset++] = (byte) (value >> 24 & 0xff);
        buf[offset++] = (byte) (value >> 16 & 0xff);
        buf[offset++] = (byte) (value >> 8 & 0xff);
        buf[offset] = (byte) (value & 0xff);
        return 4;
    }

    /***
     * 将long值写入buf数组(大端模式)
     *
     * @param buf
     * @param offset
     * @param value
     */
    public static int writeLong(byte[] buf, int offset, long value) {
        buf[offset++] = (byte) (value >> 56 & 0xff);
        buf[offset++] = (byte) (value >> 48 & 0xff);
        buf[offset++] = (byte) (value >> 40 & 0xff);
        buf[offset++] = (byte) (value >> 32 & 0xff);
        buf[offset++] = (byte) (value >> 24 & 0xff);
        buf[offset++] = (byte) (value >> 16 & 0xff);
        buf[offset++] = (byte) (value >> 8 & 0xff);
        buf[offset] = (byte) (value & 0xff);
        return 8;
    }

    /***
     * 读取一个int值(大端模式)
     *
     * @param buf
     * @param offset
     */
    public static int readInt(byte[] buf, int offset) {
        int value = 0;
        value |= (buf[offset++] & 0xFF) << 24;
        value |= (buf[offset++] & 0xFF) << 16;
        value |= (buf[offset++] & 0xFF) << 8;
        value |= buf[offset] & 0xFF;
        return value;
    }

    /***
     * 读取一个long值(大端模式)
     *
     * @param buf
     * @param offset
     */
    public static long readLong(byte[] buf, int offset) {
        long value = 0;
        long mask = 0xFF;
        value |= (buf[offset++] & mask) << 56;
        value |= (buf[offset++] & mask) << 48;
        value |= (buf[offset++] & mask) << 40;
        value |= (buf[offset++] & mask) << 32;
        value |= (buf[offset++] & mask) << 24;
        value |= (buf[offset++] & mask) << 16;
        value |= (buf[offset++] & mask) << 8;
        value |= buf[offset] & mask;
        return value;
    }

    public static float readFloat(byte[] buf, int off) {
        int bits = readInt(buf, off);
        return Float.intBitsToFloat(bits);
    }

    public static double readDouble(byte[] buf, int off) {
        long bits = readLong(buf, off);
        return Double.longBitsToDouble(bits);
    }

    /**
     * 将字节数组以二进制序列输出
     *
     * @param b
     * @param splitChar
     * @return
     */
    public static String toBinaryString(byte[] b, char splitChar) {
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < b.length; i++) {
            String bits = Integer.toBinaryString(b[i] & 0xFF);
            int count = bits.length();
            while (count++ < 8) {
                builder.append('0');
            }
            builder.append(bits);
            if (splitChar > 0) {
                builder.append(splitChar);
            }
        }
        return builder.toString();
    }

    /**
     * 16进制字符串转二进制序列
     *
     * @param hexString
     * @return
     */
    public static String hexToBinaryString(String hexString) {
        StringBuilder builder = new StringBuilder();
        char[] chars = hexString.toCharArray();
        for (char ch : chars) {
            int numIndex = ch > '9' ? ch - 55 : ch - 48;
            if (numIndex < 0 || numIndex >= 16) {
                builder.append(ch);
                continue;
            }
            String bits = Integer.toBinaryString(numIndex);
            int count = bits.length();
            while (count++ < 4) {
                builder.append('0');
            }
            builder.append(bits);
        }
        return builder.toString();
    }

    /**
     * 将byte数组转化为16进制输出（每个字节转化为2位16进制）
     *
     * @param b
     * @return
     */
    public static String printHexString(byte[] b) {
        return printHexString(b, (char) 0);
    }

    /**
     * 将byte数组转为16进制字符串，每个byte转成2位16进制字符，长度 = b.length * 2
     *
     * @param b
     * @param splitChar
     * @return
     */
    public static String printHexString(byte[] b, char splitChar) {
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < b.length; i++) {
            String hex = Integer.toHexString(b[i] & 0xFF);
            if (hex.length() == 1) {
                builder.append('0');
            }
            builder.append(hex.toUpperCase());
            if (splitChar > 0) {
                builder.append(splitChar);
            }
        }
        return builder.toString();
    }

    /**
     * 将16进制的字符串还原为byte数组
     *
     * @param hexString
     * @return
     */
    public static byte[] hexString2Bytes(String hexString) {
        char[] chars = hexString.toCharArray();
        return hexString2Bytes(chars, 0, chars.length);
    }

    /**
     * 将16进制的字符数组还原为byte数组
     *
     * @param chars
     * @param offset
     * @param len
     * @return
     */
    public static byte[] hexString2Bytes(char[] chars, int offset, int len) {
        byte[] bytes = new byte[len / 2];
        int byteLength = 0;
        int b = -1;
        for (int i = offset, count = offset + len; i < count; i++) {
            char ch = Character.toUpperCase(chars[i]);
            int numIndex = ch > '9' ? ch - 55 : ch - 48;
            if (numIndex < 0 || numIndex >= 16) continue;
            if (b == -1) {
                b = numIndex << 4;
            } else {
                b += numIndex;
                bytes[byteLength++] = (byte) b;
                b = -1;
            }
        }
        if (byteLength == bytes.length) {
            return bytes;
        }
        return memcpy(bytes, 0, byteLength);
    }

    /**
     * 读取输入流中的数据返回字节数组
     *
     * @param is
     * @return
     * @throws IOException
     */
    public static byte[] readStreamBytes(InputStream is) throws IOException {
        return IOUtils.readBytes(is);
    }
}
