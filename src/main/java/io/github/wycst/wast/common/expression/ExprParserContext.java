package io.github.wycst.wast.common.expression;

/**
 * @Author wangyunchao
 * @Date 2022/10/15 14:44
 */
class ExprParserContext {

    // 当前执行器
    ExprEvaluator exprEvaluator;
    // 负
    boolean negate;
    // 非
    boolean logicalNot;

    // 结束标志
    boolean endFlag;

    ExprParserContext() {
    }

    ExprParserContext(ExprEvaluator evaluator, boolean negate, boolean logicalNot) {
        setContext(evaluator, negate, logicalNot);
    }

    void setContext(ExprEvaluator evaluator, boolean negate, boolean logicalNot) {
        this.exprEvaluator = evaluator;
        this.negate = negate;
        this.logicalNot = logicalNot;
    }
}
