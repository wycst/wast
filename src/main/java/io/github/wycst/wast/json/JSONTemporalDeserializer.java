package io.github.wycst.wast.json;

import io.github.wycst.wast.common.beans.DateTemplate;
import io.github.wycst.wast.common.reflect.ClassStrucWrap;
import io.github.wycst.wast.common.reflect.GenericParameterizedType;
import io.github.wycst.wast.common.utils.EnvUtils;
import io.github.wycst.wast.common.utils.NumberUtils;
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

    final protected int patternType;
    final boolean isDefaultTemporal;
    protected DateTemplate dateTemplate;
    protected final static int[] NANO_OF_SECOND_PADDING = {1, 10, 100, 1000, 10000, 100000, 1000000, 10000000, 100000000, 1000000000};

    protected JSONTemporalDeserializer(TemporalConfig temporalConfig) {
        checkClass(temporalConfig.getGenericParameterizedType());
        String pattern = temporalConfig.getDatePattern();
        patternType = getPatternType(pattern);
        isDefaultTemporal = patternType == 0;
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
     * Temporal 支持字符串("/')/null/时间戳
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
    protected Object deserialize(CharSource charSource, char[] buf, int fromIndex, GenericParameterizedType parameterizedType, Object defaultValue, int endToken, JSONParseContext jsonParseContext) throws Exception {
        char beginChar = buf[fromIndex];
        if(beginChar == '"' || beginChar == '\'') {
            if (isDefaultTemporal) {
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
        }
        if(beginChar == 'n') {
            return parseNull(buf, fromIndex, jsonParseContext);
        }
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
    protected Object deserialize(CharSource charSource, byte[] buf, int fromIndex, GenericParameterizedType parameterizedType, Object defaultValue, int endToken, JSONParseContext jsonParseContext) throws Exception {
        byte beginByte = buf[fromIndex];
        if(beginByte == '"' || beginByte == '\'') {
            if(isDefaultTemporal) {
                return deserializeDefault(buf, fromIndex + 1, beginByte, jsonParseContext);
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
        }
        if(beginByte == 'n') {
            return parseNull(buf, fromIndex, jsonParseContext);
        }
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
        throw new JSONException("Syntax error, at pos " + fromIndex + ", context text by '" + errorContextTextAt + "', unexpected '" + (char) beginByte + "' for Temporal Type, expected '\"' ");
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
    protected abstract Object deserializeDefault(byte[] buf, int offset, byte endToken, JSONParseContext jsonParseContext) throws Exception;

    protected final static int parseFourDigitsYear(byte[] buf, int offset) {
        final int value = JSONUnsafe.getInt(buf, offset);
        if ((value & 0xF0F0F0F0) == 0x30303030) {
            return THREE_DIGITS_MUL10[(buf[offset] & 0xf) << 8 | (buf[offset + 1] & 0xf) << 4 | (buf[offset + 2] & 0xf)] + buf[offset + 3];
        }
        return -1;
    }

    protected final static int parseNanoOfSecond(byte[] buf, int offset, JSONParseContext parseContext) {
        int nanoOfSecond;
        if((nanoOfSecond = digits2Bytes(buf, offset)) != -1) {
            long mask;
            if ((mask = getDigits8Mask(buf, offset += 2)) != 0) {
                int cnt = (EnvUtils.LITTLE_ENDIAN ? Long.numberOfTrailingZeros(mask) >> 3 : Long.numberOfLeadingZeros(mask) >> 3);
                parseContext.endIndex = offset + cnt;
                if(cnt == 7) {
                    return nanoOfSecond * 10000000
                            + (THREE_DIGITS_MUL10[(buf[offset] & 0xf) << 4 | (buf[offset + 1] & 0xf)] + buf[offset + 2]) * 10000
                            + THREE_DIGITS_MUL10[(buf[offset + 3] & 0xf) << 8 | (buf[offset + 4] & 0xf) << 4 | (buf[offset + 5] & 0xf)] + buf[offset + 6];
                } else {
                    return calculateNextDigits(nanoOfSecond, buf, offset, cnt);
                }
            } else {
                String errorContextTextAt = createErrorContextText(buf, offset - 2);
                throw new JSONException("Syntax error, at pos " + offset + ", context text by '" + errorContextTextAt + "', illegal nanoOfSecond out of range(0-999999999)");
            }
        } else {
            int c;
            if (NumberUtils.isDigit(c = buf[offset])) {
                parseContext.endIndex = offset + 1;
                return (c & 0xf) * 100000000;
            }
        }
        String errorContextTextAt = createErrorContextText(buf, offset);
        throw new JSONException("Syntax error, at pos " + offset + ", context text by '" + errorContextTextAt + "', illegal nanoOfSecond");
    }

    protected final static int calculateNextDigits(int nanoOfSecond, byte[] buf, int offset, int cnt) {
        switch (cnt) {
            case 0: return nanoOfSecond * 10000000;
            case 1: return (nanoOfSecond * 10 + (buf[offset] & 0xF)) * 1000000;
            case 2: return (nanoOfSecond * 100 + TWO_DIGITS_VALUES[buf[offset] ^ ((buf[offset + 1] & 0xf) << 4)]) * 100000;
            case 3: return (nanoOfSecond * 1000 + THREE_DIGITS_MUL10[(buf[offset] & 0xf) << 4 | (buf[offset + 1] & 0xf)] + buf[offset + 2]) * 10000;
            case 4: return (nanoOfSecond * 10000 + THREE_DIGITS_MUL10[(buf[offset] & 0xf) << 8 | (buf[offset + 1] & 0xf) << 4 | (buf[offset + 2] & 0xf)] + buf[offset + 3]) * 1000;
            case 5: return (nanoOfSecond * 100000 + (buf[offset] & 0xf) * 10000 + THREE_DIGITS_MUL10[(buf[offset + 1] & 0xf) << 8 | (buf[offset + 2] & 0xf) << 4 | (buf[offset + 3] & 0xf)] + buf[offset + 4]) * 100;
            case 6: return (nanoOfSecond * 1000000 + (TWO_DIGITS_VALUES[buf[offset] ^ ((buf[offset + 1] & 0xf) << 4)]) * 10000 + THREE_DIGITS_MUL10[(buf[offset + 2] & 0xf) << 8 | (buf[offset + 3] & 0xf) << 4 | (buf[offset + 4] & 0xf)] + buf[offset + 5]) * 10;
        }
        return nanoOfSecond;
    }
}
