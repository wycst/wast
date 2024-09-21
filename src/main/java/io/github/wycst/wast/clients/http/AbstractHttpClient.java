package io.github.wycst.wast.clients.http;

import io.github.wycst.wast.clients.http.definition.HttpClientRequest;
import io.github.wycst.wast.clients.http.definition.HttpClientResponse;
import io.github.wycst.wast.clients.http.executor.HttpClientExecutor;
import io.github.wycst.wast.clients.http.provider.ServiceProvider;
import io.github.wycst.wast.clients.http.url.UrlHttpClientExecutor;

import java.io.InputStream;
import java.util.Map;

/**
 * @Author: wangy
 * @Date: 2020/8/23 20:35
 * @Description:
 */
class AbstractHttpClient {

    private HttpClientExecutor httpClientExecutor;

    public AbstractHttpClient() {
        this(new UrlHttpClientExecutor());
    }

    public AbstractHttpClient(HttpClientExecutor httpClientExecutor) {
        this.httpClientExecutor = httpClientExecutor;
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

}
