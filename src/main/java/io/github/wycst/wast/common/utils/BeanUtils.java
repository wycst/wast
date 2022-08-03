package io.github.wycst.wast.common.utils;

import io.github.wycst.wast.common.reflect.ClassStructureWrapper;
import io.github.wycst.wast.common.reflect.GetterInfo;
import io.github.wycst.wast.common.reflect.SetterInfo;

import java.util.List;
import java.util.Map;

/**
 * 实体bean相关工具类
 *
 * @Author: wangy
 * @Date: 2019/12/7 11:48
 * @Description:
 */
public class BeanUtils {

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
     * @param srcBean       源对象
     * @param targetBean    目标对象
     * @param excludeFields 忽略拷贝的属性
     */
    public static void copyProperties(Object srcBean, Object targetBean, String[] excludeFields) {

        if (srcBean == null || targetBean == null)
            return;

        // List不支持元素拷贝
        Map targetMap = null;
        ClassStructureWrapper targetClassStructureWrapper = null;
        boolean isSameBeanType = srcBean.getClass() == targetBean.getClass();

        if (targetBean instanceof Map) {
            targetMap = (Map) targetBean;
        } else {
            targetClassStructureWrapper = ClassStructureWrapper.get(targetBean.getClass());
        }

        if (srcBean instanceof Map) {
            Map sourceMap = (Map) srcBean;
            if (targetMap != null) {
                targetMap.putAll(sourceMap);
            } else {
                for (Object key : sourceMap.keySet()) {
                    Object value = sourceMap.get(key);
                    SetterInfo setterInfo = targetClassStructureWrapper.getSetterInfo(key.toString());
                    if (setterInfo != null) {
                        setterInfo.invoke(targetBean, value);
                    }
                }
            }
        } else {
            ClassStructureWrapper sourceClassStructureWrapper = isSameBeanType ? targetClassStructureWrapper : ClassStructureWrapper.get(srcBean.getClass());
            List<GetterInfo> sourceGetterInfos = sourceClassStructureWrapper.getGetterInfos();

            for (GetterInfo getterInfo : sourceGetterInfos) {
                String fieldName = getterInfo.getName();
                if (targetMap != null) {
                    targetMap.put(fieldName, getterInfo.invoke(srcBean));
                } else {
                    SetterInfo setterInfo = targetClassStructureWrapper.getSetterInfo(fieldName);
                    if (setterInfo != null) {
                        setterInfo.invoke(targetBean, getterInfo.invoke(srcBean));
                    }
                }
            }
        }
    }

    /**
     * 合并属性（将srcBean中非空的属性拷贝到targetBean中）
     *
     * @param srcBean    源对象
     * @param targetBean 目标对象
     */
    public static void mergeProperties(Object srcBean, Object targetBean) {

        if (srcBean == null || targetBean == null)
            return;

        // List不支持元素拷贝
        Map targetMap = null;
        ClassStructureWrapper targetClassStructureWrapper = null;

        boolean isSameBeanType = srcBean.getClass() == targetBean.getClass();

        if (targetBean instanceof Map) {
            targetMap = (Map) targetBean;
        } else {
            targetClassStructureWrapper = ClassStructureWrapper.get(targetBean.getClass());
        }

        if (srcBean instanceof Map) {
            Map sourceMap = (Map) srcBean;
            if (targetMap != null) {
                // 如果是map中往map拷贝直接覆盖不考虑非空
                targetMap.putAll(sourceMap);
            } else {
                for (Object key : sourceMap.keySet()) {
                    Object value = sourceMap.get(key);
                    // 如果是map中往对象拷贝直接覆盖不考虑非空
                    if (targetClassStructureWrapper.containsSetterKey(String.valueOf(key))) {
                        SetterInfo setterInfo = targetClassStructureWrapper.getSetterInfo(String.valueOf(key));
                        setterInfo.invoke(targetBean, value);
                    }
                }
            }
        } else {
            ClassStructureWrapper sourceClassStructureWrapper = isSameBeanType ? targetClassStructureWrapper : ClassStructureWrapper.get(srcBean.getClass());
            List<GetterInfo> sourceGetterInfos = sourceClassStructureWrapper.getGetterInfos();

            for (GetterInfo getterInfo : sourceGetterInfos) {
                String fieldName = getterInfo.getName();
                Object val = getterInfo.invoke(srcBean);
                if (targetMap != null) {
                    targetMap.put(fieldName, val);
                } else {
                    // 只有非空的属性进行合并操作
                    if (val != null && !"".equals(val) && targetClassStructureWrapper.containsSetterKey(fieldName)) {
                        SetterInfo setterInfo = targetClassStructureWrapper.getSetterInfo(fieldName);
                        setterInfo.invoke(targetBean, val);
                    }
                }
            }
        }
    }

}
