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

import io.github.wycst.wast.common.expression.invoker.ChainVariableInvoker;
import io.github.wycst.wast.common.expression.invoker.Invoker;
import io.github.wycst.wast.common.expression.invoker.VariableInvoker;
import io.github.wycst.wast.common.expression.invoker.VariableUtils;
import io.github.wycst.wast.common.utils.NumberUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 表达式解析器,生成执行模型
 *
 * <p>
 * 符号   优先级
 * ()      1
 * **      9 (指数运算,平方（根）,立方（根）)
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

    public ExprParser(String exprSource) {
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
    public static final int IDENTIFIER_TOKEN = 2;
    public static final int NUM_TOKEN = 3;
    public static final int BRACKET_TOKEN = 4;
    public static final int BRACKET_END_TOKEN = 6;

    // 字符串
    public static final int STR_TOKEN = 7;
    // 数组token
    public static final int ARR_TOKEN = 8;
    // 函数(method)token,以@开始并且紧跟java标识符（@fun(...arg0)或者bean.fun(...arg0)）
    public static final int FUN_TOKEN = 9;
    public static final int NEGATE_TOKEN = 10;
    public static final int NOT_TOKEN = 11;

    // 最大超过token数量时开启优化
    static final int Max_Optimize_Count = (ExprEvaluator.Optimize_Depth_Value << 1) + 1;

    // 表达式源
    private String exprSource;
    private char[] sourceChars;
    private int offset;
    private int count;
    private int readIndex;

    private String errorMsg;
    private Map<String, VariableInvoker> invokes;
    private Map<String, VariableInvoker> tailInvokes;

    protected Invoker chainInvoker;
    protected Invoker chainValues;
    protected int variableCount;
//    protected boolean cachedVariableValue;

    // 标记类型
    private int prevTokenType;
    private int tokenType;

    // 操作符类型
    private int opsType;
    // 操作符优先级
    private int opsLevel;

    // 括号计数>0, 解析过程中如果小于0为异常,最终为0
    private int bracketCount;

    // 数量
    private int evaluatorCount;

    // 表达式解析器
    private ExprEvaluator exprEvaluator = createExprEvaluator();
    // 上下文
    private ExprParserContext exprParserContext = new ExprParserContext();

    private final AtomicInteger cntForCompress = new AtomicInteger(0);
    private boolean compressed = false;

    // 返回全局的parser
    protected ExprParser global() {
        return this;
    }

    protected Map<String, VariableInvoker> getInvokes() {
        return invokes;
    }

    protected Map<String, VariableInvoker> getTailInvokes() {
        return tailInvokes;
    }

    protected ExprEvaluator createExprEvaluator() {
        return new ExprEvaluator();
    }

    protected String getSource() {
        return this.exprSource;
    }

    public int length() {
        return count;
    }

    protected ExprEvaluator getEvaluator() {
        return exprEvaluator;
    }

    protected void init(String exprSource) {
        this.exprSource = exprSource;
        this.count = exprSource.length();
        this.sourceChars = getChars(exprSource);
    }

    protected void init(char[] buffers, int offset, int count) {
        this.sourceChars = buffers;
        this.offset = offset;
        this.count = count;
    }

    /**
     * 解析表达式字符串
     */
    protected void parse() {
        // 解析执行器
        parseEvaluator();
        // 置换优先级
        displacement(exprEvaluator);
        // 合并
        merge();
        // 压缩变量
        compressVariables();
        // optimize
        checkOptimizeRequired();
    }

    private void checkOptimizeRequired() {
        if (evaluatorCount > Max_Optimize_Count) {
            optimize();
        }
    }

    private void parseEvaluator() {
        exprParserContext.setContext(exprEvaluator, false, false);
        do {
            parseNext(exprParserContext);
            ++evaluatorCount;
        } while (readable());
    }

    private void merge() {
        if (exprEvaluator.evalType == 0) {
            exprEvaluator = exprEvaluator.left;
        } else {
            mergeLast(exprEvaluator);
        }
    }

    private void mergeLast(ExprEvaluator root) {
        ExprEvaluator rr = root.right;
        if (rr != null && rr.evalType == 0 && rr.left != null) {
            root.right = rr.left;
        }
    }

    // 优先级置换
    private void displacement(ExprEvaluator exprEvaluator) {
        // 1次置换结构优先级
        displacementChain(exprEvaluator);

        // 2次置换运算顺序
        displacementChain(exprEvaluator);
    }

    private void displacementChain(ExprEvaluator exprEvaluator) {
        exprParserContext.exprEvaluator = exprEvaluator;
        do {
            displacementSplit(exprParserContext);
        } while (exprParserContext.exprEvaluator != null);
    }

    private void displacementSplit(ExprParserContext exprParserContext) {
        ExprEvaluator exprEvaluator = exprParserContext.exprEvaluator;

        if (exprEvaluator == null) {
            return;
        }
        int opsType = exprEvaluator.opsType;
        int level = exprEvaluator.level;

        int evalType = exprEvaluator.getEvalType();
        ExprEvaluator left = exprEvaluator.getLeft();

        ExprEvaluator right = exprEvaluator.getRight();
        if (right == null) {
            exprParserContext.exprEvaluator = null;
            return;
        }

        // 三目运算冒号（：）特殊处理
        if(level == 700) {
            displacement(right);
            exprParserContext.exprEvaluator = null;
            return;
        }

        int rLevel = right.level;
        if (rLevel <= 0) {
            exprParserContext.exprEvaluator = null;
            return;
        }
        if (level > rLevel) {
            // 发现了右侧的优先级更高,触发右侧合并
            mergeRight(right, level);
            rLevel = right.level;
        }

        int rEvalType = right.getEvalType();
        ExprEvaluator rLeft = right.getLeft();
        ExprEvaluator rRight = right.getRight();

        if (level <= rLevel) {
            // 如果左侧的优先级级别高（level小）于右侧就开启轮换
            ExprEvaluator newLeft = createExprEvaluator();
            newLeft.setEvalType(evalType);
            newLeft.setOperator(opsType, level);
            newLeft.setNegate(exprEvaluator.negate);
            newLeft.setLogicalNot(exprEvaluator.logicalNot);
            newLeft.setLeft(left);
            newLeft.setRight(rLeft);

            exprEvaluator.setOperator(right.opsType, rLevel);
            exprEvaluator.setNegate(right.isNegate());
            exprEvaluator.setLogicalNot(right.logicalNot);
            exprEvaluator.setEvalType(rEvalType);
            exprEvaluator.setLeft(newLeft);
            exprEvaluator.setRight(rRight);

            // 继续合并
//            displacementSplit(exprEvaluator);
            exprParserContext.exprEvaluator = exprEvaluator;
        } else {
            if (rLevel == 1) {
                // 如果是括号子表达式开始子轮换
                displacement(right.getRight().getLeft());
            }
            // 继续右侧轮换,如果遇到与指定level一致的就终止
            // 后续实现,暂时统一将-转换为+处理二级优先级问题
            // 是否使用while循环代替递归？
//            displacementSplit(right);
            exprParserContext.exprEvaluator = right;
        }
    }

    private void mergeRight(ExprEvaluator exprEvaluator, int targetLevel) {
        if (exprEvaluator == null) {
            return;
        }
        int evalType = exprEvaluator.getEvalType();
        int level = exprEvaluator.level;

        if (level >= targetLevel) {
            // 停止合并
            return;
        }

        ExprEvaluator left = exprEvaluator.getLeft();
        ExprEvaluator right = exprEvaluator.getRight();
        if (right == null) {
            return;
        }

        // 三目运算冒号（：）特殊处理
        if(level == 700) {
            displacement(right);
            return;
        }

        int rLevel = right.level;
        if (rLevel <= 0) {
            return;
        }

        int rEvalType = right.getEvalType();
        ExprEvaluator rLeft = right.getLeft();
        ExprEvaluator rRight = right.getRight();

        if (level <= rLevel) {
            // parse后表达式从右至左
            // 如果当前的优先级级别高（level越小）就开启轮换
            ExprEvaluator newLeft = createExprEvaluator();
            newLeft.setEvalType(evalType);
            newLeft.setOperator(exprEvaluator.opsType, exprEvaluator.level);
            newLeft.setNegate(exprEvaluator.negate);
            newLeft.setLogicalNot(exprEvaluator.logicalNot);
            newLeft.setLeft(left);
            newLeft.setRight(rLeft);

            exprEvaluator.setOperator(right.opsType, right.level);
            exprEvaluator.setNegate(right.negate);
            exprEvaluator.setLogicalNot(right.logicalNot);
            exprEvaluator.setEvalType(rEvalType);
            exprEvaluator.setLeft(newLeft);
            exprEvaluator.setRight(rRight);

            // 继续合并
            mergeRight(exprEvaluator, targetLevel);
        }
    }

    protected void compressVariables() {
        // invokes
        if (tailInvokes != null) {
            chainInvoker = ChainVariableInvoker.build(tailInvokes);
            chainValues = ChainVariableInvoker.build(invokes, true);
            variableCount = invokes.size();
//            cachedVariableValue = tailInvokes.size() != invokes.size();

            tailInvokes.clear();
            invokes.clear();

            tailInvokes = null;
            invokes = null;
        }
    }

    private void parseOpsToken(ExprParserContext exprParserContext) {
        ExprEvaluator evaluator = exprParserContext.exprEvaluator;
        boolean negate = exprParserContext.negate;
        boolean logicalNot = exprParserContext.logicalNot;
        evaluator.setEvalType(1);
        ExprEvaluator right = createExprEvaluator();
        if (this.opsType == 12) {
            evaluator.setOperator(11, 100);
            negate = true;
        } else {
            evaluator.setOperator(this.opsType, this.opsLevel);
        }
        evaluator.setRight(right);
        exprParserContext.setContext(right, negate, logicalNot);
    }

    private void parseVarToken(ExprParserContext exprParserContext, char startChar, String identifierValue, List<String> variableKeys) {
        ExprEvaluator evaluator = exprParserContext.exprEvaluator;
        boolean negate = exprParserContext.negate;
        boolean logicalNot = exprParserContext.logicalNot;

        switch (startChar) {
            case 't': {
                if ("true".equals(identifierValue)) {
                    ExprEvaluator.ExprEvaluatorConstantImpl left = new ExprEvaluator.ExprEvaluatorConstantImpl(true && !logicalNot);
                    left.setEvalType(-1);
                    evaluator.setLeft(left);
                    exprParserContext.setContext(evaluator, false, false);
                    return;
                }
                break;
            }
            case 'f': {
                if ("false".equals(identifierValue)) {
                    ExprEvaluator.ExprEvaluatorConstantImpl left = new ExprEvaluator.ExprEvaluatorConstantImpl(false || logicalNot);
                    left.setEvalType(-1);
                    evaluator.setLeft(left);
                    exprParserContext.setContext(evaluator, false, false);
                    return;
                }
                break;
            }
            case 'n': {
                if ("null".equals(identifierValue)) {
                    ExprEvaluator.ExprEvaluatorConstantImpl left = new ExprEvaluator.ExprEvaluatorConstantImpl(null);
                    left.setEvalType(-1);
                    evaluator.setLeft(left);
                    exprParserContext.setContext(evaluator, false, false);
                    return;
                }
                break;
            }
        }
        // 检查是否已初始化
        checkInitializedInvokes();
        // 变量
        ExprEvaluator.ExprEvaluatorVariableImpl left = new ExprEvaluator.ExprEvaluatorVariableImpl();
        left.setEvalType(6);
        left.setNegate(negate);
        left.setLogicalNot(logicalNot);

        left.setVariableInvoker(identifierValue == null ? VariableUtils.build(variableKeys, getInvokes(), getTailInvokes()) : VariableUtils.buildRoot(identifierValue, getInvokes(), getTailInvokes()));
        evaluator.setLeft(left);
        exprParserContext.setContext(evaluator, false, false);
    }

    // 检查是否已初始化Invokes，如果没有进行初始化
    void checkInitializedInvokes() {
        if (invokes == null) {
            invokes = new HashMap<String, VariableInvoker>();
            tailInvokes = new HashMap<String, VariableInvoker>();
        }
    }

    private void parseNumToken(ExprParserContext exprParserContext, Number numberValue) {
        ExprEvaluator evaluator = exprParserContext.exprEvaluator;
        ExprEvaluator.ExprEvaluatorConstantImpl left = new ExprEvaluator.ExprEvaluatorConstantImpl(numberValue);
        // left.setEvalType(3);
        // left.setLogicalNot(logicalNot);
//        if(exprParserContext.logicalNot) {
//            throw new ExpressionException("!"+numberValue+" not supported ");
//        }

        evaluator.setLeft(left);
        exprParserContext.setContext(evaluator, false, false);
    }

    private void parseBracketToken(ExprParserContext exprParserContext) {
        ExprEvaluator evaluator = exprParserContext.exprEvaluator;
        boolean negate = exprParserContext.negate;
        boolean logicalNot = exprParserContext.logicalNot;

        // 构建子表达式作为当前表达式evaluator的right
        // 遇到)（BRACKET_END_TOKEN）结束
        evaluator.setOperator(this.opsType, this.opsLevel);
        evaluator.setNegate(negate);
        evaluator.setLogicalNot(logicalNot);

        // 括号执行器
        ExprEvaluator child = createExprEvaluator();

        // 循环解析不适合处理括号嵌套问题
        // 使用递归解析
        ExprParserContext bracketParserContext = new ExprParserContext(child, false, false);
        do {
            parseNext(bracketParserContext);
        } while (readable() && !bracketParserContext.endFlag);
        // bracketParserContext的endFlag设置为true时,当前递归栈结束；
        // 解析类型如果没有结束括号说明表达式错误
        if (!bracketParserContext.endFlag) {
            String errorMessage = createErrorMessage(sourceChars, this.readIndex);
            throw new ExpressionException("Expression syntax error, pos: " + this.readIndex + ",Expression[... " + errorMessage + " ...], missing closing symbol')'");
        }
        //  置换子表达式优先级
        //  displacementSplit(child);
        displacementChain(child);
        mergeLast(child);

        ExprEvaluator right = createExprEvaluator();
        evaluator.setEvalType(5);
        evaluator.setRight(right);

        right.setLeft(child);
        exprParserContext.setContext(right, false, false);
    }

    private void parseStrToken(ExprParserContext exprParserContext, String strValue) {
        ExprEvaluator evaluator = exprParserContext.exprEvaluator;
        boolean negate = exprParserContext.negate;
        boolean logicalNot = exprParserContext.logicalNot;

        // 字符串常量
        ExprEvaluator.ExprEvaluatorConstantImpl left = new ExprEvaluator.ExprEvaluatorConstantImpl(strValue);
        left.setEvalType(7);
        left.setNegate(negate);
        left.setLogicalNot(logicalNot);
        evaluator.setLeft(left);
        exprParserContext.setContext(evaluator, false, false);
    }

    private void parseArrToken(ExprParserContext exprParserContext, String arrStr) {
        ExprEvaluator evaluator = exprParserContext.exprEvaluator;
        boolean negate = exprParserContext.negate;
        boolean logicalNot = exprParserContext.logicalNot;

        // 静态数组
        ExprEvaluator left = createExprEvaluator();
        left.setEvalType(8);
        left.setNegate(negate);
        left.setLogicalNot(logicalNot);
        left.setArrayValue(arrStr);
        evaluator.setLeft(left);
        exprParserContext.setContext(evaluator, false, false);
    }

    private void parseFunToken(ExprParserContext exprParserContext, String functionName, String args) {
        ExprEvaluator evaluator = exprParserContext.exprEvaluator;
        boolean negate = exprParserContext.negate;
        boolean logicalNot = exprParserContext.logicalNot;

        // 函数token
        ExprEvaluator.ExprEvaluatorFunctionImpl left = new ExprEvaluator.ExprEvaluatorFunctionImpl();
        left.setEvalType(9);
        left.setNegate(negate);
        left.setLogicalNot(logicalNot);
        left.setFunction(functionName, args, global());
        evaluator.setLeft(left);
        exprParserContext.setContext(evaluator, false, false);
    }

    private void parseMethodToken(ExprParserContext exprParserContext, List<String> variableKeys, String methodName, String args) {
        ExprEvaluator evaluator = exprParserContext.exprEvaluator;
        boolean negate = exprParserContext.negate;
        boolean logicalNot = exprParserContext.logicalNot;

        checkInitializedInvokes();

        // 函数token
        ExprEvaluator.ExprEvaluatorMethodImpl left = new ExprEvaluator.ExprEvaluatorMethodImpl();
        left.setEvalType(9);
        left.setNegate(negate);
        left.setLogicalNot(logicalNot);
        left.setMethod(VariableUtils.build(variableKeys, getInvokes(), getTailInvokes()), methodName, args, global());
        evaluator.setLeft(left);
        exprParserContext.setContext(evaluator, false, false);
    }

    private void parseNegateToken(ExprParserContext exprParserContext) {
        // 负数标记,上一个token必为操作符
        // 只做桥接作用,基于负负得正与上一个negate值互斥
        boolean negate = exprParserContext.negate;
        exprParserContext.negate = !negate;
    }

    private void parseNotToken(ExprParserContext exprParserContext) {
        // 取非标记,上一个token必为操作符
        // 只做桥接作用,基于`非非得是`与上一个logicalNot值互斥
        boolean logicalNot = exprParserContext.logicalNot;
        exprParserContext.logicalNot = !logicalNot;
    }

    private static ThreadLocal<List<String>> localVariableKeys = new ThreadLocal<List<String>>() {
        @Override
        protected List<String> initialValue() {
            return new ArrayList<String>();
        }
    };

    private static List<String> getLocalVariableKeys() {
        List<String> keys = localVariableKeys.get();
        return keys;
    }

    private void parseNext(ExprParserContext exprParserContext) {

        // reset
        this.resetToken();

        // 读取跳过空白
        while (readable() && isWhitespace(read())) {
            ++this.readIndex;
        }

        if (!readable()) {
            return;
        }

        char currentChar = read();

        // 预判规则
        // 1 如果上一个token是运算符号,则当前一定不是运算符号
        // 2 如果上一个token是数值,则当前一定不是数值
        int opsSymbolIndex = -1;
        // 是否减号操作符
        boolean isMinusSymbol = false;
        if ((isDecimalDigitStart(currentChar) || (isMinusSymbol = currentChar == '-')) && getTokenTypeGroup(prevTokenType) != GROUP_TOKEN_VALUE) {
            int start = this.readIndex;
            int cnt = 0;
            int readIndex = ++this.readIndex;
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
                    // currentChar is already '0'
                    this.readIndex = readIndex;
                    ++cnt;
                } else {
                    if (Character.isDigit(secondeDigitChar)) {
                        numberRadix = 8;
                        this.readIndex = readIndex;
                        currentChar = secondeDigitChar;
                        ++cnt;
                    }
                }
            }

            double value = isMinusSymbol ? 0 : digit(currentChar, numberRadix);
            int mode = 0;
            int specifySuffix = 0;
            int expValue = 0;
            boolean expNegative = false;
            int decimalCount = 0;
            char prevChar = '\0';

            Number numberValue = null;
            while (readable() && isAvailableDigit(currentChar = read(), numberRadix)) {
                if (currentChar == '-' && prevChar != 'E' && prevChar != 'e') {
                    break;
                }

                // parse number
                ++cnt;
                int digit = digit(currentChar, numberRadix);
                if (currentChar == '.') {
                    if (mode != 0) {
                        throwNumberFormatException(start);
                        return;
                    }
                    // 小数点模式
                    mode = 1;
                    ++this.readIndex;
                    if (readable()) {
                        currentChar = read();
                    } else {
                        break;
                    }
                    digit = digit(currentChar, numberRadix);
                } else if (currentChar == 'E' || currentChar == 'e') {
                    if (mode == 2) {
                        throwNumberFormatException(start);
                        return;
                    }
                    // 科学计数法
                    mode = 2;
                    ++this.readIndex;
                    if (readable()) {
                        currentChar = read();
                    } else {
                        break;
                    }
                    if (currentChar == '-') {
                        expNegative = true;
                        ++this.readIndex;
                        if (readable()) {
                            currentChar = read();
                        } else {
                            break;
                        }
                    }
                    digit = digit(currentChar, numberRadix);
                }

                if (digit == -1) {
                    switch (currentChar) {
                        case 'l':
                        case 'L': {
                            if (specifySuffix == 0) {
                                specifySuffix = 1;
                                break;
                            }
                            throwNumberFormatException(start);
                            return;
                        }
                        case 'f':
                        case 'F': {
                            if (specifySuffix == 0) {
                                specifySuffix = 2;
                                break;
                            }
                            throwNumberFormatException(start);
                            return;
                        }
                        case 'd':
                        case 'D': {
                            if (specifySuffix == 0) {
                                specifySuffix = 2;
                                break;
                            }
                            throwNumberFormatException(start);
                            return;
                        }
                        default: {
                            if (currentChar <= ' ') {
                                break;
                            }
                            throwNumberFormatException(start);
                            return;
                        }
                    }
                }
                switch (mode) {
                    case 0:
                        value *= numberRadix;
                        value += digit;
                        break;
                    case 1:
                        value *= numberRadix;
                        value += digit;
                        decimalCount++;
                        break;
                    case 2:
                        // exp 模式只支持10进制
                        expValue = (expValue << 3) + (expValue << 1);
                        expValue += digit;
                        break;
                }

                ++this.readIndex;
                prevChar = currentChar;
                if (this.readIndex >= length()) {
                    break;
                }
            }
            if (cnt == 0 && isMinusSymbol) {
                // (子)表达式以'-'开始（RESET_TOKEN或者BRACKET_TOKEN）或者上一个是操作符号都识别为负数
                switch (prevTokenType) {
                    case RESET_TOKEN:
                    case OPS_TOKEN:
                    case BRACKET_TOKEN: {
                        // 减号允许出现在括号前或者number变量前
                        // 负数token不参与运算
                        this.tokenType = NEGATE_TOKEN;
                        checkTokenSyntaxError();
                        parseNegateToken(exprParserContext);
                        return;
                    }
                    default: {
                        // 操作符-
                        this.tokenType = OPS_TOKEN;
                        this.opsType = 12;
                        this.opsLevel = 100;

                        checkTokenSyntaxError();
                        parseOpsToken(exprParserContext);
                        return;
                    }
                }
            } else {
                this.tokenType = NUM_TOKEN;
                if (exprParserContext.negate ^ isMinusSymbol) {
                    value = -value;
                }
                if (mode == 0) {
                    if (specifySuffix == 0) {
                        if (value >= Long.MIN_VALUE && value <= Long.MAX_VALUE) {
                            numberValue = (long) value;
                        } else {
                            numberValue = value;
                        }
                    } else if (specifySuffix == 1) {
                        numberValue = (long) value;
                    } else if (specifySuffix == 2) {
                        numberValue = (float) value;
                    } else {
                        numberValue = value;
                    }
                } else {
                    expValue = expNegative ? -expValue - decimalCount : expValue - decimalCount;
                    if (expValue > 0) {
                        double powValue = NumberUtils.getDecimalPowerValue(expValue);
                        value *= powValue;
                    } else if (expValue < 0) {
                        double powValue = NumberUtils.getDecimalPowerValue(-expValue);
                        value /= powValue;
                    }
                    if (specifySuffix == 0) {
                        numberValue = value;
                    } else if (specifySuffix == 1) {
                        numberValue = (long) value;
                    } else if (specifySuffix == 2) {
                        numberValue = (float) value;
                    } else {
                        numberValue = value;
                    }
                }

                checkTokenSyntaxError();
                parseNumToken(exprParserContext, numberValue);
                return;
            }
        } else if (this.prevTokenType != OPS_TOKEN && (opsSymbolIndex = getOpsSymbolIndex(currentChar)) > -1) {
            // 运算操作符号 +-*/% **
            // 位运算符 >> << &(位-与) |（位-或） ^（位-异或）  ~（去反）
            // 逻辑操作符号 > < >= <= == != ! &&（逻辑与） ||（逻辑或） !（逻辑非）
            char firstMatchChar = currentChar;
            ++this.readIndex;
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
                    ++this.readIndex;
                } else if (opsSymbolIndex == 31) {
                    // &&
                    if (currentChar == '&') {
                        // 逻辑与： &&
                        ++this.readIndex;
                        this.opsType = 61;
                        this.opsLevel = 600;
                    } else {
                        this.errorMsg = String.valueOf(firstMatchChar) + currentChar;
                        throwOperationNotSupported();
                    }
                } else if (opsSymbolIndex == 33) {
                    // ||
                    if (currentChar == '|') {
                        // 逻辑或： ||
                        ++this.readIndex;
                        this.opsType = 62;
                        this.opsLevel = 600;
                    } else {
                        this.errorMsg = String.valueOf(firstMatchChar) + currentChar;
                        throwOperationNotSupported();
                    }
                } else if (opsSymbolIndex == 43) {
                    // !搭配=使用
                    if (currentChar == '=') {
                        // 逻辑不等于 != 和 == >=优先级一致
                        ++this.readIndex;
                        this.opsType = 56;
                        this.opsLevel = 500;
                    } else if (currentChar == '!') {
                        // 或者紧跟非操作符号!
                        this.tokenType = NOT_TOKEN;
                        checkTokenSyntaxError();
                        parseNotToken(exprParserContext);
                        return;
                    } else {
                        if (getOpsSymbolIndex(currentChar) > -1) {
                            this.errorMsg = String.valueOf(firstMatchChar) + currentChar;
                            throwOperationNotSupported();
                        }
                        this.tokenType = NOT_TOKEN;
                        checkTokenSyntaxError();
                        parseNotToken(exprParserContext);
                        return;
                    }
                } else if (opsSymbolIndex == 51) {
                    // 只支持 >> >=
                    if (currentChar == '>') {
                        // 位移运算: >>
                        ++this.readIndex;
                        this.opsType = 21;
                        this.opsLevel = 200;
                    } else if (currentChar == '=') {
                        // 逻辑运算符： >=
                        ++this.readIndex;
                        this.opsType = 54;
                        this.opsLevel = 500;
                    } else {
                        if (getOpsSymbolIndex(currentChar) > -1) {
                            this.errorMsg = String.valueOf(firstMatchChar) + currentChar;
                            throwOperationNotSupported();
                        }
                    }
                } else if (opsSymbolIndex == 52) {
                    // 只支持 << <=
                    if (currentChar == '<') {
                        // 位移运算: <<
                        ++this.readIndex;
                        this.opsType = 22;
                        this.opsLevel = 200;
                    } else if (currentChar == '=') {
                        // 逻辑运算符： <=
                        ++this.readIndex;
                        this.opsType = 55;
                        this.opsLevel = 500;
                    } else {
                        // fix a<b bug when no whitespace
                        if (getOpsSymbolIndex(currentChar) > -1) {
                            this.errorMsg = String.valueOf(firstMatchChar) + currentChar;
                            throwOperationNotSupported();
                        }
                    }
                } else if (opsSymbolIndex == 53) {
                    if (currentChar == '=') {
                        // 逻辑==
                        ++this.readIndex;
                        this.opsType = 53;
                        this.opsLevel = 500;
                    } else {
                        // 暂时不支持赋值表达式
                        this.errorMsg = String.valueOf(firstMatchChar) + currentChar;
                        throwOperationNotSupported();
                    }
                } else {
//                    this.tokenBuffer.append(currentChar);
//                    throwOperationNotSupported();
                }
            } else {
                // 2022-03-11 开始支持！运算
                if (/*opsSymbolIndex == 43 ||*/ opsSymbolIndex == 53) {
                    this.errorMsg = String.valueOf(firstMatchChar);
                    // 不支持=和！单独使用
                    // 后续实现！针对逻辑表达式的非运算
                    throwOperationNotSupported();
                }
            }
            checkTokenSyntaxError();
            parseOpsToken(exprParserContext);
            return;
        } else if (isBracketSymbol(currentChar)) {
            this.opsType = 5;
            this.opsLevel = 1;
            // 括号
            ++this.readIndex;
            if (currentChar == '(') {
                this.tokenType = BRACKET_TOKEN;
                bracketCount++;
                checkBeforeBracketTokenSyntaxError();
                parseBracketToken(exprParserContext);
                return;
            } else {
                bracketCount--;
                this.tokenType = BRACKET_END_TOKEN;
                if (bracketCount < 0) {
                    this.errorMsg = ")";
                    throwOperationNotSupported();
                }
                checkBeforeBracketEndTokenSyntaxError();
                exprParserContext.endFlag = true;
                return;
            }
        } else if (isIdentifierStart(currentChar)) {
            int start = this.readIndex;
            char startChar = currentChar;
            ++this.readIndex;

            int localOffset = start;
            List<String> variableKeys = getLocalVariableKeys();
            try {
                while (readable() && (isVariableAppend(currentChar = read()))) {
                    if (currentChar == '.') {
                        if (readIndex == localOffset) {
                            String errorMessage = createErrorMessage(sourceChars, this.readIndex);
                            throw new ExpressionException("Expression syntax error, pos: " + this.readIndex + ", Expression[... " + errorMessage + " ...],token '\'' is duplicate or conflict with [] ");
                        }
                        variableKeys.add(new String(sourceChars, localOffset + this.offset, readIndex - localOffset));
                        localOffset = ++this.readIndex;
                        continue;
                    }
                    if (currentChar == '[') {
                        if (readIndex == localOffset) {
                            String errorMessage = createErrorMessage(sourceChars, this.readIndex);
                            throw new ExpressionException("Expression syntax error, pos: " + this.readIndex + ", Expression[... " + errorMessage + " ...],token '[' is duplicate or conflict with '.' ");
                        }
                        // 内层循环直到遇到]结束
                        variableKeys.add(new String(sourceChars, localOffset + this.offset, readIndex - localOffset));
                        localOffset = ++this.readIndex;

                        // 查找结束的']',如果紧跟'则[]中的内容以字符串解析
                        // 字符串中的'['和']'需要忽略
                        int cnt = 1;
                        boolean stringKey = true;
                        while (readable()) {
                            if ((currentChar = read()) <= ' ') {
                                ++this.readIndex;
                                continue;
                            }
                            if (currentChar == '\'') {
                                // find end ',ignore
                                this.scanString();
                                ++this.readIndex;
                                while (readable() && (currentChar = read()) <= ' ') {
                                    ++this.readIndex;
                                }
                                if (currentChar == ']' && cnt == 1) {
                                    break;
                                }
                                // continue find
                                continue;
                            }
                            stringKey = false;
                            if (currentChar == ']') {
                                cnt--;
                            } else if (currentChar == '[') {
                                cnt++;
                            }
                            if (cnt == 0) {
                                break;
                            }
                            ++this.readIndex;
                        }
                        if (currentChar != ']') {
                            String errorMessage = createErrorMessage(sourceChars, this.readIndex);
                            throw new ExpressionException("Expression syntax error, pos: " + this.readIndex + ", Expression[... " + errorMessage + " ...],未找到与开始字符'['相匹配的结束字符 ']'");
                        }
                        if (stringKey) {
                            // 去除''
                            String valueWithQuotation = new String(sourceChars, localOffset + this.offset, readIndex - localOffset).trim();
                            variableKeys.add(valueWithQuotation.substring(1, valueWithQuotation.length() - 1));
                        } else {
                            // 子表达式使用（）
                            char[] chars = new char[readIndex - localOffset + 2];
                            chars[0] = '(';
                            chars[chars.length - 1] = ')';
                            System.arraycopy(sourceChars, localOffset + this.offset, chars, 1, chars.length - 2);
                            variableKeys.add(new String(chars));
                        }
                        localOffset = ++this.readIndex;
                        continue;
                    }
                    ++this.readIndex;
                }
                String identifierValue = new String(sourceChars, localOffset + this.offset, readIndex - localOffset);
                boolean oneLevelAccess = localOffset == start;
                if (currentChar == '(') {
                    // forward method or function
                    localOffset = ++this.readIndex;
                    int bracketCount = 1;
                    while (readable()) {
                        currentChar = read();
                        if (currentChar == '\'') {
                            // find end ',ignore
                            this.scanString();
                            ++this.readIndex;
                            continue;
                        }
                        ++this.readIndex;
                        if (currentChar == ')') {
                            bracketCount--;
                        } else if (currentChar == '(') {
                            bracketCount++;
                        }
                        if (bracketCount == 0) {
                            break;
                        }
                    }
                    if (currentChar != ')') {
                        String errorMessage = createErrorMessage(sourceChars, this.readIndex);
                        throw new ExpressionException("Expression syntax error, pos: " + this.readIndex + ",Expression[... " + errorMessage + " ...], end token ')' not found !");
                    }
                    String args = new String(sourceChars, localOffset + this.offset, this.readIndex - localOffset - 1);
                    if (oneLevelAccess) {
                        // static Function
                        // @see @function
                        this.tokenType = FUN_TOKEN;
                        checkTokenSyntaxError();
                        parseFunToken(exprParserContext, identifierValue, args);
                    } else {
                        // reflect invoke : obj.method(...args)
                        this.tokenType = FUN_TOKEN;
                        checkTokenSyntaxError();
                        parseMethodToken(exprParserContext, variableKeys, identifierValue, args);
                        // String errorMessage = createErrorMessage(sourceChars, this.readIndex);
                        // throw new ExpressionException("Expression syntax error, pos: " + this.readIndex + ", Expression[... " + errorMessage + " ...],non static function token() is not supported temporarily !");
                    }

                } else {
                    if (localOffset > start && readIndex > localOffset) {
                        variableKeys.add(identifierValue);
                    }
                    this.tokenType = IDENTIFIER_TOKEN; //设置标记类型为标识符token
                    // todo 检查是否内置关键字（目前存在问题）
                    // this.checkIfBuiltInKeywords();
                    checkTokenSyntaxError();
                    parseVarToken(exprParserContext, startChar, oneLevelAccess ? identifierValue : null, variableKeys);
                }
            } finally {
                variableKeys.clear();
            }
            return;
        } else if (currentChar == '!') {
            ++this.readIndex;
            // 非运算与负数运算处理方式一致
            this.tokenType = NOT_TOKEN;
            parseNotToken(exprParserContext);
            return;
        } else if (currentChar == '\'') {
            int start = ++this.readIndex;
            this.scanString();
            // 最后一个字符属于token范围内字符需要++
            // 标记为常量
            this.tokenType = STR_TOKEN;
            checkTokenSyntaxError();
            parseStrToken(exprParserContext, new String(sourceChars, this.offset + start, readIndex - start));
            ++this.readIndex;
            return;
        } else if (currentChar == '{') {
            // 静态数组（只支持number和字符串两种，不支持嵌套数组）
            // java中静态数组使用{}而不是[]
            // 只能在解释模式下运行,编译模式java只支持声明时使用，运算不支持
            int start = ++this.readIndex;
            while (readable() && (currentChar = read()) != '}') {
                ++this.readIndex;
                if (this.readIndex >= length()) {
                    break;
                }
            }
            if (currentChar != '}') {
                String errorMessage = createErrorMessage(sourceChars, this.readIndex);
                throw new ExpressionException("Expression syntax error, pos: " + this.readIndex + ", Expression[... " + errorMessage + " ...],未找到与开始字符'{'相匹配的结束字符 '}'");
            }
            this.tokenType = ARR_TOKEN;
            checkTokenSyntaxError();
            parseArrToken(exprParserContext, new String(sourceChars, start + this.offset, this.readIndex - start));
            ++this.readIndex;
            return;
        } else if (currentChar == '@') {
            // 函数token(解析函数内容)
            int start = ++this.readIndex;
            // 校验@后紧跟的标识符是否合法
            if (readable()) {
                ++this.readIndex;
                // 以java标识符开头
                if (!Character.isJavaIdentifierStart(currentChar = read())) {
                    String errorMessage = createErrorMessage(sourceChars, this.readIndex);
                    throw new ExpressionException("Expression syntax error, pos: " + this.readIndex + ", Expression[... " + errorMessage + " ...], unexpected function start char  : '" + currentChar + "'");
                }
            }
            // 查找'('前面的字符
            while (readable() && isIdentifierAppend(currentChar = read())) {
                ++this.readIndex;
                if (this.readIndex >= length()) {
                    break;
                }
            }
            // 清除空白
            while (readable() && isWhitespace(currentChar = read())) {
                ++this.readIndex;
            }

            // 查找'('
            if (currentChar != '(') {
                String readSource = this.exprSource.substring(0, this.readIndex);
                throw new ExpressionException("Expression syntax error, pos: " + this.readIndex + ", source: '" + readSource + "' function start symbol '(' not found !");
            }
            String functionName = new String(sourceChars, start + this.offset, this.readIndex - start);
            start = ++this.readIndex;

            // 查找结束的')'
            int bracketCount = 1;
            while (readable()) {
                currentChar = read();
                if (currentChar == '\'') {
                    // find end ',ignore
                    this.scanString();
                    ++this.readIndex;
                    continue;
                }
                ++this.readIndex;
                if (currentChar == ')') {
                    bracketCount--;
                } else if (currentChar == '(') {
                    bracketCount++;
                }
                if (bracketCount == 0) {
                    break;
                }
            }
            String args = new String(sourceChars, start + this.offset, this.readIndex - start - 1);
            this.tokenType = FUN_TOKEN;
            checkTokenSyntaxError();
            parseFunToken(exprParserContext, functionName, args);
            return;
        } else {
            // 无法识别的字符
            String errorMessage = createErrorMessage(sourceChars, this.readIndex);
            throw new ExpressionException("Expression syntax error, pos: " + this.readIndex + ", Expression[... " + errorMessage + " ...], 表达式中未预期出现的token字符 '" + currentChar + "'");
        }
    }

    // 读取字符串最后一个"'"
    private void scanString() {
        char currentChar = 0;
        ++this.readIndex;
        char prevCh = 0;
        while (readable() && ((currentChar = read()) != '\'' || prevCh == '\\')) {
            ++this.readIndex;
            prevCh = currentChar;
        }
        if (currentChar != '\'' || prevCh == '\\') {
            String errorMessage = createErrorMessage(sourceChars, this.readIndex);
            throw new ExpressionException("Expression syntax error, pos: " + this.readIndex + ", Expression[... " + errorMessage + " ...],未找到与开始字符'\''相匹配的结束字符 '\''");
        }
    }

    private boolean readable() {
        return this.readIndex < count;
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

    private void checkIfBuiltInKeywords(String varValue) {
        if (varValue == null) return;
        int length = varValue.length();
        if (length == 2) {
            char charFir = varValue.charAt(0);
            char charSec = varValue.charAt(1);
            if ((charFir == 'i' && charSec == 'n') || (charFir == 'I' && charSec == 'N')) {
                // in(IN)语法归纳到操作符于&&,||, != 同级别
                // 判断左边的对象是否为右边集合的中元素或者属性,或者map的key
                this.opsType = 63;
                this.opsLevel = 600;
                this.tokenType = OPS_TOKEN;
            }
        } else if (length == 3) {
            if (varValue.indexOf("out") == 0) {
                // out指令 实际上是in操作取反
                this.opsType = 64;
                this.opsLevel = 600;
                this.tokenType = OPS_TOKEN;
            }
        }
    }

    private int getTokenTypeGroup(int tokenType) {
        switch (tokenType) {
            case OPS_TOKEN:
                return GROUP_TOKEN_OPS;
            case NUM_TOKEN:
            case STR_TOKEN:
            case IDENTIFIER_TOKEN:
            case ARR_TOKEN:
                return GROUP_TOKEN_VALUE;
            default:
                return 0;
        }
    }

    private void checkTokenSyntaxError() {
        // 暂时校验最近两个token类型一定不能一致
        int preGroupValue = getTokenTypeGroup(this.prevTokenType);
        int groupValue = getTokenTypeGroup(this.tokenType);
        if (preGroupValue == groupValue) {
            if (preGroupValue == GROUP_TOKEN_OPS) {
                String readSource = createErrorMessage(sourceChars, this.readIndex);
                throw new ExpressionException("Expression syntax error, pos: " + this.readIndex + ", duplicate operation type: '" + this.errorMsg + "', Expression[" + readSource + "]");
            } else if (preGroupValue == GROUP_TOKEN_VALUE) {
                String readSource = createErrorMessage(sourceChars, this.readIndex);
                throw new ExpressionException("Expression syntax error, pos: " + this.readIndex + ", missing operation symbol: '" + this.errorMsg + "', Expression[" + readSource + "]");
            }
        } else {
            // value token before is )
            if (groupValue == GROUP_TOKEN_VALUE && this.prevTokenType == BRACKET_END_TOKEN) {
                String readSource = createErrorMessage(sourceChars, this.readIndex);
                throw new ExpressionException("Expression syntax error, pos: " + this.readIndex + ", unexpected symbol : '" + this.errorMsg + "', Expression[" + readSource + "]");
            }
            // // ops token before is (
            if (groupValue == GROUP_TOKEN_OPS && this.prevTokenType == BRACKET_TOKEN) {
                String readSource = createErrorMessage(sourceChars, this.readIndex);
                throw new ExpressionException("Expression syntax error, pos: " + this.readIndex + ", unexpected symbol : '" + this.errorMsg + "', Expression[" + readSource + "]");
            }
        }
    }

    // check bracket ) before
    // expected value type,  unexpected ( && ops ,
    private void checkBeforeBracketEndTokenSyntaxError() {
        int preGroupValue = getTokenTypeGroup(this.prevTokenType);
        if (this.prevTokenType == BRACKET_TOKEN || preGroupValue == GROUP_TOKEN_OPS) {
            String readSource = createErrorMessage(sourceChars, this.readIndex);
            throw new ExpressionException("Expression syntax error, pos: " + this.readIndex + ",unexpected symbol : '" + this.errorMsg + "', Expression[" + readSource + "]");
        }
    }

    // check bracket ( before
    // expected ops type,  unexpected ) && value ,
    private void checkBeforeBracketTokenSyntaxError() {
        int preGroupValue = getTokenTypeGroup(this.prevTokenType);
        if (this.prevTokenType == BRACKET_END_TOKEN && preGroupValue == GROUP_TOKEN_VALUE) {
            String readSource = createErrorMessage(sourceChars, this.readIndex);
            throw new ExpressionException("Expression syntax error, pos: " + this.readIndex + ",unexpected symbol : '" + this.errorMsg + "', Expression[" + readSource + "]");
        }
    }

    private void throwOperationNotSupported() {
        String readSource = createErrorMessage(sourceChars, this.readIndex);
        throw new ExpressionException("Expression syntax error, pos: " + this.readIndex + ", unexpected symbol '" + this.errorMsg + "', Expression[" + readSource + "]");
    }

    private void throwNumberFormatException(int start) {
        String numberSource = new String(sourceChars, this.offset + start, this.readIndex - start);
        String readSource = createErrorMessage(sourceChars, this.readIndex);
        throw new ExpressionException("Expression syntax error, pos: " + this.readIndex + ", invalid number text '" + numberSource + "', Expression[" + readSource + "]");
    }

    private boolean isWhitespace(char c) {
        return c <= ' ';
    }

    // 解析模式除了数字和.开头
    protected boolean isIdentifierStart(char c) {
        return c == '_' || c == '$' /*|| c == '#'*/ || Character.isLetter(c);
    }

    // 解析模式下支持 _ $ . 字母,数字等
    protected boolean isIdentifierAppend(char c) {
        return c == '_' || c == '$' || c == '.' /*|| c == '#'*/ || Character.isLetter(c) || isDigit(c);
    }

    // 是否变量表达式字符
    protected boolean isVariableAppend(char c) {
        if (Character.isLetter(c)) return true;
        if (c <= ' ') return false;
        return c == '.' || c == '_' || c == '$' || isDigit(c) || c == '[' /*|| c == '('*/;
    }

    private static int digit(char c, int numberRadix) {
        switch (c) {
            case '0':
            case '1':
            case '2':
            case '3':
            case '4':
            case '5':
            case '6':
            case '7':
            case '8':
            case '9':
                return c - '0';
            default: {
                if (numberRadix == 16) {
                    if (c >= 'a' && c <= 'f') {
                        return c - ('a' - 10);
                    }
                    if (c >= 'A' && c <= 'F') {
                        return c - ('A' - 10);
                    }
                }
                return -1;
            }
        }
    }

    /**
     * 判断是否为10进制 0-9
     */
    private boolean isDigit(char c) {
        return c >= 48 && c <= 57;
    }

    /**
     * 10进制数字开头
     *
     * @param c
     * @return
     */
    private boolean isDecimalDigitStart(char c) {
        switch (c) {
            case '0':
            case '1':
            case '2':
            case '3':
            case '4':
            case '5':
            case '6':
            case '7':
            case '8':
            case '9':
            case '.':
                return true;
            default:
                return false;
        }
    }

    /**
     * 判断是否为{numberRadix}进制有效的字符 0-9 . E/e
     */
    private boolean isAvailableDigit(char c, int numberRadix) {
        if (numberRadix == 10) {
            switch (c) {
                case '0':
                case '1':
                case '2':
                case '3':
                case '4':
                case '5':
                case '6':
                case '7':
                case '8':
                case '9':
                case '.':
                case '-':
                case 'E':
                case 'e':
                case 'D':
                case 'd':
                case 'F':
                case 'f':
                case 'L':
                case 'l':
                    return true;
                default:
                    return false;
            }
//            return (c >= 48 && c <= 57) || c == '.' || c == 'E' || c == 'e' || c == '-' || c == 'D' || c == 'd';
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
                // 非运算暂时级别定位5仅仅次于括号运算（1）
//                this.opsLevel = 5;
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
        return exprEvaluator.evaluate(EvaluatorContext.Empty, EvaluateEnvironment.DefaultEnvironment);
    }

    public Object evaluate(Object context) {
        if (context instanceof EvaluateEnvironment) {
            return evaluate((EvaluateEnvironment) context);
        }
        if (context instanceof Map) {
            return evaluate((Map) context);
        }
        // from pojo entity
        return evaluate(context, EvaluateEnvironment.DefaultEnvironment);
    }

    public Object evaluate(Map context) {
        EvaluateEnvironment defaultEnvironment = EvaluateEnvironment.DefaultEnvironment;
        EvaluatorContext evaluatorContext = defaultEnvironment.createEvaluateContext(context, chainInvoker);
        return doEvaluate(evaluatorContext, defaultEnvironment);
    }

    @Override
    public Object evaluate(EvaluateEnvironment evaluateEnvironment) {
        if (evaluateEnvironment == null) {
            return exprEvaluator.evaluate(EvaluatorContext.Empty, EvaluateEnvironment.DefaultEnvironment);
        } else {
            EvaluatorContext evaluatorContext = evaluateEnvironment.createEvaluateContext(chainInvoker, variableCount);
            return doEvaluate(evaluatorContext, evaluateEnvironment);
        }
    }

    // evaluate entrance
    Object evaluate(Object context, EvaluateEnvironment evaluateEnvironment) {
        EvaluatorContext evaluatorContext = evaluateEnvironment.createEvaluateContext(context, chainInvoker);
        return doEvaluate(evaluatorContext, evaluateEnvironment);
    }

    // evaluate entrance
    Object doEvaluate(EvaluatorContext evaluatorContext, EvaluateEnvironment evaluateEnvironment) {
        try {
            Object result = exprEvaluator.evaluate(evaluatorContext, evaluateEnvironment);
            if (!compressed) {
                if (cntForCompress.getAndIncrement() == 1) {
                    compressEvaluator();
                }
            }
            return result;
        } finally {
            clearContextVariables(evaluatorContext);
        }
    }

    void clearContextVariables(EvaluatorContext evaluatorContext) {
        evaluatorContext.clearContextVariables(chainValues);
    }

    protected void compressEvaluator() {
        this.exprEvaluator = compressEvaluator(exprEvaluator);
        this.compressed = true;
    }

    private ExprEvaluator compressEvaluator(ExprEvaluator exprEvaluator) {
        if (exprEvaluator.isStatic) {
            if (exprEvaluator instanceof ExprEvaluator.ExprEvaluatorConstantImpl) {
                return exprEvaluator;
            }
            return new ExprEvaluator.ExprEvaluatorConstantImpl(exprEvaluator.result);
        }

        if (exprEvaluator instanceof ExprEvaluator.ExprEvaluatorStackSplitImpl) {
            ExprEvaluator.ExprEvaluatorStackSplitImpl stackSplit = (ExprEvaluator.ExprEvaluatorStackSplitImpl) exprEvaluator;
            stackSplit.front = compressEvaluator(stackSplit.front);
            stackSplit.left = compressEvaluator(stackSplit.left);
            return stackSplit;
        }

        if (exprEvaluator instanceof ExprEvaluator.ExprEvaluatorContextValueHolderImpl) {
            return exprEvaluator;
        }

        ExprEvaluator left = exprEvaluator.left;
        ExprEvaluator right = exprEvaluator.right;
        int evalType = exprEvaluator.getEvalType();
        if (evalType == 0) {
            return compressEvaluator(left);
        } else if (evalType == 1) {
            left = compressEvaluator(left);
            if (right == null) {
                return left;
            }
            right = compressEvaluator(right);
            int opsType = exprEvaluator.opsType;
            if (opsType == 71) {
                return ExprEvaluator.ExprEvaluatorTernaryImpl.of(exprEvaluator.update(left, right));
            }
            switch (opsType) {
                case 1:
                    // *乘法
                    return ExprEvaluator.ExprEvaluatorMultiplyImpl.of(exprEvaluator.update(left, right));
                case 2:
                    // /除法
                    return ExprEvaluator.ExprEvaluatorDivisionImpl.of(exprEvaluator.update(left, right));
                case 3:
                    // %取余数
                    return ExprEvaluator.ExprEvaluatorModulusImpl.of(exprEvaluator.update(left, right));
                case 4:
                    // **指数,平方/根
                    return ExprEvaluator.ExprEvaluatorPowerImpl.of(exprEvaluator.update(left, right));
                case 11:
                    return ExprEvaluator.ExprEvaluatorPlusImpl.of(exprEvaluator.update(left, right));
                case 12:
                    // -减法（理论上代码不可达） 因为'-'统一转化为了'+'(负)
                    return ExprEvaluator.ExprEvaluatorMinusImpl.of(exprEvaluator.update(left, right));
                case 21:
                    // >> 位运算右移
                    return ExprEvaluator.ExprEvaluatorBitRightImpl.of(exprEvaluator.update(left, right));
                case 22:
                    // << 位运算左移
                    return ExprEvaluator.ExprEvaluatorBitLeftImpl.of(exprEvaluator.update(left, right));
                case 31:
                    // &
                    return ExprEvaluator.ExprEvaluatorBitAndImpl.of(exprEvaluator.update(left, right));
                case 32:
                    // ^
                    return ExprEvaluator.ExprEvaluatorBitXorImpl.of(exprEvaluator.update(left, right));
                case 33:
                    // |
                    return ExprEvaluator.ExprEvaluatorBitOrImpl.of(exprEvaluator.update(left, right));
                case 51:
                    // >
                    return ExprEvaluator.ExprEvaluatorGtImpl.of(exprEvaluator.update(left, right));
                case 52:
                    // <
                    return ExprEvaluator.ExprEvaluatorLtImpl.of(exprEvaluator.update(left, right));
                case 53:
                    // ==
                    return ExprEvaluator.ExprEvaluatorEqualImpl.of(exprEvaluator.update(left, right));
                case 54:
                    // >=
                    return ExprEvaluator.ExprEvaluatorGEImpl.of(exprEvaluator.update(left, right));
                case 55:
                    // <=
                    return ExprEvaluator.ExprEvaluatorLEImpl.of(exprEvaluator.update(left, right));
                case 56:
                    // !=
                    return ExprEvaluator.ExprEvaluatorNEImpl.of(exprEvaluator.update(left, right));
                case 61:
                    // &&
                    return ExprEvaluator.ExprEvaluatorLogicalAndImpl.of(exprEvaluator.update(left, right));
                case 62:
                    // ||
                    return ExprEvaluator.ExprEvaluatorLogicalOrImpl.of(exprEvaluator.update(left, right));
                case 63:
                    // in
                    return ExprEvaluator.ExprEvaluatorInImpl.of(exprEvaluator.update(left, right));
                case 64:
                    // out
                    return ExprEvaluator.ExprEvaluatorOutImpl.of(exprEvaluator.update(left, right));
                case 70:
                    // : 三目运算结果, 不支持单独运算
                case 71:
                default:
                    // ? 三目运算条件
                    return exprEvaluator.update(left, right);
            }
            // 默认返回null
        } else if (evalType == 5) {
            // 括号运算
            return ExprEvaluator.ExprEvaluatorBracketImpl.of(exprEvaluator.update(left, compressEvaluator(right)));
        } else if (evalType == 6) {
            // 变量运算
            return ((ExprEvaluator.ExprEvaluatorVariableImpl) exprEvaluator).internKey();
        } else if (evalType == 9) {
            // 函数运算
            return exprEvaluator;
        } else {
            // 其他统一返回left
            return left == null ? null : compressEvaluator(left);
        }
    }

    protected static String createErrorMessage(char[] buf, int at) {
        try {
            int len = buf.length;
            char[] text = new char[40];
            int count;
            int begin = Math.max(at - 18, 0);
            System.arraycopy(buf, begin, text, 0, count = at - begin);
            text[count++] = '^';
            int end = Math.min(len, at + 18);
            System.arraycopy(buf, at, text, count, end - at);
            count += end - at;
            return new String(text, 0, count);
        } catch (Throwable throwable) {
            return "";
        }
    }

    public void optimize() {
        this.exprEvaluator = exprEvaluator.optimize();
    }
}