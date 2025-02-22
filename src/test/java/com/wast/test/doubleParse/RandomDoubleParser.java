package com.wast.test.doubleParse;

import io.github.wycst.wast.common.utils.NumberUtils;

import java.util.Random;

/**
 * @Date 2025/2/18 23:48
 * @Created by wangyc
 */
public class RandomDoubleParser {

    public static void main(String[] args) {

        Random random = new Random();
        double result = 0;
        long l1 = System.currentTimeMillis();

        for (int i = 0 ; i < 100; ++i) {
            long l = random.nextLong() & 0x7FFFFFFFFFL;
            long begin = System.currentTimeMillis();
            for (int k = 0 ; k < 1000000; ++k) {
                for (int j = -280; j < 280; ++j) {
                    result = NumberUtils.scientificToIEEEDouble(l, j);
                }
            }
            long end = System.currentTimeMillis();
            System.out.println("i = " + i + ": complete use " + (end - begin));
        }

        long l2 = System.currentTimeMillis();
        System.out.println("use " + (l2 - l1));
        System.out.println(result);


    }

}
