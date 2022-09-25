package io.github.wycst.wast.json.temporal;

import io.github.wycst.wast.json.JSONTemporalSerializer;
import io.github.wycst.wast.json.annotations.JsonProperty;
import io.github.wycst.wast.json.options.JsonConfig;
import io.github.wycst.wast.json.reflect.ObjectStructureWrapper;

import java.io.IOException;
import java.io.Writer;

/**
 * ZonedDateTime序列化，使用反射实现
 *
 * @Author: wangy
 * @Date: 2022/8/13 15:06
 * @Description:
 * @see io.github.wycst.wast.common.beans.Date
 * @see io.github.wycst.wast.common.beans.DateTemplate
 */
public class TemporalZonedDateTimeSerializer extends JSONTemporalSerializer {

    public TemporalZonedDateTimeSerializer(ObjectStructureWrapper objectStructureWrapper, JsonProperty property) {
        super(objectStructureWrapper, property);
    }

    protected void checkClass(ObjectStructureWrapper objectStructureWrapper) {
        Class<?> sourceClass = objectStructureWrapper.getSourceClass();
        if (sourceClass != TemporalAloneInvoker.zonedDateTimeClass) {
            throw new UnsupportedOperationException("Not Support for class temporal type " + sourceClass);
        }
    }

    @Override
    protected void writeTemporalWithTemplate(Object value, Writer writer, JsonConfig jsonConfig) throws Exception {
        int year = TemporalAloneInvoker.invokeZonedDateTimeYear(value).intValue();
        int month = TemporalAloneInvoker.invokeZonedDateTimeMonth(value).intValue();
        int day = TemporalAloneInvoker.invokeZonedDateTimeDay(value).intValue();
        int hour = TemporalAloneInvoker.invokeZonedDateTimeHour(value).intValue();
        int minute = TemporalAloneInvoker.invokeZonedDateTimeMinute(value).intValue();
        int second = TemporalAloneInvoker.invokeZonedDateTimeSecond(value).intValue();
        int nano = TemporalAloneInvoker.invokeZonedDateTimeNano(value).intValue();
        int millisecond = nano / 1000000;
        String zoneId = TemporalAloneInvoker.invokeZonedDateTimeZone(value).toString();
        writer.append('"');
        // localDateTime
        dateTemplate.formatTo(year, month, day, hour, minute, second, millisecond, writer, true);
        writeZoneId(writer, zoneId);
        writer.append('"');
    }

    private void writeZoneId(Writer writer, String zoneId) throws IOException {
        // zoneID
        if (zoneId.length() > 0) {
            char c = zoneId.charAt(0);
            if (c == '+' || c == '-' || c == 'Z') {
                writer.write(zoneId);
            } else {
                writer.write('[');
                writer.write(zoneId);
                writer.write(']');
            }
        }
    }

    // yyyy-MM-ddTHH:mm:ss.SSS+xx:yy or yyyy-MM-ddTHH:mm:ss.SSS[Asia/Shanghai]
    // note: toString有细微差别
    @Override
    protected void writeDefault(Object value, Writer writer, JsonConfig jsonConfig, int indent) throws Exception {
        int year = TemporalAloneInvoker.invokeZonedDateTimeYear(value).intValue();
        int month = TemporalAloneInvoker.invokeZonedDateTimeMonth(value).intValue();
        int day = TemporalAloneInvoker.invokeZonedDateTimeDay(value).intValue();
        int hour = TemporalAloneInvoker.invokeZonedDateTimeHour(value).intValue();
        int minute = TemporalAloneInvoker.invokeZonedDateTimeMinute(value).intValue();
        int second = TemporalAloneInvoker.invokeZonedDateTimeSecond(value).intValue();
        int nano = TemporalAloneInvoker.invokeZonedDateTimeNano(value).intValue();
        writer.append('"');
        writeYYYY_MM_dd_T_HH_mm_ss_SSS(writer, year, month, day, hour, minute, second, nano / 1000000);
        String zoneId = TemporalAloneInvoker.invokeZonedDateTimeZone(value).toString();
        writeZoneId(writer, zoneId);
        writer.append('"');
    }
}
