package io.github.wycst.wast.common.reflect;

import java.lang.reflect.*;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 解决多层泛型类型解析
 *
 * @Author wangyunchao
 * @Date 2021/12/30 10:34
 */
public final class GenericParameterizedType<T> {

    private GenericParameterizedType() {
    }
    // cache
    private static final Map<Class, GenericParameterizedType> GENERIC_PARAMETERIZED_TYPE_MAP = new ConcurrentHashMap<Class, GenericParameterizedType>();

    /**
     * AnyType类型
     */
    public static final GenericParameterizedType AnyType = GenericParameterizedType.actualType(Object.class);

    /**
     * 字符串类型
     */
    public static final GenericParameterizedType StringType = GenericParameterizedType.actualType(String.class);
    /**
     * 默认Map类型
     */
    public static final GenericParameterizedType DefaultMap = GenericParameterizedType.actualType(LinkedHashMap.class);
//    /**
//     * 默认Collection类型
//     */
//    public static final GenericParameterizedType DefaultCollection = GenericParameterizedType.collectionType(ArrayList.class, Object.class);

    /***
     * int类型
     */
    public static final GenericParameterizedType<Integer> IntType = GenericParameterizedType.actualType(int.class);

    /***
     * long类型
     */
    public static final GenericParameterizedType<Long> LongType = GenericParameterizedType.actualType(long.class);

    /***
     * double类型
     */
    public static final GenericParameterizedType<Double> DoubleType = GenericParameterizedType.actualType(double.class);

    /***
     * BigDecimalType类型
     */
    public static final GenericParameterizedType<BigDecimal> BigDecimalType = GenericParameterizedType.actualType(BigDecimal.class);

    /**
     * 是否数组类型，数组无对应的class
     */
    boolean array;

    /**
     * 实际类型： Map类型(key和value两个泛型)/Collection类型（value一个泛型）/其他实体类（0-n个）
     */
    Class<?> actualType;

    // ClassCategory
    ReflectConsts.ClassCategory actualClassCategory;

    /**
     * map类型的泛型key类型
     */
    Class<?> mapKeyClass;

    /**
     * map类的泛型value类型或者Collection类型的元素泛型
     */
    GenericParameterizedType valueType;

    /**
     * 是否泛型
     */
    boolean generic;

    /**
     * 其他实体类泛型列表(实体类暂时只支持单泛型且不能嵌套)
     */
    Class<?> genericClass;

    /**
     * 泛型映射
     */
    Map<String, Class<?>> genericClassMap;

    /**
     * 是否伪泛型
     */
    private boolean camouflage;

    /**
     * 伪泛型名称，可以通过上级泛型结构的genericClassMap中解析出实际类型
     */
    String genericName;

    /**
     * 根据实际类型构建泛型结构对象
     *
     * @param actualType
     * @return
     */
    public static <T> GenericParameterizedType<T> actualType(Class<T> actualType) {
        if (actualType == null) return AnyType;
        GenericParameterizedType genericParameterizedType = GENERIC_PARAMETERIZED_TYPE_MAP.get(actualType);
        if (genericParameterizedType == null) {
            genericParameterizedType = new GenericParameterizedType();
            genericParameterizedType.setActualType(actualType);
            genericParameterizedType.actualClassCategory = ReflectConsts.getClassCategory(actualType);
            if(actualType.isArray()) {
                genericParameterizedType.valueType = actualType(actualType.getComponentType());
            }
            GENERIC_PARAMETERIZED_TYPE_MAP.put(actualType, genericParameterizedType);
        }
        return genericParameterizedType;
    }

    /**
     * 通过new构建,场景需要隔离时使用
     *
     * @param actualType
     * @return
     */
    static GenericParameterizedType createInternal(Class actualType) {
        if(actualType == Object.class) {
            return AnyType;
        }
        if(actualType == String.class) {
            return StringType;
        }
        if(actualType == Integer.class || actualType == int.class) {
            return IntType;
        }
        if(actualType == Long.class || actualType == long.class) {
            return LongType;
        }
        GenericParameterizedType genericParameterizedType = new GenericParameterizedType();
        genericParameterizedType.setActualType(actualType);
        genericParameterizedType.actualClassCategory = ReflectConsts.getClassCategory(actualType);
        return genericParameterizedType;
    }

    public static GenericParameterizedType mapOf(Class<? extends Map> mapClass) {
        Type type = mapClass.getGenericSuperclass();
        if (type instanceof ParameterizedType) {
            ParameterizedType parameterizedType = (ParameterizedType) type;
            Type[] actualTypeArguments = parameterizedType.getActualTypeArguments();
            if (actualTypeArguments != null && actualTypeArguments.length == 2) {
                Type arg0 = actualTypeArguments[0], arg1 = actualTypeArguments[1];
                if (arg0 instanceof Class) {
                    return GenericParameterizedType.mapType(mapClass, (Class<?>) arg0, GenericParameterizedType.of(arg1));
                }
            }
        }
        return actualType(mapClass);
    }

    public static GenericParameterizedType collectionOf(Class<? extends Collection> collectionClass) {
        Type type = collectionClass.getGenericSuperclass();
        if (type instanceof ParameterizedType) {
            ParameterizedType parameterizedType = (ParameterizedType) type;
            Type[] actualTypeArguments = parameterizedType.getActualTypeArguments();
            if (actualTypeArguments != null && actualTypeArguments.length == 1) {
                return collectionType(collectionClass, GenericParameterizedType.of(actualTypeArguments[0]));
            }
        }
        return actualType(collectionClass);
    }

//    static Type getEntityGenericParameterizedType(Class entityClass) {
//        Type type = entityClass.getGenericSuperclass();
//        if (type instanceof ParameterizedType) {
//            ParameterizedType parameterizedType = (ParameterizedType) type;
//            Type[] actualTypeArguments = parameterizedType.getActualTypeArguments();
//            if (actualTypeArguments != null && actualTypeArguments.length == 1) {
//                return actualTypeArguments[0];
//            }
//        }
//        return null;
//    }

    public boolean isAnyType() {
        return this == AnyType;
    }

    private <T> void setActualType(Class<T> actualType) {
        this.actualType = actualType;
    }

    /**
     * 构建map类型的泛型结构对象, value为简单类型（无泛型）
     *
     * @param mapClass
     * @param mapKeyClass
     * @param valueActualType
     * @return
     */
    public static GenericParameterizedType mapType(Class<? extends Map> mapClass, Class<?> mapKeyClass, Class<?> valueActualType) {
        GenericParameterizedType parameterizedType = new GenericParameterizedType();
        parameterizedType.generic = true;
        parameterizedType.setActualType(mapClass);

        parameterizedType.mapKeyClass = mapKeyClass;

        GenericParameterizedType valueType = new GenericParameterizedType();
        valueType.setActualType(valueActualType);

        parameterizedType.valueType = valueType;
        return parameterizedType;
    }

    /**
     * 构建map类型的泛型结构, value也可能存在泛型
     *
     * @param mapClass
     * @param mapKeyClass
     * @param valueType
     * @return
     */
    public static GenericParameterizedType mapType(Class<? extends Map> mapClass, Class<?> mapKeyClass, GenericParameterizedType valueType) {
        GenericParameterizedType parameterizedType = new GenericParameterizedType();
        parameterizedType.generic = true;
        parameterizedType.setActualType(mapClass);
        parameterizedType.mapKeyClass = mapKeyClass;

        parameterizedType.valueType = valueType;
        return parameterizedType;
    }

    /**
     * 构建数组类型的泛型结构，元素为简单类型
     *
     * @param componentType 数组元素泛型
     * @return
     */
    public static GenericParameterizedType arrayType(Class<?> componentType) {
        GenericParameterizedType parameterizedType = new GenericParameterizedType();
        parameterizedType.actualType = Array.newInstance(componentType, 0).getClass();
        parameterizedType.generic = true;
        parameterizedType.array = true;

        GenericParameterizedType valueType = actualType(componentType);
        parameterizedType.valueType = valueType;
        return parameterizedType;
    }

//    /**
//     * 构建数组类型的泛型结构，元素为可嵌套类型
//     *
//     * @param valueType 数组元素泛型结构
//     * @return
//     */
//    public static GenericParameterizedType arrayType(GenericParameterizedType valueType) {
//        GenericParameterizedType parameterizedType = new GenericParameterizedType();
//        parameterizedType.generic = true;
//        parameterizedType.array = true;
//        parameterizedType.valueType = valueType;
//        return parameterizedType;
//    }

    /**
     * 构建Collection类型的泛型结构，元素为简单类型
     *
     * @param collectionClass
     * @param valueActualType
     * @return
     */
    public static <E> GenericParameterizedType<E> collectionType(Class<E> collectionClass, Class<?> valueActualType) {

        GenericParameterizedType parameterizedType = new GenericParameterizedType();
        parameterizedType.generic = true;
        parameterizedType.setActualType(collectionClass == null ? ArrayList.class : collectionClass);

        GenericParameterizedType valueType = actualType(valueActualType);
        parameterizedType.valueType = valueType;
        return parameterizedType;
    }

    /**
     * 构建Collection类型的泛型结构，元素也可能存在泛型
     *
     * @param collectionClass
     * @param valueType
     * @return
     */
    public static <E> GenericParameterizedType<E> collectionType(Class<E> collectionClass, GenericParameterizedType valueType) {
        GenericParameterizedType parameterizedType = new GenericParameterizedType();
        parameterizedType.generic = true;
        parameterizedType.setActualType(collectionClass);
        parameterizedType.valueType = valueType;
        return parameterizedType;
    }

    /**
     * 构建普通实体类型的泛型结构
     * (ps:单泛型模式)
     *
     * @param entityClass
     * @param genericClass
     * @return
     */
    public static <E> GenericParameterizedType<E> entityType(Class<E> entityClass, Class<?> genericClass) {
        GenericParameterizedType<E> parameterizedType = new GenericParameterizedType<E>();
        parameterizedType.setActualType(entityClass);
        parameterizedType.generic = true;
        parameterizedType.genericClass = genericClass;
        // If it is an array type, it needs to be handled separately ?
        return parameterizedType;
    }

    /**
     * 构建普通实体类型的泛型结构
     * 支持n个泛型，通过名称进行映射
     *
     * @param entityClass
     * @param genericClassMap
     * @return
     */
    public static <E> GenericParameterizedType<E> entityType(Class<E> entityClass, Map<String, Class<?>> genericClassMap) {
        GenericParameterizedType parameterizedType = new GenericParameterizedType();
        parameterizedType.setActualType(entityClass);
        parameterizedType.genericClassMap = genericClassMap;
        return parameterizedType;
    }

    /**
     * 递归解析Collection的泛型结构
     *
     * @param parameterType
     * @param genericType
     * @return
     */
    static GenericParameterizedType genericCollectionType(Class<?> parameterType, Type genericType) {
        GenericParameterizedType genericParameterizedType = new GenericParameterizedType();
        genericParameterizedType.setActualType(parameterType);
        genericParameterizedType.generic = true;
        parseValueType(genericParameterizedType, genericType);
        return genericParameterizedType;
    }

    /**
     * 递归解析数组的泛型结构
     *
     * @param genericComponentType
     * @return
     */
    static GenericParameterizedType genericArrayType(Type genericComponentType) {
        GenericParameterizedType genericParameterizedType = new GenericParameterizedType();
        genericParameterizedType.array = true;
        genericParameterizedType.generic = true;
        parseValueType(genericParameterizedType, genericComponentType);
        return genericParameterizedType;
    }

    /**
     * @param parameterType
     * @param key           <p> map的key值不支持泛型
     * @param value
     * @return
     */
    static GenericParameterizedType genericMapType(Class<?> parameterType, Type key, Type value) {
        GenericParameterizedType genericParameterizedType = new GenericParameterizedType();
        genericParameterizedType.setActualType(parameterType);
        genericParameterizedType.generic = true;
        genericParameterizedType.mapKeyClass = !(key instanceof Class<?>) ? Object.class : (Class<?>) key;
        parseValueType(genericParameterizedType, value);
        return genericParameterizedType;
    }

    /**
     * 通过类上的伪泛型构建泛型结构
     *
     * @param parameterType
     * @param genericName   伪泛型名称
     * @return
     */
    static GenericParameterizedType genericEntityType(Class<?> parameterType, String genericName) {
        GenericParameterizedType genericParameterizedType = new GenericParameterizedType();
        genericParameterizedType.setActualType(parameterType);
        genericParameterizedType.genericName = genericName;
        genericParameterizedType.camouflage = true;
        return genericParameterizedType;
    }

    private static void parseValueType(GenericParameterizedType genericParameterizedType, Type genericType) {
        if (genericType instanceof Class) {
            // 实体类型
            Class<?> entityClass = (Class<?>) genericType;
            genericParameterizedType.valueType = actualType(entityClass);
        } else if (genericType instanceof GenericArrayType) {
            // 数组类型
            GenericArrayType genericArrayType = (GenericArrayType) genericType;
            Type genericComponentType = genericArrayType.getGenericComponentType();

            // jdk6 char[]等基础数据的genericType依然是Type(数组加componentType)
            // jdk7+ char[]等基础数据的enericType已经是一个数组类型(char[].class)
            ReflectConsts.PrimitiveType primitiveType;
            // jdk1.6 兼容处理
            if (genericComponentType instanceof Class && (primitiveType = ReflectConsts.PrimitiveType.typeOf((Class) genericComponentType)) != null) {
                genericParameterizedType.valueType = actualType(primitiveType.getGenericArrayType());
            } else {
                genericParameterizedType.valueType = genericArrayType(genericComponentType);
            }
        } else if (genericType instanceof ParameterizedType) {
            // 两级泛型时如： List<Map<String,Object>>
            ParameterizedType ptType = (ParameterizedType) genericType;
            Class<?> genericClazz = (Class<?>) ptType.getRawType();
            // 设置二级泛型类型
            Type[] types = ptType.getActualTypeArguments();
            if (Map.class.isAssignableFrom(genericClazz)) {
                if (types.length == 2) {
                    genericParameterizedType.valueType = genericMapType(genericClazz, types[0], types[1]);
                } else {
                    genericParameterizedType.valueType = genericMapType(genericClazz, Object.class, Object.class);
                }
            } else if (Collection.class.isAssignableFrom(genericClazz)) {
                if (types.length == 1) {
                    genericParameterizedType.valueType = genericCollectionType(genericClazz, types[0]);
                }
            } else if (genericClazz.isArray()) {
                Class<?> componentType = genericClazz.getComponentType();
                genericParameterizedType.valueType = entityType(genericClazz, componentType);
            } else {
                // 实体类上的泛型，仅仅支持单泛型,且不能嵌套，否则跳过
                if (types.length == 1) {
                    try {
                        Class<?> cls = (Class<?>) types[0];
                        genericParameterizedType.valueType = entityType(genericClazz, cls);
                    } catch (Throwable throwable) {
                    }
                }
            }
        } else if (genericType instanceof WildcardType) {
            // ?
            genericParameterizedType.valueType = actualType(Object.class);
        }
    }

    /**
     * 接口或者抽象类使用属性默认值时，可以替换实例类型，并保留其泛型结构
     *
     * @param actualType
     * @return
     */
    public GenericParameterizedType<?> copyAndReplaceActualType(Class<?> actualType) {
        GenericParameterizedType genericParameterizedType = new GenericParameterizedType();
        genericParameterizedType.setActualType(actualType);
        genericParameterizedType.valueType = this.valueType;
        genericParameterizedType.mapKeyClass = this.mapKeyClass;
        genericParameterizedType.genericClass = this.genericClass;
        return genericParameterizedType;
    }

    public Class<?> getActualType() {
        return actualType;
    }

    public Class<?> getMapKeyClass() {
        return mapKeyClass;
    }

    public GenericParameterizedType getValueType() {
        return valueType;
    }

    public Class<?> getGenericClass() {
        return genericClass;
    }

    public Class<?> getGenericClass(String genericName) {
        if (genericClass != null) {
            return genericClass;
        }
        if (genericClassMap != null) {
            return genericClassMap.get(genericName);
        }
        return null;
    }

    public boolean isGeneric() {
        return generic;
    }

    public boolean isCamouflage() {
        return camouflage;
    }

    public String getGenericName() {
        return genericName;
    }

    public ReflectConsts.ClassCategory getActualClassCategory() {
        if (actualClassCategory != null) {
            return actualClassCategory;
        }
        return actualClassCategory = ReflectConsts.getClassCategory(actualType);
    }

    public boolean isAssignableFrom(Class<?> childClass) {
        return actualType == childClass || actualType.isAssignableFrom(childClass);
    }

    /**
     * 通过Type构建GenericParameterizedType实例
     *
     * @param type
     * @return
     */
    public static GenericParameterizedType of(Type type) {
        if (type instanceof Class<?>) {
            Class typeClass = (Class<?>) type;
            if (Map.class.isAssignableFrom(typeClass)) {
                return mapOf(typeClass);
            } else if (Collection.class.isAssignableFrom(typeClass)) {
                return collectionOf(typeClass);
            }
            return actualType(typeClass);
        }
        try {
            if (type instanceof ParameterizedType) {
                ParameterizedType pt = (ParameterizedType) type;
                Type[] actualTypeArguments = pt.getActualTypeArguments();
                Type rawType = pt.getRawType();
                if (rawType instanceof Class<?>) {
                    Class rawClass = (Class<?>) rawType;
                    if (Collection.class.isAssignableFrom(rawClass)) {
                        return collectionType(rawClass, of(actualTypeArguments[0]));
                    } else if (Map.class.isAssignableFrom(rawClass)) {
                        Type key = actualTypeArguments[0];
                        return mapType(rawClass, key instanceof Class<?> ? (Class<?>) key : Object.class, of(actualTypeArguments[1]));
                    } else {
                        // maybe an entity<T>
                        return genericEntityType(rawClass, actualTypeArguments[0].getTypeName());
                    }
                }
            }
            if (type instanceof GenericArrayType) {
                GenericArrayType genericArrayType = (GenericArrayType) type;
                Type genericComponentType = genericArrayType.getGenericComponentType();
                return GenericParameterizedType.genericArrayType(genericComponentType);
            }
            if (type instanceof TypeVariable) {
                TypeVariable typeVariable = (TypeVariable) type;
                Type[] bounds = typeVariable.getBounds();
                if (bounds.length > 0) {
                    return of(bounds[bounds.length - 1]);
                }
            }
            if (type instanceof WildcardType) {
                WildcardType wildcardType = (WildcardType) type;
                Type[] upperBounds = wildcardType.getUpperBounds();
                if (upperBounds.length > 0) {
                    return of(upperBounds[upperBounds.length - 1]);
                }
                return AnyType;
            }
            return null;
        } catch (Throwable throwable) {
            return null;
        }
    }
}
