package io.github.wycst.wast.common.beans;

import java.io.IOException;
import java.io.Writer;
import java.math.BigDecimal;

/**
 * <p> 字符数组实现
 * <p> jdk version <= 8 场景下String取value值或者直接通过char[]构建
 *
 * @Author wangyunchao
 * @Date 2022/8/27 15:31
 */
public class CharArraySource extends AbstractCharSource implements CharSource {

    // 字符数组
    private final char[] source;

    CharArraySource(char[] source) {
        super(0, source.length);
        this.source = source;
    }

    /**
     * 构建对象
     *
     * @param source
     * @return
     */
    public static CharArraySource of(char[] source) {
        return new CharArraySource(source);
    }

    @Override
    public char[] charArray() {
        return source;
    }

    @Override
    public byte[] byteArray() {
        throw new UnsupportedOperationException();
    }

    @Override
    public char charAt(int index) {
        return source[index];
    }

    @Override
    public String getString(int offset, int len) {
        return new String(source, offset, len);
    }

    @Override
    public void writeTo(Writer writer, int offset, int len) throws IOException {
        writer.write(source, offset, len);
    }

    @Override
    public void appendTo(StringBuffer stringBuffer, int offset, int len) {
        stringBuffer.append(source, offset, len);
    }

    @Override
    public void appendTo(StringBuilder stringBuilder, int offset, int len) {
        stringBuilder.append(source, offset, len);
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
        throw new UnsupportedOperationException();
    }

    @Override
    public String substring(int beginIndex, int endIndex) {
        throw new UnsupportedOperationException();
    }

    @Override
    public String input() {
        throw new UnsupportedOperationException();
    }

    @Override
    public String toString() {
        return new String(source, fromIndex(), length());
    }
}
