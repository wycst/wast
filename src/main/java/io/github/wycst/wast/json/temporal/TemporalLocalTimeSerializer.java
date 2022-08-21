package io.github.wycst.wast.json.temporal;

import io.github.wycst.wast.json.JSONTemporalSerializer;
import io.github.wycst.wast.json.annotations.JsonProperty;
import io.github.wycst.wast.json.options.JsonConfig;
import io.github.wycst.wast.json.reflect.ObjectStructureWrapper;

import java.io.Writer;

/**
 * LocalTime序列化，使用反射实现
 *
 * @Author: wangy
 * @Date: 2022/8/13 15:06
 * @Description:
 * @see io.github.wycst.wast.common.beans.Date
 * @see io.github.wycst.wast.common.beans.DateTemplate
 */
public class TemporalLocalTimeSerializer extends JSONTemporalSerializer {

    public TemporalLocalTimeSerializer(ObjectStructureWrapper objectStructureWrapper, JsonProperty property) {
        super(objectStructureWrapper, property);
    }

    protected void checkClass(ObjectStructureWrapper objectStructureWrapper) {
        Class<?> sourceClass = objectStructureWrapper.getSourceClass();
        if (sourceClass != TemporalAloneInvoker.localTimeClass) {
            throw new UnsupportedOperationException("Not Support for class temporal type " + sourceClass);
        }
    }

    @Override
    protected void writeTemporalWithTemplate(Object value, Writer writer, JsonConfig jsonConfig) throws Exception {
        int hour = TemporalAloneInvoker.invokeLocalTimeHour(value).intValue();
        int minute = TemporalAloneInvoker.invokeLocalTimeMinute(value).intValue();
        int second = TemporalAloneInvoker.invokeLocalTimeSecond(value).intValue();
        int nano = TemporalAloneInvoker.invokeLocalTimeNano(value).intValue();
        int millisecond = nano / 1000000;

        writer.append('"');
        dateTemplate.formatTo(1970, 1, 1, hour, minute, second, millisecond, writer);
        writer.append('"');
    }

}
