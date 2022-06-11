package io.github.wycst.wast.clients.redis.exception;

/**
 * redis连接异常
 *
 * @Author: wangy
 * @Date: 2020/5/19 16:39
 * @Description:
 */
public class RedisConnectionException extends ConnectionException {

    public RedisConnectionException() {
    }

    public RedisConnectionException(String message) {
        super(message);
    }

    public RedisConnectionException(String message, Throwable cause) {
        super(message, cause);
    }

    public RedisConnectionException(Throwable cause) {
        super(cause);
    }
}
