package io.github.wycst.wast.clients.http.loadbalance;

import io.github.wycst.wast.clients.http.provider.ServerZone;
import io.github.wycst.wast.clients.http.provider.ServiceInstance;

import java.util.List;

/**
 * 负载均衡轮询策略
 *
 * @Author: wangy
 * @Date: 2020/8/25 14:29
 * @Description:
 */
public class PollingLoadBalanceStrategy extends AbstractLoadBalanceStrategy {

    @Override
    public ServiceInstance doSelect(List<ServiceInstance> aliveInstances, ServerZone serverZone) {
        int count = aliveInstances.size();
        int nextIndex = serverZone.nextIndex(count);
        return aliveInstances.get(nextIndex);
    }

}
