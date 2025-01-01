package io.github.wycst.wast.jdbc.annotations;

import io.github.wycst.wast.jdbc.transform.TypeTransformer;
import io.github.wycst.wast.jdbc.transform.UndoTypeTransformer;

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

//    /**
//     * 字段类型 创建表时使用 definition 即可，如果都没有定义智能判断
//     *
//     * @return
//     */
//    public String type() default "";

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
     * 缺省情况下不参与更新字段
     *
     * @return
     */
    public boolean disabledOnUpdate() default false;

    /**
     * 缺省情况下不参与插入字段
     *
     * @return
     */
    public boolean disabledOnInsert() default false;

    /**
     * 缺省情况下不参与查询字段
     *
     * @return
     */
    public boolean disabledOnSelect() default false;

    /**
     * 类型转化器(插入、更新和查询)
     *
     * @return
     */
    public Class<? extends TypeTransformer> transformer() default UndoTypeTransformer.class;

    /**
     * 读取字段时使用的占位声明（比如空间函数），默认空，
     * <p>如果需要配置必须包含占位关键字${value}，运行态生成sql时动态替换</p>
     * example: ST_AsText(${value})
     *
     * @return
     */
    public String placeholderOnRead() default "";

    /**
     * 写入字段时使用的占位声明（比如空间函数），默认空:
     * <p>如果需要配置必须包含占位关键字${value}，运行态生成sql时动态替换</p>
     * example: GeomFromText(${value})
     *
     * @return
     */
    public String placeholderOnWrite() default "";

    public enum DateType {
        Date,
        Timestamp
    }
}
