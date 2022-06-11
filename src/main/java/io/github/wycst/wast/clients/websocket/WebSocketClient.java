package io.github.wycst.wast.clients.websocket;

import io.netty.channel.Channel;

public final class WebSocketClient extends AbstractWebSocketClient {

    WebSocketClient(Channel channel) {
        super(channel);
    }

    public static WebSocketClient create(String websocketURL, String subprotocol, WebSocketEventHandler eventHandler) {
        return WebSocketClientFactory.createWebSocketClient(websocketURL, subprotocol, eventHandler);
    }

    public static WebSocketClient create(String websocketURL, WebSocketEventHandler eventHandler) {
        return WebSocketClientFactory.createWebSocketClient(websocketURL, eventHandler);
    }
}