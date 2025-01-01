package io.github.wycst.wast.common.expression.functions;

import io.github.wycst.wast.common.beans.GregorianDate;
import io.github.wycst.wast.common.expression.ExpressionException;

import java.lang.reflect.Array;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Collection;
import java.util.Date;
import java.util.Map;

/**
 * 内置函数
 *
 * @Author: wangy
 * @Date: 2021/11/30 22:48
 * @Description:
 */
public final class BuiltInFunction {

    /**
     * 支持将变量/常量参数转化为数组
     *
     * @param params
     * @return
     */
    public static Object[] toArray(Object... params) {
        return params;
    }

    /**
     * // 兼容1.5,1,6不使用流
     * 最大值number && string && Comparable
     *
     * @param params
     * @return
     */
    public static Object max(Object... params) {
        int length = params.length;
        if (length == 0) {
            return 0;
        }
        Object max = null;
        for (int i = 0; i < length; i++) {
            Object value = params[i];
            if (max == null) {
                max = value;
            } else {
                max = compareTo(max, value) > 0 ? max : value;
            }
        }
        return max;
    }

    /**
     * 最小值
     *
     * @param params
     * @return
     */
    public static Object min(Object... params) {
        int length = params.length;
        if (length == 0) {
            return 0;
        }
        Object min = null;
        for (int i = 0; i < length; i++) {
            Object value = params[i];
            if (min == null) {
                min = value;
            } else {
                min = compareTo(min, value) < 0 ? min : value;
            }
        }
        return min;
    }

    // 比较
    private static int compareTo(Object o1, Object o2) {
        if (o1.getClass() == o2.getClass()) {
            Comparable c1 = (Comparable) o1;
            Comparable c2 = (Comparable) o2;
            return c1.compareTo(c2);
        }
        if (o1 instanceof Number && o2 instanceof Number) {
            double d1 = ((Number) o1).doubleValue();
            double d2 = ((Number) o2).doubleValue();
            return d1 > d2 ? 1 : -1;
        } else {
            // 统一转字符串比较
            return o1.toString().compareTo(o2.toString());
        }
    }

//    private static void sort(Number[] numbers) {
//        Arrays.sort(numbers, new Comparator<Number>() {
//            @Override
//            public int compare(Number o1, Number o2) {
//                return compareTo(o1, o2);
//            }
//        });
//    }

    /**
     * 平均值
     *
     * @param numbers
     * @return
     */
    public static Number avg(Number... numbers) {
        int length = numbers.length;
        if (length == 0) {
            return 0;
        }
        return sum(numbers).doubleValue() / length;
    }

    /**
     * 求和
     *
     * @param numbers
     * @return
     */
    public static Number sum(Number... numbers) {
        int length = numbers.length;
        if (length == 0) return 0;
        Double total = 0d;
        boolean useDouble = false;
        boolean useLong = false;
        for (Number number : numbers) {
            if (number instanceof Double || number instanceof Float) {
                useDouble = true;
            } else if (number instanceof Long) {
                useLong = true;
            }
            total += number.doubleValue();
        }
        if (useDouble) {
            return total;
        }
        if (useLong) {
            return total.longValue();
        }
        return total.intValue();
    }

    /**
     * 绝对值
     *
     * @param number
     * @return
     */
    public static Number abs(Number number) {
        return Math.abs(number.doubleValue());
    }

    /**
     * 平方
     *
     * @param number
     * @return
     */
    public static Number sqrt(Number number) {
        return Math.sqrt(number.doubleValue());
    }

    /**
     * 计算字符串长度
     *
     * @param str
     * @return
     */
    public static int length(String str) {
        if (str == null) {
            return 0;
        }
        return str.length();
    }

    /**
     * 转小写
     *
     * @param str
     * @return
     */
    public static String lower(String str) {
        return str.toLowerCase();
    }

    /**
     * 转大写
     *
     * @param str
     * @return
     */
    public static String upper(String str) {
        return str.toUpperCase();
    }

    /**
     * 计算对象的size(数组，List,Map, String)
     *
     * @param object
     * @return
     */
    public static int size(Object object) {
        if (object == null) return 0;
        if (object.getClass().isArray()) {
            return Array.getLength(object);
        } else if (object instanceof Collection) {
            return ((Collection<?>) object).size();
        } else if (object instanceof Map) {
            return ((Map<?, ?>) object).size();
        } else if (object instanceof String) {
            return ((String) object).length();
        }
        throw new ExpressionException("Unsupported call size() by type: " + object.getClass());
    }

    /**
     * 左边为空返回右边值，否则返回左边
     *
     * @param left
     * @param right
     * @return
     */
    public static Object ifNull(Object left, Object right) {
        return left == null ? right : left;
    }

    /**
     * 判断变量是否为null
     *
     * @param value
     * @return
     */
    public static Object isNull(Object value) {
        return value == null;
    }

    /**
     * 判断变量是否不为null
     *
     * @param value
     * @return
     */
    public static Object isNotNull(Object value) {
        return value != null;
    }

    /**
     * 当前时间
     *
     * @return
     */
    public static Date now() {
        return new Date();
    }

    /**
     * 以当前时间为标准，计算偏移量offset（毫秒）, 将日期转化为指定模板（Y-M-d H:m:s）的字符串
     *
     * @param offset 偏移量（单位秒） 0代表当前时间
     * @return
     * @see GregorianDate#format(String)
     */
    public static String date(long offset) {
        return date_format(offset, "Y-M-d H:m:s");
    }

    /**
     * 以当前时间为标准，计算偏移量offset（毫秒）, 将日期转化为指定模板的字符串
     *
     * @param offset   偏移量（单位秒）
     * @param template
     * @return
     * @see GregorianDate#format(String)
     */
    public static String date_format(long offset, String template) {
        long current = System.currentTimeMillis();
        return new GregorianDate(current + offset * 1000).format(template);
    }

    public static String toString(Object value) {
        return (value == null) ? "null" : value.toString();
    }

    /**
     * 提供仿构造函数构建BigDecimal实例
     *
     * @param text
     * @return
     */
    public static BigDecimal BigDecimal(String text) {
        return new BigDecimal(text);
    }

    /**
     * 提供仿构造函数构建BigInteger实例
     *
     * @param text
     * @return
     */
    public static BigInteger BigInteger(String text) {
        return new BigInteger(text);
    }
}
