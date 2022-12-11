package io.github.wycst.wast.common.annotation;

import java.lang.annotation.*;

/**
 * 基于注解的切面标志
 *
 * @Author: wangy
 * @Date: 2019/12/8 10:37
 * @Description:
 */
@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface AnnotationAspect {

    /**
     * 指定切面注解类型
     *
     * @return
     */
    public Class<? extends Annotation> aspectType();

    /**
     * 启用控制
     *
     * @return
     */
    public Class<? extends Annotation> enableControlType();

}
