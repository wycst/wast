package io.github.wycst.wast.common.utils;

import java.io.IOException;
import java.math.BigInteger;
import java.util.Arrays;

/**
 * @Author: wangy
 * @Date: 2022/7/17 16:16
 * @Description:
 */
public final class NumberUtils {

    final static double[] POSITIVE_DECIMAL_POWER = new double[325];
    final static double[] NEGATIVE_DECIMAL_POWER = new double[325];
    final static long[] POW10_LONG_VALUES = new long[]{
            10, 100, 1000, 10000, 100000, 1000000, 10000000, 100000000, 1000000000, 10000000000L, 100000000000L, 1000000000000L, 10000000000000L, 100000000000000L, 1000000000000000L, 10000000000000000L, 100000000000000000L, 1000000000000000000L, 9223372036854775807L
    };

//    final static long[] POW10_OPPOSITE_VALUES = new long[]{
//            0x6666666666666667L, 0x51eb851eb851eb86L, 0x4189374bc6a7ef9eL, 0x68db8bac710cb296L, 0x53e2d6238da3c212L, 0x431bde82d7b634dbL, 0x6b5fca6af2bd215fL, 0x55e63b88c230e77fL, 0x44b82fa09b5a52ccL, 0x6df37f675ef6eae0L, 0x57f5ff85e5925580L, 0x465e6604b7a84466L, 0x709709a125da070aL, 0x5a126e1a84ae6c08L, 0x480ebe7b9d58566dL, 0x734aca5f6226f0aeL, 0x5c3bd5191b525a25L, 0x49c97747490eae84L, 0x4000000000000001L
//    };
//
//    final static byte[] POW10_OPPOSITE_RB = new byte[]{
//            2, 5, 8, 12, 15, 18, 22, 25, 28, 32, 35, 38, 42, 45, 48, 52, 55, 58, 61
//    };

    //0-9a-f
    final static char[] HEX_DIGITS = {'0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'a', 'b', 'c', 'd', 'e', 'f'};

    final static byte[] HEX_DIGITS_REVERSE = {0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 0, 0, 0, 0, 0, 0, 0, 10, 11, 12, 13, 14, 15, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 10, 11, 12, 13, 14, 15};

    final static char[] DigitOnes = {
            '0', '1', '2', '3', '4', '5', '6', '7', '8', '9',
            '0', '1', '2', '3', '4', '5', '6', '7', '8', '9',
            '0', '1', '2', '3', '4', '5', '6', '7', '8', '9',
            '0', '1', '2', '3', '4', '5', '6', '7', '8', '9',
            '0', '1', '2', '3', '4', '5', '6', '7', '8', '9',
            '0', '1', '2', '3', '4', '5', '6', '7', '8', '9',
            '0', '1', '2', '3', '4', '5', '6', '7', '8', '9',
            '0', '1', '2', '3', '4', '5', '6', '7', '8', '9',
            '0', '1', '2', '3', '4', '5', '6', '7', '8', '9',
            '0', '1', '2', '3', '4', '5', '6', '7', '8', '9',
    };

    final static char[] DigitTens = {
            '0', '0', '0', '0', '0', '0', '0', '0', '0', '0',
            '1', '1', '1', '1', '1', '1', '1', '1', '1', '1',
            '2', '2', '2', '2', '2', '2', '2', '2', '2', '2',
            '3', '3', '3', '3', '3', '3', '3', '3', '3', '3',
            '4', '4', '4', '4', '4', '4', '4', '4', '4', '4',
            '5', '5', '5', '5', '5', '5', '5', '5', '5', '5',
            '6', '6', '6', '6', '6', '6', '6', '6', '6', '6',
            '7', '7', '7', '7', '7', '7', '7', '7', '7', '7',
            '8', '8', '8', '8', '8', '8', '8', '8', '8', '8',
            '9', '9', '9', '9', '9', '9', '9', '9', '9', '9',
    };

    final static long[] POW5_LONG_VALUES = new long[27];
    final static BigInteger[] POW5_BI_VALUES = new BigInteger[343];

    static {
        // e0 ~ e360(e306)
        for (int i = 0, len = POSITIVE_DECIMAL_POWER.length; i < len; ++i) {
            POSITIVE_DECIMAL_POWER[i] = Double.valueOf("1.0E" + i);
            NEGATIVE_DECIMAL_POWER[i] = Double.valueOf("1.0E-" + i);
        }
        // 4.9e-324
        NEGATIVE_DECIMAL_POWER[NEGATIVE_DECIMAL_POWER.length - 1] = Double.MIN_VALUE;

        long val = 1;
        for (int i = 0; i < POW5_LONG_VALUES.length; ++i) {
            POW5_LONG_VALUES[i] = val;
            val *= 5;
        }

        BigInteger five = BigInteger.valueOf(5);
        POW5_BI_VALUES[0] = BigInteger.ONE;
        for (int i = 1; i < POW5_BI_VALUES.length; ++i) {
            BigInteger pow5Value = five.pow(i);
            POW5_BI_VALUES[i] = pow5Value;
        }
    }

    static final int MOD_DOUBLE_EXP = (1 << 12) - 1;
    static final int MOD_FLOAT_EXP = (1 << 9) - 1;
    static final long MOD_DOUBLE_MANTISSA = (1L << 52) - 1;
    static final int MOD_FLOAT_MANTISSA = (1 << 23) - 1;
    static final long MASK_32_BITS = 0xffffffffL;
    public static char[] copyDigitOnes() {
        return Arrays.copyOf(DigitOnes, DigitOnes.length);
    }

    public static char[] copyDigitTens() {
        return Arrays.copyOf(DigitTens, DigitTens.length);
    }

    /**
     * 获取以10为底数的指定数值（-1 < expValue > 310）指数值,
     *
     * @param expValue ensure expValue >= 0
     * @return
     */
    public static double getDecimalPowerValue(int expValue) {
        if (expValue < POSITIVE_DECIMAL_POWER.length) {
            return POSITIVE_DECIMAL_POWER[expValue];
        }
        return Math.pow(10, expValue);
    }

    /**
     * 获取以10为底数的指定数值（-1 < expValue > 310）指数值,
     *
     * @param expValue ensure expValue < 0
     * @return
     */
    public static double getNegativeDecimalPowerValue(int expValue) {
        int index = -expValue;
        if (index < NEGATIVE_DECIMAL_POWER.length) {
            return NEGATIVE_DECIMAL_POWER[index];
        }
        return Math.pow(10, expValue);
    }

    /**
     * 获取long值的字符串长度
     *
     * @param value
     * @return
     */
    public static int stringSize(long value) {
        int i = 1;
        if (value < 0) {
            ++i;
            value = -value;
        }
        for (long val : POW10_LONG_VALUES) {
            if (value < val) {
                return i;
            }
            ++i;
        }
        return 19;
    }

    /**
     * 十进制字符转数字
     *
     * @param ch 字符值（非数字）
     * @return
     */
    public static int digitDecimal(int ch) {
        switch (ch) {
            case '0':
            case '1':
            case '2':
            case '3':
            case '4':
            case '5':
            case '6':
            case '7':
            case '8':
            case '9': {
                return ch - '0';
            }
            default: {
                return -1;
            }
        }
    }

    /**
     * 判断一个字符或者字节是否为数字(48-57)
     *
     * @param c
     * @return
     */
    public static boolean isDigit(int c) {
        switch (c) {
            case '0':
            case '1':
            case '2':
            case '3':
            case '4':
            case '5':
            case '6':
            case '7':
            case '8':
            case '9':
                return true;
            default:
                return false;
        }
    }

    /**
     * 转化为4位十进制数int数字
     *
     * @param buf
     * @param fromIndex
     * @return
     */
    public static int parseInt4(char[] buf, int fromIndex)
            throws NumberFormatException {
        return parseInt4(buf[fromIndex++], buf[fromIndex++], buf[fromIndex++], buf[fromIndex]);
    }

    /**
     * 转化为4位十进制数int数字
     *
     * @param buf
     * @param fromIndex
     * @return
     */
    public static int parseInt4(byte[] buf, int fromIndex)
            throws NumberFormatException {
        return parseInt4(buf[fromIndex++], buf[fromIndex++], buf[fromIndex++], buf[fromIndex]);
    }

    /**
     * 转化为4位十进制数
     *
     * @param c1 千位
     * @param c2 百位
     * @param c3 十位
     * @param c4 个位
     * @return
     */
    public static int parseInt4(int c1, int c2, int c3, int c4) {
        int v1 = digitDecimal(c1);
        int v2 = digitDecimal(c2);
        int v3 = digitDecimal(c3);
        int v4 = digitDecimal(c4);
        if ((v1 | v2 | v3 | v4) == -1) {
            throw new NumberFormatException("For input string: \"" + new String(new char[]{(char) c1, (char) c2, (char) c3, (char) c4}) + "\"");
        }
        return v1 * 1000 + v2 * 100 + v3 * 10 + v4;
    }

    /**
     * 转化为2位十进制数
     *
     * @param buf
     * @param fromIndex
     * @return
     */
    public static int parseInt2(char[] buf, int fromIndex)
            throws NumberFormatException {
        return parseInt2(buf[fromIndex++], buf[fromIndex]);
    }

    /**
     * 转化为2位十进制数
     *
     * @param buf
     * @param fromIndex
     * @return
     */
    public static int parseInt2(byte[] buf, int fromIndex)
            throws NumberFormatException {
        return parseInt2(buf[fromIndex++], buf[fromIndex]);
    }

    /**
     * 转化为2位十进制数
     *
     * @param c1
     * @param c2
     * @return
     * @throws NumberFormatException
     */
    public static int parseInt2(int c1, int c2)
            throws NumberFormatException {
        int v1 = digitDecimal(c1);
        int v2 = digitDecimal(c2);
        if ((v1 | v2) == -1) {
            throw new NumberFormatException("For input string: \"" + new String(new char[]{(char) c1, (char) c2}) + "\"");
        }
        return v1 * 10 + v2;
    }


    /**
     * 转化为1位int数字
     *
     * @param buf
     * @param fromIndex
     * @return
     */
    public static int parseInt1(char[] buf, int fromIndex)
            throws NumberFormatException {
        int v1 = digitDecimal(buf[fromIndex]);
        if (v1 == -1) {
            throw new NumberFormatException("For input string: \"" + new String(buf, fromIndex, 1) + "\"");
        }
        return v1;
    }

    /**
     * 转化为1位int数字
     *
     * @param buf
     * @param fromIndex
     * @return
     */
    public static int parseInt1(byte[] buf, int fromIndex)
            throws NumberFormatException {
        int v1 = digitDecimal(buf[fromIndex]);
        if (v1 == -1) {
            throw new NumberFormatException("For input string: \"" + new String(buf, fromIndex, 1) + "\"");
        }
        return v1;
    }

    /**
     * 转化为1位int数字
     *
     * @param ch
     * @return
     */
    public static int parseInt1(int ch)
            throws NumberFormatException {
        int v1 = digitDecimal(ch);
        if (v1 == -1) {
            throw new NumberFormatException("For input string: \"" + ch + "\"");
        }
        return v1;
    }

    /**
     * 将long类型的value转为长度为16的16进制字符串,缺省补字符0
     *
     * @param value
     * @return
     * @see Long#toHexString(long)
     */
    public static String toHexString16(long value) {
        char[] chars = new char[16];
        for (int i = 15; i > -1; --i) {
            int val = (int) (value & 0xf);
            chars[i] = HEX_DIGITS[val];
            value >>= 4L;
        }
        return new String(chars);
    }

    /**
     * Convert to double through 64 bit integer val and precision scale
     *
     * <p> IEEEF Double 64 bits: 1 + 11 + 52  </p>
     * <p> ensure the parameter val is greater than 0, otherwise return 0 </p>
     *
     * @param val
     * @param scale
     * @return
     * @author wycst
     */
    public static double scientificToIEEEDouble(long val, int scale) {
        if (val <= 0) {
            if (val == Long.MIN_VALUE) {
                val = 9223372036854775807L;
            } else {
                return 0.0D;
            }
        }
        int leadingZeros = Long.numberOfLeadingZeros(val);
        double dv = val;
        if (scale <= 0) {
            if (scale == 0) return dv;
            int e10 = -scale;
            if (e10 > 308) return Double.POSITIVE_INFINITY;
            if ((long) dv == val && e10 < 23) {
                return dv * POSITIVE_DECIMAL_POWER[e10];
            }
            BigInteger multiplier = POW5_BI_VALUES[e10];
            ED5 ed5 = ED5.ED5_A[e10];
            long left = val << (leadingZeros - 1);
            long h = EnvUtils.JDK_AGENT_INSTANCE.multiplyHighKaratsuba(left, ed5.y);
            int sr, mask, mmask;
            if (h >= 1L << 61) {
                sr = 9;
                mask = 0xFF;
                mmask = 0x1FF;
            } else {
                sr = 8;
                mask = 0x7F;
                mmask = 0xFF;
            }
            if (e10 < POW5_LONG_VALUES.length) {
                // accurate mode
                long mantissa0 = h >>> sr;
                long e2 = e10 - leadingZeros + multiplier.bitLength() + sr + 1077;
                long mod = h & mmask;
                boolean plus = mod > mask + 1 || (mod == mask + 1 && ((mantissa0 & 1) == 1 || left * ed5.y != 0));
                if (plus) {
                    ++mantissa0;
                    if (mantissa0 == 1L << 53) {
                        mantissa0 = 1L << 52;
                        ++e2;
                    }
                }
                long bits = (e2 << 52) | (mantissa0 & MOD_DOUBLE_MANTISSA);
                return Double.longBitsToDouble(bits);
            }
            // todo 先修复bug后续研究
            if ((h & mask) != mask /*|| ((h >> sr - 1) & 1) == 0*/) {
                return longBitsToIntegerDouble(h, e10 - leadingZeros + ed5.dfb + sr + 1140, sr);
            }
            long l = left * ed5.y;
            if (checkLowCarry(l, left, ed5.f)) {
                // tail h like 10000000
                return longBitsToIntegerDouble(h + 1, e10 - leadingZeros + ed5.dfb + sr + 1140, sr);
            } else if (!checkLowCarry(l, left, ed5.f + 1)) {
                // tail h like 01111111
                return longBitsToIntegerDouble(h, e10 - leadingZeros + ed5.dfb + sr + 1140, sr);
            } else {
                // This is a scenario that is extremely rare or unlikely to occur, although the low bit is only 32 bits.
                // If it occurs, use the difference method for carry detection
                int e52 = e10 - leadingZeros + ed5.dfb + sr + 65;
                if (e52 >= 972) return Double.POSITIVE_INFINITY;
                long mantissa0 = h >>> sr;
                long rightSum = (mantissa0 << 1) + 1;
                int sb = 1 - e52 + e10; // sb < 0
                long diff = BigInteger.valueOf(val).multiply(multiplier).compareTo(BigInteger.valueOf(rightSum).shiftLeft(-sb));
                if (diff > 0 || (diff == 0 && (mantissa0 & 1) == 1)) {
                    ++mantissa0;
                    if (mantissa0 == 1l << 53) {
                        mantissa0 >>= 1;
                        ++e52;
                    }
                }
                long e2 = e52 + 1075;
                long bits = (e2 << 52) | (mantissa0 & MOD_DOUBLE_MANTISSA);
                return Double.longBitsToDouble(bits);
            }
        } else {
            if (scale > 342) return 0.0D;
            if ((long) dv == val && scale < 23) {
                return dv / POSITIVE_DECIMAL_POWER[scale];
            }
            ED5 ed5 = ED5.ED5_A[scale];
            long left = val << (leadingZeros - 1);
            // h is 61~62 bits
            long h = EnvUtils.JDK_AGENT_INSTANCE.multiplyHighKaratsuba(left, ed5.oy);
            int sr, mask;
            if (h >= 1L << 61) {
                sr = 9;
                mask = 0xFF;
            } else {
                sr = 8;
                mask = 0x7F;
            }
            // todo 先修复bug后续研究
            if ((h & mask) != mask /*|| ((h >> sr - 1) & 1) == 0*/) {
                return longBitsToDecimalDouble(h, 33 - scale - ed5.ob - leadingZeros + sr, sr);
            }
            long l = left * ed5.oy;
            int e52 = 33 - scale - ed5.ob - leadingZeros + sr;
            if (checkLowCarry(l, left, ed5.of)) {
                // tail h like 10000000
                return longBitsToDecimalDouble(h + 1, e52, sr);
            } else if (!checkLowCarry(l, left, ed5.of + 1)) {
                // tail h like 01111111
                return longBitsToDecimalDouble(h, e52, sr);
            } else if (scale < POW5_LONG_VALUES.length) {
                // if reach here, there is a high probability that val can be evenly divided by p5sv
                long p5sv = POW5_LONG_VALUES[scale];
                long mantissa0 = h >>> sr;
                long e2 = e52 + 1075;
                long bits = (e2 << 52) | (mantissa0 & MOD_DOUBLE_MANTISSA);
                double dv0 = Double.longBitsToDouble(bits);
                int sb = 1 - scale - e52;
                // difference comparison method: diff = dv - dv0 >= 1/2 * 2^e52  -> val * 2^sb - mantissa0 * 2 * p5sv >= p5sv
                long diff = sb > 0 ? (val << sb) - (mantissa0 << 1) * p5sv - p5sv : val - (mantissa0 << 1 - sb) * p5sv - (p5sv << -sb);
                if (diff > 0 || (diff == 0 && (mantissa0 & 1) == 1)) {
                    return Double.longBitsToDouble(bits + 1);
                }
                return dv0;
            }
            // This is a scenario that is extremely rare or unlikely to occur, although the low bit is only 32 bits.
            // If it occurs, use the difference method for carry detection
            BigInteger divisor = POW5_BI_VALUES[scale];
            long e2, mantissa0 = h >>> sr;
            boolean oddFlag = (mantissa0 & 1) == 1;
            if (e52 < -1074) {
                sr += -1074 - e52;
                e2 = 0;
                if (sr >= 62) {
                    return 0.0D;
                }
            } else {
                e2 = e52 + 1075;
            }
            long rightSum = (mantissa0 << 1) + 1;
            int sb = 1 - e52 - scale;
            long diff = BigInteger.valueOf(val).shiftLeft(sb).compareTo(divisor.multiply(BigInteger.valueOf(rightSum)));
            if (diff > 0 || (diff == 0 && oddFlag)) {
                ++mantissa0;
                if (mantissa0 == 1l << 53) {
                    mantissa0 >>= 1;
                    ++e2;
                }
            }
            long bits = (e2 << 52) | (mantissa0 & MOD_DOUBLE_MANTISSA);
            return Double.longBitsToDouble(bits);
        }
    }

    private static boolean checkLowCarry(long l, long x, long y32) {
        return l < 0 && l + ((EnvUtils.JDK_AGENT_INSTANCE.multiplyHighKaratsuba(x, y32) << 32) + ((x * y32) >>> 32)) >= 0;
        //        long h1 = EnvUtils.JDK_AGENT_INSTANCE.multiplyHighKaratsuba(x, y32), l1 = x * y32, carry = (h1 << 32) + (l1 >>> 32);
        //        long l2 = l + carry;
        //        return (l | carry) < 0 && ((l & carry) < 0 || l2 >= 0);
    }

    static double longBitsToDecimalDouble(long l62, int e52, int sr) {
        long e2, mantissa0;
        if (e52 < -1074) {
            sr += -1074 - e52;
            e2 = 0;
            if (sr >= 62) {
                return 0.0D;
            }
            mantissa0 = ((l62 >> sr - 1) + 1) >> 1;
        } else {
            e2 = e52 + 1075;
            mantissa0 = ((l62 >>> sr - 1) + 1) >> 1;
            if (mantissa0 == 1L << 53) {
                mantissa0 >>= 1;
                ++e2;
            }
        }
        long bits = (e2 << 52) | (mantissa0 & MOD_DOUBLE_MANTISSA);
        return Double.longBitsToDouble(bits);
    }

    static double longBitsToIntegerDouble(long l62, long e2, int sr) {
        if (e2 >= 2047) return Double.POSITIVE_INFINITY;
        long mantissa0 = ((l62 >>> sr - 1) + 1) >> 1;
        if (mantissa0 == 1L << 53) {
            mantissa0 = 1L << 52;
            ++e2;
        }
        long bits = (e2 << 52) | (mantissa0 & MOD_DOUBLE_MANTISSA);
        return Double.longBitsToDouble(bits);
    }

    /**
     * Convert to float through 64 bit integer val and precision scale
     *
     * <p> IEEEF Float 32 bits: 1 + 8 + 23  </p>
     * <p> cleverly convert using the double bit structure </p>
     * <p> IEEEF Double:  m1 * 2^n1 </p>
     * <p> IEEEF Float :  (m1 >> 29) * 2^(n1 - 1023 + 127) </p>
     * <p> ensure the parameter val is greater than 0, otherwise return 0 </p>
     * <pre>
     *    long doubleBits = Double.doubleToLongBits(dv);
     *    long mantissa0 = (doubleBits & MOD_DOUBLE_MANTISSA);
     *    int doubleE2 = (int) (doubleBits >> 52) & MOD_DOUBLE_EXP;
     *
     *    int floatE2 = doubleE2 - 1023 + 127;
     *    long floatMantissa = mantissa0 >> 29;
     *    int floatBits = (int) ((floatE2 << 23) | floatMantissa);
     *    float result = Float.intBitsToFloat(floatBits);
     * </pre>
     *
     * @param val
     * @param scale
     * @return
     * @author wycst
     */
    public static float scientificToIEEEFloat(long val, int scale) {
        if (val <= 0) {
            return 0.0f;
        }
        double dv;
        float fv = val;
        float val0;
        if (scale <= 0) {
            if (scale == 0) return fv;
            int e10 = -scale;
            if (e10 > 38) return Float.POSITIVE_INFINITY;
            dv = val * NumberUtils.getDecimalPowerValue(e10);
            val0 = (float) dv;
        } else {
            if (scale > 63) return 0.0F;
            dv = val / NumberUtils.getDecimalPowerValue(scale);
            val0 = (float) dv;
        }
        return val0;
    }

//    /**
//     * Ensure x is greater than 0
//     *
//     * @param x
//     * @param index(0,17)
//     * @return
//     */
//    static long divPowTen(long x, int index) {
//        // Use Karatsuba technique with two base 2^32 digits.
//        long y = POW10_OPPOSITE_VALUES[index];
////        long x1 = x >>> 32;
////        long y1 = y >>> 32;
////        long x2 = x & 0xFFFFFFFFL;
////        long y2 = y & 0xFFFFFFFFL;
////        long A = x1 * y1;
////        long B = x2 * y2;
////        long C = (x1 + x2) * (y1 + y2);
////        long K = C - A - B;
////        long H = (((B >>> 32) + K) >>> 32) + A;
//        long H = EnvUtils.JDK_AGENT_INSTANCE.multiplyHighKaratsuba(x, y);
//        return H >> POW10_OPPOSITE_RB[index];
//    }

    /**
     * multiplyOutput
     *
     * @param x     > 0
     * @param y     > 0
     * @param shift > 0
     * @return
     */
    static long multiplyHighAndShift(long x, long y, int shift) {
        long H = EnvUtils.JDK_AGENT_INSTANCE.multiplyHighKaratsuba(x, y);
        if (shift >= 64) {
            int sr = shift - 64;
            return H >>> sr;
        }
        long L = x * y;
        return H << (64 - shift) | (L >>> shift);
    }

    /**
     * Unsigned algorithm:  (H * 2^64 * 2^n + L * 2^n + H1 * 2^64 + L1) / 2^(n+s) = (H * 2^64 + L + (H1 * 2^64 + L1) / 2^n) / 2^s
     * <p>
     * use BigInteger:  BigInteger.valueOf(pd.y).shiftLeft(n).add(BigInteger.valueOf(f & MASK_32_BITS)).multiply(BigInteger.valueOf(x)).shiftRight(s + n).longValue()
     *
     * @param x   63bits
     * @param y   63bits
     * @param y32 unsigned int32 f(32bits)
     * @param s   s > 0
     * @return
     */
    static long multiplyHighAndShift(long x, long y, long y32, int s) {
        // -> BigInteger.valueOf(y).shiftLeft(32).add(BigInteger.valueOf(y32 & MASK_32_BITS)).multiply(BigInteger.valueOf(x)).shiftRight(s + 32)
        int sr = s - 64;
        long H = EnvUtils.JDK_AGENT_INSTANCE.multiplyHighKaratsuba(x, y);
        long L = x * y;

        // cal x * f -> H1, L1
        long H1 = EnvUtils.JDK_AGENT_INSTANCE.multiplyHighKaratsuba(x, y32);
        long L1 = x * y32;

        // carry = (H1 * 2^64 + L1) / 2^n
        long carry = (H1 << 32) + (L1 >>> 32);
        long L2 = L + carry;
        if ((L | carry) < 0 && ((L & carry) < 0 || L2 >= 0)) {
            ++H;
        }
        L = L2;
        if (sr >= 0) {
            return H >>> sr;
        }
        return H << -sr | (L >>> s);
    }

    /**
     * Conversion of ieee floating point numbers to Scientific notation
     *
     * <p> Using the difference estimation method </p>
     * <p> The output may not be the shortest, but the general result is correct </p>
     *
     * @param doubleValue > 0
     * @return
     */
    public static Scientific doubleToScientific(double doubleValue) {
        if(doubleValue == Double.MIN_VALUE) {
            // Double.MIN_VALUE JDK转化最小double为4.9e-324， 本方法转化为5.0e-324 , 由于此值特殊为了和JDK转化一致
            return Scientific.DOUBLE_MIN;
        }
        long bits = Double.doubleToRawLongBits(doubleValue);
        int e2 = (int) (bits >> 52) & MOD_DOUBLE_EXP;
        long mantissa0 = bits & MOD_DOUBLE_MANTISSA;
        // boolean flagForUp = mantissa0 < MOD_DOUBLE_MANTISSA;
        boolean flagForDown = mantissa0 > 0;
        int e52;
        long output;
        long rawOutput, /*d2, */d3, d4;
        int e10, adl;
        if (e2 > 0) {
            if (e2 == 2047) return Scientific.SCIENTIFIC_NULL;
            mantissa0 = 1L << 52 | mantissa0;
            e52 = e2 - 1075;
        } else {
            int lz52 = Long.numberOfLeadingZeros(mantissa0) - 11;
            mantissa0 <<= lz52;
            e52 = -1074 - lz52;
        }
        boolean /*tflag = true,*/ accurate = false;
        if (e52 >= 0) {
            ED d = ED.E2_D_A[e52];
            e10 = d.e10;   // e10 > 15
            adl = d.adl;
            // d2 = d.d2;
            d3 = d.d3;
            d4 = d.d4;
            if (d.b && mantissa0 >= d.bv) {
                if (mantissa0 > d.bv) {
                    ++e10;
                    ++adl;
                } else {
                    if (doubleValue == POSITIVE_DECIMAL_POWER[e10 + 1]) {
                        return new Scientific(e10 + 1, true);
                    }
                }
            }
            int o5 = d.o5;  // adl + 2 - e10
            int sb = e52 + o5;
            if (o5 < 0) {
                // mantissa0 * 2^(e52 + o5) * 5^o5 -> mantissa0 * 2^sb / 5^(-o5)
                ED5 d5 = ED5.ED5_A[-o5];
                int rb = sb - 10 - d5.ob;
                // rawOutput = BigInteger.valueOf(mantissa0).shiftLeft(sb).divide(POW5_BI_VALUES[-o5]).longValue();
                rawOutput = multiplyHighAndShift(mantissa0 << 10, d5.oy, d5.of, 32 - rb);
                accurate = o5 == -1 && sb < 11;
            } else {
                // o5 > 0 -> sb > 0
                // accurate
                rawOutput = (mantissa0 * POW5_LONG_VALUES[o5]) << sb;
                accurate = true;
            }
        } else {
            // e52 >= -1074 -> p5 <= 1074
            int e5 = -e52;
            ED d = ED.E5_D_A[e5];
            e10 = d.e10;
            adl = d.adl;
            // d2 = d.d2;
            d3 = d.d3;
            d4 = d.d4;
            if (d.b && mantissa0 >= d.bv) {
                if (mantissa0 > d.bv) {
                    ++e10;
                    ++adl;
                } else {
                    if (e10 >= -1 && doubleValue == POSITIVE_DECIMAL_POWER[e10 + 1]) {
                        return new Scientific(e10 + 1, true);
                    }
                    if (e10 < -1 && doubleValue == NEGATIVE_DECIMAL_POWER[-e10 - 1]) {
                        return new Scientific(e10 + 1, true);
                    }
                }
            }

            int o5 = d.o5; // adl + 2 - e10; // o5 > 0
            int sb = o5 + e52;
            if (sb < 0) {
                if (o5 < POW5_LONG_VALUES.length) {
                    rawOutput = multiplyHighAndShift(mantissa0, POW5_LONG_VALUES[o5], -sb);
                } else if (o5 < POW5_LONG_VALUES.length + 4) {
                    rawOutput = multiplyHighAndShift(mantissa0 * POW5_LONG_VALUES[o5 - POW5_LONG_VALUES.length + 1], POW5_LONG_VALUES[POW5_LONG_VALUES.length - 1], -sb);
                } else {
                    ED5 ed5 = ED5.ED5_A[o5];
                    rawOutput = multiplyHighAndShift(mantissa0 << 10, ed5.y, ed5.f, -(ed5.dfb + sb) + 10);
                }
            } else {
                rawOutput = POW5_LONG_VALUES[o5] * mantissa0 << sb;
            }
        }
        if (accurate) {
            rawOutput = rawOutput / 10;
            if (adl == 16) {
                --adl;
                rawOutput = (rawOutput + 5) / 10; // rawOutput = rawOutput / 10 + ((rawOutput % 10) >= 5 ? 1 : 0);
            }
            return new Scientific(rawOutput, adl + 2, e10);
        }

        // rem <= Actual Rem Value
        long div = rawOutput / 1000;
        long rem = rawOutput - div * 1000;
        long remUp = (10001 - rem * 10) << 1;
        boolean up;
        if ((up = (remUp <= d4)) || ((rem + 1) << (flagForDown ? 1 : 2)) <= d3) {
            output = div + (up ? 1 : 0);
            --adl;
        } else {
            if (flagForDown) {
                output = (rawOutput + 50) / 100; // rawOutput / 100 + ((rawOutput % 100) >= 50 ? 1 : 0)
            } else {
                output = (rawOutput + 5) / 10; // rawOutput / 10 + ((rawOutput % 10) >= 5 ? 1 : 0)
                ++adl;
            }
        }
        return new Scientific(output, adl + 1, e10);
    }

    /**
     * ieee float -> scientific
     *
     * @param floatValue != 0
     * @return
     */
    public static Scientific floatToScientific(float floatValue) {
        final int bits = Float.floatToRawIntBits(floatValue);
        int e2 = (bits >> 23) & MOD_FLOAT_EXP;
        int mantissa0 = bits & MOD_FLOAT_MANTISSA;
        boolean nonZeroFlag = mantissa0 > 0;
        int e23;
        long output, rawOutput;
        long d4;
        int e10, adl;
        boolean accurate = false;
        if (e2 > 0) {
            mantissa0 = 1 << 23 | mantissa0;
            e23 = e2 - 150;   // 1023 - 52
        } else {
            // e2 == 0
            int l = Integer.numberOfLeadingZeros(mantissa0) - 8;
            mantissa0 <<= l;
            e23 = -149 - l;
        }
        if (e23 >= 0) {
            ED d = EF.E2_F_A[e23];
            e10 = d.e10;
            adl = d.adl;
            d4 = d.d4;
            if (d.b && mantissa0 > d.bv) {
                ++e10;
                ++adl;
            }
            int o5 = d.o5 + 6;  // 相对double(adl + 2 - e10)多加6个数字增加命中概率
            int sb = e23 + o5;
            if (o5 < 0) {
                // mantissa0 * 2^(e23 + o5) * 5^o5 -> mantissa0 * 2^sb / 5^(-o5)
                // rawOutput = BigInteger.valueOf(mantissa0).shiftLeft(sb).divide(POW5_BI_VALUES[-o5]).longValue();
                if(sb < 40) {
                    rawOutput = ((long) mantissa0 << sb) / POW5_LONG_VALUES[-o5];
                } else {
                    ED5 d5 = ED5.ED5_A[-o5];
                    rawOutput = multiplyHighAndShift((long) mantissa0 << 39, d5.oy, d5.of, 71 + d5.ob - sb);
                }
            } else {
                // o5 > 0 -> sb > 0
                // accurate
                rawOutput = mantissa0 * POW5_LONG_VALUES[o5] << sb;
                accurate = true;
            }

        } else {
            // e52 >= -149 -> p5 <= 149
            int e5 = -e23;
            ED d = EF.E5_F_A[e5];
            e10 = d.e10;
            adl = d.adl;
            d4 = d.d4;
            if (d.b && mantissa0 > d.bv) {
                ++e10;
                ++adl;
            }

            int o5 = d.o5 + 6; // 相对double(adl + 2 - e10)多加6个数字增加命中概率
            int sb = o5 + e23;
            if (sb < 0) {
                // todo To be optimized
                if(o5 < 17) {
                    rawOutput = mantissa0 * POW5_LONG_VALUES[o5] >> -sb;
                } else if (o5 < POW5_LONG_VALUES.length) {
                    rawOutput = multiplyHighAndShift(mantissa0, POW5_LONG_VALUES[o5], -sb);
                } else if (o5 < POW5_LONG_VALUES.length + 4) {
                    rawOutput = multiplyHighAndShift(mantissa0 * POW5_LONG_VALUES[o5 - POW5_LONG_VALUES.length + 1], POW5_LONG_VALUES[POW5_LONG_VALUES.length - 1], -sb);
                } else {
                    ED5 ed5 = ED5.ED5_A[o5];
                    rawOutput = multiplyHighAndShift((long) mantissa0 << 39, ed5.y, ed5.f, -(ed5.dfb + sb) + 39);   // 39 = 63 - 24
                }
            } else {
                rawOutput = POW5_LONG_VALUES[o5] * mantissa0 << sb;
                accurate = true;
            }
        }

        // todo if accurate Fast conversion ?
        if(accurate) {
        // 如果追求性能可以这里返回，但可能返回非最短数字序列（结果一定是正确的）
//            rawOutput = EnvUtils.JDK_AGENT_INSTANCE.multiplyHighKaratsuba(rawOutput, 0x6b5fca6af2bd215fL) >> 22; // rawOutput / 10000000;
//            if (adl == 7) {
//                --adl;
//                rawOutput = (rawOutput + 5) / 10; // rawOutput = rawOutput / 10 + ((rawOutput % 10) >= 5 ? 1 : 0);
//            }
//            return new Scientific(rawOutput, adl + 2, e10);
        }

        if(rawOutput < 1000000000) {
            return new Scientific(EnvUtils.JDK_AGENT_INSTANCE.multiplyHighKaratsuba(rawOutput, 0x6b5fca6af2bd215fL) >> 22, 2, e10); // rawOutput / 10000000
        }
        long div = EnvUtils.JDK_AGENT_INSTANCE.multiplyHighKaratsuba(rawOutput, 0x44b82fa09b5a52ccL) >> 28; // rawOutput / 1000000000;
        long rem = rawOutput - div * 1000000000;
        long remUp = (1000000001 - rem) << 1;
        boolean up = remUp <= d4;
        if (up || ((rem + 1) << (nonZeroFlag ? 1 : 2)) <= d4) {
            output = div + (up ? 1 : 0);
            --adl;
            if (up) {
                if (POW10_LONG_VALUES[adl] == output) {
                    ++e10;
                    output = 1;
                    adl = 0;
                }
            }
        } else {
            if (nonZeroFlag) {
                long div0 = EnvUtils.JDK_AGENT_INSTANCE.multiplyHighKaratsuba(rawOutput, 0x55e63b88c230e77fL) >> 25; // rawOutput / 100000000
                output = div0 + (rem % 100000000 >= 50000000 ? 1 : 0);
            } else {
                long div0 = EnvUtils.JDK_AGENT_INSTANCE.multiplyHighKaratsuba(rawOutput, 0x6b5fca6af2bd215fL) >> 22; // rawOutput / 10000000
                output = div0 + (rem % 10000000 >= 5000000 ? 1 : 0);
                ++adl;
            }
        }

        return new Scientific(output, adl + 1, e10);
    }

    public static void writePositiveLong(long val, Appendable appendable) throws IOException {
        int v, v1, v2, v3, v4, v5, v6, v7, v8, v9;
        if (val < 100) {
            v = (int) val;
            if (v > 9) {
                appendable.append(DigitTens[v]);
            }
            appendable.append(DigitOnes[v]);
            return;
        }
        long numValue = val;
        val = numValue / 100;
        v1 = (int) (numValue - val * 100);
        if (val < 100) {
            v = (int) val;
            if (v > 9) {
                appendable.append(DigitTens[v]);
            }
            appendable.append(DigitOnes[v]);
            appendable.append(DigitTens[v1]);
            appendable.append(DigitOnes[v1]);
            return;
        }

        numValue = val;
        val = numValue / 100;
        v2 = (int) (numValue - val * 100);
        if (val < 100) {
            v = (int) val;
            if (v > 9) {
                appendable.append(DigitTens[v]);
            }
            appendable.append(DigitOnes[v]);
            appendable.append(DigitTens[v2]);
            appendable.append(DigitOnes[v2]);
            appendable.append(DigitTens[v1]);
            appendable.append(DigitOnes[v1]);
            return;
        }

        numValue = val;
        val = numValue / 100;
        v3 = (int) (numValue - val * 100);
        if (val < 100) {
            v = (int) val;
            if (v > 9) {
                appendable.append(DigitTens[v]);
            }
            appendable.append(DigitOnes[v]);
            appendable.append(DigitTens[v3]);
            appendable.append(DigitOnes[v3]);
            appendable.append(DigitTens[v2]);
            appendable.append(DigitOnes[v2]);
            appendable.append(DigitTens[v1]);
            appendable.append(DigitOnes[v1]);
            return;
        }

        numValue = val;
        val = numValue / 100;
        v4 = (int) (numValue - val * 100);
        if (val < 100) {
            v = (int) val;
            if (v > 9) {
                appendable.append(DigitTens[v]);
            }
            appendable.append(DigitOnes[v]);
            appendable.append(DigitTens[v4]);
            appendable.append(DigitOnes[v4]);
            appendable.append(DigitTens[v3]);
            appendable.append(DigitOnes[v3]);
            appendable.append(DigitTens[v2]);
            appendable.append(DigitOnes[v2]);
            appendable.append(DigitTens[v1]);
            appendable.append(DigitOnes[v1]);
            return;
        }

        numValue = val;
        val = numValue / 100;
        v5 = (int) (numValue - val * 100);
        if (val < 100) {
            v = (int) val;
            if (v > 9) {
                appendable.append(DigitTens[v]);
            }
            appendable.append(DigitOnes[v]);
            appendable.append(DigitTens[v5]);
            appendable.append(DigitOnes[v5]);
            appendable.append(DigitTens[v4]);
            appendable.append(DigitOnes[v4]);
            appendable.append(DigitTens[v3]);
            appendable.append(DigitOnes[v3]);
            appendable.append(DigitTens[v2]);
            appendable.append(DigitOnes[v2]);
            appendable.append(DigitTens[v1]);
            appendable.append(DigitOnes[v1]);
            return;
        }

        numValue = val;
        val = numValue / 100;
        v6 = (int) (numValue - val * 100);
        if (val < 100) {
            v = (int) val;
            if (v > 9) {
                appendable.append(DigitTens[v]);
            }
            appendable.append(DigitOnes[v]);
            appendable.append(DigitTens[v6]);
            appendable.append(DigitOnes[v6]);
            appendable.append(DigitTens[v5]);
            appendable.append(DigitOnes[v5]);
            appendable.append(DigitTens[v4]);
            appendable.append(DigitOnes[v4]);
            appendable.append(DigitTens[v3]);
            appendable.append(DigitOnes[v3]);
            appendable.append(DigitTens[v2]);
            appendable.append(DigitOnes[v2]);
            appendable.append(DigitTens[v1]);
            appendable.append(DigitOnes[v1]);
            return;
        }

        numValue = val;
        val = numValue / 100;
        v7 = (int) (numValue - val * 100);
        if (val < 100) {
            v = (int) val;
            if (v > 9) {
                appendable.append(DigitTens[v]);
            }
            appendable.append(DigitOnes[v]);
            appendable.append(DigitTens[v7]);
            appendable.append(DigitOnes[v7]);
            appendable.append(DigitTens[v6]);
            appendable.append(DigitOnes[v6]);
            appendable.append(DigitTens[v5]);
            appendable.append(DigitOnes[v5]);
            appendable.append(DigitTens[v4]);
            appendable.append(DigitOnes[v4]);
            appendable.append(DigitTens[v3]);
            appendable.append(DigitOnes[v3]);
            appendable.append(DigitTens[v2]);
            appendable.append(DigitOnes[v2]);
            appendable.append(DigitTens[v1]);
            appendable.append(DigitOnes[v1]);
            return;
        }

        numValue = val;
        val = numValue / 100;
        v8 = (int) (numValue - val * 100);
        if (val < 100) {
            v = (int) val;
            if (v > 9) {
                appendable.append(DigitTens[v]);
            }
            appendable.append(DigitOnes[v]);
            appendable.append(DigitTens[v8]);
            appendable.append(DigitOnes[v8]);
            appendable.append(DigitTens[v7]);
            appendable.append(DigitOnes[v7]);
            appendable.append(DigitTens[v6]);
            appendable.append(DigitOnes[v6]);
            appendable.append(DigitTens[v5]);
            appendable.append(DigitOnes[v5]);
            appendable.append(DigitTens[v4]);
            appendable.append(DigitOnes[v4]);
            appendable.append(DigitTens[v3]);
            appendable.append(DigitOnes[v3]);
            appendable.append(DigitTens[v2]);
            appendable.append(DigitOnes[v2]);
            appendable.append(DigitTens[v1]);
            appendable.append(DigitOnes[v1]);
            return;
        }

        numValue = val;
        val = numValue / 100;
        v9 = (int) (numValue - val * 100);
        if (val < 100) {
            v = (int) val;
            if (v > 9) {
                appendable.append(DigitTens[v]);
            }
            appendable.append(DigitOnes[v]);
            appendable.append(DigitTens[v9]);
            appendable.append(DigitOnes[v9]);
            appendable.append(DigitTens[v8]);
            appendable.append(DigitOnes[v8]);
            appendable.append(DigitTens[v7]);
            appendable.append(DigitOnes[v7]);
            appendable.append(DigitTens[v6]);
            appendable.append(DigitOnes[v6]);
            appendable.append(DigitTens[v5]);
            appendable.append(DigitOnes[v5]);
            appendable.append(DigitTens[v4]);
            appendable.append(DigitOnes[v4]);
            appendable.append(DigitTens[v3]);
            appendable.append(DigitOnes[v3]);
            appendable.append(DigitTens[v2]);
            appendable.append(DigitOnes[v2]);
            appendable.append(DigitTens[v1]);
            appendable.append(DigitOnes[v1]);
        }
    }

    public static byte hexDigitAt(int c) {
        return HEX_DIGITS_REVERSE[c];
    }

    public static boolean equals(long val, String text) {
        if(text == null || text.isEmpty()) return false;
        if(val == Long.MIN_VALUE) {
            return text.equals("-9223372036854775808");
        }
        long result = 0, len = text.length();
        int i = 0;
        if(text.charAt(0) == '-') {
            ++i;
            val = -val;
        }
        for (; i < len; ++i) {
            int d = digitDecimal(text.charAt(i));
            if(d == -1) {
                return false;
            }
            result = result * 10 + d;
        }
        return val == result;
    }
}
