package io.github.wycst.wast.common.expression;

import java.math.BigDecimal;
import java.math.BigInteger;

/**
 * @Author: wangy
 * @Date: 2022/10/22 13:58
 * @Description:
 */
final class ExprCalculateUtils {

    /**
     * 获取value值的负数运算
     *
     * @param value
     * @return
     */
    public static Number negate(Object value) {
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

    /**
     * 乘法(*)
     *
     * @param left
     * @param right
     * @param environment
     * @return
     */
    public static Object multiply(Object left, Object right, EvaluateEnvironment environment) {
        Number leftValue = (Number) left, rightValue = (Number) right;
        boolean isLeftBi = leftValue instanceof BigDecimal, isRightBi = rightValue instanceof BigDecimal;
        if (isLeftBi || isRightBi) {
            BigDecimal leftBi = isLeftBi ? (BigDecimal) leftValue : BigDecimal.valueOf(leftValue.doubleValue());
            BigDecimal rightBi = isRightBi ? (BigDecimal) rightValue : BigDecimal.valueOf(rightValue.doubleValue());
            return leftBi.multiply(rightBi, environment.mathContext);
        }
        boolean isDouble = leftValue instanceof Double || rightValue instanceof Double;
        if (isDouble) {
            return leftValue.doubleValue() * rightValue.doubleValue();
        } else {
            return leftValue.longValue() * rightValue.longValue();
        }
    }

    /**
     * 除法(/)
     *
     * @param left
     * @param right
     * @param environment
     * @return
     */
    public static Object divide(Object left, Object right, EvaluateEnvironment environment) {
        Number leftValue = (Number) left, rightValue = (Number) right;
        boolean isLeftBi = leftValue instanceof BigDecimal, isRightBi = rightValue instanceof BigDecimal;
        if (isLeftBi || isRightBi) {
            BigDecimal leftBi = isLeftBi ? (BigDecimal) leftValue : BigDecimal.valueOf(leftValue.doubleValue());
            BigDecimal rightBi = isRightBi ? (BigDecimal) rightValue : BigDecimal.valueOf(rightValue.doubleValue());
            return leftBi.divide(rightBi, environment.mathContext);
        }
        boolean isDouble = leftValue instanceof Double || rightValue instanceof Double;
        if (isDouble) {
            return leftValue.doubleValue() / rightValue.doubleValue();
        } else {
            return leftValue.longValue() / rightValue.longValue();
        }
    }

    /**
     * 余数(%)
     *
     * @param left
     * @param right
     * @param environment
     * @return
     */
    public static Object mod(Object left, Object right, EvaluateEnvironment environment) {
        Number leftValue = (Number) left, rightValue = (Number) right;
        boolean isLeftBi = leftValue instanceof BigDecimal, isRightBi = rightValue instanceof BigDecimal;
        if (isLeftBi || isRightBi) {
            BigDecimal leftBi = isLeftBi ? (BigDecimal) leftValue : BigDecimal.valueOf(leftValue.doubleValue());
            BigDecimal rightBi = isRightBi ? (BigDecimal) rightValue : BigDecimal.valueOf(rightValue.doubleValue());
            return leftBi.remainder(rightBi, environment.mathContext);
        }
        boolean isDouble = leftValue instanceof Double || rightValue instanceof Double;
        if (isDouble) {
            return leftValue.doubleValue() % rightValue.doubleValue();
        } else {
            return leftValue.longValue() % rightValue.longValue();
        }
    }

    /**
     * 指数运算(**)
     *
     * @param left
     * @param right
     * @param environment
     * @return
     */
    public static Object pow(Object left, Object right, EvaluateEnvironment environment) {
        Number leftValue = (Number) left, rightValue = (Number) right;
        boolean isLeftBi = leftValue instanceof BigDecimal;
        if (isLeftBi) {
            BigDecimal leftBi = isLeftBi ? (BigDecimal) leftValue : BigDecimal.valueOf(leftValue.doubleValue());
            int n = rightValue.intValue();
            return leftBi.pow(n, environment.mathContext);
        }
        boolean isDouble = leftValue instanceof Double || rightValue instanceof Double;
        if (isDouble) {
            return Math.pow(leftValue.doubleValue(), rightValue.doubleValue());
        } else {
            return Math.pow(leftValue.longValue(), rightValue.longValue());
        }
    }

    /**
     * 加法(+)
     *
     * @param left
     * @param right
     * @param environment
     * @return
     */
    public static Object plus(Object left, Object right, EvaluateEnvironment environment) {
        if (left instanceof Number && right instanceof Number) {
            Number leftValue = (Number) left, rightValue = (Number) right;
            boolean isLeftBi = leftValue instanceof BigDecimal, isRightBi = rightValue instanceof BigDecimal;
            if (isLeftBi || isRightBi) {
                BigDecimal leftBi = isLeftBi ? (BigDecimal) leftValue : BigDecimal.valueOf(leftValue.doubleValue());
                BigDecimal rightBi = isRightBi ? (BigDecimal) rightValue : BigDecimal.valueOf(rightValue.doubleValue());
                return leftBi.add(rightBi, environment.mathContext);
            }
            boolean isDouble = leftValue instanceof Double || rightValue instanceof Double;
            if (isDouble) {
                return leftValue.doubleValue() + rightValue.doubleValue();
            } else {
                return leftValue.longValue() + rightValue.longValue();
            }
        }
        // 字符串加法
        return left + String.valueOf(right);
    }

    /**
     * 减法(/)
     * @param left
     * @param right
     * @param environment
     * @return
     */
    public static Object subtract(Object left, Object right, EvaluateEnvironment environment) {
        Number leftValue = (Number) left, rightValue = (Number) right;
        boolean isLeftBi = leftValue instanceof BigDecimal, isRightBi = rightValue instanceof BigDecimal;
        if (isLeftBi || isRightBi) {
            BigDecimal leftBi = isLeftBi ? (BigDecimal) leftValue : BigDecimal.valueOf(leftValue.doubleValue());
            BigDecimal rightBi = isRightBi ? (BigDecimal) rightValue : BigDecimal.valueOf(rightValue.doubleValue());
            return leftBi.subtract(rightBi, environment.mathContext);
        }
        boolean isDouble = leftValue instanceof Double || rightValue instanceof Double;
        if (isDouble) {
            return leftValue.doubleValue() - rightValue.doubleValue();
        } else {
            return leftValue.longValue() - rightValue.longValue();
        }
    }
}
