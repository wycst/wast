package io.github.wycst.wast.json;

import io.github.wycst.wast.common.beans.DateTemplate;
import io.github.wycst.wast.common.beans.GregorianDate;
import io.github.wycst.wast.common.reflect.GenericParameterizedType;
import io.github.wycst.wast.common.reflect.UnsafeHelper;
import io.github.wycst.wast.common.utils.NumberUtils;
import io.github.wycst.wast.json.exceptions.JSONException;
import io.github.wycst.wast.json.options.ReadOption;
import io.github.wycst.wast.json.util.FixedNameValueMap;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Serializable;
import java.lang.reflect.Constructor;
import java.sql.Time;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @Author wangyunchao
 * @Date 2021/12/7 19:30
 */
class JSONGeneral {

    // Format output character pool
    static final char[] FORMAT_OUT_SYMBOL_TABS = "\n\t\t\t\t\t\t\t\t\t\t".toCharArray();
    static final char[] FORMAT_OUT_SYMBOL_SPACES = new char[32];

    static {
        Arrays.fill(FORMAT_OUT_SYMBOL_SPACES, ' ');
    }

    // null
    protected final static char[] NULL = new char[]{'n', 'u', 'l', 'l'};
    protected final static char[] EMPTY_ARRAY = new char[]{'[', ']'};
    protected final static char[] EMPTY_OBJECT = new char[]{'{', '}'};
    protected final static int TRUE_INT = UnsafeHelper.getInt(new byte[]{'t', 'r', 'u', 'e'}, 0);
    protected final static long TRUE_LONG = UnsafeHelper.getLong(new char[]{'t', 'r', 'u', 'e'}, 0);
    protected final static int ALSE_INT = UnsafeHelper.getInt(new byte[]{'a', 'l', 's', 'e'}, 0);
    protected final static long ALSE_LONG = UnsafeHelper.getLong(new char[]{'a', 'l', 's', 'e'}, 0);

    protected final static byte ZERO = 0;
    protected final static byte COMMA = ',';
    protected final static byte DOUBLE_QUOTATION = '"';
    protected final static byte COLON_SIGN = ':';
    protected final static byte END_ARRAY = ']';
    protected final static byte END_OBJECT = '}';
    protected final static byte WHITE_SPACE = ' ';
    protected final static byte ESCAPE = '\\';

    final static int TYPE_BIGDECIMAL = 1;
    final static int TYPE_BIGINTEGER = 2;
    final static int TYPE_FLOAT = 3;
    final static int TYPE_DOUBLE = 4;

    // 转义字符与字符串映射（序列化）
    final static String[] ESCAPE_VALUES = new String[256];
    final static byte[] ESCAPE_FLAGS = new byte[256];

    final static String MONTH_ABBR[] = {
            "Jan", "Feb", "Mar", "Apr", "May", "Jun",
            "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"
    };

    final static char[] DigitOnes = NumberUtils.copyDigitOnes();

    final static char[] DigitTens = NumberUtils.copyDigitTens();

    final static int[] ESCAPE_CHARS = new int[160];

    // cache keys
    private static FixedNameValueMap<String> KEY_32_TABLE = new FixedNameValueMap<String>(4096);
    private static FixedNameValueMap<String> KEY_EIGHT_BYTES_TABLE = new FixedNameValueMap<String>(2048);

    static {
        for (int i = 0; i < 160; i++) {
            ESCAPE_CHARS[i] = i;
        }
//        ESCAPE_CHARS['\''] = '\'';
//        ESCAPE_CHARS['"'] = '"';
        ESCAPE_CHARS['n'] = '\n';
        ESCAPE_CHARS['r'] = '\r';
        ESCAPE_CHARS['t'] = '\t';
        ESCAPE_CHARS['b'] = '\b';
        ESCAPE_CHARS['f'] = '\f';
        ESCAPE_CHARS['u'] = -1;
    }

    public final static int DIRECT_READ_BUFFER_SIZE = 8192;

    // 时间钟加入缓存
    final static Map<String, TimeZone> GMT_TIME_ZONE_MAP = new ConcurrentHashMap<String, TimeZone>();

    // zero zone
    public final static TimeZone ZERO_TIME_ZONE = TimeZone.getTimeZone("GMT+00:00");

    // 24
    final static ThreadLocal<char[]> CACHED_CHARS_24 = new ThreadLocal<char[]>() {
        @Override
        protected char[] initialValue() {
            return new char[24];
        }
    };

    // "yyyy-MM-dd HH:mm:ss"
    final static ThreadLocal<char[]> CACHED_CHARS_DATE_21 = new ThreadLocal<char[]>() {
        @Override
        protected char[] initialValue() {
            char[] chars = new char[21];
            chars[0] = chars[20] = '"';
            chars[5] = chars[8] = '-';
            chars[11] = ' ';
            chars[14] = chars[17] = ':';
            return chars;
        }
    };

    final static ThreadLocal<double[]> DOUBLE_ARRAY_TL = new ThreadLocal<double[]>() {
        @Override
        protected double[] initialValue() {
            return new double[32];
        }
    };
    final static ThreadLocal<long[]> LONG_ARRAY_TL = new ThreadLocal<long[]>() {
        @Override
        protected long[] initialValue() {
            return new long[32];
        }
    };
    final static ThreadLocal<int[]> INT_ARRAY_TL = new ThreadLocal<int[]>() {
        @Override
        protected int[] initialValue() {
            return new int[32];
        }
    };
    final static long[] EMPTY_LONGS = new long[0];
    final static int[] EMPTY_INTS = new int[0];
    final static double[] EMPTY_DOUBLES = new double[0];
    final static String[] EMPTY_STRINGS = new String[0];

    public static String toEscapeString(int ch) {
        return String.format("\\u%04x", ch);
    }

    // Default interface or abstract class implementation class configuration
    private static final Map<Class<?>, JSONImplInstCreator> DEFAULT_IMPL_INST_CREATOR_MAP = new HashMap<Class<?>, JSONImplInstCreator>();

    static {
        for (int i = 0; i < ESCAPE_VALUES.length; ++i) {
            switch (i) {
                case '\n':
                    ESCAPE_VALUES[i] = "\\n";
                    break;
                case '\t':
                    ESCAPE_VALUES[i] = "\\t";
                    break;
                case '\r':
                    ESCAPE_VALUES[i] = "\\r";
                    break;
                case '\b':
                    ESCAPE_VALUES[i] = "\\b";
                    break;
                case '\f':
                    ESCAPE_VALUES[i] = "\\f";
                    break;
                case '"':
                    ESCAPE_VALUES[i] = "\\\"";
                    break;
                case '\\':
                    ESCAPE_VALUES[i] = "\\\\";
                    break;
                default:
                    if (i < 32) {
                        ESCAPE_VALUES[i] = toEscapeString(i);
                    }
            }
            if(i < 32 || i == '"' || i == '\\') {
                ESCAPE_FLAGS[i] = 1;
            }
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

        registerImplCreator(EnumSet.class, new JSONImplInstCreator<EnumSet>() {
            @Override
            public EnumSet create(GenericParameterizedType<EnumSet> parameterizedType) {
                Class actualType = parameterizedType.getValueType().getActualType();
                return EnumSet.noneOf(actualType);
            }
        });
        registerImplCreator(EnumMap.class, new JSONImplInstCreator<EnumMap>() {
            @Override
            public EnumMap create(GenericParameterizedType<EnumMap> parameterizedType) {
                Class mapKeyClass = parameterizedType.getMapKeyClass();
                return new EnumMap(mapKeyClass);
            }
        });
    }

    /**
     * @param parentClass
     * @param creator
     * @param <T>
     */
    public final static <T> void registerImplCreator(Class<? extends T> parentClass, JSONImplInstCreator<T> creator) {
        DEFAULT_IMPL_INST_CREATOR_MAP.put(parentClass, creator);
    }

    final static JSONImplInstCreator getJSONImplInstCreator(Class<?> targetClass) {
        return DEFAULT_IMPL_INST_CREATOR_MAP.get(targetClass);
    }

    final static String getCacheKey(char[] buf, int offset, int len, long hashCode) {
        if (len > 32) {
            return new String(buf, offset, len);
        }
        //  len > 0
        String value = KEY_32_TABLE.getValue(buf, offset, offset + len, hashCode);
        if (value == null) {
            value = new String(buf, offset, len);
            KEY_32_TABLE.putValue(value, hashCode, value);
        }
        return value;
    }

    final static String getCacheKey(byte[] bytes, int offset, int len, long hashCode) {
        if (len > 32) {
            return new String(bytes, offset, len);
        }
        String value = KEY_32_TABLE.getValueByHash(hashCode);
        if (value == null) {
            value = new String(bytes, offset, len);
            KEY_32_TABLE.putValue(value, hashCode, value);
        }
        return value;
    }

    final static String getCacheEightCharsKey(char[] buf, int offset, int len, long hashCode) {
        String value = KEY_EIGHT_BYTES_TABLE.getValueByHash(hashCode);
        if (value == null) {
            value = new String(buf, offset, len);
            KEY_EIGHT_BYTES_TABLE.putExactHashValue(hashCode, value);
        }
        return value;
    }

    final static String getCacheEightBytesKey(byte[] bytes, int offset, int len, long hashCode) {
        String value = KEY_EIGHT_BYTES_TABLE.getValueByHash(hashCode);
        if (value == null) {
            value = new String(bytes, offset, len);
            KEY_EIGHT_BYTES_TABLE.putExactHashValue(hashCode, value);
        }
        return value;
    }

    /**
     * 获取以10为底数的指定数值（-1 < expValue > 310）指数值
     *
     * @param expValue
     * @return
     */
    protected static double getDecimalPowerValue(int expValue) {
        return NumberUtils.getDecimalPowerValue(expValue);
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
    protected final static int escapeNext(char[] buf, char next, int i, int beginIndex, JSONCharArrayWriter writer, JSONParseContext jsonParseContext) {
        if (i > beginIndex) {
            writer.write(buf, beginIndex, i - beginIndex);
        }
        if (next < ESCAPE_CHARS.length) {
            int escapeChar = ESCAPE_CHARS[next];
            if (escapeChar > -1) {
                writer.write((char) escapeChar);
                beginIndex = ++i + 1;
            } else {
                // \\u
                int c = hex4(buf, i + 2);
                writer.write((char) c);
                i += 4;
                beginIndex = ++i + 1;
            }
        } else {
            writer.write(next);
            beginIndex = ++i + 1;
        }
//        if (next == 'u') {
//            int c = hex4(buf, i + 2);
//            writer.append((char) c);
//            i += 4;
//            beginIndex = ++i + 1;
//        } else if (next < 160) {
//            writer.append(ESCAPE_CHARS[next]);
//            beginIndex = ++i + 1;
//        } else {
//            writer.append(next);
//            beginIndex = ++i + 1;
//        }
        jsonParseContext.endIndex = i;
        return beginIndex;
    }

    /**
     * escape next
     *
     * @param bytes
     * @param next
     * @param i
     * @param beginIndex
     * @param writer
     * @param jsonParseContext
     * @return
     */
    final static int escape(byte[] bytes, byte next, int i, int beginIndex, JSONCharArrayWriter writer, JSONParseContext jsonParseContext) {
        int len;
        switch (next) {
            case '\'':
            case '"':
                if (i > beginIndex) {
                    writer.writeBytes(bytes, beginIndex, i - beginIndex + 1);
                    writer.setCharAt(writer.size() - 1, (char) next);
                } else {
                    writer.write((char) next);
                }
                beginIndex = ++i + 1;
                break;
            case 'n':
                len = i - beginIndex;
                writer.writeBytes(bytes, beginIndex, len + 1);
                writer.setCharAt(writer.size() - 1, '\n');
                beginIndex = ++i + 1;
                break;
            case 'r':
                len = i - beginIndex;
                writer.writeBytes(bytes, beginIndex, len + 1);
                writer.setCharAt(writer.size() - 1, '\r');
                beginIndex = ++i + 1;
                break;
            case 't':
                len = i - beginIndex;
                writer.writeBytes(bytes, beginIndex, len + 1);
                writer.setCharAt(writer.size() - 1, '\t');
                beginIndex = ++i + 1;
                break;
            case 'b':
                len = i - beginIndex;
                writer.writeBytes(bytes, beginIndex, len + 1);
                writer.setCharAt(writer.size() - 1, '\b');
                beginIndex = ++i + 1;
                break;
            case 'f':
                len = i - beginIndex;
                writer.writeBytes(bytes, beginIndex, len + 1);
                writer.setCharAt(writer.size() - 1, '\f');
                beginIndex = ++i + 1;
                break;
            case 'u':
                len = i - beginIndex;
                writer.writeBytes(bytes, beginIndex, len + 1);

                int c;
                int j = i + 2;
                try {
                    int c1 = hex(bytes[j++]);
                    int c2 = hex(bytes[j++]);
                    int c3 = hex(bytes[j++]);
                    int c4 = hex(bytes[j++]);
                    c = (c1 << 12) | (c2 << 8) | (c3 << 4) | c4;
                } catch (Throwable throwable) {
                    // \\u parse error
                    String errorContextTextAt = createErrorContextText(bytes, i + 1);
                    throw new JSONException("Syntax error, from pos " + (i + 1) + ", context text by '" + errorContextTextAt + "', " + throwable.getMessage());
                }

                writer.setCharAt(writer.size() - 1, (char) c);
                i += 4;
                beginIndex = ++i + 1;
                break;
            default: {
                // other case delete char '\\'
                len = i - beginIndex;
                writer.writeBytes(bytes, beginIndex, len + 1);
                writer.setCharAt(writer.size() - 1, (char) next);
                beginIndex = ++i + 1;
            }
        }
        jsonParseContext.endIndex = i;
        return beginIndex;
    }

    /**
     * escape next
     *
     * @param source
     * @param bytes
     * @param next
     * @param i                escapeIndex
     * @param beginIndex
     * @param writer
     * @param jsonParseContext
     * @return 返回转义内容处理完成后的下一个未知字符位置
     */
    final static int escapeAscii(String source, byte[] bytes, byte next, int i, int beginIndex, JSONCharArrayWriter writer, JSONParseContext jsonParseContext) {

        if (i > beginIndex) {
            writer.write(source, beginIndex, i - beginIndex);
        }
        if (next == 'u') {
            int c;
            int j = i + 2;
            try {
                int c1 = hex(bytes[j++]);
                int c2 = hex(bytes[j++]);
                int c3 = hex(bytes[j++]);
                int c4 = hex(bytes[j]);
                c = (c1 << 12) | (c2 << 8) | (c3 << 4) | c4;
            } catch (Throwable throwable) {
                // \\u parse error
                String errorContextTextAt = createErrorContextText(bytes, i + 1);
                throw new JSONException("Syntax error, from pos " + (i + 1) + ", context text by '" + errorContextTextAt + "', " + throwable.getMessage());
            }
            writer.write((char) c);
            i += 4;
            beginIndex = ++i + 1;
        } else if (next < ESCAPE_CHARS.length) {
            writer.write((char) ESCAPE_CHARS[next & 0xFF]);
            beginIndex = ++i + 1;
        } else {
            writer.write((char) next);
            beginIndex = ++i + 1;
        }
        jsonParseContext.endIndex = i;
        return beginIndex;
    }

    /**
     * 解析处理字符串中转义字符，如果不存在转义字符，返回原字符串
     *
     * @param str
     * @return
     */
    public static final String parseEscapeString(String str) {
        str.getClass();
        int beginIndex = str.indexOf('\\');
        // none Escape
        if (beginIndex == -1) return str;

        JSONParseContext jsonParseContext = new JSONParseContext();
        JSONCharArrayWriter writer = getContextWriter(jsonParseContext);
        try {
            char[] chars = getChars(str);
            char next = chars[beginIndex + 1];
            beginIndex = escapeNext(chars, next, beginIndex, 0, writer, jsonParseContext);
            int max = chars.length;
            for (int i = beginIndex + 1; i < max; i++) {
                if (chars[i] != '\\') continue;
                next = chars[i + 1];
                beginIndex = escapeNext(chars, next, i, beginIndex, writer, jsonParseContext);
                i = jsonParseContext.endIndex;
            }
            writer.write(chars, beginIndex, max - beginIndex);
            return writer.toString();
        } finally {
            jsonParseContext.clear();
        }
    }

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
    protected static int digitDecimal(int ch) {
        return NumberUtils.digitDecimal(ch);
    }

    protected static final boolean isDigit(int c) {
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
                        millsecond = NumberUtils.parseIntWithin5(buf, from + 14, len - 14);
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
                        millsecond = NumberUtils.parseIntWithin5(buf, from + 20, len - 20);
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
                case 1: {
                    // yyyy-MM-dd HH:mm:ss or yyyy/MM/dd HH:mm:ss
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
                    int year = parseInt4(buf, from + 1);
                    int month = parseInt2(buf, from + 6);
                    int day = parseInt2(buf, from + 9);
                    return parseDate(year, month, day, 0, 0, 0, 0, timezoneIdAt, dateCls);
                }
                case 3: {
                    // yyyyMMddHHmmss
                    int year = parseInt4(buf, from + 1);
                    int month = parseInt2(buf, from + 5);
                    int day = parseInt2(buf, from + 7);
                    int hour = parseInt2(buf, from + 9);
                    int minute = parseInt2(buf, from + 11);
                    int second = parseInt2(buf, from + 13);
                    return parseDate(year, month, day, hour, minute, second, 0, timezoneIdAt, dateCls);
                }
                case 4: {
                    TimeZone timeZone = getTimeZone(timezoneIdAt);
                    long time = dateTemplate.parseTime(buf, from + 1, to - from - 2, timeZone);
                    return parseDate(time, dateCls);
                }
                default: {
                    return matchDate(buf, from + 1, to - 1, timezone, dateCls);
                }
            }
        } catch (Throwable throwable) {
            if (throwable instanceof RuntimeException) {
                throw (RuntimeException) throwable;
            }
            String dateSource = new String(buf, from + 1, to - from - 2);
            if (patternType > 0) {
                throw new JSONException("Syntax error, at pos " + realFrom + ", dateStr " + dateSource + " mismatch date pattern '" + pattern + "'");
            } else {
                throw new JSONException("Syntax error, at pos " + realFrom + ", dateStr " + dateSource + " mismatch any date format.");
            }
        }
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
    final static Date matchDate(byte[] buf, int from, int to, String timezone, Class<? extends Date> dateCls) {

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
                        int hour = NumberUtils.parseInt2(buf, from);
                        int minute = NumberUtils.parseInt2(buf, from + 3);
                        int second = NumberUtils.parseInt2(buf, from + 6);
                        return parseDate(1970, 1, 1, hour, minute, second, 0, timezoneIdAt, dateCls);
                    } else {
                        int year = NumberUtils.parseInt4(buf, from);
                        int month = NumberUtils.parseInt2(buf, from + 4);
                        int day = NumberUtils.parseInt2(buf, from + 6);
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
                    int year = NumberUtils.parseInt4(buf, from);
                    int month = NumberUtils.parseInt2(buf, from + 5);
                    int day = NumberUtils.parseInt2(buf, from + 8);
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
                    int year = NumberUtils.parseInt4(buf, from);
                    int month = NumberUtils.parseInt2(buf, from + 4);
                    int day = NumberUtils.parseInt2(buf, from + 6);
                    int hour = NumberUtils.parseInt2(buf, from + 8);
                    int minute = NumberUtils.parseInt2(buf, from + 10);
                    int second = NumberUtils.parseInt2(buf, from + 12);
                    int millsecond = 0;
                    if (len > 14) {
                        millsecond = NumberUtils.parseIntWithin5(buf, from + 14, len - 14);
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
                    int year = NumberUtils.parseInt4(buf, from);
                    int month = NumberUtils.parseInt2(buf, from + 5);
                    int day = NumberUtils.parseInt2(buf, from + 8);
                    int hour = NumberUtils.parseInt2(buf, from + 11);
                    int minute = NumberUtils.parseInt2(buf, from + 14);
                    int second = NumberUtils.parseInt2(buf, from + 17);
                    int millsecond = 0;
                    if (len > 20) {
                        millsecond = NumberUtils.parseIntWithin5(buf, from + 20, len - 20);
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
                    int year = NumberUtils.parseInt4(buf, from + 24);
                    String monthAbbr = new String(buf, from + 4, 3);
                    int month = getMonthAbbrIndex(monthAbbr) + 1;
                    int day = NumberUtils.parseInt2(buf, from + 8);
                    int hour = NumberUtils.parseInt2(buf, from + 11);
                    int minute = NumberUtils.parseInt2(buf, from + 14);
                    int second = NumberUtils.parseInt2(buf, from + 17);
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
     * @param bytes        字节数组
     * @param from         开始引号位置
     * @param to           结束引号位置后一位
     * @param pattern      日期格式
     * @param patternType  格式分类
     * @param dateTemplate 日期模板
     * @param timezone     时间钟
     * @param dateCls      日期类型
     * @return
     */
    protected static Object parseDateValueOfString(byte[] bytes, int from, int to, String pattern, int patternType, DateTemplate dateTemplate, String timezone,
                                                   Class<? extends Date> dateCls) {
        int realFrom = from;
        String timezoneIdAt = timezone;
        try {
            switch (patternType) {
                case 0: {
                    return matchDate(bytes, from + 1, to - 1, timezone, dateCls);
                }
                case 1: {
                    // yyyy-MM-dd HH:mm:ss or yyyy/MM/dd HH:mm:ss
                    int year = NumberUtils.parseInt4(bytes, from + 1);
                    int month = NumberUtils.parseInt2(bytes, from + 6);
                    int day = NumberUtils.parseInt2(bytes, from + 9);
                    int hour = NumberUtils.parseInt2(bytes, from + 12);
                    int minute = NumberUtils.parseInt2(bytes, from + 15);
                    int second = NumberUtils.parseInt2(bytes, from + 18);
                    return parseDate(year, month, day, hour, minute, second, 0, timezoneIdAt, dateCls);
                }
                case 2: {
                    // yyyy-MM-dd yyyy/MM/dd
                    int year = NumberUtils.parseInt4(bytes, from + 1);
                    int month = NumberUtils.parseInt2(bytes, from + 6);
                    int day = NumberUtils.parseInt2(bytes, from + 9);
                    return parseDate(year, month, day, 0, 0, 0, 0, timezoneIdAt, dateCls);
                }
                case 3: {
                    // yyyyMMddHHmmss
                    int year = NumberUtils.parseInt4(bytes, from + 1);
                    int month = NumberUtils.parseInt2(bytes, from + 5);
                    int day = NumberUtils.parseInt2(bytes, from + 7);
                    int hour = NumberUtils.parseInt2(bytes, from + 9);
                    int minute = NumberUtils.parseInt2(bytes, from + 11);
                    int second = NumberUtils.parseInt2(bytes, from + 13);
                    return parseDate(year, month, day, hour, minute, second, 0, timezoneIdAt, dateCls);
                }
                default: {
                    TimeZone timeZone = getTimeZone(timezoneIdAt);
                    long time = dateTemplate.parseTime(bytes, from + 1, to - from - 2, timeZone);
                    return parseDate(time, dateCls);
                }
            }
        } catch (Throwable throwable) {
            if (throwable instanceof JSONException) {
                throw (JSONException) throwable;
            }
            String dateSource = new String(bytes, from + 1, to - from - 2);
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
            throw new JSONException("Syntax error, unexpected '/', position " + (beginIndex - 1));
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
            throw new JSONException("Syntax error, unexpected '" + ch + "', position " + beginIndex);
        }
        return i;
    }

    /**
     * 清除注释和空白
     *
     * @param bytes
     * @param beginIndex       开始位置
     * @param toIndex          最大允许结束位置
     * @param jsonParseContext 上下文配置
     * @return 去掉注释后的第一个非空字节位置（Non empty character position after removing comments）
     * @see ReadOption#AllowComment
     */
    protected static int clearCommentAndWhiteSpaces(byte[] bytes, int beginIndex, int toIndex, JSONParseContext jsonParseContext) {
        int i = beginIndex;
        if (i >= toIndex) {
            throw new JSONException("Syntax error, unexpected '/', position " + (beginIndex - 1));
        }
        // 注释和 /*注释
        // / or *
        byte b = bytes[beginIndex];
        if (b == '/') {
            // End with newline \ n
            while (i < toIndex && bytes[i] != '\n') {
                ++i;
            }
            // continue clear WhiteSpaces
            b = '\0';
            while (i + 1 < toIndex && (b = bytes[++i]) <= ' ') ;
            if (b == '/') {
                // 递归清除
                i = clearCommentAndWhiteSpaces(bytes, i + 1, toIndex, jsonParseContext);
            }
        } else if (b == '*') {
            // End with */
            byte prev = 0;
            boolean matched = false;
            while (i + 1 < toIndex) {
                b = bytes[++i];
                if (b == '/' && prev == '*') {
                    matched = true;
                    break;
                }
                prev = b;
            }
            if (!matched) {
                throw new JSONException("Syntax error, not found the close comment '*/' util the end ");
            }
            // continue clear WhiteSpaces
            b = '\0';
            while (i + 1 < toIndex && (b = bytes[++i]) <= ' ') ;
            if (b == '/') {
                // 递归清除
                i = clearCommentAndWhiteSpaces(bytes, i + 1, toIndex, jsonParseContext);
            }
        } else {
            throw new JSONException("Syntax error, unexpected '" + (char) b + "', position " + beginIndex);
        }
        return i;
    }

    /**
     * 格式化缩进,默认使用\t来进行缩进
     *
     * @param content
     * @param level
     * @param formatOut
     * @throws IOException
     */
    protected final static void writeFormatOutSymbols(JSONWriter content, int level, boolean formatOut, JSONConfig jsonConfig) throws IOException {
        if (formatOut && level > -1) {
            boolean formatIndentUseSpace = jsonConfig.isFormatIndentUseSpace();
            if (formatIndentUseSpace) {
                content.write('\n');
                if (level == 0) return;
                int totalSpaceNum = level * jsonConfig.getFormatIndentSpaceNum();
                int symbolSpaceNum = FORMAT_OUT_SYMBOL_SPACES.length;
                while (totalSpaceNum >= symbolSpaceNum) {
                    content.write(FORMAT_OUT_SYMBOL_SPACES);
                    totalSpaceNum -= symbolSpaceNum;
                }
                while (totalSpaceNum-- > 0) {
                    content.write(' ');
                }
            } else {
                char[] symbol = FORMAT_OUT_SYMBOL_TABS;
                int symbolLen = 11;
                if (symbolLen - 1 > level) {
                    content.write(symbol, 0, level + 1);
                } else {
                    // 全部的symbol
                    content.write(symbol);
                    // 补齐差的\t个数
                    int appendTabLen = level - symbolLen + 1;
                    while (appendTabLen-- > 0) {
                        content.write('\t');
                    }
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
        long timeInMillis = GregorianDate.getTime(year, month, day, hour, minute, second, millsecond, timeZone);
        return parseDate(timeInMillis, dateCls);
    }

    // 获取时钟，默认GMT
    static TimeZone getTimeZone(String timeZoneId) {
        if (timeZoneId != null && timeZoneId.trim().length() > 0) {
            TimeZone timeZone;
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
            return timeZone;
        } else {
            return UnsafeHelper.getDefaultTimeZone();
        }
    }

    // 将时间戳转化为指定类型的日期对象
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
                UnsafeHelper.setAccessible(constructor);
                return constructor.newInstance(timeInMillis);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return null;
    }

    /**
     * @param from
     * @param to
     * @param buf
     * @param isUnquotedFieldName
     * @return
     */
    final static Serializable parseKeyOfMap(char[] buf, int from, int to, boolean isUnquotedFieldName) {
        if (isUnquotedFieldName) {
            while ((from < to) && ((buf[from]) <= ' ')) {
                from++;
            }
            while ((to > from) && ((buf[to - 1]) <= ' ')) {
                to--;
            }
            int count = to - from;
            if (count == 4) {
                if (buf[from] == 'n' && buf[from + 1] == 'u' && buf[from + 2] == 'l' && buf[from + 3] == 'l') {
                    return null;
                }
                if (buf[from] == 't' && buf[from + 1] == 'r' && buf[from + 2] == 'u' && buf[from + 3] == 'e') {
                    return true;
                }
            }
            if (count == 5 && buf[from] == 'f' && buf[from + 1] == 'a' && buf[from + 2] == 'l' && buf[from + 3] == 's' && buf[from + 4] == 'e') {
                return false;
            }
            boolean numberFlag = true;
            int pointFlag = 0;
            for (int i = from; i < to; ++i) {
                int c = buf[i];
                if (c == '.') {
                    ++pointFlag;
                } else {
                    if (i != from || c != '-') {
                        if (!isDigit(c)) {
                            numberFlag = false;
                            break;
                        }
                    }
                }
            }
            String result = new String(buf, from, count);
            if (numberFlag && pointFlag <= 1) {
                if (pointFlag == 1) {
                    return Double.parseDouble(result);
                } else {
                    long val = Long.parseLong(result);
                    if (val <= Integer.MAX_VALUE && val >= Integer.MIN_VALUE) {
                        return (int) val;
                    }
                    return val;
                }
            }
            return result;
        } else {
            int len = to - from - 2;
            return new String(buf, from + 1, len);
        }
    }

    /**
     * @param from
     * @param to
     * @param buf
     * @param isUnquotedFieldName
     * @return
     */
    final static Serializable parseKeyOfMap(byte[] buf, int from, int to, boolean isUnquotedFieldName) {
        if (isUnquotedFieldName) {
            while ((from < to) && ((buf[from]) <= ' ')) {
                from++;
            }
            while ((to > from) && ((buf[to - 1]) <= ' ')) {
                to--;
            }
            int count = to - from;
            if (count == 4) {
                if (buf[from] == 'n' && buf[from + 1] == 'u' && buf[from + 2] == 'l' && buf[from + 3] == 'l') {
                    return null;
                }
                if (buf[from] == 't' && buf[from + 1] == 'r' && buf[from + 2] == 'u' && buf[from + 3] == 'e') {
                    return true;
                }
            }
            if (count == 5 && buf[from] == 'f' && buf[from + 1] == 'a' && buf[from + 2] == 'l' && buf[from + 3] == 's' && buf[from + 4] == 'e') {
                return false;
            }
            boolean numberFlag = true;
            int pointFlag = 0;
            for (int i = from; i < to; ++i) {
                int c = buf[i];
                if (c == '.') {
                    ++pointFlag;
                } else {
                    if (i != from || c != '-') {
                        if (!isDigit(c)) {
                            numberFlag = false;
                            break;
                        }
                    }
                }
            }
            String result = new String(buf, from, count);
            if (numberFlag && pointFlag <= 1) {
                if (pointFlag == 1) {
                    return Double.parseDouble(result);
                } else {
                    long val = Long.parseLong(result);
                    if (val <= Integer.MAX_VALUE && val >= Integer.MIN_VALUE) {
                        return (int) val;
                    }
                    return val;
                }
            }
            return result;
        } else {
            int len = to - from - 2;
            return new String(buf, from + 1, len);
        }
    }

    // 字节数组转16进制字符串
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

    // 16进制字符数组（字符串）转化字节数组
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

    // 16进制字符数组（字符串）转化字节数组
    protected static byte[] hexString2Bytes(byte[] buf, int offset, int len) {
        byte[] bytes = new byte[len / 2];
        int byteLength = 0;
        int b = -1;
        for (int i = offset, count = offset + len; i < count; ++i) {
            char ch = Character.toUpperCase((char) buf[i]);
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

    // 读取流中的最大限度（maxLen）的字符数组
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

    protected static void handleCatchException(Throwable ex, char[] buf, int toIndex) {
        // There is only one possibility to control out of bounds exceptions when indexing toindex
        if (ex instanceof IndexOutOfBoundsException) {
            String errorContextTextAt = createErrorContextText(buf, toIndex);
            throw new JSONException("Syntax error, context text by '" + errorContextTextAt + "', JSON format error, and the end token may be missing, such as '\"' or ', ' or '}' or ']'.", ex);
        }
        if (ex instanceof RuntimeException) {
            throw (RuntimeException) ex;
        }
    }

    protected static void handleCatchException(Throwable ex, byte[] bytes, int toIndex) {
        // There is only one possibility to control out of bounds exceptions when indexing toindex
        if (ex instanceof IndexOutOfBoundsException) {
            String errorContextTextAt = createErrorContextText(bytes, toIndex);
            throw new JSONException("Syntax error, context text by '" + errorContextTextAt + "', JSON format error, and the end token may be missing, such as '\"' or ', ' or '}' or ']'.", ex);
        }
        if (ex instanceof RuntimeException) {
            throw (RuntimeException) ex;
        }
    }

    // 常规日期格式分类
    protected static int getPatternType(String pattern) {
        if (pattern != null) {
            if (pattern.equalsIgnoreCase("yyyy-MM-dd HH:mm:ss")
                    || pattern.equalsIgnoreCase("yyyy/MM/dd HH:mm:ss")
                    || pattern.equalsIgnoreCase("yyyy-MM-ddTHH:mm:ss")
            ) {
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
        return UnsafeHelper.toArray(collection, componentType);
    }

    protected static final int COLLECTION_ARRAYLIST_TYPE = 1;
    protected static final int COLLECTION_HASHSET_TYPE = 2;
    protected static final int COLLECTION_OTHER_TYPE = 3;

    protected final static int getCollectionType(Class<?> actualType) {
        if (actualType == List.class || actualType == ArrayList.class || actualType.isAssignableFrom(ArrayList.class)) {
            return COLLECTION_ARRAYLIST_TYPE;
        } else if (actualType == Set.class || actualType == HashSet.class || actualType.isAssignableFrom(HashSet.class)) {
            return COLLECTION_HASHSET_TYPE;
        } else {
            return COLLECTION_OTHER_TYPE;
        }
    }

    protected final static Collection createCollectionInstance(Class<?> collectionCls) {
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
                try {
                    return (Collection<Object>) collectionCls.newInstance();
                } catch (Exception e) {
                    throw new JSONException("create Collection instance error, class " + collectionCls);
                }
            }
        }
    }

    // create map
    static Map createMapInstance(GenericParameterizedType genericParameterizedType) {
        Class<? extends Map> mapCls = genericParameterizedType.getActualType();
        Map map = createCommonMapInstance(mapCls);
        if (map != null) return map;
        JSONImplInstCreator implInstCreator = getJSONImplInstCreator(mapCls);
        if (implInstCreator != null) {
            return (Map) implInstCreator.create(genericParameterizedType);
        }
        try {
            return (Map) UnsafeHelper.newInstance(mapCls);
        } catch (Exception e) {
            throw new JSONException("create map error for " + mapCls);
        }
    }

    static Map createMapInstance(Class<? extends Map> mapCls) {
        Map map = createCommonMapInstance(mapCls);
        if (map != null) return map;
        try {
            return (Map) UnsafeHelper.newInstance(mapCls);
        } catch (Exception e) {
            throw new JSONException("create map error for " + mapCls);
        }
    }

    static Map createCommonMapInstance(Class<? extends Map> mapCls) {
        if (mapCls == Map.class || mapCls == null || mapCls == LinkedHashMap.class) {
            return new LinkedHashMap();
        }
        if (mapCls == HashMap.class) {
            return new HashMap();
        }
        if (mapCls == Hashtable.class) {
            return new Hashtable();
        }
        if (mapCls == AbstractMap.class) {
            return new LinkedHashMap();
        }
        if (mapCls == TreeMap.class || mapCls == SortedMap.class) {
            return new TreeMap();
        }
        return null;
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

    /**
     * 报错位置取前后18个字符，最多一共40个字符(字节)
     */
    protected static String createErrorContextText(byte[] bytes, int at) {
        try {
            int len = bytes.length;
            byte[] text = new byte[40];
            int count;
            int begin = Math.max(at - 18, 0);
            System.arraycopy(bytes, begin, text, 0, count = at - begin);
            text[count++] = '^';
            int end = Math.min(len, at + 18);
            System.arraycopy(bytes, at, text, count, end - at);
            count += end - at;
            return new String(text, 0, count);
        } catch (Throwable throwable) {
            return "";
        }
    }

    protected final static JSONCharArrayWriter getContextWriter(JSONParseContext jsonParseContext) {
        JSONCharArrayWriter jsonWriter = jsonParseContext.getContextWriter();
        if (jsonWriter == null) {
            jsonParseContext.setContextWriter(jsonWriter = new JSONCharArrayWriter());
        }
        return jsonWriter;
    }

    /***
     * get char[]
     *
     * @param value
     * @return
     */
    protected final static char[] getChars(String value) {
        return UnsafeHelper.getChars(value);
    }

    /**
     * get value
     *
     * @param value
     * @return
     */
    protected final static Object getStringValue(String value) {
        return UnsafeHelper.getStringValue(value);
    }

}
