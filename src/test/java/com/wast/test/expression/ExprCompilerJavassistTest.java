package com.wast.test.expression;

import io.github.wycst.wast.common.expression.EvaluateEnvironment;
import io.github.wycst.wast.common.expression.Expression;
import io.github.wycst.wast.common.expression.compile.CompilerEnvironment;
import io.github.wycst.wast.common.expression.compile.CompilerExpression;
import io.github.wycst.wast.common.expression.functions.JavassistExprFunction;

import java.util.HashMap;
import java.util.Map;

/**
 * @Author: wangy
 * @Date: 2022/11/6 11:36
 * @Description:
 */
public class ExprCompilerJavassistTest {

    public static void main(String[] args) {

        String el = "arg.a+arg.b+@max(140,113,arg.b)";

        Map aa = new HashMap();
        aa.put("a", 120);
        aa.put("b", 150);

        Map map = new HashMap();
        map.put("arg", aa);
        map.put("b", 8);
        map.put("c", 1);
        map.put("中路", 1d);

        EvaluateEnvironment evaluateEnvironment = EvaluateEnvironment.create(map);
        evaluateEnvironment.registerStaticMethods(Math.class);

        double result = 0, a = 4, b = 5;
        CompilerEnvironment compileEnvironment = new CompilerEnvironment();
        compileEnvironment.setSkipParse(true);
        compileEnvironment.setVariableType(int.class, "arg.a", "arg.b");
        compileEnvironment.registerJavassistFunction("max", new JavassistExprFunction<Integer, Integer>() {

            @Override
            public Integer execute(Integer... p) {
                return 1;
            }
        }, int.class, int.class, int.class, int.class);

        System.out.println(CompilerExpression.generateJavaCode(el, compileEnvironment));

        long s1 = System.currentTimeMillis();
        CompilerExpression compiler = CompilerExpression.compile(el, compileEnvironment, CompilerExpression.Coder.Javassist);
        long s2 = System.currentTimeMillis();
        System.out.println("==== eval2 " + compiler.evaluate(map));

        Object r = Expression.eval(el, map);

        System.out.println(" == compile " + (s2 - s1));

        long l1 = System.currentTimeMillis();
        for (int i = 0 ; i < 100000000; i++) {
        }

        long l2 = System.currentTimeMillis();
        System.out.println(l2 - l1);
        System.out.println(result);
    }


}
