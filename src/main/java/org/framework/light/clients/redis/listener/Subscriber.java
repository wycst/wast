package org.framework.light.clients.redis.listener;

import org.framework.light.clients.redis.exception.RedisInvalidException;

import java.util.List;

/**
 * @Author: wangy
 * @Date: 2020/5/29 16:33
 * @Description:
 */
public abstract class Subscriber {

    private String[] channels;

    public Subscriber(String... channels) {
        if (channels.length == 0) {
            throw new RedisInvalidException(" subscribe channels cannot be empty .");
        }
        this.channels = channels;
    }

//    public void ok(List<String> messages) {
//
//    }

    public abstract void onMessage(String channal, String topic, String message);

    public abstract void onError(String error);

    public String[] getChannels() {
        return channels;
    }

}
