package io.github.wycst.wast.jdbc.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 数据库表和实体的字段注解配置
 *
 * @author wangy
 */

@Target({ElementType.METHOD, ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
public @interface Column {

    /**
     * 字段名称
     *
     * @return
     */
    public String name() default "";

    /**
     * 字段类型 创建表时使用 definition 即可，如果都没有定义智能判断
     *
     * @return
     */
    public String type() default "";

    /**
     * 字段定义
     *
     * @return
     */
    public String definition() default "";

    /**
     * 日期类型： 默认时间戳
     */
    public DateType dateType() default DateType.Timestamp;

    /**
     * 1   日期类型转字符串属时序列化格式
     * 2   其他需求转换格式
     *
     * @return
     */
    public String pattern() default "";

    /**
     * prepared 占位符号默认空代表?
     */
    public String placeholder() default "";

    public enum DateType {
        Date,
        Timestamp
    }
}
