package io.github.wycst.wast.jdbc.transform;

import io.github.wycst.wast.common.reflect.GenericParameterizedType;

/**
 * 类型转化器
 *
 */
public abstract class TypeTransformer<E> {

    protected GenericParameterizedType parameterizedType;

    public final void setParameterizedType(GenericParameterizedType parameterizedType) {
        this.parameterizedType = parameterizedType;
    }

    public GenericParameterizedType getParameterizedType() {
        return parameterizedType;
    }

    /**
     * from从java属性读出或转化需要往数据库中插入的值
     *
     * @param value
     * @return
     */
    public abstract Object fromJavaField(E value);

    /**
     * 从数据库读出的字段内容转化为java属性中
     *
     * @param value
     * @return
     */
    public abstract E toJavaField(Object value);
}
