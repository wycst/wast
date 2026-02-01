package io.github.wycst.wast.json.temporal;

import io.github.wycst.wast.common.beans.DateTemplate;
import io.github.wycst.wast.common.beans.GeneralDate;
import io.github.wycst.wast.common.beans.GregorianDate;
import io.github.wycst.wast.common.reflect.GenericParameterizedType;
import io.github.wycst.wast.common.utils.NumberUtils;
import io.github.wycst.wast.json.JSONParseContext;
import io.github.wycst.wast.json.JSONTemporalDeserializer;
import io.github.wycst.wast.json.exceptions.JSONException;

import java.time.YearMonth;

/**
 * YearMonth反序列化
 *
 * @Author: wangy
 * @Description:
 * @see GregorianDate
 * @see DateTemplate
 */
public class TemporalYearMonthDeserializer extends JSONTemporalDeserializer {

    public TemporalYearMonthDeserializer(TemporalConfig temporalConfig) {
        super(temporalConfig);
    }

    protected void checkClass(GenericParameterizedType<?> genericParameterizedType) {
    }

    protected Object deserializeTemporal(char[] buf, int fromIndex, int endIndex, JSONParseContext jsonParseContext) throws Exception {
        // use dateTemplate && pattern
        GeneralDate generalDate = dateTemplate.parseGeneralDate(buf, fromIndex + 1, endIndex - fromIndex - 1, ZERO_TIME_ZONE);
        return YearMonth.of(generalDate.getYear(), generalDate.getMonth());
    }

    protected Object deserializeTemporal(byte[] buf, int fromIndex, int endIndex, JSONParseContext jsonParseContext) throws Exception {
        // use dateTemplate && pattern
        GeneralDate generalDate = dateTemplate.parseGeneralDate(buf, fromIndex + 1, endIndex - fromIndex - 1, ZERO_TIME_ZONE);
        return YearMonth.of(generalDate.getYear(), generalDate.getMonth());
    }

    // default ymd support yyyy-MM, yyyy/MM, yyyy.MM, yyyy~MM
    @Override
    protected YearMonth deserializeDefault(char[] buf, int offset, char endToken, JSONParseContext jsonParseContext) throws Exception {
        int year = fourDigitsYear(buf, offset), month, endIndex;
        // expected quick matching mode (yyyy-MM, yyyy/MM, yyyy.MM, yyyy~MM)
        char symbol;
        if (year != -1 && ((symbol = buf[offset + 4]) == '-' || symbol == '/' || symbol == '.' || symbol == '~')
                && (month = digits2Chars(buf, offset + 5)) != -1 && buf[endIndex = offset + 7] == endToken) {
            jsonParseContext.endIndex = endIndex;
            return YearMonth.of(year, month);
        }
        // compatible mode
        return compatibleDefault(year, buf, offset, endToken, jsonParseContext);
    }

    protected final static YearMonth compatibleDefault(int year, char[] buf, int offset, char endToken, JSONParseContext jsonParseContext) throws Exception {
        int i = offset, month;
        char c, symbol;
        if (year != -1) {
            i += 3;
        } else {
            if (buf[i] == '-' && (year = fourDigitsYear(buf, i + 1)) != -1) {
                year = -year;
                i += 4;
            } else {
                String errorContextTextAt = createErrorContextText(buf, i);
                throw new JSONException("Syntax error, at pos " + i + ", context text by '" + errorContextTextAt + "', year field error ");
            }
        }
        while (NumberUtils.isDigit(c = buf[++i])) {
            year = year * 10 + (c & 0xf);
        }
        if ((symbol = c) != '-' && symbol != '/' && symbol != '.' && symbol != '~') {
            String errorContextTextAt = createErrorContextText(buf, i);
            throw new JSONException("Syntax error, at pos " + i + ", context text by '" + errorContextTextAt + "', default symbol error, only support '-' or '/' or '.' or '~', but " + symbol);
        }
        if ((month = digits2Chars(buf, ++i)) == -1) {
            if (NumberUtils.isDigit(c = buf[i])) {
                month = c & 0xf;
            } else {
                String errorContextTextAt = createErrorContextText(buf, i);
                throw new JSONException("Syntax error, at pos " + i + ", context text by '" + errorContextTextAt + "', month field error ");
            }
        } else {
            ++i;
        }
        if (buf[++i] == endToken) {
            jsonParseContext.endIndex = i;
            return YearMonth.of(year, month);
        }
        String errorContextTextAt = createErrorContextText(buf, i);
        throw new JSONException("Syntax error, at pos " + i + ", context text by '" + errorContextTextAt + "', unexpected token '" + buf[i] + "', expected '" + (char) endToken + "'");
    }

    // default yyyy-MM compatible yyyy.MM?
    @Override
    protected YearMonth deserializeDefault(byte[] buf, int offset, byte endToken, JSONParseContext jsonParseContext) throws Exception {
        int year = fourDigitsYear(buf, offset), month, endIndex;
        // expected quick matching mode (yyyy-MM, yyyy/MM, yyyy.MM, yyyy~MM)
        byte symbol;
        if (year != -1 && ((symbol = buf[offset + 4]) == '-' || symbol == '/' || symbol == '.' || symbol == '~')
                && (month = digits2Bytes(buf, offset + 5)) != -1 && buf[endIndex = offset + 7] == endToken) {
            jsonParseContext.endIndex = endIndex;
            return YearMonth.of(year, month);
        }
        // compatible mode
        return compatibleDefault(year, buf, offset, endToken, jsonParseContext);
    }

    protected final static YearMonth compatibleDefault(int year, byte[] buf, int offset, byte endToken, JSONParseContext jsonParseContext) throws Exception {
        int i = offset, month;
        byte c, symbol;
        if (year != -1) {
            i += 3;
        } else {
            if (buf[i] == '-' && (year = fourDigitsYear(buf, i + 1)) != -1) {
                year = -year;
                i += 4;
            } else {
                String errorContextTextAt = createErrorContextText(buf, i);
                throw new JSONException("Syntax error, at pos " + i + ", context text by '" + errorContextTextAt + "', year field error ");
            }
        }
        while (NumberUtils.isDigit(c = buf[++i])) {
            year = year * 10 + (c & 0xf);
        }
        if ((symbol = c) != '-' && symbol != '/' && symbol != '.' && symbol != '~') {
            String errorContextTextAt = createErrorContextText(buf, i);
            throw new JSONException("Syntax error, at pos " + i + ", context text by '" + errorContextTextAt + "', default symbol error, only support '-' or '/' or '.' or '~', but " + symbol);
        }
        if ((month = digits2Bytes(buf, ++i)) == -1) {
            if (NumberUtils.isDigit(c = buf[i])) {
                month = c & 0xf;
            } else {
                String errorContextTextAt = createErrorContextText(buf, i);
                throw new JSONException("Syntax error, at pos " + i + ", context text by '" + errorContextTextAt + "', month field error ");
            }
        } else {
            ++i;
        }
        if (buf[++i] == endToken) {
            jsonParseContext.endIndex = i;
            return YearMonth.of(year, month);
        }
        String errorContextTextAt = createErrorContextText(buf, i);
        throw new JSONException("Syntax error, at pos " + i + ", context text by '" + errorContextTextAt + "', unexpected token '" + (char) buf[i] + "', expected '" + (char) endToken + "'");
    }

    @Override
    protected YearMonth valueOf(String value, Class<?> actualType) throws Exception {
        return YearMonth.parse(value);
    }
}
