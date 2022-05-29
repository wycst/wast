package com.light.test.json.custom;

import org.framework.light.json.custom.JsonSerializer;
import org.framework.light.json.options.JsonConfig;

import java.io.Writer;
import java.util.Map;

/**
 * @Author: wangy
 * @Date: 2022/1/2 9:07
 * @Description:
 */
public class CustomDetailSerializer extends JsonSerializer<Map> {

    @Override
    public void serialize(Writer writer, Map map, int indentLevel, JsonConfig jsonConfig) throws Exception {
        String str = "hello";
        writer.write('"');
        writer.write(str);
        writer.write('"');
    }
}
