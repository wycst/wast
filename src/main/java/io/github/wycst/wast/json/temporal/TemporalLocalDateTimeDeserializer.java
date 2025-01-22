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
    protected Object deserializeDefault(char[] buf, int offset, char endToken, JSONParseContext jsonParseContext) throws Exception {
        int i = offset;
        int year, month, day, hour, minute, second;
        char c1, c2, c3, c4;
        if (NumberUtils.isDigit(c1 = buf[i]) && NumberUtils.isDigit(c2 = buf[++i]) && NumberUtils.isDigit(c3 = buf[++i]) && NumberUtils.isDigit(c4 = buf[++i])) {
            year = fourDigitsValue(c1 & 0xf, c2 & 0xf, c3 & 0xf, c4);
        } else {
            if (c1 == '-' && NumberUtils.isDigit(c1 = buf[++i]) && NumberUtils.isDigit(c2 = buf[++i]) && NumberUtils.isDigit(c3 = buf[++i]) && NumberUtils.isDigit(c4 = buf[++i])) {
                year = -fourDigitsValue(c1 & 0xf, c2 & 0xf, c3 & 0xf, c4);
            } else {
                String errorContextTextAt = createErrorContextText(buf, i);
                throw new JSONException("Syntax error, at pos " + i + ", context text by '" + errorContextTextAt + "', year field error ");
            }
        }
        while (NumberUtils.isDigit(c1 = buf[++i])) {
            year = year * 10 + (c1 & 0xf);
        }
        boolean isDigitFlag;
        if ((isDigitFlag = NumberUtils.isDigit(c1 = buf[++i])) && NumberUtils.isDigit(c2 = buf[++i])) {
            month = twoDigitsValue(c1, c2);
            ++i;
        } else {
            if (isDigitFlag) {
                month = c1 & 0xf;
            } else {
                String errorContextTextAt = createErrorContextText(buf, i);
                throw new JSONException("Syntax error, at pos " + i + ", context text by '" + errorContextTextAt + "', month field error ");
            }
        }
        if ((isDigitFlag = NumberUtils.isDigit(c1 = buf[++i])) && NumberUtils.isDigit(c2 = buf[++i])) {
            day = twoDigitsValue(c1, c2);
            ++i;
        } else {
            if (isDigitFlag) {
                day = c1 & 0xf;
            } else {
                String errorContextTextAt = createErrorContextText(buf, i);
                throw new JSONException("Syntax error, at pos " + i + ", context text by '" + errorContextTextAt + "', day field error ");
            }
        }
        if ((isDigitFlag = NumberUtils.isDigit(c1 = buf[++i])) && NumberUtils.isDigit(c2 = buf[++i])) {
            hour = twoDigitsValue(c1, c2);
            ++i;
        } else {
            if (isDigitFlag) {
                hour = c1 & 0xf;
            } else {
                String errorContextTextAt = createErrorContextText(buf, i);
                throw new JSONException("Syntax error, at pos " + i + ", context text by '" + errorContextTextAt + "', hour field error ");
            }
        }
        if ((isDigitFlag = NumberUtils.isDigit(c1 = buf[++i])) && NumberUtils.isDigit(c2 = buf[++i])) {
            minute = twoDigitsValue(c1, c2);
            ++i;
        } else {
            if (isDigitFlag) {
                minute = c1 & 0xf;
            } else {
                String errorContextTextAt = createErrorContextText(buf, i);
                throw new JSONException("Syntax error, at pos " + i + ", context text by '" + errorContextTextAt + "', minute field error ");
            }
        }
        if ((isDigitFlag = NumberUtils.isDigit(c1 = buf[++i])) && NumberUtils.isDigit(c2 = buf[++i])) {
            second = twoDigitsValue(c1, c2);
            ++i;
        } else {
            if (isDigitFlag) {
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
            while ((isDigitFlag = NumberUtils.isDigit(c = buf[++i])) && NumberUtils.isDigit(c1 = buf[++i])) {
                cnt -= 2;
                nanoOfSecond = nanoOfSecond * 100 + twoDigitsValue(c, c1);
                ;
            }
            if (isDigitFlag) {
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
            return LocalDateTime.of(year, month, day, hour, minute, second, nanoOfSecond);
        }
        String errorContextTextAt = createErrorContextText(buf, i);
        throw new JSONException("Syntax error, at pos " + i + ", context text by '" + errorContextTextAt + "', unexpected token '" + c + "', expected '" + endToken + "'");
    }

    // default yyyy*MM*dd*HH*mm*ss
    @Override
    protected Object deserializeDefault(byte[] buf, int offset, char endToken, JSONParseContext jsonParseContext) throws Exception {
        int i = offset;
        int year, month, day, hour, minute, second;
        byte b1, b2, b3, b4;
        if (NumberUtils.isDigit(b1 = buf[i]) && NumberUtils.isDigit(b2 = buf[++i]) && NumberUtils.isDigit(b3 = buf[++i]) && NumberUtils.isDigit(b4 = buf[++i])) {
            year = fourDigitsValue(b1 & 0xf, b2 & 0xf, b3 & 0xf, b4);
        } else {
            if (b1 == '-' && NumberUtils.isDigit(b1 = buf[++i]) && NumberUtils.isDigit(b2 = buf[++i]) && NumberUtils.isDigit(b3 = buf[++i]) && NumberUtils.isDigit(b4 = buf[++i])) {
                year = -fourDigitsValue(b1 & 0xf, b2 & 0xf, b3 & 0xf, b4);
            } else {
                String errorContextTextAt = createErrorContextText(buf, i);
                throw new JSONException("Syntax error, at pos " + i + ", context text by '" + errorContextTextAt + "', year field error ");
            }
        }
        while (NumberUtils.isDigit(b1 = buf[++i])) {
            year = year * 10 + (b1 & 0xf);
        }
        boolean isDigitFlag;
        if ((isDigitFlag = NumberUtils.isDigit(b1 = buf[++i])) && NumberUtils.isDigit(b2 = buf[++i])) {
            month = twoDigitsValue(b1, b2);
            ++i;
        } else {
            if (isDigitFlag) {
                month = b1 & 0xf;
            } else {
                String errorContextTextAt = createErrorContextText(buf, i);
                throw new JSONException("Syntax error, at pos " + i + ", context text by '" + errorContextTextAt + "', month field error ");
            }
        }
        if ((isDigitFlag = NumberUtils.isDigit(b1 = buf[++i])) && NumberUtils.isDigit(b2 = buf[++i])) {
            day = twoDigitsValue(b1, b2);
            ++i;
        } else {
            if (isDigitFlag) {
                day = b1 & 0xf;
            } else {
                String errorContextTextAt = createErrorContextText(buf, i);
                throw new JSONException("Syntax error, at pos " + i + ", context text by '" + errorContextTextAt + "', day field error ");
            }
        }
        if ((isDigitFlag = NumberUtils.isDigit(b1 = buf[++i])) && NumberUtils.isDigit(b2 = buf[++i])) {
            hour = twoDigitsValue(b1, b2);
            ++i;
        } else {
            if (isDigitFlag) {
                hour = b1 & 0xf;
            } else {
                String errorContextTextAt = createErrorContextText(buf, i);
                throw new JSONException("Syntax error, at pos " + i + ", context text by '" + errorContextTextAt + "', hour field error ");
            }
        }
        if ((isDigitFlag = NumberUtils.isDigit(b1 = buf[++i])) && NumberUtils.isDigit(b2 = buf[++i])) {
            minute = twoDigitsValue(b1, b2);
            ++i;
        } else {
            if (isDigitFlag) {
                minute = b1 & 0xf;
            } else {
                String errorContextTextAt = createErrorContextText(buf, i);
                throw new JSONException("Syntax error, at pos " + i + ", context text by '" + errorContextTextAt + "', minute field error ");
            }
        }
        if ((isDigitFlag = NumberUtils.isDigit(b1 = buf[++i])) && NumberUtils.isDigit(b2 = buf[++i])) {
            second = twoDigitsValue(b1, b2);
            ++i;
        } else {
            if (isDigitFlag) {
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
            if (NumberUtils.isDigit(c = buf[i])) {
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
