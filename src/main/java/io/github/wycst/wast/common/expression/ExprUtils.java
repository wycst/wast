package io.github.wycst.wast.common.expression;

/**
 * 抽取静态公共方法
 *
 * @Author: wangy
 * @Date: 2022/10/22 13:58
 * @Description:
 */
public class ExprUtils {

    /**
     * 获取value值的负数运算
     *
     * @param value
     * @return
     */
    public static Object getNegateNumber(Object value) {
        Number number = (Number) value;
        if (value instanceof Long) {
            return -(Long) value;
        }
        if (value instanceof Integer) {
            return -(Integer) value;
        }
        return -number.doubleValue();
    }

}
