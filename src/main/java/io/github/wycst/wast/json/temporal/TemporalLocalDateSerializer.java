package io.github.wycst.wast.json.temporal;

import io.github.wycst.wast.common.beans.GregorianDate;
import io.github.wycst.wast.json.JSONConfig;
import io.github.wycst.wast.json.JSONTemporalSerializer;
import io.github.wycst.wast.json.JSONWriter;
import io.github.wycst.wast.json.annotations.JsonProperty;

/**
 * LocalDate序列化，使用反射实现
 *
 * @Author: wangy
 * @Date: 2022/8/13 15:06
 * @Description:
 * @see GregorianDate
 * @see io.github.wycst.wast.common.beans.DateTemplate
 */
public class TemporalLocalDateSerializer extends JSONTemporalSerializer {

    public TemporalLocalDateSerializer(Class<?> temporalClass, JsonProperty property) {
        super(temporalClass, property);
    }

    protected void checkClass(Class<?> temporalClass) {
    }

    @Override
    protected void writeTemporalWithTemplate(Object value, JSONWriter writer, JSONConfig jsonConfig) throws Exception {
        int year = TemporalAloneInvoker.invokeLocalDateYear(value);
        int month = TemporalAloneInvoker.invokeLocalDateMonth(value);
        int day = TemporalAloneInvoker.invokeLocalDateDay(value);
        writer.write('"');
        writeDate(year, month, day, 0, 0, 0, 0, dateFormatter, writer);
        writer.write('"');
    }

    // yyyy-MM-dd
    @Override
    protected void writeDefault(Object value, JSONWriter writer, JSONConfig jsonConfig, int indent) throws Exception {
        int year = TemporalAloneInvoker.invokeLocalDateYear(value);
        int month = TemporalAloneInvoker.invokeLocalDateMonth(value);
        int day = TemporalAloneInvoker.invokeLocalDateDay(value);
        writer.writeJSONLocalDate(year, month, day);
    }
}
