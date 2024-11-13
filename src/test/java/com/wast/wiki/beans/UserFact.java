package com.wast.wiki.beans;

import io.github.wycst.wast.json.annotations.JsonProperty;

/**
 * @Date 2024/11/13 23:20
 * @Created by wangyc
 */
public class UserFact {

    private int userId;
    private String userName;
    @JsonProperty(serialize = false)
    private String password;
    @JsonProperty(name = "addr")
    private String address;

    public int getUserId() {
        return userId;
    }

    public void setUserId(int userId) {
        this.userId = userId;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }
}
