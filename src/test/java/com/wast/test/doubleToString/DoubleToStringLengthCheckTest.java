package com.wast.test.doubleToString;

import io.github.wycst.wast.json.JSON;
// 由于double值得范围太大（Long.MAX_VALUE）使用float转化得double进行测试
public class DoubleToStringLengthCheckTest {

    public static void main(String[] args) {
        long wastDiffCount = 0, jdkDiffCount = 0, total = 0;
        int i = Integer.MIN_VALUE;
        for (; i < Integer.MAX_VALUE ; ++i) {
            double d0 = Float.intBitsToFloat(i);
            if(d0 == Double.POSITIVE_INFINITY || d0 == Double.NEGATIVE_INFINITY || Double.isNaN(d0)) {
                continue;
            }
            ++total;
            String jdkString = Double.toString(d0);
            String jsonString = JSON.toString(d0);
            if(jsonString.length() > jdkString.length()) {
                wastDiffCount ++;
//                System.out.println("jdkToString " + jdkString + ", jsonString " + jsonString);
//                System.out.println("finish " + total + ", wastDiffCount " + wastDiffCount + ", total " + total + " diffRate = " + (1.0 * wastDiffCount / total));
            } else if(jsonString.length() < jdkString.length()) {
                jdkDiffCount ++;
//                System.out.println("jdkToString " + jdkString + ", jsonString " + jsonString);
//                System.out.println("finish " + total + ", jdkDiffCount " + jdkDiffCount + ", total " + total + " diffRate = " + (1.0 * jdkDiffCount / total));
            }
            if(total % 100000000 == 0) {
                System.out.println("complete " + total);
                System.out.println("finish " + total + ", wastDiffCount " + wastDiffCount + ", total " + total + " diffRate = " + (1.0 * wastDiffCount / total));
                System.out.println("finish " + total + ", jdkDiffCount " + jdkDiffCount + ", total " + total + " diffRate = " + (1.0 * jdkDiffCount / total));
            }
        }
        System.out.println("complete " + total);
        System.out.println("finish " + total + ", wastDiffCount " + wastDiffCount + ", total " + total + " diffRate = " + (1.0 * wastDiffCount / total));
        System.out.println("finish " + total + ", jdkDiffCount " + jdkDiffCount + ", total " + total + " diffRate = " + (1.0 * jdkDiffCount / total));
    }

}
