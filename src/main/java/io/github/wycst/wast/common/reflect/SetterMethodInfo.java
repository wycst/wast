package io.github.wycst.wast.common.reflect;

import io.github.wycst.wast.common.exceptions.InvokeReflectException;

import java.lang.reflect.Method;

/**
 * 使用setter方法反射
 *
 * @Author: wangy
 * @Date: 2022/1/8 13:42
 * @Description:
 */
final class SetterMethodInfo extends SetterInfo {

    private final Method method;

    SetterMethodInfo(Method method) {
        this.method = method;
    }

    /**
     * 反射调用
     */
    public void invokeObjectValue(Object target, Object value) {
        try {
            method.invoke(target, value);
        } catch (Exception e) {
            throw new InvokeReflectException(e);
        }
    }

}
