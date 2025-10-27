package io.github.wycst.wast.json.annotations;

import io.github.wycst.wast.common.beans.GregorianDate;

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
     * @see GregorianDate#format(String)
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
     * <p> 反序列化使用；
     * <p> 如果属性声明的类型为接口或者抽象类，可以为属性指定缺省实现类(如果不是属性声明的子类会被忽略)；
     * <p> 如果不是接口或抽象类，此配置忽略；
     * <p> 支持Map/List等指定实现类
     *
     * @return
     */
    public Class<?> impl() default Object.class;

    /**
     * <p> 反序列化使用；
     * <p> 取值可能为多个类型(联合类型)，比如字符串，数字，对象，数组，集合，map，对象等，常用于使用Object声明的类型，但又希望在反序列时能具体到某个对象类型；
     * <p> 如果多个类型都支持，则优先使用排在靠前的类型；
     * <p> 如果数组中某个类型不能适配Field类型会被忽略；
     *
     * @return
     */
    Class<?>[] possibleTypes() default {};

    /**
     * <p> 反序列化使用；
     * <p> 如果为空忽略处理；
     * <p> 针对possibleTypes定义存在多个适配的实现子类;
     * <p> 表达式返回int类型，将作为possibleTypes数组的下标类型；
     * <p> 场景1 - 三目运算符： type == 'user' ? 0 : 1；
     * <p> 场景2 - 数组索引：  ['User', 'Menu', 'Role'].indexOf(type)；
     *
     * @return
     */
    String possibleExpression() default "";

    /**
     * 是否不固定类型，比如属性的声明类型为父类存在很多子类实现，实际的值类型不确定是哪个场景
     *
     * <p>1.序列化会写入字段类型(@c)</p>
     * <p>2.反序列化会第一时间读取类型（第一个字段@c）</p>
     * <p>3.只针对pojo类型的字段生效</p>
     *
     * @return
     */
    public boolean unfixedType() default false;
}
