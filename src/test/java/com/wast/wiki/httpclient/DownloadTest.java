package com.wast.wiki.httpclient;

import io.github.wycst.wast.clients.http.HttpClient;
import io.github.wycst.wast.clients.http.definition.HttpClientConfig;
import io.github.wycst.wast.clients.http.definition.ResponseCallback;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.OutputStream;

public class DownloadTest {

    public static void main(String[] args) throws FileNotFoundException {
        // 异步下载(支持下载进度显示)
        String url = "https://download.java.net/java/GA/jdk21.0.2/f2283984656d49d69e91c558476027ac/13/GPL/openjdk-21.0.2_linux-aarch64_bin.tar.gz";
        HttpClientConfig clientConfig = HttpClientConfig.
                create()
                .responseCallback(new ResponseCallback() {
                    @Override
                    public void onDownloadProgress(long downloaded, long total) {
                        System.out.println("downloaded " + downloaded + " total " + total + " progress " + (downloaded * 100 / total) + "%");
                    }
                });
        // 指定输出文件流
        OutputStream target = new FileOutputStream("E:/tmp/openjdk-21.0.2_linux-aarch64_bin-1.tar.gz");
        HttpClient.create().download(url, clientConfig, target);

        // 普通默认下载（${用户目录}/Downloads/${fileName}）
        HttpClient.create().download(
                "http://localhost:8818/rest/monitor-business-server/toml/downloadTemplate?tabCode=Redfish&gatherCode=0ee1318aa2394348b16c29182772e10d",
                HttpClientConfig.create().downloadFileName("test.xlsx"));  // 如果不指定文件名，则使用服务端返回的Content-Disposition中的文件名或者url中的文件名
    }

}
