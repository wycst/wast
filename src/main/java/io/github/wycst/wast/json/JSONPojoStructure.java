package io.github.wycst.wast.json;

import io.github.wycst.wast.common.reflect.ClassStructureWrapper;
import io.github.wycst.wast.common.reflect.GenericParameterizedType;
import io.github.wycst.wast.common.reflect.GetterInfo;
import io.github.wycst.wast.common.reflect.SetterInfo;
import io.github.wycst.wast.json.annotations.JsonProperty;
import io.github.wycst.wast.json.annotations.JsonTypeSetting;
import io.github.wycst.wast.json.util.FixedNameValueMap;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 对ClassStructureWrapper的进一步包装，提供给json模块使用
 *
 * @Author wangyunchao
 */
public final class JSONPojoStructure {

    private static Map<Class<?>, JSONPojoStructure> objectStructureWarppers = new ConcurrentHashMap<Class<?>, JSONPojoStructure>();

    private final ClassStructureWrapper classStructureWrapper;
    private ClassStructureWrapper.ClassWrapperType classWrapperType;
    private final GenericParameterizedType genericType;
    private final FixedNameValueMap<JSONPojoFieldDeserializer> fixedFieldDeserializerValueMap;
    // deserializers
    private final List<JSONPojoFieldDeserializer> fieldDeserializers;

    // getter methods
    private JSONPojoFieldSerializer[] getterMethodSerializers;
    // field
    private JSONPojoFieldSerializer[] getterFieldSerializers;
    private final boolean collision;
    private boolean forceUseFields;
    private JsonTypeSetting jsonTypeSetting;

    private JSONPojoStructure(ClassStructureWrapper classStructureWrapper) {
        classStructureWrapper.getClass();
        this.classStructureWrapper = classStructureWrapper;
        this.classWrapperType = classStructureWrapper.getClassWrapperType();
        this.forceUseFields = classStructureWrapper.isForceUseFields();
        this.jsonTypeSetting = classStructureWrapper.getSourceClass().getDeclaredAnnotation(JsonTypeSetting.class);
        // serializer info
        List<GetterInfo> getterInfos = classStructureWrapper.getGetterInfos();
        List<JSONPojoFieldSerializer> fieldSerializers = new ArrayList<JSONPojoFieldSerializer>();
        for (GetterInfo getterInfo : getterInfos) {
            JsonProperty jsonProperty = (JsonProperty) getterInfo.getAnnotation(JsonProperty.class);
            String name = getterInfo.getName();
            if (jsonProperty != null) {
                if (!jsonProperty.serialize()) {
                    continue;
                }
                String aliasName;
                if ((aliasName = jsonProperty.name().trim()).length() > 0) {
                    name = aliasName;
                }
            }
            JSONPojoFieldSerializer fieldSerializer = new JSONPojoFieldSerializer(getterInfo, name);
            fieldSerializers.add(fieldSerializer);
        }
        getterMethodSerializers = fieldSerializers.toArray(new JSONPojoFieldSerializer[fieldSerializers.size()]);

        fieldSerializers.clear();
        List<GetterInfo> getterByFieldInfos = classStructureWrapper.getGetterInfos(true);
        for (GetterInfo getterInfo : getterByFieldInfos) {
            JsonProperty jsonProperty = (JsonProperty) getterInfo.getAnnotation(JsonProperty.class);
            String name = getterInfo.getName();
            if (jsonProperty != null) {
                if (!jsonProperty.serialize()) {
                    continue;
                }
                String aliasName;
                if ((aliasName = jsonProperty.name().trim()).length() > 0) {
                    name = aliasName;
                }
            }
            JSONPojoFieldSerializer fieldSerializer = new JSONPojoFieldSerializer(getterInfo, name);
            fieldSerializers.add(fieldSerializer);
        }
        getterFieldSerializers = fieldSerializers.toArray(new JSONPojoFieldSerializer[fieldSerializers.size()]);

        // 反序列化初始化
        this.genericType = GenericParameterizedType.actualType(classStructureWrapper.getSourceClass());
        Set<String> setterNames = classStructureWrapper.setterNames();

        Map<String, JSONPojoFieldDeserializer> fieldSerializerMap = new HashMap<String, JSONPojoFieldDeserializer>();
        for (String setterName : setterNames) {
            SetterInfo setterInfo = classStructureWrapper.getSetterInfo(setterName);
            JsonProperty jsonProperty = (JsonProperty) setterInfo.getAnnotation(JsonProperty.class);
            String name = setterName;
            if (jsonProperty != null) {
                if (!jsonProperty.deserialize())
                    continue;
                String mapperName = jsonProperty.name().trim();
                if (mapperName.length() > 0) {
                    name = mapperName;
                }
            }
            JSONPojoFieldDeserializer fieldDeserializer = new JSONPojoFieldDeserializer(name, setterInfo, jsonProperty);
            fieldSerializerMap.put(name, fieldDeserializer);
        }
        this.fieldDeserializers = new ArrayList<JSONPojoFieldDeserializer>(fieldSerializerMap.values());
        this.fixedFieldDeserializerValueMap = FixedNameValueMap.build(fieldSerializerMap);
        this.collision = this.fixedFieldDeserializerValueMap.isCollision() || (jsonTypeSetting != null && jsonTypeSetting.strict());
    }

    private void init() {
        for (JSONPojoFieldDeserializer fieldDeserializer : fieldDeserializers) {
            fieldDeserializer.initDeserializer();
        }
        for (JSONPojoFieldSerializer fieldSerializer : getterMethodSerializers) {
            fieldSerializer.initSerializer();
        }
        for (JSONPojoFieldSerializer fieldSerializer : getterFieldSerializers) {
            fieldSerializer.initSerializer();
        }
    }

    public Class<?> getSourceClass() {
        return classStructureWrapper.getSourceClass();
    }

    public boolean isRecord() {
        return classStructureWrapper.isRecord();
    }

    public boolean isTemporal() {
        return classStructureWrapper.isTemporal();
    }

    public int getFieldCount() {
        return classStructureWrapper.getFieldCount();
    }

    public ClassStructureWrapper.ClassWrapperType getClassWrapperType() {
        return classWrapperType;
    }

    public Object[] createConstructorArgs() {
        return classStructureWrapper.createConstructorArgs();
    }

    public JSONPojoFieldDeserializer getFieldDeserializer(char[] buf, int beginIndex, int endIndex, long hashValue) {
        return fixedFieldDeserializerValueMap.getValue(buf, beginIndex, endIndex, hashValue);
    }

    public JSONPojoFieldDeserializer getFieldDeserializer(byte[] buf, int beginIndex, int endIndex, long hashValue) {
        return fixedFieldDeserializerValueMap.getValue(buf, beginIndex, endIndex, hashValue);
    }

    public JSONPojoFieldDeserializer getFieldDeserializer(String field) {
        return fixedFieldDeserializerValueMap.getValue(field);
    }

    /**
     * Note: Ensure that the hash does not collide, otherwise do not use it
     * call isCollision() to check if collide
     *
     * @param hashValue
     * @return
     */
    public JSONPojoFieldDeserializer getFieldDeserializer(long hashValue) {
        return fixedFieldDeserializerValueMap.getValueByHash(hashValue);
    }

    public long hashChar(long rv, int c) {
        return fixedFieldDeserializerValueMap.hash(rv, c);
    }

    public long hashChar(long hv, int c1, int c2) {
        return fixedFieldDeserializerValueMap.hash(hv, c1, c2);
    }

    /**
     * create structure
     *
     * @param pojoClass
     * @return
     */
    public static JSONPojoStructure get(Class<?> pojoClass) {
        if (pojoClass == null) {
            throw new IllegalArgumentException("pojoClass is null");
        }
        JSONPojoStructure objectWrapper = objectStructureWarppers.get(pojoClass);
        if (objectWrapper != null) {
            return objectWrapper;
        }
        synchronized (pojoClass) {
            if (objectStructureWarppers.containsKey(pojoClass)) {
                return objectStructureWarppers.get(pojoClass);
            }
            ClassStructureWrapper classStructureWrapper = ClassStructureWrapper.get(pojoClass);
            if (classStructureWrapper == null) {
                throw new IllegalArgumentException("pojoClass " + pojoClass + " is not supported !");
            }
            objectWrapper = new JSONPojoStructure(classStructureWrapper);
            objectStructureWarppers.put(pojoClass, objectWrapper);
            // Delay initialization to solve the problem of circular dependency
            objectWrapper.init();
        }
        return objectWrapper;
    }

    public Object newInstance() throws Exception {
        return classStructureWrapper.newInstance();
    }

    public Object newInstance(Object[] constructorArgs) throws Exception {
        return classStructureWrapper.newInstance(constructorArgs);
    }

    public GenericParameterizedType getGenericType() {
        return genericType;
    }

    public boolean isCollision() {
        return collision;
    }

    public boolean isAssignableFromMap() {
        return classStructureWrapper.isAssignableFromMap();
    }

    public JSONPojoFieldSerializer[] getFieldSerializers(boolean useFields) {
        return useFields || forceUseFields ? getterFieldSerializers : getterMethodSerializers;
    }

    public boolean isPrivate() {
        return classStructureWrapper.isPrivate();
    }

    public boolean isForceUseFields() {
        return forceUseFields;
    }
}
