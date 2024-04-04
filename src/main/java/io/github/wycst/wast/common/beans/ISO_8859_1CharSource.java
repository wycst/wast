package io.github.wycst.wast.common.beans;

import io.github.wycst.wast.common.reflect.UnsafeHelper;
import io.github.wycst.wast.common.utils.EnvUtils;
/**
 * <p> create string source use iso_8859_1
 * <p> jdk9以下不要使用
 *
 * @Date 2024/3/28 20:34
 * @Created by wangyc
 */
public class ISO_8859_1CharSource implements CharSource {

    final String input;
    final byte[] bytes;

    ISO_8859_1CharSource(String input, byte[] bytes) {
        this.input = input;
        this.bytes = bytes;
    }

    /**
     * 构建对象
     *
     * @param bytes
     * @return
     */
    public static ISO_8859_1CharSource of(byte[] bytes) {
        String input = UnsafeHelper.getAsciiString(bytes);
        return new ISO_8859_1CharSource(input, bytes);
    }

    @Override
    public String input() {
        return input;
    }

    @Override
    public byte[] byteArray() {
        return bytes;
    }

    @Override
    public int indexOf(int ch, int beginIndex) {
        return input.indexOf(ch, beginIndex);
    }

    @Override
    public String substring(int beginIndex, int endIndex) {
        return new String(bytes, beginIndex, endIndex - beginIndex, EnvUtils.CHARSET_UTF_8);
    }
}
