package io.github.wycst.wast.clients.http.executor;

import io.github.wycst.wast.clients.http.definition.*;
import io.github.wycst.wast.clients.http.exception.ConnectException;
import io.github.wycst.wast.clients.http.exception.SocketTimeoutException;
import io.github.wycst.wast.clients.http.exception.UnknownHostException;
import io.github.wycst.wast.clients.http.provider.DefaultServiceProvider;
import io.github.wycst.wast.clients.http.provider.RequestServiceInstance;
import io.github.wycst.wast.clients.http.provider.ServiceInstance;
import io.github.wycst.wast.clients.http.provider.ServiceProvider;

import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.util.List;
import java.util.Map;

/**
 * @Author: wangy
 * @Date: 2020/7/4 10:11
 * @Description:
 */
public abstract class HttpClientExecutor {

    private ServiceProvider serviceProvider = new DefaultServiceProvider();
    private boolean enableLoadBalance = false;
    private boolean keepAliveOnTimeout;

    protected abstract HttpClientResponse doExecuteRequest(HttpClientRequest httpRequest) throws Throwable;

    public abstract byte[] fastGetBody(String url, Map<String, String> headers);

    public abstract InputStream fastGetInputStream(String url, Map<String, String> headers);

    public HttpClientResponse executeRequest(HttpClientRequest httpRequest) {
        checkIfEmptyRequest(httpRequest);
        HttpClientResponse clientResponse = null;
        try {
            clientResponse = doExecuteRequest(httpRequest);
        } catch (Throwable e) {
            this.handleExecuteRequestThrowable(e, httpRequest);
        } finally {
        }
        return clientResponse;
    }

    protected RequestServiceInstance getRequestServiceInstance(HttpClientRequest httpRequest) throws MalformedURLException {
        RequestServiceInstance requestServiceInstance = new RequestServiceInstance();
        if(enableLoadBalance) {
            checkServiceProvider();
            ServiceInstance serviceInstance = serviceProvider.getServiceInstance(httpRequest);
            if(serviceInstance == null) {
                requestServiceInstance.setUrl(httpRequest.getURL());
            } else {
                URL url = httpRequest.getURL();
                String baseUrl = serviceInstance.getBaseUrl();
                String newUrl = url.getProtocol() + "://" + baseUrl + url.getFile();
                requestServiceInstance.setServiceInstance(serviceInstance);
                requestServiceInstance.setUrl(new URL(newUrl));
            }
        } else {
            requestServiceInstance.setUrl(httpRequest.getURL());
        }
        return requestServiceInstance;
    }

    private void checkServiceProvider() throws MalformedURLException {
        if(serviceProvider == null) {
            throw new HttpClientException("No service provider was specified when load balancing scheduling was enabled");
        }
    }

    private void checkIfEmptyRequest(HttpClientRequest httpRequest) {
        if (httpRequest == null) {
            throw new HttpClientException("Request is null");
        }
        if(httpRequest.getURL() == null) {
            throw new HttpClientException("The URL of the request is not defined");
        }
    }

    protected URL parseQueryUrl(URL instanceUrl, String method, HttpClientConfig clientConfig) throws UnsupportedEncodingException, MalformedURLException {
        if(instanceUrl == null) {
            return null;
        }
        if ("GET".equals(method) || "DELETE".equals(method)) {
            List<HttpClientParameter> clientParameters = clientConfig.getParameterList();
            if(clientParameters == null || clientParameters.size() == 0) {
                return instanceUrl;
            }
            String url = instanceUrl.toString();
            // 追加query时注意是否存在？或者#问题
            int deleteIndex = url.indexOf("#");
            if(deleteIndex > -1) {
                url = new String(url.substring(0, deleteIndex));
            }
            StringBuilder queryParamBuffer = new StringBuilder();
            if(url.indexOf("?") == -1) {
                queryParamBuffer.append("?");
            } else {
                if(!url.endsWith("?")) {
                    queryParamBuffer.append("&");
                }
            }
            int length = clientParameters.size();
            int i = 0;
            for (HttpClientParameter clientParameter : clientParameters) {
                i++;
                if(clientParameter.isFileUpload()) continue;
                queryParamBuffer.append(URLEncoder.encode(clientParameter.getName(), "UTF-8")).append('=').append(URLEncoder.encode(clientParameter.getValue(), "UTF-8"));
                if (i < length) {
                    queryParamBuffer.append('&');
                }
            }
            url += queryParamBuffer.toString();
            return new URL(url);
        }
        return instanceUrl;
    }


    public void setEnableLoadBalance(boolean enableLoadBalance) {
        this.enableLoadBalance = enableLoadBalance;
    }

    public void setServiceProvider(ServiceProvider serviceProvider) {
        this.serviceProvider = serviceProvider;
    }

    public ServiceProvider getServiceProvider() {
        return serviceProvider;
    }

    public boolean isEnableLoadBalance() {
        return enableLoadBalance;
    }

    public boolean isKeepAliveOnTimeout() {
        return keepAliveOnTimeout;
    }

    public void setKeepAliveOnTimeout(boolean keepAliveOnTimeout) {
        this.keepAliveOnTimeout = keepAliveOnTimeout;
    }

    private void handleExecuteRequestThrowable(Throwable e, HttpClientRequest httpRequest) {
        if(e instanceof java.net.UnknownHostException) {
            throw new UnknownHostException(e.getMessage(), e);
        } else if(e instanceof java.net.ConnectException) {
            throw new ConnectException(e.getMessage(), e);
        } else if(e instanceof java.net.SocketTimeoutException) {
            throw new SocketTimeoutException(e.getMessage(), e);
        }
        // if throw an exception ?
        throw new HttpClientException(e.getMessage(), e);
    }
}
