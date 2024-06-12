package io.github.wycst.wast.common.utils;

/**
 * @Date 2024/5/25 10:46
 * @Created by wangyc
 */
class Scientific {

    public long output;
    public int count;
    public final int e10;
    public boolean b;

    static final Scientific SCIENTIFIC_NULL = new Scientific(0, true);

    public Scientific(long output, int count, int e10) {
        this.output = output;
        this.count = count;
        this.e10 = e10;
    }

    public Scientific(int e10, boolean b) {
        this.e10 = e10;
        this.b = b;
    }
}
