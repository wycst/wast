package io.github.wycst.wast.common.utils;

import java.io.IOException;
import java.io.InputStream;

/**
 * byte工具类
 *
 * @Author: wangy
 * @Description:
 */
public class ByteUtils {

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

    /**
     * 比较两个bytes数组在指定的长度内是否一致
     *
     * @param a
     * @param b
     * @param count
     * @return
     */
    public static boolean differByte(byte[] a, byte[] b, int count) {
        boolean flag = false;
        int length = a.length < b.length ? a.length : b.length;
        if (count <= length) {
            for (int i = 0; i < count; i++) {
                if (a[i] != b[i]) {
                    flag = true;
                    break;
                }
            }
        }
        return flag;
    }

    /**
     * 获取int值（32位）占位4个字节的数组
     *
     * @param num
     * @return
     */
    public static byte[] int2bytes(int num) {
        byte[] b = new byte[4];
        for (int i = 0; i < 4; i++) {
            b[i] = (byte) (num >>> 24 - i * 8);
        }
        return b;
    }

    /**
     * 获取int值（32位）占位4个字节的数组并反转
     *
     * @param num
     * @return
     */
    public static byte[] int2bytesReverse(int num) {
        byte[] b = new byte[4];
        for (int i = 0; i < 4; i++) {
            b[3 - i] = (byte) (num >>> 24 - i * 8);
        }
        return b;
    }

    /**
     * 将字节数组转化为int数，
     * <p> 约定 b.length == 4
     *
     * @param b
     * @return
     */
    public static int bytes2int(byte[] b) {
        int s = 0;
        for (int i = 0; i < b.length; i++) {
            s |= (b[i] & 0xFF) << (b.length - i - 1) * 8;
        }
        return s;
    }

    /**
     * 将字节数组反转后转化为int数
     * <p> 约定 b.length == 4
     *
     * @param b
     * @return
     */
    public static int bytes2intReverse(byte[] b) {
        byte[] tmp = new byte[4];
        for (int i = 0; i < 4; i++) {
            tmp[i] = b[(3 - i)];
        }
        return bytes2int(tmp);
    }

//    public static int checkSum(byte[] arr) {
//        byte[] tmp = new byte[4];
//        int sum = 0;
//        for (int i = 0; i < arr.length; i += 4) {
//            System.arraycopy(arr, i, tmp, 0, 4);
//            int t = bytes2intReverse(tmp);
//            sum ^= t;
//        }
//        return sum;
//    }
//
//    public static int checkSum(byte[] arr, int len) {
//        byte[] tmp = new byte[len];
//        System.arraycopy(arr, 0, tmp, 0, len);
//        return checkSum(tmp);
//    }
//
//    public static String getHex(int num) {
//        char[] digits = {'0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'A', 'B', 'C', 'D', 'E', 'F'};
//        int length = 32;
//        StringBuffer sb = new StringBuffer();
//        char[] result = new char[length];
//        String tmp = "0x0";
//        do {
//            result[(--length)] = digits[(num & 0xF)];
//            num >>>= 4;
//        } while (num != 0);
//        for (int i = length; i < result.length; i++) {
//            sb.append(result[i]);
//        }
//        tmp = tmp + new String(sb);
//        return tmp;
//    }

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

//    public static int readStreamBytes(InputStream is, byte[] buffers) throws IOException {
//        int b = -1;
//        int length = 0;
//        while ((b = is.read()) > -1) {
//            buffers[length++] = (byte) b;
//        }
//        is.close();
//        return length;
//    }

//    private static byte[] readStreamBytes(InputStream is, boolean closed) throws IOException {
//        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
//        int b;
//        while ((b = is.read()) > -1) {
//            byteArrayOutputStream.write(b);
//        }
//        if (closed) {
//            is.close();
//        }
//        byteArrayOutputStream.close();
//        return byteArrayOutputStream.toByteArray();
//    }

}
