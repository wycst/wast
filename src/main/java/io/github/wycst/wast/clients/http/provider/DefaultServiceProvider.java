package io.github.wycst.wast.clients.http.provider;

import io.github.wycst.wast.clients.http.definition.HttpClientException;
import io.github.wycst.wast.clients.http.definition.HttpClientRequest;
import io.github.wycst.wast.clients.http.loadbalance.LoadBalanceStrategy;
import io.github.wycst.wast.clients.http.loadbalance.PollingLoadBalanceStrategy;
import io.github.wycst.wast.clients.http.provider.ServerZone;
import io.github.wycst.wast.clients.http.provider.ServiceInstance;
import io.github.wycst.wast.clients.http.provider.ServiceProvider;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.*;

/**
 * @Author: wangy
 * @Date: 2020/7/10 15:14
 * @Description:
 */
public class DefaultServiceProvider implements ServiceProvider {

    private final Map<String, ServerZone> servers;

    // 负载规则
    private LoadBalanceStrategy loadBalanceStrategy;

    public DefaultServiceProvider() {
        this(new LinkedHashMap<String, ServerZone>());
    }

    public DefaultServiceProvider(Map<String, ServerZone> servers) {
        if (servers == null) {
            throw new HttpClientException(" servers is null ");
        }
        this.servers = servers;
        this.loadBalanceStrategy = new PollingLoadBalanceStrategy();
    }

    public final void setLoadBalanceStrategy(LoadBalanceStrategy loadBalanceStrategy) {
        this.loadBalanceStrategy = loadBalanceStrategy;
    }

    @Override
    public final void registerServer(ServerZone serverZone) {
        servers.put(serverZone.getServerName(), serverZone);
    }

    @Override
    public final ServiceInstance getServiceInstance(HttpClientRequest httpRequest) throws MalformedURLException {
        URL url = httpRequest.getURL();
        if (!httpRequest.isUseDefaultPort()) {
            return null;
        }
        String hostname = url.getHost();
        if (ifExist(hostname)) {
            ServiceInstance serviceInstance = loadBalanceStrategy.select(getServer(hostname));
            return serviceInstance;
        }
        return null;
    }

    @Override
    public final ServerZone getServer(String serviceName) {
        return servers.get(serviceName);
    }

    @Override
    public final void clear() {
        servers.clear();
    }

    @Override
    public final void clearIfNotExist(Collection<String> doms) {
        List<String> serviceList = new ArrayList<String>(servers.keySet());
        for (String serviceName : serviceList) {
            if (!doms.contains(serviceName)) {
                ServerZone serverZone = servers.get(serviceName);
                if (!serverZone.isStaticServer()) {
                    servers.remove(serviceName);
                }
            }
        }
    }

    private boolean ifExist(String serverName) {
        return servers.containsKey(serverName);
    }

    public void destroy() {
        this.clear();
    }
}
