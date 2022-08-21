package io.github.wycst.wast.json.reflect;

import io.github.wycst.wast.common.reflect.GetterInfo;
import io.github.wycst.wast.common.reflect.ReflectConsts;
import io.github.wycst.wast.json.JSONTypeSerializer;
import io.github.wycst.wast.json.annotations.JsonProperty;
import io.github.wycst.wast.json.annotations.JsonSerialize;
import io.github.wycst.wast.json.custom.JsonSerializer;
import io.github.wycst.wast.json.options.JsonConfig;

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
     * 注解配置的日期序列化格式
     */
    private String pattern;

    /**
     * 注解配置的时区
     */
    private String timezone;

    /**
     * 注解配置的日期序列化为时间搓
     */
    private boolean writeDateAsTime;

    /***
     * 自定义序列化
     */
    private JsonSerializer jsonSerializer;

    /**
     * 是否自定义序列器
     */
    private boolean customSerialize = false;

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
        if (jsonProperty != null) {
            this.writeDateAsTime = jsonProperty.asTimestamp();
            this.pattern = jsonProperty.pattern();
            this.timezone = jsonProperty.timezone();
        }
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
            this.customSerialize = true;
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
        if (classCategory == ReflectConsts.ClassCategory.ObjectCategory) {
            // Temporal of Object cache singletons cannot be used
        } else if(classCategory == ReflectConsts.ClassCategory.NumberCategory) {
            // From cache by different number types (int/float/double/long...)
            return JSONTypeSerializer.getTypeSerializer(returnType);
        }

        return JSONTypeSerializer.getFieldTypeSerializer(classCategory, returnType, jsonProperty);
//        return JSONTypeSerializer.getTypeSerializer(classCategory, jsonProperty);
    }

    private void fixed() {
        int len = name.length();
        fixedFieldName = new char[len + 9];
        fixedFieldName[0] = ',';
        fixedFieldName[1] = '"';
        name.getChars(0, len, fixedFieldName, 2);
        fixedFieldName[len + 2] = '"';
        fixedFieldName[len + 3] = ':';
        fixedFieldName[len + 4] = 'n';
        fixedFieldName[len + 5] = 'u';
        fixedFieldName[len + 6] = 'l';
        fixedFieldName[len + 7] = 'l';
        fixedFieldName[len + 8] = ',';
    }

    public GetterInfo getGetterInfo() {
        return getterInfo;
    }

    public char[] getFixedFieldName() {
        return fixedFieldName;
    }

    public JsonSerializer getJsonSerializer() {
        return jsonSerializer;
    }

    public JSONTypeSerializer getSerializer() {
        return serializer;
    }

    protected void serialize(Object value, Writer writer, JsonConfig jsonConfig, int indent) throws Exception {
    }
}
