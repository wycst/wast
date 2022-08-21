package io.github.wycst.wast.json.temporal;

import io.github.wycst.wast.common.beans.Date;
import io.github.wycst.wast.json.JSONTemporalSerializer;
import io.github.wycst.wast.json.annotations.JsonProperty;
import io.github.wycst.wast.json.options.JsonConfig;
import io.github.wycst.wast.json.reflect.ObjectStructureWrapper;

import java.io.Writer;
import java.util.TimeZone;

/**
 * Instant序列化，使用反射实现
 *
 * @Author: wangy
 * @Date: 2022/8/13 15:06
 * @Description:
 * @see io.github.wycst.wast.common.beans.Date
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
        long defaultOffset = Date.getDefaultOffset();
        writer.append('"');
        dateTemplate.formatTo(new Date(epochMilli + defaultOffset), writer);
        writer.append('"');
    }

}
