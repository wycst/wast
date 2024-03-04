package io.github.wycst.wast.json;

import io.github.wycst.wast.common.beans.DateFormatter;
import io.github.wycst.wast.common.reflect.ClassStructureWrapper;
import io.github.wycst.wast.json.annotations.JsonProperty;
import io.github.wycst.wast.json.options.JsonConfig;
import io.github.wycst.wast.json.reflect.ObjectStructureWrapper;
import io.github.wycst.wast.json.temporal.*;

import java.io.Writer;

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
public abstract class JSONTemporalSerializer extends JSONTypeSerializer {

    protected final ObjectStructureWrapper objectStructureWrapper;
    protected DateFormatter dateFormatter;
    protected final boolean useFormatter;

    protected JSONTemporalSerializer(ObjectStructureWrapper objectStructureWrapper, JsonProperty property) {
        checkClass(objectStructureWrapper);
        this.objectStructureWrapper = objectStructureWrapper;
        if (property != null) {
            String pattern = property.pattern().trim();
            if(pattern.length() > 0) {
                dateFormatter = DateFormatter.of(pattern);
            }
        }
        useFormatter = dateFormatter != null;
    }

    static JSONTypeSerializer getTemporalSerializerInstance(ObjectStructureWrapper objectStructureWrapper, JsonProperty property) {
        ClassStructureWrapper.ClassWrapperType classWrapperType = objectStructureWrapper.getClassWrapperType();
        switch (classWrapperType) {
            case TemporalLocalDate: {
                return new TemporalLocalDateSerializer(objectStructureWrapper, property);
            }
            case TemporalLocalTime: {
                return new TemporalLocalTimeSerializer(objectStructureWrapper, property);
            }
            case TemporalLocalDateTime: {
                return new TemporalLocalDateTimeSerializer(objectStructureWrapper, property);
            }
            case TemporalZonedDateTime: {
                return new TemporalZonedDateTimeSerializer(objectStructureWrapper, property);
            }
            case TemporalInstant: {
                return new TemporalInstantSerializer(objectStructureWrapper, property);
            }
            default: {
                throw new UnsupportedOperationException();
            }
        }
    }

    // check
    protected abstract void checkClass(ObjectStructureWrapper objectStructureWrapper);

    protected void serialize(Object value, Writer writer, JsonConfig jsonConfig, int indent) throws Exception {
        if (useFormatter) {
            writeTemporalWithTemplate(value, writer, jsonConfig);
        } else {
            writeDefault(value, writer, jsonConfig, indent);
        }
    }

    protected abstract void writeTemporalWithTemplate(Object value, Writer writer, JsonConfig jsonConfig) throws Exception;

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
    protected void writeDefault(Object value, Writer writer, JsonConfig jsonConfig, int indent) throws Exception {
        String temporal = value.toString();
        CHAR_SEQUENCE.serialize(temporal, writer, jsonConfig, indent);
    }

}
