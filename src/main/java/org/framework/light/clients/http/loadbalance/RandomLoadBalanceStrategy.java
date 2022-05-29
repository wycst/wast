package org.framework.light.clients.http.loadbalance;

import org.framework.light.clients.http.provider.ServerZone;
import org.framework.light.clients.http.provider.ServiceInstance;

import java.util.List;
import java.util.Random;

/**
 * 负载均衡随机策略
 *
 * @Author: wangy
 * @Date: 2020/8/25 14:30
 * @Description:
 */
public class RandomLoadBalanceStrategy extends AbstractLoadBalanceStrategy {

    @Override
    public ServiceInstance doSelect(List<ServiceInstance> aliveInstances, ServerZone serverZone) {
        int count = aliveInstances.size();
        Random random = new Random();
        int index = random.nextInt(count);
        return aliveInstances.get(index);
    }

}
