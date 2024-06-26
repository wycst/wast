package com.wast.test.expression;

import io.github.wycst.wast.common.expression.Expression;

import java.util.HashMap;
import java.util.Map;

/**
 * @Author: wangy
 * @Date: 2021/11/28 21:20
 * @Description:
 */
public class ExpressionTemplateTest {

    public static void main(String[] args) {

        Map context = new HashMap();
        context.put("name", "zhangsan");
        context.put("msg", "hello");

        String template = "aasdsdsd-${ name }-aasdsdsd-${msg}-${name}-ffffff";
        String msg = null;

        long begin = System.currentTimeMillis();
        for (int i = 0 ; i < 1000000; i++) {
            msg = Expression.renderTemplate(template, "${", "}" ,context);
//              msg = StringUtils.replaceGroupRegex(template, context);
        }
        long end = System.currentTimeMillis();
        System.out.println(end - begin);
        System.out.println(msg);
    }

}
