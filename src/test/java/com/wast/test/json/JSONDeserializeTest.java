package com.wast.test.json;

import io.github.wycst.wast.json.JSON;

import java.util.Map;

/**
 * @Author wangyunchao
 */
public class JSONDeserializeTest {

    static void test1() {
        String json = "{\"msg\":\"hello, light json !\",\"name\":\"zhangsan\"}";
        Map map = (Map) JSON.parse(json);
        System.out.println(map);
    }

    static void test2() {
        String json = "{\"msg\":\"hello, light json !\",\"name\":\"zhangsan\"}";
        Map map = JSON.parseObject(json, Map.class);
        System.out.println(map);
    }

    public static void main(String[] args) {
        test1();
        test2();
    }

}
