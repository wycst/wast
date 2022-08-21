package io.github.wycst.wast.json.temporal;

import io.github.wycst.wast.json.JSONTemporalSerializer;
import io.github.wycst.wast.json.annotations.JsonProperty;
import io.github.wycst.wast.json.options.JsonConfig;
import io.github.wycst.wast.json.reflect.ObjectStructureWrapper;

import java.io.Writer;

/**
 * LocalDate序列化，使用反射实现
 *
 * @Author: wangy
 * @Date: 2022/8/13 15:06
 * @Description:
 * @see io.github.wycst.wast.common.beans.Date
 * @see io.github.wycst.wast.common.beans.DateTemplate
 */
public class TemporalLocalDateSerializer extends JSONTemporalSerializer {

    public TemporalLocalDateSerializer(ObjectStructureWrapper objectStructureWrapper, JsonProperty property) {
        super(objectStructureWrapper, property);
    }

    protected void checkClass(ObjectStructureWrapper objectStructureWrapper) {
        Class<?> sourceClass = objectStructureWrapper.getSourceClass();
        if (sourceClass != TemporalAloneInvoker.localDateClass) {
            throw new UnsupportedOperationException("Not Support for class temporal type " + sourceClass);
        }
    }

    @Override
    protected void writeTemporalWithTemplate(Object value, Writer writer, JsonConfig jsonConfig) throws Exception {
        int year = TemporalAloneInvoker.invokeLocalDateYear(value).intValue();
        int month = TemporalAloneInvoker.invokeLocalDateMonth(value).intValue();
        int day = TemporalAloneInvoker.invokeLocalDateDay(value).intValue();
        writer.append('"');
        dateTemplate.formatTo(year, month, day, 0, 0, 0, 0, writer);
        writer.append('"');
    }
}
