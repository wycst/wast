package io.github.wycst.wast.jdbc.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.RetentionPolicy;

/**
 * 忽略映射标记
 *
 * @Author: wangy
 * @Date: 2021/2/15 16:54
 * @Description:
 */
@java.lang.annotation.Target(value = {ElementType.FIELD, ElementType.METHOD})
@java.lang.annotation.Retention(value = RetentionPolicy.RUNTIME)
public @interface Transient {
}
