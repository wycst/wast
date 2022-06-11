package com.wast.test.json.list;

import java.util.List;
import java.util.Map;

/**
 * @Author wangyunchao
 * @Date 2021/7/30 15:58
 */
public class AuthInfo {

    // 账号登录结构
    private Account account;
    // token结构
    private List<Map> token;
    // 校验结果
    private List<Map> result;

    public Account getAccount() {
        return account;
    }

    public void setAccount(Account account) {
        this.account = account;
    }

    public List<Map> getToken() {
        return token;
    }

    public void setToken(List<Map> token) {
        this.token = token;
    }

    public List<Map> getResult() {
        return result;
    }

    public void setResult(List<Map> result) {
        this.result = result;
    }
}
