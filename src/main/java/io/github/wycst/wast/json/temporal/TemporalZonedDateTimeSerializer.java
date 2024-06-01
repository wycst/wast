package io.github.wycst.wast.json.temporal;

import io.github.wycst.wast.common.beans.GregorianDate;
import io.github.wycst.wast.json.JSONConfig;
import io.github.wycst.wast.json.JSONTemporalSerializer;
import io.github.wycst.wast.json.JSONWriter;
import io.github.wycst.wast.json.annotations.JsonProperty;

import java.time.ZonedDateTime;

/**
 * ZonedDateTime序列化
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
    }

    @Override
    protected void writeTemporalWithTemplate(Object value, JSONWriter writer, JSONConfig jsonConfig) throws Exception {
        ZonedDateTime zonedDateTime = (ZonedDateTime) value;
        int year = zonedDateTime.getYear();
        int month = zonedDateTime.getMonthValue();
        int day = zonedDateTime.getDayOfMonth();
        int hour = zonedDateTime.getHour();
        int minute = zonedDateTime.getMinute();
        int second = zonedDateTime.getSecond();
        int nano = zonedDateTime.getNano();
        int millisecond = nano / 1000000;
        writer.write('"');
        // localDateTime
        writeDate(year, month, day, hour, minute, second, millisecond, dateFormatter, writer);
        String zoneId = zonedDateTime.getZone().toString();
        writer.writeZoneId(zoneId);
        writer.write('"');
    }

    // yyyy-MM-ddTHH:mm:ss.SSS+xx:yy or yyyy-MM-ddTHH:mm:ss.SSS[Asia/Shanghai]
    // note: toString有细微差别
    @Override
    protected void writeDefault(Object value, JSONWriter writer, JSONConfig jsonConfig, int indent) throws Exception {
        ZonedDateTime zonedDateTime = (ZonedDateTime) value;
        writer.writeJSONLocalDateTime(
                zonedDateTime.getYear(),
                zonedDateTime.getMonthValue(),
                zonedDateTime.getDayOfMonth(),
                zonedDateTime.getHour(),
                zonedDateTime.getMinute(),
                zonedDateTime.getSecond(),
                zonedDateTime.getNano(),
                zonedDateTime.getZone().toString());
    }
}
