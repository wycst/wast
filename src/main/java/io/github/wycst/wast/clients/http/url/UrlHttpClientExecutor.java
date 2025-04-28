package io.github.wycst.wast.clients.http.url;

import io.github.wycst.wast.clients.http.consts.HttpHeaderNames;
import io.github.wycst.wast.clients.http.definition.*;
import io.github.wycst.wast.clients.http.impl.HttpClientResponseImpl;
import io.github.wycst.wast.clients.http.provider.RequestServiceInstance;
import io.github.wycst.wast.clients.http.provider.ServiceInstance;
import io.github.wycst.wast.common.reflect.UnsafeHelper;
import io.github.wycst.wast.common.utils.IOUtils;
import io.github.wycst.wast.json.JSON;
import io.github.wycst.wast.log.Log;
import io.github.wycst.wast.log.LogFactory;

import java.io.*;
import java.net.*;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * @Author: wangy
 * @Date: 2020/7/4 10:41
 * @Description:
 */
public class UrlHttpClientExecutor extends AbstractUrlHttpClientExecutor {

    // log
    private Log log = LogFactory.getLog(UrlHttpClientExecutor.class);

    @Override
    public final byte[] fastGetBody(String targetUrl, Map<String, String> headers) {
        HttpURLConnection connection = null;
        try {
            connection = connection(targetUrl, headers);
            return IOUtils.readBytes(getInputStream(connection));
        } catch (Throwable throwable) {
            throw clientException(throwable);
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }

    @Override
    public InputStream fastGetInputStream(String targetUrl, Map<String, String> headers) {
        try {
            return getInputStream(connection(targetUrl, headers));
        } catch (Throwable throwable) {
            throw clientException(throwable);
        }
    }

    private HttpClientException clientException(Throwable throwable) {
        if (throwable instanceof HttpClientException) {
            return (HttpClientException) throwable;
        }
        return new HttpClientException(throwable.getMessage(), throwable);
    }

    final HttpURLConnection connection(String targetUrl, Map<String, String> headers) {
        HttpURLConnection httpConnection;
        try {
            URL url = new URL(targetUrl);
            httpConnection = (HttpURLConnection) url.openConnection();
            httpConnection.setRequestMethod("GET");
            if (headers != null) {
                Set<Map.Entry<String, String>> entries = headers.entrySet();
                for (Map.Entry<String, String> entry : entries) {
                    httpConnection.setRequestProperty(entry.getKey(), entry.getValue());
                }
            }
            return httpConnection;
        } catch (Throwable throwable) {
            throw new HttpClientException(throwable.getMessage(), throwable);
        }
    }

    final InputStream getInputStream(HttpURLConnection httpConnection) throws IOException {
        InputStream is;
        try {
            int resCode = httpConnection.getResponseCode();
            boolean httpOk = resCode == HttpURLConnection.HTTP_OK;
            if (httpOk) {
                is = httpConnection.getInputStream();
            } else {
                is = httpConnection.getErrorStream();
            }
            if (is == null) {
                is = httpOk ? httpConnection.getErrorStream() : httpConnection.getInputStream();
            }
            return is;
        } catch (Throwable throwable) {
            throw clientException(throwable);
        }
    }

    @Override
    protected HttpClientResponse doExecuteRequest(HttpClientRequest httpRequest) throws Throwable {
        RequestServiceInstance requestServiceInstance = getRequestServiceInstance(httpRequest);
        try {
            return this.doExecuteRequestInstance(requestServiceInstance, httpRequest);
        } catch (Throwable throwable) {
            // Timeout or connection failure
            ServiceInstance serviceInstance = requestServiceInstance.getServiceInstance();
            // if null throw exception
            if (serviceInstance == null) {
                // Non load balancing mode
                throw throwable;
            }

            // load balancing mode keepAlive instance
            boolean keepAliveOnTimeout = isKeepAliveOnTimeout() || httpRequest.isKeepAliveOnTimeout();
            if (keepAliveOnTimeout && throwable instanceof SocketTimeoutException) {
                throw throwable;
            }

            // mark instance disable and switch next instance
            // This step is crucial
            serviceInstance.setAlive(false);
            // Recursive repeated call
            // How to prevent dead circulation ?
            return doExecuteRequest(httpRequest);
        }
    }

    private HttpClientResponse doExecuteRequestInstance(RequestServiceInstance requestServiceInstance, HttpClientRequest httpRequest) throws Throwable {

        URL instanceUrl = requestServiceInstance.getUrl();
        String method = httpRequest.getMethod();
        log.debug("{} {} ", method, instanceUrl);
        HttpClientConfig clientConfig = httpRequest.getHttpClientConfig();
        URL url = parseQueryUrl(instanceUrl, method, clientConfig);
        Proxy proxy = clientConfig.getProxy();
        HttpURLConnection httpConnection = null;
        if (proxy == null) {
            httpConnection = (HttpURLConnection) url.openConnection();
        } else {
            httpConnection = (HttpURLConnection) url.openConnection(proxy);
        }
        if (!UnsafeHelper.setRequestMethod(httpConnection, method)) {
            httpConnection.setRequestMethod(method);
        }
        httpConnection.setConnectTimeout((int) clientConfig.getMaxConnectTimeout());
        httpConnection.setReadTimeout((int) clientConfig.getMaxReadTimeout());
        httpConnection.setUseCaches(clientConfig.isUseCaches());
        httpConnection.setInstanceFollowRedirects(clientConfig.isFollowRedirect());

        Map<String, Serializable> header = clientConfig.getHeaders();
        final boolean headerNameToLowerCase = clientConfig.isHeaderNameToLowerCase();
        final boolean logApplicationHeaders = clientConfig.isLogApplicationHeaders();
        if (header != null) {
            for (Map.Entry<String, Serializable> entry : header.entrySet()) {
                String key = entry.getKey();
                String headerKey = headerNameToLowerCase ? key.toLowerCase() : key;
                String headerValue = String.valueOf(entry.getValue());
                httpConnection.setRequestProperty(headerKey, headerValue);
                if (logApplicationHeaders) {
                    log.debug("Header Set -> {}: {}", headerKey, headerValue);
                }
            }
        }

        if ("POST".equals(method) || "PUT".equals(method) || "PATCH".equals(method)) {
            String contentType = clientConfig.getContentType();
            if (contentType != null && !contentType.trim().isEmpty()) {
                String headerKey = headerNameToLowerCase ? HttpHeaderNames.CONTENT_TYPE : HttpHeaderNames.CONTENT_TYPE_BROWSER;
                httpConnection.setRequestProperty(headerKey, contentType);
                if (logApplicationHeaders) {
                    log.debug("Header Set -> {}: {}", headerKey, contentType);
                }
            }
            postRequestData(clientConfig, httpConnection);
        }
        int resCode = httpConnection.getResponseCode();
        int contentLength = httpConnection.getContentLength();

        String resContentType = httpConnection.getContentType();
        InputStream is = null;
        try {
            boolean httpOk = resCode == HttpURLConnection.HTTP_OK;
            if (httpOk) {
                is = httpConnection.getInputStream();
            } else {
                is = httpConnection.getErrorStream();
            }
            if (is == null) {
                is = httpOk ? httpConnection.getErrorStream() : httpConnection.getInputStream();
            }
        } catch (Throwable throwable) {
            throwable.printStackTrace();
        }

        if (clientConfig.isResponseStream()) {
            return new HttpClientResponseImpl(resCode, httpConnection.getResponseMessage(), is, contentLength, resContentType, httpConnection.getHeaderFields());
        }

        HttpClientResponse clientResponse = new HttpClientResponseImpl(resCode, httpConnection.getResponseMessage(), readInputStream(is, contentLength), contentLength, resContentType, httpConnection.getHeaderFields());
        httpConnection.disconnect();
        return clientResponse;
    }

    private void postRequestData(HttpClientConfig clientConfig, HttpURLConnection httpConnection) throws IOException {

        byte[] postData = parsePostRequestData(clientConfig);
        // There is actually no need to set content length here, Content-Length key value will auto calculation and write to the header by HttpURLConnection
        httpConnection.setRequestProperty("content-length", String.valueOf(postData.length));

        // Support IO streaming
        httpConnection.setDoOutput(true);
        httpConnection.connect();

        DataOutputStream dataOutputStream = new DataOutputStream(httpConnection.getOutputStream());
        dataOutputStream.write(postData);

        // flush
        dataOutputStream.flush();
        dataOutputStream.close();
    }

    private byte[] parsePostRequestData(HttpClientConfig clientConfig) throws IOException {

        byte[] requestData = null;
        List<HttpClientParameter> clientParameters = clientConfig.getParameterList();
        boolean chunked = clientConfig.isChunked();
        boolean multipart = clientConfig.isMultipart();
        String boundary = clientConfig.getBoundary();
        if (multipart) {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            String formDataSeparator = "--" + boundary;
            byte[] lineSign = new byte[]{'\r', '\n'};
            StringBuilder builder = null;
            for (HttpClientParameter clientParameter : clientParameters) {
                baos.write(formDataSeparator.getBytes());
                baos.write(lineSign);

                String name = clientParameter.getName();
                String value = clientParameter.getValue();
                String itemContentType = clientParameter.getContentType();
                long contentLength = clientParameter.getContentLength();

                builder = new StringBuilder();
                if (clientParameter.isFileUpload()) {
                    builder.append("content-disposition: form-data; name=\"").append(name).append("\"; filename=\"").append(value).append("\"\r\n");
                    builder.append("content-transfer-encoding: binary").append("\r\n");
                } else {
                    builder.append("content-disposition: form-data; name=\"").append(name).append("\"\r\n");
                }
                builder.append("content-length: ").append(contentLength).append("\r\n");
                if (itemContentType != null && itemContentType.length() > 0) {
                    builder.append("content-type: ").append(itemContentType).append("\r\n");
                }
                builder.append("\r\n");
                baos.write(builder.toString().getBytes());
                clientParameter.writeContentTo(baos);
                baos.write(lineSign);
            }
            baos.write(("--" + boundary + "--").getBytes());
            baos.write(lineSign);

            requestData = baos.toByteArray();
        } else if (chunked) {

        } else {
            Object requestBody = clientConfig.getRequestBody();
            if (requestBody != null) {
                boolean applicationJson = clientConfig.isApplicationJson();
                if (applicationJson) {
                    String content = requestBody == null ? "" : JSON.toJsonString(requestBody);
                    return content.getBytes();
                } else {
                    if (requestBody instanceof byte[]) {
                        return (byte[]) requestBody;
                    }
                    return requestBody.toString().getBytes();
                }
            } else {
                StringBuilder builder = new StringBuilder();
                int length = clientParameters.size();
                int i = 0;
                for (HttpClientParameter clientParameter : clientParameters) {
                    builder.append(URLEncoder.encode(clientParameter.getName(), "UTF-8")).append('=').append(URLEncoder.encode(clientParameter.getValue(), "UTF-8"));
                    if (i++ < length - 1) {
                        builder.append('&');
                    }
                }
                requestData = builder.toString().getBytes();
            }
        }
        return requestData;
    }

}
