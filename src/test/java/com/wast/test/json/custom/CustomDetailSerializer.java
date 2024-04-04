package com.wast.test.json.custom;

import io.github.wycst.wast.json.JSONConfig;
import io.github.wycst.wast.json.JSONWriter;
import io.github.wycst.wast.json.custom.JsonSerializer;

import java.util.Map;

/**
 * @Author: wangy
 * @Date: 2022/1/2 9:07
 * @Description:
 */
public class CustomDetailSerializer extends JsonSerializer<Map> {

    @Override
    public void serialize(JSONWriter writer, Map map, int indentLevel, JSONConfig jsonConfig) throws Exception {
        String str = "hello";
//        writer.write('"');
//        writer.write(str);
//        writer.write('"');
        writer.writeJSONString(str);
    }
}
