package io.github.wycst.wast.clients.http.definition;

import io.github.wycst.wast.clients.http.consts.HttpHeaderValues;
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
@SuppressWarnings("ALL")
public final class HttpClientConfig {

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

    private final Map<String, Object> headers = new HashMap<String, Object>();
    private final List<HttpClientParameter> parameterList = new ArrayList<HttpClientParameter>();

    private boolean multipart;
    private String boundary;
    private boolean chunked;
    private Object content;
    private boolean applicationJson;
    private String contentType = "application/x-www-form-urlencoded";
    // headerName -> lowercase
    private boolean headerNameToLowerCase;
    private boolean logApplicationHeaders;

    private Proxy proxy;
    private String charset = "UTF-8";

    private String downloadFileName;
    private boolean responseStream;
    private ResponseCallback responseCallback;
    // SSE有效 失败默认重试3次,如果为-1代表不限次数
    private int errorRetryCount = 3;
    // SSE有效 失败重试间隔秒数默认5秒
    private int retryIntervalSeconds = 5;
    private boolean retryIfServerClosed;

    public long getMaxContentLength() {
        return maxContentLength;
    }

    public void setMaxContentLength(long maxContentLength) {
        this.maxContentLength = maxContentLength;
    }

    public void clearParameters() {
        parameterList.clear();
    }

    public void addTextParameters(HttpClientParameter... clientParameters) {
        for (HttpClientParameter clientParameter : clientParameters) {
            if (clientParameter != null) {
                parameterList.add(clientParameter);
            }
        }
    }

    public HttpClientConfig textParameters(HttpClientParameter... clientParameters) {
        addTextParameters(clientParameters);
        return this;
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
     * 添加普通文本参数
     *
     * @param name
     * @param value
     */
    public HttpClientConfig textParameter(String name, String value) {
        parameterList.add(new HttpClientParameter(name, value));
        return this;
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
     * 添加文件域
     *
     * @param name
     * @param file
     * @param contentType
     */
    public HttpClientConfig fileParameter(String name, File file, String contentType) {
        parameterList.add(new HttpClientParameter(name, file, contentType));
        return this;
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

    /***
     *  以字节参数添加文件域
     *
     * @param name
     * @param fileName 文件名称
     * @param fileContent
     * @param contentType
     */
    public HttpClientConfig fileParameter(String name, String fileName, byte[] fileContent, String contentType) {
        parameterList.add(new HttpClientParameter(name, fileName, fileContent, contentType));
        return this;
    }

    public void setHeader(String name, Serializable value) {
        headers.put(name, String.valueOf(value));
    }

    public HttpClientConfig header(String name, Serializable value) {
        headers.put(name, String.valueOf(value));
        return this;
    }

    public void setHeaders(Map headerMap) {
        if (headerMap != null) {
            headers.putAll(headerMap);
        }
    }

    public HttpClientConfig headers(Map headerMap) {
        setHeaders(headerMap);
        return this;
    }

    public void removeHeader(String name) {
        headers.remove(name);
    }

    public Map<String, Object> getHeaders() {
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

    public HttpClientConfig keepAlive(boolean keepAlive) {
        this.keepAlive = keepAlive;
        return this;
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

    public HttpClientConfig proxy(Proxy proxy) {
        this.proxy = proxy;
        return this;
    }

    public void setProxy(String proxyHost, int proxyPort) {
        InetSocketAddress proxyAddr = new InetSocketAddress(proxyHost, proxyPort);
        this.proxy = new Proxy(Proxy.Type.HTTP, proxyAddr);
    }

    public HttpClientConfig proxy(String proxyHost, int proxyPort) {
        setProxy(proxyHost, proxyPort);
        return this;
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

    public HttpClientConfig multipart(boolean multipart) {
        setMultipart(multipart);
        return this;
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

    public HttpClientConfig requestBody(Object requestBody, String contentType, boolean applicationJson) {
        setRequestBody(requestBody, contentType, applicationJson);
        return this;
    }

    public void setJsonBody(Object requestBody) {
        this.content = requestBody;
        this.contentType = HttpHeaderValues.APPLICATION_JSON;
        this.applicationJson = true;
    }

    public HttpClientConfig jsonBody(Object body) {
        setJsonBody(body);
        return this;
    }

    public boolean isApplicationJson() {
        return applicationJson;
    }

    public void setContentType(String contentType) {
        this.contentType = contentType;
    }

    public HttpClientConfig contentType(String contentType) {
        this.contentType = contentType;
        return this;
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

    public HttpClientConfig keepAliveOnTimeout(boolean keepAliveOnTimeout) {
        this.keepAliveOnTimeout = keepAliveOnTimeout;
        return this;
    }

    public boolean isFollowRedirect() {
        return followRedirect;
    }

    public void setFollowRedirect(boolean followRedirect) {
        this.followRedirect = followRedirect;
    }

    public HttpClientConfig followRedirect(boolean followRedirect) {
        this.followRedirect = followRedirect;
        return this;
    }

    public boolean isHeaderNameToLowerCase() {
        return headerNameToLowerCase;
    }

    public HttpClientConfig headerNameToLowerCase(boolean headerNameToLowerCase) {
        this.headerNameToLowerCase = headerNameToLowerCase;
        return this;
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

    public boolean isLogApplicationHeaders() {
        return logApplicationHeaders;
    }

    public void setLogApplicationHeaders(boolean logApplicationHeaders) {
        this.logApplicationHeaders = logApplicationHeaders;
    }

    public HttpClientConfig logApplicationHeaders(boolean printlnDebugHeaders) {
        this.logApplicationHeaders = printlnDebugHeaders;
        return this;
    }

    public boolean isResponseStream() {
        return responseStream;
    }

    public void setResponseStream(boolean responseStream) {
        this.responseStream = responseStream;
    }

    /**
     * 设置流响应模式（适合大文件流下载）
     *
     * @param responseStream
     * @return
     */
    public HttpClientConfig responseStream(boolean responseStream) {
        this.responseStream = responseStream;
        return this;
    }

    /**
     * 回调处理响应流（异步）
     *
     * @param responseCallback
     * @return
     */
    public HttpClientConfig responseCallback(ResponseCallback responseCallback) {
        this.responseCallback = responseCallback;
        return this;
    }

    public String getDownloadFileName() {
        return downloadFileName;
    }

    public HttpClientConfig downloadFileName(String downloadFileName) {
        this.downloadFileName = downloadFileName;
        return this;
    }

    public ResponseCallback getResponseCallback() {
        return responseCallback;
    }

    public int getErrorRetryCount() {
        return errorRetryCount;
    }

    public void setErrorRetryCount(int errorRetryCount) {
        this.errorRetryCount = errorRetryCount;
    }

    public int getRetryIntervalSeconds() {
        return retryIntervalSeconds;
    }

    public void setRetryIntervalSeconds(int retryIntervalSeconds) {
        this.retryIntervalSeconds = retryIntervalSeconds;
    }

    public boolean isRetryIfServerClosed() {
        return retryIfServerClosed;
    }

    public void setRetryIfServerClosed(boolean retryIfServerClosed) {
        this.retryIfServerClosed = retryIfServerClosed;
    }

    public HttpClientConfig retry(int errorRetryCount, int retryIntervalSeconds) {
        this.errorRetryCount = errorRetryCount;
        this.retryIntervalSeconds = retryIntervalSeconds;
        return this;
    }

    public HttpClientConfig retry(int errorRetryCount, int retryIntervalSeconds, boolean retryIfServerClosed) {
        this.errorRetryCount = errorRetryCount;
        this.retryIntervalSeconds = retryIntervalSeconds;
        this.retryIfServerClosed = retryIfServerClosed;
        return this;
    }
}

