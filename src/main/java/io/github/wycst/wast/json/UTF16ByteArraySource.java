package io.github.wycst.wast.json;

final class UTF16ByteArraySource implements CharSource {

    final String input;

    UTF16ByteArraySource(String input) {
        this.input = input;
    }

    public static UTF16ByteArraySource of(String input) {
        return new UTF16ByteArraySource(input);
    }

    @Override
    public String input() {
        return input;
    }

    @Override
    public String substring(byte[] bytes, int beginIndex, int endIndex) {
        return input.substring(beginIndex, endIndex);
    }

    @Override
    public void writeString(JSONCharArrayWriter writer, byte[] buf, int offset, int len) {
        writer.writeString(input, offset, len);
    }
}
