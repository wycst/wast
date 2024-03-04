package io.github.wycst.wast.jdbc.executer;

/**
 * @Author: wangy
 * @Date: 2021/2/17 16:14
 * @Description:
 */
public class FieldOrder {

    private String field;
    private Order order = Order.ASC;

    public FieldOrder(String field, Order order) {
        this.field = field;
        this.order = order;
    }

    public String getField() {
        return field;
    }

    public void setField(String field) {
        this.field = field;
    }

    public Order getOrder() {
        return order;
    }

    public void setOrder(Order order) {
        this.order = order;
    }

    public enum Order {
        ASC,DESC
    }
}
