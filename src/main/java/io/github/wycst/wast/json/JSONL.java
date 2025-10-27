package io.github.wycst.wast.json;

import io.github.wycst.wast.common.reflect.GenericParameterizedType;
import io.github.wycst.wast.common.utils.EnvUtils;
import io.github.wycst.wast.json.exceptions.JSONException;
import io.github.wycst.wast.json.options.ReadOption;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * 支持ndjson读写
 */
final class JSONL extends JSONGeneral {

    /**
     * 解析ndjson(jsonl)
     *
     * @param json
     * @param readOptions
     * @return
     */
    static List parseNdJson(String json, JSONTypeDeserializer typeDeserializer, ReadOption[] readOptions) {
        json.getClass();
        if (EnvUtils.JDK_9_PLUS) {
            byte[] bytes = (byte[]) JSONMemoryHandle.getStringValue(json);
            if (bytes.length == json.length()) {
                return parseNdJson(typeDeserializer, AsciiStringSource.of(json), bytes, JSONParseContext.of(readOptions));
            } else {
                char[] chars = json.toCharArray();
                return parseNdJson(typeDeserializer, UTF16ByteArraySource.of(json), chars, JSONParseContext.of(readOptions));
            }
        }
        char[] chars = (char[]) JSONMemoryHandle.getStringValue(json);
        return parseNdJson(typeDeserializer, null, chars, JSONParseContext.of(readOptions));
    }

    static List parseNdJson(final JSONTypeDeserializer typeDeserializer, CharSource source, byte[] bytes, JSONParseContext parseContext) {
        int fromIndex = 0, toIndex = bytes.length;
        parseContext.toIndex = toIndex;
        parseContext.multiple = true;
        List results = new ArrayList();
        try {
            while (true) {
                byte b;
                while (fromIndex < toIndex && ((b = bytes[fromIndex]) <= ' ' || b == ',')) {
                    // 支持以逗号结尾来兼容单行的数字
                    ++fromIndex;
                }
                if (fromIndex == toIndex) break;
                Object result = typeDeserializer.deserialize(source, bytes, fromIndex, GenericParameterizedType.AnyType, null, ZERO, parseContext);
                results.add(result);
                fromIndex = parseContext.endIndex + 1;
            }
            return results;
        } catch (Exception e) {
            String errorContextTextAt = createErrorContextText(bytes, fromIndex);
            throw new JSONException("Syntax error, at pos " + fromIndex + ", " + errorContextTextAt, e);
        }
    }

    static List parseNdJson(final JSONTypeDeserializer typeDeserializer, CharSource source, char[] chars, JSONParseContext parseContext) {
        int fromIndex = 0, toIndex = chars.length;
        parseContext.toIndex = toIndex;
        parseContext.multiple = true;
        List results = new ArrayList();
        try {
            while (true) {
                char ch;
                while (fromIndex < toIndex && ((ch = chars[fromIndex]) <= ' ' || ch == ',')) {
                    // 支持以逗号结尾来兼容单行的数字
                    ++fromIndex;
                }
                if (fromIndex == toIndex) break;
                Object result = typeDeserializer.deserialize(source, chars, fromIndex, GenericParameterizedType.AnyType, null, ',', parseContext);
                results.add(result);
                fromIndex = parseContext.endIndex + 1;
            }
            return results;
        } catch (Exception e) {
            String errorContextTextAt = createErrorContextText(chars, fromIndex);
            throw new JSONException("Syntax error, at pos " + fromIndex + " " + errorContextTextAt, e);
        }
    }

    static void writeNdJsonTo(JSONWriter content, Collection collection, JSONConfig jsonConfig) {
        try {
            int indentLevel = 0;
            Class<?> firstClass = null;
            JSONTypeSerializer firstSerializer = null;
            for (Object object : collection) {
                if (object == null) {
                    continue;
                }
                Class<?> objectClass = object.getClass();
                JSONTypeSerializer typeSerializer;
                if (firstClass == null) {
                    firstClass = objectClass;
                    typeSerializer = firstSerializer = JSONTypeSerializer.getTypeSerializer(objectClass);
                } else {
                    if (objectClass == firstClass) {
                        typeSerializer = firstSerializer;
                    } else {
                        typeSerializer = JSONTypeSerializer.getTypeSerializer(objectClass);
                    }
                }
                typeSerializer.serialize(object, content, jsonConfig, indentLevel);
                if (!content.endsWith('}') && !content.endsWith(']') && !content.endsWith('"')) {
                    // 如果是boolean或者number追加逗号
                    content.writeJSONToken(',');
                }
                content.writeJSONToken('\n');
            }
        } catch (Exception e) {
            throw (e instanceof JSONException) ? (JSONException) e : new JSONException(e);
        }
    }
}
