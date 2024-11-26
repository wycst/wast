package io.github.wycst.wast.common.expression.compile;

/**
 * @Author: wangy
 * @Date: 2022/11/13 21:19
 * @Description:
 */
public class ValueHolder {
    public Object value;

    public void setValue() {
        this.value = "";
    }

    public void setValue(int value) {
        this.value = value;
    }

    public void setValue(byte value) {
        this.value = value;
    }

    public void setValue(double value) {
        this.value = value;
    }

    public void setValue(float value) {
        this.value = value;
    }

    public void setValue(long value) {
        this.value = value;
    }

    public void setValue(char value) {
        this.value = value;
    }

    public void setValue(short value) {
        this.value = value;
    }

    public void setValue(boolean value) {
        this.value = value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public void setValue(Object value) {
        this.value = value;
    }
}
