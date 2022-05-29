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
package org.framework.light.common.expression;

import org.framework.light.common.utils.ObjectUtils;

import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.util.Collection;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * <p> 按java语法的操作符号优先级仅供参考，值越小优先级越高
 * <p> 支持科学计数法e+n，16进制0x，8进制0n
 *
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
 * @author wangyunchao
 */
public class ExprParser extends Expression {

    protected ExprParser(String exprSource) {
        this.init(exprSource);
        this.parse();
    }

    ExprParser(char[] buffers, int offset, int count) {
        this.init(buffers, offset, count);
        this.parse();
    }

    // 空解析器
    protected ExprParser() {
    }

    public static final int GROUP_TOKEN_OPS = 1;
    public static final int GROUP_TOKEN_VALUE = 2;

    public static final int RESET_TOKEN = 0;
    public static final int OPS_TOKEN = 1;
    public static final int VAR_TOKEN = 2;
    public static final int NUM_TOKEN = 3;
    public static final int BRACKET_TOKEN = 4;
    public static final int BRACKET_END_TOKEN = 6;

    // 字符串
    public static final int STR_TOKEN = 7;
    // 数组token
    public static final int ARR_TOKEN = 8;
    // 函数token，以@开始并且紧跟java标识符（@fun(...arg0)或者@bean.fun(...arg0)）
    public static final int FUN_TOKEN = 9;
    public static final int NEGATE_TOKEN = 10;

    // 表达式源
    private String exprSource;
    private char[] sourceChars;
    private int offset;
    private int count;
    private int readIndex;
    private StringBuilder tokenBuffer = new StringBuilder();
    // 标记类型
    private int prevTokenType;
    private int tokenType;
    private int numberRadix = 10;

    // 操作符类型
    private int opsType;
    // 操作符优先级
    private int opsLevel;

    // 括号计数>0, 解析过程中如果小于0为异常，最终为0
    private int bracketCount;

    // 表达式解析器
    private ExprEvaluator exprEvaluator = createExprEvaluator();

    protected ExprEvaluator createExprEvaluator() {
        return new ExprEvaluator();
    }

    protected ExprEvaluator getEvaluator() {
        return exprEvaluator;
    }

    protected String getSource() {
        return this.exprSource;
    }

    public class ExprEvaluator {

        // 执行类型
        private int evalType;
        // 操作符号
        private String ops;
        // 操作类型和优先级level组合使用 + - * / % **
        private int opsType;

        // 表达式左边
        private ExprEvaluator left;
        // 表达式右边
        private ExprEvaluator right;
        // 优先级，参考类上注释
        private int level = -1;

        // 值或者变量名称
        private String value;

        // 是否负数
        private boolean negate;

        // 是否常量
        private boolean isStatic;
        // 执行结果
        private Object result;

        // 函数名称
        private String functionName;
        // 参数表达式数组
        private String[] paramExprs;
        // 参数数组长度
        private int paramLength;
        // 参数表达式解释数组
        private Expression[] paramExpressions;

        public boolean isStaticExpr() {
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

        public String getValue() {
            return value;
        }

        public int getEvalType() {
            return evalType;
        }

        public void setEvalType(int evalType) {
            this.evalType = evalType;
        }

        public String getOps() {
            return ops;
        }

        public void setOps(String ops, int opsType, int level) {
//            this.ops = ops;
            this.opsType = opsType;
            this.level = level;
        }

        public int getOpsType() {
            return opsType;
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


        /***
         * 以10进制解析double数
         * 一般与Double.parseDouble(String)的结果一致（a.b{n}当n的值不大于15时）
         * 当小数位数n超过15位（double精度）时有几率和Double.parseDouble(String)的结果存在误差
         *
         * @author wangyunchao
         * @see Double#parseDouble(String)
         */
        protected Number parseNumber(CharSequence chars, int radix, boolean useNegate) {
            int i = 0, endIndex = chars.length();
            double value = 0;
            int decimalCount = 0;
            char ch = chars.charAt(0);
            boolean negative = false;
            // 负数
            if (ch == '-') {
                // 检查第一个字符是否为符号位
                negative = true;
                i++;
            }
            // 模式
            int mode = 0;
            // 指数值（以radix为底数）
            int expValue = 0;
            boolean expNegative = false;
            for (; i < endIndex; i++) {
                ch = chars.charAt(i);
                if (radix == 10) {
                    if (ch == '.') {
                        if (mode != 0) {
                            throw new NumberFormatException("For input string: \"" + chars + "\"");
                        }
                        // 小数点模式
                        mode = 1;
                        if (++i < endIndex) {
                            ch = chars.charAt(i);
                        }
                    } else if (ch == 'E' || ch == 'e') {
                        if (mode == 2) {
                            throw new NumberFormatException("For input string: \"" + chars + "\"");
                        }
                        // 科学计数法
                        mode = 2;
                        if (++i < endIndex) {
                            ch = chars.charAt(i);
                        }
                        if (ch == '-') {
                            expNegative = true;
                            if (++i < endIndex) {
                                ch = chars.charAt(i);
                            }
                        }
                    }
                }

                int digit = Character.digit(ch, radix);
                if (digit == -1) {
                    if ((ch != 'd' && ch != 'D') || i < endIndex - 1) {
                        throw new NumberFormatException("For input string: \"" + chars + "\"");
                    }
                }
                switch (mode) {
                    case 0:
                        value *= radix;
                        value += digit;
                        break;
                    case 1:
                        value *= radix;
                        value += digit;
                        decimalCount++;
                        break;
                    case 2:
                        expValue *= 10;
                        expValue += digit;
                        break;
                }
            }

            boolean useDouble = decimalCount > 0 || expValue != 0;

            expValue = expNegative ? -expValue - decimalCount : expValue - decimalCount;
            if (expValue > 0) {
                double powValue = Math.pow(radix, expValue);
                value *= powValue;
            } else {
                if (expValue < 0) {
                    double powValue = Math.pow(radix, -expValue);
                    value /= powValue;
                }
            }

            double numberVal = negative ? -value : value;
            numberVal = useNegate ? -numberVal : numberVal;
            if (!useDouble) {
                if(numberVal >= Long.MIN_VALUE && numberVal <= Long.MAX_VALUE) {
                    return (long) numberVal;
                }
            }
            return numberVal;
        }

        /***
         * 设置number值(double/long)
         *
         * @param value
         */
        public void setNumberValue(CharSequence value, int numberRadix) {
//            String stringVal = value.toString();
//            if(stringVal.indexOf('.') > 0) {
//                double val = Double.parseDouble(stringVal);
//                this.result = negate ? -val : val;
//            } else {
//                long val = Long.parseLong(stringVal);
//                this.result = negate ? -val : val;
//            }
            this.result = parseNumber(value, numberRadix, negate);
            this.isStatic = true;
        }

        /***
         * 设置变量值？
         *
         * @param value
         */
        public void setVarValue(String value) {
            // 处理可能为常量的3个变量标识符： true / false / null
            if ("true".equals(value)) {
                this.isStatic = true;
                this.result = true;
            } else if ("false".equals(value)) {
                this.isStatic = true;
                this.result = false;
            } else if ("null".equals(value)) {
                this.isStatic = true;
                this.result = null;
            } else {
                // 变量
                this.value = value;
            }
        }

        /**
         * 设置字符串常量
         *
         * @param value
         */
        public void setStrValue(String value) {
            this.isStatic = true;
            this.result = value;
        }

        private String[] parseStringArr(String splitStr, AtomicInteger atomicInteger, AtomicBoolean strArr, AtomicBoolean doubleArr) {
            int length = splitStr.length();
            boolean isStrArr = false, isDoubleArr = false;
            String[] strings = new String[10];
            int beginIndex = 0;
            int arrLen = 0;
            int bracketCount = 0;
            int bigBracketCount = 0;
            for (int i = 0; i < length; i++) {
                char ch = splitStr.charAt(i);
                if (!isStrArr && (ch == '"' || ch == '\'')) {
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
                    } else if (val.startsWith("\"") && val.endsWith("\"")) {
                        val = val.substring(1, val.length() - 1);
                    } else {
                        throw new ExpressionException("无效的数组元素: '" + val + "', 数组片段： '" + splitStr + "'");
                    }
                    result[i] = val;
                } else {
                    if (isDoubleArr) {
                        result[i] = Double.parseDouble(val);
                    } else {
                        result[i] = Long.parseLong(val);
                    }
                }
            }
            this.result = result;
        }

        // 以@作为标记
        public void setFunction(String functionToken) {
            int atIndex = functionToken.indexOf('@');
            // 左边的为句柄，右边为参数以逗号分解
            String funName = functionToken.substring(0, atIndex);
            // 参数列表
            String params = functionToken.substring(atIndex + 1).trim();
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

            Expression[] paramExpressions = new Expression[paramLength];
            for (int i = 0; i < paramLength; i++) {
                paramExpressions[i] = Expression.parse(paramExprs[i]);
            }
            this.paramExpressions = paramExpressions;
        }

        public boolean isNegate() {
            return negate;
        }

        public void setNegate(boolean negate) {
            this.negate = negate;
        }

        public int getLevel() {
            return level;
        }

        public void setLevel(int level) {
            this.level = level;
        }

        public Object evaluate() {
            return evaluate(null);
        }

        public Object evaluate(EvaluateEnvironment evaluateEnvironment) {
            if (this.isStatic) {
                // 字符串，数字，常量数组等
                return this.result;
            }
            if (evalType == 1) {
                Object leftValue = left.evaluate(evaluateEnvironment);
                if (right == null) {
                    this.isStatic = left.isStatic;
                    return result = leftValue;
                }
                Object rightValue = null;
                if (opsType == 71) {
                    // ?运算不计算right分支
                    Object result = right.evaluateTernary(evaluateEnvironment, (Boolean) leftValue, left.isStatic);
                    this.isStatic = left.isStatic && right.isStatic;
                    if (this.isStatic) {
                        this.result = result;
                    }
                    return result;
                } else {
                    rightValue = right.evaluate(evaluateEnvironment);
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
                        return result = ((Number) leftValue).doubleValue() - ((Number) rightValue).doubleValue();
                    case 21:
                        // >> 位运算右移
                        return result = ((Number) leftValue).longValue() >> ((Number) rightValue).longValue();
                    case 22:
                        // << 位运算右移
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
                        return result = leftValue == Boolean.TRUE && rightValue == Boolean.TRUE;
                    case 62:
                        // ||
                        return result = leftValue == Boolean.TRUE || rightValue == Boolean.TRUE;
                    case 63:
                        /// in
                        return this.evaluateIn(leftValue, rightValue);
                    case 64:
                        /// out
                        return !this.evaluateIn(leftValue, rightValue);
                    case 70:
                        // : 三目运算结果, 不支持单独运算
                        throw new ExpressionException(" 不支持单独使用冒号运算符: ':'");
                    case 71:
                        // ? 三目运算条件
                        return right.evaluateTernary(evaluateEnvironment, (Boolean) leftValue, left.isStatic);
                }
                // 默认返回null
            } else if (evalType == 5) {
                // 括号运算
                Object bracketValue = right.evaluate(evaluateEnvironment);
                this.isStatic = right.isStatic;
                if(negate) {
                    if(bracketValue instanceof Double) {
                        return result = -(Double) bracketValue;
                    } else if(bracketValue instanceof Long){
                        return result = -(Long) bracketValue;
                    } else {
                        return result = -((Number) bracketValue).doubleValue();
                    }
                }
                return result = bracketValue;
            } else if (evalType == 6) {

                // 变量运算
                if (evaluateEnvironment == null || evaluateEnvironment.isEmptyContext()) {
                    throw new ExpressionException("无法解析的变量： '" + value + "', 上下文对象为null");
                }
                Object context = evaluateEnvironment.getEvaluateContext();
                Object obj = ObjectUtils.get(context, value);
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
                return obj;
            } else if (evalType == 7) {
                // 字符串常量， 理论上不可达
                return this.result;
            } else if (evalType == 9) {
                // 执行函数
                return this.evaluateFunction(evaluateEnvironment);
            } else {
                // 其他统一返回left
                result = left.evaluate(evaluateEnvironment);
                this.isStatic = left.isStatic;
                return result;
            }
            return null;
        }

        // 执行函数表达式
        private Object evaluateFunction(EvaluateEnvironment evaluateEnvironment) {
            // 函数参数
            Object[] params = new Object[paramLength];
            for (int i = 0; i < paramLength; i++) {
                params[i] = paramExpressions[i].evaluate(evaluateEnvironment);
            }
            ExprFunction exprFunction = evaluateEnvironment.getFunction(this.functionName);
            if (exprFunction == null) {
                throw new ExpressionException("function '" + functionName + "' is not register !");
            }
            return exprFunction.call(params);
        }

        /**
         * in表达式
         */
        private boolean evaluateIn(Object leftValue, Object rightValue) {
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
        private Object evaluateTernary(EvaluateEnvironment evaluateEnvironment, Boolean bool, boolean isStatic) {
            if (this.isStatic) {
                return this.result;
            }
            Object result = null;
            if (bool == null || bool == Boolean.FALSE) {
                this.isStatic = isStatic && right.isStatic;
                result = right.evaluate(evaluateEnvironment);
            } else {
                this.isStatic = isStatic && left.isStatic;
                result = left.evaluate(evaluateEnvironment);
            }
            if (this.isStatic) {
                this.result = result;
            }
            return result;
        }

    }

    public int length() {
        return count;
    }

    private void init(String exprSource) {
        this.exprSource = exprSource;
        this.count = exprSource.length();
        this.sourceChars = getChars(exprSource);
    }

    private void init(char[] buffers, int offset, int count) {
        this.sourceChars = buffers;
        this.offset = offset;
        this.count = count;
    }

    private static Field stringToChars;

    static {
        try {
            stringToChars = String.class.getDeclaredField("value");
            stringToChars.setAccessible(true);
        } catch (NoSuchFieldException e) {
        }
    }

    private static char[] getChars(String source) {
        if (source == null) return null;
        try {
            return (char[]) stringToChars.get(source);
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * 解析表达式字符串
     */
    protected void parse() {
        // 解析
        parseNext(exprEvaluator, false);
        // 置换优先级
        displacement(exprEvaluator);
        // 合并
        merge();
    }

    private void merge() {
    }


    // 优先级置换
    private void displacement(ExprEvaluator exprEvaluator) {
        // 可置换优先级多次
        displacementSplit(exprEvaluator);
        displacementSplit(exprEvaluator);
    }

    private void displacementSplit(ExprEvaluator exprEvaluator) {
        if (exprEvaluator == null) {
            return;
        }
        String ops = exprEvaluator.getOps();
        int opsType = exprEvaluator.getOpsType();
        int level = exprEvaluator.getLevel();

        int evalType = exprEvaluator.getEvalType();
        ExprEvaluator left = exprEvaluator.getLeft();

        ExprEvaluator right = exprEvaluator.getRight();
        if (right == null) {
            return;
        }
        int rLevel = right.getLevel();
        if (rLevel <= 0) {
            return;
        }
        if (level > rLevel) {
            // 发现了右侧的优先级更高，触发右侧合并
            mergeRight(right, level);
            rLevel = right.getLevel();
        }

        String rOps = right.getOps();
        int rEvalType = right.getEvalType();
        ExprEvaluator rLeft = right.getLeft();
        ExprEvaluator rRight = right.getRight();

        if (level <= rLevel) {
            // parse后表达式从右至左
            // 如果当前的优先级级别高（level越小）就开启轮换
            ExprEvaluator newLeft = createExprEvaluator();
            newLeft.setEvalType(evalType);
            newLeft.setOps(ops, opsType, level);
            newLeft.setNegate(exprEvaluator.isNegate());
            newLeft.setLeft(left);
            newLeft.setRight(rLeft);

            exprEvaluator.setOps(rOps, right.getOpsType(), rLevel);
            exprEvaluator.setNegate(right.isNegate());
            exprEvaluator.setEvalType(rEvalType);
            exprEvaluator.setLeft(newLeft);
            exprEvaluator.setRight(rRight);

            // 继续合并
            displacementSplit(exprEvaluator);
        } else {
            if (rLevel == 1) {
                // 如果是括号子表达式开始子轮换
                displacement(right.getRight().getLeft());
            }
            // 继续右侧轮换，如果遇到与指定level一致的就终止
            // 后续实现，暂时统一将-转换为+处理二级优先级问题
            // 是否使用while循环代替递归？
            displacementSplit(right);
        }
    }

    private void mergeRight(ExprEvaluator exprEvaluator, int targetLevel) {
        if (exprEvaluator == null) {
            return;
        }
        String ops = exprEvaluator.getOps();
        int evalType = exprEvaluator.getEvalType();
        int level = exprEvaluator.getLevel();

        if (level >= targetLevel) {
            // 停止合并
            return;
        }

        ExprEvaluator left = exprEvaluator.getLeft();
        ExprEvaluator right = exprEvaluator.getRight();
        if (right == null) {
            return;
        }
        int rLevel = right.getLevel();
        if (rLevel <= 0) {
            return;
        }

        String rOps = right.getOps();
        int rEvalType = right.getEvalType();
        ExprEvaluator rLeft = right.getLeft();
        ExprEvaluator rRight = right.getRight();

        if (level <= rLevel) {
            // parse后表达式从右至左
            // 如果当前的优先级级别高（level越小）就开启轮换
            ExprEvaluator newLeft = createExprEvaluator();
            newLeft.setEvalType(evalType);
            newLeft.setOps(ops, exprEvaluator.getOpsType(), exprEvaluator.getLevel());
            newLeft.setNegate(exprEvaluator.isNegate());
            newLeft.setLeft(left);
            newLeft.setRight(rLeft);

            exprEvaluator.setOps(rOps, right.getOpsType(), right.getLevel());
            exprEvaluator.setNegate(right.isNegate());
            exprEvaluator.setEvalType(rEvalType);
            exprEvaluator.setLeft(newLeft);
            exprEvaluator.setRight(rRight);

            // 继续合并
            mergeRight(exprEvaluator, targetLevel);
        }
    }

    private void parseNext(ExprEvaluator evaluator, boolean negate) {
        // 获取下一个token
        this.parseNextToken();
        if (this.tokenType == RESET_TOKEN) {
            return;
        }
        if (this.tokenType == VAR_TOKEN) {
            // 变量
            ExprEvaluator left = createExprEvaluator();
            left.setEvalType(6);
            left.setNegate(negate);
            left.setVarValue(this.tokenBuffer.toString());
            evaluator.setLeft(left);
            // 继续查找token直到终止
            parseNext(evaluator, false);
        } else if (this.tokenType == STR_TOKEN) {
            // 字符串常量
            ExprEvaluator left = createExprEvaluator();
            left.setEvalType(7);
            left.setNegate(negate);
            left.setStrValue(this.tokenBuffer.toString());
            evaluator.setLeft(left);
            // 继续查找token直到终止
            parseNext(evaluator, false);
        } else if (this.tokenType == NUM_TOKEN) {
            // 常量数值赋值
            ExprEvaluator left = createExprEvaluator();
            left.setEvalType(3);
            left.setNegate(negate);
            left.setNumberValue(this.tokenBuffer, this.numberRadix);

            evaluator.setLeft(left);
            // 继续查找token直到终止
            parseNext(evaluator, false);
        } else if (tokenType == ARR_TOKEN) {
            // 静态数组
            ExprEvaluator left = createExprEvaluator();
            left.setEvalType(8);
            left.setNegate(negate);
            left.setArrayValue(this.tokenBuffer.substring(1, this.tokenBuffer.length() - 1));
            evaluator.setLeft(left);
            // 继续查找token直到终止
            parseNext(evaluator, false);
        } else if (tokenType == FUN_TOKEN) {
            // 函数token
            // 静态数组
            ExprEvaluator left = createExprEvaluator();
            left.setEvalType(9);
            left.setNegate(negate);
            left.setFunction(this.tokenBuffer.toString());
            evaluator.setLeft(left);
            // 继续查找token直到终止
            parseNext(evaluator, false);

        } else if (tokenType == BRACKET_TOKEN) {
//            // 括号
//            char tokenChar = this.tokenBuffer.charAt(0);
//            if (tokenChar == '(') {
//                // 构建子表达式作为当前表达式evaluator的right
//                // 遇到)结束
//                evaluator.setOps(/*this.tokenBuffer.toString()*/ null, this.opsType, this.opsLevel);
//                evaluator.setNegate(negate);
//                ExprEvaluator child = createExprEvaluator();
//                parseNext(child, false);
//
//                // 解析类型如果没有结束括号说明表达式错误
//                if (tokenType != BRACKET_END_TOKEN) {
//                    throw new ExpressionException("表达式解析失败，位置： " + this.readIndex + "，缺少结束符号')'");
//                }
//                //  置换子表达式优先级
//                displacementSplit(child);
//                ExprEvaluator right = createExprEvaluator();
//                evaluator.setEvalType(5);
//                evaluator.setRight(right);
//
//                right.setLeft(child);
//                // 继续解析
//                if (readable()) {
//                    parseNext(right, false);
//                }
//            } else {
//                // 结束）
//                this.tokenType = BRACKET_END_TOKEN;
//                return;
//            }
            // 构建子表达式作为当前表达式evaluator的right
            // 遇到)结束
            evaluator.setOps(/*this.tokenBuffer.toString()*/ null, this.opsType, this.opsLevel);
            evaluator.setNegate(negate);
            ExprEvaluator child = createExprEvaluator();
            parseNext(child, false);

            // 解析类型如果没有结束括号说明表达式错误
            if (tokenType != BRACKET_END_TOKEN) {
                throw new ExpressionException("表达式解析失败，位置： " + this.readIndex + "，缺少结束符号')'");
            }
            //  置换子表达式优先级
            displacementSplit(child);
            ExprEvaluator right = createExprEvaluator();
            evaluator.setEvalType(5);
            evaluator.setRight(right);

            right.setLeft(child);
            // 继续解析
            if (readable()) {
                parseNext(right, false);
            }
        } else if (this.tokenType == OPS_TOKEN) {
            evaluator.setEvalType(1);
//            String token = this.tokenBuffer.toString();
            ExprEvaluator right = createExprEvaluator();
            if (this.opsType == 12) {
                evaluator.setOps(/*"+"*/null, 11, 100);
                negate = true;
            } else {
                evaluator.setOps(/*token*/null, this.opsType, this.opsLevel);
            }
            evaluator.setRight(right);
            parseNext(right, negate);
        } else if (this.tokenType == NEGATE_TOKEN) {
            // 负数标记，上一个token必为操作符
            // 只做桥接作用,基于负负得正与上一个negate值互斥
            parseNext(evaluator, !negate);
        }

        if (!readable()) {
            return;
        }
    }

    private boolean readable() {
        return this.readIndex < length();
    }

    /**
     * 读取下一个字符
     *
     * @return
     */
    private char read() {
        return this.sourceChars[offset + this.readIndex];
    }

    /**
     * 读取指定位置字符
     */
    private char read(int index) {
        return this.sourceChars[offset + index];
    }

    /**
     * 获取下一个标记
     */
    private void parseNextToken() {

        // reset
        this.resetToken();

        if (!readable()) {
            return;
        }

        // 读取跳过空白
        while (readable() && isWhitespace(read())) {
            this.readIndex++;
        }

        if (!readable()) {
            return;
        }

        char currentChar = read();

        // 默认下预判规则
        // 1 如果上一个token是运算符号，则当前一定不是运算符号
        // 2 如果上一个token是数值，则当前一定不是数值
        int opsSymbolIndex = -1;
        // 是否减号操作符
        boolean isMinusSymbol = false;
        if ((Character.isDigit(currentChar) || (isMinusSymbol = currentChar == '-')) && getTokenTypeGroup(prevTokenType) != GROUP_TOKEN_VALUE) {
            this.tokenBuffer.append(currentChar);
            this.readIndex++;

            int readIndex = this.readIndex;
            int numberRadix = 10;
            char firstDigitChar = '\0', secondeDigitChar = '\0';
            if (isMinusSymbol) {
                if (readIndex + 1 < length()) {
                    firstDigitChar = read(readIndex++);
                    secondeDigitChar = read(readIndex++);
                }
            } else {
                firstDigitChar = currentChar;
                if (readIndex < length()) {
                    secondeDigitChar = read(readIndex++);
                }
            }

            if (firstDigitChar == '0') {
                if (secondeDigitChar == 'x' || secondeDigitChar == 'X') {
                    numberRadix = 16;
                    this.readIndex = readIndex;
                } else {
                    if (Character.isDigit(secondeDigitChar)) {
                        numberRadix = 8;
                        this.readIndex = readIndex;
                        this.tokenBuffer.append(secondeDigitChar);
                    }
                }
            }
            char prevChar = '\0';
            while (readable() && isDigit(currentChar = read(), numberRadix)) {
                if(currentChar == '-' && prevChar != 'E' && prevChar != 'e') {
                    break;
                }
                this.tokenBuffer.append(currentChar);
                this.readIndex++;
                prevChar = currentChar;
                if (this.readIndex >= length()) {
                    break;
                }
            }
            if (this.tokenBuffer.length() == 1 && isMinusSymbol) {
                if (this.prevTokenType == OPS_TOKEN) {
                    // 减号允许出现在括号前或者number变量前
                    // 拓展一个负数token协助解析,但不参与运算
                    this.tokenType = NEGATE_TOKEN;
//                    throw new ExpressionException("表达式解析失败,位置: " + this.readIndex + "，多余的操作符号'" + (this.exprSource.substring(0, readIndex)) + "'");
                } else {
                    // 操作符-
                    this.tokenType = OPS_TOKEN;
                    this.opsType = 12;
                    this.opsLevel = 100;
                }
            } else {
                this.tokenType = NUM_TOKEN;
                this.numberRadix = numberRadix;
            }
            checkTokenSyntaxError();
        } else if (this.prevTokenType != OPS_TOKEN && (opsSymbolIndex = getOpsSymbolIndex(currentChar)) > -1) {
            // 运算操作符号 +-*/% **
            // 位运算符 >> << &(位-与) |（位-或） ^（位-异或）  ~（去反）
            // 逻辑操作符号 > < >= <= == != ! &&（逻辑与） ||（逻辑或） !（逻辑非）
            char firstMatchChar = currentChar;
            this.readIndex++;
            this.tokenType = OPS_TOKEN;
            // 查找运算分隔符
            if (readable()
                    && !isWhitespace(currentChar = read())) {
                // 双操作符只支持:
                // 运算操作符 ** << >>
                // 逻辑操作符 >= <= == && ||
                if (currentChar == '*' && opsSymbolIndex == 1) {
                    // 指数运算调整类型和优先级
                    this.opsType = 4;
                    this.opsLevel = 9;
                    this.readIndex++;
                } else if (opsSymbolIndex == 31) {
                    // &&
                    if (currentChar == '&') {
                        // 逻辑与： &&
                        this.readIndex++;
                        this.opsType = 61;
                        this.opsLevel = 600;
                    } else {
                        this.tokenBuffer.append(firstMatchChar).append(currentChar);
                        throwOperationNotSupported();
                    }
                } else if (opsSymbolIndex == 33) {
                    // ||
                    if (currentChar == '|') {
                        // 逻辑或： ||
                        this.readIndex++;
                        this.opsType = 62;
                        this.opsLevel = 600;
                    } else {
                        this.tokenBuffer.append(firstMatchChar).append(currentChar);
                        throwOperationNotSupported();
                    }
                } else if (opsSymbolIndex == 43) {
                    // !只能搭配=使用
                    if (currentChar == '=') {
                        // 逻辑不等于 != 和 == >=优先级一致
                        this.readIndex++;
                        this.opsType = 56;
                        this.opsLevel = 500;
                    } else {
                        this.tokenBuffer.append(firstMatchChar).append(currentChar);
                        throwOperationNotSupported();
                    }
                } else if (opsSymbolIndex == 51) {
                    // 只支持 >> >=
                    if (currentChar == '>') {
                        // 位移运算: >>
                        this.readIndex++;
                        this.opsType = 21;
                        this.opsLevel = 200;
                    } else if (currentChar == '=') {
                        // 逻辑运算符： >=
                        this.readIndex++;
                        this.opsType = 54;
                        this.opsLevel = 500;
                    } else {
                        this.tokenBuffer.append(firstMatchChar).append(currentChar);
                        throwOperationNotSupported();
                    }
                } else if (opsSymbolIndex == 52) {
                    // 只支持 << <=
                    if (currentChar == '<') {
                        // 位移运算: <<
                        this.readIndex++;
                        this.opsType = 22;
                        this.opsLevel = 200;
                    } else if (currentChar == '=') {
                        // 逻辑运算符： <=
                        this.readIndex++;
                        this.opsType = 55;
                        this.opsLevel = 500;
                    } else {
                        this.tokenBuffer.append(firstMatchChar).append(currentChar);
                        throwOperationNotSupported();
                    }
                } else if (opsSymbolIndex == 53) {
                    if (currentChar == '=') {
                        // 逻辑==
                        this.readIndex++;
                        this.opsType = 53;
                        this.opsLevel = 500;
                    } else {
                        this.tokenBuffer.append(firstMatchChar).append(currentChar);
                        throwOperationNotSupported();
                    }
                } else {
//                    this.tokenBuffer.append(currentChar);
//                    throwOperationNotSupported();
                }
            } else {
                if (opsSymbolIndex == 43 || opsSymbolIndex == 53) {
                    this.tokenBuffer.append(firstMatchChar);
                    // 不支持=和！单独使用
                    // 后续实现！针对逻辑表达式的非运算
                    throwOperationNotSupported();
                }
            }
            checkTokenSyntaxError();
        } else if (isBracketSymbol(currentChar)) {
            this.opsType = 5;
            this.opsLevel = 1;
            // 括号
            this.readIndex++;
            if (currentChar == '(') {
                this.tokenType = BRACKET_TOKEN;
                bracketCount++;
                checkBeforeBracketTokenSyntaxError();
            } else {
                bracketCount--;
                this.tokenType = BRACKET_END_TOKEN;
                if (bracketCount < 0) {
                    this.tokenBuffer.append(')');
                    throwOperationNotSupported();
                }
                checkBeforeBracketEndTokenSyntaxError();
            }
        } else if (isIdentifierStart(currentChar)) {
            this.tokenBuffer.append(currentChar);
            this.readIndex++;
            while (readable() && (isIdentifierAppend(currentChar = read()))) {
                // 支持a.b.c.d
                this.tokenBuffer.append(currentChar);
                this.readIndex++;
            }
            this.tokenType = VAR_TOKEN; //设置标记类型为变量

            // 检查是否内置关键字
            this.checkIfBuiltInKeywords();
            checkTokenSyntaxError();
        } else if (currentChar == '\'') {
            this.readIndex++;
            char prevCh = '\0';
            // 查找直到遇到下一个字符'\''终止(前置无转义符)
            // expr: !(currentChar == '\'' && prevCh ！= '\\') <==> currentChar != '\'' || prevCh == '\\'
            while (readable() && ((currentChar = read()) != '\'' || prevCh == '\\')) {
                this.tokenBuffer.append(currentChar);
                this.readIndex++;
                if (readable()) {
                    prevCh = currentChar;
                }
            }
            if (currentChar != '\'' || prevCh == '\\') {
                throw new ExpressionException("语法错误: 位置: " + this.readIndex + "，未找到结束字符\"'\"");
            }
            // 最后一个字符属于token范围内字符需要++
            this.readIndex++;
            // 标记为常量
            this.tokenType = STR_TOKEN;
            checkTokenSyntaxError();
        } else if (currentChar == '{') {
            // 静态数组（只支持number和字符串两种）
            // java中静态数组使用{}而不是[]
            // 只能在解释模式下运行，编译模式java不支持
            // 不支持嵌套数组，即数组只能一层，如果要嵌套请使用变量
            this.readIndex++;
            this.tokenBuffer.append(currentChar);
            while (readable() && (currentChar = read()) != '}') {
                this.tokenBuffer.append(currentChar);
                this.readIndex++;
                if (this.readIndex >= length()) {
                    break;
                }
            }
            if (currentChar != '}') {
                throw new ExpressionException("表达式错误， 未找到与开始字符'{'相匹配的结束字符 '}'");
            }
            this.tokenBuffer.append(currentChar);
            this.readIndex++;
            this.tokenType = ARR_TOKEN;
            checkTokenSyntaxError();
        } else if (currentChar == '@') {
            // 函数token(解析函数内容)
            this.readIndex++;
            // 校验@后紧跟的标识符是否合法
            if (readable()) {
                char identifierStart = read();
                this.readIndex++;
                // 以java标识符开头
                if (Character.isJavaIdentifierStart(identifierStart)) {
                    this.tokenBuffer.append(identifierStart);
                } else {
                    throw new ExpressionException(" Expression syntax error, position:" + this.readIndex + ",unexpected function start char  : '" + identifierStart + "'");
                }
            }
            // 查找'('前面的字符
            while (readable() && isIdentifierAppend(currentChar = read())) {
                this.tokenBuffer.append(currentChar);
                this.readIndex++;
                if (this.readIndex >= length()) {
                    break;
                }
            }
            // 清除空白
            while (readable() && isWhitespace(currentChar = read())) {
                this.readIndex++;
            }

            // 查找'('
            if (currentChar != '(') {
                String readSource = this.exprSource.substring(0, this.readIndex);
                throw new ExpressionException(" Expression syntax error, position:" + this.readIndex + ", source: '" + readSource + "' function start symbol '(' not found !");
            }
            this.readIndex++;

            // 句柄和参数分割标记
            this.tokenBuffer.append('@');
            // 查找结束的')'
            int bracketCount = 1;
            while (readable()) {
                currentChar = read();
                this.readIndex++;
                if (currentChar == ')') {
                    bracketCount--;
                } else if (currentChar == '(') {
                    bracketCount++;
                }
                if (bracketCount == 0) {
                    break;
                }
                this.tokenBuffer.append(currentChar);
            }

            this.tokenType = FUN_TOKEN;
            checkTokenSyntaxError();
        } else {
            // 无法识别的字符
            throw new ExpressionException("表达式中未预期出现的token字符 '" + currentChar + "', pos " + readIndex + ", source: " + new String(sourceChars, offset, length()));
        }
    }

    private void checkIfBuiltInKeywords() {
        int length = this.tokenBuffer.length();
        char charFir = this.tokenBuffer.charAt(0);
        if (length == 2) {
            char charSec = this.tokenBuffer.charAt(1);
            if ((charFir == 'i' && charSec == 'n') || (charFir == 'I' && charSec == 'N')) {
                // in(IN)语法归纳到操作符于&&，||， != 同级别
                // 判断左边的对象是否为右边集合的中元素或者属性，或者map的key
                this.opsType = 63;
                this.opsLevel = 600;
                this.tokenType = OPS_TOKEN;
            }
        } else if (length == 3) {
            if (this.tokenBuffer.indexOf("out") == 0) {
                // out指令 实际上是in操作取反
                this.opsType = 64;
                this.opsLevel = 600;
                this.tokenType = OPS_TOKEN;
            }
        }
    }

    private int getTokenTypeGroup(int tokenType) {
        if (tokenType == OPS_TOKEN) {
            // 操作符号
            return GROUP_TOKEN_OPS;
        } else if (tokenType == NUM_TOKEN || tokenType == STR_TOKEN || tokenType == VAR_TOKEN || tokenType == ARR_TOKEN) {
            // 值
            return GROUP_TOKEN_VALUE;
        }
        return 0;
    }

    private void checkTokenSyntaxError() {
        // 暂时校验最近两个token类型一定不能一致
        int preGroupValue = getTokenTypeGroup(this.prevTokenType);
        int groupValue = getTokenTypeGroup(this.tokenType);
        if (preGroupValue == groupValue) {
            if (preGroupValue == GROUP_TOKEN_OPS) {
                String readSource = this.exprSource.substring(0, this.readIndex);
                throw new ExpressionException(" Expression syntax error, position:" + this.readIndex + ",Duplicate operation type: '" + readSource + "'");
            } else if (preGroupValue == GROUP_TOKEN_VALUE) {
                String readSource = this.exprSource.substring(0, this.readIndex);
                throw new ExpressionException(" Expression syntax error, position:" + this.readIndex + ",Missing operation symbol: '" + readSource + "'");
            }
        } else {
            // value token before is )
            if (groupValue == GROUP_TOKEN_VALUE && this.prevTokenType == BRACKET_END_TOKEN) {
                String readSource = this.exprSource.substring(0, this.readIndex);
                throw new ExpressionException(" Expression syntax error, position:" + this.readIndex + ",unexpected symbol : '" + readSource + "'");
            }
            // // ops token before is (
            if (groupValue == GROUP_TOKEN_OPS && this.prevTokenType == BRACKET_TOKEN) {
                String readSource = this.exprSource.substring(0, this.readIndex);
                throw new ExpressionException(" Expression syntax error, position:" + this.readIndex + ",unexpected symbol : '" + readSource + "'");
            }
        }


    }

    // check bracket ) before
    // expected value type,  unexpected ( && ops ,
    private void checkBeforeBracketEndTokenSyntaxError() {
        int preGroupValue = getTokenTypeGroup(this.prevTokenType);
        if (this.prevTokenType == BRACKET_TOKEN || preGroupValue == GROUP_TOKEN_OPS) {
            String readSource = this.exprSource.substring(0, this.readIndex);
            throw new ExpressionException(" Expression syntax error, position:" + this.readIndex + ",unexpected symbol : '" + readSource + "'");
        }
    }

    // check bracket ( before
    // expected ops type,  unexpected ) && value ,
    private void checkBeforeBracketTokenSyntaxError() {
        int preGroupValue = getTokenTypeGroup(this.prevTokenType);
        if (this.prevTokenType == BRACKET_END_TOKEN && preGroupValue == GROUP_TOKEN_VALUE) {
            String readSource = this.exprSource.substring(0, this.readIndex);
            throw new ExpressionException(" Expression syntax error, position:" + this.readIndex + ",unexpected symbol : '" + readSource + "'");
        }
    }

    private void throwOperationNotSupported() {
        throw new ExpressionException(" Expression parsing failed at position: " + this.readIndex + ", unexpected symbol '" + this.tokenBuffer.toString() + "'");
    }

    private boolean isWhitespace(char c) {
        return c <= ' ';  // ' '
//        return Character.isWhitespace(c);
    }

    // 解析模式除了数字和.开头
    protected boolean isIdentifierStart(char c) {
        // Character.isJavaIdentifierStart(c);
        return c == '_' || c == '$' /*|| c == '#'*/ || Character.isLetter(c);
    }

    // 解析模式下支持 _ $ . 字母，数字等
    protected boolean isIdentifierAppend(char c) {
        return c == '_' || c == '$' || c == '.' /*|| c == '#'*/ || Character.isLetter(c) || isDigit(c);
    }

    /**
     * 判断是否为10进制 0-9 . E/e
     */
    private boolean isDigit(char c) {
        return (c >= 48 && c <= 57) || c == '.' || c == 'E' || c == 'e';
    }

    /**
     * 判断是否为10进制 0-9 . E/e
     */
    private boolean isDigit(char c, int numberRadix) {
        if (numberRadix == 10) {
            return (c >= 48 && c <= 57) || c == '.' || c == 'E' || c == 'e' || c == '-' || c == 'D' || c == 'd';
        } else if (numberRadix == 16) {
            // 16进制数字： 0-9 a-f A-F
            return (c >= 48 && c <= 57) || (c >= 'a' && c <= 'f') || (c >= 'A' && c <= 'F');
        } else {
            // 8进制 0-7
            return c >= 48 && c <= 55;
        }
    }

    private void resetToken() {
        // 重置记录上一个token类型
        this.prevTokenType = this.tokenType;
        if (this.tokenBuffer.length() > 0) {
            this.tokenBuffer.setLength(0);
        }
        this.tokenType = RESET_TOKEN;
        this.opsType = 0;
        this.opsLevel = 0;
    }

    /**
     * 判断是否为操作符号
     * +-/*%**（加(+)减(-)乘(*)除(/)余(%)及指数(**)）
     */
    private int getOpsSymbolIndex(char c) {
        switch (c) {
            case '*':
                this.opsType = 1;
                this.opsLevel = 10;
                return this.opsType;
            case '/':
                this.opsType = 2;
                this.opsLevel = 10;
                return this.opsType;
            case '%':
                this.opsType = 3;
                this.opsLevel = 10;
                return this.opsType;
            case '+':
                this.opsType = 11;
                this.opsLevel = 100;
                return this.opsType;
            case '-':
                this.opsType = 12;
                this.opsLevel = 100;
                return this.opsType;
            case '&':
                this.opsType = 31;
                this.opsLevel = 300;
                return this.opsType;
            case '^':
                this.opsType = 32;
                this.opsLevel = 301;
                return this.opsType;
            case '|':
                this.opsType = 33;
                this.opsLevel = 302;
                return this.opsType;
            case '!':
                // &&(41) ||(42)  !=(43)
                this.opsType = 43;
                this.opsLevel = 400;
                return this.opsType;
            case '>':
                this.opsType = 51;
                this.opsLevel = 500;
                return this.opsType;
            case '<':
                this.opsType = 52;
                this.opsLevel = 500;
                return this.opsType;
            case '=':
                this.opsType = 53;
                this.opsLevel = 500;
                return this.opsType;
            case ':':
                this.opsType = 70;
                this.opsLevel = 700;
                return this.opsType;
            case '?':
                this.opsType = 71;
                this.opsLevel = 701;
                return this.opsType;
            default:
                return -1;
        }
    }

    /**
     * 是否括号标识符
     *
     * @param c
     * @return
     */
    private boolean isBracketSymbol(char c) {
        return c == '(' || c == ')';
    }

    public Object evaluate() {
        return exprEvaluator.evaluate(EvaluateEnvironment.create());
    }

    public Object evaluate(Object context) {
        return evaluate(EvaluateEnvironment.create(context));
    }

    @Override
    public Object evaluate(EvaluateEnvironment evaluateEnvironment) {
        return exprEvaluator.evaluate(evaluateEnvironment);
    }

}