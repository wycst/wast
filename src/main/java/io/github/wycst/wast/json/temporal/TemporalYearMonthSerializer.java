package io.github.wycst.wast.json.temporal;

import io.github.wycst.wast.common.beans.GregorianDate;
import io.github.wycst.wast.json.JSONConfig;
import io.github.wycst.wast.json.JSONPropertyDefinition;
import io.github.wycst.wast.json.JSONTemporalSerializer;
import io.github.wycst.wast.json.JSONWriter;

import java.time.YearMonth;

/**
 * YearMonth序列化
 *
 * @Author: wangy
 * @Description:
 * @see GregorianDate
 * @see io.github.wycst.wast.common.beans.DateTemplate
 */
public class TemporalYearMonthSerializer extends JSONTemporalSerializer {

    public TemporalYearMonthSerializer(Class<?> temporalClass, JSONPropertyDefinition property) {
        super(temporalClass, property);
    }

    protected void checkClass(Class<?> temporalClass) {
    }

    @Override
    protected void writeTemporalWithTemplate(Object value, JSONWriter writer, JSONConfig jsonConfig) throws Exception {
        YearMonth yearMonth = (YearMonth) value;
        writer.write('"');
        writeDate(yearMonth.getYear(), yearMonth.getMonthValue(), 0, 0, 0, 0, 0, dateFormatter, writer);
        writer.write('"');
    }

    // yyyy-MM
    @Override
    protected void writeDefault(Object value, JSONWriter writer, JSONConfig jsonConfig, int indent) throws Exception {
        YearMonth yearMonth = (YearMonth) value;
        writer.writeJSONYearMonth(yearMonth.getYear(), yearMonth.getMonthValue());
    }
}
