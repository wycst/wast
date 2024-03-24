package io.github.wycst.wast.clients.http.definition;

import io.github.wycst.wast.common.idgenerate.providers.IdGenerator;

import java.io.File;
import java.io.Serializable;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @Author: wangy
 * @Date: 2020/7/2 16:10
 * @Description:
 */
public class HttpClientConfig {

    private static boolean defaultFollowRedirect = false;
    private static boolean defaultUseCaches = false;
    private static long defaultMaxConnectTimeout = 30000;
    private static long defaultMaxReadTimeout = 30000;

    private boolean keepAlive;
    private long maxConnectTimeout = defaultMaxConnectTimeout;
    private long maxReadTimeout = defaultMaxReadTimeout;
    private long maxCloseTimeout;
    private long maxContentLength;
    private boolean useCaches = defaultUseCaches;
    // If response status 302 requests a redirect address (read the location value in the response header)
    private boolean followRedirect = defaultFollowRedirect;

    // use by loadblance
    private boolean keepAliveOnTimeout;

    private final Map<String, Serializable> headers = new HashMap<String, Serializable>();
    private final List<HttpClientParameter> parameterList = new ArrayList<HttpClientParameter>();

    private boolean multipart;
    private String boundary;
    private boolean chunked;
    private Object content;
    private boolean applicationJson;
    private String contentType = "application/x-www-form-urlencoded";

    private Proxy proxy;
    private String charset = "UTF-8";

    public long getMaxContentLength() {
        return maxContentLength;
    }

    public void setMaxContentLength(long maxContentLength) {
        this.maxContentLength = maxContentLength;
    }

    public void clearParameters() {
        parameterList.clear();
    }

    public void addTextParameters(HttpClientParameter...clientParameters) {
        for(HttpClientParameter clientParameter : clientParameters) {
            if(clientParameter != null) {
                parameterList.add(clientParameter);
            }
        }
    }

    /***
     * 添加普通文本参数
     *
     * @param name
     * @param value
     */
    public void addTextParameter(String name, String value) {
        parameterList.add(new HttpClientParameter(name, value));
    }

    /***
     * 添加文件域
     *
     * @param name
     * @param file
     * @param contentType
     */
    public void addFileParameter(String name, File file, String contentType) {
        parameterList.add(new HttpClientParameter(name, file, contentType));
    }

    /***
     *  以字节参数添加文件域
     *
     * @param name
     * @param fileName 文件名称
     * @param fileContent
     * @param contentType
     */
    public void addFileParameter(String name, String fileName, byte[] fileContent, String contentType) {
        parameterList.add(new HttpClientParameter(name, fileName, fileContent, contentType));
    }

    public void setHeader(String name, Serializable value) {
        headers.put(name, String.valueOf(value));
    }

    public void setHeaders(Map<String, String> headerMap) {
        if(headerMap != null) {
            headers.putAll(headerMap);
        }
    }

    public void removeHeader(String name) {
        headers.remove(name);
    }

    public Map<String, Serializable> getHeaders() {
        return headers;
    }

    public List<HttpClientParameter> getParameterList() {
        return parameterList;
    }

    public void clearHeaders() {
        headers.clear();
    }

    public void clear() {
        clearParameters();
        clearHeaders();
        this.content = null;
    }

    public boolean isKeepAlive() {
        return keepAlive;
    }

    public void setKeepAlive(boolean keepAlive) {
        this.keepAlive = keepAlive;
    }

    public long getMaxConnectTimeout() {
        return maxConnectTimeout;
    }

    public void setMaxConnectTimeout(long maxConnectTimeout) {
        this.maxConnectTimeout = maxConnectTimeout;
    }

    public long getMaxReadTimeout() {
        return maxReadTimeout;
    }

    public void setMaxReadTimeout(long maxReadTimeout) {
        this.maxReadTimeout = maxReadTimeout;
    }

    public long getMaxCloseTimeout() {
        return maxCloseTimeout;
    }

    public void setMaxCloseTimeout(long maxCloseTimeout) {
        this.maxCloseTimeout = maxCloseTimeout;
    }

    public void setProxy(Proxy proxy) {
        this.proxy = proxy;
    }

    public void setProxy(String proxyHost, int proxyPort) {
        InetSocketAddress proxyAddr = new InetSocketAddress(proxyHost, proxyPort);
        this.proxy = new Proxy(Proxy.Type.HTTP, proxyAddr);
    }

    public Proxy getProxy() {
        return proxy;
    }

    public boolean isMultipart() {
        return multipart;
    }

    public void setMultipart(boolean multipart) {
        this.multipart = multipart;
        if (multipart) {
            initMultipart();
        }
    }

    private void initMultipart() {
        if (this.boundary == null) {
            this.boundary = IdGenerator.hex(); // Base64.getEncoder().encodeToString(String.valueOf(1 + new Random().nextDouble()).getBytes());
        }
        this.contentType = "multipart/form-data; boundary=" + boundary;
    }

    public boolean isChunked() {
        return chunked;
    }

    public void setChunked(boolean chunked) {
        this.chunked = chunked;
    }

    public boolean isUseCaches() {
        return useCaches;
    }

    public void setUseCaches(boolean useCaches) {
        this.useCaches = useCaches;
    }

    public String getCharset() {
        return charset;
    }

    public void setCharset(String charset) {
        this.charset = charset;
    }

    public Object getRequestBody() {
        return content;
    }

    public void setRequestBody(Object requestBody, String contentType, boolean applicationJson) {
        this.content = requestBody;
        this.contentType = contentType;
        this.applicationJson = applicationJson;
    }

    public boolean isApplicationJson() {
        return applicationJson;
    }

    public void setContentType(String contentType) {
        this.contentType = contentType;
    }

    public String getContentType() {
        return contentType;
    }

    public String getBoundary() {
        return boundary;
    }

    public boolean isKeepAliveOnTimeout() {
        return keepAliveOnTimeout;
    }

    public void setKeepAliveOnTimeout(boolean keepAliveOnTimeout) {
        this.keepAliveOnTimeout = keepAliveOnTimeout;
    }

    public boolean isFollowRedirect() {
        return followRedirect;
    }

    public void setFollowRedirect(boolean followRedirect) {
        this.followRedirect = followRedirect;
    }

    public static HttpClientConfig create() {
        return new HttpClientConfig();
    }

    public static void setDefaultFollowRedirect(boolean defaultFollowRedirect) {
        HttpClientConfig.defaultFollowRedirect = defaultFollowRedirect;
    }

    public static void setDefaultUseCaches(boolean defaultUseCaches) {
        HttpClientConfig.defaultUseCaches = defaultUseCaches;
    }

    public static void setDefaultMaxConnectTimeout(long defaultMaxConnectTimeout) {
        HttpClientConfig.defaultMaxConnectTimeout = defaultMaxConnectTimeout;
    }

    public static void setDefaultMaxReadTimeout(long defaultMaxReadTimeout) {
        HttpClientConfig.defaultMaxReadTimeout = defaultMaxReadTimeout;
    }
}
