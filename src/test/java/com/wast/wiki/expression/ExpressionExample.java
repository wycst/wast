package com.wast.wiki.expression;

import com.wast.wiki.beans.ElFact;
import io.github.wycst.wast.common.expression.CacheableExpression;
import io.github.wycst.wast.common.expression.Expression;
import io.github.wycst.wast.common.expression.compile.CompilerEnvironment;
import io.github.wycst.wast.common.expression.compile.CompilerExpression;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

/**
 * @Date 2024/11/16 11:00
 * @Created by wangyc
 */
public class ExpressionExample {

    static Map map = new HashMap();
    static ElFact fact = new ElFact();

    static {
        map.put("a", 100);
        map.put("b", 200);
        map.put("c", 300);
        map.put("string", "hello");
        map.put("bd", new BigDecimal("123.456789"));
        map.put("self", map);
        map.put("fact", fact);
    }

    public static void main(String[] args) {
        parseMode();
        compileMode();
    }

    // 解析模式
    private static void parseMode() {
        // 1.静态数学表达式计算
        System.out.println(Expression.eval("1+1"));  // 2

        // 2.变量表达式计算
        System.out.println(Expression.eval("a+b+c", map));  // 使用map作为上下文 600
        System.out.println(Expression.eval("a+b+c", fact));  // 使用pojo作为上下文 6
        System.out.println(Expression.evalParameters("a+b+b+c", 1, 2, 3));  // 输出8(a=1, b = 2, c = 3) 使用可变参数作为上下文按参数解析顺序获取

        // 3.将计算的结果类型转为目标类型
        int result = Expression.evalResult("1+1", int.class);
        System.out.println("result: " + result);

        // 4.字符串(支持函数)
        System.out.println(Expression.eval("1+string", map));  // 1hello
        System.out.println(Expression.evalParameters("str.length() + 6", "abcdefgh"));  // 14
        System.out.println(Expression.evalParameters("str.length() + 6 + str", "abcdefgh"));  // 14abcdefgh
        System.out.println(Expression.evalParameters("str.contains('cde') ? 1 : 2", "abcdefgh"));  // 1
        System.out.println(Expression.evalParameters("str.indexOf('cde')", "abcdefgh"));  // 2

        // 5.指数运算(使用**)
        System.out.println(Expression.eval("6**2"));  // 36

        // 6.常规四则运算
        System.out.println(Expression.evalResult("1+2.5-3.777 * 45.6 / (31.0 / 6 + (5 >> 2))", double.class) == (1 + 2.5 - 3.777 * 45.6 / (31.0 / 6 + (5 >> 2))));

        // 7.内置函数(max/min/sum/avg/abs/sqrt/lower/upper/size/ifNull/toArray)
        System.out.println(Expression.eval("sum(1+max(5,7,9,11,13))")); // 14
        System.out.println(Expression.eval("sum(1+min(5,7))")); // 6

        // 8.精确计算（使用$作为浮点数的后缀会解析为BigDecimal），只要存在BigDecimal的运算都会转为BigDecimal
        System.out.println(Expression.eval("1.2345678123232323232323$+7.45678$"));
        System.out.println(Expression.eval("bd + 7", map).getClass()); //

        // 9.变量支持链式访问（map和pojo兼容）
        System.out.println(Expression.eval("self.self.self.self.a + self.fact.a", map)); // 100 + 1

        // 10.判断是否属于/不属于(使用数学符号∈和∉)
        System.out.println(Expression.eval("1 ∈ {1,2,3}")); // true
        System.out.println(Expression.eval("1 ∉ {1,2,3}")); // false
        // {}只支持静态数组，动态数组可以使用toArray函数支持
        System.out.println(Expression.eval("100 ∈ toArray(a, b, c)", map)); // true
        System.out.println(Expression.eval("100 ∉ toArray(a, b, c)", map)); // false

        // 11.支持先解析生成表达式模型，然后运算
        Expression expr = Expression.parse("a+b+c");
        System.out.println(expr.evaluateParameters(1, 3, 5)); // a = 1 b = 3 c = 5 -> 9
        System.out.println(expr.evaluate(map)); // 600
        System.out.println(expr.evaluate(fact)); // 6
    }

    /**
     * 支持将表达式编译为class运行，性能基本可以逼近java直接调用
     */
    private static void compileMode() {
        String source = "a+b+c";
        // 1.直接编译
        Expression compilerExpression = CompilerExpression.compile(source);
        // 直接编译可变参数只支持double
        System.out.println(compilerExpression.evaluateParameters(11d, 22d, 33d));

        // 2.指定变量类型
        compilerExpression = CompilerExpression.createEnvironment().variableTypes(int.class, "a", "b", "c").compile(source);
        // 设置类型后可以按类型传入
        System.out.println(compilerExpression.evaluateParameters(11, 22, 33));

        // 3.多级变量
        source = "fact.a+fact.b";
        CompilerEnvironment environment = CompilerExpression.createEnvironment();
        environment.setVariableType(ElFact.class, "fact");
        environment.setVariableType(int.class, "fact.a", "fact.b");  // 注只有evaluateParameters需要设置
        compilerExpression = Expression.compile(source, environment);
        System.out.println(compilerExpression.evaluate(map));

        // 如果只有一个根的场景如上（最佳实践）
        environment.setSupportedContextRoot(true);
        compilerExpression = Expression.compile(source, environment);
        System.out.println(compilerExpression.evaluate(fact));  // 这里直接传入fact对象,性能最高
        System.out.println(compilerExpression.evaluateParameters(4, 6));  // 这里fact.a = 4, fact.b = 6 性能次之

        // 4.脚本模式支持（多行代码）
        // 目前解析基于单行表达式语法，不支持多行，脚本模式需要跳过解析，语法完全参考java语法，只做了部分安全校验比如System和Runtime等关键字不允许出现在非字符串内容中
        // 跳过解析
        environment.setSkipParse(true);
        environment.setSupportedContextRoot(true);
        // 脚本模式需要指定每个根变量的类型即可
        environment.setVariableType(ElFact.class, "fact");
        String script =
                        " fact.a = 131;\n" +
                        " fact.b = 541;\n" +
                        " return fact.a + fact.b;";
        compilerExpression = Expression.compile(script, environment);
        System.out.println(compilerExpression.evaluate(fact));

        // 5.import（脚本模式可以根据需要引入）
        environment.importSet(ElFact.class);
    }

}
