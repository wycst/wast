package io.github.wycst.wast.clients.websocket;

/*
 * Copyright 2012 The Netty Project
 *
 * The Netty Project licenses this file to you under the Apache License,
 * version 2.0 (the "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at:
 *
 *   https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations
 * under the License.
 */
//The MIT License
//
//Copyright (c) 2009 Carl Bystršm
//
//Permission is hereby granted, free of charge, to any person obtaining a copy
//of this software and associated documentation files (the "Software"), to deal
//in the Software without restriction, including without limitation the rights
//to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
//copies of the Software, and to permit persons to whom the Software is
//furnished to do so, subject to the following conditions:
//
//The above copyright notice and this permission notice shall be included in
//all copies or substantial portions of the Software.
//
//THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
//IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
//FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
//AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
//LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
//OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
//THE SOFTWARE.

import io.netty.buffer.ByteBuf;
import io.netty.channel.*;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.websocketx.*;
import io.netty.util.CharsetUtil;

public class WebSocketClientHandler extends SimpleChannelInboundHandler<Object> {

    private final WebSocketClientHandshaker handshaker;
    private ChannelPromise handshakeFuture;
    private final WebSocketEventHandler eventHandler;

    public WebSocketClientHandler(WebSocketClientHandshaker handshaker, WebSocketEventHandler eventHandler) {
        this.handshaker = handshaker;
        this.eventHandler = eventHandler;
    }

    public ChannelFuture handshakeFuture() {
        return handshakeFuture;
    }

    @Override
    public void handlerAdded(ChannelHandlerContext ctx) {
        handshakeFuture = ctx.newPromise();
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) {
        handshaker.handshake(ctx.channel());
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) {
        /** 客户端主动关闭 */
        //  eventHandler.onClose(WebSocketEvent.closeEvent("close "));
    }

    @Override
    public void channelRead0(ChannelHandlerContext ctx, Object msg) throws Exception {
        Channel ch = ctx.channel();
        if (!handshaker.isHandshakeComplete()) {
            try {
                handshaker.finishHandshake(ch, (FullHttpResponse) msg);
                handshakeFuture.setSuccess();
                eventHandler.onOpen(WebSocketEvent.openEvent());
            } catch (WebSocketHandshakeException e) {
                handshakeFuture.setFailure(e);
                eventHandler.onError(WebSocketEvent.errorEvent(e));
            }
            return;
        }

        if (msg instanceof FullHttpResponse) {
            FullHttpResponse response = (FullHttpResponse) msg;
            throw new IllegalStateException(
                    "Unexpected FullHttpResponse (getStatus=" + response.status() +
                            ", content=" + response.content().toString(CharsetUtil.UTF_8) + ')');
        }

        WebSocketFrame frame = (WebSocketFrame) msg;
        if (frame instanceof TextWebSocketFrame) {
            byte[] message = getBytes(frame);
            eventHandler.onMessage(WebSocketEvent.messageEvent(message, WebSocketEvent.WebSocketContentType.Text));
        } else if (frame instanceof BinaryWebSocketFrame) {
            byte[] message = getBytes(frame);
            eventHandler.onMessage(WebSocketEvent.messageEvent(message, WebSocketEvent.WebSocketContentType.Binary));
        } else if (frame instanceof PongWebSocketFrame) {
            eventHandler.onPong(WebSocketEvent.pongEvent());
        } else if (frame instanceof CloseWebSocketFrame) {
            ch.close();
            eventHandler.onClose(WebSocketEvent.closeEvent(((CloseWebSocketFrame) frame).reasonText()));
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        cause.printStackTrace();
        if (!handshakeFuture.isDone()) {
            handshakeFuture.setFailure(cause);
            eventHandler.onError(WebSocketEvent.errorEvent(cause));
        }
        ctx.close();
    }

    private byte[] getBytes(WebSocketFrame webSocketFrame) {
        ByteBuf byteBuf = webSocketFrame.content();
        if (byteBuf.hasArray()) {
            return byteBuf.array();
        } else {
            byte[] content = new byte[byteBuf.readableBytes()];
            byteBuf.getBytes(byteBuf.readerIndex(), content);
            return content;
        }
    }

}