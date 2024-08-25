/*
 * Copyright [2020-2024] [wangyunchao]
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0

 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */
package io.github.wycst.wast.common.expression;

import io.github.wycst.wast.common.utils.ObjectUtils;
import io.github.wycst.wast.common.utils.ReflectUtils;

import java.lang.reflect.Array;
import java.util.Collection;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 常量、变量、操作执行器
 *
 * @Author wangyunchao
 * @Date 2022/10/20 12:37
 */
public class ExprEvaluator {

    protected final static int EVAL_TYPE_OPERATOR = 1;
    protected final static int EVAL_TYPE_VARIABLE = 2;
    protected final static int EVAL_TYPE_FUN = 3;
    protected final static int EVAL_TYPE_BRACKET = 4;

    protected int evalType;
    protected ElOperator operator = ElOperator.NONE;

    ExprEvaluator left;
    ExprEvaluator right;

    boolean negate;
    boolean logicalNot;
    boolean constant;
    Object result;
    static final int OPTIMIZE_DEPTH_VALUE = 1 << 10;

    // use by code()
    public boolean isConstantExpr() {
        // if ':' retutn false
        if (operator == ElOperator.COLON) return false;
        if (left == null && right == null) {
            return this.constant;
        }
        if (left == null) {
            return right.isConstantExpr();
        }
        if (right == null) {
            return left.isConstantExpr();
        }
        return left.isConstantExpr() && right.isConstantExpr();
    }

    public int getEvalType() {
        return evalType;
    }

    public void setEvalType(int evalType) {
        this.evalType = evalType;
    }

    public void setOperator(ElOperator operator) {
        this.operator = operator;
    }

    public ExprEvaluator getLeft() {
        return left;
    }

    public void setLeft(ExprEvaluator left) {
        this.left = left;
    }

    public ExprEvaluator getRight() {
        return right;
    }

    public void setRight(ExprEvaluator right) {
        this.right = right;
    }

    String[] parseStringArr(String splitStr, AtomicInteger atomicInteger, AtomicBoolean strArr, AtomicBoolean doubleArr) {
        int length = splitStr.length();
        boolean isStrArr = false, isDoubleArr = false;
        String[] strings = new String[10];
        int beginIndex = 0;
        int arrLen = 0;
        int bracketCount = 0;
        int bigBracketCount = 0;
        for (int i = 0; i < length; i++) {
            char ch = splitStr.charAt(i);
            if (!isStrArr && (/*ch == '"' || */ch == '\'')) {
                isStrArr = true;
            }
            if (!isDoubleArr && ch == '.') {
                isDoubleArr = true;
            }
            if (ch == '(') {
                bracketCount++;
            } else if (ch == ')') {
                bracketCount--;
            } else if (ch == '{') {
                bigBracketCount++;
            } else if (ch == '}') {
                bigBracketCount--;
            } else {
                if (bracketCount == 0 && bigBracketCount == 0 && ch == ',') {
                    if (arrLen == strings.length) {
                        Object[] tmp = strings;
                        strings = new String[strings.length << 1];
                        System.arraycopy(tmp, 0, strings, 0, tmp.length);
                    }
                    String val = new String(splitStr.substring(beginIndex, i)).trim();
                    strings[arrLen++] = val;
                    beginIndex = i + 1;
                }
            }
        }
        if (arrLen == strings.length) {
            Object[] tmp = strings;
            strings = new String[strings.length << 1];
            System.arraycopy(tmp, 0, strings, 0, tmp.length);
        }
        // 添加最后一个
        strings[arrLen++] = new String(splitStr.substring(beginIndex, length)).trim();
        // 设置输出长度
        atomicInteger.set(arrLen);
        strArr.set(isStrArr);
        doubleArr.set(isDoubleArr);
        return strings;
    }


    /**
     * 设置数组常量，常量数组仅支持number数组和字符串数组
     * 去掉了开始和结束的标记，使用逗号分割进行解析
     *
     * @param splitStr
     */
    public void setArrayValue(String splitStr) {
        this.constant = true;
        splitStr = splitStr.trim();

        int length = splitStr.length();
        if (length == 0) {
            this.result = new Object[0];
            return;
        }

        AtomicInteger atomicInteger = new AtomicInteger();
        AtomicBoolean strArr = new AtomicBoolean();
        AtomicBoolean doubleArr = new AtomicBoolean();
        String[] strings = parseStringArr(splitStr, atomicInteger, strArr, doubleArr);

        int arrLen = atomicInteger.get();
        boolean isStrArr = strArr.get(), isDoubleArr = doubleArr.get();

        // 结果
        Object[] result;
        if (isStrArr) {
            result = new String[arrLen];
        } else {
            if (isDoubleArr) {
                result = new Double[arrLen];
            } else {
                result = new Long[arrLen];
            }
        }

        for (int i = 0; i < arrLen; i++) {
            String val = strings[i];
            if (isStrArr) {
                // 字符串数组
                if (val.startsWith("'") && val.endsWith("'")) {
                    val = new String(val.substring(1, val.length() - 1));
                } /*else if (val.startsWith("\"") && val.endsWith("\"")) {
                    val = val.substring(1, val.length() - 1);
                } */ else {
                    throw new ExpressionException("无效的字符串数组元素: " + val + ", 数组片段： '" + splitStr + "'");
                }
                result[i] = val;
            } else {
                if (isDoubleArr) {
                    result[i] = Double.parseDouble(val.trim());
                } else {
                    result[i] = Long.parseLong(val.trim());
                }
            }
        }
        this.result = result;
    }

    public boolean isNegate() {
        return negate;
    }

    public boolean isLogicalNot() {
        return logicalNot;
    }

    public ExprEvaluator negate(boolean negate) {
        this.negate = negate;
        return this;
    }

    public void setNegate(boolean negate) {
        this.negate = negate;
    }

    public void setLogicalNot(boolean logicalNot) {
        this.logicalNot = logicalNot;
    }

    public Object evaluate() {
        return evaluate(null, EvaluateEnvironment.DEFAULT);
    }

    public Object evaluate(EvaluatorContext context, EvaluateEnvironment evaluateEnvironment) {
        if (this.constant) {
            // 字符串，数字，常量数组等
            return this.result;
        }
        if (evalType == 0) {
            result = left.evaluate(context, evaluateEnvironment);
            this.constant = left.constant;
            return result;
        } else if (evalType == EVAL_TYPE_OPERATOR) {
            Object leftValue = left.evaluate(context, evaluateEnvironment);
            if (right == null) {
                this.constant = left.constant;
                return result = leftValue;
            }
            Object rightValue = null;
            if (operator == ElOperator.QUESTION) {
                // ?运算不计算right分支(根据left的值获取right子表达式的r-left和r-right)
                Object result = right.evaluateTernary(context, evaluateEnvironment, (Boolean) leftValue, left.constant);
                this.constant = left.constant && right.constant;
                if (this.constant) {
                    this.result = result;
                }
                return result;
            } else {
                rightValue = right.evaluate(context, evaluateEnvironment);
            }

            if (evaluateEnvironment.isAutoParseStringAsDouble()) {
                if (leftValue instanceof String) {
                    leftValue = Double.parseDouble(leftValue.toString());
                }
                if (rightValue instanceof String) {
                    rightValue = Double.parseDouble(rightValue.toString());
                }
            }

            this.constant = left.constant && right.constant;
            boolean isDouble = leftValue instanceof Double || rightValue instanceof Double;
            // use for debug
            // System.out.println("leftValue: " + leftValue + " rightValue: " + rightValue + " opsType: " + opsType);
            switch (operator) {
                case MULTI:
                    // *乘法
                    if (isDouble) {
                        result = ((Number) leftValue).doubleValue() * ((Number) rightValue).doubleValue();
                    } else {
                        result = ((Number) leftValue).longValue() * ((Number) rightValue).longValue();
                    }
                    return result;
                case DIVISION:
                    // /除法
                    if (isDouble) {
                        result = ((Number) leftValue).doubleValue() / ((Number) rightValue).doubleValue();
                    } else {
                        result = ((Number) leftValue).longValue() / ((Number) rightValue).longValue();
                    }
                    return result;
                case MOD:
                    // %取余数
                    if (isDouble) {
                        result = ((Number) leftValue).doubleValue() % ((Number) rightValue).doubleValue();
                    } else {
                        result = ((Number) leftValue).longValue() % ((Number) rightValue).longValue();
                    }
                    return result;
                case EXP:
                    // **指数，平方/根
                    if (isDouble) {
                        result = Math.pow(((Number) leftValue).doubleValue(), ((Number) rightValue).doubleValue());
                    } else {
                        result = Math.pow(((Number) leftValue).longValue(), ((Number) rightValue).longValue());
                    }
                    return result;
                case PLUS:
                    // +加法
                    if (leftValue instanceof Number && rightValue instanceof Number) {
                        // 数值加法
                        if (isDouble) {
                            result = ((Number) leftValue).doubleValue() + ((Number) rightValue).doubleValue();
                        } else {
                            result = ((Number) leftValue).longValue() + ((Number) rightValue).longValue();
                        }
                        return result;
                    }
                    // 字符串加法
                    return leftValue + String.valueOf(rightValue);
                case MINUS:
                    // -减法（理论上代码不可达） 因为'-'统一转化为了'+'(负)
                    if (isDouble) {
                        result = ((Number) leftValue).doubleValue() - ((Number) rightValue).doubleValue();
                    } else {
                        result = ((Number) leftValue).longValue() - ((Number) rightValue).longValue();
                    }
                    return result;
                case BIT_RIGHT:
                    // >> 位运算右移
                    return result = ((Number) leftValue).longValue() >> ((Number) rightValue).longValue();
                case BIT_LEFT:
                    // << 位运算左移
                    return result = ((Number) leftValue).longValue() << ((Number) rightValue).longValue();
                case AND:
                    // &
                    return result = ((Number) leftValue).longValue() & ((Number) rightValue).longValue();
                case XOR:
                    // ^
                    return result = ((Number) leftValue).longValue() ^ ((Number) rightValue).longValue();
                case OR:
                    // |
                    return result = ((Number) leftValue).longValue() | ((Number) rightValue).longValue();
//                    case 43:
//                        // 代码不可达 !
//                        return result = !(rightValue == Boolean.TRUE) ;
                case GT:
                    // >
                    return result = ((Number) leftValue).doubleValue() > ((Number) rightValue).doubleValue();
                case LT:
                    // <
                    return result = ((Number) leftValue).doubleValue() < ((Number) rightValue).doubleValue();
                case EQ:
                    // ==
                    if (leftValue instanceof Number && rightValue instanceof Number) {
                        return result = ((Number) leftValue).doubleValue() == ((Number) rightValue).doubleValue();
                    }
                    if (leftValue == rightValue) {
                        return result = true;
                    }
                    return result = leftValue != null && leftValue.equals(rightValue);
                case GE:
                    // >=
                    return result = ((Number) leftValue).doubleValue() >= ((Number) rightValue).doubleValue();
                case LE:
                    // <=
                    return result = ((Number) leftValue).doubleValue() <= ((Number) rightValue).doubleValue();
                case NE:
                    // !=
                    if (leftValue instanceof Number && rightValue instanceof Number) {
                        // 基础类型double直接比较值
                        return result = ((Number) leftValue).doubleValue() != ((Number) rightValue).doubleValue();
                    }
                    if (leftValue == rightValue) {
                        return result = false;
                    }
                    return result = leftValue == null || !leftValue.equals(rightValue);
                case LOGICAL_AND:
                    // &&
                    return result = (Boolean) leftValue && (Boolean) rightValue;
                case LOGICAL_OR:
                    // ||
                    return result = (Boolean) leftValue || (Boolean) rightValue;
                case IN:
                    /// in
                    return result = this.evaluateIn(leftValue, rightValue);
                case OUT:
                    /// out
                    return result = !this.evaluateIn(leftValue, rightValue);
                case COLON:
                    // : 三目运算结果, 不支持单独运算
                    throw new ExpressionException(" 不支持单独使用冒号运算符: ':'");
                case QUESTION:
                    // ? 三目运算条件
                    return right.evaluateTernary(context, evaluateEnvironment, (Boolean) leftValue, left.constant);
            }
            // 默认返回null
        } else if (evalType == EVAL_TYPE_BRACKET) {
            // Brackets operation calculates right
            Object bracketValue = right.evaluate(context, evaluateEnvironment);
            this.constant = right.constant;
            if (negate) {
                return result = ExprUtils.getNegateNumber(bracketValue);
            }
            if (logicalNot) {
                return result = bracketValue == Boolean.FALSE || bracketValue == null;
            }
            return result = bracketValue;
        } else {
            // Other unified returns to left
            result = left.evaluate(context, evaluateEnvironment);
            this.constant = left.constant;
            return result;
        }
        return null;
    }

    /**
     * in表达式
     */
    boolean evaluateIn(Object leftValue, Object rightValue) {
        if (leftValue == null || rightValue == null)
            return false;
        if (rightValue instanceof Collection) {
            Collection collection = (Collection) rightValue;
            for (Object value : collection) {
                boolean isEqual = this.execEquals(value, leftValue);
                if (isEqual) {
                    return true;
                }
            }
        } else if (rightValue.getClass().isArray()) {
            int length = Array.getLength(rightValue);
            for (int i = 0; i < length; i++) {
                Object value = Array.get(rightValue, i);
                boolean isEqual = this.execEquals(value, leftValue);
                if (isEqual) {
                    return true;
                }
            }
        } else {
            if (rightValue instanceof Number || rightValue instanceof CharSequence) {
                return false;
            }
            return ObjectUtils.contains(rightValue, String.valueOf(leftValue));
        }
        return false;
    }

    private boolean execEquals(Object value, Object leftValue) {
        if (value instanceof Number && leftValue instanceof Number) {
            if (((Number) value).doubleValue() == ((Number) leftValue).doubleValue()) {
                return true;
            }
        }
        if (value == leftValue || String.valueOf(leftValue).equals(String.valueOf(value))) {
            return true;
        }
        return false;
    }

    /**
     * 运行三目运算符号
     */
    Object evaluateTernary(EvaluatorContext context, EvaluateEnvironment evaluateEnvironment, Boolean bool, boolean isStatic) {
        if (this.constant) {
            return this.result;
        }
        Object result = null;
        if (bool == null || bool == Boolean.FALSE) {
            this.constant = isStatic && right.constant;
            result = right.evaluate(context, evaluateEnvironment);
        } else {
            this.constant = isStatic && left.constant;
            result = left.evaluate(context, evaluateEnvironment);
        }
        if (this.constant) {
            this.result = result;
        }
        return result;
    }

    final ExprEvaluator update(ExprEvaluator left, ExprEvaluator right) {
        this.left = left;
        this.right = right;
        return this;
    }

    /**
     * 变量执行器
     */
    static class VariableImpl extends ExprEvaluator {

        // 变量访问模型
        ElVariableInvoker variableInvoker;

        public VariableImpl() {
            this.evalType = EVAL_TYPE_VARIABLE;
        }

        /***
         * 设置变量值？
         *
         * @param variableInvoker
         */
        public void setVariableInvoker(ElVariableInvoker variableInvoker) {
            this.variableInvoker = variableInvoker;
        }

        public VariableImpl normal() {
            return new NormalVariableImpl(variableInvoker);
        }

        public final Object getVariableValue(EvaluatorContext evaluatorContext, EvaluateEnvironment evaluateEnvironment) {
            Object obj = null;
            try {
                obj = evaluatorContext.getContextValue(variableInvoker); // context.variableValues[variableInvoke.index];
                if (obj == null && !evaluateEnvironment.isAllowVariableNull()) {
                    throw new ExpressionException("Unresolved property or variable:'" + variableInvoker + "' by context");
                }
                if (evaluateEnvironment.isAutoParseStringAsDouble() && obj instanceof String) {
                    obj = Double.parseDouble((String) obj);
                }
            } catch (RuntimeException e) {
                if (obj == null && evaluatorContext == null) {
                    throw new ExpressionException("Unresolved property or variable:'" + variableInvoker + "' by context");
                } else {
                    throw e;
                }
            }
            return obj;
        }

        @Override
        public Object evaluate(EvaluatorContext context, EvaluateEnvironment evaluateEnvironment) {
            Object obj = getVariableValue(context, evaluateEnvironment);
            if (negate) {
                Number number = (Number) obj;
                if (number instanceof Double || number instanceof Float) {
                    return -number.doubleValue();
                }
                if (number instanceof Integer) {
                    return -number.intValue();
                }
                return -number.longValue();
            }
            if (logicalNot) {
                return obj == Boolean.FALSE || obj == null;
            }
            return obj;
        }

        @Override
        public String code() {
            StringBuilder builder = new StringBuilder();
            if(negate) {
                builder.append('-');
            }
            if(logicalNot) {
                builder.append("!(");
            }
            builder.append("_$").append(variableInvoker.getIndex());
            if(logicalNot) {
                builder.append(")");
            }
            return builder.toString();
        }

        // 内联key
        ExprEvaluator internKey() {
            variableInvoker.internKey();
            return this;
        }

        @Override
        public String toString() {
            return "VariableImpl{" + variableInvoker + "}";
        }

        final static class NormalVariableImpl extends VariableImpl {
            NormalVariableImpl(ElVariableInvoker variableInvoke) {
                setVariableInvoker(variableInvoke);
            }

            @Override
            public Object evaluate(EvaluatorContext context, EvaluateEnvironment evaluateEnvironment) {
                return getVariableValue(context, evaluateEnvironment);
            }
        }
    }

    /**
     * 常量执行器(字符串,数字,布尔值,null)
     */
    final static class ConstantImpl extends ExprEvaluator {

        ConstantImpl(Object result) {
            this.result = result;
            this.constant = true;
        }

        @Override
        public Object evaluate(EvaluatorContext context, EvaluateEnvironment evaluateEnvironment) {
            return result;
        }

        @Override
        public String toString() {
            return String.valueOf(result);
        }

        @Override
        public String code() {
            if (result instanceof String) {
                StringBuilder builder = new StringBuilder();
                String strValue = (String) result;
                if (strValue.indexOf('"') > -1) {
                    strValue = strValue.replace("\"", "\\\"");
                }
                return builder.append("\"").append(strValue).append("\"").toString();
            }
            return String.valueOf(result);
        }
    }

    /**
     * 括号执行器
     */
    final static class BracketImpl extends ExprEvaluator {
        static BracketImpl of(ExprEvaluator evaluator) {
            BracketImpl exprEvaluatorImpl = new BracketImpl();
            exprEvaluatorImpl.right = evaluator.right;
            exprEvaluatorImpl.negate = evaluator.negate;
            exprEvaluatorImpl.logicalNot = evaluator.logicalNot;
            return exprEvaluatorImpl;
        }

        @Override
        public String toString() {
            return "BracketImpl{()}";
        }

        @Override
        public Object evaluate(EvaluatorContext context, EvaluateEnvironment evaluateEnvironment) {
            Object bracketValue = right.evaluate(context, evaluateEnvironment);
            if (negate) {
                if (bracketValue instanceof Double) {
                    return -(Double) bracketValue;
                } else if (bracketValue instanceof Long) {
                    return -(Long) bracketValue;
                } else if (bracketValue instanceof Integer) {
                    return -(Integer) bracketValue;
                } else {
                    return -((Number) bracketValue).doubleValue();
                }
            }
            if (logicalNot) {
                return bracketValue == Boolean.FALSE || bracketValue == null;
            }
            return bracketValue;
        }
    }

    /**
     * 加法执行器
     */
    final static class PlusImpl extends ExprEvaluator {
        static PlusImpl of(ExprEvaluator evaluator) {
            PlusImpl evaluatorImpl = new PlusImpl();
            evaluatorImpl.left = evaluator.left;
            evaluatorImpl.right = evaluator.right;
            return evaluatorImpl;
        }

        @Override
        public String toString() {
            return "PlusImpl{+}";
        }

        @Override
        public Object evaluate(EvaluatorContext context, EvaluateEnvironment evaluateEnvironment) {
            Object leftValue = left.evaluate(context, evaluateEnvironment);
            Object rightValue = right.evaluate(context, evaluateEnvironment);
            // +plus
            if (leftValue instanceof Number && rightValue instanceof Number) {
                boolean isDouble = leftValue instanceof Double || rightValue instanceof Double;
                if (isDouble) {
                    return ((Number) leftValue).doubleValue() + ((Number) rightValue).doubleValue();
                } else {
                    return ((Number) leftValue).longValue() + ((Number) rightValue).longValue();
                }
            }
            if(String.class.isInstance(leftValue) || String.class.isInstance(rightValue)) {
                StringBuilder builder = context.getStringBuilder();
                builder.append(leftValue).append(rightValue);
                return builder.toString();
            }

            throw new ExpressionException("not supported operate '+'");
        }
    }

    final static class MinusImpl extends ExprEvaluator {

        static MinusImpl of(ExprEvaluator evaluator) {
            MinusImpl evaluatorImpl = new MinusImpl();
            evaluatorImpl.left = evaluator.left;
            evaluatorImpl.right = evaluator.right;
            return evaluatorImpl;
        }

        @Override
        public String toString() {
            return "MinusImpl{-}";
        }

        @Override
        public Object evaluate(EvaluatorContext context, EvaluateEnvironment evaluateEnvironment) {
            Object leftValue = left.evaluate(context, evaluateEnvironment);
            Object rightValue = right.evaluate(context, evaluateEnvironment);
            boolean isDouble = leftValue instanceof Double || rightValue instanceof Double;
            // -减法
            if (isDouble) {
                return ((Number) leftValue).doubleValue() - ((Number) rightValue).doubleValue();
            } else {
                return ((Number) leftValue).longValue() - ((Number) rightValue).longValue();
            }
        }
    }

    final static class MultiplyImpl extends ExprEvaluator {
        static MultiplyImpl of(ExprEvaluator evaluator) {
            MultiplyImpl evaluatorImpl = new MultiplyImpl();
            evaluatorImpl.left = evaluator.left;
            evaluatorImpl.right = evaluator.right;
            return evaluatorImpl;
        }

        @Override
        public String toString() {
            return "MultiplyImpl{*}";
        }

        @Override
        public Object evaluate(EvaluatorContext context, EvaluateEnvironment evaluateEnvironment) {
            Object leftValue = left.evaluate(context, evaluateEnvironment);
            Object rightValue = right.evaluate(context, evaluateEnvironment);
            boolean isDouble = leftValue instanceof Double || rightValue instanceof Double;
            // *乘法
            if (isDouble) {
                return ((Number) leftValue).doubleValue() * ((Number) rightValue).doubleValue();
            } else {
                return ((Number) leftValue).longValue() * ((Number) rightValue).longValue();
            }
        }
    }

    final static class PowerImpl extends ExprEvaluator {
        static PowerImpl of(ExprEvaluator evaluator) {
            PowerImpl evaluatorImpl = new PowerImpl();
            evaluatorImpl.left = evaluator.left;
            evaluatorImpl.right = evaluator.right;
            return evaluatorImpl;
        }

        @Override
        public String toString() {
            return "PowerImpl{**}";
        }

        @Override
        public Object evaluate(EvaluatorContext context, EvaluateEnvironment evaluateEnvironment) {
            Object leftValue = left.evaluate(context, evaluateEnvironment);
            Object rightValue = right.evaluate(context, evaluateEnvironment);
            double powValue = Math.pow(((Number) leftValue).doubleValue(), ((Number) rightValue).doubleValue());
            long longVal = (long) powValue;
            if (longVal == powValue) {
                return longVal;
            } else {
                return powValue;
            }
        }
    }

    final static class DivisionImpl extends ExprEvaluator {
        static DivisionImpl of(ExprEvaluator evaluator) {
            DivisionImpl evaluatorImpl = new DivisionImpl();
            evaluatorImpl.left = evaluator.left;
            evaluatorImpl.right = evaluator.right;
            return evaluatorImpl;
        }

        @Override
        public String toString() {
            return "DivisionImpl{/}";
        }

        @Override
        public Object evaluate(EvaluatorContext context, EvaluateEnvironment evaluateEnvironment) {
            Object leftValue = left.evaluate(context, evaluateEnvironment);
            Object rightValue = right.evaluate(context, evaluateEnvironment);
            boolean isDouble = leftValue instanceof Double || rightValue instanceof Double;
            // (/)
            if (isDouble) {
                return ((Number) leftValue).doubleValue() / ((Number) rightValue).doubleValue();
            } else {
                return ((Number) leftValue).longValue() / ((Number) rightValue).longValue();
            }
        }
    }

    /**
     * 取模（求余）执行器modulus
     */
    final static class ModulusImpl extends ExprEvaluator {
        static ModulusImpl of(ExprEvaluator evaluator) {
            ModulusImpl evaluatorImpl = new ModulusImpl();
            evaluatorImpl.left = evaluator.left;
            evaluatorImpl.right = evaluator.right;
            return evaluatorImpl;
        }

        @Override
        public String toString() {
            return "ModulusImpl{%}";
        }

        @Override
        public Object evaluate(EvaluatorContext context, EvaluateEnvironment evaluateEnvironment) {
            Object leftValue = left.evaluate(context, evaluateEnvironment);
            Object rightValue = right.evaluate(context, evaluateEnvironment);
            boolean isDouble = leftValue instanceof Double || rightValue instanceof Double;
            if (isDouble) {
                return ((Number) leftValue).doubleValue() % ((Number) rightValue).doubleValue();
            } else {
                return ((Number) leftValue).longValue() % ((Number) rightValue).longValue();
            }
        }
    }

    // bit left
    final static class BitLeftImpl extends ExprEvaluator {
        static BitLeftImpl of(ExprEvaluator evaluator) {
            BitLeftImpl evaluatorImpl = new BitLeftImpl();
            evaluatorImpl.left = evaluator.left;
            evaluatorImpl.right = evaluator.right;
            return evaluatorImpl;
        }

        @Override
        public String toString() {
            return "BitLeftImpl{<<}";
        }

        @Override
        public Object evaluate(EvaluatorContext context, EvaluateEnvironment evaluateEnvironment) {
            Object leftValue = left.evaluate(context, evaluateEnvironment);
            Object rightValue = right.evaluate(context, evaluateEnvironment);
            return ((Number) leftValue).longValue() << ((Number) rightValue).longValue();
        }
    }

    final static class BitRightImpl extends ExprEvaluator {
        static BitRightImpl of(ExprEvaluator evaluator) {
            BitRightImpl evaluatorImpl = new BitRightImpl();
            evaluatorImpl.left = evaluator.left;
            evaluatorImpl.right = evaluator.right;
            return evaluatorImpl;
        }

        @Override
        public String toString() {
            return "BitRightImpl{>>}";
        }

        @Override
        public Object evaluate(EvaluatorContext context, EvaluateEnvironment evaluateEnvironment) {
            Object leftValue = left.evaluate(context, evaluateEnvironment);
            Object rightValue = right.evaluate(context, evaluateEnvironment);
            return ((Number) leftValue).longValue() >> ((Number) rightValue).longValue();
        }
    }

    final static class BitAndImpl extends ExprEvaluator {
        static BitAndImpl of(ExprEvaluator evaluator) {
            BitAndImpl evaluatorImpl = new BitAndImpl();
            evaluatorImpl.left = evaluator.left;
            evaluatorImpl.right = evaluator.right;
            return evaluatorImpl;
        }

        @Override
        public String toString() {
            return "BitAndImpl{&}";
        }

        @Override
        public Object evaluate(EvaluatorContext context, EvaluateEnvironment evaluateEnvironment) {
            Object leftValue = left.evaluate(context, evaluateEnvironment);
            Object rightValue = right.evaluate(context, evaluateEnvironment);
            return ((Number) leftValue).longValue() & ((Number) rightValue).longValue();
        }
    }

    final static class BitOrImpl extends ExprEvaluator {
        static BitOrImpl of(ExprEvaluator evaluator) {
            BitOrImpl evaluatorImpl = new BitOrImpl();
            evaluatorImpl.left = evaluator.left;
            evaluatorImpl.right = evaluator.right;
            return evaluatorImpl;
        }

        @Override
        public String toString() {
            return "BitOrImpl{|}";
        }

        @Override
        public Object evaluate(EvaluatorContext context, EvaluateEnvironment evaluateEnvironment) {
            Object leftValue = left.evaluate(context, evaluateEnvironment);
            Object rightValue = right.evaluate(context, evaluateEnvironment);
            return ((Number) leftValue).longValue() | ((Number) rightValue).longValue();
        }
    }

    final static class BitXorImpl extends ExprEvaluator {
        static BitXorImpl of(ExprEvaluator evaluator) {
            BitXorImpl evaluatorImpl = new BitXorImpl();
            evaluatorImpl.left = evaluator.left;
            evaluatorImpl.right = evaluator.right;
            return evaluatorImpl;
        }

        @Override
        public String toString() {
            return "BitOrImpl{^}";
        }

        @Override
        public Object evaluate(EvaluatorContext context, EvaluateEnvironment evaluateEnvironment) {
            Object leftValue = left.evaluate(context, evaluateEnvironment);
            Object rightValue = right.evaluate(context, evaluateEnvironment);
            // ^ 异或运算
            return ((Number) leftValue).longValue() ^ ((Number) rightValue).longValue();
        }
    }

    final static class EqualImpl extends ExprEvaluator {
        static EqualImpl of(ExprEvaluator evaluator) {
            EqualImpl evaluatorImpl = new EqualImpl();
            evaluatorImpl.left = evaluator.left;
            evaluatorImpl.right = evaluator.right;
            return evaluatorImpl;
        }

        @Override
        public String toString() {
            return "EqualImpl{==}";
        }

        @Override
        public Object evaluate(EvaluatorContext context, EvaluateEnvironment evaluateEnvironment) {
            Object leftValue = left.evaluate(context, evaluateEnvironment);
            Object rightValue = right.evaluate(context, evaluateEnvironment);
            // == 运算
            if (leftValue instanceof Number && rightValue instanceof Number) {
                return ((Number) leftValue).doubleValue() == ((Number) rightValue).doubleValue();
            }
            if (leftValue == rightValue) {
                return true;
            }
            return leftValue != null && leftValue.equals(rightValue);
        }
    }

    final static class GtImpl extends ExprEvaluator {
        static GtImpl of(ExprEvaluator evaluator) {
            GtImpl evaluatorImpl = new GtImpl();
            evaluatorImpl.left = evaluator.left;
            evaluatorImpl.right = evaluator.right;
            return evaluatorImpl;
        }

        @Override
        public String toString() {
            return "GtImpl{>}";
        }

        @Override
        public Object evaluate(EvaluatorContext context, EvaluateEnvironment evaluateEnvironment) {
            Object leftValue = left.evaluate(context, evaluateEnvironment);
            Object rightValue = right.evaluate(context, evaluateEnvironment);
            return ((Number) leftValue).doubleValue() > ((Number) rightValue).doubleValue();
        }
    }

    final static class LtImpl extends ExprEvaluator {
        static LtImpl of(ExprEvaluator evaluator) {
            LtImpl evaluatorImpl = new LtImpl();
            evaluatorImpl.left = evaluator.left;
            evaluatorImpl.right = evaluator.right;
            return evaluatorImpl;
        }

        @Override
        public String toString() {
            return "LtImpl{<}";
        }

        @Override
        public Object evaluate(EvaluatorContext context, EvaluateEnvironment evaluateEnvironment) {
            Object leftValue = left.evaluate(context, evaluateEnvironment);
            Object rightValue = right.evaluate(context, evaluateEnvironment);
            return ((Number) leftValue).doubleValue() < ((Number) rightValue).doubleValue();
        }
    }

    final static class GEImpl extends ExprEvaluator {
        static GEImpl of(ExprEvaluator evaluator) {
            GEImpl evaluatorImpl = new GEImpl();
            evaluatorImpl.left = evaluator.left;
            evaluatorImpl.right = evaluator.right;
            return evaluatorImpl;
        }

        @Override
        public String toString() {
            return "GEImpl{>=}";
        }

        @Override
        public Object evaluate(EvaluatorContext context, EvaluateEnvironment evaluateEnvironment) {
            Object leftValue = left.evaluate(context, evaluateEnvironment);
            Object rightValue = right.evaluate(context, evaluateEnvironment);
            return ((Number) leftValue).doubleValue() >= ((Number) rightValue).doubleValue();
        }
    }

    final static class LEImpl extends ExprEvaluator {
        static LEImpl of(ExprEvaluator evaluator) {
            LEImpl evaluatorImpl = new LEImpl();
            evaluatorImpl.left = evaluator.left;
            evaluatorImpl.right = evaluator.right;
            return evaluatorImpl;
        }

        @Override
        public String toString() {
            return "LEImpl{<=}";
        }

        @Override
        public Object evaluate(EvaluatorContext context, EvaluateEnvironment evaluateEnvironment) {
            Object leftValue = left.evaluate(context, evaluateEnvironment);
            Object rightValue = right.evaluate(context, evaluateEnvironment);
            return ((Number) leftValue).doubleValue() <= ((Number) rightValue).doubleValue();
        }
    }

    final static class NEImpl extends ExprEvaluator {
        static NEImpl of(ExprEvaluator evaluator) {
            NEImpl evaluatorImpl = new NEImpl();
            evaluatorImpl.left = evaluator.left;
            evaluatorImpl.right = evaluator.right;
            return evaluatorImpl;
        }

        @Override
        public String toString() {
            return "NEImpl{!=}";
        }

        @Override
        public Object evaluate(EvaluatorContext context, EvaluateEnvironment evaluateEnvironment) {
            Object leftValue = left.evaluate(context, evaluateEnvironment);
            Object rightValue = right.evaluate(context, evaluateEnvironment);
            if (leftValue instanceof Number && rightValue instanceof Number) {
                return ((Number) leftValue).doubleValue() != ((Number) rightValue).doubleValue();
            }
            if (leftValue == rightValue) {
                return false;
            }
            return leftValue == null || !leftValue.equals(rightValue);
        }
    }

    final static class LogicalAndImpl extends ExprEvaluator {
        static LogicalAndImpl of(ExprEvaluator evaluator) {
            LogicalAndImpl evaluatorImpl = new LogicalAndImpl();
            evaluatorImpl.left = evaluator.left;
            evaluatorImpl.right = evaluator.right;
            return evaluatorImpl;
        }

        @Override
        public String toString() {
            return "LogicalAndImpl{&&}";
        }

        @Override
        public Object evaluate(EvaluatorContext context, EvaluateEnvironment evaluateEnvironment) {
            Object leftValue = left.evaluate(context, evaluateEnvironment);
            Object rightValue = right.evaluate(context, evaluateEnvironment);
            // &&
            return (Boolean) leftValue && (Boolean) rightValue;
        }
    }

    final static class LogicalOrImpl extends ExprEvaluator {
        static LogicalOrImpl of(ExprEvaluator evaluator) {
            LogicalOrImpl evaluatorImpl = new LogicalOrImpl();
            evaluatorImpl.left = evaluator.left;
            evaluatorImpl.right = evaluator.right;
            return evaluatorImpl;
        }

        @Override
        public String toString() {
            return "LogicalOrImpl{||}";
        }

        @Override
        public Object evaluate(EvaluatorContext context, EvaluateEnvironment evaluateEnvironment) {
            Object leftValue = left.evaluate(context, evaluateEnvironment);
            Object rightValue = right.evaluate(context, evaluateEnvironment);
            // ||
            return (Boolean) leftValue || (Boolean) rightValue;
        }
    }

    final static class TernaryImpl extends ExprEvaluator {
        private ExprEvaluator condition;
        private ExprEvaluator question;
        private ExprEvaluator colon;

        static TernaryImpl of(ExprEvaluator evaluator) {
            TernaryImpl evaluatorImpl = new TernaryImpl();
            evaluatorImpl.condition = evaluator.left;
            evaluatorImpl.question = evaluator.right.left;
            evaluatorImpl.colon = evaluator.right.right;
            return evaluatorImpl;
        }

        @Override
        public String toString() {
            return "TernaryImpl{?:}";
        }

        @Override
        public Object evaluate(EvaluatorContext context, EvaluateEnvironment evaluateEnvironment) {
            Object leftValue = condition.evaluate(context, evaluateEnvironment);
            boolean conditionResult = leftValue == null ? false : (Boolean) leftValue;
            return conditionResult ? question.evaluate(context, evaluateEnvironment) : colon.evaluate(context, evaluateEnvironment);/*right.evaluateTernary(context, evaluateEnvironment, (Boolean) leftValue, left.isStatic)*/
        }
    }

    final static class InImpl extends ExprEvaluator {
        static InImpl of(ExprEvaluator evaluator) {
            InImpl evaluatorImpl = new InImpl();
            evaluatorImpl.left = evaluator.left;
            evaluatorImpl.right = evaluator.right;
            return evaluatorImpl;
        }

        @Override
        public String toString() {
            return "InImpl{in}";
        }

        @Override
        public Object evaluate(EvaluatorContext context, EvaluateEnvironment evaluateEnvironment) {
            Object leftValue = left.evaluate(context, evaluateEnvironment);
            Object rightValue = right.evaluate(context, evaluateEnvironment);
            // in
            return this.evaluateIn(leftValue, rightValue);
        }
    }

    final static class OutImpl extends ExprEvaluator {
        static OutImpl of(ExprEvaluator evaluator) {
            OutImpl evaluatorImpl = new OutImpl();
            evaluatorImpl.left = evaluator.left;
            evaluatorImpl.right = evaluator.right;
            return evaluatorImpl;
        }

        @Override
        public String toString() {
            return "OutImpl{out}";
        }

        @Override
        public Object evaluate(EvaluatorContext context, EvaluateEnvironment evaluateEnvironment) {
            Object leftValue = left.evaluate(context, evaluateEnvironment);
            Object rightValue = right.evaluate(context, evaluateEnvironment);
            // out
            return !this.evaluateIn(leftValue, rightValue);
        }
    }

    static class FunctionImpl extends ExprEvaluator {
        // 函数名称
        String functionName;
        // 参数表达式数组
        String[] paramExprs;
        // 参数数组长度
        int paramLength;
        // 参数表达式数组
        ExprParser[] paramExprParsers;

        public FunctionImpl() {
            this.evalType = EVAL_TYPE_FUN;
        }

        @Override
        public String toString() {
            return "FunctionImpl{@function}";
        }

        @Override
        public String code() {
            //template: "%s(%s)"
            StringBuilder builder = new StringBuilder();
            builder.append(functionName)
                    .append("(");
            for (int i = 0; i < paramLength; i++) {
                builder.append(paramExprParsers[i].getEvaluator().code());
                if (i < paramLength - 1) {
                    builder.append(",");
                }
            }
            builder.append(")");
            return builder.toString();
        }

        // 以@作为标记
        public void setFunction(String funName, String params, ExprParser global) {

            this.functionName = funName.trim();
            if (params.isEmpty()) {
                this.paramLength = 0;
                this.paramExprs = new String[0];
                return;
            }

            // 解析数据
            AtomicInteger atomicInteger = new AtomicInteger();
            AtomicBoolean strArr = new AtomicBoolean(true);
            AtomicBoolean doubleArr = new AtomicBoolean(true);
            String[] paramExprs = parseStringArr(params, atomicInteger, strArr, doubleArr);

            this.paramExprs = paramExprs;
            this.paramLength = atomicInteger.get();

            ExprParser[] paramExprParsers = new ExprParser[paramLength];
            for (int i = 0; i < paramLength; i++) {
                paramExprParsers[i] = new ExprChildParser(paramExprs[i], global);
            }
            this.paramExprParsers = paramExprParsers;
        }

        // 执行函数表达式
        Object evaluateFunction(EvaluatorContext evaluatorContext, EvaluateEnvironment evaluateEnvironment) {
            // 函数参数
            Object[] params = new Object[paramLength];
            for (int i = 0; i < paramLength; i++) {
                params[i] = paramExprParsers[i].doEvaluate(evaluatorContext, evaluateEnvironment);
            }
            ExprFunction exprFunction = evaluateEnvironment.getFunction(this.functionName);
            if (exprFunction == null) {
                throw new ExpressionException("function '" + functionName + "' is unregistered!");
            }
            return exprFunction.call(params);
        }

        @Override
        public Object evaluate(EvaluatorContext evaluatorContext, EvaluateEnvironment evaluateEnvironment) {
            // 执行函数
            Object functionValue = this.evaluateFunction(evaluatorContext, evaluateEnvironment);
            if (negate) {
                return ExprUtils.getNegateNumber(functionValue);
            }
            if (logicalNot) {
                return functionValue == Boolean.FALSE || functionValue == null;
            }
            return functionValue;
        }
    }

    /**
     * java对象方法调用（解析模式下使用反射调用，编译模式和常规编码一致）
     */
    final static class MethodImpl extends FunctionImpl {
        ElVariableInvoker variableInvoker;

        @Override
        public String toString() {
            return "MethodImpl{@method}";
        }

        @Override
        public String code() {
            // template: "_$%d.%s(%s)"
            StringBuilder builder = new StringBuilder();
            builder.append("_$")
                    .append(variableInvoker.getIndex())
                    .append(".")
                    .append(functionName)
                    .append("(");
            for (int i = 0; i < paramLength; i++) {
                builder.append(paramExprParsers[i].getEvaluator().code());
                if (i < paramLength - 1) {
                    builder.append(",");
                }
            }
            builder.append(")");
            return builder.toString();
        }

        // 以@作为标记
        public void setMethod(ElVariableInvoker variableInvoker, String methodName, String params, ExprParser global) {
            this.variableInvoker = variableInvoker;
            setFunction(methodName, params, global);
        }

        Object evaluateMethod(EvaluatorContext evaluatorContext, EvaluateEnvironment evaluateEnvironment) {
            // 获取方法调用者
            Object invokeObj = null;
            try {
                invokeObj = evaluatorContext.getContextValue(variableInvoker);
            } catch (RuntimeException e) {
                if (invokeObj == null) {
                    throw new ExpressionException("Unresolved property or variable:'" + variableInvoker + "' by context");
                } else {
                    throw e;
                }
            }

            // 函数参数
            Object[] params = new Object[paramLength];
            for (int i = 0; i < paramLength; i++) {
                params[i] = paramExprParsers[i].doEvaluate(evaluatorContext, evaluateEnvironment);
            }
            try {
                return ReflectUtils.invoke(invokeObj, functionName, params);
            } catch (Throwable throwable) {
                throw new ExpressionException(String.format("invoke error, %s and methodName '%s', message: %s", invokeObj.getClass(), functionName, throwable.getMessage()));
            }
        }

        @Override
        public Object evaluate(EvaluatorContext evaluatorContext, EvaluateEnvironment evaluateEnvironment) {
            // 执行函数
            Object functionValue = this.evaluateMethod(evaluatorContext, evaluateEnvironment);
            if (negate) {
                return ExprUtils.getNegateNumber(functionValue);
            }
            if (logicalNot) {
                return functionValue == Boolean.FALSE || functionValue == null;
            }
            return functionValue;
        }
    }

    /**
     * Stack splitting optimizer to solve the problem of stack depth overflow;
     * It is generally not useful. When the native compiler cannot solve the expression, expression splitting optimization can be enabled;
     *
     * default depth = 2 << 12 - 1
     */
    static class StackSplitImpl extends ExprEvaluator {
        ExprEvaluator front;

        StackSplitImpl(ExprEvaluator front, ExprEvaluator left) {
            this.front = front;
            this.left = left;
        }

        @Override
        public Object evaluate(EvaluatorContext context, EvaluateEnvironment evaluateEnvironment) {
            Object val = left.evaluate(context, evaluateEnvironment);
            try {
                context.value = val;
                return front.evaluate(context, evaluateEnvironment);
            } finally {
                context.value = null;
            }
        }
    }

    static class ContextValueHolderImpl extends ExprEvaluator {
        @Override
        public Object evaluate(EvaluatorContext context, EvaluateEnvironment evaluateEnvironment) {
            return context.value;
        }
    }

    // Optimize
    public ExprEvaluator optimize() {
        ExprEvaluator optimized = optimizeDepth();

        // contine call optimizeDepth util next is self
        ExprEvaluator next;
        while (optimized != (next = optimized.optimizeDepth())) {
            optimized = next;
        }

        return optimized;
    }

    // Optimize the depth on the left side, and support the super long expression
    public ExprEvaluator optimizeDepth() {
        ExprEvaluator optimizedRoot = optimizeDepth(this, 0);
        ExprEvaluator target = optimizedRoot;
        boolean optimizeFlag = target != this;
        while (optimizeFlag) {
            ExprEvaluator oldLeft = target.left;
            ExprEvaluator newLeft = oldLeft.optimizeDepth(oldLeft, 0);
            // if return is not self, continue optimize
            optimizeFlag = newLeft != oldLeft;
            // update left
            target.left = newLeft;
            // next optimize target
            target = newLeft;
        }
        return optimizedRoot;
    }

    /**
     * 优化深度
     *
     * @param target
     * @param depth
     * @return
     */
    ExprEvaluator optimizeDepth(ExprEvaluator target, int depth) {
        if (left == null) return target;
        // 每1024优化一次left
        if (++depth > OPTIMIZE_DEPTH_VALUE) {
            StackSplitImpl stackSplit = new StackSplitImpl(target, left);
            ContextValueHolderImpl valueHolder = new ContextValueHolderImpl();
            // update target left
            left = valueHolder;
            return stackSplit;
        } else {
            return left.optimizeDepth(target, depth);
        }
    }

    /**
     * 返回表达式的代码
     *
     * @return
     */
    public String code() {
        throw new UnsupportedOperationException("non compiled executor are not supported code()");
    }

}
