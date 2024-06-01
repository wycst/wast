package com.wast.test.json;

import io.github.wycst.wast.json.JSON;
import io.github.wycst.wast.json.JSONNode;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;

/**
 * @Author: wangy
 * @Description:
 */
public class BigJsonTest {

    public static void main(String[] args) throws IOException {

        // download addr: https://codeload.github.com/zemirco/sf-city-lots-json/zip/refs/heads/master
        File file = new File("D:\\sf-city-lots-json-master\\citylots.json");
        BufferedReader reader = new BufferedReader(new FileReader(file));

        String line = null;
        StringBuilder buffer = new StringBuilder();
        while ((line = reader.readLine()) != null) {
            buffer.append(line);
        }
        String result = buffer.toString();
        System.out.println(result.length());
        Object obj = null;

        JSONNode jsonNode = null;
        long s = System.currentTimeMillis();
        for (int i = 0; i < 1; i++) {
            obj = JSON.parse(result);
//            obj = JSON.read(file, Map.class);
//            jsonNode = JSONNode.parse(result, "/features/[100000]/properties/STREET");
//            length = jsonNode.get("/features").getElementCount();
        }
        obj = null;
        long e = System.currentTimeMillis();
        System.out.println(e - s);
        reader.close();

    }

}
