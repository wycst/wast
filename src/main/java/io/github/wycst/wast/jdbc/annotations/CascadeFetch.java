package io.github.wycst.wast.jdbc.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 级联和提取注解（可配约束）
 * 查询时fetch可以控制是否加载关联的实体或实体集合
 * 删除时cascade可以控制是否要级联删除的实体或者实体集合
 *
 * @Author: wangy
 * @Date: 2021/8/5 09:36
 * @Description:
 */
@Target({ElementType.METHOD, ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
public @interface CascadeFetch {

    /**
     * 当前实体用于关联的属性域 默认id
     *
     * @return
     */
    public String field() default "id";

    /**
     * 目标实体中和field关联的属性域
     *
     * @return
     */
    public String targetField();

    /***
     *  级联删除(一对多，删除一时级联删除多)
     *
     * @return
     */
    public boolean cascade() default true;

    /***
     * 同步提取（一对多或者一对一，查询一时同步加载多）
     *
     * @return
     */
    public boolean fetch() default true;
}
