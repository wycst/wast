package io.github.wycst.wast.json;

import io.github.wycst.wast.common.reflect.UnsafeHelper;

final class AsciiStringSource implements CharSource {

    private final String input;
    private final byte[] bytes;

    public AsciiStringSource(String input, byte[] bytes) {
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
    public static AsciiStringSource of(String input, byte[] bytes) {
        return new AsciiStringSource(input, bytes);
    }

    /**
     * 构建对象
     *
     * @param input
     * @return
     */
    public static AsciiStringSource of(String input) {
        byte[] bytes = (byte[]) UnsafeHelper.getStringValue(input);
        return new AsciiStringSource(input, bytes);
    }

    /**
     * 构建对象
     *
     * @param bytes
     * @return
     */
    public static AsciiStringSource of(byte[] bytes) {
        String input = UnsafeHelper.getAsciiString(bytes);
        return new AsciiStringSource(input, bytes);
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
        return JSONUnsafe.createAsciiString(bytes, beginIndex, endIndex - beginIndex);
    }
}
