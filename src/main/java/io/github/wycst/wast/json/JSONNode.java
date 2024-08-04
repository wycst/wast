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

import io.github.wycst.wast.common.reflect.ClassStructureWrapper;
import io.github.wycst.wast.common.reflect.GenericParameterizedType;
import io.github.wycst.wast.common.reflect.SetterInfo;
import io.github.wycst.wast.common.utils.Base64Utils;
import io.github.wycst.wast.common.utils.EnvUtils;
import io.github.wycst.wast.common.utils.ObjectUtils;
import io.github.wycst.wast.json.exceptions.JSONException;
import io.github.wycst.wast.json.options.ReadOption;

import java.io.Serializable;
import java.lang.reflect.Array;
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
 * @see JSON
 * @see JSONReader
 * @see JSONCharArrayWriter
 */
public abstract class JSONNode extends JSONGeneral implements Comparable<JSONNode> {

    final JSONNode root;
    final JSONNodeContext parseContext;
    JSONNode parent;
    final int beginIndex;
    final int endIndex;
    String text;
    int offset;
    boolean completed;

    Map<Serializable, JSONNode> fieldValues;
    JSONNode[] elementValues;
    int elementSize;

    // Type: 1 object; 2 array; 3. String; 4 number; 5 boolean; 6 null
    int type;
    public final static int OBJECT = 1;
    public final static int ARRAY = 2;
    public final static int STRING = 3;
    public final static int NUMBER = 4;
    public final static int BOOLEAN = 5;
    public final static int NULL = 6;
    private final boolean isArray;
    private final boolean leaf;
    Serializable leafValue;
    private boolean changed;

    static class RootContext {
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
        this.leafValue = rootContext.leafValue;
        this.leaf = type > 2;
        this.isArray = type == 2;
    }

    /***
     * Construct an array root node (local root) based on parsed attribute data
     *
     * @param elementValues
     * @param beginIndex
     * @param endIndex
     * @param parseContext
     */
    JSONNode(List<JSONNode> elementValues, int beginIndex, int endIndex, JSONNodeContext parseContext) {
        this.elementSize = elementValues.size();
        this.elementValues = elementValues.toArray(new JSONNode[elementSize]);
        this.completed = true;
        this.isArray = true;
        this.leaf = false;
        this.type = ARRAY;
        this.parseContext = parseContext;
        this.beginIndex = beginIndex;
        this.endIndex = endIndex;
        this.root = this;
    }

    /***
     * Construct object root nodes (local roots) based on parsed attribute data
     *
     * @param fieldValues
     * @param beginIndex
     * @param endIndex
     * @param parseContext
     */
    JSONNode(Map<Serializable, JSONNode> fieldValues, int beginIndex, int endIndex, JSONNodeContext parseContext) {
        this.fieldValues = fieldValues;
        this.completed = true;
        this.isArray = false;
        this.leaf = false;
        this.type = OBJECT;
        this.parseContext = parseContext;
        this.beginIndex = beginIndex;
        this.endIndex = endIndex;
        this.root = this;
    }

    /***
     * leaf node
     *  @param leafValue
     * @param parseContext
     * @param rootNode
     */
    JSONNode(Serializable leafValue, int beginIndex, int endIndex, int type, JSONNodeContext parseContext, JSONNode rootNode) {
        this.leafValue = leafValue;
        this.completed = true;
        this.isArray = false;
        this.leaf = true;
        this.parseContext = parseContext;
        this.root = rootNode;
        this.beginIndex = beginIndex;
        this.endIndex = endIndex;
        this.type = type;
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
        this.endIndex = endIndex;
        this.parseContext = parseContext;
        this.root = rootNode;
        this.type = type;
        this.leaf = type > 2;
        this.isArray = type == 2;
        this.offset = this.beginIndex;
    }

    static RootContext buildRootContext(char[] buf, int beginIndex, int endIndex) {
        char start = '\0';
        char end = '\0';
        while ((beginIndex < endIndex) && ((start = buf[beginIndex]) <= ' ')) {
            beginIndex++;
        }
        while ((endIndex > beginIndex) && ((end = buf[endIndex - 1]) <= ' ')) {
            endIndex--;
        }
        int type;
        Serializable leafValue = null;
        if (start == '{' && end == '}') {
            type = OBJECT;
        } else if (start == '[' && end == ']') {
            type = ARRAY;
        } else if (start == '"' && end == '"') {
            type = STRING;
        } else {
            int len = endIndex - beginIndex;
            switch (start) {
                case 't': {
                    if (len == 4) {
                        long unsafeValue = JSONUnsafe.getLong(buf, beginIndex);
                        if (unsafeValue == TRUE_LONG) {
                            type = BOOLEAN;
                            leafValue = true;
                            break;
                        }
                    }
                    throw new JSONException("Syntax error, unexpected input '" + new String(buf, beginIndex, len) + "', position " + beginIndex);
                }
                case 'f': {
                    if (len == 5) {
                        long unsafeValue = JSONUnsafe.getLong(buf, beginIndex + 1);
                        if (unsafeValue == ALSE_LONG) {
                            type = BOOLEAN;
                            leafValue = false;
                            break;
                        }
                    }
                    throw new JSONException("Syntax error, unexpected input '" + new String(buf, beginIndex, len) + "', position " + beginIndex);
                }
                case 'n': {
                    if (len == 4) {
                        long unsafeValue = JSONUnsafe.getLong(buf, beginIndex);
                        if (unsafeValue == NULL_LONG) {
                            type = NULL;
                            leafValue = null;
                            break;
                        }
                    }
                    throw new JSONException("Syntax error, unexpected input '" + new String(buf, beginIndex, len) + "', position " + beginIndex);
                }
                default: {
                    type = NUMBER;
                }
            }
        }
        return new RootContext(beginIndex, endIndex, type, leafValue);
    }

    static RootContext buildRootContext(byte[] buf, int beginIndex, int endIndex) {
        byte start = '\0';
        byte end = '\0';
        while ((beginIndex < endIndex) && ((start = buf[beginIndex]) <= ' ')) {
            beginIndex++;
        }
        while ((endIndex > beginIndex) && ((end = buf[endIndex - 1]) <= ' ')) {
            endIndex--;
        }
        int type;
        Serializable leafValue = null;
        if (start == '{' && end == '}') {
            type = OBJECT;
        } else if (start == '[' && end == ']') {
            type = ARRAY;
        } else if (start == '"' && end == '"') {
            type = STRING;
        } else {
            int len = endIndex - beginIndex;
            switch (start) {
                case 't': {
                    if (len == 4) {
                        long unsafeValue = JSONUnsafe.getInt(buf, beginIndex);
                        if (unsafeValue == TRUE_INT) {
                            type = BOOLEAN;
                            leafValue = true;
                            break;
                        }
                    }
                    throw new JSONException("Syntax error, unexpected input '" + new String(buf, beginIndex, len) + "', position " + beginIndex);
                }
                case 'f': {
                    if (len == 5) {
                        long unsafeValue = JSONUnsafe.getInt(buf, beginIndex + 1);
                        if (unsafeValue == ALSE_INT) {
                            type = BOOLEAN;
                            leafValue = false;
                            break;
                        }
                    }
                    throw new JSONException("Syntax error, unexpected input '" + new String(buf, beginIndex, len) + "', position " + beginIndex);
                }
                case 'n': {
                    if (len == 4) {
                        long unsafeValue = JSONUnsafe.getInt(buf, beginIndex);
                        if (unsafeValue == NULL_INT) {
                            type = NULL;
                            leafValue = null;
                            break;
                        }
                    }
                    throw new JSONException("Syntax error, unexpected input '" + new String(buf, beginIndex, len) + "', position " + beginIndex);
                }
                default: {
                    type = NUMBER;
                }
            }
        }
        return new RootContext(beginIndex, endIndex, type, leafValue);
    }

    static class CharsImpl extends JSONNode {

        CharSource charSource;
        // source
        char[] buf;

        CharsImpl(CharSource charSource, char[] buf, int beginIndex, int endIndex, JSONNodeContext parseContext) {
            super(buildRootContext(buf, beginIndex, endIndex), parseContext);
            this.charSource = charSource;
            this.buf = buf;
        }

        CharsImpl(CharSource charSource, List<JSONNode> elementValues, char[] buf, int beginIndex, int endIndex, JSONNodeContext parseContext) {
            super(elementValues, beginIndex, endIndex, parseContext);
            this.charSource = charSource;
            this.buf = buf;
        }

        CharsImpl(CharSource charSource, Map<Serializable, JSONNode> fieldValues, char[] buf, int beginIndex, int endIndex, JSONNodeContext parseContext) {
            super(fieldValues, beginIndex, endIndex, parseContext);
            this.charSource = charSource;
            this.buf = buf;
        }

        CharsImpl(CharSource charSource, Serializable leafValue, char[] buf, int beginIndex, int endIndex, int type, JSONNodeContext parseContext, JSONNode rootNode) {
            super(leafValue, beginIndex, endIndex, type, parseContext, rootNode);
            this.charSource = charSource;
            this.buf = buf;
        }

        public CharsImpl(CharSource charSource, char[] buf, int beginIndex, int endIndex, int type, JSONNodeContext parseContext, JSONNode rootNode) {
            super(beginIndex, endIndex, type, parseContext, rootNode);
            this.charSource = charSource;
            this.buf = buf;
        }

        @Override
        public String getText() {
            if (text == null) {
                if (type == STRING) {
                    text = (String) JSONTypeDeserializer.CHAR_SEQUENCE_STRING.deserializeString(charSource, buf, beginIndex, endIndex, '"', GenericParameterizedType.StringType, parseContext);
                    leafValue = text;
                } else {
                    return source();
                }
            }
            return text;
        }

        public String source() {
            if (leafValue != null && type != STRING) {
                return String.valueOf(this.leafValue);
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
            return hexString2Bytes(buf, beginIndex + 1, endIndex - beginIndex - 2);
        }

        @Override
        JSONNode parseFieldNode(String fieldName, boolean skipIfUnleaf) {
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
                            key = (String) JSONDefaultParser.parseKeyOfMap(buf, fieldKeyFrom, i, false);
                        } else {
                            throw new JSONException("Syntax error, the single quote symbol ' is not allowed at pos " + i);
                        }
                    } else {
                        if (parseContext.allowUnquotedFieldNames) {
                            while (i + 1 < endIndex && buf[++i] != ':') ;
                            empty = false;
                            key = String.valueOf(JSONDefaultParser.parseKeyOfMap(buf, fieldKeyFrom, i, true));
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

        JSONNode parseValueNode(int beginIndex, char endChar) throws Exception {
            JSONNode value;
            char ch = buf[beginIndex];
            switch (ch) {
                case '{': {
                    JSONTypeDeserializer.ANY.skip(charSource, buf, beginIndex, endIndex, '}', parseContext);
                    int eIndex = parseContext.endIndex;
                    value = new CharsImpl(charSource, buf, beginIndex, eIndex, OBJECT, parseContext, root);
                    break;
                }
                case '[': {
                    JSONTypeDeserializer.ANY.skip(charSource, buf, beginIndex, endIndex, ']', parseContext);
                    int eIndex = parseContext.endIndex;
                    value = new CharsImpl(charSource, buf, beginIndex, eIndex, ARRAY, parseContext, root);
                    break;
                }
                case '"': {
                    // 3 string
                    value = parseStringPathNode(charSource, buf, beginIndex, endIndex, false, parseContext, root);
                    break;
                }
                case 'n': {
                    // null
                    value = parseNullPathNode(charSource, buf, beginIndex, endIndex, parseContext, root);
                    break;
                }
                case 't': {
                    value = parseBoolTruePathNode(charSource, buf, beginIndex, endIndex, parseContext, root);
                    break;
                }
                case 'f': {
                    value = parseBoolFalsePathNode(charSource, buf, beginIndex, endIndex, parseContext, root);
                    break;
                }
                default: {
                    // number
                    value = parseNumberPathNode(charSource, buf, beginIndex, endIndex, endChar, parseContext, root);
                    break;
                }
            }
            value.parent = this;
            return value;
        }

        JSONNode parseElementNodeAt(int index) {
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
                    if (elementSize > 0) {
                        throw new JSONException("Syntax error, not allowed ',' followed by ']', pos " + i);
                    }
                    offset = i;
                    completed = true;
                    return null;
                }
                matchedIndex = index > -1 && index == elementSize;

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

        @Override
        protected void clearSource() {
            this.buf = null;
            this.charSource = null;
        }
    }

    static class BytesImpl extends JSONNode {

        CharSource charSource;
        // source
        byte[] buf;

        BytesImpl(CharSource charSource, byte[] buf, int beginIndex, int endIndex, JSONNodeContext parseContext) {
            super(buildRootContext(buf, beginIndex, endIndex), parseContext);
            this.charSource = charSource;
            this.buf = buf;
        }

        BytesImpl(CharSource charSource, List<JSONNode> elementValues, byte[] buf, int beginIndex, int endIndex, JSONNodeContext parseContext) {
            super(elementValues, beginIndex, endIndex, parseContext);
            this.charSource = charSource;
            this.buf = buf;
        }

        BytesImpl(CharSource charSource, Map<Serializable, JSONNode> fieldValues, byte[] buf, int beginIndex, int endIndex, JSONNodeContext parseContext) {
            super(fieldValues, beginIndex, endIndex, parseContext);
            this.charSource = charSource;
            this.buf = buf;
        }

        BytesImpl(CharSource charSource, Serializable leafValue, byte[] buf, int beginIndex, int endIndex, int type, JSONNodeContext parseContext, JSONNode rootNode) {
            super(leafValue, beginIndex, endIndex, type, parseContext, rootNode);
            this.charSource = charSource;
            this.buf = buf;
        }

        public BytesImpl(CharSource charSource, byte[] buf, int beginIndex, int endIndex, int type, JSONNodeContext parseContext, JSONNode rootNode) {
            super(beginIndex, endIndex, type, parseContext, rootNode);
            this.charSource = charSource;
            this.buf = buf;
        }

        @Override
        public String getText() {
            if (text == null) {
                if (type == STRING) {
                    text = (String) JSONTypeDeserializer.CHAR_SEQUENCE_STRING.deserializeString(charSource, buf, beginIndex, endIndex, '"', GenericParameterizedType.StringType, parseContext);
                    leafValue = text;
                } else {
                    return source();
                }
            }
            return text;
        }

        public String source() {
            if (leafValue != null && type != STRING) {
                return String.valueOf(this.leafValue);
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
            return hexString2Bytes(buf, beginIndex + 1, endIndex - beginIndex - 2);
        }

        @Override
        JSONNode parseFieldNode(String fieldName, boolean skipIfUnleaf) {
            if (completed) {
                return fieldValues.get(fieldName);
            }
            byte ch;
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
                    key = JSONTypeDeserializer.parseMapKeyByCache(buf, i, endIndex, '"', parseContext);
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
                            key = (String) JSONDefaultParser.parseKeyOfMap(buf, fieldKeyFrom, i, false);
                        } else {
                            throw new JSONException("Syntax error, the single quote symbol ' is not allowed at pos " + i);
                        }
                    } else {
                        if (parseContext.allowUnquotedFieldNames) {
                            while (i + 1 < endIndex && buf[++i] != ':') ;
                            empty = false;
                            key = String.valueOf(JSONDefaultParser.parseKeyOfMap(buf, fieldKeyFrom, i, true));
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
                            i = clearCommentAndWhiteSpaces(buf, i + 1, endIndex, parseContext);
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

        JSONNode parseValueNode(int beginIndex, char endChar) throws Exception {
            JSONNode value;
            byte ch = buf[beginIndex];
            switch (ch) {
                case '{': {
                    JSONTypeDeserializer.ANY.skip(charSource, buf, beginIndex, endIndex, (byte) '}', parseContext);
                    value = new BytesImpl(charSource, buf, beginIndex, parseContext.endIndex, OBJECT, parseContext, root);
                    break;
                }
                case '[': {
                    JSONTypeDeserializer.ANY.skip(charSource, buf, beginIndex, endIndex, (byte) ']', parseContext);
                    value = new BytesImpl(charSource, buf, beginIndex, parseContext.endIndex, ARRAY, parseContext, root);
                    break;
                }
                case '"': {
                    // 3 string
                    value = parseStringPathNode(charSource, buf, beginIndex, endIndex, false, parseContext, root);
                    break;
                }
                case 'n': {
                    // null
                    value = parseNullPathNode(charSource, buf, beginIndex, endIndex, parseContext, root);
                    break;
                }
                case 't': {
                    value = parseBoolTruePathNode(charSource, buf, beginIndex, endIndex, parseContext, root);
                    break;
                }
                case 'f': {
                    value = parseBoolFalsePathNode(charSource, buf, beginIndex, endIndex, parseContext, root);
                    break;
                }
                default: {
                    // number
                    value = parseNumberPathNode(charSource, buf, beginIndex, endIndex, (byte) endChar, parseContext, root);
                    break;
                }
            }
            value.parent = this;
            return value;
        }

        JSONNode parseElementNodeAt(int index) {
            byte ch;
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
                    if (elementSize > 0) {
                        throw new JSONException("Syntax error, not allowed ',' followed by ']', pos " + i);
                    }
                    offset = i;
                    completed = true;
                    return null;
                }
                matchedIndex = index > -1 && index == elementSize;

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
    public static JSONNode parse(String source, ReadOption... readOptions) {
        return parseInternal(source, null, false, readOptions);
    }

//    /**
//     * Return the complete JSON tree node
//     *
//     * @param source
//     * @param readOptions
//     * @return
//     */
//    public static JSONNode parse(String source, boolean reverseParseNode, ReadOption... readOptions) {
//        return parse(source, null, reverseParseNode, readOptions);
//    }

    /**
     * Return the complete JSON tree node
     *
     * @param source      json source
     * @param path        <p> 1. Access the root and split path through '/' off;
     *                    <p> 2. use [n] to access the array subscript;
     * @param readOptions
     * @return
     */
    public static JSONNode parse(String source, String path, ReadOption... readOptions) {
        return parseInternal(source, path, false, readOptions);
    }

    private static JSONNode parseInternal(String source, String path, boolean reverseParseNode, ReadOption... readOptions) {
        if (source == null)
            return null;
        source = source.trim();
        try {
            JSONNodeContext parseContext = new JSONNodeContext();
            JSONOptions.readOptions(readOptions, parseContext);
            parseContext.reverseParseNode = reverseParseNode;
            JSONNode root;
            if (EnvUtils.JDK_9_PLUS) {
                byte[] bytes = (byte[]) JSONUnsafe.getStringValue(source);
                if (bytes.length == source.length()) {
                    root = new BytesImpl(AsciiStringSource.of(source, bytes), bytes, 0, bytes.length, parseContext);
                } else {
                    char[] chars = source.toCharArray();
                    root = new CharsImpl(UTF16ByteArraySource.of(source), chars, 0, chars.length, parseContext);
                }
            } else {
                char[] chars = (char[]) JSONUnsafe.getStringValue(source);
                root = new CharsImpl(null, chars, 0, chars.length, parseContext);
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


    /***
     * Generate JSON root node (local root) based on specified path
     *
     * @param source
     * @param path
     * @param readOptions
     * @return
     */
    public static JSONNode from(String source, String path, ReadOption... readOptions) {
        return from(source, path, false, readOptions);
    }

    /**
     * Generate JSON root node (local root) based on specified path
     *
     * @param source
     * @param path
     * @param lazy
     * @param readOptions
     * @return
     */
    public static JSONNode from(String source, String path, boolean lazy, ReadOption... readOptions) {
        if (EnvUtils.JDK_9_PLUS) {
            byte[] bytes = (byte[]) JSONUnsafe.getStringValue(source.toString());
            if (bytes.length == source.length()) {
                return parseInternal(AsciiStringSource.of(source, bytes), bytes, path, lazy, readOptions);
            } else {
                char[] chars = source.toCharArray();
                return parseInternal(UTF16ByteArraySource.of(source), chars, path, lazy, readOptions);
            }
        } else {
            return parseInternal(null, (char[]) JSONUnsafe.getStringValue(source.toString()), path, lazy, readOptions);
        }
    }

    /**
     * Generate JSON root node (local root) based on specified path
     *
     * @param buf
     * @param path
     * @param readOptions
     * @return
     */
    public static JSONNode from(char[] buf, String path, ReadOption... readOptions) {
        return parseInternal(null, buf, path, false, readOptions);
    }

    /***
     * 根据指定path生成json根节点(局部根)
     *
     * @param charSource
     * @param buf
     * @param path
     * @param lazy
     * @param readOptions
     * @return
     */
    static JSONNode parseInternal(CharSource charSource, char[] buf, String path, boolean lazy, ReadOption... readOptions) {
        JSONNodeContext parseContext = new JSONNodeContext();
        JSONOptions.readOptions(readOptions, parseContext);
        parseContext.lazy = lazy;
        int toIndex = buf.length;
        if (path == null || (path = path.trim()).length() == 0) {
            return new CharsImpl(charSource, buf, 0, toIndex, parseContext);
        }
        if (!path.startsWith("/")) {
            path = "/" + path;
        }
        return parseNode(charSource, buf, path, false, parseContext);
    }

    static JSONNode parseInternal(CharSource charSource, byte[] buf, String path, boolean lazy, ReadOption... readOptions) {
        JSONNodeContext parseContext = new JSONNodeContext();
        JSONOptions.readOptions(readOptions, parseContext);
        parseContext.lazy = lazy;
        int toIndex = buf.length;
        if (path == null || (path = path.trim()).length() == 0) {
            return new BytesImpl(charSource, buf, 0, toIndex, parseContext);
        }
        if (!path.startsWith("/")) {
            path = "/" + path;
        }
        return parseNode(charSource, buf, path, false, parseContext);
    }

    /**
     * Extract path value list
     *
     * @param json
     * @param path
     * @param readOptions
     * @return
     */
    public static List extract(String json, String path, ReadOption... readOptions) {
        if (EnvUtils.JDK_9_PLUS) {
            byte[] bytes = (byte[]) JSONUnsafe.getStringValue(json.toString());
            if (bytes.length == json.length()) {
                return extractInternal(AsciiStringSource.of(json, bytes), bytes, path, readOptions);
            } else {
                // utf16
                char[] chars = json.toCharArray();
                return extractInternal(UTF16ByteArraySource.of(json), chars, path, readOptions);
            }
        } else {
            return extractInternal(null, (char[]) JSONUnsafe.getStringValue(json.toString()), path, readOptions);
        }
    }

    /**
     * Extract path value list
     *
     * @param buf
     * @param path
     * @param readOptions
     * @return
     */
    public static List extract(char[] buf, String path, ReadOption... readOptions) {
        return extractInternal(null, buf, path, readOptions);
    }

    static List extractInternal(CharSource charSource, char[] buf, String path, ReadOption... readOptions) {
        JSONNodeContext parseContext = new JSONNodeContext();
        JSONOptions.readOptions(readOptions, parseContext);
        parseContext.extract = true;
        if (!path.startsWith("/")) {
            path = "/" + path;
        }
        parseNode(charSource, buf, path, false, parseContext);
        return parseContext.extractValues;
    }

    static List extractInternal(CharSource charSource, byte[] buf, String path, ReadOption... readOptions) {
        JSONNodeContext parseContext = new JSONNodeContext();
        JSONOptions.readOptions(readOptions, parseContext);
        parseContext.extract = true;
        if (!path.startsWith("/")) {
            path = "/" + path;
        }
        parseNode(charSource, buf, path, false, parseContext);
        return parseContext.extractValues;
    }

    private static JSONNode parseNode(CharSource charSource, char[] buf, String path, boolean skipValue, JSONNodeContext parseContext) {
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
                    result = parseObjectPathNode(charSource, buf, fromIndex, toIndex, path, 0, skipValue, true, parseContext);
                    break;
                case '[':
                    result = parseArrayPathNode(charSource, buf, fromIndex, toIndex, path, 0, skipValue, true, parseContext);
                    break;
                case '"':
                    result = parseStringPathNode(charSource, buf, fromIndex, toIndex, skipValue, parseContext, null);
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

    private static JSONNode parseNode(CharSource charSource, byte[] buf, String path, boolean skipValue, JSONNodeContext parseContext) {
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

            boolean allowComment = parseContext.allowComment;
            if (allowComment && beginByte == '/') {
                /** 去除声明在头部的注释*/
                fromIndex = clearCommentAndWhiteSpaces(buf, fromIndex + 1, toIndex, parseContext);
                beginByte = buf[fromIndex];
            }

            // ignore null / true / false / number of the root
            switch (beginByte) {
                case '{':
                    result = parseObjectPathNode(charSource, buf, fromIndex, toIndex, path, 0, skipValue, true, parseContext);
                    break;
                case '[':
                    result = parseArrayPathNode(charSource, buf, fromIndex, toIndex, path, 0, skipValue, true, parseContext);
                    break;
                case '"':
                    result = parseStringPathNode(charSource, buf, fromIndex, toIndex, skipValue, parseContext, null);
                    break;
                default:
                    throw new UnsupportedOperationException("Unsupported for begin character with '" + beginByte + "'");
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

    private static JSONNode parseObjectPathNode(CharSource charSource, char[] buf, int fromIndex, int toIndex, String path, int beginPathIndex, boolean skipValue, boolean returnIfMatched, JSONNodeContext jsonParseContext) throws Exception {
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
                    key = String.valueOf(JSONDefaultParser.parseKeyOfMap(buf, fieldKeyFrom, fieldKeyTo, isUnquotedFieldName));
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
                        value = parseObjectPathNode(charSource, buf, i, toIndex, path, nextPathIndex, isSkipValue, returnIfMatched, jsonParseContext);
                        if (lazy) {
                            value = new CharsImpl(null, buf, i, jsonParseContext.endIndex + 1, jsonParseContext);
                        }
                        i = jsonParseContext.endIndex;
                        break;
                    }
                    case '[': {
                        if (lazy) {
                            isSkipValue = true;
                        }
                        // 2 [ array
                        value = parseArrayPathNode(charSource, buf, i, toIndex, path, nextPathIndex, isSkipValue, returnIfMatched, jsonParseContext);
                        if (lazy) {
                            value = new CharsImpl(null, buf, i, jsonParseContext.endIndex + 1, jsonParseContext);
                        }
                        i = jsonParseContext.endIndex;
                        break;
                    }
                    case '"': {
                        // 3 string
                        isLeafValue = true;
                        value = parseStringPathNode(charSource, buf, i, toIndex, isSkipValue, jsonParseContext, null);
                        i = jsonParseContext.endIndex;
                        break;
                    }
                    case 'n': {
                        // null
                        isLeafValue = true;
                        value = parseNullPathNode(charSource, buf, i, toIndex, jsonParseContext, null);
                        i = jsonParseContext.endIndex;
                        break;
                    }
                    case 't': {
                        isLeafValue = true;
                        value = parseBoolTruePathNode(charSource, buf, i, toIndex, jsonParseContext, null);
                        i = jsonParseContext.endIndex;
                        break;
                    }
                    case 'f': {
                        isLeafValue = true;
                        value = parseBoolFalsePathNode(charSource, buf, i, toIndex, jsonParseContext, null);
                        i = jsonParseContext.endIndex;
                        break;
                    }
                    default: {
                        // number
                        isLeafValue = true;
                        value = parseNumberPathNode(charSource, buf, i, toIndex, '}', jsonParseContext, null);
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
                            return new CharsImpl(null, fieldValues, buf, fromIndex, i + 1, jsonParseContext);
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

    private static JSONNode parseObjectPathNode(CharSource charSource, byte[] buf, int fromIndex, int toIndex, String path, int beginPathIndex, boolean skipValue, boolean returnIfMatched, JSONNodeContext jsonParseContext) throws Exception {
        int beginIndex = fromIndex + 1;
        byte b;
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
            while ((b = buf[i]) <= ' ') {
                ++i;
            }
            if (jsonParseContext.allowComment) {
                if (b == '/') {
                    b = buf[i = clearCommentAndWhiteSpaces(buf, i + 1, toIndex, jsonParseContext)];
                }
            }
            if (b == '"') {
                key = JSONTypeDeserializer.parseMapKeyByCache(buf, i, toIndex, '"', jsonParseContext);
                i = jsonParseContext.endIndex;
                empty = false;
                ++i;
            } else {
                if (b == '}') {
                    if (!empty) {
                        throw new JSONException("Syntax error, the closing symbol '}' is not allowed at pos " + i);
                    }
                    jsonParseContext.endIndex = i;
                    return null;
                }
                if (b == '\'') {
                    if (jsonParseContext.allowSingleQuotes) {
                        key = JSONTypeDeserializer.parseMapKeyByCache(buf, i, toIndex, '\'', jsonParseContext);
                        i = jsonParseContext.endIndex;
                        empty = false;
                        ++i;
                    } else {
                        throw new JSONException("Syntax error, the single quote symbol ' is not allowed at pos " + i);
                    }
                } else {
                    if (jsonParseContext.allowUnquotedFieldNames) {
                        int begin = i;
                        while (i + 1 < toIndex && buf[++i] != ':') ;
                        empty = false;
                        key = String.valueOf(JSONDefaultParser.parseKeyOfMap(buf, begin, i, true));
                    }
                }
            }
            while ((b = buf[i]) <= ' ') {
                ++i;
            }
            if (allowComment) {
                if (b == '/') {
                    b = buf[i = clearCommentAndWhiteSpaces(buf, i + 1, toIndex, jsonParseContext)];
                }
            }
            if (b == ':') {
                if (!skipValue) {
                    if (!isLastPathLevel) {
                        matched = stringEqual(path, beginPathIndex + 1, (nextPathIndex == -1 ? path.length() : nextPathIndex) - beginPathIndex - 1, key, 0, key.length());
                    }
                }
                while ((b = buf[++i]) <= ' ') ;
                if (allowComment) {
                    if (b == '/') {
                        b = buf[i = clearCommentAndWhiteSpaces(buf, i + 1, toIndex, jsonParseContext)];
                    }
                }
                boolean isLeafValue = false;
                JSONNode value;
                boolean isSkipValue = isLastPathLevel ? false : !matched || skipValue;
                boolean lazy = lazyParseLastNode || nextPathIndex == -1;
                switch (b) {
                    case '{': {
                        if (lazy) {
                            isSkipValue = true;
                        }
                        value = parseObjectPathNode(charSource, buf, i, toIndex, path, nextPathIndex, isSkipValue, returnIfMatched, jsonParseContext);
                        if (lazy) {
                            value = new BytesImpl(charSource, buf, i, jsonParseContext.endIndex + 1, jsonParseContext);
                        }
                        i = jsonParseContext.endIndex;
                        break;
                    }
                    case '[': {
                        if (lazy) {
                            isSkipValue = true;
                        }
                        // 2 [ array
                        value = parseArrayPathNode(charSource, buf, i, toIndex, path, nextPathIndex, isSkipValue, returnIfMatched, jsonParseContext);
                        if (lazy) {
                            value = new BytesImpl(charSource, buf, i, jsonParseContext.endIndex + 1, jsonParseContext);
                        }
                        i = jsonParseContext.endIndex;
                        break;
                    }
                    case '"': {
                        // 3 string
                        isLeafValue = true;
                        value = parseStringPathNode(charSource, buf, i, toIndex, isSkipValue, jsonParseContext, null);
                        i = jsonParseContext.endIndex;
                        break;
                    }
                    case 'n': {
                        // null
                        isLeafValue = true;
                        value = parseNullPathNode(charSource, buf, i, toIndex, jsonParseContext, null);
                        i = jsonParseContext.endIndex;
                        break;
                    }
                    case 't': {
                        isLeafValue = true;
                        value = parseBoolTruePathNode(charSource, buf, i, toIndex, jsonParseContext, null);
                        i = jsonParseContext.endIndex;
                        break;
                    }
                    case 'f': {
                        isLeafValue = true;
                        value = parseBoolFalsePathNode(charSource, buf, i, toIndex, jsonParseContext, null);
                        i = jsonParseContext.endIndex;
                        break;
                    }
                    default: {
                        // number
                        isLeafValue = true;
                        value = parseNumberPathNode(charSource, buf, i, toIndex, (byte) '}', jsonParseContext, null);
                        i = jsonParseContext.endIndex;
                        break;
                    }
                }
                while ((b = buf[++i]) <= ' ') ;
                if (allowComment) {
                    // clearComment and append whiteSpaces
                    if (b == '/') {
                        b = buf[i = clearCommentAndWhiteSpaces(buf, i + 1, toIndex, jsonParseContext)];
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
                boolean isClosingSymbol = b == '}';
                if (b == ',' || isClosingSymbol) {
                    if (fieldValues != null) {
                        fieldValues.put(key, value);
                    }
                    if (isClosingSymbol) {
                        jsonParseContext.endIndex = i;
                        if (isLastPathLevel) {
                            return new BytesImpl(null, fieldValues, buf, fromIndex, i + 1, jsonParseContext);
                        }
                        return null;
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

    private static boolean stringEqual(String path, int s1, int len1, String key, int s2, int len2) {
        if (len1 != len2) return false;
        for (int i = 0; i < len1; ++i) {
            if (path.charAt(s1 + i) != key.charAt(s2 + i)) return false;
        }
        return true;
    }

    private static JSONNode parseArrayPathNode(CharSource charSource, char[] buf, int fromIndex, int toIndex, String path, int beginPathIndex, boolean skipValue, boolean returnIfMatched, JSONNodeContext jsonParseContext) throws Exception {
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
                // 1 以'['开始，以']'结束或者数字串
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
                            targetElementIndex = readArrayIndex(path, numBeginIndex, numEndIndex - numBeginIndex);
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
                    value = parseObjectPathNode(charSource, buf, i, toIndex, path, nextPathIndex, isSkipValue, returnValueIfMathched, jsonParseContext);
                    break;
                }
                case '[': {
                    // 2 [ array
                    value = parseArrayPathNode(charSource, buf, i, toIndex, path, nextPathIndex, isSkipValue, returnValueIfMathched, jsonParseContext);
                    break;
                }
                case '"': {
                    isLeafValue = true;
                    // 3 string
                    value = parseStringPathNode(charSource, buf, i, toIndex, isSkipValue, jsonParseContext, null);
                    break;
                }
                case 'n': {
                    // null
                    isLeafValue = true;
                    value = parseNullPathNode(charSource, buf, i, toIndex, jsonParseContext, null);
                    break;
                }
                case 't': {
                    isLeafValue = true;
                    value = parseBoolTruePathNode(charSource, buf, i, toIndex, jsonParseContext, null);
                    break;
                }
                case 'f': {
                    isLeafValue = true;
                    value = parseBoolFalsePathNode(charSource, buf, i, toIndex, jsonParseContext, null);
                    break;
                }
                default: {
                    // number
                    isLeafValue = true;
                    value = parseNumberPathNode(charSource, buf, i, toIndex, ']', jsonParseContext, null);
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
                        return new CharsImpl(null, elementValues, buf, fromIndex, i + 1, jsonParseContext);
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
                        return new CharsImpl(null, elementValues, buf, fromIndex, i + 1, jsonParseContext);
                    }
                    return null;
                }
            } else {
                throw new JSONException("Syntax error, unexpected '" + ch + "', position " + i + ", Missing ',' or '}'");
            }
        }
        throw new JSONException("Syntax error, the closing symbol ']' is not found ");
    }

    private static JSONNode parseArrayPathNode(CharSource charSource, byte[] buf, int fromIndex, int toIndex, String path, int beginPathIndex, boolean skipValue, boolean returnIfMatched, JSONNodeContext jsonParseContext) throws Exception {
        int beginIndex = fromIndex + 1;
        byte ch;
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
                            targetElementIndex = readArrayIndex(path, numBeginIndex, numEndIndex - numBeginIndex);
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
                    value = parseObjectPathNode(charSource, buf, i, toIndex, path, nextPathIndex, isSkipValue, returnValueIfMathched, jsonParseContext);
                    break;
                }
                case '[': {
                    // 2 [ array
                    value = parseArrayPathNode(charSource, buf, i, toIndex, path, nextPathIndex, isSkipValue, returnValueIfMathched, jsonParseContext);
                    break;
                }
                case '"': {
                    isLeafValue = true;
                    // 3 string
                    value = parseStringPathNode(charSource, buf, i, toIndex, isSkipValue, jsonParseContext, null);
                    break;
                }
                case 'n': {
                    // null
                    isLeafValue = true;
                    value = parseNullPathNode(charSource, buf, i, toIndex, jsonParseContext, null);
                    break;
                }
                case 't': {
                    isLeafValue = true;
                    value = parseBoolTruePathNode(charSource, buf, i, toIndex, jsonParseContext, null);
                    break;
                }
                case 'f': {
                    isLeafValue = true;
                    value = parseBoolFalsePathNode(charSource, buf, i, toIndex, jsonParseContext, null);
                    break;
                }
                default: {
                    // number
                    isLeafValue = true;
                    value = parseNumberPathNode(charSource, buf, i, toIndex, (byte) ']', jsonParseContext, null);
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
                        return new BytesImpl(null, elementValues, buf, fromIndex, i + 1, jsonParseContext);
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
                        return new BytesImpl(null, elementValues, buf, fromIndex, i + 1, jsonParseContext);
                    }
                    return null;
                }
            } else {
                throw new JSONException("Syntax error, unexpected '" + ch + "', position " + i + ", Missing ',' or '}'");
            }
        }
        throw new JSONException("Syntax error, the closing symbol ']' is not found ");
    }

    private static JSONNode parseStringPathNode(CharSource charSource, char[] buf, int fromIndex, int toIndex, boolean skipValue, JSONNodeContext jsonParseContext, JSONNode rootNode) throws Exception {
        if (skipValue) {
            JSONTypeDeserializer.CHAR_SEQUENCE_STRING.skip(charSource, buf, fromIndex, '"', jsonParseContext);
            return null;
        }
        String value = (String) JSONTypeDeserializer.CHAR_SEQUENCE_STRING.deserializeString(charSource, buf, fromIndex, toIndex, '"', GenericParameterizedType.StringType, jsonParseContext);
        int endIndex = jsonParseContext.endIndex;
        return new CharsImpl(charSource, value, buf, fromIndex, endIndex + 1, STRING, jsonParseContext, rootNode);
    }

    private static JSONNode parseStringPathNode(CharSource charSource, byte[] buf, int fromIndex, int toIndex, boolean skipValue, JSONNodeContext jsonParseContext, JSONNode rootNode) throws Exception {
        if (skipValue) {
            JSONTypeDeserializer.CHAR_SEQUENCE_STRING.skip(charSource, buf, fromIndex, '"', jsonParseContext);
            return null;
        }
        String value = (String) JSONTypeDeserializer.CHAR_SEQUENCE_STRING.deserializeString(charSource, buf, fromIndex, toIndex, '"', GenericParameterizedType.StringType, jsonParseContext);
        int endIndex = jsonParseContext.endIndex;
        return new BytesImpl(charSource, value, buf, fromIndex, endIndex + 1, STRING, jsonParseContext, rootNode);
    }

    private static JSONNode parseNullPathNode(CharSource charSource, char[] buf, int fromIndex, int toIndex, JSONNodeContext jsonParseContext, JSONNode rootNode) throws Exception {
        JSONTypeDeserializer.NULL.deserialize(charSource, buf, fromIndex, toIndex, null, null, jsonParseContext);
        int endIndex = jsonParseContext.endIndex;
        return new CharsImpl(charSource, null, buf, fromIndex, endIndex + 1, NULL, jsonParseContext, rootNode);
    }

    private static JSONNode parseNullPathNode(CharSource charSource, byte[] buf, int fromIndex, int toIndex, JSONNodeContext jsonParseContext, JSONNode rootNode) throws Exception {
        JSONTypeDeserializer.NULL.deserialize(charSource, buf, fromIndex, toIndex, null, null, jsonParseContext);
        int endIndex = jsonParseContext.endIndex;
        return new BytesImpl(charSource, null, buf, fromIndex, endIndex + 1, NULL, jsonParseContext, rootNode);
    }

    private static JSONNode parseBoolTruePathNode(CharSource charSource, char[] buf, int fromIndex, int toIndex, JSONNodeContext jsonParseContext, JSONNode rootNode) throws Exception {
        JSONTypeDeserializer.parseTrue(buf, fromIndex, toIndex, jsonParseContext);
        int endIndex = jsonParseContext.endIndex;
        return new CharsImpl(charSource, true, buf, fromIndex, endIndex + 1, BOOLEAN, jsonParseContext, rootNode);
    }

    private static JSONNode parseBoolTruePathNode(CharSource charSource, byte[] buf, int fromIndex, int toIndex, JSONNodeContext jsonParseContext, JSONNode rootNode) throws Exception {
        JSONTypeDeserializer.parseTrue(buf, fromIndex, toIndex, jsonParseContext);
        int endIndex = jsonParseContext.endIndex;
        return new BytesImpl(charSource, true, buf, fromIndex, endIndex + 1, BOOLEAN, jsonParseContext, rootNode);
    }

    private static JSONNode parseBoolFalsePathNode(CharSource charSource, char[] buf, int fromIndex, int toIndex, JSONNodeContext jsonParseContext, JSONNode rootNode) throws Exception {
        JSONTypeDeserializer.parseFalse(buf, fromIndex, toIndex, jsonParseContext);
        int endIndex = jsonParseContext.endIndex;
        return new CharsImpl(charSource, false, buf, fromIndex, endIndex + 1, BOOLEAN, jsonParseContext, rootNode);
    }

    private static JSONNode parseBoolFalsePathNode(CharSource charSource, byte[] buf, int fromIndex, int toIndex, JSONNodeContext jsonParseContext, JSONNode rootNode) throws Exception {
        JSONTypeDeserializer.parseFalse(buf, fromIndex, toIndex, jsonParseContext);
        int endIndex = jsonParseContext.endIndex;
        return new BytesImpl(charSource, false, buf, fromIndex, endIndex + 1, BOOLEAN, jsonParseContext, rootNode);
    }

    private static JSONNode parseNumberPathNode(CharSource charSource, char[] buf, int fromIndex, int toIndex, char endToken, JSONNodeContext jsonParseContext, JSONNode rootNode) throws Exception {
        Number value = (Number) JSONTypeDeserializer.NUMBER.deserialize(charSource, buf, fromIndex, toIndex, GenericParameterizedType.AnyType, null, endToken, jsonParseContext);
        int endIndex = jsonParseContext.endIndex;
        return new CharsImpl(charSource, value, buf, fromIndex, endIndex + 1, NUMBER, jsonParseContext, rootNode);
    }

    private static JSONNode parseNumberPathNode(CharSource charSource, byte[] buf, int fromIndex, int toIndex, byte endToken, JSONNodeContext jsonParseContext, JSONNode rootNode) throws Exception {
        Number value = (Number) JSONTypeDeserializer.NUMBER.deserialize(charSource, buf, fromIndex, toIndex, GenericParameterizedType.AnyType, null, endToken, jsonParseContext);
        int endIndex = jsonParseContext.endIndex;
        return new BytesImpl(charSource, value, buf, fromIndex, endIndex + 1, NUMBER, jsonParseContext, rootNode);
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
     * 获取指定路径的对象
     * <p>如果path为空将返回当前JSONNode节点对象 </p>
     * <p>如果/开头表示从根节点开始查找 </p>
     * <p>数组元素使用[n]</p>
     *
     * @param childPath 查找路径，以/作为级别分割线
     * @return
     */
    public final JSONNode get(String childPath) {
        return get(childPath, 0, childPath.length());
    }

    private JSONNode get(String path, int beginIndex, int endIndex) {
        if (path == null || (endIndex - beginIndex) == 0)
            return this;
        char beginChar = path.charAt(beginIndex);
        if (beginChar == '/') {
            return root.get(path, beginIndex + 1, endIndex);
        }
        if (leaf) {
            return null;
        }
        int splitIndex = path.indexOf('/', beginIndex + 1);
//        long hashValue = beginChar > 0xFF ? (ESCAPE << 16) | beginChar : (ESCAPE << 8) | beginChar;
//        char ch;
//        for (int i = beginIndex + 1; i < endIndex; ++i) {
//            if ((ch = path.charAt(i)) == '/') {
//                splitIndex = i;
//                break;
//            }
//            if (ch > 0xFF) {
//                hashValue = hashValue << 16 | ch;
//            } else {
//                hashValue = hashValue << 8 | ch;
//            }
//        }
        if (splitIndex == -1) {
            return getPathNode(path, beginIndex, endIndex - beginIndex/*, hashValue*/);
        } else {
            JSONNode childNode = getPathNode(path, beginIndex, splitIndex - beginIndex/*, hashValue*/);
            if (childNode != null) {
                return childNode.get(path, splitIndex + 1, endIndex);
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
        if (isArray) {
            throw new UnsupportedOperationException();
        }
        if (!completed) {
            parseFullNode();
        }
        return fieldValues.keySet();
    }

    private JSONNode getPathNode(String buf, int offset, int len) {
        if (isArray) {
            char ch = buf.charAt(offset);
            int digit = digitDecimal(ch);
            int index = -1;
            try {
                if (digit > -1) {
                    index = readArrayIndex(buf, offset, len);
                } else {
                    char endChar = buf.charAt(offset + len - 1);
                    if (ch == '[' && endChar == ']') {
                        index = readArrayIndex(buf, offset + 1, len - 2);
                    }
                }
            } catch (Throwable throwable) {
                throw new IllegalArgumentException("offset " + offset + ", key '" + buf.substring(offset, offset + len) + " is mismatch array index ");
            }
            if (index > -1) {
                return getElementAt(index);
            } else {
                throw new IllegalArgumentException("offset " + offset + ", key '" + buf.substring(offset, offset + len) + " is mismatch array index ");
            }
        } else {
            // String field1 = getCacheKey(buf, offset, len, hashCode);
            String field = buf.substring(offset, offset + len);
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
            int index, length = field.length();
            try {
                if (field.charAt(0) == '[' && field.charAt(length - 1) == ']') {
                    index = readArrayIndex(field, 1, length - 2);
                } else {
                    index = readArrayIndex(field, 0, length);
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

    protected static int readArrayIndex(String buf, int offset, int len) {
        if (len == 0) return -1;
        int value = 0;
        char ch;
        for (int i = offset, n = i + len; i < n; ++i) {
            ch = buf.charAt(i);
            int d = digitDecimal(ch);
            if (d < 0) {
                if (ch == ' ') continue;
                throw new NumberFormatException("For input string: \"" + buf.substring(offset, offset + len) + "\"");
            }
            value = value * 10 + d;
        }
        return value;
    }

    abstract JSONNode parseFieldNode(String fieldName, boolean skipIfUnleaf);

    abstract JSONNode parseElementNodeAt(int index);

    final void addElementNode(JSONNode value) {
        if (elementValues == null) {
            elementValues = new JSONNode[16];
        } else {
            // add to list
            if (this.elementSize == elementValues.length) {
                JSONNode[] tmp = elementValues;
                elementValues = new JSONNode[this.elementSize << 1];
                System.arraycopy(tmp, 0, elementValues, 0, tmp.length);
            }
        }
        elementValues[elementSize++] = value;
    }

    abstract JSONNode parseValueNode(int beginIndex, char endChar) throws Exception;

    public final JSONNode getElementAt(int index) {
        if (!isArray) {
            throw new UnsupportedOperationException();
        }
        if (completed || elementSize > index) {
            return elementValues[index];
        }
        return parseElementNodeAt(index);
    }

    public final int getElementCount() {
        if (isArray) {
            if (completed) {
                return elementSize;
            }
            parseElementNodeAt(-1);
            return elementSize;
        }
        return -1;
    }

    public final JSONNode parent() {
        return this.parent;
    }

    public final <E> E getPathValue(String childPath, Class<E> clazz) {
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

    public final <E> E getChildValue(String name, Class<E> eClass) {
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

    public abstract String getText();

    public final String getStringValue() {
        return getValue(String.class);
    }

    public final Serializable getValue() {
        if (this.leafValue != null) return this.leafValue;
        return getValue(null);
    }

    abstract byte[] hexString2Bytes();

    public final <E> E getValue(Class<E> eClass) {
        if (leaf) {
            if (eClass != null && eClass.isInstance(leafValue)) {
                return (E) leafValue;
            }
            if (type == NULL) {
                return null;
            }
            if (eClass == null) {
                return (E) source();
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
                    return (E) Enum.valueOf(enumCls, getText());
                } else {
                    throw new JSONException("source [" + source() + "] cannot convert to Enum '" + eClass + "");
                }
            }
            Object result = ObjectUtils.toType(source(), eClass);
            if (result != null) return (E) result;
            try {
                JSONTypeDeserializer typeDeserializer = JSONTypeDeserializer.getTypeDeserializer(eClass);
                if (typeDeserializer != null) {
                    return (E) typeDeserializer.valueOf(leafValue != null ? String.valueOf(leafValue) : source(), eClass);
                }
            } catch (Throwable throwable) {
            }
            return null;
        } else {
            if (isArray) {
                if (eClass == String.class) {
                    return (E) getText();
                }
                throw new UnsupportedOperationException("Please use toList or toArray method instead !");
            }
            if (eClass != null) {
                if (eClass.isEnum() || eClass.isPrimitive() || eClass.isArray()) {
                    throw new UnsupportedOperationException("Type is Unsupported for '" + eClass + "'");
                }
            }
            return toBean(eClass);
        }
    }

    public final <E> Map<?, E> toMap(Class<? extends Map> eClass, Class<E> valueCls) {
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
            throw new UnsupportedOperationException(" NodeValue is a leaf value, please call method getValue width Java basic type param instead !");
        }
        Object instance;
        boolean isMapInstance;
        ClassStructureWrapper structureWrapper = null;
        if (eClass == null || eClass == Map.class || eClass == LinkedHashMap.class) {
            instance = new LinkedHashMap();
            isMapInstance = true;
        } else {
            structureWrapper = ClassStructureWrapper.get(eClass);
            isMapInstance = structureWrapper.isAssignableFromMap();
            try {
                if (!isMapInstance) {
                    instance = structureWrapper.newInstance();
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
                SetterInfo setterInfo = structureWrapper.getSetterInfo(fieldName);
                if (setterInfo != null) {
                    Class<?> parameterType = setterInfo.getParameterType();
                    GenericParameterizedType parameterizedType = setterInfo.getGenericParameterizedType();
                    Object value;
                    if (childNode.isArray) {
                        value = childNode.toCollection(parameterizedType.getActualType(), parameterizedType.getValueType().getActualType());
                    } else {
                        value = childNode.getValue(parameterType);
                    }
                    JSON_SECURE_TRUSTED_ACCESS.set(setterInfo, instance, value); // setterInfo.invoke(instance, value);
                }
            }
        }
        return (E) instance;
    }

    public final <E> List<E> toList(Class<E> entityClass) {
        return (List<E>) toCollection(ArrayList.class, entityClass);
    }

    public final <E> E[] toArray(Class<E> entityClass) {
        return (E[]) toCollection(Object[].class, entityClass);
    }

    public final Object toCollection(Class<?> collectionCls, Class<?> entityClass) {
        collectionCls.getClass();
        if (!completed) {
            parseFullNode();
        }
        Class target = entityClass;
        Collection collection = null;
        boolean isArrayCls = collectionCls.isArray(), primitive = entityClass.isPrimitive();
        Object arrayObj = null;
        if (isArrayCls) {
            arrayObj = Array.newInstance(entityClass, elementSize); // primitive ? Array.newInstance(entityClass, elementSize) : new Object[elementSize];
        } else {
            collection = createCollectionInstance(collectionCls);
        }
        for (int i = 0; i < elementSize; ++i) {
            JSONNode node = elementValues[i];
            Object result;
            if (node.isArray) {
                result = node.toList(null);
            } else {
                result = node.getValue(target);
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

    final void parseFullNode() {
        if (isArray) {
            parseElementNodeAt(-1);
        } else {
            if (fieldValues == null) {
                fieldValues = new LinkedHashMap(16);
            }
            parseFieldNode(null, false);
        }
    }

    public abstract String source();

    public final boolean isArray() {
        return isArray;
    }

    public final boolean isLeaf() {
        return leaf;
    }

    public final int getType() {
        return type;
    }

    public final int compareTo(JSONNode o) {
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
                for (int j = 0; j < elementSize; ++j) {
                    JSONNode value = elementValues[j];
                    value.writeTo(stringBuilder);
                    if (j < elementSize - 1) {
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

    abstract void writeSourceTo(StringBuilder stringBuilder);

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

    public final void setPathValue(String path, Serializable value) {
        JSONNode node = get(path);
        if (node != null && node.leaf) {
            node.setLeafValue(value);
        }
    }

    public final void removeElementAt(int index) {
        if (!isArray) {
            throw new UnsupportedOperationException();
        }
        if (index >= elementSize || index < 0) {
            throw new IndexOutOfBoundsException(" length: " + elementSize + ", to remove index at " + index);
        }
        if (!completed) {
            parseFullNode();
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
            parseFullNode();
        }
        JSONNode removeNode = fieldValues.remove(field);
        if (removeNode != null) {
            handleChange();
        }
        return removeNode;
    }

    public final List collect(String childPath) {
        return collect(childPath, String.class);
    }

    public final List collect(String childPath, Class typeClass) {
        if (!isArray) {
            throw new UnsupportedOperationException();
        }
        if (!this.completed) {
            this.parseFullNode();
        }
        List result = new ArrayList();
        for (int i = 0; i < elementSize; ++i) {
            JSONNode jsonNode = elementValues[i];
            result.add(jsonNode.getPathValue(childPath, typeClass));
        }
        return result;
    }

    public final void setLeafValue(Serializable value) {
        if (value == this.leafValue) {
            return;
        }
        this.leafValue = value;
        this.updateType();
        this.handleChange();
    }

    final void updateType() {
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

    public final void clear() {
        if (isArray) {
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
        leafValue = null;
        elementValues = null;
        fieldValues = null;
        clearSource();
    }

    protected abstract void clearSource();

    final void handleChange() {
        if (parent != null) {
            parent.handleChange();
        }
        changed = true;
    }

    public final String toJsonString() {
        if (!this.changed) {
            return this.source();
        }
        StringBuilder stringBuilder = new StringBuilder();
        writeTo(stringBuilder);
        return stringBuilder.toString();
    }
}
