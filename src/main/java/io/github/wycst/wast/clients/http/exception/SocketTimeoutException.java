package io.github.wycst.wast.clients.http.exception;

import io.github.wycst.wast.clients.http.definition.HttpClientException;

/**
 * @Author wangyunchao
 * @Date 2021/10/15 15:17
 */
public class SocketTimeoutException extends HttpClientException {

    public SocketTimeoutException(String message) {
        super(message);
    }

    public SocketTimeoutException(String message, Throwable cause) {
        super(message, cause);
    }

    public SocketTimeoutException(Throwable cause) {
        super(cause);
    }
}
