package io.github.wycst.wast.json.temporal;

import io.github.wycst.wast.common.beans.GeneralDate;
import io.github.wycst.wast.common.beans.GregorianDate;
import io.github.wycst.wast.json.JSONConfig;
import io.github.wycst.wast.json.JSONPropertyDefinition;
import io.github.wycst.wast.json.JSONTemporalSerializer;
import io.github.wycst.wast.json.JSONWriter;

import java.time.LocalDateTime;

/**
 * LocalDateTime序列化
 *
 * @Author: wangy
 * @Date: 2022/8/13 15:06
 * @Description:
 * @see GregorianDate
 * @see io.github.wycst.wast.common.beans.DateTemplate
 */
public class TemporalLocalDateTimeSerializer extends JSONTemporalSerializer {

    final boolean asTimestamp;

    public TemporalLocalDateTimeSerializer(Class<?> temporalClass, JSONPropertyDefinition property) {
        super(temporalClass, property);
        this.asTimestamp = property != null && property.asTimestamp();
    }

    protected void checkClass(Class<?> temporalClass) {
    }

    @Override
    protected void writeTemporalWithTemplate(Object value, JSONWriter writer, JSONConfig jsonConfig) throws Exception {
        LocalDateTime localDateTime = (LocalDateTime) value;
        if (asTimestamp) {
            long time = GeneralDate.getTime(localDateTime.getYear(),
                    localDateTime.getMonthValue(),
                    localDateTime.getDayOfMonth(),
                    localDateTime.getHour(),
                    localDateTime.getMinute(),
                    localDateTime.getSecond(),
                    localDateTime.getNano() / 1000000,
                    null);
            writer.writeLong(time);
        } else {
            writer.write('"');
            writeDate(
                    localDateTime.getYear(),
                    localDateTime.getMonthValue(),
                    localDateTime.getDayOfMonth(),
                    localDateTime.getHour(),
                    localDateTime.getMinute(),
                    localDateTime.getSecond(),
                    localDateTime.getNano() / 1000000, dateFormatter, writer);
            writer.write('"');
        }
    }

    // yyyy-MM-ddTHH:mm:ss.SSS
    @Override
    protected void writeDefault(Object value, JSONWriter writer, JSONConfig jsonConfig, int indent) throws Exception {
        LocalDateTime localDateTime = (LocalDateTime) value;
        if (asTimestamp) {
            long time = GeneralDate.getTime(localDateTime.getYear(),
                    localDateTime.getMonthValue(),
                    localDateTime.getDayOfMonth(),
                    localDateTime.getHour(),
                    localDateTime.getMinute(),
                    localDateTime.getSecond(),
                    localDateTime.getNano() / 1000000,
                    null);
            writer.writeLong(time);
        } else {
            writer.writeJSONLocalDateTime(
                    localDateTime.getYear(),
                    localDateTime.getMonthValue(),
                    localDateTime.getDayOfMonth(),
                    localDateTime.getHour(),
                    localDateTime.getMinute(),
                    localDateTime.getSecond(),
                    localDateTime.getNano(),
                    "");
        }
    }
}
