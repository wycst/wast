package io.github.wycst.wast.json;

import io.github.wycst.wast.common.beans.DateTemplate;
import io.github.wycst.wast.common.reflect.UnsafeHelper;
import io.github.wycst.wast.common.utils.NumberUtils;
import io.github.wycst.wast.json.exceptions.JSONException;
import io.github.wycst.wast.json.options.JSONParseContext;
import io.github.wycst.wast.json.options.Options;
import io.github.wycst.wast.json.options.ReadOption;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Writer;
import java.lang.reflect.Array;
import java.lang.reflect.Constructor;
import java.sql.Time;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @Author wangyunchao
 * @Date 2021/12/7 19:30
 */
class JSONGeneral {

    // null
    protected final static char[] NULL = new char[]{'n', 'u', 'l', 'l'};
    protected final static char[] EMPTY_ARRAY = new char[]{'[', ']'};
    protected final static char[] EMPTY_OBJECT = new char[]{'{', '}'};

    // 转义字符与字符串映射（0-159）（序列化）
    protected final static String[] escapes = new String[160];

    // 是否需要转义定义（序列化转义校验）
    protected final static boolean[] needEscapes = new boolean[160];

    protected final static char[][] FORMAT_DIGITS = new char[10][];

    protected final static String MONTH_ABBR[] = {
            "Jan", "Feb", "Mar", "Apr", "May", "Jun",
            "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"
    };

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

    // Double.MAX_VALUE 1.7976931348623157e+308
    final static double[] PositiveDecimalPower = new double[310];

    protected final static int DIRECT_READ_BUFFER_SIZE = 8192;

    // 时间钟加入缓存
    protected final static Map<String, TimeZone> GMT_TIME_ZONE_MAP = new ConcurrentHashMap<String, TimeZone>();

    // zero zone
    public final static TimeZone ZERO_TIME_ZONE = TimeZone.getTimeZone("GMT+00:00");

    public static String toEscapeString(int ch) {
        return String.format("\\u%04x", ch);
    }

    static {
        for (int i = 0; i < escapes.length; ++i) {
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

        for (int i = 0; i < 10; ++i) {
            FORMAT_DIGITS[i] = new char[]{'0', Character.forDigit(i, 10)};
        }

        // e0 ~ e360(e306)
        for (int i = 0, len = PositiveDecimalPower.length; i < len; ++i) {
            PositiveDecimalPower[i] = Math.pow(10, i);
        }

        String[] availableIDs = TimeZone.getAvailableIDs();
        for (String availableID : availableIDs) {
            GMT_TIME_ZONE_MAP.put(availableID, TimeZone.getTimeZone(availableID));
        }
        TimeZone timeZone = ZERO_TIME_ZONE;
        GMT_TIME_ZONE_MAP.put("GMT+00:00", timeZone);
        GMT_TIME_ZONE_MAP.put("+00:00", timeZone);
        GMT_TIME_ZONE_MAP.put("-00:00", timeZone);
        GMT_TIME_ZONE_MAP.put("+0", timeZone);
        GMT_TIME_ZONE_MAP.put("-0", timeZone);
        GMT_TIME_ZONE_MAP.put("+08:00", timeZone = TimeZone.getTimeZone("GMT+08:00"));
        GMT_TIME_ZONE_MAP.put("GMT+08:00", timeZone);
    }

    protected double getDecimalPowerValue(int expValue) {
        if (expValue < PositiveDecimalPower.length) {
            return PositiveDecimalPower[expValue];
        }
        return Math.pow(10, expValue);
    }

    /**
     * 转化为4位int数字
     *
     * @param buf
     * @param fromIndex
     * @return
     */
    protected final static int parseInt4(char[] buf, int fromIndex)
            throws NumberFormatException {
        return NumberUtils.parseInt4(buf, fromIndex);
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
    protected final static int parseIntWithin5(char[] buf, int fromIndex, int n)
            throws NumberFormatException {
        return NumberUtils.parseIntWithin5(buf, fromIndex, n);
    }

    /**
     * escape next
     *
     * @param buf
     * @param next
     * @param i
     * @param beginIndex
     * @param writer
     * @param jsonParseContext
     * @return
     */
    protected final static int escapeNext(char[] buf, char next, int i, int beginIndex, JSONStringWriter writer, JSONParseContext jsonParseContext) {
        int len;
        switch (next) {
            case '\'':
            case '"':
                if (i > beginIndex) {
                    writer.write(buf, beginIndex, i - beginIndex + 1);
                    writer.setCharAt(writer.size() - 1, next);
                } else {
                    writer.append(next);
                }
                beginIndex = ++i + 1;
                break;
            case 'n':
                len = i - beginIndex;
                writer.write(buf, beginIndex, len + 1);
                writer.setCharAt(writer.size() - 1, '\n');
                beginIndex = ++i + 1;
                break;
            case 'r':
                len = i - beginIndex;
                writer.write(buf, beginIndex, len + 1);
                writer.setCharAt(writer.size() - 1, '\r');
                beginIndex = ++i + 1;
                break;
            case 't':
                len = i - beginIndex;
                writer.write(buf, beginIndex, len + 1);
                writer.setCharAt(writer.size() - 1, '\t');
                beginIndex = ++i + 1;
                break;
            case 'b':
                len = i - beginIndex;
                writer.write(buf, beginIndex, len + 1);
                writer.setCharAt(writer.size() - 1, '\b');
                beginIndex = ++i + 1;
                break;
            case 'f':
                len = i - beginIndex;
                writer.write(buf, beginIndex, len + 1);
                writer.setCharAt(writer.size() - 1, '\f');
                beginIndex = ++i + 1;
                break;
            case 'u':
                len = i - beginIndex;
                writer.write(buf, beginIndex, len + 1);
                int c = hex4(buf, i + 2);
                writer.setCharAt(writer.size() - 1, (char) c);
                i += 4;
                beginIndex = ++i + 1;
                break;
            default: {
                // other case delete char '\\'
                len = i - beginIndex;
                writer.write(buf, beginIndex, len + 1);
                writer.setCharAt(writer.size() - 1, next);
                beginIndex = ++i + 1;
            }
        }
        jsonParseContext.setEndIndex(i);
        return beginIndex;
    }

    /**
     * 转化为3位int数字
     *
     * @param buf
     * @param fromIndex
     * @return
     */
    protected final static int parseInt3(char[] buf, int fromIndex)
            throws NumberFormatException {
        return NumberUtils.parseInt3(buf, fromIndex);
    }

    /**
     * 转化为2位int数字
     *
     * @param buf
     * @param fromIndex
     * @return
     */
    protected final static int parseInt2(char[] buf, int fromIndex)
            throws NumberFormatException {
        return NumberUtils.parseInt2(buf, fromIndex);
    }

//    /**
//     * 转化为1位int数字
//     *
//     * @param buf
//     * @param fromIndex
//     * @return
//     */
//    protected final static int parseInt1(char[] buf, int fromIndex)
//            throws NumberFormatException {
//        return NumberUtils.parseInt1(buf, fromIndex);
//    }

    /**
     * 将\\u后面的4个16进制字符转化为int值
     *
     * @param i1
     * @param i2
     * @param i3
     * @param i4
     * @return
     */
    protected final static int hex4(int i1, int i2, int i3, int i4) {
        return (hex(i1) << 12) | (hex(i2) << 8) | (hex(i3) << 4) | hex(i4);
    }

    protected static int hex(int c) {
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
                return c - 48;
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
            default:
                throw new IllegalArgumentException("invalid character: '" + (char) c + "', expected character in '0123456789abcdef(ABCDEF)'");
        }
    }

    /**
     * 将\\u后面的4个16进制字符转化为int值
     *
     * @param buf
     * @param fromIndex \\u位置后一位
     * @return
     */
    protected static int hex4(char[] buf, int fromIndex) {
        int j = fromIndex;
        try {
            int c1 = hex(buf[j++]);
            int c2 = hex(buf[j++]);
            int c3 = hex(buf[j++]);
            int c4 = hex(buf[j++]);
            return (c1 << 12) | (c2 << 8) | (c3 << 4) | c4;
        } catch (Throwable throwable) {
            // \\u parse error
            String errorContextTextAt = createErrorContextText(buf, j - 1);
            throw new JSONException("Syntax error, from pos " + fromIndex + ", context text by '" + errorContextTextAt + "', " + throwable.getMessage());
        }
    }

    /**
     * 十进制字符转数字
     *
     * @param ch
     * @return
     */
    protected static int digitDecimal(char ch) {
        return NumberUtils.digitDecimal(ch);
    }

    /***
     * 查找字符索引（Find character index）
     *
     * @param buf
     * @param ch
     * @param beginIndex
     * @param toIndex
     * @return
     */
    protected final static int indexOf(char[] buf, char ch, int beginIndex, int toIndex) {
        if (buf == null) return -1;
        for (int i = beginIndex; i < toIndex; ++i) {
            if (buf[i] == ch) {
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
        for (int i = fromIndex; i < arr.length; ++i) {
            if (arr[i] == j) {
                return i;
            }
        }
        return -1;
    }

    /**
     * 匹配日期
     *
     * @param buf
     * @param from
     * @param to
     * @param dateCls
     * @return
     */
    final static Date matchDate(char[] buf, int from, int to, String timezone, Class<? extends Date> dateCls) {

        int len = to - from;
        String timezoneIdAt = timezone;
        if (len > 19) {
            // yyyy-MM-ddTHH:mm:ss.SSS+XX:YY
            int j = to, ch;
            while (j > from) {
                // Check whether the time zone ID exists in the date
                // Check for '+' or '-' or 'Z'
                if ((ch = buf[--j]) == '.' || ch == ' ') break;
                if (ch == '+' || ch == '-' || ch == 'Z') {
                    timezoneIdAt = new String(buf, j, to - j);
                    to = j;
                    len = to - from;
                    break;
                }
            }
        }
        switch (len) {
            case 8: {
                // yyyyMMdd
                // HH:mm:ss
                try {
                    if (dateCls != null && Time.class.isAssignableFrom(dateCls)) {
                        int hour = parseInt2(buf, from);
                        int minute = parseInt2(buf, from + 3);
                        int second = parseInt2(buf, from + 6);
                        return parseDate(1970, 1, 1, hour, minute, second, 0, timezoneIdAt, dateCls);
                    } else {
                        int year = parseInt4(buf, from);
                        int month = parseInt2(buf, from + 4);
                        int day = parseInt2(buf, from + 6);
                        return parseDate(year, month, day, 0, 0, 0, 0, timezoneIdAt, dateCls);
                    }
                } catch (Throwable throwable) {
                }
                return null;
            }
            case 10: {
                // yyyy-MM-dd yyyy/MM/dd
                // \d{4}[-/]\d{2}[-/]\d{2}
                try {
                    int year = parseInt4(buf, from);
                    int month = parseInt2(buf, from + 5);
                    int day = parseInt2(buf, from + 8);
                    return parseDate(year, month, day, 0, 0, 0, 0, timezoneIdAt, dateCls);
                } catch (Throwable throwable) {
                }
                return null;
            }
            case 14:
            case 15:
            case 16:
            case 17: {
                // yyyyMMddHHmmss or yyyyMMddhhmmssSSS
                try {
                    int year = parseInt4(buf, from);
                    int month = parseInt2(buf, from + 4);
                    int day = parseInt2(buf, from + 6);
                    int hour = parseInt2(buf, from + 8);
                    int minute = parseInt2(buf, from + 10);
                    int second = parseInt2(buf, from + 12);
                    int millsecond = 0;
                    if (len > 14) {
                        millsecond = parseIntWithin5(buf, from + 14, len - 14);
                    }
                    return parseDate(year, month, day, hour, minute, second, millsecond, timezoneIdAt, dateCls);
                } catch (Throwable throwable) {
                }
                return null;
            }
            case 19:
            case 21:
            case 22:
            case 23: {
                // yyyy-MM-dd HH:mm:ss yyyy/MM/dd HH:mm:ss 19位
                // yyyy-MM-dd HH:mm:ss.SSS? yyyy/MM/dd HH:mm:ss.SSS? 23位
                // \\d{4}[-/]\\d{2}[-/]\\d{2} \\d{2}:\\d{2}:\\d{2}
                try {
                    int year = parseInt4(buf, from);
                    int month = parseInt2(buf, from + 5);
                    int day = parseInt2(buf, from + 8);
                    int hour = parseInt2(buf, from + 11);
                    int minute = parseInt2(buf, from + 14);
                    int second = parseInt2(buf, from + 17);
                    int millsecond = 0;
                    if (len > 20) {
                        millsecond = parseIntWithin5(buf, from + 20, len - 20);
                    }
                    return parseDate(year, month, day, hour, minute, second, millsecond, timezoneIdAt, dateCls);
                } catch (Throwable throwable) {
                }
                return null;
            }
            case 28: {
                /***
                 * dow mon dd hh:mm:ss zzz yyyy 28位
                 * example Sun Jan 02 21:51:14 CST 2020
                 * @see Date#toString()
                 */
                try {
                    int year = parseInt4(buf, from + 24);
                    String monthAbbr = new String(buf, from + 4, 3);
                    int month = getMonthAbbrIndex(monthAbbr) + 1;
                    int day = parseInt2(buf, from + 8);
                    int hour = parseInt2(buf, from + 11);
                    int minute = parseInt2(buf, from + 14);
                    int second = parseInt2(buf, from + 17);
                    return parseDate(year, month, day, hour, minute, second, 0, timezoneIdAt, dateCls);
                } catch (Throwable throwable) {
                }
                return null;
            }
            default:
                return null;
        }
    }

    /**
     * 字符串转日期
     *
     * @param buf
     * @param from         开始引号位置
     * @param to           结束引号位置后一位
     * @param pattern      日期格式
     * @param patternType  格式分类
     * @param dateTemplate 日期模板
     * @param timezone     时间钟
     * @param dateCls      日期类型
     * @return
     */
    protected static Object parseDateValueOfString(char[] buf, int from, int to, String pattern, int patternType, DateTemplate dateTemplate, String timezone,
                                                   Class<? extends Date> dateCls) {
        int realFrom = from;
        String timezoneIdAt = timezone;
        try {
            switch (patternType) {
                case 0: {
                    return matchDate(buf, from + 1, to - 1, timezone, dateCls);
                }
                case 1: {
                    // yyyy-MM-dd HH:mm:ss or yyyy/MM/dd HH:mm:ss
//                int len = to - from - 2;
//                if (len == 19) {
//                    int i, j;
//                    boolean error = false;
//                    for (i = from + 1, j = 0; i < to - 1; i++, j++) {
//                        char ch = buf[i];
//                        if (j == 4 || j == 7) {
//                            if (ch != '-' && ch != '/') {
//                                error = true;
//                                break;
//                            }
//                        } else if (j == 10) {
//                            if (ch != ' ') {
//                                error = true;
//                                break;
//                            }
//                        } else if (j == 13 || j == 16) {
//                            if (ch != ':') {
//                                error = true;
//                                break;
//                            }
//                        } else {
//                            if (digitDecimal(ch) == -1) {
//                                error = true;
//                                break;
//                            }
//                        }
//                    }
//                    if (error) {
//                        throw new JSONException("fromIndex " + realFrom + ", toIndex " + realTo + " str " + new String(buf, from + 1, to - from - 2) + " pattern " + pattern + ", parse error !");
//                    }
//                } else {
//                    throw new JSONException("fromIndex " + realFrom + ", toIndex " + realTo + " str " + new String(buf, from + 1, to - from - 2) + " pattern " + pattern + ", parse error !");
//                }
                    int year = parseInt4(buf, from + 1);
                    int month = parseInt2(buf, from + 6);
                    int day = parseInt2(buf, from + 9);
                    int hour = parseInt2(buf, from + 12);
                    int minute = parseInt2(buf, from + 15);
                    int second = parseInt2(buf, from + 18);
                    return parseDate(year, month, day, hour, minute, second, 0, timezoneIdAt, dateCls);
                }
                case 2: {
                    // yyyy-MM-dd yyyy/MM/dd
//                int len = to - from - 2;
//                if (len == 10) {
//                    int i, j;
//                    boolean error = false;
//                    for (i = from + 1, j = 0; i < to - 1; i++, j++) {
//                        char ch = buf[i];
//                        if (j == 4 || j == 7) {
//                            if (ch != '-' && ch != '/') {
//                                error = true;
//                                break;
//                            }
//                        } else {
//                            if (digitDecimal(ch) == -1) {
//                                error = true;
//                                break;
//                            }
//                        }
//                    }
//                    if (error) {
//                        throw new JSONException("fromIndex " + realFrom + ", toIndex " + realTo + " str " + new String(buf, from + 1, to - from - 2) + " pattern " + pattern + ", parse error !");
//                    }
//                } else {
//                    throw new JSONException("fromIndex " + realFrom + ", toIndex " + realTo + " str " + new String(buf, from + 1, to - from - 2) + " pattern " + pattern + ", parse error !");
//                }
                    int year = parseInt4(buf, from + 1);
                    int month = parseInt2(buf, from + 6);
                    int day = parseInt2(buf, from + 9);
                    return parseDate(year, month, day, 0, 0, 0, 0, timezoneIdAt, dateCls);
                }
                case 3: {
                    // yyyyMMddHHmmss
//                int len = to - from - 2;
//                if (len == 14) {
//                    int i;
//                    boolean error = false;
//                    for (i = from + 1; i < to - 1; i++) {
//                        char ch = buf[i];
//                        if (digitDecimal(ch) == -1) {
//                            error = true;
//                            break;
//                        }
//                    }
//                    if (error) {
//                        throw new JSONException("fromIndex " + realFrom + ", toIndex " + realTo + " str " + new String(buf, from + 1, to - from - 2) + " pattern " + pattern + ", parse error !");
//                    }
//                } else {
//                    throw new JSONException("fromIndex " + realFrom + ", toIndex " + realTo + " str " + new String(buf, from + 1, to - from - 2) + " pattern " + pattern + ", parse error !");
//                }
                    int year = parseInt4(buf, from + 1);
                    int month = parseInt2(buf, from + 5);
                    int day = parseInt2(buf, from + 7);
                    int hour = parseInt2(buf, from + 9);
                    int minute = parseInt2(buf, from + 11);
                    int second = parseInt2(buf, from + 13);
                    return parseDate(year, month, day, hour, minute, second, 0, timezoneIdAt, dateCls);
                }
                default: {
//                    io.github.wycst.wast.common.beans.Date date = io.github.wycst.wast.common.beans.Date.parse(buf, from + 1, to - from - 2, dateTemplate);
//                    if (timezoneIdAt != null) {
//                        date.setTimeZone(TimeZone.getTimeZone(timezoneIdAt));
//                    }
                    TimeZone timeZone = getTimeZone(timezoneIdAt);
                    long time = dateTemplate.parseTime(buf, from + 1, to - from - 2, timeZone);
                    if (dateCls == Date.class) {
                        return new Date(time);
                    }
                    Constructor<? extends Date> constructor = dateCls.getConstructor(long.class);
                    constructor.setAccessible(true);
                    return constructor.newInstance(time);
                }
            }
        } catch (Throwable throwable) {
            if (throwable instanceof JSONException) {
                throw (JSONException) throwable;
            }
            String dateSource = new String(buf, from + 1, to - from - 2);
            if (patternType > 0) {
                throw new JSONException("Syntax error, at pos " + realFrom + ", dateStr " + dateSource + " mismatch date pattern '" + pattern + "'");
            } else {
                throw new JSONException("Syntax error, at pos " + realFrom + ", dateStr " + dateSource + " mismatch any date format.");
            }
        }
    }

    /***
     * 反序列化日期（Deserialization date）
     *
     * @param from     开始位置（双引号或者数字首位置）
     * @param to       结束位置(逗号或者括号位置)
     * @param buf  字符数组
     * @param pattern  日期格式
     * @param timezone 时区
     * @param dateCls  日期类型
     * @return 日期对象
     */
    protected static Object parseDateValue(int from, int to, char[] buf, String pattern, String timezone,
                                           Class<? extends Date> dateCls) {
        int realFrom = from;
        int realTo = to;
        // 去除前后空格
        char start = '"';
        while ((from < to) && ((start = buf[from]) <= ' ')) {
            from++;
        }
        char end = '"';
        while ((to > from) && ((end = buf[to - 1]) <= ' ')) {
            to--;
        }
        if (start == '"' && end == '"') {
            return parseDateValueOfString(buf, from, to, pattern, pattern == null ? 0 : 4, pattern == null ? null : new DateTemplate(pattern), timezone, dateCls);
        } else {
            // If it is a long time rub, it has nothing to do with the time zone
            try {
                long timestamp = Long.parseLong(new String(buf, from, to - from));
                return parseDate(timestamp, dateCls);
            } catch (Exception e) {
                String timestampStr = new String(buf, from, to - from);
                throw new JSONException("fromIndex " + realFrom + ", toIndex " + realTo + " str " + timestampStr + " error !");
            }
        }
    }

    /**
     * 清除注释和空白,返回第一个非空字符 （Clear comments and whitespace and return the first non empty character）
     * 开启注释支持后，支持//.*\n 和 /* *\/ （After enabling comment support, support / /* \N and / **\/）
     *
     * @param buf
     * @param beginIndex       开始位置
     * @param toIndex          最大允许结束位置
     * @param jsonParseContext 上下文配置
     * @return 去掉注释后的第一个非空字符位置（Non empty character position after removing comments）
     * @see ReadOption#AllowComment
     */
    protected final static int clearCommentAndWhiteSpaces(char[] buf, int beginIndex, int toIndex, JSONParseContext jsonParseContext) {
        int i = beginIndex;
        if (i >= toIndex) {
            throw new JSONException("Syntax error, unexpected token character '/', position " + (beginIndex - 1));
        }
        // 注释和 /*注释
        // / or *
        char ch = buf[beginIndex];
        if (ch == '/') {
            // End with newline \ n
            while (i < toIndex && buf[i] != '\n') {
                ++i;
            }
            // continue clear WhiteSpaces
            ch = '\0';
            while (i + 1 < toIndex && (ch = buf[++i]) <= ' ') ;
            if (ch == '/') {
                // 递归清除
                i = clearCommentAndWhiteSpaces(buf, i + 1, toIndex, jsonParseContext);
            }
        } else if (ch == '*') {
            // End with */
            char prev = '\0';
            boolean matched = false;
            while (i + 1 < toIndex) {
                ch = buf[++i];
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
            while (i + 1 < toIndex && (ch = buf[++i]) <= ' ') ;
            if (ch == '/') {
                // 递归清除
                i = clearCommentAndWhiteSpaces(buf, i + 1, toIndex, jsonParseContext);
            }
        } else {
            throw new JSONException("Syntax error, unexpected token character '" + ch + "', position " + beginIndex);
        }
        return i;
    }

    /**
     * 格式化缩进
     *
     * @param content
     * @param level
     * @param formatOut
     * @throws IOException
     */
    protected final static void writeFormatSymbolOut(Writer content, int level, boolean formatOut) throws IOException {
        if (formatOut && level > -1) {
            String symbol = Options.writeFormatOutSymbol;
            int symbolLen = 11;
            if (symbolLen - 1 > level) {
                content.write(symbol, 0, level + 1);
            } else {
                // 全部的symbol
                content.append(symbol);
                // 补齐差的\t个数
                int appendTabLen = level - symbolLen + 1;
                while (appendTabLen-- > 0) {
                    content.append('\t');
                }
            }
        }
    }

    private static int getMonthAbbrIndex(String monthAbbr) {
        for (int i = 0, len = MONTH_ABBR.length; i < len; ++i) {
            if (MONTH_ABBR[i].equals(monthAbbr)) {
                return i;
            }
        }
        return -1;
    }

    private static Date parseDate(int year, int month, int day, int hour, int minute, int second, int millsecond, String timeZoneId, Class<? extends Date> dateCls) {
        TimeZone timeZone = getTimeZone(timeZoneId);
        long timeInMillis = io.github.wycst.wast.common.beans.Date.getTime(year, month, day, hour, minute, second, millsecond, timeZone);
        return parseDate(timeInMillis, dateCls);
    }

    static TimeZone getTimeZone(String timeZoneId) {
        TimeZone timeZone = null;
        if (timeZoneId != null && timeZoneId.trim().length() > 0) {
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
        return timeZone;
    }

    protected static Date parseDate(long timeInMillis, Class<? extends Date> dateCls) {
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
        for (int i = 0; i < b.length; ++i) {
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
        for (int i = offset, count = offset + len; i < count; ++i) {
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
     * 读取UTF-8编码的字节数组转化为字符数组
     *
     * @param bytes
     * @return
     */
    public static char[] readUTF8Bytes(byte[] bytes) {
        if (bytes == null) return null;
        int len = bytes.length;
        char[] chars = new char[len];
        int charLen = readUTF8Bytes(bytes, chars);
        if (charLen != len) {
            chars = Arrays.copyOf(chars, charLen);
        }
        return chars;
    }

    /***
     * 读取UTF-8编码的字节到指定字符数组，请确保字符数组长度
     *
     * 一个字节 0000 0000-0000 007F | 0xxxxxxx
     * 二个字节 0000 0080-0000 07FF | 110xxxxx 10xxxxxx
     * 三个字节 0000 0800-0000 FFFF | 1110xxxx 10xxxxxx 10xxxxxx
     * 四个字节 0001 0000-0010 FFFF | 11110xxx 10xxxxxx 10xxxxxx 10xxxxxx
     *
     * @param bytes
     * @param chars 目标字符
     * @return 字符数组长度
     */
    public static int readUTF8Bytes(byte[] bytes, char[] chars) {
        if (bytes == null) return 0;
        int len = bytes.length;
        int charLen = 0;

        for (int j = 0; j < len; ++j) {
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
                        int a = ((b & 0x7) << 18) | ((b1 & 0x3f) << 12) | ((b2 & 0x3f) << 6) | (b3 & 0x3f);
                        if (Character.isSupplementaryCodePoint(a)) {
                            chars[charLen++] = (char) ((a >>> 10)
                                    + (Character.MIN_HIGH_SURROGATE - (Character.MIN_SUPPLEMENTARY_CODE_POINT >>> 10)));
                            chars[charLen++] = (char) ((a & 0x3ff) + Character.MIN_LOW_SURROGATE);
                        } else {
                            chars[charLen++] = (char) a;
                        }
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
                        int a = ((b & 0xf) << 12) | ((b1 & 0x3f) << 6) | (b2 & 0x3f);
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
                        int a = ((b & 0x1f) << 6) | (b1 & 0x3f);
                        chars[charLen++] = (char) a;
                        break;
                    } else {
                        throw new UnsupportedOperationException("utf-8 character error ");
                    }
                default:
                    throw new UnsupportedOperationException("utf-8 character error ");
            }
        }
        return charLen;
    }

    protected static int getPatternType(String pattern) {
        if (pattern != null) {
            if (pattern.equalsIgnoreCase("yyyy-MM-dd HH:mm:ss")
                    || pattern.equalsIgnoreCase("yyyy/MM/dd HH:mm:ss")) {
                return 1;
            } else if (pattern.equalsIgnoreCase("yyyy-MM-dd") || pattern.equalsIgnoreCase("yyyy/MM/dd")) {
                return 2;
            } else if (pattern.equalsIgnoreCase("yyyyMMddHHmmss")) {
                return 3;
            } else {
                return 4;
            }
        }
        return 0;
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
            if (collectionCls == List.class || collectionCls == Collection.class) {
                return new ArrayList<Object>();
            } else if (collectionCls == Set.class) {
                return new HashSet<Object>();
            } else {
                throw new UnsupportedOperationException("Unsupported for collection type '" + collectionCls + "', Please specify an implementation class");
            }
        } else {
            if (collectionCls == ArrayList.class || collectionCls == Object.class) {
                return new HashSet<Object>();
            } else if (collectionCls == HashSet.class) {
                return new HashSet<Object>();
            } else if (collectionCls == Vector.class) {
                return new Vector<Object>();
            } else {
                return (Collection<Object>) collectionCls.newInstance();
            }
        }
    }

    /**
     * 报错位置取前后18个字符，最多一共40个字符
     */
    protected static String createErrorContextText(char[] buf, int at) {
        try {
            int len = buf.length;
            char[] text = new char[40];
            int count;
            int begin = Math.max(at - 18, 0);
            System.arraycopy(buf, begin, text, 0, count = at - begin);
            text[count++] = '^';
            int end = Math.min(len, at + 18);
            System.arraycopy(buf, at, text, count, end - at);
            count += end - at;
            return new String(text, 0, count);
        } catch (Throwable throwable) {
            return "";
        }
    }

    protected static JSONStringWriter getContextWriter(JSONParseContext jsonParseContext) {
        JSONStringWriter jsonWriter = jsonParseContext.getContextWriter();
        if (jsonWriter == null) {
            jsonParseContext.setContextWriter(jsonWriter = new JSONStringWriter());
        }
        return jsonWriter;
    }

    /***
     * get char[]
     *
     * @param value
     * @return
     */
    protected static char[] getChars(String value) {
        return UnsafeHelper.getChars(value);
    }
}
