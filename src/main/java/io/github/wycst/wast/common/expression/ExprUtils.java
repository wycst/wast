package io.github.wycst.wast.common.expression;

import java.math.BigDecimal;
import java.math.BigInteger;

/**
 *
 * @Author: wangy
 * @Date: 2022/10/22 13:58
 * @Description:
 */
final class ExprUtils {

    /**
     * 获取value值的负数运算
     *
     * @param value
     * @return
     */
    public static Object negate(Object value) {
        Number number = (Number) value;
        if (value instanceof Long) {
            return -(Long) value;
        }
        if (value instanceof Integer) {
            return -(Integer) value;
        }
        if (value instanceof BigDecimal) {
            return ((BigDecimal) value).negate();
        }
        if (value instanceof BigInteger) {
            return ((BigInteger) value).negate();
        }
        return -number.doubleValue();
    }
}
