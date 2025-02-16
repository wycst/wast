package com.wast.test.integerToString;

import io.github.wycst.wast.json.JSON;

// 全int数解析速度比较
public class IntegerToStringSpeedTest {

    final static int min = 1;
    final static int max = Integer.MAX_VALUE;

    public static void main(String[] args) {
        System.out.println(JSON.toString(-1234551232323232L).equals(Long.toString(-1234551232323232L)));
        testJSONToString();  // 4862ms
        testJdkToString();   // 27088ms
    }

    private static void testJSONToString() {
        int i = min;
        long start = System.currentTimeMillis();
        for (; i < max ; ++i) {
            String jdkString = JSON.toString(i);
        }
        long end = System.currentTimeMillis();
        System.out.println("JSONtoString " + (end - start));
    }

    private static void testJdkToString() {
        int i = min;
        long start = System.currentTimeMillis();
        for (; i < max ; ++i) {
            String jdkString = Integer.toString(i);
        }
        long end = System.currentTimeMillis();
        System.out.println("jdktoString " + (end - start));
    }

}
