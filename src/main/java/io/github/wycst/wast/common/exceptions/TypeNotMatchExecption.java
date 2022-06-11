package io.github.wycst.wast.common.exceptions;

/**
 * @Author: wangy
 * @Date: 2021/9/9 23:30
 * @Description:
 */
public class TypeNotMatchExecption extends RuntimeException {

    public TypeNotMatchExecption(String message) {
        super(message);
    }

    public TypeNotMatchExecption(String message, Throwable cause) {
        super(message, cause);
    }

    public TypeNotMatchExecption(Throwable cause) {
        super(cause);
    }
}
