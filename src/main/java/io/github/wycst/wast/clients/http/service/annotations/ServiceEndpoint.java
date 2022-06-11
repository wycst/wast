package io.github.wycst.wast.clients.http.service.annotations;

import io.github.wycst.wast.clients.http.definition.HttpClientMethod;

import java.lang.annotation.*;

/**
 * @Author: wangy
 * @Date: 2021/8/14 23:36
 * @Description:
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface ServiceEndpoint {

    /**
     * 端点uri
     *
     * @return
     */
    public String uri();

    /**
     * 请求方式（需要显示指定一个）
     *
     * @return
     */
    public HttpClientMethod method();

    /***
     * 内容格式
     *
     * @return
     */
    public String contentType() default "";

    /***
     * 超时时间
     *
     * @return
     */
    public long timeout() default 0;
}
