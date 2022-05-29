//package org.framework.light.clients.http.netty;
//
//import io.netty.channel.ChannelInitializer;
//import io.netty.channel.ChannelPipeline;
//import io.netty.channel.socket.SocketChannel;
//import io.netty.handler.codec.http.HttpClientCodec;
//import io.netty.handler.codec.http.HttpObjectAggregator;
//import io.netty.handler.ssl.SslContext;
//import io.netty.handler.ssl.SslContextBuilder;
//import io.netty.handler.ssl.util.InsecureTrustManagerFactory;
//import io.netty.handler.stream.ChunkedWriteHandler;
//
///**
// * @Author: wangy
// * @Date: 2020/7/2 17:22
// * @Description:
// */
//public class HttpClientChannelInitializer extends ChannelInitializer<SocketChannel> {
//
//    private HttpClientHandler clientHandler = new HttpClientHandler();
//
//    public HttpClientChannelInitializer() {
//    }
//
//
//    @Override
//    protected void initChannel(SocketChannel socketChannel) throws Exception {
//
//        ChannelPipeline pipeline = socketChannel.pipeline();
//
//        SslContext sslContext = SslContextBuilder.forClient().trustManager(InsecureTrustManagerFactory.INSTANCE).build();
//        pipeline.addLast(sslContext.newHandler(socketChannel.alloc()));
//
//        pipeline.addLast(new HttpClientCodec());
//        pipeline.addLast(new ChunkedWriteHandler());
//        // pipeline.addLast("aggregator",new HttpObjectAggregator(512 * 1024));
//        pipeline.addLast(new HttpObjectAggregator(512000 * 1024));
//        // httpServerHandler
//        pipeline.addLast(clientHandler);
//
//    }
//
//
//}
