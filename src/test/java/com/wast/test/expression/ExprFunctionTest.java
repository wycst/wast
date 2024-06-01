package com.wast.test.expression;

import io.github.wycst.wast.common.expression.EvaluateEnvironment;
import io.github.wycst.wast.common.expression.ExprFunction;
import io.github.wycst.wast.common.expression.Expression;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

/**
 * @Author: wangy
 * @Date: 2021/11/29 1:00
 * @Description:
 */
public class ExprFunctionTest {

    public static void main(String[] args) {
        Map context = new HashMap();
        context.put("tip", "1 ");
        context.put("name", "zhangsan, %s");
        context.put("msg", "hello");
        context.put("type", 1);
        context.put("a", 1);
        context.put("b", 12);
        context.put("c", 111);
        context.put("B6_AvgCpuUsed", 1.0);
        context.put("B5_AvgCpuUsed", 2.0);
        context.put("B4_AvgCpuUsed", 3.0);
        context.put("vars", new String[] {"hello"});

        EvaluateEnvironment evaluateEnvironment = EvaluateEnvironment.create(context);
        evaluateEnvironment.registerStaticMethods(Math.class, String.class);

//        evaluateEnvironment.registerFunction("MAX", new ExprFunction<Object, Number>() {
//            @Override
//            public Number call(Object... params) {
//                Arrays.sort(params);
//                return (Number) params[params.length - 1];
//            }
//        });
        evaluateEnvironment.registerFunction("min", new ExprFunction<Object, Number>() {

            public Number call(Object... params) {
                Arrays.sort(params);
                return (Number) params[params.length - 1];
            }
        });

//        Object result = Expression.eval("@Math.max(B6_AvgCpuUsed,(B5_AvgCpuUsed+B6_AvgCpuUsed))/2.0 + @Math.max(B6_AvgCpuUsed,B5_AvgCpuUsed)", evaluateEnvironment);

        System.out.println( Expression.eval("@min(@sum(a,b,c), 50, 125, 2, -11)", evaluateEnvironment));
        System.out.println( Expression.eval("@max(@sum(a,b,c), 50, 125, 55, 152)", evaluateEnvironment));

        System.out.println("zasd".compareTo("50"));

        Object result = null;
        Expression expression = Expression.parse("@max(@sum(1,1,@avg(1, 2, 3, 400000 + 10000)), 3, 50, 12500, 55, -152)");
        long begin = System.currentTimeMillis();
        for(int i = 0 ; i < 100000; i ++) {
//            result = Expression.eval("@Math.pow(b, a)", evaluateEnvironment);
            result = expression.evaluate(evaluateEnvironment);
//            result = expression.eval("1 + 2 - 3 * 4 / 5 - 6 + 8 - 0");

//            result = Expression.eval("@min(9, 50, 125, 55, 152)", evaluateEnvironment);
//            result = BuiltInFunction.max(9, 50, 125, 55, 152);
//            result = Math.pow(12, 1);
        }
        long end = System.currentTimeMillis();
        System.out.println(result);
        System.out.println(" use " + (end - begin));
    }

}
