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

    /**
     * 使用子查询
     * <p> 使用子查询（关联表主键查询）标记后会替换默认的join语法（left join）,请注意子查询和left join区别 </p>
     * <p> 请注意关联同一个实体的多个字段时join字段超过1个时不适合使用子查询，此时useSubQuery不会生效，会退回使用join  </p>
     */
    public boolean useSubQuery() default false;

}
