package com.light.test.json.custom;

import org.framework.light.common.reflect.ClassStructureWrapper;
import org.framework.light.common.reflect.SetterInfo;
import org.framework.light.json.JSON;
import org.framework.light.json.annotations.JsonDeserialize;
import org.framework.light.json.annotations.JsonSerialize;

import java.util.HashMap;
import java.util.Map;

/**
 * @Author: wangy
 * @Date: 2022/1/2 9:05
 * @Description:
 */
public class CustomBean {

    private String name;

    @JsonSerialize(CustomDetailSerializer.class)
    @JsonDeserialize(CustomDetailDeserializer.class)
    private Map detail;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Map getDetail() {
        return detail;
    }

    public void setDetail(Map detail) {
        this.detail = detail;
    }

    public static void main(String[] args) {

        ClassStructureWrapper classStructureWrapper = ClassStructureWrapper.get(CustomBean.class);

        SetterInfo setterInfo = classStructureWrapper.getSetterInfo("detail");

        System.out.println(classStructureWrapper);

        CustomBean customBean = new CustomBean();
        customBean.setName("custom example");

        Map map = customBean.detail = new HashMap();
        map.put("key1", "hello kitty cat");

        String json = JSON.toJsonString(customBean);
        System.out.println(json);

        CustomBean newBean = JSON.parseObject(json, CustomBean.class);
        System.out.println(newBean);

    }

}
