package io.github.wycst.wast.common.utils;

import java.util.Arrays;

/**
 * @Author: wangy(wycst)
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

//    final static byte[] HEX_DIGITS_REVERSE = {0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 0, 0, 0, 0, 0, 0, 0, 10, 11, 12, 13, 14, 15, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 10, 11, 12, 13, 14, 15};

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
//    final static BigInteger[] POW5_BI_VALUES = new BigInteger[343];

    static {
        // e0 ~ e360(e306)
        for (int i = 0, len = POSITIVE_DECIMAL_POWER.length; i < len; ++i) {
            POSITIVE_DECIMAL_POWER[i] = Double.parseDouble("1.0E" + i);
            NEGATIVE_DECIMAL_POWER[i] = Double.parseDouble("1.0E-" + i);
        }
        // 4.9e-324
        NEGATIVE_DECIMAL_POWER[NEGATIVE_DECIMAL_POWER.length - 1] = Double.MIN_VALUE;

        long val = 1;
        for (int i = 0; i < POW5_LONG_VALUES.length; ++i) {
            POW5_LONG_VALUES[i] = val;
            val *= 5;
        }
    }

    static final int MOD_DOUBLE_EXP = (1 << 11) - 1;
    static final int MOD_FLOAT_EXP = (1 << 8) - 1;
    static final long MOD_DOUBLE_MANTISSA = (1L << 52) - 1;
    static final int MOD_FLOAT_MANTISSA = (1 << 23) - 1;

    //    static final long MASK_32_BITS = 0xffffffffL;
    public static char[] copyDigitOnes() {
        return Arrays.copyOf(DigitOnes, DigitOnes.length);
    }

    public static char[] copyDigitTens() {
        return Arrays.copyOf(DigitTens, DigitTens.length);
    }

//    /**
//     * 实时获取5的指定次方对应的BigInteger对象，减少初始化POW5_BI_VALUES内存占用
//     *
//     * @param e5
//     * @return
//     */
//    static BigInteger pow5_bi(int e5) {
//        BigInteger pow5 = POW5_BI_VALUES[e5];
//        if (pow5 == null) {
//            pow5 = BigInteger.valueOf(5).pow(e5);
//            POW5_BI_VALUES[e5] = pow5;
//        }
//        return pow5;
//    }

    /**
     * 获取以10为底数的指定数值（-1 < expValue > 310）指数值,
     *
     * @param expValue ensure expValue >= 0
     */
    public static double getDecimalPowerValue(int expValue) {
        if (expValue < POSITIVE_DECIMAL_POWER.length) {
            return POSITIVE_DECIMAL_POWER[expValue];
        }
        return Math.pow(10, expValue);
    }

    /**
     * 获取long值的字符串长度
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
     * 字符转数字
     */
    public static int digit(int c) {
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
                return c & 0xF;
            default:
                return -1;
        }
    }

    /**
     * 转化为4位十进制数int数字
     */
    public static int parseInt4(char[] buf, int fromIndex)
            throws NumberFormatException {
        return parseInt4(buf[fromIndex++], buf[fromIndex++], buf[fromIndex++], buf[fromIndex]);
    }

    /**
     * 转化为4位十进制数int数字
     *
     * @param buf    源字节数组
     * @param offset 读取的起始位置
     * @return 4位十进制数
     */
    public static int parseInt4(byte[] buf, int offset)
            throws NumberFormatException {
        return parseInt4(buf[offset++], buf[offset++], buf[offset++], buf[offset]);
    }

    /**
     * 转化为4位十进制数
     *
     * @param c1 千位
     * @param c2 百位
     * @param c3 十位
     * @param c4 个位
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
     * @param buf    源字节数组
     * @param offset 读取的起始位置
     * @return 转换后的数字
     */
    public static int parseInt2(char[] buf, int offset)
            throws NumberFormatException {
        return parseInt2(buf[offset++], buf[offset]);
    }

    /**
     * 转化为2位十进制数
     *
     * @param buf    源字节数组
     * @param offset 读取的起始位置
     * @return 转换后的数字
     */
    public static int parseInt2(byte[] buf, int offset)
            throws NumberFormatException {
        return parseInt2(buf[offset++], buf[offset]);
    }

    /**
     * 转化为2位十进制数
     *
     * @param c1 十位
     * @param c2 个位
     * @return 转换后的数字
     * @throws NumberFormatException 输入的字符串不是数字
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
     * @param buf    源字节数组
     * @param offset 读取的起始位置
     * @return 1位数字
     */
    public static int parseInt1(char[] buf, int offset)
            throws NumberFormatException {
        int v1 = digitDecimal(buf[offset]);
        if (v1 == -1) {
            throw new NumberFormatException("For input string: \"" + new String(buf, offset, 1) + "\"");
        }
        return v1;
    }

    /**
     * 转化为1位int数字
     *
     * @param buf    源字节数组
     * @param offset 读取的起始位置
     * @return 1位数字
     */
    public static int parseInt1(byte[] buf, int offset)
            throws NumberFormatException {
        int v1 = digitDecimal(buf[offset]);
        if (v1 == -1) {
            throw new NumberFormatException("For input string: \"" + new String(buf, offset, 1) + "\"");
        }
        return v1;
    }

    /**
     * 将long类型的value转为长度为16的16进制字符串,缺省补字符0
     *
     * @param value long value
     * @return 16进制字符串
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
     * <p> Convert to double through 64 bit integer val and precision scale -> val / 10^scale;</p>
     *
     * <p> to simplify the code, code blocks that are considered to have an extremely low probability of occurrence have been temporarily removed (due to the inability to fully test within the double range).</p>
     * <p> if any errors(up to one bit error) occur as a result, please provide the relevant parameters(the val and scale) and contact me(shallxiao@126.com).</p>
     *
     * <p>
     * 注: 之前的版本缓存了一个BigInteger数组（POW5_BI_VALUES）存储5的指数幂，能确保结果100%正确. 考虑到占用内存有点大且只有极小概率可达就删除了. <br/>
     * 简化后的代码通常情况下结果是正确的，如果有发现错误（一个bit的误差）请发送到我的邮箱(shallxiao@126.com)
     * </p>
     *
     * <p> IEEEF Double 64 bits: 1 + 11 + 52  </p>
     * <p> ensure the parameter val is greater than 0, otherwise return 0 </p>
     *
     * @param val   value
     * @param scale precision
     * @author wycst
     */
    public static double scientificToIEEEDouble(long val, int scale) {
        if (val < 1) {
            if (val == Long.MIN_VALUE) {
                val = 9223372036854775807L;
            } else return 0.0D;
        }
        int leadingZeros = Long.numberOfLeadingZeros(val);
        long y, y32, e0;
        if (scale < 1) {
            if (scale == 0) return val;
            if (scale > -23 && val < 9007199254740993L) return val * POSITIVE_DECIMAL_POWER[-scale]; // 1L << 53
            if (scale < -308) return Double.POSITIVE_INFINITY;
            ED5 ed5 = ED5.ED5_A[-scale];
            y = ed5.y;
            y32 = ed5.f + 1;
            e0 = 1140 + ed5.dfb;
        } else {
            if (scale > 342) return 0.0D;
            ED5 ed5 = ED5.ED5_A[scale];
            y = ed5.oy;
            y32 = ed5.of + 1;
            e0 = 1108 - ed5.ob;
        }
        long x = val << (leadingZeros - 1);
        long h = EnvUtils.JDK_AGENT_INSTANCE.multiplyHighKaratsuba(x, y), l; // h is 61~62 bits
        int sr = h > 0x1fffffffffffffffL ? 9 : 8;
        long e2 = e0 - scale - leadingZeros + sr;  // e52 + 1075
        if (e2 < 1) {
            if ((sr += 1 - (int) e2) > 61) return 0.0D;
            e2 = 0;
        } else if (e2 > 2046) return Double.POSITIVE_INFINITY;
        long mmask = (1L << sr) - 1, mask = mmask >> 1;
        if ((h & mmask) != mask || (l = x * y) > 0 || !checkLowCarry(l, x, y32)) {
            return longBitsToDouble(h, e2, sr);
        }
        return longBitsToDouble(h + 1, e2, sr);
    }

    // check whether a carry-over may occur in the lower bits
    private static boolean checkLowCarry(long l, long x, long y32) {
        return l + ((EnvUtils.JDK_AGENT_INSTANCE.multiplyHighKaratsuba(x, y32) << 32) + ((x * y32) >>> 32)) > -1;
    }

    static double longBitsToDouble(long l62, long e2, int sr) {
        long mantissa0 = ((l62 >>> sr - 1) + 1) >> 1;
        if (mantissa0 == 1L << 53) {
            mantissa0 = 1L << 52;
            ++e2;
        }
        long bits = (e2 << 52) | (mantissa0 & MOD_DOUBLE_MANTISSA);
        return Double.longBitsToDouble(bits);
    }

    /**
     * Convert to float through 64 bit integer val and precision scale -> val / 10^scale
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
     * @param val   ensure val > 0, otherwise return 0
     * @param scale the scale of the number
     * @return float
     * @author wycst
     */
    public static float scientificToIEEEFloat(long val, int scale) {
        if (val < 1) {
            return 0.0f;
        }
        double dv;
        float val0;
        if (scale < 1) {
            if (scale == 0) return (float) val;
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

    /**
     * multiplyOutput
     *
     * @param x     > 0
     * @param y     > 0
     * @param shift > 0
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
     * @param doubleValue best to ensure that the doubleValue > 0 before call.
     *                    otherwise the absolute value of Scientific notation will be returned if doubleValue < 0.
     *                    if doubleValue is NaN or POSIFINETY or NEGATIVE-INFINETY, it will directly return SCIENTIFIC_NULL
     *                    if doubleValue == 0, return SCIENTIFIC_ZERO or SCIENTIFIC_NEGATIVE_ZERO
     */
    public static Scientific doubleToScientific(double doubleValue) {
        if (doubleValue == Double.MIN_VALUE) {
            // Double.MIN_VALUE JDK转化最小double为4.9e-324， 本方法转化为5.0e-324 , 由于此值特殊为了和JDK转化一致
            return Scientific.DOUBLE_MIN;
        }
        long bits = Double.doubleToRawLongBits(doubleValue);
        int e2 = (int) (bits >> 52) & MOD_DOUBLE_EXP;
        long mantissa0 = bits & MOD_DOUBLE_MANTISSA;
        if (mantissa0 == 0) {
            return bits != 0x8000000000000000L ? ScientificMantissaZeroTable.DOUBLE_MANTISSA_ZERO_TABLE[e2] : Scientific.NEGATIVE_ZERO;
        }
        int e52;
        long output;
        long rawOutput, /*d2, */d3, d4;
        int e10, adl;
        if (e2 > 0) {
            // Double.NaN/Double.POSITIVE_INFINITY/Double.NEGATIVE_INFINITY -> NULL
            if (e2 == 2047) return Scientific.SCIENTIFIC_NULL;
            mantissa0 = 1L << 52 | mantissa0;
            e52 = e2 - 1075;
        } else {
            int lz52 = Long.numberOfLeadingZeros(mantissa0) - 11;
            mantissa0 <<= lz52;
            e52 = -1074 - lz52;
        }
        // boolean /*tflag = true,*/ accurate = false;
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
                // accurate = o5 == -1 && sb < 11;
            } else {
                // o5 > 0 -> sb > 0
                // accurate
                rawOutput = (mantissa0 * POW5_LONG_VALUES[o5]) << sb;
                // accurate = true;
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
        // rem <= Actual Rem Value
//        long div = EnvUtils.JDK_AGENT_INSTANCE.multiplyHighKaratsuba(rawOutput, 0x4189374bc6a7ef9eL) >> 8; // rawOutput / 1000;
        long div = rawOutput / 1000;
        long rem = rawOutput - div * 1000;
        boolean down;
        if ((down = ((rem + 1) << 1) <= d3) || ((10001 - rem * 10) << 1) <= d4) {
            output = div + (down ? 0 : 1);
            return new Scientific(output, adl, e10);
        } else {
            int scale = -e10 + adl - 1;
            if (scientificToIEEEDouble(output = rem > 500 ? div + 1 : div, scale) == doubleValue) {
                return new Scientific(output, adl, e10);
            }
            output = (rawOutput + 50) / 100;
            return new Scientific(output, adl + 1, e10);
        }
    }

    /**
     * ieee float to Scientific notation.
     * better to ensure that floatValue>0.
     * <p>
     * if floatValue is less than 0, the absolute value of Scientific notation will be return
     *
     * @param floatValue != 0
     */
    public static Scientific floatToScientific(float floatValue) {
        final int bits = Float.floatToRawIntBits(floatValue);
        int e2 = (bits >> 23) & MOD_FLOAT_EXP;
        int mantissa0 = bits & MOD_FLOAT_MANTISSA;
        if (mantissa0 == 0) {
            return bits != 0x80000000 ? ScientificMantissaZeroTable.FLOAT_MANTISSA_ZERO_TABLE[e2] : Scientific.NEGATIVE_ZERO;
        }
        int e23;
        long output, rawOutput;
        long d4;
        int e10, adl;
        if (e2 > 0) {
            if (e2 == MOD_FLOAT_EXP) return Scientific.SCIENTIFIC_NULL;
            mantissa0 = 1 << 23 | mantissa0;
            e23 = e2 - 150;   // - 127 - 23
        } else {
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
                if (sb < 40) {
                    rawOutput = ((long) mantissa0 << sb) / POW5_LONG_VALUES[-o5];
                } else {
                    ED5 d5 = ED5.ED5_A[-o5];
                    rawOutput = multiplyHighAndShift((long) mantissa0 << 39, d5.oy, d5.of, 71 + d5.ob - sb);
                }
            } else {
                // o5 > 0 -> sb > 0
                // accurate
                rawOutput = mantissa0 * POW5_LONG_VALUES[o5] << sb;
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
                if (o5 < 17) {
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
            }
        }
        if (rawOutput < 1000000000) {
            return new Scientific(EnvUtils.JDK_AGENT_INSTANCE.multiplyHighKaratsuba(rawOutput, 0x6b5fca6af2bd215fL) >> 22, 2, e10); // rawOutput / 10000000
        }
        long div = EnvUtils.JDK_AGENT_INSTANCE.multiplyHighKaratsuba(rawOutput, 0x44b82fa09b5a52ccL) >> 28; // rawOutput / 1000000000;
        long rem = rawOutput - div * 1000000000;
        long remUp = (1000000001 - rem) << 1;
        boolean up = remUp <= d4;
        if (up || ((rem + 1) << 1) <= d4) {
            output = div + (up ? 1 : 0);
            if (up && POW10_LONG_VALUES[adl - 1] == output) {
                return new Scientific(1, 1, e10 + 1);
            }
            return new Scientific(output, adl, e10);
        } else {
            int scale = -e10 + adl - 1;
            if (scientificToIEEEFloat(output = rem > 500000000 ? div + 1 : div, scale) == floatValue /*|| scientificToIEEEFloat(output = div, scale) == floatValue*/) {
                return new Scientific(output, adl, e10);
            }
            long div0 = EnvUtils.JDK_AGENT_INSTANCE.multiplyHighKaratsuba(rawOutput, 0x55e63b88c230e77fL) >> 25; // rawOutput / 100000000
            output = div0 + (rem % 100000000 >= 50000000 ? 1 : 0);
            return new Scientific(output, adl + 1, e10);
        }
    }

    public static boolean equals(long val, String text) {
        if (text == null || text.isEmpty()) return false;
        if (val == Long.MIN_VALUE) {
            return text.equals("-9223372036854775808");
        }
        long result = 0, len = text.length();
        int i = 0;
        if (text.charAt(0) == '-') {
            ++i;
            val = -val;
        }
        for (; i < len; ++i) {
            int d = digitDecimal(text.charAt(i));
            if (d == -1) {
                return false;
            }
            result = result * 10 + d;
        }
        return val == result;
    }
}
