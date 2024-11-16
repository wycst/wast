package io.github.wycst.wast.common.expression;

/**
 * <p> 按java语法的操作符号优先级仅供参考，值越小优先级越高
 * <p> 支持科学计数法e+n，16进制0x，8进制0n
 * <p> 支持∈和∉
 *
 * <p>
 * 符号   优先级
 * ()      1
 * **      9 (指数运算，平方（根），立方（根）)
 * /*%     10
 * +-      100
 * << >>   200
 * &|^     300+ &(300) < ^(301) < |(302)
 * ><==    500
 * && ||   600
 * ? :     700( ? 701, : 700)  三目运算符优先级放最低,其中:优先级高于?
 * <p>
 *
 * @Date 2024/8/24 22:35
 * @Created by wangyc
 */
public enum ElOperator {

    ATOM("", 0, 0),
    BRACKET("()", 1, 2),
    NOT("!", 5, 43),
    EXP("**", 9, 4),
    MULTI("*", 10, 1),
    DIVISION("/", 10, 2),
    MOD("%", 10, 3),
    PLUS("+", 100, 11),
    MINUS("-", 100, 12),
    BIT_RIGHT(">>", 200, 21),
    BIT_LEFT("<<", 200, 22),
    AND("&", 300, 31),
    XOR("^", 301, 32),
    OR("|", 302, 33),
    GT(">", 500, 51),
    LT("<", 500, 52),
    EQ("==", 500, 53),
    GE(">=", 500, 54),
    LE("<=", 500, 55),
    NE("!=", 500, 56),
    LOGICAL_AND("&&", 600, 61),
    LOGICAL_OR("||", 600, 62),
    IN("∈", 600, 63),
    OUT("∉", 600, 64),
    COLON(":", 700, 70),
    QUESTION("?", 701, 71),

    // todo 拓展类型
    EXPAND("\uffff", 800, 80);

    static final ElOperator[] INDEXS_OPERATORS = new ElOperator[128];

    static {
        ElOperator[] values = ElOperator.values();
        for (ElOperator value : values) {
            String symbol = value.symbol;
            if (symbol.length() == 1) {
                char c = symbol.charAt(0);
                if (c < INDEXS_OPERATORS.length) {
                    INDEXS_OPERATORS[c] = value;
                }
            }
        }
        INDEXS_OPERATORS['='] = EQ;
    }

    final String symbol;
    final int level;
    final int type;

    ElOperator(String symbol, int level, int type) {
        this.symbol = symbol;
        this.level = level;
        this.type = type;
    }
}
