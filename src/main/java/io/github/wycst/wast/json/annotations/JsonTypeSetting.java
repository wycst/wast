package io.github.wycst.wast.json.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * json type setting
 *
 * @Date 2024/3/24 16:10
 * @Created by wangyc
 * @since 0.0.11
 */
@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface JsonTypeSetting {

    /**
     * <p> running on Strict mode
     *
     * @return
     */
    boolean strict() default false;

    /**
     * Enable JIT optimization (currently only supported for beans of conventions)
     *
     * @return
     */
    boolean enableJIT() default false;
}
