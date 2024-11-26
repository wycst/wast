package io.github.wycst.wast.common.utils;

import io.github.wycst.wast.common.beans.DateParser;
import io.github.wycst.wast.common.beans.GregorianDate;
import io.github.wycst.wast.common.exceptions.TypeNotMatchExecption;
import io.github.wycst.wast.common.reflect.*;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.sql.Timestamp;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

@SuppressWarnings("unchecked")
public final class ObjectUtils extends InvokeUtils {

    public static boolean contains(Object target, String key) {
        target.getClass();
        if (target instanceof Map) {
            return ((Map<?, ?>) target).containsKey(key);
        } else {
            ClassStrucWrap classStrucWrap = ClassStrucWrap.get(target.getClass());
            List<GetterInfo> getterInfos = classStrucWrap.getGetterInfos();
            for (GetterInfo getterInfo : getterInfos) {
                if (getterInfo.getName().equals(key.trim())) {
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
        if (result == null || clazz == null) {
            return (E) result;
        }
        if (isInstance(clazz, result)) {
            return (E) result;
        }
        throw new ClassCastException(result.getClass().getName() + " cannot be cast to " + clazz.getName());
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
        key = key.trim();
        int dotIndex;
        if (target instanceof Map) {
            Map<String, Object> mapTarget = (Map<String, Object>) target;
            Object value = mapTarget.get(key);
            if (value != null) {
                return value;
            }
            // 判断是否为多级属性（key中是否包含.），如果是先取一级，递归获取
            dotIndex = key.indexOf('.');
            if (dotIndex == -1) {
                return null;
            }
            String topKey = key.substring(0, dotIndex);
            String nextKey = key.substring(dotIndex + 1);
            Object nextTarget = mapTarget.get(topKey);
            return get(nextTarget, nextKey);
        } else {
            dotIndex = key.indexOf('.');
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
                    return getObjectFieldValue(target, key);
                }
            }
        }
    }

    /**
     * 获取对象的属性值
     *
     * @param target 对象
     * @param field  属性
     * @return 返回对象中的属性值
     */
    public static Object getAttrValue(Object target, String field) {
        if (target == null)
            return null;
        field.getClass();
        if (target instanceof Map) {
            return ((Map<?, ?>) target).get(field);
        }
        ReflectConsts.ClassCategory classCategory = ReflectConsts.getClassCategory(target.getClass());
        switch (classCategory) {
            case ObjectCategory: {
                return getObjectFieldValue(target, field);
            }
            case CollectionCategory:
            case ArrayCategory: {
                if ("size".equals(field) || "length".equals(field)) {
                    return CollectionUtils.getSize(target);
                }
                if (field.startsWith("[") && field.endsWith("]")) {
                    return CollectionUtils.getElement(target, Integer.parseInt(field.substring(1, field.length() - 1)));
                }
                throw new TypeNotMatchExecption("context property '" + field + "' is invalid ");
            }
            default: {
                return null;
            }
        }
    }

    public static Object getObjectFieldValue(Object target, String field) {
        ClassStrucWrap classStrucWrap = ClassStrucWrap.get(target.getClass());
        GetterInfo getterInfo = classStrucWrap.getGetterInfo(field);
        if (getterInfo != null) {
            return getterInfo.invoke(target);
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
                    ClassStrucWrap classStrucWrap = ClassStrucWrap.get(target.getClass());
                    SetterInfo setterInfo = classStrucWrap.getSetterInfo(key);
                    if (setterInfo != null) {
                        invokeSet(setterInfo, target, ObjectUtils.toType(value, setterInfo.getParameterType()));
                    }
                }
            }
        }
    }

    public static Object[] get(Object target, List<String> keys) {
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
        ClassStrucWrap classStrucWrap = ClassStrucWrap.get(target.getClass());
        List<GetterInfo> getterInfos = classStrucWrap.getGetterInfos();
        for (GetterInfo getterInfo : getterInfos) {
            map.put(getterInfo.getName(), invokeGet(getterInfo, target));
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
        return getNonEmptyFields(target, new String[0]);
    }

    /**
     * 获取对象的非空属性列表
     *
     * @param target
     * @param excludeKeys
     * @return
     */
    public static List<String> getNonEmptyFields(Object target, String... excludeKeys) {
        if (target == null)
            return null;
        List<String> fields = new ArrayList<String>();
        if (target instanceof Map) {
            Map<Object, Object> map = (Map<Object, Object>) target;
            for (Map.Entry<Object, Object> entry : map.entrySet()) {
                Object key = entry.getKey();
                if (key == null) continue;
                String field = key.toString();
                Object val = entry.getValue();
                if (val != null && !val.equals("") && !CollectionUtils.contains(excludeKeys, field)) {
                    fields.add(field);
                }
            }
        } else {
            ClassStrucWrap classStrucWrap = ClassStrucWrap.get(target.getClass());
            List<GetterInfo> getterInfos = classStrucWrap.getGetterInfos();
            for (GetterInfo getterInfo : getterInfos) {
                Object val = getterInfo.invoke(target);
                if (val == null || val.equals("")) continue;
                // exclude zero val
                if (Number.class.isInstance(val) && val.toString().equals("0")) continue;
                String name = getterInfo.getName();
                if (CollectionUtils.contains(excludeKeys, name)) {
                    continue;
                }
                fields.add(name);
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

    public static Number toTypeNumber(Number value, Class<?> numberType) {
        value.getClass();
        if (numberType.isInstance(value) || numberType == Object.class || numberType == Number.class) {
            return value;
        }
        if (numberType == Double.class || numberType == double.class) {
            return value.doubleValue();
        } else if (numberType == Long.class || numberType == long.class) {
            return value.longValue();
        } else if (numberType == Integer.class || numberType == int.class) {
            return value.intValue();
        } else if (numberType == Float.class || numberType == float.class) {
            return value.floatValue();
        } else if (numberType == Short.class || numberType == short.class) {
            return value.shortValue();
        } else if (numberType == Byte.class || numberType == byte.class) {
            return value.byteValue();
        } else if (numberType == BigDecimal.class) {
            return new BigDecimal(value.toString());
        } else if (numberType == BigInteger.class) {
            return new BigInteger(value.toString());
        } else if (numberType == AtomicInteger.class) {
            return new AtomicInteger(value.intValue());
        } else if (numberType == AtomicLong.class) {
            return new AtomicLong(value.longValue());
        }
        return null;
    }

    public static boolean isInstance(Class<?> valueClass, Object value) {
        if (valueClass.isPrimitive()) {
            valueClass = ReflectConsts.PrimitiveType.getWrap(valueClass);
        }
        return valueClass.isInstance(value);
    }

    public static <E> E toType(Object value, Class<E> valueClass, ReflectConsts.ClassCategory classCategory) {
        if (value == null || valueClass == null) return (E) value;
        if (isInstance(valueClass, value)) {
            return (E) value;
        }
        return toTypeByClassCategory(value, valueClass, classCategory);
    }

    /**
     * 将value转化为valueClass的实例,缺省情况下返回0或者null
     *
     * @param value
     * @param valueClass
     * @param <E>
     * @return
     */
    public static <E> E toType(Object value, Class<E> valueClass) {
        if (value == null || valueClass == null) {
            return (E) value;
        }
        if (isInstance(valueClass, value)) {
            return (E) value;
        }
        ReflectConsts.ClassCategory classCategory = ReflectConsts.getClassCategory(valueClass);
        return toTypeByClassCategory(value, valueClass, classCategory);
    }

    private static <E> E toTypeByClassCategory(Object value, Class<E> valueClass, ReflectConsts.ClassCategory classCategory) {
        switch (classCategory) {
            case CharSequence: {
                if (valueClass == String.class) {
                    if (value instanceof Date) {
                        return (E) new GregorianDate(((Date) value).getTime()).format();
                    } else if (value instanceof byte[]) {
                        return (E) new String((byte[]) value);
                    }
                    return (E) value.toString();
                }
                break;
            }
            case NumberCategory: {
                boolean isNumber = value instanceof Number;
                Number numValue;
                if (isNumber) {
                    numValue = (Number) value;
                    return (E) toTypeNumber(numValue, valueClass);
                } else {
                    if (valueClass == Double.class || valueClass == double.class) {
                        numValue = Double.parseDouble(value.toString());
                        return (E) numValue;
                    }
                    if (valueClass == Float.class || valueClass == float.class) {
                        numValue = Float.parseFloat(value.toString());
                        return (E) numValue;
                    }
                    if (valueClass == BigDecimal.class) {
                        numValue = new BigDecimal(value.toString());
                    } else if (valueClass == BigInteger.class) {
                        numValue = new BigInteger(value.toString());
                    } else {
                        numValue = Long.parseLong(value.toString());
                    }
                    return (E) toTypeNumber(numValue, valueClass);
                }
            }
            case BoolCategory: {
                if (value == Boolean.TRUE || value == Boolean.FALSE) {
                    return (E) value;
                }
                if (value instanceof Number) {
                    Boolean bool = ((Number) value).intValue() != 0;
                    return (E) bool;
                } else {
                    String stringValue = value.toString().toLowerCase();
                    if (stringValue.equals("true") || stringValue.equals("yes") || stringValue.equals("on")) {
                        return (E) Boolean.TRUE;
                    } else if (stringValue.equals("false") || stringValue.equals("no") || stringValue.equals("off")) {
                        return (E) Boolean.FALSE;
                    }
                }
                break;
            }
            case DateCategory: {
                String dateValue = value.toString();
                long time = DateParser.parseTime(dateValue);
                if (valueClass == Date.class) {
                    return (E) new Date(time);
                } else if (valueClass == Timestamp.class) {
                    return (E) new Timestamp(time);
                } else {
                    try {
                        Constructor constructor = valueClass.getDeclaredConstructor(new Class[]{long.class});
                        UnsafeHelper.setAccessible(constructor);
                        return (E) constructor.newInstance(time);
                    } catch (Exception e) {
                        throw new UnsupportedOperationException("not supported for date type " + valueClass);
                    }
                }
            }
            case EnumCategory: {
                if (value instanceof Number) {
                    Class<? extends Enum> enumCls = (Class<? extends Enum>) valueClass;
                    Enum[] values = enumCls.getEnumConstants();
                    int index = ((Number) value).intValue();
                    Enum enumValue = index < values.length ? values[index] : null;
                    return (E) enumValue;
                } else {
                    return (E) Enum.valueOf((Class<? extends Enum>) valueClass, value.toString());
                }
            }
            case Binary: {
                if (value instanceof String) {
                    return (E) ((String) value).getBytes();
                }
            }
            case ANY: {
                return (E) value;
            }
            case ObjectCategory: {
                ClassStrucWrap structureWrapper = ClassStrucWrap.get(valueClass);
                if (structureWrapper.isTemporal()) {
                    // jdk8+ time api
                }
                break;
            }
        }
        return null;
    }
}
