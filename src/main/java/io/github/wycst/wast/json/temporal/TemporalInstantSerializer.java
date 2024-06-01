package io.github.wycst.wast.json.temporal;

import io.github.wycst.wast.common.beans.GeneralDate;
import io.github.wycst.wast.common.beans.GregorianDate;
import io.github.wycst.wast.json.JSONConfig;
import io.github.wycst.wast.json.JSONTemporalSerializer;
import io.github.wycst.wast.json.JSONWriter;
import io.github.wycst.wast.json.annotations.JsonProperty;

import java.time.Instant;

/**
 * Instant序列化
 *
 * @Author: wangy
 * @Date: 2022/8/13 15:06
 * @Description:
 * @see GregorianDate
 * @see io.github.wycst.wast.common.beans.DateTemplate
 */
public class TemporalInstantSerializer extends JSONTemporalSerializer {

    public TemporalInstantSerializer(Class<?> temporalClass, JsonProperty property) {
        super(temporalClass, property);
    }

    protected void checkClass(Class<?> temporalClass) {
    }

    @Override
    protected void writeTemporalWithTemplate(Object value, JSONWriter writer, JSONConfig jsonConfig) throws Exception {
        Instant instant = (Instant) value;
        long epochMilli = instant.toEpochMilli();
        GeneralDate date = new GeneralDate(epochMilli, ZERO_TIME_ZONE);
        writer.write('"');
        writeGeneralDate(date, dateFormatter, writer);
        writer.write('"');
    }

    // YYYY-MM-ddTHH:mm:ss.SSSZ(时区为0)
    @Override
    protected void writeDefault(Object value, JSONWriter writer, JSONConfig jsonConfig, int indent) throws Exception {
        Instant instant = (Instant) value;
        long epochSeconds = instant.getEpochSecond();
        int nano = instant.getNano();
        writer.writeJSONInstant(epochSeconds, nano);
    }
}
