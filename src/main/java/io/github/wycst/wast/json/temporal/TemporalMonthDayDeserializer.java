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
import java.time.MonthDay;

/**
 * MonthDay反序列化
 *
 * @Author: wangy
 * @Description:
 * @see GregorianDate
 * @see DateTemplate
 */
public class TemporalMonthDayDeserializer extends JSONTemporalDeserializer {

    public TemporalMonthDayDeserializer(TemporalConfig temporalConfig) {
        super(temporalConfig);
    }

    protected void checkClass(GenericParameterizedType<?> genericParameterizedType) {
    }

    protected MonthDay deserializeTemporal(char[] buf, int fromIndex, int endIndex, JSONParseContext parseContext) throws Exception {
        // use dateTemplate && pattern
        GeneralDate generalDate = dateTemplate.parseGeneralDate(buf, fromIndex + 1, endIndex - fromIndex - 1, ZERO_TIME_ZONE);
        return MonthDay.of(generalDate.getMonth(), generalDate.getDay());
    }

    protected MonthDay deserializeTemporal(byte[] buf, int fromIndex, int endIndex, JSONParseContext parseContext) throws Exception {
        // use dateTemplate && pattern
        GeneralDate generalDate = dateTemplate.parseGeneralDate(buf, fromIndex + 1, endIndex - fromIndex - 1, ZERO_TIME_ZONE);
        return MonthDay.of(generalDate.getMonth(), generalDate.getDay());
    }

    // default md support MM-dd, MM/dd, MM.dd, MM~dd
    @Override
    protected MonthDay deserializeDefault(char[] buf, int offset, char endToken, JSONParseContext parseContext) throws Exception {
        int month, day, endIndex;
        // expected quick matching mode (MM-dd, --MM-dd)
        if (buf[offset] == '-' && buf[offset + 1] == '-') {
            offset += 2;
        }
        if ((month = digits2Chars(buf, offset)) != -1 && (buf[offset + 2] == '-')
                && (day = digits2Chars(buf, offset + 3)) != -1 && buf[endIndex = offset + 5] == endToken) {
            parseContext.endIndex = endIndex;
            return MonthDay.of(month, day);
        }
        // compatible mode
        return compatibleDefault(buf, offset, endToken, parseContext);
    }

    protected final static MonthDay compatibleDefault(char[] buf, int offset, char endToken, JSONParseContext parseContext) throws Exception {
        int i = offset, month, day;
        char c, symbol;
        if ((month = digits2Chars(buf, i)) == -1) {
            if (NumberUtils.isDigit(c = buf[i])) {
                month = c & 0xf;
            } else {
                String errorContextTextAt = createErrorContextText(buf, i);
                throw new JSONException("Syntax error, at pos " + i + ", context text by '" + errorContextTextAt + "', month field error ");
            }
        } else {
            ++i;
        }
        if ((symbol = buf[++i]) != '-') {
            String errorContextTextAt = createErrorContextText(buf, i);
            throw new JSONException("Syntax error, at pos " + i + ", context text by '" + errorContextTextAt + "', default symbol error, only support '-', but " + symbol);
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
            parseContext.endIndex = i;
            return MonthDay.of(month, day);
        }
        String errorContextTextAt = createErrorContextText(buf, i);
        throw new JSONException("Syntax error, at pos " + i + ", context text by '" + errorContextTextAt + "', unexpected token '" + buf[i] + "', expected '" + (char) endToken + "'");
    }

    // default yyyy-MM-dd compatible yyyy.MM?.dd?
    @Override
    protected MonthDay deserializeDefault(byte[] buf, int offset, byte endToken, JSONParseContext parseContext) throws Exception {
        int month, day, endIndex;
        // expected quick matching mode (MM-dd, --MM-dd)
        if (buf[offset] == '-' && buf[offset + 1] == '-') {
            offset += 2;
        }
        if ((month = digits2Bytes(buf, offset)) != -1 && (buf[offset + 2] == '-')
                && (day = digits2Bytes(buf, offset + 3)) != -1 && buf[endIndex = offset + 5] == endToken) {
            parseContext.endIndex = endIndex;
            return MonthDay.of(month, day);
        }
        // compatible mode
        return compatibleDefault(buf, offset, endToken, parseContext);
    }

    protected final static MonthDay compatibleDefault(byte[] buf, int offset, byte endToken, JSONParseContext parseContext) throws Exception {
        int i = offset, month, day;
        byte c, symbol;
        if ((month = digits2Bytes(buf, i)) == -1) {
            if (NumberUtils.isDigit(c = buf[i])) {
                month = c & 0xf;
            } else {
                String errorContextTextAt = createErrorContextText(buf, i);
                throw new JSONException("Syntax error, at pos " + i + ", context text by '" + errorContextTextAt + "', month field error ");
            }
        } else {
            ++i;
        }
        if ((symbol = buf[++i]) != '-') {
            String errorContextTextAt = createErrorContextText(buf, i);
            throw new JSONException("Syntax error, at pos " + i + ", context text by '" + errorContextTextAt + "', default symbol error, only support '-', but " + symbol);
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
            parseContext.endIndex = i;
            return MonthDay.of(month, day);
        }
        String errorContextTextAt = createErrorContextText(buf, i);
        throw new JSONException("Syntax error, at pos " + i + ", context text by '" + errorContextTextAt + "', unexpected token '" + (char) buf[i] + "', expected '" + (char) endToken + "'");
    }

    @Override
    protected LocalDate valueOf(String value, Class<?> actualType) throws Exception {
        return LocalDate.parse(value);
    }
}
