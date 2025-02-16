package io.github.wycst.wast.json;

import io.github.wycst.wast.common.reflect.GenericParameterizedType;
import io.github.wycst.wast.json.exceptions.JSONException;

import java.net.URI;
import java.net.URL;
import java.util.UUID;

/**
 * @Date 2024/9/29 13:46
 * @Created by wangyc
 */
final class JSONTypeExtensionDesr {

    static void initExtens() {
        JSONTypeDeserializer.putTypeDeserializer(new UUIDImpl(), UUID.class);
        JSONTypeDeserializer.putTypeDeserializer(new URLImpl(), URL.class);
        JSONTypeDeserializer.putTypeDeserializer(new URIImpl(), URI.class);
    }

    final static class UUIDImpl extends JSONTypeDeserializer {

        @Override
        protected Object valueOf(String value, Class<?> actualType) {
            return UUID.fromString(value);
        }

        @Override
        protected Object deserialize(CharSource charSource, char[] buf, int fromIndex, GenericParameterizedType parameterizedType, Object defaultValue, char endToken, JSONParseContext parseContext) throws Exception {
            final char beginChar = buf[fromIndex];
            int offset = fromIndex + 1, endIndex;
            if (beginChar == DOUBLE_QUOTATION || beginChar == '\'') {
                if (buf[offset] == beginChar) {
                    parseContext.endIndex = offset;
                    return null;
                }
                try {
                    long mh = hex8ToLong(buf, offset);
                    long ml = hex4ToLong(buf, offset + 9) << 16 | hex4ToLong(buf, offset + 14);
                    long lh = hex4ToLong(buf, offset + 19) << 16 | hex4ToLong(buf, offset + 24);
                    long ll = hex8ToLong(buf, offset + 28);
                    if ((mh | ml | lh | ll) > -1 && buf[offset + 8] == '-' && buf[offset + 13] == '-' && buf[offset + 18] == '-' && buf[offset + 23] == '-' && buf[endIndex = offset + 36] == beginChar) {
                        parseContext.endIndex = endIndex;
                        return new UUID(mh << 32 | ml, lh << 32 | ll);
                    }
                } catch (Throwable throwable) {}
            } else if (beginChar == 'n') {
                return parseNull(buf, fromIndex, parseContext);
            }
            String errorContextTextAt = createErrorContextText(buf, fromIndex);
            throw new JSONException("Syntax error, at pos " + fromIndex + ", context text by '" + errorContextTextAt + "', deserialize UUID fail");
        }

        @Override
        protected Object deserialize(CharSource charSource, byte[] buf, int fromIndex, GenericParameterizedType parameterizedType, Object defaultValue, byte endToken, JSONParseContext parseContext) throws Exception {
            final byte beginByte = buf[fromIndex];
            int offset = fromIndex + 1, endIndex;
            if (beginByte == DOUBLE_QUOTATION || beginByte == '\'') {
                if (buf[offset] == beginByte) {
                    parseContext.endIndex = offset;
                    return null;
                }
                try {
                    long mh = hex8ToLong(buf, offset);
                    long ml = hex4ToLong(buf, offset + 9) << 16 | hex4ToLong(buf, offset + 14);
                    long lh = hex4ToLong(buf, offset + 19) << 16 | hex4ToLong(buf, offset + 24);
                    long ll = hex8ToLong(buf, offset + 28);
                    if ((mh | ml | lh | ll) > -1 && buf[offset + 8] == '-' && buf[offset + 13] == '-' && buf[offset + 18] == '-' && buf[offset + 23] == '-' && buf[endIndex = offset + 36] == beginByte) {
                        parseContext.endIndex = endIndex;
                        return new UUID(mh << 32 | ml, lh << 32 | ll);
                    }
                } catch (Throwable throwable) {}
            } else if (beginByte == 'n') {
                return parseNull(buf, fromIndex, parseContext);
            }
            String errorContextTextAt = createErrorContextText(buf, fromIndex);
            throw new JSONException("Syntax error, at pos " + fromIndex + ", context text by '" + errorContextTextAt + "', deserialize UUID fail");
        }
    }

    final static class URLImpl extends JSONTypeDeserializer {

        @Override
        protected Object valueOf(String value, Class<?> actualType) throws Exception {
            return new URL(value);
        }

        @Override
        protected Object deserialize(CharSource charSource, char[] buf, int fromIndex, GenericParameterizedType parameterizedType, Object defaultValue, char endToken, JSONParseContext jsonParseContext) throws Exception {
            String urlString = (String) CHAR_SEQUENCE_STRING.deserialize(charSource, buf, fromIndex, parameterizedType, defaultValue, endToken, jsonParseContext);
            return new URL(urlString);
        }

        @Override
        protected Object deserialize(CharSource charSource, byte[] bytes, int fromIndex, GenericParameterizedType parameterizedType, Object defaultValue, byte endToken, JSONParseContext jsonParseContext) throws Exception {
            String urlString = (String) CHAR_SEQUENCE_STRING.deserialize(charSource, bytes, fromIndex, parameterizedType, defaultValue, endToken, jsonParseContext);
            return new URL(urlString);
        }
    }

    final static class URIImpl extends JSONTypeDeserializer {
        @Override
        protected Object valueOf(String value, Class<?> actualType) throws Exception {
            return new URI(value);
        }

        @Override
        protected Object deserialize(CharSource charSource, char[] buf, int fromIndex, GenericParameterizedType parameterizedType, Object defaultValue, char endToken, JSONParseContext jsonParseContext) throws Exception {
            String urlString = (String) CHAR_SEQUENCE_STRING.deserialize(charSource, buf, fromIndex, parameterizedType, defaultValue, endToken, jsonParseContext);
            return new URI(urlString);
        }

        @Override
        protected Object deserialize(CharSource charSource, byte[] bytes, int fromIndex, GenericParameterizedType parameterizedType, Object defaultValue, byte endToken, JSONParseContext jsonParseContext) throws Exception {
            String urlString = (String) CHAR_SEQUENCE_STRING.deserialize(charSource, bytes, fromIndex, parameterizedType, defaultValue, endToken, jsonParseContext);
            return new URI(urlString);
        }
    }
}
