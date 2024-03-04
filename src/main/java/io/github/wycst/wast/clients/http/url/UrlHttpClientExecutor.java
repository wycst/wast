package io.github.wycst.wast.clients.http.url;

import io.github.wycst.wast.clients.http.definition.HttpClientConfig;
import io.github.wycst.wast.clients.http.definition.HttpClientParameter;
import io.github.wycst.wast.clients.http.definition.HttpClientRequest;
import io.github.wycst.wast.clients.http.definition.HttpClientResponse;
import io.github.wycst.wast.clients.http.impl.HttpClientResponseImpl;
import io.github.wycst.wast.clients.http.provider.RequestServiceInstance;
import io.github.wycst.wast.clients.http.provider.ServiceInstance;
import io.github.wycst.wast.json.JSON;
import io.github.wycst.wast.log.Log;
import io.github.wycst.wast.log.LogFactory;

import java.io.*;
import java.net.*;
import java.util.List;
import java.util.Map;

/**
 * @Author: wangy
 * @Date: 2020/7/4 10:41
 * @Description:
 */
public class UrlHttpClientExecutor extends AbstractUrlHttpClientExecutor {

    // log
    private Log log = LogFactory.getLog(UrlHttpClientExecutor.class);

    @Override
    protected HttpClientResponse doExecuteRequest(HttpClientRequest httpRequest) throws Throwable {
        RequestServiceInstance requestServiceInstance = getRequestServiceInstance(httpRequest);
        try {
            return this.doExecuteRequestInstance(requestServiceInstance, httpRequest);
        } catch (Throwable throwable) {
            // Timeout or connection failure
            ServiceInstance serviceInstance = requestServiceInstance.getServiceInstance();
            // if null throw exception
            if(serviceInstance == null) {
                // Non load balancing mode
                throw throwable;
            }

            // load balancing mode keepAlive instance
            boolean keepAliveOnTimeout = isKeepAliveOnTimeout() || httpRequest.isKeepAliveOnTimeout();
            if(keepAliveOnTimeout && throwable instanceof SocketTimeoutException) {
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

        httpConnection.setRequestMethod(method);
        httpConnection.setConnectTimeout((int) clientConfig.getMaxConnectTimeout());
        httpConnection.setReadTimeout((int) clientConfig.getMaxReadTimeout());
        httpConnection.setUseCaches(clientConfig.isUseCaches());
        httpConnection.setInstanceFollowRedirects(clientConfig.isFollowRedirect());

        Map<String, Serializable> header = clientConfig.getHeaders();
        if (header != null) {
            for (Map.Entry<String, Serializable> entry : header.entrySet()) {
                httpConnection.setRequestProperty(entry.getKey().toLowerCase(), String.valueOf(entry.getValue()));
            }
        }
        httpConnection.setRequestProperty("content-type", clientConfig.getContentType());
        if ("POST".equals(method) || "PUT".equals(method)) {
            postRequestData(clientConfig, httpConnection);
        }
        int resCode = httpConnection.getResponseCode();
        int contentLength = httpConnection.getContentLength();

        String resContentType = httpConnection.getContentType();
        InputStream is = null;
        try {
            is = httpConnection.getErrorStream();
            if (is == null) {
                is = httpConnection.getInputStream();
            }
        } catch (Throwable throwable) {
            throwable.printStackTrace();
        }

        HttpClientResponse clientResponse = new HttpClientResponseImpl(resCode, httpConnection.getResponseMessage(),  is, contentLength);
        clientResponse.setContentType(resContentType);
        clientResponse.setHeaders(httpConnection.getHeaderFields());

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
                if(itemContentType != null && itemContentType.length() > 0) {
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
