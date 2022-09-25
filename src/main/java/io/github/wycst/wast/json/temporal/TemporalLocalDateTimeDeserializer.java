package io.github.wycst.wast.json.temporal;

import io.github.wycst.wast.common.beans.GeneralDate;
import io.github.wycst.wast.common.reflect.GenericParameterizedType;
import io.github.wycst.wast.common.utils.NumberUtils;
import io.github.wycst.wast.json.JSONTemporalDeserializer;
import io.github.wycst.wast.json.options.JSONParseContext;

/**
 * 参考java.util.Date反序列化，使用反射实现
 *
 * @Author: wangy
 * @Date: 2022/8/13 15:06
 * @Description:
 * @see io.github.wycst.wast.common.beans.Date
 * @see io.github.wycst.wast.common.beans.DateTemplate
 */
public class TemporalLocalDateTimeDeserializer extends JSONTemporalDeserializer {

    public TemporalLocalDateTimeDeserializer(GenericParameterizedType genericParameterizedType) {
        super(genericParameterizedType);
    }

    protected void checkClass(GenericParameterizedType genericParameterizedType) {
        if (genericParameterizedType.getActualType() != TemporalAloneInvoker.localDateTimeClass) {
            throw new UnsupportedOperationException("Not Support for class " + genericParameterizedType.getActualType());
        }
    }

    protected Object deserializeTemporal(char[] buf, int fromIndex, int endIndex, JSONParseContext jsonParseContext) throws Exception {
        if (patternType == 0) {
            // default yyyy-MM-ddTHH:mm:ss
            int year = parseInt4(buf, fromIndex + 1);
            int month = parseInt2(buf, fromIndex + 6);
            int day = parseInt2(buf, fromIndex + 9);
            int hour = parseInt2(buf, fromIndex + 12);
            int minute = parseInt2(buf, fromIndex + 15);
            int second = parseInt2(buf, fromIndex + 18);
            return TemporalAloneInvoker.ofLocalDateTime(year, month, day, hour, minute, second, 0);
        } else {
            // use dateTemplate && pattern
            GeneralDate generalDate = dateTemplate.parseGeneralDate(buf, fromIndex + 1, endIndex - fromIndex - 1, null);
            return TemporalAloneInvoker.ofLocalDateTime(generalDate.getYear(), generalDate.getMonth(), generalDate.getDay(), generalDate.getHourOfDay(), generalDate.getMinute(), generalDate.getSecond(), generalDate.getMillisecond() * 1000000);
        }
    }

    protected Object deserializeTemporal(byte[] buf, int fromIndex, int endIndex, JSONParseContext jsonParseContext) throws Exception {
        if (patternType == 0) {
            // default yyyy-MM-ddTHH:mm:ss
            int year = NumberUtils.parseInt4(buf, fromIndex + 1);
            int month = NumberUtils.parseInt2(buf, fromIndex + 6);
            int day = NumberUtils.parseInt2(buf, fromIndex + 9);
            int hour = NumberUtils.parseInt2(buf, fromIndex + 12);
            int minute = NumberUtils.parseInt2(buf, fromIndex + 15);
            int second = NumberUtils.parseInt2(buf, fromIndex + 18);
            return TemporalAloneInvoker.ofLocalDateTime(year, month, day, hour, minute, second, 0);
        } else {
            // use dateTemplate && pattern
            GeneralDate generalDate = dateTemplate.parseGeneralDate(buf, fromIndex + 1, endIndex - fromIndex - 1, null);
            return TemporalAloneInvoker.ofLocalDateTime(generalDate.getYear(), generalDate.getMonth(), generalDate.getDay(), generalDate.getHourOfDay(), generalDate.getMinute(), generalDate.getSecond(), generalDate.getMillisecond() * 1000000);
        }
    }

}
