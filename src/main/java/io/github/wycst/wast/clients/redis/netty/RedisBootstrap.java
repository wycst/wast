package io.github.wycst.wast.clients.redis.netty;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.redis.RedisArrayAggregator;
import io.netty.handler.codec.redis.RedisBulkStringAggregator;
import io.netty.handler.codec.redis.RedisDecoder;
import io.netty.handler.codec.redis.RedisEncoder;
import io.github.wycst.wast.clients.redis.exception.RedisConnectionException;

import java.net.InetSocketAddress;
import java.util.concurrent.TimeUnit;

/**
 * @Author: wangy
 * @Date: 2020/5/21 15:14
 * @Description:
 */
public class RedisBootstrap extends Bootstrap {

    private EventLoopGroup group;
    private static RedisBootstrap redisBootstrap;

    private RedisBootstrap() {
        group = new NioEventLoopGroup();
        group(group)
                .channel(NioSocketChannel.class)
                .option(ChannelOption.TCP_NODELAY, true)
                .option(ChannelOption.SO_KEEPALIVE, true)
                .handler(new ChannelInitializer<Channel>() {
                    protected void initChannel(Channel channel) throws Exception {
                        ChannelPipeline pipeline = channel.pipeline();
                        pipeline.addLast(new RedisDecoder());
                        pipeline.addLast(new RedisBulkStringAggregator());
                        pipeline.addLast(new RedisArrayAggregator());
                        pipeline.addLast(new RedisEncoder());
                        pipeline.addLast(new RedisChannelHandler());
                    }
                });
    }

    public synchronized static RedisBootstrap bootstrap() {
        if (redisBootstrap == null) {
            redisBootstrap = new RedisBootstrap();
        }
        return redisBootstrap;
    }

    public Channel channel(String host, int port) {
        return channel(host, port, 0);
    }

    public Channel channel(String host, int port, long timeout) {
        try {
            ChannelFuture channelFuture = connect(new InetSocketAddress(host, port));
            if (timeout >= 200) {
                channelFuture.get(timeout, TimeUnit.MILLISECONDS);
                return channelFuture.channel();
            } else {
                return channelFuture.sync().channel();
            }
        } catch (Throwable e) {
            throw new RedisConnectionException(e.getMessage(), e);
        }
    }

    public void shutdown() {
        group.shutdownGracefully();
        redisBootstrap = null;
    }
}
