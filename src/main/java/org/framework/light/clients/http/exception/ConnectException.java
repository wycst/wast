package org.framework.light.clients.http.exception;

import org.framework.light.clients.http.definition.HttpClientException;

/**
 * @Author wangyunchao
 * @Date 2021/10/15 15:15
 */
public class ConnectException extends HttpClientException {

    public ConnectException(String message) {
        super(message);
    }

    public ConnectException(String message, Throwable cause) {
        super(message, cause);
    }

    public ConnectException(Throwable cause) {
        super(cause);
    }
}
