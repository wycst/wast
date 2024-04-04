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
package io.github.wycst.wast.json;

import io.github.wycst.wast.common.reflect.GenericParameterizedType;
import io.github.wycst.wast.common.reflect.SetterInfo;
import io.github.wycst.wast.common.tools.Base64;
import io.github.wycst.wast.json.exceptions.JSONException;
import io.github.wycst.wast.json.options.ReadOption;

import java.io.Serializable;
import java.lang.reflect.Array;
import java.math.BigDecimal;
import java.util.*;

/**
 * <ol>
 * <li> 根据输入的字符串或者字符数组生成一个JSON树
 * <li> 只关注的节点数据可以根据需要进行解析，而无需解析完整的JSON字符串.
 * <li> 两种构建方式的区别(parse和from都是基于按需解析):
 * <br> parse: 扫描完整的json内容，但只对路径覆盖的节点进行解析，其他只做扫描和位置计算;只需要parse一次，并支持全局查找搜索；
 * <br> from: 扫描到路径指定节点的结束字符后直接返回，并对提取的内容进行解析生成根节点，效率比parse高(超大文本解析下优势明显)，但只支持局部查找;支持懒加载；
 * <li> 节点路径统一以'/'为分隔符，数组元素通过[n]下标访问，例如： '/students/[0]/name'.
 * <li> 支持json内容中存在注释(非json标准规范).
 * </ol>
 * <p>
 * example:
 * <pre>
 *
 * String json = "{\"age\":25,\"name\":\"Miss Zhang\",\"students\":[{\"name\":\"Li Lei\",\"age\":12},{\"age\":16,\"name\":\"Mei Mei Han\"}]}";
 *
 * // root
 * JSONNode jsonNode = JSONNode.parse(json);
 *
 * // Navigate to the specified node (students)
 * JSONNode studentsNode = JSONNode.parse(json, "/students"); // students
 *
 * // Extract fragment string as root node
 * JSONNode studentsNode = JSONNode.from(json, "/students"); // students为根节点
 *
 * 1. Get the properties of the node, such as name and age)
 *
 *    String name = jsonNode.getChildValue("name", String.class);
 *    int age = jsonNode.getChildValue("age", int.class);
 *
 * 2. Supports global arbitrary path access, using the getpathvalue method
 *
 *   eg: Get the name and age of the first student
 *
 *    String studentName1 = jsonNode.getPathValue("/students/[0]/name", String.class);
 *    int studentAge = jsonNode.getPathValue("/students/[0]/age", int.class);
 *
 * 3. Get the node object under any path
 *
 *    JSONNode student1 = jsonNode.get("/students/[0]");
 *    JSONNode student2 = jsonNode.get("/students/[1]");
 *
 * 4. Convert node to entity bean
 *
 *    JSONNode studentNode = jsonNode.get("/students/[0]");
 *    Student student = studentNode.toBean(Student.class);
 *
 * </pre>
 * <p>
 * More usage self discovery ...
 *
 * @Author: wangyunchao
 * @see JSONNode#isLeaf()
 * @see JSONNode#toList(Class)
 * @see JSONNode#toDate(Class)
 * @see JSONNode#toDate(Class, String)
 * @see JSON
 * @see JSONReader
 * @see JSONCharArrayWriter
 */
public final class JSONNode extends JSONGeneral implements Comparable<JSONNode> {

    // root
    private JSONNode root;
    private final JSONNodeContext parseContext;

    // parent
    private JSONNode parent;
    // source
    private final char[] buf;
    // begin pos
    private int beginIndex;
    // end pos
    private int endIndex;
    // 文本
    private String text;
    // current cursor pos, finish set -1
    private int offset;
    // if completed ?
    private boolean completed;

    // Object mapping
    private Map<Serializable, JSONNode> fieldValues;
    // list
    private JSONNode[] elementValues;
    // Actual length of array（length <= elementValues.size）
    private int length;

    // Type: 1 object; 2 array; 3. String; 4 number; 5 boolean; 6 null
    private int type;
    public final static int OBJECT = 1;
    public final static int ARRAY = 2;
    public final static int STRING = 3;
    public final static int NUMBER = 4;
    public final static int BOOLEAN = 5;
    public final static int NULL = 6;

    // is an Array
    private final boolean isArray;
    // Leaf node
    private final boolean leaf;
    // Leaf node value
    private Serializable leafValue;

    /**
     * 内容是否修改状态
     */
    private boolean changed;

    /**
     * 构建JSON对象节点 (Building JSON object nodes)
     * <p> 实例为根节点
     * <p>The instance is the root node
     *
     * @param buf
     * @param beginIndex
     * @param endIndex
     */
    JSONNode(char[] buf, int beginIndex, int endIndex, JSONNodeContext parseContext) {
        this.buf = buf;
        this.beginIndex = beginIndex;
        this.endIndex = endIndex;
        this.parseContext = parseContext;
        this.root = this;
        this.init();
        this.leaf = type > 2;
        this.isArray = type == 2;
        this.offset = this.beginIndex;
    }

    /***
     * 根据已解析属性数据构建数组根节点(局部根)
     *
     * @param elementValues
     * @param buf
     * @param beginIndex
     * @param endIndex
     * @param parseContext
     */
    JSONNode(List<JSONNode> elementValues, char[] buf, int beginIndex, int endIndex, JSONNodeContext parseContext) {
        this.length = elementValues.size();
        this.elementValues = elementValues.toArray(new JSONNode[length]);
        this.completed = true;
        this.isArray = true;
        this.leaf = false;
        this.type = ARRAY;
        this.parseContext = parseContext;
        this.buf = buf;
        this.beginIndex = beginIndex;
        this.endIndex = endIndex;
        this.root = this;
    }

    /***
     * 根据已解析属性数据构建对象根节点(局部根)
     *
     * @param fieldValues
     * @param buf
     * @param beginIndex
     * @param endIndex
     * @param parseContext
     */
    JSONNode(Map<Serializable, JSONNode> fieldValues, char[] buf, int beginIndex, int endIndex, JSONNodeContext parseContext) {
        this.fieldValues = fieldValues;
        this.completed = true;
        this.isArray = false;
        this.leaf = false;
        this.type = OBJECT;
        this.parseContext = parseContext;
        this.buf = buf;
        this.beginIndex = beginIndex;
        this.endIndex = endIndex;
        this.root = this;
    }

    /***
     * 根据已解析属性数据构建对象根节点(局部根)
     *
     * @param leafValue
     * @param parseContext
     */
    JSONNode(Serializable leafValue, char[] buf, int beginIndex, int endIndex, int type, JSONNodeContext parseContext) {
        this.leafValue = leafValue;
        this.completed = true;
        this.isArray = false;
        this.leaf = true;
        this.parseContext = parseContext;
        this.buf = buf;
        this.beginIndex = beginIndex;
        this.endIndex = endIndex;
        this.type = type;
    }

    /**
     * 构建子节点只有内部构建 （Build child nodes have only internal builds）
     *
     * @param buf
     * @param beginIndex
     * @param endIndex
     * @param type
     * @param parseContext
     * @param rootNode
     */
    private JSONNode(char[] buf, int beginIndex, int endIndex, int type, JSONNodeContext parseContext, JSONNode rootNode) {
        this.buf = buf;
        this.beginIndex = beginIndex;
        this.endIndex = endIndex;
        this.parseContext = parseContext;
        this.root = rootNode;
        this.type = type;
        this.leaf = type > 2;
        this.isArray = type == 2;
        this.offset = this.beginIndex;
    }

    /**
     * 解析完整文档生成全局根节点 （Parse source string to generate root node）
     *
     * @param source
     * @return
     */
    public static JSONNode parse(String source, ReadOption... readOptions) {
        return parse(source, null, readOptions);
    }

    /**
     * 解析完整文档生成全局根节点
     *
     * @param source
     * @return
     */
    public static JSONNode parse(String source, boolean reverseParseNode, ReadOption... readOptions) {
        return parse(source, null, reverseParseNode, readOptions);
    }

    /**
     * 解析完整文档生成全局根节点并定位指定path节点
     *
     * @param source 标准JSON字符串
     * @param path   1 通过字符'/'访问根以及分割路径； 2 使用[n]访问数组下标；
     *               <p> 1. Access the root and split path through '/' off;
     *               <p> 2. use [n] to access the array subscript;
     * @return
     */
    public static JSONNode parse(String source, String path, ReadOption... readOptions) {
        return parse(source, path, false, readOptions);
    }

    /***
     * 根据指定path生成json根节点(局部根)
     *
     * @param source
     * @param path
     * @param readOptions
     * @return
     */
    public static JSONNode from(String source, String path, ReadOption... readOptions) {
        return from(source, path, false, readOptions);
    }

    /***
     * 根据指定path生成json根节点(局部根)
     *
     * @param source        数据
     * @param path          路径
     * @param lazy          懒加载
     * @param readOptions   配置项
     * @return
     */
    public static JSONNode from(String source, String path, boolean lazy, ReadOption... readOptions) {
        return from(getChars(source), path, lazy, readOptions);
    }

    /**
     * 根据指定path生成json根节点(局部根)
     *
     * @param buf
     * @param path
     * @param readOptions
     * @return
     */
    public static JSONNode from(char[] buf, String path, ReadOption... readOptions) {
        return from(buf, path, false, readOptions);
    }

    /***
     * 根据指定path生成json根节点(局部根)
     *
     * @param buf
     * @param path
     * @param lazy 延迟加载
     * @param readOptions
     * @return
     */
    public static JSONNode from(char[] buf, String path, boolean lazy, ReadOption... readOptions) {
        JSONNodeContext parseContext = new JSONNodeContext();
        JSONOptions.readOptions(readOptions, parseContext);
        parseContext.lazy = lazy;
        int toIndex = buf.length;
        if (path == null || (path = path.trim()).length() == 0) {
            return new JSONNode(buf, 0, toIndex, parseContext);
        }
        if (!path.startsWith("/")) {
            path = "/" + path;
        }
        return parseNode(buf, path, false, parseContext);
    }

    /**
     * 提取指定路径的value,统一返回列表
     *
     * @param json
     * @param path
     * @param readOptions
     * @return
     */
    public static List extract(String json, String path, ReadOption... readOptions) {
        return extract(getChars(json), path, readOptions);
    }

    /**
     * 提取指定路径的value,统一返回列表
     *
     * @param buf
     * @param path
     * @param readOptions
     * @return
     */
    public static List extract(char[] buf, String path, ReadOption... readOptions) {
        JSONNodeContext parseContext = new JSONNodeContext();
        JSONOptions.readOptions(readOptions, parseContext);
        parseContext.extract = true;
        if (!path.startsWith("/")) {
            path = "/" + path;
        }
        parseNode(buf, path, false, parseContext);
        return parseContext.getExtractValues();
    }

    private static JSONNode parseNode(char[] buf, String path, boolean skipValue, JSONNodeContext parseContext) {
        int toIndex = buf.length;
        JSONNode result;
        try {
            int fromIndex = 0;
            // Trim remove white space characters
            char beginChar = '\0';
            while ((fromIndex < toIndex) && (beginChar = buf[fromIndex]) <= ' ') {
                fromIndex++;
            }
            while ((toIndex > fromIndex) && buf[toIndex - 1] <= ' ') {
                toIndex--;
            }

            boolean allowComment = parseContext.allowComment;
            if (allowComment && beginChar == '/') {
                /** 去除声明在头部的注释*/
                fromIndex = clearCommentAndWhiteSpaces(buf, fromIndex + 1, toIndex, parseContext);
                beginChar = buf[fromIndex];
            }

            // ignore null / true / false / number of the root
            switch (beginChar) {
                case '{':
                    result = parseObjectPathNode(buf, fromIndex, toIndex, path, 0, skipValue, true, parseContext);
                    break;
                case '[':
                    result = parseArrayPathNode(buf, fromIndex, toIndex, path, 0, skipValue, true, parseContext);
                    break;
                case '"':
                    result = parseStringPathNode(buf, fromIndex, toIndex, skipValue, parseContext);
                    break;
                default:
                    throw new UnsupportedOperationException("Unsupported for begin character with '" + beginChar + "'");
            }

            // validate
            if (parseContext.validate) {
                int endIndex = parseContext.endIndex;
                if (endIndex != toIndex - 1) {
                    int wordNum = Math.min(50, buf.length - endIndex - 1);
                    throw new JSONException("Syntax error, extra characters found, '" + new String(buf, endIndex + 1, wordNum) + "', at pos " + endIndex);
                }
            }
            return result;
        } catch (Exception ex) {
            handleCatchException(ex, buf, toIndex);
            throw new JSONException("Error: " + ex.getMessage(), ex);
        }
    }


    /**
     * 解析对象，以{开始，直到遇到}结束 （Resolve the object, starting with '{', until '}' is encountered）
     *
     * @param buf              缓冲数组（char[]）
     * @param fromIndex        开始位置（Start index of '{'）
     * @param toIndex          最大索引位置（Maximum end index）
     * @param jsonParseContext 上下文配置（context config）
     * @return 对象（object）
     * @throws Exception 异常(Exception)
     */
    private static JSONNode parseObjectPathNode(char[] buf, int fromIndex, int toIndex, String path, int beginPathIndex, boolean skipValue, boolean returnIfMatched, JSONNodeContext jsonParseContext) throws Exception {
        int beginIndex = fromIndex + 1;
        char ch;
        String key = null;
        boolean empty = true;
        boolean allowComment = jsonParseContext.allowComment;
        Map<Serializable, JSONNode> fieldValues = null;
        boolean matched = false;
        boolean isLastPathLevel = false;
        int nextPathIndex = -1;
        if (!skipValue) {
            if (beginPathIndex == -1) {
                // 最后一级对象节点
                isLastPathLevel = true;
            } else {
                nextPathIndex = path.indexOf('/', beginPathIndex + 1);
                // path以/结尾
                isLastPathLevel = /*nextPathIndex == -1 ||*/ beginPathIndex == path.length() - 1;
            }
        }
        if (isLastPathLevel) {
            fieldValues = new LinkedHashMap<Serializable, JSONNode>();
        }
        boolean lazyParseLastNode = isLastPathLevel && jsonParseContext.lazy;
        for (int i = beginIndex; i < toIndex; ++i) {
            while ((ch = buf[i]) <= ' ') {
                ++i;
            }
            if (jsonParseContext.allowComment) {
                if (ch == '/') {
                    ch = buf[i = clearCommentAndWhiteSpaces(buf, i + 1, toIndex, jsonParseContext)];
                }
            }

            int fieldKeyFrom = i, fieldKeyTo;
            boolean isUnquotedFieldName = false;
            if (ch == '"') {
                while (i + 1 < toIndex && (buf[++i] != '"' || buf[i - 1] == '\\')) ;
                empty = false;
                ++i;
            } else {
                if (ch == '}') {
                    if (!empty) {
                        throw new JSONException("Syntax error, the closing symbol '}' is not allowed at pos " + i);
                    }
                    jsonParseContext.endIndex = i;
                    return null;
                }
                if (ch == '\'') {
                    if (jsonParseContext.allowSingleQuotes) {
                        while (i + 1 < toIndex && buf[++i] != '\'') ;
                        empty = false;
                        ++i;
                    } else {
                        throw new JSONException("Syntax error, the single quote symbol ' is not allowed at pos " + i);
                    }
                } else {
                    if (jsonParseContext.allowUnquotedFieldNames) {
                        while (i + 1 < toIndex && buf[++i] != ':') ;
                        empty = false;
                        isUnquotedFieldName = true;
                    }
                }
            }
            while ((ch = buf[i]) <= ' ') {
                ++i;
            }
            fieldKeyTo = i;
            if (allowComment) {
                if (ch == '/') {
                    ch = buf[i = clearCommentAndWhiteSpaces(buf, i + 1, toIndex, jsonParseContext)];
                }
            }
            if (ch == ':') {
                if (!skipValue) {
                    key = JSONDefaultParser.parseKeyOfMap(buf, fieldKeyFrom, fieldKeyTo, isUnquotedFieldName);
                    if (!isLastPathLevel) {
                        matched = stringEqual(path, beginPathIndex + 1, (nextPathIndex == -1 ? path.length() : nextPathIndex) - beginPathIndex - 1, key, 0, key.length());
                    }
                }
                while ((ch = buf[++i]) <= ' ') ;
                if (allowComment) {
                    if (ch == '/') {
                        ch = buf[i = clearCommentAndWhiteSpaces(buf, i + 1, toIndex, jsonParseContext)];
                    }
                }
                boolean isLeafValue = false;
                JSONNode value;
                boolean isSkipValue = isLastPathLevel ? false : !matched || skipValue;
                boolean lazy = lazyParseLastNode || nextPathIndex == -1;
                switch (ch) {
                    case '{': {
                        if (lazy) {
                            isSkipValue = true;
                        }
                        value = parseObjectPathNode(buf, i, toIndex, path, nextPathIndex, isSkipValue, returnIfMatched, jsonParseContext);
                        if (lazy) {
                            value = new JSONNode(buf, i, jsonParseContext.endIndex + 1, jsonParseContext);
                        }
                        i = jsonParseContext.endIndex;
                        break;
                    }
                    case '[': {
                        if (lazy) {
                            isSkipValue = true;
                        }
                        // 2 [ array
                        value = parseArrayPathNode(buf, i, toIndex, path, nextPathIndex, isSkipValue, returnIfMatched, jsonParseContext);
                        if (lazy) {
                            value = new JSONNode(buf, i, jsonParseContext.endIndex + 1, jsonParseContext);
                        }
                        i = jsonParseContext.endIndex;
                        break;
                    }
                    case '"': {
                        // 3 string
                        isLeafValue = true;
                        value = parseStringPathNode(buf, i, toIndex, isSkipValue, jsonParseContext);
                        i = jsonParseContext.endIndex;
                        break;
                    }
                    case 'n': {
                        // null
                        isLeafValue = true;
                        value = parseNullPathNode(buf, i, toIndex, jsonParseContext);
                        i = jsonParseContext.endIndex;
                        break;
                    }
                    case 't': {
                        isLeafValue = true;
                        value = parseBoolTruePathNode(buf, i, toIndex, jsonParseContext);
                        i = jsonParseContext.endIndex;
                        break;
                    }
                    case 'f': {
                        isLeafValue = true;
                        value = parseBoolFalsePathNode(buf, i, toIndex, jsonParseContext);
                        i = jsonParseContext.endIndex;
                        break;
                    }
                    default: {
                        // number
                        isLeafValue = true;
                        value = parseNumberPathNode(buf, i, toIndex, '}', jsonParseContext);
                        i = jsonParseContext.endIndex;
                        break;
                    }
                }
                while ((ch = buf[++i]) <= ' ') ;
                if (allowComment) {
                    // clearComment and append whiteSpaces
                    if (ch == '/') {
                        ch = buf[i = clearCommentAndWhiteSpaces(buf, i + 1, toIndex, jsonParseContext)];
                    }
                }
                if (matched) {
                    if (isLeafValue && nextPathIndex > -1) {
                        throw new JSONException(String.format("path '%s' error, '%s' is the last leaf level, The following path '%s' does not exist ", path, path.substring(0, nextPathIndex), path.substring(nextPathIndex)));
                    }
                    if ((isLastPathLevel || isLeafValue || nextPathIndex == -1) && jsonParseContext.extract) {
                        jsonParseContext.extractValue(isLeafValue ? value.leafValue : value);
                    }
                    jsonParseContext.endIndex = i;
                    if (returnIfMatched) {
                        return value;
                    }
                }
                boolean isClosingSymbol = ch == '}';
                if (ch == ',' || isClosingSymbol) {
                    if (fieldValues != null) {
                        fieldValues.put(key, value);
                    }
                    if (isClosingSymbol) {
                        jsonParseContext.endIndex = i;
                        if (isLastPathLevel) {
                            return new JSONNode(fieldValues, buf, fromIndex, i + 1, jsonParseContext);
                        }
                        return null;
                    }
                } else {
                    throw new JSONException("Syntax error, unexpected '" + ch + "', position " + i);
                }
            } else {
                throw new JSONException("Syntax error, unexpected '" + ch + "', position " + i);
            }
        }
        throw new JSONException("Syntax error, the closing symbol '}' is not found ");
    }

    private static boolean stringEqual(String path, int s1, int len1, String key, int s2, int len2) {
        if (len1 != len2) return false;
        for (int i = 0; i < len1; ++i) {
            if (path.charAt(s1 + i) != key.charAt(s2 + i)) return false;
        }
        return true;
    }

    private static JSONNode parseArrayPathNode(char[] buf, int fromIndex, int toIndex, String path, int beginPathIndex, boolean skipValue, boolean returnIfMatched, JSONNodeContext jsonParseContext) throws Exception {
        int beginIndex = fromIndex + 1;
        char ch;
        List<JSONNode> elementValues = null;
        boolean allowComment = jsonParseContext.allowComment;
        int elementIndex = 0;
        boolean fetchAllElement = false;
        boolean matched = false;
        boolean isLastPathLevel = false;
        boolean returnValueIfMathched = false;
        int targetElementIndex = -1;
        final int EqualMode = 0, GtMode = 1, LtMode = -1, AllMode = 2;
        int compareMode = EqualMode;
        int nextPathIndex = -1;
        if (!skipValue) {
            if (beginPathIndex == -1) {
                isLastPathLevel = true;
                matched = true;
                fetchAllElement = true;
            } else {
                nextPathIndex = path.indexOf('/', beginPathIndex + 1);
                // 斜杠位置
                int endPathIndex = nextPathIndex == -1 ? path.length() : nextPathIndex;
                // 约定几种支持规则；
                // 1 以[开始，以]结束或者数字串
                // 2 * 匹配所有；
                // 3 n+ 索引号大于或者等于n;
                // 4 n- 索引号小于或者等于n;
                // 5 n  索引号等于n;
                // 6 其他不支持抛出异常；
                if (beginPathIndex + 1 < endPathIndex) {
                    int numBeginIndex;
                    int numEndIndex;
                    if (path.charAt(beginPathIndex + 1) == '[' && path.charAt(endPathIndex - 1) == ']') {
                        numBeginIndex = beginPathIndex + 2;
                        numEndIndex = endPathIndex - 1;
                    } else {
                        numBeginIndex = beginPathIndex + 1;
                        numEndIndex = endPathIndex;
                    }
                    int len = numEndIndex - numBeginIndex;
                    if (len <= 0) {
                        throw new UnsupportedOperationException("Path error, array element access must use [n] or n, n is a int value ");
                    }
                    char endCharOfPath = path.charAt(numEndIndex - 1);
                    switch (endCharOfPath) {
                        case '*':
                            if (len == 1) {
                                compareMode = AllMode;
                                fetchAllElement = true;
                                matched = true;
                                break;
                            } else {
                                // not support ,use GtMode instead
                            }
                        case '+':
                            compareMode = GtMode;
                        case '-':
                            if (compareMode == EqualMode) compareMode = LtMode;
                            numEndIndex--;
                        default:
                            if (compareMode == EqualMode) {
                                returnValueIfMathched = true;
                            }
                            targetElementIndex = readArrayIndex(getChars(path), numBeginIndex, numEndIndex - numBeginIndex);
                    }
                }
                isLastPathLevel = nextPathIndex == -1 || beginPathIndex == path.length() - 1;
            }
        }
        if (isLastPathLevel || !returnValueIfMathched) {
            elementValues = new ArrayList<JSONNode>();
        }
        for (int i = beginIndex; i < toIndex; ++i) {
            boolean returnListIfMathched = false;
            if (!skipValue && !fetchAllElement) {
                int index = elementIndex++;
                switch (compareMode) {
                    case LtMode:
                        matched = index <= targetElementIndex;
                        returnListIfMathched = index == targetElementIndex;
                        break;
                    case EqualMode:
                        matched = index == targetElementIndex;
                        break;
                    case GtMode:
                        matched = index >= targetElementIndex;
                        break;
                    default:
                        matched = true;
                }
            }
            while ((ch = buf[i]) <= ' ') {
                ++i;
            }
            if (allowComment) {
                if (ch == '/') {
                    ch = buf[i = clearCommentAndWhiteSpaces(buf, i + 1, toIndex, jsonParseContext)];
                }
            }
            if (ch == ']') {
                if (elementValues != null && elementValues.size() > 0) {
                    throw new JSONException("Syntax error, not allowed ',' followed by ']', pos " + i);
                }
                jsonParseContext.endIndex = i;
                return null;
            }
            boolean isLeafValue = false;
            boolean isSkipValue = !matched || skipValue;
            JSONNode value;
            switch (ch) {
                case '{': {
                    value = parseObjectPathNode(buf, i, toIndex, path, nextPathIndex, isSkipValue, returnValueIfMathched, jsonParseContext);
                    break;
                }
                case '[': {
                    // 2 [ array
                    value = parseArrayPathNode(buf, i, toIndex, path, nextPathIndex, isSkipValue, returnValueIfMathched, jsonParseContext);
                    break;
                }
                case '"': {
                    isLeafValue = true;
                    // 3 string
                    value = parseStringPathNode(buf, i, toIndex, isSkipValue, jsonParseContext);
                    break;
                }
                case 'n': {
                    // null
                    isLeafValue = true;
                    value = parseNullPathNode(buf, i, toIndex, jsonParseContext);
                    break;
                }
                case 't': {
                    isLeafValue = true;
                    value = parseBoolTruePathNode(buf, i, toIndex, jsonParseContext);
                    break;
                }
                case 'f': {
                    isLeafValue = true;
                    value = parseBoolFalsePathNode(buf, i, toIndex, jsonParseContext);
                    break;
                }
                default: {
                    // number
                    isLeafValue = true;
                    value = parseNumberPathNode(buf, i, toIndex, ']', jsonParseContext);
                    break;
                }
            }
            if (matched && elementValues != null) {
                elementValues.add(value);
            }
            i = jsonParseContext.endIndex;
            while (i + 1 < toIndex && (ch = buf[++i]) <= ' ') ;
            if (allowComment) {
                if (ch == '/') {
                    ch = buf[i = clearCommentAndWhiteSpaces(buf, i + 1, toIndex, jsonParseContext)];
                }
            }
            if (matched) {
                if (isLeafValue && nextPathIndex > -1) {
                    throw new JSONException(String.format("path '%s' error, '%s' is the last level, The following path '%s' does not exist ", path, path.substring(0, nextPathIndex), path.substring(nextPathIndex)));
                }
                if (isLastPathLevel && jsonParseContext.extract) {
                    jsonParseContext.extractValue(isLeafValue ? value.leafValue : value);
                }
                if (returnIfMatched && returnValueIfMathched) {
                    return value;
                }
                if (returnListIfMathched) {
                    if (isLastPathLevel) {
                        return new JSONNode(elementValues, buf, fromIndex, i + 1, jsonParseContext);
                    } else {
                        return null;
                    }
                }
            }
            boolean isEnd = ch == ']';
            if (ch == ',' || isEnd) {
                if (isEnd) {
                    jsonParseContext.endIndex = i;
                    if (isLastPathLevel) {
                        return new JSONNode(elementValues, buf, fromIndex, i + 1, jsonParseContext);
                    }
                    return null;
                }
            } else {
                throw new JSONException("Syntax error, unexpected '" + ch + "', position " + i + ", Missing ',' or '}'");
            }
        }
        throw new JSONException("Syntax error, the closing symbol ']' is not found ");
    }

    private static JSONNode parseStringPathNode(char[] buf, int fromIndex, int toIndex, boolean skipValue, JSONNodeContext jsonParseContext) throws Exception {
        if (skipValue) {
            JSONTypeDeserializer.CHAR_SEQUENCE_STRING.skip(null, buf, fromIndex, '"', jsonParseContext);
            return null;
        }
        String value = (String) JSONTypeDeserializer.CHAR_SEQUENCE_STRING.deserializeString(null, buf, fromIndex, toIndex, '"', GenericParameterizedType.StringType, jsonParseContext);
        int endIndex = jsonParseContext.endIndex;
        return new JSONNode(value, buf, fromIndex, endIndex + 1, STRING, jsonParseContext);
    }

    private static JSONNode parseNullPathNode(char[] buf, int fromIndex, int toIndex, JSONNodeContext jsonParseContext) throws Exception {
        JSONTypeDeserializer.NULL.deserialize(null, buf, fromIndex, toIndex, null, null, jsonParseContext);
        int endIndex = jsonParseContext.endIndex;
        return new JSONNode((Serializable) null, buf, fromIndex, endIndex + 1, NULL, jsonParseContext);
    }

    private static JSONNode parseBoolTruePathNode(char[] buf, int fromIndex, int toIndex, JSONNodeContext jsonParseContext) throws Exception {
        JSONTypeDeserializer.BOOLEAN.deserializeTrue(buf, fromIndex, toIndex, null, jsonParseContext);
        int endIndex = jsonParseContext.endIndex;
        return new JSONNode(true, buf, fromIndex, endIndex + 1, BOOLEAN, jsonParseContext);
    }

    private static JSONNode parseBoolFalsePathNode(char[] buf, int fromIndex, int toIndex, JSONNodeContext jsonParseContext) throws Exception {
        JSONTypeDeserializer.BOOLEAN.deserializeFalse(buf, fromIndex, toIndex, null, jsonParseContext);
        int endIndex = jsonParseContext.endIndex;
        return new JSONNode(false, buf, fromIndex, endIndex + 1, BOOLEAN, jsonParseContext);
    }

    private static JSONNode parseNumberPathNode(char[] buf, int fromIndex, int toIndex, char endToken, JSONNodeContext jsonParseContext) throws Exception {
        Number value = JSONTypeDeserializer.NUMBER.deserializeDefault(buf, fromIndex, toIndex, endToken, jsonParseContext);
        int endIndex = jsonParseContext.endIndex;
        return new JSONNode(value, buf, fromIndex, endIndex + 1, NUMBER, jsonParseContext);
    }

    private static JSONNode parse(String source, String path, boolean reverseParseNode, ReadOption... readOptions) {
        if (source == null)
            return null;
        source = source.trim();
        try {
            char[] buf = getChars(source);
            JSONNodeContext parseContext = new JSONNodeContext();
            JSONOptions.readOptions(readOptions, parseContext);
            parseContext.reverseParseNode = reverseParseNode;
            JSONNode jsonNode = new JSONNode(buf, 0, buf.length, parseContext);
            if (path == null) {
                return jsonNode;
            }
            return jsonNode.get(path);
        } catch (Throwable throwable) {
            if (throwable instanceof JSONException) {
                throw (JSONException) throwable;
            }
            throw new JSONException(throwable.getMessage(), throwable);
        }
    }

    private void init() {
        char start = '\0';
        char end = '\0';
        while ((beginIndex < endIndex) && ((start = buf[beginIndex]) <= ' ')) {
            beginIndex++;
        }
        while ((endIndex > beginIndex) && ((end = buf[endIndex - 1]) <= ' ')) {
            endIndex--;
        }
        if (start == '{' && end == '}') {
            type = OBJECT;
        } else if (start == '[' && end == ']') {
            type = ARRAY;
        } else if (start == '"' && end == '"') {
            type = STRING;
        } else {
            text = new String(buf, beginIndex, endIndex - beginIndex);
            boolean isTrue;
            if (isTrue = text.equals("true") || text.equals("false")) {
                type = BOOLEAN;
                leafValue = isTrue;
            } else if (text.equals("null")) {
                type = NULL;
                leafValue = null;
            } else {
                // 数字暂时不解析
                type = NUMBER;
            }
        }
    }

    /**
     * 获取根节点 root
     *
     * @return
     */
    public JSONNode root() {
        return root;
    }

    /**
     * 获取指定路径的对象
     * <p>如果path为空将返回当前JSONNode节点对象 </p>
     * <p>如果/开头表示从根节点开始查找 </p>
     * <p>数组元素使用[n]</p>
     *
     * @param childPath 查找路径，以/作为级别分割线
     * @return
     */
    public JSONNode get(String childPath) {
        char[] pathBuf = getChars(childPath.trim());
        return get(pathBuf, 0, pathBuf.length);
    }

    private JSONNode get(char[] pathBuf, int beginIndex, int endIndex) {
        if (pathBuf == null || (endIndex - beginIndex) == 0)
            return this;
        char beginChar = pathBuf[beginIndex];
        if (beginChar == '/') {
            return root.get(pathBuf, beginIndex + 1, endIndex);
        }
        if (leaf) {
            return null;
        }
        int splitIndex = -1;
        int hashValue = beginChar;
        char ch;
        for (int i = beginIndex + 1; i < endIndex; ++i) {
            if ((ch = pathBuf[i]) == '/') {
                splitIndex = i;
                break;
            }
            hashValue = hashValue * 31 + ch;
        }
        if (splitIndex == -1) {
            return getPathNode(pathBuf, beginIndex, endIndex - beginIndex, hashValue);
        } else {
            JSONNode childNode = getPathNode(pathBuf, beginIndex, splitIndex - beginIndex, hashValue);
            if (childNode != null) {
                return childNode.get(pathBuf, splitIndex + 1, endIndex);
            }
        }
        return null;
    }

    /**
     * 获取所有的字段名称
     *
     * @return
     */
    public Collection<Serializable> keyNames() {
        if (isArray) {
            throw new UnsupportedOperationException();
        }
        if (!completed) {
            parseFullNode();
        }
        return fieldValues.keySet();
    }

    private JSONNode getPathNode(char[] buf, int offset, int len, int hashCode) {
        if (isArray) {
            char ch = buf[offset];
            int digit = digitDecimal(ch);
            int index = -1;
            try {
                if (digit > -1) {
                    index = readArrayIndex(buf, offset, len);
                } else {
                    char endChar = buf[offset + len - 1];
                    if (ch == '[' && endChar == ']') {
                        index = readArrayIndex(buf, offset + 1, len - 2);
                    }
                }
            } catch (Throwable throwable) {
                String key = new String(buf, offset, len);
                throw new IllegalArgumentException(" key '" + key + " is mismatch array index ");
            }
            if (index > -1) {
                return getElementAt(index);
            } else {
                String key = new String(buf, offset, len);
                throw new IllegalArgumentException(" key '" + key + " is mismatch array index ");
            }
        } else {
            // key
            String field = parseContext.getCacheKey(buf, offset, len, hashCode);
            return getFieldNodeAt(field);
        }
    }

    private JSONNode getFieldNodeAt(String field) {
        JSONNode value = null;
        if (fieldValues == null) {
            fieldValues = new LinkedHashMap(16);
        } else {
            value = fieldValues.get(field);
        }
        if (value != null) {
            return value;
        }
        if (completed) {
            return null;
        }
        return parseFieldNode(field, false);
    }

    // 如果是数组获取指定下标节点，使用[n]
    private JSONNode getPathNode(String field) {
        if (isArray) {
            char[] buf = getChars(field);
            int index = -1;
            try {
                if (buf[0] == '[' && buf[buf.length - 1] == ']') {
                    index = readArrayIndex(buf, 1, buf.length - 2);
                } else {
                    index = readArrayIndex(buf, 0, buf.length);
                }
            } catch (NumberFormatException exception) {
                throw new IllegalArgumentException(" field '" + field + " is mismatch array index ");
            }
            if (index > -1) {
                return getElementAt(index);
            } else {
                throw new IllegalArgumentException(" key '" + field + " is mismatch array index ");
            }
        } else {
            return getFieldNodeAt(field);
        }
    }

    protected static int readArrayIndex(char[] buf, int offset, int len) {
        if (len == 0) return -1;
        int value = 0;
        char ch;
        for (int i = offset, n = i + len; i < n; ++i) {
            ch = buf[i];
            int d = digitDecimal(ch);
            if (d < 0) {
                if (ch == ' ') continue;
                throw new NumberFormatException("For input string: \"" + new String(buf, offset, len) + "\"");
            }
            value = value * 10 + d;
        }
        return value;
    }

    private JSONNode parseFieldNode(String fieldName, boolean skipIfUnleaf) {
        if (completed) {
            return fieldValues.get(fieldName);
        }
        char ch;
        boolean allowComment = parseContext.allowComment;
        boolean empty = true;
        for (int i = offset + 1; i < endIndex; ++i) {
            while ((ch = buf[i]) <= ' ') {
                ++i;
            }
            if (allowComment) {
                if (ch == '/') {
                    ch = buf[i = clearCommentAndWhiteSpaces(buf, i + 1, endIndex, parseContext)];
                }
            }
            int fieldKeyFrom = i;
            String key;
            boolean matchedField;
            if (ch == '"') {
                key = JSONDefaultParser.parseMapKeyByCache(buf, i, endIndex, '"', parseContext);
                empty = false;
                i = parseContext.endIndex + 1;
            } else {
                if (ch == '}') {
                    if (!empty) {
                        throw new JSONException("Syntax error, the closing symbol '}' is not allowed at pos " + i);
                    }
                    offset = i;
                    completed = true;
                    return null;
                }
                if (ch == '\'') {
                    if (parseContext.allowSingleQuotes) {
                        while (i + 1 < endIndex && (buf[++i] != '\'' || buf[i - 1] == '\\')) ;
                        empty = false;
                        ++i;
                        key = JSONDefaultParser.parseKeyOfMap(buf, fieldKeyFrom, i, false);
                    } else {
                        throw new JSONException("Syntax error, the single quote symbol ' is not allowed at pos " + i);
                    }
                } else {
                    if (parseContext.allowUnquotedFieldNames) {
                        while (i + 1 < endIndex && buf[++i] != ':') ;
                        empty = false;
                        key = JSONDefaultParser.parseKeyOfMap(buf, fieldKeyFrom, i, true);
                    } else {
                        String errorContextTextAt = createErrorContextText(buf, i);
                        throw new JSONException("Syntax error, at pos " + i + ", context text by '" + errorContextTextAt + "', unexpected '" + ch + "', expected '\"' or use option ReadOption.AllowUnquotedFieldNames ");
                    }
                }
            }
            while ((ch = buf[i]) <= ' ') {
                ++i;
            }
            if (allowComment) {
                if (ch == '/') {
                    ch = buf[i = clearCommentAndWhiteSpaces(buf, i + 1, endIndex, parseContext)];
                }
            }
            if (ch == ':') {
                matchedField = fieldName != null && fieldName.equals(key);
                while ((ch = buf[++i]) <= ' ') ;
                if (allowComment) {
                    if (ch == '/') {
                        ch = buf[i = clearCommentAndWhiteSpaces(buf, i + 1, endIndex, parseContext)];
                    }
                }
                JSONNode value;
                try {
                    value = parseValueNode(i, '}');
                } catch (Throwable throwable) {
                    if (throwable instanceof JSONException) {
                        throw (JSONException) throwable;
                    }
                    throw new JSONException(throwable.getMessage(), throwable);
                }
                i = parseContext.endIndex;
                while ((ch = buf[++i]) <= ' ') ;
                if (allowComment) {
                    if (ch == '/') {
                        ch = buf[i = clearCommentAndWhiteSpaces(buf, i + 1, endIndex, parseContext)];
                    }
                }
                offset = i;
                boolean isClosingSymbol = ch == '}';
                if (ch == ',' || isClosingSymbol) {
                    fieldValues.put(key, value);
                    if (isClosingSymbol) {
                        completed = true;
                    }
                    if (matchedField) {
                        return value;
                    }
                } else {
                    throw new JSONException("Syntax error, unexpected '" + ch + "', position " + i);
                }
            } else {
                throw new JSONException("Syntax error, unexpected '" + ch + "', position " + i);
            }
        }
        return null;
    }

    private JSONNode parseElementNodeAt(int index) {
        char ch;
        boolean allowComment = parseContext.allowComment, matchedIndex;
        for (int i = offset + 1; i < endIndex; ++i) {
            while ((ch = buf[i]) <= ' ') {
                ++i;
            }
            if (allowComment) {
                if (ch == '/') {
                    ch = buf[i = clearCommentAndWhiteSpaces(buf, i + 1, endIndex, parseContext)];
                }
            }
            if (ch == ']') {
                if (length > 0) {
                    throw new JSONException("Syntax error, not allowed ',' followed by ']', pos " + i);
                }
                offset = i;
                completed = true;
                return null;
            }
            matchedIndex = index > -1 && index == length;

            JSONNode value;
            try {
                value = parseValueNode(i, ']');
            } catch (Throwable throwable) {
                if (throwable instanceof JSONException) {
                    throw (JSONException) throwable;
                }
                throw new JSONException(throwable.getMessage(), throwable);
            }
            i = parseContext.endIndex;
            while ((ch = buf[++i]) <= ' ') ;
            if (allowComment) {
                if (ch == '/') {
                    ch = buf[i = clearCommentAndWhiteSpaces(buf, i + 1, endIndex, parseContext)];
                }
            }
            offset = i;
            boolean isEnd = ch == ']';
            if (ch == ',' || isEnd) {
                addElementNode(value);
                if (isEnd) {
                    completed = true;
                }
                if (matchedIndex) {
                    return value;
                }
            } else {
                throw new JSONException("Syntax error, unexpected '" + ch + "', position " + i + ", Missing ',' or '}'");
            }
        }

        return null;
    }

    private void addElementNode(JSONNode value) {
        if (elementValues == null) {
            elementValues = new JSONNode[16];
        } else {
            // add to list
            if (this.length == elementValues.length) {
                JSONNode[] tmp = elementValues;
                elementValues = new JSONNode[this.length << 1];
                System.arraycopy(tmp, 0, elementValues, 0, tmp.length);
            }
        }
        elementValues[length++] = value;
    }

    private JSONNode parseValueNode(int i, char endChar) throws Exception {
        JSONNode value;
        char ch = buf[i];
        switch (ch) {
            case '{': {
                JSONTypeDeserializer.ANY.skip(null, buf, i, endIndex, '}', parseContext);
                int eIndex = parseContext.endIndex;
                value = new JSONNode(buf, i, eIndex, OBJECT, parseContext, root);
                break;
            }
            case '[': {
                JSONTypeDeserializer.ANY.skip(null, buf, i, endIndex, ']', parseContext);
                int eIndex = parseContext.endIndex;
                value = new JSONNode(buf, i, eIndex, ARRAY, parseContext, root);
                break;
            }
            case '"': {
                // 3 string
                value = parseStringPathNode(buf, i, endIndex, false, parseContext);
                break;
            }
            case 'n': {
                // null
                value = parseNullPathNode(buf, i, endIndex, parseContext);
                break;
            }
            case 't': {
                value = parseBoolTruePathNode(buf, i, endIndex, parseContext);
                break;
            }
            case 'f': {
                value = parseBoolFalsePathNode(buf, i, endIndex, parseContext);
                break;
            }
            default: {
                // number
                value = parseNumberPathNode(buf, i, endIndex, endChar, parseContext);
                break;
            }
        }
        value.parent = this;
        return value;
    }

    /**
     * <p> 根据索引获取
     * <p> 如果是数组将参数作为索引返回下标元素
     *
     * @param index
     * @return
     */
    public JSONNode getElementAt(int index) {
        if (!isArray) {
            throw new UnsupportedOperationException();
        }
        if (completed || length > index) {
            return elementValues[index];
        }
        return parseElementNodeAt(index);
    }

    /**
     * <p> 返回指定节点在json中的总子节点数量，如果解析完毕直接返回，否则进行解析(数组)
     *
     * @return 元素数量
     */
    public int getElementCount() {
        if (isArray) {
            if (completed) {
                return length;
            }
            parseElementNodeAt(-1);
            return length;
        }
        return -1;
    }

    /**
     * 直接返回当前解析的数组长度
     *
     * @return
     */
    public int getLength() {
        return length;
    }


    /**
     * 返回父节点
     *
     * @return
     */
    public JSONNode parent() {
        return this.parent;
    }

    /**
     * <p> 直接获取子节点
     * <p> 注：请在确保已经完成解析的情况下调用，否则请调用get(field)
     *
     * @param field
     * @return
     */
    public JSONNode child(String field) {
        if (isArray) {
            throw new UnsupportedOperationException();
        }
        return fieldValues.get(field);
    }

    /**
     * <p> 获取指定路径的对象并类型转化
     *
     * @param childPath
     * @param clazz
     * @param <E>
     * @return
     */
    public <E> E getPathValue(String childPath, Class<E> clazz) {
        JSONNode jsonNode = get(childPath);
        if (jsonNode == null)
            return null;
        if (jsonNode.isArray) {
            if (clazz == String.class) {
                return (E) jsonNode.source();
            }
            throw new UnsupportedOperationException("Type is not matched !");
        }
        return jsonNode.getValue(clazz);
    }

    /**
     * <p> 获取下一级的node对象值并转化
     *
     * @param name
     * @param eClass
     * @param <E>
     * @return
     */
    public <E> E getChildValue(String name, Class<E> eClass) {
        JSONNode jsonNode = getPathNode(name);
        if (jsonNode == null)
            return null;
        if (jsonNode.isArray) {
            if (eClass == String.class) {
                return (E) jsonNode.source();
            }
            throw new UnsupportedOperationException("Type is not matched !");
        }
        return jsonNode.getValue(eClass);
    }

    /**
     * 获取文本
     *
     * @return
     */
    public String getText() {
        if (text == null) {
            if (type == STRING) {
                text = (String) JSONTypeDeserializer.CHAR_SEQUENCE_STRING.deserializeString(null, buf, beginIndex, endIndex, '"', GenericParameterizedType.StringType, parseContext);
                leafValue = text;
            } else {
                return source();
            }
        }
        return text;
    }

    /**
     * <p> 转化为日期
     *
     * @param eClass  指定日期类
     * @param pattern 指定日期格式
     * @return
     */
    public Date toDate(Class<? extends Date> eClass, String pattern) {
        return (Date) parseDateValue(beginIndex, endIndex, buf, pattern, null, eClass == null ? Date.class : eClass);
    }

    /**
     * 转化为日期
     *
     * @param eClass
     * @return
     */
    public Date toDate(Class<? extends Date> eClass) {
        return (Date) parseDateValue(beginIndex, endIndex, buf, null, null, eClass == null ? Date.class : eClass);
    }

    // 返回字符串值
    public String getStringValue() {
        return getValue(String.class);
    }

    // 返回int值
    public int getIntValue() {
        return getValue(Integer.class);
    }

    // 返回long值
    public long getLongValue() {
        return getValue(Long.class);
    }

    // 返回double值
    public double getDoubleValue() {
        return getValue(Double.class);
    }

    /***
     * 返回叶子节点的值
     *
     * @return
     */
    public Serializable getValue() {
        if (this.leafValue != null) return this.leafValue;
        return getValue(null);
    }

    /**
     * 将节点转化为目标类型
     *
     * @param eClass
     * @param <E>
     * @return
     */
    public <E> E getValue(Class<E> eClass) {
        if (leaf) {
            if (eClass != null && eClass.isInstance(leafValue)) {
                return (E) leafValue;
            }
            if (type == NULL) {
                return null;
            }
            String source;
            if (eClass == null) {
                if (type == STRING) {
                    // 字符串
                    return (E) getText();
                }
                source = source();
                if (type == NUMBER) {
                    return (E) toDefaultNumber(source);
                }
                return (E) source;
            }
            // 字符串
            if (eClass == String.class) {
                return (E) getText();
            }
            // 字符数组
            if (eClass == char[].class) {
                return (E) getText().toCharArray();
            }
            // 字节数组
            if (eClass == byte[].class) {
                String text = getText();
                if (parseContext.byteArrayFromHexString) {
                    return (E) hexString2Bytes(buf, beginIndex + 1, endIndex - beginIndex - 2);
                } else {
                    return (E) Base64.getDecoder().decode(text);
                }
            }
            // 枚举类
            if (Enum.class.isAssignableFrom(eClass)) {
                if (type == STRING) {
                    Class enumCls = eClass;
                    return (E) Enum.valueOf(enumCls, getText());
                } else {
                    throw new JSONException("source [" + source() + "] cannot convert to Enum '" + eClass + "");
                }
            }
            // others
            source = source();
            if (type == NUMBER) {
                // 数字
                if (eClass == double.class || eClass == Double.class) {
                    return (E) Double.valueOf(source);
                } else if (eClass == long.class || eClass == Long.class) {
                    return (E) Long.valueOf(source);
                } else if (eClass == float.class || eClass == Float.class) {
                    return (E) Float.valueOf(source);
                } else if (eClass == int.class || eClass == Integer.class) {
                    return (E) Integer.valueOf(source);
                } else if (eClass == byte.class || eClass == Byte.class) {
                    return (E) Byte.valueOf(source);
                } else if (eClass == BigDecimal.class) {
                    return (E) new BigDecimal(source);
                } else {
                    return (E) toDefaultNumber(source);
                }
            } else if (type == BOOLEAN) {
                // boollean
                if (eClass == boolean.class || eClass == Boolean.class) {
                    return (E) leafValue;
                }
            } else if (eClass.isEnum()) {
                Class cls = eClass;
                return (E) Enum.valueOf(cls, source);
            }
            // 其他
            return (E) source;
        } else {
            // 解析对象
            if (isArray) {
                if (eClass == String.class) {
                    return (E) getText();
                }
                throw new UnsupportedOperationException(" Please use toList or toArray method instead !");
            }
            if (eClass != null) {
                if (eClass.isEnum() || eClass.isPrimitive() || eClass.isArray()) {
                    throw new UnsupportedOperationException(" Type is Unsupported for '" + eClass + "'");
                }
            }
            return toBean(eClass);
        }
    }

    private Number toDefaultNumber(String source) {
        if (source.indexOf('.') > -1) {
            return Double.valueOf(source);
        } else {
            return Long.valueOf(source);
        }
    }

    /**
     * 以指定value泛型转化map
     *
     * @param eClass
     * @param valueCls
     * @param <E>
     * @return
     */
    public <E> Map<?, E> toMap(Class<? extends Map> eClass, Class<E> valueCls) {
        return toBean(eClass, valueCls);
    }

    /**
     * 转化为实体bean对象
     *
     * @param eClass
     * @param <E>
     * @return
     */
    public <E> E toBean(Class<E> eClass) {
        return toBean(eClass, null);
    }

    /**
     * 转化为实体bean对象
     *
     * @param eClass
     * @param actualCls
     * @param <E>
     * @return
     */
    private <E> E toBean(Class<E> eClass, Class actualCls) {
        if (eClass == String.class) {
            return (E) this.source();
        }
        if (leaf) {
            throw new UnsupportedOperationException(" NodeValue is a leaf value, please call method getValue width Java basic type param instead !");
        }
        Object instance;
        boolean isMapInstance;
        JSONPojoStructure classStructureWrapper = null;
        if (eClass == null || eClass == Map.class || eClass == LinkedHashMap.class) {
            instance = new LinkedHashMap();
            isMapInstance = true;
        } else {
            classStructureWrapper = JSONPojoStructure.get(eClass);
            isMapInstance = classStructureWrapper.isAssignableFromMap();
            try {
                if (!isMapInstance) {
                    instance = classStructureWrapper.newInstance();
                } else {
                    instance = eClass.newInstance();
                }
            } catch (Throwable throwable) {
                throw new JSONException("Create instance error.", throwable);
            }
        }
        if (!completed) {
            parseFullNode();
        }
        for (Serializable key : fieldValues.keySet()) {
            JSONNode childNode = fieldValues.get(key);
            if (isMapInstance) {
                Map map = (Map) instance;
                Object value;
                if (childNode.isArray) {
                    value = childNode.toList(null);
                } else {
                    value = childNode.getValue(actualCls);
                }
                map.put(key, value);
            } else {
                String fieldName = key.toString();
                JSONPojoFieldDeserializer fieldDeserializer = classStructureWrapper.getFieldDeserializer(fieldName);
                SetterInfo setterInfo = fieldDeserializer == null ? null : fieldDeserializer.getSetterInfo();
                if (setterInfo != null) {
                    Class<?> parameterType = setterInfo.getParameterType();
                    Class<?> entityClass = parameterType;
                    Class<?> collCls = null;
                    Class<?> actualTypeArgumentType = setterInfo.getActualTypeArgument();
                    if (actualTypeArgumentType != null) {
                        // 取范型的类
                        entityClass = actualTypeArgumentType;
                        collCls = parameterType;
                    }

                    Object value;
                    if (childNode.isArray) {
                        value = childNode.toCollection(collCls, entityClass);
                    } else {
                        if (Date.class.isAssignableFrom(parameterType)) {
                            value = parseDateValue(beginIndex, endIndex, buf, fieldDeserializer.getDatePattern(), fieldDeserializer.getDateTimezone(), (Class<? extends Date>) parameterType);
                        } else {
                            value = childNode.getValue(entityClass);
                        }
                    }
                    setterInfo.invoke(instance, value);
                }
            }
        }
        return (E) instance;
    }

    /**
     * 转化为List对象
     *
     * @param entityClass
     * @param <E>
     * @return
     */
    public <E> List<E> toList(Class<E> entityClass) {
        return (List<E>) toCollection(ArrayList.class, entityClass);
    }

    /**
     * 转化为数组对象
     *
     * @param entityClass
     * @param <E>
     * @return
     */
    public <E> E[] toArray(Class<E> entityClass) {
        return (E[]) toCollection(Object[].class, entityClass);
    }

    /**
     * 转化为指定集合对象
     *
     * @param collectionCls
     * @param entityClass
     * @return
     */
    public Object toCollection(Class<?> collectionCls, Class<?> entityClass) {
        if (!completed) {
            parseFullNode();
        }
        Class target = entityClass;
        Collection collection = null;
        boolean isArrayCls = false;
        Object arrayObj = null;

        if (collectionCls == null || collectionCls == ArrayList.class) {
            collection = new ArrayList<Object>(length);
        } else {
            isArrayCls = collectionCls.isArray();
            if (isArrayCls) {
                arrayObj = Array.newInstance(entityClass, length);
            } else {
                if (collectionCls.isInterface()) {
                    if (Set.class.isAssignableFrom(collectionCls)) {
                        collection = new LinkedHashSet<Object>();
                    } else if (List.class.isAssignableFrom(collectionCls)) {
                        collection = new ArrayList<Object>();
                    }
                } else {
                    if (collectionCls == HashSet.class) {
                        collection = new HashSet<Object>();
                    } else if (collectionCls == Vector.class) {
                        collection = new Vector<Object>();
                    } else {
                    }
                }
            }
        }
        for (int i = 0; i < length; ++i) {
            JSONNode element = elementValues[i];
            Object result = null;
            if (element.isArray) {
                result = element.toList(null);
            } else {
                result = element.getValue(target);
            }
            if (isArrayCls) {
                Array.set(arrayObj, i, result);
            } else {
                collection.add(result);
            }
        }
        return isArrayCls ? arrayObj : collection;
    }

    private void parseFullNode() {
        if (isArray) {
            parseElementNodeAt(-1);
        } else {
            if (fieldValues == null) {
                fieldValues = new LinkedHashMap(16);
            }
            parseFieldNode(null, false);
        }
    }

    /**
     * 获取当前Node的源字符串
     *
     * @return
     */
    public String source() {

        if (leafValue != null && type != STRING) {
            return String.valueOf(this.leafValue);
        }

        // 如果是number类型返回
        String source = null;

        if (type > STRING) {
            // 复杂类型
            if (text == null) {
                source = new String(buf, beginIndex, endIndex - beginIndex);
                text = source;
            }
            return text;
        }

        return new String(buf, beginIndex, endIndex - beginIndex);
    }

    public boolean isCompleted() {
        return completed;
    }

    public boolean isArray() {
        return isArray;
    }

    public boolean isLeaf() {
        return leaf;
    }

    public int getType() {
        return type;
    }

    public int compareTo(JSONNode o) {
        Serializable value = leafValue;
        Serializable o1 = o.leafValue;

        if (value instanceof Number && o1 instanceof Number) {
            Double v1 = ((Number) value).doubleValue();
            Double v2 = ((Number) o1).doubleValue();
            return v1.compareTo(v2);
        }

        if (value instanceof String && o1 instanceof String) {
            return ((String) value).compareTo((String) o1);
        }

        /** 同类型比较 */
        if (value instanceof Comparable && o1 instanceof Comparable) {
            if (value.getClass() == o1.getClass()) {
                return ((Comparable) value).compareTo(o1);
            }
            String v1 = value.toString();
            String v2 = o1.toString();
            return v1.compareTo(v2);
        }

        return 0;
    }

    private void writeTo(StringBuilder stringBuilder) {
        if (!this.changed) {
            writeSourceTo(stringBuilder);
            return;
        }
        switch (type) {
            case OBJECT:
                stringBuilder.append('{');
                int len = fieldValues.size();
                int i = 0;
                for (Map.Entry<Serializable, JSONNode> entry : fieldValues.entrySet()) {
                    String key = entry.getKey().toString();
                    JSONNode value = entry.getValue();
                    stringBuilder.append('"').append(key).append("\":");
                    value.writeTo(stringBuilder);
                    if (i++ < len - 1) {
                        stringBuilder.append(',');
                    }
                }
                stringBuilder.append('}');
                break;
            case ARRAY:
                stringBuilder.append('[');
                for (int j = 0; j < length; ++j) {
                    JSONNode value = elementValues[j];
                    value.writeTo(stringBuilder);
                    if (j < length - 1) {
                        stringBuilder.append(',');
                    }
                }
                stringBuilder.append(']');
                break;
            case STRING:
                stringBuilder.append('"');
                writeStringTo((String) this.leafValue, stringBuilder);
                stringBuilder.append('"');
                break;
            default:
                stringBuilder.append(this.leafValue);
        }
    }

    private void writeSourceTo(StringBuilder stringBuilder) {
        stringBuilder.append(buf, beginIndex, endIndex - beginIndex);
    }

    /**
     * 转义处理
     */
    private static void writeStringTo(String leafValue, StringBuilder content) {
        int len = leafValue.length();
        int beginIndex = 0;
        for (int i = 0; i < len; ++i) {
            char ch = leafValue.charAt(i);
            String escapeStr;
            if ((ch > '"' && ch != '\\') || (escapeStr = ESCAPE_VALUES[ch]) == null) continue;
            int length = i - beginIndex;
            if (length > 0) {
                content.append(leafValue, beginIndex, i);
            }
            content.append(escapeStr);
            beginIndex = i + 1;
        }
        content.append(leafValue, beginIndex, len);
    }

    /**
     * 给指定路径节点设值
     * <p> 只支持叶子节点
     *
     * @param path
     * @param value
     */
    public void setPathValue(String path, Serializable value) {
        JSONNode node = get(path);
        if (node != null && node.leaf) {
            node.setLeafValue(value);
        }
    }

    /**
     * 清除指定位置元素
     */
    public void removeElementAt(int index) {
        if (!isArray) {
            throw new UnsupportedOperationException();
        }
        if (index >= length || index < 0) {
            throw new IndexOutOfBoundsException(" length: " + length + ", to remove index at " + index);
        }
        if (!completed) {
            parseFullNode();
        }
        int newLen = length - 1;
        JSONNode[] newElementNodes = new JSONNode[newLen];
        System.arraycopy(elementValues, 0, newElementNodes, 0, index);
        System.arraycopy(elementValues, index + 1, newElementNodes, index, newLen - index);
        length = newLen;
        handleChange();
    }

    /**
     * 清除指定位置元素
     */
    public JSONNode removeField(String field) {
        if (type != OBJECT) {
            throw new UnsupportedOperationException();
        }
        if (!completed) {
            parseFullNode();
        }
        JSONNode removeNode = fieldValues.remove(field);
        if (removeNode != null) {
            handleChange();
        }
        return removeNode;
    }

    /**
     * 返回集合元素中根据指定路径元素的值组成新的数组
     *
     * @param childPath
     * @return
     */
    public List collect(String childPath) {
        return collect(childPath, String.class);
    }

    /***
     * 返回集合元素中根据指定路径元素的值映射为typeClass类型，组成新的数组
     *
     * @param childPath
     * @param typeClass
     * @return
     */
    public List collect(String childPath, Class typeClass) {
        if (!isArray) {
            throw new UnsupportedOperationException();
        }
        if (!this.completed) {
            this.parseFullNode();
        }
        List result = new ArrayList();
        for (int i = 0; i < length; ++i) {
            JSONNode jsonNode = elementValues[i];
            result.add(jsonNode.getPathValue(childPath, typeClass));
        }
        return result;
    }

    /**
     * 设置叶子节点值
     */
    public void setLeafValue(Serializable value) {
        if (value == this.leafValue) {
            return;
        }
        this.leafValue = value;
        this.updateType();
        this.handleChange();
    }

    private void updateType() {
        if (leafValue == null) {
            this.type = NULL;
        } else if (leafValue instanceof Number) {
            this.type = NUMBER;
        } else if (leafValue instanceof Boolean) {
            this.type = BOOLEAN;
        } else if (type == STRING) {
            this.type = STRING;
            this.text = (String) leafValue;
        } else {
        }
    }

    /**
     * 传播值变化信息
     */
    private void handleChange() {
        if (parent != null) {
            parent.handleChange();
        }
        changed = true;
    }

    /***
     * 将节点转化为json字符串
     *
     * @return
     */
    public String toJsonString() {
        if (!this.changed) {
            return this.source();
        }
        // 开始重新序列化
        StringBuilder stringBuilder = new StringBuilder();
        writeTo(stringBuilder);
        return stringBuilder.toString();
    }

}
