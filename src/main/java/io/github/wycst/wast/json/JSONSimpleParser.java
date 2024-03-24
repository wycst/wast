//package io.github.wycst.wast.json;
//
//import io.github.wycst.wast.common.reflect.GenericParameterizedType;
//import io.github.wycst.wast.common.reflect.UnsafeHelper;
//import io.github.wycst.wast.json.exceptions.JSONException;
//import io.github.wycst.wast.json.JSONParseContext;
//
//import java.io.Serializable;
//import java.util.ArrayList;
//import java.util.Collection;
//import java.util.LinkedHashMap;
//import java.util.Map;
//
///**
// * 简单模式解析
// *
// * <p>
// * 适合场景
// * <p> 没有转义符的长字符串文本</p>
// * <p> 如果存在转义符但并不需要处理或者不关注转义字符的长字段； </p>
// *
// * @Author wangyunchao
// * @Date 2022/9/10 14:14
// */
//public final class JSONSimpleParser extends JSONGeneral {
//
//    /**
//     * return Map or List
//     *
//     * @param json
//     * @return Map or List
//     */
//    public static Object parse(String json) {
//        json.getClass();
//        if (StringCoder) {
//            int code = UnsafeHelper.getStringCoder(json);
//            if (code == 0) {
//                byte[] bytes = (byte[]) UnsafeHelper.getStringValue(json);
//                return parse(json, bytes);
//            }
//        }
//        return parse(json, getChars(json));
//    }
//
//    static Object parse(String source, char[] buf) {
//
//        int fromIndex = 0, toIndex = buf.length;
//
//        // Trim remove white space characters
//        char beginChar = '\0';
//        while ((fromIndex < toIndex) && (beginChar = buf[fromIndex]) <= ' ') {
//            fromIndex++;
//        }
//        while ((toIndex > fromIndex) && buf[toIndex - 1] <= ' ') {
//            toIndex--;
//        }
//        JSONParseContext jsonParseContext = new JSONParseContext();
//        try {
//            Object result;
//            switch (beginChar) {
//                case '{':
//                    result = parseJSONObject(source, buf, fromIndex, toIndex, new LinkedHashMap(), jsonParseContext);
//                    break;
//                case '[':
//                    result = parseJSONArray(source, buf, fromIndex, toIndex, new ArrayList(), jsonParseContext);
//                    break;
//                case '"':
//                    result = parseJSONString(source, buf, fromIndex, toIndex, beginChar, jsonParseContext);
//                    break;
//                default:
//                    throw new UnsupportedOperationException("Unsupported for begin character with '" + beginChar + "'");
//            }
//
//            int endIndex = jsonParseContext.endIndex;
//            if (endIndex != toIndex - 1) {
//                int wordNum = Math.min(50, buf.length - endIndex - 1);
//                String errorContextTextAt = createErrorContextText(buf, endIndex + 1);
//                throw new JSONException("Syntax error, at pos " + endIndex + ", context text by '" + errorContextTextAt + "', extra characters found, '" + new String(buf, endIndex + 1, wordNum) + " ...'");
//            }
//            return result;
//        } catch (Exception ex) {
//            handleCatchException(ex, buf, toIndex);
//            throw new JSONException("Error: " + ex.getMessage(), ex);
//        } finally {
//            jsonParseContext.clear();
//        }
//    }
//
//    static Collection parseJSONArray(String source, char[] buf, int fromIndex, int toIndex, Collection list, JSONParseContext jsonParseContext) throws Exception {
//
//        int beginIndex = fromIndex + 1;
//        char ch;
//
//        // The core token of the collection array is a comma
//        for (int i = beginIndex; /*i < toIndex*/ ; ++i) {
//            // clear white space characters
//            while ((ch = buf[i]) <= ' ') {
//                ++i;
//            }
//            // empty set or exception
//            if (ch == ']') {
//                if (list.size() > 0) {
//                    throw new JSONException("Syntax error, at pos " + i + ", the closing symbol ']' is not allowed here.");
//                }
//                jsonParseContext.endIndex = i;
//                return list;
//            }
//            Object value;
//            switch (ch) {
//                case '{': {
//                    value = parseJSONObject(source, buf, i, toIndex, new LinkedHashMap(), jsonParseContext);
//                    list.add(value);
//                    i = jsonParseContext.endIndex;
//                    break;
//                }
//                case '[': {
//                    // 2 [ array
//                    value = parseJSONArray(source, buf, i, toIndex, new ArrayList(), jsonParseContext);
//                    list.add(value);
//                    i = jsonParseContext.endIndex;
//                    break;
//                }
//                case '\'':
//                case '"': {
//                    // 3 string
//                    // When there are escape characters, the escape character needs to be parsed
//                    value = parseJSONString(source, buf, i, toIndex, ch, jsonParseContext);
//                    list.add(value);
//                    i = jsonParseContext.endIndex;
//                    break;
//                }
//                case 'n':
//                    value = JSONTypeDeserializer.NULL.deserialize(null, buf, i, toIndex, null, null, '\0', jsonParseContext);
//                    i = jsonParseContext.endIndex;
//                    list.add(value);
//                    break;
//                case 't':
//                    value = JSONTypeDeserializer.BOOLEAN.deserializeTrue(buf, i, toIndex, null, jsonParseContext);
//                    i = jsonParseContext.endIndex;
//                    list.add(value);
//                    break;
//                case 'f':
//                    value = JSONTypeDeserializer.BOOLEAN.deserializeFalse(buf, i, toIndex, null, jsonParseContext);
//                    i = jsonParseContext.endIndex;
//                    list.add(value);
//                    break;
//                default: {
//                    value = JSONTypeDeserializer.NUMBER.deserialize(null, buf, i, toIndex, GenericParameterizedType.AnyType, null, ']', jsonParseContext);
//                    i = jsonParseContext.endIndex;
//                    list.add(value);
//                    // either continue or return
//                    ++i;
//                    char next = buf[i];
//                    if (next == ']') {
//                        jsonParseContext.endIndex = i;
//                        return list;
//                    } else {
//                        continue;
//                    }
//                }
//            }
//            // clear white space characters
//            while ((ch = buf[++i]) <= ' ') ;
//            // （Check whether the next character is a comma or end symbol. If yes, continue or return . If not, throw an exception）
//            if (ch == ',') {
//                continue;
//            }
//            if (ch == ']') {
//                jsonParseContext.endIndex = i;
//                return list;
//            }
//            String errorContextTextAt = createErrorContextText(buf, i);
//            throw new JSONException("Syntax error, at pos " + i + ", context text by '" + errorContextTextAt + "', unexpected token character '" + ch + "', expected ',' or ']'");
//        }
////        throw new JSONException("Syntax error, cannot find closing symbol ']' matching '['");
//    }
//
//    static Map parseJSONObject(String source, char[] buf, int fromIndex, int toIndex, Map instance, JSONParseContext jsonParseContext) throws Exception {
//
//        int beginIndex = fromIndex + 1;
//        char ch;
//
//        boolean empty = true;
//        // for loop to parse
//        for (int i = beginIndex; /*i < toIndex*/ ; ++i) {
//            // clear white space characters
//            while ((ch = buf[i]) <= ' ') {
//                ++i;
//            }
//            Serializable key;
//            // Standard JSON field name with "
//            if (ch == '"') {
//                int endIndex = source.indexOf('"', i + 1);
//                key = new String(buf, i + 1, endIndex - i - 1);
//                i = endIndex;
//                empty = false;
//                ++i;
//            } else {
//                // empty object or exception
//                if (ch == '}') {
//                    if (!empty) {
//                        throw new JSONException("Syntax error, at pos " + i + ", the closing symbol '}' is not allowed here.");
//                    }
//                    jsonParseContext.endIndex = i;
//                    return instance;
//                }
//                boolean isNullKey = false;
//                int j = i;
//                key = null;
//                if (ch == 'n' && buf[++i] == 'u' && buf[++i] == 'l' && buf[++i] == 'l') {
//                    isNullKey = true;
//                    ++i;
//                }
//                if (!isNullKey) {
//                    String errorContextTextAt = createErrorContextText(buf, j);
//                    throw new JSONException("Syntax error, at pos " + j + ", context text by '" + errorContextTextAt + "', unexpected token character '" + ch + "', expected '\"' ");
//                }
//            }
//
//            // clear white space characters
//            while ((ch = buf[i]) <= ' ') {
//                ++i;
//            }
//            if (ch == ':') {
//                // clear white space characters
//                while ((ch = buf[++i]) <= ' ') ;
//                Object value;
//                switch (ch) {
//                    case '{': {
//                        value = parseJSONObject(source, buf, i, toIndex, new LinkedHashMap(), jsonParseContext);
//                        i = jsonParseContext.endIndex;
//                        instance.put(key, value);
//                        break;
//                    }
//                    case '[': {
//                        // 2 [ array
//                        value = parseJSONArray(source, buf, i, toIndex, new ArrayList(), jsonParseContext);
//                        i = jsonParseContext.endIndex;
//                        instance.put(key, value);
//                        break;
//                    }
//                    case '"':
//                    case '\'': {
//                        value = parseJSONString(source, buf, i, toIndex, ch, jsonParseContext);
//                        i = jsonParseContext.endIndex;
//                        instance.put(key, value);
//                        break;
//                    }
//                    case 'n':
//                        value = JSONTypeDeserializer.NULL.deserialize(null, buf, i, toIndex, null, null, '\0', jsonParseContext);
//                        i = jsonParseContext.endIndex;
//                        instance.put(key, value);
//                        break;
//                    case 't':
//                        value = JSONTypeDeserializer.BOOLEAN.deserializeTrue(buf, i, toIndex, null, jsonParseContext);
//                        i = jsonParseContext.endIndex;
//                        instance.put(key, value);
//                        break;
//                    case 'f':
//                        value = JSONTypeDeserializer.BOOLEAN.deserializeFalse(buf, i, toIndex, null, jsonParseContext);
//                        i = jsonParseContext.endIndex;
//                        instance.put(key, value);
//                        break;
//                    default: {
//                        value = JSONTypeDeserializer.NUMBER.deserialize(null, buf, i, toIndex, GenericParameterizedType.AnyType, null, '}', jsonParseContext);
//                        i = jsonParseContext.endIndex;
//                        instance.put(key, value);
//                        char next = buf[++i];
//                        if (next == '}') {
//                            jsonParseContext.endIndex = i;
//                            return instance;
//                        } else {
//                            continue;
//                        }
//                    }
//                }
//                // clear white space characters
//                while ((ch = buf[++i]) <= ' ') ;
//                // Check whether the next character is a comma or end symbol '}'. If yes, continue or break. If not, throw an exception
//                if (ch == ',') {
//                    continue;
//                }
//                if (ch == '}') {
//                    jsonParseContext.endIndex = i;
//                    return instance;
//                }
//                String errorContextTextAt = createErrorContextText(buf, i);
//                throw new JSONException("Syntax error, at pos " + i + ", context text by '" + errorContextTextAt + "', unexpected token character '" + ch + "', expected ',' or '}'");
//            } else {
//                throw new JSONException("Syntax error, at pos " + i + ", unexpected token character '" + ch + "', Colon character ':' is expected.");
//            }
//        }
//    }
//
//    static String parseJSONString(String source, char[] buf, int from, int toIndex, char endCh, JSONParseContext jsonParseContext) {
//        int beginIndex = from + 1;
//        int endIndex = source.indexOf(endCh, beginIndex);
//        char prev = buf[endIndex - 1];
//        if (prev == '\\') {
//            do {
//                endIndex = source.indexOf(endCh, endIndex + 1);
//                prev = buf[endIndex - 1];
//            } while (prev == '\\');
//        }
//        jsonParseContext.endIndex = endIndex;
//        int len = endIndex - beginIndex;
//        return len == 0 ? "" : new String(buf, beginIndex, len);
//    }
//
//    // bytes
//    static Object parse(String source, byte[] bytes) {
//        int fromIndex = 0, toIndex = bytes.length;
//        // Trim remove white space characters
//        byte beginByte = '\0';
//        while ((fromIndex < toIndex) && (beginByte = bytes[fromIndex]) <= ' ') {
//            fromIndex++;
//        }
//        while ((toIndex > fromIndex) && bytes[toIndex - 1] <= ' ') {
//            toIndex--;
//        }
//        JSONParseContext jsonParseContext = new JSONParseContext();
//        try {
//            Object result;
//            switch (beginByte) {
//                case '{':
//                    result = parseJSONObject(source, bytes, fromIndex, toIndex, new LinkedHashMap(), jsonParseContext);
//                    break;
//                case '[':
//                    result = parseJSONArray(source, bytes, fromIndex, toIndex, new ArrayList(), jsonParseContext);
//                    break;
//                case '"':
//                    result = parseJSONString(source, bytes, fromIndex, toIndex, beginByte, jsonParseContext);
//                    break;
//                default:
//                    throw new UnsupportedOperationException("Unsupported for begin character with '" + beginByte + "'");
//            }
//            int endIndex = jsonParseContext.endIndex;
//            if (endIndex != toIndex - 1) {
//                int wordNum = Math.min(50, bytes.length - endIndex - 1);
//                String errorContextTextAt = createErrorContextText(bytes, endIndex + 1);
//                throw new JSONException("Syntax error, at pos " + endIndex + ", context text by '" + errorContextTextAt + "', extra characters found, '" + new String(bytes, endIndex + 1, wordNum) + " ...'");
//            }
//            return result;
//        } catch (Exception ex) {
//            handleCatchException(ex, bytes, toIndex);
//            throw new JSONException("Error: " + ex.getMessage(), ex);
//        } finally {
//            jsonParseContext.clear();
//        }
//    }
//
//    static Collection parseJSONArray(String source, byte[] bytes, int fromIndex, int toIndex, Collection list, JSONParseContext jsonParseContext) throws Exception {
//
//        int beginIndex = fromIndex + 1;
//        byte b;
//
//        // The core token of the collection array is a comma
//        for (int i = beginIndex; /*i < toIndex*/ ; ++i) {
//            // clear white space characters
//            while ((b = bytes[i]) <= ' ') {
//                ++i;
//            }
//            // empty set or exception
//            if (b == ']') {
//                if (list.size() > 0) {
//                    throw new JSONException("Syntax error, at pos " + i + ", the closing symbol ']' is not allowed here.");
//                }
//                jsonParseContext.endIndex = i;
//                return list;
//            }
//            Object value;
//            switch (b) {
//                case '{': {
//                    value = parseJSONObject(source, bytes, i, toIndex, new LinkedHashMap(), jsonParseContext);
//                    list.add(value);
//                    i = jsonParseContext.endIndex;
//                    break;
//                }
//                case '[': {
//                    // 2 [ array
//                    value = parseJSONArray(source, bytes, i, toIndex, new ArrayList(), jsonParseContext);
//                    list.add(value);
//                    i = jsonParseContext.endIndex;
//                    break;
//                }
//                case '"': {
//                    // 3 string
//                    // When there are escape characters, the escape character needs to be parsed
//                    value = parseJSONString(source, bytes, i, toIndex, b, jsonParseContext);
//                    list.add(value);
//                    i = jsonParseContext.endIndex;
//                    break;
//                }
//                case 'n':
//                    value = JSONTypeDeserializer.parseNull(bytes, i, toIndex, jsonParseContext);
//                    i = jsonParseContext.endIndex;
//                    list.add(value);
//                    break;
//                case 't':
//                    value = JSONTypeDeserializer.parseTrue(bytes, i, toIndex, jsonParseContext);
//                    i = jsonParseContext.endIndex;
//                    list.add(value);
//                    break;
//                case 'f':
//                    value = JSONTypeDeserializer.parseFalse(bytes, i, toIndex, jsonParseContext);
//                    i = jsonParseContext.endIndex;
//                    list.add(value);
//                    break;
//                default: {
//                    value = JSONTypeDeserializer.NUMBER.deserialize(null, bytes, i, toIndex, GenericParameterizedType.AnyType, null, END_ARRAY, jsonParseContext);
//                    i = jsonParseContext.endIndex;
//                    list.add(value);
//                    // either continue or return
//                    ++i;
//                    char next = (char) bytes[i];
//                    if (next == ']') {
//                        jsonParseContext.endIndex = i;
//                        return list;
//                    } else {
//                        continue;
//                    }
//                }
//            }
//            // clear white space characters
//            while ((b = bytes[++i]) <= ' ') ;
//            // （Check whether the next character is a comma or end symbol. If yes, continue or return . If not, throw an exception）
//            if (b == ',') {
//                continue;
//            }
//            if (b == ']') {
//                jsonParseContext.endIndex = i;
//                return list;
//            }
//            String errorContextTextAt = createErrorContextText(bytes, i);
//            throw new JSONException("Syntax error, at pos " + i + ", context text by '" + errorContextTextAt + "', unexpected token character '" + b + "', expected ',' or ']'");
//        }
////        throw new JSONException("Syntax error, cannot find closing symbol ']' matching '['");
//    }
//
//    static Map parseJSONObject(String source, byte[] bytes, int fromIndex, int toIndex, Map instance, JSONParseContext jsonParseContext) throws Exception {
//
//        int beginIndex = fromIndex + 1;
//        byte b;
//
//        boolean empty = true;
//        // for loop to parse
//        for (int i = beginIndex; /*i < toIndex*/ ; ++i) {
//            // clear white space characters
//            while ((b = bytes[i]) <= ' ') {
//                ++i;
//            }
//            Serializable key;
//            int fieldKeyFrom = i;
//            // Standard JSON field name with "
//            if (b == '"') {
//                int endIndex = source.indexOf('"', i + 1);
//                key = new String(bytes, i + 1, endIndex - i - 1);
//                i = endIndex;
//                empty = false;
//                ++i;
//            } else {
//                // empty object or exception
//                if (b == '}') {
//                    if (!empty) {
//                        throw new JSONException("Syntax error, at pos " + i + ", the closing symbol '}' is not allowed here.");
//                    }
//                    jsonParseContext.endIndex = i;
//                    return instance;
//                }
//                int j = i;
//                boolean isNullKey = false;
//                key = null;
//                if (b == 'n' && bytes[++i] == 'u' && bytes[++i] == 'l' && bytes[++i] == 'l') {
//                    isNullKey = true;
//                    ++i;
//                }
//                if (!isNullKey) {
//                    String errorContextTextAt = createErrorContextText(bytes, j);
//                    throw new JSONException("Syntax error, at pos " + j + ", context text by '" + errorContextTextAt + "', unexpected token character '" + b + "', expected '\"'");
//                }
//            }
//
//            // clear white space characters
//            while ((b = bytes[i]) <= ' ') {
//                ++i;
//            }
//            // Standard JSON rules:
//            // 1 if matched, it can only be followed by a colon, and all other symbols are wrong
//            // 2 after the attribute value is parsed, it can only be followed by a comma(,) or end, and other symbols are wrong
//            if (b == ':') {
//                // clear white space characters
//                while ((b = bytes[++i]) <= ' ') ;
//                Object value;
//                switch (b) {
//                    case '{': {
//                        value = parseJSONObject(source, bytes, i, toIndex, new LinkedHashMap(), jsonParseContext);
//                        i = jsonParseContext.endIndex;
//                        instance.put(key, value);
//                        break;
//                    }
//                    case '[': {
//                        // 2 [ array
//                        value = parseJSONArray(source, bytes, i, toIndex, new ArrayList(), jsonParseContext);
//                        i = jsonParseContext.endIndex;
//                        instance.put(key, value);
//                        break;
//                    }
//                    case '"': {
//                        value = parseJSONString(source, bytes, i, toIndex, b, jsonParseContext);
//                        i = jsonParseContext.endIndex;
//                        instance.put(key, value);
//                        break;
//                    }
//                    case 'n':
//                        value = JSONTypeDeserializer.parseNull(bytes, i, toIndex, jsonParseContext);
//                        i = jsonParseContext.endIndex;
//                        instance.put(key, value);
//                        break;
//                    case 't':
//                        value = JSONTypeDeserializer.parseTrue(bytes, i, toIndex, jsonParseContext);
//                        i = jsonParseContext.endIndex;
//                        instance.put(key, value);
//                        break;
//                    case 'f':
//                        value = JSONTypeDeserializer.parseFalse(bytes, i, toIndex, jsonParseContext);
//                        i = jsonParseContext.endIndex;
//                        instance.put(key, value);
//                        break;
//                    default: {
//                        value = JSONTypeDeserializer.NUMBER.deserialize(null, bytes, i, toIndex, GenericParameterizedType.AnyType, null, END_OBJECT, jsonParseContext);
//                        i = jsonParseContext.endIndex;
//                        instance.put(key, value);
//                        char next = (char) bytes[++i];
//                        if (next == '}') {
//                            jsonParseContext.endIndex = i;
//                            return instance;
//                        } else {
//                            continue;
//                        }
//                    }
//                }
//                // clear white space characters
//                while ((b = bytes[++i]) <= ' ') ;
//                // Check whether the next character is a comma or end symbol '}'. If yes, continue or break. If not, throw an exception
//                if (b == ',') {
//                    continue;
//                }
//                if (b == '}') {
//                    jsonParseContext.endIndex = i;
//                    return instance;
//                }
//                String errorContextTextAt = createErrorContextText(bytes, i);
//                throw new JSONException("Syntax error, at pos " + i + ", context text by '" + errorContextTextAt + "', unexpected token character '" + b + "', expected ',' or '}'");
//            } else {
//                throw new JSONException("Syntax error, at pos " + i + ", unexpected token character '" + b + "', Colon character ':' is expected.");
//            }
//        }
//    }
//
//    static String parseJSONString(String source, byte[] bytes, int from, int toIndex, byte endCh, JSONParseContext jsonParseContext) {
//        int beginIndex = from + 1;
//        int endIndex = source.indexOf(endCh, beginIndex);
//        byte prev = bytes[endIndex - 1];
//        if (prev == '\\') {
//            do {
//                endIndex = source.indexOf(endCh, endIndex + 1);
//                prev = bytes[endIndex - 1];
//            } while (prev == '\\');
//        }
//        jsonParseContext.endIndex = endIndex;
//        int len = endIndex - beginIndex;
//        return len == 0 ? "" : new String(bytes, beginIndex, len);
//    }
//}
