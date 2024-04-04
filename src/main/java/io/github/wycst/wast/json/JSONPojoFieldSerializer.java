package io.github.wycst.wast.json;

import io.github.wycst.wast.common.reflect.GetterInfo;
import io.github.wycst.wast.common.reflect.ReflectConsts;
import io.github.wycst.wast.common.reflect.UnsafeHelper;
import io.github.wycst.wast.common.utils.EnvUtils;
import io.github.wycst.wast.json.annotations.JsonProperty;
import io.github.wycst.wast.json.annotations.JsonSerialize;
import io.github.wycst.wast.json.custom.JsonSerializer;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * @Author: wangy
 * @Description:
 */
public class JSONPojoFieldSerializer extends JSONTypeSerializer {

    /**
     * getter信息
     */
    private final GetterInfo getterInfo;
    private final JsonProperty jsonProperty;

    /**
     * name
     */
    final String name;

    /**
     * 类型
     */
    private final ReflectConsts.ClassCategory classCategory;

    /**
     * 序列化器
     */
    private JSONTypeSerializer serializer;

    private char[] fieldNameTokenChars;
    private String fieldNameToken;
    private int fieldNameTokenOffset;
    private long[] fieldNameTokenLongs;
    private int[] fieldNameTokenInts;
    /**
     * 自定义序列化器
     */
    private final static Map<Class<? extends JsonSerializer>, JsonSerializer> customSerializers = new HashMap<Class<? extends JsonSerializer>, JsonSerializer>();

    JSONPojoFieldSerializer(GetterInfo getterInfo, String name) {
        this.getterInfo = getterInfo;
        this.classCategory = getterInfo.getClassCategory();
        this.name = name;
        JsonProperty jsonProperty = (JsonProperty) getterInfo.getAnnotation(JsonProperty.class);
        this.jsonProperty = jsonProperty;
        setFieldNameToken();
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
        if (returnType == String.class) {
            return CHAR_SEQUENCE_STRING;
        } else if (classCategory == ReflectConsts.ClassCategory.NumberCategory) {
            // From cache by different number types (int/float/double/long...)
            return JSONTypeSerializer.getTypeSerializer(returnType);
        } else {
            return JSONTypeSerializer.getFieldTypeSerializer(classCategory, returnType, jsonProperty);
        }
    }

    private void setFieldNameToken() {
        int len = name.length();
        // "${name}":null
        fieldNameTokenChars = new char[len + 7];
        int i = 0;
        fieldNameTokenChars[i++] = '"';
        name.getChars(0, len, fieldNameTokenChars, i);
        fieldNameTokenChars[len + 1] = '"';
        fieldNameTokenChars[len + 2] = ':';
        fieldNameTokenOffset = len + 3;
        fieldNameTokenChars[len + 3] = 'n';
        fieldNameTokenChars[len + 4] = 'u';
        fieldNameTokenChars[len + 5] = 'l';
        fieldNameTokenChars[len + 6] = 'l';
        if (EnvUtils.JDK_9_PLUS) {
            fieldNameToken = new String(fieldNameTokenChars);
        }
        // optimize use unsafe(适用JDK8，JDK9+提升不明显)
        if (name.getBytes().length == len) {
            String stringForUnsafe = new String(fieldNameTokenChars, 0, fieldNameTokenOffset);
            fieldNameTokenLongs = UnsafeHelper.getLongs(stringForUnsafe);
            fieldNameTokenInts = UnsafeHelper.getInts(stringForUnsafe);
        }
    }

    public GetterInfo getGetterInfo() {
        return getterInfo;
    }

    public Object invoke(Object pojo) {
        return getterInfo.invoke(pojo);
    }

    public void writeJSONFieldName(JSONWriter writer) throws IOException {
        if (fieldNameTokenLongs != null) {
            writer.writeUnsafe(fieldNameTokenLongs, fieldNameTokenInts, fieldNameTokenOffset);
        } else {
            if (fieldNameToken != null) {
                writer.writeFieldString(fieldNameToken, 0, fieldNameTokenOffset);
            } else {
                writer.writeShortChars(fieldNameTokenChars, 0, fieldNameTokenOffset);
            }
        }
    }

    public void writeJSONFieldNameWithNull(JSONWriter writer) throws IOException {
        if (fieldNameToken != null) {
            writer.write(fieldNameToken, 0, fieldNameTokenChars.length);
        } else {
            writer.writeShortChars(fieldNameTokenChars, 0, fieldNameTokenChars.length);
        }
    }

    public JSONTypeSerializer getSerializer() {
        return serializer;
    }

    protected void serialize(Object value, JSONWriter writer, JSONConfig jsonConfig, int indent) throws Exception {
    }

    public String getName() {
        return name;
    }

    public JsonProperty getJsonProperty() {
        return jsonProperty;
    }
}
