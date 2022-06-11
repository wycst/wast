package io.github.wycst.wast.clients.redis.connection;

import io.github.wycst.wast.clients.redis.netty.RedisChannelPool;
import io.netty.channel.Channel;
import io.netty.util.concurrent.Future;

/**
 * tcp抽象连接池
 *
 * @Author: wangy
 * @Date: 2020/5/19 19:38
 * @Description:
 */
abstract class AbstractConnectonPool {

    private RedisChannelPool redisChannelPool;

    public final RedisChannelPool getRedisChannelPool() {
        return redisChannelPool;
    }

    protected AbstractConnectonPool(RedisChannelPool channelPool) {
        this.redisChannelPool = channelPool;
    }

    protected final Future<Channel> acquire() {
        return redisChannelPool.acquire();
    }

    public void close() {
        redisChannelPool.close();
    }

}
