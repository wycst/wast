package io.github.wycst.wast.json;

import io.github.wycst.wast.common.beans.DateTemplate;
import io.github.wycst.wast.common.reflect.ClassStrucWrap;
import io.github.wycst.wast.common.reflect.GenericParameterizedType;
import io.github.wycst.wast.json.annotations.JsonProperty;
import io.github.wycst.wast.json.exceptions.JSONException;
import io.github.wycst.wast.json.temporal.*;

import java.lang.reflect.InvocationTargetException;

/**
 * java.time support
 * <p>
 * Deserialization using reflection
 * <p>
 * Localtime, localdate, localdatetime do not consider time zone
 *
 * @Author: wangy
 * @Date: 2022/8/13 15:08
 * @Description:
 */
public abstract class JSONTemporalDeserializer extends JSONTypeDeserializer {

    protected int patternType;
    protected DateTemplate dateTemplate;
    protected final static int[] NANO_OF_SECOND_PADDING = {1, 10, 100, 1000, 10000, 100000, 1000000, 10000000, 100000000, 1000000000};

    protected JSONTemporalDeserializer(TemporalConfig temporalConfig) {
        checkClass(temporalConfig.getGenericParameterizedType());
        String pattern = temporalConfig.getDatePattern();
        patternType = getPatternType(pattern);
        if (patternType == 0) {
            createDefaultTemplate();
        } else {
            dateTemplate = new DateTemplate(pattern);
        }
    }

    static JSONTypeDeserializer getTemporalDeserializerInstance(ClassStrucWrap.ClassWrapperType classWrapperType, GenericParameterizedType genericParameterizedType, JsonProperty property) {
        TemporalConfig temporalConfig = TemporalConfig.of(genericParameterizedType, property);
        switch (classWrapperType) {
            case TemporalLocalDate: {
                return new TemporalLocalDateDeserializer(temporalConfig);
            }
            case TemporalLocalTime: {
                return new TemporalLocalTimeDeserializer(temporalConfig);
            }
            case TemporalLocalDateTime: {
                return new TemporalLocalDateTimeDeserializer(temporalConfig);
            }
            case TemporalZonedDateTime: {
                return new TemporalZonedDateTimeDeserializer(temporalConfig);
            }
            case TemporalOffsetDateTime: {
                return new TemporalOffsetDateTimeDeserializer(temporalConfig);
            }
            case TemporalInstant: {
                return new TemporalInstantDeserializer(temporalConfig);
            }
            default: {
                throw new UnsupportedOperationException();
            }
        }
    }

    protected void createDefaultTemplate() {
    }

    // check
    protected abstract void checkClass(GenericParameterizedType genericParameterizedType);

    /**
     * Temporal 暂时只支持字符串(")和null
     *
     * @param buf
     * @param fromIndex
     * @param parameterizedType
     * @param defaultValue
     * @param endToken
     * @param jsonParseContext
     * @return
     * @throws Exception
     */
    @Override
    protected Object deserialize(CharSource charSource, char[] buf, int fromIndex, GenericParameterizedType parameterizedType, Object defaultValue, char endToken, JSONParseContext jsonParseContext) throws Exception {
        char beginChar = buf[fromIndex];
        switch (beginChar) {
            case '\'':
            case '"':
                if (patternType == 0) {
                    return deserializeDefault(buf, fromIndex + 1, beginChar, jsonParseContext);
                } else {
                    CHAR_SEQUENCE_STRING.skip(charSource, buf, fromIndex, beginChar, jsonParseContext);
                    int endIndex = jsonParseContext.endIndex;
                    try {
                        return deserializeTemporal(buf, fromIndex, endIndex, jsonParseContext);
                    } catch (Throwable throwable) {
                        if (throwable instanceof InvocationTargetException) {
                            throwable = ((InvocationTargetException) throwable).getTargetException();
                        }
                        String source = new String(buf, fromIndex + 1, endIndex - fromIndex - 1);
                        String errorContextTextAt = createErrorContextText(buf, fromIndex);
                        throw new JSONException("Syntax error, at pos " + fromIndex + ", context text by '" + errorContextTextAt + "', text '" + source + "' cannot convert to " + parameterizedType.getActualType() + ", exception: " + throwable.getMessage());
                    }
                }
            case 'n':
                return parseNull(buf, fromIndex, jsonParseContext);
            default: {
                if(supportedTime()) {
                    try {
                        long timestamp = (Long) NUMBER_LONG.deserialize(charSource, buf, fromIndex, GenericParameterizedType.LongType, null, endToken, jsonParseContext);
                        return fromTime(timestamp);
                    } catch (Throwable throwable) {
                    }
                }
                // not support or custom handle ?
                String errorContextTextAt = createErrorContextText(buf, fromIndex);
                throw new JSONException("Syntax error, at pos " + fromIndex + ", context text by '" + errorContextTextAt + "', unexpected '" + beginChar + "' for Temporal Type, expected '\"' ");
            }
        }
    }

    /**
     * Temporal 暂时只支持字符串(")和null
     *
     * @param buf
     * @param fromIndex
     * @param parameterizedType
     * @param defaultValue
     * @param endToken
     * @param jsonParseContext
     * @return
     * @throws Exception
     */
    @Override
    protected Object deserialize(CharSource charSource, byte[] buf, int fromIndex, GenericParameterizedType parameterizedType, Object defaultValue, byte endToken, JSONParseContext jsonParseContext) throws Exception {
        byte beginByte = buf[fromIndex];
        char beginChar = (char) beginByte;
        switch (beginChar) {
            case '\'':
            case '"':
                if(patternType == 0) {
                    return deserializeDefault(buf, fromIndex + 1, beginChar, jsonParseContext);
                } else {
                    CHAR_SEQUENCE_STRING.skip(charSource, buf, fromIndex, beginByte, jsonParseContext);
                    int endIndex = jsonParseContext.endIndex;
                    try {
                        return deserializeTemporal(buf, fromIndex, endIndex, jsonParseContext);
                    } catch (Throwable throwable) {
                        if (throwable instanceof InvocationTargetException) {
                            throwable = ((InvocationTargetException) throwable).getTargetException();
                        }
                        String source = new String(buf, fromIndex + 1, endIndex - fromIndex - 1);
                        String errorContextTextAt = createErrorContextText(buf, fromIndex);
                        throw new JSONException("Syntax error, at pos " + fromIndex + ", context text by '" + errorContextTextAt + "', text '" + source + "' cannot convert to " + parameterizedType.getActualType() + ", exception: " + throwable.getMessage());
                    }
                }
            case 'n':
                return parseNull(buf, fromIndex, jsonParseContext);
            default: {
                if(supportedTime()) {
                    try {
                        // long
                        long timestamp = (Long) NUMBER_LONG.deserialize(charSource, buf, fromIndex, GenericParameterizedType.LongType, null, endToken, jsonParseContext);
                        return fromTime(timestamp);
                    } catch (Throwable throwable) {
                    }
                }
                // not support or custom handle ?
                String errorContextTextAt = createErrorContextText(buf, fromIndex);
                throw new JSONException("Syntax error, at pos " + fromIndex + ", context text by '" + errorContextTextAt + "', unexpected '" + beginChar + "' for Temporal Type, expected '\"' ");
            }
        }
    }

    protected Object fromTime(long timestamp) {
        throw new UnsupportedOperationException();
    }

    protected boolean supportedTime() {
        return false;
    }

    protected abstract Object deserializeTemporal(char[] buf, int fromIndex, int toIndex, JSONParseContext jsonParseContext) throws Exception;

    protected abstract Object deserializeTemporal(byte[] buf, int fromIndex, int toIndex, JSONParseContext jsonParseContext) throws Exception;

    protected abstract Object deserializeDefault(char[] buf, int offset, char endChar, JSONParseContext jsonParseContext) throws Exception;
    protected abstract Object deserializeDefault(byte[] buf, int offset, char endChar, JSONParseContext jsonParseContext) throws Exception;

}
