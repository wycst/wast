package org.framework.light.clients.http.provider;

/**
 * @Author: wangy
 * @Date: 2020/7/10 15:16
 * @Description:
 */
public class ServiceInstance {

    private String serviceName;
    private String baseUrl;
    private boolean alive;

    public String getServiceName() {
        return serviceName;
    }

    public void setServiceName(String serviceName) {
        this.serviceName = serviceName;
    }

    public void setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    public String getBaseUrl() {
        return baseUrl;
    }

    public boolean isAlive() {
        return alive;
    }

    public void  setAlive(boolean alive) {
        this.alive = alive;
    }
}
