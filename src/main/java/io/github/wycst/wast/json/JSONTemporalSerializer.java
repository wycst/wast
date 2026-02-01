package io.github.wycst.wast.json;

import io.github.wycst.wast.common.beans.DateFormatter;
import io.github.wycst.wast.common.reflect.ClassStrucWrap;
import io.github.wycst.wast.json.temporal.*;

import java.util.TimeZone;

/**
 * java.time support
 * <p>
 * Serialization using reflection
 * <p>
 * Localtime, localdate, localdatetime do not consider time zone
 *
 * @Author: wangy
 * @Date: 2022/8/13 15:08
 * @Description:
 */
@SuppressWarnings({"all"})
public abstract class JSONTemporalSerializer extends JSONTypeSerializer {

    protected final Class<?> temporalClass;
    protected DateFormatter dateFormatter;
    protected final boolean useFormatter;

    protected JSONTemporalSerializer(Class<?> temporalClass, JSONPropertyDefinition property) {
        checkClass(temporalClass);
        this.temporalClass = temporalClass;
        if (property != null) {
            String pattern = property.pattern();
            if (pattern.length() > 0) {
                dateFormatter = DateFormatter.of(pattern);
            }
        }
        useFormatter = dateFormatter != null;
    }

    static JSONTypeSerializer getTemporalSerializerInstance(ClassStrucWrap classStrucWrap, JSONPropertyDefinition property) {
        ClassStrucWrap.ClassWrapperType classWrapperType = classStrucWrap.getClassWrapperType();
        Class<?> temporalClass = classStrucWrap.getSourceClass();
        switch (classWrapperType) {
            case TemporalMonthDay: {
                return new TemporalMonthDaySerializer(temporalClass, property);
            }
            case TemporalYearMonth: {
                return new TemporalYearMonthSerializer(temporalClass, property);
            }
            case TemporalLocalDate: {
                return new TemporalLocalDateSerializer(temporalClass, property);
            }
            case TemporalLocalDateTime: {
                return new TemporalLocalDateTimeSerializer(temporalClass, property);
            }
            case TemporalLocalTime: {
                return new TemporalLocalTimeSerializer(temporalClass, property);
            }
            case TemporalInstant: {
                return new TemporalInstantSerializer(temporalClass, property);
            }
            case TemporalZonedDateTime: {
                return new TemporalZonedDateTimeSerializer(temporalClass, property);
            }
            case TemporalOffsetDateTime: {
                return new TemporalOffsetDateTimeSerializer(temporalClass, property);
            }
            default: {
                throw new UnsupportedOperationException();
            }
        }
    }

    // check
    protected abstract void checkClass(Class<?> temporalClass);

    protected void serialize(Object value, JSONWriter writer, JSONConfig jsonConfig, int indent) throws Exception {
        if (useFormatter) {
            writeTemporalWithTemplate(value, writer, jsonConfig);
        } else {
            writeDefault(value, writer, jsonConfig, indent);
        }
    }

    protected abstract void writeTemporalWithTemplate(Object value, JSONWriter writer, JSONConfig jsonConfig) throws Exception;

    /**
     * <p> 默认toString方式序列化
     * <p> 可重写优化，减少一次字符串的构建
     *
     * @param value
     * @param writer
     * @param jsonConfig
     * @param indent
     * @throws Exception
     */
    protected void writeDefault(Object value, JSONWriter writer, JSONConfig jsonConfig, int indent) throws Exception {
        String temporal = value.toString();
        CHAR_SEQUENCE_STRING.serialize(temporal, writer, jsonConfig, indent);
    }

    protected final static TimeZone getTimeZone(String zoneId) {
        return JSONGeneral.getTimeZone(zoneId);
    }
}
