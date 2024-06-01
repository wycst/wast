package io.github.wycst.wast.json.temporal;

import io.github.wycst.wast.common.beans.GregorianDate;
import io.github.wycst.wast.json.JSONConfig;
import io.github.wycst.wast.json.JSONTemporalSerializer;
import io.github.wycst.wast.json.JSONWriter;
import io.github.wycst.wast.json.annotations.JsonProperty;

import java.time.LocalTime;

/**
 * LocalTime序列化
 *
 * @Author: wangy
 * @Date: 2022/8/13 15:06
 * @Description:
 * @see GregorianDate
 * @see io.github.wycst.wast.common.beans.DateTemplate
 */
public class TemporalLocalTimeSerializer extends JSONTemporalSerializer {

    public TemporalLocalTimeSerializer(Class<?> temporalClass, JsonProperty property) {
        super(temporalClass, property);
    }

    protected void checkClass(Class<?> temporalClass) {
    }

    @Override
    protected void writeTemporalWithTemplate(Object value, JSONWriter writer, JSONConfig jsonConfig) throws Exception {
        LocalTime localTime = (LocalTime) value;
        writer.write('"');
        writeDate(1970, 1, 1, localTime.getHour(), localTime.getMinute(), localTime.getSecond(), localTime.getNano() / 1000000, dateFormatter, writer);
        writer.write('"');
    }

    @Override
    protected void writeDefault(Object value, JSONWriter writer, JSONConfig jsonConfig, int indent) throws Exception {
        LocalTime localTime = (LocalTime) value;
        writer.writeJSONTimeWithNano(localTime.getHour(), localTime.getMinute(), localTime.getSecond(), localTime.getNano());
    }
}
