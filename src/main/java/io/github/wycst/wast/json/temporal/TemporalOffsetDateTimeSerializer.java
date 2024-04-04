package io.github.wycst.wast.json.temporal;

import io.github.wycst.wast.common.beans.GregorianDate;
import io.github.wycst.wast.json.JSONConfig;
import io.github.wycst.wast.json.JSONTemporalSerializer;
import io.github.wycst.wast.json.JSONWriter;
import io.github.wycst.wast.json.annotations.JsonProperty;

/**
 * OffsetDateTime序列化，使用反射实现
 *
 * @Author: wangy
 * @Date: 2022/8/13 15:06
 * @Description:
 * @see GregorianDate
 * @see io.github.wycst.wast.common.beans.DateTemplate
 */
public class TemporalOffsetDateTimeSerializer extends JSONTemporalSerializer {

    public TemporalOffsetDateTimeSerializer(Class<?> temporalClass, JsonProperty property) {
        super(temporalClass, property);
    }

    protected void checkClass(Class<?> temporalClass) {
    }

    @Override
    protected void writeTemporalWithTemplate(Object value, JSONWriter writer, JSONConfig jsonConfig) throws Exception {
        int year = TemporalAloneInvoker.invokeOffsetDateTimeYear(value);
        int month = TemporalAloneInvoker.invokeOffsetDateTimeMonth(value);
        int day = TemporalAloneInvoker.invokeOffsetDateTimeDay(value);
        int hour = TemporalAloneInvoker.invokeOffsetDateTimeHour(value);
        int minute = TemporalAloneInvoker.invokeOffsetDateTimeMinute(value);
        int second = TemporalAloneInvoker.invokeOffsetDateTimeSecond(value);
        int nano = TemporalAloneInvoker.invokeOffsetDateTimeNano(value);
        int millisecond = nano / 1000000;
        writer.write('"');
        // localDateTime
        writeDate(year, month, day, hour, minute, second, millisecond, dateFormatter, writer);
        String zoneId = TemporalAloneInvoker.invokeOffsetDateTimeZone(value).toString();
        writer.writeZoneId(zoneId);
        writer.write('"');
    }

    // yyyy-MM-ddTHH:mm:ss.SSS+xx:yy
    @Override
    protected void writeDefault(Object value, JSONWriter writer, JSONConfig jsonConfig, int indent) throws Exception {
        int year = TemporalAloneInvoker.invokeOffsetDateTimeYear(value);
        int month = TemporalAloneInvoker.invokeOffsetDateTimeMonth(value);
        int day = TemporalAloneInvoker.invokeOffsetDateTimeDay(value);
        int hour = TemporalAloneInvoker.invokeOffsetDateTimeHour(value);
        int minute = TemporalAloneInvoker.invokeOffsetDateTimeMinute(value);
        int second = TemporalAloneInvoker.invokeOffsetDateTimeSecond(value);
        int nano = TemporalAloneInvoker.invokeOffsetDateTimeNano(value);
        writer.writeJSONLocalDateTime(year, month, day, hour, minute, second, nano, TemporalAloneInvoker.invokeOffsetDateTimeZone(value).toString());
    }
}
