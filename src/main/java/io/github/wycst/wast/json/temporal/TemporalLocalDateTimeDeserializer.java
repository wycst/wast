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
    protected Object deserializeDefault(char[] buf, int offset, char endToken, JSONParseContext parseContext) throws Exception {
        LocalDateTime localDateTime = parseLocalDateTime(buf, offset, parseContext);
        int i = parseContext.endIndex;
        char c = buf[i];
        if (c == endToken) {
            return localDateTime;
        }
        String errorContextTextAt = createErrorContextText(buf, i);
        throw new JSONException("Syntax error, at pos " + i + ", context text by '" + errorContextTextAt + "', unexpected token '" + c + "', expected '" + endToken + "'");
    }

    // default yyyy*MM*dd*HH*mm*ss
    @Override
    protected Object deserializeDefault(byte[] buf, int offset, byte endToken, JSONParseContext parseContext) throws Exception {
        LocalDateTime localDateTime = parseLocalDateTime(buf, offset, parseContext);
        int i = parseContext.endIndex;
        int c = buf[i];
        if (c == endToken) {
            return localDateTime;
        }
        String errorContextTextAt = createErrorContextText(buf, i);
        throw new JSONException("Syntax error, at pos " + i + ", context text by '" + errorContextTextAt + "', unexpected token '" + (char) c + "', expected '" + (char) endToken + "'");
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
