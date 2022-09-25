package io.github.wycst.wast.common.beans;

import io.github.wycst.wast.common.reflect.UnsafeHelper;

import java.io.IOException;
import java.io.Writer;
import java.math.BigDecimal;
import java.util.Arrays;

/**
 * <p> 针对jdk9+字符串内置bytes数组coder=LATIN1(0)时的字符转换读取
 *
 * @Author wangyunchao
 * @Date 2022/8/27 15:53
 */
public final class AsciiStringSource extends AbstractCharSource implements CharSource {

    private final String input;
    private final byte[] bytes;

    AsciiStringSource(String input, byte[] bytes) {
        super(0, bytes.length);
        this.bytes = bytes;
        this.input = input;
    }

    /**
     * 构建对象
     *
     * @param input
     * @return
     */
    public static AsciiStringSource of(String input) {
        byte coder = UnsafeHelper.getStringCoder(input);
        if (coder == 1) {
            throw new UnsupportedOperationException("only support LATIN1 input string.");
        }
        byte[] bytes = (byte[]) UnsafeHelper.getStringValue(input);
        return new AsciiStringSource(input, bytes);
    }

    @Override
    public char[] charArray() {
        throw new UnsupportedOperationException();
    }

    @Override
    public byte[] byteArray() {
        return bytes;
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
//        byte[] newBytes = new byte[len];
//        System.arraycopy(bytes, offset, newBytes, 0, len);
        return UnsafeHelper.getAsciiString(Arrays.copyOfRange(bytes, offset, offset + len));
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
        input.getChars(srcOff, srcOff + len, target, tarOff);
    }

    // todo 优化
    @Override
    public BigDecimal ofBigDecimal(int fromIndex, int len) {
        return new BigDecimal(getString(fromIndex, len));
    }

    @Override
    public int indexOf(char ch, int beginIndex) {
        return input.indexOf(ch, beginIndex);
    }

    @Override
    public String substring(int beginIndex, int endIndex) {
        return UnsafeHelper.getAsciiString(Arrays.copyOfRange(bytes, beginIndex, endIndex));
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
    public void setCharAt(int endIndex, char c) {
        bytes[endIndex] = (byte) c;
    }
}
