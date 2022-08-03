package io.github.wycst.wast.json.annotations;

import io.github.wycst.wast.json.custom.JsonDeserializer;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 自定义反序列注解
 *
 * @Author: wangy
 * @Description:
 */
@Target({ElementType.METHOD, ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
public @interface JsonDeserialize {

    /**
     * 自定义反序列化的实现类
     *
     * @return
     */
    public Class<? extends JsonDeserializer> value();

    /**
     * 是否单例模式（单例模式下会缓存序列化器对象）
     * 默认false, 每次会new有一个JsonDeserializer的实例
     */
    public boolean singleton() default false;

    /**
     * 复杂类型默认转化为Map或者List，开启后将不做转化
     * （似乎没有意义，暂时无用）
     *
     * @return
     */
    public boolean useSource() default false;
}
