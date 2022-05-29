//package org.framework.light.clients.http.netty;
//
//import org.framework.light.clients.http.impl.HttpClientResponseImpl;
//import io.netty.buffer.ByteBuf;
//import io.netty.channel.Channel;
//import io.netty.channel.ChannelHandler;
//import io.netty.channel.ChannelHandlerContext;
//import io.netty.channel.ChannelInboundHandlerAdapter;
//import io.netty.handler.codec.http.FullHttpResponse;
//import io.netty.handler.codec.http.HttpResponse;
//import io.netty.handler.codec.http.HttpResponseStatus;
//import io.netty.util.CharsetUtil;
//
//import static org.framework.light.clients.http.netty.NettyHttpClientExecutor.RESPONSE_FUTURE_ATTRIBUTE_KEY;
//
//
//@ChannelHandler.Sharable
//public class HttpClientHandler extends ChannelInboundHandlerAdapter {
//
//    public HttpClientHandler() {
//    }
//
//    @Override
//    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
//
//        Channel channel = ctx.channel();
//        HttpClientResponseFuture responseFuture = channel.attr(RESPONSE_FUTURE_ATTRIBUTE_KEY).get();
//        if (responseFuture == null) {
//            ctx.fireChannelRead(msg);
//            return;
//        }
//        if (msg instanceof HttpResponse) {
//            HttpResponse httpResponse = (HttpResponse) msg;
//            HttpResponseStatus responseStatus = httpResponse.status();
//            int statusCode = responseStatus.code();
//            String reasonPhrase = responseStatus.reasonPhrase();
//            String content = null;
//            if (httpResponse instanceof FullHttpResponse) {
//                FullHttpResponse fullHttpResponse = (FullHttpResponse) httpResponse;
//                ByteBuf byteBuf = fullHttpResponse.content();
//                content = byteBuf.toString(CharsetUtil.UTF_8);
//            }
//            System.out.println("=========== content: ");
//            System.out.println(content);
//
//            responseFuture.setHttpResponse(new HttpClientResponseImpl(statusCode, reasonPhrase, content.getBytes()));
//
//        } else {
//            ctx.fireChannelRead(msg);
//        }
//    }
//
//
//    @Override
//    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
//        System.out.println("======= error ");
//        cause.printStackTrace();
//        ctx.close();
//    }
//
//
//}
