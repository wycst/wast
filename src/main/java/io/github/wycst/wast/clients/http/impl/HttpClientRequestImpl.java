package io.github.wycst.wast.clients.http.impl;

import io.github.wycst.wast.clients.http.definition.HttpClientConfig;
import io.github.wycst.wast.clients.http.definition.HttpClientException;
import io.github.wycst.wast.clients.http.definition.HttpClientMethod;
import io.github.wycst.wast.clients.http.definition.HttpClientRequest;

import java.io.Serializable;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Map;

/**
 * @Author: wangy
 * @Date: 2020/7/2 19:13
 * @Description:
 */
class HttpClientRequestImpl implements HttpClientRequest {

    private URL url;
    private String uri;
    private String host;
    private String userInfo;
    private String protocol;
    private int port;
    private String query;
    private boolean https;
    private final String method;
    private final HttpClientConfig httpClientConfig;
    private boolean useDefaultPort;

    public HttpClientRequestImpl(String spec) {
        this(spec, HttpClientMethod.GET);
    }

    public HttpClientRequestImpl(String spec, HttpClientMethod httpClientMethod) {
        this(spec, httpClientMethod, new HttpClientConfig());
    }

    public HttpClientRequestImpl(String spec, HttpClientMethod httpClientMethod, HttpClientConfig httpClientConfig) {
        this.method = String.valueOf(httpClientMethod);
        this.parseUrlInfo(spec);
        if (httpClientConfig == null) {
            httpClientConfig = new HttpClientConfig();
        }
        this.httpClientConfig = httpClientConfig;
    }

    public boolean isKeepAliveOnTimeout() {
        return httpClientConfig == null ? false : httpClientConfig.isKeepAliveOnTimeout();
    }

    private void parseUrlInfo(String spec) {
        URL url = null;
        try {
            url = new URL(spec);
        } catch (MalformedURLException e) {
            throw new HttpClientException("Invalid url [" + spec + "]", e);
        }
        this.protocol = url.getProtocol().toLowerCase();
        if ("https".indexOf(this.protocol) == -1) {
            // Supported http or https
            throw new HttpClientException("Protocol not supported for [" + protocol + "]");
        }
        this.https = "https".endsWith(this.protocol);
        this.query = url.getQuery();
        this.uri = url.getFile();
        this.userInfo = url.getUserInfo();

        this.url = url;
        this.host = url.getHost();
        this.port = url.getPort();
        if (this.port == -1) {
            this.port = url.getDefaultPort();
            this.useDefaultPort = true;
        }
    }

    public URL getURL() {
        return url;
    }

    @Override
    public String getUri() {
        return uri;
    }

    @Override
    public String getHost() {
        return host;
    }

    public String getUserInfo() {
        return userInfo;
    }

    public String getProtocol() {
        return protocol;
    }

    @Override
    public int getPort() {
        return port;
    }

    public String getQuery() {
        return query;
    }

    public boolean isHttps() {
        return https;
    }

    @Override
    public String getMethod() {
        return method;
    }

    public HttpClientConfig getHttpClientConfig() {
        return httpClientConfig;
    }

    public void addTextParameter(String name, String value) {
        httpClientConfig.addTextParameter(name, value);
    }

    public void setHeader(String name, Serializable value) {
        httpClientConfig.setHeader(name, value);
    }

    public void setHeaders(Map<String, String> headers) {
        httpClientConfig.setHeaders(headers);
    }

    public void removeHeader(String name) {
        httpClientConfig.removeHeader(name);
    }

    public boolean isUseDefaultPort() {
        return useDefaultPort;
    }
}
