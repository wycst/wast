package io.github.wycst.wast.clients.redis.connection;

import io.netty.channel.Channel;
import io.netty.channel.pool.ChannelPool;

import java.util.concurrent.CountDownLatch;

/**
 * @Author: wangy
 * @Date: 2020/5/21 13:54
 * @Description:
 */
class PooledRedisConnection extends RedisConnection {

    private CountDownLatch nextCountDownLatch;
    private ChannelPool channelPool;

    PooledRedisConnection(Channel channel, boolean await) {
        super(channel, await);
    }

    public final boolean recycleable() {
        return !recycled();
    }

    private boolean recycled() {
        return isClosed();
    }

    void setNextCountDownLatch(CountDownLatch nextCountDownLatch) {
        this.nextCountDownLatch = nextCountDownLatch;
    }

    void setChannelPool(ChannelPool channelPool) {
        this.channelPool = channelPool;
    }

    @Override
    protected void afterQueueResponse() {
        super.afterQueueResponse();
        if(this.nextCountDownLatch != null)
            this.nextCountDownLatch.countDown();

    }

    public void close() {
        Channel channel = getChannel();
        if(channel.isActive()) {
            this.channelPool.release(channel);
        } else {
            channel.close();
        }
        this.nextCountDownLatch = null;
        this.channelPool = null;
        super.close();
    }
}
