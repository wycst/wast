package org.framework.light.clients.websocket;

/**
 * @Author: wangy
 * @Date: 2022/3/12 23:06
 * @Description:
 */
public class WebSocketEvent {

    private final WebSocketEventType type;
    private byte[] message;
    private Throwable throwable;
    private String reason;
    private WebSocketContentType contentType = WebSocketContentType.Text;

    WebSocketEvent(WebSocketEventType type) {
        this.type = type;
    }

    static WebSocketEvent openEvent() {
        return new WebSocketEvent(WebSocketEventType.Open);
    }

    static WebSocketEvent messageEvent(byte[] message, WebSocketContentType contentType) {
        WebSocketEvent webSocketEvent = new WebSocketEvent(WebSocketEventType.Message);
        webSocketEvent.message = message;
        webSocketEvent.contentType = contentType;
        return webSocketEvent;
    }

    public String getText() {
        if (contentType == WebSocketContentType.Text) {
            return new String(message);
        }
        return null;
    }

    public byte[] getMessage() {
        return message;
    }

    static WebSocketEvent pongEvent() {
        WebSocketEvent webSocketEvent = new WebSocketEvent(WebSocketEventType.Pong);
        return webSocketEvent;
    }

    static WebSocketEvent closeEvent(String reason) {
        WebSocketEvent webSocketEvent = new WebSocketEvent(WebSocketEventType.Close);
        webSocketEvent.reason = reason;
        return webSocketEvent;
    }

    static WebSocketEvent errorEvent(Throwable throwable) {
        WebSocketEvent webSocketEvent = new WebSocketEvent(WebSocketEventType.Error);
        webSocketEvent.throwable = throwable;
        return webSocketEvent;
    }

    public WebSocketEventType getType() {
        return type;
    }

    public Throwable getThrowable() {
        return throwable;
    }

    public String getReason() {
        return reason;
    }

    public enum WebSocketEventType {
        Open, Error, Message, Close,Pong
    }

    public enum WebSocketContentType {
        Text, Binary
    }

}
