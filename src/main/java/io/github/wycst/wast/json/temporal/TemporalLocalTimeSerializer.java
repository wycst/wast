package io.github.wycst.wast.json.temporal;

import io.github.wycst.wast.common.beans.GregorianDate;
import io.github.wycst.wast.json.JSONConfig;
import io.github.wycst.wast.json.JSONTemporalSerializer;
import io.github.wycst.wast.json.JSONWriter;
import io.github.wycst.wast.json.annotations.JsonProperty;

/**
 * LocalTime序列化，使用反射实现
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
        int hour = TemporalAloneInvoker.invokeLocalTimeHour(value);
        int minute = TemporalAloneInvoker.invokeLocalTimeMinute(value);
        int second = TemporalAloneInvoker.invokeLocalTimeSecond(value);
        int nano = TemporalAloneInvoker.invokeLocalTimeNano(value);
        int millisecond = nano / 1000000;
        writer.write('"');
        writeDate(1970, 1, 1, hour, minute, second, millisecond, dateFormatter, writer);
        writer.write('"');
    }

    @Override
    protected void writeDefault(Object value, JSONWriter writer, JSONConfig jsonConfig, int indent) throws Exception {
        int hour = TemporalAloneInvoker.invokeLocalTimeHour(value);
        int minute = TemporalAloneInvoker.invokeLocalTimeMinute(value);
        int second = TemporalAloneInvoker.invokeLocalTimeSecond(value);
        int nano = TemporalAloneInvoker.invokeLocalTimeNano(value);
        writer.writeJSONTimeWithNano(hour, minute, second, nano);
    }
}
