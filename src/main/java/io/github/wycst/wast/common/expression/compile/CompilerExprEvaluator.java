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
        int opsType = this.opsType;
        // 如果当前为静态执行器进行解析压缩
        if (isStaticExpr()) {
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

        int evalType = getEvalType();
        boolean negate = isNegate();
        boolean logicalNot = isLogicalNot();

        ExprEvaluator left = getLeft();
        ExprEvaluator right = getRight();

        if (evalType == 1) {
            String leftGenerateCode = left.code();
            if (right == null) {
                return leftGenerateCode;
            }
            String rightGenerateCode = right.code();
            switch (opsType) {
                case 1:
                    // *乘法 double
                    return builder.append(leftGenerateCode).append(" * ").append(rightGenerateCode).toString();
                case 2:
                    // /除法 double
                    return builder.append(leftGenerateCode).append(" / ").append(rightGenerateCode).toString();
                case 3:
                    // %取余数 double
                    return builder.append(leftGenerateCode).append(" % ").append(rightGenerateCode).toString();
                case 4:
                    // **指数 double
                    return builder.append("Math.pow(").append(leftGenerateCode).append(", ").append(rightGenerateCode).append(")").toString();
                case 11:
                    // 加法 double
                    return builder.append(leftGenerateCode).append(" + ").append(rightGenerateCode).toString();
                case 12:
                    // -减法（理论上代码不可达） 因为'-'统一转化为了'+'(负) double
                    return builder.append(leftGenerateCode).append(" - ").append(rightGenerateCode).toString();
                case 21:
                    // >> 位运算右移 long
                    return builder.append(leftGenerateCode).append(" >> ").append(rightGenerateCode).toString();
                case 22:
                    // << 位运算右移 long
                    return builder.append(leftGenerateCode).append(" << ").append(rightGenerateCode).toString();
                case 31:
                    // & long
                    return builder.append(leftGenerateCode).append(" & ").append(rightGenerateCode).toString();
                case 32:
                    // ^ long
                    return builder.append(leftGenerateCode).append(" ^ ").append(rightGenerateCode).toString();
                case 33:
                    // | long
                    return builder.append(leftGenerateCode).append(" | ").append(rightGenerateCode).toString();
                case 51:
                    // > double
                    return builder.append(leftGenerateCode).append(" > ").append(rightGenerateCode).toString();
                case 52:
                    // < double
                    return builder.append(leftGenerateCode).append(" < ").append(rightGenerateCode).toString();
                case 53:
                    // == Object
                    return builder.append(leftGenerateCode).append(" == ").append(rightGenerateCode).toString();
                case 54:
                    // >= double
                    return builder.append(leftGenerateCode).append(" >= ").append(rightGenerateCode).toString();
                case 55:
                    // <= double
                    return builder.append(leftGenerateCode).append(" <= ").append(rightGenerateCode).toString();
                case 56:
                    // != Object
                    return builder.append(leftGenerateCode).append(" != ").append(rightGenerateCode).toString();
                case 61:
                    // && Boolean
                    return builder.append(leftGenerateCode).append(" && ").append(rightGenerateCode).toString();
                case 62:
                    // || Boolean
                    return builder.append(leftGenerateCode).append(" || ").append(rightGenerateCode).toString();
                case 63:
                    // in 暂时不支持
                    throw new ExpressionException("暂时不支持'in'符号编译");
                case 64:
                    // out 暂时不支持out
                    throw new ExpressionException("暂时不支持'out'符号编译");
                case 70:
                    // : 三目运算条件
                    return builder.append(leftGenerateCode).append(" : ").append(rightGenerateCode).toString();
                case 71:
                    // ? 三目运算结果
                    return builder.append(leftGenerateCode).append(" ? ").append(rightGenerateCode).toString();
            }

        } else if (evalType == 5) {
            // 括号运算
            if (negate) {
                return builder.append("-(").append(right.code()).append(")").toString();
            }
            if (logicalNot) {
                return builder.append("!(").append(right.code()).append(")").toString();
            }
            return builder.append("(").append(right.code()).append(")").toString();
        } else if (evalType == 6) {
            // 不可达
            throw new UnsupportedOperationException();
        } else {
            // 其他统一返回left
            return left.code();
        }
        return null;
    }

}
