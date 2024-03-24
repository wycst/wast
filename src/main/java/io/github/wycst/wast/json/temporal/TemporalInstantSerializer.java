package io.github.wycst.wast.json.temporal;

import io.github.wycst.wast.common.beans.GeneralDate;
import io.github.wycst.wast.common.beans.GregorianDate;
import io.github.wycst.wast.json.JSONConfig;
import io.github.wycst.wast.json.JSONTemporalSerializer;
import io.github.wycst.wast.json.JSONWriter;
import io.github.wycst.wast.json.annotations.JsonProperty;

/**
 * Instant序列化，使用反射实现
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
        long epochMilli = TemporalAloneInvoker.invokeInstantEpochMilli(value);
        GeneralDate date = new GeneralDate(epochMilli, ZERO_TIME_ZONE);
        writer.write('"');
        writeGeneralDate(date, dateFormatter, writer);
        writer.write('"');
    }

    // YYYY-MM-ddTHH:mm:ss.SSSZ(时区为0)
    @Override
    protected void writeDefault(Object value, JSONWriter writer, JSONConfig jsonConfig, int indent) throws Exception {
        long epochSeconds = TemporalAloneInvoker.invokeInstantEpochSeconds(value);
        int nano = TemporalAloneInvoker.invokeInstantNano(value);

        GeneralDate generalDate = new GeneralDate(epochSeconds * 1000, ZERO_TIME_ZONE);
        int year = generalDate.getYear();
        int month = generalDate.getMonth();
        int day = generalDate.getDay();
        int hour = generalDate.getHourOfDay();
        int minute = generalDate.getMinute();
        int second = generalDate.getSecond();

        writer.write('"');
        writer.writeLocalDateTime(year, month, day, hour, minute, second, nano);
        writer.write('Z');
        writer.write('"');
    }
}
