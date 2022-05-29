package com.light.test.json.inputstream;

import org.framework.light.json.JSON;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Map;

/**
 * @Author: wangy
 * @Description:
 */
public class InputStreamTest {

    public static void main(String[] args) throws IOException {

        Map result = null;

        // 1 读取网络资源 GET
        result = JSON.read(new URL("https://developer.aliyun.com/artifact/aliyunMaven/searchArtifactByGav?groupId=spring&artifactId=&version=&repoId=all&_input_charset=utf-8"), Map.class);

        // 2 读取输入流
        InputStream inputStream = InputStreamTest.class.getResourceAsStream("/sample.json");
        result = JSON.read(inputStream, Map.class);

        // 3 读取文件
        result = JSON.read(new File("/tmp/smaple.json"), Map.class);

    }

}
