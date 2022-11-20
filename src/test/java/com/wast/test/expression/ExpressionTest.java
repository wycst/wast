package com.wast.test.expression;

import io.github.wycst.wast.common.expression.ExprParser;
import io.github.wycst.wast.common.expression.Expression;
import io.github.wycst.wast.common.expression.compile.CompilerExpression;

import java.util.HashMap;
import java.util.Map;

/**
 * @Author wangyunchao
 * @Date 2021/11/3 19:20
 */
public class ExpressionTest {


    public static void main(String[] args) {

        String longExpr = "1111111111111111111111111111111111111111111111111111111111111111111*3";
        System.out.println(Expression.eval(longExpr));
        long sq =1 - -(1+2);
        double s = -0xaE1;
        System.out.println(s);
//        Expression expression = ExpressionCompiler.compile(longExpr, true);
//        System.out.println(expression.evaluate());
        System.out.println( 1 - 1.223e-103);

        long s1 = System.currentTimeMillis();
        long end1 = System.currentTimeMillis();
        System.out.println(" compile " + (end1 - s1));

        Map context = new HashMap();
        context.put("a", 14);
        context.put("b", 2);
        context.put("c", 12);
        context.put("g", 2);

        Map map = new HashMap();
        map.put("a", 14);
        map.put("b", 2);
        map.put("c", 12);
        map.put("g", 2);

        System.out.println(Expression.renderTemplate("asdssd${a + b + c}sdsdsdsd${ a < b ? 'hello' : 'hello2'}qqqqq", map));


        Expression varExpr = Expression.parse("100 * 15 / 20 - (((((25 + 4 - 1050))))) * a * (4.0 - 16 * 3 / b / 10.0 + 182 / 2.0 * 3 / 121.0) + 15 % c - 12 * 14 / 5.0");
//        varExpr = Expression.compile("3 + a * 2 - 6 == c + g + c - 1");

        String exprStr = "100.1e-103 * 1 / 20.1e-123 *  (25 + 4 - 1050) * 14 * (4.0 - 16 * 3 / 2 / 10.0 + 182 / 2.0 * 3 / 121.0) + 15 % 12 - 12 * 14 / 5.0+ 15 % 12 - 12 * 14 / 5.0 + 100 * 15 / 20 *  (25 + 4 - 1050) * 14 * (4.0 - 16 * 3 / 2 / 10.0 + 182 / 2.0 * 3 / 121.0) + 15 % 12 - 12 * 14 / 5.0+ 15 % 12 - 12 * 14 / 5.0 + 100 * 15 / 20 *  (25 + 4 - 1050) * 14 * (4.0 - 16 * 3 / 2 / 10.0 + 182 / 2.0 * 3 / 121.0) + 15 % 12 - 12 * 14 / 5.0+ 15 % 12 - 12 * 14 / 5.0 + 100 * 15 / 20 *  (25 + 4 - 1050) * 14 * (4.0 - 16 * 3 / 2 / 10.0 + 182 / 2.0 * 3 / 121.0) + 15 % 12 - 12 * 14 / 5.0+ 15 % 12 - 12 * 14 / 5.0 + 100 * 15 / 20 *  (25 + 4 - 1050) * 14 * (4.0 - 16 * 3 / 2 / 10.0 + 182 / 2.0 * 3 / 121.0) + 15 % 12 - 12 * 14 / 5.0+ 15 % 12 - 12 * 14 / 5.0 + 100 * 15 / 20 *  (25 + 4 - 1050) * 14 * (4.0 - 16 * 3 / 2 / 10.0 + 182 / 2.0 * 3 / 121.0) + 15 % 12 - 12 * 14 / 5.0+ 15 % 12 - 12 * 14 / 5.0 + 100 * 15 / 20 *  (25 + 4 - 1050) * 14 * (4.0 - 16 * 3 / 2 / 10.0 + 182 / 2.0 * 3 / 121.0) + 15 % 12 - 12 * 14 / 5.0+ 15 % 12 - 12 * 14 / 5.0 + 100 * 15 / 20 *  (25 + 4 - 1050) * 14 * (4.0 - 16 * 3 / 2 / 10.0 + 182 / 2.0 * 3 / 121.0) + 15 % 12 - 12 * 14 / 5.0+ 15 % 12 - 12 * 14 / 5.0 + 100 * 15 / 20 *  (25 + 4 - 1050) * 14 * (4.0 - 16 * 3 / 2 / 10.0 + 182 / 2.0 * 3 / 121.0) + 15 % 12 - 12 * 14 / 5.0+ 15 % 12 - 12 * 14 / 5.0 + 100 * 15 / 20 *  (25 + 4 - 1050) * 14 * (4.0 - 16 * 3 / 2 / 10.0 + 182 / 2.0 * 3 / 121.0) + 15 % 12 - 12 * 14 / 5.0+ 15 % 12 - 12 * 14 / 5.0 + 100 * 15 / 20 *  (25 + 4 - 1050) * 14 * (4.0 - 16 * 3 / 2 / 10.0 + 182 / 2.0 * 3 / 121.0) + 15 % 12 - 12 * 14 / 5.0+ 15 % 12 - 12 * 14 / 5.0 + 100 * 15 / 20 *  (25 + 4 - 1050) * 14 * (4.0 - 16 * 3 / 2 / 10.0 + 182 / 2.0 * 3 / 121.0) + 15 % 12 - 12 * 14 / 5.0+ 15 % 12 - 12 * 14 / 5.0 + 100 * 15 / 20 *  (25 + 4 - 1050) * 14 * (4.0 - 16 * 3 / 2 / 10.0 + 182 / 2.0 * 3 / 121.0) + 15 % 12 - 12 * 14 / 5.0+ 15 % 12 - 12 * 14 / 5.0 + 100 * 15 / 20 *  (25 + 4 - 1050) * 14 * (4.0 - 16 * 3 / 2 / 10.0 + 182 / 2.0 * 3 / 121.0) + 15 % 12 - 12 * 14 / 5.0+ 15 % 12 - 12 * 14 / 5.0 + 100 * 15 / 20 *  (25 + 4 - 1050) * 14 * (4.0 - 16 * 3 / 2 / 10.0 + 182 / 2.0 * 3 / 121.0) + 15 % 12 - 12 * 14 / 5.0+ 15 % 12 - 12 * 14 / 5.0 + 100 * 15 / 20 *  (25 + 4 - 1050) * 14 * (4.0 - 16 * 3 / 2 / 10.0 + 182 / 2.0 * 3 / 121.0) + 15 % 12 - 12 * 14 / 5.0+ 15 % 12 - 12 * 14 / 5.0 + 100 * 15 / 20 *  (25 + 4 - 1050) * 14 * (4.0 - 16 * 3 / 2 / 10.0 + 182 / 2.0 * 3 / 121.0) + 15 % 12 - 12 * 14 / 5.0+ 15 % 12 - 12 * 14 / 5.0 + 100 * 15 / 20 *  (25 + 4 - 1050) * 14 * (4.0 - 16 * 3 / 2 / 10.0 + 182 / 2.0 * 3 / 121.0) + 15 % 12 - 12 * 14 / 5.0+ 15 % 12 - 12 * 14 / 5.0 + 100 * 15 / 20 *  (25 + 4 - 1050) * 14 * (4.0 - 16 * 3 / 2 / 10.0 + 182 / 2.0 * 3 / 121.0) + 15 % 12 - 12 * 14 / 5.0+ 15 % 12 - 12 * 14 / 5.0 + 100 * 15 / 20 *  (25 + 4 - 1050) * 14 * (4.0 - 16 * 3 / 2 / 10.0 + 182 / 2.0 * 3 / 121.0) + 15 % 12 - 12 * 14 / 5.0+ 15 % 12 - 12 * 14 / 5.0 + 100 * 1 / 20 *  (25 + 4 - 1050) * 14 * (4.0 - 16 * 3 / 2 / 10.0 + 182 / 2.0 * 3 / 121.0) + 15 % 12 - 12 * 14 / 5.0+ 15 % 12 - 12 * 14 / 5.0 + 100 * 15 / 20 *  (25 + 4 - 1050) * 14 * (4.0 - 16 * 3 / 2 / 10.0 + 182 / 2.0 * 3 / 121.0) + 15 % 12 - 12 * 14 / 5.0+ 15 % 12 - 12 * 14 / 5.0 + 100 * 15 / 20 *  (25 + 4 - 1050) * 14 * (4.0 - 16 * 3 / 2 / 10.0 + 182 / 2.0 * 3 / 121.0) + 15 % 12 - 12 * 14 / 5.0+ 15 % 12 - 12 * 14 / 5.0 + 100 * 15 / 20 *  (25 + 4 - 1050) * 14 * (4.0 - 16 * 3 / 2 / 10.0 + 182 / 2.0 * 3 / 121.0) + 15 % 12 - 12 * 14 / 5.0+ 15 % 12 - 12 * 14 / 5.0 + 100 * 15 / 20 *  (25 + 4 - 1050) * 14 * (4.0 - 16 * 3 / 2 / 10.0 + 182 / 2.0 * 3 / 121.0) + 15 % 12 - 12 * 14 / 5.0+ 15 % 12 - 12 * 14 / 5.0 + 100 * 15 / 20 *  (25 + 4 - 1050) * 14 * (4.0 - 16 * 3 / 2 / 10.0 + 182 / 2.0 * 3 / 121.0) + 15 % 12 - 12 * 14 / 5.0+ 15 % 12 - 12 * 14 / 5.0 + 100 * 15 / 20 *  (25 + 4 - 1050) * 14 * (4.0 - 16 * 3 / 2 / 10.0 + 182 / 2.0 * 3 / 121.0) + 15 % 12 - 12 * 14 / 5.0+ 15 % 12 - 12 * 14 / 5.0 + 100 * 15 / 20 *  (25 + 4 - 1050) * 14 * (4.0 - 16 * 3 / 2 / 10.0 + 182 / 2.0 * 3 / 121.0) + 15 % 12 - 12 * 14 / 5.0+ 15 % 12 - 12 * 14 / 5.0 + 100 * 15 / 20 *  (25 + 4 - 1050) * 14 * (4.0 - 16 * 3 / 2 / 10.0 + 182 / 2.0 * 3 / 121.0) + 15 % 12 - 12 * 14 / 5.0+ 15 % 12 - 12 * 14 / 5.0 + 100 * 15 / 20 *  (25 + 4 - 1050) * 14 * (4.0 - 16 * 3 / 2 / 10.0 + 182 / 2.0 * 3 / 121.0) + 15 % 12 - 12 * 14 / 5.0+ 15 % 12 - 12 * 14 / 5.0 + 100 * 15 / 20 *  (25 + 4 - 1050) * 14 * (4.0 - 16 * 3 / 2 / 10.0 + 182 / 2.0 * 3 / 121.0) + 15 % 12 - 12 * 14 / 5.0+ 15 % 12 - 12 * 14 / 5.0 + 100 * 15 / 20 *  (25 + 4 - 1050) * 14 * (4.0 - 16 * 3 / 2 / 10.0 + 182 / 2.0 * 3 / 121.0) + 15 % 12 - 12 * 14 / 5.0+ 15 % 12 - 12 * 14 / 5.0 + 100 * 15 / 20 *  (25 + 4 - 1050) * 14 * (4.0 - 16 * 3 / 2 / 10.0 + 182 / 2.0 * 3 / 121.0) + 15 % 12 - 12 * 14 / 5.0+ 15 % 12 - 12 * 14 / 5.0 + 100 * 15 / 20 *  (25 + 4 - 1050) * 14 * (4.0 - 16 * 3 / 2 / 10.0 + 182 / 2.0 * 3 / 121.0) + 15 % 12 - 12 * 14 / 5.0+ 15 % 12 - 12 * 14 / 5.0 + 100 * 15 / 20 *  (25 + 4 - 1050) * 14 * (4.0 - 16 * 3 / 2 / 10.0 + 182 / 2.0 * 3 / 121.0) + 15 % 12 - 12 * 14 / 5.0+ 15 % 12 - 12 * 14 / 5.0 + 100 * 15 / 20 *  (25 + 4 - 1050) * 14 * (4.0 - 16 * 3 / 2 / 10.0 + 182 / 2.0 * 3 / 121.0) + 15 % 12 - 12 * 14 / 5.0+ 15 % 12 - 12 * 14 / 5.0 + 100 * 15 / 20 *  (25 + 4 - 1050) * 14 * (4.0 - 16 * 3 / 2 / 10.0 + 182 / 2.0 * 3 / 121.0) + 15 % 12 - 12 * 14 / 5.0+ 15 % 12 - 12 * 14 / 5.0 + 100 * 15 / 20 *  (25 + 4 - 1050) * 14 * (4.0 - 16 * 3 / 2 / 10.0 + 182 / 2.0 * 3 / 121.0) + 15 % 12 - 12 * 14 / 5.0+ 15 % 12 - 12 * 14 / 5.0 + 100 * 15 / 20 *  (25 + 4 - 1050) * 14 * (4.0 - 16 * 3 / 2 / 10.0 + 182 / 2.0 * 3 / 121.0) + 15 % 12 - 12 * 14 / 5.0+ 15 % 12 - 12 * 14 / 5.0 + 100 * 15 / 20 *  (25 + 4 - 1050) * 14 * (4.0 - 16 * 3 / 2 / 10.0 + 182 / 2.0 * 3 / 121.0) + 15 % 12 - 12 * 14 / 5.0+ 15 % 12 - 12 * 14 / 5.0 + 100 * 1 / 20 *  (25 + 4 - 1050) * 14 * (4.0 - 16 * 3 / 2 / 10.0 + 182 / 2.0 * 3 / 121.0) + 15 % 12 - 12 * 14 / 5.0+ 15 % 12 - 12 * 14 / 5.0 + 100 * 15 / 20 *  (25 + 4 - 1050) * 14 * (4.0 - 16 * 3 / 2 / 10.0 + 182 / 2.0 * 3 / 121.0) + 15 % 12 - 12 * 14 / 5.0+ 15 % 12 - 12 * 14 / 5.0 + 100 * 15 / 20 *  (25 + 4 - 1050) * 14 * (4.0 - 16 * 3 / 2 / 10.0 + 182 / 2.0 * 3 / 121.0) + 15 % 12 - 12 * 14 / 5.0+ 15 % 12 - 12 * 14 / 5.0 + 100 * 15 / 20 *  (25 + 4 - 1050) * 14 * (4.0 - 16 * 3 / 2 / 10.0 + 182 / 2.0 * 3 / 121.0) + 15 % 12 - 12 * 14 / 5.0+ 15 % 12 - 12 * 14 / 5.0 + 100 * 15 / 20 *  (25 + 4 - 1050) * 14 * (4.0 - 16 * 3 / 2 / 10.0 + 182 / 2.0 * 3 / 121.0) + 15 % 12 - 12 * 14 / 5.0+ 15 % 12 - 12 * 14 / 5.0 + 100 * 15 / 20 *  (25 + 4 - 1050) * 14 * (4.0 - 16 * 3 / 2 / 10.0 + 182 / 2.0 * 3 / 121.0) + 15 % 12 - 12 * 14 / 5.0+ 15 % 12 - 12 * 14 / 5.0 + 100 * 15 / 20 *  (25 + 4 - 1050) * 14 * (4.0 - 16 * 3 / 2 / 10.0 + 182 / 2.0 * 3 / 121.0) + 15 % 12 - 12 * 14 / 5.0+ 15 % 12 - 12 * 14 / 5.0 + 100 * 15 / 20 *  (25 + 4 - 1050) * 14 * (4.0 - 16 * 3 / 2 / 10.0 + 182 / 2.0 * 3 / 121.0) + 15 % 12 - 12 * 14 / 5.0+ 15 % 12 - 12 * 14 / 5.0 + 100 * 15 / 20 *  (25 + 4 - 1050) * 14 * (4.0 - 16 * 3 / 2 / 10.0 + 182 / 2.0 * 3 / 121.0) + 15 % 12 - 12 * 14 / 5.0+ 15 % 12 - 12 * 14 / 5.0 + 100 * 15 / 20 *  (25 + 4 - 1050) * 14 * (4.0 - 16 * 3 / 2 / 10.0 + 182 / 2.0 * 3 / 121.0) + 15 % 12 - 12 * 14 / 5.0+ 15 % 12 - 12 * 14 / 5.0 + 100 * 15 / 20 *  (25 + 4 - 1050) * 14 * (4.0 - 16 * 3 / 2 / 10.0 + 182 / 2.0 * 3 / 121.0) + 15 % 12 - 12 * 14 / 5.0+ 15 % 12 - 12 * 14 / 5.0 + 100 * 15 / 20 *  (25 + 4 - 1050) * 14 * (4.0 - 16 * 3 / 2 / 10.0 + 182 / 2.0 * 3 / 121.0) + 15 % 12 - 12 * 14 / 5.0+ 15 % 12 - 12 * 14 / 5.0 + 100 * 15 / 20 *  (25 + 4 - 1050) * 14 * (4.0 - 16 * 3 / 2 / 10.0 + 182 / 2.0 * 3 / 121.0) + 15 % 12 - 12 * 14 / 5.0+ 15 % 12 - 12 * 14 / 5.0 + 100 * 15 / 20 *  (25 + 4 - 1050) * 14 * (4.0 - 16 * 3 / 2 / 10.0 + 182 / 2.0 * 3 / 121.0) + 15 % 12 - 12 * 14 / 5.0+ 15 % 12 - 12 * 14 / 5.0 + 100 * 15 / 20 *  (25 + 4 - 1050) * 14 * (4.0 - 16 * 3 / 2 / 10.0 + 182 / 2.0 * 3 / 121.0) + 15 % 12 - 12 * 14 / 5.0+ 15 % 12 - 12 * 14 / 5.0 + 100 * 15 / 20 *  (25 + 4 - 1050) * 14 * (4.0 - 16 * 3 / 2 / 10.0 + 182 / 2.0 * 3 / 121.0) + 15 % 12 - 12 * 14 / 5.0+ 15 % 12 - 12 * 14 / 5.0 + 100 * 15 / 20 *  (25 + 4 - 1050) * 14 * (4.0 - 16 * 3 / 2 / 10.0 + 182 / 2.0 * 3 / 121.0) + 15 % 12 - 12 * 14 / 5.0+ 15 % 12 - 12 * 14 / 5.0 + 100 * 15 / 20 *  (25 + 4 - 1050) * 14 * (4.0 - 16 * 3 / 2 / 10.0 + 182 / 2.0 * 3 / 121.0) + 15 % 12 - 12 * 14 / 5.0+ 15 % 12 - 12 * 14 / 5.0 + 100 * 15 / 20 *  (25 + 4 - 1050) * 14 * (4.0 - 16 * 3 / 2 / 10.0 + 182 / 2.0 * 3 / 121.0) + 15 % 12 - 12 * 14 / 5.0+ 15 % 12 - 12 * 14 / 5.0 + 100 * 15 / 20 *  (25 + 4 - 1050) * 14 * (4.0 - 16 * 3 / 2 / 10.0 + 182 / 2.0 * 3 / 121.0) + 15 % 12 - 12 * 14 / 5.0+ 15 % 12 - 12 * 14 / 5.0 + 100 * 1 / 20 *  (25 + 4 - 1050) * 14 * (4.0 - 16 * 3 / 2 / 10.0 + 182 / 2.0 * 3 / 121.0) + 15 % 12 - 12 * 14 / 5.0+ 15 % 12 - 12 * 14 / 5.0 + 100 * 15 / 20 *  (25 + 4 - 1050) * 14 * (4.0 - 16 * 3 / 2 / 10.0 + 182 / 2.0 * 3 / 121.0) + 15 % 12 - 12 * 14 / 5.0+ 15 % 12 - 12 * 14 / 5.0 + 100 * 15 / 20 *  (25 + 4 - 1050) * 14 * (4.0 - 16 * 3 / 2 / 10.0 + 182 / 2.0 * 3 / 121.0) + 15 % 12 - 12 * 14 / 5.0+ 15 % 12 - 12 * 14 / 5.0 + 100 * 15 / 20 *  (25 + 4 - 1050) * 14 * (4.0 - 16 * 3 / 2 / 10.0 + 182 / 2.0 * 3 / 121.0) + 15 % 12 - 12 * 14 / 5.0+ 15 % 12 - 12 * 14 / 5.0 + 100 * 15 / 20 *  (25 + 4 - 1050) * 14 * (4.0 - 16 * 3 / 2 / 10.0 + 182 / 2.0 * 3 / 121.0) + 15 % 12 - 12 * 14 / 5.0+ 15 % 12 - 12 * 14 / 5.0 + 100 * 15 / 20 *  (25 + 4 - 1050) * 14 * (4.0 - 16 * 3 / 2 / 10.0 + 182 / 2.0 * 3 / 121.0) + 15 % 12 - 12 * 14 / 5.0+ 15 % 12 - 12 * 14 / 5.0 + 100 * 15 / 20 *  (25 + 4 - 1050) * 14 * (4.0 - 16 * 3 / 2 / 10.0 + 182 / 2.0 * 3 / 121.0) + 15 % 12 - 12 * 14 / 5.0+ 15 % 12 - 12 * 14 / 5.0 + 100 * 15 / 20 *  (25 + 4 - 1050) * 14 * (4.0 - 16 * 3 / 2 / 10.0 + 182 / 2.0 * 3 / 121.0) + 15 % 12 - 12 * 14 / 5.0+ 15 % 12 - 12 * 14 / 5.0 + 100 * 15 / 20 *  (25 + 4 - 1050) * 14 * (4.0 - 16 * 3 / 2 / 10.0 + 182 / 2.0 * 3 / 121.0) + 15 % 12 - 12 * 14 / 5.0+ 15 % 12 - 12 * 14 / 5.0 + 100 * 15 / 20 *  (25 + 4 - 1050) * 14 * (4.0 - 16 * 3 / 2 / 10.0 + 182 / 2.0 * 3 / 121.0) + 15 % 12 - 12 * 14 / 5.0+ 15 % 12 - 12 * 14 / 5.0 + 100 * 15 / 20 *  (25 + 4 - 1050) * 14 * (4.0 - 16 * 3 / 2 / 10.0 + 182 / 2.0 * 3 / 121.0) + 15 % 12 - 12 * 14 / 5.0+ 15 % 12 - 12 * 14 / 5.0 + 100 * 15 / 20 *  (25 + 4 - 1050) * 14 * (4.0 - 16 * 3 / 2 / 10.0 + 182 / 2.0 * 3 / 121.0) + 15 % 12 - 12 * 14 / 5.0+ 15 % 12 - 12 * 14 / 5.0 + 100 * 15 / 20 *  (25 + 4 - 1050) * 14 * (4.0 - 16 * 3 / 2 / 10.0 + 182 / 2.0 * 3 / 121.0) + 15 % 12 - 12 * 14 / 5.0+ 15 % 12 - 12 * 14 / 5.0 + 100 * 15 / 20 *  (25 + 4 - 1050) * 14 * (4.0 - 16 * 3 / 2 / 10.0 + 182 / 2.0 * 3 / 121.0) + 15 % 12 - 12 * 14 / 5.0+ 15 % 12 - 12 * 14 / 5.0 + 100 * 15 / 20 *  (25 + 4 - 1050) * 14 * (4.0 - 16 * 3 / 2 / 10.0 + 182 / 2.0 * 3 / 121.0) + 15 % 12 - 12 * 14 / 5.0+ 15 % 12 - 12 * 14 / 5.0 + 100 * 15 / 20 *  (25 + 4 - 1050) * 14 * (4.0 - 16 * 3 / 2 / 10.0 + 182 / 2.0 * 3 / 121.0) + 15 % 12 - 12 * 14 / 5.0+ 15 % 12 - 12 * 14 / 5.0 + 100 * 15 / 20 *  (25 + 4 - 1050) * 14 * (4.0 - 16 * 3 / 2 / 10.0 + 182 / 2.0 * 3 / 121.0) + 15 % 12 - 12 * 14 / 5.0+ 15 % 12 - 12 * 14 / 5.0 + 100 * 15 / 20 *  (25 + 4 - 1050) * 14 * (4.0 - 16 * 3 / 2 / 10.0 + 182 / 2.0 * 3 / 121.0) + 15 % 12 - 12 * 14 / 5.0+ 15 % 12 - 12 * 14 / 5.0 + 100 * 15 / 20 *  (25 + 4 - 1050) * 14 * (4.0 - 16 * 3 / 2 / 10.0 + 182 / 2.0 * 3 / 121.0) + 15 % 12 - 12 * 14 / 5.0+ 15 % 12 - 12 * 14 / 5.0 + 100 * 15 / 20 *  (25 + 4 - 1050) * 14 * (4.0 - 16 * 3 / 2 / 10.0 + 182 / 2.0 * 3 / 121.0) + 15 % 12 - 12 * 14 / 5.0+ 15 % 12 - 12 * 14 / 6.0";

        long l = System.currentTimeMillis();
        Object result = null;
        for (int i = 0 ; i < 1000 ; i++) {
            // new ExprParserTest("100 * 5 / 20 * 25 + 4 - 5 * 4 + 6 * 3 + 2 / 10 + 8 / 2 * 3 / 12");
//            result = Expression.eval("100 * 15 / 20 - (((((25 + 4 - 1050))))) * 14 * (4.0 - 16 * 3 / 2 / 10.0 + 182 / 2.0 * 3 / 121.0) + 15 % 12 - 12 * 14 / 5.0");
//            result = Expression.eval("100 * 15 / 20 - (((((25 + 4 - 1050))))) * 14 * (4.0 - 16 * 3 / 2 / 10.0 + 182 / 2.0 * 3 / 121.0) + 15 % 12 - 12 * 14 / 5.0");
//            result = exprParser.evaluate();
//            result = Expression.eval("aa.cc.dd.ee * bb - (5 + ff) * g", map);

//            varExpr = Expression.parse("100 * 15 / 20 - (((((25 + 4 - 1050))))) * a * (4.0 - 16 * 3 / b / 10.0 + 182 / 2.0 * 3 / 121.0) + 15 % c - 12 * 14 / 5.0");

//            Expression.parse("100 * 15 / 20 - (((((25 + 4 - 1050))))) * a * (4.0 - 16 * 3 / b / 10.0 + 182 / 2.0 * 3 / 121.0) + 15 % c - 12 * 14 / 5.0");

//            result = Expression.eval("123 in {111, 123, 333}");

            result = io.github.wycst.wast.common.expression.Expression.eval("100 * 1 / 20 *  (25 + 4 - 1050) * 14 * (4.0 - 16 * 3 / 2 / 10.0 + 182 / 2.0 * 3 / 121.0) + 15 % 12 - 12 * 14 / 5.0+ 15 % 12 - 12 * 14 / 5.0 + 100 * 15 / 20 *  (25 + 4 - 1050) * 14 * (4.0 - 16 * 3 / 2 / 10.0 + 182 / 2.0 * 3 / 121.0) + 15 % 12 - 12 * 14 / 5.0+ 15 % 12 - 12 * 14 / 5.0 + 100 * 15 / 20 *  (25 + 4 - 1050) * 14 * (4.0 - 16 * 3 / 2 / 10.0 + 182 / 2.0 * 3 / 121.0) + 15 % 12 - 12 * 14 / 5.0+ 15 % 12 - 12 * 14 / 5.0 + 100 * 15 / 20 *  (25 + 4 - 1050) * 14 * (4.0 - 16 * 3 / 2 / 10.0 + 182 / 2.0 * 3 / 121.0) + 15 % 12 - 12 * 14 / 5.0+ 15 % 12 - 12 * 14 / 5.0 + 100 * 15 / 20 *  (25 + 4 - 1050) * 14 * (4.0 - 16 * 3 / 2 / 10.0 + 182 / 2.0 * 3 / 121.0) + 15 % 12 - 12 * 14 / 5.0+ 15 % 12 - 12 * 14 / 5.0 + 100 * 15 / 20 *  (25 + 4 - 1050) * 14 * (4.0 - 16 * 3 / 2 / 10.0 + 182 / 2.0 * 3 / 121.0) + 15 % 12 - 12 * 14 / 5.0+ 15 % 12 - 12 * 14 / 5.0 + 100 * 15 / 20 *  (25 + 4 - 1050) * 14 * (4.0 - 16 * 3 / 2 / 10.0 + 182 / 2.0 * 3 / 121.0) + 15 % 12 - 12 * 14 / 5.0+ 15 % 12 - 12 * 14 / 5.0 + 100 * 15 / 20 *  (25 + 4 - 1050) * 14 * (4.0 - 16 * 3 / 2 / 10.0 + 182 / 2.0 * 3 / 121.0) + 15 % 12 - 12 * 14 / 5.0+ 15 % 12 - 12 * 14 / 5.0 + 100 * 15 / 20 *  (25 + 4 - 1050) * 14 * (4.0 - 16 * 3 / 2 / 10.0 + 182 / 2.0 * 3 / 121.0) + 15 % 12 - 12 * 14 / 5.0+ 15 % 12 - 12 * 14 / 5.0 + 100 * 15 / 20 *  (25 + 4 - 1050) * 14 * (4.0 - 16 * 3 / 2 / 10.0 + 182 / 2.0 * 3 / 121.0) + 15 % 12 - 12 * 14 / 5.0+ 15 % 12 - 12 * 14 / 5.0 + 100 * 15 / 20 *  (25 + 4 - 1050) * 14 * (4.0 - 16 * 3 / 2 / 10.0 + 182 / 2.0 * 3 / 121.0) + 15 % 12 - 12 * 14 / 5.0+ 15 % 12 - 12 * 14 / 5.0 + 100 * 15 / 20 *  (25 + 4 - 1050) * 14 * (4.0 - 16 * 3 / 2 / 10.0 + 182 / 2.0 * 3 / 121.0) + 15 % 12 - 12 * 14 / 5.0+ 15 % 12 - 12 * 14 / 5.0 + 100 * 15 / 20 *  (25 + 4 - 1050) * 14 * (4.0 - 16 * 3 / 2 / 10.0 + 182 / 2.0 * 3 / 121.0) + 15 % 12 - 12 * 14 / 5.0+ 15 % 12 - 12 * 14 / 5.0 + 100 * 15 / 20 *  (25 + 4 - 1050) * 14 * (4.0 - 16 * 3 / 2 / 10.0 + 182 / 2.0 * 3 / 121.0) + 15 % 12 - 12 * 14 / 5.0+ 15 % 12 - 12 * 14 / 5.0 + 100 * 15 / 20 *  (25 + 4 - 1050) * 14 * (4.0 - 16 * 3 / 2 / 10.0 + 182 / 2.0 * 3 / 121.0) + 15 % 12 - 12 * 14 / 5.0+ 15 % 12 - 12 * 14 / 5.0 + 100 * 15 / 20 *  (25 + 4 - 1050) * 14 * (4.0 - 16 * 3 / 2 / 10.0 + 182 / 2.0 * 3 / 121.0) + 15 % 12 - 12 * 14 / 5.0+ 15 % 12 - 12 * 14 / 5.0 + 100 * 15 / 20 *  (25 + 4 - 1050) * 14 * (4.0 - 16 * 3 / 2 / 10.0 + 182 / 2.0 * 3 / 121.0) + 15 % 12 - 12 * 14 / 5.0+ 15 % 12 - 12 * 14 / 5.0 + 100 * 15 / 20 *  (25 + 4 - 1050) * 14 * (4.0 - 16 * 3 / 2 / 10.0 + 182 / 2.0 * 3 / 121.0) + 15 % 12 - 12 * 14 / 5.0+ 15 % 12 - 12 * 14 / 5.0 + 100 * 15 / 20 *  (25 + 4 - 1050) * 14 * (4.0 - 16 * 3 / 2 / 10.0 + 182 / 2.0 * 3 / 121.0) + 15 % 12 - 12 * 14 / 5.0+ 15 % 12 - 12 * 14 / 5.0 + 100 * 15 / 20 *  (25 + 4 - 1050) * 14 * (4.0 - 16 * 3 / 2 / 10.0 + 182 / 2.0 * 3 / 121.0) + 15 % 12 - 12 * 14 / 5.0+ 15 % 12 - 12 * 14 / 5.0 + 100 * 1 / 20 *  (25 + 4 - 1050) * 14 * (4.0 - 16 * 3 / 2 / 10.0 + 182 / 2.0 * 3 / 121.0) + 15 % 12 - 12 * 14 / 5.0+ 15 % 12 - 12 * 14 / 5.0 + 100 * 15 / 20 *  (25 + 4 - 1050) * 14 * (4.0 - 16 * 3 / 2 / 10.0 + 182 / 2.0 * 3 / 121.0) + 15 % 12 - 12 * 14 / 5.0+ 15 % 12 - 12 * 14 / 5.0 + 100 * 15 / 20 *  (25 + 4 - 1050) * 14 * (4.0 - 16 * 3 / 2 / 10.0 + 182 / 2.0 * 3 / 121.0) + 15 % 12 - 12 * 14 / 5.0+ 15 % 12 - 12 * 14 / 5.0 + 100 * 15 / 20 *  (25 + 4 - 1050) * 14 * (4.0 - 16 * 3 / 2 / 10.0 + 182 / 2.0 * 3 / 121.0) + 15 % 12 - 12 * 14 / 5.0+ 15 % 12 - 12 * 14 / 5.0 + 100 * 15 / 20 *  (25 + 4 - 1050) * 14 * (4.0 - 16 * 3 / 2 / 10.0 + 182 / 2.0 * 3 / 121.0) + 15 % 12 - 12 * 14 / 5.0+ 15 % 12 - 12 * 14 / 5.0 + 100 * 15 / 20 *  (25 + 4 - 1050) * 14 * (4.0 - 16 * 3 / 2 / 10.0 + 182 / 2.0 * 3 / 121.0) + 15 % 12 - 12 * 14 / 5.0+ 15 % 12 - 12 * 14 / 5.0 + 100 * 15 / 20 *  (25 + 4 - 1050) * 14 * (4.0 - 16 * 3 / 2 / 10.0 + 182 / 2.0 * 3 / 121.0) + 15 % 12 - 12 * 14 / 5.0+ 15 % 12 - 12 * 14 / 5.0 + 100 * 15 / 20 *  (25 + 4 - 1050) * 14 * (4.0 - 16 * 3 / 2 / 10.0 + 182 / 2.0 * 3 / 121.0) + 15 % 12 - 12 * 14 / 5.0+ 15 % 12 - 12 * 14 / 5.0 + 100 * 15 / 20 *  (25 + 4 - 1050) * 14 * (4.0 - 16 * 3 / 2 / 10.0 + 182 / 2.0 * 3 / 121.0) + 15 % 12 - 12 * 14 / 5.0+ 15 % 12 - 12 * 14 / 5.0 + 100 * 15 / 20 *  (25 + 4 - 1050) * 14 * (4.0 - 16 * 3 / 2 / 10.0 + 182 / 2.0 * 3 / 121.0) + 15 % 12 - 12 * 14 / 5.0+ 15 % 12 - 12 * 14 / 5.0 + 100 * 15 / 20 *  (25 + 4 - 1050) * 14 * (4.0 - 16 * 3 / 2 / 10.0 + 182 / 2.0 * 3 / 121.0) + 15 % 12 - 12 * 14 / 5.0+ 15 % 12 - 12 * 14 / 5.0 + 100 * 15 / 20 *  (25 + 4 - 1050) * 14 * (4.0 - 16 * 3 / 2 / 10.0 + 182 / 2.0 * 3 / 121.0) + 15 % 12 - 12 * 14 / 5.0+ 15 % 12 - 12 * 14 / 5.0 + 100 * 15 / 20 *  (25 + 4 - 1050) * 14 * (4.0 - 16 * 3 / 2 / 10.0 + 182 / 2.0 * 3 / 121.0) + 15 % 12 - 12 * 14 / 5.0+ 15 % 12 - 12 * 14 / 5.0 + 100 * 15 / 20 *  (25 + 4 - 1050) * 14 * (4.0 - 16 * 3 / 2 / 10.0 + 182 / 2.0 * 3 / 121.0) + 15 % 12 - 12 * 14 / 5.0+ 15 % 12 - 12 * 14 / 5.0 + 100 * 15 / 20 *  (25 + 4 - 1050) * 14 * (4.0 - 16 * 3 / 2 / 10.0 + 182 / 2.0 * 3 / 121.0) + 15 % 12 - 12 * 14 / 5.0+ 15 % 12 - 12 * 14 / 5.0 + 100 * 15 / 20 *  (25 + 4 - 1050) * 14 * (4.0 - 16 * 3 / 2 / 10.0 + 182 / 2.0 * 3 / 121.0) + 15 % 12 - 12 * 14 / 5.0+ 15 % 12 - 12 * 14 / 5.0 + 100 * 15 / 20 *  (25 + 4 - 1050) * 14 * (4.0 - 16 * 3 / 2 / 10.0 + 182 / 2.0 * 3 / 121.0) + 15 % 12 - 12 * 14 / 5.0+ 15 % 12 - 12 * 14 / 5.0 + 100 * 15 / 20 *  (25 + 4 - 1050) * 14 * (4.0 - 16 * 3 / 2 / 10.0 + 182 / 2.0 * 3 / 121.0) + 15 % 12 - 12 * 14 / 5.0+ 15 % 12 - 12 * 14 / 5.0 + 100 * 15 / 20 *  (25 + 4 - 1050) * 14 * (4.0 - 16 * 3 / 2 / 10.0 + 182 / 2.0 * 3 / 121.0) + 15 % 12 - 12 * 14 / 5.0+ 15 % 12 - 12 * 14 / 5.0 + 100 * 15 / 20 *  (25 + 4 - 1050) * 14 * (4.0 - 16 * 3 / 2 / 10.0 + 182 / 2.0 * 3 / 121.0) + 15 % 12 - 12 * 14 / 5.0+ 15 % 12 - 12 * 14 / 5.0 + 100 * 1 / 20 *  (25 + 4 - 1050) * 14 * (4.0 - 16 * 3 / 2 / 10.0 + 182 / 2.0 * 3 / 121.0) + 15 % 12 - 12 * 14 / 5.0+ 15 % 12 - 12 * 14 / 5.0 + 100 * 15 / 20 *  (25 + 4 - 1050) * 14 * (4.0 - 16 * 3 / 2 / 10.0 + 182 / 2.0 * 3 / 121.0) + 15 % 12 - 12 * 14 / 5.0+ 15 % 12 - 12 * 14 / 5.0 + 100 * 15 / 20 *  (25 + 4 - 1050) * 14 * (4.0 - 16 * 3 / 2 / 10.0 + 182 / 2.0 * 3 / 121.0) + 15 % 12 - 12 * 14 / 5.0+ 15 % 12 - 12 * 14 / 5.0 + 100 * 15 / 20 *  (25 + 4 - 1050) * 14 * (4.0 - 16 * 3 / 2 / 10.0 + 182 / 2.0 * 3 / 121.0) + 15 % 12 - 12 * 14 / 5.0+ 15 % 12 - 12 * 14 / 5.0 + 100 * 15 / 20 *  (25 + 4 - 1050) * 14 * (4.0 - 16 * 3 / 2 / 10.0 + 182 / 2.0 * 3 / 121.0) + 15 % 12 - 12 * 14 / 5.0+ 15 % 12 - 12 * 14 / 5.0 + 100 * 15 / 20 *  (25 + 4 - 1050) * 14 * (4.0 - 16 * 3 / 2 / 10.0 + 182 / 2.0 * 3 / 121.0) + 15 % 12 - 12 * 14 / 5.0+ 15 % 12 - 12 * 14 / 5.0 + 100 * 15 / 20 *  (25 + 4 - 1050) * 14 * (4.0 - 16 * 3 / 2 / 10.0 + 182 / 2.0 * 3 / 121.0) + 15 % 12 - 12 * 14 / 5.0+ 15 % 12 - 12 * 14 / 5.0 + 100 * 15 / 20 *  (25 + 4 - 1050) * 14 * (4.0 - 16 * 3 / 2 / 10.0 + 182 / 2.0 * 3 / 121.0) + 15 % 12 - 12 * 14 / 5.0+ 15 % 12 - 12 * 14 / 5.0 + 100 * 15 / 20 *  (25 + 4 - 1050) * 14 * (4.0 - 16 * 3 / 2 / 10.0 + 182 / 2.0 * 3 / 121.0) + 15 % 12 - 12 * 14 / 5.0+ 15 % 12 - 12 * 14 / 5.0 + 100 * 15 / 20 *  (25 + 4 - 1050) * 14 * (4.0 - 16 * 3 / 2 / 10.0 + 182 / 2.0 * 3 / 121.0) + 15 % 12 - 12 * 14 / 5.0+ 15 % 12 - 12 * 14 / 5.0 + 100 * 15 / 20 *  (25 + 4 - 1050) * 14 * (4.0 - 16 * 3 / 2 / 10.0 + 182 / 2.0 * 3 / 121.0) + 15 % 12 - 12 * 14 / 5.0+ 15 % 12 - 12 * 14 / 5.0 + 100 * 15 / 20 *  (25 + 4 - 1050) * 14 * (4.0 - 16 * 3 / 2 / 10.0 + 182 / 2.0 * 3 / 121.0) + 15 % 12 - 12 * 14 / 5.0+ 15 % 12 - 12 * 14 / 5.0 + 100 * 15 / 20 *  (25 + 4 - 1050) * 14 * (4.0 - 16 * 3 / 2 / 10.0 + 182 / 2.0 * 3 / 121.0) + 15 % 12 - 12 * 14 / 5.0+ 15 % 12 - 12 * 14 / 5.0 + 100 * 15 / 20 *  (25 + 4 - 1050) * 14 * (4.0 - 16 * 3 / 2 / 10.0 + 182 / 2.0 * 3 / 121.0) + 15 % 12 - 12 * 14 / 5.0+ 15 % 12 - 12 * 14 / 5.0 + 100 * 15 / 20 *  (25 + 4 - 1050) * 14 * (4.0 - 16 * 3 / 2 / 10.0 + 182 / 2.0 * 3 / 121.0) + 15 % 12 - 12 * 14 / 5.0+ 15 % 12 - 12 * 14 / 5.0 + 100 * 15 / 20 *  (25 + 4 - 1050) * 14 * (4.0 - 16 * 3 / 2 / 10.0 + 182 / 2.0 * 3 / 121.0) + 15 % 12 - 12 * 14 / 5.0+ 15 % 12 - 12 * 14 / 5.0 + 100 * 15 / 20 *  (25 + 4 - 1050) * 14 * (4.0 - 16 * 3 / 2 / 10.0 + 182 / 2.0 * 3 / 121.0) + 15 % 12 - 12 * 14 / 5.0+ 15 % 12 - 12 * 14 / 5.0 + 100 * 15 / 20 *  (25 + 4 - 1050) * 14 * (4.0 - 16 * 3 / 2 / 10.0 + 182 / 2.0 * 3 / 121.0) + 15 % 12 - 12 * 14 / 5.0+ 15 % 12 - 12 * 14 / 5.0 + 100 * 15 / 20 *  (25 + 4 - 1050) * 14 * (4.0 - 16 * 3 / 2 / 10.0 + 182 / 2.0 * 3 / 121.0) + 15 % 12 - 12 * 14 / 5.0+ 15 % 12 - 12 * 14 / 5.0 + 100 * 15 / 20 *  (25 + 4 - 1050) * 14 * (4.0 - 16 * 3 / 2 / 10.0 + 182 / 2.0 * 3 / 121.0) + 15 % 12 - 12 * 14 / 5.0+ 15 % 12 - 12 * 14 / 5.0 + 100 * 1 / 20 *  (25 + 4 - 1050) * 14 * (4.0 - 16 * 3 / 2 / 10.0 + 182 / 2.0 * 3 / 121.0) + 15 % 12 - 12 * 14 / 5.0+ 15 % 12 - 12 * 14 / 5.0 + 100 * 15 / 20 *  (25 + 4 - 1050) * 14 * (4.0 - 16 * 3 / 2 / 10.0 + 182 / 2.0 * 3 / 121.0) + 15 % 12 - 12 * 14 / 5.0+ 15 % 12 - 12 * 14 / 5.0 + 100 * 15 / 20 *  (25 + 4 - 1050) * 14 * (4.0 - 16 * 3 / 2 / 10.0 + 182 / 2.0 * 3 / 121.0) + 15 % 12 - 12 * 14 / 5.0+ 15 % 12 - 12 * 14 / 5.0 + 100 * 15 / 20 *  (25 + 4 - 1050) * 14 * (4.0 - 16 * 3 / 2 / 10.0 + 182 / 2.0 * 3 / 121.0) + 15 % 12 - 12 * 14 / 5.0+ 15 % 12 - 12 * 14 / 5.0 + 100 * 15 / 20 *  (25 + 4 - 1050) * 14 * (4.0 - 16 * 3 / 2 / 10.0 + 182 / 2.0 * 3 / 121.0) + 15 % 12 - 12 * 14 / 5.0+ 15 % 12 - 12 * 14 / 5.0 + 100 * 15 / 20 *  (25 + 4 - 1050) * 14 * (4.0 - 16 * 3 / 2 / 10.0 + 182 / 2.0 * 3 / 121.0) + 15 % 12 - 12 * 14 / 5.0+ 15 % 12 - 12 * 14 / 5.0 + 100 * 15 / 20 *  (25 + 4 - 1050) * 14 * (4.0 - 16 * 3 / 2 / 10.0 + 182 / 2.0 * 3 / 121.0) + 15 % 12 - 12 * 14 / 5.0+ 15 % 12 - 12 * 14 / 5.0 + 100 * 15 / 20 *  (25 + 4 - 1050) * 14 * (4.0 - 16 * 3 / 2 / 10.0 + 182 / 2.0 * 3 / 121.0) + 15 % 12 - 12 * 14 / 5.0+ 15 % 12 - 12 * 14 / 5.0 + 100 * 15 / 20 *  (25 + 4 - 1050) * 14 * (4.0 - 16 * 3 / 2 / 10.0 + 182 / 2.0 * 3 / 121.0) + 15 % 12 - 12 * 14 / 5.0+ 15 % 12 - 12 * 14 / 5.0 + 100 * 15 / 20 *  (25 + 4 - 1050) * 14 * (4.0 - 16 * 3 / 2 / 10.0 + 182 / 2.0 * 3 / 121.0) + 15 % 12 - 12 * 14 / 5.0+ 15 % 12 - 12 * 14 / 5.0 + 100 * 15 / 20 *  (25 + 4 - 1050) * 14 * (4.0 - 16 * 3 / 2 / 10.0 + 182 / 2.0 * 3 / 121.0) + 15 % 12 - 12 * 14 / 5.0+ 15 % 12 - 12 * 14 / 5.0 + 100 * 15 / 20 *  (25 + 4 - 1050) * 14 * (4.0 - 16 * 3 / 2 / 10.0 + 182 / 2.0 * 3 / 121.0) + 15 % 12 - 12 * 14 / 5.0+ 15 % 12 - 12 * 14 / 5.0 + 100 * 15 / 20 *  (25 + 4 - 1050) * 14 * (4.0 - 16 * 3 / 2 / 10.0 + 182 / 2.0 * 3 / 121.0) + 15 % 12 - 12 * 14 / 5.0+ 15 % 12 - 12 * 14 / 5.0 + 100 * 15 / 20 *  (25 + 4 - 1050) * 14 * (4.0 - 16 * 3 / 2 / 10.0 + 182 / 2.0 * 3 / 121.0) + 15 % 12 - 12 * 14 / 5.0+ 15 % 12 - 12 * 14 / 5.0 + 100 * 15 / 20 *  (25 + 4 - 1050) * 14 * (4.0 - 16 * 3 / 2 / 10.0 + 182 / 2.0 * 3 / 121.0) + 15 % 12 - 12 * 14 / 5.0+ 15 % 12 - 12 * 14 / 5.0 + 100 * 15 / 20 *  (25 + 4 - 1050) * 14 * (4.0 - 16 * 3 / 2 / 10.0 + 182 / 2.0 * 3 / 121.0) + 15 % 12 - 12 * 14 / 5.0+ 15 % 12 - 12 * 14 / 5.0 + 100 * 15 / 20 *  (25 + 4 - 1050) * 14 * (4.0 - 16 * 3 / 2 / 10.0 + 182 / 2.0 * 3 / 121.0) + 15 % 12 - 12 * 14 / 5.0+ 15 % 12 - 12 * 14 / 5.0 + 100 * 15 / 20 *  (25 + 4 - 1050) * 14 * (4.0 - 16 * 3 / 2 / 10.0 + 182 / 2.0 * 3 / 121.0) + 15 % 12 - 12 * 14 / 5.0+ 15 % 12 - 12 * 14 / 5.0 + 100 * 15 / 20 *  (25 + 4 - 1050) * 14 * (4.0 - 16 * 3 / 2 / 10.0 + 182 / 2.0 * 3 / 121.0) + 15 % 12 - 12 * 14 / 5.0+ 15 % 12 - 12 * 14 / 5.0 + 100 * 15 / 20 *  (25 + 4 - 1050) * 14 * (4.0 - 16 * 3 / 2 / 10.0 + 182 / 2.0 * 3 / 121.0) + 15 % 12 - 12 * 14 / 5.0+ 15 % 12 - 12 * 14 / 6.0");
//            result = io.github.wycst.wast.common.expression.Expression.eval(exprStr);

//            result = varExpr.evaluate(map);
//            result = e4.evaluate(map);
        }
        long l2 = System.currentTimeMillis();
        System.out.println(" use " + (l2 - l));
        System.out.println(" result " + result);
        System.out.println(100 * 1 / 20 *  (25 + 4 - 1050) * 14 * (4.0 - 16 * 3 / 2 / 10.0 + 182 / 2.0 * 3 / 121.0) + 15 % 12 - 12 * 14 / 5.0+ 15 % 12 - 12 * 14 / 5.0 + 100 * 15 / 20 *  (25 + 4 - 1050) * 14 * (4.0 - 16 * 3 / 2 / 10.0 + 182 / 2.0 * 3 / 121.0) + 15 % 12 - 12 * 14 / 5.0+ 15 % 12 - 12 * 14 / 5.0 + 100 * 15 / 20 *  (25 + 4 - 1050) * 14 * (4.0 - 16 * 3 / 2 / 10.0 + 182 / 2.0 * 3 / 121.0) + 15 % 12 - 12 * 14 / 5.0+ 15 % 12 - 12 * 14 / 5.0 + 100 * 15 / 20 *  (25 + 4 - 1050) * 14 * (4.0 - 16 * 3 / 2 / 10.0 + 182 / 2.0 * 3 / 121.0) + 15 % 12 - 12 * 14 / 5.0+ 15 % 12 - 12 * 14 / 5.0 + 100 * 15 / 20 *  (25 + 4 - 1050) * 14 * (4.0 - 16 * 3 / 2 / 10.0 + 182 / 2.0 * 3 / 121.0) + 15 % 12 - 12 * 14 / 5.0+ 15 % 12 - 12 * 14 / 5.0 + 100 * 15 / 20 *  (25 + 4 - 1050) * 14 * (4.0 - 16 * 3 / 2 / 10.0 + 182 / 2.0 * 3 / 121.0) + 15 % 12 - 12 * 14 / 5.0+ 15 % 12 - 12 * 14 / 5.0 + 100 * 15 / 20 *  (25 + 4 - 1050) * 14 * (4.0 - 16 * 3 / 2 / 10.0 + 182 / 2.0 * 3 / 121.0) + 15 % 12 - 12 * 14 / 5.0+ 15 % 12 - 12 * 14 / 5.0 + 100 * 15 / 20 *  (25 + 4 - 1050) * 14 * (4.0 - 16 * 3 / 2 / 10.0 + 182 / 2.0 * 3 / 121.0) + 15 % 12 - 12 * 14 / 5.0+ 15 % 12 - 12 * 14 / 5.0 + 100 * 15 / 20 *  (25 + 4 - 1050) * 14 * (4.0 - 16 * 3 / 2 / 10.0 + 182 / 2.0 * 3 / 121.0) + 15 % 12 - 12 * 14 / 5.0+ 15 % 12 - 12 * 14 / 5.0 + 100 * 15 / 20 *  (25 + 4 - 1050) * 14 * (4.0 - 16 * 3 / 2 / 10.0 + 182 / 2.0 * 3 / 121.0) + 15 % 12 - 12 * 14 / 5.0+ 15 % 12 - 12 * 14 / 5.0 + 100 * 15 / 20 *  (25 + 4 - 1050) * 14 * (4.0 - 16 * 3 / 2 / 10.0 + 182 / 2.0 * 3 / 121.0) + 15 % 12 - 12 * 14 / 5.0+ 15 % 12 - 12 * 14 / 5.0 + 100 * 15 / 20 *  (25 + 4 - 1050) * 14 * (4.0 - 16 * 3 / 2 / 10.0 + 182 / 2.0 * 3 / 121.0) + 15 % 12 - 12 * 14 / 5.0+ 15 % 12 - 12 * 14 / 5.0 + 100 * 15 / 20 *  (25 + 4 - 1050) * 14 * (4.0 - 16 * 3 / 2 / 10.0 + 182 / 2.0 * 3 / 121.0) + 15 % 12 - 12 * 14 / 5.0+ 15 % 12 - 12 * 14 / 5.0 + 100 * 15 / 20 *  (25 + 4 - 1050) * 14 * (4.0 - 16 * 3 / 2 / 10.0 + 182 / 2.0 * 3 / 121.0) + 15 % 12 - 12 * 14 / 5.0+ 15 % 12 - 12 * 14 / 5.0 + 100 * 15 / 20 *  (25 + 4 - 1050) * 14 * (4.0 - 16 * 3 / 2 / 10.0 + 182 / 2.0 * 3 / 121.0) + 15 % 12 - 12 * 14 / 5.0+ 15 % 12 - 12 * 14 / 5.0 + 100 * 15 / 20 *  (25 + 4 - 1050) * 14 * (4.0 - 16 * 3 / 2 / 10.0 + 182 / 2.0 * 3 / 121.0) + 15 % 12 - 12 * 14 / 5.0+ 15 % 12 - 12 * 14 / 5.0 + 100 * 15 / 20 *  (25 + 4 - 1050) * 14 * (4.0 - 16 * 3 / 2 / 10.0 + 182 / 2.0 * 3 / 121.0) + 15 % 12 - 12 * 14 / 5.0+ 15 % 12 - 12 * 14 / 5.0 + 100 * 15 / 20 *  (25 + 4 - 1050) * 14 * (4.0 - 16 * 3 / 2 / 10.0 + 182 / 2.0 * 3 / 121.0) + 15 % 12 - 12 * 14 / 5.0+ 15 % 12 - 12 * 14 / 5.0 + 100 * 15 / 20 *  (25 + 4 - 1050) * 14 * (4.0 - 16 * 3 / 2 / 10.0 + 182 / 2.0 * 3 / 121.0) + 15 % 12 - 12 * 14 / 5.0+ 15 % 12 - 12 * 14 / 5.0 + 100 * 15 / 20 *  (25 + 4 - 1050) * 14 * (4.0 - 16 * 3 / 2 / 10.0 + 182 / 2.0 * 3 / 121.0) + 15 % 12 - 12 * 14 / 5.0+ 15 % 12 - 12 * 14 / 5.0 + 100 * 1 / 20 *  (25 + 4 - 1050) * 14 * (4.0 - 16 * 3 / 2 / 10.0 + 182 / 2.0 * 3 / 121.0) + 15 % 12 - 12 * 14 / 5.0+ 15 % 12 - 12 * 14 / 5.0 + 100 * 15 / 20 *  (25 + 4 - 1050) * 14 * (4.0 - 16 * 3 / 2 / 10.0 + 182 / 2.0 * 3 / 121.0) + 15 % 12 - 12 * 14 / 5.0+ 15 % 12 - 12 * 14 / 5.0 + 100 * 15 / 20 *  (25 + 4 - 1050) * 14 * (4.0 - 16 * 3 / 2 / 10.0 + 182 / 2.0 * 3 / 121.0) + 15 % 12 - 12 * 14 / 5.0+ 15 % 12 - 12 * 14 / 5.0 + 100 * 15 / 20 *  (25 + 4 - 1050) * 14 * (4.0 - 16 * 3 / 2 / 10.0 + 182 / 2.0 * 3 / 121.0) + 15 % 12 - 12 * 14 / 5.0+ 15 % 12 - 12 * 14 / 5.0 + 100 * 15 / 20 *  (25 + 4 - 1050) * 14 * (4.0 - 16 * 3 / 2 / 10.0 + 182 / 2.0 * 3 / 121.0) + 15 % 12 - 12 * 14 / 5.0+ 15 % 12 - 12 * 14 / 5.0 + 100 * 15 / 20 *  (25 + 4 - 1050) * 14 * (4.0 - 16 * 3 / 2 / 10.0 + 182 / 2.0 * 3 / 121.0) + 15 % 12 - 12 * 14 / 5.0+ 15 % 12 - 12 * 14 / 5.0 + 100 * 15 / 20 *  (25 + 4 - 1050) * 14 * (4.0 - 16 * 3 / 2 / 10.0 + 182 / 2.0 * 3 / 121.0) + 15 % 12 - 12 * 14 / 5.0+ 15 % 12 - 12 * 14 / 5.0 + 100 * 15 / 20 *  (25 + 4 - 1050) * 14 * (4.0 - 16 * 3 / 2 / 10.0 + 182 / 2.0 * 3 / 121.0) + 15 % 12 - 12 * 14 / 5.0+ 15 % 12 - 12 * 14 / 5.0 + 100 * 15 / 20 *  (25 + 4 - 1050) * 14 * (4.0 - 16 * 3 / 2 / 10.0 + 182 / 2.0 * 3 / 121.0) + 15 % 12 - 12 * 14 / 5.0+ 15 % 12 - 12 * 14 / 5.0 + 100 * 15 / 20 *  (25 + 4 - 1050) * 14 * (4.0 - 16 * 3 / 2 / 10.0 + 182 / 2.0 * 3 / 121.0) + 15 % 12 - 12 * 14 / 5.0+ 15 % 12 - 12 * 14 / 5.0 + 100 * 15 / 20 *  (25 + 4 - 1050) * 14 * (4.0 - 16 * 3 / 2 / 10.0 + 182 / 2.0 * 3 / 121.0) + 15 % 12 - 12 * 14 / 5.0+ 15 % 12 - 12 * 14 / 5.0 + 100 * 15 / 20 *  (25 + 4 - 1050) * 14 * (4.0 - 16 * 3 / 2 / 10.0 + 182 / 2.0 * 3 / 121.0) + 15 % 12 - 12 * 14 / 5.0+ 15 % 12 - 12 * 14 / 5.0 + 100 * 15 / 20 *  (25 + 4 - 1050) * 14 * (4.0 - 16 * 3 / 2 / 10.0 + 182 / 2.0 * 3 / 121.0) + 15 % 12 - 12 * 14 / 5.0+ 15 % 12 - 12 * 14 / 5.0 + 100 * 15 / 20 *  (25 + 4 - 1050) * 14 * (4.0 - 16 * 3 / 2 / 10.0 + 182 / 2.0 * 3 / 121.0) + 15 % 12 - 12 * 14 / 5.0+ 15 % 12 - 12 * 14 / 5.0 + 100 * 15 / 20 *  (25 + 4 - 1050) * 14 * (4.0 - 16 * 3 / 2 / 10.0 + 182 / 2.0 * 3 / 121.0) + 15 % 12 - 12 * 14 / 5.0+ 15 % 12 - 12 * 14 / 5.0 + 100 * 15 / 20 *  (25 + 4 - 1050) * 14 * (4.0 - 16 * 3 / 2 / 10.0 + 182 / 2.0 * 3 / 121.0) + 15 % 12 - 12 * 14 / 5.0+ 15 % 12 - 12 * 14 / 5.0 + 100 * 15 / 20 *  (25 + 4 - 1050) * 14 * (4.0 - 16 * 3 / 2 / 10.0 + 182 / 2.0 * 3 / 121.0) + 15 % 12 - 12 * 14 / 5.0+ 15 % 12 - 12 * 14 / 5.0 + 100 * 15 / 20 *  (25 + 4 - 1050) * 14 * (4.0 - 16 * 3 / 2 / 10.0 + 182 / 2.0 * 3 / 121.0) + 15 % 12 - 12 * 14 / 5.0+ 15 % 12 - 12 * 14 / 5.0 + 100 * 15 / 20 *  (25 + 4 - 1050) * 14 * (4.0 - 16 * 3 / 2 / 10.0 + 182 / 2.0 * 3 / 121.0) + 15 % 12 - 12 * 14 / 5.0+ 15 % 12 - 12 * 14 / 5.0 + 100 * 15 / 20 *  (25 + 4 - 1050) * 14 * (4.0 - 16 * 3 / 2 / 10.0 + 182 / 2.0 * 3 / 121.0) + 15 % 12 - 12 * 14 / 5.0+ 15 % 12 - 12 * 14 / 5.0 + 100 * 1 / 20 *  (25 + 4 - 1050) * 14 * (4.0 - 16 * 3 / 2 / 10.0 + 182 / 2.0 * 3 / 121.0) + 15 % 12 - 12 * 14 / 5.0+ 15 % 12 - 12 * 14 / 5.0 + 100 * 15 / 20 *  (25 + 4 - 1050) * 14 * (4.0 - 16 * 3 / 2 / 10.0 + 182 / 2.0 * 3 / 121.0) + 15 % 12 - 12 * 14 / 5.0+ 15 % 12 - 12 * 14 / 5.0 + 100 * 15 / 20 *  (25 + 4 - 1050) * 14 * (4.0 - 16 * 3 / 2 / 10.0 + 182 / 2.0 * 3 / 121.0) + 15 % 12 - 12 * 14 / 5.0+ 15 % 12 - 12 * 14 / 5.0 + 100 * 15 / 20 *  (25 + 4 - 1050) * 14 * (4.0 - 16 * 3 / 2 / 10.0 + 182 / 2.0 * 3 / 121.0) + 15 % 12 - 12 * 14 / 5.0+ 15 % 12 - 12 * 14 / 5.0 + 100 * 15 / 20 *  (25 + 4 - 1050) * 14 * (4.0 - 16 * 3 / 2 / 10.0 + 182 / 2.0 * 3 / 121.0) + 15 % 12 - 12 * 14 / 5.0+ 15 % 12 - 12 * 14 / 5.0 + 100 * 15 / 20 *  (25 + 4 - 1050) * 14 * (4.0 - 16 * 3 / 2 / 10.0 + 182 / 2.0 * 3 / 121.0) + 15 % 12 - 12 * 14 / 5.0+ 15 % 12 - 12 * 14 / 5.0 + 100 * 15 / 20 *  (25 + 4 - 1050) * 14 * (4.0 - 16 * 3 / 2 / 10.0 + 182 / 2.0 * 3 / 121.0) + 15 % 12 - 12 * 14 / 5.0+ 15 % 12 - 12 * 14 / 5.0 + 100 * 15 / 20 *  (25 + 4 - 1050) * 14 * (4.0 - 16 * 3 / 2 / 10.0 + 182 / 2.0 * 3 / 121.0) + 15 % 12 - 12 * 14 / 5.0+ 15 % 12 - 12 * 14 / 5.0 + 100 * 15 / 20 *  (25 + 4 - 1050) * 14 * (4.0 - 16 * 3 / 2 / 10.0 + 182 / 2.0 * 3 / 121.0) + 15 % 12 - 12 * 14 / 5.0+ 15 % 12 - 12 * 14 / 5.0 + 100 * 15 / 20 *  (25 + 4 - 1050) * 14 * (4.0 - 16 * 3 / 2 / 10.0 + 182 / 2.0 * 3 / 121.0) + 15 % 12 - 12 * 14 / 5.0+ 15 % 12 - 12 * 14 / 5.0 + 100 * 15 / 20 *  (25 + 4 - 1050) * 14 * (4.0 - 16 * 3 / 2 / 10.0 + 182 / 2.0 * 3 / 121.0) + 15 % 12 - 12 * 14 / 5.0+ 15 % 12 - 12 * 14 / 5.0 + 100 * 15 / 20 *  (25 + 4 - 1050) * 14 * (4.0 - 16 * 3 / 2 / 10.0 + 182 / 2.0 * 3 / 121.0) + 15 % 12 - 12 * 14 / 5.0+ 15 % 12 - 12 * 14 / 5.0 + 100 * 15 / 20 *  (25 + 4 - 1050) * 14 * (4.0 - 16 * 3 / 2 / 10.0 + 182 / 2.0 * 3 / 121.0) + 15 % 12 - 12 * 14 / 5.0+ 15 % 12 - 12 * 14 / 5.0 + 100 * 15 / 20 *  (25 + 4 - 1050) * 14 * (4.0 - 16 * 3 / 2 / 10.0 + 182 / 2.0 * 3 / 121.0) + 15 % 12 - 12 * 14 / 5.0+ 15 % 12 - 12 * 14 / 5.0 + 100 * 15 / 20 *  (25 + 4 - 1050) * 14 * (4.0 - 16 * 3 / 2 / 10.0 + 182 / 2.0 * 3 / 121.0) + 15 % 12 - 12 * 14 / 5.0+ 15 % 12 - 12 * 14 / 5.0 + 100 * 15 / 20 *  (25 + 4 - 1050) * 14 * (4.0 - 16 * 3 / 2 / 10.0 + 182 / 2.0 * 3 / 121.0) + 15 % 12 - 12 * 14 / 5.0+ 15 % 12 - 12 * 14 / 5.0 + 100 * 15 / 20 *  (25 + 4 - 1050) * 14 * (4.0 - 16 * 3 / 2 / 10.0 + 182 / 2.0 * 3 / 121.0) + 15 % 12 - 12 * 14 / 5.0+ 15 % 12 - 12 * 14 / 5.0 + 100 * 15 / 20 *  (25 + 4 - 1050) * 14 * (4.0 - 16 * 3 / 2 / 10.0 + 182 / 2.0 * 3 / 121.0) + 15 % 12 - 12 * 14 / 5.0+ 15 % 12 - 12 * 14 / 5.0 + 100 * 15 / 20 *  (25 + 4 - 1050) * 14 * (4.0 - 16 * 3 / 2 / 10.0 + 182 / 2.0 * 3 / 121.0) + 15 % 12 - 12 * 14 / 5.0+ 15 % 12 - 12 * 14 / 5.0 + 100 * 15 / 20 *  (25 + 4 - 1050) * 14 * (4.0 - 16 * 3 / 2 / 10.0 + 182 / 2.0 * 3 / 121.0) + 15 % 12 - 12 * 14 / 5.0+ 15 % 12 - 12 * 14 / 5.0 + 100 * 1 / 20 *  (25 + 4 - 1050) * 14 * (4.0 - 16 * 3 / 2 / 10.0 + 182 / 2.0 * 3 / 121.0) + 15 % 12 - 12 * 14 / 5.0+ 15 % 12 - 12 * 14 / 5.0 + 100 * 15 / 20 *  (25 + 4 - 1050) * 14 * (4.0 - 16 * 3 / 2 / 10.0 + 182 / 2.0 * 3 / 121.0) + 15 % 12 - 12 * 14 / 5.0+ 15 % 12 - 12 * 14 / 5.0 + 100 * 15 / 20 *  (25 + 4 - 1050) * 14 * (4.0 - 16 * 3 / 2 / 10.0 + 182 / 2.0 * 3 / 121.0) + 15 % 12 - 12 * 14 / 5.0+ 15 % 12 - 12 * 14 / 5.0 + 100 * 15 / 20 *  (25 + 4 - 1050) * 14 * (4.0 - 16 * 3 / 2 / 10.0 + 182 / 2.0 * 3 / 121.0) + 15 % 12 - 12 * 14 / 5.0+ 15 % 12 - 12 * 14 / 5.0 + 100 * 15 / 20 *  (25 + 4 - 1050) * 14 * (4.0 - 16 * 3 / 2 / 10.0 + 182 / 2.0 * 3 / 121.0) + 15 % 12 - 12 * 14 / 5.0+ 15 % 12 - 12 * 14 / 5.0 + 100 * 15 / 20 *  (25 + 4 - 1050) * 14 * (4.0 - 16 * 3 / 2 / 10.0 + 182 / 2.0 * 3 / 121.0) + 15 % 12 - 12 * 14 / 5.0+ 15 % 12 - 12 * 14 / 5.0 + 100 * 15 / 20 *  (25 + 4 - 1050) * 14 * (4.0 - 16 * 3 / 2 / 10.0 + 182 / 2.0 * 3 / 121.0) + 15 % 12 - 12 * 14 / 5.0+ 15 % 12 - 12 * 14 / 5.0 + 100 * 15 / 20 *  (25 + 4 - 1050) * 14 * (4.0 - 16 * 3 / 2 / 10.0 + 182 / 2.0 * 3 / 121.0) + 15 % 12 - 12 * 14 / 5.0+ 15 % 12 - 12 * 14 / 5.0 + 100 * 15 / 20 *  (25 + 4 - 1050) * 14 * (4.0 - 16 * 3 / 2 / 10.0 + 182 / 2.0 * 3 / 121.0) + 15 % 12 - 12 * 14 / 5.0+ 15 % 12 - 12 * 14 / 5.0 + 100 * 15 / 20 *  (25 + 4 - 1050) * 14 * (4.0 - 16 * 3 / 2 / 10.0 + 182 / 2.0 * 3 / 121.0) + 15 % 12 - 12 * 14 / 5.0+ 15 % 12 - 12 * 14 / 5.0 + 100 * 15 / 20 *  (25 + 4 - 1050) * 14 * (4.0 - 16 * 3 / 2 / 10.0 + 182 / 2.0 * 3 / 121.0) + 15 % 12 - 12 * 14 / 5.0+ 15 % 12 - 12 * 14 / 5.0 + 100 * 15 / 20 *  (25 + 4 - 1050) * 14 * (4.0 - 16 * 3 / 2 / 10.0 + 182 / 2.0 * 3 / 121.0) + 15 % 12 - 12 * 14 / 5.0+ 15 % 12 - 12 * 14 / 5.0 + 100 * 15 / 20 *  (25 + 4 - 1050) * 14 * (4.0 - 16 * 3 / 2 / 10.0 + 182 / 2.0 * 3 / 121.0) + 15 % 12 - 12 * 14 / 5.0+ 15 % 12 - 12 * 14 / 5.0 + 100 * 15 / 20 *  (25 + 4 - 1050) * 14 * (4.0 - 16 * 3 / 2 / 10.0 + 182 / 2.0 * 3 / 121.0) + 15 % 12 - 12 * 14 / 5.0+ 15 % 12 - 12 * 14 / 5.0 + 100 * 15 / 20 *  (25 + 4 - 1050) * 14 * (4.0 - 16 * 3 / 2 / 10.0 + 182 / 2.0 * 3 / 121.0) + 15 % 12 - 12 * 14 / 5.0+ 15 % 12 - 12 * 14 / 5.0 + 100 * 15 / 20 *  (25 + 4 - 1050) * 14 * (4.0 - 16 * 3 / 2 / 10.0 + 182 / 2.0 * 3 / 121.0) + 15 % 12 - 12 * 14 / 5.0+ 15 % 12 - 12 * 14 / 5.0 + 100 * 15 / 20 *  (25 + 4 - 1050) * 14 * (4.0 - 16 * 3 / 2 / 10.0 + 182 / 2.0 * 3 / 121.0) + 15 % 12 - 12 * 14 / 5.0+ 15 % 12 - 12 * 14 / 5.0 + 100 * 15 / 20 *  (25 + 4 - 1050) * 14 * (4.0 - 16 * 3 / 2 / 10.0 + 182 / 2.0 * 3 / 121.0) + 15 % 12 - 12 * 14 / 5.0+ 15 % 12 - 12 * 14 / 5.0 + 100 * 15 / 20 *  (25 + 4 - 1050) * 14 * (4.0 - 16 * 3 / 2 / 10.0 + 182 / 2.0 * 3 / 121.0) + 15 % 12 - 12 * 14 / 5.0+ 15 % 12 - 12 * 14 / 5.0 + 100 * 15 / 20 *  (25 + 4 - 1050) * 14 * (4.0 - 16 * 3 / 2 / 10.0 + 182 / 2.0 * 3 / 121.0) + 15 % 12 - 12 * 14 / 5.0+ 15 % 12 - 12 * 14 / 6.0);


//        System.out.println(Expression.eval("-2 * (-3 - 2 * -5) ** -1"));
//        System.out.println(100 * 15 / 20 - (25 + 4 - 1050) * 14 * (4.0 - 16 * 3 / 2 / 10.0 + 182 / 2.0 * 3 / 121.0) + 15 % 12 - 12 * 14 / 5.0);
//        System.out.println(Expression.eval("100 * 15 / 20 - (((25 + 4 - 1050))) * 14 * (4.0 - 16 * 3 / 2 / 10.0 + 182 / 2.0 * 3 / 121.0) + 15 % 12 - 12 * 14 / 5.0"));
//        System.out.println("2 - 3 - 4 * 5 - 6 - 7");
//        System.out.println(2 - 3 - 4 * 5 - 6 - 7);
//        System.out.println(Expression.eval("2 - 3 - 4 * 5 - 6 - 7"));
//
//        System.out.println("2 - (3 - 4 * (5 - 6)) - 7");
//        System.out.println(2 - (3 - 4 * (5 - 6)) - 7);
//        System.out.println(2 - 3);
//        System.out.println(Expression.eval("2 - (3 - (((((4 * 5 - 6))))) - 7)"));
//        System.out.println(Expression.eval("2 - 12 % 5 * (2 * 3 - 5 - 6) - 7"));
//        System.out.println(Expression.eval("aa * bb"));


    }

}
