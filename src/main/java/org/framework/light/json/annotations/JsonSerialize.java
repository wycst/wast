package org.framework.light.json.annotations;

import org.framework.light.common.annotations.Serialize;
import org.framework.light.json.custom.JsonSerializer;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 自定义序列化注解
 *
 * @Author: wangy
 * @Description:
 */
@Target({ElementType.METHOD, ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
@Serialize
public @interface JsonSerialize {

    /**
     * 自定义序列化的实现类
     *
     * @return
     */
    public Class<? extends JsonSerializer> value();

    /**
     * 是否单例模式（缓存）
     */
    public boolean singleton() default false;
}
