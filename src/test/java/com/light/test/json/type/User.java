package com.light.test.json.type;

import org.framework.light.json.JSON;

import java.util.HashMap;
import java.util.Map;

/**
 * @Author: wangy
 * @Date: 2021/12/6 21:10
 * @Description:
 */
public class User<T> {

    private Map<String, Fact> test;

    private Fact fact;//= new Fact();

    public User() {

    }

    public Map<String, Fact> getTest() {
        return test;
    }

    public void setTest(Map<String, Fact> test) {
        this.test = test;
    }

    public Data getFact() {
        return fact;
    }

    public void setFact(Fact fact) {
        this.fact = fact;
    }

    public static void main(String[] args) {

        User user = new User();
        Map<String, Fact> test = new HashMap<String, Fact>();

        Fact one = new Fact();
        one.setId("sdsds");
        one.setName("1");
        test.put("1", one);

        Fact two = new Fact();
        two.setId("2");
        two.setName("2");
        test.put("2", two);
        user.setTest(test);

        user.fact = two;

        String json = JSON.toJsonString(user);
        System.out.println(json);
        User s = null;
        long begin = System.currentTimeMillis();
        for (int i = 0; i < 100000; i++) {
            s = JSON.parseObject(json, User.class);
        }
        long end = System.currentTimeMillis();
        System.out.println(end - begin);
    }


}
