package com.wast.test.yaml;

import io.github.wycst.wast.common.utils.StringUtils;
import io.github.wycst.wast.yaml.YamlDocument;

import java.io.IOException;
import java.util.Properties;

/**
 * @Author: wangy
 * @Description:
 */
public class YamlTest {

    public static void main(String[] args) throws IOException {

        String yamlStr = StringUtils.fromResource("/yaml/t2.yaml");

        // 读取文档
        YamlDocument yamlDoc = YamlDocument.read(YamlTest.class.getResourceAsStream("/yaml/t2.yaml"));

        // 转换为properties
        Properties properties = yamlDoc.toProperties();
        System.out.println(properties);

    }

}
