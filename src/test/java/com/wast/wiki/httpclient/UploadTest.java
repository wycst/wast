package com.wast.wiki.httpclient;

import io.github.wycst.wast.clients.http.HttpClient;
import io.github.wycst.wast.clients.http.definition.HttpClientConfig;
import io.github.wycst.wast.clients.http.definition.HttpClientResponse;

import java.io.File;

public class UploadTest {

    public static void main(String[] args) {
        HttpClientResponse response = HttpClient.create().upload("http://www.baidu.com",
                HttpClientConfig.create()
                        .textParameter("name", "xxxx")
                        .fileParameter("file", new File("D:/download/aaasdsd.xlsx"), null)
                        .fileParameter("file2", "a.txt",  "hello upload test".getBytes(), null)
                );
        System.out.println(response);
    }

}
