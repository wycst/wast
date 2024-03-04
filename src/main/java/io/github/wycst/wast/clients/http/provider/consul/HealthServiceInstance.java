package io.github.wycst.wast.clients.http.provider.consul;

/**
 * @Author wangyunchao
 * @Date 2023/4/22 13:51
 */
public class HealthServiceInstance {

    private ServiceInfo Service;

    public ServiceInfo getService() {
        return Service;
    }

    public void setService(ServiceInfo service) {
        Service = service;
    }
}
