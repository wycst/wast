package io.github.wycst.wast.json.temporal;

import io.github.wycst.wast.common.beans.GeneralDate;
import io.github.wycst.wast.common.reflect.GenericParameterizedType;
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
public class TemporalLocalTimeDeserializer extends JSONTemporalDeserializer {

    public TemporalLocalTimeDeserializer(GenericParameterizedType genericParameterizedType) {
        super(genericParameterizedType);
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
            GeneralDate generalDate = dateTemplate.parseGeneralDate(buf, fromIndex + 1, endIndex - fromIndex - 1, null);
            return TemporalAloneInvoker.ofLocalTime(generalDate.getHourOfDay(), generalDate.getMinute(), generalDate.getSecond(), generalDate.getMillisecond() * 1000000);
        }
    }
}