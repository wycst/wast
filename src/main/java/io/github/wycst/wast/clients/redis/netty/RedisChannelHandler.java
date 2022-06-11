package io.github.wycst.wast.clients.redis.netty;

import io.netty.channel.*;
import io.netty.handler.codec.redis.*;
import io.netty.handler.timeout.IdleStateEvent;
import io.netty.util.CharsetUtil;
import io.netty.util.ReferenceCountUtil;
import io.github.wycst.wast.clients.redis.connection.RedisConnection;
import io.github.wycst.wast.clients.redis.connection.RedisConnectionHandler;

import java.util.ArrayList;

public class RedisChannelHandler extends ChannelDuplexHandler {


    // 发送 redis 命令
    @Override
    public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) {
//        String[] commands = ((String) msg).split("\\s+");
//        List<RedisMessage> children = new ArrayList<RedisMessage>(commands.length);
//        for (String cmdString : commands) {
//            children.add(new FullBulkStringRedisMessage(ByteBufUtil.writeUtf8(ctx.alloc(), cmdString)));
//        }
//        RedisMessage request = new ArrayRedisMessage(children);
//        ctx.write(request, promise);

        if (msg instanceof String) {
            InlineCommandRedisMessage redisMessage = new InlineCommandRedisMessage((String) msg);
            ctx.write(redisMessage, promise);
        } else {
            ctx.fireChannelRead(msg);
        }

    }

    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
        if (evt instanceof IdleStateEvent) {
            Channel channel = ctx.channel();
            RedisConnection connection = getBindRedisConnection(channel);
            if(connection != null && !connection.isClosed()) {
                RedisConnectionHandler.safeclose(connection);
            }
        } else {
            super.userEventTriggered(ctx, evt);
        }
    }

    // 接收 redis 响应数据
    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) {
        RedisMessage redisMessage = (RedisMessage) msg;
        try {
            // 打印响应消息
            Object result = parseRedisResponse(redisMessage);
            Channel channel = ctx.channel();
            RedisConnection redisConnection = getBindRedisConnection(channel);
            RedisConnectionHandler.handleQueueResponse(redisConnection, result);
        } finally {
            // 释放资源
            ReferenceCountUtil.release(msg);
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        forceclose(ctx);
        cause.printStackTrace();
        ctx.close();
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        forceclose(ctx);
        super.channelInactive(ctx);
    }

    private void forceclose(ChannelHandlerContext ctx) {
        Channel channel = ctx.channel();
        RedisConnection redisConnection = getBindRedisConnection(channel);
        if(redisConnection != null) {
            redisConnection.close();
        } else {
            channel.close();
        }
    }


    private Object parseRedisResponse(RedisMessage msg) {

        Object result = null;
        if (msg instanceof SimpleStringRedisMessage) {
            result = ((SimpleStringRedisMessage) msg).content();
        } else if (msg instanceof ErrorRedisMessage) {
            result = ((ErrorRedisMessage) msg).content();
        } else if (msg instanceof IntegerRedisMessage) {
            result = ((IntegerRedisMessage) msg).value();
        } else if (msg instanceof FullBulkStringRedisMessage) {
            result = getString((FullBulkStringRedisMessage) msg);
        } else if (msg instanceof ArrayRedisMessage) {
            ArrayList msgs = new ArrayList<Object>();
            for (RedisMessage child : ((ArrayRedisMessage) msg).children()) {
                msgs.add(parseRedisResponse(child));
            }
            result = msgs;
        } else {
            // throw new CodecException("unknown message type: " + msg);
        }
        return result;
    }

    private static RedisConnection getBindRedisConnection(Channel channel) {
        return channel.attr(RedisConnection.REDIS_CONNECTION_ATTRIBUTE_KEY).get();
    }

    private static String getString(FullBulkStringRedisMessage msg) {
        if (msg.isNull()) {
//            return "(null)";
            return null;
        }
        return msg.content().toString(CharsetUtil.UTF_8);
    }

}