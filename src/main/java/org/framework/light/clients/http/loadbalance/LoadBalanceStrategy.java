package org.framework.light.clients.http.loadbalance;

import org.framework.light.clients.http.provider.ServerZone;
import org.framework.light.clients.http.provider.ServiceInstance;

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



