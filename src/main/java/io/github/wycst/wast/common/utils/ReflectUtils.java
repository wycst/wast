package io.github.wycst.wast.common.utils;

import io.github.wycst.wast.common.reflect.ClassStrucWrap;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Collection;

/**
 * @Author: wangy
 * @Date: 2021/2/17 22:11
 * @Description:
 */
public final class ReflectUtils {

    /**
     * 获取实例上的泛型或者父类上的泛型
     *
     * @param targetClass
     * @return
     */
    public static Type[] getActualTypes(Class<?> targetClass) {
        Type type = targetClass.getGenericSuperclass();
        int i = 0;
        while (type instanceof Class) {
            type = ((Class<?>) type).getGenericSuperclass();
            if (i++ > 10) {
                break;
            }
        }
        if (type instanceof ParameterizedType) {
            ParameterizedType parameterizedType = (ParameterizedType) type;
            Type[] types = parameterizedType.getActualTypeArguments();
            return types;
        }
        return null;
    }

    /**
     * 获取实例上的泛型或者父类上的泛型
     *
     * @param targetClass
     * @return
     */
    public static Class<?> getActualType(Class<?> targetClass) {
        Type[] types = getActualTypes(targetClass);
        if (types != null && types.length > 0) {
            if (types[0] instanceof Class) {
                return (Class<?>) types[0];
            }
        }
        return null;
    }

    /**
     * 获取实例实现的接口的泛型
     *
     * @param targetClass
     * @return
     */
    public static Class<?> getImplementActualType(Class<?> targetClass) {
        Type[] implementTypes = targetClass.getGenericInterfaces();
        if (implementTypes.length == 0) {
            Class<?> parentCls = (Class) targetClass.getGenericSuperclass();
            implementTypes = parentCls.getGenericInterfaces();
        }
        for (Type implementType : implementTypes) {
            if (implementType instanceof ParameterizedType) {
                ParameterizedType parameterizedType = (ParameterizedType) implementType;
                Type[] types = parameterizedType.getActualTypeArguments();
                if (types != null && types.length > 0) {
                    if (types[0] instanceof Class) {
                        return (Class<?>) types[0];
                    }
                }
            }
            return null;
        }
        return null;
    }

    /**
     * 获取集合类型参数的泛型
     *
     * @param method
     * @param parameterType
     * @return
     */
    public static Class<?> getCollectionActualParamType(Method method, Class<?> parameterType) {
        if (Collection.class.isAssignableFrom(parameterType)) {
            Type genericType = method.getGenericParameterTypes()[0];
            if (genericType instanceof ParameterizedType) {
                ParameterizedType pt = (ParameterizedType) genericType;
                if (pt.getActualTypeArguments()[0] instanceof Class) {
                    Class<?> genericClazz = (Class<?>) pt.getActualTypeArguments()[0];
                    return genericClazz;
                }
            }
            return Object.class;
        } else if (parameterType.isArray()) {
            return parameterType.getComponentType();
        } else {
        }
        return null;
    }

    /**
     * 方法反射调用，注意缓存method，防止每次重新获取带来性能开销
     *
     * @param invoker
     * @param methodName
     * @param params
     * @return
     */
    public static Object invoke(Object invoker, String methodName, Object[] params) throws Exception {
        Class invokerCls = invoker.getClass();
        ClassStrucWrap classStrucWrap = ClassStrucWrap.get(invokerCls);
        return classStrucWrap.invokePublic(invoker, methodName, params);
    }

    /**
     * 递归获取反射字段
     *
     * @param sourceClass
     * @param fieldName
     * @return
     */
    public static Field getField(Class<?> sourceClass, String fieldName) {
        Class<?> target = sourceClass;
        while (true) {
            Field[] fields = target.getDeclaredFields();
            for (Field field : fields) {
                if (field.getName().equals(fieldName)) {
                    return field;
                }
            }
            target = target.getSuperclass();
            if (target == null || target == Object.class) {
                return null;
            }
        }
    }
}
