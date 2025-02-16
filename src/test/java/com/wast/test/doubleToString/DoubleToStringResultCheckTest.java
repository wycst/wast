package com.wast.test.doubleToString;

import io.github.wycst.wast.json.JSON;

/**
 * 由于double值得范围太大（Long.MAX_VALUE）使用float转化得double进行测试
 */
public class DoubleToStringResultCheckTest {

    public static void main(String[] args) {
        int i = Integer.MIN_VALUE;
        long cnt = 0;
        for (; i < Integer.MAX_VALUE ; ++i) {
            double d0 = Float.intBitsToFloat(i);
            if(d0 == Double.POSITIVE_INFINITY || d0 == Double.NEGATIVE_INFINITY || Double.isNaN(d0)) {
                continue;
            }
            String jdkString = Double.toString(d0);
            String toJson = JSON.toString(d0);
            if(Double.parseDouble(toJson) != Double.parseDouble(jdkString)) {
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
