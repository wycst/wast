package io.github.wycst.wast.json;

import io.github.wycst.wast.common.utils.EnvUtils;
final class UTF8CharSource implements CharSource {

    final String input;

    UTF8CharSource(String input) {
        this.input = input;
    }

    public static UTF8CharSource of(String input) {
        return new UTF8CharSource(input);
    }

    @Override
    public String input() {
        return input;
    }

    @Override
    public String substring(byte[] bytes, int beginIndex, int endIndex) {
        return new String(bytes, beginIndex, endIndex - beginIndex, EnvUtils.CHARSET_UTF_8);
    }
}
