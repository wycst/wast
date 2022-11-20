/*
 * Copyright [2020-2022] [wangyunchao]
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

import io.github.wycst.wast.common.expression.invoker.VariableInvoker;
import io.github.wycst.wast.common.utils.ObjectUtils;
import io.github.wycst.wast.common.utils.ReflectUtils;

import java.lang.reflect.Array;
import java.util.Collection;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 常量、变量、操作执行器
 * <p> 按java语法的操作符号优先级仅供参考，值越小优先级越高
 * <p> 支持科学计数法e+n，16进制0x，8进制0n
 *
 * <p>
 * 符号   优先级
 * ()      1
 * **      9 (指数运算，平方（根），立方（根）)
 * /*%     10
 * +-      100
 * << >>   200
 * &|^     300+ &(300) < ^(301) < |(302)
 * ><==    500
 * && ||   600
 * ? :     700( ? 701, : 700)  三目运算符优先级放最低,其中:优先级高于?
 * <p>
 * 操作关键字： in , out , matches
 *
 * @Author wangyunchao
 * @Date 2022/10/20 12:37
 */
public class ExprEvaluator {

    // 执行类型
    protected int evalType;
    // 操作类型和优先级level组合使用 + - * / % **
    protected int opsType;

    // 表达式左边
    ExprEvaluator left;
    // 表达式右边
    ExprEvaluator right;
    // 优先级，参考类上注释
    int level = -1;

    // 是否负数
    boolean negate;

    // 逻辑非运算
    boolean logicalNot;

    // 是否常量
    boolean isStatic;

    // 执行结果
    Object result;

    /**
     * 优化深度
     */
    static final int Optimize_Depth_Value = 1 << 10;

    // 字符串拼接
    private static ThreadLocal<StringBuilder> localBuilder = new ThreadLocal<StringBuilder>() {
        @Override
        protected StringBuilder initialValue() {
            return new StringBuilder();
        }
    };

    protected static StringBuilder getLocalBuilder() {
        return localBuilder.get();
    }

    // use by code()
    public boolean isStaticExpr() {
        // if ':' retutn false
        if (opsType == 70) return false;
        if (left == null && right == null) {
            return this.isStatic;
        }
        if (left == null) {
            return right.isStaticExpr();
        }
        if (right == null) {
            return left.isStaticExpr();
        }
        return left.isStaticExpr() && right.isStaticExpr();
    }

    public int getEvalType() {
        return evalType;
    }

    public void setEvalType(int evalType) {
        this.evalType = evalType;
    }

    public void setOperator(int opsType, int level) {
        this.opsType = opsType;
        this.level = level;
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
                    String val = splitStr.substring(beginIndex, i).trim();
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
        strings[arrLen++] = splitStr.substring(beginIndex, length).trim();
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
        this.isStatic = true;
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
                    val = val.substring(1, val.length() - 1);
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

    public void setNegate(boolean negate) {
        this.negate = negate;
    }

    public void setLogicalNot(boolean logicalNot) {
        this.logicalNot = logicalNot;
    }

    public Object evaluate() {
        return evaluate(null, EvaluateEnvironment.DefaultEnvironment);
    }

    public Object evaluate(EvaluatorContext context, EvaluateEnvironment evaluateEnvironment) {
        if (this.isStatic) {
            // 字符串，数字，常量数组等
            return this.result;
        }
        if (evalType == 0) {
            result = left.evaluate(context, evaluateEnvironment);
            this.isStatic = left.isStatic;
            return result;
        } else if (evalType == 1) {
            Object leftValue = left.evaluate(context, evaluateEnvironment);
            if (right == null) {
                this.isStatic = left.isStatic;
                return result = leftValue;
            }
            Object rightValue = null;
            if (opsType == 71) {
                // ?运算不计算right分支(根据left的值获取right子表达式的r-left和r-right)
                Object result = right.evaluateTernary(context, evaluateEnvironment, (Boolean) leftValue, left.isStatic);
                this.isStatic = left.isStatic && right.isStatic;
                if (this.isStatic) {
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

            this.isStatic = left.isStatic && right.isStatic;
            boolean isDouble = leftValue instanceof Double || rightValue instanceof Double;

//                System.out.println(" lr " + leftValue + " " + opsType + " " + rightValue + " / " + level);
            switch (opsType) {
                case 1:
                    // *乘法
                    if (isDouble) {
                        result = ((Number) leftValue).doubleValue() * ((Number) rightValue).doubleValue();
                    } else {
                        result = ((Number) leftValue).longValue() * ((Number) rightValue).longValue();
                    }
                    return result;
                case 2:
                    // /除法
                    if (isDouble) {
                        result = ((Number) leftValue).doubleValue() / ((Number) rightValue).doubleValue();
                    } else {
                        result = ((Number) leftValue).longValue() / ((Number) rightValue).longValue();
                    }
                    return result;
                case 3:
                    // %取余数
                    if (isDouble) {
                        result = ((Number) leftValue).doubleValue() % ((Number) rightValue).doubleValue();
                    } else {
                        result = ((Number) leftValue).longValue() % ((Number) rightValue).longValue();
                    }
                    return result;
                case 4:
                    // **指数，平方/根
                    if (isDouble) {
                        result = Math.pow(((Number) leftValue).doubleValue(), ((Number) rightValue).doubleValue());
                    } else {
                        result = Math.pow(((Number) leftValue).longValue(), ((Number) rightValue).longValue());
                    }
                    return result;
                case 11:
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
                case 12:
                    // -减法（理论上代码不可达） 因为'-'统一转化为了'+'(负)
                    if (isDouble) {
                        result = ((Number) leftValue).doubleValue() - ((Number) rightValue).doubleValue();
                    } else {
                        result = ((Number) leftValue).longValue() - ((Number) rightValue).longValue();
                    }
                    return result;
                case 21:
                    // >> 位运算右移
                    return result = ((Number) leftValue).longValue() >> ((Number) rightValue).longValue();
                case 22:
                    // << 位运算左移
                    return result = ((Number) leftValue).longValue() << ((Number) rightValue).longValue();
                case 31:
                    // &
                    return result = ((Number) leftValue).longValue() & ((Number) rightValue).longValue();
                case 32:
                    // ^
                    return result = ((Number) leftValue).longValue() ^ ((Number) rightValue).longValue();
                case 33:
                    // |
                    return result = ((Number) leftValue).longValue() | ((Number) rightValue).longValue();
//                    case 43:
//                        // 代码不可达 !
//                        return result = !(rightValue == Boolean.TRUE) ;
                case 51:
                    // >
                    return result = ((Number) leftValue).doubleValue() > ((Number) rightValue).doubleValue();
                case 52:
                    // <
                    return result = ((Number) leftValue).doubleValue() < ((Number) rightValue).doubleValue();
                case 53:
                    // ==
                    if (leftValue instanceof Number && rightValue instanceof Number) {
                        return result = ((Number) leftValue).doubleValue() == ((Number) rightValue).doubleValue();
                    }
                    if (leftValue == rightValue) {
                        return result = true;
                    }
                    return result = leftValue != null && leftValue.equals(rightValue);
                case 54:
                    // >=
                    return result = ((Number) leftValue).doubleValue() >= ((Number) rightValue).doubleValue();
                case 55:
                    // <=
                    return result = ((Number) leftValue).doubleValue() <= ((Number) rightValue).doubleValue();
                case 56:
                    // !=
                    if (leftValue instanceof Number && rightValue instanceof Number) {
                        // 基础类型double直接比较值
                        return result = ((Number) leftValue).doubleValue() != ((Number) rightValue).doubleValue();
                    }
                    if (leftValue == rightValue) {
                        return result = false;
                    }
                    return result = leftValue == null || !leftValue.equals(rightValue);
                case 61:
                    // &&
                    return result = (Boolean) leftValue && (Boolean) rightValue;
                case 62:
                    // ||
                    return result = (Boolean) leftValue || (Boolean) rightValue;
                case 63:
                    /// in
                    return result = this.evaluateIn(leftValue, rightValue);
                case 64:
                    /// out
                    return result = !this.evaluateIn(leftValue, rightValue);
                case 70:
                    // : 三目运算结果, 不支持单独运算
                    throw new ExpressionException(" 不支持单独使用冒号运算符: ':'");
                case 71:
                    // ? 三目运算条件
                    return right.evaluateTernary(context, evaluateEnvironment, (Boolean) leftValue, left.isStatic);
            }
            // 默认返回null
        } else if (evalType == 5) {
            // 括号运算
            Object bracketValue = right.evaluate(context, evaluateEnvironment);
            this.isStatic = right.isStatic;
            if (negate) {
//                if (bracketValue instanceof Double) {
//                    return result = -(Double) bracketValue;
//                } else if (bracketValue instanceof Long) {
//                    return result = -(Long) bracketValue;
//                } else {
//                    return result = -((Number) bracketValue).doubleValue();
//                }
                return result = ExprUtils.getNegateNumber(bracketValue);
            }
            if (logicalNot) {
                return result = bracketValue == Boolean.FALSE || bracketValue == null;
            }
            return result = bracketValue;
        } else if (evalType == 6) {
            // 不可达
            throw new UnsupportedOperationException();
        } else if (evalType == 7) {
            // 不可达
            throw new UnsupportedOperationException();
        } else if (evalType == 9) {
            // 不可达
            throw new UnsupportedOperationException();
        } else {
            // 其他统一返回left
            result = left.evaluate(context, evaluateEnvironment);
            this.isStatic = left.isStatic;
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
        if (this.isStatic) {
            return this.result;
        }
        Object result = null;
        if (bool == null || bool == Boolean.FALSE) {
            this.isStatic = isStatic && right.isStatic;
            result = right.evaluate(context, evaluateEnvironment);
        } else {
            this.isStatic = isStatic && left.isStatic;
            result = left.evaluate(context, evaluateEnvironment);
        }
        if (this.isStatic) {
            this.result = result;
        }
        return result;
    }

    ExprEvaluator update(ExprEvaluator left, ExprEvaluator right) {
        this.left = left;
        this.right = right;
        return this;
    }

    /**
     * 变量执行器
     */
    static class ExprEvaluatorVariableImpl extends ExprEvaluator {

        // 变量访问模型
        VariableInvoker variableInvoke;

        /***
         * 设置变量值？
         *
         * @param variableInvoke
         */
        public void setVariableInvoker(VariableInvoker variableInvoke) {
            this.variableInvoke = variableInvoke;
        }

        // 内联key
        ExprEvaluator internKey() {
            variableInvoke.internKey();
            return this;
        }

        @Override
        public Object evaluate(EvaluatorContext context, EvaluateEnvironment evaluateEnvironment) {
            Object obj = null;
            try {
                obj = context.getContextValue(variableInvoke); // evaluateEnvironment.getContextValue(this.variableInvoke, context);
                if (obj == null && !variableInvoke.existKey(context)) {
                    throw new ExpressionException("Unresolved property or variable:'" + variableInvoke + "' by context");
                }
                if (evaluateEnvironment.isAutoParseStringAsDouble() && obj instanceof String) {
                    obj = Double.parseDouble((String) obj);
                }
            } catch (RuntimeException e) {
                if (obj == null && context == null) {
                    throw new ExpressionException("Unresolved property or variable:'" + variableInvoke + "' by context");
                } else {
                    throw e;
                }
            }
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
            return builder.append("_$").append(variableInvoke.getIndex()).toString();
        }
    }

    /**
     * 常量执行器(字符串,数字,布尔值,null)
     */
    static class ExprEvaluatorConstantImpl extends ExprEvaluator {

        ExprEvaluatorConstantImpl(Object result) {
            this.result = result;
            this.isStatic = true;
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
    static class ExprEvaluatorBracketImpl extends ExprEvaluator {
        static String operator = "()";

        static ExprEvaluatorBracketImpl of(ExprEvaluator evaluator) {
            ExprEvaluatorBracketImpl exprEvaluatorImpl = new ExprEvaluatorBracketImpl();
//            exprEvaluatorImpl.left = evaluator.left;
            exprEvaluatorImpl.right = evaluator.right;
            exprEvaluatorImpl.negate = evaluator.negate;
            exprEvaluatorImpl.logicalNot = evaluator.logicalNot;
            return exprEvaluatorImpl;
        }

        @Override
        public String toString() {
            return "ExprEvaluatorBracketImpl{()}";
        }

        @Override
        public Object evaluate(EvaluatorContext context, EvaluateEnvironment evaluateEnvironment) {
            Object bracketValue = right.evaluate(context, evaluateEnvironment);
            if (negate) {
                if (bracketValue instanceof Double) {
                    return -(Double) bracketValue;
                } else if (bracketValue instanceof Long) {
                    return -(Long) bracketValue;
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
    static class ExprEvaluatorPlusImpl extends ExprEvaluator {
        static String operator = "+";

        static ExprEvaluatorPlusImpl of(ExprEvaluator evaluator) {
            ExprEvaluatorPlusImpl evaluatorImpl = new ExprEvaluatorPlusImpl();
            evaluatorImpl.left = evaluator.left;
            evaluatorImpl.right = evaluator.right;
            return evaluatorImpl;
        }

        @Override
        public String toString() {
            return "ExprEvaluatorPlusImpl{+}";
        }

        @Override
        public Object evaluate(EvaluatorContext context, EvaluateEnvironment evaluateEnvironment) {
            Object leftValue = left.evaluate(context, evaluateEnvironment);
            Object rightValue = right.evaluate(context, evaluateEnvironment);
            // +加法
            if (leftValue instanceof Number && rightValue instanceof Number) {
                boolean isDouble = leftValue instanceof Double || rightValue instanceof Double;
                // 数值加法
                if (isDouble) {
                    return ((Number) leftValue).doubleValue() + ((Number) rightValue).doubleValue();
                } else {
                    return ((Number) leftValue).longValue() + ((Number) rightValue).longValue();
                }
            }
            // 字符串加法
            StringBuilder builder = getLocalBuilder();
            try {
                return builder.append(leftValue).append(rightValue).toString();
            } finally {
                builder.setLength(0);
            }
        }
    }

    /**
     * 减法执行器
     */
    static class ExprEvaluatorMinusImpl extends ExprEvaluator {
        static String operator = "-";

        static ExprEvaluatorMinusImpl of(ExprEvaluator evaluator) {
            ExprEvaluatorMinusImpl evaluatorImpl = new ExprEvaluatorMinusImpl();
            evaluatorImpl.left = evaluator.left;
            evaluatorImpl.right = evaluator.right;
            return evaluatorImpl;
        }

        @Override
        public String toString() {
            return "ExprEvaluatorMinusImpl{-}";
        }

        @Override
        public Object evaluate(EvaluatorContext context, EvaluateEnvironment evaluateEnvironment) {
            if (this.isStatic) return this.result;
            Object leftValue = left.evaluate(context, evaluateEnvironment);
            Object rightValue = right.evaluate(context, evaluateEnvironment);
            isStatic = left.isStatic && right.isStatic;
            boolean isDouble = leftValue instanceof Double || rightValue instanceof Double;
            // -减法
            if (isDouble) {
                result = ((Number) leftValue).doubleValue() - ((Number) rightValue).doubleValue();
            } else {
                result = ((Number) leftValue).longValue() - ((Number) rightValue).longValue();
            }
            return result;
        }
    }

    /**
     * 乘法执行器
     */
    static class ExprEvaluatorMultiplyImpl extends ExprEvaluator {
        static String operator = "*";

        static ExprEvaluatorMultiplyImpl of(ExprEvaluator evaluator) {
            ExprEvaluatorMultiplyImpl evaluatorImpl = new ExprEvaluatorMultiplyImpl();
            evaluatorImpl.left = evaluator.left;
            evaluatorImpl.right = evaluator.right;
            return evaluatorImpl;
        }

        @Override
        public String toString() {
            return "ExprEvaluatorMultiplyImpl{*}";
        }

        @Override
        public Object evaluate(EvaluatorContext context, EvaluateEnvironment evaluateEnvironment) {
            if (this.isStatic) return this.result;
            Object leftValue = left.evaluate(context, evaluateEnvironment);
            Object rightValue = right.evaluate(context, evaluateEnvironment);
            isStatic = left.isStatic && right.isStatic;
            boolean isDouble = leftValue instanceof Double || rightValue instanceof Double;
            // *乘法
            if (isDouble) {
                result = ((Number) leftValue).doubleValue() * ((Number) rightValue).doubleValue();
            } else {
                result = ((Number) leftValue).longValue() * ((Number) rightValue).longValue();
            }
            return result;
        }
    }

    /**
     * 指数执行器
     */
    static class ExprEvaluatorPowerImpl extends ExprEvaluator {
        static String operator = "**";

        static ExprEvaluatorPowerImpl of(ExprEvaluator evaluator) {
            ExprEvaluatorPowerImpl evaluatorImpl = new ExprEvaluatorPowerImpl();
            evaluatorImpl.left = evaluator.left;
            evaluatorImpl.right = evaluator.right;
            return evaluatorImpl;
        }

        @Override
        public String toString() {
            return "ExprEvaluatorPowerImpl{**}";
        }

        @Override
        public Object evaluate(EvaluatorContext context, EvaluateEnvironment evaluateEnvironment) {
            if (this.isStatic) return this.result;
            Object leftValue = left.evaluate(context, evaluateEnvironment);
            Object rightValue = right.evaluate(context, evaluateEnvironment);
            isStatic = left.isStatic && right.isStatic;
            // **指数，平方/根
            double powValue = Math.pow(((Number) leftValue).doubleValue(), ((Number) rightValue).doubleValue());
            long longVal = (long) powValue;
            if (longVal == powValue) {
                return result = longVal;
            } else {
                return result = powValue;
            }
        }
    }

    /**
     * 除法执行器
     */
    static class ExprEvaluatorDivisionImpl extends ExprEvaluator {
        static String operator = "/";

        static ExprEvaluatorDivisionImpl of(ExprEvaluator evaluator) {
            ExprEvaluatorDivisionImpl evaluatorImpl = new ExprEvaluatorDivisionImpl();
            evaluatorImpl.left = evaluator.left;
            evaluatorImpl.right = evaluator.right;
            return evaluatorImpl;
        }

        @Override
        public String toString() {
            return "ExprEvaluatorDivisionImpl{/}";
        }

        @Override
        public Object evaluate(EvaluatorContext context, EvaluateEnvironment evaluateEnvironment) {
            Object leftValue = left.evaluate(context, evaluateEnvironment);
            Object rightValue = right.evaluate(context, evaluateEnvironment);
            boolean isDouble = leftValue instanceof Double || rightValue instanceof Double;
            // 除法(/)
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
    static class ExprEvaluatorModulusImpl extends ExprEvaluator {
        static String operator = "%";

        static ExprEvaluatorModulusImpl of(ExprEvaluator evaluator) {
            ExprEvaluatorModulusImpl evaluatorImpl = new ExprEvaluatorModulusImpl();
            evaluatorImpl.left = evaluator.left;
            evaluatorImpl.right = evaluator.right;
            return evaluatorImpl;
        }

        @Override
        public String toString() {
            return "ExprEvaluatorModulusImpl{%}";
        }

        @Override
        public Object evaluate(EvaluatorContext context, EvaluateEnvironment evaluateEnvironment) {
            Object leftValue = left.evaluate(context, evaluateEnvironment);
            Object rightValue = right.evaluate(context, evaluateEnvironment);
            boolean isDouble = leftValue instanceof Double || rightValue instanceof Double;
            // %取余数
            if (isDouble) {
                return ((Number) leftValue).doubleValue() % ((Number) rightValue).doubleValue();
            } else {
                return ((Number) leftValue).longValue() % ((Number) rightValue).longValue();
            }
        }
    }

    /**
     * 位运算(左)执行器
     */
    static class ExprEvaluatorBitLeftImpl extends ExprEvaluator {
        static String operator = "<<";

        static ExprEvaluatorBitLeftImpl of(ExprEvaluator evaluator) {
            ExprEvaluatorBitLeftImpl evaluatorImpl = new ExprEvaluatorBitLeftImpl();
            evaluatorImpl.left = evaluator.left;
            evaluatorImpl.right = evaluator.right;
            return evaluatorImpl;
        }

        @Override
        public String toString() {
            return "ExprEvaluatorBitLeftImpl{<<}";
        }

        @Override
        public Object evaluate(EvaluatorContext context, EvaluateEnvironment evaluateEnvironment) {
            Object leftValue = left.evaluate(context, evaluateEnvironment);
            Object rightValue = right.evaluate(context, evaluateEnvironment);
            // <<
            return ((Number) leftValue).longValue() << ((Number) rightValue).longValue();
        }
    }

    /**
     * 位运算(右)执行器
     */
    static class ExprEvaluatorBitRightImpl extends ExprEvaluator {
        static String operator = ">>";

        static ExprEvaluatorBitRightImpl of(ExprEvaluator evaluator) {
            ExprEvaluatorBitRightImpl evaluatorImpl = new ExprEvaluatorBitRightImpl();
            evaluatorImpl.left = evaluator.left;
            evaluatorImpl.right = evaluator.right;
            return evaluatorImpl;
        }

        @Override
        public String toString() {
            return "ExprEvaluatorBitRightImpl{>>}";
        }

        @Override
        public Object evaluate(EvaluatorContext context, EvaluateEnvironment evaluateEnvironment) {
            Object leftValue = left.evaluate(context, evaluateEnvironment);
            Object rightValue = right.evaluate(context, evaluateEnvironment);
            // >>
            return ((Number) leftValue).longValue() >> ((Number) rightValue).longValue();
        }
    }

    /**
     * 位运算(与)执行器
     */
    static class ExprEvaluatorBitAndImpl extends ExprEvaluator {
        static String operator = "&";

        static ExprEvaluatorBitAndImpl of(ExprEvaluator evaluator) {
            ExprEvaluatorBitAndImpl evaluatorImpl = new ExprEvaluatorBitAndImpl();
            evaluatorImpl.left = evaluator.left;
            evaluatorImpl.right = evaluator.right;
            return evaluatorImpl;
        }

        @Override
        public String toString() {
            return "ExprEvaluatorBitAndImpl{&}";
        }

        @Override
        public Object evaluate(EvaluatorContext context, EvaluateEnvironment evaluateEnvironment) {
            Object leftValue = left.evaluate(context, evaluateEnvironment);
            Object rightValue = right.evaluate(context, evaluateEnvironment);
            // &位运算与
            return ((Number) leftValue).longValue() & ((Number) rightValue).longValue();
        }
    }

    /**
     * 位运算(或)执行器
     */
    static class ExprEvaluatorBitOrImpl extends ExprEvaluator {
        static String operator = "|";

        static ExprEvaluatorBitOrImpl of(ExprEvaluator evaluator) {
            ExprEvaluatorBitOrImpl evaluatorImpl = new ExprEvaluatorBitOrImpl();
            evaluatorImpl.left = evaluator.left;
            evaluatorImpl.right = evaluator.right;
            return evaluatorImpl;
        }

        @Override
        public String toString() {
            return "ExprEvaluatorBitOrImpl{|}";
        }

        @Override
        public Object evaluate(EvaluatorContext context, EvaluateEnvironment evaluateEnvironment) {
            Object leftValue = left.evaluate(context, evaluateEnvironment);
            Object rightValue = right.evaluate(context, evaluateEnvironment);
            // |位运算或
            return ((Number) leftValue).longValue() | ((Number) rightValue).longValue();
        }
    }

    /**
     * 位运算(异或)执行器
     */
    static class ExprEvaluatorBitXorImpl extends ExprEvaluator {
        static String operator = "^";

        static ExprEvaluatorBitXorImpl of(ExprEvaluator evaluator) {
            ExprEvaluatorBitXorImpl evaluatorImpl = new ExprEvaluatorBitXorImpl();
            evaluatorImpl.left = evaluator.left;
            evaluatorImpl.right = evaluator.right;
            return evaluatorImpl;
        }

        @Override
        public String toString() {
            return "ExprEvaluatorBitOrImpl{^}";
        }

        @Override
        public Object evaluate(EvaluatorContext context, EvaluateEnvironment evaluateEnvironment) {
            Object leftValue = left.evaluate(context, evaluateEnvironment);
            Object rightValue = right.evaluate(context, evaluateEnvironment);
            // ^ 异或运算
            return ((Number) leftValue).longValue() ^ ((Number) rightValue).longValue();
        }
    }

    /**
     * 等于执行器
     */
    static class ExprEvaluatorEqualImpl extends ExprEvaluator {
        static String operator = "==";

        static ExprEvaluatorEqualImpl of(ExprEvaluator evaluator) {
            ExprEvaluatorEqualImpl evaluatorImpl = new ExprEvaluatorEqualImpl();
            evaluatorImpl.left = evaluator.left;
            evaluatorImpl.right = evaluator.right;
            return evaluatorImpl;
        }

        @Override
        public String toString() {
            return "ExprEvaluatorEqualImpl{==}";
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

    /**
     * 大于执行器
     */
    static class ExprEvaluatorGtImpl extends ExprEvaluator {
        static String operator = ">";

        static ExprEvaluatorGtImpl of(ExprEvaluator evaluator) {
            ExprEvaluatorGtImpl evaluatorImpl = new ExprEvaluatorGtImpl();
            evaluatorImpl.left = evaluator.left;
            evaluatorImpl.right = evaluator.right;
            return evaluatorImpl;
        }

        @Override
        public String toString() {
            return "ExprEvaluatorGtImpl{>}";
        }

        @Override
        public Object evaluate(EvaluatorContext context, EvaluateEnvironment evaluateEnvironment) {
            Object leftValue = left.evaluate(context, evaluateEnvironment);
            Object rightValue = right.evaluate(context, evaluateEnvironment);
            return ((Number) leftValue).doubleValue() > ((Number) rightValue).doubleValue();
        }
    }

    /**
     * 小于执行器
     */
    static class ExprEvaluatorLtImpl extends ExprEvaluator {
        static String operator = "<";

        static ExprEvaluatorLtImpl of(ExprEvaluator evaluator) {
            ExprEvaluatorLtImpl evaluatorImpl = new ExprEvaluatorLtImpl();
            evaluatorImpl.left = evaluator.left;
            evaluatorImpl.right = evaluator.right;
            return evaluatorImpl;
        }

        @Override
        public String toString() {
            return "ExprEvaluatorLtImpl{<}";
        }

        @Override
        public Object evaluate(EvaluatorContext context, EvaluateEnvironment evaluateEnvironment) {
            Object leftValue = left.evaluate(context, evaluateEnvironment);
            Object rightValue = right.evaluate(context, evaluateEnvironment);
            return ((Number) leftValue).doubleValue() < ((Number) rightValue).doubleValue();
        }
    }

    /**
     * 大于等于执行器
     */
    static class ExprEvaluatorGEImpl extends ExprEvaluator {
        static String operator = ">=";

        static ExprEvaluatorGEImpl of(ExprEvaluator evaluator) {
            ExprEvaluatorGEImpl evaluatorImpl = new ExprEvaluatorGEImpl();
            evaluatorImpl.left = evaluator.left;
            evaluatorImpl.right = evaluator.right;
            return evaluatorImpl;
        }

        @Override
        public String toString() {
            return "ExprEvaluatorGEImpl{>=}";
        }

        @Override
        public Object evaluate(EvaluatorContext context, EvaluateEnvironment evaluateEnvironment) {
            Object leftValue = left.evaluate(context, evaluateEnvironment);
            Object rightValue = right.evaluate(context, evaluateEnvironment);
            return ((Number) leftValue).doubleValue() >= ((Number) rightValue).doubleValue();
        }
    }

    /**
     * 小于等于执行器
     */
    static class ExprEvaluatorLEImpl extends ExprEvaluator {
        static String operator = "<=";

        static ExprEvaluatorLEImpl of(ExprEvaluator evaluator) {
            ExprEvaluatorLEImpl evaluatorImpl = new ExprEvaluatorLEImpl();
            evaluatorImpl.left = evaluator.left;
            evaluatorImpl.right = evaluator.right;
            return evaluatorImpl;
        }

        @Override
        public String toString() {
            return "ExprEvaluatorLEImpl{<=}";
        }

        @Override
        public Object evaluate(EvaluatorContext context, EvaluateEnvironment evaluateEnvironment) {
            Object leftValue = left.evaluate(context, evaluateEnvironment);
            Object rightValue = right.evaluate(context, evaluateEnvironment);
            return ((Number) leftValue).doubleValue() <= ((Number) rightValue).doubleValue();
        }
    }

    /**
     * 不等于执行器
     */
    static class ExprEvaluatorNEImpl extends ExprEvaluator {
        static String operator = "!=";

        static ExprEvaluatorNEImpl of(ExprEvaluator evaluator) {
            ExprEvaluatorNEImpl evaluatorImpl = new ExprEvaluatorNEImpl();
            evaluatorImpl.left = evaluator.left;
            evaluatorImpl.right = evaluator.right;
            return evaluatorImpl;
        }

        @Override
        public String toString() {
            return "ExprEvaluatorNEImpl{!=}";
        }

        @Override
        public Object evaluate(EvaluatorContext context, EvaluateEnvironment evaluateEnvironment) {
            Object leftValue = left.evaluate(context, evaluateEnvironment);
            Object rightValue = right.evaluate(context, evaluateEnvironment);
            if (leftValue instanceof Number && rightValue instanceof Number) {
                // 基础类型double直接比较值
                return ((Number) leftValue).doubleValue() != ((Number) rightValue).doubleValue();
            }
            if (leftValue == rightValue) {
                return false;
            }
            return leftValue == null || !leftValue.equals(rightValue);
        }
    }

    /**
     * 逻辑与执行器
     */
    static class ExprEvaluatorLogicalAndImpl extends ExprEvaluator {
        static String operator = "&&";

        static ExprEvaluatorLogicalAndImpl of(ExprEvaluator evaluator) {
            ExprEvaluatorLogicalAndImpl evaluatorImpl = new ExprEvaluatorLogicalAndImpl();
            evaluatorImpl.left = evaluator.left;
            evaluatorImpl.right = evaluator.right;
            return evaluatorImpl;
        }

        @Override
        public String toString() {
            return "ExprEvaluatorLogicalAndImpl{&&}";
        }

        @Override
        public Object evaluate(EvaluatorContext context, EvaluateEnvironment evaluateEnvironment) {
            Object leftValue = left.evaluate(context, evaluateEnvironment);
            Object rightValue = right.evaluate(context, evaluateEnvironment);
            // &&
            return (Boolean) leftValue && (Boolean) rightValue;
        }
    }

    /**
     * 逻辑或执行器
     */
    static class ExprEvaluatorLogicalOrImpl extends ExprEvaluator {
        static String operator = "||";

        static ExprEvaluatorLogicalOrImpl of(ExprEvaluator evaluator) {
            ExprEvaluatorLogicalOrImpl evaluatorImpl = new ExprEvaluatorLogicalOrImpl();
            evaluatorImpl.left = evaluator.left;
            evaluatorImpl.right = evaluator.right;
            return evaluatorImpl;
        }

        @Override
        public String toString() {
            return "ExprEvaluatorLogicalOrImpl{||}";
        }

        @Override
        public Object evaluate(EvaluatorContext context, EvaluateEnvironment evaluateEnvironment) {
            Object leftValue = left.evaluate(context, evaluateEnvironment);
            Object rightValue = right.evaluate(context, evaluateEnvironment);
            // ||
            return (Boolean) leftValue || (Boolean) rightValue;
        }
    }

    /**
     * 三目运算执行器
     */
    static class ExprEvaluatorTernaryImpl extends ExprEvaluator {
        static String operator = "?:";

        private ExprEvaluator condition;
        private ExprEvaluator question;
        private ExprEvaluator colon;

        static ExprEvaluatorTernaryImpl of(ExprEvaluator evaluator) {
            ExprEvaluatorTernaryImpl evaluatorImpl = new ExprEvaluatorTernaryImpl();
            evaluatorImpl.condition = evaluator.left;
            evaluatorImpl.question = evaluator.right.left;
            evaluatorImpl.colon = evaluator.right.right;
            return evaluatorImpl;
        }

        @Override
        public String toString() {
            return "ExprEvaluatorTernaryImpl{?:}";
        }

        @Override
        public Object evaluate(EvaluatorContext context, EvaluateEnvironment evaluateEnvironment) {
            Object leftValue = condition.evaluate(context, evaluateEnvironment);
            boolean conditionResult = leftValue == null ? false : (Boolean) leftValue;
            Object result = conditionResult ? question.evaluate(context, evaluateEnvironment) : colon.evaluate(context, evaluateEnvironment);/*right.evaluateTernary(context, evaluateEnvironment, (Boolean) leftValue, left.isStatic)*/
            return result;
        }
    }

    /**
     * in运算执行器
     */
    static class ExprEvaluatorInImpl extends ExprEvaluator {
        static String operator = "in";

        static ExprEvaluatorInImpl of(ExprEvaluator evaluator) {
            ExprEvaluatorInImpl evaluatorImpl = new ExprEvaluatorInImpl();
            evaluatorImpl.left = evaluator.left;
            evaluatorImpl.right = evaluator.right;
            return evaluatorImpl;
        }

        @Override
        public String toString() {
            return "ExprEvaluatorInImpl{in}";
        }

        @Override
        public Object evaluate(EvaluatorContext context, EvaluateEnvironment evaluateEnvironment) {
            Object leftValue = left.evaluate(context, evaluateEnvironment);
            Object rightValue = right.evaluate(context, evaluateEnvironment);
            // in
            return this.evaluateIn(leftValue, rightValue);
        }
    }

    /**
     * out运算执行器
     */
    static class ExprEvaluatorOutImpl extends ExprEvaluator {
        static String operator = "out";

        static ExprEvaluatorOutImpl of(ExprEvaluator evaluator) {
            ExprEvaluatorOutImpl evaluatorImpl = new ExprEvaluatorOutImpl();
            evaluatorImpl.left = evaluator.left;
            evaluatorImpl.right = evaluator.right;
            return evaluatorImpl;
        }

        @Override
        public String toString() {
            return "ExprEvaluatorOutImpl{out}";
        }

        @Override
        public Object evaluate(EvaluatorContext context, EvaluateEnvironment evaluateEnvironment) {
            Object leftValue = left.evaluate(context, evaluateEnvironment);
            Object rightValue = right.evaluate(context, evaluateEnvironment);
            // out
            return !this.evaluateIn(leftValue, rightValue);
        }
    }

    /**
     * 静态函数运算执行器
     */
    static class ExprEvaluatorFunctionImpl extends ExprEvaluator {
        static String operator = "@function";

        // 函数名称
        String functionName;
        // 参数表达式数组
        String[] paramExprs;
        // 参数数组长度
        int paramLength;
        // 参数表达式数组
        ExprParser[] paramExprParsers;

        @Override
        public String toString() {
            return "ExprEvaluatorFunctionImpl{@function}";
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

            this.functionName = funName.trim();
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
    static class ExprEvaluatorMethodImpl extends ExprEvaluatorFunctionImpl {
        static String operator = "@method";
        VariableInvoker variableInvoker;

        @Override
        public String toString() {
            return "ExprEvaluatorMethodImpl{@method}";
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
        public void setMethod(VariableInvoker variableInvoker, String methodName, String params, ExprParser global) {
            this.variableInvoker = variableInvoker;
            setFunction(methodName, params, global);
        }

        // 执行函数表达式
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

    // value暂存对象
    private static ThreadLocal<Object> valueHolder = new ThreadLocal<Object>();

    /**
     * 栈拆分优化器，解决栈深度溢出问题;
     * 一般派不上用场，当原生编译器都解决不了表达式时，可启用表达式的拆分优化；
     * default depth = 2 << 12 - 1
     */
    static class ExprEvaluatorStackSplitImpl extends ExprEvaluator {
        ExprEvaluator front;

        ExprEvaluatorStackSplitImpl(ExprEvaluator front, ExprEvaluator left) {
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

    static class ExprEvaluatorContextValueHolderImpl extends ExprEvaluator {
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
        if(left == null) return target;
        // 每1024优化一次left
        if(++depth > Optimize_Depth_Value) {
            ExprEvaluatorStackSplitImpl stackSplit = new ExprEvaluatorStackSplitImpl(target, left);
            ExprEvaluatorContextValueHolderImpl valueHolder = new ExprEvaluatorContextValueHolderImpl();
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
