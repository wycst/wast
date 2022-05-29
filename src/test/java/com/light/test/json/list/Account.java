package com.light.test.json.list;

import java.util.List;
import java.util.Map;

/**
 * @Author wangyunchao
 * @Date 2021/7/30 15:58
 */
public class Account {

    private String loginUrl;
    private List<Map<String,Object>> options;
    private Map<String,Object> submit;

    public String getLoginUrl() {
        return loginUrl;
    }

    public void setLoginUrl(String loginUrl) {
        this.loginUrl = loginUrl;
    }

    public List<Map<String, Object>> getOptions() {
        return options;
    }

    public void setOptions(List<Map<String, Object>> options) {
        this.options = options;
    }

    public Map<String, Object> getSubmit() {
        return submit;
    }

    public void setSubmit(Map<String, Object> submit) {
        this.submit = submit;
    }
}
