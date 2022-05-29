package com.light.test.json.performance;

import org.framework.light.common.reflect.GenericParameterizedType;
import org.framework.light.json.JSON;
import org.framework.light.json.JSONReader;
import org.framework.light.json.options.WriteOption;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.sql.Time;
import java.sql.Timestamp;
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
//            json = JSON.toJsonString(simpleBean);
            result = JSON.parseObject(json, SimpleBean.class);

//            FileInputStream fis = new FileInputStream(new File("E:/tmp/a.json"));
//            InputStreamReader isr = new InputStreamReader(fis);
//            char[] buf = new char[2048];
//            int len = isr.read(buf);
//            buf = Arrays.copyOfRange(buf, 0, len);
//            result = (SimpleBean) JSON.parse(buf, SimpleBean.class);


//            result = JSON.read(new File("E:/tmp/a.json"), SimpleBean.class);
//            result = (SimpleBean) JSONReader.from(new File("E:/tmp/a.json")).readAsResult(SimpleBean.class);
        }

        long end = System.currentTimeMillis();
        System.out.println(" serielize use " + (end - begin));

    }

}
