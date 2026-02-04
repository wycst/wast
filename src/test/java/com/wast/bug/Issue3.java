package com.wast.bug;

import io.github.wycst.wast.common.utils.EnvUtils;
import io.github.wycst.wast.json.JSON;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;

public class Issue3 {

    public static void main(String[] args) throws IOException {
        String jsonStr = "{\n" +
                "  \"name\": \"张三\",\n" +
                "  \"age\": 18,\n" +
                "  \"birthday\": \"2020-01-01\",\n" +
                "  \"booleanValue\": true,\n" +
                "  \"jsonObjectSub\": {\n" +
                "    \"subStr\": \"abc\",\n" +
                "    \"subNumber\": 150343445454,\n" +
                "    \"subBoolean\": true\n" +
                "  },\n" +
                "  \"jsonArraySub\": [\n" +
                "    \"abc\",\n" +
                "    123,\n" +
                "    false\n" +
                "  ]\n" +
                "}";
        System.out.println(JSON.read(new ByteArrayInputStream(jsonStr.getBytes(EnvUtils.CHARSET_UTF_8)), Map.class));


    }

}
