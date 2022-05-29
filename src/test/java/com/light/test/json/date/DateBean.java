package com.light.test.json.date;

import org.framework.light.json.JSON;

import java.sql.Time;
import java.util.Date;

/**
 * @Author: wangy
 * @Date: 2021/12/11 17:57
 * @Description:
 */
public class DateBean {

    public DateBean() {
    }

    public DateBean(Date time) {
        this.time = time;
    }

    //    private int id;
//    private String name;
//    @Property( timezone = "+0:00")
    private Date time;

    private Time time2;
//    public int getId() {
//        return id;
//    }
//
//    public void setId(int id) {
//        this.id = id;
//    }
//
//    public String getName() {
//        return name;
//    }
//
//    public void setName(String name) {
//        this.name = name;
//    }

    public Date getTime() {
        return time;
    }

    public void setTime(Date time) {
        this.time = time;
    }

    public Time getTime2() {
        return time2;
    }

    public void setTime2(Time time2) {
        this.time2 = time2;
    }

    public static void main(String[] args) throws NoSuchMethodException {

        DateBean dateBean = new DateBean();
//        dateBean.setId(1111);
//        dateBean.setName("dsdsdassdsdsdsds");
        dateBean.setTime(new Date());
        dateBean.setTime2(new Time(System.currentTimeMillis()));
        String json1 = JSON.toJsonString(dateBean);
        System.out.println(json1);

        DateBean result = null;
        long begin = System.currentTimeMillis();

        String json = "{\"time\": \"2018-01-25T20:53:09.616+03:00\", \"time2\":\"20:53:09\"}";
        System.out.println(json);
        for (int i = 0; i < 1000000; i++) {
            result = JSON.parseObject(json, DateBean.class);
        }
        long end = System.currentTimeMillis();
        System.out.println(end - begin);
        System.out.println();
    }

}
