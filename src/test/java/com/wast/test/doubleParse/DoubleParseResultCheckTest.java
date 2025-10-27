package com.wast.test.doubleParse;

import io.github.wycst.wast.common.expression.Expression;
import io.github.wycst.wast.json.JSON;

/**
 * 由于double值得范围太大（Long.MAX_VALUE）使用float转化得double进行测试
 */
public class DoubleParseResultCheckTest {

    public static void main(String[] args) {
        int i = 0; // Integer.MIN_VALUE;
        long cnt = 0;
        long start = System.currentTimeMillis();
        for (; i < Integer.MAX_VALUE ; ++i) {
            double d0 = Float.intBitsToFloat(i);
            if(d0 == Double.POSITIVE_INFINITY || d0 == Double.NEGATIVE_INFINITY || Double.isNaN(d0)) {
                continue;
            }
            String jdkString = JSON.toString(d0);
            double d1 = JSON.parseDouble(jdkString);
            if(d0 != d1) {
                System.out.println(i + "======================1 diff " +  jdkString);
                System.out.println(i + "======================2 diff " +  d1);
                throw new RuntimeException("error");
            }
            if(++cnt % 100000000 == 0) {
                System.out.println("complete " + cnt + ", use " + (System.currentTimeMillis() - start) + " ms");
            }
        }
        long end = System.currentTimeMillis();
        System.out.println("total time: " + (end - start));
    }

}
