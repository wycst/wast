package io.github.wycst.wast.common.expression.compile;

import io.github.wycst.wast.common.expression.ExprEvaluator;
import io.github.wycst.wast.common.expression.ExpressionException;

/**
 * @Author: wangy
 * @Date: 2022/11/6 12:42
 * @Description:
 */
public final class CompilerExprEvaluator extends ExprEvaluator {

    /**
     * 生成表达式代码
     *
     * @return
     */
    public String code() {
        StringBuilder builder = new StringBuilder();
        // 如果当前为静态执行器进行解析压缩
        if (isConstantExpr()) {
            Object result = evaluate();
            if (result instanceof String) {
                String strValue = (String) result;
                if (strValue.indexOf('"') > -1) {
                    // Simple replacement
                    strValue = strValue.replace("\"", "\\\"");
                }
                return builder.append("\"").append(strValue).append("\"").toString();
            }
            return String.valueOf(result);
        }

        int evalType = this.evalType; // getEvalType();
        boolean negate = this.negate; // isNegate();
        boolean logicalNot = this.logicalNot; //isLogicalNot();

        ExprEvaluator left = this.left; // getLeft();
        ExprEvaluator right = this.right; // getRight();

        if (evalType == EVAL_TYPE_OPERATOR) {
            String leftGenerateCode = left.code();
            if (right == null) {
                return leftGenerateCode;
            }
            String rightGenerateCode = right.code();
            switch (operator) {
                case MULTI:
                    // *乘法
                    return builder.append(leftGenerateCode).append(" * ").append(rightGenerateCode).toString();
                case DIVISION:
                    // /除法
                    return builder.append(leftGenerateCode).append(" / ").append(rightGenerateCode).toString();
                case MOD:
                    // %取余数
                    return builder.append(leftGenerateCode).append(" % ").append(rightGenerateCode).toString();
                case EXP:
                    // **指数
                    return builder.append("Math.pow(").append(leftGenerateCode).append(", ").append(rightGenerateCode).append(")").toString();
                case PLUS:
                    // 加法
                    if (right.isNegate()) {
                        try {
                            return builder.append(leftGenerateCode).append(" - ").append(right.negate(false).code()).toString();
                        } finally {
                            // reduction
                            right.negate(true);
                        }
                    } else if (right.isConstantExpr() && rightGenerateCode.startsWith("-")) {
                        return builder.append(leftGenerateCode).append(" - ").append(rightGenerateCode.substring(1)).toString();
                    }
                    return builder.append(leftGenerateCode).append(" + ").append(rightGenerateCode).toString();
                case MINUS:
                    // -减法（理论上代码不可达） 因为'-'统一转化为了'+'(负)
                    return builder.append(leftGenerateCode).append(" - ").append(rightGenerateCode).toString();
                case BIT_RIGHT:
                    // >> 位运算右移 long
                    return builder.append(leftGenerateCode).append(" >> ").append(rightGenerateCode).toString();
                case BIT_LEFT:
                    // << 位运算右移 long
                    return builder.append(leftGenerateCode).append(" << ").append(rightGenerateCode).toString();
                case AND:
                    // & long
                    return builder.append(leftGenerateCode).append(" & ").append(rightGenerateCode).toString();
                case XOR:
                    // ^ long
                    return builder.append(leftGenerateCode).append(" ^ ").append(rightGenerateCode).toString();
                case OR:
                    // | long
                    return builder.append(leftGenerateCode).append(" | ").append(rightGenerateCode).toString();
                case GT:
                    // >
                    return builder.append(leftGenerateCode).append(" > ").append(rightGenerateCode).toString();
                case LT:
                    // <
                    return builder.append(leftGenerateCode).append(" < ").append(rightGenerateCode).toString();
                case EQ:
                    // == Object
                    return builder.append(leftGenerateCode).append(" == ").append(rightGenerateCode).toString();
                case GE:
                    // >=
                    return builder.append(leftGenerateCode).append(" >= ").append(rightGenerateCode).toString();
                case LE:
                    // <=
                    return builder.append(leftGenerateCode).append(" <= ").append(rightGenerateCode).toString();
                case NE:
                    // != Object
                    return builder.append(leftGenerateCode).append(" != ").append(rightGenerateCode).toString();
                case LOGICAL_AND:
                    // && Boolean
                    return builder.append(leftGenerateCode).append(" && ").append(rightGenerateCode).toString();
                case LOGICAL_OR:
                    // || Boolean
                    return builder.append(leftGenerateCode).append(" || ").append(rightGenerateCode).toString();
                case IN:
                    // in 暂时不支持
                    throw new ExpressionException("暂时不支持'in(∈)'符号编译");
                case OUT:
                    // out 暂时不支持out
                    throw new ExpressionException("暂时不支持'out(∉)'符号编译");
//                case COLON:
//                    // : 三目运算条件
//                    return builder.append(leftGenerateCode).append(" : ").append(rightGenerateCode).toString();
//                case QUESTION:
//                    // ? 三目运算结果
//                    return builder.append(leftGenerateCode).append(" ? ").append(rightGenerateCode).toString();
            }

        } else if (evalType == EVAL_TYPE_BRACKET) {
            if (negate) {
                return builder.append("-(").append(right.code()).append(")").toString();
            }
            if (logicalNot) {
                return builder.append("!(").append(right.code()).append(")").toString();
            }
            return builder.append("(").append(right.code()).append(")").toString();
        } else if (evalType == EVAL_TYPE_QUESTION) {
            // 三目运算
            ExprEvaluator rLeft = right.getLeft();
            ExprEvaluator rRight = right.getRight();
            return builder.append(left.code()).append(" ? ").append(rLeft.code()).append(" : ").append(rRight.code()).toString();
        } else {
            // 其他统一返回left
            return left.code();
        }
        return null;
    }

}
