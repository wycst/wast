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

import io.github.wycst.wast.common.reflect.ClassStrucWrap;
import io.github.wycst.wast.common.reflect.GenericParameterizedType;
import io.github.wycst.wast.common.reflect.SetterInfo;
import io.github.wycst.wast.common.utils.*;
import io.github.wycst.wast.json.exceptions.JSONException;
import io.github.wycst.wast.json.options.ReadOption;

import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.io.Writer;
import java.lang.reflect.Array;
import java.util.*;

/**
 * <ol>
 * <li> 根据输入的字符串或者字符数组生成一个JSON树
 * <li> 只关注的节点数据可以根据需要进行解析，而无需解析完整的JSON字符串.
 * <li> 两种构建方式的区别(parse和from都是基于按需解析):
 * <br> parse: 扫描完整的json内容，但只对路径覆盖的节点进行解析，其他只做扫描和位置计算;只需要parse一次，并支持全局查找搜索；
 * <br> from: 扫描到路径指定节点的结束字符后直接返回，并对提取的内容进行解析生成根节点，效率比parse高(超大文本解析下优势明显)，但只支持局部查找;支持懒加载；
 * <li> 节点路径统一以'/'为分隔符，数组元素通过下标访问，例如： '/students/0/name'.
 * <li> 支持json内容中存在注释(非json标准规范).
 * <li> 支持数据提取.
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
 */
public abstract class JSONNode implements Comparable<JSONNode> {

    final JSONNode root;
    final JSONNodeContext parseContext;
    JSONNode parent;
    final int beginIndex;
    int endIndex;
    String text;
    int offset;
    boolean completed;
    private Serializable path;

    Map<Serializable, JSONNode> fieldValues;
    JSONNode[] elementValues;
    int elementSize;

    // Type: 1 object; 2 array; 3. String; 4 number; 5 boolean; 6 null
    int type;
    final static int OBJECT = 1;
    final static int ARRAY = 2;
    final static int STRING = 3;
    final static int NUMBER = 4;
    final static int BOOLEAN = 5;
    final static int NULL = 6;
    final static int EXPAND = 7;
    final static String[] TYPE_DESCS = new String[]{"", "object", "array", "string", "number", "boolean", "null", "expand"};
    boolean array;
    boolean leaf;
    Serializable value;
    Object any;
    boolean changed;
    JSONNodePathCtx collectCtx;

    final static class RootContext {
        final int beginIndex;
        final int endIndex;
        final int type;
        Serializable leafValue;

        public RootContext(int beginIndex, int endIndex, int type, Serializable leafValue) {
            this.beginIndex = beginIndex;
            this.endIndex = endIndex;
            this.type = type;
            this.leafValue = leafValue;
        }
    }

    final static void addFieldValue(Map<Serializable, JSONNode> fieldValues, Serializable key, JSONNode node) {
        node.path = key;
        fieldValues.put(key, node);
    }

    final String getParentAbsolutePath() {
        if (parent == root) {
            return "";
        } else {
            return parent == null ? "" : parent.getAbsolutePath();
        }
    }

    public final Serializable getPath() {
        return path;
    }

    public final String getAbsolutePath() {
        if (this == root) {
            return String.valueOf(path == null ? "" : path);
        }
        if (!changed || !parent.isArray()) {
            return getParentAbsolutePath() + '/' + (path == null ? "" : path);
        }
        return getParentAbsolutePath() + '/' + CollectionUtils.indexOf(parent.elementValues, this);
    }

    public final String toString() {
        int hv = hashCode();
        String absolutePath = getAbsolutePath();
        if (leaf) {
            if (type == STRING) {
                return "JNode@" + Integer.toHexString(hv) + "{'" + absolutePath + "', " + TYPE_DESCS[type] + ", '" + value + "'}";
            } else {
                return "JNode@" + Integer.toHexString(hv) + "{'" + absolutePath + "', " + TYPE_DESCS[type] + ", " + value + "}";
            }
        }
        return "JNode@" + Integer.toHexString(hv) + "{'" + absolutePath + "', " + TYPE_DESCS[type] + "}";
    }

    /**
     * <p>The instance is the root node
     *
     * @param rootContext
     * @param parseContext
     */
    JSONNode(RootContext rootContext, JSONNodeContext parseContext) {
        this.parseContext = parseContext;
        this.root = this;
        this.beginIndex = rootContext.beginIndex;
        this.offset = rootContext.beginIndex;
        this.endIndex = rootContext.endIndex;
        this.type = rootContext.type;
        this.value = rootContext.leafValue;
        this.leaf = type > 2;
        this.array = type == 2;
        this.path = "/";
    }

    /***
     * Construct object root nodes (local roots) based on parsed attribute data
     *
     * @param fieldValues
     * @param beginIndex
     * @param endIndex
     * @param parseContext
     */
    JSONNode(Map<Serializable, JSONNode> fieldValues, int beginIndex, int endIndex, JSONNodeContext parseContext, JSONNode root) {
        this.fieldValues = fieldValues;
        this.completed = true;
        this.array = false;
        this.leaf = false;
        this.type = OBJECT;
        this.parseContext = parseContext;
        this.beginIndex = beginIndex;
        this.endIndex = endIndex;
        this.root = root == null ? this : root;
        this.path = "/";
    }

    /***
     * leaf node
     *  @param value
     * @param parseContext
     * @param rootNode
     */
    JSONNode(Serializable value, int beginIndex, int endIndex, int type, JSONNodeContext parseContext, JSONNode rootNode) {
        this.value = value;
        this.completed = true;
        this.array = false;
        this.leaf = true;
        this.parseContext = parseContext;
        this.root = rootNode;
        this.beginIndex = beginIndex;
        this.endIndex = endIndex;
        this.type = type;
    }

    JSONNode(Serializable value, JSONNode rootNode) {
        this.value = value;
        this.completed = true;
        this.array = false;
        this.leaf = true;
        this.parseContext = null;
        this.root = rootNode;
        this.beginIndex = -1;
        this.endIndex = -1;
        this.type = EXPAND;
    }

    /**
     * 构建子节点只有内部构建 （Build child nodes have only internal builds）
     *
     * @param beginIndex
     * @param endIndex
     * @param type
     * @param parseContext
     * @param rootNode
     */
    JSONNode(int beginIndex, int endIndex, int type, JSONNodeContext parseContext, JSONNode rootNode) {
        this.beginIndex = beginIndex;
        this.offset = this.beginIndex;
        this.endIndex = endIndex;
        this.parseContext = parseContext;
        this.root = rootNode == null ? this : rootNode;
        this.type = type;
        this.leaf = type > 2;
        this.array = type == 2;
        this.path = "/";
    }

    // empty array node
    JSONNode(int beginIndex, JSONNodeContext parseContext, JSONNode root) {
        this.beginIndex = beginIndex;
        this.parseContext = parseContext;
        this.root = root;

        this.array = true;
        this.completed = true;
        this.leaf = false;
        this.type = ARRAY;
    }

    final static RootContext buildRootContext(char[] buf, int beginIndex, int endIndex) {
        char start = '\0';
        char end = '\0';
        while (/*(beginIndex < endIndex) &&*/ ((start = buf[beginIndex]) <= ' ')) {
            beginIndex++;
        }
        while (/*(endIndex > beginIndex) &&*/ ((end = buf[endIndex - 1]) <= ' ')) {
            endIndex--;
        }
        int type;
        Serializable leafValue = null;
        int len = endIndex - beginIndex;
        switch (start) {
            case '{': {
                if (end == '}') {
                    type = OBJECT;
                    break;
                }
                throw new JSONException("Syntax error, expected '}' but " + end + ", input '" + new String(buf, beginIndex, len) + "', position " + beginIndex);
            }
            case '[': {
                if (end == ']') {
                    type = ARRAY;
                    break;
                }
                throw new JSONException("Syntax error, expected ']' but " + end + ", input '" + new String(buf, beginIndex, len) + "', position " + beginIndex);
            }
            case '"':
            case '\'': {
                if (end == start) {
                    type = STRING;
                    break;
                }
                throw new JSONException("Syntax error, expected '" + start + "' but '" + end + "', input '" + new String(buf, beginIndex, len) + "', position " + beginIndex);
            }
            case 't': {
                if (len == 4) {
                    long unsafeValue = JSONUnsafe.getLong(buf, beginIndex);
                    if (unsafeValue == JSONGeneral.TRUE_LONG) {
                        type = BOOLEAN;
                        leafValue = true;
                        break;
                    }
                }
                throw new JSONException("Syntax error, expected 'true' but '" + new String(buf, beginIndex, len) + "', position " + beginIndex);
            }
            case 'f': {
                if (len == 5) {
                    long unsafeValue = JSONUnsafe.getLong(buf, beginIndex + 1);
                    if (unsafeValue == JSONGeneral.ALSE_LONG) {
                        type = BOOLEAN;
                        leafValue = false;
                        break;
                    }
                }
                throw new JSONException("Syntax error, expected 'false' but '" + new String(buf, beginIndex, len) + "', position " + beginIndex);
            }
            case 'n': {
                if (len == 4) {
                    long unsafeValue = JSONUnsafe.getLong(buf, beginIndex);
                    if (unsafeValue == JSONGeneral.NULL_LONG) {
                        type = NULL;
                        leafValue = null;
                        break;
                    }
                }
                throw new JSONException("Syntax error, expected 'null' but '" + new String(buf, beginIndex, len) + "', position " + beginIndex);
            }
            default: {
                type = NUMBER;
            }
        }
        return new RootContext(beginIndex, endIndex, type, leafValue);
    }

    final static RootContext buildRootContext(byte[] buf, int beginIndex, int endIndex) {
        byte start = '\0';
        byte end = '\0';
        while (/*(beginIndex < endIndex) && */((start = buf[beginIndex]) <= ' ')) {
            beginIndex++;
        }
        while (/*(endIndex > beginIndex) && */((end = buf[endIndex - 1]) <= ' ')) {
            endIndex--;
        }
        int type;
        Serializable leafValue = null;
        int len = endIndex - beginIndex;
        switch (start) {
            case '{': {
                if (end == '}') {
                    type = OBJECT;
                    break;
                }
                throw new JSONException("Syntax error, expected '}' but " + (char) end + ", input '" + new String(buf, beginIndex, len) + "', position " + beginIndex);
            }
            case '[': {
                if (end == ']') {
                    type = ARRAY;
                    break;
                }
                throw new JSONException("Syntax error, expected ']' but " + (char) end + ", input '" + new String(buf, beginIndex, len) + "', position " + beginIndex);
            }
            case '"':
            case '\'': {
                if (end == start) {
                    type = STRING;
                    break;
                }
                throw new JSONException("Syntax error, expected '" + (char) start + "' but '" + (char) end + "', input '" + new String(buf, beginIndex, len) + "', position " + beginIndex);
            }
            case 't': {
                if (len == 4) {
                    int unsafeValue = JSONUnsafe.getInt(buf, beginIndex);
                    if (unsafeValue == JSONGeneral.TRUE_INT) {
                        type = BOOLEAN;
                        leafValue = true;
                        break;
                    }
                }
                throw new JSONException("Syntax error, expected 'true' but '" + new String(buf, beginIndex, len) + "', position " + beginIndex);
            }
            case 'f': {
                if (len == 5) {
                    int unsafeValue = JSONUnsafe.getInt(buf, beginIndex + 1);
                    if (unsafeValue == JSONGeneral.ALSE_INT) {
                        type = BOOLEAN;
                        leafValue = false;
                        break;
                    }
                }
                throw new JSONException("Syntax error, expected 'false' but '" + new String(buf, beginIndex, len) + "', position " + beginIndex);
            }
            case 'n': {
                if (len == 4) {
                    int unsafeValue = JSONUnsafe.getInt(buf, beginIndex);
                    if (unsafeValue == JSONGeneral.NULL_INT) {
                        type = NULL;
                        leafValue = null;
                        break;
                    }
                }
                throw new JSONException("Syntax error, expected 'null' but '" + new String(buf, beginIndex, len) + "', position " + beginIndex);
            }
            default: {
                type = NUMBER;
            }
        }
        return new RootContext(beginIndex, endIndex, type, leafValue);
    }

    final static class D extends JSONNode {
        D(Serializable value, JSONNode root) {
            super(value, root);
        }
    }

    final static class C extends JSONNode {

        CharSource charSource;
        // source
        char[] buf;

        C(CharSource charSource, char[] buf, int beginIndex, int endIndex, JSONNodeContext parseContext) {
            super(buildRootContext(buf, beginIndex, endIndex), parseContext);
            this.charSource = charSource;
            this.buf = buf;
        }

        C(CharSource charSource, char[] buf, int beginIndex, int endIndex, int type, JSONNodeContext parseContext, JSONNode rootNode) {
            super(beginIndex, endIndex, type, parseContext, rootNode);
            this.charSource = charSource;
            this.buf = buf;
        }

        // empty array node
        C(CharSource charSource, char[] buf, int beginIndex, JSONNodeContext parseContext, JSONNode root) {
            super(beginIndex, parseContext, root);
            this.charSource = charSource;
            this.buf = buf;
        }

        C(CharSource charSource, Map<Serializable, JSONNode> fieldValues, char[] buf, int beginIndex, int endIndex, JSONNodeContext parseContext, JSONNode root) {
            super(fieldValues, beginIndex, endIndex, parseContext, root);
            this.charSource = charSource;
            this.buf = buf;
        }

        C(CharSource charSource, Serializable leafValue, char[] buf, int beginIndex, int endIndex, int type, JSONNodeContext parseContext, JSONNode rootNode) {
            super(leafValue, beginIndex, endIndex, type, parseContext, rootNode);
            this.charSource = charSource;
            this.buf = buf;
        }

        @Override
        public String getText() {
            if (text == null) {
                if (type == STRING) {
                    text = (String) JSONTypeDeserializer.CHAR_SEQUENCE_STRING.deserializeString(charSource, buf, beginIndex, '"', GenericParameterizedType.StringType, parseContext);
                    value = text;
                } else {
                    return source();
                }
            }
            return text;
        }

        public String source() {
            if (value != null && type != STRING) {
                return String.valueOf(this.value);
            }
            String source;
            if (type > STRING) {
                if (text == null) {
                    source = new String(buf, beginIndex, endIndex - beginIndex);
                    text = source;
                }
                return text;
            }
            return new String(buf, beginIndex, endIndex - beginIndex);
        }

        void writeSourceTo(StringBuilder stringBuilder) {
            stringBuilder.append(buf, beginIndex, endIndex - beginIndex);
        }

        @Override
        byte[] hexString2Bytes() {
            return JSONGeneral.hexString2Bytes(buf, beginIndex + 1, endIndex - beginIndex - 2);
        }

        @Override
        JSONNode parseObjectField(String fieldName, boolean lazy, boolean createObj) {
            if (completed) {
                return fieldValues.get(fieldName);
            }
            char ch;
            boolean allowComment = parseContext.allowComment;
            for (int i = offset + 1; i < endIndex; ++i) {
                while ((ch = buf[i]) <= ' ') {
                    ++i;
                }
                if (allowComment) {
                    if (ch == '/') {
                        ch = buf[i = JSONGeneral.clearCommentAndWhiteSpaces(buf, i + 1, parseContext)];
                    }
                }
                int fieldKeyFrom = i;
                String key;
                boolean matchedField;
                if (ch == '"') {
                    key = JSONDefaultParser.parseMapKeyByCache(buf, i, '"', parseContext);
                    i = parseContext.endIndex + 1;
                } else {
                    if (ch == '}') {
                        // parseContext.allowLastEndComma
                        offset = i;
                        completed = true;
                        return null;
                    }
                    if (ch == '\'') {
                        // parseContext.allowSingleQuotes
                        while (i + 1 < endIndex && (buf[++i] != '\'' || buf[i - 1] == '\\')) ;
                        ++i;
                        key = (String) JSONDefaultParser.parseKeyOfMap(buf, fieldKeyFrom, i, false);
                    } else {
                        // parseContext.allowUnquotedFieldNames
                        while (i + 1 < endIndex && buf[++i] != ':') ;
                        key = String.valueOf(JSONDefaultParser.parseKeyOfMap(buf, fieldKeyFrom, i, true));
                    }
                }
                while ((ch = buf[i]) <= ' ') {
                    ++i;
                }
                if (allowComment) {
                    if (ch == '/') {
                        ch = buf[i = JSONGeneral.clearCommentAndWhiteSpaces(buf, i + 1, parseContext)];
                    }
                }
                if (ch == ':') {
                    matchedField = fieldName != null && fieldName.equals(key);
                    while ((ch = buf[++i]) <= ' ') ;
                    if (allowComment) {
                        if (ch == '/') {
                            ch = buf[i = JSONGeneral.clearCommentAndWhiteSpaces(buf, i + 1, parseContext)];
                        }
                    }
                    JSONNode node;
                    try {
                        node = parseValueNode(i, '}', lazy, createObj);
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
                            ch = buf[i = JSONGeneral.clearCommentAndWhiteSpaces(buf, i + 1, parseContext)];
                        }
                    }
                    offset = i;
                    boolean isClosingSymbol = ch == '}';
                    if (ch == ',' || isClosingSymbol) {
                        addFieldValue(fieldValues, key, node);
                        if (isClosingSymbol) {
                            completed = true;
                        }
                        if (matchedField) {
                            return node;
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

        private JSONNode completeObjectNode(CharSource charSource, char[] buf, int fromIndex, boolean createObj) throws Exception {
            int beginIndex = fromIndex + 1, toIndex = endIndex;
            char ch;
            String key;
            final boolean allowComment = parseContext.allowComment;
            Map<Serializable, JSONNode> fieldValues = new LinkedHashMap<Serializable, JSONNode>();

            JSONNode objectNode = new C(charSource, fieldValues, buf, fromIndex, -1, parseContext, root);
            Map map = null;
            if (createObj) {
                map = new LinkedHashMap();
                objectNode.any = map;
            }
            for (int i = beginIndex; i < toIndex; ++i) {
                while ((ch = buf[i]) <= ' ') {
                    ++i;
                }
                if (allowComment) {
                    if (ch == '/') {
                        ch = buf[i = JSONGeneral.clearCommentAndWhiteSpaces(buf, i + 1, parseContext)];
                    }
                }
                if (ch == '"') {
                    key = JSONDefaultParser.parseMapKeyByCache(buf, i, '"', parseContext);
                    i = parseContext.endIndex + 1;
                } else {
                    if (ch == '}') {
                        // parseContext.allowLastEndComma
                        parseContext.endIndex = i++;
                        objectNode.endIndex = i;
                        return objectNode;
                    }
                    if (ch == '\'') {
                        // parseContext.allowSingleQuotes
                        key = JSONDefaultParser.parseMapKeyByCache(buf, i, '\'', parseContext);
                        i = parseContext.endIndex + 1;
                    } else {
                        // parseContext.allowUnquotedFieldNames
                        int begin = i;
                        while (i + 1 < toIndex && buf[++i] != ':') ;
                        key = String.valueOf(JSONDefaultParser.parseKeyOfMap(buf, begin, i, true));
                    }
                }
                while ((ch = buf[i]) <= ' ') {
                    ++i;
                }
                if (allowComment) {
                    if (ch == '/') {
                        ch = buf[i = JSONGeneral.clearCommentAndWhiteSpaces(buf, i + 1, parseContext)];
                    }
                }
                if (ch == ':') {
                    while ((ch = buf[++i]) <= ' ') ;
                    if (allowComment) {
                        if (ch == '/') {
                            ch = buf[i = JSONGeneral.clearCommentAndWhiteSpaces(buf, i + 1, parseContext)];
                        }
                    }
                    JSONNode node;
                    try {
                        node = parseValueNode(i, '}', false, createObj);
                        node.parent = objectNode;
                    } catch (Throwable throwable) {
                        if (throwable instanceof JSONException) {
                            throw (JSONException) throwable;
                        }
                        throw new JSONException(throwable.getMessage(), throwable);
                    }
                    i = parseContext.endIndex;
                    addFieldValue(fieldValues, key, node);
                    if (createObj) {
                        map.put(key, node.any());
                    }
                    while ((ch = buf[++i]) <= ' ') ;
                    if (allowComment) {
                        // clearComment and append whiteSpaces
                        if (ch == '/') {
                            ch = buf[i = JSONGeneral.clearCommentAndWhiteSpaces(buf, i + 1, parseContext)];
                        }
                    }
                    boolean isClosingSymbol = ch == '}';
                    if (ch == ',' || isClosingSymbol) {
                        if (isClosingSymbol) {
                            parseContext.endIndex = i++;
                            objectNode.endIndex = i;
                            return objectNode;
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

        private JSONNode completeArrayNode(CharSource charSource, char[] buf, int fromIndex, boolean createObj) throws Exception {
            int beginIndex = fromIndex + 1, toIndex = endIndex;
            char ch;
            JSONNode[] elementValues = new JSONNode[8];
            int elementSize = 0;
            boolean allowComment = parseContext.allowComment;
            JSONNode arrayNode = new C(charSource, buf, fromIndex, parseContext, root);
            List list = null;
            if (createObj) {
                list = new ArrayList();
                arrayNode.any = list;
            }
            for (int i = beginIndex; i < toIndex; ++i) {
                while ((ch = buf[i]) <= ' ') {
                    ++i;
                }
                if (allowComment) {
                    if (ch == '/') {
                        ch = buf[i = JSONGeneral.clearCommentAndWhiteSpaces(buf, i + 1, parseContext)];
                    }
                }
                if (ch == ']') {
                    // parseContext.allowLastEndComma
                    parseContext.endIndex = i++;
                    arrayNode.endIndex = i;
                    arrayNode.updateArray(elementValues, elementSize);
                    return arrayNode;
                }
                JSONNode node;
                try {
                    node = parseValueNode(i, ']', false, createObj);
                    node.parent = arrayNode;
                } catch (Throwable throwable) {
                    if (throwable instanceof JSONException) {
                        throw (JSONException) throwable;
                    }
                    throw new JSONException(throwable.getMessage(), throwable);
                }
                i = parseContext.endIndex;
                node.path = elementSize;
                if (elementSize >= elementValues.length) {
                    elementValues = Arrays.copyOf(elementValues, elementValues.length << 1);
                }
                elementValues[elementSize++] = node;
                if (createObj) {
                    list.add(node.any());
                }
                while (i + 1 < toIndex && (ch = buf[++i]) <= ' ') ;
                if (allowComment) {
                    if (ch == '/') {
                        ch = buf[i = JSONGeneral.clearCommentAndWhiteSpaces(buf, i + 1, parseContext)];
                    }
                }
                boolean isEnd = ch == ']';
                if (ch == ',' || isEnd) {
                    if (isEnd) {
                        parseContext.endIndex = i++;
                        arrayNode.endIndex = i;
                        arrayNode.updateArray(elementValues, elementSize);
                        return arrayNode;
                    }
                } else {
                    throw new JSONException("Syntax error, unexpected '" + ch + "', position " + i + ", missing ',' or '}'");
                }
            }
            throw new JSONException("Syntax error, the closing symbol ']' is not found ");
        }

        JSONNode parseValueNode(int beginIndex, char endChar, boolean lazy, boolean createObj) throws Exception {
            JSONNode node;
            char ch = buf[beginIndex];
            switch (ch) {
                case '{': {
                    if (lazy) {
                        JSONTypeDeserializer.MAP.skip(charSource, buf, beginIndex, parseContext);
                        node = new C(charSource, buf, beginIndex, parseContext.endIndex + 1, OBJECT, parseContext, root);
                    } else {
                        node = completeObjectNode(charSource, buf, beginIndex, createObj);
                    }
                    break;
                }
                case '[': {
                    if (lazy) {
                        JSONTypeDeserializer.COLLECTION.skip(charSource, buf, beginIndex, parseContext);
                        node = new C(charSource, buf, beginIndex, parseContext.endIndex + 1, ARRAY, parseContext, root);
                    } else {
                        node = completeArrayNode(charSource, buf, beginIndex, createObj);
                    }
                    break;
                }
                case '\'':
                case '"': {
                    // 3 string
                    node = parseStringPathNode(charSource, buf, beginIndex, endIndex, ch, false, parseContext, root);
                    break;
                }
                case 'n': {
                    // null
                    node = parseNullPathNode(charSource, buf, beginIndex, parseContext, root);
                    break;
                }
                case 't': {
                    node = parseBoolTruePathNode(charSource, buf, beginIndex, parseContext, root);
                    break;
                }
                case 'f': {
                    node = parseBoolFalsePathNode(charSource, buf, beginIndex, parseContext, root);
                    break;
                }
                default: {
                    // number
                    node = parseNumberPathNode(charSource, buf, beginIndex, endChar, false, parseContext, root);
                    break;
                }
            }
            node.parent = this;
            return node;
        }

        JSONNode parseArrayAt(int index, boolean lazy, boolean createObj) {
            char ch;
            boolean allowComment = parseContext.allowComment, matchedIndex;
            for (int i = offset + 1; i < endIndex; ++i) {
                while ((ch = buf[i]) <= ' ') {
                    ++i;
                }
                if (allowComment) {
                    if (ch == '/') {
                        ch = buf[i = JSONGeneral.clearCommentAndWhiteSpaces(buf, i + 1, parseContext)];
                    }
                }
                if (ch == ']') {
                    // parseContext.allowLastEndComma
                    offset = i;
                    completed = true;
                    return null;
                }
                matchedIndex = index > -1 && index == elementSize;
                JSONNode node;
                try {
                    node = parseValueNode(i, ']', lazy, createObj);
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
                        ch = buf[i = JSONGeneral.clearCommentAndWhiteSpaces(buf, i + 1, parseContext)];
                    }
                }
                offset = i;
                boolean isEnd = ch == ']';
                if (ch == ',' || isEnd) {
                    addElementNode(node);
                    if (isEnd) {
                        completed = true;
                    }
                    if (matchedIndex) {
                        return node;
                    }
                } else {
                    throw new JSONException("Syntax error, unexpected '" + ch + "', position " + i + ", missing ',' or '}'");
                }
            }
            return null;
        }

        @Override
        protected void clearSource() {
            this.buf = null;
            this.charSource = null;
        }
    }

    // update array
    private void updateArray(JSONNode[] elementValues, int elementSize) {
        this.elementValues = elementValues;
        this.elementSize = elementSize;
    }

    static class B extends JSONNode {

        CharSource charSource;
        // source
        byte[] buf;

        B(CharSource charSource, byte[] buf, int beginIndex, int endIndex, JSONNodeContext parseContext) {
            super(buildRootContext(buf, beginIndex, endIndex), parseContext);
            this.charSource = charSource;
            this.buf = buf;
        }

        B(CharSource charSource, Map<Serializable, JSONNode> fieldValues, byte[] buf, int beginIndex, int endIndex, JSONNodeContext parseContext, JSONNode root) {
            super(fieldValues, beginIndex, endIndex, parseContext, root);
            this.charSource = charSource;
            this.buf = buf;
        }

        B(CharSource charSource, Serializable leafValue, byte[] buf, int beginIndex, int endIndex, int type, JSONNodeContext parseContext, JSONNode rootNode) {
            super(leafValue, beginIndex, endIndex, type, parseContext, rootNode);
            this.charSource = charSource;
            this.buf = buf;
        }

        B(CharSource charSource, byte[] buf, int beginIndex, int endIndex, int type, JSONNodeContext parseContext, JSONNode rootNode) {
            super(beginIndex, endIndex, type, parseContext, rootNode);
            this.charSource = charSource;
            this.buf = buf;
        }

        B(CharSource charSource, byte[] buf, int fromIndex, JSONNodeContext parseContext, JSONNode root) {
            super(fromIndex, parseContext, root);
            this.charSource = charSource;
            this.buf = buf;
        }

        @Override
        public String getText() {
            if (text == null) {
                if (type == STRING) {
                    text = (String) JSONTypeDeserializer.CHAR_SEQUENCE_STRING.deserializeString(charSource, buf, beginIndex, '"', GenericParameterizedType.StringType, parseContext);
                    value = text;
                } else {
                    return source();
                }
            }
            return text;
        }

        public String source() {
            if (value != null && type != STRING) {
                return String.valueOf(this.value);
            }
            String source;
            if (type > STRING) {
                if (text == null) {
                    source = new String(buf, beginIndex, endIndex - beginIndex);
                    text = source;
                }
                return text;
            }
            return new String(buf, beginIndex, endIndex - beginIndex);
        }

        void writeSourceTo(StringBuilder stringBuilder) {
            stringBuilder.append(new String(buf, beginIndex, endIndex - beginIndex));
        }

        @Override
        byte[] hexString2Bytes() {
            return JSONGeneral.hexString2Bytes(buf, beginIndex + 1, endIndex - beginIndex - 2);
        }

        @Override
        JSONNode parseObjectField(String fieldName, boolean lazy, boolean createObj) {
            if (completed) {
                return fieldValues.get(fieldName);
            }
            byte b;
            final boolean allowComment = parseContext.allowComment, extract = parseContext.extract;
            final boolean stored = !extract || !lazy;
            final byte[] fieldNameBytes = (!stored && fieldName != null) ? JSONUnsafe.getStringUTF8Bytes(fieldName) : null;
            // boolean empty = true;
            for (int i = offset + 1; i < endIndex; ++i) {
                while ((b = buf[i]) <= ' ') {
                    ++i;
                }
                if (allowComment) {
                    if (b == '/') {
                        b = buf[i = JSONGeneral.clearCommentAndWhiteSpaces(buf, i + 1, parseContext)];
                    }
                }
                int fieldKeyFrom = i;
                String key = null;
                boolean matched, skipValue;
                if (b == '"') {
                    if (stored) {
                        key = JSONTypeDeserializer.parseMapKeyByCache(buf, i, '"', parseContext);
                        matched = fieldName != null && fieldName.equals(key);
                        skipValue = false;
                    } else {
                        // direct match bytes
                        matched = matchJSONKey(charSource, buf, i, fieldNameBytes, parseContext);
                        skipValue = !matched;
                    }
                    // empty = false;
                    i = parseContext.endIndex + 1;
                } else {
                    if (b == '}') {
                        // parseContext.allowLastEndComma
                        offset = i;
                        completed = true;
                        return null;
                    }
                    if (b == '\'') {
                        // parseContext.allowSingleQuotes
                        while (i + 1 < endIndex && (buf[++i] != '\'' || buf[i - 1] == '\\')) ;
                        ++i;
                        key = (String) JSONDefaultParser.parseKeyOfMap(buf, fieldKeyFrom, i, false);
                    } else {
                        // parseContext.allowUnquotedFieldNames
                        while (i + 1 < endIndex && buf[++i] != ':') ;
                        key = String.valueOf(JSONDefaultParser.parseKeyOfMap(buf, fieldKeyFrom, i, true));
                    }
                    matched = fieldName != null && fieldName.equals(key);
                    skipValue = !stored && !matched;
                }
                while ((b = buf[i]) <= ' ') {
                    ++i;
                }
                if (allowComment) {
                    if (b == '/') {
                        b = buf[i = JSONGeneral.clearCommentAndWhiteSpaces(buf, i + 1, parseContext)];
                    }
                }
                if (b == ':') {
                    while ((b = buf[++i]) <= ' ') ;
                    if (allowComment) {
                        if (b == '/') {
                            i = JSONGeneral.clearCommentAndWhiteSpaces(buf, i + 1, parseContext);
                        }
                    }
                    JSONNode node = null;
                    try {
                        if (skipValue) {
                            JSONTypeDeserializer.ANY.skip(charSource, buf, i, (byte) '}', parseContext);
                        } else {
                            node = parseValueNode(i, '}', lazy, createObj);
                        }
                    } catch (Throwable throwable) {
                        if (throwable instanceof JSONException) {
                            throw (JSONException) throwable;
                        }
                        throw new JSONException(throwable.getMessage(), throwable);
                    }
                    i = parseContext.endIndex;
                    while ((b = buf[++i]) <= ' ') ;
                    if (allowComment) {
                        if (b == '/') {
                            b = buf[i = JSONGeneral.clearCommentAndWhiteSpaces(buf, i + 1, parseContext)];
                        }
                    }
                    boolean isClosingSymbol = b == '}';
                    if (b == ',' || isClosingSymbol) {
                        if (stored) {
                            addFieldValue(fieldValues, key, node);
                            offset = i;
                            if (isClosingSymbol) {
                                completed = true;
                            }
                        }
                        if (matched) {
                            return node;
                        }
                    } else {
                        throw new JSONException("Syntax error, unexpected '" + (char) b + "', position " + i);
                    }
                } else {
                    throw new JSONException("Syntax error, unexpected '" + (char) b + "', position " + i);
                }
            }
            return null;
        }

        private JSONNode completeObjectNode(CharSource charSource, byte[] buf, int fromIndex, final boolean createObj) throws Exception {
            int beginIndex = fromIndex + 1, toIndex = endIndex;
            byte b;
            String key;
            boolean allowComment = parseContext.allowComment;
            Map<Serializable, JSONNode> fieldValues = new LinkedHashMap<Serializable, JSONNode>();

            JSONNode objectNode = new B(charSource, fieldValues, buf, fromIndex, -1, parseContext, root);
            Map map = null;
            if (createObj) {
                objectNode.any = map = new LinkedHashMap();
            }
            for (int i = beginIndex; i < endIndex; ++i) {
                while ((b = buf[i]) <= ' ') {
                    ++i;
                }
                if (allowComment) {
                    if (b == '/') {
                        b = buf[i = JSONGeneral.clearCommentAndWhiteSpaces(buf, i + 1, parseContext)];
                    }
                }
                if (b == '"') {
                    key = JSONTypeDeserializer.parseMapKeyByCache(buf, i, '"', parseContext);
                    i = parseContext.endIndex + 1;
                } else {
                    if (b == '}') {
                        // parseContext.allowLastEndComma
                        parseContext.endIndex = i++;
                        objectNode.endIndex = i;
                        return objectNode;
                    }
                    if (b == '\'') {
                        // parseContext.allowSingleQuotes
                        key = JSONTypeDeserializer.parseMapKeyByCache(buf, i, '\'', parseContext);
                        i = parseContext.endIndex + 1;
                    } else {
                        // parseContext.allowUnquotedFieldNames
                        int begin = i;
                        while (i + 1 < toIndex && buf[++i] != ':') ;
                        key = String.valueOf(JSONDefaultParser.parseKeyOfMap(buf, begin, i, true));
                    }
                }
                while ((b = buf[i]) <= ' ') {
                    ++i;
                }
                if (allowComment) {
                    if (b == '/') {
                        b = buf[i = JSONGeneral.clearCommentAndWhiteSpaces(buf, i + 1, parseContext)];
                    }
                }
                if (b == ':') {
                    while ((b = buf[++i]) <= ' ') ;
                    if (allowComment) {
                        if (b == '/') {
                            b = buf[i = JSONGeneral.clearCommentAndWhiteSpaces(buf, i + 1, parseContext)];
                        }
                    }
                    JSONNode node;
                    try {
                        node = parseValueNode(i, '}', false, createObj);
                        node.parent = objectNode;
                    } catch (Throwable throwable) {
                        if (throwable instanceof JSONException) {
                            throw (JSONException) throwable;
                        }
                        throw new JSONException(throwable.getMessage(), throwable);
                    }
                    i = parseContext.endIndex;
                    addFieldValue(fieldValues, key, node);
                    if (createObj) {
                        map.put(key, node.any());
                    }
                    while ((b = buf[++i]) <= ' ') ;
                    if (allowComment) {
                        // clearComment and append whiteSpaces
                        if (b == '/') {
                            b = buf[i = JSONGeneral.clearCommentAndWhiteSpaces(buf, i + 1, parseContext)];
                        }
                    }
                    boolean isClosingSymbol = b == '}';
                    if (b == ',' || isClosingSymbol) {
                        if (fieldValues != null) {
                            addFieldValue(fieldValues, key, node);
                        }
                        if (isClosingSymbol) {
                            parseContext.endIndex = i++;
                            objectNode.endIndex = i;
                            return objectNode;
                        }
                    } else {
                        throw new JSONException("Syntax error, unexpected '" + b + "', position " + i);
                    }
                } else {
                    throw new JSONException("Syntax error, unexpected '" + b + "', position " + i);
                }
            }
            throw new JSONException("Syntax error, the closing symbol '}' is not found ");
        }

        private JSONNode completeArrayNode(CharSource charSource, byte[] buf, int fromIndex, boolean createObj) throws Exception {
            int beginIndex = fromIndex + 1, toIndex = endIndex;
            byte b;
            JSONNode[] elementValues = new JSONNode[8];
            int elementSize = 0;
            boolean allowComment = parseContext.allowComment;
            JSONNode arrayNode = new B(charSource, buf, fromIndex, parseContext, root);
            List list = null;
            if (createObj) {
                arrayNode.any = list = new ArrayList();
            }
            for (int i = beginIndex; i < toIndex; ++i) {
                while ((b = buf[i]) <= ' ') {
                    ++i;
                }
                if (allowComment) {
                    if (b == '/') {
                        b = buf[i = JSONGeneral.clearCommentAndWhiteSpaces(buf, i + 1, parseContext)];
                    }
                }
                if (b == ']') {
                    // parseContext.allowLastEndComma
                    parseContext.endIndex = i++;
                    arrayNode.endIndex = i;
                    arrayNode.updateArray(elementValues, elementSize);
                    return arrayNode;
                }
                JSONNode node;
                try {
                    node = parseValueNode(i, ']', false, createObj);
                    node.parent = arrayNode;
                } catch (Throwable throwable) {
                    if (throwable instanceof JSONException) {
                        throw (JSONException) throwable;
                    }
                    throw new JSONException(throwable.getMessage(), throwable);
                }
                i = parseContext.endIndex;
                node.path = elementSize;
                if (elementSize >= elementValues.length) {
                    elementValues = Arrays.copyOf(elementValues, elementValues.length << 1);
                }
                elementValues[elementSize++] = node;
                if (createObj) {
                    list.add(node.any());
                }
                while (i + 1 < toIndex && (b = buf[++i]) <= ' ') ;
                if (allowComment) {
                    if (b == '/') {
                        b = buf[i = JSONGeneral.clearCommentAndWhiteSpaces(buf, i + 1, parseContext)];
                    }
                }
                boolean isEnd = b == ']';
                if (b == ',' || isEnd) {
                    if (isEnd) {
                        parseContext.endIndex = i++;
                        arrayNode.endIndex = i;
                        arrayNode.updateArray(elementValues, elementSize);
                        return arrayNode;
                    }
                } else {
                    throw new JSONException("Syntax error, unexpected '" + b + "', position " + i + ", missing ',' or '}'");
                }
            }
            throw new JSONException("Syntax error, the closing symbol ']' is not found ");
        }

        JSONNode parseValueNode(int beginIndex, char endChar, boolean lazy, boolean createObj) throws Exception {
            JSONNode node;
            byte ch = buf[beginIndex];
            switch (ch) {
                case '{': {
                    if (lazy) {
                        JSONTypeDeserializer.MAP.skip(charSource, buf, beginIndex, parseContext);
                        node = new B(charSource, buf, beginIndex, parseContext.endIndex + 1, OBJECT, parseContext, root);
                    } else {
                        node = completeObjectNode(charSource, buf, beginIndex, createObj);
                    }
                    break;
                }
                case '[': {
                    if (lazy) {
                        JSONTypeDeserializer.COLLECTION.skip(charSource, buf, beginIndex, parseContext);
                        node = new B(charSource, buf, beginIndex, parseContext.endIndex + 1, ARRAY, parseContext, root);
                    } else {
                        node = completeArrayNode(charSource, buf, beginIndex, createObj);
                    }
                    break;
                }
                case '\'':
                case '"': {
                    // 3 string
                    node = parseStringPathNode(charSource, buf, beginIndex, ch, false, parseContext, root);
                    break;
                }
                case 'n': {
                    // null
                    node = parseNullPathNode(charSource, buf, beginIndex, parseContext, root);
                    break;
                }
                case 't': {
                    node = parseBoolTruePathNode(charSource, buf, beginIndex, parseContext, root);
                    break;
                }
                case 'f': {
                    node = parseBoolFalsePathNode(charSource, buf, beginIndex, parseContext, root);
                    break;
                }
                default: {
                    // number
                    node = parseNumberPathNode(charSource, buf, beginIndex, (byte) endChar, false, parseContext, root);
                    break;
                }
            }
            node.parent = this;
            return node;
        }

        JSONNode parseArrayAt(int index, boolean lazy, boolean createObj) {
            byte ch;
            boolean allowComment = parseContext.allowComment, matchedIndex;
            for (int i = offset + 1; i < endIndex; ++i) {
                while ((ch = buf[i]) <= ' ') {
                    ++i;
                }
                if (allowComment) {
                    if (ch == '/') {
                        ch = buf[i = JSONGeneral.clearCommentAndWhiteSpaces(buf, i + 1, parseContext)];
                    }
                }
                if (ch == ']') {
                    // parseContext.allowLastEndComma
                    offset = i;
                    completed = true;
                    return null;
                }
                matchedIndex = index > -1 && index == elementSize;

                JSONNode node;
                try {
                    node = parseValueNode(i, ']', lazy, createObj);
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
                        ch = buf[i = JSONGeneral.clearCommentAndWhiteSpaces(buf, i + 1, parseContext)];
                    }
                }
                offset = i;
                boolean isEnd = ch == ']';
                if (ch == ',' || isEnd) {
                    addElementNode(node);
                    if (isEnd) {
                        completed = true;
                    }
                    if (matchedIndex) {
                        return node;
                    }
                } else {
                    throw new JSONException("Syntax error, unexpected '" + ch + "', position " + i + ", missing ',' or '}'");
                }
            }

            return null;
        }

        @Override
        protected void clearSource() {
            this.buf = null;
            this.charSource = null;
        }
    }

    /**
     * Return the complete JSON tree node
     *
     * @param source
     * @param readOptions
     * @return
     */
    public final static JSONNode parse(String source, ReadOption... readOptions) {
        return parseInternal(source, null, readOptions);
    }

    /**
     * Return the complete JSON tree node
     *
     * @param source      json source
     * @param path        <p> 1. Access the root and split path through '/' off;
     *                    <p> 2. use n to access the array subscript;
     *                    <p> 3. does not support recursion '//';
     * @param readOptions
     * @return
     */
    public final static JSONNode parse(String source, String path, ReadOption... readOptions) {
        return parseInternal(source, path, readOptions);
    }

    public final static JSONNode parse(byte[] bytes, ReadOption... readOptions) {
        return parse(bytes, null, readOptions);
    }

    public final static JSONNode parse(InputStream is, ReadOption... readOptions) {
        if (EnvUtils.JDK_9_PLUS) {
            try {
                return parse(IOUtils.readBytes(is), null, readOptions);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        } else {
            return parse(StringUtils.fromStream(is), null, readOptions);
        }
    }

    /**
     * Return the complete JSON tree node
     *
     * @param bytes       json source
     * @param path        <p> 1. Access the root and split path through '/' off;
     *                    <p> 2. use n to access the array subscript;
     *                    <p> 3. does not support recursion '//';
     * @param readOptions
     * @return
     */
    public final static JSONNode parse(byte[] bytes, String path, ReadOption... readOptions) {
        if (bytes == null)
            return null;
        try {
            JSONNodeContext parseContext = new JSONNodeContext();
            JSONOptions.readOptions(readOptions, parseContext);
            parseContext.toIndex = bytes.length;
            JSONNode root;
            if (EnvUtils.JDK_9_PLUS) {
                if (!EnvUtils.hasNegatives(bytes, 0, bytes.length)) {
                    root = new B(AsciiStringSource.of(JSONUnsafe.createAsciiString(bytes)), bytes, 0, bytes.length, parseContext);
                } else {
                    root = new B(UTF8CharSource.of(JSONUnsafe.createAsciiString(bytes)), bytes, 0, bytes.length, parseContext);
                }
            } else {
                root = new B(null, bytes, 0, bytes.length, parseContext);
            }
            if (path == null) {
                return root;
            }
            return root.get(path);
        } catch (Throwable throwable) {
            if (throwable instanceof JSONException) {
                throw (JSONException) throwable;
            }
            throw new JSONException(throwable.getMessage(), throwable);
        }
    }

    final static JSONNode parseInternal(String source, String path, ReadOption... readOptions) {
        if (source == null)
            return null;
        source = source.trim();
        try {
            JSONNodeContext parseContext = new JSONNodeContext();
            JSONOptions.readOptions(readOptions, parseContext);
            parseContext.toIndex = source.length();
            JSONNode root;
            if (EnvUtils.JDK_9_PLUS) {
                byte[] bytes = (byte[]) JSONUnsafe.getStringValue(source);
                if (bytes.length == source.length()) {
                    root = new B(AsciiStringSource.of(source), bytes, 0, bytes.length, parseContext);
                } else {
                    char[] chars = source.toCharArray();
                    root = new C(UTF16ByteArraySource.of(source), chars, 0, chars.length, parseContext);
                }
            } else {
                char[] chars = (char[]) JSONUnsafe.getStringValue(source);
                root = new C(null, chars, 0, chars.length, parseContext);
            }
            if (path == null) {
                return root;
            }
            return root.get(path);
        } catch (Throwable throwable) {
            if (throwable instanceof JSONException) {
                throw (JSONException) throwable;
            }
            throw new JSONException(throwable.getMessage(), throwable);
        }
    }

    /**
     * Generate JSON root node (local root) based on specified path
     *
     * @param json
     * @param path        the exact path
     * @param readOptions
     * @return
     */
    public final static JSONNode from(String json, String path, ReadOption... readOptions) {
        if (EnvUtils.JDK_9_PLUS) {
            byte[] bytes = (byte[]) JSONUnsafe.getStringValue(json.toString());
            if (bytes.length == json.length()) {
                return parseInternal(AsciiStringSource.of(json), bytes, JSONNodePath.parse(path), readOptions);
            } else {
                char[] chars = json.toCharArray();
                return parseInternal(UTF16ByteArraySource.of(json), chars, JSONNodePath.parse(path), readOptions);
            }
        } else {
            return parseInternal(null, (char[]) JSONUnsafe.getStringValue(json.toString()), JSONNodePath.parse(path), readOptions);
        }
    }

    /**
     * Generate JSON root node (local root) based on specified path
     *
     * @param bytes
     * @param path        the exact path
     * @param readOptions
     * @return
     */
    public final static JSONNode from(byte[] bytes, String path, ReadOption... readOptions) {
        if (EnvUtils.JDK_9_PLUS) {
            if (!EnvUtils.hasNegatives(bytes, 0, bytes.length)) {
                return parseInternal(AsciiStringSource.of(JSONUnsafe.createAsciiString(bytes)), bytes, JSONNodePath.parse(path), readOptions);
            } else {
                return parseInternal(UTF8CharSource.of(JSONUnsafe.createAsciiString(bytes)), bytes, JSONNodePath.parse(path), readOptions);
            }
        } else {
            return parseInternal(null, bytes, JSONNodePath.parse(path), readOptions);
        }
    }

    public final static <T> T from(String source, String path, Class<T> valueClass, ReadOption... readOptions) {
        return from(source, path, readOptions).getValue(valueClass);
    }

    /***
     * 根据指定path生成json根节点(局部根)
     *
     * @param charSource
     * @param buf
     * @param path
     * @param readOptions
     * @return
     */
    final static JSONNode parseInternal(CharSource charSource, char[] buf, JSONNodePath path, ReadOption... readOptions) {
        int toIndex = buf.length;
        JSONNodeContext parseContext = new JSONNodeContext();
        JSONOptions.readOptions(readOptions, parseContext);
        parseContext.toIndex = toIndex;
        if (path == null) {
            return new C(charSource, buf, 0, toIndex, parseContext);
        }
        return parseNode(charSource, buf, path, parseContext);
    }

    final static JSONNode parseInternal(CharSource charSource, byte[] buf, JSONNodePath path, ReadOption... readOptions) {
        int toIndex = buf.length;
        JSONNodeContext parseContext = new JSONNodeContext();
        JSONOptions.readOptions(readOptions, parseContext);
        parseContext.toIndex = toIndex;
        if (path == null) {
            return new B(charSource, buf, 0, toIndex, parseContext);
        }
        return parseNode(charSource, buf, path, parseContext);
    }

    /**
     * Extract path value list
     * <p>
     * Supports wildcard(*) search but does not support recursion and filter
     * <p>
     * 不支持递归和属性过滤器
     *
     * @param json
     * @param xpath       wildcard search path
     * @param readOptions
     * @return
     */
    public final static List<JSONNode> extract(String json, String xpath, ReadOption... readOptions) {
        return extract(json, JSONNodePath.parse(xpath), JSONNodeCollector.DEFAULT, readOptions);
    }

    /**
     * Extract path value list
     * <p>
     * Supports wildcard(*) search but does not support recursion and filter
     * <p>
     * 不支持递归和属性过滤器
     *
     * @param json
     * @param path
     * @param readOptions
     * @return
     */
    public final static List<JSONNode> extract(String json, JSONNodePath path, ReadOption... readOptions) {
        return extract(json, path, JSONNodeCollector.DEFAULT, readOptions);
    }

    /**
     * Extract path value list
     * <p>
     * Supports wildcard(*) search but does not support recursion and filter
     * <p>
     * 不支持递归和属性过滤器
     *
     * @param json
     * @param path
     * @param nodeCollector
     * @param readOptions
     * @return
     */
    public final static <T> List<T> extract(String json, JSONNodePath path, JSONNodeCollector<T> nodeCollector, ReadOption... readOptions) {
        if (EnvUtils.JDK_9_PLUS) {
            byte[] bytes = (byte[]) JSONUnsafe.getStringValue(json.toString());
            if (bytes.length == json.length()) {
                return extractInternal(AsciiStringSource.of(json), bytes, path, nodeCollector, readOptions);
            } else {
                // utf16
                char[] chars = json.toCharArray();
                return extractInternal(UTF16ByteArraySource.of(json), chars, path, nodeCollector, readOptions);
            }
        } else {
            return extractInternal(null, (char[]) JSONUnsafe.getStringValue(json.toString()), path, nodeCollector, readOptions);
        }
    }

    /**
     * Extract path value list
     * <p>
     * Supports wildcard(*) search but does not support recursion and filter
     * <p>
     * 不支持递归和属性过滤器
     *
     * @param bytes
     * @param path
     * @param nodeCollector
     * @param readOptions
     * @return
     */
    public final static <T> List<T> extract(byte[] bytes, JSONNodePath path, JSONNodeCollector<T> nodeCollector, ReadOption... readOptions) {
        if (EnvUtils.JDK_9_PLUS) {
            if (!EnvUtils.hasNegatives(bytes, 0, bytes.length)) {
                return extractInternal(AsciiStringSource.of(JSONUnsafe.createAsciiString(bytes)), bytes, path, nodeCollector, readOptions);
            } else {
                return extractInternal(UTF8CharSource.of(JSONUnsafe.createAsciiString(bytes)), bytes, path, nodeCollector, readOptions);
            }
        } else {
            return extractInternal(null, bytes, path, nodeCollector, readOptions);
        }
    }

    static List extractInternal(CharSource charSource, char[] buf, JSONNodePath path, JSONNodeCollector nodeCollector, ReadOption... readOptions) {
        JSONNodeContext parseContext = new JSONNodeContext();
        JSONOptions.readOptions(readOptions, parseContext);
        parseContext.toIndex = buf.length;
        parseContext.enableExtract(nodeCollector.self());
        parseNode(charSource, buf, path, parseContext);
        return parseContext.extractValues;
    }

    static List extractInternal(CharSource charSource, byte[] buf, JSONNodePath path, JSONNodeCollector nodeCollector, ReadOption... readOptions) {
        JSONNodeContext parseContext = new JSONNodeContext();
        JSONOptions.readOptions(readOptions, parseContext);
        parseContext.toIndex = buf.length;
        parseContext.enableExtract(nodeCollector.self());
        parseNode(charSource, buf, path, parseContext);
        return parseContext.extractValues;
    }

    private static JSONNode parseNode(CharSource charSource, char[] buf, JSONNodePath path, JSONNodeContext parseContext) {
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
                fromIndex = JSONGeneral.clearCommentAndWhiteSpaces(buf, fromIndex + 1, parseContext);
                beginChar = buf[fromIndex];
            }

            // ignore null / true / false / number of the root
            switch (beginChar) {
                case '{':
                    result = parseObjectPathNode(charSource, buf, fromIndex, toIndex, path, path.head, true, parseContext);
                    break;
                case '[':
                    result = parseArrayPathNode(charSource, buf, fromIndex, toIndex, path, path.head, true, parseContext);
                    break;
                case '\'':
                case '"':
                    result = parseStringPathNode(charSource, buf, fromIndex, toIndex, beginChar, false, parseContext, null);
                    break;
                default:
                    throw new UnsupportedOperationException("unsupported for begin character with '" + beginChar + "'");
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
            JSONGeneral.handleCatchException(ex, buf, toIndex);
            throw new JSONException("Error: " + ex.getMessage(), ex);
        }
    }

    private static JSONNode parseNode(CharSource charSource, byte[] buf, JSONNodePath path, JSONNodeContext parseContext) {
        int toIndex = buf.length;
        JSONNode result;
        try {
            int fromIndex = 0;
            // Trim remove white space characters
            byte beginByte = '\0';
            while ((fromIndex < toIndex) && (beginByte = buf[fromIndex]) <= ' ') {
                fromIndex++;
            }
            while ((toIndex > fromIndex) && buf[toIndex - 1] <= ' ') {
                toIndex--;
            }
            if (parseContext.allowComment && beginByte == '/') {
                /** 去除声明在头部的注释*/
                fromIndex = JSONGeneral.clearCommentAndWhiteSpaces(buf, fromIndex + 1, parseContext);
                beginByte = buf[fromIndex];
            }

            // ignore null / true / false / number of the root
            switch (beginByte) {
                case '{':
                    result = parseObjectPathNode(charSource, buf, fromIndex, toIndex, path, path.head, true, parseContext);
                    break;
                case '[':
                    result = parseArrayPathNode(charSource, buf, fromIndex, toIndex, path, path.head, true, parseContext);
                    break;
                case '\'':
                case '"':
                    result = parseStringPathNode(charSource, buf, fromIndex, beginByte, false, parseContext, null);
                    break;
                default:
                    throw new UnsupportedOperationException("unsupported for begin character with '" + beginByte + "'");
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
            JSONGeneral.handleCatchException(ex, buf, toIndex);
            throw new JSONException("Error: " + ex.getMessage(), ex);
        }
    }

    private static JSONNode parseObjectPathNode(CharSource charSource, char[] buf, int fromIndex, int toIndex, JSONNodePath nodePath, JSONNodePathCollector pathCollector, final boolean returnIfMatched, JSONNodeContext parseContext) throws Exception {
        int beginIndex = fromIndex + 1;
        char ch;
        String key;
        final boolean allowComment = parseContext.allowComment, extract = parseContext.extract;
        boolean matched, returnValueIfMatched;
        boolean isLastPathLevel = pathCollector.next == null;
        for (int i = beginIndex; i < toIndex; ++i) {
            while ((ch = buf[i]) <= ' ') {
                ++i;
            }
            if (allowComment) {
                if (ch == '/') {
                    ch = buf[i = JSONGeneral.clearCommentAndWhiteSpaces(buf, i + 1, parseContext)];
                }
            }
            if (ch == '"') {
                key = JSONDefaultParser.parseMapKeyByCache(buf, i, '"', parseContext);
                i = parseContext.endIndex + 1;
            } else {
                if (ch == '}') {
                    // parseContext.allowLastEndComma
                    parseContext.endIndex = i;
                    return null;
                }
                if (ch == '\'') {
                    // parseContext.allowSingleQuotes
                    key = JSONDefaultParser.parseMapKeyByCache(buf, i, '\'', parseContext);
                    i = parseContext.endIndex + 1;
                } else {
                    // parseContext.allowUnquotedFieldNames
                    int begin = i;
                    while (i + 1 < toIndex && buf[++i] != ':') ;
                    key = String.valueOf(JSONDefaultParser.parseKeyOfMap(buf, begin, i, true));
                }
            }
            while ((ch = buf[i]) <= ' ') {
                ++i;
            }
            if (allowComment) {
                if (ch == '/') {
                    ch = buf[i = JSONGeneral.clearCommentAndWhiteSpaces(buf, i + 1, parseContext)];
                }
            }
            if (ch == ':') {
                int mr = pathCollector.matchedObjectField(key);
                matched = mr > -1;
                returnValueIfMatched = mr == 1;
                while ((ch = buf[++i]) <= ' ') ;
                if (allowComment) {
                    if (ch == '/') {
                        ch = buf[i = JSONGeneral.clearCommentAndWhiteSpaces(buf, i + 1, parseContext)];
                    }
                }
                JSONNode node;
                boolean isSkipValue = !matched;
                switch (ch) {
                    case '{': {
                        if (isSkipValue || isLastPathLevel) {
                            JSONTypeDeserializer.MAP.skip(charSource, buf, i, parseContext);
                            node = isSkipValue ? null : new C(charSource, buf, i, parseContext.endIndex + 1, OBJECT, parseContext, null);
                        } else {
                            node = parseObjectPathNode(charSource, buf, i, toIndex, nodePath, pathCollector.next, returnIfMatched && returnValueIfMatched, parseContext);
                        }
                        break;
                    }
                    case '[': {
                        if (isSkipValue || isLastPathLevel) {
                            JSONTypeDeserializer.COLLECTION.skip(charSource, buf, i, parseContext);
                            node = isSkipValue ? null : new C(charSource, buf, i, parseContext.endIndex + 1, ARRAY, parseContext, null);
                        } else {
                            node = parseArrayPathNode(charSource, buf, i, toIndex, nodePath, pathCollector.next, returnIfMatched && returnValueIfMatched, parseContext);
                        }
                        break;
                    }
                    case '\'':
                    case '"': {
                        // 3 string
                        node = parseStringPathNode(charSource, buf, i, toIndex, ch, isSkipValue, parseContext, null);
                        break;
                    }
                    case 'n': {
                        // null
                        node = parseNullPathNode(charSource, buf, i, parseContext, null);
                        break;
                    }
                    case 't': {
                        node = parseBoolTruePathNode(charSource, buf, i, parseContext, null);
                        break;
                    }
                    case 'f': {
                        node = parseBoolFalsePathNode(charSource, buf, i, parseContext, null);
                        break;
                    }
                    default: {
                        // number
                        node = parseNumberPathNode(charSource, buf, i, '}', isSkipValue, parseContext, null);
                        break;
                    }
                }
                i = parseContext.endIndex;
                while ((ch = buf[++i]) <= ' ') ;
                if (allowComment) {
                    // clearComment and append whiteSpaces
                    if (ch == '/') {
                        ch = buf[i = JSONGeneral.clearCommentAndWhiteSpaces(buf, i + 1, parseContext)];
                    }
                }
                parseContext.endIndex = i;
                boolean skipNext = false;
                if (matched) {
                    if (extract) {
                        if (isLastPathLevel) {
                            parseContext.extractValue(/*isLeafValue ? node.value : */node);
                        }
                        if (returnValueIfMatched) {
                            if (returnIfMatched) {
                                return null;
                            } else {
                                skipNext = true;
                            }
                        }
                    } else {
                        return node;
                    }
                }
                boolean isClosingSymbol = ch == '}';
                if (ch == ',' || isClosingSymbol) {
                    if (isClosingSymbol) {
                        return null;
                    } else {
                        if (skipNext) {
                            JSONTypeDeserializer.MAP.skip(charSource, buf, i, parseContext);
                            return null;
                        }
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

    private static JSONNode parseObjectPathNode(CharSource charSource, byte[] buf, int fromIndex, int toIndex, JSONNodePath nodePath, JSONNodePathCollector pathCollector, final boolean returnIfMatched, JSONNodeContext parseContext) throws Exception {
        int beginIndex = fromIndex + 1;
        byte b;
        String key;
        final boolean allowComment = parseContext.allowComment, extract = parseContext.extract;
        boolean matched, returnValueIfMatched;
        boolean isLastPathLevel = pathCollector.next == null;
        for (int i = beginIndex; i < toIndex; ++i) {
            while ((b = buf[i]) <= ' ') {
                ++i;
            }
            if (allowComment) {
                if (b == '/') {
                    b = buf[i = JSONGeneral.clearCommentAndWhiteSpaces(buf, i + 1, parseContext)];
                }
            }
            if (b == '"') {
                key = JSONTypeDeserializer.parseMapKeyByCache(buf, i, '"', parseContext);
                i = parseContext.endIndex + 1;
            } else {
                if (b == '}') {
                    // parseContext.allowLastEndComma
                    parseContext.endIndex = i;
                    return null;
                }
                if (b == '\'') {
                    // parseContext.allowSingleQuotes
                    key = JSONTypeDeserializer.parseMapKeyByCache(buf, i, '\'', parseContext);
                    i = parseContext.endIndex + 1;
                } else {
                    // parseContext.allowUnquotedFieldNames
                    int begin = i;
                    while (i + 1 < toIndex && buf[++i] != ':') ;
                    key = String.valueOf(JSONDefaultParser.parseKeyOfMap(buf, begin, i, true));
                }
            }
            while ((b = buf[i]) <= ' ') {
                ++i;
            }
            if (allowComment) {
                if (b == '/') {
                    b = buf[i = JSONGeneral.clearCommentAndWhiteSpaces(buf, i + 1, parseContext)];
                }
            }
            if (b == ':') {
                int mr = pathCollector.matchedObjectField(key);
                matched = mr > -1;
                returnValueIfMatched = mr == 1;
                while ((b = buf[++i]) <= ' ') ;
                if (allowComment) {
                    if (b == '/') {
                        b = buf[i = JSONGeneral.clearCommentAndWhiteSpaces(buf, i + 1, parseContext)];
                    }
                }
                JSONNode node;
                boolean isSkipValue = !matched;
                switch (b) {
                    case '{': {
                        if (isSkipValue || isLastPathLevel) {
                            JSONTypeDeserializer.MAP.skip(charSource, buf, i, parseContext);
                            node = isSkipValue ? null : new B(charSource, buf, i, parseContext.endIndex + 1, OBJECT, parseContext, null);
                        } else {
                            node = parseObjectPathNode(charSource, buf, i, toIndex, nodePath, pathCollector.next, returnIfMatched && returnValueIfMatched, parseContext);
                        }
                        break;
                    }
                    case '[': {
                        if (isSkipValue || isLastPathLevel) {
                            JSONTypeDeserializer.COLLECTION.skip(charSource, buf, i, parseContext);
                            node = isSkipValue ? null : new B(charSource, buf, i, parseContext.endIndex + 1, ARRAY, parseContext, null);
                        } else {
                            node = parseArrayPathNode(charSource, buf, i, toIndex, nodePath, pathCollector.next, returnIfMatched && returnValueIfMatched, parseContext);
                        }
                        break;
                    }
                    case '\'':
                    case '"': {
                        // 3 string
                        node = parseStringPathNode(charSource, buf, i, b, isSkipValue, parseContext, null);
                        break;
                    }
                    case 'n': {
                        // null
                        node = parseNullPathNode(charSource, buf, i, parseContext, null);
                        break;
                    }
                    case 't': {
                        node = parseBoolTruePathNode(charSource, buf, i, parseContext, null);
                        break;
                    }
                    case 'f': {
                        node = parseBoolFalsePathNode(charSource, buf, i, parseContext, null);
                        break;
                    }
                    default: {
                        // number
                        node = parseNumberPathNode(charSource, buf, i, (byte) '}', isSkipValue, parseContext, null);
                        break;
                    }
                }
                i = parseContext.endIndex;
                while ((b = buf[++i]) <= ' ') ;
                if (allowComment) {
                    // clearComment and append whiteSpaces
                    if (b == '/') {
                        b = buf[i = JSONGeneral.clearCommentAndWhiteSpaces(buf, i + 1, parseContext)];
                    }
                }
                parseContext.endIndex = i;
                boolean skipNext = false;
                if (matched) {
                    if (extract) {
                        if (isLastPathLevel) {
                            parseContext.extractValue(/*isLeafValue ? node.value : */node);
                        }
                        if (returnValueIfMatched) {
                            if (returnIfMatched) {
                                return null;
                            } else {
                                skipNext = true;
                            }
                        }
                    } else {
                        return node;
                    }
                }
                boolean isClosingSymbol = b == '}';
                if (b == ',' || isClosingSymbol) {
                    if (isClosingSymbol) {
                        return null;
                    } else {
                        if (skipNext) {
                            JSONTypeDeserializer.MAP.skip(charSource, buf, i, parseContext);
                            return null;
                        }
                    }
                } else {
                    throw new JSONException("Syntax error, unexpected '" + b + "', position " + i);
                }
            } else {
                throw new JSONException("Syntax error, unexpected '" + b + "', position " + i);
            }
        }
        throw new JSONException("Syntax error, the closing symbol '}' is not found ");
    }

    private static JSONNode parseArrayPathNode(CharSource charSource, char[] buf, int fromIndex, int toIndex, JSONNodePath nodePath, JSONNodePathCollector pathCollector, final boolean returnIfMatched, JSONNodeContext parseContext) throws Exception {
        int beginIndex = fromIndex + 1;
        char ch;
        final boolean allowComment = parseContext.allowComment, extract = parseContext.extract;
        int elementIndex = 0;
        boolean returnValueIfMatched;
        int size = -1;
        boolean isLastPathLevel = pathCollector.next == null;
        boolean matched;
        if (pathCollector.preparedSize()) {
            JSONTypeDeserializer.COLLECTION.skip(charSource, buf, fromIndex, parseContext);
            size = parseContext.elementSize;
        }
        for (int i = beginIndex; i < toIndex; ++i) {
            int mr = pathCollector.matchedArrayIndex(elementIndex++, size);
            matched = mr > -1;
            returnValueIfMatched = mr == 1;
            while ((ch = buf[i]) <= ' ') {
                ++i;
            }
            if (allowComment) {
                if (ch == '/') {
                    ch = buf[i = JSONGeneral.clearCommentAndWhiteSpaces(buf, i + 1, parseContext)];
                }
            }
            if (ch == ']') {
                // parseContext.allowLastEndComma
                parseContext.endIndex = i;
                return null;
            }
            boolean isSkipValue = !matched;
            JSONNode node;
            switch (ch) {
                case '{': {
                    if (isSkipValue || isLastPathLevel) {
                        JSONTypeDeserializer.MAP.skip(charSource, buf, i, parseContext);
                        node = isSkipValue ? null : new C(charSource, buf, i, parseContext.endIndex + 1, OBJECT, parseContext, null);
                    } else {
                        node = parseObjectPathNode(charSource, buf, i, toIndex, nodePath, pathCollector.next, returnIfMatched && returnValueIfMatched, parseContext);
                    }
                    break;
                }
                case '[': {
                    if (isSkipValue || isLastPathLevel) {
                        JSONTypeDeserializer.COLLECTION.skip(charSource, buf, i, parseContext);
                        node = isSkipValue ? null : new C(charSource, buf, i, parseContext.endIndex + 1, ARRAY, parseContext, null);
                    } else {
                        node = parseArrayPathNode(charSource, buf, i, toIndex, nodePath, pathCollector.next, returnIfMatched && returnValueIfMatched, parseContext);
                    }
                    break;
                }
                case '\'':
                case '"': {
                    // 3 string
                    node = parseStringPathNode(charSource, buf, i, toIndex, ch, isSkipValue, parseContext, null);
                    break;
                }
                case 'n': {
                    // null
                    node = parseNullPathNode(charSource, buf, i, parseContext, null);
                    break;
                }
                case 't': {
                    node = parseBoolTruePathNode(charSource, buf, i, parseContext, null);
                    break;
                }
                case 'f': {
                    node = parseBoolFalsePathNode(charSource, buf, i, parseContext, null);
                    break;
                }
                default: {
                    // number
                    node = parseNumberPathNode(charSource, buf, i, ']', isSkipValue, parseContext, null);
                    break;
                }
            }
            i = parseContext.endIndex;
            while (i + 1 < toIndex && (ch = buf[++i]) <= ' ') ;
            if (allowComment) {
                if (ch == '/') {
                    ch = buf[i = JSONGeneral.clearCommentAndWhiteSpaces(buf, i + 1, parseContext)];
                }
            }
            parseContext.endIndex = i;
            boolean skipNext = false;
            if (matched) {
                if (extract) {
                    if (isLastPathLevel) {
                        parseContext.extractValue(/*isLeafValue ? node.value : */node);
                    }
                    if (returnValueIfMatched) {
                        if (returnIfMatched) {
                            return null;
                        } else {
                            skipNext = true;
                        }
                    }
                } else {
                    return node;
                }
            }
            boolean isEnd = ch == ']';
            if (ch == ',' || isEnd) {
                if (isEnd) {
                    return null;
                } else {
                    if (skipNext) {
                        JSONTypeDeserializer.COLLECTION.skip(charSource, buf, i, parseContext);
                        return null;
                    }
                }
            } else {
                throw new JSONException("Syntax error, unexpected '" + ch + "', position " + i + ", missing ',' or '}'");
            }
        }
        throw new JSONException("Syntax error, the closing symbol ']' is not found ");
    }

    private static JSONNode parseArrayPathNode(CharSource charSource, byte[] buf, int fromIndex, int toIndex, JSONNodePath nodePath, JSONNodePathCollector pathCollector, boolean returnIfMatched, JSONNodeContext parseContext) throws Exception {
        int beginIndex = fromIndex + 1;
        byte ch;
        final boolean allowComment = parseContext.allowComment, extract = parseContext.extract;
        int elementIndex = 0;
        boolean returnValueIfMatched;
        int size = -1;
        boolean isLastPathLevel = pathCollector.next == null;
        boolean matched;
        if (pathCollector.preparedSize()) {
            JSONTypeDeserializer.COLLECTION.skip(charSource, buf, fromIndex, parseContext);
            size = parseContext.elementSize;
        }
        for (int i = beginIndex; i < toIndex; ++i) {
            int mr = pathCollector.matchedArrayIndex(elementIndex++, size);
            matched = mr > -1;
            returnValueIfMatched = mr == 1;
            while ((ch = buf[i]) <= ' ') {
                ++i;
            }
            if (allowComment) {
                if (ch == '/') {
                    ch = buf[i = JSONGeneral.clearCommentAndWhiteSpaces(buf, i + 1, parseContext)];
                }
            }
            if (ch == ']') {
                // parseContext.allowLastEndComma
                parseContext.endIndex = i;
                return null;
            }
            boolean isSkipValue = !matched;
            JSONNode node;
            switch (ch) {
                case '{': {
                    if (isSkipValue || isLastPathLevel) {
                        JSONTypeDeserializer.MAP.skip(charSource, buf, i, parseContext);
                        node = isSkipValue ? null : new B(charSource, buf, i, parseContext.endIndex + 1, OBJECT, parseContext, null);
                    } else {
                        node = parseObjectPathNode(charSource, buf, i, toIndex, nodePath, pathCollector.next, returnIfMatched && returnValueIfMatched, parseContext);
                    }
                    break;
                }
                case '[': {
                    if (isSkipValue || isLastPathLevel) {
                        JSONTypeDeserializer.COLLECTION.skip(charSource, buf, i, parseContext);
                        node = isSkipValue ? null : new B(charSource, buf, i, parseContext.endIndex + 1, ARRAY, parseContext, null);
                    } else {
                        node = parseArrayPathNode(charSource, buf, i, toIndex, nodePath, pathCollector.next, returnIfMatched && returnValueIfMatched, parseContext);
                    }
                    break;
                }
                case '\'':
                case '"': {
                    // 3 string
                    node = parseStringPathNode(charSource, buf, i, ch, isSkipValue, parseContext, null);
                    break;
                }
                case 'n': {
                    // null
                    node = parseNullPathNode(charSource, buf, i, parseContext, null);
                    break;
                }
                case 't': {
                    node = parseBoolTruePathNode(charSource, buf, i, parseContext, null);
                    break;
                }
                case 'f': {
                    node = parseBoolFalsePathNode(charSource, buf, i, parseContext, null);
                    break;
                }
                default: {
                    // number
                    node = parseNumberPathNode(charSource, buf, i, (byte) ']', isSkipValue, parseContext, null);
                    break;
                }
            }
            i = parseContext.endIndex;
            while (i + 1 < toIndex && (ch = buf[++i]) <= ' ') ;
            if (allowComment) {
                if (ch == '/') {
                    ch = buf[i = JSONGeneral.clearCommentAndWhiteSpaces(buf, i + 1, parseContext)];
                }
            }
            parseContext.endIndex = i;
            boolean skipNext = false;
            if (matched) {
                if (extract) {
                    if (isLastPathLevel) {
                        parseContext.extractValue(/*isLeafValue ? node.value : */node);
                    }
                    if (returnValueIfMatched) {
                        if (returnIfMatched) {
                            return null;
                        } else {
                            skipNext = true;
                        }
                    }
                } else {
                    return node;
                }
            }
            boolean isEnd = ch == ']';
            if (ch == ',' || isEnd) {
                if (isEnd) {
                    return null;
                } else {
                    if (skipNext) {
                        JSONTypeDeserializer.COLLECTION.skip(charSource, buf, i, parseContext);
                        return null;
                    }
                }
            } else {
                throw new JSONException("Syntax error, unexpected '" + (char) ch + "', position " + i + ", missing ',' or '}'");
            }
        }
        throw new JSONException("Syntax error, the closing symbol ']' is not found ");
    }

    private static JSONNode parseStringPathNode(CharSource charSource, char[] buf, int fromIndex, int toIndex, char endToken, boolean skipValue, JSONNodeContext parseContext, JSONNode rootNode) throws Exception {
        if (skipValue) {
            JSONTypeDeserializer.CHAR_SEQUENCE_STRING.skip(charSource, buf, fromIndex, endToken, parseContext);
            return null;
        }
        String value = (String) JSONTypeDeserializer.CHAR_SEQUENCE_STRING.deserializeString(charSource, buf, fromIndex, endToken, GenericParameterizedType.StringType, parseContext);
        int endIndex = parseContext.endIndex;
        return new C(charSource, value, buf, fromIndex, endIndex + 1, STRING, parseContext, rootNode);
    }

    private static JSONNode parseStringPathNode(CharSource charSource, byte[] buf, int fromIndex, byte endToken, boolean skipValue, JSONNodeContext parseContext, JSONNode rootNode) throws Exception {
        if (skipValue) {
            JSONTypeDeserializer.CHAR_SEQUENCE_STRING.skip(charSource, buf, fromIndex, endToken, parseContext);
            return null;
        }
        String value = (String) JSONTypeDeserializer.CHAR_SEQUENCE_STRING.deserializeString(charSource, buf, fromIndex, endToken, GenericParameterizedType.StringType, parseContext);
        int endIndex = parseContext.endIndex;
        return new B(charSource, value, buf, fromIndex, endIndex + 1, STRING, parseContext, rootNode);
    }

    static JSONNode parseNullPathNode(CharSource charSource, char[] buf, int fromIndex, JSONNodeContext parseContext, JSONNode rootNode) throws Exception {
        JSONTypeDeserializer.parseNull(buf, fromIndex, parseContext);
        int endIndex = parseContext.endIndex;
        return new C(charSource, null, buf, fromIndex, endIndex + 1, NULL, parseContext, rootNode);
    }

    static JSONNode parseNullPathNode(CharSource charSource, byte[] buf, int fromIndex, JSONNodeContext parseContext, JSONNode rootNode) throws Exception {
        JSONTypeDeserializer.parseNull(buf, fromIndex, parseContext);
        int endIndex = parseContext.endIndex;
        return new B(charSource, null, buf, fromIndex, endIndex + 1, NULL, parseContext, rootNode);
    }

    static JSONNode parseBoolTruePathNode(CharSource charSource, char[] buf, int fromIndex, JSONNodeContext parseContext, JSONNode rootNode) throws Exception {
        JSONTypeDeserializer.parseTrue(buf, fromIndex, parseContext);
        int endIndex = parseContext.endIndex;
        return new C(charSource, true, buf, fromIndex, endIndex + 1, BOOLEAN, parseContext, rootNode);
    }

    static JSONNode parseBoolTruePathNode(CharSource charSource, byte[] buf, int fromIndex, JSONNodeContext parseContext, JSONNode rootNode) throws Exception {
        JSONTypeDeserializer.parseTrue(buf, fromIndex, parseContext);
        int endIndex = parseContext.endIndex;
        return new B(charSource, true, buf, fromIndex, endIndex + 1, BOOLEAN, parseContext, rootNode);
    }

    static JSONNode parseBoolFalsePathNode(CharSource charSource, char[] buf, int fromIndex, JSONNodeContext parseContext, JSONNode rootNode) throws Exception {
        JSONTypeDeserializer.parseFalse(buf, fromIndex, parseContext);
        int endIndex = parseContext.endIndex;
        return new C(charSource, false, buf, fromIndex, endIndex + 1, BOOLEAN, parseContext, rootNode);
    }

    static JSONNode parseBoolFalsePathNode(CharSource charSource, byte[] buf, int fromIndex, JSONNodeContext parseContext, JSONNode rootNode) throws Exception {
        JSONTypeDeserializer.parseFalse(buf, fromIndex, parseContext);
        int endIndex = parseContext.endIndex;
        return new B(charSource, false, buf, fromIndex, endIndex + 1, BOOLEAN, parseContext, rootNode);
    }

    static JSONNode parseNumberPathNode(CharSource charSource, char[] buf, int fromIndex, char endToken, boolean skipValue, JSONNodeContext parseContext, JSONNode rootNode) throws Exception {
        if (skipValue) {
            JSONTypeDeserializer.NUMBER_SKIPPER.deserialize(charSource, buf, fromIndex, null, null, endToken, parseContext);
            return null;
        }
        Number value = (Number) JSONTypeDeserializer.NUMBER.deserialize(charSource, buf, fromIndex, parseContext.useBigDecimalAsDefault ? GenericParameterizedType.BigDecimalType : GenericParameterizedType.AnyType, null, endToken, parseContext);
        int endIndex = parseContext.endIndex;
        return new C(charSource, value, buf, fromIndex, endIndex + 1, NUMBER, parseContext, rootNode);
    }

    static JSONNode parseNumberPathNode(CharSource charSource, byte[] buf, int fromIndex, byte endToken, boolean skipValue, JSONNodeContext parseContext, JSONNode rootNode) throws Exception {
        if (skipValue) {
            JSONTypeDeserializer.NUMBER_SKIPPER.deserialize(charSource, buf, fromIndex, null, null, endToken, parseContext);
            return null;
        }
        Number value = (Number) JSONTypeDeserializer.NUMBER.deserialize(charSource, buf, fromIndex, parseContext.useBigDecimalAsDefault ? GenericParameterizedType.BigDecimalType : GenericParameterizedType.AnyType, null, endToken, parseContext);
        int endIndex = parseContext.endIndex;
        return new B(charSource, value, buf, fromIndex, endIndex + 1, NUMBER, parseContext, rootNode);
    }

    /**
     * 获取根节点 root
     *
     * @return
     */
    public final JSONNode root() {
        return root;
    }

    /**
     * 强制加载此节点的所有子孙节点信息
     */
    public final void loadAll() {
        if (!completed) {
            completeNode(false, false);
        }
        if (array) {
            for (int i = 0; i < elementSize; ++i) {
                elementValues[i].loadAll();
            }
        } else {
            if (type == OBJECT) {
                for (JSONNode node : fieldValues.values()) {
                    node.loadAll();
                }
            }
        }
    }

    // todo Short circuit optimization is possible
    public static <T> T first(String source, String xpath, Class<T> targetClass, ReadOption... options) {
        List<JSONNode> nodes = collect(source, xpath, options);
        return nodes.get(0).getValue(targetClass);
    }

    public static <T> T firstIfEmpty(String source, String xpath, Class<T> targetClass, T defaultVal, ReadOption... options) {
        List<JSONNode> nodes = collect(source, xpath, options);
        if (nodes.size() > 0) {
            return nodes.get(0).getValue(targetClass);
        }
        return defaultVal;
    }

    public static <T> T last(String source, String xpath, Class<T> targetClass, ReadOption... options) {
        List<JSONNode> nodes = collect(source, xpath, options);
        return nodes.get(nodes.size() - 1).getValue(targetClass);
    }

    /**
     * <p> xpath语法支持（仅仅支持以下简单语法，部分语法进行了语义替换，xpath复杂的语法由于易用性（难记）不考虑实现）
     *
     * <h4>路径分隔语法(和xpath一致)</h4>
     * <li>"//" :  从当前节点开始查找所有满足条件的节点，并遍历所有子孙节点；</li>
     * <li>"/"  :  仅从当前节点开始查找所有满足条件的节点（不包括子孙节点）；</li>
     * <p> 更多请查看JSONNodePath
     *
     * @param json
     * @param xpath
     * @param options
     * @return
     * @see JSONNodePath
     */
    public final static List<JSONNode> collect(String json, String xpath, ReadOption... options) {
        return collect(json, JSONNodePath.parse(xpath), JSONNodeCollector.DEFAULT, options);
    }

    /**
     * <p> xpath语法支持（仅仅支持以下简单语法，部分语法进行了语义替换，xpath复杂的语法由于易用性（难记）不考虑实现）
     *
     * <h4>路径分隔语法(和xpath一致)</h4>
     * <li>"//" :  从当前节点开始查找所有满足条件的节点，并遍历所有子孙节点；</li>
     * <li>"/"  :  仅从当前节点开始查找所有满足条件的节点（不包括子孙节点）；</li>
     * <p> 更多请查看JSONNodePath
     *
     * @param bytes
     * @param xpath
     * @param options
     * @return
     * @see JSONNodePath
     */
    public final static List<JSONNode> collect(byte[] bytes, String xpath, ReadOption... options) {
        return collect(bytes, JSONNodePath.parse(xpath), JSONNodeCollector.DEFAULT, options);
    }

    /**
     * 解析并收集
     *
     * @param json
     * @param path
     * @param options
     * @return
     */
    public final static List<JSONNode> collect(String json, JSONNodePath path, ReadOption... options) {
        return collect(json, path, JSONNodeCollector.DEFAULT, options);
    }

    /**
     * <p> xpath语法支持（仅仅支持以下简单语法，部分语法进行了语义替换，xpath复杂的语法由于易用性（难记）不考虑实现）
     *
     * <h4>路径分隔语法(和xpath一致)</h4>
     * <li>"//" :  从当前节点开始查找所有满足条件的节点，并遍历所有子孙节点；</li>
     * <li>"/"  :  仅从当前节点开始查找所有满足条件的节点（不包括子孙节点）；</li>
     *
     * @param json
     * @param xpath
     * @param collector
     * @param options
     * @return
     * @see JSONNodePath
     */
    public final static <T> List<T> collect(String json, String xpath, JSONNodeCollector<T> collector, ReadOption... options) {
        return collect(json, JSONNodePath.parse(xpath), collector, options);
    }

    public final static <T> List<T> collect(String json, JSONNodePath path, JSONNodeCollector<T> nodeCollector, ReadOption... options) {
        path.head.self();
        if (path.supportedExtract) {
            return extract(json, path, nodeCollector, options);
        } else {
            JSONNodePathCollector head = path.head;
            if (head.isSupportedExtract()) {
                JSONNodePath extractPath = JSONNodePath.create();
                extractPath.next(head.clone());
                JSONNodePathCollector tail = head.next;
                while (tail.isSupportedExtract()) {
                    extractPath.next(tail.clone());
                    if (tail.next != null) {
                        tail = tail.next;
                    } else {
                        break;
                    }
                }
                List<T> results = new ArrayList<T>();
                JSONNodePathCtx pathCtx = path.newCollectCtx();
                List<JSONNode> extractNodes = extract(json, extractPath, options);
                for (JSONNode node : extractNodes) {
                    tail.collect(node, results, nodeCollector, pathCtx);
                }
                return results;
            } else {
                JSONNode root = JSONNode.parse(json, options);
                root.ensureCompleted(false, false);
                return path.collect(root, nodeCollector);
            }
        }
    }

    public final static <T> List<T> collect(byte[] bytes, JSONNodePath path, JSONNodeCollector<T> nodeCollector, ReadOption... options) {
        path.head.self();
        if (path.supportedExtract) {
            return extract(bytes, path, nodeCollector, options);
        } else {
            JSONNodePathCollector head = path.head;
            if (head.isSupportedExtract()) {
                JSONNodePath extractPath = JSONNodePath.create();
                extractPath.next(head.clone());
                JSONNodePathCollector tail = head.next;
                while (tail.isSupportedExtract()) {
                    extractPath.next(tail.clone());
                    if (tail.next != null) {
                        tail = tail.next;
                    } else {
                        break;
                    }
                }
                List<T> results = new ArrayList<T>();
                JSONNodePathCtx pathCtx = path.newCollectCtx();
                List<JSONNode> extractNodes = extract(bytes, extractPath, JSONNodeCollector.DEFAULT, options);
                for (JSONNode node : extractNodes) {
                    tail.collect(node, results, nodeCollector, pathCtx);
                }
                return results;
            } else {
                JSONNode root = JSONNode.parse(bytes, options);
                root.ensureCompleted(false, false);
                return path.collect(root, nodeCollector);
            }
        }
    }

    public final List<JSONNode> collect(String xpath) {
        return JSONNodePath.parse(xpath).collect(this, JSONNodeCollector.DEFAULT);
    }

    /**
     * @param path
     * @return
     */
    public final List<JSONNode> collect(JSONNodePath path) {
        return path.collect(this, JSONNodeCollector.DEFAULT);
    }

    /**
     * 收集所有节点
     *
     * @param leaf 是否叶子节点
     * @return
     */
    public final List<JSONNode> all(boolean leaf) {
        return JSONNodePath.ALL.collect(this, leaf ? JSONNodeCollector.DEFAULT.onlyCollectLeaf() : JSONNodeCollector.DEFAULT);
    }

    public final List<Object> collectAs(JSONNodePath path) {
        return path.collect(this, JSONNodeCollector.ANY);
    }

    public final <T> List<T> collectAs(JSONNodePath path, JSONNodeCollector<T> collector) {
        return path.collect(this, collector);
    }

    /**
     * 精确定位到指定子节点或孙子节点
     *
     * @param paths
     * @return
     */
    public final JSONNode getByPaths(Serializable... paths) {
        if (paths.length == 0) {
            return this;
        }
        try {
            return getByPaths(paths, 0, paths.length);
        } catch (Throwable throwable) {
            return null;
        }
    }

    final JSONNode getByPaths(Serializable[] paths, int offset, final int endIndex) {
        if (leaf) return null;
        Serializable path = paths[offset++];
        JSONNode node;
        if (array) {
            if (!(path instanceof Integer)) {
                return null;
            }
            int i = (Integer) path;
            if (i < 0) {
                ensureCompleted(true);
                i += elementSize;
                if (i < 0 || i >= elementSize) {
                    return null;
                }
                node = elementValues[i];
            } else {
                node = getElementAt(i);
            }
        } else {
            if (path instanceof String) {
                node = getFieldNodeAt((String) path);
            } else {
                return null;
            }
        }
        if (node == null) {
            return null;
        }
        if (offset == endIndex) {
            return node;
        } else {
            return node.getByPaths(paths, offset, endIndex);
        }
    }

    /**
     * 获取指定路径的节点对象（和extract传的参数不一样，这里的path是节点的路径，返回唯一的节点对象）
     *
     * <p>如果path为空将返回当前JSONNode节点对象 </p>
     * <p>如果/开头表示从根节点开始查找,否则从当前节点开始查找 </p>
     * <p>数组元素直接使用下标例如： '/children/1', 弃用了中括号（浪费性能）</p>
     *
     * @param path 查找路径，以/作为级别分割线
     * @return
     */
    public final JSONNode get(String path) {
        if (path == null) {
            return this;
        }
        int splitIndex = path.indexOf('/', 0);
        if (splitIndex == -1) {
            return getChild(path);
        }
        Object stringValue = JSONGeneral.getStringValue(path = path.trim());
        if (EnvUtils.JDK_9_PLUS) {
            if (path.length() == ((byte[]) stringValue).length) {
                return get(path, (byte[]) stringValue, 0, path.length());
            } else {
                return get(path, path.toCharArray(), 0, path.length());
            }
        } else {
            return get(path, (char[]) stringValue, 0, path.length());
        }
    }

    private JSONNode get(String path, char[] pathChars, int beginIndex, int endIndex) {
        if (beginIndex == endIndex) {
            return this;
        }
        char beginChar = pathChars[beginIndex];
        if (beginChar == '/') {
            return root.get(path, pathChars, beginIndex + 1, endIndex);
        }
        if (leaf) {
            return null;
        }
        int splitIndex = path.indexOf('/', beginIndex + 1);
        if (splitIndex == -1) {
            return getPathNode(path, pathChars, beginIndex, endIndex - beginIndex/*, hashValue*/);
        } else {
            JSONNode childNode = getPathNode(path, pathChars, beginIndex, splitIndex - beginIndex/*, hashValue*/);
            if (childNode != null) {
                return childNode.get(path, pathChars, splitIndex + 1, endIndex);
            }
        }
        return null;
    }

    private JSONNode get(String path, byte[] pathBytes, int beginIndex, int endIndex) {
        if (beginIndex == endIndex) {
            return this;
        }
        byte begin = pathBytes[beginIndex];
        if (begin == '/') {
            return root.get(path, pathBytes, beginIndex + 1, endIndex);
        }
        if (leaf) {
            return null;
        }
        int splitIndex = path.indexOf('/', beginIndex + 1);
        if (splitIndex == -1) {
            return getPathNode(path, pathBytes, beginIndex, endIndex - beginIndex/*, hashValue*/);
        } else {
            JSONNode childNode = getPathNode(path, pathBytes, beginIndex, splitIndex - beginIndex/*, hashValue*/);
            if (childNode != null) {
                return childNode.get(path, pathBytes, splitIndex + 1, endIndex);
            }
        }
        return null;
    }

    /**
     * 获取所有的字段名称
     *
     * @return
     */
    public final Collection<Serializable> keyNames() {
        if (array) {
            throw new UnsupportedOperationException();
        }
        if (!completed) {
            completeNode(true, false);
        }
        return fieldValues.keySet();
    }

    private JSONNode getPathNode(String path, char[] pathChars, int offset, int len) {
        if (array) {
            int i = offset, end = offset + len;
            try {
                int index = 0;
                do {
                    char ch = pathChars[i++];
                    if (NumberUtils.isDigit(ch)) {
                        index = (index << 3) + (index << 1) + (ch & 0xf);
                    } else {
                        throw new IllegalArgumentException("mismatch array index '" + path.substring(offset, end) + "'");
                    }
                } while (i < end);
                return elementSize > index || completed ? elementValues[index] : parseArrayAt(index, true, false);
            } catch (Throwable throwable) {
                throw new IllegalArgumentException("mismatch array index '" + path.substring(offset, end) + "'");
            }
        } else {
            String field = matchFieldName(pathChars, offset, len);
            return getFieldNodeAt(field);
        }
    }

    private JSONNode getPathNode(String path, byte[] pathBytes, int offset, int len) {
        if (array) {
            int i = offset, end = offset + len;
            try {
                int index = 0;
                do {
                    byte ch = pathBytes[i++];
                    if (NumberUtils.isDigit(ch)) {
                        index = (index << 3) + (index << 1) + (ch & 0xf);
                    } else {
                        throw new IllegalArgumentException("mismatch array index '" + path.substring(offset, end) + "'");
                    }
                } while (i < end);
                return elementSize > index || completed ? elementValues[index] : parseArrayAt(index, true, false);
            } catch (Throwable throwable) {
                throw new IllegalArgumentException("mismatch array index '" + path.substring(offset, end) + "'");
            }
        } else {
            String field = matchFieldName(pathBytes, offset, len);
            return getFieldNodeAt(field);
        }
    }

    private String matchFieldName(char[] chars, int offset, int len) {
        long hashValue = JSONGeneral.ESCAPE_BACKSLASH;
        boolean ascii = true;
        for (int i = 0; i < len; ++i) {
            char ch = chars[offset + i];
            if (ch > 0xFF) {
                hashValue = (hashValue << 16) | ch;
                ascii = false;
            } else {
                hashValue = hashValue << 8 | ch;
            }
        }
        if (ascii && len <= 8) {
            return parseContext.getCacheEightCharsKey(chars, offset, len, hashValue);
        }
        return parseContext.getCacheKey(chars, offset, len, hashValue);
    }

    private String matchFieldName(byte[] bytes, int offset, int len) {
        long hashValue = JSONGeneral.ESCAPE_BACKSLASH;
        for (int i = 0; i < len; ++i) {
            byte ch = bytes[offset + i];
            hashValue = hashValue << 8 | ch;
        }
        if (len <= 8) {
            return parseContext.getCacheEightBytesKey(bytes, offset, len, hashValue);
        }
        return parseContext.getCacheKey(bytes, offset, len, hashValue);
    }

    final JSONNode getFieldNodeAt(String field) {
        if (fieldValues != null) {
            JSONNode node = fieldValues.get(field);
            if (node != null) {
                return node;
            }
        } else {
            fieldValues = new LinkedHashMap(8);
        }
        if (completed) {
            return null;
        }
        return parseObjectField(field, true, false);
    }

    public final JSONNode getChild(String field) {
        if (type == OBJECT) {
            return getFieldNodeAt(field);
        } else if (array) {
            int index;
            try {
                index = Integer.parseInt(field);
            } catch (Throwable throwable) {
                throw new IllegalArgumentException("input not a number string: '" + field + '\'');
            }
            parseElementTo(index);
            if (index < 0) {
                index += elementSize;
            }
            return elementValues[index];
        } else {
            throw new UnsupportedOperationException();
        }
    }

    private JSONNode getPathNode(String field) {
        if (array) {
            int index, length = field.length();
            try {
                index = readArrayIndex(field, 0, length);
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

    protected static int readArrayIndex(String buf, int offset, int len) {
        if (len == 0) return -1;
        int value = 0;
        char ch;
        for (int i = offset, n = i + len; i < n; ++i) {
            ch = buf.charAt(i);
            int d = NumberUtils.digitDecimal(ch);
            if (d < 0) {
                if (ch <= ' ') continue;
                throw new IllegalArgumentException("mismatch array index: \"" + buf.substring(offset, offset + len) + "\"");
            }
            value = value * 10 + d;
        }
        return value;
    }

    static boolean matchJSONKey(CharSource charSource, byte[] buf, int offset, byte[] keyBytes, JSONNodeContext parseContext) {
        if (keyBytes != null) {
            int beginIndex = offset + 1, fieldLength = keyBytes.length;
            boolean matched = true;
            int i = 0;
            for (; i < fieldLength; ++i) {
                if (keyBytes[i] != buf[beginIndex + i]) {
                    matched = false;
                    break;
                }
            }
            if (matched) {
                int endIndex = fieldLength + beginIndex;
                if (buf[endIndex] == '"') {
                    parseContext.endIndex = endIndex;
                    return true;
                }
            }
            try {
                JSONTypeDeserializer.CHAR_SEQUENCE_STRING.skip(charSource, buf, beginIndex + i, (byte) '"', parseContext);
            } catch (Exception e) {
                throw e instanceof RuntimeException ? (RuntimeException) e : new JSONException(e);
            }
            return false;
        } else {
            try {
                JSONTypeDeserializer.CHAR_SEQUENCE_STRING.skip(charSource, buf, offset + 1, (byte) '"', parseContext);
            } catch (Exception e) {
                throw e instanceof RuntimeException ? (RuntimeException) e : new JSONException(e);
            }
            return false;
        }
    }

    // 关闭Extract模式
    public void closeExtractMode() {
        parseContext.extract = false;
    }

    /**
     * 针对bytes(B)有按需加载优化，chars(C)无
     *
     * @param fieldName
     * @param lazy
     * @param createObj
     * @return
     */
    JSONNode parseObjectField(String fieldName, boolean lazy, boolean createObj) {
        return null;
    }

    final void addElementNode(JSONNode node) {
        if (elementValues == null) {
            elementValues = new JSONNode[8];
        } else {
            // add to list
            if (this.elementSize == elementValues.length) {
                JSONNode[] tmp = elementValues;
                elementValues = new JSONNode[this.elementSize << 1];
                System.arraycopy(tmp, 0, elementValues, 0, tmp.length);
            }
        }
        node.path = elementSize;
        elementValues[elementSize++] = node;
    }

    public final JSONNode getElementAt(int index) {
        if (elementSize > index) {
            return elementValues[index];
        }
        if (completed) {
            return null;
        }
        if (array) {
            return parseArrayAt(index, true, false);
        }
        throw new UnsupportedOperationException("element at " + index);
    }

    final void parseElementTo(int index) {
        if (index < 0) {
            if (!completed) {
                parseArrayAt(-1, true, false);
            }
        } else {
            if (!completed && (elementSize <= index || index == -1)) {
                parseArrayAt(index, true, false);
            }
        }
    }

    public final int getElementCount() {
        if (array) {
            if (completed) {
                return elementSize;
            }
            parseArrayAt(-1, true, false);
            return elementSize;
        }
        return -1;
    }

    public final JSONNode parent() {
        return this.parent;
    }

    /**
     * 获取指定路径上的节点并转化为E
     *
     * @param path  JSON Absolute Path
     * @param clazz
     * @param <E>
     * @return
     */
    public final <E> E getPathValue(String path, Class<E> clazz) {
        JSONNode jsonNode = get(path);
        if (jsonNode == null)
            return null;
        return jsonNode.getValue(clazz);
    }

    public final <E> E getChildValue(String name, Class<E> eClass) {
        JSONNode jsonNode = getPathNode(name);
        if (jsonNode == null)
            return null;
        if (jsonNode.array) {
            if (eClass == String.class) {
                return (E) jsonNode.source();
            }
            throw new UnsupportedOperationException("not matched " + eClass);
        }
        return jsonNode.getValue(eClass);
    }

    public final String getStringValue() {
        return getValue(String.class);
    }

    public final Serializable getValue() {
        if (this.value != null) return this.value;
        return getValue(null);
    }

    /**
     * @return map/list/string/number/boolean/null
     */
    public final Object any() {
        if (leaf) {
            return value;
        }
        if (!changed && any != null) {
            return any;
        }
        ensureCompleted(false, true);
        if (array) {
            List list = new ArrayList(elementSize);
            for (int i = 0; i < elementSize; ++i) {
                JSONNode node = elementValues[i];
                list.add(node.leaf ? node.value : node.any());
            }
            return any = list;
        } else {
            Map map = new LinkedHashMap(fieldValues.size());
            Set<Map.Entry<Serializable, JSONNode>> entrySet = fieldValues.entrySet();
            for (Map.Entry<Serializable, JSONNode> entry : entrySet) {
                JSONNode node = entry.getValue();
                map.put(entry.getKey(), node.leaf ? node.value : node.any());
            }
            return any = map;
        }
    }

    public final <E> E getValue(Class<E> eClass) {
        if (leaf) {
            if (eClass == null || type == NULL) {
                return (E) value;
            }
            if (eClass.isPrimitive()) {
                return ObjectUtils.toType(value, eClass);
            }
            if (eClass.isInstance(value)) {
                return (E) value;
            }
            if (eClass == String.class) {
                return (E) getText();
            }
            if (eClass == char[].class) {
                return (E) getText().toCharArray();
            }
            if (eClass == byte[].class) {
                String text = getText();
                if (parseContext.byteArrayFromHexString) {
                    return (E) hexString2Bytes();
                } else {
                    return (E) Base64Utils.decode(text);
                }
            }
            if (Enum.class.isAssignableFrom(eClass)) {
                if (type == STRING) {
                    Class enumCls = eClass;
                    return (E) Enum.valueOf(enumCls, (String) value);
                } else if (value instanceof Integer) {
                    int index = (Integer) value;
                    E[] enumConstants = eClass.getEnumConstants();
                    if (index > -1 && index < enumConstants.length) {
                        return eClass.getEnumConstants()[index];
                    } else {
                        return null;
                    }
                } else {
                    throw new JSONException("source [" + source() + "] cannot convert to Enum '" + eClass + "");
                }
            }
            try {
                Object result = ObjectUtils.toType(source(), eClass);
                if (result != null) return (E) result;
                JSONTypeDeserializer typeDeserializer = JSONTypeDeserializer.getTypeDeserializer(eClass);
                if (typeDeserializer != null) {
                    return (E) typeDeserializer.valueOf(value != null ? String.valueOf(value) : source(), eClass);
                }
            } catch (Throwable throwable) {
            }
            return null;
        } else {
            if (array) {
                if (eClass == null || eClass == Object.class) {
                    return (E) toCollection(ArrayList.class, Object.class);
                }
                if (eClass.isArray()) {
                    return toArray(eClass);
                }
                if (Collection.class.isAssignableFrom(eClass)) {
                    return (E) toCollection(eClass, Object.class);
                }
                // collection
                throw new UnsupportedOperationException("array node not supported " + eClass);
            }
            // object
            if (eClass != null) {
                if (eClass.isEnum() || eClass.isPrimitive() || eClass.isArray()) {
                    String msg = "source '" + source() + "' not supported to type " + eClass;
                    throw new UnsupportedOperationException(msg);
                }
            }
            return toBean(eClass);
        }
    }

    public final Map asMap() {
        return (Map) any();
    }

    public final List asList() {
        return (List) any();
    }

    public final <E> Map<?, E> asMap(Class<? extends Map> eClass, Class<E> valueCls) {
        return toBean(eClass, valueCls);
    }

    public final <E> E toBean(Class<E> eClass) {
        return toBean(eClass, null);
    }

    final <E> E toBean(Class<E> eClass, Class actualCls) {
        if (eClass == String.class) {
            return (E) this.source();
        }
        if (leaf) {
            throw new UnsupportedOperationException("leaf value not supported " + eClass);
        }
        Object instance;
        boolean isMapInstance;
        ClassStrucWrap strucWrap = null;
        if (eClass == null || eClass == Object.class || eClass == Map.class || eClass == LinkedHashMap.class) {
            instance = new LinkedHashMap();
            isMapInstance = true;
        } else {
            strucWrap = ClassStrucWrap.get(eClass);
            if (strucWrap == null) {
                if (array && Collection.class.isAssignableFrom(eClass)) {
                    return (E) toCollection(eClass, actualCls);
                }
                throw new UnsupportedOperationException("object node not supported " + eClass);
            }
            isMapInstance = strucWrap.isAssignableFromMap();
            try {
                if (!isMapInstance) {
                    instance = strucWrap.newInstance();
                } else {
                    instance = eClass.newInstance();
                }
            } catch (Throwable throwable) {
                throw new JSONException("create instance error.", throwable);
            }
        }
        if (!completed) {
            completeNode(false, false);
        }
        for (Map.Entry<Serializable, JSONNode> entry : fieldValues.entrySet()) {
            Serializable key = entry.getKey();
            JSONNode childNode = entry.getValue();
            if (isMapInstance) {
                Map map = (Map) instance;
                Object value;
                if (childNode.array) {
                    value = childNode.toCollection(ArrayList.class, Object.class);
                } else {
                    value = childNode.getValue(actualCls);
                }
                map.put(key, value);
            } else {
                String fieldName = key.toString();
                SetterInfo setterInfo = strucWrap.getSetterInfo(fieldName);
                if (setterInfo != null) {
                    Class<?> parameterType = setterInfo.getParameterType();
                    GenericParameterizedType parameterizedType = setterInfo.getGenericParameterizedType();
                    Object value;
                    if (childNode.array) {
                        Class entityClass = parameterizedType.getValueType() == null ? Object.class : parameterizedType.getValueType().getActualType();
                        value = childNode.toCollection(parameterizedType.getActualType(), entityClass);
                    } else {
                        value = childNode.getValue(parameterType);
                    }
                    JSONGeneral.JSON_SECURE_TRUSTED_ACCESS.set(setterInfo, instance, value); // setterInfo.invoke(instance, value);
                }
            }
        }
        return (E) instance;
    }

    public final <E> List<E> toList(Class<E> entityClass) {
        return (List<E>) toCollection(ArrayList.class, entityClass);
    }

    public final <E> E toArray(Class<E> arrayClass) {
        if (!arrayClass.isArray()) {
            throw new JSONException(arrayClass + " is not an array class");
        }
        return (E) toCollection(arrayClass, arrayClass.getComponentType());
    }

    public final Object toCollection(Class<?> collectionCls, Class<?> entityClass) {
        collectionCls.getClass();
        if (!completed) {
            completeNode(false, false);
        }
        Collection collection = null;
        boolean isArrayCls = collectionCls.isArray(), primitive = entityClass.isPrimitive();
        Object arrayObj = null;
        if (isArrayCls) {
            arrayObj = Array.newInstance(entityClass, elementSize); // primitive ? Array.newInstance(entityClass, elementSize) : new Object[elementSize];
        } else {
            collection = JSONGeneral.createCollectionInstance(collectionCls, elementSize);
        }
        for (int i = 0; i < elementSize; ++i) {
            JSONNode node = elementValues[i];
            Object result;
            if (node.array) {
                if (entityClass.isArray()) {
                    result = node.toCollection(entityClass, entityClass.getComponentType());
                } else {
                    result = node.toCollection(ArrayList.class, Object.class);
                }
            } else {
                result = node.getValue(entityClass);
            }
            if (isArrayCls) {
                if (primitive) {
                    Array.set(arrayObj, i, result);
                } else {
                    ((Object[]) arrayObj)[i] = result;
                }
            } else {
                collection.add(result);
            }
        }
        return isArrayCls ? arrayObj : collection;
    }

    final void ensureCompleted(boolean lazy) {
        ensureCompleted(lazy, false);
    }

    final void ensureCompleted(boolean lazy, boolean createObj) {
        if (!completed) {
            completeNode(lazy, createObj);
        }
    }

    private void completeNode(boolean lazy, boolean createObj) {
        if (array) {
            parseArrayAt(-1, lazy, createObj);
        } else {
            if (fieldValues == null) {
                fieldValues = new LinkedHashMap(8);
            }
            parseObjectField(null, lazy, createObj);
        }
    }

    /**
     * 支持数组元素求元素或者对象元素属性的最大值
     *
     * @param field
     * @return
     */
    public double max(String field) {
        if (array) {
            List list = asList();
            double maxNum = Double.MIN_VALUE;
            for (Object element : list) {
                Object val = element;
                if (element instanceof Map) {
                    if (field == null) return 0;
                    val = ObjectUtils.get(element, field);  // ((Map<?, ?>) element).get("field");
                }
                if (val instanceof Number) {
                    maxNum = Math.max(maxNum, ((Number) val).doubleValue());
                }
            }
            return maxNum;
        }
        return 0;
    }

    /**
     * 支持数组元素求元素或者对象元素属性的最小值
     *
     * @param field
     * @return
     */
    public double min(String field) {
        if (array) {
            List list = asList();
            double maxNum = Double.MAX_VALUE;
            for (Object element : list) {
                Object val = element;
                if (element instanceof Map) {
                    if (field == null) return 0;
                    val = ObjectUtils.get(element, field);  // ((Map<?, ?>) element).get("field");
                }
                if (val instanceof Number) {
                    maxNum = Math.min(maxNum, ((Number) val).doubleValue());
                }
            }
            return maxNum;
        }
        return 0;
    }

    /**
     * 支持数组元素求元素或者对象元素属性的平均值
     *
     * @param field
     * @return
     */
    public double avg(String field) {
        if (array) {
            List list = asList();
            double total = 0;
            if (list.size() == 0) return 0;
            for (Object element : list) {
                Object val = element;
                if (element instanceof Map) {
                    if (field == null) return 0;
                    val = ObjectUtils.get(element, field);  // ((Map<?, ?>) element).get("field");
                }
                if (val instanceof Number) {
                    total += ((Number) val).doubleValue();
                }
            }
            return total / list.size();
        }
        return 0;
    }

    public final double max() {
        return max(null);
    }

    public final double min() {
        return min(null);
    }

    public final double avg() {
        return avg(null);
    }

    public final boolean isArray() {
        return array;
    }

    public final boolean isObject() {
        return type == OBJECT;
    }

    public final boolean isNull() {
        return type == NULL;
    }

    public final boolean isNumber() {
        return type == NUMBER;
    }

    public final boolean isLeaf() {
        return leaf;
    }

    public final int getType() {
        return type;
    }

    public final int compareTo(JSONNode o) {
        Serializable value = this.value;
        Serializable o1 = o.value;
        if (value instanceof Number && o1 instanceof Number) {
            Double v1 = ((Number) value).doubleValue();
            Double v2 = ((Number) o1).doubleValue();
            return v1.compareTo(v2);
        }
        if (value instanceof String && o1 instanceof String) {
            return ((String) value).compareTo((String) o1);
        }
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

    private void writeTo(StringBuilder builder) {
        if (!this.changed) {
            writeSourceTo(builder);
            return;
        }
        builder.append(JSON.toJsonString(any()));
    }

    private static void writeStringTo(String leafValue, StringBuilder content) {
        int len = leafValue.length();
        int beginIndex = 0;
        for (int i = 0; i < len; ++i) {
            char ch = leafValue.charAt(i);
            String escapeStr;
            if ((ch > '"' && ch != '\\') || (escapeStr = JSONGeneral.ESCAPE_VALUES[ch]) == null) continue;
            int length = i - beginIndex;
            if (length > 0) {
                content.append(leafValue, beginIndex, i);
            }
            content.append(escapeStr);
            beginIndex = i + 1;
        }
        content.append(leafValue, beginIndex, len);
    }

    public final void setPathValue(String path, Serializable value) {
        JSONNode node = get(path);
        if (node != null /*&& node.leaf*/) {
            node.setValue(value);
        }
    }

    public final void setChildValue(String field, Serializable value) {
        setChildValue(field, value, false);
    }

    public final void setChildValue(String field, Serializable value, boolean createIfNotExist) {
        if (leaf) return;
        JSONNode node = getChild(field);
        if (node != null /*&& node.leaf*/) {
            node.setValue(value);
        } else {
            if (createIfNotExist) {
                newChildValue(field, value);
            }
        }
    }

    final void newChildValue(String field, Serializable value) {
        JSONNode newChild = new D(value, root);
        newChild.parent = this;
        if (array) {
            addElementNode(newChild);
        } else {
            addFieldValue(fieldValues, field, newChild);
        }
        handleChange();
    }

    public final void removeElementAt(int index) {
        if (!array) {
            throw new UnsupportedOperationException();
        }
        if (index >= elementSize || index < 0) {
            throw new IndexOutOfBoundsException(" length: " + elementSize + ", to remove index at " + index);
        }
        if (!completed) {
            completeNode(true, false);
        }
        int newLen = elementSize - 1;
        JSONNode[] newElementNodes = new JSONNode[newLen];
        System.arraycopy(elementValues, 0, newElementNodes, 0, index);
        System.arraycopy(elementValues, index + 1, newElementNodes, index, newLen - index);
        elementSize = newLen;
        handleChange();
    }

    public final JSONNode removeField(String field) {
        if (type != OBJECT) {
            throw new UnsupportedOperationException();
        }
        if (!completed) {
            completeNode(true, false);
        }
        JSONNode removeNode = fieldValues.remove(field);
        if (removeNode != null) {
            handleChange();
        }
        return removeNode;
    }

//    public final List collect(String childPath) {
//        return collect(childPath, String.class);
//    }
//
//    public final List collect(String childPath, Class typeClass) {
//        if (!array) {
//            throw new UnsupportedOperationException();
//        }
//        if (!this.completed) {
//            this.parseFull();
//        }
//        List result = new ArrayList();
//        for (int i = 0; i < elementSize; ++i) {
//            JSONNode jsonNode = elementValues[i];
//            result.add(jsonNode.getPathValue(childPath, typeClass));
//        }
//        return result;
//    }

    public final void setValue(Serializable value) {
        if (value == this.value) {
            return;
        }
        this.value = value;
        this.leaf = true;
        this.updateType();
        this.handleChange();
    }

    final void updateType() {
        if (value == null) {
            this.type = NULL;
        } else if (value instanceof Number) {
            this.type = NUMBER;
        } else if (value instanceof Boolean) {
            this.type = BOOLEAN;
        } else if (value instanceof String) {
            this.type = STRING;
            this.text = (String) value;
        } else {
            this.type = EXPAND;
        }
    }

    public final void clear() {
        if (array) {
            for (JSONNode ele : elementValues) {
                if (ele != null) {
                    ele.clear();
                }
            }
        } else {
            for (JSONNode ele : fieldValues.values()) {
                ele.clear();
            }
        }
        text = null;
        value = null;
        elementValues = null;
        fieldValues = null;
        clearSource();
    }

    JSONNode parseArrayAt(int index, boolean lazy, boolean createObj) {
        return null;
    }

    JSONNode parseValueNode(int beginIndex, char endChar, boolean lazy, boolean createObj) throws Exception {
        return null;
    }

    public String getText() {
        return null;
    }

    byte[] hexString2Bytes() {
        return new byte[0];
    }

    public String source() {
        return null;
    }

    void writeSourceTo(StringBuilder stringBuilder) {
    }

    protected void clearSource() {
    }

    final void handleChange() {
        if (parent != null) {
            parent.handleChange();
        }
        changed = true;
        any = null;
    }

    public void writeTo(Writer writer) {
        writeTo(writer, true);
    }

    public void writeTo(Writer writer, boolean close) {
        try {
            writer.write(toJsonString(true));
            writer.flush();
            if (close) {
                writer.close();
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public final String toJsonString() {
        return toJsonString(false);
    }

    public final String toJsonString(boolean rewrite) {
        if (rewrite) {
            return JSON.toJsonString(any());
        }
        if (!this.changed) {
            return this.source();
        }
        StringBuilder stringBuilder = new StringBuilder();
        writeTo(stringBuilder);
        return stringBuilder.toString();
    }
}
