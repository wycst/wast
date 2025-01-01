package io.github.wycst.wast.common.beans;

/**
 * @Date 2024/12/12 23:07
 * @Created by wangyc
 */
public final class TimeCounter {

    private long nanoTime;

    public TimeCounter() {
        mark();
    }

    public void mark() {
        nanoTime = System.nanoTime();
    }

    public long intervalNanoTime() {
        return System.nanoTime() - nanoTime;
    }

    public long intervalMillis() {
        return (System.nanoTime() - nanoTime) / 1000000;
    }
}
