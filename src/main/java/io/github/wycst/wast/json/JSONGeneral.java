package io.github.wycst.wast.json;

import io.github.wycst.wast.json.annotations.JsonDeserialize;
import io.github.wycst.wast.json.annotations.JsonSerialize;
import io.github.wycst.wast.json.custom.JsonDeserializer;
import io.github.wycst.wast.json.custom.JsonSerializer;
import io.github.wycst.wast.json.exceptions.JSONException;
import io.github.wycst.wast.json.options.JSONParseContext;
import io.github.wycst.wast.json.options.ReadOption;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Array;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.sql.Time;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * @Author wangyunchao
 * @Date 2021/12/7 19:30
 */
class JSONGeneral {

    // 转义字符与字符串映射（0-159）（序列化）
    protected final static String[] escapes = new String[160];

    // 是否需要转义定义（序列化转义校验）
    protected final static boolean[] needEscapes = new boolean[160];

    protected final static char[][] FORMAT_DIGITS = new char[10][];

    protected final static String MONTH_ABBR[] = {
            "Jan", "Feb", "Mar", "Apr", "May", "Jun",
            "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"
    };

    protected final static int DIRECT_READ_BUFFER_SIZE = 8192;

    // 时间钟加入缓存
    protected final static Map<String, TimeZone> GMT_TIME_ZONE_MAP = new HashMap<String, TimeZone>();


    public static String toEscapeString(int ch) {
        return String.format("\\u%04x", ch);
    }

    // 序列化单例缓存
    private final static Map<Class<? extends JsonSerializer>, JsonSerializer> serializers = new HashMap<Class<? extends JsonSerializer>, JsonSerializer>();
    // 反序列化单例缓存
    private final static Map<Class<? extends JsonDeserializer>, JsonDeserializer> deserializers = new HashMap<Class<? extends JsonDeserializer>, JsonDeserializer>();

    static {
        for (int i = 0; i < escapes.length; i++) {
            switch (i) {
                case '\n':
                    escapes[i] = "\\n";
                    needEscapes[i] = true;
                    break;
                case '\t':
                    escapes[i] = "\\t";
                    needEscapes[i] = true;
                    break;
                case '\r':
                    escapes[i] = "\\r";
                    needEscapes[i] = true;
                    break;
                case '\b':
                    escapes[i] = "\\b";
                    needEscapes[i] = true;
                    break;
                case '\f':
                    escapes[i] = "\\f";
                    needEscapes[i] = true;
                    break;
                case '"':
                    escapes[i] = "\\\"";
                    needEscapes[i] = true;
                    break;
                case '\\':
                    escapes[i] = "\\\\";
                    needEscapes[i] = true;
                    break;
                default:
                    if (i < 32) {
                        escapes[i] = toEscapeString(i);
                        needEscapes[i] = true;
                    } else if (i > 126) {
                        // 127-159
                        escapes[i] = toEscapeString(i);
                        needEscapes[i] = true;
                    } else {
                        // 32 - 126 （Exclude from the above case list）
                        escapes[i] = String.valueOf((char) i);
                    }
            }
        }

        for (int i = 0; i < 10; i++) {
            FORMAT_DIGITS[i] = new char[]{'0', Character.forDigit(i, 10)};
        }

        String[] availableIDs = TimeZone.getAvailableIDs();
        for (String availableID : availableIDs) {
            GMT_TIME_ZONE_MAP.put(availableID, TimeZone.getTimeZone(availableID));
        }
        TimeZone timeZone = TimeZone.getTimeZone("GMT+00:00");
        GMT_TIME_ZONE_MAP.put("GMT+00:00", timeZone);
        GMT_TIME_ZONE_MAP.put("+00:00", timeZone);
        GMT_TIME_ZONE_MAP.put("-00:00", timeZone);
        GMT_TIME_ZONE_MAP.put("+0", timeZone);
        GMT_TIME_ZONE_MAP.put("-0", timeZone);
        GMT_TIME_ZONE_MAP.put("+08:00", timeZone = TimeZone.getTimeZone("GMT+08:00"));
        GMT_TIME_ZONE_MAP.put("GMT+08:00", timeZone);
    }

    /***
     * 以10进制解析double数
     * 一般与Double.parseDouble(String)的结果一致（a.b{n}当n的值不大于15时）
     * 当小数位数n超过15位（double精度）时有几率和Double.parseDouble(String)的结果存在误差
     *
     * <p> 性能大约是Double.parseDouble的2-3+倍,
     * <p> 此解析代码无任何依赖，感兴趣的可以将代码摘出去测试。
     *
     * @param chars
     * @param beginIndex
     * @param endIndex
     * @return
     *
     * <p>
     * 注：小数点后超过15位会精度丢失,比如以下两个字符串使用Double.parseDouble解析的值是相等的
     * 113.232310000033344343323323231313311111
     * 113.232310000033344343323323231313311
     *
     * @author wangyunchao
     * @see Double#parseDouble(String)
     */
    protected static double parseDouble(char[] chars, int beginIndex, int endIndex) {
        int i = beginIndex, len = endIndex - beginIndex;
        // 考虑到精度问题，实际指数值 = E指数值 - 小数点的位数
        double value = 0;
        // 10进制, 如果要支持16进制解析，需要判断是否以0x开头，然后将radix重置为16
        // 但16进制的一般出现在整型场景下，这里只做浮点数的解析暂时不做处理
        int radix = 10;
        // 小数位数
        int decimalCount = 0;
        char ch = chars[beginIndex];
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
                    throw new NumberFormatException("For input string: \"" + new String(chars, beginIndex, len) + "\"");
                }
                // 小数点模式
                mode = 1;
                if (++i < endIndex) {
                    ch = chars[i];
                }
            } else if (ch == 'E') {
                if (mode == 2) {
                    throw new NumberFormatException("For input string: \"" + new String(chars, beginIndex, len) + "\"");
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
                    throw new NumberFormatException("For input string: \"" + new String(chars, beginIndex, len) + "\"");
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
//        double powValue = Math.pow(radix, expValue);
//        value *= powValue;
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
     * 转化为int数字
     *
     * @param buffers
     * @param fromIndex
     * @param toIndex
     * @return
     */
    protected final static int parseInt(char[] buffers, int fromIndex, int toIndex)
            throws NumberFormatException {
        return parseInt(buffers, fromIndex, toIndex, 10);
    }

    /**
     * 转化为int数字
     *
     * @param buffers
     * @param fromIndex
     * @param toIndex
     * @return
     */
    protected final static int parseInt(char[] buffers, int fromIndex, int toIndex, int radix)
            throws NumberFormatException {
        try {
            return doParseInt(buffers, fromIndex, toIndex, radix);
        } catch (Throwable throwable) {
            String str = new String(buffers, fromIndex, toIndex - fromIndex);
            throw new NumberFormatException("For input string: \"" + str + "\"");
        }
    }

    /**
     * 转化为long数字
     *
     * @param buffers
     * @param fromIndex
     * @param toIndex
     * @return
     */
    protected final static long parseLong(char[] buffers, int fromIndex, int toIndex) {
        return parseLong(buffers, fromIndex, toIndex, 10);
    }

    /**
     * 转化为long数字
     *
     * @param buffers
     * @param fromIndex
     * @param toIndex
     * @return
     */
    protected final static long parseLong(char[] buffers, int fromIndex, int toIndex, int radix) {
        try {
            return doParseLong(buffers, fromIndex, toIndex, radix);
        } catch (Throwable throwable) {
            String str = new String(buffers, fromIndex, toIndex - fromIndex);
            throw new NumberFormatException("For input string: \"" + str + "\"");
        }
    }

    protected static Number parseNumber(char[] buffers, int fromIndex, int toIndex, boolean useBigDecimal) {
        if (useBigDecimal) {
            return new BigDecimal(buffers, fromIndex, toIndex - fromIndex);
        }
        int ch;
        boolean useDoubleParse = false;
        for (int i = fromIndex; i < toIndex; i++) {
            ch = buffers[i];
            if (ch == '.' || ch == 'E') {
//                dotIndex = i;
                useDoubleParse = true;
                break;
            }
        }
//        if (useDoubleParse) {
//            return Double.parseDouble(new String(buffers, fromIndex, toIndex - fromIndex));
//        }
        if (/*dotIndex > -1*/useDoubleParse) {
            // max LONG 9223372036854775807（19位）
//            if (dotIndex - fromIndex > 18) {
//                return Double.parseDouble(new String(buffers, fromIndex, toIndex - fromIndex));
//            }
//            // to double
//            long integerNum = parseLong(buffers, fromIndex, dotIndex);
//            long dotNum = parseLong(buffers, dotIndex + 1, toIndex);
//            long dotLen = toIndex - dotIndex - 1;
//            double dotValue = dotNum / Math.pow(10, dotLen);
//            boolean negative = buffers[fromIndex] == '-';
//            return negative ? integerNum - dotValue : integerNum + dotValue;
            // if use Double.parseDouble ？
            return parseDouble(buffers, fromIndex, toIndex);
        } else {
            // max LONG 9223372036854775807
            if (toIndex - fromIndex > 18) {
                return new BigInteger(new String(buffers, fromIndex, toIndex - fromIndex));
            }
            long longValue = parseLong(buffers, fromIndex, toIndex);
            if (longValue < (long) Integer.MAX_VALUE && longValue > (long) Integer.MIN_VALUE) {
                return (int) longValue;
            } else {
                return longValue;
            }
        }
    }

    /***
     * 查找字符索引（Find character index）
     *
     * @param buffers
     * @param ch
     * @param beginIndex
     * @param toIndex
     * @return
     */
    protected final static int indexOf(char[] buffers, char ch, int beginIndex, int toIndex) {
        if (buffers == null) return -1;
        for (int i = beginIndex; i < toIndex; i++) {
            if (buffers[i] == ch) {
                return i;
            }
        }
        return -1;
    }

    /**
     * 查找int索引 （Find int index）
     *
     * @param arr
     * @param j
     * @param fromIndex
     * @return
     */
    protected static int indexOf(int[] arr, int j, int fromIndex) {
        if (arr == null) return -1;
        for (int i = fromIndex; i < arr.length; i++) {
            if (arr[i] == j) {
                return i;
            }
        }
        return -1;
    }

    /**
     * 解析整数
     *
     * @param buffers
     * @param fromIndex
     * @param toIndex
     * @return
     * @throws NumberFormatException
     * @see Integer#parseInt(String, int)
     */
    private final static int doParseInt(char[] buffers, int fromIndex, int toIndex, int radix)
            throws NumberFormatException {

        if (buffers == null) {
            throw new NumberFormatException("null");
        }
        int result = 0;
        boolean negative = false;
        int i = 0, len = toIndex - fromIndex;
        int limit = -Integer.MAX_VALUE;
        int multmin;
        int digit;

        if (len > 0) {
            char firstChar = buffers[fromIndex];
            if (firstChar < '0') { // Possible leading "+" or "-"
                if (firstChar == '-') {
                    negative = true;
                    limit = Integer.MIN_VALUE;
                } else if (firstChar != '+') {
                    return 1 / 0;
                }
                if (len == 1) {
                    return 1 / 0;
                }
                i++;
            }
            multmin = limit / radix;
            while (i < len) {
                // Accumulating negatively avoids surprises near MAX_VALUE
                digit = Character.digit(buffers[fromIndex + i++], radix);
                if (digit < 0) {
                    return 1 / 0;
                }
                if (result < multmin) {
                    return 1 / 0;
                }
                result *= radix;
                if (result < limit + digit) {
                    return 1 / 0;
                }
                result -= digit;
            }
        } else {
            return 0;
        }
        return negative ? result : -result;
    }

    /**
     * 转化为long, 支持字符数组
     *
     * @param buffers
     * @param fromIndex
     * @param toIndex
     * @return
     * @throws NumberFormatException
     * @see Long#parseLong(String, int)
     */
    private final static long doParseLong(char[] buffers, int fromIndex, int toIndex, int radix)
            throws NumberFormatException {

        if (buffers == null) {
            throw new RuntimeException();
        }
        long result = 0;
        boolean negative = false;
        int i = 0, len = toIndex - fromIndex;
        long limit = -Long.MAX_VALUE;
        long multmin;
        int digit;
        if (len > 0) {
            char firstChar = buffers[fromIndex];
            if (firstChar < '0') { // Possible leading "+" or "-"
                if (firstChar == '-') {
                    negative = true;
                    limit = Long.MIN_VALUE;
                } else if (firstChar != '+') {
                    return 1 / 0;
                }

                if (len == 1) {
                    return 1 / 0;
                }
                i++;
            }

            multmin = limit / radix;
            while (i < len) {
                // Accumulating negatively avoids surprises near MAX_VALUE
                digit = Character.digit(buffers[fromIndex + i++], radix);
                if (digit < 0) {
                    return 1 / 0;
                }
                if (result < multmin) {
                    return 1 / 0;
                }
                result *= radix;
                if (result < limit + digit) {
                    return 1 / 0;
                }
                result -= digit;
            }
        } else {
            return 0;
        }
        return negative ? result : -result;
    }

    /**
     * 解析日期
     *
     * @param dateStr
     * @param dateCls
     * @return
     */
    public final static Date parseDate(String dateStr, Class<? extends Date> dateCls) {
        char[] buf = getChars(dateStr);
        return autoMatchDate(buf, 0, dateStr.length(), null, dateCls);
    }

    /**
     * 解析日期
     *
     * @param buffers
     * @param from
     * @param to
     * @param dateCls
     * @return
     */
    private final static Date autoMatchDate(char[] buffers, int from, int to, String timezone, Class<? extends Date> dateCls) {

        int len = to - from;
        String timezoneIdAt = timezone;
        if (len > 23) {
            // yyyy-MM-ddTHH:mm:ss.SSS+XX:YY
            int j = to, ch;
            while (j > from) {
                // Check whether the time zone ID exists in the date
                // Check for '+' or '-' or 'Z'
                if ((ch = buffers[--j]) == '.') break;
                if (ch == '+' || ch == '-' || ch == 'Z') {
                    timezoneIdAt = new String(buffers, j, to - j);
                    to = j;
                    len = to - from;
                    break;
                }
            }
        }
        if (len == 28) {
            /***
             * dow mon dd hh:mm:ss zzz yyyy 28位
             * example Sun Jan 02 21:51:14 CST 2020
             * @see Date#toString()
             */
            try {
                int year = parseInt(buffers, from + 24, from + 28);
                String monthAbbr = new String(buffers, from + 4, 3);
                int month = getMonthAbbrIndex(monthAbbr) + 1;
                int day = parseInt(buffers, from + 8, from + 10);
                int hour = parseInt(buffers, from + 11, from + 13);
                int minute = parseInt(buffers, from + 14, from + 16);
                int second = parseInt(buffers, from + 17, from + 19);
                return parseDate(year, month, day, hour, minute, second, 0, timezoneIdAt, dateCls);
            } catch (Throwable throwable) {
            }
        } else if (len == 19 || len == 23 || len == 22 || len == 21) {
            // yyyy-MM-dd HH:mm:ss yyyy/MM/dd HH:mm:ss 19位
            // yyyy-MM-dd HH:mm:ss.SSS? yyyy/MM/dd HH:mm:ss.SSS? 23位
            // \\d{4}[-/]\\d{2}[-/]\\d{2} \\d{2}:\\d{2}:\\d{2}
            try {
                int year = parseInt(buffers, from, from + 4);
                int month = parseInt(buffers, from + 5, from + 7);
                int day = parseInt(buffers, from + 8, from + 10);
                int hour = parseInt(buffers, from + 11, from + 13);
                int minute = parseInt(buffers, from + 14, from + 16);
                int second = parseInt(buffers, from + 17, from + 19);
                int millsecond = 0;
                if (len > 20) {
                    millsecond = parseInt(buffers, from + 20, from + len);
                }
                return parseDate(year, month, day, hour, minute, second, millsecond, timezoneIdAt, dateCls);
            } catch (Throwable throwable) {
            }
        } else if (len == 14 || len == 17) {
            // yyyyMMddHHmmss or yyyyMMddhhmmssSSS
            try {
                int year = parseInt(buffers, from, from + 4);
                int month = parseInt(buffers, from + 4, from + 6);
                int day = parseInt(buffers, from + 6, from + 8);
                int hour = parseInt(buffers, from + 8, from + 10);
                int minute = parseInt(buffers, from + 10, from + 12);
                int second = parseInt(buffers, from + 12, from + 14);
                int millsecond = 0;
                if (len == 17) {
                    millsecond = parseInt(buffers, from + 14, from + 17);
                }
                return parseDate(year, month, day, hour, minute, second, millsecond, timezoneIdAt, dateCls);
            } catch (Throwable throwable) {
            }
        } else if (len == 10) {
            // yyyy-MM-dd yyyy/MM/dd
            // \d{4}[-/]\d{2}[-/]\d{2}
            try {
                int year = parseInt(buffers, from, from + 4);
                int month = parseInt(buffers, from + 5, from + 7);
                int day = parseInt(buffers, from + 8, from + 10);
                return parseDate(year, month, day, 0, 0, 0, 0, timezoneIdAt, dateCls);
            } catch (Throwable throwable) {
            }
        } else if (len == 8) {
            // yyyyMMdd
            // HH:mm:ss
            // "\\d{8}"
            try {
                if (dateCls != null && Time.class.isAssignableFrom(dateCls)) {
                    int hour = parseInt(buffers, from, from + 2);
                    int minute = parseInt(buffers, from + 3, from + 5);
                    int second = parseInt(buffers, from + 6, from + 8);
                    return parseDate(1970, 1, 1, hour, minute, second, 0, timezoneIdAt, dateCls);
                } else {
                    int year = parseInt(buffers, from, from + 4);
                    int month = parseInt(buffers, from + 4, from + 6);
                    int day = parseInt(buffers, from + 6, from + 8);
                    return parseDate(year, month, day, 0, 0, 0, 0, timezoneIdAt, dateCls);
                }
            } catch (Throwable throwable) {
            }
        }
        return null;
    }

    /***
     * 反序列化日期（Deserialization date）
     *
     * @param from     开始位置（双引号或者数字首位置）
     * @param to       结束位置(逗号或者括号位置)
     * @param buffers  字符数组
     * @param pattern  日期格式
     * @param timezone 时区
     * @param dateCls  日期类型
     * @return 日期对象
     */
    protected static Object parseDateValue(int from, int to, char[] buffers, String pattern, String timezone,
                                           Class<? extends Date> dateCls) {
        int realFrom = from;
        int realTo = to;
        // 去除前后空格
        char start = '"';
        while ((from < to) && ((start = buffers[from]) <= ' ')) {
            from++;
        }
        char end = '"';
        while ((to > from) && ((end = buffers[to - 1]) <= ' ')) {
            to--;
        }
        String timezoneIdAt = timezone;
        if (start == '"' && end == '"') {
            if (pattern == null) {
                // from为双引号位置+1为日期开始
                // to为逗号或者}位置，-1为日期结束位置（不包含）
                return autoMatchDate(buffers, from + 1, to - 1, timezone, dateCls);
            } else {
                if (pattern.equals("yyyy-MM-dd HH:mm:ss") || pattern.equals("yyyy/MM/dd HH:mm:ss")) {
                    // 判断格式是否正确
                    // 判断长度 = 19 ，[4] = -,/ [7] = -,/ [10] = ' ' []
                    int len = to - from - 2;
                    if (len == 19) {
                        int i, j;
                        boolean error = false;
                        for (i = from + 1, j = 0; i < to - 1; i++, j++) {
                            char ch = buffers[i];
                            if (j == 4 || j == 7) {
                                if (ch != '-' && ch != '/') {
                                    error = true;
                                    break;
                                }
                            } else if (j == 10) {
                                if (ch != ' ') {
                                    error = true;
                                    break;
                                }
                            } else if (j == 13 || j == 16) {
                                if (ch != ':') {
                                    error = true;
                                    break;
                                }
                            } else {
                                if (!Character.isDigit(ch)) {
                                    error = true;
                                    break;
                                }
                            }
                        }
                        if (error) {
                            throw new JSONException("fromIndex " + realFrom + ", toIndex " + realTo + " str " + new String(buffers, from + 1, to - from - 2) + " pattern " + pattern + ", parse error !");
                        }
                    } else {
                        throw new JSONException("fromIndex " + realFrom + ", toIndex " + realTo + " str " + new String(buffers, from + 1, to - from - 2) + " pattern " + pattern + ", parse error !");
                    }
                    int year = parseInt(buffers, from + 1, from + 5);
                    int month = parseInt(buffers, from + 6, from + 8);
                    int day = parseInt(buffers, from + 9, from + 11);
                    int hour = parseInt(buffers, from + 12, from + 14);
                    int minute = parseInt(buffers, from + 15, from + 17);
                    int second = parseInt(buffers, from + 18, from + 20);
                    return parseDate(year, month, day, hour, minute, second, 0, timezoneIdAt, dateCls);
                } else if (pattern.equals("yyyy-MM-dd") || pattern.equals("yyyy/MM/dd")) {
                    // 判断格式是否正确
                    // 判断长度 = 19 ，[4] = -,/ [7] = -,/ [10] = ' ' []
                    int len = to - from - 2;
                    if (len == 10) {
                        int i, j;
                        boolean error = false;
                        for (i = from + 1, j = 0; i < to - 1; i++, j++) {
                            char ch = buffers[i];
                            if (j == 4 || j == 7) {
                                if (ch != '-' && ch != '/') {
                                    error = true;
                                    break;
                                }
                            } else {
                                if (!Character.isDigit(ch)) {
                                    error = true;
                                    break;
                                }
                            }
                        }
                        if (error) {
                            throw new JSONException("fromIndex " + realFrom + ", toIndex " + realTo + " str " + new String(buffers, from + 1, to - from - 2) + " pattern " + pattern + ", parse error !");
                        }
                    } else {
                        throw new JSONException("fromIndex " + realFrom + ", toIndex " + realTo + " str " + new String(buffers, from + 1, to - from - 2) + " pattern " + pattern + ", parse error !");
                    }
                    int year = parseInt(buffers, from + 1, from + 5);
                    int month = parseInt(buffers, from + 6, from + 8);
                    int day = parseInt(buffers, from + 9, from + 11);
                    return parseDate(year, month, day, 0, 0, 0, 0, timezoneIdAt, dateCls);
                } else if (pattern.equals("yyyyMMddHHmmss")) {
                    int len = to - from - 2;
                    if (len == 14) {
                        int i;
                        boolean error = false;
                        for (i = from + 1; i < to - 1; i++) {
                            char ch = buffers[i];
                            if (!Character.isDigit(ch)) {
                                error = true;
                                break;
                            }
                        }
                        if (error) {
                            throw new JSONException("fromIndex " + realFrom + ", toIndex " + realTo + " str " + new String(buffers, from + 1, to - from - 2) + " pattern " + pattern + ", parse error !");
                        }
                    } else {
                        throw new JSONException("fromIndex " + realFrom + ", toIndex " + realTo + " str " + new String(buffers, from + 1, to - from - 2) + " pattern " + pattern + ", parse error !");
                    }

                    int year = parseInt(buffers, from + 1, from + 5);
                    int month = parseInt(buffers, from + 5, from + 7);
                    int day = parseInt(buffers, from + 7, from + 9);
                    int hour = parseInt(buffers, from + 9, from + 11);
                    int minute = parseInt(buffers, from + 11, from + 13);
                    int second = parseInt(buffers, from + 13, from + 15);
                    return parseDate(year, month, day, hour, minute, second, 0, timezoneIdAt, dateCls);
                } else {
                    // SimpleDateFormat
                    String dateSource = new String(buffers, from + 1, to - from - 2);
                    try {
                        SimpleDateFormat simpleDateFormat = new SimpleDateFormat(pattern);
                        if (timezoneIdAt != null) {
                            simpleDateFormat.setTimeZone(TimeZone.getTimeZone(timezoneIdAt));
                        }
                        Date temp = simpleDateFormat.parse(dateSource);
                        if (dateCls == Date.class)
                            return temp;
                        Constructor<? extends Date> constructor = dateCls.getConstructor(long.class);
                        constructor.setAccessible(true);
                        return constructor.newInstance(temp.getTime());
                    } catch (Exception e) {
                        throw new JSONException("fromIndex " + realFrom + ", toIndex " + realTo + " str " + dateSource + " error !");
                    }
                }
            }
        } else {
            // If it is a long time rub, it has nothing to do with the time zone
            try {
                long timestamp = parseLong(buffers, from, to);
                return parseDate(timestamp, dateCls);
            } catch (Exception e) {
                String timestampStr = new String(buffers, from, to - from);
                throw new JSONException("fromIndex " + realFrom + ", toIndex " + realTo + " str " + timestampStr + " error !");
            }
        }
    }

    /**
     * 清除注释和空白,返回第一个非空字符 （Clear comments and whitespace and return the first non empty character）
     * 开启注释支持后，支持//.*\n 和 /* *\/ （After enabling comment support, support / /* \N and / **\/）
     *
     * @param buffers
     * @param beginIndex       开始位置
     * @param toIndex          最大允许结束位置
     * @param jsonParseContext 上下文配置
     * @return 去掉注释后的第一个非空字符位置（Non empty character position after removing comments）
     * @see ReadOption#AllowComment
     */
    protected static int clearCommentAndWhiteSpaces(char[] buffers, int beginIndex, int toIndex, JSONParseContext jsonParseContext) {
        int i = beginIndex;
        if (i >= toIndex) {
            throw new JSONException("Syntax error, unexpected token character '/', position " + (beginIndex - 1));
        }
        // 注释和 /*注释
        // / or *
        char ch = buffers[beginIndex];
        if (ch == '/') {
            // End with newline \ n
            while (i < toIndex && buffers[i] != '\n') {
                i++;
            }
            // continue clear WhiteSpaces
            ch = '\0';
            while (i + 1 < toIndex && (ch = buffers[++i]) <= ' ') ;
            if (ch == '/') {
                // 递归清除
                i = clearCommentAndWhiteSpaces(buffers, i + 1, toIndex, jsonParseContext);
            }
        } else if (ch == '*') {
            // End with */
            char prev = '\0';
            boolean matched = false;
            while (i + 1 < toIndex) {
                ch = buffers[++i];
                if (ch == '/' && prev == '*') {
                    matched = true;
                    break;
                }
                prev = ch;
            }
            if (!matched) {
                throw new JSONException("Syntax error, not found the close comment '*/' util the end ");
            }
            // continue clear WhiteSpaces
            ch = '\0';
            while (i + 1 < toIndex && (ch = buffers[++i]) <= ' ') ;
            if (ch == '/') {
                // 递归清除
                i = clearCommentAndWhiteSpaces(buffers, i + 1, toIndex, jsonParseContext);
            }
        } else {
            throw new JSONException("Syntax error, unexpected token character '" + ch + "', position " + beginIndex);
        }
        return i;
    }


    private static int getMonthAbbrIndex(String monthAbbr) {
        for (int i = 0, len = MONTH_ABBR.length; i < len; i++) {
            if (MONTH_ABBR[i].equals(monthAbbr)) {
                return i;
            }
        }
        return -1;
    }

    private static Date parseDate(int year, int month, int day, int hour, int minute, int second, int millsecond, String timeZoneId, Class<? extends Date> dateCls) {
        TimeZone timeZone = null;
        if (timeZoneId != null) {
            if (GMT_TIME_ZONE_MAP.containsKey(timeZoneId)) {
                timeZone = GMT_TIME_ZONE_MAP.get(timeZoneId);
            } else {
                if (timeZoneId.startsWith("GMT")) {
                    timeZone = TimeZone.getTimeZone(timeZoneId);
                } else {
                    timeZone = TimeZone.getTimeZone("GMT" + timeZoneId);
                }
                if (timeZone != null && timeZone.getRawOffset() != 0) {
                    GMT_TIME_ZONE_MAP.put(timeZoneId, timeZone);
                }
            }
        }
        io.github.wycst.wast.common.beans.Date date = new io.github.wycst.wast.common.beans.Date(year, month, day, hour, minute, second, millsecond, timeZone);
        long timeInMillis = date.getTime();
        return parseDate(timeInMillis, dateCls);
    }

    private static Date parseDate(long timeInMillis, Class<? extends Date> dateCls) {
        if (dateCls == Date.class) {
            return new Date(timeInMillis);
        } else if (dateCls == java.sql.Date.class) {
            return new java.sql.Date(timeInMillis);
        } else if (dateCls == java.sql.Timestamp.class) {
            return new java.sql.Timestamp(timeInMillis);
        } else {
            try {
                Constructor<? extends Date> constructor = dateCls.getConstructor(long.class);
                constructor.setAccessible(true);
                return constructor.newInstance(timeInMillis);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return null;
    }

    protected static String printHexString(byte[] b, char splitChar) {
        StringBuilder returnValue = new StringBuilder();
        for (int i = 0; i < b.length; i++) {
            String hex = Integer.toHexString(b[i] & 0xFF);
            if (hex.length() == 1) {
                hex = '0' + hex;
            }
            returnValue.append(hex.toUpperCase());
            if (splitChar > 0) {
                returnValue.append(splitChar);
            }
        }
        return returnValue.toString();
    }

    protected static byte[] hexString2Bytes(char[] chars, int offset, int len) {
        byte[] bytes = new byte[len / 2];
        int byteLength = 0;
        int b = -1;
        for (int i = offset, count = offset + len; i < count; i++) {
            char ch = Character.toUpperCase(chars[i]);
            int numIndex = ch > '9' ? ch - 55 : ch - 48;
            if (numIndex < 0 || numIndex >= 16) continue;
            if (b == -1) {
                b = numIndex << 4;
            } else {
                b += numIndex;
                bytes[byteLength++] = (byte) b;
                b = -1;
            }
        }
        if (byteLength == bytes.length) {
            return bytes;
        }

        byte[] buffer = new byte[byteLength];
        System.arraycopy(bytes, 0, buffer, 0, byteLength);
        return buffer;
    }

    protected static char[] readInputStream(InputStream is, int maxLen) throws IOException {
        try {
            char[] buf = new char[maxLen];
            InputStreamReader streamReader = new InputStreamReader(is);
            int len = streamReader.read(buf);
            streamReader.close();
            if (len != maxLen) {
                char[] tmp = new char[len];
                System.arraycopy(buf, 0, tmp, 0, len);
                buf = tmp;
            }
            return buf;
        } catch (RuntimeException rx) {
            throw rx;
        } finally {
            is.close();
        }
    }

    /***
     * 读取UTF-8编码的字节数组
     *
     * 一个字节 0000 0000-0000 007F | 0xxxxxxx
     * 二个字节 0000 0080-0000 07FF | 110xxxxx 10xxxxxx
     * 三个字节 0000 0800-0000 FFFF | 1110xxxx 10xxxxxx 10xxxxxx
     * 四个字节 0001 0000-0010 FFFF | 11110xxx 10xxxxxx 10xxxxxx 10xxxxxx
     *
     * @param bytes
     * @return
     */
    public static char[] readUTF8Bytes(byte[] bytes) {
        if (bytes == null) return null;
        int len = bytes.length;
        char[] chars = new char[len];
        int charLen = 0;

        for (int j = 0; j < len; j++) {
            byte b = bytes[j];
            if (b > 0) {
                chars[charLen++] = (char) b;
                continue;
            }
            // 读取字节b的前4位判断需要读取几个字节
            int s = b >> 4;
            switch (s) {
                case -1:
                    // 1111 4个字节
                    if (j < len - 3) {
                        // 第1个字节的后4位 + 第2个字节的后6位 + 第3个字节的后6位 + 第4个字节的后6位
                        byte b1 = bytes[++j];
                        byte b2 = bytes[++j];
                        byte b3 = bytes[++j];
                        int a = ((b & 0x7) << 18) + ((b1 & 0x3f) << 12) + ((b2 & 0x3f) << 6) + (b3 & 0x3f);
                        chars[charLen++] = (char) a;
                        break;
                    } else {
                        throw new UnsupportedOperationException("utf-8 character error ");
                    }
                case -2:
                    // 1110 3个字节
                    if (j < len - 2) {
                        // 第1个字节的后4位 + 第2个字节的后6位 + 第3个字节的后6位
                        byte b1 = bytes[++j];
                        byte b2 = bytes[++j];
                        int a = ((b & 0xf) << 12) + ((b1 & 0x3f) << 6) + (b2 & 0x3f);
                        chars[charLen++] = (char) a;
                        break;
                    } else {
                        throw new UnsupportedOperationException("utf-8 character error ");
                    }
                case -3:
                    // 1101 和 1100都按2字节处理
                case -4:
                    // 1100 2个字节
                    if (j < len - 1) {
                        byte b1 = bytes[++j];
                        int a = ((b & 0x1f) << 6) + (b1 & 0x3f);
                        chars[charLen++] = (char) a;
                        break;
                    } else {
                        throw new UnsupportedOperationException("utf-8 character error ");
                    }
                default:
                    throw new UnsupportedOperationException("utf-8 character error ");
            }
        }
        if (charLen != len) {
            chars = Arrays.copyOf(chars, charLen);
        }
        return chars;
    }

    /**
     * 将集合类型转化为数组类型 (Convert collection type to array type)
     *
     * @param collection    集合对象
     * @param componentType 数组元素类型
     * @return 数组实例（array instance）
     */
    protected static Object collectionToArray(Collection<Object> collection, Class<?> componentType) {
        Object arr = Array.newInstance(componentType, collection.size());
        int k = 0;
        for (Object obj : collection) {
            Array.set(arr, k++, obj);
        }
        return arr;
    }

    protected static Collection createCollectionInstance(Class<?> collectionCls) throws Exception {
        if (collectionCls.isInterface()) {
            if (Set.class.isAssignableFrom(collectionCls)) {
                return new LinkedHashSet<Object>();
            } else if (List.class.isAssignableFrom(collectionCls)) {
                return new ArrayList<Object>();
            } else {
                throw new UnsupportedOperationException("Unsupported for collection type '" + collectionCls + "', Please specify an implementation class");
            }
        } else {
            if (collectionCls == HashSet.class) {
                return new HashSet<Object>();
            } else if (collectionCls == Vector.class) {
                return new Vector<Object>();
            } else {
                return (Collection<Object>) collectionCls.newInstance();
            }
        }
    }

    protected static JSONWriter getContextWriter(JSONParseContext jsonParseContext) {
        JSONWriter jsonWriter = jsonParseContext.getContextWriter();
        if(jsonWriter == null) {
            jsonParseContext.setContextWriter(jsonWriter = new JSONWriter());
        }
        return jsonWriter;
    }

    private static Field stringToChars;

    static {
        Field valueField;
        try {
            valueField = String.class.getDeclaredField("value");
            // jdk9+ not support setAccessible
            // type is byte[]
            valueField.setAccessible(true);
        } catch (Exception e) {
            valueField = null;
        }
        stringToChars = valueField;
    }

    public static char[] getChars(String json) {
        if (stringToChars == null) {
            // jdk9+
            return json.toCharArray();
        }
        try {
            return (char[]) stringToChars.get(json);
        } catch (IllegalAccessException e) {
            throw new JSONException(e);
        }
    }

//    protected static String getString(char[] buffers) throws Exception {
//        if (stringToChars == null) {
//            return new String(buffers);
//        }
//        String result = new String();
//        stringToChars.set(result, buffers);
//        return result;
//    }

    /***
     * 获取自定义序列化实例
     *
     * @param jsonSerialize 序列化注解实例
     * @return
     */
    protected static JsonSerializer getJsonSerializer(JsonSerialize jsonSerialize) throws Exception {
        boolean singleton = jsonSerialize.singleton();
        Class<? extends JsonSerializer> serializerClass = jsonSerialize.value();
        if (singleton) {
            JsonSerializer jsonSerializer = serializers.get(serializerClass);
            if (jsonSerializer == null) {
                jsonSerializer = serializerClass.newInstance();
                serializers.put(serializerClass, jsonSerializer);
            }
            return jsonSerializer;
        } else {
            return serializerClass.newInstance();
        }
    }

    /***
     * 获取自定义反序列化实例
     *
     * @param jsonDeserialize 反序列化注解实例
     * @return
     */
    protected static JsonDeserializer getJsonDeserializer(JsonDeserialize jsonDeserialize) throws IllegalAccessException, InstantiationException {
        boolean singleton = jsonDeserialize.singleton();
        Class<? extends JsonDeserializer> deserializerClass = jsonDeserialize.value();
        if (singleton) {
            JsonDeserializer jsonDeserializer = deserializers.get(deserializerClass);
            if (jsonDeserializer == null) {
                jsonDeserializer = deserializerClass.newInstance();
                deserializers.put(deserializerClass, jsonDeserializer);
            }
            return jsonDeserializer;
        } else {
            return deserializerClass.newInstance();
        }
    }

}
