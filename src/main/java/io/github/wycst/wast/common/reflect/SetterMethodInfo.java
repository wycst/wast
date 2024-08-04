package io.github.wycst.wast.common.reflect;

import io.github.wycst.wast.common.exceptions.InvokeReflectException;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

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

    public void invoke(Object target, Object value) {
        invokeInternal(target, value);
    }

    @Override
    void invokeInternal(Object target, Object value) {
        try {
            method.invoke(target, value);
        } catch (Exception e) {
            throw new InvokeReflectException(e);
        }
    }

    public Object getDefaultFieldValue(Object instance) {
        return null;
    }

    public boolean isMethod() {
        return true;
    }

    public boolean isPrivate() {
        return Modifier.isPrivate(method.getModifiers());
    }
}
