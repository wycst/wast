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
    public byte[] byteArray() {
        throw new UnsupportedOperationException();
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
        return input.substring(beginIndex, endIndex);
    }
}
