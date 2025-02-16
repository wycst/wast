package com.wast.test.doubleToString;

import io.github.wycst.wast.json.JSON;

// 全float数解析速度比较
public class DoubleToStringSpeedTest {

    final static int min = Integer.MIN_VALUE;
    final static int max = Integer.MAX_VALUE;

    public static void main(String[] args) {
        testJSONToString();  // 106804ms
        testJdkToString();   // 763429ms
    }

    private static void testJSONToString() {
        int i = min;
        String result  = null;
        long start = System.currentTimeMillis();
        for (; i < max ; ++i) {
            double d0 = Float.intBitsToFloat(i);
            if(Double.isNaN(d0) || Double.isInfinite(d0)) {
                continue;
            }
            result = JSON.toString(d0);
        }
        long end = System.currentTimeMillis();
        System.out.println("JSONtoString " + (end - start));
    }

    private static void testJdkToString() {
        int i = min;
        String jdkString;
        long start = System.currentTimeMillis();
        for (; i < max ; ++i) {
            double d0 = Float.intBitsToFloat(i);
            if(Double.isNaN(d0) || Double.isInfinite(d0)) {
                continue;
            }
            jdkString = Double.toString(d0);
        }
        long end = System.currentTimeMillis();
        System.out.println("jdktoString " + (end - start));
    }

}
