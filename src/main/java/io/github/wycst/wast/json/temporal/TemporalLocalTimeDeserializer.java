package io.github.wycst.wast.json.temporal;

import io.github.wycst.wast.common.beans.GeneralDate;
import io.github.wycst.wast.common.beans.GregorianDate;
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
 * @see GregorianDate
 * @see io.github.wycst.wast.common.beans.DateTemplate
 */
public class TemporalLocalTimeDeserializer extends JSONTemporalDeserializer {

    public TemporalLocalTimeDeserializer(TemporalConfig temporalConfig) {
        super(temporalConfig);
    }

    protected void checkClass(GenericParameterizedType genericParameterizedType) {
        if (genericParameterizedType.getActualType() != TemporalAloneInvoker.localTimeClass) {
            throw new UnsupportedOperationException("Not Support for class " + genericParameterizedType.getActualType());
        }
    }

    protected Object deserializeTemporal(char[] buf, int fromIndex, int endIndex, JSONParseContext jsonParseContext) throws Exception {
        if (patternType == 0) {
            // default hh:mm:ss
            int h = parseInt2(buf, fromIndex + 1);
            int m = parseInt2(buf, fromIndex + 4);
            int s = parseInt2(buf, fromIndex + 7);
            return TemporalAloneInvoker.ofLocalTime(h, m, s, 0);
        } else {
            // use dateTemplate && pattern
            GeneralDate generalDate = dateTemplate.parseGeneralDate(buf, fromIndex + 1, endIndex - fromIndex - 1, ZERO_TIME_ZONE);
            return TemporalAloneInvoker.ofLocalTime(generalDate.getHourOfDay(), generalDate.getMinute(), generalDate.getSecond(), generalDate.getMillisecond() * 1000000);
        }
    }

    protected Object deserializeTemporal(byte[] buf, int fromIndex, int endIndex, JSONParseContext jsonParseContext) throws Exception {
        if (patternType == 0) {
            // default hh:mm:ss
            int h = NumberUtils.parseInt2(buf, fromIndex + 1);
            int m = NumberUtils.parseInt2(buf, fromIndex + 4);
            int s = NumberUtils.parseInt2(buf, fromIndex + 7);
            return TemporalAloneInvoker.ofLocalTime(h, m, s, 0);
        } else {
            // use dateTemplate && pattern
            GeneralDate generalDate = dateTemplate.parseGeneralDate(buf, fromIndex + 1, endIndex - fromIndex - 1, ZERO_TIME_ZONE);
            return TemporalAloneInvoker.ofLocalTime(generalDate.getHourOfDay(), generalDate.getMinute(), generalDate.getSecond(), generalDate.getMillisecond() * 1000000);
        }
    }
}
