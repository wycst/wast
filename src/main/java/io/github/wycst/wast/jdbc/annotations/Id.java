package io.github.wycst.wast.jdbc.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.RetentionPolicy;

/**
 * 主键标记
 *
 * @author wangy
 */

@java.lang.annotation.Target(value = {ElementType.FIELD, ElementType.METHOD})
@java.lang.annotation.Retention(value = RetentionPolicy.RUNTIME)
public @interface Id {

    /**
     * 生成策略
     *
     * @return
     */
    GenerationType strategy() default GenerationType.None;

    /**
     * 序列名称（仅当strategy = GenerationType.Sequence时有效）
     *
     * @return
     */
    String sequenceName() default "";

    /**
     * id策略
     */
    public enum GenerationType {

        /**
         * 无策略
         */
        None,

        /**
         * 系统自增
         */
        Identity,

        /**
         * 唯一标识
         */
        UUID,

        /**
         * 序列
         */
        Sequence,

        /**
         * 程序算法（雪花算法）
         */
        AutoAlg,

    }
}
