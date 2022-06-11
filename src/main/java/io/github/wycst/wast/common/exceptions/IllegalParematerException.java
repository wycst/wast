package io.github.wycst.wast.common.exceptions;

/**
 * 参数错误
 *
 * @author wangych
 */
public class IllegalParematerException extends RuntimeException {

    /**
     *
     */
    private static final long serialVersionUID = 848546195214892170L;

    public IllegalParematerException(String message) {
        super(message);
    }

    public IllegalParematerException(String message, Throwable cause) {
        super(message, cause);
    }

}
