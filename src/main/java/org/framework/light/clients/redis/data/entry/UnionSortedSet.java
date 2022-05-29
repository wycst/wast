package org.framework.light.clients.redis.data.entry;

/**
 * @Author: wangy
 * @Date: 2020/6/29 14:29
 * @Description:
 */
public class UnionSortedSet {

    private String key;
    private double weight = 1;

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public double getWeight() {
        return weight;
    }

    public void setWeight(double weight) {
        this.weight = weight;
    }
}
