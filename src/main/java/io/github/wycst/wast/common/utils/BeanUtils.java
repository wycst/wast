package io.github.wycst.wast.common.utils;

import io.github.wycst.wast.common.reflect.ClassStrucWrap;
import io.github.wycst.wast.common.reflect.GetterInfo;
import io.github.wycst.wast.common.reflect.SetterInfo;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 实体bean相关工具类
 *
 * @Author: wangy
 * @Date: 2019/12/7 11:48
 * @Description:
 */
public final class BeanUtils extends InvokeUtils {

    /**
     * 拷贝对象（指针拷贝）
     *
     * @param srcBean    源对象
     * @param targetBean 目标对象
     */
    public static void copy(Object srcBean, Object targetBean) {
        copyProperties(srcBean, targetBean, null);
    }

    /**
     * 拷贝对象（指针拷贝）
     *
     * @param src           源对象
     * @param target        目标对象
     * @param excludeFields 忽略拷贝的属性
     */
    public static void copyProperties(Object src, Object target, String[] excludeFields) {
        if (src == null || target == null)
            return;
        Map targetMap = null;
        ClassStrucWrap targetClassStrucWrap = null;
        Class<?> targetClass = target.getClass();
        boolean isSameBeanType = src.getClass() == targetClass;
        if (target instanceof Map) {
            targetMap = (Map) target;
        } else {
            targetClassStrucWrap = ClassStrucWrap.get(targetClass);
        }
        if (src instanceof Map) {
            Map sourceMap = (Map) src;
            if (targetMap != null) {
                if (excludeFields == null || excludeFields.length == 0) {
                    targetMap.putAll(sourceMap);
                } else {
                    Set<Map.Entry> entrySet = sourceMap.entrySet();
                    for (Map.Entry entry : entrySet) {
                        Object key = entry.getKey();
                        Object value = entry.getValue();
                        if (CollectionUtils.indexOf(excludeFields, value) > -1) {
                            continue;
                        }
                        sourceMap.put(key, value);
                    }
                }
            } else {
                Set<Map.Entry> entrySet = sourceMap.entrySet();
                for (Map.Entry entry : entrySet) {
                    Object key = entry.getKey();
                    if (key == null) continue;
                    if (CollectionUtils.indexOf(excludeFields, key) > -1) {
                        continue;
                    }
                    Object value = entry.getValue();
                    SetterInfo setterInfo = targetClassStrucWrap.getSetterInfo(key.toString());
                    if (setterInfo != null) {
                        value = ObjectUtils.toType(value, setterInfo.getParameterType());
                        invokeSet(setterInfo, target, value);
                    }
                }
            }
        } else {
            ClassStrucWrap sourceClassStrucWrap = isSameBeanType ? targetClassStrucWrap : ClassStrucWrap.get(src.getClass());
            List<GetterInfo> sourceGetterInfos = sourceClassStrucWrap.getGetterInfos();
            for (GetterInfo getterInfo : sourceGetterInfos) {
                String fieldName = getterInfo.getName();
                if (CollectionUtils.indexOf(excludeFields, fieldName) > -1) {
                    continue;
                }
                Object value = invokeGet(getterInfo, src);
                if (targetMap != null) {
                    targetMap.put(fieldName, value);
                } else {
                    SetterInfo setterInfo = targetClassStrucWrap.getSetterInfo(fieldName);
                    if (setterInfo != null) {
                        invokeSet(setterInfo, target, ObjectUtils.toType(value, setterInfo.getParameterType()));
                    }
                }
            }
        }
    }

    /**
     * 合并属性（将srcBean中非空的属性拷贝到targetBean中）
     *
     * @param src    源对象
     * @param target 目标对象
     */
    public static void mergeProperties(Object src, Object target) {
        if (src == null || target == null)
            return;
        Class<?> targetClass = target.getClass();
        Map targetMap = null;
        ClassStrucWrap targetClassStrucWrap = null;
        boolean isSameBeanType = src.getClass() == targetClass;
        if (target instanceof Map) {
            targetMap = (Map) target;
        } else {
            targetClassStrucWrap = ClassStrucWrap.get(targetClass);
        }
        if (src instanceof Map) {
            Map sourceMap = (Map) src;
            if (targetMap != null) {
                targetMap.putAll(sourceMap);
            } else {
                Set<Map.Entry> entrySet = sourceMap.entrySet();
                for (Map.Entry entry : entrySet) {
                    Object key = entry.getKey();
                    SetterInfo setterInfo = targetClassStrucWrap.getSetterInfo(String.valueOf(key));
                    if (setterInfo != null) {
                        invokeSet(setterInfo, target, ObjectUtils.toType(entry.getValue(), setterInfo.getParameterType()));
                    }
                }
            }
        } else {
            ClassStrucWrap sourceClassStrucWrap = isSameBeanType ? targetClassStrucWrap : ClassStrucWrap.get(src.getClass());
            List<GetterInfo> getterInfos = sourceClassStrucWrap.getGetterInfos();
            for (GetterInfo getterInfo : getterInfos) {
                String fieldName = getterInfo.getName();
                Object value = invokeGet(getterInfo, src);
                if (targetMap != null) {
                    targetMap.put(fieldName, value);
                } else {
                    SetterInfo setterInfo;
                    if (value != null && !"".equals(value) && (setterInfo = targetClassStrucWrap.getSetterInfo(fieldName)) != null) {
                        invokeSet(setterInfo, target, ObjectUtils.toType(value, setterInfo.getParameterType()));
                    }
                }
            }
        }
    }
}
