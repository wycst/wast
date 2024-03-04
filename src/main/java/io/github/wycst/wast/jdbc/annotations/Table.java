package io.github.wycst.wast.jdbc.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 表注解信息
 *
 * @author wangy
 */
@Target(value = {ElementType.TYPE})
@Retention(value = RetentionPolicy.RUNTIME)
public @interface Table {

    /**
     * 表名
     *
     * @return
     */
    String name() default "";

    /**
     * 表前缀  xxx.tableName
     * 如果是oracle配置用户名，如果是mysql配置实例库名
     * 可以为空
     *
     * @return
     */
    String schame() default "";

    /**
     * 当前表是否开启数据缓存(只对id查询有效)
     *
     * @return
     */
    boolean cacheable() default false;

    /**
     * cacheable为true时有效，缓存的过期时间
     *
     * @return
     */
    long expires() default 8 * 3600 * 1000;
}
