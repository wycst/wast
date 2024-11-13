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
import io.github.wycst.wast.common.utils.EnvUtils;
import io.github.wycst.wast.json.exceptions.JSONException;
import io.github.wycst.wast.json.options.ReadOption;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * <p> 默认解析器，根据字符的开头前缀解析为Map、List、String
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
        if (EnvUtils.JDK_9_PLUS) {
            byte[] bytes = (byte[]) JSONUnsafe.getStringValue(json);
            if (bytes.length == json.length()) {
                return parseInternal(AsciiStringSource.of(json, bytes), bytes, 0, bytes.length, null, readOptions);
            } else {
                char[] chars = json.toCharArray();
                return parseInternal(UTF16ByteArraySource.of(json), chars, 0, chars.length, null, readOptions);
            }
        }
        char[] chars = (char[]) JSONUnsafe.getStringValue(json);
        return parseInternal(null, chars, 0, chars.length, null, readOptions);
    }

    /**
     * 解析buf返回Map或者List
     *
     * @param buf
     * @param readOptions
     * @return
     */
    public static Object parse(char[] buf, ReadOption... readOptions) {
        if (EnvUtils.JDK_9_PLUS) {
            String json = new String(buf);
            byte[] bytes = (byte[]) JSONUnsafe.getStringValue(json);
            if (bytes.length == buf.length) {
                return parseInternal(AsciiStringSource.of(json, bytes), bytes, 0, bytes.length, null, readOptions);
            } else {
                return parseInternal(UTF16ByteArraySource.of(json), buf, 0, buf.length, null, readOptions);
            }
        }
        return parseInternal(null, buf, 0, buf.length, null, readOptions);
    }

    /**
     * 解析buf返回Map或者List
     *
     * @param buf
     * @param offset
     * @param len
     * @param readOptions
     * @return
     */
    static Object parse(char[] buf, int offset, int len, ReadOption... readOptions) {
        if (EnvUtils.JDK_9_PLUS) {
            String json = new String(buf, offset, len);
            byte[] bytes = (byte[]) JSONUnsafe.getStringValue(json);
            if (bytes.length == json.length()) {
                return parseInternal(AsciiStringSource.of(json, bytes), bytes, 0, bytes.length, null, readOptions);
            } else {
                char[] chars = len == buf.length ? buf : json.toCharArray();
                return parseInternal(UTF16ByteArraySource.of(json), chars, 0, chars.length, null, readOptions);
            }
        }
        return parseInternal(null, buf, offset, len, null, readOptions);
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
        if (EnvUtils.JDK_9_PLUS) {
            byte[] bytes = (byte[]) JSONUnsafe.getStringValue(json);
            if (bytes.length == json.length()) {
                return (Map) parseInternal(AsciiStringSource.of(json, bytes), bytes, 0, bytes.length, createMapInstance(mapCls), readOptions);
            } else {
                char[] chars = json.toCharArray();
                return (Map) parseInternal(UTF16ByteArraySource.of(json), chars, 0, chars.length, createMapInstance(mapCls), readOptions);
            }
        }
        char[] chars = (char[]) JSONUnsafe.getStringValue(json);
        return (Map) parseInternal(null, chars, 0, chars.length, createMapInstance(mapCls), readOptions);
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
        if (EnvUtils.JDK_9_PLUS) {
            byte[] bytes = (byte[]) JSONUnsafe.getStringValue(json);
            if (bytes.length == json.length()) {
                return (Collection) parseInternal(AsciiStringSource.of(json, bytes), bytes, 0, bytes.length, createCollectionInstance(listCls), readOptions);
            } else {
                char[] chars = json.toCharArray();
                return (Collection) parseInternal(UTF16ByteArraySource.of(json), chars, 0, chars.length, createCollectionInstance(listCls), readOptions);
            }
        }
        char[] chars = (char[]) JSONUnsafe.getStringValue(json);
        return (Collection) parseInternal(null, chars, 0, chars.length, createCollectionInstance(listCls), readOptions);
    }

    static Object parseInternal(CharSource source, char[] buf, int fromIndex, int toIndex, Object defaultValue, ReadOption... readOptions) {
        char beginChar = '\0';
        while ((fromIndex < toIndex) && (beginChar = buf[fromIndex]) <= ' ') {
            fromIndex++;
        }
        while ((toIndex > fromIndex) && buf[toIndex - 1] <= ' ') {
            toIndex--;
        }
        JSONParseContext jsonParseContext = new JSONParseContext();
        JSONOptions.readOptions(readOptions, jsonParseContext);
        try {
            boolean allowComment = jsonParseContext.allowComment;
            if (allowComment && beginChar == '/') {
                fromIndex = clearCommentAndWhiteSpaces(buf, fromIndex + 1, toIndex, jsonParseContext);
                beginChar = buf[fromIndex];
            }
            Object result;
            switch (beginChar) {
                case '{':
                    result = parseJSONObject(source, buf, fromIndex, toIndex, defaultValue == null ? new LinkedHashMap() : (Map) defaultValue, jsonParseContext);
                    break;
                case '[':
                    result = parseJSONArray(source, buf, fromIndex, toIndex, defaultValue == null ? new ArrayList(10) : (Collection) defaultValue, jsonParseContext);
                    break;
                case '\'':
                case '"':
                    result = JSONTypeDeserializer.CHAR_SEQUENCE_STRING.deserializeString(source, buf, fromIndex, toIndex, beginChar, null, jsonParseContext);
                    break;
                default:
                    throw new UnsupportedOperationException("Unsupported for begin character with '" + beginChar + "'");
            }

            int endIndex = jsonParseContext.endIndex;

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
            handleCatchException(ex, buf, toIndex);
            throw new JSONException("Error: " + ex.getMessage(), ex);
        } finally {
            jsonParseContext.clear();
        }
    }

    static Collection parseJSONArray(CharSource source, char[] buf, int fromIndex, int toIndex, Collection list, JSONParseContext jsonParseContext) throws Exception {
        char ch;
        int i = fromIndex;
        for (; ; ) {
            while ((ch = buf[++i]) <= ' ') ;
            if (jsonParseContext.allowComment) {
                if (ch == '/') {
                    ch = buf[i = clearCommentAndWhiteSpaces(buf, i + 1, toIndex, jsonParseContext)];
                }
            }
            if (ch == ']') {
                if (list.size() > 0 && !jsonParseContext.allowLastEndComma) {
                    throw new JSONException("Syntax error, at pos " + i + ", the closing symbol ']' is not allowed here.");
                }
                jsonParseContext.endIndex = i;
                return list;
            }
            Object value;
            switch (ch) {
                case '{': {
                    value = parseJSONObject(source, buf, i, toIndex, new LinkedHashMap(10), jsonParseContext);
                    list.add(value);
                    i = jsonParseContext.endIndex;
                    break;
                }
                case '[': {
                    // Multilayer collections are relatively rare and can easily cause waste of collection space. The specified initialization capacity is displayed as 2
                    value = parseJSONArray(source, buf, i, toIndex, new ArrayList(10), jsonParseContext);
                    list.add(value);
                    i = jsonParseContext.endIndex;
                    break;
                }
                case '\'':
                case '"': {
                    value = JSONTypeDeserializer.CHAR_SEQUENCE_STRING.deserializeString(source, buf, i, toIndex, ch, null, jsonParseContext);
                    list.add(value);
                    i = jsonParseContext.endIndex;
                    break;
                }
                case 'n':
                    value = JSONTypeDeserializer.NULL.deserialize(null, buf, i, toIndex, null, null, '\0', jsonParseContext);
                    i = jsonParseContext.endIndex;
                    list.add(value);
                    break;
                case 't':
                    value = JSONTypeDeserializer.BOOLEAN.parseTrue(buf, i, toIndex, jsonParseContext);
                    i = jsonParseContext.endIndex;
                    list.add(value);
                    break;
                case 'f':
                    value = JSONTypeDeserializer.BOOLEAN.parseFalse(buf, i, toIndex, jsonParseContext);
                    i = jsonParseContext.endIndex;
                    list.add(value);
                    break;
                default: {
                    value = JSONTypeDeserializer.NUMBER.deserialize(source, buf, i, toIndex, jsonParseContext.useBigDecimalAsDefault ? GenericParameterizedType.BigDecimalType : GenericParameterizedType.AnyType, null, ']', jsonParseContext);
                    i = jsonParseContext.endIndex;
                    list.add(value);
                    // either continue or return
                    char next = buf[++i];
                    if (next == ']') {
                        jsonParseContext.endIndex = i;
                        return list;
                    } else {
                        continue;
                    }
                }
            }
            while ((ch = buf[++i]) <= ' ') ;
            if (jsonParseContext.allowComment) {
                if (ch == '/') {
                    ch = buf[i = clearCommentAndWhiteSpaces(buf, i + 1, toIndex, jsonParseContext)];
                }
            }
            if (ch == ',') {
                continue;
            }
            if (ch == ']') {
                jsonParseContext.endIndex = i;
                return list;
            }
            String errorContextTextAt = createErrorContextText(buf, i);
            throw new JSONException("Syntax error, at pos " + i + ", context text by '" + errorContextTextAt + "', unexpected '" + ch + "', expected ',' or ']'");
        }
    }

    static Map parseJSONObject(CharSource source, char[] buf, int fromIndex, int toIndex, Map instance, JSONParseContext jsonParseContext) throws Exception {
        int i = fromIndex;
        char ch;
        boolean empty = true;
        boolean allowomment = jsonParseContext.allowComment;
        boolean disableCacheMapKey = jsonParseContext.disableCacheMapKey;
        for (; ; ) {
            while ((ch = buf[++i]) <= ' ') ;
            if (allowomment) {
                if (ch == '/') {
                    ch = buf[i = clearCommentAndWhiteSpaces(buf, i + 1, toIndex, jsonParseContext)];
                }
            }
            Serializable key;
            int fieldKeyFrom = i;
            if (ch == '"') {
                key = disableCacheMapKey ? (String) JSONTypeDeserializer.CHAR_SEQUENCE_STRING.deserializeString(source, buf, i, toIndex, '"', null, jsonParseContext) : parseMapKeyByCache(buf, i, toIndex, '"', jsonParseContext);
                i = jsonParseContext.endIndex;
                empty = false;
                ++i;
            } else {
                if (ch == '}') {
                    if (!empty && !jsonParseContext.allowLastEndComma) {
                        throw new JSONException("Syntax error, at pos " + i + ", the closing symbol '}' is not allowed here.");
                    }
                    jsonParseContext.endIndex = i;
                    return instance;
                }
                if (ch == '\'') {
                    if (jsonParseContext.allowSingleQuotes) {
                        while (i + 1 < toIndex && (buf[++i] != '\'' || buf[i - 1] == '\\')) ;
                        empty = false;
                        ++i;
                        key = parseKeyOfMap(buf, fieldKeyFrom, i, false);
                    } else {
                        throw new JSONException("Syntax error, at pos " + i + ", the single quote symbol ' is not allowed here.");
                    }
                } else {
                    if (jsonParseContext.allowUnquotedFieldNames) {
                        while (i + 1 < toIndex && buf[++i] != ':') ;
                        empty = false;
                        key = parseKeyOfMap(buf, fieldKeyFrom, i, true);
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
                            throw new JSONException("Syntax error, at pos " + j + ", context text by '" + errorContextTextAt + "', unexpected '" + ch + "', expected '\"' or use option ReadOption.AllowUnquotedFieldNames ");
                        }
                    }
                }
            }
            while ((ch = buf[i]) <= ' ') {
                ++i;
            }
            if (allowomment) {
                if (ch == '/') {
                    ch = buf[i = clearCommentAndWhiteSpaces(buf, i + 1, toIndex, jsonParseContext)];
                }
            }
            if (ch == ':') {
                while ((ch = buf[++i]) <= ' ') ;
                if (allowomment) {
                    if (ch == '/') {
                        ch = buf[i = clearCommentAndWhiteSpaces(buf, i + 1, toIndex, jsonParseContext)];
                    }
                }
                Object value;
                switch (ch) {
                    case '{': {
                        value = parseJSONObject(source, buf, i, toIndex, new LinkedHashMap(10), jsonParseContext);
                        i = jsonParseContext.endIndex;
                        instance.put(key, value);
                        break;
                    }
                    case '[': {
                        // 2 [ array
                        value = parseJSONArray(source, buf, i, toIndex, new ArrayList(10), jsonParseContext);
                        i = jsonParseContext.endIndex;
                        instance.put(key, value);
                        break;
                    }
                    case '"':
                    case '\'': {
                        value = JSONTypeDeserializer.CHAR_SEQUENCE_STRING.deserializeString(source, buf, i, toIndex, ch, null, jsonParseContext);
                        i = jsonParseContext.endIndex;
                        instance.put(key, value);
                        break;
                    }
                    case 'n':
                        value = JSONTypeDeserializer.NULL.deserialize(null, buf, i, toIndex, null, null, '\0', jsonParseContext);
                        i = jsonParseContext.endIndex;
                        instance.put(key, value);
                        break;
                    case 't':
                        value = JSONTypeDeserializer.BOOLEAN.parseTrue(buf, i, toIndex, jsonParseContext);
                        i = jsonParseContext.endIndex;
                        instance.put(key, value);
                        break;
                    case 'f':
                        value = JSONTypeDeserializer.BOOLEAN.parseFalse(buf, i, toIndex, jsonParseContext);
                        i = jsonParseContext.endIndex;
                        instance.put(key, value);
                        break;
                    default: {
                        value = JSONTypeDeserializer.NUMBER.deserialize(source, buf, i, toIndex, jsonParseContext.useBigDecimalAsDefault ? GenericParameterizedType.BigDecimalType : GenericParameterizedType.AnyType, null, '}', jsonParseContext);
                        i = jsonParseContext.endIndex;
                        instance.put(key, value);
                        char next = buf[++i];
                        if (next == '}') {
                            jsonParseContext.endIndex = i;
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
                if (ch == ',') {
                    continue;
                }
                if (ch == '}') {
                    jsonParseContext.endIndex = i;
                    return instance;
                }
                String errorContextTextAt = createErrorContextText(buf, i);
                throw new JSONException("Syntax error, at pos " + i + ", context text by '" + errorContextTextAt + "', unexpected '" + ch + "', expected ',' or '}'");
            } else {
                throw new JSONException("Syntax error, at pos " + i + ", unexpected '" + ch + "', token ':' is expected.");
            }
        }
    }

//    static String parseJSONString(CharSource charSource, char[] buf, int from, int toIndex, char endCh, JSONParseContext jsonParseContext) {
//        int beginIndex = from + 1;
//        JSONCharArrayWriter writer = null;
//        if (charSource != null) {
//            String source = charSource.input();
//            int endIndex = source.indexOf(endCh, beginIndex);
//            if (!jsonParseContext.checkEscapeUseChar(source, beginIndex, endIndex)) {
//                jsonParseContext.endIndex = endIndex;
//                return new String(buf, beginIndex, endIndex - beginIndex);
//            }
//            // must exist \\ in range {beginIndex, endIndex}
//            writer = getContextWriter(jsonParseContext);
//            do {
//                int escapeIndex = jsonParseContext.getEscapeOffset();
//                beginIndex = escapeNext(buf, buf[escapeIndex + 1], escapeIndex, beginIndex, writer, jsonParseContext);
//                if (beginIndex > endIndex) {
//                    endIndex = source.indexOf(endCh, endIndex + 1);
//                }
//            } while (jsonParseContext.checkEscapeUseChar(source, beginIndex, endIndex));
//
//            jsonParseContext.endIndex = endIndex;
//            writer.write(charSource.input(), beginIndex, endIndex - beginIndex);
//            return writer.toString();
//        }
//        return (String) JSONTypeDeserializer.CHAR_SEQUENCE_STRING.deserializeString(charSource, buf, from, toIndex, endCh, GenericParameterizedType.StringType, jsonParseContext);
//        char ch, next;
//        int i = beginIndex;
//        int len;
//        boolean escape = false;
//        for (; ; ++i) {
//            while ((ch = buf[i]) != '\\' && ch != endCh) {
//                ++i;
//            }
//            // ch is \\ or "
//            if (ch == '\\') {
//                next = buf[i + 1];
//                if (writer == null) {
//                    writer = getContextWriter(jsonParseContext);
//                    escape = true;
//                }
//                beginIndex = escapeNext(buf, next, i, beginIndex, writer, jsonParseContext);
//                i = jsonParseContext.endIndex;
//            } else {
//                jsonParseContext.endIndex = i;
//                len = i - beginIndex;
//                if (escape) {
//                    writer.write(buf, beginIndex, len);
//                    return writer.toString();
//                } else {
//                    return new String(buf, beginIndex, len);
//                }
//            }
//        }
//    }

    static String parseMapKeyByCache(char[] buf, int from, int toIndex, char endCh, JSONParseContext jsonParseContext) {
        int beginIndex = from + 1;
        char ch;
        int i = from;
        int len;
        JSONCharArrayWriter writer = null;
        boolean escape = false;
        boolean ascii = true;
        for (; ; ) {
            long hashValue = ESCAPE;
            while ((ch = buf[++i]) != '\\' && ch != endCh) {
                if (ch > 0xFF) {
                    hashValue = (hashValue << 16) | ch;
                    ascii = false;
                } else {
                    hashValue = hashValue << 8 | ch;
                }
            }
            if (ch == '\\') {
                if (writer == null) {
                    writer = getContextWriter(jsonParseContext);
                }
                escape = true;
                beginIndex = escapeNext(buf, buf[i + 1], i, beginIndex, writer, jsonParseContext);
                i = jsonParseContext.endIndex;
            } else {
                jsonParseContext.endIndex = i;
                len = i - beginIndex;
                if (escape) {
                    writer.write(buf, beginIndex, len);
                    return writer.toString();
                } else {
                    if (ascii && len <= 8) {
                        return jsonParseContext.getCacheEightCharsKey(buf, beginIndex, len, hashValue);
                    }
                    return jsonParseContext.getCacheKey(buf, beginIndex, len, hashValue);
                }
            }
        }
    }

    // bytes #####################################################################################################
    static Object parseInternal(CharSource source, byte[] bytes, int fromIndex, int toIndex, Object defaultValue, ReadOption... readOptions) {
        byte beginByte = '\0';
        while ((fromIndex < toIndex) && (beginByte = bytes[fromIndex]) <= ' ') {
            fromIndex++;
        }
        while ((toIndex > fromIndex) && bytes[toIndex - 1] <= ' ') {
            toIndex--;
        }
        JSONParseContext jsonParseContext = new JSONParseContext();
        JSONOptions.readOptions(readOptions, jsonParseContext);
        try {
            boolean allowComment = jsonParseContext.allowComment;
            if (allowComment && beginByte == '/') {
                fromIndex = clearCommentAndWhiteSpaces(bytes, fromIndex + 1, toIndex, jsonParseContext);
                beginByte = bytes[fromIndex];
            }
            Object result;
            switch (beginByte) {
                case '{':
                    result = parseJSONObject(source, bytes, fromIndex, toIndex, defaultValue == null ? new LinkedHashMap() : (Map) defaultValue, jsonParseContext);
                    break;
                case '[':
                    result = parseJSONArray(source, bytes, fromIndex, toIndex, defaultValue == null ? new ArrayList() : (Collection) defaultValue, jsonParseContext);
                    break;
                case '\'':
                case '"':
                    result = JSONTypeDeserializer.CHAR_SEQUENCE_STRING.deserializeString(source, bytes, fromIndex, toIndex, beginByte, GenericParameterizedType.StringType, jsonParseContext);
                    break;
                default:
                    throw new UnsupportedOperationException("Unsupported for begin character with '" + beginByte + "'");
            }

            int endIndex = jsonParseContext.endIndex;

            if (allowComment) {
                if (endIndex < toIndex - 1) {
                    char commentStart = '\0';
                    while (endIndex + 1 < toIndex && (commentStart = (char) bytes[++endIndex]) <= ' ') ;
                    if (commentStart == '/') {
                        endIndex = clearCommentAndWhiteSpaces(bytes, endIndex + 1, toIndex, jsonParseContext);
                    }
                }
            }
            if (endIndex != toIndex - 1) {
                int wordNum = Math.min(50, bytes.length - endIndex - 1);
                String errorContextTextAt = createErrorContextText(bytes, endIndex + 1);
                throw new JSONException("Syntax error, at pos " + endIndex + ", context text by '" + errorContextTextAt + "', extra characters found, '" + new String(bytes, endIndex + 1, wordNum) + " ...'");
            }
            return result;
        } catch (Exception ex) {
            handleCatchException(ex, bytes, toIndex);
            throw new JSONException("Error: " + ex.getMessage(), ex);
        } finally {
            jsonParseContext.clear();
        }
    }

    static Collection parseJSONArray(CharSource source, byte[] bytes, int fromIndex, int toIndex, Collection list, JSONParseContext jsonParseContext) throws Exception {
        byte b;
        int i = fromIndex;
        for (; ; ) {
            while ((b = bytes[++i]) <= ' ') ;
            if (jsonParseContext.allowComment) {
                if (b == '/') {
                    b = bytes[i = clearCommentAndWhiteSpaces(bytes, i + 1, toIndex, jsonParseContext)];
                }
            }
            if (b == ']') {
                if (list.size() > 0 && !jsonParseContext.allowLastEndComma) {
                    throw new JSONException("Syntax error, at pos " + i + ", the closing symbol ']' is not allowed here.");
                }
                jsonParseContext.endIndex = i;
                return list;
            }
            Object value;
            switch (b) {
                case '{': {
                    value = parseJSONObject(source, bytes, i, toIndex, new LinkedHashMap(), jsonParseContext);
                    list.add(value);
                    i = jsonParseContext.endIndex;
                    break;
                }
                case '[': {
                    value = parseJSONArray(source, bytes, i, toIndex, new ArrayList(), jsonParseContext);
                    list.add(value);
                    i = jsonParseContext.endIndex;
                    break;
                }
                case '\'':
                case '"': {
                    value = JSONTypeDeserializer.CHAR_SEQUENCE_STRING.deserializeString(source, bytes, i, toIndex, b, GenericParameterizedType.StringType, jsonParseContext);
                    list.add(value);
                    i = jsonParseContext.endIndex;
                    break;
                }
                case 'n':
                    value = JSONTypeDeserializer.parseNull(bytes, i, toIndex, jsonParseContext);
                    i = jsonParseContext.endIndex;
                    list.add(value);
                    break;
                case 't':
                    value = JSONTypeDeserializer.parseTrue(bytes, i, toIndex, jsonParseContext);
                    i = jsonParseContext.endIndex;
                    list.add(value);
                    break;
                case 'f':
                    value = JSONTypeDeserializer.parseFalse(bytes, i, toIndex, jsonParseContext);
                    i = jsonParseContext.endIndex;
                    list.add(value);
                    break;
                default: {
                    value = JSONTypeDeserializer.NUMBER.deserialize(source, bytes, i, toIndex, jsonParseContext.useBigDecimalAsDefault ? GenericParameterizedType.BigDecimalType : GenericParameterizedType.AnyType, null, END_ARRAY, jsonParseContext);
                    i = jsonParseContext.endIndex;
                    list.add(value);
                    byte next = bytes[++i];
                    if (next == END_ARRAY) {
                        jsonParseContext.endIndex = i;
                        return list;
                    } else {
                        continue;
                    }
                }
            }
            while ((b = bytes[++i]) <= ' ') ;
            if (jsonParseContext.allowComment) {
                if (b == '/') {
                    b = bytes[i = clearCommentAndWhiteSpaces(bytes, i + 1, toIndex, jsonParseContext)];
                }
            }
            if (b == ',') {
                continue;
            }
            if (b == ']') {
                jsonParseContext.endIndex = i;
                return list;
            }
            String errorContextTextAt = createErrorContextText(bytes, i);
            throw new JSONException("Syntax error, at pos " + i + ", context text by '" + errorContextTextAt + "', unexpected '" + b + "', expected ',' or ']'");
        }
    }

    static Map parseJSONObject(CharSource source, byte[] bytes, int fromIndex, int toIndex, Map instance, JSONParseContext jsonParseContext) throws Exception {
        byte b;
        boolean empty = true, allowomment = jsonParseContext.allowComment, disableCacheMapKey = jsonParseContext.disableCacheMapKey;
        int i = fromIndex;
        for (; ; ) {
            while ((b = bytes[++i]) <= ' ') ;
            if (allowomment) {
                if (b == '/') {
                    b = bytes[i = clearCommentAndWhiteSpaces(bytes, i + 1, toIndex, jsonParseContext)];
                }
            }
            Serializable key;
            int fieldKeyFrom = i;
            if (b == '"') {
                key = disableCacheMapKey ? JSONTypeDeserializer.parseMapKey(bytes, i, toIndex, '"', jsonParseContext) : JSONTypeDeserializer.parseMapKeyByCache(bytes, i, toIndex, '"', jsonParseContext);
                i = jsonParseContext.endIndex;
                empty = false;
                ++i;
            } else {
                if (b == '}') {
                    if (!empty && !jsonParseContext.allowLastEndComma) {
                        throw new JSONException("Syntax error, at pos " + i + ", the closing symbol '}' is not allowed here.");
                    }
                    jsonParseContext.endIndex = i;
                    return instance;
                }
                if (b == '\'') {
                    if (jsonParseContext.allowSingleQuotes) {
                        while (i + 1 < toIndex && (bytes[++i] != '\'' || bytes[i - 1] == '\\')) ;
                        empty = false;
                        ++i;
                        key = parseKeyOfMap(bytes, fieldKeyFrom, i, false);
                    } else {
                        throw new JSONException("Syntax error, at pos " + i + ", the single quote symbol ' is not allowed here.");
                    }
                } else {
                    if (jsonParseContext.allowUnquotedFieldNames) {
                        while (i + 1 < toIndex && bytes[++i] != ':') ;
                        empty = false;
                        key = parseKeyOfMap(bytes, fieldKeyFrom, i, true);
                    } else {
                        int j = i;
                        boolean isNullKey = false;
                        key = null;
                        if (b == 'n' && bytes[++i] == 'u' && bytes[++i] == 'l' && bytes[++i] == 'l') {
                            isNullKey = true;
                            ++i;
                        }
                        if (!isNullKey) {
                            String errorContextTextAt = createErrorContextText(bytes, j);
                            throw new JSONException("Syntax error, at pos " + j + ", context text by '" + errorContextTextAt + "', unexpected '" + b + "', expected '\"' or use option ReadOption.AllowUnquotedFieldNames ");
                        }
                    }
                }
            }
            while ((b = bytes[i]) <= ' ') {
                ++i;
            }
            if (allowomment) {
                if (b == '/') {
                    b = bytes[i = clearCommentAndWhiteSpaces(bytes, i + 1, toIndex, jsonParseContext)];
                }
            }
            if (b == ':') {
                while ((b = bytes[++i]) <= ' ') ;
                if (jsonParseContext.allowComment) {
                    if (b == '/') {
                        b = bytes[i = clearCommentAndWhiteSpaces(bytes, i + 1, toIndex, jsonParseContext)];
                    }
                }
                Object value;
                switch (b) {
                    case '{': {
                        value = parseJSONObject(source, bytes, i, toIndex, new LinkedHashMap(), jsonParseContext);
                        i = jsonParseContext.endIndex;
                        instance.put(key, value);
                        break;
                    }
                    case '[': {
                        value = parseJSONArray(source, bytes, i, toIndex, new ArrayList(), jsonParseContext);
                        i = jsonParseContext.endIndex;
                        instance.put(key, value);
                        break;
                    }
                    case '"':
                    case '\'': {
                        value = JSONTypeDeserializer.CHAR_SEQUENCE_STRING.deserializeString(source, bytes, i, toIndex, b, GenericParameterizedType.StringType, jsonParseContext);
                        i = jsonParseContext.endIndex;
                        instance.put(key, value);
                        break;
                    }
                    case 'n':
                        value = JSONTypeDeserializer.parseNull(bytes, i, toIndex, jsonParseContext);
                        i = jsonParseContext.endIndex;
                        instance.put(key, value);
                        break;
                    case 't':
                        value = JSONTypeDeserializer.parseTrue(bytes, i, toIndex, jsonParseContext);
                        i = jsonParseContext.endIndex;
                        instance.put(key, value);
                        break;
                    case 'f':
                        value = JSONTypeDeserializer.parseFalse(bytes, i, toIndex, jsonParseContext);
                        i = jsonParseContext.endIndex;
                        instance.put(key, value);
                        break;
                    default: {
                        value = JSONTypeDeserializer.NUMBER.deserialize(source, bytes, i, toIndex, jsonParseContext.useBigDecimalAsDefault ? GenericParameterizedType.BigDecimalType : GenericParameterizedType.AnyType, null, END_OBJECT, jsonParseContext);
                        i = jsonParseContext.endIndex;
                        instance.put(key, value);
                        byte next = bytes[++i];
                        if (next == END_OBJECT) {
                            jsonParseContext.endIndex = i;
                            return instance;
                        } else {
                            continue;
                        }
                    }
                }
                while ((b = bytes[++i]) <= ' ') ;
                if (allowomment) {
                    if (b == '/') {
                        b = bytes[i = clearCommentAndWhiteSpaces(bytes, i + 1, toIndex, jsonParseContext)];
                    }
                }
                if (b == COMMA) {
                    continue;
                }
                if (b == END_OBJECT) {
                    jsonParseContext.endIndex = i;
                    return instance;
                }
                String errorContextTextAt = createErrorContextText(bytes, i);
                throw new JSONException("Syntax error, at pos " + i + ", context text by '" + errorContextTextAt + "', unexpected token '" + (char) b + "', expected ',' or '}'");
            } else {
                throw new JSONException("Syntax error, at pos " + i + ", unexpected '" + (char) b + "', colon ':' is expected.");
            }
        }
    }
}
