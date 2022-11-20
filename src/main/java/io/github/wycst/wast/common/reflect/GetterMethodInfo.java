package io.github.wycst.wast.common.reflect;

import io.github.wycst.wast.common.exceptions.InvokeReflectException;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

/**
 * 使用getter方法反射
 *
 * @Author: wangy
 * @Date: 2022/1/8 13:32
 * @Description:
 */
final class GetterMethodInfo extends GetterInfo {

    private final Method method;

    GetterMethodInfo(Method method) {
        this.method = method;
    }

    /**
     * 反射属性
     */
    public Object invokeObjectValue(Object target) {
        try {
            return method.invoke(target);
        } catch (Exception e) {
            throw new InvokeReflectException(e);
        }
    }

    // 是否通过getter方法
    public boolean isMethod() {
        return true;
    }

    // 是否private私有方法
    public boolean isPrivate() {
        return false;
    }

    public Class<?> getReturnType() {
        return method.getReturnType();
    }

    @Override
    public String getMethodName() {
        return method.getName();
    }
}
