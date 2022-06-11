package com.wast.test.json.anonymous;

import io.github.wycst.wast.json.JSON;
import io.github.wycst.wast.json.options.ReadOption;

/**
 * @Author wangyunchao
 * @Date 2022/1/5 12:48
 */
public class Test {

    private A a = new A() {
    };

    public A getA() {
        return a;
    }

    public void setA(A a) {
        this.a = a;
    }

    public static void main(String[] args) {
        Test test = new Test();
        test.a.setId("123");
        test.a.setName("456");

        String json = JSON.toJsonString(test);
        System.out.println(json);

        Test t2 = JSON.parseObject(json, Test.class, ReadOption.UseDefaultFieldInstance);
        System.out.println(t2);

        System.out.println(t2.a.getClass());
    }


}
