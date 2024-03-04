package io.github.wycst.wast.common.reflect;

public class FieldInfo {
    private String name;
    private SetterInfo setterInfo;
    private GetterInfo getterInfo;
    private int index;

    public String getName() {
        return name;
    }

    void setName(String name) {
        this.name = name;
    }

    public SetterInfo getSetterInfo() {
        return setterInfo;
    }

    void setSetterInfo(SetterInfo setterInfo) {
        this.setterInfo = setterInfo;
    }

    public GetterInfo getGetterInfo() {
        return getterInfo;
    }

    void setGetterInfo(GetterInfo getterInfo) {
        this.getterInfo = getterInfo;
    }

    public int getIndex() {
        return index;
    }

    void setIndex(int index) {
        this.index = index;
    }
}
