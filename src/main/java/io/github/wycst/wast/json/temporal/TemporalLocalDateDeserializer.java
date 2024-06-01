package io.github.wycst.wast.json.temporal;

import io.github.wycst.wast.common.beans.GeneralDate;
import io.github.wycst.wast.common.beans.GregorianDate;
import io.github.wycst.wast.common.reflect.GenericParameterizedType;
import io.github.wycst.wast.common.utils.NumberUtils;
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
    protected Object deserializeDefaultTemporal(char[] buf, int offset, char endChar, JSONParseContext jsonParseContext) throws Exception {
        int year = NumberUtils.parseInt4(buf, offset);
        int month = NumberUtils.parseInt2(buf, offset + 5);
        int day = NumberUtils.parseInt2(buf, offset + 8);
        offset += 10;
        if (buf[offset] == endChar) {
            jsonParseContext.endIndex = offset;
            return LocalDate.of(year, month, day);
        }
        String errorContextTextAt = createErrorContextText(buf, offset);
        throw new JSONException("Syntax error, at pos " + offset + ", context text by '" + errorContextTextAt + "', unexpected token '" + buf[offset] + "', expected '" + endChar + "'");
    }

    // default yyyy-MM-dd
    @Override
    protected Object deserializeDefaultTemporal(byte[] buf, int offset, char endChar, JSONParseContext jsonParseContext) throws Exception {
//        int year = NumberUtils.parseInt4(buf, offset);
//        int month = NumberUtils.parseInt2(buf, offset + 5);
//        int day = NumberUtils.parseInt2(buf, offset + 8);
        int i = offset;
        int year, month, day;
        byte b1, b2, b3, b4;
        if (isDigit(b1 = buf[i]) && isDigit(b2 = buf[++i]) && isDigit(b3 = buf[++i]) && isDigit(b4 = buf[++i])) {
            year = b1 * 1000 + b2 * 100 + b3 * 10 + b4 - 53328;
        } else {
            if (b1 == '-' && isDigit(b1 = buf[++i]) && isDigit(b2 = buf[++i]) && isDigit(b3 = buf[++i]) && isDigit(b4 = buf[++i])) {
                year = b1 * 1000 + b2 * 100 + b3 * 10 + b4 - 53328;
                year = -year;
            } else {
                String errorContextTextAt = createErrorContextText(buf, offset);
                throw new JSONException("Syntax error, at pos " + offset + ", context text by '" + errorContextTextAt + "', year field error ");
            }
        }
        ++i;
        if (isDigit(b1 = buf[++i])) {
            month = b1 - 48;
        } else {
            String errorContextTextAt = createErrorContextText(buf, offset);
            throw new JSONException("Syntax error, at pos " + offset + ", context text by '" + errorContextTextAt + "', month field error ");
        }
        if (isDigit(b1 = buf[++i])) {
            month = (month << 3) + (month << 1) + b1 - 48;
        }
        ++i;
        if (isDigit(b1 = buf[++i])) {
            day = b1 - 48;
        } else {
            String errorContextTextAt = createErrorContextText(buf, offset);
            throw new JSONException("Syntax error, at pos " + offset + ", context text by '" + errorContextTextAt + "', day field error ");
        }
        if (isDigit(b1 = buf[++i])) {
            day = (day << 3) + (day << 1) + b1 - 48;
            b1 = buf[++i];
        }
        if (b1 == endChar) {
            jsonParseContext.endIndex = i;
            return LocalDate.of(year, month, day);
        }
        String errorContextTextAt = createErrorContextText(buf, offset);
        throw new JSONException("Syntax error, at pos " + offset + ", context text by '" + errorContextTextAt + "', unexpected token '" + (char) buf[offset] + "', expected '" + endChar + "'");
    }

    @Override
    protected Object valueOf(String value, Class<?> actualType) throws Exception {
        return LocalDate.parse(value);
    }
}
