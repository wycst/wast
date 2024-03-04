package io.github.wycst.wast.json.temporal;

import io.github.wycst.wast.common.beans.GeneralDate;
import io.github.wycst.wast.common.beans.GregorianDate;
import io.github.wycst.wast.json.JSONTemporalSerializer;
import io.github.wycst.wast.json.annotations.JsonProperty;
import io.github.wycst.wast.json.options.JsonConfig;
import io.github.wycst.wast.json.reflect.ObjectStructureWrapper;

import java.io.Writer;

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

    public TemporalInstantSerializer(ObjectStructureWrapper objectStructureWrapper, JsonProperty property) {
        super(objectStructureWrapper, property);
    }

    protected void checkClass(ObjectStructureWrapper objectStructureWrapper) {
        Class<?> sourceClass = objectStructureWrapper.getSourceClass();
        if (sourceClass != TemporalAloneInvoker.instantClass) {
            throw new UnsupportedOperationException("Not Support for class temporal type " + sourceClass);
        }
    }

    @Override
    protected void writeTemporalWithTemplate(Object value, Writer writer, JsonConfig jsonConfig) throws Exception {
        long epochMilli = TemporalAloneInvoker.invokeInstantEpochMilli(value).longValue();
        GeneralDate date = new GeneralDate(epochMilli, ZERO_TIME_ZONE);
        writer.write('"');
        writeGeneralDate(date, dateFormatter, writer);
        writer.write('"');
    }

    // YYYY-MM-ddTHH:mm:ss.SSSZ(时区为0)
    @Override
    protected void writeDefault(Object value, Writer writer, JsonConfig jsonConfig, int indent) throws Exception {
        long epochMilli = TemporalAloneInvoker.invokeInstantEpochMilli(value).longValue();
        GeneralDate generalDate = new GeneralDate(epochMilli, ZERO_TIME_ZONE);
        int year = generalDate.getYear();
        int month = generalDate.getMonth();
        int day = generalDate.getDay();
        int hour = generalDate.getHourOfDay();
        int minute = generalDate.getMinute();
        int second = generalDate.getSecond();
        int millisecond = generalDate.getMillisecond();
        writer.append('"');
        writeYYYY_MM_dd_T_HH_mm_ss_SSS(writer, year, month, day, hour, minute, second, millisecond);
        writer.append('Z');
        writer.append('"');
    }
}
