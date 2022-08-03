package io.github.wycst.wast.json.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * @Author: wangy
 * @Description:
 */
@Target({ElementType.METHOD, ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
public @interface JsonProperty {

    /**
     * 指定别名序列化或者反序列化名称
     *
     * @return
     */
    public String name() default "";

    /**
     * json序列化操作时，当前属性是否序列化
     *
     * @return
     */
    public boolean serialize() default true;

    /**
     * json解析时当前属性是否反序列化
     *
     * @return
     */
    public boolean deserialize() default true;

    /**
     * 数据转换时日期序列化格式或字符串反序列化为日期
     *
     * @return
     * @see io.github.wycst.wast.common.beans.Date#format(String)
     */
    public String pattern() default "";

    /**
     * 序列化为时间戳，如果为true优先级高于pattern
     */
    public boolean asTimestamp() default false;

    /**
     * 时区表达式
     * 默认情况下使用TimeZone的默认时区
     * 支持格式: +1, +1:00, -8, +8:00, +08:30等使用GMT
     *
     * @return
     * @see java.util.TimeZone#getTimeZone(String)
     */
    public String timezone() default "";

    /**
     * <p> 如果属性声明的类型为接口或者抽象类，可以为属性指定缺省实现类；
     * <p> 如果不是接口或抽象类，此配置忽略；
     *
     * @return
     */
    public Class<?> impl() default Object.class;
}
