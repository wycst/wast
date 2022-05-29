package com.light.test.json;

import org.framework.light.json.JSON;

/**
 * @Author: wangy
 * @Date: 2022/1/3 1:54
 * @Description:
 */
public class GenType {

    private GenTypeA<Integer> genTypeA;

    public GenTypeA<Integer> getGenTypeA() {
        return genTypeA;
    }

    public void setGenTypeA(GenTypeA<Integer> genTypeA) {
        this.genTypeA = genTypeA;
    }

    static class GenTypeA<T> {
        T start;
        T end;

        public T getStart() {
            return start;
        }

        public void setStart(T start) {
            this.start = start;
        }

        public T getEnd() {
            return end;
        }

        public void setEnd(T end) {
            this.end = end;
        }
    }

    public static void main(String[] args) {
        GenType genType = new GenType();
        GenTypeA<Integer> genTypeA = new GenTypeA<Integer>();
        genTypeA.setStart(12);
        genTypeA.setEnd(34);
        genType.setGenTypeA(genTypeA);

        String json = JSON.toJsonString(genType);
        System.out.println(json);
//        json = "{\"genTypeA\":{\"end\":34,\"start\":12}}";


        GenType genType1 = JSON.parseObject(json, GenType.class);
        System.out.println(genType1);

    }
}
