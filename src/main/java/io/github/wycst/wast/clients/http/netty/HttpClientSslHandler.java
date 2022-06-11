//package io.github.wycst.wast.clients.http.netty;
//
//import io.netty.channel.ChannelHandlerContext;
//import io.netty.channel.ChannelInboundHandlerAdapter;
//import io.netty.handler.ssl.SslHandler;
//
//import static io.github.wycst.wast.clients.http.netty.NettyHttpClientExecutor.HTTPS_ATTRIBUTE_KEY;
//
///**
// * @Author: wangy
// * @Date: 2020/7/3 17:57
// * @Description:
// */
//public class HttpClientSslHandler extends ChannelInboundHandlerAdapter {
//
//    @Override
//    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
//        Boolean https = ctx.channel().attr(HTTPS_ATTRIBUTE_KEY).get();
//        if(https != Boolean.TRUE) {
//            SslHandler sslHandler = ctx.pipeline().get(SslHandler.class);
//            if(sslHandler != null) {
//                ctx.pipeline().remove(sslHandler);
//            }
//        }
//        ctx.fireChannelRead(msg);
//    }
//}
