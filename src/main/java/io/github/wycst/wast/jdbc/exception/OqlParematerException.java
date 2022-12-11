package io.github.wycst.wast.jdbc.exception;

/**
 * @Author: wangy
 * @Date: 2021/2/15 20:06
 * @Description:
 */
public class OqlParematerException extends RuntimeException {

    public OqlParematerException(String message) {
        super(message);
    }

    public OqlParematerException(String message, Throwable cause) {
        super(message, cause);
    }
}
