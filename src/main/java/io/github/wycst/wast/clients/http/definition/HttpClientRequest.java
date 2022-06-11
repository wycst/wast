package io.github.wycst.wast.clients.http.definition;

import java.io.Serializable;
import java.net.URL;
import java.util.Map;

/**
 * @Author: wangy
 * @Date: 2020/7/2 16:38
 * @Description:
 */
public interface HttpClientRequest {

    public URL getURL();

    public String getUri();

    String getHost();

    boolean isUseDefaultPort();

    int getPort();

    boolean isHttps();

    public String getMethod();

    public HttpClientConfig getHttpClientConfig();

    public void setHeader(String name, Serializable value);

    public void setHeaders(Map<String, String> headers);

    public void removeHeader(String name);
}
