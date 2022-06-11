package io.github.wycst.wast.clients.http.impl;

import io.github.wycst.wast.clients.http.definition.HttpClientException;
import io.github.wycst.wast.clients.http.definition.HttpClientRequest;
import io.github.wycst.wast.clients.http.loadbalance.LoadBalanceStrategy;
import io.github.wycst.wast.clients.http.loadbalance.PollingLoadBalanceStrategy;
import io.github.wycst.wast.clients.http.provider.ServerZone;
import io.github.wycst.wast.clients.http.provider.ServiceInstance;
import io.github.wycst.wast.clients.http.provider.ServiceProvider;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

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

    public void setLoadBalanceStrategy(LoadBalanceStrategy loadBalanceStrategy) {
        this.loadBalanceStrategy = loadBalanceStrategy;
    }

    @Override
    public void registerServer(ServerZone serverZone) {
        servers.put(serverZone.getServerName(), serverZone);
    }

//    @Override
//    public URL fromRequest(HttpClientRequest httpRequest) throws MalformedURLException {
//        // Resolving domain name information
//
//        URL url = httpRequest.getURL();
//        if (!httpRequest.isUseDefaultPort()) {
//            return url;
//        }
//        String hostname = url.getHost();
//        if (ifExist(hostname)) {
//            ServiceInstance serviceInstance = loadBalanceStrategy.select(getServer(hostname));
//            String baseUrl = serviceInstance.getBaseUrl();
//            String newUrl = url.getProtocol() + "://" + baseUrl + url.getFile();
//            return new URL(newUrl);
//        }
//
//        return httpRequest.getURL();
//    }

    @Override
    public ServiceInstance getServiceInstance(HttpClientRequest httpRequest) throws MalformedURLException {
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
    public ServerZone getServer(String serviceName) {
        return servers.get(serviceName);
    }

    @Override
    public void clear() {
        servers.clear();
    }

    @Override
    public void clearIfNotExist(List<String> doms) {
        List<String> serviceList = new ArrayList<String>(servers.keySet());
        for (String serviceName : serviceList) {
            if(!doms.contains(serviceName)) {
                ServerZone serverZone = servers.get(serviceName);
                if(!serverZone.isStaticServer()) {
                    servers.remove(serviceName);
                }
            }
        }
    }

    private boolean ifExist(String serverName) {
        return servers.containsKey(serverName);
    }

}
