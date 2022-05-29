package com.light.test.beans;


import java.util.Map;

public class User {

    private long id;

    private String name;

    private City city;

    private Map customMap;

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public City getCity() {
        return city;
    }

    public void setCity(City city) {
        this.city = city;
    }

    public Map getCustomMap() {
        return customMap;
    }

    public void setCustomMap(Map customMap) {
        this.customMap = customMap;
    }


}
