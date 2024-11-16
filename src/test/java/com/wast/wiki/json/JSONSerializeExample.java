package com.wast.wiki.json;

import com.wast.wiki.beans.UserFact;
import io.github.wycst.wast.json.JSON;
import io.github.wycst.wast.json.options.WriteOption;

import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @Date 2024/11/13
 * @Created by wangyc
 */
public class JSONSerializeExample {


    public static void main(String[] args) throws IOException {
        serializeTest();
        serializeToOutputStream();
        serializePretty();
        serializeUnderline();
        serializeCustom();
    }

    /**
     * 1.常规序列化
     */
    private static void serializeTest() {
        // map类
        Map map = new HashMap();
        map.put("msg", "hello, wastjson!");
        String mapJson = JSON.toJsonString(map);
        System.out.println(mapJson);

        // 集合类
        List list = new ArrayList();
        list.add(map);
        list.add(map);
        System.out.println(JSON.toJsonString(list));

        // pojo类
        UserFact userFact = new UserFact();
        userFact.setUserId(1);
        userFact.setUserName("test");
        System.out.println(JSON.toJsonString(userFact));
    }

    /**
     * 2.写入文件或者输出流
     */
    private static void serializeToOutputStream() throws IOException {
        // 文件
        Map map = new HashMap();
        map.put("msg", "hello, wastjson !");
        JSON.writeJsonTo(map, new File("/tmp/test.json"));

        // 输出流
        OutputStream os = new FileOutputStream("/tmp/test.json");
        JSON.writeJsonTo(map, os);

        // 或者自定义的writer
        Writer writer = new FileWriter("/tmp/test.json");
        JSON.writeJsonTo(map, writer);
    }


    /**
     * 3.美化输出
     */
    private static void serializePretty() {
        UserFact userFact = new UserFact();
        userFact.setUserId(1);
        userFact.setUserName("test");
        System.out.println(JSON.toJsonString(userFact, WriteOption.FormatOutColonSpace));
    }

    /**
     * 4.pojo输出下划线
     */
    private static void serializeUnderline() {
        UserFact userFact = new UserFact();
        userFact.setUserId(1);
        userFact.setUserName("test");
        System.out.println(JSON.toJsonString(userFact, WriteOption.FormatOutColonSpace, WriteOption.CamelCaseToUnderline));
    }

    /**
     * 5.pojo定制输出
     */
    private static void serializeCustom() {
        UserFact userFact = new UserFact();
        userFact.setUserId(1);
        userFact.setUserName("test");
        userFact.setAddress("asdfgh");
        userFact.setPassword("1235");
        System.out.println(JSON.toJsonString(userFact, WriteOption.FormatOutColonSpace));
    }

}
