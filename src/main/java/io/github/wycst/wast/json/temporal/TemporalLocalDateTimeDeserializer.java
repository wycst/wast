package io.github.wycst.wast.json.temporal;

import io.github.wycst.wast.common.beans.GeneralDate;
import io.github.wycst.wast.common.beans.GregorianDate;
import io.github.wycst.wast.common.reflect.GenericParameterizedType;
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
    protected Object deserializeDefaultTemporal(char[] buf, int offset, char endToken, JSONParseContext jsonParseContext) throws Exception {
        int i = offset;
        int year, month, day, hour, minute, second;
        char c1, c2, c3, c4;
        if (isDigit(c1 = buf[i]) && isDigit(c2 = buf[++i]) && isDigit(c3 = buf[++i]) && isDigit(c4 = buf[++i])) {
            year = c1 * 1000 + c2 * 100 + c3 * 10 + c4 - 53328;
        } else {
            if (c1 == '-' && isDigit(c1 = buf[++i]) && isDigit(c2 = buf[++i]) && isDigit(c3 = buf[++i]) && isDigit(c4 = buf[++i])) {
                year = c1 * 1000 + c2 * 100 + c3 * 10 + c4 -53328;
                year = -year;
            } else {
                String errorContextTextAt = createErrorContextText(buf, i);
                throw new JSONException("Syntax error, at pos " + i + ", context text by '" + errorContextTextAt + "', year field error ");
            }
        }
        ++i;
        if (isDigit(c1 = buf[++i])) {
            month = c1 - 48;
        } else {
            String errorContextTextAt = createErrorContextText(buf, i);
            throw new JSONException("Syntax error, at pos " + i + ", context text by '" + errorContextTextAt + "', month field error ");
        }
        if (isDigit(c1 = buf[++i])) {
            month = (month << 3) + (month << 1) + c1 - 48;
        }
        ++i;
        if (isDigit(c1 = buf[++i])) {
            day = c1 - 48;
        } else {
            String errorContextTextAt = createErrorContextText(buf, i);
            throw new JSONException("Syntax error, at pos " + i + ", context text by '" + errorContextTextAt + "', day field error ");
        }
        if (isDigit(c1 = buf[++i])) {
            day = (day << 3) + (day << 1) + c1 - 48;
        }
        ++i;
        if (isDigit(c1 = buf[++i])) {
            hour = c1 - 48;
        } else {
            String errorContextTextAt = createErrorContextText(buf, i);
            throw new JSONException("Syntax error, at pos " + i + ", context text by '" + errorContextTextAt + "', hour field error ");
        }
        if (isDigit(c1 = buf[++i])) {
            hour = (hour << 3) + (hour << 1) + c1 - 48;
        }
        ++i;
        if (isDigit(c1 = buf[++i])) {
            minute = c1 - 48;
        } else {
            String errorContextTextAt = createErrorContextText(buf, i);
            throw new JSONException("Syntax error, at pos " + i + ", context text by '" + errorContextTextAt + "', minute field error ");
        }
        if (isDigit(c1 = buf[++i])) {
            minute = (minute << 3) + (minute << 1) + c1 - 48;
        }

        ++i;
        if (isDigit(c1 = buf[++i])) {
            second = c1 - 48;
        } else {
            String errorContextTextAt = createErrorContextText(buf, i);
            throw new JSONException("Syntax error, at pos " + i + ", context text by '" + errorContextTextAt + "', second field error ");
        }
        if (isDigit(c1 = buf[++i])) {
            second = (second << 3) + (second << 1) + c1 - 48;
            c1 = buf[++i];
        }

        int nanoOfSecond = 0;
        char c = c1;
        if (c == '.') {
            int cnt = 9;
            boolean isDigitFlag;
            while ((isDigitFlag = isDigit(c = buf[++i])) && isDigit(c1 = buf[++i])) {
                cnt -= 2;
                nanoOfSecond = nanoOfSecond * 100 + c * 10 + c1 - 528;
            }
            if(isDigitFlag) {
                nanoOfSecond = (nanoOfSecond << 3) + (nanoOfSecond << 1) + c - 48;
                c = c1;
                --cnt;
            }
            if (cnt > 0) {
                nanoOfSecond *= NANO_OF_SECOND_PADDING[cnt];
            }
        }
        if (c == endToken) {
            jsonParseContext.endIndex = i;
            return LocalDateTime.of(year, month, day, hour, minute, second, nanoOfSecond);
        }
        String errorContextTextAt = createErrorContextText(buf, i);
        throw new JSONException("Syntax error, at pos " + i + ", context text by '" + errorContextTextAt + "', unexpected token '" + c + "', expected '" + endToken + "'");
    }

    // default yyyy*MM*dd*HH*mm*ss
    @Override
    protected Object deserializeDefaultTemporal(byte[] buf, int offset, char endToken, JSONParseContext jsonParseContext) throws Exception {
        int i = offset;
        int year, month, day, hour, minute, second;
        byte b1, b2, b3, b4;
        if (isDigit(b1 = buf[i]) && isDigit(b2 = buf[++i]) && isDigit(b3 = buf[++i]) && isDigit(b4 = buf[++i])) {
            year = b1 * 1000 + b2 * 100 + b3 * 10 + b4 - 53328;
        } else {
            if (b1 == '-' && isDigit(b1 = buf[++i]) && isDigit(b2 = buf[++i]) && isDigit(b3 = buf[++i]) && isDigit(b4 = buf[++i])) {
                year = b1 * 1000 + b2 * 100 + b3 * 10 + b4 -53328;
                year = -year;
            } else {
                String errorContextTextAt = createErrorContextText(buf, i);
                throw new JSONException("Syntax error, at pos " + i + ", context text by '" + errorContextTextAt + "', year field error ");
            }
        }
        ++i;
        if (isDigit(b1 = buf[++i])) {
            month = b1 - 48;
        } else {
            String errorContextTextAt = createErrorContextText(buf, i);
            throw new JSONException("Syntax error, at pos " + i + ", context text by '" + errorContextTextAt + "', month field error ");
        }
        if (isDigit(b1 = buf[++i])) {
            month = (month << 3) + (month << 1) + b1 - 48;
        }
        ++i;
        if (isDigit(b1 = buf[++i])) {
            day = b1 - 48;
        } else {
            String errorContextTextAt = createErrorContextText(buf, i);
            throw new JSONException("Syntax error, at pos " + i + ", context text by '" + errorContextTextAt + "', day field error ");
        }
        if (isDigit(b1 = buf[++i])) {
            day = (day << 3) + (day << 1) + b1 - 48;
        }
        ++i;
        if (isDigit(b1 = buf[++i])) {
            hour = b1 - 48;
        } else {
            String errorContextTextAt = createErrorContextText(buf, i);
            throw new JSONException("Syntax error, at pos " + i + ", context text by '" + errorContextTextAt + "', hour field error ");
        }
        if (isDigit(b1 = buf[++i])) {
            hour = (hour << 3) + (hour << 1) + b1 - 48;
        }
        ++i;
        if (isDigit(b1 = buf[++i])) {
            minute = b1 - 48;
        } else {
            String errorContextTextAt = createErrorContextText(buf, i);
            throw new JSONException("Syntax error, at pos " + i + ", context text by '" + errorContextTextAt + "', minute field error ");
        }
        if (isDigit(b1 = buf[++i])) {
            minute = (minute << 3) + (minute << 1) + b1 - 48;
        }

        ++i;
        if (isDigit(b1 = buf[++i])) {
            second = b1 - 48;
        } else {
            String errorContextTextAt = createErrorContextText(buf, i);
            throw new JSONException("Syntax error, at pos " + i + ", context text by '" + errorContextTextAt + "', second field error ");
        }
        if (isDigit(b1 = buf[++i])) {
            second = (second << 3) + (second << 1) + b1 - 48;
            b1 = buf[++i];
        }

        int nanoOfSecond = 0;
        byte c = b1;
        if (c == '.') {
            int cnt = 9;
            boolean isDigitFlag;
            while ((isDigitFlag = isDigit(c = buf[++i])) && isDigit(b1 = buf[++i])) {
                cnt -= 2;
                nanoOfSecond = nanoOfSecond * 100 + c * 10 + b1 - 528;
            }
            if(isDigitFlag) {
                nanoOfSecond = (nanoOfSecond << 3) + (nanoOfSecond << 1) + c - 48;
                c = b1;
                --cnt;
            }
            if (cnt > 0) {
                nanoOfSecond *= NANO_OF_SECOND_PADDING[cnt];
            }
        }
        if (c == endToken) {
            jsonParseContext.endIndex = i;
            return LocalDateTime.of(year, month, day, hour, minute, second, nanoOfSecond);
        }
        String errorContextTextAt = createErrorContextText(buf, i);
        throw new JSONException("Syntax error, at pos " + i + ", context text by '" + errorContextTextAt + "', unexpected token '" + (char) c + "', expected '" + endToken + "'");
    }

    @Override
    protected Object valueOf(String value, Class<?> actualType) throws Exception {
        return LocalDateTime.parse(value);
    }

    @Override
    protected Object fromTime(long timestamp) {
        GeneralDate generalDate = new GeneralDate(timestamp);
        return LocalDateTime.of(generalDate.getYear(), generalDate.getMonth(), generalDate.getDay(), generalDate.getHourOfDay(), generalDate.getMinute(), generalDate.getSecond(), generalDate.getMillisecond() * 1000000);
    }

    @Override
    protected boolean supportedTime() {
        return true;
    }
}
