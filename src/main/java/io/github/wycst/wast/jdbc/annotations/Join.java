package io.github.wycst.wast.jdbc.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 支持join查询的注解配置
 *
 * @Author: wangy
 * @Date: 2021/7/25 21:07
 * @Description:
 */
@Target({ElementType.METHOD, ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
public @interface Join {

    /**
     * 目标实体类
     *
     * @return
     */
    public Class<?> target();

    /**
     * 关联字段,缺省与id关联
     *
     * @return
     */
    public String field() default "id";

}
