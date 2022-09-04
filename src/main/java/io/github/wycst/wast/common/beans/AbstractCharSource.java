package io.github.wycst.wast.common.beans;

import io.github.wycst.wast.common.reflect.UnsafeHelper;

import java.util.Arrays;

/**
 * @Author wangyunchao
 * @Date 2022/8/27 15:58
 */
public abstract class AbstractCharSource implements CharSource {

    private final int from;
    private final int to;

    @Override
    public CharSequence subSequence(int start, int end) {
        throw new UnsupportedOperationException();
    }

    AbstractCharSource(int from, int to) {
        this.from = from;
        this.to = to;
    }

    @Override
    public char begin() {
        return charAt(from);
    }

    @Override
    public final int fromIndex() {
        return from;
    }

    @Override
    public final int toIndex() {
        return to;
    }

    @Override
    public int length() {
        return to - from;
    }

    private static final boolean StringCoder;

    static {
        long stringCoderOffset = UnsafeHelper.getStringCoderOffset();
        StringCoder = stringCoderOffset > -1;
    }

    /**
     * 通过字符串构建
     *
     * @param input
     * @return
     */
    public static CharSource of(String input) {
        Object stringValue = UnsafeHelper.getStringValue(input);
        if (StringCoder) {
            byte[] bytes = (byte[]) stringValue;
            if (input.length() == bytes.length) {
                return AsciiStringSource.ofTrim(input, bytes);
            }
//            return UTF16ByteArraySource.ofTrim(input, bytes);
            return CharArraySource.ofTrim(input.toCharArray());
        } else {
            return CharArraySource.ofTrim((char[]) stringValue);
        }
    }

    /**
     * 字符数组构建源
     *
     * @param source
     * @return
     */
    public static CharSource of(char[] source) {
        return CharArraySource.ofTrim(source);
    }

    /**
     * 字符数组构建源
     *
     * @param source
     * @param fromIndex
     * @param toIndex
     * @return
     */
    public static CharSource of(char[] source, int fromIndex, int toIndex) {
        return CharArraySource.ofTrim(source, fromIndex, toIndex);
    }


    /**
     * 通过utf-8字节数组构建源,转化为字符数组源
     *
     * @param source utf-8字节数组
     * @return
     */
    public static CharSource of(byte[] source) {
        if (StringCoder) {
            return of(new String(source));
        }
        char[] chars = readUTF8Bytes(source);
        return of(chars);
    }

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
     * 一个字节 0000 0000-0000 007F | 0xxxxxxx
     * 二个字节 0000 0080-0000 07FF | 110xxxxx 10xxxxxx
     * 三个字节 0000 0800-0000 FFFF | 1110xxxx 10xxxxxx 10xxxxxx
     * 四个字节 0001 0000-0010 FFFF | 11110xxx 10xxxxxx 10xxxxxx 10xxxxxx
     *
     * @param bytes
     * @param chars 目标字符
     * @return 字符数组长度
     */
    public final static int readUTF8Bytes(byte[] bytes, char[] chars) {
        if (bytes == null) return 0;
        int len = bytes.length;
        int charLen = 0;

        for (int j = 0; j < len; ++j) {
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
                    if (j < len - 3) {
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
                    if (j < len - 2) {
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
                    if (j < len - 1) {
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
