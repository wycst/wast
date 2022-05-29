package org.framework.light.clients.websocket;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.http.DefaultHttpHeaders;
import io.netty.handler.codec.http.HttpClientCodec;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.websocketx.WebSocketClientHandshakerFactory;
import io.netty.handler.codec.http.websocketx.WebSocketVersion;
import io.netty.handler.codec.http.websocketx.extensions.compression.WebSocketClientCompressionHandler;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.ssl.util.InsecureTrustManagerFactory;

import java.net.URI;

/**
 * websocket 客户端构建工厂
 *
 * @Author: wangy
 * @Date: 2022/3/13 15:09
 * @Description:
 */
public class WebSocketClientFactory {

    private static Bootstrap bootstrap;
    private static EventLoopGroup group;
    private static boolean shutdown;

    static {
        init();
    }

    private static void init() {
        bootstrap = new Bootstrap();
        group = new NioEventLoopGroup();
        bootstrap.group(group).channel(NioSocketChannel.class);
    }

    public static WebSocketClient createWebSocketClient(String websocketURL, String subprotocol, WebSocketEventHandler eventHandler) {

        try {
            URI uri = new URI(websocketURL);
            String scheme = uri.getScheme() == null ? "ws" : uri.getScheme();
            final String host = uri.getHost() == null ? "127.0.0.1" : uri.getHost();
            final int port;
            if (uri.getPort() == -1) {
                if ("ws".equalsIgnoreCase(scheme)) {
                    port = 80;
                } else if ("wss".equalsIgnoreCase(scheme)) {
                    port = 443;
                } else {
                    port = -1;
                }
            } else {
                port = uri.getPort();
            }

            if (!"ws".equalsIgnoreCase(scheme) && !"wss".equalsIgnoreCase(scheme)) {
                throw new WebSocketException("Only WS(S) is supported ");
            }

            final boolean ssl = "wss".equalsIgnoreCase(scheme);
            final SslContext sslCtx;
            if (ssl) {
                sslCtx = SslContextBuilder.forClient()
                        .trustManager(InsecureTrustManagerFactory.INSTANCE).build();
            } else {
                sslCtx = null;
            }

            // Connect with V13 (RFC 6455 aka HyBi-17). You can change it to V08 or V00.
            // If you change it to V00, ping is not supported and remember to change
            // HttpResponseDecoder to WebSocketHttpResponseDecoder in the pipeline.
            final WebSocketClientHandler handler =
                    new WebSocketClientHandler(
                            WebSocketClientHandshakerFactory.newHandshaker(
                                    uri, WebSocketVersion.V13, subprotocol, true, new DefaultHttpHeaders()), eventHandler);
            if (shutdown) {
                synchronized (bootstrap) {
                    if (shutdown) {
                        init();
                        shutdown = false;
                    }
                }
            }
            bootstrap.handler(new ChannelInitializer<SocketChannel>() {
                @Override
                protected void initChannel(SocketChannel ch) {
                    ChannelPipeline p = ch.pipeline();
                    if (sslCtx != null) {
                        p.addLast(sslCtx.newHandler(ch.alloc(), host, port));
                    }
                    p.addLast(
                            new HttpClientCodec(),
                            new HttpObjectAggregator(8192),
                            WebSocketClientCompressionHandler.INSTANCE,
                            handler);
                }
            });

            Channel channel = bootstrap.connect(uri.getHost(), port).sync().channel();
            handler.handshakeFuture().sync();
            return new WebSocketClient(channel);
        } catch (Throwable e) {
            throw new WebSocketException(e);
        } finally {
        }
    }

    public static WebSocketClient createWebSocketClient(String websocketURL, WebSocketEventHandler eventHandler) {
        return createWebSocketClient(websocketURL, null, eventHandler);
    }

    /**
     * 关闭组
     */
    public static void shutdown() {
        group.shutdownGracefully();
        shutdown = true;
    }
}
