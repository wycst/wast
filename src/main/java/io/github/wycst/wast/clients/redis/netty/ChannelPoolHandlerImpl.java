package io.github.wycst.wast.clients.redis.netty;

import io.netty.channel.Channel;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.pool.ChannelPoolHandler;
import io.netty.handler.codec.redis.RedisArrayAggregator;
import io.netty.handler.codec.redis.RedisBulkStringAggregator;
import io.netty.handler.codec.redis.RedisDecoder;
import io.netty.handler.codec.redis.RedisEncoder;
import io.netty.handler.timeout.IdleStateHandler;

import java.util.concurrent.TimeUnit;

/**
 * 连接池实现
 *
 * @Author: wangy
 * @Date: 2020/5/17 19:43
 * @Description:
 */
public class ChannelPoolHandlerImpl implements ChannelPoolHandler {

    public void channelReleased(Channel channel) throws Exception {
    }

    public void channelAcquired(Channel channel) throws Exception {
    }

    public void channelCreated(Channel channel) throws Exception {

        ChannelPipeline pipeline = channel.pipeline();
        pipeline.addLast(new IdleStateHandler(0, 0, 300, TimeUnit.SECONDS));
        pipeline.addLast(new RedisDecoder());
        pipeline.addLast(new RedisBulkStringAggregator());
        pipeline.addLast(new RedisArrayAggregator());
        pipeline.addLast(new RedisEncoder());
        pipeline.addLast(new RedisChannelHandler());

    }
}
