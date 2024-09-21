package io.github.wycst.wast.common.reflect;

import io.github.wycst.wast.common.exceptions.InvokeReflectException;

import java.lang.reflect.Method;

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

    // always public
    public boolean isAccess() {
        return true;
    }

    @Override
    public boolean isPublic() {
        return true;
    }

    public Class<?> getReturnType() {
        return method.getReturnType();
    }

    @Override
    public String getMethodName() {
        return method.getName();
    }

    public String generateCode() {
        return method.getName() + "()";
    }
}
