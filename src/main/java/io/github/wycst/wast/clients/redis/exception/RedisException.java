package io.github.wycst.wast.clients.redis.exception;

/**
 * @Author: wangy
 * @Date: 2020/5/24 9:25
 * @Description:
 */
public class RedisException extends RuntimeException {

    public RedisException() {
    }
    public RedisException(String message) {
        super(message);
    }

    public RedisException(String message, Throwable cause) {
        super(message, cause);
    }

    public RedisException(Throwable cause) {
        super(cause);
    }

}
