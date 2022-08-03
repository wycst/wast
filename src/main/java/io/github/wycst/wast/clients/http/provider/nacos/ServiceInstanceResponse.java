package io.github.wycst.wast.clients.http.provider.nacos;

import java.util.List;
import java.util.Map;

/**
 * @Author: wangy
 * @Date: 2021/8/7 20:03
 * @Description:
 */
public class ServiceInstanceResponse {

    private String name;
    private String groupName;
    private List<Map> hosts;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getGroupName() {
        return groupName;
    }

    public void setGroupName(String groupName) {
        this.groupName = groupName;
    }

    public List<Map> getHosts() {
        return hosts;
    }

    public void setHosts(List<Map> hosts) {
        this.hosts = hosts;
    }
}
