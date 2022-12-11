package io.github.wycst.wast.jdbc.annotations;

import java.lang.annotation.*;

/**
 * 启用声明式事务管理
 *
 * @author wangy
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface EnableTransaction {

}
