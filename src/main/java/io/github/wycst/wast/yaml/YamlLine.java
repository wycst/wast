package io.github.wycst.wast.yaml;


import java.io.IOException;
import java.io.Writer;
import java.util.Arrays;

/**
 * 非空行结构信息
 *
 * @Author: wangy
 * @Date: 2022/4/4 23:21
 * @Description:
 */
class YamlLine extends YamlGeneral {

    /**
     * 行号
     */
    protected int lineNum;

    /**
     * 当前节点缩进字符位（空格数）,只支持空格' '(32)
     * <p>
     * 缩进信息，缩进紧跟正文（key）
     */
    protected int indent;

    /**
     * 行内容
     */
    protected char[] content;

    /**
     * key
     */
    protected String key;

    /**
     * value
     */
    protected String value;

    /**
     * 值类型
     * 0(不确定)
     * 1(str)
     * 2(float)
     * 3(int)
     * 4(bool)
     * 5(binary)
     * 6(timestamp)
     * 7(set)
     * 8(omap, pairs)
     * 9(seq)
     * 10(map)
     */
    protected int valueType;

    /***
     * 强类型转化后的值
     *
     */
    protected Object typeOfValue;

    /**
     * 级别
     */
    protected int level;

    /**
     * 是否叶子节点
     * <p>分隔符后面存在非#开头的内容时为叶子节点，否则为对象</p>
     */
    protected boolean leaf;

    /**
     * 是否文本块
     */
    protected boolean textBlock;

    /**
     * 文本块类型
     */
    public int blockType;

    /**
     * 是否数组token '-'
     */
    protected boolean arrayToken;

    /**
     * 锚点key
     */
    protected String anchorKey;

    /**
     * 引用key
     */
    public String referenceKey;

    /**
     * 数组索引
     */
    protected int arrayIndex = -1;

    protected void writeTypeToken(Writer writer) throws IOException {
        if (valueType > 0) {
            switch (valueType) {
                case 1:
//                    writer.write("!!str ");
                    break;
                case 2:
                    writer.write("!!float ");
                    break;
                case 3:
                    writer.write("!!int ");
                    break;
                case 4:
                    writer.write("!!bool ");
                    break;
                case 5:
                    writer.write("!!binary ");
                    break;
                case 6:
                    writer.write("!!timestamp ");
                    break;
                case 7:
                    writer.write("!!set ");
                    break;
                case 8:
                    writer.write("!!omap ");
                    break;
                case 9:
                    writer.write("!!seq ");
                    break;
                default:
                    writer.write("!!map ");
            }
        }
    }

    protected void writeBlockToken(Writer writer) throws IOException {
        writer.write("|");
        if (blockType == 1) {
            writer.write("+");
        } else if (blockType == 2) {
            writer.write("-");
        }
    }

    protected void writeArrayToken(Writer writer) throws IOException {
        if (arrayIndex > -1) {
//            writeIndent(writer, indent - 2);
            writer.write("- ");
        }
    }

    protected void writeNewLine(Writer writer) throws IOException {
        writer.write(IS_WINDOW_OS ? "\r\n" : "\n");
    }

    protected void writeIndent(Writer writer) throws IOException {
        writeIndent(writer, indent);
    }

    protected void writeIndent(Writer writer, int indent) throws IOException {
        char[] indents = new char[indent];
        Arrays.fill(indents, ' ');
        writer.write(indents);
    }
}
