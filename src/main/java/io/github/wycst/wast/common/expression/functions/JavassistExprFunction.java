package io.github.wycst.wast.common.expression.functions;

import io.github.wycst.wast.common.expression.ExprFunction;
import io.github.wycst.wast.common.utils.ReflectUtils;

import java.lang.reflect.Array;
import java.lang.reflect.Type;

/**
 * Compatibility processing of functions for javassist
 *
 * @Author: wangy
 * @Date: 2022/11/14 21:31
 * @Description:
 */
public abstract class JavassistExprFunction<I, O> implements ExprFunction {

    private final Class<?> actualParamCls;

    public JavassistExprFunction() {
        Type[] types = ReflectUtils.getActualTypes(this.getClass());
        if (types == null || types.length < 2 || !(types[1] instanceof Class)) {
            actualParamCls = Object.class;
        } else {
            actualParamCls = (Class<?>) types[0];
        }
    }

    public Class<?> getActualParamClass() {
        return actualParamCls;
    }

    /**
     * @param p
     * @return
     */
    public abstract O execute(I... p);

    @Override
    public final Object call(Object[] params) {
        I[] p = toArray(params);
        return execute(p);
    }

    public final Object call(int[] params) {
        I[] p = newInstance(params.length);
        for (int i = 0; i < params.length; i++) {
            p[i] = (I) (Integer) params[i];
        }
        return execute(p);
    }

    public final Object call(float[] params) {
        I[] p = newInstance(params.length);
        for (int i = 0; i < params.length; i++) {
            p[i] = (I) (Float) params[i];
        }
        return execute(p);
    }

    public final Object call(long[] params) {
        I[] p = newInstance(params.length);
        for (int i = 0; i < params.length; i++) {
            p[i] = (I) (Long) params[i];
        }
        return execute(p);
    }

    public final Object call(double[] params) {
        I[] p = newInstance(params.length);
        for (int i = 0; i < params.length; i++) {
            p[i] = (I) (Double) params[i];
        }
        return execute(p);
    }

    public final Object call(boolean[] params) {
        I[] p = newInstance(params.length);
        for (int i = 0; i < params.length; i++) {
            p[i] = (I) (Boolean) params[i];
        }
        return execute(p);
    }

    public final Object call(short[] params) {
        I[] p = newInstance(params.length);
        for (int i = 0; i < params.length; i++) {
            p[i] = (I) (Short) params[i];
        }
        return execute(p);
    }

    public final Object call(byte[] params) {
        I[] p = newInstance(params.length);
        for (int i = 0; i < params.length; i++) {
            p[i] = (I) (Byte) params[i];
        }
        return execute(p);
    }

    private I[] toArray(Object[] params) {
        if (params.getClass().getComponentType() == actualParamCls) {
            return (I[]) params;
        } else {
            I[] r = newInstance(params.length);
            for (int i = 0, len = params.length; i < len; i++) {
                r[i] = (I) params[i];
            }
            return r;
        }
    }

    private I[] newInstance(int len) {
        return (I[]) Array.newInstance(actualParamCls, len);
    }
}
