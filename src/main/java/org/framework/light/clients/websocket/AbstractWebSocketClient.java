package org.framework.light.clients.websocket;

import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.handler.codec.http.websocketx.*;

/**
 * @Author: wangy
 * @Date: 2022/3/13 12:14
 * @Description:
 */
abstract class AbstractWebSocketClient {

    private final Channel channel;
    private boolean closed;

    AbstractWebSocketClient(Channel channel) {
        this.channel = channel;
    }

    private void checkChannelIfClosed() {
        if (this.closed) {
            throw new WebSocketException("WebSocketClient is closed ");
        }
    }

    /***
     * 关闭连接
     *
     */
    public void close(String resion) {
        this.checkChannelIfClosed();
        channel.writeAndFlush(new CloseWebSocketFrame(1000, resion));
        try {
            channel.closeFuture().sync();
            this.closed = true;
        } catch (InterruptedException e) {
        } finally {
        }
    }

    /***
     * 关闭连接
     *
     */
    public void close() {
        this.checkChannelIfClosed();
        channel.writeAndFlush(new CloseWebSocketFrame());
        try {
            channel.closeFuture().sync();
            this.closed = true;
        } catch (InterruptedException e) {
        } finally {
        }
    }

    /**
     * 进行ping操作
     *
     * @return
     */
    public ChannelFuture ping() {
        this.checkChannelIfClosed();
        WebSocketFrame frame = new PingWebSocketFrame(Unpooled.wrappedBuffer(new byte[]{8, 1, 8, 1}));
        return channel.writeAndFlush(frame);
    }

    /**
     * 发送文本
     *
     * @param text
     * @return
     */
    public ChannelFuture sendText(String text) {
        this.checkChannelIfClosed();
        WebSocketFrame frame = new TextWebSocketFrame(text);
        return channel.writeAndFlush(frame);
    }

    /**
     * 发送二进制数据
     *
     * @param data
     * @return
     */
    public ChannelFuture sendBinary(byte[] data) {
        this.checkChannelIfClosed();
        BinaryWebSocketFrame binaryWebSocketFrame = new BinaryWebSocketFrame(Unpooled.wrappedBuffer(data));
        return channel.writeAndFlush(binaryWebSocketFrame);
    }
}
