package io.github.wycst.wast.common.csv;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target({ElementType.METHOD, ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
public @interface CSVColumn {

    /**
     * 映射名称
     *
     * @return
     */
    public String value();

    /**
     * 检查字段是否存在以及值是否为空，如果为空转化将抛出异常
     *
     * @return
     */
    public boolean required() default false;

    /**
     * 类型转化handler
     *
     * @return
     */
    public Class<? extends CSVTypeHandler> handler() default CSVTypeHandler.DefaultCSVTypeHandler.class;
}
