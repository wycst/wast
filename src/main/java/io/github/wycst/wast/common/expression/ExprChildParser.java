package io.github.wycst.wast.common.expression;

import io.github.wycst.wast.common.expression.invoker.VariableInvoker;

import java.util.Map;

/**
 * 子表达式（方法参数表达式和函数参数表达式，非括号）
 * 变量解析注册到全局解析器（global）中
 * 子表达式执行时入口使用doEvaluate跳过参数初始化；
 * 编译模式下解决预处理变量名称错误问题；
 *
 * @Author wangyunchao
 * @Date 2022/11/13 16:03
 */
class ExprChildParser extends ExprParser {

    private final ExprParser global;

    public ExprChildParser(String expr, ExprParser global) {
        this.init(expr);
        this.global = global;
        this.parse();
    }

    @Override
    protected ExprParser global() {
        return global;
    }

    // note: Do not use super, not parent-child relationship
    @Override
    protected Map<String, VariableInvoker> getInvokes() {
        return global.getInvokes();
    }

    // note: Do not use super, not parent-child relationship
    @Override
    protected Map<String, VariableInvoker> getTailInvokes() {
        return global.getTailInvokes();
    }

    // note: Do not use super, not parent-child relationship
    @Override
    void checkInitializedInvokes() {
        global.checkInitializedInvokes();
    }

    // Override the default behavior and do nothing
    @Override
    protected void compressVariables() {
    }

    // Override the default behavior and do nothing
    @Override
    void clearContextVariables(EvaluatorContext evaluatorContext) {
    }
}