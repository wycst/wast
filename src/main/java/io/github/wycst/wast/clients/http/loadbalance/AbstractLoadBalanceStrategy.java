package io.github.wycst.wast.clients.http.loadbalance;

import io.github.wycst.wast.clients.http.provider.ServerZone;
import io.github.wycst.wast.clients.http.provider.ServiceInstance;

import java.util.ArrayList;
import java.util.List;

/**
 * @Author: wangy
 * @Date: 2020/8/25 15:16
 * @Description:
 */
public abstract class AbstractLoadBalanceStrategy implements LoadBalanceStrategy {

    public abstract ServiceInstance doSelect(List<ServiceInstance> aliveInstances, ServerZone serverZone);

    @Override
    public ServiceInstance select(ServerZone serverZone) {
        String serverName = serverZone.getServerName();
        List<ServiceInstance> instances = serverZone.getServiceInstances();
        checkIf(instances, "No instances available for " + serverName);

        List<ServiceInstance> aliveInstances = new ArrayList<ServiceInstance>();
        for (ServiceInstance serviceInstance : instances) {
            if (serviceInstance != null && serviceInstance.isAlive()) {
                aliveInstances.add(serviceInstance);
            }
        }
        checkIf(aliveInstances, "No instances alive for " + serverName);

        return doSelect(aliveInstances, serverZone);
    }

    private void checkIf(List<ServiceInstance> instances, String msg) {
        if (instances == null || instances.size() == 0) {
            throw new IllegalStateException(msg);
        }
    }

}
