package com.wast.wiki.json;
import io.github.wycst.wast.json.JSON;
import io.github.wycst.wast.json.options.WriteOption;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.util.List;

public class NdjsonExample {

    public static void main(String[] args) throws FileNotFoundException {
        String json = "{\"key\": 123}\n" +
                "{\"key\": 123}\n" +
                "{\"key\": 123}\n" +
                "{\"key\": 123}\n" +
                "{\"key\": 123}";
        List results = JSON.parseNdJson(json);
        results.add(123);
        results.add(456);
        System.out.println(JSON.toNdJsonString(results, WriteOption.FormatOut));
        System.out.println(results);
        JSON.writeNdJsonTo(results, new FileOutputStream("e:/tmp/ndjson.ndjson"), WriteOption.FormatOut);

        List list = JSON.parseNdJson(new FileInputStream("e:/tmp/ndjson.ndjson"));
        System.out.println(list);
    }

}
