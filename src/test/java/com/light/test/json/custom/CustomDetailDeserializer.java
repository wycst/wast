package com.light.test.json.custom;

import org.framework.light.json.custom.JsonDeserializer;
import org.framework.light.json.options.JSONParseContext;

import java.util.HashMap;
import java.util.Map;

/**
 * @Author: wangy
 * @Date: 2022/1/2 9:09
 * @Description:
 */
public class CustomDetailDeserializer extends JsonDeserializer<Map> {

    @Override
    public Map deserialize(Object value, String source, JSONParseContext parseContext) throws Exception {
        System.out.println(value);

        Map map = new HashMap();
        map.put("key1", value);

        return map;
    }

}
