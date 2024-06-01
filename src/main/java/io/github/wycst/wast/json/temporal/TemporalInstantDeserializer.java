package io.github.wycst.wast.json.temporal;

import io.github.wycst.wast.common.beans.GeneralDate;
import io.github.wycst.wast.common.beans.GregorianDate;
import io.github.wycst.wast.common.reflect.GenericParameterizedType;
import io.github.wycst.wast.common.utils.NumberUtils;
import io.github.wycst.wast.json.JSONParseContext;
import io.github.wycst.wast.json.JSONTemporalDeserializer;
import io.github.wycst.wast.json.exceptions.JSONException;

import java.time.Instant;

/**
 * Instant反序列化
 *
 * @Author: wangy
 * @Date: 2022/8/13 15:06
 * @Description:
 * @see GregorianDate
 * @see io.github.wycst.wast.common.beans.DateTemplate
 */
public class TemporalInstantDeserializer extends JSONTemporalDeserializer {

    public TemporalInstantDeserializer(TemporalConfig temporalConfig) {
        super(temporalConfig);
    }

    protected void checkClass(GenericParameterizedType genericParameterizedType) {
    }

    protected Object deserializeTemporal(char[] buf, int fromIndex, int endIndex, JSONParseContext jsonParseContext) throws Exception {
        long millis = dateTemplate.parseTime(buf, fromIndex + 1, endIndex - fromIndex - 1, ZERO_TIME_ZONE);
        return Instant.ofEpochMilli(millis);
    }

    protected Object deserializeTemporal(byte[] buf, int fromIndex, int endIndex, JSONParseContext jsonParseContext) throws Exception {
        long millis = dateTemplate.parseTime(buf, fromIndex + 1, endIndex - fromIndex - 1, ZERO_TIME_ZONE);
        return Instant.ofEpochMilli(millis);
    }

    // use default pattern yyyy*MM*dd*HH*mm*ss.SZ?
    @Override
    protected Object deserializeDefaultTemporal(char[] buf, int offset, char endChar, JSONParseContext jsonParseContext) throws Exception {
        int year = NumberUtils.parseInt4(buf, offset);
        int month = NumberUtils.parseInt2(buf, offset + 5);
        int day = NumberUtils.parseInt2(buf, offset + 8);
        int hour = NumberUtils.parseInt2(buf, offset + 11);
        int minute = NumberUtils.parseInt2(buf, offset + 14);
        int second = NumberUtils.parseInt2(buf, offset + 17);
        long millis = GeneralDate.getTime(year, month, day, hour, minute, second, 0, ZERO_TIME_ZONE);
        int nanoOfSecond = 0;
        offset += 19;
        char c = buf[offset];
        if (c == '.') {
            int cnt = 9;
            while (isDigit((c = buf[++offset]))) {
                nanoOfSecond = (nanoOfSecond << 3) + (nanoOfSecond << 1) + c - 48;
                --cnt;
            }
            if (cnt > 0) {
                nanoOfSecond *= NANO_OF_SECOND_PADDING[cnt];
            }
        }
        switch (c) {
            case 'z':
            case 'Z': {
                c = buf[++offset];
            }
            default: {
                if (c == endChar) {
                    jsonParseContext.endIndex = offset;
                    return Instant.ofEpochSecond(millis / 1000, nanoOfSecond);
                }
            }
        }
        String errorContextTextAt = createErrorContextText(buf, offset);
        throw new JSONException("Syntax error, at pos " + offset + ", context text by '" + errorContextTextAt + "', unexpected token '" + c + "', expected '" + endChar + "'");
    }

    // use default supported pattern like yyyy?MM?dd?HH:mm:ss.SZ
    @Override
    protected Object deserializeDefaultTemporal(byte[] buf, int offset, char endChar, JSONParseContext jsonParseContext) throws Exception {
        int year = NumberUtils.parseInt4(buf, offset);
        int month = NumberUtils.parseInt2(buf, offset + 5);
        int day = NumberUtils.parseInt2(buf, offset + 8);
        int hour = NumberUtils.parseInt2(buf, offset + 11);
        int minute = NumberUtils.parseInt2(buf, offset + 14);
        int second = NumberUtils.parseInt2(buf, offset + 17);
        long millis = GeneralDate.getTime(year, month, day, hour, minute, second, 0, ZERO_TIME_ZONE);

        int nanoOfSecond = 0;
        offset += 19;
        byte c = buf[offset];
        if (c == '.') {
            int cnt = 9;
            while (isDigit((c = buf[++offset]))) {
                nanoOfSecond = (nanoOfSecond << 3) + (nanoOfSecond << 1) + c - 48;
                --cnt;
            }
            if (cnt > 0) {
                nanoOfSecond *= NANO_OF_SECOND_PADDING[cnt];
            }
        }
        switch (c) {
            case 'z':
            case 'Z': {
                c = buf[++offset];
            }
            default: {
                if (c == endChar) {
                    jsonParseContext.endIndex = offset;
                    return Instant.ofEpochSecond(millis / 1000, nanoOfSecond);
                }
            }
        }
        String errorContextTextAt = createErrorContextText(buf, offset);
        throw new JSONException("Syntax error, at pos " + offset + ", context text by '" + errorContextTextAt + "', unexpected token '" + (char) c + "', expected '" + endChar + "'");
    }

    @Override
    protected Object valueOf(String value, Class<?> actualType) throws Exception {
        return Instant.parse(value);
    }
}
