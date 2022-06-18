package com.wast.test.yaml;

import io.github.wycst.wast.common.utils.StringUtils;
import io.github.wycst.wast.yaml.YamlDocument;
import io.github.wycst.wast.yaml.YamlNode;

import java.io.File;
import java.io.IOException;
import java.util.Properties;

/**
 * @Author: wangy
 * @Description:
 */
public class YamlTest {

    private String kind;

    public String getKind() {
        return kind;
    }

    public void setKind(String kind) {
        this.kind = kind;
    }

    public static void main(String[] args) throws IOException {

        String yamlStr = StringUtils.fromResource("/yaml/t2.yaml");

        // 读取文档
        YamlDocument yamlDoc = YamlDocument.parse(yamlStr);

        // 转换为properties
        Properties properties = yamlDoc.toProperties();
        System.out.println(properties);

        // 转换为map
        yamlDoc.toMap();

        // 转化为指定bean
        YamlTest bean = yamlDoc.toEntity(YamlTest.class);

        // 获取根节点
        YamlNode yamlRoot = yamlDoc.getRoot();

        // 查找node
        YamlNode nameNode = yamlRoot.get("/metadata/name");

        // 获取/metadata/name
        String metadataName = yamlRoot.getPathValue("/metadata/name", String.class);
        // 或者 nameNode.getValue();
        System.out.println(" metadataName " + metadataName);

        // 修改
        yamlRoot.setPathValue("/metadata/name", "this is new Value ");

        String newMetadataName = (String) nameNode.getValue();
        System.out.println(newMetadataName.equals("this is new Value "));

        // 反向序列化生成yaml字符串
        System.out.println(yamlDoc.toYamlString());

        // 输出到文件
        yamlDoc.writeTo(new File("/tmp/test.yaml"));

    }

}
