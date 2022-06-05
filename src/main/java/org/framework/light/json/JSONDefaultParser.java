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
package org.framework.light.json;

import org.framework.light.json.exceptions.JSONException;
import org.framework.light.json.options.JSONParseContext;
import org.framework.light.json.options.Options;
import org.framework.light.json.options.ReadOption;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 默认解析器，根据字符的开头前缀解析为Map或者List
 *
 * @Author wangyunchao
 * @Date 2022/5/31 21:28
 */
class JSONDefaultParser extends JSONGeneral {

    /**
     * 解析json返回Map或者List
     *
     * @param json
     * @param readOptions
     * @return
     */
    public final static Object parse(String json, ReadOption... readOptions) {
        if (json == null) return null;
        return parse(getChars(json), readOptions);
    }

    /**
     * 解析buf返回Map或者List
     *
     * @param buf
     * @param readOptions
     * @return
     */
    public final static Object parse(char[] buf, ReadOption... readOptions) {
        if (buf == null || buf.length == 0) return null;
        int fromIndex = 0;
        int toIndex = buf.length;
        // Trim remove white space characters
        char beginChar = '\0';
        while ((fromIndex < toIndex) && (beginChar = buf[fromIndex]) <= ' ') {
            fromIndex++;
        }
        while ((toIndex > fromIndex) && buf[toIndex - 1] <= ' ') {
            toIndex--;
        }

        JSONParseContext jsonParseContext = new JSONParseContext();
        Options.readOptions(readOptions, jsonParseContext);
        jsonParseContext.setContextWriter(new JSONWriter());
        try {

            boolean allowComment = jsonParseContext.isAllowComment();
            if (allowComment && beginChar == '/') {
                /** 去除声明在头部的注释*/
                fromIndex = clearCommentAndWhiteSpaces(buf, fromIndex + 1, toIndex, jsonParseContext);
                beginChar = buf[fromIndex];
            }
            Object result;

            switch (beginChar) {
                case '{':
                    result = parseJSONObject(fromIndex, toIndex, buf, jsonParseContext);
                    break;
                case '[':
                    result = parseJSONArray(fromIndex, toIndex, buf, jsonParseContext);
                    break;
                case '"':
                    result = parseJSONString(fromIndex, toIndex, buf, beginChar, jsonParseContext);
                    break;
                default:
                    throw new UnsupportedOperationException("Unsupported for begin character with '" + beginChar + "' pos 0 ");
            }

            int endIndex = jsonParseContext.getEndIndex();

            if (allowComment) {
                /** 去除声明在尾部的注释*/
                if (endIndex < toIndex - 1) {
                    char commentStart = '\0';
                    while (endIndex + 1 < toIndex && (commentStart = buf[++endIndex]) <= ' ') ;
                    if (commentStart == '/') {
                        endIndex = clearCommentAndWhiteSpaces(buf, endIndex + 1, toIndex, jsonParseContext);
                    }
                }
            }

            if (endIndex != toIndex - 1) {
                int wordNum = Math.min(50, buf.length - endIndex - 1);
                throw new JSONException("Syntax error, extra characters found, '" + new String(buf, endIndex + 1, wordNum) + "', at pos " + endIndex);
            }
            return result;
        } catch (Exception ex) {
            if (ex instanceof JSONException) {
                throw (JSONException) ex;
            }
            throw new JSONException("Error: " + ex.getMessage(), ex);
        } finally {
            jsonParseContext.clear();
        }
    }

    private static List parseJSONArray(int fromIndex, int toIndex, char[] buf, JSONParseContext jsonParseContext) {

        List<Object> list = new ArrayList<Object>();

        int beginIndex = fromIndex + 1;
        char ch = '\0';

        // 集合数组核心token是逗号（The core token of the collection array is a comma）
        // for loop
        for (int i = beginIndex; i < toIndex; i++) {

            // clear white space characters
            while ((ch = buf[i]) <= ' ') {
                i++;
            }

            // for simple element parse
            int simpleFromIndex = i, simpleToIndex = -1;

            // 如果提前遇到字符']'，说明是空集合
            //  （If the character ']' is encountered in advance, it indicates that it is an empty set）
            // 另一种可能性(语法错误): 非空集合，发生在空逗号后接结束字符']'直接抛出异常
            //  (Another possibility(Syntax error): whether the empty comma followed by the closing character ']' throws an exception)
            if (ch == ']') {
                if (list.size() > 0) {
                    throw new JSONException("Syntax error, not allowed ',' followed by ']', pos " + i);
                }
                jsonParseContext.setEndIndex(i);
                return list;
            }
            // 是否简单元素如null, true or false or numeric or string
            // (is simple elements such as null, true or false or numeric or string)
            boolean isSimpleElement = false;
            Object value = null;
            if (ch == '{') {
                value = parseJSONObject(i, toIndex, buf, jsonParseContext);
                list.add(value);
                i = jsonParseContext.getEndIndex();
            } else if (ch == '[') {
                // 2 [ array
                value = parseJSONArray(i, toIndex, buf, jsonParseContext);
                list.add(value);
                i = jsonParseContext.getEndIndex();
            } else if (ch == '"' || ch == '\'') {
                // 3 string
                // When there are escape characters, the escape character needs to be parsed
                value = parseJSONString(i, toIndex, buf, ch, jsonParseContext);
                list.add(value);
                i = jsonParseContext.getEndIndex();
            } else {
                // 简单元素（Simple element）
                isSimpleElement = true;
                while (i + 1 < toIndex) {
                    ch = buf[i + 1];
                    if (ch == ',' || ch == ']') {
                        break;
                    }
                    i++;
                }
            }

            // 清除空白字符（clear white space characters）
            while ((ch = buf[++i]) <= ' ') ;
            if (simpleToIndex == -1) {
                simpleToIndex = i;
            }

            // 检查下一个字符是逗号还是结束符号。如果是，继续或者终止。如果不是，则抛出异常
            // （Check whether the next character is a comma or end symbol. If yes, continue or return . If not, throw an exception）
            boolean isEnd = ch == ']';
            if (ch == ',' || isEnd) {
                if (isSimpleElement) {
                    Serializable simpleValue = (Serializable) parseSimpleValue(simpleFromIndex, simpleToIndex, buf, jsonParseContext);
                    list.add(simpleValue);
                }
                if (isEnd) {
                    jsonParseContext.setEndIndex(i);
                    return list;
                }
            } else {
                throw new JSONException("Syntax error, unexpected token character '" + ch + "', position " + i + ", Missing ',' or '}'");
            }
        }
        throw new JSONException("Syntax error, the closing symbol ']' is not found ");
    }

    private static Map parseJSONObject(int fromIndex, int toIndex, char[] buf, JSONParseContext jsonParseContext) {
        Map<String, Object> instance = new LinkedHashMap<String, Object>();

        int beginIndex = fromIndex + 1;
        char ch = '\0';
        String key = null;

        boolean empty = true;

        // for loop to parse
        for (int i = beginIndex; i < toIndex; i++) {
            // clear white space characters
            while ((ch = buf[i]) <= ' ') {
                i++;
            }
            int fieldKeyFrom = i, fieldKeyTo, splitIndex, simpleToIndex = -1;
            // Standard JSON field name with "
            if (ch == '"') {
                while (i + 1 < toIndex && (buf[++i] != '"' || buf[i - 1] == '\\')) ;
                empty = false;
                i++;
            } else {
                if (ch == '}') {
                    if (!empty) {
                        throw new JSONException("Syntax error, the closing symbol '}' is not allowed at pos " + i);
                    }
                    jsonParseContext.setEndIndex(i);
                    // 空对象提前结束查找
                    return instance;
                }

                if (ch == '\'') {
                    if (jsonParseContext.isAllowSingleQuotes()) {
                        while (i + 1 < toIndex && buf[++i] != '\'') ;
                        empty = false;
                        i++;
                    } else {
                        throw new JSONException("Syntax error, the single quote symbol ' is not allowed at pos " + i);
                    }
                } else {
                    if (jsonParseContext.isAllowUnquotedFieldNames()) {
                        // 无引号key处理
                        // 直接锁定冒号（:）位置
                        while (i + 1 < toIndex && buf[++i] != ':') ;
                        empty = false;
                    }
                }

            }

            // clear white space characters
            while ((ch = buf[i]) <= ' ') {
                i++;
            }
            // 清除注释前记录属性字段的token结束位置
            fieldKeyTo = i;

            // Standard JSON rules:
            // 1 if matched, it can only be followed by a colon, and all other symbols are wrong
            // 2 after the attribute value is parsed, it can only be followed by a comma(,) or end, and other symbols are wrong
            if (ch == ':') {
                // Resolve key value pairs
                key = parseFieldKey(fieldKeyFrom, fieldKeyTo, buf);
                // 清除空白字符（clear white space characters）
                while ((ch = buf[++i]) <= ' ') ;

                // 分割符位置,SimpleMode
                splitIndex = i - 1;

                // 空，布尔值，数字，或者字符串 （ null, true or false or numeric or string）
                boolean isSimpleValue = false;
                Object value = null;

                switch (ch) {
                    case '{': {
                        value = parseJSONObject(i, toIndex, buf, jsonParseContext);
                        i = jsonParseContext.getEndIndex();
                        instance.put(key, value);
                        break;
                    }
                    case '[': {
                        // 2 [ array
                        // 解析集合或者数组 （Parse a collection or array）
                        value = parseJSONArray(i, toIndex, buf, jsonParseContext);
                        i = jsonParseContext.getEndIndex();
                        instance.put(key, value);
                        break;
                    }
                    case '"':
                    case '\'': {
                        value = parseJSONString(i, toIndex, buf, ch, jsonParseContext);
                        i = jsonParseContext.getEndIndex();
                        instance.put(key, value);
                        break;
                    }
                    default: {
                        isSimpleValue = true;
                        // 4 null, true or false or numeric
                        // Find comma(,) or closing symbol(})
                        while (i + 1 < toIndex) {
                            ch = buf[i + 1];
                            if (ch == ',' || ch == '}') {
                                break;
                            }
                            i++;
                        }
                        // Check whether post comments are appended
                    }
                }

                // clear white space characters
                while ((ch = buf[++i]) <= ' ') ;
                if (simpleToIndex == -1) {
                    simpleToIndex = i;
                }

                // Check whether the next character is a comma or end symbol '}'. If yes, continue or break. If not, throw an exception
                boolean isClosingSymbol = ch == '}';
                if (ch == ',' || isClosingSymbol) {
                    if (isSimpleValue) {
                        value = parseSimpleValue(splitIndex + 1, simpleToIndex, buf, jsonParseContext);
                        instance.put(key, value);
                    }
                    if (isClosingSymbol) {
                        jsonParseContext.setEndIndex(i);
                        return instance;
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

    private static String parseJSONString(int from, int toIndex, char[] buf, char endCh, JSONParseContext jsonParseContext) {
        int beginIndex = from + 1;
        char ch = '\0', next = '\0';
        int i = beginIndex;
        int len;

        JSONWriter writer = jsonParseContext.getContextWriter();
        if (writer == null) {
            writer = new JSONWriter();
            jsonParseContext.setContextWriter(writer);
        }
        for (; i < toIndex; i++) {
            while (i < toIndex && (ch = buf[i]) != '\\' && ch != endCh) {
                i++;
            }
            // ch is \\ or "
            if (ch == '\\') {
                if (i < toIndex - 1) {
                    next = buf[i + 1];
                }
                switch (next) {
                    // 这里不管了，单引号双引号前面有转义符通通给处理掉
                    case '\'':
                    case '"':
                        if (i > beginIndex) {
                            writer.write(buf, beginIndex, i - beginIndex + 1);
                            writer.setCharAt(writer.size() - 1, next);
                        } else {
                            writer.append(next);
                        }
                        beginIndex = ++i + 1;
                        break;
                    case 'n':
                        len = i - beginIndex;
                        writer.write(buf, beginIndex, len + 1);
                        writer.setCharAt(writer.size() - 1, '\n');
                        beginIndex = ++i + 1;
                        break;
                    case 'r':
                        len = i - beginIndex;
                        writer.write(buf, beginIndex, len + 1);
                        writer.setCharAt(writer.size() - 1, '\r');
                        beginIndex = ++i + 1;
                        break;
                    case 't':
                        len = i - beginIndex;
                        writer.write(buf, beginIndex, len + 1);
                        writer.setCharAt(writer.size() - 1, '\t');
                        beginIndex = ++i + 1;
                        break;
                    case 'b':
                        len = i - beginIndex;
                        writer.write(buf, beginIndex, len + 1);
                        writer.setCharAt(writer.size() - 1, '\b');
                        beginIndex = ++i + 1;
                        break;
                    case 'f':
                        len = i - beginIndex;
                        writer.write(buf, beginIndex, len + 1);
                        writer.setCharAt(writer.size() - 1, '\f');
                        beginIndex = ++i + 1;
                        break;
                    case 'u':
                        len = i - beginIndex;
                        writer.write(buf, beginIndex, len + 1);
                        int c = parseInt(buf, i + 2, i + 6, 16);
                        writer.setCharAt(writer.size() - 1, (char) c);
                        i += 4;
                        beginIndex = ++i + 1;
                        break;
                    case '\\':
                        if (i > beginIndex) {
                            writer.write(buf, beginIndex, i - beginIndex + 1);
                            writer.setCharAt(writer.size() - 1, '\\');
                        } else {
                            writer.append('\\');
                        }
                        beginIndex = ++i + 1;
                        break;
                }
            } else {
                jsonParseContext.setEndIndex(i);
                len = i - beginIndex;
                if (writer.size() > 0) {
                    writer.write(buf, beginIndex, len);
                    return writer.toString();
                } else {
                    return len == 0 ? "" : new String(buf, beginIndex, len);
                }
            }
        }
        throw new JSONException("Syntax error, the closing symbol '" + endCh + "' is not found ");
    }

    // 数字， boolean， null
    private static Object parseSimpleValue(int fromIndex, int toIndex, char[] buf, JSONParseContext jsonParseContext) {
        // 初始位置
        int pos = fromIndex;
        char beginChar = '\0';
        char endChar = '\0';

        while ((fromIndex < toIndex) && ((beginChar = buf[fromIndex]) <= ' ')) {
            fromIndex++;
        }
        while ((toIndex > fromIndex) && ((endChar = buf[toIndex - 1]) <= ' ')) {
            toIndex--;
        }

        int len = toIndex - fromIndex;
        switch (beginChar) {
            case '"':
                // 读取项设置禁用转义会出现，endChar一定是\”不用判断
                return new String(buf, fromIndex + 1, len - 2);
            case 't':
                if (buf[fromIndex + 1] == 'r'
                        && buf[fromIndex + 2] == 'u'
                        && endChar == 'e') {
                    return true;
                }
                throw new JSONException("Syntax error, offset pos " + fromIndex + ", 't' is unexpected");
            case 'f':
                if (buf[fromIndex + 1] == 'a'
                        && buf[fromIndex + 2] == 'l'
                        && buf[fromIndex + 3] == 's'
                        && endChar == 'e') {
                    return false;
                }
                throw new JSONException("Syntax error, offset pos " + fromIndex + ", 'f' is unexpected");
            case 'n':
                if (buf[fromIndex + 1] == 'u'
                        && buf[fromIndex + 2] == 'l'
                        && endChar == 'l') {
                    return null;
                }
                throw new JSONException("Syntax error, offset pos " + fromIndex + ", 'n' is unexpected");
            default:
                return parseNumber(buf, fromIndex, toIndex, jsonParseContext.isUseBigDecimalAsDefault());
        }

    }

    /**
     * @param from
     * @param to
     * @param buf
     * @return
     */
    private static String parseFieldKey(int from, int to, char[] buf) {
        char start = '"';
        while ((from < to) && ((start = buf[from]) <= ' ')) {
            from++;
        }
        char end = '"';
        while ((to > from) && ((end = buf[to - 1]) <= ' ')) {
            to--;
        }
        if (start == '"' && end == '"' || (start == '\'' && end == '\'')) {
            int len = to - from - 2;
            return new String(buf, from + 1, len);
        }
        return new String(buf, from, to - from);
    }


}
