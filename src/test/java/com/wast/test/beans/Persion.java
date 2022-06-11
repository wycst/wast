package com.wast.test.beans;

import java.util.List;
import java.util.Map;

public final class Persion {
    private String name;
    private Integer age;

    private Map<String, String> customs;

    private List<Student> students;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Integer getAge() {
        return age;
    }

    public void setAge(Integer age) {
        this.age = age;
    }

    public List<Student> getStudents() {
        return students;
    }

    public void setStudents(List<Student> students) {
        this.students = students;
    }

    public Map<String, String> getCustoms() {
        return customs;
    }

    public void setCustoms(Map<String, String> customs) {
        this.customs = customs;
    }

    public static class Student {

        private String name;
        private int age;

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public int getAge() {
            return age;
        }

        public void setAge(int age) {
            this.age = age;
        }
    }

}
