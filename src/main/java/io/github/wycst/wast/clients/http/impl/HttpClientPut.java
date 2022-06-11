package io.github.wycst.wast.clients.http.impl;

import io.github.wycst.wast.clients.http.definition.HttpClientConfig;
import io.github.wycst.wast.clients.http.definition.HttpClientMethod;

import java.io.File;

/**
 * @Author: wangy
 * @Date: 2020/8/23 18:25
 * @Description:
 */
public class HttpClientPut extends HttpClientRequestImpl {

    public HttpClientPut(String url) {
        super(url, HttpClientMethod.PUT);
    }

    public HttpClientPut(String url, HttpClientConfig requestConfig) {
        super(url, HttpClientMethod.PUT, requestConfig);
    }

    public void addFileParameter(String name, File file, String contentType) {
        getHttpClientConfig().addFileParameter(name, file, contentType);
    }

    public void setMultipart(boolean multipart) {
        getHttpClientConfig().setMultipart(multipart);
    }

    public void setRequestBody(Object requestBody, String contentType, boolean applicationJson) {
        getHttpClientConfig().setRequestBody(requestBody, contentType, applicationJson);
    }

    public void setContentType(String contentType) {
        getHttpClientConfig().setContentType(contentType);
    }


}
