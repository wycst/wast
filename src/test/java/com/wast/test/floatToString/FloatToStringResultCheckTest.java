package com.wast.test.floatToString;

import io.github.wycst.wast.json.JSON;

public class FloatToStringResultCheckTest {

    public static void main(String[] args) {
        int i = Integer.MIN_VALUE;
        long cnt = 0;
        for (; i < Integer.MAX_VALUE ; ++i) {
            float d0 = Float.intBitsToFloat(i);
            if(d0 == Float.POSITIVE_INFINITY || d0 == Float.NEGATIVE_INFINITY || Float.isNaN(d0)) {
                continue;
            }
            String jdkString = Float.toString(d0);
            String toJson = JSON.toString(d0);
            if(Float.parseFloat(toJson) != Float.parseFloat(jdkString)) {
                System.out.println(i + "======================1 diff " +  jdkString);
                System.out.println(i + "======================2 diff " +  toJson);
                throw new RuntimeException("error");
            }
            if(++cnt % 100000000 == 0) {
                System.out.println("complete " + cnt);
            }
        }

    }

}
