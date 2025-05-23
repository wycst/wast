package io.github.wycst.wast.clients.http;

import io.github.wycst.wast.clients.http.definition.*;
import io.github.wycst.wast.clients.http.impl.*;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Map;

/**
 * @Author: wangy
 * @Date: 2020/7/2 16:36
 * @Description:
 */
public class HttpClient extends AbstractHttpClient {

    // 创建实例
    public static HttpClient create() {
        return new HttpClient();
    }

    /**
     * 无参数http请求
     *
     * @param url
     * @param method
     * @param rtnType
     * @param <E>
     * @return
     */
    public <E> E request(String url, HttpClientMethod method, Class<E> rtnType) {
        HttpClientRequest httpRequest = HttpClientRequestBuilder.buildRequest(url, method);
        HttpClientResponse httpResponse = executeRequest(httpRequest);
        return httpResponse.getEntity(rtnType);
    }

    /**
     * 定制配置参数进行http请求
     *
     * @param url
     * @param method
     * @param rtnType
     * @param requestConfig
     * @param <E>
     * @return
     */
    public <E> E request(String url, HttpClientMethod method, Class<E> rtnType, HttpClientConfig requestConfig) {
        HttpClientRequest httpRequest = HttpClientRequestBuilder.buildRequest(url, method, requestConfig);
        HttpClientResponse httpResponse = executeRequest(httpRequest);
        return httpResponse.getEntity(rtnType);
    }

    /**
     * simple get request
     *
     * @param url
     * @return
     */
    public HttpClientResponse get(String url) {
        return get(url, HttpClientResponse.class);
    }

    /**
     * 简单的get请求
     *
     * @param url
     * @param rtnType
     * @param <E>
     * @return
     */
    public <E> E get(String url, Class<E> rtnType) {
        HttpClientRequest httpRequest = new HttpClientGet(url);
        HttpClientResponse httpResponse = executeRequest(httpRequest);
        return httpResponse.getEntity(rtnType);
    }

    /**
     * 带配置的get请求
     *
     * @param url
     * @param headers
     * @return
     */
    public HttpClientResponse get(String url, Map<String, String> headers) {
        HttpClientConfig requestConfig = new HttpClientConfig();
        requestConfig.setHeaders(headers);
        HttpClientRequest httpRequest = new HttpClientGet(url, requestConfig);
        return executeRequest(httpRequest);
    }

    /**
     * 带配置的get请求
     *
     * @param url
     * @param params
     * @param headers
     * @return
     */
    public HttpClientResponse get(String url, Map<String, Object> params, Map<String, String> headers) {
        HttpClientConfig requestConfig = new HttpClientConfig();
        requestConfig.setHeaders(headers);
        HttpClientRequest httpRequest = new HttpClientGet(toStringifyUrl(url, params), requestConfig);
        return executeRequest(httpRequest);
    }

    /**
     * 带配置的get请求
     *
     * @param url
     * @param requestConfig
     * @return
     */
    public HttpClientResponse get(String url, HttpClientConfig requestConfig) {
        HttpClientRequest httpRequest = new HttpClientGet(url, requestConfig);
        return executeRequest(httpRequest);
    }

    /**
     * 带map参数的get请求
     *
     * @param url
     * @param params
     * @param rtnType
     * @param <E>
     * @return
     */
    public <E> E get(String url, Map<String, Object> params, Class<E> rtnType) {
        HttpClientRequest httpRequest = new HttpClientGet(toStringifyUrl(url, params));
        HttpClientResponse httpResponse = executeRequest(httpRequest);
        return httpResponse.getEntity(rtnType);
    }

    /**
     * 带配置的get请求
     *
     * @param url
     * @param rtnType
     * @param requestConfig
     * @param <E>
     * @return
     */
    public <E> E get(String url, Class<E> rtnType, HttpClientConfig requestConfig) {
        HttpClientRequest httpRequest = new HttpClientGet(url, requestConfig);
        HttpClientResponse httpResponse = executeRequest(httpRequest);
        return httpResponse.getEntity(rtnType);
    }

    /**
     * 带配置的get请求
     *
     * @param url
     * @param rtnType
     * @param requestConfig
     * @param <E>
     * @return
     */
    public <E> E get(String url, Map<String, Object> params, Class<E> rtnType, HttpClientConfig requestConfig) {
        HttpClientRequest httpRequest = new HttpClientGet(toStringifyUrl(url, params), requestConfig);
        HttpClientResponse httpResponse = executeRequest(httpRequest);
        return httpResponse.getEntity(rtnType);
    }

    /**
     * 下载文件
     *
     * @param url
     * @param file
     * @return
     */
    public Throwable download(String url, File file) {
        return download(url, null, file);
    }

    /**
     * 下载文件
     *
     * @param url
     * @param headers
     * @param file
     * @return
     */
    public Throwable download(String url, Map<String, String> headers, File file) {
        FileOutputStream fos = null;
        try {
            byte[] bytes = download(url, headers);
            if (bytes == null) {
                return new IOException("empty data");
            }
            fos = new FileOutputStream(file);
            fos.write(bytes);
            fos.flush();
            return null;
        } catch (Throwable throwable) {
            return throwable;
        } finally {
            if (fos != null) {
                try {
                    fos.close();
                } catch (IOException e) {
                }
            }
        }
    }

    public HttpClientResponse post(String url) {
        return post(url, HttpClientResponse.class);
    }

    /**
     * 简单的post请求
     *
     * @param url
     * @param rtnType
     * @param <E>
     * @return
     */
    public <E> E post(String url, Class<E> rtnType) {
        HttpClientRequest httpRequest = new HttpClientPost(url);
        HttpClientResponse httpResponse = executeRequest(httpRequest);
        return httpResponse.getEntity(rtnType);
    }

    /**
     * post请求
     *
     * @param url
     * @param requestConfig
     * @return
     */
    public HttpClientResponse post(String url, HttpClientConfig requestConfig) {
        return post(url, HttpClientResponse.class, requestConfig);
    }

    /**
     * 带配置的post请求(multipart = false)
     *
     * @param url
     * @param rtnType
     * @param requestConfig
     * @param <E>
     * @return
     */
    public <E> E post(String url, Class<E> rtnType, HttpClientConfig requestConfig) {
        HttpClientRequest httpRequest = new HttpClientPost(url, requestConfig);
        HttpClientResponse httpResponse = executeRequest(httpRequest);
        return httpResponse.getEntity(rtnType);
    }

    /**
     * 上传请求
     *
     * @param url
     * @param requestConfig
     * @return
     */
    public HttpClientResponse upload(String url, HttpClientConfig requestConfig) {
        return upload(url, HttpClientResponse.class, requestConfig);
    }

    /**
     * 上传请求
     *
     * @param url
     * @param rtnType
     * @param requestConfig
     * @param <E>
     * @return
     */
    public <E> E upload(String url, Class<E> rtnType, HttpClientConfig requestConfig) {
        HttpClientPost httpRequest = new HttpClientPost(url, requestConfig);
        httpRequest.setMultipart(true);
        HttpClientResponse httpResponse = executeRequest(httpRequest);
        return httpResponse.getEntity(rtnType);
    }

    /**
     * 上传请求
     *
     * @param url
     * @param rtnType
     * @param <E>
     * @return
     */
    public <E> E upload(String url, Class<E> rtnType, HttpClientParameter... clientParameters) {
        HttpClientConfig requestConfig = new HttpClientConfig();
        requestConfig.addTextParameters(clientParameters);
        HttpClientPost httpRequest = new HttpClientPost(url, requestConfig);
        httpRequest.setMultipart(true);
        HttpClientResponse httpResponse = executeRequest(httpRequest);
        return httpResponse.getEntity(rtnType);
    }

    /**
     * 简单的put请求
     *
     * @param url
     * @param rtnType
     * @param <E>
     * @return
     */
    public <E> E put(String url, Class<E> rtnType) {
        HttpClientRequest httpRequest = new HttpClientPut(url);
        HttpClientResponse httpResponse = executeRequest(httpRequest);
        return httpResponse.getEntity(rtnType);
    }

    /**
     * put请求
     *
     * @param url
     * @param requestConfig
     * @return
     */
    public HttpClientResponse put(String url, HttpClientConfig requestConfig) {
        return put(url, HttpClientResponse.class, requestConfig);
    }

    /**
     * 带配置的put请求
     *
     * @param url
     * @param rtnType
     * @param requestConfig
     * @param <E>
     * @return
     */
    public <E> E put(String url, Class<E> rtnType, HttpClientConfig requestConfig) {
        HttpClientRequest httpRequest = new HttpClientPut(url, requestConfig);
        HttpClientResponse httpResponse = executeRequest(httpRequest);
        return httpResponse.getEntity(rtnType);
    }

    /**
     * 带配置的patch请求
     *
     * @param url
     * @param rtnType
     * @param requestConfig
     * @param <E>
     * @return
     */
    public <E> E patch(String url, Class<E> rtnType, HttpClientConfig requestConfig) {
        HttpClientRequest httpRequest = HttpClientRequestBuilder.buildRequest(url, HttpClientMethod.PATCH, requestConfig);
        HttpClientResponse httpResponse = executeRequest(httpRequest);
        return httpResponse.getEntity(rtnType);
    }

//    /**
//     * post form请求
//     *
//     * @param url
//     * @param rtnType
//     * @param entity
//     * @param <E>
//     * @return
//     */
//    public <E> E postForm(String url, Class<E> rtnType, Object entity) {
//        HttpClientConfig requestConfig = new HttpClientConfig();
//        requestConfig.setRequestBody(entity, HttpHeaderValues.APPLICATION_X_WWW_FORM_URLENCODED, false);
//        HttpClientRequest httpRequest = new HttpClientPost(url, requestConfig);
//        HttpClientResponse httpResponse = executeRequest(httpRequest);
//        return httpResponse.getEntity(rtnType);
//    }
//
//    /**
//     * put form请求
//     *
//     * @param url
//     * @param rtnType
//     * @param entity
//     * @param <E>
//     * @return
//     */
//    public <E> E putForm(String url, Class<E> rtnType, Object entity) {
//        HttpClientConfig requestConfig = new HttpClientConfig();
//        requestConfig.setRequestBody(entity, HttpHeaderValues.APPLICATION_X_WWW_FORM_URLENCODED, false);
//        HttpClientRequest httpRequest = new HttpClientPut(url, requestConfig);
//        HttpClientResponse httpResponse = executeRequest(httpRequest);
//        return httpResponse.getEntity(rtnType);
//    }

    /**
     * post json 请求
     *
     * <p> 返回响应结果
     *
     * @param url
     * @param entity
     * @return
     */
    public final HttpClientResponse postJson(String url, Object entity) {
        return postJson(url, HttpClientResponse.class, entity);
    }

    /**
     * post json请求
     *
     * @param url
     * @param rtnType
     * @param entity
     * @param <E>
     * @return
     */
    public final <E> E postJson(String url, Class<E> rtnType, Object entity) {
        return doPostJson(url, rtnType, entity, EMPTY_HEADERS, new HttpClientConfig());
    }

    /**
     * post json请求
     *
     * @param url
     * @param rtnType
     * @param entity
     * @param headers
     * @param <E>
     * @return
     */
    public <E> E postJson(String url, Class<E> rtnType, Object entity, Map<String, String> headers) {
        return doPostJson(url, rtnType, entity, headers == null ? EMPTY_HEADERS : headers, new HttpClientConfig());
    }

    /**
     * post json请求
     *
     * @param url
     * @param rtnType
     * @param entity
     * @param requestConfig
     * @param <E>
     * @return
     */
    public <E> E postJson(String url, Class<E> rtnType, Object entity, HttpClientConfig requestConfig) {
        return doPostJson(url, rtnType, entity, EMPTY_HEADERS, requestConfig);
    }

    final <E> E doPostJson(String url, Class<E> rtnType, Object entity, Map<String, String> headers, HttpClientConfig requestConfig) {
        requestConfig.setHeaders(headers);
        requestConfig.setJsonBody(entity);
        HttpClientRequest httpRequest = new HttpClientPost(url, requestConfig);
        HttpClientResponse httpResponse = executeRequest(httpRequest);
        return httpResponse.getEntity(rtnType);
    }

    /**
     * put json 请求
     *
     * <p> 返回响应结果
     *
     * @param url
     * @param entity
     * @return
     */
    public HttpClientResponse putJson(String url, Object entity) {
        return putJson(url, HttpClientResponse.class, entity);
    }

    /**
     * put json请求
     *
     * @param url
     * @param rtnType
     * @param entity
     * @param <E>
     * @return
     */
    public <E> E putJson(String url, Class<E> rtnType, Object entity) {
        HttpClientConfig requestConfig = new HttpClientConfig();
        requestConfig.setJsonBody(entity);
        HttpClientRequest httpRequest = new HttpClientPut(url, requestConfig);
        HttpClientResponse httpResponse = executeRequest(httpRequest);
        return httpResponse.getEntity(rtnType);
    }

    /**
     * simple delete request
     *
     * @param url
     * @return
     */
    public HttpClientResponse delete(String url) {
        return delete(url, HttpClientResponse.class);
    }

    /**
     * 简单的delete请求
     *
     * @param url
     * @param rtnType
     * @param <E>
     * @return
     */
    public <E> E delete(String url, Class<E> rtnType) {
        HttpClientRequest httpRequest = new HttpClientDelete(url);
        HttpClientResponse httpResponse = executeRequest(httpRequest);
        return httpResponse.getEntity(rtnType);
    }

    /**
     * 带map参数的delete请求
     *
     * @param url
     * @param params
     * @param rtnType
     * @param <E>
     * @return
     */
    public <E> E delete(String url, Map<String, Object> params, Class<E> rtnType) {
        HttpClientRequest httpRequest = new HttpClientDelete(toStringifyUrl(url, params));
        HttpClientResponse httpResponse = executeRequest(httpRequest);
        return httpResponse.getEntity(rtnType);
    }

    /**
     * 带配置的delete请求
     *
     * @param url
     * @param rtnType
     * @param requestConfig
     * @param <E>
     * @return
     */
    public <E> E delete(String url, Class<E> rtnType, HttpClientConfig requestConfig) {
        HttpClientRequest httpRequest = new HttpClientDelete(url, requestConfig);
        HttpClientResponse httpResponse = executeRequest(httpRequest);
        return httpResponse.getEntity(rtnType);
    }

    /**
     * 带配置的delete请求
     *
     * @param url
     * @param rtnType
     * @param requestConfig
     * @param <E>
     * @return
     */
    public <E> E delete(String url, Map<String, Object> params, Class<E> rtnType, HttpClientConfig requestConfig) {
        HttpClientRequest httpRequest = new HttpClientDelete(toStringifyUrl(url, params), requestConfig);
        HttpClientResponse httpResponse = executeRequest(httpRequest);
        return httpResponse.getEntity(rtnType);
    }
}
