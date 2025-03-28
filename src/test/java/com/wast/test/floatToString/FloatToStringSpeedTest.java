package com.wast.test.floatToString;

import io.github.wycst.wast.json.JSON;

// 全float数解析速度比较
public class FloatToStringSpeedTest {

    final static int min = 1;
    final static int max = Integer.MAX_VALUE;

    public static void main(String[] args) {
        testJSONToString();  // 48610ms
        testJdkToString();   // 182120ms
    }

    private static void testJSONToString() {
        int i = min;
        String result  = null;
        long start = System.currentTimeMillis();
        for (; i < max ; ++i) {
            float d0 = Float.intBitsToFloat(i);
            if(Float.isNaN(d0) || Float.isInfinite(d0)) {
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
            float d0 = Float.intBitsToFloat(i);
            if(Float.isNaN(d0) || Float.isInfinite(d0)) {
                continue;
            }
            jdkString = Float.toString(d0);
        }
        long end = System.currentTimeMillis();
        System.out.println("jdktoString " + (end - start));
    }

}
