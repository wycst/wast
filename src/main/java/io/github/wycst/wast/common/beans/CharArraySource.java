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

    CharArraySource(char[] source, int from, int to) {
        super(from, to);
        this.source = source;
    }

    /**
     * 构建对象
     *
     * @param source
     * @return
     */
    public static CharArraySource of(char[] source) {
        return new CharArraySource(source, 0, source.length);
    }

    /**
     * 构建对象并trim
     *
     * @param source
     * @return
     */
    public static CharArraySource ofTrim(char[] source) {
        return ofTrim(source, 0, source.length);
    }

    /**
     * 构建对象并trim
     *
     * @param source
     * @param fromIndex 开始位置
     * @param toIndex   结束位置
     * @return
     */
    public static CharArraySource ofTrim(char[] source, int fromIndex, int toIndex) {
        while ((fromIndex < toIndex) && source[fromIndex] <= ' ') {
            fromIndex++;
        }
        while ((toIndex > fromIndex) && source[toIndex - 1] <= ' ') {
            toIndex--;
        }
        return new CharArraySource(source, fromIndex, toIndex);
    }

    @Override
    public char[] getSource() {
        return source;
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
    public String toString() {
        return new String(source, fromIndex(), length());
    }
}
