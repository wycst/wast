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

import io.github.wycst.wast.common.beans.CharSource;
import io.github.wycst.wast.common.beans.UTF16ByteArraySource;
import io.github.wycst.wast.common.reflect.GenericParameterizedType;
import io.github.wycst.wast.common.reflect.UnsafeHelper;
import io.github.wycst.wast.json.exceptions.JSONException;
import io.github.wycst.wast.json.options.JSONParseContext;
import io.github.wycst.wast.json.options.Options;
import io.github.wycst.wast.json.options.ReadOption;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 默认解析器，根据字符的开头前缀解析为Map、List、String
 * <p> 去掉类型check问题，理论上最快。
 *
 * @Author wangyunchao
 * @Date 2022/5/31 21:28
 */
public final class JSONDefaultParser extends JSONGeneral {

    /**
     * return Map or List
     *
     * @param json
     * @param readOptions
     * @return Map or List
     */
    public static Object parse(String json, ReadOption... readOptions) {
        json.getClass();
        if (StringCoder) {
            int code = UnsafeHelper.getStringCoder(json);
            if (code == 0) {
                byte[] bytes = (byte[]) UnsafeHelper.getStringValue(json);
                return JSONByteArrayParser.parse(json, bytes, readOptions);
            } else {
                char[] chars = getChars(json);
                return parse(UTF16ByteArraySource.of(json, chars), chars, null, readOptions);
            }
        }
        return parse(getChars(json), readOptions);
    }

    /**
     * 解析buf返回Map或者List
     *
     * @param buf
     * @param readOptions
     * @return
     */
    public static Object parse(char[] buf, ReadOption... readOptions) {
        return parse(null, buf, null, readOptions);
    }

    /**
     * 解析字节数组
     *
     * @param bytes
     * @param readOptions
     * @return
     */
    public static Object parseBytes(byte[] bytes, ReadOption[] readOptions) {
//        if (StringCoder) {
//            // jdk9+
//            return JSONByteArrayParser.parse(bytes, readOptions);
//        }
//        // jdk <= 8 ，use char[]
//        return parse(getChars(new String(bytes)), readOptions);
        return JSONByteArrayParser.parse(bytes, readOptions);
    }

    /**
     * 指定外层的map类型
     *
     * @param json
     * @param mapCls
     * @param readOptions
     * @return
     */
    static Map parseMap(String json, Class<? extends Map> mapCls, ReadOption... readOptions) {
        json.getClass();
        if (StringCoder) {
            int code = UnsafeHelper.getStringCoder(json);
            if (code == 0) {
                byte[] bytes = (byte[]) UnsafeHelper.getStringValue(json);
                return (Map) JSONByteArrayParser.parse(json, bytes, createMapInstance(mapCls), readOptions);
            } else {
                char[] chars = getChars(json);
                return (Map) parse(UTF16ByteArraySource.of(json, chars), chars, createMapInstance(mapCls), readOptions);
            }
        }
        return (Map) parse(getChars(json), createMapInstance(mapCls), readOptions);
    }

    /**
     * 指定外层的map类型
     *
     * @param json
     * @param listCls
     * @param readOptions
     * @return
     */
    static Collection parseCollection(String json, Class<? extends Collection> listCls, ReadOption... readOptions) {
        json.getClass();
        if (StringCoder) {
            int code = UnsafeHelper.getStringCoder(json);
            if (code == 0) {
                byte[] bytes = (byte[]) UnsafeHelper.getStringValue(json);
                return (Collection) JSONByteArrayParser.parse(json, bytes, createCollectionInstance(listCls), readOptions);
            } else {
                char[] chars = getChars(json);
                return (Collection) parse(UTF16ByteArraySource.of(json, chars), chars, createCollectionInstance(listCls), readOptions);
            }
        }
        return (Collection) parse(getChars(json), createCollectionInstance(listCls), readOptions);
    }

    /**
     * 解析buf返回Map或者List
     *
     * @param buf
     * @param defaultValue
     * @param readOptions
     * @return
     */
    static Object parse(char[] buf, Object defaultValue, ReadOption... readOptions) {
        return parse(null, buf, defaultValue, readOptions);
    }

    static Object parse(CharSource source, char[] buf, Object defaultValue, ReadOption... readOptions) {
        return parse(source, buf, 0, buf.length, defaultValue, readOptions);
    }

    static Object parse(CharSource source, char[] buf, int fromIndex, int toIndex, Object defaultValue, ReadOption... readOptions) {
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
        try {

            boolean allowComment = jsonParseContext.isAllowComment();
            if (allowComment && beginChar == '/') {
                /** Remove comments declared in the header */
                fromIndex = clearCommentAndWhiteSpaces(buf, fromIndex + 1, toIndex, jsonParseContext);
                beginChar = buf[fromIndex];
            }
            Object result;
            switch (beginChar) {
                case '{':
                    result = parseJSONObject(source, buf, fromIndex, toIndex, defaultValue == null ? new LinkedHashMap() : (Map) defaultValue, jsonParseContext);
                    break;
                case '[':
                    result = parseJSONArray(source, buf, fromIndex, toIndex, defaultValue == null ? new ArrayList() : (Collection) defaultValue, jsonParseContext);
                    break;
                case '"':
                    result = parseJSONString(source, buf, fromIndex, toIndex, beginChar, jsonParseContext);
                    break;
                default:
                    throw new UnsupportedOperationException("Unsupported for begin character with '" + beginChar + "'");
            }

            int endIndex = jsonParseContext.getEndIndex();

            if (allowComment) {
                /** Remove comments at the end of the declaration */
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
                String errorContextTextAt = createErrorContextText(buf, endIndex + 1);
                throw new JSONException("Syntax error, at pos " + endIndex + ", context text by '" + errorContextTextAt + "', extra characters found, '" + new String(buf, endIndex + 1, wordNum) + " ...'");
            }
            return result;
        } catch (Exception ex) {
            if (ex instanceof JSONException) {
                throw (JSONException) ex;
            }
            // There is only one possibility to control out of bounds exceptions when indexing toindex
            if (ex instanceof IndexOutOfBoundsException) {
                String errorContextTextAt = createErrorContextText(buf, toIndex);
                throw new JSONException("Syntax error, context text by '" + errorContextTextAt + "', JSON format error, and the end token may be missing, such as '\"' or ', ' or '}' or ']'.");
            }
            throw new JSONException("Error: " + ex.getMessage(), ex);
        } finally {
            jsonParseContext.clear();
        }
    }

    static Collection parseJSONArray(CharSource source, char[] buf, int fromIndex, int toIndex, Collection list, JSONParseContext jsonParseContext) throws Exception {

        int beginIndex = fromIndex + 1;
        char ch;

        // The core token of the collection array is a comma
        for (int i = beginIndex; /*i < toIndex*/ ; ++i) {
            // clear white space characters
            while ((ch = buf[i]) <= ' ') {
                ++i;
            }
            // clear comment and whiteSpaces
            if (jsonParseContext.isAllowComment()) {
                if (ch == '/') {
                    ch = buf[i = clearCommentAndWhiteSpaces(buf, i + 1, toIndex, jsonParseContext)];
                }
            }
            // empty set or exception
            if (ch == ']') {
                if (list.size() > 0) {
                    throw new JSONException("Syntax error, at pos " + i + ", the closing symbol ']' is not allowed here.");
                }
                jsonParseContext.setEndIndex(i);
                return list;
            }
            Object value;
            switch (ch) {
                case '{': {
                    value = parseJSONObject(source, buf, i, toIndex, new LinkedHashMap(), jsonParseContext);
                    list.add(value);
                    i = jsonParseContext.getEndIndex();
                    break;
                }
                case '[': {
                    // 2 [ array
                    value = parseJSONArray(source, buf, i, toIndex, new ArrayList(), jsonParseContext);
                    list.add(value);
                    i = jsonParseContext.getEndIndex();
                    break;
                }
                case '\'':
                case '"': {
                    // 3 string
                    // When there are escape characters, the escape character needs to be parsed
                    value = parseJSONString(source, buf, i, toIndex, ch, jsonParseContext);
                    list.add(value);
                    i = jsonParseContext.getEndIndex();
                    break;
                }
                case 'n':
                    value = JSONTypeDeserializer.NULL.deserialize(null, buf, i, toIndex, null, null, '\0', jsonParseContext);
                    i = jsonParseContext.getEndIndex();
                    list.add(value);
                    break;
                case 't':
                    value = JSONTypeDeserializer.BOOLEAN.deserializeTrue(buf, i, toIndex, null, jsonParseContext);
                    i = jsonParseContext.getEndIndex();
                    list.add(value);
                    break;
                case 'f':
                    value = JSONTypeDeserializer.BOOLEAN.deserializeFalse(buf, i, toIndex, null, jsonParseContext);
                    i = jsonParseContext.getEndIndex();
                    list.add(value);
                    break;
                default: {
                    value = JSONTypeDeserializer.NUMBER.deserialize(source, buf, i, toIndex, jsonParseContext.isUseBigDecimalAsDefault() ? GenericParameterizedType.BigDecimalType : GenericParameterizedType.AnyType, null, ']', jsonParseContext);
                    i = jsonParseContext.getEndIndex();
                    list.add(value);
                    // either continue or return
                    ++i;
                    char next = buf[i];
                    if (next == ']') {
                        jsonParseContext.setEndIndex(i);
                        return list;
                    } else {
                        continue;
                    }
                }
            }
            // clear white space characters
            while ((ch = buf[++i]) <= ' ') ;
            // clear comment and whiteSpaces
            if (jsonParseContext.isAllowComment()) {
                if (ch == '/') {
                    ch = buf[i = clearCommentAndWhiteSpaces(buf, i + 1, toIndex, jsonParseContext)];
                }
            }
            // （Check whether the next character is a comma or end symbol. If yes, continue or return . If not, throw an exception）
            if (ch == ',') {
                continue;
            }
            if (ch == ']') {
                jsonParseContext.setEndIndex(i);
                return list;
            }
            String errorContextTextAt = createErrorContextText(buf, i);
            throw new JSONException("Syntax error, at pos " + i + ", context text by '" + errorContextTextAt + "', unexpected token character '" + ch + "', expected ',' or ']'");
        }
//        throw new JSONException("Syntax error, cannot find closing symbol ']' matching '['");
    }

    static Map parseJSONObject(CharSource source, char[] buf, int fromIndex, int toIndex, Map instance, JSONParseContext jsonParseContext) throws Exception {

        int beginIndex = fromIndex + 1;
        char ch;

        boolean empty = true;
        boolean allowomment = jsonParseContext.isAllowComment();
        boolean disableCacheMapKey = jsonParseContext.isDisableCacheMapKey();
        // for loop to parse
        for (int i = beginIndex; /*i < toIndex*/ ; ++i) {
            // clear white space characters
            while ((ch = buf[i]) <= ' ') {
                ++i;
            }
            // clear comment and whiteSpaces
            if (allowomment) {
                if (ch == '/') {
                    ch = buf[i = clearCommentAndWhiteSpaces(buf, i + 1, toIndex, jsonParseContext)];
                }
            }
            Serializable key;
            int fieldKeyFrom = i;
            // Standard JSON field name with "
            if (ch == '"') {
//                key = (Serializable) JSONTypeDeserializer.STRING.deserializeString(buf, i, toIndex, GenericParameterizedType.StringType, jsonParseContext);
                key = disableCacheMapKey ? parseMapKey(buf, i, toIndex, '"', jsonParseContext) : parseMapKeyByCache(buf, i, toIndex, '"', jsonParseContext);
                i = jsonParseContext.getEndIndex();
                empty = false;
                ++i;
            } else {
                // empty object or exception
                if (ch == '}') {
                    if (!empty) {
                        throw new JSONException("Syntax error, at pos " + i + ", the closing symbol '}' is not allowed here.");
                    }
                    jsonParseContext.setEndIndex(i);
                    return instance;
                }
                if (ch == '\'') {
                    if (jsonParseContext.isAllowSingleQuotes()) {
                        while (i + 1 < toIndex && (buf[++i] != '\'' || buf[i - 1] == '\\')) ;
                        empty = false;
                        ++i;
                        key = parseKeyOfMap(buf, fieldKeyFrom, i, false);
                    } else {
                        throw new JSONException("Syntax error, at pos " + i + ", the single quote symbol ' is not allowed here.");
                    }
                } else {
                    if (jsonParseContext.isAllowUnquotedFieldNames()) {
                        while (i + 1 < toIndex && buf[++i] != ':') ;
                        empty = false;
                        key = parseKeyOfMap(buf, fieldKeyFrom, i, true);
                        if (key.equals("null")) {
                            key = null;
                        }
                    } else {
                        int j = i;
                        boolean isNullKey = false;
                        key = null;
                        if (ch == 'n' && buf[++i] == 'u' && buf[++i] == 'l' && buf[++i] == 'l') {
                            isNullKey = true;
                            ++i;
                        }
                        if (!isNullKey) {
                            String errorContextTextAt = createErrorContextText(buf, j);
                            throw new JSONException("Syntax error, at pos " + j + ", context text by '" + errorContextTextAt + "', unexpected token character '" + ch + "', expected '\"' or use option ReadOption.AllowUnquotedFieldNames ");
                        }
                    }
                }

            }

            // clear white space characters
            while ((ch = buf[i]) <= ' ') {
                ++i;
            }
            // clear comment and whiteSpaces
            if (allowomment) {
                if (ch == '/') {
                    ch = buf[i = clearCommentAndWhiteSpaces(buf, i + 1, toIndex, jsonParseContext)];
                }
            }
            // Standard JSON rules:
            // 1 if matched, it can only be followed by a colon, and all other symbols are wrong
            // 2 after the attribute value is parsed, it can only be followed by a comma(,) or end, and other symbols are wrong
            if (ch == ':') {
                // clear white space characters
                while ((ch = buf[++i]) <= ' ') ;

                // clear comment and whiteSpaces
                if (jsonParseContext.isAllowComment()) {
                    if (ch == '/') {
                        ch = buf[i = clearCommentAndWhiteSpaces(buf, i + 1, toIndex, jsonParseContext)];
                    }
                }
                Object value;
                switch (ch) {
                    case '{': {
                        value = parseJSONObject(source, buf, i, toIndex, new LinkedHashMap(), jsonParseContext);
                        i = jsonParseContext.getEndIndex();
                        instance.put(key, value);
                        break;
                    }
                    case '[': {
                        // 2 [ array
                        value = parseJSONArray(source, buf, i, toIndex, new ArrayList(), jsonParseContext);
                        i = jsonParseContext.getEndIndex();
                        instance.put(key, value);
                        break;
                    }
                    case '"':
                    case '\'': {
                        value = parseJSONString(source, buf, i, toIndex, ch, jsonParseContext);
                        i = jsonParseContext.getEndIndex();
                        instance.put(key, value);
                        break;
                    }
                    case 'n':
                        value = JSONTypeDeserializer.NULL.deserialize(null, buf, i, toIndex, null, null, '\0', jsonParseContext);
                        i = jsonParseContext.getEndIndex();
                        instance.put(key, value);
                        break;
                    case 't':
                        value = JSONTypeDeserializer.BOOLEAN.deserializeTrue(buf, i, toIndex, null, jsonParseContext);
                        i = jsonParseContext.getEndIndex();
                        instance.put(key, value);
                        break;
                    case 'f':
                        value = JSONTypeDeserializer.BOOLEAN.deserializeFalse(buf, i, toIndex, null, jsonParseContext);
                        i = jsonParseContext.getEndIndex();
                        instance.put(key, value);
                        break;
                    default: {
                        value = JSONTypeDeserializer.NUMBER.deserialize(source, buf, i, toIndex, jsonParseContext.isUseBigDecimalAsDefault() ? GenericParameterizedType.BigDecimalType : GenericParameterizedType.AnyType, null, '}', jsonParseContext);
                        i = jsonParseContext.getEndIndex();
                        instance.put(key, value);
                        char next = buf[++i];
                        if (next == '}') {
                            jsonParseContext.setEndIndex(i);
                            return instance;
                        } else {
                            continue;
                        }
                    }
                }
                // clear white space characters
                while ((ch = buf[++i]) <= ' ') ;
                // clear comment and whiteSpaces
                if (allowomment) {
                    if (ch == '/') {
                        ch = buf[i = clearCommentAndWhiteSpaces(buf, i + 1, toIndex, jsonParseContext)];
                    }
                }
                // Check whether the next character is a comma or end symbol '}'. If yes, continue or break. If not, throw an exception
                if (ch == ',') {
                    continue;
                }
                if (ch == '}') {
                    jsonParseContext.setEndIndex(i);
                    return instance;
                }
                String errorContextTextAt = createErrorContextText(buf, i);
                throw new JSONException("Syntax error, at pos " + i + ", context text by '" + errorContextTextAt + "', unexpected token character '" + ch + "', expected ',' or '}'");
            } else {
                throw new JSONException("Syntax error, at pos " + i + ", unexpected token character '" + ch + "', Colon character ':' is expected.");
            }
        }
//        throw new JSONException("Syntax error, the closing symbol '}' is not found ");
    }

    static String parseJSONString(CharSource source, char[] buf, int from, int toIndex, char endCh, JSONParseContext jsonParseContext) {
        int beginIndex = from + 1;
        JSONStringWriter writer = null;

        if (source != null && JDK_9_ABOVE) {
            int endIndex = source.indexOf(endCh, beginIndex);
            source.setCharAt(endIndex, '\\');
            // find \\
            int escapeIndex = source.indexOf('\\', beginIndex);
            source.setCharAt(endIndex, endCh);
            if (escapeIndex == endIndex) {
                jsonParseContext.setEndIndex(endIndex);
                int len = endIndex - beginIndex;
                return len < 1024 ? new String(buf, beginIndex, len) : source.substring(beginIndex, endIndex);
            }
            writer = getContextWriter(jsonParseContext);
            char prev = buf[endIndex - 1];
            if (prev == '\\') {
                do {
                    endIndex = source.indexOf(endCh, endIndex + 1);
                    prev = buf[endIndex - 1];
                } while (prev == '\\');
            }
            try {
                source.setCharAt(endIndex, '\\');
                do {
                    beginIndex = escapeNext(buf, buf[escapeIndex + 1], escapeIndex, beginIndex, writer, jsonParseContext);
                    escapeIndex = source.indexOf('\\', beginIndex);
                } while (escapeIndex != endIndex);
            } finally {
                // restore
                source.setCharAt(endIndex, endCh);
            }
            jsonParseContext.setEndIndex(endIndex);
            writer.writeString(source.input(), beginIndex, escapeIndex - beginIndex);
            return writer.toString();
        }

        char ch = '\0', next = '\0';
        int i = beginIndex;
        int len;
        boolean escape = false;

        for (; /*i < toIndex*/ ; ++i) {
            while (/*i < toIndex &&*/ (ch = buf[i]) != '\\' && ch != endCh) {
                ++i;
            }
            // ch is \\ or "
            if (ch == '\\') {
                next = buf[i + 1];
                if (writer == null) {
                    writer = getContextWriter(jsonParseContext);
                    escape = true;
                }
                beginIndex = escapeNext(buf, next, i, beginIndex, writer, jsonParseContext);
                i = jsonParseContext.getEndIndex();
            } else {
                jsonParseContext.setEndIndex(i);
                len = i - beginIndex;
                if (escape) {
                    writer.write(buf, beginIndex, len);
                    return writer.toString();
                } else {
                    return len == 0 ? "" : new String(buf, beginIndex, len);
                }
            }
        }
//        throw new JSONException("Syntax error, the closing symbol '" + endCh + "' is not found ");
    }

    static String parseMapKey(char[] buf, int from, int toIndex, char endCh, JSONParseContext jsonParseContext) {
        int beginIndex = from + 1;
        char ch = '\0', next = '\0';
        int i = beginIndex;
        int len;

        JSONStringWriter writer = null;
        boolean escape = false;
        for (; ; ++i) {
            while ((ch = buf[i]) != '\\' && ch != endCh) {
                ++i;
            }
            // ch is \\ or "
            if (ch == '\\') {
                next = buf[i + 1];
                if (writer == null) {
                    writer = getContextWriter(jsonParseContext);
                }
                escape = true;
                beginIndex = escapeNext(buf, next, i, beginIndex, writer, jsonParseContext);
                i = jsonParseContext.getEndIndex();
            } else {
                jsonParseContext.setEndIndex(i);
                len = i - beginIndex;
                if (escape) {
                    writer.write(buf, beginIndex, len);
                    return writer.toString();
                } else {
                    return len == 0 ? "" : new String(buf, beginIndex, len);
                }
            }
        }
    }

    static String parseMapKeyByCache(char[] buf, int from, int toIndex, char endCh, JSONParseContext jsonParseContext) {
        int beginIndex = from + 1;
        char ch = '\0', next = '\0';
        int i = beginIndex;
        int len;

        JSONStringWriter writer = null;
        boolean escape = false;
        for (; ; ++i) {
            int hashValue = 0;
            while ((ch = buf[i]) != '\\' && ch != endCh) {
                ++i;
                hashValue = hashValue * 31 + ch;
            }
            // ch is \\ or "
            if (ch == '\\') {
                if (i < toIndex - 1) {
                    next = buf[i + 1];
                }
                if (writer == null) {
                    writer = getContextWriter(jsonParseContext);
                }
                escape = true;
                beginIndex = escapeNext(buf, next, i, beginIndex, writer, jsonParseContext);
                i = jsonParseContext.getEndIndex();
            } else {
                jsonParseContext.setEndIndex(i);
                len = i - beginIndex;
                if (escape) {
                    writer.write(buf, beginIndex, len);
                    return writer.toString();
                } else {
                    return len == 0 ? "" : jsonParseContext.getCacheKey(buf, beginIndex, len, hashValue);
                }
            }
        }
    }

    /**
     * @param from
     * @param to
     * @param buf
     * @param isUnquotedFieldName
     * @return
     */
    static String parseKeyOfMap(char[] buf, int from, int to, boolean isUnquotedFieldName) {
        if (isUnquotedFieldName) {
            while ((from < to) && ((buf[from]) <= ' ')) {
                from++;
            }
            while ((to > from) && ((buf[to - 1]) <= ' ')) {
                to--;
            }
            return new String(buf, from, to - from);
        } else {
            int len = to - from - 2;
            return new String(buf, from + 1, len);
        }
    }
}
