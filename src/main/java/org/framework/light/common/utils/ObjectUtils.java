package org.framework.light.common.utils;

import org.framework.light.common.exceptions.LogicNullPointerException;
import org.framework.light.common.exceptions.TypeNotMatchExecption;
import org.framework.light.common.reflect.ClassStructureWrapper;
import org.framework.light.common.reflect.GetterInfo;
import org.framework.light.common.reflect.SetterInfo;

import java.lang.reflect.Method;
import java.util.*;

@SuppressWarnings("unchecked")
public class ObjectUtils {

    public static boolean contains(Object target, String key) {

        if (target == null)
            throw new LogicNullPointerException(" target is null !");

        if (target instanceof Map) {
            return ((Map<?, ?>) target).containsKey(key);
        } else {
            ClassStructureWrapper classStructureWrapper = ClassStructureWrapper.get(target.getClass());
            List<GetterInfo> getterInfos = classStructureWrapper.getGetterInfos();
            for (GetterInfo getterInfo : getterInfos) {
                if (getterInfo.getFieldName().equals(key.trim())) {
                    return true;
                }
            }
        }

        return false;
    }

    /**
     * 从上文中读取表达式key
     *
     * @param context
     * @param exprKey
     * @param clazz
     * @param <E>
     * @return
     */
    public static <E> E getContext(Object context, String exprKey, Class<E> clazz) {
        Object result = get(context, exprKey);
        if (result == null) return (E) null;
        if (clazz != null && !clazz.isInstance(result)) {
            throw new TypeNotMatchExecption(" type is not match, expect " + clazz + ", but get " + result.getClass());
        }
        return (E) result;
    }

    public static <E> E get(Object target, String key, Class<E> clazz) {
        Object result = get(target, key);
        if (result == null) return null;
        try {
            return (E) result;
        } catch (Throwable throwable) {
            throw new TypeNotMatchExecption(" type is not match, expect " + clazz + ", but get " + result.getClass());
        }
    }

    /**
     * 从对象中查找属性或key对应的value		<br>
     * 路径中的属性一旦为空返回null
     * <p>
     * 支持多级访问	eg: user.name
     * 支持数组访问   eg: users.[n].name
     *
     * @param target
     * @param key
     * @return
     */
    public static Object get(Object target, String key) {
        if (target == null)
            return null;
        if (key == null)
            throw new LogicNullPointerException(" key is null !");

        key = key.trim();
        // 判断是否为多级属性（key中是否包含.），如果是先取一级，递归获取
        int dotIndex = key.indexOf('.');
        if (target instanceof Map) {
            Map<String, Object> mapTarget = (Map<String, Object>) target;
            if (mapTarget.containsKey(key)) {
                return mapTarget.get(key);
            } else {
                if (dotIndex > -1) {
                    String topKey = key.substring(0, dotIndex);
                    String nextKey = key.substring(dotIndex + 1);
                    Object nextTarget = mapTarget.get(topKey);
                    return get(nextTarget, nextKey);
                }
            }
        } else {
            if (dotIndex > -1) {
                String topKey = key.substring(0, dotIndex);
                String nextKey = key.substring(dotIndex + 1);
                Object nextTarget = get(target, topKey);
                return get(nextTarget, nextKey);
            } else {
                if (CollectionUtils.isCollection(target)) {
                    if ("size".equals(key) || "length".equals(key)) {
                        return CollectionUtils.getSize(target);
                    }
                    if (key.startsWith("[") && key.endsWith("]")) {
                        return CollectionUtils.getElement(target, Integer.parseInt(key.substring(1, key.length() - 1)));
                    }
                    throw new TypeNotMatchExecption("context property '" + key + "' is invalid ");
                } else {
                    ClassStructureWrapper classStructureWrapper = ClassStructureWrapper.get(target.getClass());
                    List<GetterInfo> getterInfos = classStructureWrapper.getGetterInfos();
                    for (GetterInfo getterInfo : getterInfos) {
                        if (getterInfo.getFieldName().equals(key.trim())) {
                            return getterInfo.invoke(target);
                        }
                    }
                }
            }
        }

        return null;
    }

    /**
     * 给目标对象的结构（key）赋值(update)
     * <p>
     * 支持多级访问	eg: user.name
     * 支持数组访问   eg: users.[n].name
     *
     * @param target
     * @param key
     * @param value
     */
    public static void set(Object target, String key, Object value) {
        set(target, key, value, false);
    }

    /**
     * 给目标对象的结构（key）赋值(update)
     * <p>
     * 支持多级访问	eg: user.name
     * 支持数组访问   eg: users.[n].name
     *
     * @param target
     * @param key
     * @param value
     * @param createIfMapNull 如果路径中属性为空是否创建对象（map属性）
     */
    public static void set(Object target, String key, Object value, boolean createIfMapNull) {
        if (target == null)
            return;

        if (key == null)
            throw new LogicNullPointerException(" key is null !");

        key = key.trim();
        /**
         *  判断是否为多级属性（key中是否包含.），如果是先取一级，递归获取
         */
        int dotIndex = key.indexOf('.');
        if (target instanceof Map) {
            Map<String, Object> mapTarget = (Map<String, Object>) target;
            if (mapTarget.containsKey(key)) {
                mapTarget.put(key, value);
            } else {
                if (dotIndex > -1) {
                    String topKey = key.substring(0, dotIndex);
                    String nextKey = key.substring(dotIndex + 1);
                    Object nextTarget = mapTarget.get(topKey);
                    if (createIfMapNull && nextTarget == null) {
                        nextTarget = new LinkedHashMap<String, Object>();
                        mapTarget.put(topKey, nextTarget);
                    }
                    set(nextTarget, nextKey, value, createIfMapNull);
                } else {
                    mapTarget.put(key, value);
                }
            }
        } else {
            if (dotIndex > -1) {
                String topKey = key.substring(0, dotIndex);
                String nextKey = key.substring(dotIndex + 1);
                Object nextTarget = get(target, topKey);
                set(nextTarget, nextKey, value, createIfMapNull);
            } else {
                if (CollectionUtils.isCollection(target)) {
                    if (key.startsWith("[") && key.endsWith("]")) {
                        CollectionUtils.setElement(target, Integer.parseInt(key.substring(1, key.length() - 1)), value);
                    }
                    throw new TypeNotMatchExecption("context property '" + key + "' is invalid ");
                } else {
                    ClassStructureWrapper classStructureWrapper = ClassStructureWrapper.get(target.getClass());
                    SetterInfo setterInfo = classStructureWrapper.getSetterInfo(key);
                    if (setterInfo != null) {
                        setterInfo.invoke(target, value);
                    }
                }
            }
        }
    }

    public static Object[] get(Object target, List<String> keys) {
        if (keys == null)
            throw new LogicNullPointerException(" keys is null !");
        Object[] values = new Object[keys.size()];
        int index = 0;
        for (String key : keys) {
            values[index++] = get(target, key);
        }
        return values;
    }

    public static Map<String, Object> toMap(Object target) {

        if (target == null)
            return null;

        if (target instanceof Map) {
            return (Map<String, Object>) target;
        }

        Map<String, Object> map = new HashMap<String, Object>();
        ClassStructureWrapper classStructureWrapper = ClassStructureWrapper.get(target.getClass());
        List<GetterInfo> getterInfos = classStructureWrapper.getGetterInfos();
        for (GetterInfo getterInfo : getterInfos) {
            map.put(getterInfo.getFieldName(), getterInfo.invoke(target));
        }

        return map;
    }

    /**
     * 获取对象的非空属性列表
     *
     * @param target
     * @return
     */
    public static List<String> getNonEmptyFields(Object target) {
        if (target == null)
            return null;
        List<String> fields = new ArrayList<String>();
        if (target instanceof Map) {
            Map<String, Object> map = (Map<String, Object>) target;
            for (String field : map.keySet()) {
                Object val = map.get(field);
                if (val != null && !val.equals("")) {
                    fields.add(field);
                }
            }
        } else {
            ClassStructureWrapper classStructureWrapper = ClassStructureWrapper.get(target.getClass());
            List<GetterInfo> getterInfos = classStructureWrapper.getGetterInfos();
            for (GetterInfo getterInfo : getterInfos) {
                Object val = getterInfo.invoke(target);
                if (val != null && !val.equals("") && !val.toString().equals("0")) {
                    fields.add(getterInfo.getFieldName());
                }
            }
        }
        return fields;
    }

    /**
     * 判断2个方法内容是否相同（弱比较）
     */
    public static boolean methodWeakEquals(Method source, Method target) {

        // 忽略声明类比较
        if (!source.getName().equals(target.getName()))
            return false;

        if (!source.getReturnType().equals(target.getReturnType()))
            return false;
        /* Avoid unnecessary cloning */
        Class<?>[] params1 = source.getParameterTypes();
        Class<?>[] params2 = target.getParameterTypes();
        if (params1.length == params2.length) {
            for (int i = 0; i < params1.length; i++) {
                if (params1[i] != params2[i])
                    return false;
            }
            return true;
        }

        return false;
    }

    public static Iterable<Object> getIterable(Object context, String key) {
        Object target = get(context, key);
        if (target == null) {
            throw new TypeNotMatchExecption("context property '" + key + "' is null or not iterable ");
        }
        if (target instanceof Iterable) {
            return (Iterable) target;
        }
        if (target.getClass().isArray()) {
            Object[] array = (Object[]) target;
            return Arrays.asList(array);
        } else {
            throw new TypeNotMatchExecption("context property '" + key + "' is not array or iterable ");
        }
    }
}
