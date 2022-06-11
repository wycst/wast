package io.github.wycst.wast.clients.http.loadbalance;

import io.github.wycst.wast.clients.http.provider.ServerZone;
import io.github.wycst.wast.clients.http.provider.ServiceInstance;

/**
 * 负载均衡调度接口
 *
 * @Author: wangy
 * @Date: 2020/8/25 14:23
 * @Description:
 */
public interface LoadBalanceStrategy {

    ServiceInstance select(ServerZone serverZone);

}



