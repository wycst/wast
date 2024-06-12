package io.github.wycst.wast.common.utils;

/**
 * @Date 2024/6/8 14:22
 * @Created by wangyc
 */
public abstract class JdkApiAgent {

    public long multiplyHigh(long x, long y) {
        long x1 = x >> 32;
        long x2 = x & 0xFFFFFFFFL;
        long y1 = y >> 32;
        long y2 = y & 0xFFFFFFFFL;

        long z2 = x2 * y2;
        long t = x1 * y2 + (z2 >>> 32);
        long z1 = t & 0xFFFFFFFFL;
        long z0 = t >> 32;
        z1 += x2 * y1;
        return x1 * y1 + z0 + (z1 >> 32);
    }

    public long unsignedMultiplyHigh(long x, long y) {
        // Compute via multiplyHigh() to leverage the intrinsic
        long result = multiplyHigh(x, y);
        result += (y & (x >> 63));
        result += (x & (y >> 63));
        return result;
    }

    /**
     * Calculate the high bits of two long values. To be compatible with performance below JDK9, please ensure that x and y are both greater than 0
     *
     * @param x > 0
     * @param y > 0
     * @return
     */
    public long multiplyHighKaratsuba(long x, long y) {
        long x1 = x >>> 32, x2 = x & 0xffffffffL;
        long y1 = y >>> 32, y2 = y & 0xffffffffL;
        long A = x1 * y1;
        long B = x2 * y2;
        long C = (x1 + x2) * (y1 + y2);
        // karatsuba
        long K = C - A - B;
        long BC = B >>> 32;
        return ((BC + K) >>> 32) + A;
    }
}
