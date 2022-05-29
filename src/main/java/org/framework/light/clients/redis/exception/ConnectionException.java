package org.framework.light.clients.redis.exception;

/**
 * 连接异常
 *
 * @Author: wangy
 * @Date: 2020/5/19 16:40
 * @Description:
 */
public class ConnectionException extends RuntimeException {

    public ConnectionException() {
    }

    public ConnectionException(String message) {
        super(message);
    }

    public ConnectionException(String message, Throwable cause) {
        super(message, cause);
    }

    public ConnectionException(Throwable cause) {
        super(cause);
    }
}
