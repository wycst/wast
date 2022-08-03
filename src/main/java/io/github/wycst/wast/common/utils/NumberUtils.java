package io.github.wycst.wast.common.utils;

/**
 * @Author: wangy
 * @Date: 2022/7/17 16:16
 * @Description:
 */
public class NumberUtils {

    /***
     * 以10进制解析double数
     * 一般与Double.parseDouble(String)的结果一致（a.b{n}当n的值不大于15时）
     * 当小数位数n超过15位（double精度）时有几率和Double.parseDouble(String)的结果存在误差
     *
     * <p> 性能大约是Double.parseDouble的2-3+倍；
     * <p> 此解析代码无任何依赖，感兴趣的可以将代码摘出去测试。
     *
     * @param chars
     * @param offset
     * @param len
     * @return
     *
     * <p>
     * 注：小数点后超过15位会精度丢失,比如以下两个字符串使用Double.parseDouble解析的值是相等的
     * 113.232310000033344343323323231313311111
     * 113.232310000033344343323323231313311
     *
     * @see Double#parseDouble(String)
     */
    public static double parseDouble(char[] chars, int offset, int len) {
        int i = offset, endIndex = len + offset;
        // 考虑到精度问题，实际指数值 = E指数值 - 小数点的位数
        double value = 0;
        // 10进制, 如果要支持16进制解析，需要判断是否以0x开头，然后将radix重置为16
        // 但16进制的一般出现在整型场景下，这里只做浮点数的解析暂时不做处理
        int radix = 10;
        // 小数位数
        int decimalCount = 0;
        char ch = chars[offset];
        boolean negative = false;
        // 负数
        if (ch == '-') {
            // 检查第一个字符是否为符号位
            negative = true;
            i++;
        }
        // 模式
        int mode = 0;
        // 指数值（以radix为底数）
        int expValue = 0;
        boolean expNegative = false;
        for (; i < endIndex; i++) {
            ch = chars[i];
            if (ch == '.') {
                if (mode != 0) {
                    throw new NumberFormatException("For input string: \"" + new String(chars, offset, len) + "\"");
                }
                // 小数点模式
                mode = 1;
                if (++i < endIndex) {
                    ch = chars[i];
                }
            } else if (ch == 'E') {
                if (mode == 2) {
                    throw new NumberFormatException("For input string: \"" + new String(chars, offset, len) + "\"");
                }
                // 科学计数法
                mode = 2;
                if (++i < endIndex) {
                    ch = chars[i];
                }
                if (ch == '-') {
                    expNegative = true;
                    if (++i < endIndex) {
                        ch = chars[i];
                    }
                }
            }
            int digit = Character.digit(ch, radix);
            if (digit == -1) {
                if ((ch != 'd' && ch != 'D') || i < endIndex - 1) {
                    throw new NumberFormatException("For input string: \"" + new String(chars, offset, len) + "\"");
                }
            }
            switch (mode) {
                case 0:
                    value *= radix;
                    value += digit;
                    break;
                case 1:
                    value *= radix;
                    value += digit;
                    decimalCount++;
                    break;
                case 2:
                    expValue *= 10;
                    expValue += digit;
                    break;
            }
        }
        expValue = expNegative ? -expValue - decimalCount : expValue - decimalCount;
        if (expValue > 0) {
            double powValue = Math.pow(radix, expValue);
            value *= powValue;
        } else if (expValue < 0) {
            double powValue = Math.pow(radix, -expValue);
            value /= powValue;
        }
        return negative ? -value : value;
    }

    /**
     * 十进制字符转数字
     *
     * @param ch
     * @return
     */
    public static int digitDecimal(char ch) {
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
    public final static int parseIntWithin5(char[] buf, int fromIndex, int n)
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
     * 转化为4位int数字
     *
     * @param buf
     * @param fromIndex
     * @return
     */
    public final static int parseInt4(char[] buf, int fromIndex)
            throws NumberFormatException {
        int beginIndex = fromIndex;
        int v1 = digitDecimal(buf[beginIndex++]);
        int v2 = digitDecimal(buf[beginIndex++]);
        int v3 = digitDecimal(buf[beginIndex++]);
        int v4 = digitDecimal(buf[beginIndex]);
        if ((v1 | v2 | v3 | v4) == -1) {
            throw new NumberFormatException("For input string: \"" + new String(buf, fromIndex, 4) + "\"");
        }
        return v1 * 1000 + v2 * 100 + v3 * 10 + v4;
    }

    /**
     * 转化为3位int数字
     *
     * @param buf
     * @param fromIndex
     * @return
     */
    public final static int parseInt3(char[] buf, int fromIndex)
            throws NumberFormatException {
        int beginIndex = fromIndex;
        int v1 = digitDecimal(buf[beginIndex++]);
        int v2 = digitDecimal(buf[beginIndex++]);
        int v3 = digitDecimal(buf[beginIndex]);
        if ((v1 | v2 | v3) == -1) {
            throw new NumberFormatException("For input string: \"" + new String(buf, fromIndex, 2) + "\"");
        }
        return v1 * 100 + v2 * 10 + v3;
    }

    /**
     * 转化为2位int数字
     *
     * @param buf
     * @param fromIndex
     * @return
     */
    public final static int parseInt2(char[] buf, int fromIndex)
            throws NumberFormatException {
        int beginIndex = fromIndex;
        int v1 = digitDecimal(buf[beginIndex++]);
        int v2 = digitDecimal(buf[beginIndex]);
        if ((v1 | v2) == -1) {
            throw new NumberFormatException("For input string: \"" + new String(buf, fromIndex, 2) + "\"");
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
    public final static int parseInt1(char[] buf, int fromIndex)
            throws NumberFormatException {
        int v1 = digitDecimal(buf[fromIndex]);
        if (v1 == -1) {
            throw new NumberFormatException("For input string: \"" + new String(buf, fromIndex, 2) + "\"");
        }
        return v1;
    }
}
