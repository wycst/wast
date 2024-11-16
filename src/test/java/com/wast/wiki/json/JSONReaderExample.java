package com.wast.wiki.json;

import com.wast.test.json.JSONReaderTest;
import io.github.wycst.wast.json.JSONReader;
import io.github.wycst.wast.json.JSONReaderHook;
import io.github.wycst.wast.json.JSONReaderHookDefault;

import java.io.File;
import java.io.InputStream;
import java.util.List;
import java.util.Map;

/**
 * @Date 2024/11/14 13:16
 * @Created by wangyc
 */
public class JSONReaderExample {

    public static void main(String[] args) throws Exception {
        jsonReaderNormal();
        jsonReaderOnDemand();
        jsonReaderShortBack();
        jsonReaderAsync();
        jsonReaderMultiple();
    }

    /**
     * 1.常规场景
     */
    private static void jsonReaderNormal() {
        // 1.从流读取
        final JSONReader reader1 = JSONReader.from(JSONReaderExample.class.getResourceAsStream("/json/path.json"));
        Map map = (Map) reader1.read(); // 或者使用readAsResult转化为指定类型
        System.out.println(map);

        // 2.从文件读取
        final JSONReader reader2 = JSONReader.from(new File("e:/tmp/test.json"));
        Map map2 = (Map) reader2.read();
        System.out.println(map2);
    }

    /**
     * 2.支持按需读取模式(节省内存)
     */
    private static void jsonReaderOnDemand() {
        final JSONReader reader1 = JSONReader.from(JSONReaderExample.class.getResourceAsStream("/json/path.json"));
        // 根据路径匹配到/store/book/1
        JSONReaderHook readerHook = JSONReaderHook.regularPath("/store/book/1");
        reader1.read(readerHook);
        List<Object> results = readerHook.getResults();
        System.out.println(results);
    }

    /**
     * 3.支持短路中断模式(节省内存且性能高效)
     */
    private static void jsonReaderShortBack() {
        final JSONReader reader = JSONReader.from(JSONReaderExample.class.getResourceAsStream("/json/path.json"));
        // 1.使用内置的短路实现获取路径对象: /store/book/1
        JSONReaderHook readerHook = JSONReaderHook.exactPath("/store/book/1");
        reader.read(readerHook);
        List<Object> results = readerHook.getResults();
        System.out.println(results);

        // 2.自定义短路实现获取某个叶子节点后短路退出；
        JSONReaderHook readerHook2 = new JSONReaderHook() {
            @Override
            protected void parseValue(String key, Object value, Object host, int elementIndex, String path, int type) throws Exception {
                if ("/store/book/1/author".equals(path)) {
                    results.add(value);
                    // 获取到author后中断退出
                    abort();
                }
            }
        };

        final JSONReader reader2 = JSONReader.from(JSONReaderExample.class.getResourceAsStream("/json/path.json"));
        reader2.read(readerHook2);
        System.out.println(readerHook2.getResults());
    }

    /**
     * 4.支持异步读取
     */
    private static void jsonReaderAsync() {
        final JSONReader reader = JSONReader.from(JSONReaderExample.class.getResourceAsStream("/json/path.json"));
        reader.read(true);
        final JSONReader reader2 = JSONReader.from(JSONReaderExample.class.getResourceAsStream("/json/path.json"));
        reader2.read(true);

        // 也可以自定义hook，在onCompleted中完成业务处理
        final JSONReader reader3  = JSONReader.from(JSONReaderExample.class.getResourceAsStream("/json/path.json"));
        reader3.read(new JSONReaderHookDefault() {
            @Override
            protected void onCompleted(Object result) {
                System.out.println("async onCompleted " + result);
            }
        }, true);

        // 这里可以继续并行的处理其他业务
        // 两个读取任务基本上是同时完成的
        System.out.println("reader result1:" + reader.getResult());
        System.out.println(System.currentTimeMillis());
        System.out.println("reader result2:" + reader2.getResult());
        System.out.println(System.currentTimeMillis());
    }

    /**
     * 5.支持Multiple模式(一个文件中存在多个json)
     */
    private static void jsonReaderMultiple() {
        // 示例文件共10个json
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
