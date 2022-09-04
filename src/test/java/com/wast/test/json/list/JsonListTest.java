package com.wast.test.json.list;

import io.github.wycst.wast.json.JSON;

/**
 * @Author wangyunchao
 * @Date 2021/7/30 17:53
 */
public class JsonListTest {

    public static void main(String[] args) {

        long begin = System.currentTimeMillis();
        String json = "{\"account\":{\"loginUrl\":\"http://192.168.1.113:9191/login\",\"options\":[{\"uid\":\"17af1e06d17\",\"name\":\"邮箱/用户名\",\"defaultValue\":\"wangyunchao\",\"selectType\":\"id\",\"selectKey\":\"account\",\"elementType\":\"input\"},{\"uid\":\"17af1e06db2\",\"name\":\"密码\",\"defaultValue\":\"123456\",\"selectType\":\"id\",\"selectKey\":\"password\",\"elementType\":\"input\"},{\"uid\":\"17af68da958\",\"elementType\":\"event\",\"name\":\"记住密码\",\"eventType\":\"click\",\"selectType\":\"xpath\",\"selectKey\":\"/html/body/div[1]/div[1]/div/form/div[3]/label/input\"}],\"submit\":{\"selectType\":\"id\",\"selectKey\":\"btn-login\",\"timeout\":2,\"eventType\":\"click\"}},\"result\":[{},{}],\"token\":[]}";
        AuthInfo authInfo = null;
        for (int i = 0; i < 1000000; i++) {
            authInfo = io.github.wycst.wast.json.JSON.parseObject("{\"account\":{\"loginUrl\":\"http://192.168.1.113:9191/login\",\"options\":[{\"uid\":\"17af1e06d17\",\"name\":\"邮箱/用户名\",\"defaultValue\":\"wangyunchao\",\"selectType\":\"id\",\"selectKey\":\"account\",\"elementType\":\"input\"},{\"uid\":\"17af1e06db2\",\"name\":\"密码\",\"defaultValue\":\"123456\",\"selectType\":\"id\",\"selectKey\":\"password\",\"elementType\":\"input\"},{\"uid\":\"17af68da958\",\"elementType\":\"event\",\"name\":\"记住密码\",\"eventType\":\"click\",\"selectType\":\"xpath\",\"selectKey\":\"/html/body/div[1]/div[1]/div/form/div[3]/label/input\"}],\"submit\":{\"selectType\":\"id\",\"selectKey\":\"btn-login\",\"timeout\":2,\"eventType\":\"click\"}},\"token\":[],\"result\":[{},{}]}", AuthInfo.class);
        }
        long end = System.currentTimeMillis();
        System.out.println(" JsonListTest use(ms) " + (end - begin));
        System.out.println(json.equals(JSON.toJsonString(authInfo)));
    }

}
