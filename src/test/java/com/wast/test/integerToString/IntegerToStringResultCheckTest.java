package com.wast.test.integerToString;

import io.github.wycst.wast.json.JSON;

public class IntegerToStringResultCheckTest {


    public static void main(String[] args) {
        int i = Integer.MIN_VALUE;
        for (; i < Integer.MAX_VALUE ; ++i) {
            String jdkString = Integer.toString(i);
            String toJson = JSON.toJsonString(i);
            if(!jdkString.equals(toJson)) {
                System.out.println(i + "======================1 diff " +  jdkString);
                System.out.println(i + "======================2 diff " +  toJson);
                throw new RuntimeException("error");
            }
            if(i % 100000000 == 0) {
                System.out.println("complete " + i);
            }
        }

    }

}
