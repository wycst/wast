package com.wast.test.expression;

import io.github.wycst.wast.common.expression.Expression;

/**
 * 表达式优先级测试
 *
 */
public class ExprPriorityTest {

    public static void main(String[] args) {
        System.out.println((1 > 2 == 3 >= 4) == Expression.evalResult("1 > 2 == 3 >= 4", boolean.class));
        System.out.println((true | true & false) == Expression.evalResult("true | true & false", boolean.class));
        System.out.println((true ^ true & false) == Expression.evalResult("true ^ true & false", boolean.class));
        System.out.println((true ^ true | true) == Expression.evalResult("true ^ true | true", boolean.class));
        System.out.println((false & false | true) == Expression.evalResult("false & false | true", boolean.class));
        System.out.println((false & false ^ true) == Expression.evalResult("false & false ^ true", boolean.class));
        System.out.println((true | true ^ true) == Expression.evalResult("true | true ^ true", boolean.class));
        System.out.println((false | 3 < 5) == Expression.evalResult("false | 3 < 5", boolean.class));
        System.out.println((false | 5 == 5) == Expression.evalResult("false | 5 == 5", boolean.class));
        System.out.println((true || true & false) == Expression.evalResult("true || true & false", boolean.class));
        System.out.println((false & false | true) == Expression.evalResult("false & false | true", boolean.class));
        System.out.println((true | true & false) == Expression.evalResult("true | true & false", boolean.class));
    }
}
