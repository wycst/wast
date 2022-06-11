package io.github.wycst.wast.clients.http;

import io.github.wycst.wast.clients.http.consts.HttpHeaderValues;
import io.github.wycst.wast.clients.http.definition.*;
import io.github.wycst.wast.clients.http.impl.*;
import io.github.wycst.wast.clients.http.definition.*;
import io.github.wycst.wast.clients.http.impl.*;

import java.net.URLEncoder;
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
    public String get(String url) {
        return get(url, String.class);
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

    public String stringify(Map<String, Object> params) {
        StringBuilder builder = new StringBuilder();
        int length = params.size();
        int i = 0;
        for (String key : params.keySet()) {
            // if npc exception ?
            String value = params.get(key).toString();
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
        if (params != null && params.size() > 0) {
            String paramString = stringify(params);
            if (url.indexOf("?") > -1) {
                url += "&" + paramString;
            } else {
                url += "?" + paramString;
            }
        }
        HttpClientRequest httpRequest = new HttpClientGet(url);
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

    public String post(String url) {
        return post(url, String.class);
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
    public String upload(String url, HttpClientConfig requestConfig) {
        return upload(url, String.class, requestConfig);
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
     * 带配置的put请求(multipart = false)
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
     * post json请求
     *
     * @param url
     * @param rtnType
     * @param entity
     * @param <E>
     * @return
     */
    public <E> E postJson(String url, Class<E> rtnType, Object entity) {
        HttpClientConfig requestConfig = new HttpClientConfig();
        requestConfig.setRequestBody(entity, HttpHeaderValues.APPLICATION_JSON, true);
        HttpClientRequest httpRequest = new HttpClientPost(url, requestConfig);
        HttpClientResponse httpResponse = executeRequest(httpRequest);
        return httpResponse.getEntity(rtnType);
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
        requestConfig.setRequestBody(entity, HttpHeaderValues.APPLICATION_JSON, true);
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
    public String delete(String url) {
        return delete(url, String.class);
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
        if (params != null && params.size() > 0) {
            String paramString = stringify(params);
            if (url.indexOf("?") > -1) {
                url += "&" + paramString;
            } else {
                url += "?" + paramString;
            }
        }
        HttpClientRequest httpRequest = new HttpClientDelete(url);
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

}
