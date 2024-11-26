package com.wast.wiki.expression;

import io.github.wycst.wast.common.expression.EvaluateEnvironment;
import io.github.wycst.wast.common.expression.Expression;
import io.github.wycst.wast.log.Log;
import io.github.wycst.wast.log.LogFactory;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

/**
 * 嵌套计算变量支持 v0.0.19+
 *
 * @Date 2024/11/16 11:00
 * @Created by wangyc
 */
public class ExpressionNestComputedExample {

    static Log log = LogFactory.getLog(ExpressionNestComputedExample.class);

    public static void main(String[] args) {

        EvaluateEnvironment environment = EvaluateEnvironment.create();
        Map vars = new HashMap();
        vars.put("a", 10);
        vars.put("b", 2);
        vars.put("c", 3);

        // 定义一个拓展变量d和2个计算变量e和f
        environment.binding("d", 8);
        environment.bindingComputed("e", "a + b - c * d");  // e = -12
        environment.bindingComputed("f", "e << 1");         // f = -24
        environment.registerStaticMethods(true, Arrays.class, System.class);

        log.info("run first ====================");
        // 注意：运行时必须带上environment
        log.info("e -> {}", Expression.eval("e", vars, environment));
        log.info("f -> {}", Expression.eval("f", vars, environment));
        log.info("           -> {}", Expression.eval("asList(a, b, c, d, e, f)", vars, environment));
        log.info("a + e + f  -> {}", Expression.eval("a + e + f", vars, environment));

        // 修改输入变量a,b,c 都放大10倍
        vars.put("a", 100);
        vars.put("b", 20);
        vars.put("c", 30);
        log.info("run second ====================");
        log.info("e -> {}", Expression.eval("e", vars, environment));
        log.info("f -> {}", Expression.eval("f", vars, environment));
        log.info("           -> {}", Expression.eval("asList(a, b, c, d, e, f)", vars, environment));
        log.info("a + e + f  -> {}", Expression.eval("a + e + f", vars, environment));
    }

}
