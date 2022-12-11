package io.github.wycst.wast.jdbc.exception;

/**
 * @Author: wangy
 * @Date: 2021/7/25 22:30
 * @Description:
 */
public class EntityException extends RuntimeException {

    public EntityException(String message) {
        super(message);
    }

    public EntityException(String message, Throwable cause) {
        super(message, cause);
    }
}
