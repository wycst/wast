package com.wast.test.json;

import io.github.wycst.wast.json.JSON;
import io.github.wycst.wast.json.options.WriteOption;

import java.util.HashMap;
import java.util.Map;

/**
 * @Author wangyunchao
 */
public class JSONSerializeTest {

    static void test1() {
        Map map = new HashMap();
        map.put("name", "zhangsan");
        map.put("msg", "hello, light json !");
        String json = JSON.toJsonString(map);
        System.out.println(String.format("test1 - result: \n%s", json));
    }

    static void test2() {
        Map map = new HashMap();
        map.put("name", "zhangsan");
        map.put("msg", "hello, light json !");
        String json = JSON.toJsonString(map, WriteOption.FormatOut);
        System.out.println(String.format("test2 - result: \n%s", json));
    }

    public static void main(String[] args) {
        test1();
        test2();
    }
}
