package io.github.wycst.wast.json.temporal;

import io.github.wycst.wast.common.beans.GeneralDate;
import io.github.wycst.wast.common.beans.GregorianDate;
import io.github.wycst.wast.common.reflect.GenericParameterizedType;
import io.github.wycst.wast.common.utils.NumberUtils;
import io.github.wycst.wast.json.JSONParseContext;
import io.github.wycst.wast.json.JSONTemporalDeserializer;
import io.github.wycst.wast.json.exceptions.JSONException;

import java.time.LocalDateTime;

/**
 * LocalDateTime反序列化
 *
 * @Author: wangy
 * @Date: 2022/8/13 15:06
 * @Description:
 * @see GregorianDate
 * @see io.github.wycst.wast.common.beans.DateTemplate
 */
public class TemporalLocalDateTimeDeserializer extends JSONTemporalDeserializer {

    public TemporalLocalDateTimeDeserializer(TemporalConfig temporalConfig) {
        super(temporalConfig);
    }

    protected void checkClass(GenericParameterizedType genericParameterizedType) {
    }

    protected Object deserializeTemporal(char[] buf, int fromIndex, int endIndex, JSONParseContext jsonParseContext) throws Exception {
        // use dateTemplate && pattern
        GeneralDate generalDate = dateTemplate.parseGeneralDate(buf, fromIndex + 1, endIndex - fromIndex - 1, ZERO_TIME_ZONE);
        return LocalDateTime.of(generalDate.getYear(), generalDate.getMonth(), generalDate.getDay(), generalDate.getHourOfDay(), generalDate.getMinute(), generalDate.getSecond(), generalDate.getMillisecond() * 1000000);
    }

    protected Object deserializeTemporal(byte[] buf, int fromIndex, int endIndex, JSONParseContext jsonParseContext) throws Exception {
        // use dateTemplate && pattern
        GeneralDate generalDate = dateTemplate.parseGeneralDate(buf, fromIndex + 1, endIndex - fromIndex - 1, ZERO_TIME_ZONE);
        return LocalDateTime.of(generalDate.getYear(), generalDate.getMonth(), generalDate.getDay(), generalDate.getHourOfDay(), generalDate.getMinute(), generalDate.getSecond(), generalDate.getMillisecond() * 1000000);
    }

    // default yyyy*MM*dd*HH*mm*ss
    @Override
    protected Object deserializeDefaultTemporal(char[] buf, int offset, char endChar, JSONParseContext jsonParseContext) throws Exception {
        int year = parseInt4(buf, offset);
        int month = parseInt2(buf, offset + 5);
        int day = parseInt2(buf, offset + 8);
        int hour = parseInt2(buf, offset + 11);
        int minute = parseInt2(buf, offset + 14);
        int second = parseInt2(buf, offset + 17);
        int nanoOfSecond = 0;
        offset += 19;
        char c = buf[offset];
        if(c == '.') {
            int cnt = 9;
            while (isDigit((c = buf[++offset]))) {
                nanoOfSecond = (nanoOfSecond << 3) + (nanoOfSecond << 1) + c - 48;
                --cnt;
            }
            if(cnt > 0) {
                nanoOfSecond *= NANO_OF_SECOND_PADDING[cnt];
            }
        }
        if(c == endChar) {
            jsonParseContext.endIndex = offset;
            return LocalDateTime.of(year, month, day, hour, minute, second, nanoOfSecond);
        }

        String errorContextTextAt = createErrorContextText(buf, offset);
        throw new JSONException("Syntax error, at pos " + offset + ", context text by '" + errorContextTextAt + "', unexpected token '" + c + "', expected '" + endChar + "'");
    }

    // default yyyy*MM*dd*HH*mm*ss
    @Override
    protected Object deserializeDefaultTemporal(byte[] buf, int offset, char endChar, JSONParseContext jsonParseContext) throws Exception {
        int year = NumberUtils.parseInt4(buf, offset);
        int month = NumberUtils.parseInt2(buf, offset + 5);
        int day = NumberUtils.parseInt2(buf, offset + 8);
        int hour = NumberUtils.parseInt2(buf, offset + 11);
        int minute = NumberUtils.parseInt2(buf, offset + 14);
        int second = NumberUtils.parseInt2(buf, offset + 17);
        int nanoOfSecond = 0;
        offset += 19;
        byte c = buf[offset];
        if(c == '.') {
            int cnt = 9;
            while (isDigit((c = buf[++offset]))) {
                nanoOfSecond = (nanoOfSecond << 3) + (nanoOfSecond << 1) + c - 48;
                --cnt;
            }
            if(cnt > 0) {
                nanoOfSecond *= NANO_OF_SECOND_PADDING[cnt];
            }
        }
        if(c == endChar) {
            jsonParseContext.endIndex = offset;
            return LocalDateTime.of(year, month, day, hour, minute, second, nanoOfSecond);
        }

        String errorContextTextAt = createErrorContextText(buf, offset);
        throw new JSONException("Syntax error, at pos " + offset + ", context text by '" + errorContextTextAt + "', unexpected token '" + (char) c + "', expected '" + endChar + "'");
    }

    @Override
    protected Object valueOf(String value, Class<?> actualType) throws Exception {
        return LocalDateTime.parse(value);
    }
}
