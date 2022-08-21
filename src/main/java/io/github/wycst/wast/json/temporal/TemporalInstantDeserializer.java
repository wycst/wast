package io.github.wycst.wast.json.temporal;

import io.github.wycst.wast.common.beans.DateTemplate;
import io.github.wycst.wast.common.reflect.GenericParameterizedType;
import io.github.wycst.wast.json.JSONTemporalDeserializer;
import io.github.wycst.wast.json.options.JSONParseContext;

/**
 * 参考java.util.Date反序列化，使用反射实现
 *
 * @Author: wangy
 * @Date: 2022/8/13 15:06
 * @Description:
 * @see io.github.wycst.wast.common.beans.Date
 * @see io.github.wycst.wast.common.beans.DateTemplate
 */
public class TemporalInstantDeserializer extends JSONTemporalDeserializer {

    public TemporalInstantDeserializer(GenericParameterizedType genericParameterizedType) {
        super(genericParameterizedType);
    }

    protected void checkClass(GenericParameterizedType genericParameterizedType) {
        if (genericParameterizedType.getActualType() != TemporalAloneInvoker.instantClass) {
            throw new UnsupportedOperationException("Not Support for class " + genericParameterizedType.getActualType());
        }
    }

    @Override
    protected void createDefaultTemplate() {
        dateTemplate = new DateTemplate("yyyy-MM-ddTHH:mm:ss.SZ");
    }

    protected Object deserializeTemporal(char[] buf, int fromIndex, int endIndex, JSONParseContext jsonParseContext) throws Exception {
        long time = dateTemplate.parseTime(buf, fromIndex + 1, endIndex - fromIndex - 1, ZERO_TIME_ZONE);
        return TemporalAloneInvoker.createOrOfInstant(time);
    }
}
