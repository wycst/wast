package io.github.wycst.wast.json;

import io.github.wycst.wast.common.beans.ArrayQueueMap;
import io.github.wycst.wast.common.beans.DateFormatter;
import io.github.wycst.wast.common.compiler.JDKCompiler;
import io.github.wycst.wast.common.expression.EvaluateEnvironment;
import io.github.wycst.wast.common.expression.Expression;
import io.github.wycst.wast.common.reflect.ClassStrucWrap;
import io.github.wycst.wast.common.reflect.GenericParameterizedType;
import io.github.wycst.wast.common.reflect.ReflectConsts;
import io.github.wycst.wast.common.reflect.UnsafeHelper;
import io.github.wycst.wast.common.utils.EnvUtils;
import io.github.wycst.wast.json.exceptions.JSONException;

import java.lang.reflect.Constructor;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * JSONStore 是一个用于内部管理 JSON 序列化和反序列化组件的存储类（工厂），提供以下主要功能：
 * POJO 结构缓存：通过 pojoStrucs 缓存 POJO 类的结构信息
 * 类型序列化器缓存：serializerMap
 * 类型反序列化器缓存： deserializerMap
 * 动态创建：根据类型动态创建相应的序列化器和反序列化器
 *
 * @Author: wangy
 * @Date: 2025/12/29 21:05
 * @Description:
 */
@SuppressWarnings({"all"})
final class JSONStore {
    final static int CATEGORY_LEN = ReflectConsts.ClassCategory.values().length;
    // Default JSONStore
    static final JSONStore INSTANCE = new JSONStore();
    static final JSONDefaultParser PARSER;

    static {
        // default any
        JSONTypeDeserializer.ANY = INSTANCE.ANY_DESER;
        PARSER = INSTANCE.parser;
    }

    final JSONTypeDeserializer[] TYPE_DESERIALIZERS = new JSONTypeDeserializer[CATEGORY_LEN];
    final JSONTypeSerializer[] TYPE_SERIALIZERS = new JSONTypeSerializer[CATEGORY_LEN];

    // POJO structure cache
    final Map<Class<?>, JSONPojoStructure> pojoStrucs = new ArrayQueueMap<Class<?>, JSONPojoStructure>(8192);
    // Type deserializer cache
    final Map<Class<?>, JSONTypeDeserializer> deserializerMap;
    final Map<Class<?>, JSONTypeDeserializer> temporaryDeserializerMap = new HashMap<Class<?>, JSONTypeDeserializer>();
    final Set<Class<?>> registeredDeserializerApplyAllSubTypes = new HashSet<Class<?>>();
    static Set<Class<?>> BUILT_IN_DESER_SET;

    // Type serializer cache
    final Map<Class<?>, JSONTypeSerializer> serializerMap;
    final Map<Class<?>, JSONTypeSerializer> temporarySerializerMap = new HashMap<Class<?>, JSONTypeSerializer>();
    final Set<Class<?>> registeredSerializerApplyAllSubTypes = new HashSet<Class<?>>();
    static Set<Class<?>> BUILT_IN_SER_SET;

    // Collection deserializer
    final JSONTypeDeserializer.ArrayImpl ARRAY_DESER;
    final JSONTypeDeserializer.CollectionImpl COLLECTION_DESER;
    final JSONTypeDeserializer.MapImpl MAP_DESER;
    final JSONTypeDeserializer.ObjectImpl OBJECT_DESER;
    final JSONTypeDeserializer.AnyImpl ANY_DESER;
    final JSONTypeDeserializer.MapImpl HASH_TABLE_DESER;

    final JSONTypeSerializer.CollectionImpl COLLECTION_SER;
    final JSONTypeSerializer ARRAY_SER;
    final JSONTypeSerializer OBJECT_SER;
    final JSONTypeSerializer MAP_SER;
    final JSONTypeSerializer ANY_SER;
    final JSONDefaultParser parser = new JSONDefaultParser();

    boolean enableJIT = JSONGeneral.ENABLE_JIT;
    boolean noneStringMode = false;
    JSONTypeDeserializer.CharSequenceImpl STRING_DESER;
    JSONTypeSerializer STRING_SER;

    public JSONStore() {
        deserializerMap = new HashMap<Class<?>, JSONTypeDeserializer>(64);
        serializerMap = new HashMap<Class<?>, JSONTypeSerializer>(64);

        STRING_DESER = JSONTypeDeserializer.CHAR_SEQUENCE_STRING;
        ARRAY_DESER = new JSONTypeDeserializer.ArrayImpl(this);
        COLLECTION_DESER = new JSONTypeDeserializer.CollectionImpl(this);
        MAP_DESER = new JSONTypeDeserializer.MapImpl(this);
        HASH_TABLE_DESER = JSONTypeDeserializer.MapImpl.hashtable(this);
        OBJECT_DESER = new JSONTypeDeserializer.ObjectImpl(this);
        ANY_DESER = new JSONTypeDeserializer.AnyImpl(this);

        // Category serializers
        TYPE_DESERIALIZERS[ReflectConsts.ClassCategory.CharSequence.ordinal()] = JSONTypeDeserializer.CHAR_SEQUENCE;
        TYPE_DESERIALIZERS[ReflectConsts.ClassCategory.NumberCategory.ordinal()] = JSONTypeDeserializer.NUMBER;
        TYPE_DESERIALIZERS[ReflectConsts.ClassCategory.BoolCategory.ordinal()] = JSONTypeDeserializer.BOOLEAN;
        TYPE_DESERIALIZERS[ReflectConsts.ClassCategory.DateCategory.ordinal()] = JSONTypeDeserializer.DATE;
        TYPE_DESERIALIZERS[ReflectConsts.ClassCategory.ClassCategory.ordinal()] = JSONTypeDeserializer.CLASS;
        TYPE_DESERIALIZERS[ReflectConsts.ClassCategory.EnumCategory.ordinal()] = JSONTypeDeserializer.ENUM;
        TYPE_DESERIALIZERS[ReflectConsts.ClassCategory.AnnotationCategory.ordinal()] = JSONTypeDeserializer.ANNOTATION;
        TYPE_DESERIALIZERS[ReflectConsts.ClassCategory.Binary.ordinal()] = JSONTypeDeserializer.BINARY;
        TYPE_DESERIALIZERS[ReflectConsts.ClassCategory.ArrayCategory.ordinal()] = ARRAY_DESER;
        TYPE_DESERIALIZERS[ReflectConsts.ClassCategory.CollectionCategory.ordinal()] = COLLECTION_DESER;
        TYPE_DESERIALIZERS[ReflectConsts.ClassCategory.MapCategory.ordinal()] = MAP_DESER;
        TYPE_DESERIALIZERS[ReflectConsts.ClassCategory.ObjectCategory.ordinal()] = OBJECT_DESER;
        TYPE_DESERIALIZERS[ReflectConsts.ClassCategory.ANY.ordinal()] = ANY_DESER;
        // TYPE_DESERIALIZERS[ReflectConsts.ClassCategory.NonInstance.ordinal()] = null;

        STRING_SER = JSONTypeSerializer.CHAR_SEQUENCE_STRING;
        COLLECTION_SER = new JSONTypeSerializer.CollectionImpl(this);
        ARRAY_SER = new JSONTypeSerializer.ArrayImpl(this);
        OBJECT_SER = new JSONTypeSerializer.ObjectImpl(this);
        MAP_SER = new JSONTypeSerializer.MapImpl(this);
        ANY_SER = new JSONTypeSerializer.AnyImpl(this);

        // Default serializer
        TYPE_SERIALIZERS[ReflectConsts.ClassCategory.CharSequence.ordinal()] = JSONTypeSerializer.CHAR_SEQUENCE;
        TYPE_SERIALIZERS[ReflectConsts.ClassCategory.NumberCategory.ordinal()] = JSONTypeSerializer.NUMBER;
        TYPE_SERIALIZERS[ReflectConsts.ClassCategory.BoolCategory.ordinal()] = JSONTypeSerializer.SIMPLE;
        TYPE_SERIALIZERS[ReflectConsts.ClassCategory.DateCategory.ordinal()] = JSONTypeSerializer.DATE;
        TYPE_SERIALIZERS[ReflectConsts.ClassCategory.ClassCategory.ordinal()] = JSONTypeSerializer.CLASS;
        TYPE_SERIALIZERS[ReflectConsts.ClassCategory.EnumCategory.ordinal()] = JSONTypeSerializer.ENUM;
        TYPE_SERIALIZERS[ReflectConsts.ClassCategory.AnnotationCategory.ordinal()] = JSONTypeSerializer.ANNOTATION;
        TYPE_SERIALIZERS[ReflectConsts.ClassCategory.Binary.ordinal()] = JSONTypeSerializer.BINARY;
        TYPE_SERIALIZERS[ReflectConsts.ClassCategory.ArrayCategory.ordinal()] = ARRAY_SER;
        TYPE_SERIALIZERS[ReflectConsts.ClassCategory.CollectionCategory.ordinal()] = COLLECTION_SER;
        TYPE_SERIALIZERS[ReflectConsts.ClassCategory.MapCategory.ordinal()] = MAP_SER;
        TYPE_SERIALIZERS[ReflectConsts.ClassCategory.ObjectCategory.ordinal()] = OBJECT_SER;
        TYPE_SERIALIZERS[ReflectConsts.ClassCategory.ANY.ordinal()] = ANY_SER;

        init();

        // only once
        if (BUILT_IN_DESER_SET == null) {
            BUILT_IN_DESER_SET = new HashSet<Class<?>>(JSONTypeDeserializer.GLOBAL_DESERIALIZERS.keySet());
            BUILT_IN_DESER_SET.addAll(deserializerMap.keySet());
        }
        if (BUILT_IN_SER_SET == null) {
            BUILT_IN_SER_SET = new HashSet<Class<?>>(JSONTypeSerializer.GLOBAL_SERIALIZERS.keySet());
            BUILT_IN_SER_SET.addAll(serializerMap.keySet());
        }
    }

    void init() {
        putTypeDeserializer(ANY_DESER, null, Object.class);
        putTypeDeserializer(HASH_TABLE_DESER, Dictionary.class, Hashtable.class);
        putTypeDeserializer(MAP_DESER, Map.class, HashMap.class, LinkedHashMap.class);
        putTypeDeserializer(COLLECTION_DESER, List.class, Set.class);

        putTypeSerializer(ARRAY_SER, Object[].class);
        putTypeSerializer(ANY_SER, Object.class);
        putTypeSerializer(MAP_SER, LinkedHashMap.class, HashMap.class, ConcurrentHashMap.class, Hashtable.class);
        putTypeSerializer(COLLECTION_SER, ArrayList.class, LinkedList.class, Vector.class, HashSet.class, LinkedHashSet.class);
    }

    void putTypeDeserializer(JSONTypeDeserializer typeDeserializer, Class<?>... types) {
        JSONTypeDeserializer.putTypeDeserializer(deserializerMap, typeDeserializer, types);
    }

    void putTypeSerializer(JSONTypeSerializer typeSerializer, Class<?>... types) {
        JSONTypeSerializer.putTypeSerializer(serializerMap, typeSerializer, types);
    }

    /**
     * 注册模块
     *
     * @param module 模块实例
     */
    public void register(JSONTypeModule module) {
        module.register(new JSONTypeRegistor(this));
    }

    /**
     * 注册模块
     *
     * @param moduleClass 模块类
     */
    public void register(Class<? extends JSONTypeModule> moduleClass) {
        try {
            JSONTypeModule module = moduleClass.newInstance();
            register(module);
        } catch (Throwable throwable) {
            throw new JSONException(throwable);
        }
    }

    public <T> void register(Class<T> targetClass, JSONTypeMapper<T> mapper, boolean applyAllSubClass) {
        targetClass.getClass();
        register(buildDeserializer(mapper), targetClass, applyAllSubClass);
        register(buildSerializer(mapper), targetClass, applyAllSubClass);
    }

    /**
     * create structure
     *
     * @param pojoClass 实体类
     * @return 实体类结构
     */
    JSONPojoStructure getPojoStruc(Class<?> pojoClass) {
        if (pojoClass == null) {
            throw new IllegalArgumentException("pojoClass is null");
        }
        JSONPojoStructure pojoStructure = pojoStrucs.get(pojoClass);
        if (pojoStructure != null) {
            return pojoStructure;
        }
        synchronized (pojoClass) {
            if (pojoStrucs.containsKey(pojoClass)) {
                return pojoStrucs.get(pojoClass);
            }
            ClassStrucWrap wrap = ClassStrucWrap.get(pojoClass);
            if (wrap == null) {
                throw new IllegalArgumentException("pojoClass " + pojoClass + " is not supported !");
            }
            pojoStructure = new JSONPojoStructure(this, wrap);
            pojoStrucs.put(pojoClass, pojoStructure);
        }
        return pojoStructure;
    }

    // Get the corresponding deserializer according to the type
    // Single example may be used
    public JSONTypeDeserializer getTypeDeserializer(Class<?> type) {
        JSONTypeDeserializer deserializer = deserializerMap.get(type);
        if (deserializer != null) {
            return deserializer;
        }

        // global
        deserializer = JSONTypeDeserializer.GLOBAL_DESERIALIZERS.get(type);
        if (deserializer != null) {
            deserializerMap.put(type, deserializer);
            return deserializer;
        }

        // secondary search resolve the initialization dead loop dependency
        deserializer = temporaryDeserializerMap.get(type);
        if (deserializer != null) {
            return deserializer.ensureInitialized();
        }

        if ((deserializer = checkSuperclassDeserializerRegistered(type)) != null) {
            deserializerMap.put(type, deserializer);
            return deserializer;
        }

        ReflectConsts.ClassCategory classCategory = ReflectConsts.getClassCategory(type);
        synchronized (type) {
            deserializer = deserializerMap.get(type);
            if (deserializer != null) {
                return deserializer;
            }
            switch (classCategory) {
                case ObjectCategory: {
                    deserializer = createObjectDeserializer(type);
                    break;
                }
                case EnumCategory: {
                    deserializer = createEnumDeserializer(type);
                    break;
                }
                case CollectionCategory: {
                    GenericParameterizedType<?> parameterizedType = GenericParameterizedType.collectionOf((Class<? extends Collection>) type);
                    deserializer = new JSONTypeDeserializer.CollectionImpl.CollectionInstanceImpl(this, parameterizedType);
                    break;
                }
                case MapCategory: {
                    GenericParameterizedType<?> parameterizedType = GenericParameterizedType.mapOf((Class<? extends Map>) type);
                    deserializer = new JSONTypeDeserializer.MapInstanceImpl(this, parameterizedType);
                    break;
                }
                case NonInstance: {
                    deserializer = new JSONTypeDeserializer.NonInstanceImpl(this, type);
                    break;
                }
            }
            if (deserializer == null) {
                if ((deserializer = TYPE_DESERIALIZERS[classCategory.ordinal()]) == null) {
                    return null;
                }
            }
            // put to temporary
            temporaryDeserializerMap.put(type, deserializer);
            // ensure initialization is complete
            deserializer.ensureInitialized();

            // add to official map after initialization is complete
            deserializerMap.put(type, deserializer);
            // remove
            temporaryDeserializerMap.remove(type);
        }

        return deserializer;
    }

    /**
     * use for FieldDeserializer
     *
     * @param genericParameterizedType 泛型参数
     * @param property                 配置属性
     * @return Pojo属性反序列化器
     */
    JSONTypeDeserializer getFieldDeserializer(GenericParameterizedType<?> genericParameterizedType, JSONPropertyDefinition property) {
        if (genericParameterizedType == null || genericParameterizedType.getActualType() == null)
            return ANY_DESER;
        ReflectConsts.ClassCategory classCategory = genericParameterizedType.getActualClassCategory();
        // Find matching deserializers or new deserializer instance by type
        switch (classCategory) {
            case CollectionCategory:
                // collection Deserializer instance
                int collectionType = JSONTypeDeserializer.getCollectionType(genericParameterizedType.getActualType());
                switch (collectionType) {
                    case JSONGeneral.COLLECTION_ARRAYLIST_TYPE:
                        return new JSONTypeDeserializer.CollectionImpl.ArrayListImpl(this, genericParameterizedType);
                    case JSONGeneral.COLLECTION_HASHSET_TYPE:
                        return new JSONTypeDeserializer.CollectionImpl.HashSetImpl(this, genericParameterizedType);
                    default:
                        return new JSONTypeDeserializer.CollectionImpl.CollectionInstanceImpl(this, genericParameterizedType);
                }
            case DateCategory: {
                // Date Deserializer Instance
                // cannot use singleton because the pattern of each field may be different
                return property == null ? JSONTypeDeserializer.DATE : new JSONTypeDeserializer.DateImpl.DateInstanceImpl(genericParameterizedType, property);
            }
            case ObjectCategory: {
                ClassStrucWrap classStrucWrap = ClassStrucWrap.get(genericParameterizedType.getActualType());
                // Like the date type, the temporal type cannot use singletons also
                if (classStrucWrap.isTemporal()) {
                    ClassStrucWrap.ClassWrapperType classWrapperType = classStrucWrap.getClassWrapperType();
                    return JSONTemporalDeserializer.getTemporalDeserializerInstance(classWrapperType, genericParameterizedType, property);
                }
            }
        }

        // singleton from cache
        return getTypeDeserializer(genericParameterizedType.getActualType());
    }

    JSONTypeDeserializer createObjectDeserializer(Class<?> type) {
        ClassStrucWrap classStrucWrap = ClassStrucWrap.get(type);
        if (classStrucWrap.isRecord()) {
            return new JSONPojoDefaultDeserializer.RecordImpl(getPojoStruc(type));
        } else {
            if (classStrucWrap.isTemporal()) {
                ClassStrucWrap.ClassWrapperType classWrapperType = classStrucWrap.getClassWrapperType();
                return JSONTemporalDeserializer.getTemporalDeserializerInstance(classWrapperType, GenericParameterizedType.actualType(type), null);
            }
            JSONPojoStructure pojoStructure = getPojoStruc(type);
            if (pojoStructure.isSupportedOptimize()) {
                return JSONPojoOptimizeDeserializer.optimize(pojoStructure);
            } else {
                return JSONPojoDefaultDeserializer.create(pojoStructure);
            }
        }
    }

    void registerInternal(JSONTypeDeserializer typeDeserializer, Class<?> type, boolean applyAllSubClass) {
        doRegister(typeDeserializer, type, applyAllSubClass);
    }

    public void register(JSONTypeDeserializer typeDeserializer, Class<?> type) {
        doRegister(wrapTypeDeserializer(typeDeserializer, type), type, false);
    }

    public void register(JSONTypeDeserializer typeDeserializer, Class<?> type, boolean applyAllSubClass) {
        doRegister(wrapTypeDeserializer(typeDeserializer, type), type, applyAllSubClass);
    }

    void doRegister(JSONTypeDeserializer typeDeserializer, Class<?> type, boolean applyAllSubClass) {
        // built-in types cannot be registered
        if (BUILT_IN_DESER_SET.contains(type)) {
            return;
        }
        deserializerMap.put(type, typeDeserializer);
        if (applyAllSubClass) {
            registeredDeserializerApplyAllSubTypes.add(type);
        }
    }

    JSONTypeDeserializer checkSuperclassDeserializerRegistered(Class<?> cls) {
        for (Class<?> type : registeredDeserializerApplyAllSubTypes) {
            if (type.isAssignableFrom(cls)) {
                return deserializerMap.get(type);
            }
        }
        return null;
    }

    JSONTypeDeserializer createEnumDeserializer(Class<?> type) {
        Enum[] values = (Enum[]) type.getEnumConstants();
        Map<String, Enum> enumValues = new HashMap<String, Enum>();
        for (Enum value : values) {
            String name = value.name();
            enumValues.put(name, value);
        }
        JSONValueMatcher<Enum> enumMatcher = JSONValueMatcher.build(enumValues);
        return enumMatcher.isPlhv() ? new JSONTypeDeserializer.EnumImpl.EnumInstanceOptimizeImpl(values, enumMatcher) : new JSONTypeDeserializer.EnumImpl.EnumInstanceImpl(values, enumMatcher);
    }

    JSONTypeDeserializer getCachedTypeDeserializer(Class<?> type) {
        JSONTypeDeserializer cachedDeserializer = deserializerMap.get(type);
        if (cachedDeserializer != null) {
            return cachedDeserializer;
        }
        return JSONTypeDeserializer.GLOBAL_DESERIALIZERS.get(type);
    }

    /**
     * 联合类型反序列化
     *
     * @param parentType
     * @param possibleTypes
     * @param possibleExpression
     * @return
     */
    JSONTypeDeserializer getPossibleTypesTypeDeserializer(final Class<?> parentType, final Class<?>[] possibleTypes, final Expression possibleExpression) {
        final JSONTypeDeserializer[] possibleDeserializers = new JSONTypeDeserializer[possibleTypes.length + 1];
        for (int i = 0; i < possibleTypes.length; i++) {
            Class<?> possibleType = possibleTypes[i];
            if (possibleType == null) continue;
            if (parentType.isAssignableFrom(possibleType)) {
                JSONTypeDeserializer typeDeserializer = getTypeDeserializer(possibleType);
                if (typeDeserializer != null) {
                    possibleDeserializers[i] = typeDeserializer;
                }
            }
        }
        JSONTypeDeserializer parentDeserializer = getTypeDeserializer(parentType);
        if (parentDeserializer != null) {
            possibleDeserializers[possibleTypes.length] = parentDeserializer;
        }
        final EvaluateEnvironment evaluateEnvironment = EvaluateEnvironment.create();
        evaluateEnvironment.setAllowVariableNull(true);

        return new JSONTypeDeserializer() {
            @Override
            protected Object deserialize(CharSource charSource, char[] buf, final int fromIndex, GenericParameterizedType parameterizedType, Object defaultValue, int endToken, JSONParseContext parseContext) throws Exception {
                if (possibleExpression == null) {
                    for (int deserIndex = 0, len = possibleDeserializers.length; deserIndex < len; ++deserIndex) {
                        JSONTypeDeserializer deserializer = possibleDeserializers[deserIndex];
                        if (deserializer == null || !deserializer.checkIfSupportedStartsWith(buf[fromIndex])) continue;
                        Class<?> actualType = deserIndex < possibleTypes.length ? possibleTypes[deserIndex] : parentType;
                        try {
                            GenericParameterizedType<?> localParameterizedType = GenericParameterizedType.actualType(actualType);
                            Object result = deserializer.deserialize(charSource, buf, fromIndex, localParameterizedType, defaultValue, endToken, parseContext);
                            if (result != null) {
                                return result;
                            }
                        } catch (Exception e) {
                        }
                    }
                } else {
                    try {
                        Object context = ANY_DESER.deserialize(charSource, buf, fromIndex, parameterizedType, defaultValue, endToken, parseContext);
                        Object evalResult = possibleExpression.evaluate(context, evaluateEnvironment);
                        if (evalResult instanceof Integer) {
                            int index = (Integer) evalResult;
                            if (index < possibleTypes.length && index > -1) {
                                Class<?> actualType = possibleTypes[index];
                                GenericParameterizedType<?> localParameterizedType = GenericParameterizedType.actualType(actualType);
                                JSONTypeDeserializer deserializer = possibleDeserializers[index];
                                if (deserializer != null) {
                                    return deserializer.deserialize(charSource, buf, fromIndex, localParameterizedType, defaultValue, endToken, parseContext);
                                }
                            }
                        }
                    } catch (Exception throwable) {
                    }
                }
                ANY_DESER.skip(charSource, buf, fromIndex, endToken, parseContext);
                return null;
            }

            @Override
            protected Object deserialize(CharSource charSource, byte[] buf, final int fromIndex, GenericParameterizedType parameterizedType, Object defaultValue, int endToken, JSONParseContext parseContext) throws Exception {
                if (possibleExpression == null) {
                    for (int deserIndex = 0, len = possibleDeserializers.length; deserIndex < len; ++deserIndex) {
                        JSONTypeDeserializer deserializer = possibleDeserializers[deserIndex];
                        if (deserializer == null || !deserializer.checkIfSupportedStartsWith(buf[fromIndex])) continue;
                        Class<?> actualType = deserIndex < possibleTypes.length ? possibleTypes[deserIndex] : parentType;
                        try {
                            GenericParameterizedType<?> localParameterizedType = GenericParameterizedType.actualType(actualType);
                            Object result = deserializer.deserialize(charSource, buf, fromIndex, localParameterizedType, defaultValue, endToken, parseContext);
                            if (result != null) {
                                return result;
                            }
                        } catch (Exception e) {
                        }
                    }
                } else {
                    try {
                        Object context = ANY_DESER.deserialize(charSource, buf, fromIndex, parameterizedType, defaultValue, endToken, parseContext);
                        Object evalResult = possibleExpression.evaluate(context, evaluateEnvironment);
                        if (evalResult instanceof Integer) {
                            int index = (Integer) evalResult;
                            if (index < possibleTypes.length && index > -1) {
                                Class<?> actualType = possibleTypes[index];
                                GenericParameterizedType<?> localParameterizedType = GenericParameterizedType.actualType(actualType);
                                JSONTypeDeserializer deserializer = possibleDeserializers[index];
                                if (deserializer != null) {
                                    return deserializer.deserialize(charSource, buf, fromIndex, localParameterizedType, defaultValue, endToken, parseContext);
                                }
                            }
                        }
                    } catch (Exception throwable) {
                    }
                }
                ANY_DESER.skip(charSource, buf, fromIndex, endToken, parseContext);
                return null;
            }
        };
    }

    void register(JSONTypeSerializer typeSerializer, Class<?> type) {
        register(typeSerializer, type, false);
    }

    void register(JSONTypeSerializer typeSerializer, Class<?> type, boolean applyAllSubClass) {
        // Built-in instances are not allowed to be overridden
        if (BUILT_IN_SER_SET.contains(type)) {
            return;
        }
        serializerMap.put(type, typeSerializer);
        if (applyAllSubClass) {
            registeredSerializerApplyAllSubTypes.add(type);
        }
    }

    JSONTypeSerializer checkSuperclassSerializerRegistered(Class<?> cls) {
        for (Class<?> type : registeredSerializerApplyAllSubTypes) {
            if (type.isAssignableFrom(cls)) {
                return serializerMap.get(type);
            }
        }
        return null;
    }

    // Quick search based on usage frequency, significant effect when serializing map objects
    JSONTypeSerializer getTypeSerializer(final Class<?> cls) {
        JSONTypeSerializer serializer = serializerMap.get(cls);
        if (serializer != null) {
            return serializer;
        }

        // GLOBAL
        serializer = JSONTypeSerializer.GLOBAL_SERIALIZERS.get(cls);
        if (serializer != null) {
            serializerMap.put(cls, serializer);
            return serializer;
        }

        // secondary search resolve the initialization dead loop dependency
        serializer = temporarySerializerMap.get(cls);
        if (serializer != null) {
            return serializer.ensureInitialized();
        }

        // registered parent class
        if ((serializer = checkSuperclassSerializerRegistered(cls)) != null) {
            serializerMap.put(cls, serializer);
            return serializer;
        }

        synchronized (cls) {
            serializer = serializerMap.get(cls);
            if (serializer != null) {
                return serializer;
            }
            ReflectConsts.ClassCategory classCategory = ReflectConsts.getClassCategory(cls);
            if (classCategory == ReflectConsts.ClassCategory.ObjectCategory) {
                ClassStrucWrap classStrucWrap = ClassStrucWrap.get(cls);
                if (classStrucWrap.isTemporal()) {
                    serializer = JSONTemporalSerializer.getTemporalSerializerInstance(classStrucWrap, null);
                } else if (classStrucWrap.isSubEnum()) {
                    serializer = JSONTypeSerializer.ENUM;
                } else {
                    JSONPojoStructure pojoStructure = getPojoStruc(cls);
                    if (enableJIT && pojoStructure.isSupportedJIT()) {
                        try {
                            Class<?> serializerClass = JDKCompiler.compileJavaSource(JSONPojoSerializer.generateRuntimeJavaCodeSource(pojoStructure));
                            Constructor constructor = serializerClass.getDeclaredConstructor(new Class[]{JSONPojoStructure.class});
                            UnsafeHelper.setAccessible(constructor);
                            serializer = (JSONPojoSerializer) constructor.newInstance(pojoStructure);
                        } catch (Throwable throwable) {
                            serializer = new JSONTypeSerializer.ObjectImpl.ObjectWrapperImpl(cls, pojoStructure);
                        }
                    } else {
                        serializer = new JSONTypeSerializer.ObjectImpl.ObjectWrapperImpl(cls, pojoStructure);
                    }
                }
            } else {
                serializer = TYPE_SERIALIZERS[classCategory.ordinal()];
            }
            //  put to temporary_map
            temporarySerializerMap.put(cls, serializer);
            serializer.ensureInitialized();

            // put to map
            serializerMap.put(cls, serializer);
            // remove if ensureInitialized
            temporarySerializerMap.remove(cls);
            return serializer;
        }
    }

    JSONTypeSerializer getMapValueSerializer(Class<?> cls) {
        int classHashCode = cls.getName().hashCode();
        switch (classHashCode) {
            case EnvUtils.STRING_HV: {
                if (cls == String.class) {
                    return STRING_SER;
                }
                break;
            }
            case EnvUtils.INT_HV:
            case EnvUtils.INTEGER_HV:
            case EnvUtils.LONG_PRI_HV:
            case EnvUtils.LONG_HV:
                if (Number.class.isAssignableFrom(cls)) {
                    return JSONTypeSerializer.NUMBER_LONG;
                }
                break;
            case EnvUtils.HASHMAP_HV:
            case EnvUtils.LINK_HASHMAP_HV:
                if (Map.class.isAssignableFrom(cls)) {
                    return MAP_SER;
                }
                break;
            case EnvUtils.ARRAY_LIST_HV:
            case EnvUtils.HASH_SET_HV:
                if (Collection.class.isAssignableFrom(cls)) {
                    return COLLECTION_SER;
                }
                break;
        }
        return getTypeSerializer(cls);
    }

    /**
     * <p> 序列化大部分场景可以使用分类序列化器即可比如Object类型,枚举类型，字符串类型；
     * <p> 日期时间类如果存在pattern等配置需要单独构建,不能缓存；
     * <p> 部分number类型比如int和long可以使用缓存好的序列化器提升一定性能；
     *
     * @param classCategory
     * @param type
     * @param propertyDefinition
     * @return
     */
    JSONTypeSerializer getFieldTypeSerializer(ReflectConsts.ClassCategory classCategory, Class<?> type, JSONPropertyDefinition propertyDefinition) {
        // find by classCategory
        switch (classCategory) {
            case EnumCategory: {
                return getEnumSerializer(type);
            }
            case NumberCategory: {
                return getTypeSerializer(type);
            }
            case ArrayCategory: {
                if (Object[].class.isAssignableFrom(type)) {
                    return type == String[].class ? JSONTypeSerializer.ARRAY_STRING : ARRAY_SER;
                } else {
                    return getTypeSerializer(type);
                }
            }
            case ObjectCategory: {
                ClassStrucWrap classStrucWrap = ClassStrucWrap.get(type);
                if (classStrucWrap.isTemporal()) {
                    return JSONTemporalSerializer.getTemporalSerializerInstance(classStrucWrap, propertyDefinition);
                } else {
                    if (propertyDefinition != null && propertyDefinition.unfixedType()) {
                        // auto type
                        return new JSONTypeSerializer.ObjectImpl.ObjectWithTypeImpl(this);
                    }
                    return getTypeSerializer(type);
                }
            }
            default: {
                // searching for instances in the cache
                JSONTypeSerializer typeSerializer = getCachedTypeSerializer(type);
                if (typeSerializer != null) {
                    return typeSerializer;
                }
            }
        }

        // others by classCategory
        return getTypeSerializer(classCategory, propertyDefinition);
    }

    JSONTypeSerializer getTypeSerializer(ReflectConsts.ClassCategory classCategory, JSONPropertyDefinition propertyDefinition) {
        int ordinal = classCategory.ordinal();
        if (classCategory == ReflectConsts.ClassCategory.DateCategory) {
            if (propertyDefinition != null) {
                if (propertyDefinition.asTimestamp()) {
                    return JSONTypeSerializer.DATE_AS_TIME_SERIALIZER;
                }
                String pattern = propertyDefinition.pattern();
                if (!pattern.isEmpty()) {
                    String timeZoneId = propertyDefinition.timezone();
                    return new JSONTypeSerializer.DatePatternImpl(DateFormatter.of(pattern), JSONGeneral.getTimeZone(timeZoneId));
                }
            }
        }
        if (classCategory == ReflectConsts.ClassCategory.NonInstance) {
            // write classname
            return new JSONTypeSerializer.ObjectImpl.ObjectWithTypeImpl(this);
        }
        return TYPE_SERIALIZERS[ordinal];
    }

    JSONTypeSerializer getCachedTypeSerializer(Class<?> type) {
        JSONTypeSerializer serializer = serializerMap.get(type);
        if (serializer != null) {
            return serializer;
        }
        return JSONTypeSerializer.GLOBAL_SERIALIZERS.get(type);
    }

    JSONTypeSerializer createCollectionSerializer(Class<?> valueClass) {
        return new JSONTypeSerializer.CollectionImpl.CollectionFinalTypeImpl(getTypeSerializer(valueClass));
    }

    JSONTypeSerializer getEnumSerializer(Class<?> enumClass) {
        JSONTypeSerializer enumSerializer = serializerMap.get(enumClass);
        if (enumSerializer != null) {
            return enumSerializer;
        }
        Enum[] values = (Enum[]) enumClass.getEnumConstants();
        char[][] enumNames = new char[values.length][];
        for (Enum value : values) {
            String enumName = value.name();
            char[] chars = new char[enumName.length() + 2];
            chars[0] = chars[chars.length - 1] = '"';
            enumName.getChars(0, enumName.length(), chars, 1);
            enumNames[value.ordinal()] = chars;
        }
        enumSerializer = new JSONTypeSerializer.EnumImpl.EnumInstanceImpl(enumNames);
        serializerMap.put(enumClass, enumSerializer);
        return enumSerializer;
    }

    <T> JSONTypeSerializer buildSerializer(final JSONTypeMapper<T> mapper) {
        return new JSONTypeSerializer() {
            @Override
            protected void serialize(Object value, JSONWriter writer, JSONConfig jsonConfig, int indent) throws Exception {
                JSONValue<?> result = mapper.writeAs((T) value, jsonConfig);
                Object baseValue;
                if (result == null || (baseValue = result.value) == null) {
                    writer.writeNull();
                } else {
                    JSONTypeSerializer typeSerializer = getTypeSerializer(baseValue.getClass());
                    typeSerializer.serialize(baseValue, writer, jsonConfig, indent);
                }
            }
        };
    }

    <E> JSONTypeDeserializer buildDeserializer(final JSONTypeMapper<E> mapper) {
        return new JSONTypeDeserializer() {
            @Override
            protected Object deserialize(CharSource charSource, char[] buf, int fromIndex, GenericParameterizedType<?> parameterizedType, Object defaultValue, int endToken, JSONParseContext jsonParseContext) throws Exception {
                Object value = ANY_DESER.deserialize(charSource, buf, fromIndex, parameterizedType, defaultValue, endToken, jsonParseContext);
                Object result = mapper.readOf(value);
                if (result == null || parameterizedType.getActualType().isInstance(result)) {
                    return result;
                }
                throw new JSONException("Syntax error, at pos " + fromIndex + ", context text by '" + JSONGeneral.createErrorContextText(buf, fromIndex) + result.getClass().getName() + " cannot be cast to " + parameterizedType.getActualType().getName());
            }

            @Override
            protected Object deserialize(CharSource charSource, byte[] bytes, int fromIndex, GenericParameterizedType<?> parameterizedType, Object defaultValue, int endToken, JSONParseContext jsonParseContext) throws Exception {
                Object value = ANY_DESER.deserialize(charSource, bytes, fromIndex, parameterizedType, defaultValue, endToken, jsonParseContext);
                Object result = mapper.readOf(value);
                if (result == null || parameterizedType.getActualType().isInstance(result)) {
                    return result;
                }
                throw new JSONException("Syntax error, at pos " + fromIndex + ", context text by '" + JSONGeneral.createErrorContextText(bytes, fromIndex) + result.getClass().getName() + " cannot be cast to " + parameterizedType.getActualType().getName());
            }
        };
    }

    /**
     * 包装一层反序列化，确保类型转化正确(防止内存污染导致意外)
     *
     * @param original
     * @param targetType
     * @return
     */
    final static JSONTypeDeserializer wrapTypeDeserializer(final JSONTypeDeserializer original, final Class<?> targetType) {
        return new JSONTypeDeserializer() {
            @Override
            protected Object deserialize(CharSource charSource, char[] buf, int fromIndex, GenericParameterizedType<?> parameterizedType, Object defaultValue, int endToken, JSONParseContext jsonParseContext) throws Exception {
                Object result = original.deserialize(charSource, buf, fromIndex, parameterizedType, defaultValue, endToken, jsonParseContext);
                if (result == null || targetType.isInstance(result)) {
                    return result;
                }
                throw new JSONException("Syntax error, at pos " + fromIndex + ", context text by '" + JSONGeneral.createErrorContextText(buf, fromIndex) + "...', " + result.getClass().getName() + " cannot be cast to " + targetType.getName());
            }

            @Override
            protected Object deserialize(CharSource charSource, byte[] bytes, int fromIndex, GenericParameterizedType<?> parameterizedType, Object defaultValue, int endToken, JSONParseContext jsonParseContext) throws Exception {
                Object result = original.deserialize(charSource, bytes, fromIndex, parameterizedType, defaultValue, endToken, jsonParseContext);
                if (result == null || targetType.isInstance(result)) {
                    return result;
                }
                throw new JSONException("Syntax error, at pos " + fromIndex + ", context text by '" + JSONGeneral.createErrorContextText(bytes, fromIndex) + "...', " + result.getClass().getName() + " cannot be cast to " + targetType.getName());
            }
        };
    }

    public void clearAll() {
        pojoStrucs.clear();
        temporaryDeserializerMap.clear();
        registeredDeserializerApplyAllSubTypes.clear();
        temporarySerializerMap.clear();
        registeredSerializerApplyAllSubTypes.clear();

        deserializerMap.clear();
        serializerMap.clear();
    }

    public void reset() {
        synchronized (this) {
            clearAll();
            init();
        }
    }

    public void setStringDeserializer(JSONTypeDeserializer.CharSequenceImpl stringDeser) {
        this.STRING_DESER = stringDeser;
        parser.setStringDeserializer(stringDeser);
        putTypeDeserializer(stringDeser, String.class, CharSequence.class);
        deserializerMap.put(String[].class, new JSONTypeDeserializer.ArrayImpl.StringArrayImpl(stringDeser));
    }

    public void disableJIT() {
        this.enableJIT = false;
    }

    public void setNoneStringMode(boolean noneStringMode) {
        if (noneStringMode == this.noneStringMode) return;
        this.noneStringMode = noneStringMode;
        if (noneStringMode) {
            // deserializer
            setStringDeserializer(JSONTypeDeserializer.STRING_SKIPPER);
            deserializerMap.put(String[].class, JSONTypeDeserializer.ANY_SKIPPER);
            // serializer
            putTypeSerializer(STRING_SER = JSONTypeSerializer.TO_NULL, String.class, CharSequence.class, String[].class);
        } else {
            // deserializer
            setStringDeserializer(JSONTypeDeserializer.CHAR_SEQUENCE_STRING);
            // serializer
            putTypeSerializer(STRING_SER = JSONTypeSerializer.CHAR_SEQUENCE_STRING, String.class, CharSequence.class);
            putTypeSerializer(JSONTypeSerializer.ARRAY_STRING, String[].class);
        }
    }

    public boolean isNoneStringMode() {
        return noneStringMode;
    }

    public void updatePojoFieldAliasName(Class<?> pojoClass, String fieldName, String aliasName) {
        Map<String, JSONPropertyDefinition> properties = new HashMap<String, JSONPropertyDefinition>();
        properties.put(fieldName, new JSONPropertyDefinition(aliasName));
        updatePojoProperties(pojoClass, properties);
    }

    public void updatePojoFieldAliasNames(Class<?> pojoClass, Map<String, String> aliasNames) {
        if (aliasNames == null || aliasNames.isEmpty()) return;
        Map<String, JSONPropertyDefinition> properties = new HashMap<String, JSONPropertyDefinition>();
        for (Map.Entry<String, String> entry : aliasNames.entrySet()) {
            properties.put(entry.getKey(), new JSONPropertyDefinition(entry.getValue()));
        }
        updatePojoProperties(pojoClass, properties);
    }

    public void updatePojoProperties(Class<?> pojoClass, Map<String, JSONPropertyDefinition> properties) {
        JSONPojoStructure pojoStruc = getPojoStruc(pojoClass);
        if (pojoStruc != null) {
            Map<String, JSONPropertyDefinition> targetProperties = new HashMap<String, JSONPropertyDefinition>(pojoStruc.propertyDefinitions);
            targetProperties.putAll(properties);
            JSONPojoStructure newPojoStruc = new JSONPojoStructure(this, pojoStruc.classStrucWrap, targetProperties);
            pojoStrucs.put(pojoClass, newPojoStruc);
            // 清除缓存（暂时是否清除关联缓存），使用上尽量初始化后，避免缓存污染
            deserializerMap.remove(pojoClass);
        }
    }
}
