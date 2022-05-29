package org.framework.light.clients.redis.data.future;

/**
 * @Author: wangy
 * @Date: 2020/5/18 17:15
 * @Description:
 */
public class ResultRedisFuture<E> extends RedisFuture {

    public boolean isKeepAlive() {
        return false;
    }
}
