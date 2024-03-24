package io.github.wycst.wast.json.temporal;

import io.github.wycst.wast.common.beans.GregorianDate;
import io.github.wycst.wast.json.JSONConfig;
import io.github.wycst.wast.json.JSONTemporalSerializer;
import io.github.wycst.wast.json.JSONWriter;
import io.github.wycst.wast.json.annotations.JsonProperty;

/**
 * ZonedDateTime序列化，使用反射实现
 *
 * @Author: wangy
 * @Date: 2022/8/13 15:06
 * @Description:
 * @see GregorianDate
 * @see io.github.wycst.wast.common.beans.DateTemplate
 */
public class TemporalZonedDateTimeSerializer extends JSONTemporalSerializer {

    public TemporalZonedDateTimeSerializer(Class<?> temporalClass, JsonProperty property) {
        super(temporalClass, property);
    }

    protected void checkClass(Class<?> temporalClass) {
//        if (temporalClass != TemporalAloneInvoker.zonedDateTimeClass) {
//            throw new UnsupportedOperationException("not support for class temporal type " + temporalClass);
//        }
    }

    @Override
    protected void writeTemporalWithTemplate(Object value, JSONWriter writer, JSONConfig jsonConfig) throws Exception {
        int year = TemporalAloneInvoker.invokeZonedDateTimeYear(value);
        int month = TemporalAloneInvoker.invokeZonedDateTimeMonth(value);
        int day = TemporalAloneInvoker.invokeZonedDateTimeDay(value);
        int hour = TemporalAloneInvoker.invokeZonedDateTimeHour(value);
        int minute = TemporalAloneInvoker.invokeZonedDateTimeMinute(value);
        int second = TemporalAloneInvoker.invokeZonedDateTimeSecond(value);
        int nano = TemporalAloneInvoker.invokeZonedDateTimeNano(value);
        int millisecond = nano / 1000000;
        writer.write('"');
        // localDateTime
        writeDate(year, month, day, hour, minute, second, millisecond, dateFormatter, writer);
        String zoneId = TemporalAloneInvoker.invokeZonedDateTimeZone(value).toString();
        writeZoneId(writer, zoneId);
        writer.write('"');
    }

    // yyyy-MM-ddTHH:mm:ss.SSS+xx:yy or yyyy-MM-ddTHH:mm:ss.SSS[Asia/Shanghai]
    // note: toString有细微差别
    @Override
    protected void writeDefault(Object value, JSONWriter writer, JSONConfig jsonConfig, int indent) throws Exception {
        int year = TemporalAloneInvoker.invokeZonedDateTimeYear(value);
        int month = TemporalAloneInvoker.invokeZonedDateTimeMonth(value);
        int day = TemporalAloneInvoker.invokeZonedDateTimeDay(value);
        int hour = TemporalAloneInvoker.invokeZonedDateTimeHour(value);
        int minute = TemporalAloneInvoker.invokeZonedDateTimeMinute(value);
        int second = TemporalAloneInvoker.invokeZonedDateTimeSecond(value);
        int nano = TemporalAloneInvoker.invokeZonedDateTimeNano(value);
        writer.write('"');
        writer.writeLocalDateTime(year, month, day, hour, minute, second, nano);
        String zoneId = TemporalAloneInvoker.invokeZonedDateTimeZone(value).toString();
        writeZoneId(writer, zoneId);
        writer.write('"');
    }
}
