package io.github.wycst.wast.json;

import io.github.wycst.wast.common.reflect.GenericParameterizedType;
import io.github.wycst.wast.common.utils.EnvUtils;
import io.github.wycst.wast.json.options.ReadOption;

import java.util.Arrays;

/**
 * json校验
 * （通常情况下只要JSON格式错误，解析一定会抛出异常，但通过异常来判定影响性能，拷贝的解析的代码然后做了修改）
 *
 * @Author: wangy
 * @Date: 2022/6/3 20:01
 * @Description:
 */
final class JSONValidator extends JSONGeneral {

    static boolean validate(String json, ReadOption[] readOptions) {
        if (EnvUtils.JDK_9_PLUS) {
            byte[] bytes = (byte[]) JSONUnsafe.getStringValue(json);
            if (bytes.length == json.length()) {
                return validate(AsciiStringSource.of(json, bytes), bytes, readOptions);
            } else {
                char[] chars = json.toCharArray();
                return validate(UTF16ByteArraySource.of(json), chars, readOptions);
            }
        }
        char[] chars = (char[]) JSONUnsafe.getStringValue(json);
        return validate(null, chars, readOptions);
    }

    static boolean validate(char[] buf, ReadOption[] readOptions) {
        if (EnvUtils.JDK_9_PLUS) {
            String json = new String(buf);
            byte[] bytes = (byte[]) JSONUnsafe.getStringValue(json);
            if (bytes.length == json.length()) {
                return validate(AsciiStringSource.of(json, bytes), bytes, readOptions);
            } else {
                return validate(UTF16ByteArraySource.of(json), buf, readOptions);
            }
        }
        return validate(null, buf, readOptions);
    }

    public static boolean validate(byte[] buf, ReadOption[] readOptions) {
        if (EnvUtils.JDK_9_PLUS) {
            return validate(AsciiStringSource.of(buf), buf, readOptions);
        }
        return validate(null, buf, readOptions);
    }

    static boolean validate(CharSource source, char[] chars, ReadOption... readOptions) {
        int fromIndex = 0, toIndex = chars.length;
        char beginChar = '\0';
        while ((fromIndex < toIndex) && (beginChar = chars[fromIndex]) <= ' ') {
            fromIndex++;
        }
        while ((toIndex > fromIndex) && chars[toIndex - 1] <= ' ') {
            toIndex--;
        }
        JSONParseContext jsonParseContext = new JSONParseContext();
        JSONOptions.readOptions(readOptions, jsonParseContext);
        jsonParseContext.validate = true;
        try {
            final boolean allowComment = jsonParseContext.allowComment;
            if (allowComment && beginChar == '/') {
                fromIndex = clearCommentAndWhiteSpaces(chars, fromIndex + 1, toIndex, jsonParseContext);
                beginChar = chars[fromIndex];
            }
            boolean validate = true;
            switch (beginChar) {
                case '{':
                    validate = JSONTypeDeserializer.MAP.validate(source, chars, fromIndex, toIndex, (char) 0, jsonParseContext);
                    break;
                case '[':
                    validate = JSONTypeDeserializer.COLLECTION.validate(source, chars, fromIndex, toIndex, (char) 0, jsonParseContext);
                    break;
                case '\'':
                case '"':
                    JSONTypeDeserializer.CHAR_SEQUENCE_STRING.skip(source, chars, fromIndex, beginChar, jsonParseContext);
                    break;
                default:
                    try {
                        switch (beginChar) {
                            case 't': {
                                JSONTypeDeserializer.parseTrue(chars, fromIndex, toIndex, jsonParseContext);
                                break;
                            }
                            case 'f': {
                                JSONTypeDeserializer.parseFalse(chars, fromIndex, toIndex, jsonParseContext);
                                break;
                            }
                            case 'n': {
                                JSONTypeDeserializer.parseNull(chars, fromIndex, toIndex, jsonParseContext);
                                break;
                            }
                            default: {
                                char[] numBuf = Arrays.copyOfRange(chars, fromIndex, toIndex + 1);
                                numBuf[numBuf.length - 1] = ',';
                                JSONTypeDeserializer.NUMBER_SKIPPER.deserialize(source, numBuf, 0, numBuf.length, jsonParseContext.useBigDecimalAsDefault ? GenericParameterizedType.BigDecimalType : GenericParameterizedType.AnyType, null, ',', jsonParseContext);
                                toIndex = numBuf.length - 1;
                            }
                        }
                        if (jsonParseContext.validateFail) {
                            return false;
                        }
                    } catch (Throwable throwable) {
                        return false;
                    }
            }
            if (!validate) {
                return false;
            }
            int endIndex = jsonParseContext.endIndex;
            if (allowComment) {
                if (endIndex < toIndex - 1) {
                    char commentStart = '\0';
                    while (endIndex + 1 < toIndex && (commentStart = (char) chars[++endIndex]) <= ' ') ;
                    if (commentStart == '/') {
                        endIndex = clearCommentAndWhiteSpaces(chars, endIndex + 1, toIndex, jsonParseContext);
                    }
                }
            }
            return endIndex == toIndex - 1;
        } catch (Exception ex) {
            return false;
        } finally {
            jsonParseContext.clear();
        }
    }

    static boolean validate(CharSource source, byte[] bytes, ReadOption... readOptions) {
        int fromIndex = 0, toIndex = bytes.length;
        byte beginByte = '\0';
        while ((fromIndex < toIndex) && (beginByte = bytes[fromIndex]) <= ' ') {
            fromIndex++;
        }
        while ((toIndex > fromIndex) && bytes[toIndex - 1] <= ' ') {
            toIndex--;
        }
        JSONParseContext jsonParseContext = new JSONParseContext();
        JSONOptions.readOptions(readOptions, jsonParseContext);
        jsonParseContext.validate = true;
        try {
            boolean allowComment = jsonParseContext.allowComment;
            if (allowComment && beginByte == '/') {
                fromIndex = clearCommentAndWhiteSpaces(bytes, fromIndex + 1, toIndex, jsonParseContext);
                beginByte = bytes[fromIndex];
            }
            boolean validate = true;
            switch (beginByte) {
                case '{':
                    validate = JSONTypeDeserializer.MAP.validate(source, bytes, fromIndex, toIndex, JSONGeneral.ZERO, jsonParseContext);
                    break;
                case '[':
                    validate = JSONTypeDeserializer.COLLECTION.validate(source, bytes, fromIndex, toIndex, JSONGeneral.ZERO, jsonParseContext);
                    break;
                case '\'':
                case '"':
                    JSONTypeDeserializer.CHAR_SEQUENCE_STRING.skip(source, bytes, fromIndex, beginByte, jsonParseContext);
                    break;
                default:
                    try {
                        switch (beginByte) {
                            case 't': {
                                JSONTypeDeserializer.parseTrue(bytes, fromIndex, toIndex, jsonParseContext);
                                break;
                            }
                            case 'f': {
                                JSONTypeDeserializer.parseFalse(bytes, fromIndex, toIndex, jsonParseContext);
                                break;
                            }
                            case 'n': {
                                JSONTypeDeserializer.parseNull(bytes, fromIndex, toIndex, jsonParseContext);
                                break;
                            }
                            default: {
                                byte[] numBuf = Arrays.copyOfRange(bytes, fromIndex, toIndex + 1);
                                numBuf[numBuf.length - 1] = ',';
                                JSONTypeDeserializer.NUMBER_SKIPPER.deserialize(source, numBuf, 0, numBuf.length, jsonParseContext.useBigDecimalAsDefault ? GenericParameterizedType.BigDecimalType : GenericParameterizedType.AnyType, null, (byte) ',', jsonParseContext);
                                toIndex = numBuf.length - 1;
                            }
                        }
                        if (jsonParseContext.validateFail) {
                            return false;
                        }
                    } catch (Throwable throwable) {
                        return false;
                    }
            }
            if (!validate) {
                return false;
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
            return endIndex == toIndex - 1;
        } catch (Exception ex) {
            return false;
        } finally {
            jsonParseContext.clear();
        }
    }
}