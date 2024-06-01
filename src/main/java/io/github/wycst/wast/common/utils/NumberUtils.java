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

    final static long[] POW10_LONG_VALUES = new long[]{
            10, 100, 1000, 10000, 100000, 1000000, 10000000, 100000000, 1000000000, 10000000000L, 100000000000L, 1000000000000L, 10000000000000L, 100000000000000L, 1000000000000000L, 10000000000000000L, 100000000000000000L, 1000000000000000000L, 9223372036854775807L
    };

    final static long[] POW10_OPPOSITE_VALUES = new long[]{
            0x6666666666666667L, 0x51eb851eb851eb86L, 0x4189374bc6a7ef9eL, 0x68db8bac710cb296L, 0x53e2d6238da3c212L, 0x431bde82d7b634dbL, 0x6b5fca6af2bd215fL, 0x55e63b88c230e77fL, 0x44b82fa09b5a52ccL, 0x6df37f675ef6eae0L, 0x57f5ff85e5925580L, 0x465e6604b7a84466L, 0x709709a125da070aL, 0x5a126e1a84ae6c08L, 0x480ebe7b9d58566dL, 0x734aca5f6226f0aeL, 0x5c3bd5191b525a25L, 0x49c97747490eae84L, 0x4000000000000001L
    };

    final static byte[] POW10_OPPOSITE_RB = new byte[]{
            2, 5, 8, 12, 15, 18, 22, 25, 28, 32, 35, 38, 42, 45, 48, 52, 55, 58, 61
    };

    final static BigInteger BI_TO_DECIMAL_BASE = BigInteger.valueOf(1000000000L);
    final static BigInteger BI_MAX_VALUE_FOR_LONG = BigInteger.valueOf(Long.MAX_VALUE);

    //0-9a-f
    final static char[] HEX_DIGITS = {
            '0', '1', '2', '3', '4', '5',
            '6', '7', '8', '9', 'a', 'b',
            'c', 'd', 'e', 'f'
    };

    final static int[] HEX_DIGITS_INT32 = new int[256];

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
    final static BigInteger[] POW5_BI_VALUES = new BigInteger[343];
//    final static BigInteger[] POW10_BI_VALUES = new BigInteger[308];

    final static ThreadLocal<char[]> THREAD_LOCAL_CHARS = new ThreadLocal<char[]>() {
        @Override
        protected char[] initialValue() {
            return new char[24];
        }
    };

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

        BigInteger five = BigInteger.valueOf(5);
        POW5_BI_VALUES[0] = BigInteger.ONE;
        for (int i = 1; i < POW5_BI_VALUES.length; ++i) {
            BigInteger pow5Value = five.pow(i);
            POW5_BI_VALUES[i] = pow5Value;
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

        // 0-255 for HEX
        for (int b = 0; b < 256; b++) {
            int b1 = b >> 4, b2 = b & 0xF;
            HEX_DIGITS_INT32[b] = EnvUtils.BIG_ENDIAN ? (HEX_DIGITS[b1] << 16 | HEX_DIGITS[b2]) : (HEX_DIGITS[b2] << 16 | HEX_DIGITS[b1]);
        }
    }

    static final int MOD_DOUBLE_EXP = (1 << 12) - 1;
    static final int MOD_FLOAT_EXP = (1 << 9) - 1;
    static final long MOD_DOUBLE_MANTISSA = (1L << 52) - 1;
    static final int MOD_FLOAT_MANTISSA = (1 << 23) - 1;
    static final long MASK_32_BITS = 0xffffffffL;
    // Math.log2(10)
//    static final double LOG2_10 = 3.321928094887362;


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
        long v1, v2, v3, v4;
        int b1 = (int) (mostSigBits >>> 56 & 0xff), b2 = (int) (mostSigBits >>> 48 & 0xff), b3 = (int) (mostSigBits >>> 40 & 0xff), b4 = (int) (mostSigBits >>> 32 & 0xff);
        int b5 = (int) (mostSigBits >>> 24 & 0xff), b6 = (int) (mostSigBits >>> 16 & 0xff), b7 = (int) (mostSigBits >>> 8 & 0xff), b8 = (int) (mostSigBits & 0xff);
        if (EnvUtils.BIG_ENDIAN) {
            v1 = (long) HEX_DIGITS_INT32[b1] << 32 | HEX_DIGITS_INT32[b2];
            v2 = (long) HEX_DIGITS_INT32[b3] << 32 | HEX_DIGITS_INT32[b4];
            v3 = (long) HEX_DIGITS_INT32[b5] << 32 | HEX_DIGITS_INT32[b6];
            v4 = (long) HEX_DIGITS_INT32[b7] << 32 | HEX_DIGITS_INT32[b8];
        } else {
            v1 = (long) HEX_DIGITS_INT32[b2] << 32 | HEX_DIGITS_INT32[b1];
            v2 = (long) HEX_DIGITS_INT32[b4] << 32 | HEX_DIGITS_INT32[b3];
            v3 = (long) HEX_DIGITS_INT32[b6] << 32 | HEX_DIGITS_INT32[b5];
            v4 = (long) HEX_DIGITS_INT32[b8] << 32 | HEX_DIGITS_INT32[b7];
        }
        UnsafeHelper.putLong(buf, offset, v1);
        offset += 4;
        UnsafeHelper.putLong(buf, offset, v2);
        offset += 4;
        buf[offset++] = '-';
        UnsafeHelper.putLong(buf, offset, v3);
        offset += 4;
        buf[offset++] = '-';
        UnsafeHelper.putLong(buf, offset, v4);
        return 18;
    }

    public static int writeUUIDLeastSignificantBits(long leastSigBits, char[] buf, int offset) {
        long v1, v2, v3, v4;
        int b1 = (int) (leastSigBits >>> 56 & 0xff), b2 = (int) (leastSigBits >>> 48 & 0xff), b3 = (int) (leastSigBits >>> 40 & 0xff), b4 = (int) (leastSigBits >>> 32 & 0xff);
        int b5 = (int) (leastSigBits >>> 24 & 0xff), b6 = (int) (leastSigBits >>> 16 & 0xff), b7 = (int) (leastSigBits >>> 8 & 0xff), b8 = (int) (leastSigBits & 0xff);
        if (EnvUtils.BIG_ENDIAN) {
            v1 = (long) HEX_DIGITS_INT32[b1] << 32 | HEX_DIGITS_INT32[b2];
            v2 = (long) HEX_DIGITS_INT32[b3] << 32 | HEX_DIGITS_INT32[b4];
            v3 = (long) HEX_DIGITS_INT32[b5] << 32 | HEX_DIGITS_INT32[b6];
            v4 = (long) HEX_DIGITS_INT32[b7] << 32 | HEX_DIGITS_INT32[b8];
        } else {
            v1 = (long) HEX_DIGITS_INT32[b2] << 32 | HEX_DIGITS_INT32[b1];
            v2 = (long) HEX_DIGITS_INT32[b4] << 32 | HEX_DIGITS_INT32[b3];
            v3 = (long) HEX_DIGITS_INT32[b6] << 32 | HEX_DIGITS_INT32[b5];
            v4 = (long) HEX_DIGITS_INT32[b8] << 32 | HEX_DIGITS_INT32[b7];
        }
        buf[offset++] = '-';
        UnsafeHelper.putLong(buf, offset, v1);
        offset += 4;
        buf[offset++] = '-';
        UnsafeHelper.putLong(buf, offset, v2);
        offset += 4;
        UnsafeHelper.putLong(buf, offset, v3);
        offset += 4;
        UnsafeHelper.putLong(buf, offset, v4);
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

//    static BigInteger pow10Bi(int p10) {
//        BigInteger pow10Bi = POW10_BI_VALUES[p10];
//        if (pow10Bi == null) {
//            POW10_BI_VALUES[p10] = pow10Bi = POW5_BI_VALUES[p10].shiftLeft(p10);
//        }
//        return pow10Bi;
//    }
//
//    static long multiHigh64(long val, int[] mag) {
//        return multiHigh64(val, mag, 0);
//    }
//
//    /**
//     * Accelerate operations
//     *
//     * @param val
//     * @param mag
//     * @return
//     */
//    static long multiHigh64(long val, int[] mag, int ro) {
//        long a1 = val >> 32, a2 = val & MASK_32_BITS;
//        if (a1 == 0) {
//            a1 = a2;
//            a2 = 0;
//        }
//        long b1, b2, b3, b4 = 0;
//        if (mag.length == 2) {
//            b1 = 0;
//            b2 = mag[0] & MASK_32_BITS;
//            b3 = mag[1] & MASK_32_BITS;
//        } else {
//            b1 = mag[0] & MASK_32_BITS;
//            b2 = mag[1] & MASK_32_BITS;
//            b3 = mag[2] & MASK_32_BITS;
//            if (mag.length > 3) {
//                b4 = mag[3] & MASK_32_BITS;
//            }
//        }
//        long carry = a2 * b4 >>> 32;
//        long h4 = a1 * b4 + carry;
//        carry = h4 >>> 32;
//        h4 = (h4 & MASK_32_BITS) + a2 * b3;
//        carry += h4 >>> 32;
//        long h3 = a1 * b3 + carry;
//        carry = h3 >>> 32;
//        h3 = (h3 & MASK_32_BITS) + a2 * b2;
//        carry += h3 >>> 32;
//        long h2 = a1 * b2 + carry;
//        carry = h2 >>> 32;
//        h2 = (h2 & MASK_32_BITS) + a2 * b1;
//        carry += h2 >>> 32;
//        long h1 = a1 * b1 + carry;
//        carry = h1 >>> 32;
//        if (ro == 1) {
//            if (carry == 0) {
//                return (h2 & MASK_32_BITS) << 32 | (h3 & MASK_32_BITS);
//            } else {
//                return (h1 & MASK_32_BITS) << 32 | (h2 & MASK_32_BITS);
//            }
//        }
//        if (carry == 0) {
//            return (h1 & MASK_32_BITS) << 32 | (h2 & MASK_32_BITS);
//        }
//        return carry << 32 | (h1 & MASK_32_BITS);
//    }
//    static long multiHighWithFactor(long val, int[] mag, int factor) {
//        long a1 = val >> 32, a2 = val & MASK_32_BITS;
//        if (a1 == 0) {
//            a1 = a2;
//            a2 = 0;
//        }
//        long b1, b2, b3, b4 = 0;
//        if (mag.length == 2) {
//            b1 = 0;
//            b2 = mag[0] & MASK_32_BITS;
//            b3 = (mag[1] & MASK_32_BITS);
//        } else {
//            b1 = mag[0] & MASK_32_BITS;
//            b2 = mag[1] & MASK_32_BITS;
//            b3 = mag[2] & MASK_32_BITS;
//            if (mag.length > 3) {
//                b4 = (mag[3] & MASK_32_BITS) + factor;
//            } else {
//                b3 += factor;
//            }
//        }
//        long carry = a2 * b4 >>> 32;
//        long h4 = a1 * b4 + carry;
//        carry = h4 >>> 32;
//        h4 = (h4 & MASK_32_BITS) + a2 * b3;
//        carry += h4 >>> 32;
//        long h3 = a1 * b3 + carry;
//        carry = h3 >>> 32;
//        h3 = (h3 & MASK_32_BITS) + a2 * b2;
//        carry += h3 >>> 32;
//        long h2 = a1 * b2 + carry;
//        carry = h2 >>> 32;
//        h2 = (h2 & MASK_32_BITS) + a2 * b1;
//        carry += h2 >>> 32;
//        long h1 = a1 * b1 + carry;
//        carry = h1 >>> 32;
//        if (carry == 0) {
//            return (h1 & MASK_32_BITS) << 32 | (h2 & MASK_32_BITS);
//        }
//        return carry << 32 | (h1 & MASK_32_BITS);
//    }

    static int compareProductHigh(long val, int[] mag, long targetHigh64, long targetLow32) {
        long a1 = val >> 32, a2 = val & MASK_32_BITS;
        if (a1 == 0) {
            a1 = a2;
            a2 = 0;
        }
        long b1, b2, b3, b4 = 0;
        if (mag.length == 2) {
            b1 = 0;
            b2 = mag[0] & MASK_32_BITS;
            b3 = (mag[1] & MASK_32_BITS);
        } else {
            b1 = mag[0] & MASK_32_BITS;
            b2 = mag[1] & MASK_32_BITS;
            b3 = mag[2] & MASK_32_BITS;
            if (mag.length > 3) {
                b4 = (mag[3] & MASK_32_BITS);
            }
        }
        long carry = a2 * b4 >>> 32;
        long h4 = a1 * b4 + carry;
        carry = h4 >>> 32;
        h4 = (h4 & MASK_32_BITS) + a2 * b3;
        carry += h4 >>> 32;
        long h3 = a1 * b3 + carry;
        carry = h3 >>> 32;
        h3 = (h3 & MASK_32_BITS) + a2 * b2;
        carry += h3 >>> 32;
        long h2 = a1 * b2 + carry;
        carry = h2 >>> 32;
        h2 = (h2 & MASK_32_BITS) + a2 * b1;
        carry += h2 >>> 32;
        long h1 = a1 * b1 + carry;
        carry = h1 >>> 32;

        long resultHigh64, resultLow32;
        if (carry == 0) {
            resultHigh64 = (h1 & MASK_32_BITS) << 32 | (h2 & MASK_32_BITS);
            resultLow32 = h3 & MASK_32_BITS;
        } else {
            resultHigh64 = carry << 32 | (h1 & MASK_32_BITS);
            resultLow32 = h2 & MASK_32_BITS;
        }
        if (resultHigh64 - targetHigh64 >= 0) {
            if (resultHigh64 == targetHigh64) {
                if (resultLow32 >= targetLow32) {
                    return 1;
                }
                if (targetLow32 - resultLow32 > 1) {
                    return -1;
                }
                return 0;
            }
            return 1;
        }
        return -1;
    }

    static double exactlyDoubleWithDiff(double val0, long bits, long mantissa0, long diff, long target) {
        long doubleDiff = Math.abs(diff << 1);
        if (doubleDiff < target) {
            return val0;
        }
        if (diff < 0) {
            // the estimated value is less than the accurate value
            diff = -diff;
            while (diff >= target) {
                ++bits;
                diff -= target;
            }
            diff = diff << 1;
            if (diff > target || (diff == target && (mantissa0 & 1) == 1)) {
                return Double.longBitsToDouble(bits + 1);
            }
            return Double.longBitsToDouble(bits);
        } else {
            // The estimated value is greater than the accurate value
            while (diff >= target) {
                --bits;
                diff -= target;
            }
            diff <<= 1;
            if (target > diff || (target == diff && (mantissa0 & 1) == 0)) {
                return Double.longBitsToDouble(bits);
            } else {
                return Double.longBitsToDouble(bits - 1);
            }
        }
    }

    /**
     * Convert to double through 64 bit integer val and precision scale
     *
     * <p> IEEEF Double 64 bits: 1 + 11 + 52  </p>
     * <p> using the difference estimation method </p>
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
        double val0;
        if (scale <= 0) {
            if (scale == 0) return dv;
            int e10 = -scale;
            if (e10 > 308) return Double.POSITIVE_INFINITY;
            if ((long) dv == val && e10 < 23) {
                return dv * NumberUtils.getDecimalPowerValue(e10);
            }
            BigInteger multiplier = POW5_BI_VALUES[e10];
            int multiBitLength = multiplier.bitLength();
            int dt = multiBitLength + (64 - leadingZeros) - 53;
            if (dt < 62) {
                val0 = dv * NumberUtils.getDecimalPowerValue(e10);
                if (val0 == Double.POSITIVE_INFINITY) return Double.POSITIVE_INFINITY;
                long bits = Double.doubleToLongBits(val0);
                long mantissa0 = 1L << 52 | (bits & MOD_DOUBLE_MANTISSA);
                int e2 = (int) (bits >> 52) & MOD_DOUBLE_EXP;
                int sb = e2 - 1075 - e10;
                if (sb > 0) {
                    if (e10 >= POW5_LONG_VALUES.length) {
                        return exactlyDoubleWithDiff(val0, bits, mantissa0, (mantissa0 << sb) - (val * POW5_LONG_VALUES[e10 - POW5_LONG_VALUES.length + 1] * POW5_LONG_VALUES[POW5_LONG_VALUES.length - 1]), 1l << sb);
                    } else {
                        return exactlyDoubleWithDiff(val0, bits, mantissa0, (mantissa0 << sb) - val * POW5_LONG_VALUES[e10], 1l << sb);
                    }
                } else {
                    // Perhaps it does not exist or appears with a small probability, but it has not been proven yet
                    return exactlyDoubleWithDiff(val0, bits, mantissa0, mantissa0 - (val * POW5_LONG_VALUES[e10] << -sb), 1L);
                }
            }
            ED5 ed5 = ED5.ED5_A[e10];
            long left = val << (leadingZeros - 1);
            int e52 = e10 + 1 - leadingZeros;
            long output;
            if (e10 < POW5_LONG_VALUES.length) {
                output = multiplyOutput(left, POW5_LONG_VALUES[e10], multiBitLength);
                e52 += multiBitLength;
            } else {
                output = multiplyfOutput(left, ed5.y, ed5.f + 1, 64);
                e52 += ed5.dfb + 64;
            }
            int sr = 11 - Long.numberOfLeadingZeros(output);
            long mantissa0 = output >>> sr;
            e52 += sr;
            if (e52 >= 972) return Double.POSITIVE_INFINITY;
            long remMask = (1L << sr) - 1, rem = output & remMask, half = (remMask >> 1) + 1;
            if (rem > half) {
                ++mantissa0;
            } else {
                if (rem == half) {
                    boolean oddFlag = (mantissa0 & 1) == 1;
                    long rightSum = (mantissa0 << 1) + 1;
                    int sb = 1 - e52 + e10; // sb < 0

                    int remBits = -sb & 63, rlz = Long.numberOfLeadingZeros(rightSum);
//                    long rightHigh64 = rlz >= remBits ? rightSum << remBits : rightSum >> (64 - remBits);
//                    long rLow32;
//                    if ((rightHigh64 >>> 32) == 0) {
//                        long rLow64 = rightSum << remBits;
//                        rightHigh64 = (rightHigh64 << 32) | (rLow64 >>> 32);
//                        rLow32 = rLow64 & MASK_32_BITS;
//                    } else {
//                        rLow32 = 0;
//                    }

                    long rightHigh64, rLow32;
                    if(remBits == 0) {
                        rightHigh64 = rightSum;
                        rLow32 = 0;
                    } else {
                        long hr = rightSum >> (64 - remBits), lr = rightSum << remBits;
                        if(hr == 0) {
                            rightHigh64 = lr;
                            rLow32 = 0;
                        } else {
                            if ((hr >>> 32) == 0) {
                                rightHigh64 = (hr << 32) | (lr >>> 32);
                                rLow32 = lr & MASK_32_BITS;
                            } else {
                                rightHigh64 = hr;
                                rLow32 = lr >>> 32;
                            }
                        }
                    }

                    // diff high
                    int compareFlag = compareProductHigh(val, UnsafeHelper.getMag(multiplier), rightHigh64, rLow32);
                    if (compareFlag != 0) {
                        if (compareFlag == 1) {
                            ++mantissa0;
                        }
                    } else {
                        // use BigInteger Accurate comparison
                        long diff = BigInteger.valueOf(val).multiply(multiplier).compareTo(BigInteger.valueOf(rightSum).shiftLeft(-sb));
                        if (diff > 0 || (diff == 0 && oddFlag)) {
                            ++mantissa0;
                        }
                    }
                }
            }
            long e2 = e52 + 1075;
            long bits = (e2 << 52) | (mantissa0 & MOD_DOUBLE_MANTISSA);
            return Double.longBitsToDouble(bits);
////          The code implementation logic in the following comments is also correct, but the probability of hitting an efficient judgment is relatively low when the e10 value reaches a certain range
//            val0 = dv * NumberUtils.getDecimalPowerValue(e10);
//            if (val0 == Double.POSITIVE_INFINITY) return Double.POSITIVE_INFINITY;
//            long bits = Double.doubleToLongBits(val0);
//            long mantissa0 = 1L << 52 | (bits & MOD_DOUBLE_MANTISSA);
//            int e2 = (int) (bits >> 52) & MOD_DOUBLE_EXP;
//            int sb = e2 - 1075 - e10;
//            if (sb < 62) {
//                if (sb > 0) {
//                    if (e10 >= POW5_LONG_VALUES.length) {
//                        return exactlyDoubleWithDiff(val0, bits, mantissa0, (mantissa0 << sb) - (val * POW5_LONG_VALUES[e10 - POW5_LONG_VALUES.length + 1] * POW5_LONG_VALUES[POW5_LONG_VALUES.length - 1]), 1l << sb);
//                    } else {
//                        return exactlyDoubleWithDiff(val0, bits, mantissa0, (mantissa0 << sb) - val * POW5_LONG_VALUES[e10], 1l << sb);
//                    }
//                } else {
//                    // Perhaps it does not exist or appears with a small probability, but it has not been proven yet
//                    return exactlyDoubleWithDiff(val0, bits, mantissa0, mantissa0 - (val * POW5_LONG_VALUES[e10] << -sb), 1L);
//                }
//            } else {
//                BigInteger pow5Bi = POW5_BI_VALUES[e10];
//                int[] mag = UnsafeHelper.getMag(pow5Bi);
//                int rb = sb & 31;
//                if (rb == 0) {
//                    rb = 32;
//                }
//                int srb = sb - rb;
//                long r64 = mantissa0 << rb;
//                long target = 1l << rb;
//                long v64 = 0;
//                int vb = 63 - Long.numberOfLeadingZeros(val) + pow5Bi.bitLength() - srb;
//                boolean estimatedSupported = false;
//                if (vb - 1 > 64 && rb > 11) {
//                    v64 = multiHigh64(val, mag, 1);
//                    estimatedSupported = true; // v64 >> 63 == r64 >> 63 && (v64 != Long.MIN_VALUE && v64 != Long.MAX_VALUE) ;
//                } else if (vb < 64 && rb < 11) {
//                    v64 = multiHigh64(val, mag);
//                    estimatedSupported = true; // v64 != Long.MAX_VALUE;
//                }
//                if (estimatedSupported) {
//                    long v64Plus = v64 + 1;
//                    if (r64 > v64Plus) {
//                        long diffH64Plus = r64 - v64;
//                        long doubleDiffH64 = Math.abs(diffH64Plus << 1);
//                        if (doubleDiffH64 < target) {
//                            return val0;
//                        }
//                        long diffH64 = r64 - v64Plus;
//                        if (diffH64 > target) {
//                            long bits0 = bits;
//                            do {
//                                --bits0;
//                                diffH64 -= target;
//                            } while (diffH64 > target);
//                            if (diffH64 << 1 > target) {
//                                return Double.longBitsToDouble(bits0 - 1);
//                            }
//                            if ((diffH64) << 1 < target) {
//                                return Double.longBitsToDouble(bits0);
//                            }
//                        } else if (diffH64Plus < target) {
//                            if (diffH64 << 1 >= target) {
//                                return Double.longBitsToDouble(bits - 1);
//                            }
//                            if (diffH64Plus << 1 <= target) {
//                                return Double.longBitsToDouble(bits);
//                            }
//                        }
//                    } else if (r64 < v64) {
//                        long diffH64Plus = v64Plus - r64;
//                        long doubleDiffH64 = Math.abs(diffH64Plus << 1);
//                        if (doubleDiffH64 < target) {
//                            return val0;
//                        }
//                        long diffH64 = v64 - r64;
//                        if (diffH64 > target) {
//                            long bits0 = bits;
//                            do {
//                                ++bits0;
//                                diffH64 -= target;
//                            } while (diffH64 > target);
//                            if (diffH64 << 1 >= target) {
//                                return Double.longBitsToDouble(bits0 + 1);
//                            }
//                            if (diffH64 << 1 < target) {
//                                return Double.longBitsToDouble(bits0);
//                            }
//                        } else if (diffH64Plus < target) {
//                            if (diffH64 << 1 >= target) {
//                                return Double.longBitsToDouble(bits + 1);
//                            }
//                            if (diffH64Plus << 1 < target) {
//                                return Double.longBitsToDouble(bits);
//                            }
//                        }
//                    }
//                }
//            }
//            return pow10Bi(e10).multiply(BigInteger.valueOf(val)).doubleValue();
        } else {
            if (scale > 342) return 0.0D;
            if ((long) dv == val && scale < 23) {
                return dv / NumberUtils.getDecimalPowerValue(scale);
            }
            if (scale < POW5_LONG_VALUES.length) {
                val0 = dv / NumberUtils.getDecimalPowerValue(scale);
                long bits = Double.doubleToLongBits(val0);
                if (bits == 0) return 0.0D;
                int e2 = (int) (bits >> 52) & MOD_DOUBLE_EXP;
                int sb;
                long mantissa0 = bits & MOD_DOUBLE_MANTISSA;
                if (e2 > 0) {
                    mantissa0 = 1L << 52 | mantissa0;
                    sb = 1075 - scale - e2;
                } else {
                    sb = 1074 - scale;
                }
                if (sb > 0) {
                    long p5sv = POW5_LONG_VALUES[scale];
                    return exactlyDoubleWithDiff(val0, bits, mantissa0, mantissa0 * p5sv - (sb >= 64 ? 0 : val << sb), p5sv);
                } else {
                    long p = POW5_LONG_VALUES[scale] << (-sb);
                    return exactlyDoubleWithDiff(val0, bits, mantissa0, mantissa0 * p - val, p);
                }
            }

            ED5 ed5 = ED5.ED5_A[scale];
            BigInteger divisor = POW5_BI_VALUES[scale];
            int srb = divisor.bitLength() + leadingZeros - 1;
            // the output Greater than the true value by diff in [0, 1)
            long output = multiplyfOutput(val, ed5.oy, ed5.of + 1, ed5.ob + 32 - srb);
            int sr = output < 0 ? 11 : 10;
            int e52 = -scale - srb + sr;
            long e2;
            if (e52 < -1074) {
                sr += -1074 - e52;
                e2 = 0;
                if (sr >= 64) {
                    if (sr == 64 && output < 0) {
                        return Double.MIN_VALUE;
                    }
                    return 0.0D;
                }
            } else {
                e2 = e52 + 1075;
            }
            // 53bits
            long mantissa0 = output >>> sr;
            long remMask = (1L << sr) - 1, rem = output & remMask, half = (remMask >> 1) + 1;
            if (rem > half) {
                ++mantissa0;
            } else {
                boolean oddFlag = (mantissa0 & 1) == 1;
                if (rem == half) {
//                    long outputMin = multiplyfOutput(val, ed5.oy, ed5.of, ed5.ob + 32 - srb);
//                    if (outputMin == output) {
//                        ++mantissa0;
//                    } else {
//                    }
                    long rightSum = (mantissa0 << 1) + 1;
                    int sb = 1 - e52 - scale;

                    int remBits = sb & 63;
                    long rightHigh64, rLow32;
                    if(remBits == 0) {
                        rightHigh64 = val;
                        rLow32 = 0;
                    } else {
                        long hr = val >> (64 - remBits), lr = val << remBits;
                        if(hr == 0) {
                            rightHigh64 = lr;
                            rLow32 = 0;
                        } else {
                            if ((hr >>> 32) == 0) {
                                rightHigh64 = (hr << 32) | (lr >>> 32);
                                rLow32 = lr & MASK_32_BITS;
                            } else {
                                rightHigh64 = hr;
                                rLow32 = lr >>> 32;
                            }
                        }
                    }

                    // diff high
                    int compareFlag = compareProductHigh(rightSum, UnsafeHelper.getMag(divisor), rightHigh64, rLow32);
                    if (compareFlag != 0) {
                        if (compareFlag == -1) {
                            ++mantissa0;
                        }
                    } else {
                        long diff = BigInteger.valueOf(val).shiftLeft(sb).compareTo(divisor.multiply(BigInteger.valueOf(rightSum)));
                        if (diff > 0 || (diff == 0 && oddFlag)) {
                            ++mantissa0;
                        }
                    }
                }
            }
            long bits = (e2 << 52) | (mantissa0 & MOD_DOUBLE_MANTISSA);
            return Double.longBitsToDouble(bits);

////          The code implementation logic in the following comments is also correct, but the probability of hitting an efficient judgment is relatively low when the e10 value reaches a certain range
//
//            if (scale >= 308) {
//                val0 = dv / NumberUtils.getDecimalPowerValue(308) / NumberUtils.getDecimalPowerValue(scale - 308);
//            } else {
//                val0 = dv / NumberUtils.getDecimalPowerValue(scale);
//            }
//            long bits = Double.doubleToLongBits(val0);
//            if (bits == 0) return 0.0D;
//            int e2 = (int) (bits >> 52) & MOD_DOUBLE_EXP;
//            int sb;
//            long mantissa0 = bits & MOD_DOUBLE_MANTISSA;
//            // boolean maxMantissaFlag = mantissa0 == MOD_DOUBLE_MANTISSA;
//            if (e2 > 0) {
//                mantissa0 = 1L << 52 | mantissa0;
//                sb = 1075 - scale - e2;
//            } else {
//                sb = 1074 - scale;
//            }
//            if (sb > 0) {
//                if (scale < POW5_LONG_VALUES.length) {
//                    long p5sv = POW5_LONG_VALUES[scale];
//                    return exactlyDoubleWithDiff(val0, bits, mantissa0, mantissa0 * p5sv - (sb >= 64 ? 0 : val << sb), p5sv);
//                }
//                BigInteger pow5Bi = POW5_BI_VALUES[scale];
//                int tb = 64 - Long.numberOfLeadingZeros(val) + sb;
//                int rem = tb & 31;
//                int srb = rem == 0 ? tb - 64 : tb - (rem + 32);
//                int p5hbits = pow5Bi.bitLength() - srb;
//                boolean srsFlag = sb >= srb;
//                if ((srsFlag || Long.numberOfTrailingZeros(val) >= (srb - sb)) && p5hbits > 0 && p5hbits < 62) {
//                    long v64 = srsFlag ? val << (sb - srb) : val >> (srb - sb);
//                    int[] mag = UnsafeHelper.getMag(pow5Bi);
//                    int magn = mag.length - (srb >> 5);
//                    long r64 = multiHigh64(mantissa0, mag);
//                    long r64Plus = r64 + 1;
//                    long p5h64;
//                    if (magn == 1) {
//                        p5h64 = mag[0] & MASK_32_BITS;
//                    } else if (magn == 2) {
//                        p5h64 = (mag[0] & MASK_32_BITS) << 32 | (mag[1] & MASK_32_BITS);
//                    } else {
//                        p5h64 = 0;
//                    }
//                    long p5h64Plus = p5h64 + 1;
//                    if (r64 > v64) {
//                        long diffH64Plus = r64Plus - v64;
//                        long doubleDiffH64 = Math.abs(diffH64Plus << 1);
//                        if (doubleDiffH64 <= p5h64) {
//                            return val0;
//                        }
//                        long diffH64 = r64 - v64;
//                        if (diffH64 > p5h64Plus) {
//                            long bits0 = bits;
//                            int n = 0;
//                            do {
//                                --bits0;
//                                diffH64 -= p5h64Plus;
//                                ++n;
//                            } while (diffH64 > p5h64Plus);
//                            if (diffH64 << 1 > p5h64Plus) {
//                                return Double.longBitsToDouble(bits0 - 1);
//                            }
//                            if ((diffH64 + n * 2) << 1 < p5h64 && p5h64 > n) {
//                                return Double.longBitsToDouble(bits0);
//                            }
//                        } else if (diffH64Plus < p5h64) {
//                            if (diffH64 << 1 >= p5h64Plus) {
//                                return Double.longBitsToDouble(bits - 1);
//                            }
//                            if (diffH64Plus << 1 <= p5h64) {
//                                return Double.longBitsToDouble(bits);
//                            }
//                        }
//                    } else if (r64Plus < v64) {
//                        long diffH64Plus = v64 - r64;
//                        long doubleDiffH64 = Math.abs(diffH64Plus << 1);
//                        if (doubleDiffH64 <= p5h64) {
//                            return val0;
//                        }
//                        long diffH64 = v64 - r64Plus;
//                        if (diffH64 > p5h64Plus) {
//                            long bits0 = bits;
//                            int n = 0;
//                            do {
//                                ++bits0;
//                                diffH64 -= p5h64Plus;
//                                ++n;
//                            } while (diffH64 > p5h64Plus);
//                            if (diffH64 << 1 > p5h64Plus) {
//                                return Double.longBitsToDouble(bits0 + 1);
//                            }
//                            if ((diffH64 + n * 2) << 1 < p5h64 && p5h64 > n) {
//                                return Double.longBitsToDouble(bits0);
//                            }
//                        } else if (diffH64Plus < p5h64) {
//                            if (diffH64 << 1 >= p5h64Plus) {
//                                return Double.longBitsToDouble(bits + 1);
//                            }
//                            if (diffH64Plus << 1 <= p5h64) {
//                                return Double.longBitsToDouble(bits);
//                            }
//                        }
//                    }
//                } else {
//                    // todo
//                    // The missed conditions can still be further optimized
//                }
//                BigInteger rbi = BigInteger.valueOf(mantissa0).multiply(pow5Bi);
//                BigInteger vbi = BigInteger.valueOf(val).shiftLeft(sb);
//                BigInteger dbi = rbi.subtract(vbi);
//                BigInteger ddbi = dbi.shiftLeft(1).abs();
//                if (ddbi.compareTo(pow5Bi) == -1) {
//                    return val0;
//                }
//                int signnum = dbi.signum();
//                if (signnum == -1) {
//                    dbi = dbi.negate();
//                    if (dbi.compareTo(pow5Bi) >= 0) {
//                        do {
//                            ++bits;
//                            dbi = dbi.subtract(pow5Bi);
//                        } while (dbi.compareTo(pow5Bi) >= 0);
//                        dbi = dbi.shiftLeft(1);
//                        int compareFlag = dbi.compareTo(pow5Bi);
//                        if (compareFlag > 0 || (compareFlag == 0 && (mantissa0 & 1) == 1)) {
//                            return Double.longBitsToDouble(bits + 1);
//                        } else {
//                            return Double.longBitsToDouble(bits);
//                        }
//                    } else {
//                        return Double.longBitsToDouble(bits + 1);
//                    }
//                } else {
//                    if (dbi.compareTo(pow5Bi) >= 0) {
//                        do {
//                            --bits;
//                            dbi = dbi.subtract(pow5Bi);
//                        } while (dbi.compareTo(pow5Bi) >= 0);
//                        dbi = dbi.shiftLeft(1);
//                        int compareFlag = pow5Bi.compareTo(dbi);
//                        if (compareFlag > 0 || (compareFlag == 0 && (mantissa0 & 1) == 0)) {
//                            return Double.longBitsToDouble(bits);
//                        } else {
//                            return Double.longBitsToDouble(bits - 1);
//                        }
//                    } else {
//                        return Double.longBitsToDouble(bits - 1);
//                    }
//                }
//            } else {
//                long p = POW5_LONG_VALUES[scale] << (-sb);
//                return exactlyDoubleWithDiff(val0, bits, mantissa0, mantissa0 * p - val, p);
//            }
        }
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

    /**
     * Ensure x is greater than 0
     *
     * @param x
     * @param index(0,17)
     * @return
     */
    static long divPowTen(long x, int index) {
        // Use Karatsuba technique with two base 2^32 digits.
        long y = POW10_OPPOSITE_VALUES[index];
        long x1 = x >>> 32;
        long y1 = y >>> 32;
        long x2 = x & 0xFFFFFFFFL;
        long y2 = y & 0xFFFFFFFFL;
        long A = x1 * y1;
        long B = x2 * y2;
        long C = (x1 + x2) * (y1 + y2);
        long K = C - A - B;
        long H = (((B >>> 32) + K) >>> 32) + A;
        return H >> POW10_OPPOSITE_RB[index];
    }

    /**
     * multiplyOutput
     *
     * @param x     > 0
     * @param y     > 0
     * @param shift > 0
     * @return
     */
    static long multiplyOutput(long x, long y, int shift) {
        long x1 = x >>> 32, x2 = x & MASK_32_BITS;
        long y1 = y >>> 32, y2 = y & MASK_32_BITS;
        long A = x1 * y1;
        long B = x2 * y2;
        long C = (x1 + x2) * (y1 + y2);
        // karatsuba
        long K = C - A - B;
        long BC = B >>> 32;
        long H = ((BC + K) >>> 32) + A;
        if (shift >= 64) {
            int sr = shift - 64;
            return H >>> sr;
        }
        long L = (((K & MASK_32_BITS) + BC) << 32) | (B & MASK_32_BITS);
        int rn = 64 - shift;
        return H << rn | (L >>> shift);
    }

    /**
     * Unsigned algorithm:  (H * 2^64 * 2^n + L * 2^n + H1 * 2^64 + L1) / 2^(n+s) = (H * 2^64 + L + (H1 * 2^64 + L1) / 2^n) / 2^s
     * <p>
     * use BigInteger:  BigInteger.valueOf(pd.y).shiftLeft(n).add(BigInteger.valueOf(f & MASK_32_BITS)).multiply(BigInteger.valueOf(x)).shiftRight(s + n).longValue()
     *
     * @param x 63bits
     * @param y 63bits
     * @param f unsigned int32 f(32bits)
     * @param s s > 0
     * @return
     */
    static long multiplyfOutput(long x, long y, int f, int s) {
        int sr = s - 64;
        // multiply x * y
        long x1 = x >>> 32, x2 = x & MASK_32_BITS;
        long y1 = y >>> 32, y2 = y & MASK_32_BITS;
        long A = x1 * y1;
        long B = x2 * y2;
        long C = (x1 + x2) * (y1 + y2);
        // karatsuba
        long K = C - A - B;
        long BC = B >>> 32;
        long H = ((BC + K) >>> 32) + A;
        long L = (((K & MASK_32_BITS) + BC) << 32) | (B & MASK_32_BITS);

        // cal x * f -> H1, L1
        long fl = f & MASK_32_BITS;
        long B1 = x2 * fl;
        long K1 = x1 * fl;
        long BC1 = B1 >>> 32;
        long H1 = (BC1 + K1) >>> 32;
        long L1 = (((K1 & MASK_32_BITS) + BC1) << 32) | (B1 & MASK_32_BITS);

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
     * @param doubleValue != 0
     * @return
     */
    static Scientific doubleToScientific(double doubleValue) {
        long bits = Double.doubleToRawLongBits(doubleValue);
        int e2 = (int) (bits >> 52) & MOD_DOUBLE_EXP;
        long mantissa0 = bits & MOD_DOUBLE_MANTISSA;
        // boolean flagForUp = mantissa0 < MOD_DOUBLE_MANTISSA;
        boolean flagForDown = mantissa0 > 0;
        int e52;
        long output;
        long rawOutput, d2, d3, d4;
        int e10, adl;
        if (e2 > 0) {
            mantissa0 = 1L << 52 | mantissa0;
            e52 = e2 - 1075;
        } else {
            int lz52 = Long.numberOfLeadingZeros(mantissa0) - 11;
            mantissa0 <<= lz52;
            e52 = -1074 - lz52;
        }
        boolean tflag = true, accurate = false;
        if (e52 >= 0) {
            ED d = ED.E2_D_A[e52];
            e10 = d.e10;   // e10 > 15
            adl = d.adl;
            d2 = d.d2;
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
                rawOutput = multiplyfOutput(mantissa0 << 10, d5.oy, d5.of, 32 - rb);
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
            d2 = d.d2;
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
                    rawOutput = multiplyOutput(mantissa0, ED5.ED5_A[o5].y, -sb);
                } else if (o5 < POW5_LONG_VALUES.length + 4) {
                    rawOutput = multiplyOutput(mantissa0 * POW5_LONG_VALUES[o5 - POW5_LONG_VALUES.length + 1], ED5.ED5_A[POW5_LONG_VALUES.length - 1].y, -sb);
                } else {
                    ED5 ed5 = ED5.ED5_A[o5];
                    rawOutput = multiplyfOutput(mantissa0 << 10, ed5.y, ed5.f, -(ed5.dfb + sb) + 10);
                }
            } else {
                rawOutput = POW5_LONG_VALUES[o5] * mantissa0 << sb;
            }
        }
        if (accurate) {
            rawOutput = rawOutput / 10;
            if (adl == 16) {
                --adl;
                long rem = rawOutput % 10;
                rawOutput = rawOutput / 10 + (rem >= 5 ? 1 : 0);
            }
            return new Scientific(rawOutput, adl + 2, e10);
        }
        if (rawOutput < 0) {
            rawOutput = (rawOutput >>> 1) / 5;
            tflag = false;
        }
        long div, rem;
        if (tflag) {
            // rem <= Actual Rem Value
            div = rawOutput / 1000;
            rem = rawOutput - div * 1000;
            long remUp = (10001 - rem * 10) << 1;
            boolean up;
            if ((up = (remUp <= d4)) || ((rem + 1) << (flagForDown ? 1 : 2)) <= d3) {
                output = div + (up ? 1 : 0);
                --adl;
            } else {
                if (flagForDown) {
                    div = rawOutput / 100;
                    rem = rawOutput - div * 100;
                    output = div + (rem >= 50 ? 1 : 0);
                } else {
                    div = rawOutput / 10;
                    rem = rawOutput - div * 10;
                    output = div + (rem >= 5 ? 1 : 0);
                    ++adl;
                }
            }
        } else {
            rem = rawOutput % 100;
            long remUp = (101 - rem) << 1;
            boolean up;
            if ((up = (remUp <= d2)) || (rem + 1) << (flagForDown ? 1 : 2) <= d2) {
                output = rawOutput / 100 + (up ? 1 : 0);
                --adl;
            } else {
                if (flagForDown) {
                    output = rawOutput / 10 + (rem % 10 >= 5 ? 1 : 0);
                } else {
                    output = rawOutput;
                    ++adl;
                }
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
    static Scientific floatToScientific(float floatValue) {
        int bits = Float.floatToRawIntBits(floatValue);
        int e2 = (bits >> 23) & MOD_FLOAT_EXP;
        int mantissa0 = bits & MOD_FLOAT_MANTISSA;
        boolean nonZeroFlag = mantissa0 > 0;
        int e23;
        long output;
        long d4;
        int e10, adl;
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
            e10 = d.e10;   // e10 > 15
            adl = d.adl;
            d4 = d.d4;
            if (d.b && mantissa0 > d.bv) {
                ++e10;
                ++adl;
            }
        } else {
            // e52 >= -1074 -> p5 <= 1074
            int e5 = -e23;
            ED d = EF.E5_F_A[e5];
            e10 = d.e10;
            adl = d.adl;
            d4 = d.d4;
            if (d.b && mantissa0 > d.bv) {
                ++e10;
                ++adl;
            }
        }

        long rawOutput;
        int rn = adl + 5 - e10;
        double dv = floatValue;
        if (rn >= 0) {
            rawOutput = (long) (dv * NumberUtils.getDecimalPowerValue(rn));
        } else {
            rawOutput = (long) (dv / NumberUtils.getDecimalPowerValue(-rn));
        }
        long div = divPowTen(rawOutput, 5); // rawOutput / 1000000
        long rem = rawOutput - div * 1000000;
        long remUp = (1000001 - rem) << 1;
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
                output = rawOutput / 100000 + (rem % 100000 >= 50000 ? 1 : 0);
            } else {
                output = rawOutput / 10000 + (rem % 10000 >= 5000 ? 1 : 0);
                ++adl;
            }
        }

        return new Scientific(output, adl + 1, e10);
    }

    /**
     * 将double值写入到字符数组
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

        if (doubleValue == (long) doubleValue) {
            long output = (long) doubleValue;
            int numLength = stringSize(output);
            return writeDecimal(output, numLength, numLength - 1, buf, beginIndex, off);
        }

        Scientific scientific = doubleToScientific(doubleValue);
        int e10 = scientific.e10;
        if (!scientific.b) {
            return writeDecimal(scientific.output, scientific.count, e10, buf, beginIndex, off);
        }
        if (e10 >= 0) {
            char[] chars = POSITIVE_DECIMAL_POWER_CHARS[e10];
            System.arraycopy(chars, 0, buf, off, chars.length);
            off += chars.length;
            return off - beginIndex;
        } else {
            char[] chars = NEGATIVE_DECIMAL_POWER_CHARS[-e10];
            System.arraycopy(chars, 0, buf, off, chars.length);
            off += chars.length;
            return off - beginIndex;
        }
    }

    /**
     * float写入buf
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

        Scientific scientific = floatToScientific(floatValue);
        return writeDecimal(scientific.output, scientific.count, scientific.e10, buf, beginIndex, off);
    }

    private static int writeDecimal(long value, int digitCnt, int e10, char[] buf, int beginIndex, int off) {
        if ((value & 1) == 0 && value % 5 == 0) {
            while (value % 100 == 0) {
                digitCnt -= 2;
                value /= 100;
                if (digitCnt == 1) break;
            }
            if ((value & 1) == 0 && value % 5 == 0) {
                if (value > 0) {
                    --digitCnt;
                    value /= 10;
                }
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
                long tl = POW10_LONG_VALUES[pos];
                int fd = (int) divPowTen(value, pos); // (value / tl);
                buf[off++] = (char) (fd + 48);
                buf[off++] = '.';

                long pointAfter = value - fd * tl;
                // 补0
                while (--pos > -1 && pointAfter < POW10_LONG_VALUES[pos]) {
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
                    long tl = POW10_LONG_VALUES[pos];
                    int pointBefore = (int) divPowTen(value, pos); // (value / tl);
                    off += writePositiveLong(pointBefore, buf, off);
                    buf[off++] = '.';
                    long pointAfter = value - pointBefore * tl;
                    // 补0
                    while (--pos > -1 && pointAfter < POW10_LONG_VALUES[pos]) {
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

        if (doubleValue == (long) doubleValue) {
            long output = (long) doubleValue;
            int numLength = stringSize(output);
            return writeDecimal(output, numLength, numLength - 1, buf, beginIndex, off);
        }

        Scientific scientific = doubleToScientific(doubleValue);
        int e10 = scientific.e10;
        if (!scientific.b) {
            return writeDecimal(scientific.output, scientific.count, scientific.e10, buf, beginIndex, off);
        }
        if (e10 >= 0) {
            char[] chars = POSITIVE_DECIMAL_POWER_CHARS[e10];
            for (char c : chars) {
                buf[off++] = (byte) c;
            }
            return off - beginIndex;
        } else {
            char[] chars = NEGATIVE_DECIMAL_POWER_CHARS[-e10];
            for (char c : chars) {
                buf[off++] = (byte) c;
            }
            return off - beginIndex;
        }
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

        Scientific scientific = floatToScientific(floatValue);
        return writeDecimal(scientific.output, scientific.count, scientific.e10, buf, beginIndex, off);
    }

    private static int writeDecimal(long value, int digitCnt, int e10, byte[] buf, int beginIndex, int off) {
        if ((value & 1) == 0 && value % 5 == 0) {
            while (value % 100 == 0) {
                digitCnt -= 2;
                value /= 100;
                if (digitCnt == 1) break;
            }
            if ((value & 1) == 0 && value % 5 == 0) {
                if (value > 0) {
                    --digitCnt;
                    value /= 10;
                }
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
                long tl = POW10_LONG_VALUES[pos];
                int fd = (int) (value / tl);
                buf[off++] = (byte) (fd + 48);
                buf[off++] = '.';

                long pointAfter = value - fd * tl;
                // 补0
                while (--pos > -1 && pointAfter < POW10_LONG_VALUES[pos]) {
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
                    long tl = POW10_LONG_VALUES[pos];
                    int pointBefore = (int) (value / tl);
                    off += writePositiveLong(pointBefore, buf, off);
                    buf[off++] = '.';
                    long pointAfter = value - pointBefore * tl;
                    // 补0
                    while (--pos > -1 && pointAfter < POW10_LONG_VALUES[pos]) {
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
                while (--pos > -1 && rem < POW10_LONG_VALUES[pos]) {
                    chars[off++] = '0';
                }
                off += NumberUtils.writePositiveLong(rem, chars, off);
                for (int j = len - 1; j > -1; --j) {
                    int value = values[j];
                    pos = 8;
                    // value < BI_TO_DECIMAL_BASE
                    while (--pos > -1 && value < POW10_LONG_VALUES[pos]) {
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
                while (--pos > -1 && rem < POW10_LONG_VALUES[pos]) {
                    buf[off++] = '0';
                }
                off += NumberUtils.writePositiveLong(rem, buf, off);
                for (int j = len - 1; j > -1; --j) {
                    int value = values[j];
                    pos = 8;
                    // value < BI_TO_DECIMAL_BASE
                    while (--pos > -1 && value < POW10_LONG_VALUES[pos]) {
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
            UnsafeHelper.putInt(chars, off, TWO_DIGITS_32_BITS[val]);
            return 2;
        }
        int v = val / 100;
        int v1 = val - v * 100;
        chars[off++] = DigitOnes[v];
        UnsafeHelper.putInt(chars, off, TWO_DIGITS_32_BITS[v1]);
        return 3;
    }

    static int writeThreeDigits(int val, byte[] buf, int off) {
        if (val < 10) {
            buf[off] = (byte) DigitOnes[val];
            return 1;
        }
        if (val < 100) {
            UnsafeHelper.putShort(buf, off, TWO_DIGITS_16_BITS[val]);
            return 2;
        }
        int v = val / 100;
        int v1 = val - v * 100;
        buf[off++] = (byte) DigitOnes[v];
        UnsafeHelper.putShort(buf, off, TWO_DIGITS_16_BITS[v1]);
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
        int v, v1, v2, v3, v4;
        if (val < 10000) {
            v = (int) val;
            if (v < 1000) {
                return writeThreeDigits(v, chars, off);
            } else {
                return UnsafeHelper.putLong(chars, off, FOUR_DIGITS_64_BITS[v]);
            }
        }
        final int beginIndex = off;
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
        int v, v1, v2, v3, v4;
        if (val < 10000) {
            v = (int) val;
            if (v < 1000) {
                return writeThreeDigits(v, buf, off);
            } else {
                return UnsafeHelper.putInt(buf, off, FOUR_DIGITS_32_BITS[v]);
            }
        }
        final int beginIndex = off;
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

    public static String toString(double val) {
        char[] chars = THREAD_LOCAL_CHARS.get();
        return new String(chars, 0, writeDouble(val, chars, 0));
    }

    public static String toString(float val) {
        char[] chars = THREAD_LOCAL_CHARS.get();
        return new String(chars, 0, writeFloat(val, chars, 0));
    }
}
