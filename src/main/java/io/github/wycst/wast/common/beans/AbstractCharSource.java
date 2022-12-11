package io.github.wycst.wast.common.beans;

/**
 * @Author wangyunchao
 * @Date 2022/8/27 15:58
 */
abstract class AbstractCharSource implements CharSource {

    private final int from;
    private final int to;

    @Override
    public CharSequence subSequence(int start, int end) {
        throw new UnsupportedOperationException();
    }

    AbstractCharSource(int from, int to) {
        this.from = from;
        this.to = to;
    }

    @Override
    public char begin() {
        return charAt(from);
    }

    @Override
    public final int fromIndex() {
        return from;
    }

    @Override
    public final int toIndex() {
        return to;
    }

    @Override
    public int length() {
        return to - from;
    }

    @Override
    public void setCharAt(int endIndex, char c) {
        throw new UnsupportedOperationException();
    }
}
