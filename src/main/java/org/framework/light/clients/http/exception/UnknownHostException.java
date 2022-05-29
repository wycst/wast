package org.framework.light.clients.http.exception;

import org.framework.light.clients.http.definition.HttpClientException;

/**
 * @Author wangyunchao
 * @Date 2021/10/15 15:10
 */
public class UnknownHostException extends HttpClientException {

    public UnknownHostException(String message) {
        super(message);
    }

    public UnknownHostException(String message, Throwable cause) {
        super(message, cause);
    }

    public UnknownHostException(Throwable cause) {
        super(cause);
    }
}
