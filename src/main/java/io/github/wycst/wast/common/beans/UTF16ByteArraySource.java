package io.github.wycst.wast.common.beans;

import io.github.wycst.wast.common.reflect.UnsafeHelper;

/**
 * <p> 针对jdk9+字符串内置bytes数组coder=UTF16(1)时的字符转换读取
 * <p> 2字节一个字符,字符总长度为数组一半
 * <p> jdk9以下不要使用
 *
 * @Author: wangy
 * @Date: 2022/8/27 20:07
 * @Description:
 */
public final class UTF16ByteArraySource implements CharSource {

    private String input;
    private final byte[] bytes;
    // private final char[] source;

    UTF16ByteArraySource(String input, char[] source, byte[] bytes) {
        this.input = input;
        this.bytes = bytes;
        // this.source = source;
    }

    /**
     * 构建对象
     *
     * @param input
     * @return
     */
    public static UTF16ByteArraySource of(String input, char[] source, byte[] bytes) {
        return new UTF16ByteArraySource(input, source, bytes);
    }

    /**
     * 构建对象
     *
     * @param input
     * @return
     */
    public static UTF16ByteArraySource of(String input, char[] source) {
        byte[] bytes = (byte[]) UnsafeHelper.getStringValue(input);
        return new UTF16ByteArraySource(input, source, bytes);
    }

    @Override
    public byte[] byteArray() {
        return bytes;
    }

    @Override
    public String input() {
        return input;
    }

    @Override
    public int indexOf(int ch, int beginIndex) {
        return input.indexOf(ch, beginIndex);
    }

    @Override
    public String substring(int beginIndex, int endIndex) {
        return input.substring(beginIndex, endIndex);
    }

//    @Override
//    public void setCharAt(int index, char c) {
//        index <<= 1;
//        bytes[index++] = (byte) (c >> HI_BYTE_SHIFT);
//        bytes[index] = (byte) (c >> LO_BYTE_SHIFT);
//    }

    //    /**
//     * 获取UTF16编码直接数组在指定字符位置下的字符
//     *
//     * @param source
//     * @param index
//     * @return
//     */
//    private static char charAt(byte[] source, int index) {
//        index <<= 1;
//        return (char) ((source[index++] & 255) << HI_BYTE_SHIFT | (source[index] & 255) << LO_BYTE_SHIFT);
//    }

//    static final boolean LE;
//    static final int HI_BYTE_SHIFT;
//    static final int LO_BYTE_SHIFT;

//    static {
//        // JDK9+ 可以直接使用地址判断
//        LE = ByteOrder.nativeOrder() == ByteOrder.LITTLE_ENDIAN;
//        if (LE) {
//            HI_BYTE_SHIFT = 0;
//            LO_BYTE_SHIFT = 8;
//        } else {
//            HI_BYTE_SHIFT = 8;
//            LO_BYTE_SHIFT = 0;
//        }
//    }
}
