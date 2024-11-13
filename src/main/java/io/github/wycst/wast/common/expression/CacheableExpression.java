package io.github.wycst.wast.common.expression;

import io.github.wycst.wast.common.beans.ArrayQueueMap;
import io.github.wycst.wast.common.utils.ObjectUtils;

/**
 * 支持缓存的表达式API(一般不推荐使用，仅仅与某些带缓存的库进行性能测试比较实用，实际生产没有必要缓存)
 *
 * @Date 2024/10/22 20:34
 * @Created by wangyc
 */
public final class CacheableExpression {

    // Cache up to 256 units
    static final ArrayQueueMap<String, Expression> ARRAY_QUEUE_MAP = new ArrayQueueMap<String, Expression>(256);
    static int maxExprLength = 1 << 16;

    public static void setMaxExprLength(int maxExprLength) {
        CacheableExpression.maxExprLength = maxExprLength;
    }

    /***
     * 解析表达式并缓存（不推荐使用）
     *
     * @param expr
     * @return
     */
    public static Expression parse(String expr) {
        expr = expr.trim();
        if (expr.length() > maxExprLength) {
            // not cache
            return new ExprParser(expr);
        }
        Expression expression = ARRAY_QUEUE_MAP.get(expr);
        if (expression == null) {
            synchronized (ARRAY_QUEUE_MAP) {
                expression = ARRAY_QUEUE_MAP.get(expr);
                if (expression == null) {
                    expression = new ExprParser(expr);
                    ARRAY_QUEUE_MAP.put(expr.trim(), expression);
                }
                return expression;
            }
        }
        return expression;
    }

    /**
     * 执行静态表达式
     *
     * @param expr
     * @return
     */
    public static Object eval(String expr) {
        return parse(expr).evaluate();
    }

    /***
     * 执行表达式
     *
     * @param expr
     * @param evaluateEnvironment 执行环境
     * @return
     */
    public static Object eval(String expr, EvaluateEnvironment evaluateEnvironment) {
        return parse(expr).evaluate(evaluateEnvironment);
    }

    /***
     * 简化调用
     *
     * @param expr
     * @param params 可变参数
     * @return
     */
    public static Object evalParameters(String expr, Object... params) {
        return parse(expr).evaluateParameters(params);
    }

    /***
     * 简化调用
     *
     * @param expr
     * @param targetClass
     * @param params 可变参数
     * @return
     */
    public static <T> T evalParameters(String expr, Class<T> targetClass, Object... params) {
        return ObjectUtils.toType(parse(expr).evaluateParameters(params), targetClass);
    }

    /***
     * 执行表达式
     *
     * @param expr
     * @param evaluateEnvironment 执行环境
     * @return
     */
    public static <T> T evalResult(String expr, EvaluateEnvironment evaluateEnvironment, Class<T> targetClass) {
        return ObjectUtils.toType(parse(expr).evaluate(evaluateEnvironment), targetClass);
    }

    public static void clearCaches() {
        ARRAY_QUEUE_MAP.clear();
    }
}
