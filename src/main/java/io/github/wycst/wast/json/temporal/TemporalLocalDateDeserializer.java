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
public class TemporalLocalDateDeserializer extends JSONTemporalDeserializer {

    public TemporalLocalDateDeserializer(GenericParameterizedType genericParameterizedType) {
        super(genericParameterizedType);
    }

    protected void checkClass(GenericParameterizedType genericParameterizedType) {
        if (genericParameterizedType.getActualType() != TemporalAloneInvoker.localDateClass) {
            throw new UnsupportedOperationException("Not Support for class " + genericParameterizedType.getActualType());
        }
    }

    protected Object deserializeTemporal(char[] buf, int fromIndex, int endIndex, JSONParseContext jsonParseContext) throws Exception {
        if (patternType == 0) {
            // default yyyy-MM-dd
            int year = parseInt4(buf, fromIndex + 1);
            int month = parseInt2(buf, fromIndex + 6);
            int day = parseInt2(buf, fromIndex + 9);
            return TemporalAloneInvoker.ofLocalDate(year, month, day);
        } else {
            // use dateTemplate && pattern
            GeneralDate generalDate = dateTemplate.parseGeneralDate(buf, fromIndex + 1, endIndex - fromIndex - 1, null);
            return TemporalAloneInvoker.ofLocalDate(generalDate.getYear(), generalDate.getMonth(), generalDate.getDay());
        }
    }
}
