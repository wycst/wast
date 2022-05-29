package org.framework.light.clients.http.impl;

import org.framework.light.clients.http.definition.HttpClientConfig;
import org.framework.light.clients.http.definition.HttpClientMethod;

import java.io.File;

/**
 * @Author: wangy
 * @Date: 2020/7/2 19:19
 * @Description:
 */
public class HttpClientPost extends HttpClientRequestImpl {

    public HttpClientPost(String url) {
        super(url, HttpClientMethod.POST);
    }

    public HttpClientPost(String url, HttpClientConfig requestConfig) {
        super(url, HttpClientMethod.POST, requestConfig);
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
