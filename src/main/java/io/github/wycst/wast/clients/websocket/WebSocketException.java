package io.github.wycst.wast.clients.websocket;

/**
 * @Author: wangy
 * @Date: 2022/3/13 12:05
 * @Description:
 */
public class WebSocketException extends RuntimeException {

    public WebSocketException() {
    }

    public WebSocketException(String message) {
        super(message);
    }

    public WebSocketException(String message, Throwable cause) {
        super(message, cause);
    }

    public WebSocketException(Throwable cause) {
        super(cause);
    }
}
