/*
 * Copyright [2020-2026] [wangyunchao]
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
import java.math.BigInteger;
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

    ExprParser(char[] buf, int offset, int count) {
        this.init(buf, offset, count);
        this.parse();
    }

    ExprParser(String exprSource, int offset) {
        this.init(exprSource, offset, exprSource.length() - offset);
        this.findMode = true;
        this.findEndIndex = exprSource.length();
        this.parse();
    }

    ExprParser(char[] buf, int offset) {
        this.init(buf, offset, buf.length - offset);
        this.findMode = true;
        this.findEndIndex = buf.length;
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
    public static final int LIST_TOKEN = 13;
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

    // 变量计数(不去重)
    int variableCount;
    boolean existChain;
    // 去重后的变量个数
    protected int variableSize;
    protected ElInvoker tailChainInvoker;
    EvaluatorContextBuilder evaluatorContextBuilder = EvaluatorContextBuilder.EMPTY;

    // 标记类型
    private int prevTokenType;
    private int tokenType = RESET_TOKEN;

    // 操作类型
    private ElOperator operator;

    // 数量
    private int evaluatorCount;

    // 表达式解析器
    private ExprEvaluator exprEvaluator = createExprEvaluator();
    // 上下文
    private final ExprParserContext parserContext = new ExprParserContext();

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
            return getString(0, findEndIndex - offset);
        }
        if (offset == 0 && count == sourceChars.length) {
            return this.exprSource;
        }
        return getString(0, count);
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

    protected final void init(char[] buf, int offset, int count) {
        this.sourceChars = buf;
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
        final ExprEvaluator exprEvaluator = exprParserContext.exprEvaluator;
        if (exprEvaluator == null) {
            return;
        }
        ElOperator exprOperator = exprEvaluator.operator;
        int level = exprOperator.level;

        int evalType = exprEvaluator.evalType;
        ExprEvaluator left = exprEvaluator.left;
        ExprEvaluator right = exprEvaluator.right;
        if (right == null) {
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

        int rEvalType = right.evalType;
        ExprEvaluator rLeft = right.left;
        ExprEvaluator rRight = right.right;

        if (level <= rLevel) {
            // 如果左侧的优先级级别高（level小）于右侧就开启轮换
            ExprEvaluator newLeft = createExprEvaluator();
            newLeft.evalType = evalType;
            newLeft.operator = exprOperator;
            newLeft.negate = exprEvaluator.negate;
            newLeft.logicalNot = exprEvaluator.logicalNot;
            newLeft.left = left;
            newLeft.right = rLeft;

            exprEvaluator.operator = right.operator;
            exprEvaluator.negate = right.negate;
            exprEvaluator.logicalNot = right.logicalNot;
            exprEvaluator.evalType = rEvalType;
            exprEvaluator.left = newLeft;
            exprEvaluator.right = rRight;
            // exprParserContext.exprEvaluator = exprEvaluator;
        } else {
            if (rLevel == 1) {
                // 如果是括号子表达式开始子轮换
                displacement(right.right.left);
            }
            exprParserContext.exprEvaluator = right;
        }
    }

    final void mergeRight(ExprEvaluator exprEvaluator, int targetLevel) {
        if (exprEvaluator == null) {
            return;
        }
        int evalType = exprEvaluator.evalType;
        int level = exprEvaluator.operator.level;

        if (level >= targetLevel) {
            // 停止合并
            return;
        }

        ExprEvaluator left = exprEvaluator.left;
        ExprEvaluator right = exprEvaluator.right;
        if (right == null) {
            return;
        }

        int rLevel = right.operator.level;
        if (rLevel <= 0) {
            return;
        }

        int rEvalType = right.evalType;
        ExprEvaluator rLeft = right.left;
        ExprEvaluator rRight = right.right;

        if (level <= rLevel) {
            // parse后表达式从右至左
            // 如果当前的优先级级别高（level越小）就开启轮换
            ExprEvaluator newLeft = createExprEvaluator();
            newLeft.evalType = evalType;
            newLeft.operator = exprEvaluator.operator;
            newLeft.negate = exprEvaluator.negate;
            newLeft.logicalNot = exprEvaluator.logicalNot;
            newLeft.left = left;
            newLeft.right = rLeft;

            exprEvaluator.operator = right.operator;
            exprEvaluator.negate = right.negate;
            exprEvaluator.logicalNot = right.logicalNot;
            exprEvaluator.evalType = rEvalType;
            exprEvaluator.left = newLeft;
            exprEvaluator.right = rRight;
            // continue merge
            mergeRight(exprEvaluator, targetLevel);
        }
    }

    protected void compressVariables() {
        // invokes
        if (tailInvokes != null) {
            variableSize = invokes.size();
            final int tailSize = tailInvokes.size();
            if (variableCount == tailSize || !existChain) {
                // all root and once vars without reusable
                if (tailSize == 1) {
                    evaluatorContextBuilder = new EvaluatorContextBuilder.SingleReusableRootImpl((ElVariableInvoker) tailChainInvoker);
                } else if (tailSize == 2) {
                    Iterator<ElVariableInvoker> iterator = invokes.values().iterator();
                    evaluatorContextBuilder = new EvaluatorContextBuilder.SibbingTwinsRootReusableImpl(iterator.next(), iterator.next());
                } else {
                    if (variableCount == tailSize) {
                        evaluatorContextBuilder = new EvaluatorContextBuilder.ReusablelessRootImpl();
                    } else {
                        ElVariableInvoker[] arr = new ElVariableInvoker[tailSize];
                        int len = 0;
                        for (ElVariableInvoker variableInvoker : invokes.values()) {
                            arr[len++] = variableInvoker;
                        }
                        evaluatorContextBuilder = new EvaluatorContextBuilder.SibbingRootReusableImpl(arr);
                    }
                }
            } else {
                if (tailSize == 1) {
                    evaluatorContextBuilder = new EvaluatorContextBuilder.SingleReusableImpl(tailChainInvoker);
                } else {
                    Collection<ElVariableInvoker> values = tailInvokes.values();
                    if (tailSize == 2) {
                        Iterator<ElVariableInvoker> iterator = values.iterator();
                        ElVariableInvoker one = iterator.next(), next = iterator.next();
                        if (one.parent == next.parent) {
                            // parent + child1 + child2
                            evaluatorContextBuilder = new EvaluatorContextBuilder.SibbingTwinsReusableImpl(one.parent, one, next);
                        } else {
                            evaluatorContextBuilder = new EvaluatorContextBuilder.GeneralReusableImpl(ElChainVariableInvoker.buildTailChainInvoker(tailInvokes), variableSize);
                        }
                    } else {
                        ElVariableInvoker tailParent = null;
                        boolean sibbingMode = true;
                        for (ElVariableInvoker variableInvoker : values) {
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
                        if (sibbingMode) {
                            // parent + children
                            evaluatorContextBuilder = new EvaluatorContextBuilder.SibbingReusableImpl(tailParent, ElChainVariableInvoker.buildTailChainInvoker(tailInvokes), variableSize);
                        } else {
                            // default
                            evaluatorContextBuilder = new EvaluatorContextBuilder.GeneralReusableImpl(ElChainVariableInvoker.buildTailChainInvoker(tailInvokes), variableSize);
                        }
                    }
                }
            }
        }
    }

    public final Collection<ElVariableInvoker> getTailVariableInvokers() {
        return tailInvokes == null ? new ArrayList<ElVariableInvoker>() : new ArrayList<ElVariableInvoker>(tailInvokes.values());
    }

    @Override
    public final List<String> getVariables() {
        if (variableSize > 0) {
            return new ArrayList<String>(tailInvokes.keySet());
        } else {
            return new ArrayList<String>();
        }
    }

    @Override
    public final List<String> getRootVariables() {
        if (variableSize > 0) {
            List<String> variables = new ArrayList<String>();
            Set<Map.Entry<String, ElVariableInvoker>> entrySet = invokes.entrySet();
            for (Map.Entry<String, ElVariableInvoker> entry : entrySet) {
                if (entry.getValue().parent == null) {
                    variables.add(entry.getKey());
                }
            }
            return variables;
        } else {
            return new ArrayList<String>();
        }
    }

    private void parseOperatorToken(ExprParserContext exprParserContext) {
        ExprEvaluator evaluator = exprParserContext.exprEvaluator;
        boolean negate = exprParserContext.negate;
        boolean logicalNot = exprParserContext.logicalNot;
        evaluator.evalType = ExprEvaluator.EVAL_TYPE_OPERATOR;
        ExprEvaluator right = createExprEvaluator();
        if (operator == ElOperator.MINUS) {
            evaluator.operator = ElOperator.PLUS;
            negate = true;
        } else {
            evaluator.operator = operator;
        }
        evaluator.right = right;
        exprParserContext.setContext(right, negate, logicalNot);
    }

    private void parseVariableToken(ExprParserContext exprParserContext, char startChar, String
            identifierValue, List<String> variableKeys) {
        ExprEvaluator evaluator = exprParserContext.exprEvaluator;
        boolean negate = exprParserContext.negate;
        boolean logicalNot = exprParserContext.logicalNot;
        if (identifierValue != null) {
            switch (startChar) {
                case 't': {
                    if ("true".equals(identifierValue)) {
                        evaluator.left = new ExprEvaluator.ConstantImpl(!logicalNot);
                        exprParserContext.setContext(evaluator, false, false);
                        return;
                    }
                    break;
                }
                case 'f': {
                    if ("false".equals(identifierValue)) {
                        evaluator.left = new ExprEvaluator.ConstantImpl(logicalNot);
                        exprParserContext.setContext(evaluator, false, false);
                        return;
                    }
                    break;
                }
                case 'n': {
                    if ("null".equals(identifierValue)) {
                        evaluator.left = new ExprEvaluator.ConstantImpl(null);
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
        left.negate = negate;
        left.logicalNot = logicalNot;

        ElVariableInvoker variableInvoker;
        if (identifierValue == null) {
            variableInvoker = ElVariableUtils.build(variableKeys, getInvokes(), getTailInvokes());
            global().variableCount += variableKeys.size();
            global().existChain = true;
        } else {
            variableInvoker = ElVariableUtils.buildRoot(identifierValue, getInvokes(), getTailInvokes());
            ++global().variableCount;
        }
        global().tailChainInvoker = variableInvoker;
        left.variableInvoker = variableInvoker;
        evaluator.left = left;
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
        evaluator.left = new ExprEvaluator.ConstantImpl(numberValue);
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
        bracketParserContext.bracketMode = true;
        do {
            parseNext(bracketParserContext);
        } while (readable() && !bracketParserContext.bracketEndFlag);
        // bracketParserContext的endFlag设置为true时,当前递归栈结束；
        // 解析类型如果没有结束括号说明表达式错误
        if (!bracketParserContext.bracketEndFlag) {
            String errorMessage = createErrorContextText(sourceChars, this.readIndex);
            throw new ExpressionException("syntax error, pos: " + this.readIndex + ", missing closing symbol ')'" + ", Expression[... " + errorMessage + " ]");
        }
        //  置换子表达式优先级
        //  displacementSplit(child);
        displacementChain(bracketChild);
        mergeLast(bracketChild);

        ExprEvaluator bracketEvaluator = createExprEvaluator();
        bracketEvaluator.negate(negate).setLogicalNot(logicalNot);
        bracketEvaluator.evalType = ExprEvaluator.EVAL_TYPE_BRACKET;
        bracketEvaluator.operator = ElOperator.BRACKET;
        bracketEvaluator.right = bracketChild;
        evaluator.left = bracketEvaluator;
        exprParserContext.setContext(evaluator, false, false);
    }

    final void parseQuestionToken(ExprParserContext exprParserContext) {
        ExprEvaluator evaluator = exprParserContext.exprEvaluator;
        evaluator.operator = ElOperator.QUESTION;
        evaluator.evalType = ExprEvaluator.EVAL_TYPE_QUESTION;
        boolean negate = exprParserContext.negate;
        boolean logicalNot = exprParserContext.logicalNot;
        // question
        ExprEvaluator questionChild = createExprEvaluator();
        ExprParserContext questionParserContext = new ExprParserContext(questionChild, false, false);
        questionParserContext.questionMode = true;
        do {
            parseNext(questionParserContext);
        } while (readable() && !questionParserContext.questionEndFlag);
        if (!questionParserContext.questionEndFlag) {
            String errorMessage = createErrorContextText(sourceChars, this.readIndex);
            throw new ExpressionException("syntax error, pos: " + this.readIndex + ", missing symbol ':'" + ", Expression[... " + errorMessage + " ]");
        }
        displacementChain(questionChild);
        mergeLast(questionChild);

        // colon use exprParserContext
        ExprEvaluator colonChild = createExprEvaluator();
        // ExprParserContext colonParserContext = new ExprParserContext(colonChild, false, false);
        exprParserContext.setContext(colonChild, false, false);
        do {
            parseNext(exprParserContext);
        } while (readable() && !exprParserContext.questionEndFlag && !exprParserContext.bracketEndFlag);
        displacementChain(colonChild);
        mergeLast(colonChild);

        // build
        ExprEvaluator questionColonEvaluator = createExprEvaluator();
        questionColonEvaluator.evalType = ExprEvaluator.EVAL_TYPE_OPERATOR; // only used for compress
        questionColonEvaluator.negate(negate).setLogicalNot(logicalNot);
        questionColonEvaluator.left = questionChild;
        questionColonEvaluator.right = colonChild;

        ExprEvaluator next = createExprEvaluator();
        next.left = questionColonEvaluator;
        evaluator.right = next;

        // continue next
        exprParserContext.setContext(next, false, false);
    }

    final static void parseStrToken(ExprParserContext exprParserContext, String strValue) {
        ExprEvaluator evaluator = exprParserContext.exprEvaluator;
        boolean negate = exprParserContext.negate;
        boolean logicalNot = exprParserContext.logicalNot;

        // 字符串常量
        ExprEvaluator.ConstantImpl left = new ExprEvaluator.ConstantImpl(strValue);
        left.negate = negate;
        left.logicalNot = logicalNot;
        evaluator.left = left;
        exprParserContext.setContext(evaluator, false, false);
    }

    final void parseListToken(ExprParserContext exprParserContext, List<Object> arrList) {
        ExprEvaluator evaluator = exprParserContext.exprEvaluator;
        boolean negate = exprParserContext.negate;
        boolean logicalNot = exprParserContext.logicalNot;
        ExprEvaluator left = new ExprEvaluator.ListImpl(arrList);
        left.negate = negate;
        left.logicalNot = logicalNot;

        evaluator.left = left;
        exprParserContext.setContext(evaluator, false, false);
    }

    /**
     * 从offset开始解析列表，直到endChar结束，多个子表达式使用逗号分隔.
     * 考虑到数据使用局限性，使用[]或者{}包起来的默认解析为集合（java.util.ArrayList），可以调用List的支持的方法.
     *
     * @param startChar 开始token字符
     * @param endChar   结束token字符
     * @param begin     开始位置
     * @return 列表
     */
    List<Object> parseList(int startChar, int endChar, int begin) {
        List<Object> arrayList = new ArrayList<Object>();
        int beginIndex = begin;
        int bracketCount = 0;
        int midBracketCount = 0;
        int bigBracketCount = 0;
        char currentChar = 0;
        while (readable()) {
            currentChar = read();
            // 判断字符串，检测常量
            if (currentChar == '\'') {
                boolean isBegin = readIndex == beginIndex;
                scanString();
                int endIndex = readIndex;
                ++readIndex;
                // 清除空白
                while (isWhitespace(currentChar = read())) {
                    ++this.readIndex;
                }
                if (isBegin) {
                    boolean isEnd = currentChar == endChar;
                    if ((isEnd || currentChar == ',')) {
                        arrayList.add(getString(beginIndex + 1, endIndex - beginIndex - 1));
                        // ++readIndex;
                        if (!isEnd) {
                            ++readIndex;
                            while (isWhitespace(currentChar = read())) {
                                ++this.readIndex;
                            }
                            beginIndex = readIndex;
                        } else {
                            return arrayList;
                        }
                    }
                }
                continue;
            }
            // 判断数字,检测常量
            if (NumberUtils.isDigit(currentChar) || currentChar == '-') {
                if (readIndex == beginIndex) {
                    boolean negate = currentChar == '-', decimal = false;
                    long val = negate ? 0L : currentChar & 0xF;
                    ++readIndex;
                    int cnt = 0, decimalCount = 0;
                    while (NumberUtils.isDigit(currentChar = read())) {
                        val = val * 10 + (currentChar & 0xf);
                        ++cnt;
                        ++readIndex;
                    }
                    if (currentChar == '.') {
                        decimal = true;
                        ++readIndex;
                        while (NumberUtils.isDigit(currentChar = read())) {
                            val = val * 10 + (currentChar & 0xf);
                            ++cnt;
                            ++decimalCount;
                            ++readIndex;
                        }
                    }
                    while (isWhitespace(currentChar)) {
                        ++this.readIndex;
                        currentChar = read();
                    }
                    boolean isEnd = currentChar == endChar;
                    if (isEnd || currentChar == ',') {
                        final boolean overflow = cnt > 18 && (cnt > 19 || val < 0);
                        if (overflow) {
                            if (decimal) {
                                arrayList.add(new BigDecimal(sourceChars, offset + beginIndex, readIndex - beginIndex));
                            } else {
                                arrayList.add(new BigInteger(getString(beginIndex, readIndex - beginIndex)));
                            }
                        } else {
                            if (decimal) {
                                double dv = NumberUtils.scientificToIEEEDouble(val, decimalCount);
                                arrayList.add(negate ? -dv : dv);
                            } else {
                                arrayList.add(negate ? -val : val);
                            }
                        }
                        if (!isEnd) {
                            ++readIndex;
                            while (isWhitespace(currentChar = read())) {
                                ++this.readIndex;
                            }
                            beginIndex = readIndex;
                            continue;
                        } else {
                            return arrayList;
                        }
                    }
                }
            }
            // 是否结束
            if (currentChar == endChar && bracketCount == 0 && midBracketCount == 0 && bigBracketCount == 0) {
                // 判定结束
                break;
            }
            switch (currentChar) {
                case '(': {
                    ++bracketCount;
                    break;
                }
                case ')': {
                    --bracketCount;
                    break;
                }
                case '[': {
                    ++midBracketCount;
                    break;
                }
                case ']': {
                    --midBracketCount;
                    break;
                }
                case '{': {
                    ++bigBracketCount;
                    break;
                }
                case '}': {
                    --bigBracketCount;
                    break;
                }
                case ',': {
                    if (bracketCount == 0 && midBracketCount == 0 && bigBracketCount == 0) {
                        String childEl = getString(beginIndex, readIndex - beginIndex).trim();
                        if (childEl.isEmpty()) {
                            arrayList.add(null);
                        } else {
                            ExprParser exprParser = new ExprChildParser(childEl, this);
                            arrayList.add(exprParser.isConstantExpr() ? exprParser.evaluate() : exprParser);
                        }
                        do {
                            ++this.readIndex;
                        } while (isWhitespace(currentChar = read()));
                        beginIndex = readIndex;
                        continue;
                    }
                }
            }
            ++readIndex;
        }
        if (currentChar != endChar) {
            String errorMessage = createErrorContextText(sourceChars, this.readIndex);
            throw new ExpressionException("syntax error, pos: " + this.readIndex + ", Expression[... " + errorMessage + " ],未找到与开始字符'" + startChar + "'相匹配的结束字符 '" + endChar + "'");
        }
        String childEl = getString(beginIndex, readIndex - beginIndex).trim();
        if (childEl.isEmpty()) {
            if (!arrayList.isEmpty()) {
                arrayList.add(null);
            }
        } else {
            ExprParser exprParser = new ExprChildParser(childEl, this);
            arrayList.add(exprParser.isConstantExpr() ? exprParser.evaluate() : exprParser);
        }
        return arrayList;
    }

    final void parseFunToken(ExprParserContext exprParserContext, String functionName, List<Object> args) {
        ExprEvaluator evaluator = exprParserContext.exprEvaluator;
        boolean negate = exprParserContext.negate;
        boolean logicalNot = exprParserContext.logicalNot;

        ExprEvaluator.FunctionImpl left = new ExprEvaluator.FunctionImpl(functionName, args);
        left.negate = negate;
        left.logicalNot = logicalNot;
        evaluator.left = left;
        exprParserContext.setContext(evaluator, false, false);
    }

    final void parseMethodToken(ExprParserContext exprParserContext, List<String> variableKeys, String
            methodName, List<Object> args) {
        ExprEvaluator evaluator = exprParserContext.exprEvaluator;
        boolean negate = exprParserContext.negate;
        boolean logicalNot = exprParserContext.logicalNot;

        checkInitializedInvokes();
        ElVariableInvoker variableInvoker = ElVariableUtils.build(variableKeys, getInvokes(), getTailInvokes());
        global().tailChainInvoker = variableInvoker;
        int kl = variableKeys.size();
        global().variableCount += kl;
        if (kl > 1) {
            global().existChain = true;
        }

        ExprEvaluator.MethodImpl left = new ExprEvaluator.MethodImpl(variableInvoker, methodName, args);
        left.negate = negate;
        left.logicalNot = logicalNot;
        evaluator.left = left;
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
        // skip whitespace
        while (readable() && isWhitespace(currentChar = read())) {
            ++this.readIndex;
        }
        if (isEnd()) {
            checkEndTokenSyntaxError(prevTokenType);
            return;
        }
        parseNextChar0(currentChar, exprParserContext);
    }

    private void parseNextChar0(char currentChar, ExprParserContext exprParserContext) {
        // Based on prediction rules, it seems more efficient than switch
        ElOperator elOperator;
        // Is it a minus sign operator
        boolean isMinusSymbol = false;
        if ((NumberUtils.isDigit(currentChar) || (isMinusSymbol = currentChar == '-')) && prevTokenType < NUM_TOKEN) {
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
                    // only supported long suffix eg: 0x123L
                    if (currentChar == 'l' || currentChar == 'L') {
                        ++this.readIndex;
                    }
                } else {
                    if (NumberUtils.isDigit(secondeDigitChar)) {
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
            double value;
            int mode = 0, suffix = 0, e10 = 0, decimalCount = 0;
            boolean expNegative = false;
            if (numberRadix == 10) {
                if (!isMinusSymbol && !valInitSet) {
                    decimalVal = currentChar & 0xf;
                    if (decimalVal != 0) {
                        ++cnt;
                    }
                }
                while (readable() && NumberUtils.isDigit(currentChar = read())) {
                    decimalVal = decimalVal * 10 + (currentChar & 0xf);
                    ++this.readIndex;
                    if (decimalVal != 0) {
                        ++cnt;
                    }
                }
                if (currentChar == '.') {
                    ++this.readIndex;
                    mode = 1;
                    // direct scan numbers
                    while (readable() && NumberUtils.isDigit(currentChar = read())) {
                        decimalVal = decimalVal * 10 + (currentChar & 0xf);
                        ++decimalCount;
                        if (decimalVal != 0) {
                            ++cnt;
                        }
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
                        if (NumberUtils.isDigit(currentChar)) {
                            e10 = currentChar & 0xf;
                            ++this.readIndex;
                            while (readable() && NumberUtils.isDigit(currentChar = read())) {
                                e10 = (e10 << 3) + (e10 << 1) + (currentChar & 0xf);
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
                        suffix = 1;
                        ++this.readIndex;
                        break;
                    }
                    case 'f':
                    case 'F': {
                        suffix = 2;
                        ++this.readIndex;
                        break;
                    }
                    case 'd':
                    case 'D': {
                        suffix = 3;
                        ++this.readIndex;
                        break;
                    }
                    case '$': {
                        // BigDecimal
                        suffix = 4;
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
                        parseOperatorToken(exprParserContext);
                    }
                }
            } else {
                this.tokenType = NUM_TOKEN;
                final boolean overflow = cnt > 18 && (cnt > 19 || decimalVal < 0);
                final boolean negate = exprParserContext.negate ^ isMinusSymbol;
                if (mode == 0 && !overflow) {
                    if (negate) {
                        value = -value;
                    }
                    if (suffix == 0) {
                        if (value >= Long.MIN_VALUE && value <= Long.MAX_VALUE) {
                            numberValue = (long) value;
                        } else {
                            numberValue = value;
                        }
                    } else if (suffix == 1) {
                        numberValue = (long) value;
                    } else if (suffix == 2) {
                        numberValue = (float) value;
                    } else if (suffix == 4) {
                        numberValue = BigDecimal.valueOf((long) value);
                    } else {
                        numberValue = value;
                    }
                } else {
                    do {
                        if (overflow) {
                            if (suffix == 4) {
                                numberValue = createBigDecimal(begin, this.readIndex - 1, negate);
                                break;
                            }
                            decimalVal = 0;
                            cnt = 0;
                            int j = begin, decimalPointIndex = this.readIndex;
                            char c;
                            decimalCount = 0;
                            for (; j < this.readIndex; ++j) {
                                if (NumberUtils.isDigit(c = sourceChars[offset + j])) {
                                    if (cnt < 18) {
                                        decimalVal = decimalVal * 10 + (c & 0xf);
                                    }
                                    if (decimalVal != 0) {
                                        ++cnt;
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
                        e10 = expNegative ? -e10 - decimalCount : e10 - decimalCount;
                        if (suffix == 4) {
                            numberValue = BigDecimal.valueOf(negate ? -decimalVal : decimalVal, -e10);
                            break;
                        }
                        value = NumberUtils.scientificToIEEEDouble(decimalVal, -e10);
                        if (negate) {
                            value = -value;
                        }
                        if (suffix == 0) {
                            numberValue = value;
                        } else if (suffix == 1) {
                            numberValue = (long) value;
                        } else if (suffix == 2) {
                            numberValue = (float) value;
                        } else {
                            numberValue = value;
                        }
                    } while (false);
                }
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
                    String readSource = createErrorContextText(sourceChars, this.readIndex);
                    throw new ExpressionException("syntax error, pos: " + this.readIndex + ", '=' is not supported, please use '==' instead, Expression[" + readSource + "]");
                }
            }
            checkOpsTokenSyntaxError();
            parseOperatorToken(exprParserContext);
        } else if (isBracketSymbol(currentChar)) {
            this.operator = ElOperator.BRACKET;
            // 括号
            ++this.readIndex;
            if (currentChar == '(') {
                this.tokenType = BRACKET_TOKEN;
                checkBeforeBracketTokenSyntaxError();
                parseBracketToken(exprParserContext);
            } else {
                this.tokenType = BRACKET_END_TOKEN;
                if (!exprParserContext.bracketMode) {
                    this.errorMsg = ")";
                    throwSyntaxError();
                }
                checkBeforeBracketEndTokenSyntaxError();
                exprParserContext.bracketEndFlag = true;
            }
        } else if (isQuestionColon(currentChar)) {
            // ? :
            // this.operator = ElOperator.QUESTION_COLON;
            ++this.readIndex;
            this.tokenType = OPS_TOKEN;
            if (currentChar == '?') {
                checkBeforeQuestionTokenSyntaxError();
                parseQuestionToken(exprParserContext);
            } else {
                // :
                if (!exprParserContext.questionMode) {
                    this.errorMsg = ":";
                    throwSyntaxError();
                }
                checkBeforeColonTokenSyntaxError();
                exprParserContext.questionEndFlag = true;
            }
        } else if (isIdentifierStart(currentChar)) {
            // 解析标识符开头可能的变量表达式例如a.b.c.d
            if (prevTokenType >= NUM_TOKEN) {
                this.errorMsg = String.valueOf(currentChar);
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
                            variableKeys.add(getString(localOffset, readIndex - localOffset));
                            localOffset = ++this.readIndex;

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
                                String identifierValue;
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
                                parseVariableToken(exprParserContext, startChar, null, variableKeys);
                                return;
                            }
                            continue;
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
                        parseVariableToken(exprParserContext, startChar, var, variableKeys);
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
            // 字符串
            int start = this.readIndex + 1;
            this.scanString();
            this.tokenType = STR_TOKEN;
            checkTokenSyntaxError();
            parseStrToken(exprParserContext, getString(start, readIndex - start));
            ++this.readIndex;
        } else if (currentChar == '[' || currentChar == '{') {
            // 统一转化ArrayList；
            final char endChar = currentChar == '{' ? '}' : ']';
            int start = ++this.readIndex;
            List<Object> arrayList = parseList(currentChar, endChar, start);
            this.tokenType = LIST_TOKEN;
            checkTokenSyntaxError();
            parseListToken(exprParserContext, arrayList);
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
            // find '('
            if (currentChar != '(') {
                String readSource = this.exprSource.substring(0, this.readIndex);
                throw new ExpressionException("syntax error, pos: " + this.readIndex + ", source: '" + readSource + "' function start symbol '(' not found !");
            }
            String functionName = getString(start, this.readIndex - start);
            List<Object> args = parseList('(', ')', ++this.readIndex);
            this.tokenType = FUN_TOKEN;
            checkTokenSyntaxError();
            parseFunToken(exprParserContext, functionName, args);
            ++this.readIndex;
        } else if ((currentChar == '.') && prevTokenType >= NUM_TOKEN) {
            int start = ++this.readIndex;
            // 一般出现在常量的成员访问(属性或者方法)，如果是变量不会进入此逻辑，请使用.访问,暂时不考虑对常量[]的支持，必须标识符开头；
            // update prev evaluator
            this.tokenType = prevTokenType;
            try {
                while (true) {
                    if (isIdentifierStart(currentChar = read())) {
                        ++this.readIndex;
                        while (readable() && isVariableAppend(currentChar = read())) {
                            ++this.readIndex;
                        }
                        String member = getString(start, this.readIndex - start);
                        while (isWhitespace(currentChar)) {
                            ++readIndex;
                            if (readable()) {
                                currentChar = read();
                            } else {
                                break;
                            }
                        }
                        if (currentChar == '(') {
                            this.tokenType = FUN_TOKEN;
                            List<Object> params = parseList(currentChar, ')', ++this.readIndex);
                            exprParserContext.exprEvaluator.left = new ExprEvaluator.MemberImpl(exprParserContext.exprEvaluator.left, true, member, params);
                            ++this.readIndex;
                            return;
                        } else {
                            exprParserContext.exprEvaluator.left = new ExprEvaluator.MemberImpl(exprParserContext.exprEvaluator.left, false, member, null);
                            if (currentChar == '.') {
                                start = ++this.readIndex;
                            } else {
                                break;
                            }
                        }
                    } else {
                        errorMsg = String.valueOf(currentChar);
                        throwUnsupportedError();
                        return;
                    }
                }
            } catch (Throwable throwable) {
                throw new ExpressionException("syntax error, pos: " + this.readIndex, throwable);
            }
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

    protected final Number createBigDecimal(int beginIndex, int endIndex, boolean negate) {
        BigDecimal value = new BigDecimal(sourceChars, offset + beginIndex, endIndex - beginIndex);
        return negate ? value.negate() : value;
    }

    private void handleParseFunctionToken(String identifierValue, boolean oneLevelAccess, List<String> variableKeys, ExprParserContext exprParserContext) {
        List<Object> params = parseList('(', ')', ++this.readIndex);
        this.tokenType = FUN_TOKEN;
        checkValueTokenSyntaxError();
        if (oneLevelAccess) {
            // static Function
            // @see @function
            parseFunToken(exprParserContext, identifierValue, params);
        } else {
            // reflect invoke : obj.method(...args)
            parseMethodToken(exprParserContext, variableKeys, identifierValue, params);
        }
        ++this.readIndex;
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
        if (currentChar != '\'') {
            String errorMessage = createErrorContextText(sourceChars, this.readIndex);
            throw new ExpressionException("syntax error, pos: " + this.readIndex + ", Expression[... " + errorMessage + " ],未找到与开始字符\"'\"相匹配的结束字符 \"'\"");
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
            case LIST_TOKEN:
            case FUN_TOKEN:
                return GROUP_TOKEN_VALUE;
            default:
                return 0;
        }
    }

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

    private void checkBeforeColonTokenSyntaxError() {
    }

    private void checkBeforeQuestionTokenSyntaxError() {
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
        return c == '_' || c == '$' || c == '.' /*|| c == '#'*/ || Character.isLetter(c) || NumberUtils.isDigit(c);
    }

    // 是否变量表达式字符
    final static boolean isVariableAppend(char c) {
        if (Character.isLetter(c)) return true;
        if (c <= ' ') return false;
        return /*c == '.' || */c == '_' || c == '$' || NumberUtils.isDigit(c) /*|| c == '[' *//*|| c == '('*/;
    }

    final static int digit(char c, int radix) {
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
                if (radix == 16) {
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


    final void resetToken() {
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
            if (symbol == '∈') {
                return this.operator = ElOperator.IN;
            } else if (symbol == '∉') {
                return this.operator = ElOperator.OUT;
            } else {
                return null;
            }
        }
        ElOperator elOperator = ElOperator.INDEXS_OPERATORS[symbol];
        if (elOperator != null) {
            this.operator = elOperator;
        }
        return elOperator;
    }

    /**
     * 是否括号标识符
     */
    final boolean isBracketSymbol(char c) {
        return c == '(' || c == ')';
    }

    /**
     * 是否三目运算符(?:)
     */
    final boolean isQuestionColon(char c) {
        return c == '?' || c == ':';
    }

    public Object evaluate() {
        return doEvaluate(EvaluatorContext.EMPTY, EvaluateEnvironment.DEFAULT);
    }

    public Object evaluate(Object context) {
        if (context instanceof EvaluateEnvironment) {
            return evaluate((EvaluateEnvironment) context);
        }
        // from pojo entity
        return doEvaluate(evaluatorContextBuilder.createEvaluatorContext(context), EvaluateEnvironment.DEFAULT); // evaluate(context, EvaluateEnvironment.DEFAULT);
    }

    @Override
    public final Object evaluate(Map context, long timeout) {
        return evaluate(context);
    }

    @Override
    public final Object evaluate(Object context, long timeout) {
        return evaluate(context);
    }

    public final Object evaluate(Map context) {
        return doEvaluate(evaluatorContextBuilder.createEvaluatorContext(context), EvaluateEnvironment.DEFAULT);
    }

    @Override
    public final Object evaluate(Map context, EvaluateEnvironment evaluateEnvironment) {
        if (variableSize == 0) {
            return doEvaluate(EvaluatorContext.EMPTY, evaluateEnvironment);
        } else {
            if (evaluateEnvironment.computable) {
                return doEvaluate(evaluatorContextBuilder.createEvaluatorContext(evaluateEnvironment.computedVariables(context)), evaluateEnvironment);
            }
            return doEvaluate(evaluatorContextBuilder.createEvaluatorContext(context), evaluateEnvironment);
        }
    }

    final Object evaluateInternal(Map context, EvaluateEnvironment evaluateEnvironment) {
        return doEvaluate(evaluatorContextBuilder.createEvaluatorContext(context), evaluateEnvironment);
    }

    @Override
    public final Object evaluate(Object context, EvaluateEnvironment evaluateEnvironment) {
        if (variableSize > 0 && evaluateEnvironment.computable) {
            context = evaluateEnvironment.computedVariables(context);
        }
        return doEvaluate(evaluatorContextBuilder.createEvaluatorContext(context), evaluateEnvironment);
    }

    @Override
    public final Object evaluate(EvaluateEnvironment evaluateEnvironment) {
        if (variableSize == 0 || evaluateEnvironment == null) {
            return exprEvaluator.evaluate(EvaluatorContext.EMPTY, evaluateEnvironment);
        }
        if (evaluateEnvironment.isMapContext()) {
            return doEvaluate(evaluatorContextBuilder.createEvaluatorContext((Map) evaluateEnvironment.computedVariables()), evaluateEnvironment);
        } else {
            return doEvaluate(evaluatorContextBuilder.createEvaluatorContext(evaluateEnvironment.computedVariables()), evaluateEnvironment);
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
        int evalType = exprEvaluator.evalType;
        if (evalType == 0) {
            return compressEvaluator(left);
        } else if (evalType == ExprEvaluator.EVAL_TYPE_OPERATOR) {
            left = compressEvaluator(left);
            right = compressEvaluator(right);
            ElOperator elOperator = exprEvaluator.operator;
            switch (elOperator) {
                case EXP:
                    // **指数,平方/根
                    return ExprEvaluator.PowerImpl.of(exprEvaluator.update(left, right));
                case MULTI:
                    // *乘法
                    return ExprEvaluator.MultiplyImpl.of(exprEvaluator.update(left, right));
                case DIVISION:
                    // /除法
                    return ExprEvaluator.DivisionImpl.of(exprEvaluator.update(left, right));
                case MOD:
                    // %取余数
                    return ExprEvaluator.ModulusImpl.of(exprEvaluator.update(left, right));
                case PLUS:
                    if (right.negate) {
                        right.negate = false;
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
                case AND:
                    // &
                    return ExprEvaluator.BitAndImpl.of(exprEvaluator.update(left, right));
                case XOR:
                    // ^
                    return ExprEvaluator.BitXorImpl.of(exprEvaluator.update(left, right));
                case OR:
                    // |
                    return ExprEvaluator.BitOrImpl.of(exprEvaluator.update(left, right));
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
//                case COLON:
//                    // : 三目运算结果, 不支持单独运算
//                case QUESTION:
                default:
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
        } else if (evalType == ExprEvaluator.EVAL_TYPE_QUESTION) {
            // 三目运算
            return ExprEvaluator.TernaryImpl.of(exprEvaluator.update(compressEvaluator(left), compressEvaluator(right)));
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

    String getString(int begin, int count) {
        return new String(sourceChars, offset + begin, count);
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

    public final void optimize() {
        this.exprEvaluator = exprEvaluator.optimize();
    }

    final boolean isConstantExpr() {
        return exprEvaluator.isConstantExpr();
    }
}