package io.github.wycst.wast.json.temporal;

import io.github.wycst.wast.common.beans.GeneralDate;
import io.github.wycst.wast.common.beans.GregorianDate;
import io.github.wycst.wast.common.reflect.GenericParameterizedType;
import io.github.wycst.wast.common.utils.NumberUtils;
import io.github.wycst.wast.json.JSONParseContext;
import io.github.wycst.wast.json.JSONTemporalDeserializer;
import io.github.wycst.wast.json.exceptions.JSONException;

/**
 * 参考java.util.Date反序列化，使用反射实现
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
        return TemporalAloneInvoker.ofLocalDate(generalDate.getYear(), generalDate.getMonth(), generalDate.getDay());
    }

    protected Object deserializeTemporal(byte[] buf, int fromIndex, int endIndex, JSONParseContext jsonParseContext) throws Exception {
        // use dateTemplate && pattern
        GeneralDate generalDate = dateTemplate.parseGeneralDate(buf, fromIndex + 1, endIndex - fromIndex - 1, ZERO_TIME_ZONE);
        return TemporalAloneInvoker.ofLocalDate(generalDate.getYear(), generalDate.getMonth(), generalDate.getDay());
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
            return TemporalAloneInvoker.ofLocalDate(year, month, day);
        }
        String errorContextTextAt = createErrorContextText(buf, offset);
        throw new JSONException("Syntax error, at pos " + offset + ", context text by '" + errorContextTextAt + "', unexpected token '" + buf[offset] + "', expected '" + endChar + "'");
    }

    // default yyyy-MM-dd
    @Override
    protected Object deserializeDefaultTemporal(byte[] buf, int offset, char endChar, JSONParseContext jsonParseContext) throws Exception {
        int year = NumberUtils.parseInt4(buf, offset);
        int month = NumberUtils.parseInt2(buf, offset + 5);
        int day = NumberUtils.parseInt2(buf, offset + 8);
        offset += 10;
        if (buf[offset] == endChar) {
            jsonParseContext.endIndex = offset;
            return TemporalAloneInvoker.ofLocalDate(year, month, day);
        }
        String errorContextTextAt = createErrorContextText(buf, offset);
        throw new JSONException("Syntax error, at pos " + offset + ", context text by '" + errorContextTextAt + "', unexpected token '" + (char) buf[offset] + "', expected '" + endChar + "'");
    }
}
