package io.github.wycst.wast.jdbc.executer;

import java.io.Serializable;
import java.util.List;

/**
 * @Author: wangy
 * @Date: 2021/2/16 11:44
 * @Description:
 */
public class FieldCondition {

    private final String field;
    private final Operator operator;
    private final Serializable value;
    private final boolean like;
    private String logicType = "and";

    public FieldCondition(String field, Operator ops, Serializable value) {
        this.field = field;
        this.operator = ops == null ? Operator.EQ : ops;
        this.value = value;
        this.like = operator.type == 1;
    }

    public FieldCondition(String field, Operator ops) {
        this(field, ops, null);
    }

    public String getField() {
        return field;
    }

    public String getOperator() {
        return operator.getSymbol();
    }

    public String likeValueLeft() {
        return operator.likeValueLeft();
    }

    public String likeValueRight() {
        return operator.likeValueRight();
    }

    public String getLogicType() {
        return logicType;
    }

    public void setLogicType(String logicType) {
        this.logicType = logicType;
    }

    public Serializable getValue() {
        return value;
    }

    public boolean isLike() {
        return like;
    }

    public void appendWhereValue(StringBuilder whereBuilder, List<Object> paramValues, Object conditionValue) {
        whereBuilder.append("?");
        paramValues.add(conditionValue);
    }

    public void appendWhereField(StringBuilder whereBuilder, FieldColumn fieldColumn) {
        whereBuilder.append("t.").append(fieldColumn.getColumnName())
                .append(" ").append(getOperator()).append(" ");
    }

    public enum Operator {

        GT(">", 0),
        LT("<", 0),
        EQ("=", 0),
        GE(">=", 0),
        LE("<=", 0),
        NE("<>", 0),
        NE2("!=", 0),
        LIKE("LIKE", 1),
        LEFT_LIKE("LIKE", 1) {
            @Override
            public String likeValueRight() {
                return "'";
            }
        },
        RIGHT_LIKE("LIKE", 1) {
            @Override
            public String likeValueLeft() {
                return "'";
            }
        },
        // 自定义模糊内容查询,解决LeftLike和RightLike满足的不了的查询问题
        CUSTOM_LIKE("LIKE", 1) {
            @Override
            public String likeValueLeft() {
                return "'";
            }

            @Override
            public String likeValueRight() {
                return "'";
            }
        };

        Operator(String symbol, int type) {
            this.symbol = symbol;
            this.type = type;
        }

        final String symbol;
        final int type;

        public String getSymbol() {
            return symbol;
        }

        public String likeValueLeft() {
            return "'%";
        }

        public String likeValueRight() {
            return "%'";
        }

        public int getType() {
            return type;
        }
    }
}
