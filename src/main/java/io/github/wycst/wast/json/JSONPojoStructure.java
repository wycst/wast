package io.github.wycst.wast.json;

import io.github.wycst.wast.common.reflect.ClassStrucWrap;
import io.github.wycst.wast.common.reflect.GenericParameterizedType;
import io.github.wycst.wast.common.reflect.GetterInfo;
import io.github.wycst.wast.common.reflect.SetterInfo;
import io.github.wycst.wast.json.annotations.JsonProperty;
import io.github.wycst.wast.json.annotations.JsonTypeSetting;

import java.util.*;

/**
 * 对ClassStrucWrap的进一步包装，提供给json模块使用
 *
 * @author wangyunchao
 */
public final class JSONPojoStructure {

    final JSONStore store;
    final ClassStrucWrap classStrucWrap;
    private final ClassStrucWrap.ClassWrapperType classWrapperType;
    private final GenericParameterizedType<?> genericType;
    final JSONValueMatcher<JSONPojoFieldDeserializer> fieldDeserializerMatcher;
    // deserializers
    final List<JSONPojoFieldDeserializer> fieldDeserializers;

    // getter methods
    private final JSONPojoFieldSerializer[] getterMethodSerializers;
    // field
    private final JSONPojoFieldSerializer[] getterFieldSerializers;
    private final boolean forceUseFields;
    volatile boolean fieldDeserializersInitialized;
    volatile boolean fieldSerializersInitialized;
    private final boolean supportedJavaBeanConvention;
    private final boolean enableJIT;
    final boolean supportedDeserOptimize;
    final Map<String, JSONPropertyDefinition> propertyDefinitions;

    JSONPojoStructure(JSONStore store, ClassStrucWrap strucWrap) {
        this(store, strucWrap, null);
    }

    JSONPojoStructure(JSONStore store, ClassStrucWrap strucWrap, Map<String, JSONPropertyDefinition> propertyDefinitions) {
        strucWrap.getClass();
        this.store = store;
        this.classStrucWrap = strucWrap;
        this.classWrapperType = strucWrap.getClassWrapperType();
        this.forceUseFields = strucWrap.isForceUseFields();
        JsonTypeSetting jsonTypeSetting = (JsonTypeSetting) strucWrap.getDeclaredAnnotation(JsonTypeSetting.class);
        if (propertyDefinitions == null) {
            propertyDefinitions = new HashMap<String, JSONPropertyDefinition>();
        }
        this.propertyDefinitions = propertyDefinitions;
        // serializer info
        List<GetterInfo> getterInfos = strucWrap.getGetterInfos();
        List<JSONPojoFieldSerializer> fieldSerializers = new ArrayList<JSONPojoFieldSerializer>();
        Map<String, JSONPojoFieldSerializer> fieldSerializerHashMap = new HashMap<String, JSONPojoFieldSerializer>();
        for (GetterInfo getterInfo : getterInfos) {
            if (store.isNoneStringMode() && isStringParameterizedType(getterInfo.getGenericParameterizedType()))
                continue;
            String name = getterInfo.getName();
            JsonProperty jsonProperty = (JsonProperty) getterInfo.getAnnotation(JsonProperty.class);
            JSONPropertyDefinition annotationedProperty = JSONPropertyDefinition.of(jsonProperty);
            JSONPropertyDefinition definition = propertyDefinitions.get(name);
            if (definition == null) {
                definition = annotationedProperty;
            } else {
                definition.merge(annotationedProperty);
            }
            if (definition != null) {
                if (!definition.serialize()) {
                    continue;
                }
                String aliasName;
                if (!(aliasName = definition.name()).isEmpty()) {
                    name = aliasName;
                }
            }
            JSONPojoFieldSerializer fieldSerializer = new JSONPojoFieldSerializer(store, getterInfo, name, definition);
            fieldSerializers.add(fieldSerializer);
            fieldSerializerHashMap.put(name, fieldSerializer);
        }
        getterMethodSerializers = fieldSerializers.toArray(new JSONPojoFieldSerializer[fieldSerializers.size()]);

        fieldSerializers.clear();
        List<GetterInfo> getterByFieldInfos = strucWrap.getGetterInfos(true);
        for (GetterInfo getterInfo : getterByFieldInfos) {
            if (store.isNoneStringMode() && isStringParameterizedType(getterInfo.getGenericParameterizedType()))
                continue;
            String name = getterInfo.getName();
            JsonProperty jsonProperty = (JsonProperty) getterInfo.getAnnotation(JsonProperty.class);
            JSONPropertyDefinition annotationedProperty = JSONPropertyDefinition.of(jsonProperty);
            JSONPropertyDefinition definition = propertyDefinitions.get(name);
            if (definition == null) {
                definition = annotationedProperty;
            } else {
                definition.merge(annotationedProperty);
            }
            if (definition != null) {
                if (!definition.serialize()) {
                    continue;
                }
                String aliasName;
                if (!(aliasName = definition.name()).isEmpty()) {
                    name = aliasName;
                }
            }
            JSONPojoFieldSerializer fieldSerializer = new JSONPojoFieldSerializer(store, getterInfo, name, definition);
            fieldSerializers.add(fieldSerializer);
        }
        getterFieldSerializers = fieldSerializers.toArray(new JSONPojoFieldSerializer[fieldSerializers.size()]);
        supportedJavaBeanConvention = checkJavaBeanConvention(fieldSerializerHashMap);

        // 反序列化初始化
        this.genericType = GenericParameterizedType.actualType(strucWrap.getSourceClass());
        Set<String> setterNames = strucWrap.setterNames();

        Map<String, JSONPojoFieldDeserializer> fieldDeserializerHashMap = new HashMap<String, JSONPojoFieldDeserializer>();
        boolean deserializeOptimizable = true;
        for (String setterName : setterNames) {
            SetterInfo setterInfo = strucWrap.getSetterInfo(setterName);
            String name = setterName;
            boolean priority = name.equals(setterInfo.getName());
            JsonProperty jsonProperty = (JsonProperty) setterInfo.getAnnotation(JsonProperty.class);
            JSONPropertyDefinition annotationedProperty = JSONPropertyDefinition.of(jsonProperty);
            JSONPropertyDefinition definition = propertyDefinitions.get(name);
            if (definition == null) {
                definition = annotationedProperty;
            } else {
                definition.merge(annotationedProperty);
            }
            if (definition != null) {
                if (!definition.deserialize())
                    continue;
                String mapperName = definition.name();
                if (!mapperName.isEmpty()) {
                    name = mapperName;
                    priority = true;
                }
            }
            JSONPojoFieldDeserializer fieldDeserializer = new JSONPojoFieldDeserializer(store, name, setterInfo, definition);
            fieldDeserializer.setPriority(priority);
            fieldDeserializerHashMap.put(name, fieldDeserializer);

            if (!fieldDeserializer.ensuredTypeDeserializable()) {
                deserializeOptimizable = false;
            }
        }
        this.fieldDeserializers = new ArrayList<JSONPojoFieldDeserializer>(fieldDeserializerHashMap.values());
        this.fieldDeserializerMatcher = JSONValueMatcher.build(fieldDeserializerHashMap);

        this.enableJIT = jsonTypeSetting != null && jsonTypeSetting.enableJIT();
        this.supportedDeserOptimize = /*this.enableJIT && */deserializeOptimizable;
    }

    static boolean isStringParameterizedType(GenericParameterizedType<?> parameterizedType) {
        if (parameterizedType == null) return false;
        if (parameterizedType.getActualType() == String.class) return true;
        return parameterizedType.getValueType() == GenericParameterizedType.StringType;
    }

    /**
     * 检查pojo是否满足实体bean公约规范（每个属性都定义了相应的getter/setter方法）
     */
    private boolean checkJavaBeanConvention(Map<String, JSONPojoFieldSerializer> fieldSerializerHashMap) {
        return !classStrucWrap.isPrivate();
    }

    void ensureInitializedFieldSerializers() {
        if (fieldSerializersInitialized) return;
        synchronized (this) {
            if (fieldSerializersInitialized) return;
            for (JSONPojoFieldSerializer fieldSerializer : getterMethodSerializers) {
                fieldSerializer.initSerializer();
            }
            for (JSONPojoFieldSerializer fieldSerializer : getterFieldSerializers) {
                fieldSerializer.initSerializer();
            }
            fieldSerializersInitialized = true;
        }
    }

    void ensureInitializedFieldDeserializers() {
        if (fieldDeserializersInitialized) return;
        synchronized (this) {
            if (fieldDeserializersInitialized) return;
            for (JSONPojoFieldDeserializer fieldDeserializer : fieldDeserializers) {
                fieldDeserializer.initDeserializer();
            }
            fieldDeserializersInitialized = true;
        }
    }

    public Class<?> getSourceClass() {
        return classStrucWrap.getSourceClass();
    }

    public boolean isRecord() {
        return classStrucWrap.isRecord();
    }

    public boolean isTemporal() {
        return classStrucWrap.isTemporal();
    }

    public int getFieldCount() {
        return classStrucWrap.getFieldCount();
    }

    public ClassStrucWrap.ClassWrapperType getClassWrapperType() {
        return classWrapperType;
    }

    public Object[] createConstructorArgs() {
        return classStrucWrap.createConstructorArgs();
    }

    public Object newInstance() throws Exception {
        return classStrucWrap.newInstance();
    }

    public Object newInstance(Object[] constructorArgs) throws Exception {
        return classStrucWrap.newInstance(constructorArgs);
    }

    public GenericParameterizedType<?> getGenericType() {
        return genericType;
    }

    public boolean isAssignableFromMap() {
        return classStrucWrap.isAssignableFromMap();
    }

    public JSONPojoFieldSerializer[] getFieldSerializers(boolean useFields) {
        return useFields || forceUseFields ? getterFieldSerializers : getterMethodSerializers;
    }

    public boolean isPrivate() {
        return classStrucWrap.isPrivate();
    }

    public boolean isPublic() {
        return classStrucWrap.isPublic();
    }

    public boolean isForceUseFields() {
        return forceUseFields;
    }

    /**
     * supported java bean convention
     */
    public boolean isSupportedJavaBeanConvention() {
        return this.supportedJavaBeanConvention;
    }

    /**
     * if supported JIT optimization
     */
    public boolean isSupportedJIT() {
        return isSupportedJavaBeanConvention() && isPublic() && enableJIT;
    }

    boolean isSupportedOptimize() {
        return supportedDeserOptimize && fieldDeserializerMatcher.isSupportedOptimize();
    }
}
