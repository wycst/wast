package com.wast.wiki.json;

import com.wast.wiki.beans.RestResult;
import com.wast.wiki.beans.UserFact;
import io.github.wycst.wast.common.reflect.GenericParameterizedType;
import io.github.wycst.wast.json.JSON;
import io.github.wycst.wast.json.options.ReadOption;

import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * @Date 2024/11/13
 * @Created by wangyc
 */
public class DeserializeExample {

    static final String userFactJson = "{\"addr\":\"asdfgh\",\"userId\":1,\"userName\":\"test\"}";
    static final String userFactCommontJson =
            "/**\n" +
                    " * 注释\n" +
                    " */\n" +
                    "{\n" +
                    "\t\"addr\": \"asdfgh\",   // 地址\n" +
                    "\t\"userId\": 1,        // id\n" +
                    "\t\"userName\": \"test\"  // 用户名\n" +
                    "}";
    static final String mapJson = "{\"msg\":\"hello, wastjson!\"}";
    static final String listJson = "[{\"msg\":\"hello, wastjson!\"},{\"msg\":\"hello, wastjson!\"}]";

    static final String restResultJson = "{\"data\":{\"addr\":\"asdfgh\",\"userId\":1,\"userName\":\"test\"},\"msg\":\"success\",\"status\":\"200\"}";

    public static void main(String[] args) throws IOException {
        deserializeTest();
        deserializeComment();
        deserializeGeneric();
    }

    /**
     * 1.反序列化map/list/pojo
     */
    private static void deserializeTest() {
        // map类
        Map map = (Map) JSON.parse(mapJson);
        Map map1 = JSON.parseMap(mapJson, LinkedHashMap.class);
        System.out.println(map);

        // 集合类
        List list = (List) JSON.parse(listJson);
        List list1 = JSON.parseArray(listJson, Object.class);
        System.out.println(list);

        // pojo类
        UserFact userFact = JSON.parseObject(userFactJson, UserFact.class);
        System.out.println(JSON.toJsonString(userFact));
    }

    /**
     * 2.解析带注释的JSON
     */
    private static void deserializeComment() {
        // pojo类
        UserFact userFact = JSON.parseObject(userFactCommontJson, UserFact.class, ReadOption.AllowComment);
        System.out.println(JSON.toJsonString(userFact));
    }

    /**
     * 3.解析伪泛型场景
     */
    private static void deserializeGeneric() {
        // 伪泛型为单泛型场景可以简单如下处理
        GenericParameterizedType<RestResult> parameterizedType = GenericParameterizedType.entityType(RestResult.class, UserFact.class);
        RestResult userFact = JSON.parse(restResultJson, parameterizedType);
        System.out.println(userFact.getData().getClass() == UserFact.class);

        // 伪泛型为多个泛型组合的场景
        Map<String, Class<?>> genericClassMap = new HashMap<String, Class<?>>();
        genericClassMap.put("T", UserFact.class); // T为RestResult类上的泛型声明
        GenericParameterizedType<RestResult> parameterizedType2 = GenericParameterizedType.entityType(RestResult.class, genericClassMap);
        RestResult userFact2 = JSON.parse(restResultJson, parameterizedType2);
        System.out.println(userFact2.getData().getClass() == UserFact.class);
    }
}
