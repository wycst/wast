package com.wast.test.json.performance;

import io.github.wycst.wast.json.JSON;

import java.io.IOException;
import java.sql.Time;
import java.util.*;

/**
 * @Author: wangy
 * @Date: 2022/1/15 23:17
 * @Description:
 */
public class SimpleBeanTest {


    public static void main(String[] args) throws IOException {

        SimpleBean simpleBean = new SimpleBean();
        simpleBean.setId(1);
        simpleBean.setDate(new Date());
        simpleBean.setTime(new Time(System.currentTimeMillis()));
        simpleBean.setName("simple");
        simpleBean.setPercent(12.34);
        simpleBean.setVersion(System.currentTimeMillis());

        Map mapInstance = new HashMap();
        mapInstance.put("msg", "hello light");
        simpleBean.setMapInstance(mapInstance);

        List<Object> versions = new ArrayList<Object>();
        versions.add("v0.0.1");
        versions.add("v0.0.2");
        versions.add("v0.0.3");
        simpleBean.setList(versions);

        simpleBean.setSimpleEnum(SimpleBean.SimpleEnum.EnumOne);

        List<char[]> charsList = new ArrayList();
        charsList.add("hello".toCharArray());
        charsList.add("hello2".toCharArray());
        simpleBean.setCharsList(charsList);

        int count = 20000;
        String json = JSON.toJsonString(simpleBean);

//        JSON.writeJsonTo(simpleBean, new File("E:/tmp/a.json"), WriteOption.FormatOut);

//        SimpleBean simpleBean3 = JSON.read(new File("E:/tmp/a.json"), GenericParameterizedType.actualType(SimpleBean.class));

        SimpleBean result = null;

        System.out.println(json);

//        JSON.read(new File("E:/tmp/a.json"), null);
        long begin = System.currentTimeMillis();
        for (int i = 0; i < count; i++) {
            result = JSON.parseObject(json, SimpleBean.class);
        }

        long end = System.currentTimeMillis();
        System.out.println(" deserielize use " + (end - begin));
        System.out.println(JSON.toJsonString(result).equals(json));

    }

}
