package io.github.wycst.wast.yaml;

import io.github.wycst.wast.common.reflect.UnsafeHelper;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @Author wangyunchao
 */
class YamlGeneral {

    /**
     * 是否window系统
     */
    public static final boolean IS_WINDOW_OS;

    /**
     * 空格字符
     */
    protected static final char SPACE_CHAR = 32;

    /**
     * 分隔符
     */
    protected static final char SPLIT_CHAR = ':';

    /**
     * 类型映射
     */
    protected static Map<String, Integer> typeValues = new ConcurrentHashMap<String, Integer>();

    static {
        typeValues.put("str", 1);
        typeValues.put("float", 2);
        typeValues.put("int", 3);
        typeValues.put("bool", 4);
        typeValues.put("binary", 5);
        typeValues.put("timestamp", 6);
        typeValues.put("set", 7);
        typeValues.put("omap", 8);
        typeValues.put("pairs", 8);
        typeValues.put("seq", 9);
        typeValues.put("map", 10);

        IS_WINDOW_OS = System.getProperty("os.name").toLowerCase().contains("windows");
    }

    protected final static char[] getChars(String value) {
        return UnsafeHelper.getChars(value);
    }

    /**
     * 解析整数
     *
     * @param buffers
     * @param fromIndex
     * @param len
     * @return
     * @throws NumberFormatException
     * @see Integer#parseInt(String, int)
     */
    protected final static int parseInt(char[] buffers, int fromIndex, int len, int radix)
            throws NumberFormatException {

        if (buffers == null) {
            throw new NumberFormatException("null");
        }

        int result = 0;
        boolean negative = false;
        int i = 0;
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
}
