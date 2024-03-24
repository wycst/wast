package io.github.wycst.wast.clients.http.service.annotations;

import java.lang.annotation.*;

/**
 * @Author: wangy
 * @Date: 2021/8/14 20:53
 * @Description:
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface HttpServiceClient {

    /***
     * 服务名称
     *
     * @return
     */
    public String serviceName() default "";

    /**
     * 上下文路径
     *
     * @return
     */
    public String contextPath() default "";

    /***
     * http(s)
     *
     * @return
     */
    public String protocol() default "http";

    /**
     * 服务地址列表
     *
     * @return
     */
    public String[] serverHosts() default {};

    /***
     * 超时
     *
     * @return
     */
    public long timeout() default 0;

    /**
     * 负载均衡模式下发生超时时是否保持服务实例可用；
     * 默认设置alive = false,下次请求将跳过该实例；
     *
     * @return
     */
    public boolean keepAliveOnTimeout() default false;
}
