package org.framework.light.clients.redis.netty;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoop;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.pool.ChannelPool;
import io.netty.channel.pool.FixedChannelPool;
import io.netty.channel.pool.SimpleChannelPool;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.Promise;
import org.framework.light.clients.redis.conf.RedisConfig;

import java.net.InetSocketAddress;

/**
 * @Author: wangy
 * @Date: 2020/5/19 22:29
 * @Description:
 */
public class RedisChannelPool implements ChannelPool {

    private ChannelPool channelPool;
    // 事件循环
    private EventLoopGroup group;
    // redis 配置
    private RedisConfig redisConfig;

    public RedisChannelPool(RedisConfig redisConfig) {
        group = new NioEventLoopGroup();
        Bootstrap bootstrap = new Bootstrap();
        bootstrap.group(group)
                .channel(NioSocketChannel.class)
                .option(ChannelOption.TCP_NODELAY, true)
                .option(ChannelOption.SO_KEEPALIVE, true)
                .remoteAddress(new InetSocketAddress(redisConfig.getHost(), redisConfig.getPort()));
        this.redisConfig = redisConfig;
        this.channelPool = new FixedChannelPool(bootstrap, new ChannelPoolHandlerImpl(), redisConfig.getPoolMaxActive());
    }

    public RedisConfig getRedisConfig() {
        return redisConfig;
    }

    public Future<Channel> acquire() {
        return channelPool.acquire();
    }

    public Future<Channel> acquire(Promise<Channel> promise) {
        return channelPool.acquire(promise);
    }

    public Future<Void> release(Channel channel) {
        return channelPool.release(channel);
    }

    public Future<Void> release(Channel channel, Promise<Void> promise) {
        return channelPool.release(channel,promise);
    }

    public void close() {
        // 关闭连接池(异步)
        channelPool.close();
        try {
            Thread.sleep(200);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        // 关闭事件循环
        group.shutdownGracefully();
    }
}
