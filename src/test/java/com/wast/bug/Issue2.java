package com.wast.bug;

import io.github.wycst.wast.json.JSON;

import java.util.List;
import java.util.Random;

/**
 * @Date 2024/11/28 18:38
 * @Created by wangyc
 */
public class Issue2 {

    public static void main(String[] args) {
        double d = 9.49113649602955E-309;
        String s = Double.toString(d);

        {
            double d2 = JSON.parseAs(s);
            System.out.println(d2 == d);
            boolean flag1 = JSON.parseAs("true");
            System.out.println(flag1 == true);
            boolean flag2 = JSON.parseAs("false");
            System.out.println(flag2 == false);
            System.out.println(JSON.parseAs("null") == null);
        }

        {
            System.out.println(s); // 输出: 9.49113649602955E-309
            double d1 = ((List<Double>)io.github.wycst.wast.json.JSON.parse("[" + s + "]")).get(0);
            System.out.println(d1); // 输出: 9.491136496029548E-309
            double d2 = JSON.parseAs(s);
            System.out.println(d2); // 输出: 9.49113649602955E-309
            System.out.println(d1 == d2); // 输出: false

        }

        {
            Random r = new Random();
            for (int i = 0; i < 10000000; i++) {
                long v;
                do
                    v = r.nextLong();
                while ((v & 0x7ff0000000000000L) == 0x7ff0000000000000L); // 排除Infinity,NaN
                d = Double.longBitsToDouble(v);
                try {
                    double d1 = JSON.parseAs(String.valueOf(d));
                    if (d != d1)
                        throw new AssertionError("testRandomWastParser[" + i + "]: " + d + " != " + d1);
                } catch (Throwable throwable) {
                    throw new AssertionError("testRandomWastParser[" + i + "]: " + d);
                }
            }
            System.out.println("testRandomWastParser OK!");
        }
    }

}
