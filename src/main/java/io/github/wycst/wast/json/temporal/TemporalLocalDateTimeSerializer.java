package io.github.wycst.wast.json.temporal;

import io.github.wycst.wast.json.JSONTemporalSerializer;
import io.github.wycst.wast.json.annotations.JsonProperty;
import io.github.wycst.wast.json.options.JsonConfig;
import io.github.wycst.wast.json.reflect.ObjectStructureWrapper;

import java.io.Writer;

/**
 * LocalDateTime序列化，使用反射实现
 *
 * @Author: wangy
 * @Date: 2022/8/13 15:06
 * @Description:
 * @see io.github.wycst.wast.common.beans.Date
 * @see io.github.wycst.wast.common.beans.DateTemplate
 */
public class TemporalLocalDateTimeSerializer extends JSONTemporalSerializer {

    public TemporalLocalDateTimeSerializer(ObjectStructureWrapper objectStructureWrapper, JsonProperty property) {
        super(objectStructureWrapper, property);
    }

    protected void checkClass(ObjectStructureWrapper objectStructureWrapper) {
        Class<?> sourceClass = objectStructureWrapper.getSourceClass();
        if (sourceClass != TemporalAloneInvoker.localDateTimeClass) {
            throw new UnsupportedOperationException("Not Support for class temporal type " + sourceClass);
        }
    }

    @Override
    protected void writeTemporalWithTemplate(Object value, Writer writer, JsonConfig jsonConfig) throws Exception {
        int year = TemporalAloneInvoker.invokeLocalDateTimeYear(value).intValue();
        int month = TemporalAloneInvoker.invokeLocalDateTimeMonth(value).intValue();
        int day = TemporalAloneInvoker.invokeLocalDateTimeDay(value).intValue();
        int hour = TemporalAloneInvoker.invokeLocalDateTimeHour(value).intValue();
        int minute = TemporalAloneInvoker.invokeLocalDateTimeMinute(value).intValue();
        int second = TemporalAloneInvoker.invokeLocalDateTimeSecond(value).intValue();
        int nano = TemporalAloneInvoker.invokeLocalDateTimeNano(value).intValue();
        int millisecond = nano / 1000000;

        writer.append('"');
        dateTemplate.formatTo(year, month, day, hour, minute, second, millisecond, writer);
        writer.append('"');
    }


}
