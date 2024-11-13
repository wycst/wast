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

import io.github.wycst.wast.common.utils.NumberUtils;

import java.math.BigDecimal;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 表达式解析器,生成执行模型
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

    ExprParser(String exprSource, int offset) {
        this.init(exprSource, offset, exprSource.length() - offset);
        this.findMode = true;
        this.findEndIndex = exprSource.length();
        this.parse();
    }

    // 空解析器
    protected ExprParser() {
    }

    public static final int GROUP_TOKEN_OPS = 1;
    public static final int GROUP_TOKEN_VALUE = 2;

    public static final int RESET_TOKEN = 1;
    public static final int OPS_TOKEN = 2;

    public static final int NEGATE_TOKEN = 5;
    public static final int NOT_TOKEN = 6;
    public static final int BRACKET_TOKEN = 9;
    public static final int NUM_TOKEN = 10;
    // 字符串
    public static final int STR_TOKEN = 11;
    public static final int VAR_TOKEN = 12;
    public static final int ARR_TOKEN = 13;
    // 函数(method)token,以@开始并且紧跟java标识符（@fun(...arg0)或者bean.fun(...arg0)）
    public static final int FUN_TOKEN = 14;
    public static final int BRACKET_END_TOKEN = 20;
    // 最大超过token数量时开启优化
    static final int MAX_OPTIMIZE_COUNT = (ExprEvaluator.OPTIMIZE_DEPTH_VALUE << 1) + 1;

    // 表达式源
    private String exprSource;
    private char[] sourceChars;
    private int offset;
    private int count;
    private int readIndex;
    private boolean findMode;
    private int findEndIndex;

    private String errorMsg;
    private Map<String, ElVariableInvoker> invokes;
    private Map<String, ElVariableInvoker> tailInvokes;

    protected ElInvoker tailChainInvoker;
    // protected Invoker chainValues;
    protected int variableCount;
    protected EvaluatorContext evaluatorContext;

    // 标记类型
    private int prevTokenType;
    private int tokenType = RESET_TOKEN;

    // 操作类型
    private ElOperator operator;
    private int bracketCount;

    // 数量
    private int evaluatorCount;

    // 表达式解析器
    private ExprEvaluator exprEvaluator = createExprEvaluator();
    // 上下文
    private ExprParserContext parserContext = new ExprParserContext();

    private final AtomicInteger cntForCompress = new AtomicInteger(0);
    private boolean compressed = false;

    // global instance
    protected ExprParser global() {
        return this;
    }

    protected Map<String, ElVariableInvoker> getInvokes() {
        return invokes;
    }

    protected Map<String, ElVariableInvoker> getTailInvokes() {
        return tailInvokes;
    }

    protected ExprEvaluator createExprEvaluator() {
        return new ExprEvaluator();
    }

    public String getSource() {
        if (findMode) {
            return new String(sourceChars, offset, findEndIndex - offset);
        }
        if (offset == 0 && count == sourceChars.length) {
            return this.exprSource;
        }
        return new String(sourceChars, offset, count);
    }

    public int length() {
        return count;
    }

    public final int findIndex() {
        return findMode ? findEndIndex : -1;
    }

    protected ExprEvaluator getEvaluator() {
        return exprEvaluator;
    }

    protected final void init(String exprSource) {
        init(exprSource, 0, exprSource.length());
    }

    protected final void init(String exprSource, int offset, int count) {
        this.exprSource = exprSource;
        this.sourceChars = getChars(exprSource);
        this.offset = offset;
        this.count = count;
    }

    protected final void init(char[] buffers, int offset, int count) {
        this.sourceChars = buffers;
        this.offset = offset;
        this.count = count;
    }

    /**
     * 解析表达式字符串
     */
    protected final void parse() {
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
        validate();
    }

    private void checkOptimizeRequired() {
        if (evaluatorCount > MAX_OPTIMIZE_COUNT) {
            optimize();
        }
    }

    private void validate() {
        if (exprEvaluator == null) {
            throw new ExpressionException("syntax error, input maybe empty");
        }
    }

    private void parseEvaluator() {
        parserContext.setContext(exprEvaluator, false, false);
        do {
            parseNext(parserContext);
            ++evaluatorCount;
        } while (readable());
        checkEndTokenSyntaxError(tokenType);
    }

    private void checkEndTokenSyntaxError(int tokenType) {
        if (tokenType == OPS_TOKEN) {
            String errorMessage = createErrorContextText(sourceChars, this.readIndex);
            throw new ExpressionException("syntax error, pos: " + this.readIndex + ", unsupported operator ends, Expression[... " + errorMessage + " ]");
        }
    }

    private void merge() {
        if (exprEvaluator.evalType == 0) {
            exprEvaluator = exprEvaluator.left;
        } else {
            mergeLast(exprEvaluator);
        }
    }

    final static void mergeLast(ExprEvaluator root) {
        ExprEvaluator rr = root.right;
        if (rr != null && rr.evalType == 0 && rr.left != null) {
            root.right = rr.left;
        }
    }

    // 优先级置换
    final void displacement(ExprEvaluator exprEvaluator) {
        // 1
        displacementChain(exprEvaluator);
        // 2
        displacementChain(exprEvaluator);
    }

    private void displacementChain(ExprEvaluator exprEvaluator) {
        parserContext.exprEvaluator = exprEvaluator;
        do {
            displacementSplit(parserContext);
        } while (parserContext.exprEvaluator != null);
    }

    final void displacementSplit(ExprParserContext exprParserContext) {
        ExprEvaluator exprEvaluator = exprParserContext.exprEvaluator;

        if (exprEvaluator == null) {
            return;
        }
        ElOperator exprOperator = exprEvaluator.operator;
        int level = exprOperator.level;

        int evalType = exprEvaluator.getEvalType();
        ExprEvaluator left = exprEvaluator.getLeft();

        ExprEvaluator right = exprEvaluator.getRight();
        if (right == null) {
            exprParserContext.exprEvaluator = null;
            return;
        }

        // 三目运算冒号（：）特殊处理
        if (exprOperator == ElOperator.COLON) {
            displacement(right);
            exprParserContext.exprEvaluator = null;
            return;
        }

        int rLevel = right.operator.level;
        if (rLevel <= 0) {
            exprParserContext.exprEvaluator = null;
            return;
        }
        if (level > rLevel) {
            // 发现了右侧的优先级更高,触发右侧合并
            mergeRight(right, level);
            rLevel = right.operator.level;
        }

        int rEvalType = right.getEvalType();
        ExprEvaluator rLeft = right.getLeft();
        ExprEvaluator rRight = right.getRight();

        if (level <= rLevel) {
            // 如果左侧的优先级级别高（level小）于右侧就开启轮换
            ExprEvaluator newLeft = createExprEvaluator();
            newLeft.setEvalType(evalType);
            newLeft.setOperator(exprOperator);
            newLeft.setNegate(exprEvaluator.negate);
            newLeft.setLogicalNot(exprEvaluator.logicalNot);
            newLeft.setLeft(left);
            newLeft.setRight(rLeft);

            exprEvaluator.setOperator(right.operator);
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

    final void mergeRight(ExprEvaluator exprEvaluator, int targetLevel) {
        if (exprEvaluator == null) {
            return;
        }
        int evalType = exprEvaluator.getEvalType();
        int level = exprEvaluator.operator.level;

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
        if (exprEvaluator.operator == ElOperator.COLON) {
            displacement(right);
            return;
        }

        int rLevel = right.operator.level;
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
            newLeft.setOperator(exprEvaluator.operator);
            newLeft.setNegate(exprEvaluator.negate);
            newLeft.setLogicalNot(exprEvaluator.logicalNot);
            newLeft.setLeft(left);
            newLeft.setRight(rLeft);

            exprEvaluator.setOperator(right.operator);
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
            // chainValues = ChainVariableInvoker.build(invokes, true);
            variableCount = invokes.size();
            if (tailInvokes.size() == 1) {
                // single value
                // tailChainInvoker = tailInvokes.values().iterator().next();
                evaluatorContext = new EvaluatorContext.SingleVariableImpl();
            } else {
                //            int index = 0;
                boolean sibbingMode = true;
                ElVariableInvoker tailParent = null;
                for (ElVariableInvoker variableInvoker : invokes.values()) {
//                variableInvoker.setIndex(index++);
                    if (variableInvoker.isTail()) {
                        if (sibbingMode) {
                            if (variableInvoker.parent == null) {
                                sibbingMode = false;
                            } else {
                                if (tailParent == null) {
                                    tailParent = variableInvoker.parent;
                                } else {
                                    if (tailParent != variableInvoker.parent) {
                                        sibbingMode = false;
                                    }
                                }
                            }
                        }
                    }
                }
                tailChainInvoker = ElChainVariableInvoker.buildTailChainInvoker(tailInvokes);
                if (sibbingMode) {
                    if (tailChainInvoker.size() == 2) {
                        ElChainVariableInvoker chainVariableInvoker = (ElChainVariableInvoker) tailChainInvoker;
                        evaluatorContext = new EvaluatorContext.TwinsImpl(tailParent, chainVariableInvoker.variableInvoke, chainVariableInvoker.next.variableInvoke);
                    } else {
                        evaluatorContext = new EvaluatorContext.SibbingVariablesImpl(tailParent);
                    }
                } else {
                    evaluatorContext = new EvaluatorContext();
                }
            }
        }
    }

    public final Collection<ElVariableInvoker> getTailVariableInvokers() {
        return tailInvokes == null ? new ArrayList<ElVariableInvoker>() : new ArrayList<ElVariableInvoker>(tailInvokes.values());
    }

    @Override
    public final List<String> getVariables() {
        if (variableCount > 0) {
            return new ArrayList<String>(tailInvokes.keySet());
        } else {
            return new ArrayList<String>();
        }
    }

    @Override
    public final List<String> getRootVariables() {
        if (variableCount > 0) {
            List<String> variables = new ArrayList<String>();
            Set<Map.Entry<String, ElVariableInvoker>> entrySet = invokes.entrySet();
            for (Map.Entry<String, ElVariableInvoker> entry : entrySet) {
                if(entry.getValue().parent == null) {
                    variables.add(entry.getKey());
                }
            }
            return variables;
        } else {
            return new ArrayList<String>();
        }
    }

    private void parseOpsToken(ExprParserContext exprParserContext) {
        ExprEvaluator evaluator = exprParserContext.exprEvaluator;
        boolean negate = exprParserContext.negate;
        boolean logicalNot = exprParserContext.logicalNot;
        evaluator.setEvalType(ExprEvaluator.EVAL_TYPE_OPERATOR);
        ExprEvaluator right = createExprEvaluator();
        if (operator == ElOperator.MINUS) {
            evaluator.setOperator(ElOperator.PLUS);
            negate = true;
        } else {
            evaluator.setOperator(operator);
        }
        evaluator.setRight(right);
        exprParserContext.setContext(right, negate, logicalNot);
    }

    private void parseVarToken(ExprParserContext exprParserContext, char startChar, String identifierValue, List<String> variableKeys) {
        ExprEvaluator evaluator = exprParserContext.exprEvaluator;
        boolean negate = exprParserContext.negate;
        boolean logicalNot = exprParserContext.logicalNot;
        if (identifierValue != null) {
            switch (startChar) {
                case 't': {
                    if ("true".equals(identifierValue)) {
                        ExprEvaluator.ConstantImpl left = new ExprEvaluator.ConstantImpl(!logicalNot);
                        evaluator.setLeft(left);
                        exprParserContext.setContext(evaluator, false, false);
                        return;
                    }
                    break;
                }
                case 'f': {
                    if ("false".equals(identifierValue)) {
                        ExprEvaluator.ConstantImpl left = new ExprEvaluator.ConstantImpl(logicalNot);
                        evaluator.setLeft(left);
                        exprParserContext.setContext(evaluator, false, false);
                        return;
                    }
                    break;
                }
                case 'n': {
                    if ("null".equals(identifierValue)) {
                        ExprEvaluator.ConstantImpl left = new ExprEvaluator.ConstantImpl(null);
                        evaluator.setLeft(left);
                        exprParserContext.setContext(evaluator, false, false);
                        return;
                    }
                    break;
                }
            }
        }
        // 检查是否已初始化
        checkInitializedInvokes();
        // 变量
        ExprEvaluator.VariableImpl left = new ExprEvaluator.VariableImpl();
        left.setNegate(negate);
        left.setLogicalNot(logicalNot);

        ElVariableInvoker variableInvoker = identifierValue == null ? ElVariableUtils.build(variableKeys, getInvokes(), getTailInvokes()) : ElVariableUtils.buildRoot(identifierValue, getInvokes(), getTailInvokes());
        global().tailChainInvoker = variableInvoker;
        left.setVariableInvoker(variableInvoker);
        evaluator.setLeft(left);
        exprParserContext.setContext(evaluator, false, false);
    }

    // 检查是否已初始化Invokes，如果没有进行初始化
    void checkInitializedInvokes() {
        if (invokes == null) {
            invokes = new HashMap<String, ElVariableInvoker>();
            tailInvokes = new HashMap<String, ElVariableInvoker>();
        }
    }

    final static void parseNumToken(ExprParserContext exprParserContext, Number numberValue) {
        ExprEvaluator evaluator = exprParserContext.exprEvaluator;
        ExprEvaluator.ConstantImpl left = new ExprEvaluator.ConstantImpl(numberValue);
        evaluator.setLeft(left);
        exprParserContext.setContext(evaluator, false, false);
    }

    final void parseBracketToken(ExprParserContext exprParserContext) {
        ExprEvaluator evaluator = exprParserContext.exprEvaluator;
        boolean negate = exprParserContext.negate;
        boolean logicalNot = exprParserContext.logicalNot;

        // 括号子执行器
        ExprEvaluator bracketChild = createExprEvaluator();

        // 循环解析不适合处理括号嵌套问题
        // 使用递归解析
        ExprParserContext bracketParserContext = new ExprParserContext(bracketChild, false, false);
        do {
            parseNext(bracketParserContext);
        } while (readable() && !bracketParserContext.endFlag);
        // bracketParserContext的endFlag设置为true时,当前递归栈结束；
        // 解析类型如果没有结束括号说明表达式错误
        if (!bracketParserContext.endFlag) {
            String errorMessage = createErrorContextText(sourceChars, this.readIndex);
            throw new ExpressionException("syntax error, pos: " + this.readIndex + ", missing closing symbol ')'" + ", Expression[... " + errorMessage + " ]");
        }
        //  置换子表达式优先级
        //  displacementSplit(child);
        displacementChain(bracketChild);
        mergeLast(bracketChild);

//        ExprEvaluator right = createExprEvaluator();
//        evaluator.setEvalType(ExprEvaluator.EVAL_TYPE_BRACKET);
//        evaluator.setRight(right);
//
//        right.setLeft(bracketChild);
//        exprParserContext.setContext(right, false, false);

        ExprEvaluator bracketEvaluator = createExprEvaluator();
        bracketEvaluator.negate(negate).setLogicalNot(logicalNot);
        bracketEvaluator.setEvalType(ExprEvaluator.EVAL_TYPE_BRACKET);
        bracketEvaluator.setOperator(ElOperator.BRACKET);
        bracketEvaluator.setRight(bracketChild);
        evaluator.setLeft(bracketEvaluator);
        exprParserContext.setContext(evaluator, false, false);
    }

    final static void parseStrToken(ExprParserContext exprParserContext, String strValue) {
        ExprEvaluator evaluator = exprParserContext.exprEvaluator;
        boolean negate = exprParserContext.negate;
        boolean logicalNot = exprParserContext.logicalNot;

        // 字符串常量
        ExprEvaluator.ConstantImpl left = new ExprEvaluator.ConstantImpl(strValue);
        left.setNegate(negate);
        left.setLogicalNot(logicalNot);
        evaluator.setLeft(left);
        exprParserContext.setContext(evaluator, false, false);
    }

    final void parseArrToken(ExprParserContext exprParserContext, String arrStr) {
        ExprEvaluator evaluator = exprParserContext.exprEvaluator;
        boolean negate = exprParserContext.negate;
        boolean logicalNot = exprParserContext.logicalNot;

        // 静态数组
        ExprEvaluator left = createExprEvaluator();
        left.setNegate(negate);
        left.setLogicalNot(logicalNot);
        left.setArrayValue(arrStr);
        evaluator.setLeft(left);
        exprParserContext.setContext(evaluator, false, false);
    }

    final void parseFunToken(ExprParserContext exprParserContext, String functionName, String args) {
        ExprEvaluator evaluator = exprParserContext.exprEvaluator;
        boolean negate = exprParserContext.negate;
        boolean logicalNot = exprParserContext.logicalNot;

        ExprEvaluator.FunctionImpl left = new ExprEvaluator.FunctionImpl();
        left.setNegate(negate);
        left.setLogicalNot(logicalNot);
        left.setFunction(functionName, args, global());
        evaluator.setLeft(left);
        exprParserContext.setContext(evaluator, false, false);
    }

    final void parseMethodToken(ExprParserContext exprParserContext, List<String> variableKeys, String methodName, String args) {
        ExprEvaluator evaluator = exprParserContext.exprEvaluator;
        boolean negate = exprParserContext.negate;
        boolean logicalNot = exprParserContext.logicalNot;

        checkInitializedInvokes();
        ExprEvaluator.MethodImpl left = new ExprEvaluator.MethodImpl();
        left.setNegate(negate);
        left.setLogicalNot(logicalNot);

        ElVariableInvoker variableInvoker = ElVariableUtils.build(variableKeys, getInvokes(), getTailInvokes());
        global().tailChainInvoker = variableInvoker;

        left.setMethod(variableInvoker, methodName, args, global());
        evaluator.setLeft(left);
        exprParserContext.setContext(evaluator, false, false);
    }

    final static void parseNegateToken(ExprParserContext exprParserContext) {
        // 负数标记,上一个token必为操作符
        // 只做桥接作用,基于负负得正与上一个negate值互斥
        boolean negate = exprParserContext.negate;
        exprParserContext.negate = !negate;
    }

    final static void parseNotToken(ExprParserContext exprParserContext) {
        // 取非标记,上一个token必为操作符
        // 只做桥接作用,基于`非非得是`与上一个logicalNot值互斥
        boolean logicalNot = exprParserContext.logicalNot;
        exprParserContext.logicalNot = !logicalNot;
    }

    private final static ThreadLocal<List<String>> localVariableKeys = new ThreadLocal<List<String>>() {
        @Override
        protected List<String> initialValue() {
            return new ArrayList<String>();
        }
    };

    private void parseNext(ExprParserContext exprParserContext) {

        // reset
        this.resetToken();

        char currentChar = 0;
        // SKIP Whitespace
        while (readable() && isWhitespace(currentChar = read())) {
            ++this.readIndex;
        }

        if (isEnd()) {
            checkEndTokenSyntaxError(prevTokenType);
            return;
        }

        // Based on prediction rules, it seems more efficient than switch
        ElOperator elOperator;
        // Is it a minus sign operator
        boolean isMinusSymbol = false;
        if ((isDigit(currentChar) || (isMinusSymbol = currentChar == '-')) && prevTokenType < NUM_TOKEN) {
            final int begin = this.readIndex;
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
            // 10进制值
            long decimalVal = 0;
            boolean valInitSet = false;
            // 16进制或者8进制
            long hexOrOctVal = 0;
            // check 16进制/8进制/10进制(默认)
            if (firstDigitChar == '0') {
                int val;
                if (secondeDigitChar == 'x' || secondeDigitChar == 'X') {
                    numberRadix = 16;
                    // currentChar is already '0'
                    this.readIndex = readIndex;
                    ++cnt;
                    while (readable() && (val = digit(currentChar = read(), 16)) != -1) {
                        hexOrOctVal = (hexOrOctVal << 4) | val;
                        ++this.readIndex;
                        ++cnt;
                    }
                    // only supported long suffix sush as 0x123L
                    if (currentChar == 'l' || currentChar == 'L') {
                        ++this.readIndex;
                    }
                } else {
                    if (Character.isDigit(secondeDigitChar)) {
                        numberRadix = 8;
                        this.readIndex = readIndex;
                        hexOrOctVal = digit(secondeDigitChar, 8);
                        ++cnt;
                        while (readable() && (val = digit(currentChar = read(), 8)) != -1) {
                            hexOrOctVal = (hexOrOctVal << 3) | val;
                            ++this.readIndex;
                            ++cnt;
                        }
                        // 8进制如果出现小数点或者相关后缀，转10进制处理
                        switch (currentChar) {
                            case 'E':
                            case 'e':
                            case 'f':
                            case 'F':
                            case 'D':
                            case 'd':
                            case '.': {
                                numberRadix = 10;
                                // decimalVal = Integer.parseInt(Long.toString(hexOrOctVal, 8));
                                decimalVal = hexOrOctVal;
                                valInitSet = true;
                                break;
                            }
                            case 'L':
                            case 'l': {
                                ++this.readIndex;
                                break;
                            }
                        }
                    }
                }
            }

            // use double
            double value;
            int mode = 0;
            // supported number suffix such as 123.456d/12.05f/123456L
            int specifySuffix = 0;
            // 科学计数法指数值
            int expValue = 0;
            // 科学计数法指数是否为负数
            boolean expNegative = false;
            // 小数位数
            int decimalCount = 0;
            // 默认10进制
            if (numberRadix == 10) {
                if (!isMinusSymbol && !valInitSet) {
                    ++cnt;
                    decimalVal = currentChar - 48;
                }
                // 10进制
                while (readable() && isDigit(currentChar = read())) {
                    decimalVal = decimalVal * 10 + (currentChar - 48);
                    ++this.readIndex;
                    ++cnt;
                }
                // 小数点
                if (currentChar == '.') {
                    ++this.readIndex;
                    // 小数点模式
                    mode = 1;
                    // direct scan numbers
                    while (readable() && isDigit(currentChar = read())) {
                        decimalVal = decimalVal * 10 + currentChar - 48;
                        ++decimalCount;
                        ++cnt;
                        ++this.readIndex;
                    }
                }
                if (currentChar == 'E' || currentChar == 'e') {
                    mode = 2;
                    ++this.readIndex;
                    try {
                        currentChar = read();
                        if ((expNegative = currentChar == '-') || currentChar == '+') {
                            ++this.readIndex;
                            currentChar = read();
                        }
                        if (isDigit(currentChar)) {
                            expValue = currentChar - 48;
                            ++this.readIndex;
                            while (readable() && isDigit(currentChar = read())) {
                                expValue = (expValue << 3) + (expValue << 1) + currentChar - 48;
                                ++this.readIndex;
                            }
                        }
                    } catch (RuntimeException throwable) {
                        String errorMessage = createErrorContextText(sourceChars, this.readIndex);
                        throw new ExpressionException("syntax error, pos: " + this.readIndex + ", unexpected token '" + currentChar + "', Expression[... " + errorMessage + " ]");
                    }
                }
                switch (currentChar) {
                    case 'l':
                    case 'L': {
                        specifySuffix = 1;
                        ++this.readIndex;
                        break;
                    }
                    case 'f':
                    case 'F': {
                        specifySuffix = 2;
                        ++this.readIndex;
                        break;
                    }
                    case 'd':
                    case 'D': {
                        specifySuffix = 3;
                        ++this.readIndex;
                        break;
                    }
                    case '$': {
                        // BigDecimal
                        specifySuffix = 4;
                        ++this.readIndex;
                        break;
                    }
                }
                // end
                value = decimalVal;
            } else {
                // 16进制或者8进制值
                value = hexOrOctVal;
                decimalVal = hexOrOctVal;
            }
            Number numberValue;
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
                        this.operator = ElOperator.MINUS;
                        checkTokenSyntaxError();
                        parseOpsToken(exprParserContext);
                        return;
                    }
                }
            } else {
                this.tokenType = NUM_TOKEN;
                final boolean overflow = cnt > 19 || decimalVal < 0;
                final boolean negate = exprParserContext.negate ^ isMinusSymbol;
                if (mode == 0 && !overflow) {
                    if (negate) {
                        value = -value;
                    }
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
                    } else if (specifySuffix == 4) {
                        numberValue = BigDecimal.valueOf((long) value);
                    } else {
                        numberValue = value;
                    }
                } else {
                    do {
                        if (overflow) {
                            if (specifySuffix == 4) {
                                numberValue = createBigDecimal(begin, this.readIndex - 1, negate);
                                break;
                            }
                            decimalVal = 0;
                            cnt = 0;
                            int j = begin, decimalPointIndex = this.readIndex;
                            char c;
                            decimalCount = 0;
                            for (; j < this.readIndex; ++j) {
                                if (isDigit(c = sourceChars[offset + j])) {
                                    if (cnt++ < 18) {
                                        decimalVal = decimalVal * 10 + c - 48;
                                    }
                                    if (j > decimalPointIndex) {
                                        ++decimalCount;
                                    }
                                } else {
                                    if (c == '.') {
                                        decimalPointIndex = j;
                                    } else if (c == 'e' || c == 'E') {
                                        break;
                                    }
                                }
                                if (cnt >= 18 && decimalCount > 0) break;
                            }
                            decimalCount -= cnt - 18;
                        }
                        expValue = expNegative ? -expValue - decimalCount : expValue - decimalCount;
                        if (specifySuffix == 4) {
                            numberValue = BigDecimal.valueOf(negate ? -decimalVal : decimalVal, -expValue);
                            break;
                        }
                        value = NumberUtils.scientificToIEEEDouble(decimalVal, -expValue);
                        if (negate) {
                            value = -value;
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
                    } while (false);
                }
//                checkTokenSyntaxError();
                checkValueTokenSyntaxError();
                parseNumToken(exprParserContext, numberValue);
            }
        } else if (this.prevTokenType > OPS_TOKEN && (elOperator = getOperator(currentChar)) != null) {
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
                if (elOperator == ElOperator.MULTI) {
                    if (currentChar == '*') {
                        // 指数运算调整类型和优先级
                        this.operator = ElOperator.EXP;
                        ++this.readIndex;
                    }
                } else if (elOperator == ElOperator.AND) {
                    // &&
                    if (currentChar == '&') {
                        // 逻辑与： &&
                        ++this.readIndex;
                        this.operator = ElOperator.LOGICAL_AND;
                    }
                } else if (elOperator == ElOperator.OR) {
                    // ||
                    if (currentChar == '|') {
                        // 逻辑或： ||
                        ++this.readIndex;
                        this.operator = ElOperator.LOGICAL_OR;
                    }
                } else if (elOperator == ElOperator.NOT) {
                    // !搭配=使用
                    if (currentChar == '=') {
                        // 逻辑不等于 != 和 == >=优先级一致
                        ++this.readIndex;
                        this.operator = ElOperator.NE;
                    } else if (currentChar == '!') {
                        // 或者紧跟非操作符号!
                        this.tokenType = NOT_TOKEN;
                        checkTokenSyntaxError();
                        parseNotToken(exprParserContext);
                        return;
                    } else {
                        if (getOperator(currentChar) != null) {
                            this.errorMsg = String.valueOf(firstMatchChar) + currentChar;
                            throwSyntaxError();
                        }
                        this.tokenType = NOT_TOKEN;
                        checkTokenSyntaxError();
                        parseNotToken(exprParserContext);
                        return;
                    }
                } else if (elOperator == ElOperator.GT) {
                    // 只支持 >> >=
                    if (currentChar == '>') {
                        // 位移运算: >>
                        ++this.readIndex;
                        this.operator = ElOperator.BIT_RIGHT;
                    } else if (currentChar == '=') {
                        // 逻辑运算符： >=
                        ++this.readIndex;
                        this.operator = ElOperator.GE;
                    } else {
                        if (getOperator(currentChar) != null) {
                            this.errorMsg = String.valueOf(firstMatchChar) + currentChar;
                            throwSyntaxError();
                        }
                    }
                } else if (elOperator == ElOperator.LT) {
                    // 只支持 << <=
                    if (currentChar == '<') {
                        // 位移运算: <<
                        ++this.readIndex;
                        this.operator = ElOperator.BIT_LEFT;
                    } else if (currentChar == '=') {
                        // 逻辑运算符： <=
                        ++this.readIndex;
                        this.operator = ElOperator.LE;
                    } else {
                        // fix a<b bug when no whitespace
                        if (getOperator(currentChar) != null) {
                            this.errorMsg = String.valueOf(firstMatchChar) + currentChar;
                            throwSyntaxError();
                        }
                    }
                } else if (elOperator == ElOperator.EQ) {
                    if (currentChar == '=') {
                        // 逻辑==
                        ++this.readIndex;
                        this.operator = ElOperator.EQ;
                    } else {
                        // 暂时不支持赋值表达式
                        this.errorMsg = String.valueOf(firstMatchChar) + currentChar;
                        throwSyntaxError();
                    }
                }
            } else {
                // 2022-03-11 开始支持！运算
                if (/*opsSymbolIndex == 43 ||*/ elOperator == ElOperator.EQ) {
                    this.errorMsg = String.valueOf(firstMatchChar);
                    // 不支持=和！单独使用
                    // 后续实现！针对逻辑表达式的非运算
                    // throwSyntaxError();
                    // throwUnsupportedError();
                    String readSource = createErrorContextText(sourceChars, this.readIndex);
                    throw new ExpressionException("syntax error, pos: " + this.readIndex + ", '=' is not supported, please use '==' instead, Expression[" + readSource + "]");
                }
            }
//            checkTokenSyntaxError();
            checkOpsTokenSyntaxError();
            parseOpsToken(exprParserContext);
        } else if (isBracketSymbol(currentChar)) {
            this.operator = ElOperator.BRACKET;
            // 括号
            ++this.readIndex;
            if (currentChar == '(') {
                this.tokenType = BRACKET_TOKEN;
                ++bracketCount;
                checkBeforeBracketTokenSyntaxError();
                parseBracketToken(exprParserContext);
            } else {
                --bracketCount;
                this.tokenType = BRACKET_END_TOKEN;
                if (bracketCount < 0) {
                    this.errorMsg = ")";
                    throwSyntaxError();
                }
                checkBeforeBracketEndTokenSyntaxError();
                exprParserContext.endFlag = true;
            }
        } else if (isIdentifierStart(currentChar)) {
            if (prevTokenType >= NUM_TOKEN) {
                throwSyntaxError();
            } else {
                int start = this.readIndex;
                char startChar = currentChar;
                ++this.readIndex;

                int localOffset = start;
                List<String> variableKeys = getLocalVariableKeys(); // localVariableKeys.get();
                try {
                    for (; ; ) {
                        while (readable() && (isVariableAppend(currentChar = read()))) {
                            ++this.readIndex;
                        }
                        if (currentChar == '.') {
                            if (readIndex == localOffset) {
                                String errorMessage = createErrorContextText(sourceChars, this.readIndex);
                                throw new ExpressionException("syntax error, pos: " + this.readIndex + ", unexpected token '" + currentChar + "', Expression[... " + errorMessage + " ]");
                            }
                            variableKeys.add(ExprParserContext.getString(sourceChars, localOffset + this.offset, readIndex - localOffset));
                            localOffset = ++this.readIndex;
                            continue;
                        }
                        if (currentChar == '[') {
                            if (readIndex == localOffset) {
                                String errorMessage = createErrorContextText(sourceChars, this.readIndex);
                                throw new ExpressionException("syntax error, pos: " + this.readIndex + ", unexpected token '" + currentChar + "', Expression[... " + errorMessage + " ]");
                            }
                            // 内层循环直到遇到]结束
                            variableKeys.add(new String(sourceChars, localOffset + this.offset, readIndex - localOffset));
                            localOffset = ++this.readIndex;

                            boolean continueOuterLoop;
                            while (true) {
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
                                        --cnt;
                                    } else if (currentChar == '[') {
                                        ++cnt;
                                    }
                                    if (cnt == 0) {
                                        break;
                                    }
                                    ++this.readIndex;
                                }
                                if (currentChar != ']') {
                                    String errorMessage = createErrorContextText(sourceChars, this.readIndex);
                                    throw new ExpressionException("syntax error, pos: " + this.readIndex + ", missing closing symbol ']', Expression[... " + errorMessage + " ]");
                                }
                                String identifierValue = null;
                                if (stringKey) {
                                    // 去除''
                                    int st = localOffset + this.offset;
                                    int et = readIndex + this.offset;
                                    while ((st < et) && (sourceChars[st] <= ' ')) {
                                        ++st;
                                    }
                                    while ((st < et) && (sourceChars[et - 1] <= ' ')) {
                                        --et;
                                    }
                                    identifierValue = ExprParserContext.getString(sourceChars, st + 1, et - st - 2);
                                } else {
                                    // 子表达式使用（）
                                    char[] chars = new char[readIndex - localOffset + 2];
                                    chars[0] = '(';
                                    chars[chars.length - 1] = ')';
                                    System.arraycopy(sourceChars, localOffset + this.offset, chars, 1, chars.length - 2);
                                    identifierValue = new String(chars);
                                }
                                ++this.readIndex;
                                if (readable()) {
                                    if ((currentChar = read()) == '.') {
                                        variableKeys.add(identifierValue);
                                        localOffset = ++this.readIndex;
                                        continueOuterLoop = true;
                                        break;
                                    } else if (currentChar == '[') {
                                        variableKeys.add(identifierValue);
                                        localOffset = ++this.readIndex;
                                        continue;
                                    } else if (currentChar == '(') {
                                        handleParseFunctionToken(identifierValue, false, variableKeys, exprParserContext);
                                        return;
                                    }
                                }
                                variableKeys.add(identifierValue);
                                this.tokenType = VAR_TOKEN;
                                checkValueTokenSyntaxError();
                                parseVarToken(exprParserContext, startChar, null, variableKeys);
                                return;
                            }
                            if (continueOuterLoop) {
                                continue;
                            }
                        }
                        break;
                    }
                    if (readIndex == localOffset) {
                        // validate such as 'a. '
                        String errorMessage = createErrorContextText(sourceChars, this.readIndex);
                        throw new ExpressionException("syntax error, pos: " + this.readIndex + ", unexpected token '" + currentChar + "', Expression[... " + errorMessage + " ]");
                    }
                    String identifierValue = ExprParserContext.getString(sourceChars, localOffset + this.offset, readIndex - localOffset);
                    boolean oneLevelAccess = localOffset == start;
                    if (currentChar == '(') {
                        handleParseFunctionToken(identifierValue, oneLevelAccess, variableKeys, exprParserContext);
                    } else {
                        String var;
                        if (oneLevelAccess /*&& readIndex > localOffset*/) {
                            var = identifierValue;
                        } else {
                            variableKeys.add(identifierValue);
                            var = null;
                        }
                        this.tokenType = VAR_TOKEN; //设置标记类型为标识符token
                        // todo 检查是否内置关键字（目前存在问题）
                        // this.checkIfBuiltInKeywords();
                        // checkTokenSyntaxError();
                        checkValueTokenSyntaxError();
                        parseVarToken(exprParserContext, startChar, var, variableKeys);
                    }
                } finally {
                    variableKeys.clear();
                }
            }
        } else if (currentChar == '!') {
            ++this.readIndex;
            // 非运算与负数运算处理方式一致
            this.tokenType = NOT_TOKEN;
            parseNotToken(exprParserContext);
        } else if (currentChar == '\'') {
            int start = this.readIndex + 1;
            this.scanString();
            // 最后一个字符属于token范围内字符需要++
            // 标记为常量
            this.tokenType = STR_TOKEN;
            checkTokenSyntaxError();
            parseStrToken(exprParserContext, new String(sourceChars, this.offset + start, readIndex - start));
            ++this.readIndex;
        } else if (currentChar == '{') {
            // 静态数组（只支持number和字符串两种，不支持嵌套数组）
            // 表达式中静态数组使用{}而不是[]
            // 只能在解释模式下运行,编译模式java只支持声明时使用，运算不支持
            int start = ++this.readIndex;
            while (readable() && (currentChar = read()) != '}') {
                ++this.readIndex;
                if (this.readIndex >= length()) {
                    break;
                }
            }
            if (currentChar != '}') {
                String errorMessage = createErrorContextText(sourceChars, this.readIndex);
                throw new ExpressionException("syntax error, pos: " + this.readIndex + ", Expression[... " + errorMessage + " ],未找到与开始字符'{'相匹配的结束字符 '}'");
            }
            this.tokenType = ARR_TOKEN;
            checkTokenSyntaxError();
            parseArrToken(exprParserContext, new String(sourceChars, start + this.offset, this.readIndex - start));
            ++this.readIndex;
        } else if (currentChar == '@') {
            // 函数token(解析函数内容)
            int start = ++this.readIndex;
            // 校验@后紧跟的标识符是否合法
            if (readable()) {
                ++this.readIndex;
                // 以java标识符开头
                if (!Character.isJavaIdentifierStart(currentChar = read())) {
                    String errorMessage = createErrorContextText(sourceChars, this.readIndex);
                    throw new ExpressionException("syntax error, pos: " + this.readIndex + ", Expression[... " + errorMessage + " ], unexpected function start char  : '" + currentChar + "'");
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
                String readSource = new String(this.exprSource.substring(0, this.readIndex));
                throw new ExpressionException("syntax error, pos: " + this.readIndex + ", source: '" + readSource + "' function start symbol '(' not found !");
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
                    --bracketCount;
                } else if (currentChar == '(') {
                    ++bracketCount;
                }
                if (bracketCount == 0) {
                    break;
                }
            }
            String args = new String(sourceChars, start + this.offset, this.readIndex - start - 1);
            this.tokenType = FUN_TOKEN;
            checkTokenSyntaxError();
            parseFunToken(exprParserContext, functionName, args);
        } else if (currentChar == '.' && prevTokenType == FUN_TOKEN) {
            // todo
            throw new UnsupportedOperationException("currently, it is not supported to access method return values as variables or method call handles");
        } else if (currentChar == '+' && prevTokenType == RESET_TOKEN) {
            // dealing with the first character '+' issue
            ++readIndex;
            tokenType = OPS_TOKEN;
        } else {
            if (prevTokenType != OPS_TOKEN && findMode) {
                // todo if prevTokenType == OPS_TOKEN back to the prev value token
                endFind();
            } else {
                // unexpected character token
                String errorMessage = createErrorContextText(sourceChars, this.readIndex);
                throw new ExpressionException("syntax error, pos: " + this.readIndex + ", unexpected token '" + currentChar + "', Expression[... " + errorMessage + " ]");
            }
        }
    }

    protected Number createBigDecimal(int beginIndex, int endIndex, boolean negate) {
        BigDecimal value = new BigDecimal(sourceChars, offset + beginIndex, endIndex - beginIndex);
        return negate ? value.negate() : value;
    }

    private void handleParseFunctionToken(String identifierValue, boolean oneLevelAccess, List<String> variableKeys, ExprParserContext exprParserContext) {
        // forward method or function
        int localOffset = ++this.readIndex;
        int bracketCount = 1;
        char currentChar = 0;
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
                --bracketCount;
            } else if (currentChar == '(') {
                ++bracketCount;
            }
            if (bracketCount == 0) {
                break;
            }
        }
        if (currentChar != ')') {
            String errorMessage = createErrorContextText(sourceChars, this.readIndex);
            throw new ExpressionException("syntax error, pos: " + this.readIndex + ",Expression[... " + errorMessage + " ], end token ')' not found !");
        }
        String args = new String(sourceChars, localOffset + this.offset, this.readIndex - localOffset - 1);
        this.tokenType = FUN_TOKEN;
//        checkTokenSyntaxError();
        checkValueTokenSyntaxError();
        if (oneLevelAccess) {
            // static Function
            // @see @function
            parseFunToken(exprParserContext, identifierValue, args);
        } else {
            // reflect invoke : obj.method(...args)
            parseMethodToken(exprParserContext, variableKeys, identifierValue, args);
        }
    }

    protected List<String> getLocalVariableKeys() {
        return localVariableKeys.get();
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
            String errorMessage = createErrorContextText(sourceChars, this.readIndex);
            throw new ExpressionException("syntax error, pos: " + this.readIndex + ", Expression[... " + errorMessage + " ],未找到与开始字符'\''相匹配的结束字符 '\''");
        }
    }

    final boolean readable() {
        return this.readIndex < count;
    }

    final boolean isEnd() {
        return this.readIndex >= count;
    }

    final char read() {
        return this.sourceChars[offset + this.readIndex];
    }

    final void endFind() {
        findEndIndex = offset + readIndex;
        this.readIndex = count;
    }

    /**
     * 读取指定位置字符
     */
    final char read(int index) {
        return this.sourceChars[offset + index];
    }

//    private void checkIfBuiltInKeywords(String varValue) {
//        if (varValue == null) return;
//        int length = varValue.length();
//        if (length == 2) {
//            char charFir = varValue.charAt(0);
//            char charSec = varValue.charAt(1);
//            if ((charFir == 'i' && charSec == 'n') || (charFir == 'I' && charSec == 'N')) {
//                // in(IN)语法归纳到操作符于&&,||, != 同级别
//                // 判断左边的对象是否为右边集合的中元素或者属性,或者map的key
//                this.opsType = 63;
//                this.opsLevel = 600;
//                this.tokenType = OPS_TOKEN;
//            }
//        } else if (length == 3) {
//            if (varValue.indexOf("out") == 0) {
//                // out指令 实际上是in操作取反
//                this.opsType = 64;
//                this.opsLevel = 600;
//                this.tokenType = OPS_TOKEN;
//            }
//        }
//    }

    final static int getTokenTypeGroup(int tokenType) {
        switch (tokenType) {
            case OPS_TOKEN:
                return GROUP_TOKEN_OPS;
            case NUM_TOKEN:
            case STR_TOKEN:
            case VAR_TOKEN:
            case ARR_TOKEN:
            case FUN_TOKEN:
                return GROUP_TOKEN_VALUE;
            default:
                return 0;
        }
    }

//    final static boolean isNotGroupTokenValue(int tokenType) {
//        return tokenType < NUM_TOKEN;
//    }

    // 1.可以直接排除前置token类型一定不是VALUE_TOKEN
    // 2.只需要校验不能紧跟')'
    private void checkValueTokenSyntaxError() {
        if (this.prevTokenType == BRACKET_END_TOKEN) {
            String readSource = createErrorContextText(sourceChars, this.readIndex);
            throw new ExpressionException("syntax error, pos: " + this.readIndex + ", value cannot appear after the closing bracket, Expression[" + readSource + "]");
        }
    }

    // 1.可以直接排除前置token类型一定不是OPS_TOKEN
    // 2.只需要校验不能紧跟'('
    private void checkOpsTokenSyntaxError() {
        if (this.prevTokenType == BRACKET_TOKEN) {
            String readSource = createErrorContextText(sourceChars, this.readIndex);
            throw new ExpressionException("syntax error, pos: " + this.readIndex + ", unsupported operation symbols follow '(' , Expression[" + readSource + "]");
        }
    }

    private void checkTokenSyntaxError() {
        // 暂时校验最近两个token类型一定不能一致
        int preGroupValue = getTokenTypeGroup(this.prevTokenType);
        int groupValue = getTokenTypeGroup(this.tokenType);
        if (preGroupValue == groupValue) {
            if (preGroupValue == GROUP_TOKEN_OPS) {
                String readSource = createErrorContextText(sourceChars, this.readIndex);
                throw new ExpressionException("syntax error, pos: " + this.readIndex + ", duplicate token operation, Expression[" + readSource + "]");
            } else if (preGroupValue == GROUP_TOKEN_VALUE) {
                String readSource = createErrorContextText(sourceChars, this.readIndex);
                throw new ExpressionException("syntax error, pos: " + this.readIndex + ", missing operation symbol, Expression[" + readSource + "]");
            }
        } else {
            // value token before is )
            if (groupValue == GROUP_TOKEN_VALUE && this.prevTokenType == BRACKET_END_TOKEN) {
                String readSource = createErrorContextText(sourceChars, this.readIndex);
                throw new ExpressionException("syntax error, pos: " + this.readIndex + ", value cannot appear after the closing bracket, Expression[" + readSource + "]");
            }
            // // ops token before is (
            if (groupValue == GROUP_TOKEN_OPS && this.prevTokenType == BRACKET_TOKEN) {
                String readSource = createErrorContextText(sourceChars, this.readIndex);
                throw new ExpressionException("syntax error, pos: " + this.readIndex + ", unsupported operation symbols follow '(' , Expression[" + readSource + "]");
            }
        }
    }

    // check bracket ) before
    // expected value type,  unexpected ( && ops ,
    private void checkBeforeBracketEndTokenSyntaxError() {
        int preGroupValue = getTokenTypeGroup(this.prevTokenType);
        if (this.prevTokenType == BRACKET_TOKEN || preGroupValue == GROUP_TOKEN_OPS) {
            String readSource = createErrorContextText(sourceChars, --this.readIndex);
            throw new ExpressionException("syntax error, pos: " + this.readIndex + ", unexpected symbol : ')', Expression[" + readSource + "]");
        }
    }

    // check bracket ( before
    // expected ops type,  unexpected ) && value ,
    private void checkBeforeBracketTokenSyntaxError() {
        int preGroupValue = getTokenTypeGroup(this.prevTokenType);
        if (this.prevTokenType == BRACKET_END_TOKEN || preGroupValue == GROUP_TOKEN_VALUE) {
            String readSource = createErrorContextText(sourceChars, --this.readIndex);
            throw new ExpressionException("syntax error, pos: " + this.readIndex + ",unexpected symbol : '(', Expression[" + readSource + "]");
        }
    }

    private void throwSyntaxError() {
        if (findMode) {
            endFind();
        } else {
            throwUnsupportedError();
        }
    }

    private void throwUnsupportedError() {
        String readSource = createErrorContextText(sourceChars, this.readIndex);
        throw new ExpressionException("syntax error, pos: " + this.readIndex + ", unexpected symbol '" + this.errorMsg + "', Expression[" + readSource + "]");
    }

    final static boolean isWhitespace(char c) {
        return c <= ' ';
    }

    // 解析模式除了数字和.开头
    final static boolean isIdentifierStart(char c) {
        return c == '_' || c == '$' /*|| c == '#'*/ || Character.isLetter(c);
    }

    // 解析模式下支持 _ $ . 字母,数字等
    final static boolean isIdentifierAppend(char c) {
        return c == '_' || c == '$' || c == '.' /*|| c == '#'*/ || Character.isLetter(c) || isDigit(c);
    }

    // 是否变量表达式字符
    final static boolean isVariableAppend(char c) {
        if (Character.isLetter(c)) return true;
        if (c <= ' ') return false;
        return /*c == '.' || */c == '_' || c == '$' || isDigit(c) /*|| c == '[' *//*|| c == '('*/;
    }

    final static int digit(char c, int numberRadix) {
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
     * 10进制数字开头
     *
     * @param c
     * @return
     */
    final static boolean isDigit(char c) {
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
                return true;
            default:
                return false;
        }
    }

    private void resetToken() {
        // 重置记录上一个token类型
        this.prevTokenType = this.tokenType;
        this.tokenType = RESET_TOKEN;
        this.operator = ElOperator.ATOM;
    }

    /**
     * 判断是否为操作符号
     * +-/*%**（加(+)减(-)乘(*)除(/)余(%)及指数(**)）
     */
    private ElOperator getOperator(char symbol) {
        if (symbol >= ElOperator.INDEXS_OPERATORS.length) {
            return null;
        }
        ElOperator elOperator = ElOperator.INDEXS_OPERATORS[symbol];
        if (elOperator != null) {
            this.operator = elOperator;
        }
        return elOperator;

//        switch (symbol) {
//            case '*':
//                return this.operator = ElOperator.MULTI;
//            case '/':
//                return this.operator = ElOperator.DIVISION;
//            case '%':
//                return this.operator = ElOperator.MOD;
//            case '+':
//                return this.operator = ElOperator.PLUS;
//            case '-':
//                return this.operator = ElOperator.MINUS;
//            case '&':
//                return this.operator = ElOperator.AND;
//            case '^':
//                return this.operator = ElOperator.XOR;
//            case '|':
//                return this.operator = ElOperator.OR;
//            case '!':
//                // &&(41) ||(42)  !=(43)
//                return this.operator = ElOperator.NOT;
//            case '>':
//                return this.operator = ElOperator.GT;
//            case '<':
//                return this.operator = ElOperator.LT;
//            case '=':
//                return this.operator = ElOperator.EQ;
//            case ':':
//                return this.operator = ElOperator.COLON;
//            case '?':
//                return this.operator = ElOperator.QUESTION;
//            default:
//                return /*this.operator = */null;
//        }
    }

    /**
     * 是否括号标识符
     *
     * @param c
     * @return
     */
    final boolean isBracketSymbol(char c) {
        return c == '(' || c == ')';
    }

    public Object evaluate() {
        return doEvaluate(EvaluatorContext.EMPTY, EvaluateEnvironment.DEFAULT);
    }

    public Object evaluate(Object context) {
        if (context instanceof EvaluateEnvironment) {
            return evaluate((EvaluateEnvironment) context);
        }
        if (context instanceof Map) {
            return evaluate((Map) context);
        }
        // from pojo entity
        return doEvaluate(variableCount == 0 ? EvaluatorContext.EMPTY : evaluatorContext.cloneContext().invokeVariables(tailChainInvoker, context, variableCount), EvaluateEnvironment.DEFAULT); // evaluate(context, EvaluateEnvironment.DEFAULT);
    }


    public final Object evaluate(Map context) {
        return doEvaluate(variableCount == 0 ? EvaluatorContext.EMPTY : evaluatorContext.cloneContext().invokeVariables(tailChainInvoker, context, variableCount), EvaluateEnvironment.DEFAULT);
    }

    @Override
    public final Object evaluate(Map context, EvaluateEnvironment evaluateEnvironment) {
        return doEvaluate(variableCount == 0 ? EvaluatorContext.EMPTY : evaluatorContext.cloneContext().invokeVariables(tailChainInvoker, context, variableCount), evaluateEnvironment);
    }

    @Override
    public final Object evaluate(Object context, EvaluateEnvironment evaluateEnvironment) {
        return doEvaluate(variableCount == 0 ? EvaluatorContext.EMPTY : evaluatorContext.cloneContext().invokeVariables(tailChainInvoker, context, variableCount), evaluateEnvironment);
    }

    @Override
    public final Object evaluate(EvaluateEnvironment evaluateEnvironment) {
        if (variableCount == 0 || evaluateEnvironment == null) {
            return exprEvaluator.evaluate(EvaluatorContext.EMPTY, evaluateEnvironment);
        }
        if (evaluateEnvironment.isMapContext()) {
            return doEvaluate(evaluatorContext.cloneContext().invokeVariables(tailChainInvoker, (Map) evaluateEnvironment.getContext(), variableCount), evaluateEnvironment);
        } else {
            return doEvaluate(evaluatorContext.cloneContext().invokeVariables(tailChainInvoker, evaluateEnvironment.getContext(), variableCount), evaluateEnvironment);
        }
    }

    @Override
    public final Object evaluateParameters(Object... params) {
        return doEvaluate(new EvaluatorContext.ParametersImpl(params), EvaluateEnvironment.DEFAULT);
    }

    @Override
    public final Object evaluateParameters(EvaluateEnvironment evaluateEnvironment, Object... params) {
        return doEvaluate(new EvaluatorContext.ParametersImpl(params), evaluateEnvironment);
    }

    // evaluate entrance
    final Object doEvaluate(EvaluatorContext evaluatorContext, EvaluateEnvironment evaluateEnvironment) {
        Object result = exprEvaluator.evaluate(evaluatorContext, evaluateEnvironment);
        if (!compressed) {
            if (cntForCompress.getAndIncrement() == 1) {
                compressEvaluator();
            }
        }
        return result;
    }

    protected final void compressEvaluator() {
        this.exprEvaluator = compressEvaluator(exprEvaluator);
        this.compressed = true;
    }

    private static ExprEvaluator compressEvaluator(ExprEvaluator exprEvaluator) {
        if (exprEvaluator.constant) {
            if (exprEvaluator instanceof ExprEvaluator.ConstantImpl) {
                return exprEvaluator;
            }
            return new ExprEvaluator.ConstantImpl(exprEvaluator.result);
        }

        if (exprEvaluator instanceof ExprEvaluator.StackSplitImpl) {
            ExprEvaluator.StackSplitImpl stackSplit = (ExprEvaluator.StackSplitImpl) exprEvaluator;
            stackSplit.front = compressEvaluator(stackSplit.front);
            stackSplit.left = compressEvaluator(stackSplit.left);
            return stackSplit;
        }

        if (exprEvaluator instanceof ExprEvaluator.ContextValueHolderImpl) {
            return exprEvaluator;
        }

        ExprEvaluator left = exprEvaluator.left;
        ExprEvaluator right = exprEvaluator.right;
        int evalType = exprEvaluator.getEvalType();
        if (evalType == 0) {
            return compressEvaluator(left);
        } else if (evalType == ExprEvaluator.EVAL_TYPE_OPERATOR) {
            left = compressEvaluator(left);
            if (right == null) {
                return left;
            }
            right = compressEvaluator(right);
            ElOperator eo = exprEvaluator.operator;
            if (eo == ElOperator.QUESTION) {
                return ExprEvaluator.TernaryImpl.of(exprEvaluator.update(left, right));
            }
            switch (eo) {
                case MULTI:
                    // *乘法
                    return ExprEvaluator.MultiplyImpl.of(exprEvaluator.update(left, right));
                case DIVISION:
                    // /除法
                    return ExprEvaluator.DivisionImpl.of(exprEvaluator.update(left, right));
                case MOD:
                    // %取余数
                    return ExprEvaluator.ModulusImpl.of(exprEvaluator.update(left, right));
                case EXP:
                    // **指数,平方/根
                    return ExprEvaluator.PowerImpl.of(exprEvaluator.update(left, right));
                case PLUS:
                    if (right.negate) {
                        right.setNegate(false);
                        return ExprEvaluator.MinusImpl.of(exprEvaluator.update(left, right));
                    }
                    // 加法
                    return ExprEvaluator.PlusImpl.of(exprEvaluator.update(left, right));
                case MINUS:
                    // -减法（理论上代码不可达） 因为'-'统一转化为了'+'(负)
                    return ExprEvaluator.MinusImpl.of(exprEvaluator.update(left, right));
                case BIT_RIGHT:
                    // >> 位运算右移
                    return ExprEvaluator.BitRightImpl.of(exprEvaluator.update(left, right));
                case BIT_LEFT:
                    // << 位运算左移
                    return ExprEvaluator.BitLeftImpl.of(exprEvaluator.update(left, right));
                case AND:
                    // &
                    return ExprEvaluator.BitAndImpl.of(exprEvaluator.update(left, right));
                case XOR:
                    // ^
                    return ExprEvaluator.BitXorImpl.of(exprEvaluator.update(left, right));
                case OR:
                    // |
                    return ExprEvaluator.BitOrImpl.of(exprEvaluator.update(left, right));
                case GT:
                    // >
                    return ExprEvaluator.GtImpl.of(exprEvaluator.update(left, right));
                case LT:
                    // <
                    return ExprEvaluator.LtImpl.of(exprEvaluator.update(left, right));
                case EQ:
                    // ==
                    return ExprEvaluator.EqualImpl.of(exprEvaluator.update(left, right));
                case GE:
                    // >=
                    return ExprEvaluator.GEImpl.of(exprEvaluator.update(left, right));
                case LE:
                    // <=
                    return ExprEvaluator.LEImpl.of(exprEvaluator.update(left, right));
                case NE:
                    // !=
                    return ExprEvaluator.NEImpl.of(exprEvaluator.update(left, right));
                case LOGICAL_AND:
                    // &&
                    return ExprEvaluator.LogicalAndImpl.of(exprEvaluator.update(left, right));
                case LOGICAL_OR:
                    // ||
                    return ExprEvaluator.LogicalOrImpl.of(exprEvaluator.update(left, right));
                case IN:
                    // in
                    return ExprEvaluator.InImpl.of(exprEvaluator.update(left, right));
                case OUT:
                    // out
                    return ExprEvaluator.OutImpl.of(exprEvaluator.update(left, right));
                case COLON:
                    // : 三目运算结果, 不支持单独运算
                case QUESTION:
                default:
                    // ? 三目运算条件
                    return exprEvaluator.update(left, right);
            }
            // 默认返回null
        } else if (evalType == ExprEvaluator.EVAL_TYPE_BRACKET) {
            // optimize
            if (!exprEvaluator.negate && !exprEvaluator.logicalNot) {
                return compressEvaluator(right);
            }
            // 括号运算
            return ExprEvaluator.BracketImpl.of(exprEvaluator.update(left, compressEvaluator(right)));
        } else if (evalType == ExprEvaluator.EVAL_TYPE_VARIABLE) {
            if (!exprEvaluator.negate && !exprEvaluator.logicalNot) {
                return ((ExprEvaluator.VariableImpl) exprEvaluator).normal().internKey();
            }
            // 变量运算
            return ((ExprEvaluator.VariableImpl) exprEvaluator).internKey();
        } else if (evalType == ExprEvaluator.EVAL_TYPE_FUN) {
            // 函数运算
            return exprEvaluator;
        } else {
            // 其他统一返回left
            return left == null ? null : compressEvaluator(left);
        }
    }

    protected String createErrorContextText(char[] buf, int readIndex) {
        try {
            int at = offset + readIndex;
            int len = buf.length;
            char[] text = new char[100];
            int count;
            int begin = Math.max(at - 49, 0);
            System.arraycopy(buf, begin, text, 0, count = at - begin);
            text[count++] = '^';
            int end = Math.min(len, at + 49);
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