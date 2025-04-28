package com.wast.wiki.httpclient;

import io.github.wycst.wast.clients.http.HttpClient;
import io.github.wycst.wast.clients.http.definition.HttpClientConfig;
import io.github.wycst.wast.clients.http.definition.HttpClientMethod;
import io.github.wycst.wast.clients.http.definition.HttpClientResponse;
import io.github.wycst.wast.clients.http.impl.HttpClientRequestBuilder;

import java.util.HashMap;
import java.util.Map;

public class HttpClientTest {

    // 线程安全，可以全局使用一个
    static HttpClient httpClient = new HttpClient();

    public static void testGet() {
        System.out.println(httpClient.get("http://www.baidu.com"));
        System.out.println(httpClient.get("https://developer.aliyun.com/artifact/aliyunMaven/searchArtifactByGav?groupId=spring&artifactId=&version=&repoId=all&_input_charset=utf-8", Map.class));
        // groupId=spring&artifactId=&version=&repoId=all&_input_charset=utf-8
        Map<String, Object> params = new HashMap<String, Object>();
        params.put("groupId", "spring");
        params.put("artifactId", "");
        params.put("version", "");
        params.put("repoId", "all");
        params.put("_input_charset", "utf-8");
        Map result = httpClient.get(
                "https://developer.aliyun.com/artifact/aliyunMaven/searchArtifactByGav?",
                params,
                Map.class,
                HttpClientConfig.create().header("token", "ttt")
        );
        System.out.println(result);
    }

    public static void testPost() {
        System.out.println(httpClient.post("http://www.baidu.com"));
        System.out.println(httpClient.post("https://developer.aliyun.com/artifact/aliyunMaven/searchArtifactByGav?groupId=spring&artifactId=&version=&repoId=all&_input_charset=utf-8"));;
        Map<String, Object> body = new HashMap<String, Object>();

        // post json
        Map result = httpClient.postJson("http://www.baidu.com", Map.class, body);
        httpClient.postJson("http://www.baidu.com", Map.class, body, HttpClientConfig.create().header("token", ""));
    }


    public static void main(String[] args) {
        //1.通用方法支持(GET, HEAD, POST, PUT, PATCH, DELETE, OPTIONS, TRACE)
        HttpClientResponse httpClientResponse = httpClient.request("http://www.baidu.com", HttpClientMethod.GET, HttpClientResponse.class, HttpClientConfig.create());

        //2.任意方法支持(http实际上并不限制你用什么方法，前提服务端没有限制能接收处理)
        String POST2 = "POST2";
        httpClientResponse = httpClient.executeRequest(HttpClientRequestBuilder.buildRequest("http://www.baidu.com", POST2, HttpClientConfig.create()));

        // 简化的get和post案例
        testGet();
        testPost();
        // testPut testDelete 和 testGet 和 testPost用法一致
    }

}
