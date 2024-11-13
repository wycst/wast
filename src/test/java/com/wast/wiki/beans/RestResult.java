package com.wast.wiki.beans;

import io.github.wycst.wast.json.JSON;

/**
 * @Date 2024/11/14 1:15
 * @Created by wangyc
 */
public class RestResult<T> {

    private String msg;
    private String status;
    private T data;

    public String getMsg() {
        return msg;
    }

    public void setMsg(String msg) {
        this.msg = msg;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public T getData() {
        return data;
    }

    public void setData(T data) {
        this.data = data;
    }

    public static void main(String[] args) {
        RestResult<UserFact> restResult = new RestResult<UserFact>();
        restResult.setMsg("success");
        restResult.setStatus("200");

        UserFact userFact = new UserFact();
        userFact.setUserId(1);
        userFact.setUserName("test");
        userFact.setAddress("asdfgh");
        userFact.setPassword("1235");

        restResult.setData(userFact);

        System.out.println(JSON.toJsonString(restResult));
    }
}
