package org.framework.light.clients.http.impl;

import org.framework.light.clients.http.definition.HttpClientConfig;
import org.framework.light.clients.http.definition.HttpClientMethod;

/**
 * DELETE 资源
 *
 * @Author: wangy
 * @Date: 2020/7/2 19:19
 * @Description:
 */
public class HttpClientDelete extends HttpClientRequestImpl {

    public HttpClientDelete(String url) {
        super(url, HttpClientMethod.DELETE);
    }

    public HttpClientDelete(String spec, HttpClientConfig httpClientConfig) {
        super(spec, HttpClientMethod.DELETE, httpClientConfig);
    }
}
