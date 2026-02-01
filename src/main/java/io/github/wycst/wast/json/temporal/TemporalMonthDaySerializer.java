package io.github.wycst.wast.json.temporal;

import io.github.wycst.wast.common.beans.GregorianDate;
import io.github.wycst.wast.json.JSONConfig;
import io.github.wycst.wast.json.JSONPropertyDefinition;
import io.github.wycst.wast.json.JSONTemporalSerializer;
import io.github.wycst.wast.json.JSONWriter;

import java.time.MonthDay;

/**
 * MonthDay序列化
 *
 * @Author: wangy
 * @Description:
 * @see GregorianDate
 * @see io.github.wycst.wast.common.beans.DateTemplate
 */
public class TemporalMonthDaySerializer extends JSONTemporalSerializer {

    public TemporalMonthDaySerializer(Class<?> temporalClass, JSONPropertyDefinition property) {
        super(temporalClass, property);
    }

    protected void checkClass(Class<?> temporalClass) {
    }

    @Override
    protected void writeTemporalWithTemplate(Object value, JSONWriter writer, JSONConfig jsonConfig) throws Exception {
        MonthDay monthDay = (MonthDay) value;
        writer.write('"');
        writeDate(1900, monthDay.getMonthValue(), monthDay.getDayOfMonth(), 0, 0, 0, 0, dateFormatter, writer);
        writer.write('"');
    }

    // --MM-dd
    @Override
    protected void writeDefault(Object value, JSONWriter writer, JSONConfig jsonConfig, int indent) throws Exception {
        MonthDay monthDay = (MonthDay) value;
        writer.writeJSONDefaultMonthDay(monthDay.getMonthValue(), monthDay.getDayOfMonth());
    }
}
