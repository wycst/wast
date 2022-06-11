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
package io.github.wycst.wast.json;

import io.github.wycst.wast.common.reflect.ClassStructureWrapper;
import io.github.wycst.wast.common.reflect.ReflectConsts;
import io.github.wycst.wast.common.reflect.SetterInfo;
import io.github.wycst.wast.common.tools.Base64;
import io.github.wycst.wast.json.exceptions.JSONException;
import io.github.wycst.wast.json.options.JSONNodeContext;
import io.github.wycst.wast.json.options.Options;
import io.github.wycst.wast.json.options.ReadOption;

import java.io.Serializable;
import java.lang.reflect.Array;
import java.math.BigDecimal;
import java.util.*;

/**
 * <ol>
 * <li> 根据输入的字符串或者字符数组生成一个JSON树
 * <br> Generate a JSON tree according to the input string or character array
 * <li> 只关注的节点数据可以根据需要进行解析，而无需解析完整的JSON字符串.
 * <br> the node data that is only concerned can be parsed on demand without parsing the complete JSON string
 * <li> 两种构建方式的区别(parse和from都是基于按需解析):
 * <br> parse: 扫描完整的json内容，但只对路径覆盖的节点进行解析，其他只做扫描和位置计算;只需要parse一次，并支持全局查找搜索；
 * <br> from: 扫描到路径指定节点的结束字符后直接返回，并对提取的内容进行解析生成根节点，效率比parse高(超大文本解析下优势明显)，但只支持局部查找;支持懒加载；
 * <li> 节点路径统一以'/'为分隔符，数组元素通过[n]下标访问，例如： '/students/[0]/name'.
 * <br> Node paths are uniformly separated by '/', and array elements are accessed through [n] subscripts, such as '/students/[0]/name'
 * <li> 支持json内容中存在注释(非json标准规范).
 * <br> Support JSON annotation
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
 * String name = jsonNode.getChildValue("name", String.class);
 * int age = jsonNode.getChildValue("age", int.class);
 *
 * 2. Supports global arbitrary path access, using the getpathvalue method
 *
 * eg: Get the name and age of the first student
 *
 * String studentName1 = jsonNode.getPathValue("/students/[0]/name", String.class);
 * int studentAge = jsonNode.getPathValue("/students/[0]/age", int.class);
 *
 * 3. Get the node object under any path
 *
 * JSONNode student1 = jsonNode.get("/students/[0]");
 * JSONNode student2 = jsonNode.get("/students/[1]");
 *
 * 4. Convert node to entity bean
 * JSONNode studentNode = jsonNode.get("/students/[0]");
 * Student student = studentNode.toBean(Student.class);
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
 * @see JSONWriter
 */
public final class JSONNode extends JSONGeneral implements Comparable<JSONNode> {

    // root
    private final JSONNode root;
    private final JSONNodeContext parseContext;

    // parent
    private JSONNode parent;
    // source
    private final char[] buffers;
    // begin pos
    private int beginIndex;
    // end pos
    private int endIndex;
    // 文本
    private String text;
    // current cursor pos, finish set -1
    private int offset;
    private int quotTokenOffset = 0;
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
     * @param buffers
     * @param beginIndex
     * @param endIndex
     */
    JSONNode(char[] buffers, int beginIndex, int endIndex, JSONNodeContext parseContext) {
        this.buffers = buffers;
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
     * @param buffers
     * @param beginIndex
     * @param endIndex
     * @param parseContext
     */
    JSONNode(List<JSONNode> elementValues, char[] buffers, int beginIndex, int endIndex, JSONNodeContext parseContext) {
        this.length = elementValues.size();
        this.elementValues = elementValues.toArray(new JSONNode[length]);
        this.completed = true;
        this.isArray = true;
        this.leaf = false;
        this.type = ARRAY;
        this.parseContext = parseContext;
        this.buffers = buffers;
        this.beginIndex = beginIndex;
        this.endIndex = endIndex;
        this.root = this;
    }

    /***
     * 根据已解析属性数据构建对象根节点(局部根)
     *
     * @param fieldValues
     * @param buffers
     * @param beginIndex
     * @param endIndex
     * @param parseContext
     */
    JSONNode(Map<Serializable, JSONNode> fieldValues, char[] buffers, int beginIndex, int endIndex, JSONNodeContext parseContext) {
        this.fieldValues = fieldValues;
        this.completed = true;
        this.isArray = false;
        this.leaf = false;
        this.type = OBJECT;
        this.parseContext = parseContext;
        this.buffers = buffers;
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
    JSONNode(Serializable leafValue, char[] buffers, int beginIndex, int endIndex, JSONNodeContext parseContext) {
        this.leafValue = leafValue;
        this.completed = true;
        this.isArray = false;
        this.leaf = true;
        this.parseContext = parseContext;
        this.buffers = buffers;
        this.beginIndex = beginIndex;
        this.endIndex = endIndex;
        this.root = this;
        this.updateType();
    }

    /**
     * 构建子节点只有内部构建 （Build child nodes have only internal builds）
     *
     * @param buffers
     * @param beginIndex
     * @param endIndex
     * @param rootNode
     */
    private JSONNode(char[] buffers, int beginIndex, int endIndex, JSONNodeContext parseContext, JSONNode rootNode) {
        this.buffers = buffers;
        this.beginIndex = beginIndex;
        this.endIndex = endIndex;
        this.parseContext = parseContext;
        this.root = rootNode;
        this.init();
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
     * 解析完整文档生成全局根节点 （Parse source string to generate root node）
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
        Options.readOptions(readOptions, parseContext);
        parseContext.lazy = lazy;

        int toIndex = buf.length;
        if (path == null || (path = path.trim()).length() == 0) {
            // 等价于全内容解析
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
        Options.readOptions(readOptions, parseContext);
        parseContext.extract = true;
        if (!path.startsWith("/")) {
            path = "/" + path;
        }
        parseNode(buf, path, false, parseContext);
        return parseContext.getExtractValues();
    }


    public static boolean validate(String json, ReadOption... readOptions) {
        return validate(json, false, readOptions);
    }

    public static boolean validate(String json, boolean throwIfException, ReadOption... readOptions) {
        return validate(getChars(json), throwIfException, readOptions);
    }

    public static boolean validate(char[] buf, ReadOption... readOptions) {
        return validate(buf, false, readOptions);
    }

    public static boolean validate(char[] buf, boolean throwIfException, ReadOption... readOptions) {
        try {
            JSONNodeContext parseContext = new JSONNodeContext();
            Options.readOptions(readOptions, parseContext);
            // 校验模式
            parseContext.validate = true;
            parseNode(buf, null, true, parseContext);
        } catch (JSONException exception) {
            if (throwIfException) {
                throw exception;
            }
            return false;
        }
        return true;
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

            boolean allowComment = parseContext.isAllowComment();
            if (allowComment && beginChar == '/') {
                /** 去除声明在头部的注释*/
                fromIndex = clearCommentAndWhiteSpaces(buf, fromIndex + 1, toIndex, parseContext);
                beginChar = buf[fromIndex];
            }

            switch (beginChar) {
                case '{':
                    result = parseObjectPathNode(fromIndex, toIndex, buf, path, 0, skipValue, true, parseContext);
                    break;
                case '[':
                    result = parseArrayPathNode(fromIndex, toIndex, buf, path, 0, skipValue, true, parseContext);
                    break;
                case '"':
                    result = parseStringPathNode(fromIndex, toIndex, buf, skipValue, parseContext);
                    break;
                default:
                    throw new UnsupportedOperationException("Unsupported for begin character with '" + beginChar + "'");
            }

            // validate
            if (parseContext.validate) {
                int endIndex = parseContext.getEndIndex();
                if (endIndex != toIndex - 1) {
                    int wordNum = Math.min(50, buf.length - endIndex - 1);
                    throw new JSONException("Syntax error, extra characters found, '" + new String(buf, endIndex + 1, wordNum) + "', at pos " + endIndex);
                }
            }

            return result;

        } catch (Exception ex) {
            if (ex instanceof JSONException) {
                throw (JSONException) ex;
            }
            throw new JSONException("Error: " + ex.getMessage(), ex);
        }
    }


    /**
     * 解析对象，以{开始，直到遇到}结束 （Resolve the object, starting with '{', until '}' is encountered）
     *
     * @param fromIndex        开始位置（Start index of '{'）
     * @param toIndex          最大索引位置（Maximum end index）
     * @param buffers          缓冲数组（char[]）
     * @param jsonParseContext 上下文配置（context config）
     * @return 对象（object）
     * @throws Exception 异常(Exception)
     */
    private static JSONNode parseObjectPathNode(int fromIndex, int toIndex, char[] buffers, String path, int beginPathIndex, boolean skipValue, boolean returnIfMatched, JSONNodeContext jsonParseContext) throws Exception {

        int beginIndex = fromIndex + 1;
        char ch = '\0';
        String key = null;

        boolean empty = true;
        boolean allowComment = jsonParseContext.isAllowComment();

        Map<Serializable, JSONNode> fieldValues = null;
        boolean matched = false;
        boolean isLastPathLevel = false;
        // 下一个斜杆位置，如果为-1说明是最后一级节点
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

        // for loop to parse
        for (int i = beginIndex; i < toIndex; i++) {
            // clear white space characters
            while ((ch = buffers[i]) <= ' ') {
                i++;
            }
            if (jsonParseContext.isAllowComment()) {
                if (ch == '/') {
                    ch = buffers[i = clearCommentAndWhiteSpaces(buffers, i + 1, toIndex, jsonParseContext)];
                }
            }

            int fieldKeyFrom = i, fieldKeyTo, splitIndex, simpleToIndex = -1;
            boolean isUnquotedFieldName = false;

            // Standard JSON field name with "
            if (ch == '"') {
                while (i + 1 < toIndex && (buffers[++i] != '"' || buffers[i - 1] == '\\')) ;
                empty = false;
                i++;
            } else {
                if (ch == '}') {
                    if (!empty) {
                        throw new JSONException("Syntax error, the closing symbol '}' is not allowed at pos " + i);
                    }
                    jsonParseContext.setEndIndex(i);
                    // 空对象提前结束查找
                    return null;
                }
                if (ch == '\'') {
                    if (jsonParseContext.isAllowSingleQuotes()) {
                        while (i + 1 < toIndex && buffers[++i] != '\'') ;
                        empty = false;
                        i++;
                    } else {
                        throw new JSONException("Syntax error, the single quote symbol ' is not allowed at pos " + i);
                    }
                } else {
                    if (jsonParseContext.isAllowUnquotedFieldNames()) {
                        // 无引号key处理
                        // 直接锁定冒号（:）位置
                        while (i + 1 < toIndex && buffers[++i] != ':') ;
                        empty = false;
                        isUnquotedFieldName = true;
                    }
                }
            }

            // clear white space characters
            while ((ch = buffers[i]) <= ' ') {
                i++;
            }
            // 清除注释前记录属性字段的token结束位置
            fieldKeyTo = i;
            if (allowComment) {
                if (ch == '/') {
                    ch = buffers[i = clearCommentAndWhiteSpaces(buffers, i + 1, toIndex, jsonParseContext)];
                }
            }

            // Standard JSON rules:
            // 1 if matched, it can only be followed by a colon, and all other symbols are wrong
            // 2 after the attribute value is parsed, it can only be followed by a comma(,) or end, and other symbols are wrong
            if (ch == ':') {

                // Resolve key value pairs
                if (!skipValue) {
                    key = (String) parseFieldKey(fieldKeyFrom, fieldKeyTo, buffers);
                    if (!isLastPathLevel) {
                        matched = stringEqual(path, beginPathIndex + 1, (nextPathIndex == -1 ? path.length() : nextPathIndex) - beginPathIndex - 1, key, 0, key.length());
                    }
                }

                // 清除空白字符（clear white space characters）
                while ((ch = buffers[++i]) <= ' ') ;
                if (allowComment) {
                    if (ch == '/') {
                        ch = buffers[i = clearCommentAndWhiteSpaces(buffers, i + 1, toIndex, jsonParseContext)];
                    }
                }
                // 分割符位置,SimpleMode
                splitIndex = i - 1;

                // 空，布尔值，数字，或者字符串 （ null, true or false or numeric or string）
                boolean isSimpleValue = false;
                // 叶子节点值
                boolean isLeafValue = false;
                JSONNode value = null;
                boolean isSkipValue = isLastPathLevel ? false : !matched || skipValue;

                if (ch == '{') {
                    if (lazyParseLastNode) {
                        isSkipValue = true;
                    }
                    value = parseObjectPathNode(i, toIndex, buffers, path, nextPathIndex, isSkipValue, returnIfMatched, jsonParseContext);
                    if (lazyParseLastNode) {
                        // 构建懒加载节点
                        value = new JSONNode(buffers, i, jsonParseContext.getEndIndex() + 1, jsonParseContext);
                    }
                    i = jsonParseContext.getEndIndex();
                } else if (ch == '[') {
                    if (lazyParseLastNode) {
                        isSkipValue = true;
                    }
                    // 2 [ array
                    // 解析集合或者数组 （Parse a collection or array）
                    value = parseArrayPathNode(i, toIndex, buffers, path, nextPathIndex, isSkipValue, returnIfMatched, jsonParseContext);
                    if (lazyParseLastNode) {
                        // 构建懒加载节点
                        value = new JSONNode(buffers, i, jsonParseContext.getEndIndex() + 1, jsonParseContext);
                    }
                    i = jsonParseContext.getEndIndex();
                } else if (ch == '"') {
                    // 3 string
                    // parse string
                    isLeafValue = true;
                    value = parseStringPathNode(i, toIndex, buffers, isSkipValue, jsonParseContext);
                    i = jsonParseContext.getEndIndex();
                } else {
                    isSimpleValue = true;
                    isLeafValue = true;
                    // 4 null, true or false or numeric
                    // Find comma(,) or closing symbol(})
                    while (i + 1 < toIndex) {
                        ch = buffers[i + 1];
                        if (allowComment) {
                            // '/' in simple mode must be a comment, otherwise an exception will be thrown
                            if (ch == '/') {
                                simpleToIndex = i + 1;
                                int j = clearCommentAndWhiteSpaces(buffers, ++i + 1, toIndex, jsonParseContext);
                                ch = buffers[j];
                                // Make sure the I position precedes ',' or '}'
                                i = j - 1;
                            }
                        }
                        if (ch == ',' || ch == '}') {
                            break;
                        }
                        i++;
                    }
                    // Check whether post comments are appended
                }

                // clear white space characters
                while ((ch = buffers[++i]) <= ' ') ;
                if (simpleToIndex == -1) {
                    simpleToIndex = i;
                }

                if (allowComment) {
                    // clearComment and append whiteSpaces
                    if (ch == '/') {
                        ch = buffers[i = clearCommentAndWhiteSpaces(buffers, i + 1, toIndex, jsonParseContext)];
                    }
                }

                if (matched) {
                    // 如果是叶子节点值，并且非根节点，则路径一定错误
                    if (isLeafValue && nextPathIndex > -1) {
                        throw new JSONException(String.format("path '%s' error, '%s' is the last level, The following path '%s' does not exist ", path, path.substring(0, nextPathIndex), path.substring(nextPathIndex)));
                    }
                    if (!isSimpleValue) {
                        // extractValue: string(isLeafValue), object(nextPathIndex == -1), array(nextPathIndex == -1)
                        if ((isLastPathLevel || isLeafValue || nextPathIndex == -1) && jsonParseContext.extract) {
                            jsonParseContext.extractValue(isLeafValue ? value.leafValue : value);
                        }
                        jsonParseContext.setEndIndex(i);

                        // 立即返回标志
                        if (returnIfMatched) {
                            return value;
                        }
                    }
                }

                // Check whether the next character is a comma or end symbol '}'. If yes, continue or break. If not, throw an exception
                boolean isClosingSymbol = ch == '}';
                if (ch == ',' || isClosingSymbol) {

                    if (isSimpleValue) {
                        // 校验number, null, true or false
                        if (!skipValue || jsonParseContext.validate) {
                            value = parseSimpleNodeValue(splitIndex, simpleToIndex, buffers, jsonParseContext);
                            if (matched) {
                                // number, true/false, null
                                if (jsonParseContext.extract) {
                                    jsonParseContext.extractValue(value.leafValue);
                                }
                                jsonParseContext.setEndIndex(i);
                                return value;
                            }
                        }
                    }

                    if (fieldValues != null) {
                        fieldValues.put(key, value);
                    }

                    if (isClosingSymbol) {
                        jsonParseContext.setEndIndex(i);
                        if (isLastPathLevel) {
                            return new JSONNode(fieldValues, buffers, fromIndex, i + 1, jsonParseContext);
                        }
                        return null;
                    }
                } else {
                    throw new JSONException("Syntax error, unexpected token character '" + ch + "', position " + i);
                }
            } else {
                throw new JSONException("Syntax error, unexpected token character '" + ch + "', position " + i);
            }
        }
        throw new JSONException("Syntax error, the closing symbol '}' is not found ");
    }

    private static boolean stringEqual(String path, int s1, int len1, String key, int s2, int len2) {
        if (len1 != len2) return false;
        for (int i = 0; i < len1; i++) {
            if (path.charAt(s1 + i) != key.charAt(s2 + i)) return false;
        }
        return true;
    }

    private static JSONNode parseArrayPathNode(int fromIndex, int toIndex, char[] buffers, String path, int beginPathIndex, boolean skipValue, boolean returnIfMatched, JSONNodeContext jsonParseContext) throws Exception {

        int beginIndex = fromIndex + 1;
        char ch = '\0';

        List<JSONNode> elementValues = null;
        Collection<Object> collection = null;

        // 允许注释
        boolean allowComment = jsonParseContext.isAllowComment();

        int elementIndex = 0;

        boolean fetchAllElement = false;
        boolean matched = false;
        boolean isLastPathLevel = false;
        // 指定索引当匹配到立即返回value
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
                // 1 必须以[开始，以]结束
                // 2 * 匹配所有；
                // 3 n+ 索引号大于或者等于n;
                // 4 n- 索引号小于或者等于n;
                // 5 n  索引号等于n;
                // 6 其他不支持抛出异常；
                if (path.charAt(beginPathIndex + 1) != '[' || path.charAt(endPathIndex - 1) != ']') {
                    throw new UnsupportedOperationException("Path error, array element access must use [n]");
                }
                int numBeginIndex = beginPathIndex + 2;
                int numEndIndex = endPathIndex - 1;

                int len = numEndIndex - numBeginIndex;
                if (len == 0) {
                    throw new UnsupportedOperationException("Path error, array element access must use [n]");
                }
                char endCharOfPath = path.charAt(endPathIndex - 2);

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
                        targetElementIndex = Integer.parseInt(path.substring(numBeginIndex, numEndIndex));
                }

                isLastPathLevel = nextPathIndex == -1 || beginPathIndex == path.length() - 1;
            }
        }

        // 是否根据compareMode构建?
        if (isLastPathLevel || !returnValueIfMathched) {
            elementValues = new ArrayList<JSONNode>();
        }

        // 集合数组核心token是逗号（The core token of the collection array is a comma）
        // for loop
        for (int i = beginIndex; i < toIndex; i++) {

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
                        // AllMode
                        matched = true;
                }
            }

            // clear white space characters
            while ((ch = buffers[i]) <= ' ') {
                i++;
            }
            if (allowComment) {
                if (ch == '/') {
                    ch = buffers[i = clearCommentAndWhiteSpaces(buffers, i + 1, toIndex, jsonParseContext)];
                }
            }

            // for simple element parse
            int simpleFromIndex = i, simpleToIndex = -1;

            // 如果提前遇到字符']'，说明是空集合
            //  （If the character ']' is encountered in advance, it indicates that it is an empty set）
            // 另一种可能性(语法错误): 非空集合，发生在空逗号后接结束字符']'直接抛出异常
            //  (Another possibility(Syntax error): whether the empty comma followed by the closing character ']' throws an exception)
            if (ch == ']') {
                if (collection.size() > 0) {
                    throw new JSONException("Syntax error, not allowed ',' followed by ']', pos " + i);
                }
                jsonParseContext.setEndIndex(i);
                return null;
            }
            // 是否简单元素如null, true or false or numeric or string
            // (is simple elements such as null, true or false or numeric or string)
            boolean isSimpleElement = false;
            boolean isLeafValue = false;
            boolean isSkipValue = !matched || skipValue;
            JSONNode value = null;
            if (ch == '{') {
                value = parseObjectPathNode(i, toIndex, buffers, path, nextPathIndex, isSkipValue, returnValueIfMathched, jsonParseContext);
                if (matched && elementValues != null) {
                    elementValues.add(value);
                }
                i = jsonParseContext.getEndIndex();
            } else if (ch == '[') {
                // 2 [ array
                value = parseArrayPathNode(i, toIndex, buffers, path, nextPathIndex, isSkipValue, returnValueIfMathched, jsonParseContext);
                if (matched && elementValues != null) {
                    elementValues.add(value);
                }
                i = jsonParseContext.getEndIndex();
            } else if (ch == '"') {
                isLeafValue = true;
                // 3 string
                // When there are escape characters, the escape character needs to be parsed
                value = parseStringPathNode(i, toIndex, buffers, isSkipValue, jsonParseContext);
                if (matched && elementValues != null) {
                    elementValues.add(value);
                }
                i = jsonParseContext.getEndIndex();

            } else {
                // 简单元素（Simple element）
                isSimpleElement = true;
                isLeafValue = true;
                // 查找逗号或者结束字符']' (Find comma or closing character ']')
                // 注： 查找后i的位置在逗号或者']'之前(i pos before index of ',' or ']')
                while (i + 1 < toIndex) {
                    ch = buffers[i + 1];
                    if (allowComment) {
                        // '/' in simple mode must be a comment, otherwise an exception will be thrown
                        if (ch == '/') {
                            simpleToIndex = i + 1;
                            int j = clearCommentAndWhiteSpaces(buffers, ++i + 1, toIndex, jsonParseContext);
                            ch = buffers[j];
                            // Make sure the I position precedes', 'or'} '
                            i = j - 1;
                        }
                    }
                    if (ch == ',' || ch == ']') {
                        break;
                    }
                    i++;
                }
            }

            // 清除空白字符（clear white space characters）
            while (i + 1 < toIndex && (ch = buffers[++i]) <= ' ') ;
            if (simpleToIndex == -1) {
                simpleToIndex = i;
            }

            if (allowComment) {
                if (ch == '/') {
                    ch = buffers[i = clearCommentAndWhiteSpaces(buffers, i + 1, toIndex, jsonParseContext)];
                }
            }

            if (matched) {
                // 如果是叶子节点值，并且非根节点，则路径一定错误
                if (isLeafValue && nextPathIndex > -1) {
                    throw new JSONException(String.format("path '%s' error, '%s' is the last level, The following path '%s' does not exist ", path, path.substring(0, nextPathIndex), path.substring(nextPathIndex)));
                }
                if (!isSimpleElement) {
                    // extract
                    if (isLastPathLevel && jsonParseContext.extract) {
                        // continue find
                        jsonParseContext.extractValue(value);
                    }
                    // if continue ?
                    if (returnIfMatched && returnValueIfMathched) {
                        return value;
                    }

                    // 匹配前几个元素的的场景提前返回 Match the first few scenarios and return in advance
                    if (returnListIfMathched) {
                        if (isLastPathLevel) {
                            return new JSONNode(elementValues, buffers, fromIndex, i + 1, jsonParseContext);
                        } else {
                            return null;
                        }
                    }
                }
            }

            // 检查下一个字符是逗号还是结束符号。如果是，继续或者终止。如果不是，则抛出异常
            // （Check whether the next character is a comma or end symbol. If yes, continue or return . If not, throw an exception）
            boolean isEnd = ch == ']';
            if (ch == ',' || isEnd) {
                if (isSimpleElement) {
                    Serializable simpleValue = null;
                    if (!skipValue || jsonParseContext.validate) {
                        simpleValue = (Serializable) parseSimpleValue(simpleFromIndex, simpleToIndex, buffers, jsonParseContext);
                        if (matched) {
                            if (jsonParseContext.extract) {
                                jsonParseContext.extractValue(simpleValue);
                            }
                            jsonParseContext.setEndIndex(i);
                            return new JSONNode(simpleValue, buffers, simpleFromIndex, simpleToIndex, jsonParseContext);
                        }
                        if (matched && elementValues != null) {
                            elementValues.add(new JSONNode(simpleValue, buffers, simpleFromIndex, simpleToIndex, jsonParseContext));
                        }
                    }
                }

                if (isEnd) {
                    jsonParseContext.setEndIndex(i);
                    if (isLastPathLevel) {
                        return new JSONNode(elementValues, buffers, fromIndex, i + 1, jsonParseContext);
                    }
                    return null;
                }
            } else {
                throw new JSONException("Syntax error, unexpected token character '" + ch + "', position " + i + ", Missing ',' or '}'");
            }
        }
        throw new JSONException("Syntax error, the closing symbol ']' is not found ");
    }

    private static JSONNode parseStringPathNode(int fromIndex, int toIndex, char[] buffers, boolean skipValue, JSONNodeContext jsonParseContext) {

        int beginIndex = fromIndex + 1;
        char ch = '\0', next = '\0';
        int i = beginIndex;
        int len;

        if (skipValue) {
            char prev = '\0';
            while (i < toIndex && (ch = buffers[i]) != '"' || prev == '\\') {
                i++;
                prev = ch;
            }
            jsonParseContext.setEndIndex(i);
            return null;
        }

        StringBuilder stringBuilder = new StringBuilder();
        for (; i < toIndex; i++) {
            while (i < toIndex && (ch = buffers[i]) != '\\' && ch != '"') {
                i++;
            }
            // ch is \\ or "
            if (ch == '\\') {
                if (i < toIndex - 1) {
                    next = buffers[i + 1];
                }
                switch (next) {
                    case '"':
                        len = i - beginIndex;
                        stringBuilder.append(buffers, beginIndex, len + 1);
                        stringBuilder.setCharAt(stringBuilder.length() - 1, '"');
                        beginIndex = ++i + 1;
                        break;
                    case 'n':
                        len = i - beginIndex;
                        stringBuilder.append(buffers, beginIndex, len + 1);
                        stringBuilder.setCharAt(stringBuilder.length() - 1, '\n');
                        beginIndex = ++i + 1;
                        break;
                    case 'r':
                        len = i - beginIndex;
                        stringBuilder.append(buffers, beginIndex, len + 1);
                        stringBuilder.setCharAt(stringBuilder.length() - 1, '\r');
                        beginIndex = ++i + 1;
                        break;
                    case 't':
                        len = i - beginIndex;
                        stringBuilder.append(buffers, beginIndex, len + 1);
                        stringBuilder.setCharAt(stringBuilder.length() - 1, '\t');
                        beginIndex = ++i + 1;
                        break;
                    case 'b':
                        len = i - beginIndex;
                        stringBuilder.append(buffers, beginIndex, len + 1);
                        stringBuilder.setCharAt(stringBuilder.length() - 1, '\b');
                        beginIndex = ++i + 1;
                        break;
                    case 'f':
                        len = i - beginIndex;
                        stringBuilder.append(buffers, beginIndex, len + 1);
                        stringBuilder.setCharAt(stringBuilder.length() - 1, '\f');
                        beginIndex = ++i + 1;
                        break;
                    case 'u':
                        len = i - beginIndex;
                        stringBuilder.append(buffers, beginIndex, len + 1);
                        int c = parseInt(buffers, i + 2, i + 6, 16);
                        stringBuilder.setCharAt(stringBuilder.length() - 1, (char) c);
                        i += 4;
                        beginIndex = ++i + 1;
                        break;
                    case '\\':
                        len = i - beginIndex;
                        stringBuilder.append(buffers, beginIndex, len + 1);
                        stringBuilder.setCharAt(stringBuilder.length() - 1, '\\');
                        beginIndex = ++i + 1;
                        break;
                }
            } else {
                jsonParseContext.setEndIndex(i);
                len = i - beginIndex;
                if (stringBuilder.length() > 0) {
                    stringBuilder.append(buffers, beginIndex, len);
                    return new JSONNode(stringBuilder.toString(), buffers, fromIndex, i + 1, jsonParseContext);
                } else {
                    return new JSONNode(len == 0 ? "" : new String(buffers, beginIndex, len), buffers, fromIndex, i + 1, jsonParseContext);
                }
            }
        }
        throw new JSONException("Syntax error, the closing symbol '\"' is not found ");
    }

    /**
     * @param fromIndex        一般为冒号位置
     * @param splitIndex       逗号或者}位置
     * @param buffers
     * @param jsonParseContext
     * @throws Exception
     */
    private static JSONNode parseSimpleNodeValue(int fromIndex, int splitIndex, char[] buffers, JSONNodeContext jsonParseContext) {
        Object propertyValue = parseSimpleValue(fromIndex + 1, splitIndex, buffers, jsonParseContext);
        return new JSONNode((Serializable) propertyValue, buffers, fromIndex + 1, splitIndex, jsonParseContext);
    }

    private static Object parseSimpleValue(int fromIndex, int toIndex, char[] buffers, JSONNodeContext jsonParseContext) {
        char beginChar = '\0';
        char endChar = '\0';

        while ((fromIndex < toIndex) && ((beginChar = buffers[fromIndex]) <= ' ')) {
            fromIndex++;
        }
        while ((toIndex > fromIndex) && ((endChar = buffers[toIndex - 1]) <= ' ')) {
            toIndex--;
        }

        int len = toIndex - fromIndex;
        if (beginChar == '"' && endChar == '"') {
            return new String(buffers, fromIndex + 1, len - 2);
        } else if (len == 4
                && beginChar == 't'
                && buffers[fromIndex + 1] == 'r'
                && buffers[fromIndex + 2] == 'u'
                && endChar == 'e') {
            return true;
        } else if (len == 5
                && beginChar == 'f'
                && buffers[fromIndex + 1] == 'a'
                && buffers[fromIndex + 2] == 'l'
                && buffers[fromIndex + 3] == 's'
                && endChar == 'e') {
            return false;
        } else {
            if (len == 4
                    && beginChar == 'n'
                    && buffers[fromIndex + 1] == 'u'
                    && buffers[fromIndex + 2] == 'l'
                    && endChar == 'l') {
                return null;
            }
            // 除了字符串，true, false, null以外都以number处理
            return parseNumber(buffers, fromIndex, toIndex, jsonParseContext.isUseBigDecimalAsDefault());
        }
    }

    /**
     * 双引号中间内容，去除前后空格 eg : , "name" : "tom" , == > "name" :
     *
     * @param from
     * @param to
     * @param buffers
     * @return
     */
    private static Serializable parseFieldKey(int from, int to, char[] buffers) {
        char start = '"';
        while ((from < to) && ((start = buffers[from]) <= ' ')) {
            from++;
        }
        char end = '"';
        while ((to > from) && ((end = buffers[to - 1]) <= ' ')) {
            to--;
        }
        if (start == '"' && end == '"' || (start == '\'' && end == '\'')) {
            int len = to - from - 2;
            return new String(buffers, from + 1, len);
        }
        throw new JSONException("Syntax error,  pos " + from + ", '" + new String(buffers, from, to - from) + "' is Invalid key ");
    }

    //    /**
//     * 双引号中间内容，去除前后空格 eg : , "name" : "tom" , == > "name" :
//     *
//     * @param from
//     * @param to
//     * @param buffers
//     * @return
//     */
//    private static Serializable parseFieldKey(int from, int to, char[] buffers) {
//
//        char start = '"';
//        while ((from < to) && ((start = buffers[from]) <= ' ')) {
//            from++;
//        }
//        char end = '"';
//        while ((to > from) && ((end = buffers[to - 1]) <= ' ')) {
//            to--;
//        }
//        if (start == '"' && end == '"') {
//            // 去除前后引号2个字符
//            int len = to - from - 2;
//            return new String(buffers, from + 1, len);
//        } else if (start != '"' && end != '"') {
//            return Integer.parseInt(new String(buffers, from, to - from));
//        }
//        throw new JSONException(" offset " + from + ",len " + to);
//    }


    private static JSONNode parse(String source, String path, boolean reverseParseNode, ReadOption... readOptions) {
        if (source == null)
            return null;
        source = source.trim();
        try {
            char[] buffers = getChars(source);
            JSONNodeContext parseContext = prepareNodeContext(buffers);
            Options.readOptions(readOptions, parseContext);

            parseContext.reverseParseNode = reverseParseNode;
            JSONNode jsonNode = new JSONNode(buffers, 0, buffers.length, parseContext);
            if (path == null) {
                return jsonNode;
            }
            return jsonNode.get(path);
        } catch (Throwable throwable) {
            if (throwable instanceof JSONException) {
                throw (JSONException) throwable;
            }
            throw new JSONException("parse json error ！", throwable);
        }
    }

    private static JSONNodeContext prepareNodeContext(char[] buffers) {
        JSONNodeContext parseContext = new JSONNodeContext();
        char prev = '\0';
        char firstChar = '\0';
        char ch = '\0';
        int length = buffers.length;

        if (length > 0) {
            firstChar = buffers[0];
        }
        boolean isQuotEven = true;
        boolean addEscapeFlag = false;
        for (int i = 0; i < length; i++) {
            ch = buffers[i];
            if (prev != '\\') {
                if (ch == '"') {
                    parseContext.addQuotToken(i);
                    isQuotEven = !isQuotEven;
                    addEscapeFlag = false;
                }
            } else {
                if (!isQuotEven) {
                    parseContext.escape = true;
                    if (!addEscapeFlag) {
                        addEscapeFlag = true;
                        parseContext.addEscapeIndex(i);
                    }
                    if (ch == '\\') {
                        // reset prev
                        prev = '\0';
                        continue;
                    }
                }
            }
            prev = ch;
        }
        if (firstChar == '{' && ch == '}') {
            // 对象
            parseContext.type = 1;
        } else if (firstChar == '[' && ch == ']') {
            // 数组
            parseContext.type = 2;
        } else if (firstChar == '"' && ch == '"') {
            // 字符串
            parseContext.type = 3;
        } else {
            // 其他
            throw new UnsupportedOperationException("Unsupported for begin character with '" + firstChar + "' and end character width '" + ch + "'");
        }

        parseContext.prepared = true;
        return parseContext;
    }

    private void init() {
        char start = '\0';
        char end = '\0';
        while ((beginIndex < endIndex) && ((start = buffers[beginIndex]) <= ' ')) {
            beginIndex++;
        }
        while ((endIndex > beginIndex) && ((end = buffers[endIndex - 1]) <= ' ')) {
            endIndex--;
        }
        if (start == '{' && end == '}') {
            type = OBJECT;
        } else if (start == '[' && end == ']') {
            type = ARRAY;
        } else if (start == '"' && end == '"') {
            type = STRING;
        } else {
            text = new String(buffers, beginIndex, endIndex - beginIndex);
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
        if (childPath == null || (childPath = childPath.trim()).length() == 0)
            return this;
        if (childPath.startsWith("/")) {
            return root.get(childPath.substring(1));
        }
        if (leaf) {
            return null;
        }
        int splitIndex = childPath.indexOf('/');
        if (splitIndex == -1) {
            return getFieldNode(childPath);
        } else {
            String path = childPath.substring(0, splitIndex).trim();
            JSONNode childNode = getFieldNode(path);
            if (childNode != null) {
                String nextPath = childPath.substring(splitIndex + 1).trim();
                return childNode.get(nextPath);
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

    private JSONNode getFieldNode(String field) {
        JSONNode value = null;
        if (isArray) {
            if (field.startsWith("[") && field.endsWith("]")) {
                int index = Integer.parseInt(field.substring(1, field.length() - 1));
                return getElementAt(index);
            } else {
                throw new IllegalArgumentException("current JSONNode is an isArray type, path should like []");
            }
        } else {
            // map获取属性
            if (fieldValues != null) {
                value = fieldValues.get(field);
            }
        }
        if (value != null) {
            return value;
        }
        if (completed) {
            return null;
        }
        // 开始解析当前源，属性域 = {field}
        return parseValue(field, -1);
    }

    /**
     * 解析属性域中fieldName的JSONNode对象
     *
     * @param fieldName 对象的域名称
     * @param index     数组的下标
     * @return
     */
    @Deprecated
    private JSONNode parseValue(String fieldName, int index) {

        // 双引号标记
        int doubleQuotationMarks = 0;
        // 大括号配对标记
        int bigBracketCount = 0;
        // 中括号标记
        int midBracketCount = 0;
        // 冒号位置
        int colonIndex = -1;

        char prevCh = '\0';
        // 上一个字符是否为双引号标识
        boolean isPrevQuot;

        boolean prepared = parseContext.prepared;
        int[] quotTokenIndexs = parseContext.quotTokenIndexs;
        int next = -2;

        Serializable key = null;
        for (int j = offset + 1; j < endIndex - 1; j++) {
            char ch = buffers[j];
            isPrevQuot = prevCh == '"';
            if (ch == '"' && prevCh != '\\') {
                doubleQuotationMarks ^= 1;

                if (doubleQuotationMarks == 1) {
                    if (prepared) {
                        // 从预加载中的双引号直接读取
                        if (next == -2) {
                            // 索引查找
                            next = indexOf(quotTokenIndexs, j, quotTokenOffset == -1 ? 0 : quotTokenOffset);
                            quotTokenOffset = next;
                        } else {
                            next++;
                        }

                        j = quotTokenIndexs[next + 1];
                        prevCh = ch;
                        doubleQuotationMarks = 0;
                        next++;
                    } else {
                        // 直接读取buffers中从j开始的下一个双引号
                        // prepared如果为false则通过from模式读取，已对内容做过校验，可安全解析，不用考虑越界问题
                        while ((ch = buffers[++j]) != '"' || prevCh == '\\') {
                            prevCh = ch;
                        }
                        doubleQuotationMarks = 0;
                    }
                }

                // doubleQuotationMarks == 0  （双引号token已存在且配对）
                // 检查配对双引号紧跟的非空字符是否合法？ 支持的字符： 冒号，逗号，大括号（结束），中括号（结束）, 其他字符均为错误字符
                // 当前j索引位置为'"',从j+1开始查找
                char tmp = '\0';
                while (j + 1 < endIndex && (tmp = buffers[j + 1]) <= ' ') {
                    j++;
                    prevCh = tmp;
                }
                // 配对后单独判断下是否紧跟的双引号（其他符号校验在下面的switch中处理）
                if (tmp == '"') {
                    String msg = new String(buffers, beginIndex, j - beginIndex + 1) + " ... ";
                    throw new JSONException(" syntax error, unexpect token '" + ch + "' error, offset at " + j + ", context '" + msg + "'");
                }
                continue;
            }
            prevCh = ch;
            if (doubleQuotationMarks == 1)
                continue;

            switch (ch) {
                case '{':
                    bigBracketCount += 1;
                    continue;
                case '}':
                    bigBracketCount += -1;
                    continue;
                case '[':
                    midBracketCount += 1;
                    continue;
                case ']':
                    midBracketCount += -1;
                    continue;
                case ':':
                    if (bigBracketCount == 0 && midBracketCount == 0) {
                        if (colonIndex == -1) {
                            colonIndex = j;
                            key = parseFieldKey(offset + 1, j, buffers);
                            quotTokenOffset = next + 1;
                            offset = colonIndex;
                            continue;
                        } else {
                            String msg = new String(buffers, beginIndex, j - beginIndex + 1) + " ... ";
                            throw new JSONException(" syntax error, unexpect token ':' error, offset at " + j + ", context '" + msg + "'");
                        }
                    }
                    break;
                case ',':
                    if (bigBracketCount == 0 && midBracketCount == 0) {
                        JSONNode jsonNode = new JSONNode(buffers, offset + 1, j, parseContext, root);
                        jsonNode.quotTokenOffset = quotTokenOffset;
                        addJSONNode(key, jsonNode);
                        quotTokenOffset = next + 1;
                        offset = j;
                        if (isArray) {
                            if (length == index + 1) {
                                return jsonNode;
                            }
                        } else {
                            if (colonIndex == -1) {
                                String msg = new String(buffers, beginIndex, j - beginIndex + 1) + " ... ";
                                throw new JSONException(" syntax error, unexpect token '" + ch + "' error, offset at " + j + ", context '" + msg + "'");
                            }
                            colonIndex = -1;
                            if (fieldName != null && fieldName.equals(key)) {
                                return jsonNode;
                            }
                        }
                    }
                    break;
                default: {
                    if (isPrevQuot) {
                        String msg = new String(buffers, beginIndex, j - beginIndex + 1) + " ... ";
                        throw new JSONException(" syntax error, unexpect token '" + ch + "' error, offset at " + j + ", context '" + msg + "'");
                    }
                }
            }
        }

        completed = true;
        if (bigBracketCount == 0 && midBracketCount == 0) {
            JSONNode jsonNode = new JSONNode(buffers, offset + 1, endIndex - 1, parseContext, root);
            jsonNode.quotTokenOffset = quotTokenOffset;
            addJSONNode(key, jsonNode);
            if (isArray) {
                if (length == index + 1) {
                    return jsonNode;
                }
            } else {
                if (fieldName != null && fieldName.equals(key)) {
                    return jsonNode;
                }
            }
        } else {
            throw new JSONException(" error ");
        }
        return null;
    }

    private void addJSONNode(Serializable key, JSONNode jsonNode) {
        if (isArray) {
            if (key != null) {
                throw new JSONException("as an element of array, unexpect key '" + key + "':: source '" + jsonNode.source() + "'");
            }
            if (this.length == 0) {
                elementValues = new JSONNode[16];
            } else {
                if (this.length == elementValues.length) {
                    // 扩容
                    JSONNode[] tmp = elementValues;
                    elementValues = new JSONNode[this.length << 1];
                    System.arraycopy(tmp, 0, elementValues, 0, tmp.length);
                }
            }
            elementValues[length++] = jsonNode;
        } else {
            if (key == null) {
                throw new JSONException("as an field of object, key is not found:: source '" + jsonNode.source() + "'");
            }
            if (fieldValues == null) {
                fieldValues = new LinkedHashMap(16);
            }
            fieldValues.put(key, jsonNode);
        }
        jsonNode.parent = this;
    }

    /**
     * 根据索引获取
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
        return parseValue(null, index);
    }

    /**
     * 返回指定节点在json中的总子节点数量，如果解析完毕直接返回，否则进行解析
     *
     * <p>Returns the total number of child nodes of the specified node in JSON.
     * <p>If the parsing is completed, it will be returned directly. Otherwise, it will be parsed
     *
     * @return 元素数量
     */
    public int getElementCount() {
        if (isArray) {
            if (completed) {
                return length;
            }
            parseValue(null, -1);
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
     * 直接获取子节点
     * <p>注：请在确保已经完成解析的情况下调用，否则请调用get(field)
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
     * 获取指定路径的对象并类型转化
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
     * 获取下一级的node对象值并转化
     *
     * @param name
     * @param eClass
     * @param <E>
     * @return
     */
    public <E> E getChildValue(String name, Class<E> eClass) {
        JSONNode jsonNode = getFieldNode(name);
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
                text = parseEscapeValueStr(beginIndex, endIndex, buffers);
            } else {
                return source();
            }
        }
        return text;
    }

    private int getEscapeIndexAtRange(int fromIndex, int toIndex, int[] escapeCharIndexs, int escapeCharCount) {
        for (int i = 0; i < escapeCharCount; i++) {
            int value = escapeCharIndexs[i];
            if (value > fromIndex && value < toIndex) {
                return value;
            }
        }
        return -1;
    }

    /**
     * 将字符串转换为属性的value eg: “abc” -> abc
     * 注： 影响反序列性能的核心方法
     * 代码拷贝自JSON
     *
     * @param fromIndex
     * @param toIndex
     * @param buffers
     * @return
     */
    String parseEscapeValueStr(int fromIndex, int toIndex, char[] buffers) {

        // 需要转义符的索引位置
        int offset = 0;

        if (parseContext.prepared) {
            if (!parseContext.escape || (offset = getEscapeIndexAtRange(fromIndex, toIndex, parseContext.escapeCharIndexs, parseContext.escapeCharCount)) == -1) {
                return new String(buffers, fromIndex + 1, toIndex - fromIndex - 2);
            }
        } else {
            offset = fromIndex + 2;
        }

        int len = toIndex - fromIndex - 2;
        // 当前字符下一个
        char next = '\0';
        // 当前字符下下个
        char nextNext = '\0';

        int charsLen = 0;
        char[] chars = new char[len];

        int beginIndex = fromIndex + 1;
        int endIndex = toIndex - 1;

        System.arraycopy(buffers, beginIndex, chars, 0, offset - 1 - beginIndex);

        charsLen = offset - 1 - beginIndex;
        // reset beginIndex
        beginIndex = offset - 1;
        // 2 =========
        for (int i = beginIndex; i < endIndex; i++) {
            // 首字符
            char ch = next;
            if (ch == '\0') {
                ch = buffers[i];
            }
            // 下一个字符
            if ((next = nextNext) == '\0') {
                if (i < endIndex - 1) {
                    next = buffers[i + 1];
                }
            }
            // 下下字符
            if (i < endIndex - 2) {
                nextNext = buffers[i + 2];
            } else {
                nextNext = '\0';
            }

            if (i < endIndex - 1) {
                if (ch != '\\') {
                    chars[charsLen++] = ch;
                    continue;
                }
                switch (next) {
                    case '"':
                        chars[charsLen++] = '"';
                        i++;
                        next = nextNext;
                        nextNext = '\0';
                        break;
                    case 'n':
                        chars[charsLen++] = '\n';
                        i++;
                        next = nextNext;
                        nextNext = '\0';
                        break;
                    case 'r':
                        chars[charsLen++] = '\r';
                        i++;
                        next = nextNext;
                        nextNext = '\0';
                        break;
                    case 't':
                        chars[charsLen++] = '\t';
                        i++;
                        next = nextNext;
                        nextNext = '\0';
                        break;
                    case 'b':
                        chars[charsLen++] = '\b';
                        i++;
                        next = nextNext;
                        nextNext = '\0';
                        break;
                    case 'f':
                        chars[charsLen++] = '\f';
                        i++;
                        next = nextNext;
                        nextNext = '\0';
                        break;
                    case 'u':
                        // parse \\u\d{4}
                        String hex = new String(buffers, i + 2, 4);
                        // !(c < 32 || (c > 126 && c < 160))
                        int c = Integer.parseInt(hex, 16);
                        chars[charsLen++] = (char) c;
                        i += 5;
                        next = '\0';
                        nextNext = '\0';
                        break;
                    case '\\':
                        chars[charsLen++] = '\\';
                        i++;
                        next = nextNext;
                        nextNext = '\0';
                        break;
                    default:
                        chars[charsLen++] = ch;
                }
            } else {
                chars[charsLen++] = ch;
            }
        }

        return new String(chars, 0, charsLen);
    }

    /**
     * 转化为日期
     *
     * @param eClass  指定日期类
     * @param pattern 指定日期格式
     * @return
     */
    public Date toDate(Class<? extends Date> eClass, String pattern) {
        return (Date) parseDateValue(beginIndex, endIndex, buffers, pattern, null, eClass == null ? Date.class : eClass);
    }

    /**
     * 转化为日期
     *
     * @param eClass
     * @return
     */
    public Date toDate(Class<? extends Date> eClass) {
        return (Date) parseDateValue(beginIndex, endIndex, buffers, null, null, eClass == null ? Date.class : eClass);
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
            // 字符串转化
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
                if (parseContext.isByteArrayFromHexString()) {
                    return (E) hexString2Bytes(buffers, beginIndex + 1, endIndex - beginIndex - 2);
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
        ClassStructureWrapper classStructureWrapper = null;
        if (eClass == null || eClass == Map.class || eClass == LinkedHashMap.class) {
            instance = new LinkedHashMap();
            isMapInstance = true;
        } else {
            classStructureWrapper = ClassStructureWrapper.get(eClass);
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

        // 转换
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
                SetterInfo setterInfo = classStructureWrapper.getSetterInfo(fieldName);
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
                        int paramClassType = setterInfo.getParamClassType();
                        if (paramClassType == ReflectConsts.CLASS_TYPE_DATE) {
                            value = parseDateValue(beginIndex, endIndex, buffers, setterInfo.getPattern(), setterInfo.getTimezone(), (Class<? extends Date>) parameterType);
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
        for (int i = 0; i < length; i++) {
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
        parseValue(null, -1);
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
                source = new String(buffers, beginIndex, endIndex - beginIndex);
                text = source;
            }
            return text;
        }

        return new String(buffers, beginIndex, endIndex - beginIndex);
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
                for (int j = 0; j < length; j++) {
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
        stringBuilder.append(buffers, beginIndex, endIndex - beginIndex);
    }

    /**
     * 转义处理
     */
    private static void writeStringTo(String leafValue, StringBuilder content) {
        int len = leafValue.length();
        int beginIndex = 0;
        for (int i = 0; i < len; i++) {
            char ch = leafValue.charAt(i);
            if (ch == '\\') {
                int length = i - beginIndex;
                if (length > 0) {
                    content.append(leafValue, beginIndex, i);
                }
                content.append('\\').append('\\');
                beginIndex = i + 1;
                continue;
            }
            if (ch > 34) continue;
            if (needEscapes[ch]) {
                int length = i - beginIndex;
                if (length > 0) {
                    content.append(leafValue, beginIndex, i);
                }
                content.append(escapes[ch]);
                beginIndex = i + 1;
            }
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
        for (int i = 0; i < length; i++) {
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

//  防止debug时直接调用序列化
//    public String toString() {
//        return toJsonString();
//    }

}
