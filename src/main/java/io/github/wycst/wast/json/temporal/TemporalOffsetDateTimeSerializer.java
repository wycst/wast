package io.github.wycst.wast.json.temporal;

import io.github.wycst.wast.common.beans.GregorianDate;
import io.github.wycst.wast.json.JSONConfig;
import io.github.wycst.wast.json.JSONPropertyDefinition;
import io.github.wycst.wast.json.JSONTemporalSerializer;
import io.github.wycst.wast.json.JSONWriter;

import java.time.OffsetDateTime;

/**
 * OffsetDateTime序列化
 *
 * @Author: wangy
 * @Date: 2022/8/13 15:06
 * @Description:
 * @see GregorianDate
 * @see io.github.wycst.wast.common.beans.DateTemplate
 */
public class TemporalOffsetDateTimeSerializer extends JSONTemporalSerializer {

    public TemporalOffsetDateTimeSerializer(Class<?> temporalClass, JSONPropertyDefinition property) {
        super(temporalClass, property);
    }

    protected void checkClass(Class<?> temporalClass) {
    }

    @Override
    protected void writeTemporalWithTemplate(Object value, JSONWriter writer, JSONConfig jsonConfig) throws Exception {
        OffsetDateTime offsetDateTime = (OffsetDateTime) value;
        writer.write('"');
        // localDateTime
        writeDate(
                offsetDateTime.getYear(),
                offsetDateTime.getMonthValue(),
                offsetDateTime.getDayOfMonth(),
                offsetDateTime.getHour(),
                offsetDateTime.getMinute(),
                offsetDateTime.getSecond(),
                offsetDateTime.getNano() / 1000000,
                dateFormatter,
                writer);
        String zoneId = offsetDateTime.getOffset().toString();
        writer.writeZoneId(zoneId);
        writer.write('"');
    }

    // yyyy-MM-ddTHH:mm:ss.SSS+xx:yy
    @Override
    protected void writeDefault(Object value, JSONWriter writer, JSONConfig jsonConfig, int indent) throws Exception {
        OffsetDateTime offsetDateTime = (OffsetDateTime) value;
        writer.writeJSONLocalDateTime(
                offsetDateTime.getYear(),
                offsetDateTime.getMonthValue(),
                offsetDateTime.getDayOfMonth(),
                offsetDateTime.getHour(),
                offsetDateTime.getMinute(),
                offsetDateTime.getSecond(),
                offsetDateTime.getNano(),
                offsetDateTime.getOffset().toString());
    }
}
