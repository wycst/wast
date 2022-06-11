package io.github.wycst.wast.common.expression;

/**
 * @Author: wangy
 * @Date: 2021/9/25 22:43
 * @Description:
 */
public class ExpressionException extends RuntimeException {

    public ExpressionException(String message) {
        super(message);
    }

    public ExpressionException(String message, Throwable cause) {
        super(message, cause);
    }
}
