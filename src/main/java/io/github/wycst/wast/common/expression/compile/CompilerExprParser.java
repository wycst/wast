package io.github.wycst.wast.common.expression.compile;

import io.github.wycst.wast.common.expression.ExprEvaluator;
import io.github.wycst.wast.common.expression.ExprParser;

/**
 * @Author: wangy
 * @Date: 2021/11/20 12:30
 * @Description:
 */
public final class CompilerExprParser extends ExprParser {

    CompilerExprParser(String exprSource) {
        super(exprSource);
    }

    @Override
    protected ExprEvaluator createExprEvaluator() {
        return new CompilerExprEvaluator();
    }

    /**
     * 生成表达式代码
     *
     * @return
     */
    String code() {
        return getEvaluator().code();
    }

    int getVariableCount() {
        return variableSize;
    }
}
