package io.github.wycst.wast.common.utils;

import io.github.wycst.wast.common.reflect.UnsafeHelper;

import java.io.IOException;
import java.math.BigInteger;
import java.util.Arrays;

/**
 * @Author: wangy
 * @Date: 2022/7/17 16:16
 * @Description:
 */
public final class NumberUtils {

    // Double.MAX_VALUE 1.7976931348623157e+308
    final static double[] POSITIVE_DECIMAL_POWER = new double[325];
    final static double[] NEGATIVE_DECIMAL_POWER = new double[325];
    final static char[][] POSITIVE_DECIMAL_POWER_CHARS = new char[325][];
    final static char[][] NEGATIVE_DECIMAL_POWER_CHARS = new char[325][];

    final static long[] LONG_VALUES_FOR_STRING = new long[]{
            10, 100, 1000, 10000, 100000, 1000000, 10000000, 100000000, 1000000000, 10000000000L, 100000000000L, 1000000000000L, 10000000000000L, 100000000000000L, 1000000000000000L, 10000000000000000L, 100000000000000000L, 1000000000000000000L, 9223372036854775807L
    };

    final static BigInteger BI_TO_DECIMAL_BASE = BigInteger.valueOf(1000000000L);
    final static BigInteger BI_MAX_VALUE_FOR_LONG = BigInteger.valueOf(Long.MAX_VALUE);

    //0-9a-f
    final static char[] HEX_DIGITS = "0123456789abcdef".toCharArray();

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

    final static long[] FOUR_DIGITS_64_BITS = new long[10000];
    final static int[] FOUR_DIGITS_32_BITS = new int[10000];
    final static int[] TWO_DIGITS_32_BITS = new int[100];
    final static short[] TWO_DIGITS_16_BITS = new short[100];

    final static long[] POW5_LONG_VALUES = new long[27];

    static {
        // e0 ~ e360(e306)
        for (int i = 0, len = POSITIVE_DECIMAL_POWER.length; i < len; ++i) {
            String positive = "1.0E" + i;
            String negative = "1.0E-" + i;
            POSITIVE_DECIMAL_POWER[i] = Double.valueOf("1.0E" + i);
            NEGATIVE_DECIMAL_POWER[i] = Double.valueOf("1.0E-" + i);
            POSITIVE_DECIMAL_POWER_CHARS[i] = positive.toCharArray();
            NEGATIVE_DECIMAL_POWER_CHARS[i] = negative.toCharArray();
        }
        // 4.9e-324
        NEGATIVE_DECIMAL_POWER[NEGATIVE_DECIMAL_POWER.length - 1] = Double.MIN_VALUE;
        NEGATIVE_DECIMAL_POWER_CHARS[NEGATIVE_DECIMAL_POWER_CHARS.length - 1] = "4.9E-324".toCharArray();

        long val = 1;
        for (int i = 0; i < POW5_LONG_VALUES.length; ++i) {
            POW5_LONG_VALUES[i] = val;
            val *= 5;
        }

        for (long d1 = 0; d1 < 10; ++d1) {
            for (long d2 = 0; d2 < 10; ++d2) {
                long intVal64;
                int intVal32;
                if (EnvUtils.BIG_ENDIAN) {
                    intVal64 = (d1 + 48) << 16 | (d2 + 48);
                    intVal32 = ((int) d1 + 48) << 8 | ((int) d2 + 48);
                } else {
                    intVal64 = (d2 + 48) << 16 | (d1 + 48);
                    intVal32 = ((int) d2 + 48) << 8 | ((int) d1 + 48);
                }
                int k = (int) (d1 * 10 + d2);
                TWO_DIGITS_32_BITS[k] = (int) intVal64;
                TWO_DIGITS_16_BITS[k] = (short) intVal32;
                for (long d3 = 0; d3 < 10; ++d3) {
                    for (long d4 = 0; d4 < 10; ++d4) {
                        long int64;
                        int int32;
                        if (EnvUtils.BIG_ENDIAN) {
                            int64 = (intVal64 << 32) | (d3 + 48) << 16 | (d4 + 48);
                            int32 = (intVal32 << 16) | ((int) d3 + 48) << 8 | ((int) d4 + 48);
                        } else {
                            int64 = ((d4 + 48) << 48) | ((d3 + 48) << 32) | intVal64;
                            int32 = (((int) d4 + 48) << 24) | (((int) d3 + 48) << 16) | intVal32;
                        }
                        int index = (int) (d1 * 1000 + d2 * 100 + d3 * 10 + d4);
                        FOUR_DIGITS_64_BITS[index] = int64;
                        FOUR_DIGITS_32_BITS[index] = int32;
                    }
                }
            }
        }
    }

    static final long MOD_DOUBLE_EXP = (1L << 12) - 1;
    static final int MOD_FLOAT_EXP = (1 << 9) - 1;
    static final long MOD_DOUBLE_MANTISSA = (1L << 52) - 1;
    static final int MOD_FLOAT_MANTISSA = (1 << 23) - 1;
    // Math.log2(10)
    static final double LOG2_10 = 3.321928094887362;

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
        for (long val : LONG_VALUES_FOR_STRING) {
            if (value < val) {
                return i;
            }
            ++i;
        }
        return 19;
    }

    static int stringSize(long value, int min) {
        for (int i = min; i < LONG_VALUES_FOR_STRING.length; ++i) {
            if (value < LONG_VALUES_FOR_STRING[i]) {
                return i + 1;
            }
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
     * n 位数字（ 0 < n < 5）
     *
     * @param buf
     * @param fromIndex
     * @param n
     * @return
     * @throws NumberFormatException
     */
    public static int parseIntWithin5(char[] buf, int fromIndex, int n)
            throws NumberFormatException {
        switch (n) {
            case 1:
                return parseInt1(buf, fromIndex);
            case 2:
                return parseInt2(buf, fromIndex);
            case 3:
                return parseInt3(buf, fromIndex);
            case 4:
                return parseInt4(buf, fromIndex);
        }
        throw new NumberFormatException("For input string: \"" + new String(buf, fromIndex, n) + "\"");
    }

    /**
     * n 位数字（ 0 < n < 5）
     *
     * @param bytes
     * @param fromIndex
     * @param n
     * @return
     * @throws NumberFormatException
     */
    public static int parseIntWithin5(byte[] bytes, int fromIndex, int n)
            throws NumberFormatException {
        switch (n) {
            case 1:
                return parseInt1(bytes[fromIndex]);
            case 2:
                return parseInt2(bytes[fromIndex++], bytes[fromIndex]);
            case 3:
                return parseInt3(bytes[fromIndex++], bytes[fromIndex++], bytes[fromIndex]);
            case 4:
                return parseInt4(bytes[fromIndex++], bytes[fromIndex++], bytes[fromIndex++], bytes[fromIndex]);
        }
        throw new NumberFormatException("For input string: \"" + new String(bytes, fromIndex, n) + "\"");
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
     * 转化为3位十进制数
     *
     * @param buf
     * @param fromIndex
     * @return
     */
    public static int parseInt3(char[] buf, int fromIndex)
            throws NumberFormatException {
        return parseInt3(buf[fromIndex++], buf[fromIndex++], buf[fromIndex]);
    }

    /**
     * 转化为3位十进制数
     *
     * @param c1 百位
     * @param c2 十位
     * @param c3 个位
     * @return
     * @throws NumberFormatException
     */
    public static int parseInt3(int c1, int c2, int c3)
            throws NumberFormatException {
        int v1 = digitDecimal(c1);
        int v2 = digitDecimal(c2);
        int v3 = digitDecimal(c3);
        if ((v1 | v2 | v3) == -1) {
            throw new NumberFormatException("For input string: \"" + new String(new char[]{(char) c1, (char) c2, (char) c3}) + "\"");
        }
        return v1 * 100 + v2 * 10 + v3;
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

    // 18
    public static int writeUUIDMostSignificantBits(long mostSigBits, char[] buf, int offset) {
        // last 4
        int index = offset + 18;
        for (int i = 3; i > -1; --i) {
            int val = (int) (mostSigBits & 0xf);
            buf[--index] = HEX_DIGITS[val];
            mostSigBits >>= 4L;
        }
        buf[--index] = '-';
        for (int i = 3; i > -1; --i) {
            int val = (int) (mostSigBits & 0xf);
            buf[--index] = HEX_DIGITS[val];
            mostSigBits >>= 4L;
        }
        buf[--index] = '-';
        for (int i = 7; i > -1; --i) {
            int val = (int) (mostSigBits & 0xf);
            buf[--index] = HEX_DIGITS[val];
            mostSigBits >>= 4L;
        }
        return 18;
    }

    public static int writeUUIDLeastSignificantBits(long leastSigBits, char[] buf, int offset) {
        // last 4
        buf[offset] = '-';
        int index = offset + 18;
        for (int i = 11; i > -1; --i) {
            int val = (int) (leastSigBits & 0xf);
            buf[--index] = HEX_DIGITS[val];
            leastSigBits >>= 4L;
        }
        buf[--index] = '-';
        for (int i = 3; i > -1; --i) {
            int val = (int) (leastSigBits & 0xf);
            buf[--index] = HEX_DIGITS[val];
            leastSigBits >>= 4L;
        }
        return 18;
    }

    public static int writeUUIDMostSignificantBits(long mostSigBits, byte[] buf, int offset) {
        // last 4
        int index = offset + 18;
        for (int i = 3; i > -1; --i) {
            int val = (int) (mostSigBits & 0xf);
            buf[--index] = (byte) HEX_DIGITS[val];
            mostSigBits >>= 4L;
        }
        buf[--index] = '-';
        for (int i = 3; i > -1; --i) {
            int val = (int) (mostSigBits & 0xf);
            buf[--index] = (byte) HEX_DIGITS[val];
            mostSigBits >>= 4L;
        }
        buf[--index] = '-';
        for (int i = 7; i > -1; --i) {
            int val = (int) (mostSigBits & 0xf);
            buf[--index] = (byte) HEX_DIGITS[val];
            mostSigBits >>= 4L;
        }
        return 18;
    }

    public static int writeUUIDLeastSignificantBits(long leastSigBits, byte[] buf, int offset) {
        // last 4
        buf[offset] = '-';
        int index = offset + 18;
        for (int i = 11; i > -1; --i) {
            int val = (int) (leastSigBits & 0xf);
            buf[--index] = (byte) HEX_DIGITS[val];
            leastSigBits >>= 4L;
        }
        buf[--index] = '-';
        for (int i = 3; i > -1; --i) {
            int val = (int) (leastSigBits & 0xf);
            buf[--index] = (byte) HEX_DIGITS[val];
            leastSigBits >>= 4L;
        }
        return 18;
    }

//    /**
//     * 此方法主要为优化double序列化获取精确的十进制尾数（最大取${maxDigits <= 18}位字符，第18位四舍五入）
//     *
//     * @param l
//     * @param r
//     * @return
//     */
//    static long multiplyAsPrefix(long l, long r, int maxDigits) {
//        if (l == 0 || r == 0) return 0;
//        if (l < 0 || r < 0) {
//            new ArithmeticException("Negative arguments l = " + l + " and r = " + r);
//        }
//        if (maxDigits > 18) {
//            maxDigits = 18;
//        }
//        int zeroBits = Long.numberOfLeadingZeros(l) + Long.numberOfLeadingZeros(r);
//        if (zeroBits > 64) return l * r;
//        long BASE = 1000000000000000000L;
//        if (l >= BASE) {
//            l /= 10;
//        }
//        if (r >= BASE) {
//            r /= 10;
//        }
//        long HALF_BASE = 1000000000L;
//        long lh = l / HALF_BASE, ll = l % HALF_BASE;
//        long rh = r / HALF_BASE, rl = r % HALF_BASE;
//        long carry = lh * rh;
//        long rem = ll * rl;
//        long c = lh * rl + rh * ll;
//        if (c >= HALF_BASE) {
//            carry += c / HALF_BASE;
//            rem += (c % HALF_BASE) * HALF_BASE;
//        }
//        if (rem >= BASE) {
//            carry += rem / BASE;
//            rem = rem % BASE;
//        }
//        int len = stringSize(carry);
//        int n = len - maxDigits;
//        if (n >= 0) {
//            return n == 0 ? carry : carry / LONG_VALUES_FOR_STRING[n - 1];
//        }
//        n = -n;
//        return carry * LONG_VALUES_FOR_STRING[n - 1] + rem / LONG_VALUES_FOR_STRING[17 - n];
//    }

    /**
     * 将double值写入到字符数组(保留16位有效数字)
     *
     * @param doubleValue
     * @param buf
     * @param off
     * @return
     */
    public static int writeDouble(double doubleValue, char[] buf, int off) {

        final int beginIndex = off;
        if (Double.isNaN(doubleValue)
                || doubleValue == Double.POSITIVE_INFINITY
                || doubleValue == Double.NEGATIVE_INFINITY) {
            buf[off++] = 'n';
            buf[off++] = 'u';
            buf[off++] = 'l';
            buf[off++] = 'l';
            return off - beginIndex;
        }

        long bits;
        if (doubleValue == 0) {
            bits = Double.doubleToLongBits(doubleValue);
            if (bits == 0x8000000000000000L) {
                buf[off++] = '-';
            }
            buf[off++] = '0';
            buf[off++] = '.';
            buf[off++] = '0';
            return off - beginIndex;
        }
        boolean sign = doubleValue < 0;
        if (sign) {
            buf[off++] = '-';
            doubleValue = -doubleValue;
        }

        // if long value direct write as decimal
        if (doubleValue == (long) doubleValue) {
            return writeDecimal((long) doubleValue, buf, beginIndex, off);
        }

        // bits
        // 1/11/52
        bits = Double.doubleToRawLongBits(doubleValue);
        long exp2Bits = (bits >> 52) & MOD_DOUBLE_EXP;
        int e2 = (int) exp2Bits - 1023;
        if (exp2Bits == 0) {
            e2 -= Long.numberOfLeadingZeros(bits) - 12;
        } else {
            int e2mantissa = e2 - 52; // exp2Bits - 1023 - 52
            if (e2mantissa < 0) {
                long mantissa = bits & MOD_DOUBLE_MANTISSA;
                mantissa = (1L << 52) | mantissa;
                int trailZeros = Long.numberOfTrailingZeros(mantissa);
                // TrailZeros+e2mantissa must be less than 0
                int em = -trailZeros - e2mantissa;
                long val5;
                if (em < POW5_LONG_VALUES.length && 1L << (trailZeros + 10) > (val5 = POW5_LONG_VALUES[em])) {
                    long longVal = (mantissa >> trailZeros) * val5;
                    int e10 = stringSize((long) doubleValue) - 1;
                    int digitCnt = stringSize(longVal, e10 + 1);
                    return writeDecimal(longVal, digitCnt, e10, buf, beginIndex, off);
                }
            }
        }

        long longVal;
        int stringSize = 16;
        // 通过二进制指数计算十进制指数
        int e10 = (int) (e2 / LOG2_10 + (e2 > 0 ? 0.5 : -0.5));
        if (e10 >= 0) {
            if (doubleValue > POSITIVE_DECIMAL_POWER[e10 + 1]) {
                ++e10;
            } else if (doubleValue < POSITIVE_DECIMAL_POWER[e10]) {
                --e10;
            }
        } else {
            if (doubleValue > NEGATIVE_DECIMAL_POWER[-e10 - 1]) {
                ++e10;
            } else if (doubleValue < NEGATIVE_DECIMAL_POWER[-e10]) {
                --e10;
            }
        }

        // 判断是否和POSITIVE_DECIMAL_POWER[decimalExp]相等
        // 比如1.0E23, java内置的toString()输出结果为9.999999999999999E22, 和预期的1.0E23不一致，但javascript会正常输出1.0E+23
        if (e10 > 0) {
            if (doubleValue == POSITIVE_DECIMAL_POWER[e10]) {
                char[] chars = POSITIVE_DECIMAL_POWER_CHARS[e10];
                System.arraycopy(chars, 0, buf, off, chars.length);
                off += chars.length;
                return off - beginIndex;
            }
        } else {
            if (doubleValue == NEGATIVE_DECIMAL_POWER[-e10]) {
                char[] chars = NEGATIVE_DECIMAL_POWER_CHARS[-e10];
                System.arraycopy(chars, 0, buf, off, chars.length);
                off += chars.length;
                return off - beginIndex;
            }
        }

        // 计算output值
        // 计算17有效数字long值
        int rn = 17 - e10;
        if (rn >= 0) {
            if (rn > 308) {
                double smallVal = NEGATIVE_DECIMAL_POWER[-e10];
                longVal = (long) (doubleValue * 1E17 / smallVal);
            } else {
                longVal = (long) (doubleValue * POSITIVE_DECIMAL_POWER[rn]);
            }
        } else {
            longVal = (long) (doubleValue / POSITIVE_DECIMAL_POWER[-rn]);
        }

        int digit = (int) (longVal % 100);
        longVal = (longVal / 100) + (digit >= 50 ? 1 : 0);

        return writeDecimal(longVal, stringSize, e10, buf, beginIndex, off);
    }

    /**
     * float写入buf（保留7位有效数字）
     *
     * @param floatValue
     * @param buf
     * @param off
     * @return
     */
    public static int writeFloat(float floatValue, char[] buf, int off) {
        final int beginIndex = off;
        if (Float.isNaN(floatValue) || floatValue == Float.POSITIVE_INFINITY || floatValue == Float.NEGATIVE_INFINITY) {
            buf[off++] = 'n';
            buf[off++] = 'u';
            buf[off++] = 'l';
            buf[off++] = 'l';
            return off - beginIndex;
        }
        int bits;
        if (floatValue == 0) {
            bits = Float.floatToIntBits(floatValue);
            if (bits == 0x80000000) {
                buf[off++] = '-';
            }
            buf[off++] = '0';
            buf[off++] = '.';
            buf[off++] = '0';
            return off - beginIndex;
        }
        boolean sign = floatValue < 0;
        if (sign) {
            buf[off++] = '-';
            floatValue = -floatValue;
        }

        // write long as decimal
        if (floatValue == (long) floatValue) {
            return writeDecimal((long) floatValue, buf, beginIndex, off);
        }

        // bits
        bits = Float.floatToRawIntBits(floatValue);
        // 32bits
        // 1/8/23
        int exp2Bits = (bits >> 23) & MOD_FLOAT_EXP;
        int e2 = exp2Bits - 127;
        if (exp2Bits == 0) {
            e2 -= Integer.numberOfLeadingZeros(bits) - 9;
        } else {
            int e2mantissa = e2 - 23; //
            if (e2mantissa < 0) {
                int mantissa = bits & MOD_FLOAT_MANTISSA;
                mantissa = (1 << 23) | mantissa;
                int trailZeros = Integer.numberOfTrailingZeros(mantissa);
                // TrailZeros+e2mantissa must be less than 0
                int em = -trailZeros - e2mantissa;
                long val5;
                if (em < POW5_LONG_VALUES.length && 1L << (trailZeros + 10) > (val5 = POW5_LONG_VALUES[em])) {
                    long longVal = (mantissa >> trailZeros) * val5;
                    int e10 = stringSize((long) floatValue) - 1;
                    int digitCnt = stringSize(longVal, e10 + 1);
                    return writeDecimal(longVal, digitCnt, e10, buf, beginIndex, off);
                }
            }
        }

        long value;
        int digitCnt = 7;
        // 十进制指数
        int e10 = (int) (e2 / LOG2_10 + (e2 > 0 ? 0.5 : -0.5));
        if (e10 >= 0) {
            if (floatValue > POSITIVE_DECIMAL_POWER[e10 + 1]) {
                ++e10;
            } else if (floatValue < POSITIVE_DECIMAL_POWER[e10]) {
                --e10;
            }
        } else {
            if (floatValue > NEGATIVE_DECIMAL_POWER[-e10 - 1]) {
                ++e10;
            } else if (floatValue < NEGATIVE_DECIMAL_POWER[-e10]) {
                --e10;
            }
        }

        // 判断是否和POSITIVE_DECIMAL_POWER[decimalExp]相等
        // 比如1.0E23, java内置的toString()输出结果为9.999999999999999E22, 和预期的1.0E23不一致，但javascript会正常输出1.0E+23
        if (e10 > 0) {
            if (floatValue == POSITIVE_DECIMAL_POWER[e10]) {
                char[] chars = POSITIVE_DECIMAL_POWER_CHARS[e10];
                System.arraycopy(chars, 0, buf, off, chars.length);
                off += chars.length;
                return off - beginIndex;
            }
        } else {
            if (floatValue == NEGATIVE_DECIMAL_POWER[-e10]) {
                char[] chars = NEGATIVE_DECIMAL_POWER_CHARS[-e10];
                System.arraycopy(chars, 0, buf, off, chars.length);
                off += chars.length;
                return off - beginIndex;
            }
        }

        // 计算output值, 保留7位
        // 计算8有效数字long值
        int rn = 8 - e10;
        if (rn >= 0) {
            value = (int) (floatValue * POSITIVE_DECIMAL_POWER[rn]);
        } else {
            value = (int) (floatValue / POSITIVE_DECIMAL_POWER[-rn]);
        }
        int digit = (int) (value % 100);
        value = (value / 100) + (digit >= 50 ? 1 : 0);

        // write output
        return writeDecimal(value, digitCnt, e10, buf, beginIndex, off);
    }

    // ensure val > 0
    static int writeDecimal(long val, char[] buf, int beginIndex, int off) {
        if (val < 10000000L) {
            off += writePositiveLong(val, buf, off);
            buf[off++] = '.';
            buf[off++] = '0';
            return off - beginIndex;
        } else {
            int e = 0;
            // ensure val != 0
            while (val % 100 == 0) {
                e += 2;
                val /= 100;
            }
            // ensure val != 0
            if (val % 10 == 0) {
                ++e;
                val /= 10;
            }
            off += writePositiveLong(val, buf, off);
            buf[off++] = '.';
            buf[off++] = '0';
            // e <= 18
            if (e > 0) {
                buf[off++] = 'E';
                if (e > 9) {
                    buf[off++] = DigitTens[e];
                }
                buf[off++] = DigitOnes[e];
            }
            return off - beginIndex;
        }
    }

    private static int writeDecimal(long value, int digitCnt, int e10, char[] buf, int beginIndex, int off) {
        // 去掉尾部的0
        while (value % 100 == 0) {
            digitCnt -= 2;
            value /= 100;
            if (digitCnt == 1) break;
        }
        if (value % 10 == 0) {
            if (value > 0) {
                --digitCnt;
                value /= 10;
            }
        }
        // 是否使用科学计数法
        boolean useScientific = !((e10 >= -3) && (e10 < 7));
        if (useScientific) {
            if (digitCnt == 1) {
                buf[off++] = (char) (value + 48);
                buf[off++] = '.';
                buf[off++] = '0';
            } else {
                int pos = digitCnt - 2;
                // 获取首位数字
                long tl = LONG_VALUES_FOR_STRING[pos];
                int fd = (int) (value / tl);
                buf[off++] = (char) (fd + 48);
                buf[off++] = '.';

                long pointAfter = value - fd * tl;
                // 补0
                while (--pos > -1 && pointAfter < LONG_VALUES_FOR_STRING[pos]) {
                    buf[off++] = '0';
                }
                off += writePositiveLong(pointAfter, buf, off);
            }
            buf[off++] = 'E';
            if (e10 < 0) {
                buf[off++] = '-';
                e10 = -e10;
            }
            if (e10 > 99) {
                int n = e10 / 100;
                buf[off++] = (char) (n + 48);
                e10 = e10 - n * 100;

                buf[off++] = DigitTens[e10];
                buf[off++] = DigitOnes[e10];
            } else {
                if (e10 > 9) {
                    buf[off++] = DigitTens[e10];
                }
                buf[off++] = DigitOnes[e10];
            }
        } else {
            // 非科学计数法例如12345, 在size = decimalExp时写入小数点.
            if (e10 < 0) {
                // -1/-2/-3
                buf[off++] = '0';
                buf[off++] = '.';
                if (e10 == -2) {
                    buf[off++] = '0';
                } else if (e10 == -3) {
                    buf[off++] = '0';
                    buf[off++] = '0';
                }
                off += writePositiveLong(value, buf, off);
            } else {
                // 0 - 6
                int decimalPointPos = (digitCnt - 1) - e10;
                if (decimalPointPos > 0) {
                    int pos = decimalPointPos - 1;
                    long tl = LONG_VALUES_FOR_STRING[pos];
                    int pointBefore = (int) (value / tl);
                    off += writePositiveLong(pointBefore, buf, off);
                    buf[off++] = '.';
                    long pointAfter = value - pointBefore * tl;
                    // 补0
                    while (--pos > -1 && pointAfter < LONG_VALUES_FOR_STRING[pos]) {
                        buf[off++] = '0';
                    }
                    off += writePositiveLong(pointAfter, buf, off);
                } else {
                    off += writePositiveLong(value, buf, off);
                    int zeroCnt = -decimalPointPos;
                    if (zeroCnt > 0) {
                        for (int i = 0; i < zeroCnt; ++i) {
                            buf[off++] = '0';
                        }
                    }
                    buf[off++] = '.';
                    buf[off++] = '0';
                }
            }
        }

        return off - beginIndex;
    }

    /**
     * 将double值写入到字节数组(保留16位有效数字)
     *
     * @param doubleValue
     * @param buf
     * @param off
     * @return
     */
    public static int writeDouble(double doubleValue, byte[] buf, int off) {

        final int beginIndex = off;
        if (Double.isNaN(doubleValue)
                || doubleValue == Double.POSITIVE_INFINITY
                || doubleValue == Double.NEGATIVE_INFINITY) {
            buf[off++] = 'n';
            buf[off++] = 'u';
            buf[off++] = 'l';
            buf[off++] = 'l';
            return off - beginIndex;
        }

        long bits;
        if (doubleValue == 0) {
            bits = Double.doubleToLongBits(doubleValue);
            if (bits == 0x8000000000000000L) {
                buf[off++] = '-';
            }
            buf[off++] = '0';
            buf[off++] = '.';
            buf[off++] = '0';
            return off - beginIndex;
        }
        boolean sign = doubleValue < 0;
        if (sign) {
            buf[off++] = '-';
            doubleValue = -doubleValue;
        }

        // if long value direct write as decimal
        if (doubleValue == (long) doubleValue) {
            return writeDecimal((long) doubleValue, buf, beginIndex, off);
        }

        // bits
        // 1/11/52
        bits = Double.doubleToRawLongBits(doubleValue);
        long exp2Bits = (bits >> 52) & MOD_DOUBLE_EXP;
        int e2 = (int) exp2Bits - 1023;
        if (exp2Bits == 0) {
            e2 -= Long.numberOfLeadingZeros(bits) - 12;
        } else {
            int e2mantissa = e2 - 52; // exp2Bits - 1023 - 52
            if (e2mantissa < 0) {
                long mantissa = bits & MOD_DOUBLE_MANTISSA;
                mantissa = (1L << 52) | mantissa;
                int trailZeros = Long.numberOfTrailingZeros(mantissa);
                // TrailZeros+e2mantissa must be less than 0
                int em = -trailZeros - e2mantissa;
                long val5;
                if (em < POW5_LONG_VALUES.length && 1L << (trailZeros + 10) > (val5 = POW5_LONG_VALUES[em])) {
                    long longVal = (mantissa >> trailZeros) * val5;
                    int e10 = stringSize((long) doubleValue) - 1;
                    int digitCnt = stringSize(longVal, e10 + 1);
                    return writeDecimal(longVal, digitCnt, e10, buf, beginIndex, off);
                }
            }
        }

        long longVal;
        int stringSize = 16;
        // 通过二进制指数计算十进制指数
        int e10 = (int) (e2 / LOG2_10 + (e2 > 0 ? 0.5 : -0.5));
        if (e10 >= 0) {
            if (doubleValue > POSITIVE_DECIMAL_POWER[e10 + 1]) {
                ++e10;
            } else if (doubleValue < POSITIVE_DECIMAL_POWER[e10]) {
                --e10;
            }
        } else {
            if (doubleValue > NEGATIVE_DECIMAL_POWER[-e10 - 1]) {
                ++e10;
            } else if (doubleValue < NEGATIVE_DECIMAL_POWER[-e10]) {
                --e10;
            }
        }

        // 判断是否和POSITIVE_DECIMAL_POWER[decimalExp]相等
        // 比如1.0E23, java内置的toString()输出结果为9.999999999999999E22, 和预期的1.0E23不一致，但javascript会正常输出1.0E+23
        if (e10 > 0) {
            if (doubleValue == POSITIVE_DECIMAL_POWER[e10]) {
                char[] chars = POSITIVE_DECIMAL_POWER_CHARS[e10];
                System.arraycopy(chars, 0, buf, off, chars.length);
                off += chars.length;
                return off - beginIndex;
            }
        } else {
            if (doubleValue == NEGATIVE_DECIMAL_POWER[-e10]) {
                char[] chars = NEGATIVE_DECIMAL_POWER_CHARS[-e10];
                System.arraycopy(chars, 0, buf, off, chars.length);
                off += chars.length;
                return off - beginIndex;
            }
        }

        // 计算output值
        // 计算17有效数字long值
        int rn = 17 - e10;
        if (rn >= 0) {
            if (rn > 308) {
                double smallVal = NEGATIVE_DECIMAL_POWER[-e10];
                longVal = (long) (doubleValue * 1E17 / smallVal);
            } else {
                longVal = (long) (doubleValue * POSITIVE_DECIMAL_POWER[rn]);
            }
        } else {
            longVal = (long) (doubleValue / POSITIVE_DECIMAL_POWER[-rn]);
        }

        int digit = (int) (longVal % 100);
        longVal = (longVal / 100) + (digit >= 50 ? 1 : 0);

        return writeDecimal(longVal, stringSize, e10, buf, beginIndex, off);
    }

    /**
     * float写入buf（保留7位有效数字）
     *
     * @param floatValue
     * @param buf
     * @param off
     * @return
     */
    public static int writeFloat(float floatValue, byte[] buf, int off) {
        final int beginIndex = off;
        if (Float.isNaN(floatValue) || floatValue == Float.POSITIVE_INFINITY || floatValue == Float.NEGATIVE_INFINITY) {
            buf[off++] = 'n';
            buf[off++] = 'u';
            buf[off++] = 'l';
            buf[off++] = 'l';
            return off - beginIndex;
        }
        int bits;
        if (floatValue == 0) {
            bits = Float.floatToIntBits(floatValue);
            if (bits == 0x80000000) {
                buf[off++] = '-';
            }
            buf[off++] = '0';
            buf[off++] = '.';
            buf[off++] = '0';
            return off - beginIndex;
        }
        boolean sign = floatValue < 0;
        if (sign) {
            buf[off++] = '-';
            floatValue = -floatValue;
        }

        // write long as decimal
        if (floatValue == (long) floatValue) {
            return writeDecimal((long) floatValue, buf, beginIndex, off);
        }

        // bits
        bits = Float.floatToRawIntBits(floatValue);
        // 32bits
        // 1/8/23
        int exp2Bits = (bits >> 23) & MOD_FLOAT_EXP;
        int e2 = exp2Bits - 127;
        if (exp2Bits == 0) {
            e2 -= Integer.numberOfLeadingZeros(bits) - 9;
        } else {
            int e2mantissa = e2 - 23; //
            if (e2mantissa < 0) {
                int mantissa = bits & MOD_FLOAT_MANTISSA;
                mantissa = (1 << 23) | mantissa;
                int trailZeros = Integer.numberOfTrailingZeros(mantissa);
                // TrailZeros+e2mantissa must be less than 0
                int em = -trailZeros - e2mantissa;
                long val5;
                if (em < POW5_LONG_VALUES.length && 1L << (trailZeros + 10) > (val5 = POW5_LONG_VALUES[em])) {
                    long longVal = (mantissa >> trailZeros) * val5;
                    int e10 = stringSize((long) floatValue) - 1;
                    int digitCnt = stringSize(longVal, e10 + 1);
                    return writeDecimal(longVal, digitCnt, e10, buf, beginIndex, off);
                }
            }
        }

        long value;
        int digitCnt = 7;
        // 十进制指数
        int e10 = (int) (e2 / LOG2_10 + (e2 > 0 ? 0.5 : -0.5));
        if (e10 >= 0) {
            if (floatValue > POSITIVE_DECIMAL_POWER[e10 + 1]) {
                ++e10;
            } else if (floatValue < POSITIVE_DECIMAL_POWER[e10]) {
                --e10;
            }
        } else {
            if (floatValue > NEGATIVE_DECIMAL_POWER[-e10 - 1]) {
                ++e10;
            } else if (floatValue < NEGATIVE_DECIMAL_POWER[-e10]) {
                --e10;
            }
        }

        // 判断是否和POSITIVE_DECIMAL_POWER[decimalExp]相等
        // 比如1.0E23, java内置的toString()输出结果为9.999999999999999E22, 和预期的1.0E23不一致，但javascript会正常输出1.0E+23
        if (e10 > 0) {
            if (floatValue == POSITIVE_DECIMAL_POWER[e10]) {
                char[] chars = POSITIVE_DECIMAL_POWER_CHARS[e10];
                System.arraycopy(chars, 0, buf, off, chars.length);
                off += chars.length;
                return off - beginIndex;
            }
        } else {
            if (floatValue == NEGATIVE_DECIMAL_POWER[-e10]) {
                char[] chars = NEGATIVE_DECIMAL_POWER_CHARS[-e10];
                System.arraycopy(chars, 0, buf, off, chars.length);
                off += chars.length;
                return off - beginIndex;
            }
        }

        // 计算output值, 保留7位
        // 计算8有效数字long值
        int rn = 8 - e10;
        if (rn >= 0) {
            value = (int) (floatValue * POSITIVE_DECIMAL_POWER[rn]);
        } else {
            value = (int) (floatValue / POSITIVE_DECIMAL_POWER[-rn]);
        }
        int digit = (int) (value % 100);
        value = (value / 100) + (digit >= 50 ? 1 : 0);

        // write output
        return writeDecimal(value, digitCnt, e10, buf, beginIndex, off);
    }

    // ensure val > 0
    static int writeDecimal(long val, byte[] buf, int beginIndex, int off) {
        if (val < 10000000L) {
            off += writePositiveLong(val, buf, off);
            buf[off++] = '.';
            buf[off++] = '0';
            return off - beginIndex;
        } else {
            int e = 0;
            // ensure val != 0
            while (val % 100 == 0) {
                e += 2;
                val /= 100;
            }
            // ensure val != 0
            if (val % 10 == 0) {
                ++e;
                val /= 10;
            }
            off += writePositiveLong(val, buf, off);
            buf[off++] = '.';
            buf[off++] = '0';
            // e <= 18
            if (e > 0) {
                buf[off++] = 'E';
                if (e > 9) {
                    buf[off++] = (byte) DigitTens[e];
                }
                buf[off++] = (byte) DigitOnes[e];
            }
            return off - beginIndex;
        }
    }

    private static int writeDecimal(long value, int digitCnt, int e10, byte[] buf, int beginIndex, int off) {
        // 去掉尾部的0
        while (value % 100 == 0) {
            digitCnt -= 2;
            value /= 100;
            if (digitCnt == 1) break;
        }
        if (value % 10 == 0) {
            if (value > 0) {
                --digitCnt;
                value /= 10;
            }
        }
        // 是否使用科学计数法
        boolean useScientific = !((e10 >= -3) && (e10 < 7));
        if (useScientific) {
            if (digitCnt == 1) {
                buf[off++] = (byte) (value + 48);
                buf[off++] = '.';
                buf[off++] = '0';
            } else {
                int pos = digitCnt - 2;
                // 获取首位数字
                long tl = LONG_VALUES_FOR_STRING[pos];
                int fd = (int) (value / tl);
                buf[off++] = (byte) (fd + 48);
                buf[off++] = '.';

                long pointAfter = value - fd * tl;
                // 补0
                while (--pos > -1 && pointAfter < LONG_VALUES_FOR_STRING[pos]) {
                    buf[off++] = '0';
                }
                off += writePositiveLong(pointAfter, buf, off);
            }
            buf[off++] = 'E';
            if (e10 < 0) {
                buf[off++] = '-';
                e10 = -e10;
            }
            if (e10 > 99) {
                int n = e10 / 100;
                buf[off++] = (byte) (n + 48);
                e10 = e10 - n * 100;

                buf[off++] = (byte) DigitTens[e10];
                buf[off++] = (byte) DigitOnes[e10];
            } else {
                if (e10 > 9) {
                    buf[off++] = (byte) DigitTens[e10];
                }
                buf[off++] = (byte) DigitOnes[e10];
            }
        } else {
            // 非科学计数法例如12345, 在size = decimalExp时写入小数点.
            if (e10 < 0) {
                // -1/-2/-3
                buf[off++] = '0';
                buf[off++] = '.';
                if (e10 == -2) {
                    buf[off++] = '0';
                } else if (e10 == -3) {
                    buf[off++] = '0';
                    buf[off++] = '0';
                }
                off += writePositiveLong(value, buf, off);
            } else {
                // 0 - 6
                int decimalPointPos = (digitCnt - 1) - e10;
                if (decimalPointPos > 0) {
                    int pos = decimalPointPos - 1;
                    long tl = LONG_VALUES_FOR_STRING[pos];
                    int pointBefore = (int) (value / tl);
                    off += writePositiveLong(pointBefore, buf, off);
                    buf[off++] = '.';
                    long pointAfter = value - pointBefore * tl;
                    // 补0
                    while (--pos > -1 && pointAfter < LONG_VALUES_FOR_STRING[pos]) {
                        buf[off++] = '0';
                    }
                    off += writePositiveLong(pointAfter, buf, off);
                } else {
                    off += writePositiveLong(value, buf, off);
                    int zeroCnt = -decimalPointPos;
                    if (zeroCnt > 0) {
                        for (int i = 0; i < zeroCnt; ++i) {
                            buf[off++] = '0';
                        }
                    }
                    buf[off++] = '.';
                    buf[off++] = '0';
                }
            }
        }

        return off - beginIndex;
    }

    public static int writeBigInteger(BigInteger val, char[] chars, int off) {
        final int beginIndex = off;
        if (val.signum() == -1) {
            chars[off++] = '-';
            val = val.negate();
        }
        if (val.compareTo(BI_MAX_VALUE_FOR_LONG) < 1) {
            long value = val.longValue();
            off += NumberUtils.writePositiveLong(value, chars, off);
            return off - beginIndex;
        }
        int bigLength = val.bitLength();
        int[] values = new int[bigLength / 31];
        int len = 0;
        do {
            BigInteger[] bigIntegers = val.divideAndRemainder(BI_TO_DECIMAL_BASE);
            int rem = bigIntegers[1].intValue();
            val = bigIntegers[0];
            if (val.compareTo(BI_MAX_VALUE_FOR_LONG) < 1) {
                long headNum = val.longValue();
                off += NumberUtils.writePositiveLong(headNum, chars, off);
                // rem < BI_TO_DECIMAL_BASE
                int pos = 8;
                while (--pos > -1 && rem < LONG_VALUES_FOR_STRING[pos]) {
                    chars[off++] = '0';
                }
                off += NumberUtils.writePositiveLong(rem, chars, off);
                for (int j = len - 1; j > -1; --j) {
                    int value = values[j];
                    pos = 8;
                    // value < BI_TO_DECIMAL_BASE
                    while (--pos > -1 && value < LONG_VALUES_FOR_STRING[pos]) {
                        chars[off++] = '0';
                    }
                    off += NumberUtils.writePositiveLong(value, chars, off);
                }
                return off - beginIndex;
            }
            values[len++] = rem;
        } while (true);
    }

    public static int writeBigInteger(BigInteger val, byte[] buf, int off) {
        final int beginIndex = off;
        if (val.signum() == -1) {
            buf[off++] = '-';
            val = val.negate();
        }
        if (val.compareTo(BI_MAX_VALUE_FOR_LONG) < 1) {
            long value = val.longValue();
            off += NumberUtils.writePositiveLong(value, buf, off);
            return off - beginIndex;
        }
        int bigLength = val.bitLength();
        int[] values = new int[bigLength / 31];
        int len = 0;
        do {
            BigInteger[] bigIntegers = val.divideAndRemainder(BI_TO_DECIMAL_BASE);
            int rem = bigIntegers[1].intValue();
            val = bigIntegers[0];
            if (val.compareTo(BI_MAX_VALUE_FOR_LONG) < 1) {
                long headNum = val.longValue();
                off += NumberUtils.writePositiveLong(headNum, buf, off);
                // rem < BI_TO_DECIMAL_BASE
                int pos = 8;
                while (--pos > -1 && rem < LONG_VALUES_FOR_STRING[pos]) {
                    buf[off++] = '0';
                }
                off += NumberUtils.writePositiveLong(rem, buf, off);
                for (int j = len - 1; j > -1; --j) {
                    int value = values[j];
                    pos = 8;
                    // value < BI_TO_DECIMAL_BASE
                    while (--pos > -1 && value < LONG_VALUES_FOR_STRING[pos]) {
                        buf[off++] = '0';
                    }
                    off += NumberUtils.writePositiveLong(value, buf, off);
                }
                return off - beginIndex;
            }
            values[len++] = rem;
        } while (true);
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

    static int writeThreeDigits(int val, char[] chars, int off) {
        if (val < 10) {
            chars[off] = DigitOnes[val];
            return 1;
        }
        if (val < 100) {
            chars[off++] = DigitTens[val];
            chars[off] = DigitOnes[val];
            return 2;
        }
        int v = val / 100;
        int v1 = val - v * 100;
        chars[off++] = DigitOnes[v];
        chars[off++] = DigitTens[v1];
        chars[off] = DigitOnes[v1];
        return 3;
    }

    static int writeThreeDigits(int val, byte[] buf, int off) {
        if (val < 10) {
            buf[off] = (byte) DigitOnes[val];
            return 1;
        }
        if (val < 100) {
            buf[off++] = (byte) DigitTens[val];
            buf[off] = (byte) DigitOnes[val];
            return 2;
        }
        int v = val / 100;
        int v1 = val - v * 100;
        buf[off++] = (byte) DigitOnes[v];
        buf[off++] = (byte) DigitTens[v1];
        buf[off] = (byte) DigitOnes[v1];
        return 3;
    }

    /**
     * 写入4位数字字符，不够4位前面补0
     *
     * @param val
     * @param chars
     * @param off
     * @return 4
     * @throws IndexOutOfBoundsException if val < 0 or val > 10000
     */
    public static int writeFourDigits(int val, char[] chars, int off) {
        UnsafeHelper.putLong(chars, off, FOUR_DIGITS_64_BITS[val]);
        return 4;
    }

    /**
     * 写入4位数字字节，不够4位前面补0
     *
     * @param val
     * @param buf
     * @param off
     * @return 4
     * @throws IndexOutOfBoundsException if val < 0 or val > 10000
     */
    public static int writeFourDigits(int val, byte[] buf, int off) {
        UnsafeHelper.putInt(buf, off, FOUR_DIGITS_32_BITS[val]);
        return 4;
    }

    /**
     * 写入2位数字字符，不够2位前面补0
     *
     * @param val
     * @param chars
     * @param off
     * @return 2
     * @throws IndexOutOfBoundsException if val < 0 or val > 100
     */
    public static int writeTwoDigits(int val, char[] chars, int off) {
        UnsafeHelper.putInt(chars, off, TWO_DIGITS_32_BITS[val]);
        return 2;
    }

    /**
     * 写入2位数字字节，不够2位前面补0
     *
     * @param val
     * @param buf
     * @param off
     * @return 2
     * @throws IndexOutOfBoundsException if val < 0 or val > 100
     */
    public static int writeTwoDigits(int val, byte[] buf, int off) {
        UnsafeHelper.putShort(buf, off, TWO_DIGITS_16_BITS[val]);
        return 2;
    }

    /**
     * 一次性写入pre + 2位数字字符 + suff到字符数组(长度4个字符)
     *
     * @param val
     * @return 4
     */
    public static int writeTwoDigitsAndPreSuffix(int val, char pre, char suff, char[] chars, int off) {
        long longVal;
        if (EnvUtils.BIG_ENDIAN) {
            longVal = ((long) pre) << 48 | ((long) TWO_DIGITS_32_BITS[val]) << 16 | suff;
        } else {
            longVal = ((long) suff) << 48 | ((long) TWO_DIGITS_32_BITS[val]) << 16 | pre;
        }
        UnsafeHelper.putLong(chars, off, longVal);
        return 4;
    }

    /**
     * 一次性写入pre + 2位数字字符 + suff到字节数组(长度4个字节)
     *
     * @param val
     * @return 4
     */
    public static int writeTwoDigitsAndPreSuffix(int val, char pre, char suff, byte[] chars, int off) {
        int intVal;
        if (EnvUtils.BIG_ENDIAN) {
            intVal = (pre << 24) | (TWO_DIGITS_16_BITS[val] << 8) | suff;
        } else {
            intVal = (suff << 24) | (TWO_DIGITS_16_BITS[val] << 8) | pre;
        }
        UnsafeHelper.putInt(chars, off, intVal);
        return 4;
    }

    /**
     * ensure val >= 0 && chars.length > off + 19
     *
     * @param val
     * @param chars
     * @param off
     * @return
     */
    public static int writePositiveLong(long val, char[] chars, int off) {
        final int beginIndex = off;
        int v, v1, v2, v3, v4;
        if (val < 10000) {
            v = (int) val;
            if (v < 1000) {
                off += writeThreeDigits(v, chars, off);
            } else {
                off += UnsafeHelper.putLong(chars, off, FOUR_DIGITS_64_BITS[v]);
            }
            return off - beginIndex;
        }

        long numValue = val;
        val = numValue / 10000;
        v1 = (int) (numValue - val * 10000);
        if (val < 10000) {
            v = (int) val;
            if (v < 1000) {
                off += writeThreeDigits(v, chars, off);
            } else {
                off += UnsafeHelper.putLong(chars, off, FOUR_DIGITS_64_BITS[v]);
            }
            off += UnsafeHelper.putLong(chars, off, FOUR_DIGITS_64_BITS[v1]);
            return off - beginIndex;
        }

        numValue = val;
        val = numValue / 10000;
        v2 = (int) (numValue - val * 10000);
        if (val < 10000) {
            v = (int) val;
            if (v < 1000) {
                off += writeThreeDigits(v, chars, off);
            } else {
                off += UnsafeHelper.putLong(chars, off, FOUR_DIGITS_64_BITS[v]);
            }
            off += UnsafeHelper.putLong(chars, off, FOUR_DIGITS_64_BITS[v2]);
            off += UnsafeHelper.putLong(chars, off, FOUR_DIGITS_64_BITS[v1]);
            return off - beginIndex;
        }

        numValue = val;
        val = numValue / 10000;
        v3 = (int) (numValue - val * 10000);
        if (val < 10000) {
            v = (int) val;
            if (v < 1000) {
                off += writeThreeDigits(v, chars, off);
            } else {
                off += UnsafeHelper.putLong(chars, off, FOUR_DIGITS_64_BITS[v]);
            }
            off += UnsafeHelper.putLong(chars, off, FOUR_DIGITS_64_BITS[v3]);
            off += UnsafeHelper.putLong(chars, off, FOUR_DIGITS_64_BITS[v2]);
            off += UnsafeHelper.putLong(chars, off, FOUR_DIGITS_64_BITS[v1]);
            return off - beginIndex;
        }

        numValue = val;
        val = numValue / 10000;
        v4 = (int) (numValue - val * 10000);
        if (val < 10000) {
            v = (int) val;
            if (v < 1000) {
                off += writeThreeDigits(v, chars, off);
            } else {
                off += UnsafeHelper.putLong(chars, off, FOUR_DIGITS_64_BITS[v]);
            }
            off += UnsafeHelper.putLong(chars, off, FOUR_DIGITS_64_BITS[v4]);
            off += UnsafeHelper.putLong(chars, off, FOUR_DIGITS_64_BITS[v3]);
            off += UnsafeHelper.putLong(chars, off, FOUR_DIGITS_64_BITS[v2]);
            off += UnsafeHelper.putLong(chars, off, FOUR_DIGITS_64_BITS[v1]);
            return off - beginIndex;
        }
        return off - beginIndex;
    }

    /**
     * ensure val >= 0 && chars.length > off + 19
     *
     * @param val
     * @param buf
     * @param off
     * @return
     */
    public static int writePositiveLong(long val, byte[] buf, int off) {
        final int beginIndex = off;
        int v, v1, v2, v3, v4;
        if (val < 10000) {
            v = (int) val;
            if (v < 1000) {
                off += writeThreeDigits(v, buf, off);
            } else {
                off += UnsafeHelper.putInt(buf, off, FOUR_DIGITS_32_BITS[v]);
            }
            return off - beginIndex;
        }

        long numValue = val;
        val = numValue / 10000;
        v1 = (int) (numValue - val * 10000);
        if (val < 10000) {
            v = (int) val;
            if (v < 1000) {
                off += writeThreeDigits(v, buf, off);
            } else {
                off += UnsafeHelper.putInt(buf, off, FOUR_DIGITS_32_BITS[v]);
            }
            off += UnsafeHelper.putInt(buf, off, FOUR_DIGITS_32_BITS[v1]);
            return off - beginIndex;
        }

        numValue = val;
        val = numValue / 10000;
        v2 = (int) (numValue - val * 10000);
        if (val < 10000) {
            v = (int) val;
            if (v < 1000) {
                off += writeThreeDigits(v, buf, off);
            } else {
                off += UnsafeHelper.putInt(buf, off, FOUR_DIGITS_32_BITS[v]);
            }
            off += UnsafeHelper.putInt(buf, off, FOUR_DIGITS_32_BITS[v2]);
            off += UnsafeHelper.putInt(buf, off, FOUR_DIGITS_32_BITS[v1]);
            return off - beginIndex;
        }

        numValue = val;
        val = numValue / 10000;
        v3 = (int) (numValue - val * 10000);
        if (val < 10000) {
            v = (int) val;
            if (v < 1000) {
                off += writeThreeDigits(v, buf, off);
            } else {
                off += UnsafeHelper.putInt(buf, off, FOUR_DIGITS_32_BITS[v]);
            }
            off += UnsafeHelper.putInt(buf, off, FOUR_DIGITS_32_BITS[v3]);
            off += UnsafeHelper.putInt(buf, off, FOUR_DIGITS_32_BITS[v2]);
            off += UnsafeHelper.putInt(buf, off, FOUR_DIGITS_32_BITS[v1]);
            return off - beginIndex;
        }

        numValue = val;
        val = numValue / 10000;
        v4 = (int) (numValue - val * 10000);
        if (val < 10000) {
            v = (int) val;
            if (v < 1000) {
                off += writeThreeDigits(v, buf, off);
            } else {
                off += UnsafeHelper.putInt(buf, off, FOUR_DIGITS_32_BITS[v]);
            }
            off += UnsafeHelper.putInt(buf, off, FOUR_DIGITS_32_BITS[v4]);
            off += UnsafeHelper.putInt(buf, off, FOUR_DIGITS_32_BITS[v3]);
            off += UnsafeHelper.putInt(buf, off, FOUR_DIGITS_32_BITS[v2]);
            off += UnsafeHelper.putInt(buf, off, FOUR_DIGITS_32_BITS[v1]);
            return off - beginIndex;
        }
        return off - beginIndex;
    }

//    /**
//     * ensure chars.length > off + 19
//     *
//     * @param val
//     * @param chars
//     * @param off
//     * @return
//     */
//    public static int writePositiveLong1(long val, char[] chars, int off) {
//        final int beginIndex = off;
//        int v, v1, v2, v3, v4, v5, v6, v7, v8, v9;
//        if (val < 100) {
//            v = (int) val;
//            if (v > 9) {
//                chars[off++] = DigitTens[v];
//            }
//            chars[off++] = DigitOnes[v];
//            return off - beginIndex;
//        }
//
//        long numValue = val;
//        val = numValue / 100;
//        v1 = (int) (numValue - val * 100);
//        if (val < 100) {
//            v = (int) val;
//            if (v > 9) {
//                chars[off++] = DigitTens[v];
//            }
//            chars[off++] = DigitOnes[v];
//            chars[off++] = DigitTens[v1];
//            chars[off++] = DigitOnes[v1];
//            return off - beginIndex;
//        }
//
//        numValue = val;
//        val = numValue / 100;
//        v2 = (int) (numValue - val * 100);
//        if (val < 100) {
//            v = (int) val;
//            if (v > 9) {
//                chars[off++] = DigitTens[v];
//            }
//            chars[off++] = DigitOnes[v];
//            chars[off++] = DigitTens[v2];
//            chars[off++] = DigitOnes[v2];
//            chars[off++] = DigitTens[v1];
//            chars[off++] = DigitOnes[v1];
//            return off - beginIndex;
//        }
//
//        numValue = val;
//        val = numValue / 100;
//        v3 = (int) (numValue - val * 100);
//        if (val < 100) {
//            v = (int) val;
//            if (v > 9) {
//                chars[off++] = DigitTens[v];
//            }
//            chars[off++] = DigitOnes[v];
//            chars[off++] = DigitTens[v3];
//            chars[off++] = DigitOnes[v3];
//            chars[off++] = DigitTens[v2];
//            chars[off++] = DigitOnes[v2];
//            chars[off++] = DigitTens[v1];
//            chars[off++] = DigitOnes[v1];
//            return off - beginIndex;
//        }
//
//        numValue = val;
//        val = numValue / 100;
//        v4 = (int) (numValue - val * 100);
//        if (val < 100) {
//            v = (int) val;
//            if (v > 9) {
//                chars[off++] = DigitTens[v];
//            }
//            chars[off++] = DigitOnes[v];
//            chars[off++] = DigitTens[v4];
//            chars[off++] = DigitOnes[v4];
//            chars[off++] = DigitTens[v3];
//            chars[off++] = DigitOnes[v3];
//            chars[off++] = DigitTens[v2];
//            chars[off++] = DigitOnes[v2];
//            chars[off++] = DigitTens[v1];
//            chars[off++] = DigitOnes[v1];
//            return off - beginIndex;
//        }
//
//        numValue = val;
//        val = numValue / 100;
//        v5 = (int) (numValue - val * 100);
//        if (val < 100) {
//            v = (int) val;
//            if (v > 9) {
//                chars[off++] = DigitTens[v];
//            }
//            chars[off++] = DigitOnes[v];
//            chars[off++] = DigitTens[v5];
//            chars[off++] = DigitOnes[v5];
//            chars[off++] = DigitTens[v4];
//            chars[off++] = DigitOnes[v4];
//            chars[off++] = DigitTens[v3];
//            chars[off++] = DigitOnes[v3];
//            chars[off++] = DigitTens[v2];
//            chars[off++] = DigitOnes[v2];
//            chars[off++] = DigitTens[v1];
//            chars[off++] = DigitOnes[v1];
//            return off - beginIndex;
//        }
//
//        numValue = val;
//        val = numValue / 100;
//        v6 = (int) (numValue - val * 100);
//        if (val < 100) {
//            v = (int) val;
//            if (v > 9) {
//                chars[off++] = DigitTens[v];
//            }
//            chars[off++] = DigitOnes[v];
//            chars[off++] = DigitTens[v6];
//            chars[off++] = DigitOnes[v6];
//            chars[off++] = DigitTens[v5];
//            chars[off++] = DigitOnes[v5];
//            chars[off++] = DigitTens[v4];
//            chars[off++] = DigitOnes[v4];
//            chars[off++] = DigitTens[v3];
//            chars[off++] = DigitOnes[v3];
//            chars[off++] = DigitTens[v2];
//            chars[off++] = DigitOnes[v2];
//            chars[off++] = DigitTens[v1];
//            chars[off++] = DigitOnes[v1];
//            return off - beginIndex;
//        }
//
//        numValue = val;
//        val = numValue / 100;
//        v7 = (int) (numValue - val * 100);
//        if (val < 100) {
//            v = (int) val;
//            if (v > 9) {
//                chars[off++] = DigitTens[v];
//            }
//            chars[off++] = DigitOnes[v];
//            chars[off++] = DigitTens[v7];
//            chars[off++] = DigitOnes[v7];
//            chars[off++] = DigitTens[v6];
//            chars[off++] = DigitOnes[v6];
//            chars[off++] = DigitTens[v5];
//            chars[off++] = DigitOnes[v5];
//            chars[off++] = DigitTens[v4];
//            chars[off++] = DigitOnes[v4];
//            chars[off++] = DigitTens[v3];
//            chars[off++] = DigitOnes[v3];
//            chars[off++] = DigitTens[v2];
//            chars[off++] = DigitOnes[v2];
//            chars[off++] = DigitTens[v1];
//            chars[off++] = DigitOnes[v1];
//            return off - beginIndex;
//        }
//
//        numValue = val;
//        val = numValue / 100;
//        v8 = (int) (numValue - val * 100);
//        if (val < 100) {
//            v = (int) val;
//            if (v > 9) {
//                chars[off++] = DigitTens[v];
//            }
//            chars[off++] = DigitOnes[v];
//            chars[off++] = DigitTens[v8];
//            chars[off++] = DigitOnes[v8];
//            chars[off++] = DigitTens[v7];
//            chars[off++] = DigitOnes[v7];
//            chars[off++] = DigitTens[v6];
//            chars[off++] = DigitOnes[v6];
//            chars[off++] = DigitTens[v5];
//            chars[off++] = DigitOnes[v5];
//            chars[off++] = DigitTens[v4];
//            chars[off++] = DigitOnes[v4];
//            chars[off++] = DigitTens[v3];
//            chars[off++] = DigitOnes[v3];
//            chars[off++] = DigitTens[v2];
//            chars[off++] = DigitOnes[v2];
//            chars[off++] = DigitTens[v1];
//            chars[off++] = DigitOnes[v1];
//            return off - beginIndex;
//        }
//
//        numValue = val;
//        val = numValue / 100;
//        v9 = (int) (numValue - val * 100);
//        if (val < 100) {
//            v = (int) val;
//            if (v > 9) {
//                chars[off++] = DigitTens[v];
//            }
//            chars[off++] = DigitOnes[v];
//            chars[off++] = DigitTens[v9];
//            chars[off++] = DigitOnes[v9];
//            chars[off++] = DigitTens[v8];
//            chars[off++] = DigitOnes[v8];
//            chars[off++] = DigitTens[v7];
//            chars[off++] = DigitOnes[v7];
//            chars[off++] = DigitTens[v6];
//            chars[off++] = DigitOnes[v6];
//            chars[off++] = DigitTens[v5];
//            chars[off++] = DigitOnes[v5];
//            chars[off++] = DigitTens[v4];
//            chars[off++] = DigitOnes[v4];
//            chars[off++] = DigitTens[v3];
//            chars[off++] = DigitOnes[v3];
//            chars[off++] = DigitTens[v2];
//            chars[off++] = DigitOnes[v2];
//            chars[off++] = DigitTens[v1];
//            chars[off++] = DigitOnes[v1];
//            return off - beginIndex;
//        }
//
//        return off - beginIndex;
//    }

    public static int hex(int c) {
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
                return c - '0';
            case 'a':
            case 'b':
            case 'c':
            case 'd':
            case 'e':
            case 'f':
                return c - 'a' + 10;
            case 'A':
            case 'B':
            case 'C':
            case 'D':
            case 'E':
            case 'F':
                return c - 'A' + 10;
            default: {
                throw new IllegalArgumentException("invalid hex char " + (char) c);
            }
        }
    }
}
