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
    protected Object deserializeDefault(char[] buf, int offset, char endToken, JSONParseContext jsonParseContext) throws Exception {
        int i = offset, hour, minute, second;
        char c1, c2;
        boolean isDigitFlag;
        if ((isDigitFlag = isDigit(c1 = buf[i])) && isDigit(c2 = buf[++i])) {
            hour = twoDigitsValue(c1, c2);
            ++i;
        } else {
            if(isDigitFlag) {
                hour = c1 & 0xf;
            } else {
                String errorContextTextAt = createErrorContextText(buf, i);
                throw new JSONException("Syntax error, at pos " + i + ", context text by '" + errorContextTextAt + "', hour field error ");
            }
        }
        if ((isDigitFlag = isDigit(c1 = buf[++i])) && isDigit(c2 = buf[++i])) {
            minute = twoDigitsValue(c1, c2);
            ++i;
        } else {
            if(isDigitFlag) {
                minute = c1 & 0xf;
            } else {
                String errorContextTextAt = createErrorContextText(buf, i);
                throw new JSONException("Syntax error, at pos " + i + ", context text by '" + errorContextTextAt + "', minute field error ");
            }
        }
        if ((isDigitFlag = isDigit(c1 = buf[++i])) && isDigit(c2 = buf[++i])) {
            second = twoDigitsValue(c1, c2);
            ++i;
        } else {
            if(isDigitFlag) {
                second = c1 & 0xf;
            } else {
                String errorContextTextAt = createErrorContextText(buf, i);
                throw new JSONException("Syntax error, at pos " + i + ", context text by '" + errorContextTextAt + "', second field error ");
            }
        }
        int nanoOfSecond = 0;
        char c = buf[i];
        if (c == '.') {
            int cnt = 9;
            while ((isDigitFlag = isDigit(c = buf[++i])) && isDigit(c1 = buf[++i])) {
                cnt -= 2;
                nanoOfSecond = nanoOfSecond * 100 + twoDigitsValue(c, c1);;
            }
            if(isDigitFlag) {
                nanoOfSecond = (nanoOfSecond << 3) + (nanoOfSecond << 1) + (c & 0xf);
                c = c1;
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
        throw new JSONException("Syntax error, at pos " + i + ", context text by '" + errorContextTextAt + "', unexpected token '" + c1 + "', expected '" + endToken + "'");
    }

    // default hh:mm:ss.SSS
    @Override
    protected Object deserializeDefault(byte[] buf, int offset, char endToken, JSONParseContext jsonParseContext) throws Exception {
        int i = offset, hour, minute, second;
        byte b1, b2;
        boolean isDigitFlag;
        if ((isDigitFlag = isDigit(b1 = buf[i])) && isDigit(b2 = buf[++i])) {
            hour = twoDigitsValue(b1, b2);
            ++i;
        } else {
            if(isDigitFlag) {
                hour = b1 & 0xf;
            } else {
                String errorContextTextAt = createErrorContextText(buf, i);
                throw new JSONException("Syntax error, at pos " + i + ", context text by '" + errorContextTextAt + "', hour field error ");
            }
        }
        if ((isDigitFlag = isDigit(b1 = buf[++i])) && isDigit(b2 = buf[++i])) {
            minute = twoDigitsValue(b1, b2);
            ++i;
        } else {
            if(isDigitFlag) {
                minute = b1 & 0xf;
            } else {
                String errorContextTextAt = createErrorContextText(buf, i);
                throw new JSONException("Syntax error, at pos " + i + ", context text by '" + errorContextTextAt + "', minute field error ");
            }
        }
        if ((isDigitFlag = isDigit(b1 = buf[++i])) && isDigit(b2 = buf[++i])) {
            second = twoDigitsValue(b1, b2);
            ++i;
        } else {
            if(isDigitFlag) {
                second = b1 & 0xf;
            } else {
                String errorContextTextAt = createErrorContextText(buf, i);
                throw new JSONException("Syntax error, at pos " + i + ", context text by '" + errorContextTextAt + "', second field error ");
            }
        }
        int nanoOfSecond = 0;
        byte c = buf[i];
        if (c == '.') {
            int cnt = 9, val;
            ++i;
            while ((val = digits2Bytes(buf, i)) != -1) {
                i += 2;
                cnt -= 2;
                nanoOfSecond = nanoOfSecond * 100 + val;
            }
            if(isDigit(c = buf[i])) {
                nanoOfSecond = (nanoOfSecond << 3) + (nanoOfSecond << 1) + (c & 0xf);
                c = buf[++i];
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
