package io.github.wycst.wast.clients.http.impl;

import io.github.wycst.wast.clients.http.definition.HttpClientConfig;
import io.github.wycst.wast.clients.http.definition.HttpClientMethod;
import io.github.wycst.wast.clients.http.definition.HttpClientRequest;

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
