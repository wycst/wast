package io.github.wycst.wast.json.reflect;

import io.github.wycst.wast.common.reflect.GetterInfo;
import io.github.wycst.wast.common.reflect.ReflectConsts;
import io.github.wycst.wast.json.JSONTypeSerializer;
import io.github.wycst.wast.json.annotations.JsonProperty;
import io.github.wycst.wast.json.annotations.JsonSerialize;
import io.github.wycst.wast.json.custom.JsonSerializer;
import io.github.wycst.wast.json.options.JsonConfig;

import java.io.IOException;
import java.io.Writer;
import java.util.HashMap;
import java.util.Map;

/**
 * @Author: wangy
 * @Description:
 */
public class FieldSerializer extends JSONTypeSerializer {

    /**
     * getter信息
     */
    private final GetterInfo getterInfo;
    private final JsonProperty jsonProperty;

    /**
     * name
     */
    private String name;

    /**
     * 类型
     */
    private final ReflectConsts.ClassCategory classCategory;

    /**
     * 序列化器
     */
    public JSONTypeSerializer serializer;

    private char[] fixedFieldName;

    /**
     * 自定义序列化器
     */
    private final static Map<Class<? extends JsonSerializer>, JsonSerializer> customSerializers = new HashMap<Class<? extends JsonSerializer>, JsonSerializer>();

    FieldSerializer(GetterInfo getterInfo, String name) {
        this.getterInfo = getterInfo;
        this.classCategory = getterInfo.getClassCategory();
        this.name = name;
        fixed();
        JsonProperty jsonProperty = (JsonProperty) getterInfo.getAnnotation(JsonProperty.class);
        this.jsonProperty = jsonProperty;
    }

    void initSerializer() {
        if (this.serializer == null) {
            this.serializer = createSerializer();
        }
    }

    private JSONTypeSerializer createSerializer() {
        // check custom Deserializer
        JsonSerialize jsonSerialize = (JsonSerialize) getterInfo.getAnnotation(JsonSerialize.class);
        if (jsonSerialize != null) {
            Class<? extends JsonSerializer> jsonSerializerClass = jsonSerialize.value();
            try {
                if (jsonSerialize.singleton()) {
                    JsonSerializer jsonSerializer = customSerializers.get(jsonSerializerClass);
                    if (jsonSerializer == null) {
                        jsonSerializer = jsonSerializerClass.newInstance();
                        customSerializers.put(jsonSerializerClass, jsonSerializer);
                    }
                    return jsonSerializer;
                }
                return jsonSerializerClass.newInstance();
            } catch (Throwable e) {
                e.printStackTrace();
            }
        }
        Class<?> returnType = getterInfo.getReturnType();
        if(returnType == String.class) {
            return STRING;
        }  else if(classCategory == ReflectConsts.ClassCategory.NumberCategory) {
            // From cache by different number types (int/float/double/long...)
            return JSONTypeSerializer.getTypeSerializer(returnType);
        } else {
            return JSONTypeSerializer.getFieldTypeSerializer(classCategory, returnType, jsonProperty);
        }
    }

    private void fixed() {
        int len = name.length();
        // "${name}":null
        fixedFieldName = new char[len + 7];
        int i = 0;
        fixedFieldName[i++] = '"';
        name.getChars(0, len, fixedFieldName, i);
        fixedFieldName[len + 1] = '"';
        fixedFieldName[len + 2] = ':';
        fixedFieldName[len + 3] = 'n';
        fixedFieldName[len + 4] = 'u';
        fixedFieldName[len + 5] = 'l';
        fixedFieldName[len + 6] = 'l';
    }

    public GetterInfo getGetterInfo() {
        return getterInfo;
    }

//    public char[] getFixedFieldName() {
//        return fixedFieldName;
//    }

    public void writeFieldKey(Writer writer, int offset, int deleteCnt) throws IOException {
        writeShortChars(writer, fixedFieldName, offset, fixedFieldName.length - deleteCnt);
    }

    public JSONTypeSerializer getSerializer() {
        return serializer;
    }

    protected void serialize(Object value, Writer writer, JsonConfig jsonConfig, int indent) throws Exception {
    }
}
