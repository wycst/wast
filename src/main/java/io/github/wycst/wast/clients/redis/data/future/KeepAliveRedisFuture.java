package io.github.wycst.wast.clients.redis.data.future;

/**
 * @Author: wangy
 * @Date: 2020/5/19 0:01
 * @Description:
 */
public class KeepAliveRedisFuture<E> extends RedisFuture<E> {

    public final boolean isKeepAlive() {
        return true;
    }
}
