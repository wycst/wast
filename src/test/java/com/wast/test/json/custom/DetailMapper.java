package com.wast.test.json.custom;

import io.github.wycst.wast.json.JSONConfig;
import io.github.wycst.wast.json.JSONTypeFieldMapper;
import io.github.wycst.wast.json.JSONValue;

import java.util.HashMap;
import java.util.Map;

public class DetailMapper extends JSONTypeFieldMapper {
    @Override
    public Object readOf(Object value) throws Exception {
        Map map = new HashMap();
        map.put("key1", value);
        return map;
    }

    @Override
    public JSONValue<?> writeAs(Object value, JSONConfig jsonConfig) throws Exception {
        return JSONValue.of("hello");
    }
}
