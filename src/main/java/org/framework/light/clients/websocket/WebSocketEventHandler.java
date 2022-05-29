package org.framework.light.clients.websocket;

/**
 * @Author: wangy
 * @Date: 2022/3/12 23:05
 * @Description:
 */
public abstract class WebSocketEventHandler {

    /***
     * call by message
     *
     * @param event
     */
    public abstract void onMessage(WebSocketEvent event);

    public void onOpen(WebSocketEvent event) {
    }

    public void onClose(WebSocketEvent event) {
    }

    public void onError(WebSocketEvent event) {
    }

    public void onPong(WebSocketEvent event) {
    }
}
