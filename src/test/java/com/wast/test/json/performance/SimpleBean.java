package com.wast.test.json.performance;

import java.sql.Time;
import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * @Author: wangy
 * @Date: 2022/1/15 23:11
 * @Description:
 */
public class SimpleBean {

    private int id;
    private long version;
    private double percent;
    private String name;
    private Date date;
    private Time time;

    private SimpleEnum simpleEnum;
    private Map<String, Object> mapInstance;
    private List<Object> list;
    private List<char[]> charsList;


    enum SimpleEnum {
        EnumOne,
        EnumTwo,
        EnumThree
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public long getVersion() {
        return version;
    }

    public void setVersion(long version) {
        this.version = version;
    }

    public double getPercent() {
        return percent;
    }

    public void setPercent(double percent) {
        this.percent = percent;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Date getDate() {
        return date;
    }

    public void setDate(Date date) {
        this.date = date;
    }

    public Time getTime() {
        return time;
    }

    public void setTime(Time time) {
        this.time = time;
    }

    public SimpleEnum getSimpleEnum() {
        return simpleEnum;
    }

    public void setSimpleEnum(SimpleEnum simpleEnum) {
        this.simpleEnum = simpleEnum;
    }

    public Map<String, Object> getMapInstance() {
        return mapInstance;
    }

    public void setMapInstance(Map<String, Object> mapInstance) {
        this.mapInstance = mapInstance;
    }

    public List<Object> getList() {
        return list;
    }

    public void setList(List<Object> list) {
        this.list = list;
    }

    public List<char[]> getCharsList() {
        return charsList;
    }

    public void setCharsList(List<char[]> charsList) {
        this.charsList = charsList;
    }
}
