package io.github.wycst.wast.clients.http.provider;

import io.github.wycst.wast.clients.http.definition.HttpClientRequest;
import io.github.wycst.wast.clients.http.loadbalance.LoadBalanceStrategy;

import java.net.MalformedURLException;
import java.util.List;

/**
 * @Author: wangy
 * @Date: 2020/7/10 15:05
 * @Description:
 */
public interface ServiceProvider {

    /**
     * 设置负载均衡策略
     *
     * @param loadBalanceStrategy
     */
    public void setLoadBalanceStrategy(LoadBalanceStrategy loadBalanceStrategy);

    /**
     * 注册服务
     *
     * @param serverZone
     */
    void registerServer(ServerZone serverZone);

//    /**
//     * 从请求中获取真实的URL
//     *
//     * @param httpRequest
//     * @return
//     */
//    URL fromRequest(HttpClientRequest httpRequest) throws MalformedURLException;

    /**
     * 从请求中获取服务实例
     * <p> 如果没有找到实例返回空
     *
     * @param httpRequest
     * @return
     */
    ServiceInstance getServiceInstance(HttpClientRequest httpRequest) throws MalformedURLException;

    /**
     * 获取服务信息
     *
     * @param serviceName
     * @return
     */
    ServerZone getServer(String serviceName);

    /**
     * 清除所有
     */
    void clear();

    /***
     * 清除不存在的服务
     *
     * @param doms
     */
    void clearIfNotExist(List<String> doms);
}
