//package org.framework.light.clients.http.netty;
//
//import io.netty.bootstrap.Bootstrap;
//import io.netty.channel.*;
//import io.netty.channel.nio.NioEventLoopGroup;
//import io.netty.channel.socket.nio.NioSocketChannel;
//import io.netty.handler.codec.http.DefaultFullHttpRequest;
//import io.netty.handler.codec.http.DefaultHttpRequest;
//import io.netty.handler.codec.http.HttpMethod;
//import io.netty.handler.codec.http.HttpVersion;
//import io.netty.handler.codec.http.multipart.DefaultHttpDataFactory;
//import io.netty.handler.codec.http.multipart.HttpDataFactory;
//import io.netty.handler.codec.http.multipart.HttpPostRequestEncoder;
//import io.netty.util.AttributeKey;
//import org.framework.light.clients.http.definition.HttpClientConfig;
//import org.framework.light.clients.http.definition.HttpClientException;
//import org.framework.light.clients.http.definition.HttpClientRequest;
//import org.framework.light.clients.http.definition.HttpClientResponse;
//import org.framework.light.clients.http.executor.HttpClientExecutor;
//
//import java.io.File;
//import java.net.ConnectException;
//import java.net.InetSocketAddress;
//import java.util.concurrent.TimeUnit;
//
///**
// * @Author: wangy
// * @Date: 2020/7/4 10:11
// * @Description:
// */
//public class NettyHttpClientExecutor extends HttpClientExecutor {
//
//    static AttributeKey<HttpClientResponseFuture> RESPONSE_FUTURE_ATTRIBUTE_KEY = AttributeKey.valueOf(HttpClientResponseFuture.class, "RESPONSE_FUTURE_ATTRIBUTE_KEY");
//    static AttributeKey<Boolean> HTTPS_ATTRIBUTE_KEY = AttributeKey.valueOf(Boolean.class, "HTTPS_ATTRIBUTE_KEY");
//
//    private Bootstrap bootstrap = new Bootstrap();
//    public NettyHttpClientExecutor() {
//        EventLoopGroup group = new NioEventLoopGroup();
//        bootstrap.group(group)
//                .channel(NioSocketChannel.class)
//                .option(ChannelOption.TCP_NODELAY, true)
//                .option(ChannelOption.SO_KEEPALIVE, true)
//                .handler(new HttpClientChannelInitializer());
//        // 连接超时设置
//        bootstrap.option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 3000);
//    }
//
//    @Override
//    public HttpClientResponse doExecuteRequest(HttpClientRequest httpRequest) throws Throwable {
//
//        HttpClientConfig clientConfig = httpRequest.getHttpClientConfig();
//
//        System.out.println("=== is https " + httpRequest.isHttps());
//        Channel channel = handshake(httpRequest);
//        System.out.println("=== channel " + channel);
//
//        HttpClientResponseFuture responseFuture = new HttpClientResponseFuture();
//        channel.attr(HTTPS_ATTRIBUTE_KEY).set(httpRequest.isHttps());
//        channel.attr(RESPONSE_FUTURE_ATTRIBUTE_KEY).set(responseFuture);
//        DefaultHttpRequest defaultHttpRequest = null;
//        if (clientConfig.isMultipart()) {
//            defaultHttpRequest = new DefaultHttpRequest(
//                    HttpVersion.HTTP_1_1, HttpMethod.POST, httpRequest.getUri());
//        } else {
//            defaultHttpRequest = new DefaultFullHttpRequest(
//                    HttpVersion.HTTP_1_1, HttpMethod.POST, httpRequest.getUri());
//        }
//
////        defaultHttpRequest.headers().set("Connection", "keep-alive");
////        defaultHttpRequest.headers().set("Accept", "text/plain, application/json, application/*+json, */*");
////        defaultHttpRequest.headers().set("Content-Type", "application/x-www-form-urlencoded; charset=UTF-8");
////        // defaultHttpRequest.headers().set("Host", "api.mch.weixin.qq.com");
////        //defaultHttpRequest.headers().set("X-Requested-With", "XMLHttpRequest");
////        defaultHttpRequest.headers().set("Cache-Control", "max-age=0");
////         defaultHttpRequest.headers().set("User-Agent", "Mozilla/5.0 (Windows NT 10.0; WOW64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/78.0.3904.108 Safari/537.36");
//
//
//
//        clientConfig.getParameterList();
//
//        // send request
//        channel.writeAndFlush(defaultHttpRequest);
//
//        HttpDataFactory factory = new DefaultHttpDataFactory(DefaultHttpDataFactory.MINSIZE);
//        File file = new File("e:/tmp/表格操作列事件设计原理.txt");
//
//        boolean multipart = true;
//
//        // post
//        HttpPostRequestEncoder bodyRequestEncoder = new HttpPostRequestEncoder(factory, defaultHttpRequest, multipart);
//        bodyRequestEncoder.addBodyAttribute("name","张三");
//        bodyRequestEncoder.addBodyFileUpload("file1", file, "application/x-zip-compressed", false);
//        bodyRequestEncoder.finalizeRequest();
//
//
//        if(channel.isActive() && channel.isWritable()) {
//            // channel.writeAndFlush(request2);
//            if (bodyRequestEncoder.isChunked()) {
//                channel.writeAndFlush(bodyRequestEncoder).awaitUninterruptibly();
//            }
//            bodyRequestEncoder.cleanFiles();
//        }
//
//
//        HttpClientResponse httpResponse = responseFuture.getHttpClientResponse(30000, TimeUnit.MILLISECONDS);
//
//        // 关闭socket
//        channel.closeFuture();
//
//        return httpResponse;
//
//    }
//
//    private Channel handshake(HttpClientRequest httpRequest) {
//        String host = httpRequest.getHost();
//        int port = httpRequest.getPort();
//        ChannelFuture channelFuture = null;
//        try {
//            channelFuture = bootstrap.connect(new InetSocketAddress(host, port)).sync();
//        } catch (Exception e) {
//            if (e instanceof ConnectTimeoutException || e instanceof ConnectException) {
//                throw new HttpClientException(e.getMessage());
//            }
//            throw new HttpClientException("Failed to create connection handshake", e);
//        }
//        return channelFuture.channel();
//    }
//
//}
