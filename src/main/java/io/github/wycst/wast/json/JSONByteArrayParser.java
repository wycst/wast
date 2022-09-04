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

import io.github.wycst.wast.json.exceptions.JSONException;
import io.github.wycst.wast.json.options.JSONParseContext;
import io.github.wycst.wast.json.options.Options;
import io.github.wycst.wast.json.options.ReadOption;

import java.io.Serializable;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * <p> 字节数组解析;
 * <p> 经过验证utf-8似乎也不需要解码也可以适用,所有的字符token都在一个字节范围内，但字符串不能使用版本场景化（比如unsafe）的方式构建，使用通用的new构造器构建；
 * <p> 对于正确的json字节数组一定能正确解析（否则是bug层面问题不是设计问题），但不保证对于错误的json字节数组能精确校验出问题；
 *
 * @Author wangyunchao
 * @Date 2022/9/2 18:28
 */
class JSONByteArrayParser extends JSONGeneral {

    /**
     * 解析bytes返回Map或者List
     *
     * @param bytes
     * @param readOptions
     * @return
     */
    public static Object parse(byte[] bytes, ReadOption... readOptions) {
        return parse(bytes, null, readOptions);
    }

    static Object parse(byte[] bytes, Object defaultValue, ReadOption... readOptions) {
        bytes.getClass();
        return parse(bytes, 0, bytes.length, defaultValue, readOptions);
    }

    static Object parse(byte[] bytes, int fromIndex, int toIndex, Object defaultValue, ReadOption... readOptions) {
        // Trim remove white space characters
        byte beginByte = '\0';
        while ((fromIndex < toIndex) && (beginByte = bytes[fromIndex]) <= ' ') {
            fromIndex++;
        }
        while ((toIndex > fromIndex) && bytes[toIndex - 1] <= ' ') {
            toIndex--;
        }

        JSONParseContext jsonParseContext = new JSONParseContext();
        Options.readOptions(readOptions, jsonParseContext);
        try {

            boolean allowComment = jsonParseContext.isAllowComment();
            if (allowComment && beginByte == '/') {
                /** Remove comments declared in the header */
                fromIndex = clearComments(bytes, fromIndex + 1, toIndex, jsonParseContext);
                beginByte = bytes[fromIndex];
            }
            Object result;
            switch (beginByte) {
                case '{':
                    result = parseJSONObject(bytes, fromIndex, toIndex, defaultValue == null ? new LinkedHashMap() : (Map) defaultValue, jsonParseContext);
                    break;
                case '[':
                    result = parseJSONArray(bytes, fromIndex, toIndex, defaultValue == null ? new ArrayList() : (Collection) defaultValue, jsonParseContext);
                    break;
                case '"':
                    result = parseJSONString(bytes, fromIndex, toIndex, beginByte, jsonParseContext);
                    break;
                default:
                    throw new UnsupportedOperationException("Unsupported for begin character with '" + beginByte + "'");
            }

            int endIndex = jsonParseContext.getEndIndex();

            if (allowComment) {
                /** Remove comments at the end of the declaration */
                if (endIndex < toIndex - 1) {
                    char commentStart = '\0';
                    while (endIndex + 1 < toIndex && (commentStart = (char) bytes[++endIndex]) <= ' ') ;
                    if (commentStart == '/') {
                        endIndex = clearComments(bytes, endIndex + 1, toIndex, jsonParseContext);
                    }
                }
            }
            if (endIndex != toIndex - 1) {
                int wordNum = Math.min(50, bytes.length - endIndex - 1);
                String errorContextTextAt = createErrorMessage(bytes, endIndex + 1);
                throw new JSONException("Syntax error, at pos " + endIndex + ", context text by '" + errorContextTextAt + "', extra characters found, '" + new String(bytes, endIndex + 1, wordNum) + " ...'");
            }
            return result;
        } catch (Exception ex) {
            if (ex instanceof JSONException) {
                throw (JSONException) ex;
            }
            // There is only one possibility to control out of bounds exceptions when indexing toindex
            if (ex instanceof IndexOutOfBoundsException) {
                ex.printStackTrace();
                String errorContextTextAt = createErrorMessage(bytes, toIndex);
                throw new JSONException("Syntax error, context text by '" + errorContextTextAt + "', JSON format error, and the end token may be missing, such as '\"' or ', ' or '}' or ']'.");
            }
            throw new JSONException("Error: " + ex.getMessage(), ex);
        } finally {
            jsonParseContext.clear();
        }
    }

    static Collection parseJSONArray(byte[] bytes, int fromIndex, int toIndex, Collection list, JSONParseContext jsonParseContext) throws Exception {

        int beginIndex = fromIndex + 1;
        byte b;

        // The core token of the collection array is a comma
        for (int i = beginIndex; /*i < toIndex*/ ; ++i) {
            // clear white space characters
            while ((b = bytes[i]) <= ' ') {
                ++i;
            }
            // clear comment and whiteSpaces
            if (jsonParseContext.isAllowComment()) {
                if (b == '/') {
                    b = bytes[i = clearComments(bytes, i + 1, toIndex, jsonParseContext)];
                }
            }
            // empty set or exception
            if (b == ']') {
                if (list.size() > 0) {
                    throw new JSONException("Syntax error, at pos " + i + ", the closing symbol ']' is not allowed here.");
                }
                jsonParseContext.setEndIndex(i);
                return list;
            }
            Object value;
            switch (b) {
                case '{': {
                    value = parseJSONObject(bytes, i, toIndex, new LinkedHashMap(), jsonParseContext);
                    list.add(value);
                    i = jsonParseContext.getEndIndex();
                    break;
                }
                case '[': {
                    // 2 [ array
                    value = parseJSONArray(bytes, i, toIndex, new ArrayList(), jsonParseContext);
                    list.add(value);
                    i = jsonParseContext.getEndIndex();
                    break;
                }
                case '\'':
                case '"': {
                    // 3 string
                    // When there are escape characters, the escape character needs to be parsed
                    value = parseJSONString(bytes, i, toIndex, b, jsonParseContext);
                    list.add(value);
                    i = jsonParseContext.getEndIndex();
                    break;
                }
                case 'n':
                    value = parseNull(bytes, i, toIndex, jsonParseContext);
                    i = jsonParseContext.getEndIndex();
                    list.add(value);
                    break;
                case 't':
                    value = parseTrue(bytes, i, toIndex, jsonParseContext);
                    i = jsonParseContext.getEndIndex();
                    list.add(value);
                    break;
                case 'f':
                    value = parseFalse(bytes, i, toIndex, jsonParseContext);
                    i = jsonParseContext.getEndIndex();
                    list.add(value);
                    break;
                default: {
                    value = parseDefaultNumber(bytes, i, toIndex, (byte) ']', jsonParseContext);
                    i = jsonParseContext.getEndIndex();
                    list.add(value);
                    // either continue or return
                    ++i;
                    char next = (char) bytes[i];
                    if (next == ']') {
                        jsonParseContext.setEndIndex(i);
                        return list;
                    } else {
                        continue;
                    }
                }
            }
            // clear white space characters
            while ((b = bytes[++i]) <= ' ') ;
            // clear comment and whiteSpaces
            if (jsonParseContext.isAllowComment()) {
                if (b == '/') {
                    b = bytes[i = clearComments(bytes, i + 1, toIndex, jsonParseContext)];
                }
            }
            // （Check whether the next character is a comma or end symbol. If yes, continue or return . If not, throw an exception）
            if (b == ',') {
                continue;
            }
            if (b == ']') {
                jsonParseContext.setEndIndex(i);
                return list;
            }
            String errorContextTextAt = createErrorMessage(bytes, i);
            throw new JSONException("Syntax error, at pos " + i + ", context text by '" + errorContextTextAt + "', unexpected token character '" + b + "', expected ',' or ']'");
        }
//        throw new JSONException("Syntax error, cannot find closing symbol ']' matching '['");
    }

    static Map parseJSONObject(byte[] bytes, int fromIndex, int toIndex, Map instance, JSONParseContext jsonParseContext) throws Exception {

        int beginIndex = fromIndex + 1;
        byte b;

        boolean empty = true;
        boolean allowomment = jsonParseContext.isAllowComment();
        boolean disableCacheMapKey = jsonParseContext.isDisableCacheMapKey();
        // for loop to parse
        for (int i = beginIndex; /*i < toIndex*/ ; ++i) {
            // clear white space characters
            while ((b = bytes[i]) <= ' ') {
                ++i;
            }
            // clear comment and whiteSpaces
            if (allowomment) {
                if (b == '/') {
                    b = bytes[i = clearComments(bytes, i + 1, toIndex, jsonParseContext)];
                }
            }
            Serializable key;
            int fieldKeyFrom = i;
            // Standard JSON field name with "
            if (b == '"') {
//                key = (Serializable) JSONTypeDeserializer.STRING.parseString(bytes, i, toIndex, GenericParameterizedType.StringType, jsonParseContext);
                key = disableCacheMapKey ? parseMapKey(bytes, i, toIndex, '"', jsonParseContext) : parseMapKeyByCache(bytes, i, toIndex, '"', jsonParseContext);
                i = jsonParseContext.getEndIndex();
                empty = false;
                ++i;
            } else {
                // empty object or exception
                if (b == '}') {
                    if (!empty) {
                        throw new JSONException("Syntax error, at pos " + i + ", the closing symbol '}' is not allowed here.");
                    }
                    jsonParseContext.setEndIndex(i);
                    return instance;
                }
                if (b == '\'') {
                    if (jsonParseContext.isAllowSingleQuotes()) {
                        while (i + 1 < toIndex && (bytes[++i] != '\'' || bytes[i - 1] == '\\')) ;
                        empty = false;
                        ++i;
                        key = parseKeyOfMap(bytes, fieldKeyFrom, i, false);
                    } else {
                        throw new JSONException("Syntax error, at pos " + i + ", the single quote symbol ' is not allowed here.");
                    }
                } else {
                    if (jsonParseContext.isAllowUnquotedFieldNames()) {
                        while (i + 1 < toIndex && bytes[++i] != ':') ;
                        empty = false;
                        key = parseKeyOfMap(bytes, fieldKeyFrom, i, true);
                        if (key.equals("null")) {
                            key = null;
                        }
                    } else {
                        int j = i;
                        boolean isNullKey = false;
                        key = null;
                        if (b == 'n' && bytes[++i] == 'u' && bytes[++i] == 'l' && bytes[++i] == 'l') {
                            isNullKey = true;
                            ++i;
                        }
                        if (!isNullKey) {
                            String errorContextTextAt = createErrorMessage(bytes, j);
                            throw new JSONException("Syntax error, at pos " + j + ", context text by '" + errorContextTextAt + "', unexpected token character '" + b + "', expected '\"' or use option ReadOption.AllowUnquotedFieldNames ");
                        }
                    }
                }

            }

            // clear white space characters
            while ((b = bytes[i]) <= ' ') {
                ++i;
            }
            // clear comment and whiteSpaces
            if (allowomment) {
                if (b == '/') {
                    b = bytes[i = clearComments(bytes, i + 1, toIndex, jsonParseContext)];
                }
            }
            // Standard JSON rules:
            // 1 if matched, it can only be followed by a colon, and all other symbols are wrong
            // 2 after the attribute value is parsed, it can only be followed by a comma(,) or end, and other symbols are wrong
            if (b == ':') {
                // clear white space characters
                while ((b = bytes[++i]) <= ' ') ;

                // clear comment and whiteSpaces
                if (jsonParseContext.isAllowComment()) {
                    if (b == '/') {
                        b = bytes[i = clearComments(bytes, i + 1, toIndex, jsonParseContext)];
                    }
                }
                Object value;
                switch (b) {
                    case '{': {
                        value = parseJSONObject(bytes, i, toIndex, new LinkedHashMap(), jsonParseContext);
                        i = jsonParseContext.getEndIndex();
                        instance.put(key, value);
                        break;
                    }
                    case '[': {
                        // 2 [ array
                        value = parseJSONArray(bytes, i, toIndex, new ArrayList(), jsonParseContext);
                        i = jsonParseContext.getEndIndex();
                        instance.put(key, value);
                        break;
                    }
                    case '"':
                    case '\'': {
                        value = parseJSONString(bytes, i, toIndex, b, jsonParseContext);
                        i = jsonParseContext.getEndIndex();
                        instance.put(key, value);
                        break;
                    }
                    case 'n':
                        value = parseNull(bytes, i, toIndex, jsonParseContext);
                        i = jsonParseContext.getEndIndex();
                        instance.put(key, value);
                        break;
                    case 't':
                        value = parseTrue(bytes, i, toIndex, jsonParseContext);
                        i = jsonParseContext.getEndIndex();
                        instance.put(key, value);
                        break;
                    case 'f':
                        value = parseFalse(bytes, i, toIndex, jsonParseContext);
                        i = jsonParseContext.getEndIndex();
                        instance.put(key, value);
                        break;
                    default: {
                        value = parseDefaultNumber(bytes, i, toIndex, (byte) '}', jsonParseContext);
                        i = jsonParseContext.getEndIndex();
                        instance.put(key, value);
                        char next = (char) bytes[++i];
                        if (next == '}') {
                            jsonParseContext.setEndIndex(i);
                            return instance;
                        } else {
                            continue;
                        }
                    }
                }
                // clear white space characters
                while ((b = bytes[++i]) <= ' ') ;
                // clear comment and whiteSpaces
                if (allowomment) {
                    if (b == '/') {
                        b = bytes[i = clearComments(bytes, i + 1, toIndex, jsonParseContext)];
                    }
                }
                // Check whether the next character is a comma or end symbol '}'. If yes, continue or break. If not, throw an exception
                if (b == ',') {
                    continue;
                }
                if (b == '}') {
                    jsonParseContext.setEndIndex(i);
                    return instance;
                }
                String errorContextTextAt = createErrorMessage(bytes, i);
                throw new JSONException("Syntax error, at pos " + i + ", context text by '" + errorContextTextAt + "', unexpected token character '" + b + "', expected ',' or '}'");
            } else {
                throw new JSONException("Syntax error, at pos " + i + ", unexpected token character '" + b + "', Colon character ':' is expected.");
            }
        }
//        throw new JSONException("Syntax error, the closing symbol '}' is not found ");
    }

    static String parseJSONString(byte[] bytes, int from, int toIndex, byte endCh, JSONParseContext jsonParseContext) {
        int beginIndex = from + 1;
        byte b = '\0', next = '\0';
        int i = beginIndex;
        int len;

        JSONStringWriter writer = null;
        boolean escape = false;

        for (; /*i < toIndex*/ ; ++i) {
            while (/*i < toIndex &&*/ (b = bytes[i]) != '\\' && b != endCh) {
                ++i;
            }
            // ch is \\ or "
            if (b == '\\') {
                if (i < toIndex - 1) {
                    next = bytes[i + 1];
                }
                if (writer == null) {
                    writer = getContextWriter(jsonParseContext);
                    escape = true;
                }
                beginIndex = escape(bytes, next, i, beginIndex, writer, jsonParseContext);
                i = jsonParseContext.getEndIndex();
            } else {
                jsonParseContext.setEndIndex(i);
                len = i - beginIndex;
                if (escape) {
                    writer.writeBytes(bytes, beginIndex, len);
                    return writer.toString();
                } else {
                    return len == 0 ? "" : new String(bytes, beginIndex, len);
                }
            }
        }
    }

    /**
     * 反序列化null
     *
     * @return
     */
    static Object parseNull(byte[] bytes, int fromIndex, int toIndex, JSONParseContext jsonParseContext) {
        int endIndex = fromIndex + 3;
        if (bytes[fromIndex + 1] == 'u' && bytes[fromIndex + 2] == 'l' && bytes[endIndex] == 'l') {
            jsonParseContext.setEndIndex(endIndex);
            return null;
        }
        throw new JSONException("Syntax error, at pos " + fromIndex + ", expected 'null' because it starts with 'n', but found text '" + new String(bytes, fromIndex, Math.min(toIndex - fromIndex + 1, 4)) + "'");
    }

    /**
     * 反序列化true
     *
     * @param bytes
     * @param fromIndex
     * @param toIndex
     * @param jsonParseContext
     * @return
     * @throws Exception
     */
    static Object parseTrue(byte[] bytes, int fromIndex, int toIndex, JSONParseContext jsonParseContext) throws Exception {
        int endIndex = fromIndex + 3;
        // True only needs to read 4 characters to match true, and there is no need to read the comma or} position
        if (bytes[fromIndex + 1] == 'r' && bytes[fromIndex + 2] == 'u' && bytes[endIndex] == 'e') {
            jsonParseContext.setEndIndex(endIndex);
            return true;
        }
        int len = Math.min(toIndex - fromIndex + 1, 4);
        throw new JSONException("Syntax error, at pos " + fromIndex + ", expected 'true' because it starts with 't', but found text '" + new String(bytes, fromIndex, len) + "'");
    }

    /**
     * 反序列化false
     *
     * @param bytes
     * @param fromIndex
     * @param toIndex
     * @param jsonParseContext
     * @return
     * @throws Exception
     */
    static Object parseFalse(byte[] bytes, int fromIndex, int toIndex, JSONParseContext jsonParseContext) throws Exception {
        int endIndex = fromIndex + 4;
        // False you only need to read 5 characters to match false, and you do not need to read the comma or} position
        if (bytes[fromIndex + 1] == 'a'
                && bytes[fromIndex + 2] == 'l'
                && bytes[fromIndex + 3] == 's'
                && bytes[endIndex] == 'e') {
            jsonParseContext.setEndIndex(endIndex);
            return false;
        }
        int len = Math.min(toIndex - fromIndex + 1, 5);
        throw new JSONException("Syntax error, at pos " + fromIndex + ", expected 'false' because it starts with 'f', but found text '" + new String(bytes, fromIndex, len) + "'");
    }

    static Number parseDefaultNumber(byte[] bytes, int fromIndex, int toIndex, byte endToken, JSONParseContext jsonParseContext) throws Exception {

        boolean negative = false;
        int i = fromIndex;
        byte b = bytes[fromIndex];
        if (b == '-') {
            negative = true;
            ++i;
        } else if (b == '+') {
            ++i;
        }
        double value = 0;
        int numberLen = 0;
        int decimalCount = 0;
        final int radix = 10;
        int expValue = 0;
        boolean expNegative = false;
        // init integer type
        int mode = 0;
        // number suffix
        int specifySuffix = 0;

        // calculation
        while (true) {
            b = bytes[i];
            int digit = digitDecimal((char) b);
            if (digit == -1) {
                if (b == ',' || b == endToken) {
                    break;
                }
                if (b == '.') {
                    if (mode != 0) {
                        throw new JSONException("For input string: \"" + new String(bytes, fromIndex, i + 1 - fromIndex) + "\"");
                    }
                    // 小数点模式
                    mode = 1;
                    b = bytes[++i];
                    digit = digitDecimal((char) b);
                } else if (b == 'E' || b == 'e') {
                    if (mode == 2) {
                        throw new JSONException("For input string: \"" + new String(bytes, fromIndex, i + 1 - fromIndex) + "\"");
                    }
                    // 科学计数法
                    mode = 2;
                    b = bytes[++i];
                    if (b == '-') {
                        expNegative = true;
                        b = bytes[++i];
                    }
                    digit = digitDecimal((char) b);
                }
            }

            if (digit == -1) {
                boolean breakLoop = false;
                switch (b) {
                    case 'l':
                    case 'L': {
                        if (specifySuffix == 0) {
                            specifySuffix = 1;
                            while ((b = bytes[++i]) <= ' ') ;
                            if (b == ',' || b == endToken) {
                                breakLoop = true;
                                break;
                            }
                        }
                        String contextErrorAt = createErrorMessage(bytes, i);
                        throw new JSONException("For input string: \"" + new String(bytes, fromIndex, i - fromIndex + 1) + "\", expected ',' or '" + endToken + "', but found '" + b + "', context text by '" + contextErrorAt + "'");
                    }
                    case 'f':
                    case 'F': {
                        if (specifySuffix == 0) {
                            specifySuffix = 2;
                            while ((b = bytes[++i]) <= ' ') ;
                            if (b == ',' || b == endToken) {
                                breakLoop = true;
                                break;
                            }
                        }
                        String contextErrorAt = createErrorMessage(bytes, i);
                        throw new JSONException("For input string: \"" + new String(bytes, fromIndex, i - fromIndex + 1) + "\", expected ',' or '" + endToken + "', but found '" + b + "', context text by '" + contextErrorAt + "'");
                    }
                    case 'd':
                    case 'D': {
                        if (specifySuffix == 0) {
                            specifySuffix = 3;
                            while ((b = bytes[++i]) <= ' ') ;
                            if (b == ',' || b == endToken) {
                                breakLoop = true;
                                break;
                            }
                        }
                        String contextErrorAt = createErrorMessage(bytes, i);
                        throw new JSONException("For input string: \"" + new String(bytes, fromIndex, i - fromIndex + 1) + "\", expected ',' or '" + endToken + "', but found '" + b + "', context text by '" + contextErrorAt + "'");
                    }
                    default: {
                        if (b <= ' ') {
                            while ((b = bytes[++i]) <= ' ') ;
                            if (b == ',' || b == endToken) {
                                breakLoop = true;
                                break;
                            }
                            String contextErrorAt = createErrorMessage(bytes, i);
                            throw new JSONException("For input string: \"" + new String(bytes, fromIndex, i - fromIndex + 1) + "\", expected ',' or '" + endToken + "', but found '" + b + "', context text by '" + contextErrorAt + "'");
                        }
                    }
                }
                if (breakLoop) {
                    break;
                }
                String contextErrorAt = createErrorMessage(bytes, i);
                throw new JSONException("For input string: \"" + new String(bytes, fromIndex, i - fromIndex + 1) + "\", at pos " + i + ", context text by '" + contextErrorAt + "'");
            }
            numberLen++;
            switch (mode) {
                case 0:
                    value *= radix;
                    value += digit;
                    break;
                case 1:
                    value *= radix;
                    value += digit;
                    decimalCount++;
                    break;
                case 2:
                    expValue = (expValue << 3) + (expValue << 1);
                    expValue += digit;
                    break;
            }
            ++i;
        }

        // end
        jsonParseContext.setEndIndex(i - 1);

        // int / long / BigInteger
        if (mode == 0) {
            if (numberLen > 18) {
                // BigInteger
                // maybe Performance loss
                int endIndex = i - 1;
                while (bytes[endIndex] <= ' ') {
                    endIndex--;
                }
                return new BigInteger(new String(bytes, fromIndex, endIndex - fromIndex + 1));
            }
            value = negative ? -value : value;
            if (specifySuffix > 0) {
                switch (specifySuffix) {
                    case 1:
                        return (long) value;
                    case 2:
                        return (float) value;
                }
                return value;
            }
            if (value <= Integer.MAX_VALUE && value > Integer.MIN_VALUE) {
                return (int) value;
            }
            if (value <= Long.MAX_VALUE && value > Long.MIN_VALUE) {
                return (long) value;
            }
            return value;
        } else {
            expValue = expNegative ? -expValue - decimalCount : expValue - decimalCount;
            if (expValue > 0) {
                double powValue = getDecimalPowerValue(expValue); // Math.pow(radix, expValue);
                value *= powValue;
            } else if (expValue < 0) {
                double powValue = getDecimalPowerValue(-expValue);// Math.pow(radix, -expValue);
                value /= powValue;
            }
            value = negative ? -value : value;
            if (specifySuffix > 0) {
                switch (specifySuffix) {
                    case 1:
                        return (long) value;
                    case 2:
                        return (float) value;
                }
                return value;
            }
            return value;
        }
    }

    static String parseMapKey(byte[] bytes, int from, int toIndex, char endCh, JSONParseContext jsonParseContext) {
        int beginIndex = from + 1;
        byte b = '\0', next = '\0';
        int i = beginIndex;
        int len;

        JSONStringWriter writer = null;
        boolean escape = false;
        for (; ; ++i) {
            while ((b = bytes[i]) != '\\' && b != endCh) {
                ++i;
            }
            // b is \\ or "
            if (b == '\\') {
                if (i < toIndex - 1) {
                    next = bytes[i + 1];
                }
                if (writer == null) {
                    writer = getContextWriter(jsonParseContext);
                }
                escape = true;
                beginIndex = escape(bytes, next, i, beginIndex, writer, jsonParseContext);
                i = jsonParseContext.getEndIndex();
            } else {
                jsonParseContext.setEndIndex(i);
                len = i - beginIndex;
                if (escape) {
                    writer.writeBytes(bytes, beginIndex, len);
                    return writer.toString();
                } else {
                    return len == 0 ? "" : new String(bytes, beginIndex, len);
                }
            }
        }
    }

    static String parseMapKeyByCache(byte[] bytes, int from, int toIndex, char endCh, JSONParseContext jsonParseContext) {
        int beginIndex = from + 1;
        byte b, next = 0;
        int i = beginIndex;
        int len;

        JSONStringWriter writer = null;
        boolean escape = false;
        for (; ; ++i) {
            int hashValue = 0;
            while ((b = bytes[i]) != '\\' && b != endCh) {
                ++i;
                hashValue = hashValue * 31 + b;
            }
            // ch is \\ or "
            if (b == '\\') {
                if (i < toIndex - 1) {
                    next = bytes[i + 1];
                }
                if (writer == null) {
                    writer = getContextWriter(jsonParseContext);
                }
                escape = true;
                beginIndex = escape(bytes, next, i, beginIndex, writer, jsonParseContext);
                i = jsonParseContext.getEndIndex();
            } else {
                jsonParseContext.setEndIndex(i);
                len = i - beginIndex;
                if (escape) {
                    writer.writeBytes(bytes, beginIndex, len);
                    return writer.toString();
                } else {
                    return len == 0 ? "" : jsonParseContext.getCacheKey(bytes, beginIndex, len, hashValue);
                }
            }
        }
    }

    /**
     * @param from
     * @param to
     * @param bytes
     * @param isUnquotedFieldName
     * @return
     */
    static String parseKeyOfMap(byte[] bytes, int from, int to, boolean isUnquotedFieldName) {
        if (isUnquotedFieldName) {
            while ((from < to) && ((bytes[from]) <= ' ')) {
                from++;
            }
            while ((to > from) && ((bytes[to - 1]) <= ' ')) {
                to--;
            }
            return new String(bytes, from, to - from);
        } else {
            int len = to - from - 2;
            return new String(bytes, from + 1, len);
        }
    }

    /**
     * escape next
     *
     * @param bytes
     * @param next
     * @param i
     * @param beginIndex
     * @param writer
     * @param jsonParseContext
     * @return
     */
    static int escape(byte[] bytes, byte next, int i, int beginIndex, JSONStringWriter writer, JSONParseContext jsonParseContext) {
        int len;
        switch (next) {
            case '\'':
            case '"':
                if (i > beginIndex) {
                    writer.writeBytes(bytes, beginIndex, i - beginIndex + 1);
                    writer.setCharAt(writer.size() - 1, (char) next);
                } else {
                    writer.append((char) next);
                }
                beginIndex = ++i + 1;
                break;
            case 'n':
                len = i - beginIndex;
                writer.writeBytes(bytes, beginIndex, len + 1);
                writer.setCharAt(writer.size() - 1, '\n');
                beginIndex = ++i + 1;
                break;
            case 'r':
                len = i - beginIndex;
                writer.writeBytes(bytes, beginIndex, len + 1);
                writer.setCharAt(writer.size() - 1, '\r');
                beginIndex = ++i + 1;
                break;
            case 't':
                len = i - beginIndex;
                writer.writeBytes(bytes, beginIndex, len + 1);
                writer.setCharAt(writer.size() - 1, '\t');
                beginIndex = ++i + 1;
                break;
            case 'b':
                len = i - beginIndex;
                writer.writeBytes(bytes, beginIndex, len + 1);
                writer.setCharAt(writer.size() - 1, '\b');
                beginIndex = ++i + 1;
                break;
            case 'f':
                len = i - beginIndex;
                writer.writeBytes(bytes, beginIndex, len + 1);
                writer.setCharAt(writer.size() - 1, '\f');
                beginIndex = ++i + 1;
                break;
            case 'u':
                len = i - beginIndex;
                writer.writeBytes(bytes, beginIndex, len + 1);

                int c;
                int j = i + 2;
                try {
                    int c1 = hex(bytes[j++]);
                    int c2 = hex(bytes[j++]);
                    int c3 = hex(bytes[j++]);
                    int c4 = hex(bytes[j++]);
                    c = (c1 << 12) | (c2 << 8) | (c3 << 4) | c4;
                } catch (Throwable throwable) {
                    // \\u parse error
                    String errorContextTextAt = createErrorMessage(bytes, i + 1);
                    throw new JSONException("Syntax error, from pos " + (i + 1) + ", context text by '" + errorContextTextAt + "', " + throwable.getMessage());
                }

                writer.setCharAt(writer.size() - 1, (char) c);
                i += 4;
                beginIndex = ++i + 1;
                break;
            default: {
                // other case delete char '\\'
                len = i - beginIndex;
                writer.writeBytes(bytes, beginIndex, len + 1);
                writer.setCharAt(writer.size() - 1, (char) next);
                beginIndex = ++i + 1;
            }
        }
        jsonParseContext.setEndIndex(i);
        return beginIndex;
    }

    /**
     * 报错位置取前后18个字符，最多一共40个字符(字节)
     */
    static String createErrorMessage(byte[] bytes, int at) {
        try {
            int len = bytes.length;
            byte[] text = new byte[40];
            int count;
            int begin = Math.max(at - 18, 0);
            System.arraycopy(bytes, begin, text, 0, count = at - begin);
            text[count++] = '^';
            int end = Math.min(len, at + 18);
            System.arraycopy(bytes, at, text, count, end - at);
            count += end - at;
            return new String(text, 0, count);
        } catch (Throwable throwable) {
            return "";
        }
    }

    /**
     * 清除注释和空白
     *
     * @param bytes
     * @param beginIndex       开始位置
     * @param toIndex          最大允许结束位置
     * @param jsonParseContext 上下文配置
     * @return 去掉注释后的第一个非空字节位置（Non empty character position after removing comments）
     * @see ReadOption#AllowComment
     */
    static int clearComments(byte[] bytes, int beginIndex, int toIndex, JSONParseContext jsonParseContext) {
        int i = beginIndex;
        if (i >= toIndex) {
            throw new JSONException("Syntax error, unexpected token character '/', position " + (beginIndex - 1));
        }
        // 注释和 /*注释
        // / or *
        byte b = bytes[beginIndex];
        if (b == '/') {
            // End with newline \ n
            while (i < toIndex && bytes[i] != '\n') {
                ++i;
            }
            // continue clear WhiteSpaces
            b = '\0';
            while (i + 1 < toIndex && (b = bytes[++i]) <= ' ') ;
            if (b == '/') {
                // 递归清除
                i = clearComments(bytes, i + 1, toIndex, jsonParseContext);
            }
        } else if (b == '*') {
            // End with */
            byte prev = 0;
            boolean matched = false;
            while (i + 1 < toIndex) {
                b = bytes[++i];
                if (b == '/' && prev == '*') {
                    matched = true;
                    break;
                }
                prev = b;
            }
            if (!matched) {
                throw new JSONException("Syntax error, not found the close comment '*/' util the end ");
            }
            // continue clear WhiteSpaces
            b = '\0';
            while (i + 1 < toIndex && (b = bytes[++i]) <= ' ') ;
            if (b == '/') {
                // 递归清除
                i = clearComments(bytes, i + 1, toIndex, jsonParseContext);
            }
        } else {
            throw new JSONException("Syntax error, unexpected token character '" + (char) b + "', position " + beginIndex);
        }
        return i;
    }
}
