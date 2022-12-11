package io.github.wycst.wast.jdbc.annotations;

import io.github.wycst.wast.jdbc.transaction.TransactionType;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface Transaction {

    /**
     * 事务类型
     *
     * @return
     */
    public TransactionType value() default TransactionType.DEFAULT;


    /**
     * 名称
     *
     * @return
     */
    public String name() default "";


}
