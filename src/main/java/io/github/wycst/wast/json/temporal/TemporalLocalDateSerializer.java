package io.github.wycst.wast.json.temporal;

import io.github.wycst.wast.common.beans.GregorianDate;
import io.github.wycst.wast.json.JSONConfig;
import io.github.wycst.wast.json.JSONPropertyDefinition;
import io.github.wycst.wast.json.JSONTemporalSerializer;
import io.github.wycst.wast.json.JSONWriter;

import java.time.LocalDate;

/**
 * LocalDate序列化
 *
 * @Author: wangy
 * @Date: 2022/8/13 15:06
 * @Description:
 * @see GregorianDate
 * @see io.github.wycst.wast.common.beans.DateTemplate
 */
public class TemporalLocalDateSerializer extends JSONTemporalSerializer {

    public TemporalLocalDateSerializer(Class<?> temporalClass, JSONPropertyDefinition property) {
        super(temporalClass, property);
    }

    protected void checkClass(Class<?> temporalClass) {
    }

    @Override
    protected void writeTemporalWithTemplate(Object value, JSONWriter writer, JSONConfig jsonConfig) throws Exception {
        LocalDate localDate = (LocalDate) value;
        int year = localDate.getYear();
        int month = localDate.getMonthValue();
        int day = localDate.getDayOfMonth();
        writer.write('"');
        writeDate(year, month, day, 0, 0, 0, 0, dateFormatter, writer);
        writer.write('"');
    }

    // yyyy-MM-dd
    @Override
    protected void writeDefault(Object value, JSONWriter writer, JSONConfig jsonConfig, int indent) throws Exception {
        LocalDate localDate = (LocalDate) value;
        writer.writeJSONLocalDate(localDate.getYear(), localDate.getMonthValue(), localDate.getDayOfMonth());
    }
}
