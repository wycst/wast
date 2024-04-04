package io.github.wycst.wast.json.temporal;

import io.github.wycst.wast.common.beans.GregorianDate;
import io.github.wycst.wast.json.JSONConfig;
import io.github.wycst.wast.json.JSONTemporalSerializer;
import io.github.wycst.wast.json.JSONWriter;
import io.github.wycst.wast.json.annotations.JsonProperty;

/**
 * LocalDateTime序列化，使用反射实现
 *
 * @Author: wangy
 * @Date: 2022/8/13 15:06
 * @Description:
 * @see GregorianDate
 * @see io.github.wycst.wast.common.beans.DateTemplate
 */
public class TemporalLocalDateTimeSerializer extends JSONTemporalSerializer {

    public TemporalLocalDateTimeSerializer(Class<?> temporalClass, JsonProperty property) {
        super(temporalClass, property);
    }

    protected void checkClass(Class<?> temporalClass) {
    }

    @Override
    protected void writeTemporalWithTemplate(Object value, JSONWriter writer, JSONConfig jsonConfig) throws Exception {
        int year = TemporalAloneInvoker.invokeLocalDateTimeYear(value);
        int month = TemporalAloneInvoker.invokeLocalDateTimeMonth(value);
        int day = TemporalAloneInvoker.invokeLocalDateTimeDay(value);
        int hour = TemporalAloneInvoker.invokeLocalDateTimeHour(value);
        int minute = TemporalAloneInvoker.invokeLocalDateTimeMinute(value);
        int second = TemporalAloneInvoker.invokeLocalDateTimeSecond(value);
        int nano = TemporalAloneInvoker.invokeLocalDateTimeNano(value);
        int millisecond = nano / 1000000;
        writer.write('"');
        writeDate(year, month, day, hour, minute, second, millisecond, dateFormatter, writer);
        writer.write('"');
    }

    // yyyy-MM-ddTHH:mm:ss.SSS
    @Override
    protected void writeDefault(Object value, JSONWriter writer, JSONConfig jsonConfig, int indent) throws Exception {
        int year = TemporalAloneInvoker.invokeLocalDateTimeYear(value);
        int month = TemporalAloneInvoker.invokeLocalDateTimeMonth(value);
        int day = TemporalAloneInvoker.invokeLocalDateTimeDay(value);
        int hour = TemporalAloneInvoker.invokeLocalDateTimeHour(value);
        int minute = TemporalAloneInvoker.invokeLocalDateTimeMinute(value);
        int second = TemporalAloneInvoker.invokeLocalDateTimeSecond(value);
        int nano = TemporalAloneInvoker.invokeLocalDateTimeNano(value);
        writer.writeJSONLocalDateTime(year, month, day, hour, minute, second, nano, null);
    }
}
