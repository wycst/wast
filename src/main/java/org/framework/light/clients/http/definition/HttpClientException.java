package org.framework.light.clients.http.definition;

/**
 * @Author: wangy
 * @Date: 2020/7/2 9:20
 * @Description:
 */
public class HttpClientException extends RuntimeException {

    public HttpClientException(String message) {
        super(message);
    }

    public HttpClientException(String message, Throwable cause) {
        super(message, cause);
    }

    public HttpClientException(Throwable cause) {
        super(cause);
    }
}
