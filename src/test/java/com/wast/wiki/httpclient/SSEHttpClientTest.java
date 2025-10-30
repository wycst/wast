package com.wast.wiki.httpclient;

import io.github.wycst.wast.clients.http.HttpClient;
import io.github.wycst.wast.clients.http.definition.HttpClientConfig;
import io.github.wycst.wast.clients.http.definition.HttpClientMethod;
import io.github.wycst.wast.clients.http.definition.HttpClientResponse;
import io.github.wycst.wast.clients.http.event.EventSourceCallback;
import io.github.wycst.wast.clients.http.event.EventSourceHandler;
import io.github.wycst.wast.clients.http.event.EventSourceMessage;

import java.util.HashMap;
import java.util.Map;

public class SSEHttpClientTest {

    public static void main(String[] args) throws InterruptedException {

        HttpClient httpClient = new HttpClient();
        Map<String, Object> headers = new HashMap<String, Object>();
        headers.put("authorization", "Bearer eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiJ9.eyJ1aWQiOiIxIiwicGNvZGUiOiI3MDAxIiwidW4iOiJkZXYiLCJybiI6IueuoeeQhuWRmCIsInJpZCI6ImF2bHNHYW1DY0dhdGNEYmlLUUkiLCJleHAiOjE3NTkxNTg2MDgsIm1ybGUiOiIwIiwidGlkIjoiZGVmYXVsdF90ZW5hbnQifQ.kl2oNVb2mlmSctPWCFC9MlhHS3pdatl2IptgyPCFZag");
        headers.put("client-id", "f7cce9099aadc57e677e548f34d0a4a9");
        headers.put("Connection", "keep-alive");
        headers.put("assistant-key", "langchat-8c2f9bab5c1147fc90aa00dadc679382");

        String json =  "{\"conversationId\":\"1c76578e-30ed-4eac-b5ba-b4f62a9ea004\",\"messages\":[{\"role\":\"user\",\"content\":\"你能做什么?\"}]}";
        HttpClientConfig clientConfig = HttpClientConfig.create()
                .headers(headers)
                .contentType("application/json")
                .jsonBody(json) // 自动设置 application/json，支持直接传入对象或者序列化好的JSON字符串（内部自动检测）
                .retry(-1, 10) // 重试参数： -1代表无限次重连， 10为每次重连间隔
                .responseStream(true);  // 开启流式

        String url = "http://192.168.1.146:30080/v1/chat/completions";
        EventSourceHandler eventSourceHandler = httpClient.eventSource(url, HttpClientMethod.POST, clientConfig, new EventSourceCallback() {
            @Override
            public void onmessage(EventSourceMessage message) {
                System.out.println("data: " + message.getData());
            }

            @Override
            public void onopen(HttpClientResponse response) {
                System.out.println("open " + response);
            }

            @Override
            public void onclose() {
                System.out.println("close");
            }
        });

        System.out.println(eventSourceHandler.isSuccess());
    }
}
