package io.github.wycst.wast.clients.redis.exception;

/**
 * @Author: wangy
 * @Date: 2020/5/24 11:32
 * @Description:
 */
public class RedisInvalidException extends RedisException {

    public RedisInvalidException() {
    }

    public RedisInvalidException(String message) {
        super(message);
    }

    public RedisInvalidException(String message, Throwable cause) {
        super(message, cause);
    }

    public RedisInvalidException(Throwable cause) {
        super(cause);
    }
}
