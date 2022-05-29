package org.framework.light.clients.http.impl;

import org.framework.light.clients.http.definition.HttpClientConfig;
import org.framework.light.clients.http.definition.HttpClientMethod;
import org.framework.light.clients.http.definition.HttpClientRequest;

/**
 * @Author: wangy
 * @Date: 2020/7/5 16:19
 * @Description:
 */
public class HttpClientRequestBuilder {

    public static HttpClientRequest buildRequest(String url, HttpClientMethod method) {
        return new HttpClientRequestImpl(url, method);
    }

    public static HttpClientRequest buildRequest(String url, HttpClientMethod method, HttpClientConfig requestConfig) {
        return new HttpClientRequestImpl(url, method, requestConfig);
    }

    public static HttpClientRequest buildRequest(String url) {
        return new HttpClientRequestImpl(url);
    }
}
