package org.framework.light.common.expression.compile;

import org.framework.light.common.expression.ExprParser;
import org.framework.light.common.expression.ExpressionException;

/**
 * @Author: wangy
 * @Date: 2021/11/20 12:30
 * @Description:
 */
public final class ExprParserCompiler extends ExprParser {

    protected ExprParserCompiler(String exprSource) {
        super(exprSource);
    }

    protected ExprParserCompiler() {
    }

    @Override
    protected ExprEvaluator createExprEvaluator() {
        return new ExprEvaluatorCompiler();
    }

    /**
     * 生成表达式代码
     *
     * @return
     */
    String generateCode(CompileEnvironment environment) {
        ExprEvaluatorCompiler exprEvaluatorCompiler = (ExprEvaluatorCompiler) getEvaluator();
        return exprEvaluatorCompiler.generateCode(environment);
    }

    public final class ExprEvaluatorCompiler extends ExprEvaluator {

        /**
         * 生成表达式代码
         *
         * @return
         */
        public String generateCode(CompileEnvironment environment) {
            int evalType = getEvalType();
            int opsType = getOpsType();
            boolean negate = isNegate();
            String value = getValue();

            ExprEvaluatorCompiler left = (ExprEvaluatorCompiler) getLeft();
            ExprEvaluatorCompiler right = (ExprEvaluatorCompiler) getRight();

            if (isStaticExpr()) {
                Object result = evaluate();
                if (result instanceof String) {
                    return '"' + String.valueOf(result) + '"';
                }
                return String.valueOf(result);
            }
            if (evalType == 1) {
                String leftGenerateCode = left.generateCode(environment);
                if (right == null) {
                    return leftGenerateCode;
                }
                String rightGenerateCode = right.generateCode(environment);
                String template = "%s%s %s %s%s";
                if (!left.isStaticExpr()) {
                    leftGenerateCode = "(" + leftGenerateCode + ")";
                }
                if (!right.isStaticExpr()) {
                    rightGenerateCode = "(" + rightGenerateCode + ")";
                }
                switch (opsType) {
                    case 1:
                        // *乘法 double
                        return String.format(template, "", leftGenerateCode, "*", "", rightGenerateCode);
                    case 2:
                        // /除法 double
                        return String.format(template, "", leftGenerateCode, "/", "", rightGenerateCode);
                    case 3:
                        // %取余数 double
                        return String.format(template, "", leftGenerateCode, "%", "", rightGenerateCode);
                    case 4:
                        // **指数 double
                        return String.format("Math.pow((double)(%s), (double)(%s))", leftGenerateCode, rightGenerateCode);
                    case 11:
                        // 加法 double
                        return String.format(template, "", leftGenerateCode, "+", "", rightGenerateCode);
                    case 12:
                        // -减法（理论上代码不可达） 因为'-'统一转化为了'+'(负) double
                        return String.format(template, "", leftGenerateCode, "-", "", rightGenerateCode);
                    case 21:
                        // >> 位运算右移 long
                        return String.format(template, "long", leftGenerateCode, ">>", "long", rightGenerateCode);
                    case 22:
                        // << 位运算右移 long
                        return String.format(template, "long", leftGenerateCode, "<<", "long", rightGenerateCode);
                    case 31:
                        // & long
                        return String.format(template, "long", leftGenerateCode, "&", "long", rightGenerateCode);
                    case 32:
                        // ^ long
                        return String.format(template, "long", leftGenerateCode, "^", "long", rightGenerateCode);
                    case 33:
                        // | long
                        return String.format(template, "long", leftGenerateCode, "|", "long", rightGenerateCode);
                    case 51:
                        // > double
                        return String.format(template, "", leftGenerateCode, ">", "", rightGenerateCode);
                    case 52:
                        // < double
                        return String.format(template, "", leftGenerateCode, "<", "", rightGenerateCode);
                    case 53:
                        // == Object
                        return String.format(template, "", leftGenerateCode, "==", "", rightGenerateCode);
                    case 54:
                        // >= double
                        return String.format(template, "", leftGenerateCode, ">=", "", rightGenerateCode);
                    case 55:
                        // <= double
                        return String.format(template, "", leftGenerateCode, "<=", "", rightGenerateCode);
                    case 56:
                        // != Object
                        return String.format(template, "Object", leftGenerateCode, "!=", "Object", rightGenerateCode);
                    case 61:
                        // && Boolean
                        return String.format(template, "", leftGenerateCode, "&&", "", rightGenerateCode);
                    case 62:
                        // || Boolean
                        return String.format(template, "", leftGenerateCode, "||", "", rightGenerateCode);
                    case 63:
                        // in 暂时不支持
                        throw new ExpressionException("暂时不支持'in'符号编译");
                    case 64:
                        // out 暂时不支持out
                        throw new ExpressionException("暂时不支持'out'符号编译");
                    case 70:
                        // ? 三目运算条件
                        return String.format("%s ? %s", leftGenerateCode, rightGenerateCode);
                    case 71:
                        // : 三目运算结果
                        return String.format("%s : %s", leftGenerateCode, rightGenerateCode);
                }

            } else if (evalType == 5) {
                // 括号运算
                if (negate) {
                    return "-(" + right.generateCode(environment) + ")";
                } else {
                    return "(" + right.generateCode(environment) + ")";
                }
            } else if (evalType == 6) {
                String typeName = environment.getVariableType(value);
                if (typeName == null) {
                    // 变量运算
                    if (negate) {
                        return "-(ObjectUtils.get(context, \"" + value + "\", Number.class)).doubleValue()";
                    } else {
                        return "ObjectUtils.get(context, \"" + value + "\", Number.class).doubleValue()";
                    }
                } else {
                    // 变量运算
                    if (negate) {
                        return "-(ObjectUtils.get(context, \"" + value + "\", " + typeName + "))";
                    } else {
                        return "ObjectUtils.get(context, \"" + value + "\", " + typeName + ")";
                    }
                }
            } else {
                // 其他统一返回left
                return "(" + left.generateCode(environment) + ")";
            }
            return null;
        }

    }

}
