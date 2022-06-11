package io.github.wycst.wast.common.reflect;

import java.lang.reflect.GenericArrayType;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.WildcardType;
import java.util.Collection;
import java.util.Map;

/**
 * 解决多层泛型类型解析
 *
 * @Author wangyunchao
 * @Date 2021/12/30 10:34
 */
public final class GenericParameterizedType<T> {

    GenericParameterizedType() {
    }

    /**
     * 是否数组类型，数组无对应的class
     */
    boolean array;

    /**
     * 实际类型： Map类型(key和value两个泛型)/Collection类型（value一个泛型）/其他实体类（0-n个）
     */
    Class<?> actualType;

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
     * 所属宿主setter
     */
    SetterInfo ownerInfo;

    /**
     * 根据实际类型构建泛型结构对象
     *
     * @param actualType
     * @return
     */
    public static <T> GenericParameterizedType<T> actualType(Class<T> actualType) {
        GenericParameterizedType genericParameterizedType = new GenericParameterizedType();
        genericParameterizedType.actualType = actualType;
        return genericParameterizedType;
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
        parameterizedType.actualType = mapClass;
        parameterizedType.mapKeyClass = mapKeyClass;

        GenericParameterizedType valueType = new GenericParameterizedType();
        valueType.actualType = valueActualType;
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
        parameterizedType.actualType = mapClass;
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
        parameterizedType.generic = true;
        parameterizedType.array = true;

        GenericParameterizedType valueType = new GenericParameterizedType();
        valueType.actualType = componentType;
        parameterizedType.valueType = valueType;
        return parameterizedType;
    }

    /**
     * 构建数组类型的泛型结构，元素为可嵌套类型
     *
     * @param valueType 数组元素泛型结构
     * @return
     */
    public static GenericParameterizedType arrayType(GenericParameterizedType valueType) {
        GenericParameterizedType parameterizedType = new GenericParameterizedType();
        parameterizedType.generic = true;
        parameterizedType.array = true;
        parameterizedType.valueType = valueType;
        return parameterizedType;
    }

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
        parameterizedType.actualType = collectionClass;

        GenericParameterizedType valueType = new GenericParameterizedType();
        valueType.actualType = valueActualType;
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
        parameterizedType.actualType = collectionClass;
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
        parameterizedType.actualType = entityClass;
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
        parameterizedType.actualType = entityClass;
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
        genericParameterizedType.actualType = parameterType;
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
        genericParameterizedType.actualType = parameterType;
        genericParameterizedType.generic = true;
        genericParameterizedType.mapKeyClass = key instanceof WildcardType ? Object.class : (Class<?>) key;

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
        genericParameterizedType.actualType = parameterType;
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
            genericParameterizedType.valueType = genericArrayType(genericComponentType);
        } else if (genericType instanceof ParameterizedType) {
            // 两级泛型时如： List<Map<String,Object>>
            ParameterizedType ptType = (ParameterizedType) genericType;
            Class<?> genericClazz = (Class<?>) ptType.getRawType();
            // 设置二级泛型类型
            Type[] types = ptType.getActualTypeArguments();
            if (Map.class.isAssignableFrom(genericClazz)) {
                if (types.length == 2) {
                    genericParameterizedType.valueType = genericMapType(genericClazz, types[0], types[1]);
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
    public GenericParameterizedType copyAndReplaceActualType(Class<?> actualType) {
        GenericParameterizedType genericParameterizedType = new GenericParameterizedType();
        genericParameterizedType.actualType = actualType;
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

    public String getDatePattern() {
        return ownerInfo == null ? null : ownerInfo.getPattern();
    }

    public String getDateTimezone() {
        return ownerInfo == null ? null : ownerInfo.getTimezone();
    }

    public int getParamClassType() {
        if (ownerInfo != null) {
            return ownerInfo.getParamClassType();
        }
        return ReflectConsts.getParamClassType(actualType);
    }
//    public int getParamClassType() {
//        return ownerInfo == null ? 0 : ownerInfo.getParamClassType();
//    }

    public int getParamClassNumberType() {
        if (ownerInfo != null) {
            return ownerInfo.getParamClassNumberType();
        }
        return ReflectConsts.getParamClassNumberType(actualType);
    }

//    public int getParamClassNumberType() {
//        return ownerInfo == null ? 0 : ownerInfo.getParamClassNumberType();
//    }

    public boolean isCamouflage() {
        return camouflage;
    }

    public String getGenericName() {
        return genericName;
    }
}
