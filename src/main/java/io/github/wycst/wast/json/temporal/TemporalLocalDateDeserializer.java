package io.github.wycst.wast.json.temporal;

import io.github.wycst.wast.common.beans.DateTemplate;
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
 * @see DateTemplate
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

    // default ymd support yyyy-MM-dd, yyyy/MM/dd, yyyy.MM.dd, yyyy~MM~dd
    @Override
    protected LocalDate deserializeDefault(char[] buf, int offset, char endToken, JSONParseContext jsonParseContext) throws Exception {
        int year = fourDigitsYear(buf, offset), month, day, endIndex;
        // expected quick matching mode (yyyy-MM-dd, yyyy/MM/dd, yyyy.MM.dd, yyyy~MM~dd)
        char symbol;
        if (year != -1 && ((symbol = buf[offset + 4]) == '-' || symbol == '/' || symbol == '.' || symbol == '~')
                && (month = digits2Chars(buf, offset + 5)) != -1 && buf[offset + 7] == symbol
                && (day = digits2Chars(buf, offset + 8)) != -1 && buf[endIndex = offset + 10] == endToken) {
            jsonParseContext.endIndex = endIndex;
            return LocalDate.of(year, month, day);
        }
        // compatible mode
        return compatibleDefault(year, buf, offset, endToken, jsonParseContext);
    }

    protected final static LocalDate compatibleDefault(int year, char[] buf, int offset, char endToken, JSONParseContext jsonParseContext) throws Exception {
        int i = offset, month, day;
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
        if (buf[++i] != symbol) {
            String errorContextTextAt = createErrorContextText(buf, i);
            throw new JSONException("Syntax error, at pos " + i + ", context text by '" + errorContextTextAt + "', expect '" + symbol + "', but '" + buf[i] + "'");
        }
        if ((day = digits2Chars(buf, ++i)) == -1) {
            if (NumberUtils.isDigit(c = buf[i])) {
                day = c & 0xf;
            } else {
                String errorContextTextAt = createErrorContextText(buf, i);
                throw new JSONException("Syntax error, at pos " + i + ", context text by '" + errorContextTextAt + "', day field error ");
            }
        } else {
            ++i;
        }
        if (buf[++i] == endToken) {
            jsonParseContext.endIndex = i;
            return LocalDate.of(year, month, day);
        }
        String errorContextTextAt = createErrorContextText(buf, i);
        throw new JSONException("Syntax error, at pos " + i + ", context text by '" + errorContextTextAt + "', unexpected token '" + buf[i] + "', expected '" + (char) endToken + "'");
    }

    // default yyyy-MM-dd compatible yyyy.MM?.dd?
    @Override
    protected LocalDate deserializeDefault(byte[] buf, int offset, byte endToken, JSONParseContext jsonParseContext) throws Exception {
        int year = fourDigitsYear(buf, offset), month, day, endIndex;
        // expected quick matching mode (yyyy-MM-dd, yyyy/MM/dd, yyyy.MM.dd, yyyy~MM~dd)
        byte symbol;
        if (year != -1 && ((symbol = buf[offset + 4]) == '-' || symbol == '/' || symbol == '.' || symbol == '~')
                && (month = digits2Bytes(buf, offset + 5)) != -1 && buf[offset + 7] == symbol
                && (day = digits2Bytes(buf, offset + 8)) != -1 && buf[endIndex = offset + 10] == endToken) {
            jsonParseContext.endIndex = endIndex;
            return LocalDate.of(year, month, day);
        }
        // compatible mode
        return compatibleDefault(year, buf, offset, endToken, jsonParseContext);
    }

    protected final static LocalDate compatibleDefault(int year, byte[] buf, int offset, byte endToken, JSONParseContext jsonParseContext) throws Exception {
        int i = offset, month, day;
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
        if (buf[++i] != symbol) {
            String errorContextTextAt = createErrorContextText(buf, i);
            throw new JSONException("Syntax error, at pos " + i + ", context text by '" + errorContextTextAt + "', expect '" + (char) symbol + "', but '" + (char) buf[i] + "'");
        }
        if ((day = digits2Bytes(buf, ++i)) == -1) {
            if (NumberUtils.isDigit(c = buf[i])) {
                day = c & 0xf;
            } else {
                String errorContextTextAt = createErrorContextText(buf, i);
                throw new JSONException("Syntax error, at pos " + i + ", context text by '" + errorContextTextAt + "', day field error ");
            }
        } else {
            ++i;
        }
        if (buf[++i] == endToken) {
            jsonParseContext.endIndex = i;
            return LocalDate.of(year, month, day);
        }
        String errorContextTextAt = createErrorContextText(buf, i);
        throw new JSONException("Syntax error, at pos " + i + ", context text by '" + errorContextTextAt + "', unexpected token '" + (char) buf[i] + "', expected '" + (char) endToken + "'");
    }

    @Override
    protected LocalDate valueOf(String value, Class<?> actualType) throws Exception {
        return LocalDate.parse(value);
    }
}
