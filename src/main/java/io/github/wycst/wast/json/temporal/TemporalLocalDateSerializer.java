package io.github.wycst.wast.json.temporal;

import io.github.wycst.wast.json.JSONStringWriter;
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
        dateTemplate.formatTo(year, month, day, 0, 0, 0, 0, writer, true);
        writer.append('"');
    }

    // yyyy-MM-dd
    @Override
    protected void writeDefault(Object value, Writer writer, JsonConfig jsonConfig, int indent) throws Exception {
        int year = TemporalAloneInvoker.invokeLocalDateYear(value).intValue();
        int month = TemporalAloneInvoker.invokeLocalDateMonth(value).intValue();
        int day = TemporalAloneInvoker.invokeLocalDateDay(value).intValue();
        int y1 = year / 100, y2 = year - y1 * 100;
        writer.write('"');
        writer.write(DigitTens[y1]);
        writer.write(DigitOnes[y1]);
        writer.write(DigitTens[y2]);
        writer.write(DigitOnes[y2]);
        writer.write('-');
        writer.write(DigitTens[month]);
        writer.write(DigitOnes[month]);
        writer.write('-');
        writer.write(DigitTens[day]);
        writer.write(DigitOnes[day]);
        writer.write('"');
    }
}
