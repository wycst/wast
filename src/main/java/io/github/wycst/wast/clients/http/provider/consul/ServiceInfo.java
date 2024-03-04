package io.github.wycst.wast.clients.http.provider.consul;

/**
 * @Author wangyunchao
 * @Date 2023/4/22 13:50
 */
public class ServiceInfo {

    private String Address;
    private int Port;

    public String getAddress() {
        return Address;
    }

    public void setAddress(String address) {
        Address = address;
    }

    public int getPort() {
        return Port;
    }

    public void setPort(int port) {
        Port = port;
    }
}
