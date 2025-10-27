package io.github.wycst.wast.clients.http;

import io.github.wycst.wast.clients.http.consts.HttpHeaderNames;
import io.github.wycst.wast.clients.http.consts.HttpHeaderValues;
import io.github.wycst.wast.clients.http.definition.*;
import io.github.wycst.wast.clients.http.event.EventSourceCallback;
import io.github.wycst.wast.clients.http.event.EventSourceHandler;
import io.github.wycst.wast.clients.http.event.EventSourceMessage;
import io.github.wycst.wast.clients.http.executor.HttpClientExecutor;
import io.github.wycst.wast.clients.http.impl.HttpClientRequestBuilder;
import io.github.wycst.wast.clients.http.provider.ServiceProvider;
import io.github.wycst.wast.clients.http.url.UrlHttpClientExecutor;
import io.github.wycst.wast.log.Log;
import io.github.wycst.wast.log.LogFactory;

import java.io.*;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;

/**
 * @Author: wangy
 * @Date: 2020/8/23 20:35
 * @Description:
 */
class AbstractHttpClient {

    private HttpClientExecutor httpClientExecutor;
    final static Map EMPTY_HEADERS = new HashMap();
    final static Log log = LogFactory.getLog(AbstractHttpClient.class);

    public AbstractHttpClient() {
        this(new UrlHttpClientExecutor());
    }

    public AbstractHttpClient(HttpClientExecutor httpClientExecutor) {
        this.httpClientExecutor = httpClientExecutor;
    }

    final static String DEFAULT_DOWNLOAD_DIR;

    static {
        String userHome = System.getProperty("user.home");
        String envHome = System.getenv("HOME");
        String home = userHome == null ? envHome : userHome;
        if (home != null) {
            DEFAULT_DOWNLOAD_DIR = home + File.separator + "Downloads" + File.separator;
        } else {
            DEFAULT_DOWNLOAD_DIR = File.separator + "tmp" + File.separator + "Downloads" + File.separator;
        }
    }

    /**
     * 设置服务提供者
     *
     * @param serviceProvider
     */
    public void setServiceProvider(ServiceProvider serviceProvider) {
        httpClientExecutor.setServiceProvider(serviceProvider);
    }

    /**
     * 返回设置服务提供者
     *
     * @return
     */
    public ServiceProvider getServiceProvider() {
        return httpClientExecutor.getServiceProvider();
    }


    /**
     * 是否启用负载均衡
     *
     * @param enableLoadBalance
     */
    public void setEnableLoadBalance(boolean enableLoadBalance) {
        httpClientExecutor.setEnableLoadBalance(enableLoadBalance);
    }

    /**
     * 超时时保持实例可用不移除（某些场景单向网络只发数据但无法接收响应可以配合超时实现数据推送）
     *
     * @param keepAliveOnTimeout
     */
    public void setKeepAliveOnTimeout(boolean keepAliveOnTimeout) {
        httpClientExecutor.setKeepAliveOnTimeout(keepAliveOnTimeout);
    }

    protected final static String stringify(Map<String, Object> params) {
        StringBuilder builder = new StringBuilder();
        int length = params.size();
        int i = 0;
        for (Map.Entry<String, Object> entry : params.entrySet()) {
            String key = entry.getKey();
            Object val = entry.getValue();
            // if npc exception ?
            String value = val == null ? "" : val.toString();
            try {
                builder.append(URLEncoder.encode(key, "UTF-8")).append('=').append(URLEncoder.encode(value, "UTF-8"));
            } catch (Throwable throwable) {
                builder.append(key).append('=').append(value);
            }
            if (i++ < length - 1) {
                builder.append('&');
            }
        }
        return builder.toString();
    }

    public final static String toStringifyUrl(String url, Map<String, Object> params) {
        if (params != null && !params.isEmpty()) {
            String paramString = stringify(params);
            if (url.contains("?")) {
                url += "&" + paramString;
            } else {
                url += "?" + paramString;
            }
        }
        return url;
    }

    /**
     * 通过http客户端发送通用请求,返回响应
     * httpClient 核心处理方法
     *
     * @param httpRequest
     * @return
     */
    public HttpClientResponse executeRequest(HttpClientRequest httpRequest) {
        return httpClientExecutor.executeRequest(httpRequest);
    }

    /**
     * 下载字节内容
     *
     * @param url
     * @return
     */
    public final byte[] download(String url) {
        return httpClientExecutor.fastGetBody(url, null);
    }

    /**
     * 下载
     *
     * @param url
     * @return
     */
    public final byte[] download(String url, Map<String, String> headers) {
        return httpClientExecutor.fastGetBody(url, headers);
    }

    /**
     * 下载远程资源的输入流（GET请求）
     *
     * @param url
     * @param headers
     * @return
     */
    public final InputStream downloadInputStream(String url, Map<String, String> headers) {
        return httpClientExecutor.fastGetInputStream(url, headers);
    }

    /**
     * 下载远程资源的输入流（GET请求）
     *
     * @param url
     * @param params
     * @param headers
     * @return
     */
    public final InputStream downloadInputStream(String url, Map<String, Object> params, Map<String, String> headers) {
        return httpClientExecutor.fastGetInputStream(toStringifyUrl(url, params), headers);
    }

    /**
     * 下载远程资源的输入流到指定输出流(GET)
     *
     * @param url
     * @param clientConfig
     * @param targetOutputStream
     */
    public final void download(String url, HttpClientConfig clientConfig, final OutputStream targetOutputStream) {
        download(url, HttpClientMethod.GET, clientConfig, targetOutputStream);
    }

    /**
     * 下载远程资源的输入流到指定输出流
     *
     * @param url
     * @param method
     * @param clientConfig
     * @param targetOutputStream 下载目的位置
     * @return
     */
    public final void download(String url, HttpClientMethod method, HttpClientConfig clientConfig, final OutputStream targetOutputStream) {
        HttpClientResponse clientResponse = httpClientExecutor.executeRequest(HttpClientRequestBuilder.buildRequest(url, method, clientConfig.responseStream(true)));
        handleResponseInputStream(clientResponse, clientConfig, targetOutputStream);
    }


    /**
     * 下载远程资源到默认下载目录(GET)
     *
     * @param url
     * @param clientConfig
     * @return
     */
    public final void download(String url, HttpClientConfig clientConfig) {
        download(url, HttpClientMethod.GET, clientConfig);
    }

    /**
     * 下载远程资源到默认下载目录
     *
     * @param url
     * @param method
     * @param clientConfig
     * @return
     */
    public final void download(String url, HttpClientMethod method, HttpClientConfig clientConfig) {
        HttpClientResponse clientResponse = httpClientExecutor.executeRequest(HttpClientRequestBuilder.buildRequest(url, method, clientConfig.responseStream(true)));
        String fileName = clientConfig.getDownloadFileName();
        if (fileName == null) {
            String contentDisposition = clientResponse.getHeader("content-disposition");
            if (contentDisposition != null) {
                contentDisposition = contentDisposition.toLowerCase();
                if (contentDisposition.contains("filename=")) {
                    fileName = new String(contentDisposition.substring(contentDisposition.indexOf("filename=") + 9));
                    try {
                        fileName = URLDecoder.decode(fileName, "UTF-8");
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            } else {
                int lastIndex = url.lastIndexOf("/");
                fileName = new String(url.substring(lastIndex + 1));
            }
        }
        try {
            String targetPath = DEFAULT_DOWNLOAD_DIR + fileName;
            log.debug("download file path -> {}", targetPath);
            OutputStream targetOutputStream = new FileOutputStream(targetPath);
            handleResponseInputStream(clientResponse, clientConfig, targetOutputStream);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    final void handleResponseInputStream(final HttpClientResponse clientResponse, final HttpClientConfig clientConfig, final OutputStream targetOutputStream) {
        final ResponseCallback responseCallback = clientConfig.getResponseCallback();
        final InputStream is = clientResponse.inputStream();
        final byte[] buffer = new byte[8192];
        if (responseCallback != null) {
            final long total = clientResponse.readContentLength();
            new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        int len;
                        long downloadLength = 0;
                        while ((len = is.read(buffer)) != -1) {
                            targetOutputStream.write(buffer, 0, len);
                            downloadLength += len;
                            responseCallback.onDownloadProgress(downloadLength, Math.max(downloadLength, total));
                        }
                        is.close();
                        targetOutputStream.flush();
                        targetOutputStream.close();
                    } catch (Throwable throwable) {
                        throwable.printStackTrace();
                    }
                }
            }).start();
        } else {
            try {
                int len;
                while ((len = is.read(buffer)) != -1) {
                    targetOutputStream.write(buffer, 0, len);
                }
                is.close();
                targetOutputStream.flush();
                targetOutputStream.close();
            } catch (Throwable throwable) {
                throwable.printStackTrace();
            }
        }
    }

    /**
     * 支持客户端SSE(参考前端使用习惯)
     *
     * @param url
     * @param method
     * @param clientConfig
     * @param eventSourceCallback
     * @return
     */
    public final EventSourceHandler eventSource(final String url, final HttpClientMethod method, final HttpClientConfig clientConfig, final EventSourceCallback eventSourceCallback) {
        return eventSource(url, method, clientConfig, eventSourceCallback, null);
    }

    static final void doSleep(long milliseconds) {
        try {
            Thread.sleep(milliseconds);
        } catch (InterruptedException e) {
        }
    }

    /**
     * 支持客户端SSE(参考前端使用习惯)
     *
     * @param url
     * @param method
     * @param clientConfig
     * @param eventSourceCallback
     * @param executorService
     * @return
     */
    public final EventSourceHandler eventSource(final String url, final HttpClientMethod method, final HttpClientConfig clientConfig, final EventSourceCallback eventSourceCallback, ExecutorService executorService) {
        final EventSourceHandler eventSourceHandler = new EventSourceHandler();
        if (eventSourceCallback == null) {
            throw new IllegalArgumentException("EventSourceCallback can not be null");
        }
        clientConfig.responseCallback(eventSourceCallback);
        clientConfig.setHeader(HttpHeaderNames.ACCEPT, HttpHeaderValues.TEXT_EVENT_STREAM);
        clientConfig.setMaxReadTimeout(0L);
        final Runnable eventSourceThread = new Runnable() {
            @Override
            public void run() {
                // 最多失败重连次数
                final int errorRetryCount = clientConfig.getErrorRetryCount();
                final long retryIntervalSeconds = clientConfig.getRetryIntervalSeconds();
                final boolean retryIfServerClosed = clientConfig.isRetryIfServerClosed();
                // 实际重连次数。成功后重置
                int retryCount = 0;
                // If it doesn't close, keep trying
                while (!eventSourceHandler.isClosed()) {
                    HttpClientResponse clientResponse;
                    InputStream is;
                    boolean closedFlag = false;
                    try {
                        clientResponse = httpClientExecutor.executeRequest(HttpClientRequestBuilder.buildRequest(url, method, clientConfig.responseStream(true)));
                        // check if sse response
                        String contentType = clientResponse.getContentType();
                        if (contentType == null || !contentType.startsWith(HttpHeaderValues.TEXT_EVENT_STREAM)) {
                            eventSourceCallback.onerror(new IllegalArgumentException("content-type '" + contentType + "' not supported, EventSource content-type must be text/event-stream"));
                            eventSourceHandler.resultOf(false, clientResponse);
                            continue;
                        }
                        if (clientResponse.status() != 200) {
                            eventSourceCallback.onerror(new IllegalArgumentException("status code " + clientResponse.status()));
                            eventSourceHandler.resultOf(false, clientResponse);
                            continue;
                        }
                        retryCount = 0;
                        eventSourceCallback.onopen(clientResponse);
                        is = clientResponse.inputStream();
                        final BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(is));

                        String line;
                        EventSourceMessage eventSourceMessage = null;
                        long messageRetry;
                        while ((line = bufferedReader.readLine()) != null) {
                            if (line.isEmpty()) {
                                if (eventSourceMessage == null || eventSourceMessage.getData() == null) {
                                    continue;
                                }
                                messageRetry = eventSourceMessage.getRetry();
                                eventSourceCallback.onmessage(eventSourceMessage);
                                eventSourceMessage = null;
                                if (messageRetry > 0) {
                                    doSleep(messageRetry);
                                }
                                continue;
                            }
                            if (eventSourceMessage == null) {
                                eventSourceMessage = new EventSourceMessage();
                            }
                            if (line.startsWith("data:")) {
                                String dataStr = line.substring(5);
                                eventSourceMessage.setData(dataStr);
                            } else if (line.startsWith("event:")) {
                                String event = line.substring(6);
                                eventSourceMessage.setEvent(event);
                            } else if (line.startsWith("id:")) {
                                String id = line.substring(3);
                                eventSourceMessage.setId(id);
                            } else if (line.startsWith("retry:")) {
                                String retry = line.substring(6);
                                eventSourceMessage.setRetry(Long.parseLong(retry));
                            }
                        }
                        is.close();
                        // 通常情况下单次请后完毕后服务端会主动关闭连接等待客户端下次重新请求（比如常用的问答请求）
                        // 在服务端正常关闭连接的场景下，如果还希望保持重连，可以设置retryIfServerClosed为true
                        if (!retryIfServerClosed) {
                            eventSourceCallback.onclose();
                            eventSourceHandler.close();
                            closedFlag = true;
                        }
                    } catch (Throwable throwable) {
                        eventSourceCallback.onerror(throwable);
                    } finally {
                        if (!closedFlag) {
                            ++retryCount;
                            if ((errorRetryCount < 0 || retryCount <= errorRetryCount)) {
                                log.debug("EventSource Error, wait {} Seconds, retryCount {} ", retryIntervalSeconds, retryCount);
                                doSleep(retryIntervalSeconds * 1000);
                            } else {
                                eventSourceHandler.close();
                            }
                        }
                    }
                }
                log.debug("EventSource closed");
            }
        };
        // 异步处理
        if (executorService == null) {
            new Thread(eventSourceThread).start();
        } else {
            executorService.submit(eventSourceThread);
        }
        return eventSourceHandler;
    }
}
