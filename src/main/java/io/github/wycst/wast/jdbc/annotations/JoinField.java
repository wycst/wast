package io.github.wycst.wast.jdbc.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * join查询映射字段,追加join连接
 *
 * @Author: wangy
 * @Date: 2021/7/25 21:12
 * @Description:
 */
@Target({ElementType.METHOD, ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
public @interface JoinField {

    /**
     * 目标实体类
     *
     * @return
     */
    public Class<?> target();

    /***
     * 字段属性
     *
     * @return
     */
    public String field();

    /***
     * 转换表达式
     * TODO: 2021/7/25
     *
     * @return
     */
    public String transform() default "";
}
