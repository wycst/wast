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
        protected Object deserialize(CharSource charSource, char[] buf, int fromIndex, int toIndex, GenericParameterizedType parameterizedType, Object defaultValue, char endToken, JSONParseContext jsonParseContext) throws Exception {
            final char beginChar = buf[fromIndex], next;
            int offset = fromIndex + 1;
            if (beginChar == DOUBLE_QUOTATION || beginChar == '\'') {
                if ((next = buf[offset]) == beginChar) {
                    jsonParseContext.endIndex = offset;
                    return null;
                }
                try {
                    long h1 = hex(next), h2 = hex(buf[++offset]), h3 = hex(buf[++offset]), h4 = hex(buf[++offset]), h5 = hex(buf[++offset]), h6 = hex(buf[++offset]), h7 = hex(buf[++offset]), h8 = hex(buf[++offset]), h9 = buf[++offset],
                            h10 = hex(buf[++offset]), h11 = hex(buf[++offset]), h12 = hex(buf[++offset]), h13 = hex(buf[++offset]), h14 = buf[++offset], h15 = hex(buf[++offset]), h16 = hex(buf[++offset]), h17 = hex(buf[++offset]), h18 = hex(buf[++offset]),
                            h19 = buf[++offset], h20 = hex(buf[++offset]), h21 = hex(buf[++offset]), h22 = hex(buf[++offset]), h23 = hex(buf[++offset]), h24 = buf[++offset], h25 = hex(buf[++offset]), h26 = hex(buf[++offset]), h27 = hex(buf[++offset]),
                            h28 = hex(buf[++offset]), h29 = hex(buf[++offset]), h30 = hex(buf[++offset]), h31 = hex(buf[++offset]), h32 = hex(buf[++offset]), h33 = hex(buf[++offset]), h34 = hex(buf[++offset]), h35 = hex(buf[++offset]), h36 = hex(buf[++offset]);
                    long mostSigBits = h1 << 60 | h2 << 56 | h3 << 52 | h4 << 48 | h5 << 44 | h6 << 40 | h7 << 36 | h8 << 32 | h10 << 28 | h11 << 24 | h12 << 20 | h13 << 16 | h15 << 12 | h16 << 8 | h17 << 4 | h18;
                    long leastSigBits = h20 << 60 | h21 << 56 | h22 << 52 | h23 << 48 | h25 << 44 | h26 << 40 | h27 << 36 | h28 << 32 | h29 << 28 | h30 << 24 | h31 << 20 | h32 << 16 | h33 << 12 | h34 << 8 | h35 << 4 | h36;
                    if (h9 == '-' && h14 == '-' && h19 == '-' && h24 == '-' && buf[++offset] == beginChar) {
                        jsonParseContext.endIndex = offset;
                        return new UUID(mostSigBits, leastSigBits);
                    }
                } catch (Throwable throwable) {
                    String errorContextTextAt = createErrorContextText(buf, fromIndex);
                    throw new JSONException("Syntax error, at pos " + fromIndex + ", context text by '" + errorContextTextAt + "', deserialize UUID fail");
                }
            } else if (beginChar == 'n') {
                return NULL.deserialize(charSource, buf, fromIndex, toIndex, null, null, jsonParseContext);
            }

            String errorContextTextAt = createErrorContextText(buf, fromIndex);
            throw new JSONException("Syntax error, at pos " + fromIndex + ", context text by '" + errorContextTextAt + "', deserialize UUID fail");
        }

        @Override
        protected Object deserialize(CharSource charSource, byte[] bytes, int fromIndex, int toIndex, GenericParameterizedType parameterizedType, Object defaultValue, byte endToken, JSONParseContext jsonParseContext) throws Exception {
            final byte beginByte = bytes[fromIndex], next;
            int offset = fromIndex + 1;
            if (beginByte == DOUBLE_QUOTATION || beginByte == '\'') {
                if ((next = bytes[offset]) == beginByte) {
                    jsonParseContext.endIndex = offset;
                    return null;
                }
                try {
                    long h1 = hex(next), h2 = hex(bytes[++offset]), h3 = hex(bytes[++offset]), h4 = hex(bytes[++offset]), h5 = hex(bytes[++offset]), h6 = hex(bytes[++offset]), h7 = hex(bytes[++offset]), h8 = hex(bytes[++offset]), h9 = bytes[++offset],
                            h10 = hex(bytes[++offset]), h11 = hex(bytes[++offset]), h12 = hex(bytes[++offset]), h13 = hex(bytes[++offset]), h14 = bytes[++offset], h15 = hex(bytes[++offset]), h16 = hex(bytes[++offset]), h17 = hex(bytes[++offset]), h18 = hex(bytes[++offset]),
                            h19 = bytes[++offset], h20 = hex(bytes[++offset]), h21 = hex(bytes[++offset]), h22 = hex(bytes[++offset]), h23 = hex(bytes[++offset]), h24 = bytes[++offset], h25 = hex(bytes[++offset]), h26 = hex(bytes[++offset]), h27 = hex(bytes[++offset]),
                            h28 = hex(bytes[++offset]), h29 = hex(bytes[++offset]), h30 = hex(bytes[++offset]), h31 = hex(bytes[++offset]), h32 = hex(bytes[++offset]), h33 = hex(bytes[++offset]), h34 = hex(bytes[++offset]), h35 = hex(bytes[++offset]), h36 = hex(bytes[++offset]);
                    long mostSigBits = h1 << 60 | h2 << 56 | h3 << 52 | h4 << 48 | h5 << 44 | h6 << 40 | h7 << 36 | h8 << 32 | h10 << 28 | h11 << 24 | h12 << 20 | h13 << 16 | h15 << 12 | h16 << 8 | h17 << 4 | h18;
                    long leastSigBits = h20 << 60 | h21 << 56 | h22 << 52 | h23 << 48 | h25 << 44 | h26 << 40 | h27 << 36 | h28 << 32 | h29 << 28 | h30 << 24 | h31 << 20 | h32 << 16 | h33 << 12 | h34 << 8 | h35 << 4 | h36;
                    if (h9 == '-' && h14 == '-' && h19 == '-' && h24 == '-' && bytes[++offset] == beginByte) {
                        jsonParseContext.endIndex = offset;
                        return new UUID(mostSigBits, leastSigBits);
                    }
                } catch (Throwable throwable) {
                    String errorContextTextAt = createErrorContextText(bytes, fromIndex);
                    throw new JSONException("Syntax error, at pos " + fromIndex + ", context text by '" + errorContextTextAt + "', deserialize UUID fail");
                }
            } else if (beginByte == 'n') {
                return NULL.deserialize(charSource, bytes, fromIndex, toIndex, null, null, jsonParseContext);
            }

            String errorContextTextAt = createErrorContextText(bytes, fromIndex);
            throw new JSONException("Syntax error, at pos " + fromIndex + ", context text by '" + errorContextTextAt + "', deserialize UUID fail");
        }
    }

    final static class URLImpl extends JSONTypeDeserializer {

        @Override
        protected Object valueOf(String value, Class<?> actualType) throws Exception {
            return new URL(value);
        }

        @Override
        protected Object deserialize(CharSource charSource, char[] buf, int fromIndex, int toIndex, GenericParameterizedType parameterizedType, Object defaultValue, char endToken, JSONParseContext jsonParseContext) throws Exception {
            String urlString = (String) CHAR_SEQUENCE_STRING.deserialize(charSource, buf, fromIndex, toIndex, parameterizedType, defaultValue, endToken, jsonParseContext);
            return new URL(urlString);
        }

        @Override
        protected Object deserialize(CharSource charSource, byte[] bytes, int fromIndex, int toIndex, GenericParameterizedType parameterizedType, Object defaultValue, byte endToken, JSONParseContext jsonParseContext) throws Exception {
            String urlString = (String) CHAR_SEQUENCE_STRING.deserialize(charSource, bytes, fromIndex, toIndex, parameterizedType, defaultValue, endToken, jsonParseContext);
            return new URL(urlString);
        }
    }

    final static class URIImpl extends JSONTypeDeserializer {
        @Override
        protected Object valueOf(String value, Class<?> actualType) throws Exception {
            return new URI(value);
        }

        @Override
        protected Object deserialize(CharSource charSource, char[] buf, int fromIndex, int toIndex, GenericParameterizedType parameterizedType, Object defaultValue, char endToken, JSONParseContext jsonParseContext) throws Exception {
            String urlString = (String) CHAR_SEQUENCE_STRING.deserialize(charSource, buf, fromIndex, toIndex, parameterizedType, defaultValue, endToken, jsonParseContext);
            return new URI(urlString);
        }

        @Override
        protected Object deserialize(CharSource charSource, byte[] bytes, int fromIndex, int toIndex, GenericParameterizedType parameterizedType, Object defaultValue, byte endToken, JSONParseContext jsonParseContext) throws Exception {
            String urlString = (String) CHAR_SEQUENCE_STRING.deserialize(charSource, bytes, fromIndex, toIndex, parameterizedType, defaultValue, endToken, jsonParseContext);
            return new URI(urlString);
        }
    }
}
