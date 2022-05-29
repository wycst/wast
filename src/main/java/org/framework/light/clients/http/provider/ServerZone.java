package org.framework.light.clients.http.provider;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * @Author: wangy
 * @Date: 2020/8/25 15:34
 * @Description:
 */
public class ServerZone {

    private String serverName;
    private List<ServiceInstance> serviceInstances;
    private int current;
    private boolean staticServer;

    public ServerZone(String serverName, String[] urls) {
        this(serverName, urls, false);
    }

    public ServerZone(String serverName, String[] urls, boolean staticServer) {
        this(serverName, Arrays.asList(urls), staticServer);
    }

    public ServerZone(String serverName, List<String> urls) {
        this(serverName, urls, false);
    }

    public ServerZone(String serverName, List<String> urls, boolean staticServer) {
        this.serverName = serverName;
        this.staticServer = staticServer;
        this.serviceInstances = new ArrayList<ServiceInstance>();
        for (String baseUrl : urls) {
            ServiceInstance serviceInstance = new ServiceInstance();
            serviceInstance.setServiceName(serverName);
            serviceInstance.setBaseUrl(baseUrl);
            serviceInstance.setAlive(true);
            serviceInstances.add(serviceInstance);
        }
    }

    public String getServerName() {
        return serverName;
    }

    public void setServerName(String serverName) {
        this.serverName = serverName;
    }

    public List<ServiceInstance> getServiceInstances() {
        return serviceInstances;
    }

    public void setServiceInstances(List<ServiceInstance> serviceInstances) {
        this.serviceInstances = serviceInstances;
    }

    public synchronized int nextIndex(int capacity) {
        current++;
        if (current >= capacity) {
            current = 0;
        }
        return current;
    }

    public boolean isStaticServer() {
        return staticServer;
    }

    public void setStaticServer(boolean staticServer) {
        this.staticServer = staticServer;
    }
}
