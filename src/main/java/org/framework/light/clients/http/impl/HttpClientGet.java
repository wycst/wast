package org.framework.light.clients.http.impl;

import org.framework.light.clients.http.definition.HttpClientConfig;
import org.framework.light.clients.http.definition.HttpClientMethod;

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
