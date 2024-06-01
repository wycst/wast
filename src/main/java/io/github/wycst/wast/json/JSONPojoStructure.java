package io.github.wycst.wast.json;

import io.github.wycst.wast.common.reflect.ClassStructureWrapper;
import io.github.wycst.wast.common.reflect.GenericParameterizedType;
import io.github.wycst.wast.common.reflect.GetterInfo;
import io.github.wycst.wast.common.reflect.SetterInfo;
import io.github.wycst.wast.json.annotations.JsonProperty;
import io.github.wycst.wast.json.annotations.JsonTypeSetting;

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
    final JSONValueMatcher<JSONPojoFieldDeserializer> fieldDeserializerMatcher;
    // deserializers
    private final List<JSONPojoFieldDeserializer> fieldDeserializers;

    // getter methods
    private JSONPojoFieldSerializer[] getterMethodSerializers;
    // field
    private JSONPojoFieldSerializer[] getterFieldSerializers;
    private boolean forceUseFields;
    private JsonTypeSetting jsonTypeSetting;
    volatile boolean initialized;
    private final boolean supportedJavaBeanConvention;
    private final boolean enableJIT;

    private JSONPojoStructure(ClassStructureWrapper classStructureWrapper) {
        classStructureWrapper.getClass();
        this.classStructureWrapper = classStructureWrapper;
        this.classWrapperType = classStructureWrapper.getClassWrapperType();
        this.forceUseFields = classStructureWrapper.isForceUseFields();
        this.jsonTypeSetting = classStructureWrapper.getSourceClass().getDeclaredAnnotation(JsonTypeSetting.class);
        // serializer info
        List<GetterInfo> getterInfos = classStructureWrapper.getGetterInfos();
        List<JSONPojoFieldSerializer> fieldSerializers = new ArrayList<JSONPojoFieldSerializer>();
        Map<String, JSONPojoFieldSerializer> fieldSerializerHashMap = new HashMap<String, JSONPojoFieldSerializer>();
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
            fieldSerializerHashMap.put(name, fieldSerializer);
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
        supportedJavaBeanConvention = checkJavaBeanConvention(fieldSerializerHashMap);

        // 反序列化初始化
        this.genericType = GenericParameterizedType.actualType(classStructureWrapper.getSourceClass());
        Set<String> setterNames = classStructureWrapper.setterNames();

        Map<String, JSONPojoFieldDeserializer> fieldDeserializerHashMap = new HashMap<String, JSONPojoFieldDeserializer>();
        for (String setterName : setterNames) {
            SetterInfo setterInfo = classStructureWrapper.getSetterInfo(setterName);
            JsonProperty jsonProperty = (JsonProperty) setterInfo.getAnnotation(JsonProperty.class);
            String name = setterName;
            boolean priority = name.equals(setterInfo.getName());
            if (jsonProperty != null) {
                if (!jsonProperty.deserialize())
                    continue;
                String mapperName = jsonProperty.name().trim();
                if (mapperName.length() > 0) {
                    name = mapperName;
                    priority = true;
                }
            }
            JSONPojoFieldDeserializer fieldDeserializer = new JSONPojoFieldDeserializer(name, setterInfo, jsonProperty);
            fieldDeserializer.setPriority(priority);
            fieldDeserializerHashMap.put(name, fieldDeserializer);
        }
        this.fieldDeserializers = new ArrayList<JSONPojoFieldDeserializer>(fieldDeserializerHashMap.values());
        this.fieldDeserializerMatcher = JSONValueMatcher.build(fieldDeserializerHashMap);

        this.enableJIT = jsonTypeSetting != null && jsonTypeSetting.enableJIT();
    }

    /**
     * 检查pojo是否满足实体bean公约规范（每个属性都定义了相应的getter/setter方法）
     *
     * @param fieldSerializerHashMap
     * @return
     */
    private boolean checkJavaBeanConvention(Map<String, JSONPojoFieldSerializer> fieldSerializerHashMap) {
        // private class is not supported
//        if (classStructureWrapper.isPrivate()) {
//            return false;
//        }
//        if (classStructureWrapper.isForceUseFields()) return true;
//        for (JSONPojoFieldSerializer fieldSerializer : getterFieldSerializers) {
//            JSONPojoFieldSerializer methodSerializer = fieldSerializerHashMap.get(fieldSerializer.name);
//            if (methodSerializer == null || methodSerializer.getGetterInfo().getReturnType() != fieldSerializer.getGetterInfo().getReturnType() || !methodSerializer.getGetterInfo().existField()) {
//                return false;
//            }
//        }
        return !classStructureWrapper.isPrivate();
    }

    void ensureInitialized() {
        if (initialized) return;
        synchronized (this) {
            if (initialized) return;
            initialize();
        }
    }

    void initialize() {
        for (JSONPojoFieldDeserializer fieldDeserializer : fieldDeserializers) {
            fieldDeserializer.initDeserializer();
        }
        for (JSONPojoFieldSerializer fieldSerializer : getterMethodSerializers) {
            fieldSerializer.initSerializer();
        }
        for (JSONPojoFieldSerializer fieldSerializer : getterFieldSerializers) {
            fieldSerializer.initSerializer();
        }
        initialized = true;
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

//    public long hashChar(long rv, int c) {
//        return fieldDeserializerMatcher.hash(rv, c);
//    }
//
//    public long hashChar(long hv, int c1, int c2) {
//        return fieldDeserializerMatcher.hash(hv, c1, c2);
//    }
//
//    public JSONPojoFieldDeserializer matchFieldDeserializer(CharSource source, char[] buf, int offset, int endToken, JSONParseContext parseContext) {
//        return fieldDeserializerMatcher.matchValue(source, buf, offset, endToken, parseContext);
//    }
//
//    public JSONPojoFieldDeserializer matchFieldDeserializer(CharSource source, byte[] buf, int offset, int endToken, JSONParseContext parseContext) {
//        return fieldDeserializerMatcher.matchValue(source, buf, offset, endToken, parseContext);
//    }
//
//    public JSONPojoFieldDeserializer getFieldDeserializer(String fieldName) {
//        return fieldDeserializerMatcher.getValue(fieldName);
//    }
//
//    public JSONPojoFieldDeserializer getFieldDeserializer(char[] buf, int beginIndex, int endIndex, long hashValue) {
//        return fieldDeserializerMatcher.getValue(buf, beginIndex, endIndex, hashValue);
//    }
//
//    public JSONPojoFieldDeserializer getFieldDeserializer(byte[] buf, int beginIndex, int endIndex, long hashValue) {
//        return fieldDeserializerMatcher.getValue(buf, beginIndex, endIndex, hashValue);
//    }

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
            objectWrapper.initialize();
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

    /**
     * supported java bean convention
     *
     * @return
     */
    public boolean isSupportedJavaBeanConvention() {
        return this.supportedJavaBeanConvention;
    }

    /**
     * if supported JIT optimization
     *
     * @return
     */
    public boolean isSupportedJIT() {
        return isSupportedJavaBeanConvention() && enableJIT;
    }
}
