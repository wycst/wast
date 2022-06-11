package io.github.wycst.wast.clients.http.impl;

import io.github.wycst.wast.clients.http.definition.HttpClientConfig;
import io.github.wycst.wast.clients.http.definition.HttpClientMethod;

/**
 * @Author: wangy
 * @Date: 2020/7/2 19:19
 * @Description:
 */
public class HttpClientGet extends HttpClientRequestImpl {

    public HttpClientGet(String url) {
        super(url, HttpClientMethod.GET);
    }

    public HttpClientGet(String spec, HttpClientConfig httpClientConfig) {
        super(spec, HttpClientMethod.GET, httpClientConfig);
    }
}
