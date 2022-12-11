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
public class ExprCompilerNativeTest {

    public static void main(String[] args) {

        String el = "arg.a+arg.b+b+c";
        CompilerEnvironment compileEnvironment = new CompilerEnvironment();

        // 如果设置false会将表达式进行先解析再编译;
        // 如果设置为true将跳过解析在代码中直接return，此时最好使用setVariableType来声明变量类型
        // 不伦是否设置skipParse，使用setVariableType来声明变量类型都是不错的选择，能大大提高效率
        compileEnvironment.setSkipParse(true);
        compileEnvironment.setVariableType(int.class, "arg.a", "arg.b", "b", "c");

        // 输出编译的源代码
        System.out.println(CompilerExpression.generateJavaCode(el, compileEnvironment));
        CompilerExpression compiler = CompilerExpression.compile(el, compileEnvironment, CompilerExpression.Coder.Native);


        Map aa = new HashMap();
        aa.put("a", 120);
        aa.put("b", 150);

        Map var = new HashMap();
        var.put("arg", aa);
        var.put("b", 8);
        var.put("c", 1);

        // 执行
        System.out.println("==== eval result " + compiler.evaluate(var));
    }


}
