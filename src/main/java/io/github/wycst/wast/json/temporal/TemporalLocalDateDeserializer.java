package io.github.wycst.wast.json.temporal;

import io.github.wycst.wast.common.beans.GeneralDate;
import io.github.wycst.wast.common.beans.GregorianDate;
import io.github.wycst.wast.common.reflect.GenericParameterizedType;
import io.github.wycst.wast.json.JSONParseContext;
import io.github.wycst.wast.json.JSONTemporalDeserializer;
import io.github.wycst.wast.json.exceptions.JSONException;

import java.time.LocalDate;

/**
 * LocalDate反序列化
 *
 * @Author: wangy
 * @Date: 2022/8/13 15:06
 * @Description:
 * @see GregorianDate
 * @see io.github.wycst.wast.common.beans.DateTemplate
 */
public class TemporalLocalDateDeserializer extends JSONTemporalDeserializer {

    public TemporalLocalDateDeserializer(TemporalConfig temporalConfig) {
        super(temporalConfig);
    }

    protected void checkClass(GenericParameterizedType genericParameterizedType) {
    }

    protected Object deserializeTemporal(char[] buf, int fromIndex, int endIndex, JSONParseContext jsonParseContext) throws Exception {
        // use dateTemplate && pattern
        GeneralDate generalDate = dateTemplate.parseGeneralDate(buf, fromIndex + 1, endIndex - fromIndex - 1, ZERO_TIME_ZONE);
        return LocalDate.of(generalDate.getYear(), generalDate.getMonth(), generalDate.getDay());
    }

    protected Object deserializeTemporal(byte[] buf, int fromIndex, int endIndex, JSONParseContext jsonParseContext) throws Exception {
        // use dateTemplate && pattern
        GeneralDate generalDate = dateTemplate.parseGeneralDate(buf, fromIndex + 1, endIndex - fromIndex - 1, ZERO_TIME_ZONE);
        return LocalDate.of(generalDate.getYear(), generalDate.getMonth(), generalDate.getDay());
    }

    // default supported yyyy?MM?dd
    @Override
    protected LocalDate deserializeDefault(char[] buf, int offset, char endToken, JSONParseContext jsonParseContext) throws Exception {
        int i = offset;
        int year, month, day;
        char c1, c2, c3, c4;
        if (isDigit(c1 = buf[i]) && isDigit(c2 = buf[++i]) && isDigit(c3 = buf[++i]) && isDigit(c4 = buf[++i])) {
            year = fourDigitsValue(c1 & 0xf, c2 & 0xf, c3 & 0xf, c4);
        } else {
            if (c1 == '-' && isDigit(c1 = buf[++i]) && isDigit(c2 = buf[++i]) && isDigit(c3 = buf[++i]) && isDigit(c4 = buf[++i])) {
                year = -fourDigitsValue(c1 & 0xf, c2 & 0xf, c3 & 0xf, c4);
            } else {
                String errorContextTextAt = createErrorContextText(buf, i);
                throw new JSONException("Syntax error, at pos " + i + ", context text by '" + errorContextTextAt + "', year field error ");
            }
        }
        while (isDigit(c1 = buf[++i])) {
            year = year * 10 + (c1 & 0xf);
        }
        boolean isDigitFlag;
        if ((isDigitFlag = isDigit(c1 = buf[++i])) && isDigit(c2 = buf[++i])) {
            month = twoDigitsValue(c1, c2);
            ++i;
        } else {
            if(isDigitFlag) {
                month = c1 & 0xf;
            } else {
                String errorContextTextAt = createErrorContextText(buf, i);
                throw new JSONException("Syntax error, at pos " + i + ", context text by '" + errorContextTextAt + "', month field error ");
            }
        }
        if ((isDigitFlag = isDigit(c1 = buf[++i])) && isDigit(c2 = buf[++i])) {
            day = twoDigitsValue(c1, c2);
            ++i;
        } else {
            if(isDigitFlag) {
                day = c1 & 0xf;
            } else {
                String errorContextTextAt = createErrorContextText(buf, i);
                throw new JSONException("Syntax error, at pos " + i + ", context text by '" + errorContextTextAt + "', day field error ");
            }
        }
        if (buf[i] == endToken) {
            jsonParseContext.endIndex = i;
            return LocalDate.of(year, month, day);
        }
        String errorContextTextAt = createErrorContextText(buf, i);
        throw new JSONException("Syntax error, at pos " + i + ", context text by '" + errorContextTextAt + "', unexpected token '" + buf[i] + "', expected '" + endToken + "'");
    }

    // default yyyy-MM-dd compatible yyyy.MM?.dd?
    @Override
    protected LocalDate deserializeDefault(byte[] buf, int offset, char endToken, JSONParseContext jsonParseContext) throws Exception {
        int i = offset;
        int year, month, day;
        byte b1, b2, b3, b4;
        if (isDigit(b1 = buf[i]) && isDigit(b2 = buf[++i]) && isDigit(b3 = buf[++i]) && isDigit(b4 = buf[++i])) {
            year = fourDigitsValue(b1 & 0xf, b2 & 0xf, b3 & 0xf, b4);
        } else {
            if (b1 == '-' && isDigit(b1 = buf[++i]) && isDigit(b2 = buf[++i]) && isDigit(b3 = buf[++i]) && isDigit(b4 = buf[++i])) {
                year = -fourDigitsValue(b1 & 0xf, b2 & 0xf, b3 & 0xf, b4);
            } else {
                String errorContextTextAt = createErrorContextText(buf, i);
                throw new JSONException("Syntax error, at pos " + i + ", context text by '" + errorContextTextAt + "', year field error ");
            }
        }
        while (isDigit(b1 = buf[++i])) {
            year = year * 10 + (b1 & 0xf);
        }
        boolean isDigitFlag;
        if ((isDigitFlag = isDigit(b1 = buf[++i])) && isDigit(b2 = buf[++i])) {
            month = twoDigitsValue(b1, b2);
            ++i;
        } else {
            if(isDigitFlag) {
                month = b1 & 0xf;
            } else {
                String errorContextTextAt = createErrorContextText(buf, i);
                throw new JSONException("Syntax error, at pos " + i + ", context text by '" + errorContextTextAt + "', month field error ");
            }
        }
        if ((isDigitFlag = isDigit(b1 = buf[++i])) && isDigit(b2 = buf[++i])) {
            day = twoDigitsValue(b1, b2);
            ++i;
        } else {
            if(isDigitFlag) {
                day = b1 & 0xf;
            } else {
                String errorContextTextAt = createErrorContextText(buf, i);
                throw new JSONException("Syntax error, at pos " + i + ", context text by '" + errorContextTextAt + "', day field error ");
            }
        }
        if (buf[i] == endToken) {
            jsonParseContext.endIndex = i;
            return LocalDate.of(year, month, day);
        }
        String errorContextTextAt = createErrorContextText(buf, i);
        throw new JSONException("Syntax error, at pos " + i + ", context text by '" + errorContextTextAt + "', unexpected token '" + (char) buf[i] + "', expected '" + endToken + "'");
    }

    @Override
    protected LocalDate valueOf(String value, Class<?> actualType) throws Exception {
        return LocalDate.parse(value);
    }
}
