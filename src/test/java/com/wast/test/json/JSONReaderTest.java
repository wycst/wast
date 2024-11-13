package com.wast.test.json;

import io.github.wycst.wast.json.JSON;
import io.github.wycst.wast.json.JSONReader;
import io.github.wycst.wast.json.JSONReaderCallback;

import java.io.File;
import java.util.List;

/**
 * @Author: wangy
 * @Description:
 */
public class JSONReaderTest {

    public static void main(String[] args) {
        long begin = System.currentTimeMillis();
        // download addr: https://codeload.github.com/zemirco/sf-city-lots-json/zip/refs/heads/master
        String f = "e:/tmp/sf-city-lots-json-master/citylots.json";
        final JSONReader reader = JSONReader.from(new File(f));
        reader.read(new JSONReaderCallback() {
            @Override
            public void parseValue(String key, Object value, Object host, int elementIndex, String path) throws Exception {
                super.parseValue(key, value, host, elementIndex, path);
                if(path.equals("/features/1/properties/STREET")) {
                    System.out.println(value);
                    abort();
                }
            }
        }, true);

//        HashMap hashMap = reader.readAsResult(GenericParameterizedType.entityType(HashMap.class, Object.class));

        System.out.println(1);
        reader.getResult();
        long end = System.currentTimeMillis();
        System.out.println(end - begin);

        String json = "[\"test\", \"hello\", \"json\"]";
        List<String> msgs = (List<String>) JSON.parse(json);
        msgs = JSON.parseArray(json, String.class);

    }
}
