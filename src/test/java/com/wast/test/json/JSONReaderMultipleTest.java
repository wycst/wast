package com.wast.test.json;

import io.github.wycst.wast.json.JSONReader;

import java.io.InputStream;

/**
 * 支持一个文件或者流中的内容存在多个json的场景解析
 *
 * <p>
 * jsonReader.setMultiple(true);
 * </p>
 *
 * @Author: wangy
 * @Description:
 */
public class JSONReaderMultipleTest {

    public static void main(String[] args) {
        InputStream is = JSONReaderTest.class.getResourceAsStream("/json/multiple_jsons.txt");
        JSONReader jsonReader = new JSONReader(is);
        jsonReader.setMultiple(true);

        // 通过skipNext可以跳过指定的json数据
        jsonReader.skipNext(5);
        Object obj = null;
        while ((obj = jsonReader.read()) != null) {
            System.out.println(obj);
        }

        // 注意最后需要手动关闭流
        jsonReader.close();
    }
}
