package io.github.wycst.wast.clients.http.provider.nacos;

import java.util.List;

/**
 * @Author: wangy
 * @Date: 2021/8/7 19:22
 * @Description:
 */
public class ServiceListResponse {

    private int count;
    private List<String> doms;

    public int getCount() {
        return count;
    }

    public void setCount(int count) {
        this.count = count;
    }

    public List<String> getDoms() {
        return doms;
    }

    public void setDoms(List<String> doms) {
        this.doms = doms;
    }
}
