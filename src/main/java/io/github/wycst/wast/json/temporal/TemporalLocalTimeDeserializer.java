package io.github.wycst.wast.json.temporal;

import io.github.wycst.wast.common.beans.GeneralDate;
import io.github.wycst.wast.common.beans.GregorianDate;
import io.github.wycst.wast.common.reflect.GenericParameterizedType;
import io.github.wycst.wast.json.JSONParseContext;
import io.github.wycst.wast.json.JSONTemporalDeserializer;
import io.github.wycst.wast.json.exceptions.JSONException;

import java.time.LocalTime;

/**
 * LocalTime反序列化
 *
 * @Author: wangy
 * @Date: 2022/8/13 15:06
 * @Description:
 * @see GregorianDate
 * @see io.github.wycst.wast.common.beans.DateTemplate
 */
public class TemporalLocalTimeDeserializer extends JSONTemporalDeserializer {

    public TemporalLocalTimeDeserializer(TemporalConfig temporalConfig) {
        super(temporalConfig);
    }

    protected void checkClass(GenericParameterizedType genericParameterizedType) {
    }

    protected Object deserializeTemporal(char[] buf, int fromIndex, int endIndex, JSONParseContext jsonParseContext) throws Exception {
        // use dateTemplate && pattern
        GeneralDate generalDate = dateTemplate.parseGeneralDate(buf, fromIndex + 1, endIndex - fromIndex - 1, ZERO_TIME_ZONE);
        return LocalTime.of(generalDate.getHourOfDay(), generalDate.getMinute(), generalDate.getSecond(), generalDate.getMillisecond() * 1000000);
    }

    protected Object deserializeTemporal(byte[] buf, int fromIndex, int endIndex, JSONParseContext jsonParseContext) throws Exception {
        // use dateTemplate && pattern
        GeneralDate generalDate = dateTemplate.parseGeneralDate(buf, fromIndex + 1, endIndex - fromIndex - 1, ZERO_TIME_ZONE);
        return LocalTime.of(generalDate.getHourOfDay(), generalDate.getMinute(), generalDate.getSecond(), generalDate.getMillisecond() * 1000000);
    }

    // default hh:mm:ss.SSS
    @Override
    protected Object deserializeDefaultTemporal(char[] buf, int offset, char endToken, JSONParseContext jsonParseContext) throws Exception {
        int i = offset, hour, minute, second;
        char c;
        if (isDigit(c = buf[i])) {
            hour = c - 48;
        } else {
            String errorContextTextAt = createErrorContextText(buf, i);
            throw new JSONException("Syntax error, at pos " + i + ", context text by '" + errorContextTextAt + "', hour field error ");
        }
        if (isDigit(c = buf[++i])) {
            hour = (hour << 3) + (hour << 1) + c - 48;
        }
        ++i;
        if (isDigit(c = buf[++i])) {
            minute = c - 48;
        } else {
            String errorContextTextAt = createErrorContextText(buf, i);
            throw new JSONException("Syntax error, at pos " + i + ", context text by '" + errorContextTextAt + "', minute field error ");
        }
        if (isDigit(c = buf[++i])) {
            minute = (minute << 3) + (minute << 1) + c - 48;
        }
        ++i;
        if (isDigit(c = buf[++i])) {
            second = c - 48;
        } else {
            String errorContextTextAt = createErrorContextText(buf, i);
            throw new JSONException("Syntax error, at pos " + i + ", context text by '" + errorContextTextAt + "', second field error ");
        }
        if (isDigit(c = buf[++i])) {
            second = (second << 3) + (second << 1) + c - 48;
            c = buf[++i];
        }
        int nanoOfSecond = 0;
        char c1 = c;
        if (c1 == '.') {
            int cnt = 9;
            boolean isDigitFlag;
            while ((isDigitFlag = isDigit(c1 = buf[++i])) && isDigit(c = buf[++i])) {
                cnt -= 2;
                nanoOfSecond = nanoOfSecond * 100 + c1 * 10 + c - 528;
            }
            if(isDigitFlag) {
                nanoOfSecond = (nanoOfSecond << 3) + (nanoOfSecond << 1) + c1 - 48;
                c1 = c;
                --cnt;
            }
            if (cnt > 0) {
                nanoOfSecond *= NANO_OF_SECOND_PADDING[cnt];
            }
        }
        if (c1 == endToken) {
            jsonParseContext.endIndex = i;
            return LocalTime.of(hour, minute, second, nanoOfSecond);
        }
        String errorContextTextAt = createErrorContextText(buf, i);
        throw new JSONException("Syntax error, at pos " + i + ", context text by '" + errorContextTextAt + "', unexpected token '" + c1 + "', expected '" + endToken + "'");
    }

    // default hh:mm:ss.SSS
    @Override
    protected Object deserializeDefaultTemporal(byte[] buf, int offset, char endToken, JSONParseContext jsonParseContext) throws Exception {
        int i = offset, hour, minute, second;
        byte b;
        if (isDigit(b = buf[i])) {
            hour = b - 48;
        } else {
            String errorContextTextAt = createErrorContextText(buf, i);
            throw new JSONException("Syntax error, at pos " + i + ", context text by '" + errorContextTextAt + "', hour field error ");
        }
        if (isDigit(b = buf[++i])) {
            hour = (hour << 3) + (hour << 1) + b - 48;
        }
        ++i;
        if (isDigit(b = buf[++i])) {
            minute = b - 48;
        } else {
            String errorContextTextAt = createErrorContextText(buf, i);
            throw new JSONException("Syntax error, at pos " + i + ", context text by '" + errorContextTextAt + "', minute field error ");
        }
        if (isDigit(b = buf[++i])) {
            minute = (minute << 3) + (minute << 1) + b - 48;
        }
        ++i;
        if (isDigit(b = buf[++i])) {
            second = b - 48;
        } else {
            String errorContextTextAt = createErrorContextText(buf, i);
            throw new JSONException("Syntax error, at pos " + i + ", context text by '" + errorContextTextAt + "', second field error ");
        }
        if (isDigit(b = buf[++i])) {
            second = (second << 3) + (second << 1) + b - 48;
            b = buf[++i];
        }
        int nanoOfSecond = 0;
        byte c = b;
        if (c == '.') {
            int cnt = 9;
            boolean isDigitFlag;
            while ((isDigitFlag = isDigit(c = buf[++i])) && isDigit(b = buf[++i])) {
                cnt -= 2;
                nanoOfSecond = nanoOfSecond * 100 + c * 10 + b - 528;
            }
            if(isDigitFlag) {
                nanoOfSecond = (nanoOfSecond << 3) + (nanoOfSecond << 1) + c - 48;
                c = b;
                --cnt;
            }
            if (cnt > 0) {
                nanoOfSecond *= NANO_OF_SECOND_PADDING[cnt];
            }
        }
        if (c == endToken) {
            jsonParseContext.endIndex = i;
            return LocalTime.of(hour, minute, second, nanoOfSecond);
        }
        String errorContextTextAt = createErrorContextText(buf, i);
        throw new JSONException("Syntax error, at pos " + i + ", context text by '" + errorContextTextAt + "', unexpected token '" + (char) c + "', expected '" + endToken + "'");
    }

    @Override
    protected Object valueOf(String value, Class<?> actualType) throws Exception {
        return LocalTime.parse(value);
    }
}
