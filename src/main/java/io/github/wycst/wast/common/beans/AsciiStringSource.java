package io.github.wycst.wast.common.beans;

import io.github.wycst.wast.common.reflect.UnsafeHelper;

import java.io.IOException;
import java.io.Writer;
import java.math.BigDecimal;

/**
 * <p> 针对jdk9+字符串内置bytes数组coder=LATIN1(0)时的字符转换读取
 *
 * @Author wangyunchao
 * @Date 2022/8/27 15:53
 */
class AsciiStringSource extends AbstractCharSource implements CharSource {

    private final byte[] bytes;
    private final String input;
    private char[] source;

    AsciiStringSource(String input, byte[] bytes, int from, int to) {
        super(from, to);
        this.bytes = bytes;
        this.input = input;
    }

    /**
     * 构建对象
     *
     * @param input
     * @param bytes
     * @return
     */
    public static AsciiStringSource ofTrim(String input, byte[] bytes) {
        int fromIndex = 0;
        int toIndex = bytes.length;
        while ((fromIndex < toIndex) && input.charAt(fromIndex) <= ' ') {
            fromIndex++;
        }
        while ((toIndex > fromIndex) && input.charAt(toIndex - 1) <= ' ') {
            toIndex--;
        }
        return new AsciiStringSource(input, bytes, fromIndex, toIndex);
    }

    @Override
    public char[] getSource() {
        if (source == null) {
            source = input.toCharArray();
        }
        return source;
    }

    @Override
    public char charAt(int index) {
        return (char) (bytes[index] & 0xff);
    }

    /**
     * <p> 场景：jdk9+
     * <p> 构建字符串时如果直接通过构造方法创建字符串会进行解码然后以byte[]存储于新的字符串中（解码后其实一样）；
     * <p> 通过构造空串，然后直接设置拷贝的byte[]效率最高；
     * <p> todo jdk18未验证
     *
     * @param offset
     * @param len
     * @return
     */
    @Override
    public String getString(int offset, int len) {
        byte[] newBytes = new byte[len];
        System.arraycopy(bytes, offset, newBytes, 0, len);
        return UnsafeHelper.getAsciiString(newBytes);
    }

    /**
     * 写入writer
     *
     * @param writer
     * @param offset
     * @param len
     * @throws IOException
     */
    @Override
    public void writeTo(Writer writer, int offset, int len) throws IOException {
//        writer.write(source, offset, len);
        writer.write(input, offset, len);
    }

    @Override
    public void appendTo(StringBuffer stringBuffer, int offset, int len) {
//        stringBuffer.append(source, offset, len);
        stringBuffer.append(input, offset, len);
    }

    @Override
    public void appendTo(StringBuilder stringBuilder, int offset, int len) {
//        stringBuilder.append(source, offset, len);
        stringBuilder.append(input, offset, len);
    }

    @Override
    public void copy(int srcOff, char[] target, int tarOff, int len) {
        input.getChars(srcOff, srcOff + len, target, tarOff);
//        System.arraycopy(_source, srcOff, target, tarOff, len);
    }

    // todo 优化
    @Override
    public BigDecimal ofBigDecimal(int fromIndex, int len) {
        return new BigDecimal(getString(fromIndex, len));
    }

    @Override
    public String toString() {
        if (fromIndex() == 0 && length() == bytes.length) {
            return UnsafeHelper.getAsciiString(bytes);
        }
        return new String(bytes, fromIndex(), length());
    }
}
