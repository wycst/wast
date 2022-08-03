package io.github.wycst.wast.json.reflect;

import io.github.wycst.wast.common.reflect.ClassStructureWrapper;
import io.github.wycst.wast.common.reflect.GenericParameterizedType;
import io.github.wycst.wast.common.reflect.GetterInfo;
import io.github.wycst.wast.common.reflect.SetterInfo;
import io.github.wycst.wast.json.annotations.JsonProperty;
import io.github.wycst.wast.json.util.FixedNameValueMap;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 对ClassStructureWrapper的进一步包装，提供给json模块使用
 *
 * @Author wangyunchao
 */
public class ObjectStructureWrapper {

    private static Map<Class<?>, ObjectStructureWrapper> objectStructureWarppers = new ConcurrentHashMap<Class<?>, ObjectStructureWrapper>();

    private final ClassStructureWrapper classStructureWrapper;
    private final GenericParameterizedType genericType;
    private final FixedNameValueMap<FieldDeserializer> fieldDeserializerMap;
    // deserializers
    private List<FieldDeserializer> fieldDeserializers = new ArrayList<FieldDeserializer>();

    // getter methods
    private List<FieldSerializer> getterMethodSerializers = new ArrayList<FieldSerializer>();
    // field
    private List<FieldSerializer> getterFieldSerializers = new ArrayList<FieldSerializer>();
    private final boolean collision;
    private boolean forceUseFields;

    private ObjectStructureWrapper(ClassStructureWrapper classStructureWrapper) {
        classStructureWrapper.getClass();
        this.classStructureWrapper = classStructureWrapper;
        this.forceUseFields = classStructureWrapper.isForceUseFields();
        // serializer info
        List<GetterInfo> getterInfos = classStructureWrapper.getGetterInfos();
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
            FieldSerializer fieldSerializer = new FieldSerializer(getterInfo, name);
            getterMethodSerializers.add(fieldSerializer);
        }

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
            FieldSerializer fieldSerializer = new FieldSerializer(getterInfo, name);
            getterFieldSerializers.add(fieldSerializer);
        }

        // deserializer info
        this.fieldDeserializerMap = new FixedNameValueMap(classStructureWrapper.setterNames().size());
        this.genericType = GenericParameterizedType.actualType(classStructureWrapper.getSourceClass());
        Set<String> setterNames = classStructureWrapper.setterNames();
        for (String setterName : setterNames) {
            SetterInfo setterInfo = classStructureWrapper.getSetterInfo(setterName);
            JsonProperty jsonProperty = (JsonProperty) setterInfo.getAnnotation(JsonProperty.class);
            String mapperName = null;
            if (jsonProperty != null) {
                if (!jsonProperty.deserialize())
                    continue;
                mapperName = jsonProperty.name();
            }
            FieldDeserializer fieldDeserializer = new FieldDeserializer(setterName, setterInfo, jsonProperty);
            fieldDeserializerMap.putValue(setterName, fieldDeserializer);
            fieldDeserializers.add(fieldDeserializer);

            // alias name
            if (mapperName != null && (mapperName = mapperName.trim()).length() > 0) {
                if (!mapperName.equals(setterName)) {
                    fieldDeserializer = new FieldDeserializer(mapperName, setterInfo, jsonProperty);
                    fieldDeserializerMap.putValue(mapperName, fieldDeserializer);
                    fieldDeserializers.add(fieldDeserializer);
                }
            }
        }

        AtomicBoolean atomicBoolean = new AtomicBoolean();
        Collections.sort(fieldDeserializers, new Comparator<FieldDeserializer>() {
            @Override
            public int compare(FieldDeserializer o1, FieldDeserializer o2) {
                Integer h1 = o1.getHash();
                Integer h2 = o2.getHash();
                int r = h1.compareTo(h2);
                if (r == 0) {
                    atomicBoolean.set(true);
                }
                return r;
            }
        });
        this.collision = atomicBoolean.get();
    }

    private void init() {
        for (FieldDeserializer fieldDeserializer : fieldDeserializers) {
            fieldDeserializer.initDeserializer();
        }
        for (FieldSerializer fieldSerializer : getterMethodSerializers) {
            fieldSerializer.initSerializer();
        }
        for (FieldSerializer fieldSerializer : getterFieldSerializers) {
            fieldSerializer.initSerializer();
        }
    }

    public boolean isRecord() {
        return classStructureWrapper.isRecord();
    }

    public int getFieldCount() {
        return classStructureWrapper.getFieldCount();
    }

    public Object[] createConstructorArgs() {
        return classStructureWrapper.createConstructorArgs();
    }

    public FieldDeserializer getFieldDeserializer(char[] buf, int beginIndex, int endIndex, int hashValue) {
        return fieldDeserializerMap.getValue(buf, beginIndex, endIndex, hashValue);
    }

    public FieldDeserializer getFieldDeserializer(String field) {
        return fieldDeserializerMap.getValue(field);
    }

    /**
     * Note: Ensure that the hash does not collide, otherwise do not use it
     * call isCollision() to check if collide
     *
     * @param hashValue
     * @return
     */
    public FieldDeserializer getFieldDeserializer(int hashValue) {
        return fieldDeserializerMap.getValueByHash(hashValue);
    }

    public static ObjectStructureWrapper get(Class<?> pojoClass) {
        if (pojoClass == null) {
            throw new IllegalArgumentException("pojoClass is null");
        }
        ObjectStructureWrapper objectWrapper = objectStructureWarppers.get(pojoClass);
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
            objectWrapper = new ObjectStructureWrapper(classStructureWrapper);
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

    public List<FieldSerializer> getFieldSerializers(boolean useFields) {
        return useFields || forceUseFields ? getterFieldSerializers : getterMethodSerializers;
    }
}
