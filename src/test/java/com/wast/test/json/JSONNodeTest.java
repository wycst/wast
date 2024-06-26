package com.wast.test.json;

import io.github.wycst.wast.json.JSONNode;

/**
 * @Author: wangy
 * @Description:
 */
public class JSONNodeTest {

    public static void main(String[] args) {
        String databaseJSON = "{\"age\":25,\"name\":\"Miss Zhang\",\"students\":[{\"name\":\"Li Lei\",\"age\":12},{\"age\":16,\"name\":\"Mei Mei Han\"}]}";
//        JSON.parse(databaseJSON);
        JSONNode node = null;
        long begin = System.currentTimeMillis();
        Object result = null;

        String name = null, students = null;
        int age = 0;
        int len = -1;
        for (int i = 0; i < 1000000; i++) {
//            JSON2.parse(databaseJSON);
            node = io.github.wycst.wast.json.JSONNode.parse(databaseJSON);
            age = node.getChildValue("age", int.class);
            name = node.getChildValue("name", String.class);
            students = node.getChildValue("students", String.class);
            node.getPathValue("/name", String.class);

            // from性能更高
            node = io.github.wycst.wast.json.JSONNode.from(databaseJSON, "/students");
            len = node.getElementCount();

        }
        long end = System.currentTimeMillis();
        System.out.println("-- " + (end - begin));
        System.out.println(age);
        System.out.println(name);
        System.out.println(students);
        System.out.println(len);
    }

}
