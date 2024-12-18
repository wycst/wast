package io.github.wycst.wast.json;

import io.github.wycst.wast.common.reflect.UnsafeHelper;
import io.github.wycst.wast.common.utils.EnvUtils;
class UTF8CharSource implements CharSource {

    final String input;
    final byte[] bytes;

    UTF8CharSource(String input, byte[] bytes) {
        this.input = input;
        this.bytes = bytes;
    }

    public static UTF8CharSource of(byte[] bytes) {
        String input = UnsafeHelper.getAsciiString(bytes);
        return new UTF8CharSource(input, bytes);
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
