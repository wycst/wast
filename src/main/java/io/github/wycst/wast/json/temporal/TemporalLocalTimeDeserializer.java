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
public class TemporalLocalTimeDeserializer extends JSONTemporalDeserializer {

    public TemporalLocalTimeDeserializer(TemporalConfig temporalConfig) {
        super(temporalConfig);
    }

    protected void checkClass(GenericParameterizedType genericParameterizedType) {
//        if (genericParameterizedType.getActualType() != TemporalAloneInvoker.localTimeClass) {
//            throw new UnsupportedOperationException("Not Support for class " + genericParameterizedType.getActualType());
//        }
    }

    protected Object deserializeTemporal(char[] buf, int fromIndex, int endIndex, JSONParseContext jsonParseContext) throws Exception {
        // use dateTemplate && pattern
        GeneralDate generalDate = dateTemplate.parseGeneralDate(buf, fromIndex + 1, endIndex - fromIndex - 1, ZERO_TIME_ZONE);
        return TemporalAloneInvoker.ofLocalTime(generalDate.getHourOfDay(), generalDate.getMinute(), generalDate.getSecond(), generalDate.getMillisecond() * 1000000);
    }

    protected Object deserializeTemporal(byte[] buf, int fromIndex, int endIndex, JSONParseContext jsonParseContext) throws Exception {
        // use dateTemplate && pattern
        GeneralDate generalDate = dateTemplate.parseGeneralDate(buf, fromIndex + 1, endIndex - fromIndex - 1, ZERO_TIME_ZONE);
        return TemporalAloneInvoker.ofLocalTime(generalDate.getHourOfDay(), generalDate.getMinute(), generalDate.getSecond(), generalDate.getMillisecond() * 1000000);
    }

    // default hh:mm:ss.SSS
    @Override
    protected Object deserializeDefaultTemporal(char[] buf, int offset, char endChar, JSONParseContext jsonParseContext) throws Exception {
        int hour = NumberUtils.parseInt2(buf, offset);
        int minute = NumberUtils.parseInt2(buf, offset + 3);
        int second = NumberUtils.parseInt2(buf, offset + 6);
        offset += 8;
        int nanoOfSecond = 0;
        char c = buf[offset];
        if(c == '.') {
            int cnt = 9;
            while (isDigit((c = buf[++offset]))) {
                nanoOfSecond = (nanoOfSecond << 3) + (nanoOfSecond << 1) + c - 48;
                --cnt;
            }
            if(cnt > 0) {
                nanoOfSecond *= NANO_OF_SECOND_PADDING[cnt];
            }
        }
        if(c == endChar) {
            jsonParseContext.endIndex = offset;
            return TemporalAloneInvoker.ofLocalTime(hour, minute, second, nanoOfSecond);
        }

        String errorContextTextAt = createErrorContextText(buf, offset);
        throw new JSONException("Syntax error, at pos " + offset + ", context text by '" + errorContextTextAt + "', unexpected token '" + c + "', expected '" + endChar + "'");
    }

    // default hh:mm:ss.SSS
    @Override
    protected Object deserializeDefaultTemporal(byte[] buf, int offset, char endChar, JSONParseContext jsonParseContext) throws Exception {
        int hour = NumberUtils.parseInt2(buf, offset);
        int minute = NumberUtils.parseInt2(buf, offset + 3);
        int second = NumberUtils.parseInt2(buf, offset + 6);
        offset += 8;
        int nanoOfSecond = 0;
        byte c = buf[offset];
        if(c == '.') {
            int cnt = 9;
            while (isDigit((c = buf[++offset]))) {
                nanoOfSecond = (nanoOfSecond << 3) + (nanoOfSecond << 1) + c - 48;
                --cnt;
            }
            if(cnt > 0) {
                nanoOfSecond *= NANO_OF_SECOND_PADDING[cnt];
            }
        }
        if(c == endChar) {
            jsonParseContext.endIndex = offset;
            return TemporalAloneInvoker.ofLocalTime(hour, minute, second, nanoOfSecond);
        }

        String errorContextTextAt = createErrorContextText(buf, offset);
        throw new JSONException("Syntax error, at pos " + offset + ", context text by '" + errorContextTextAt + "', unexpected token '" + (char) c + "', expected '" + endChar + "'");
    }
}
