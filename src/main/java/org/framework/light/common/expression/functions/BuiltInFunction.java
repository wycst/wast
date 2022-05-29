package org.framework.light.common.expression.functions;

import org.framework.light.common.expression.ExpressionException;

import java.lang.reflect.Array;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
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
     * // 兼容1.5,1,6不使用流
     * 最大值number && string && Comparable
     *
     * @param params
     * @return
     */
    public final static Object max(Object... params) {
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
    public final static Object min(Object... params) {
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
//        sort(numbers);
//        return numbers[0];
    }

    // 比较
    private static int compareTo(Object o1, Object o2) {
        if(o1.getClass() == o2.getClass()) {
            Comparable c1 = (Comparable) o1;
            Comparable c2 = (Comparable) o2;
            return c1.compareTo(c2);
        }
        if(o1 instanceof Number && o2 instanceof Number) {
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
    public final static Number avg(Number... numbers) {
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
    public final static Number sum(Number... numbers) {
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
    public final static Number abs(Number number) {
        return Math.abs(number.doubleValue());
    }

    /**
     * 平方
     *
     * @param number
     * @return
     */
    public final static Number sqrt(Number number) {
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
    public final static String lower(String str) {
        return str.toLowerCase();
    }

    /**
     * 转大写
     *
     * @param str
     * @return
     */
    public final static String upper(String str) {
        return str.toUpperCase();
    }

    /**
     * 计算对象的size(数组，List,Map, String)
     *
     * @param object
     * @return
     */
    public final static int size(Object object) {
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
        throw new ExpressionException("Unsupported call size() ");
    }

    /**
     * 左边为空返回右边值，否则返回左边
     *
     * @param left
     * @param right
     * @return
     */
    public final static Object ifNull(Object left, Object right) {
        return left == null ? right : left;
    }

}
