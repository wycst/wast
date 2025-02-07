package io.github.wycst.wast.json;

final class AsciiStringSource implements CharSource {

    private final String input;
    public AsciiStringSource(String input) {
        this.input = input;
    }

    /**
     * 构建对象
     *
     * @param input
     * @return
     */
    public static AsciiStringSource of(String input) {
        return new AsciiStringSource(input);
    }

    @Override
    public String input() {
        return input;
    }

    @Override
    public String substring(byte[] bytes, int beginIndex, int endIndex) {
        return JSONUnsafe.createAsciiString(bytes, beginIndex, endIndex - beginIndex);
    }

    @Override
    public void writeString(JSONCharArrayWriter writer, byte[] buf, int offset, int len) {
        writer.writeString(input, offset, len);
    }


}
