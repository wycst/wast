package io.github.wycst.wast.common.beans;

import io.github.wycst.wast.common.reflect.UnsafeHelper;

import java.io.IOException;
import java.io.Writer;
import java.math.BigDecimal;
import java.nio.ByteOrder;

/**
 * <p> 针对jdk9+字符串内置bytes数组coder=UTF16(1)时的字符转换读取
 * <p> 2字节一个字符,字符总长度为数组一半
 * <p> jdk9以下不要使用
 *
 * @Author: wangy
 * @Date: 2022/8/27 20:07
 * @Description:
 */
public final class UTF16ByteArraySource extends AbstractCharSource implements CharSource {

    private String input;
    private final byte[] bytes;
    private final char[] source;

    UTF16ByteArraySource(String input, char[] source, byte[] bytes) {
        super(0, source.length);
        this.input = input;
        this.bytes = bytes;
        this.source = source;
    }

    /**
     * 构建对象
     *
     * @param input
     * @return
     */
    public static UTF16ByteArraySource of(String input, char[] source) {
        byte coder = UnsafeHelper.getStringCoder(input);
        if (coder == 0) {
            throw new UnsupportedOperationException("only support UTF16 input");
        }
        byte[] bytes = (byte[]) UnsafeHelper.getStringValue(input);
        return new UTF16ByteArraySource(input, source, bytes);
    }

    @Override
    public char[] charArray() {
        return source;
    }

    @Override
    public byte[] byteArray() {
        return bytes;
    }

    @Override
    public char charAt(int index) {
        return source[index];
    }

    /**
     * 构建字符串，需要计算新字符串的coder值
     *
     * @param offset 字符索引位置，计算时需要移动一位
     * @param len    长度
     * @return
     */
    @Override
    public String getString(int offset, int len) {
        return input.substring(offset, offset + len);
    }

    @Override
    public void writeTo(Writer writer, int offset, int len) throws IOException {
        writer.write(input, offset, len);
    }

    @Override
    public void appendTo(StringBuffer stringBuffer, int offset, int len) {
        stringBuffer.append(input, offset, len);
    }

    @Override
    public void appendTo(StringBuilder stringBuilder, int offset, int len) {
        stringBuilder.append(input, offset, len);
    }

    @Override
    public void copy(int srcOff, char[] target, int tarOff, int len) {
        System.arraycopy(source, srcOff, target, tarOff, len);
    }

    @Override
    public BigDecimal ofBigDecimal(int fromIndex, int len) {
        return new BigDecimal(source, fromIndex, len);
    }

    @Override
    public int indexOf(char ch, int beginIndex) {
        return input.indexOf(ch, beginIndex);
    }

    @Override
    public String substring(int beginIndex, int endIndex) {
        return input.substring(beginIndex, endIndex);
    }

    @Override
    public String input() {
        return input;
    }

    @Override
    public String toString() {
        return input;
    }

    @Override
    public void setCharAt(int index, char c) {
        index <<= 1;
        bytes[index++] = (byte) (c >> HI_BYTE_SHIFT);
        bytes[index] = (byte) (c >> LO_BYTE_SHIFT);
    }

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

    static final boolean LE;
    static final int HI_BYTE_SHIFT;
    static final int LO_BYTE_SHIFT;

    static {
        // JDK9+ 可以直接使用地址判断
        LE = ByteOrder.nativeOrder() == ByteOrder.LITTLE_ENDIAN;
        if (LE) {
            HI_BYTE_SHIFT = 0;
            LO_BYTE_SHIFT = 8;
        } else {
            HI_BYTE_SHIFT = 8;
            LO_BYTE_SHIFT = 0;
        }
    }
}
