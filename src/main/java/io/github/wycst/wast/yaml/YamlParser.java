package io.github.wycst.wast.yaml;

import java.util.List;

/**
 * @Author: wangy
 * @Date: 2022/5/22 9:28
 * @Description:
 */
class YamlParser extends YamlGeneral {

    // yaml source
    protected char[] source;
    // current pos
    private int pos;
    // 行数
    private int lineNum;
    // 根缩进数
    private int rootIndent = -1;

    // document split support
    protected void parseYamlRoot(int i) {
    }

    /**
     * 解析yaml核心实现
     *
     * @param yamlNodes
     * @param offset
     * @param toIndex
     */
    protected void parseNodes(List<YamlNode> yamlNodes, int offset, int toIndex) {

        char ch, prevCh = '\0';
        for (int i = offset; i < toIndex; i++) {
            String key = null, value = null, anchorKey = null, referenceKey = null;
            boolean isEmpty = false, leaf = false, textBlock = false, arrayToken = false;
            int j = i, splitIndex = -1, valueType = 0;
            Object typeOfValue = null;
            // 1、解析缩进（已去除后置空白字符不用考虑数组越界问题,必然会出现非空白字符终止循环）
            while ((source[i]) == SPACE_CHAR) {
                i++;
            }

            // 当前line的缩进空白数
            int indent = i - j;
            ch = source[i];
            /** 是否注释开头 */
            if (ch == '#') {
                // 去除注释行
                i++;
                while (!isNewLineChar(source[i])) {
                    i++;
                }
                continue;
            } else if (isNewLineChar(ch)) {
                continue;
            }

            // create node
//            YamlNode yamlNode = new YamlNode();
            // begin from 0
            int lineIndex = this.lineNum;

            if(rootIndent == -1) {
                rootIndent = indent;
            } else {
                if(indent < rootIndent) {
                    throw new YamlParseException("indent value " + indent + " error, cannot less then the root indent " + rootIndent + ", at lineNum " + (lineNum + 1));
                }
            }

            // 数组（- -）/分片（---）
            if (ch == '-') {
                char append = source[i + 1];
                boolean isAppendSpaceChar = append == SPACE_CHAR;
                if (isAppendSpaceChar || isNewLineChar(append)) {
                    // 第一层数组token标记
                    arrayToken = true;
                    i++;

                    // 紧跟空格或者换行符则解析为数组token
                    // 允许同一行出现多个-，每一个-代表一层数组，中间用空格分隔，需要解析每个-的缩进
                    // 多个节点的lineNum相同
                    // 当查找的下一个字符不是-或者append不是空格或者回车
                    // 循环条件
                    j = i;
                    while (isAppendSpaceChar) {
                        while ((ch = source[++j]) == SPACE_CHAR) ;
                        if (ch == '-') {
                            append = source[j + 1];
                            isAppendSpaceChar = append == SPACE_CHAR;
                            if (isAppendSpaceChar || isNewLineChar(append)) {
                                // 上一层数组token添加到list中，并更新indent
                                addYamlNode(yamlNodes, indent, lineIndex, null, null, valueType, null, false, false, true, anchorKey, referenceKey);
                                // 更新下一个indent
                                indent += j - i + 1;
                                j++;
                                i = j;
                            } else {
                                break;
                            }
                        } else {
                            break;
                        }
                    }

                    // 查找是否存在键值分隔符（: ）
                    // 如果存在分隔符，解析并将i定位到指定位置
                    // todo 优化点： 如果不存在分隔符，会将指针回退到开始位置（- 后面）重新解析集合的value(此处会存在性能损失)
                    int n = j - i;
                    j = i;
                    prevCh = '\0';
                    while (!isNewLineChar(ch = source[j])) {
                        if (ch != SPACE_CHAR || prevCh != SPLIT_CHAR) {
                            prevCh = ch;
                            j++;
                            continue;
                        }
                        splitIndex = j;
                        break;
                    }

                    if (splitIndex > -1) {
                        // key值
                        key = new String(source, i, j - i - 1).trim();
                        if (key.length() == 0) {
                            throw new UnsupportedOperationException("empty key before ': ' at line " + ++lineNum);
                        }
                        // 添加Node
                        addYamlNode(yamlNodes, indent, lineIndex, null, null, valueType, null, false, false, true, anchorKey, referenceKey);
                        // 更新indent
                        indent += n + 1;
                        // 下一个键值对节点解析
                        arrayToken = false;
                        // 并将i定位到指定位置j
                        i = j;
                    } else {
                        // 遇到\n回退lineNum值
                        if (ch == '\n') {
                            lineNum--;
                        }
                    }
                } else {
                    // 判断是否结束标志： ---
                    if (append == '-' && source[i + 2] == '-') {
                        int k = i + 2;
                        while ((ch = source[++k]) == SPACE_CHAR) ;
                        if (isNewLineChar(ch)) {
                            // --- yaml文档结束
                            // 解析下一个文档
                            parseYamlRoot(k + 1);
                            break;
                        }
                    }
                }
            }

            j = i;
            // 2、解析分隔符(: )等信息
            // 循环终止标记
            boolean breakOutLoop = false;
            while (!isNewLineChar(ch = source[i])) {

                if (!arrayToken && splitIndex == -1) {
                    // 键值对解析
                    if (ch != SPACE_CHAR || prevCh != SPLIT_CHAR) {
                        prevCh = ch;
                        i++;
                        continue;
                    }

                    // 空格位置作为分隔符
                    splitIndex = i;
                    // 计算key值
                    key = new String(source, j, i - j - 1).trim();
                    if (key.length() == 0) {
                        throw new UnsupportedOperationException("empty key before ': ' at line " + ++lineNum);
                    }
                }

                // 查找value(去除value前空白)
                while ((ch = source[++i]) == SPACE_CHAR) ;
                j = i;

                if (ch == '#') {
                    // 注释内容
                    while (!isNewLineChar(source[i])) {
                        i++;
                    }
                    // 分隔符后面如果紧跟注释token则行值为空
                    value = null;
                    break;
                } else if (ch == '\'' || ch == '"') {
                    // 判断是否以'或者"开始,匹配结束的'或者"，匹配成功后紧跟的字符： 空格，#， 回车符
                    // 否则匹配发现是否存在空格+#,如果发现代表后面的字符都为注释
                    leaf = true;
                    valueType = 1;
                    char strChar = ch;
                    i++;
                    j = i;
                    prevCh = '\'';
                    boolean matchEndFlag = false;
                    while (!isNewLineChar(ch = source[i])) {
                        // yaml不考虑转移字符\\
                        if (ch == strChar/* && prevCh != '\\'*/) {
                            matchEndFlag = true;
                            // 匹配到结束字符
                            value = new String(source, j, i - j);
                            // 去除value后的空白
                            while ((ch = source[++i]) == SPACE_CHAR) ;
                            if (ch == '#') {
                                i++;
                                while (!isNewLineChar(source[i])) {
                                    i++;
                                }
                                break;
                            } else if (isNewLineChar(ch)) {
                                // do nothing
                                break;
                            } else {
                                throw new RuntimeException("未期望的字符 '" + ch + "', lineNum " + lineNum);
                            }
                        }
                        i++;
                    }

                    if (!matchEndFlag) {
                        throw new RuntimeException("未找到结束字符 " + strChar + " , index " + i);
                    } else {
                        break;
                    }

                } else if (ch == '|' || ch == '>') {
                    leaf = true;
                    valueType = 1;
                    // > 用空格替换换行符
                    // 注意和snakeyaml库包解析有出入
                    boolean replaceAsSpace = ch == '>';

                    // | |+ |-
                    // 匹配段落，匹配的结束标志为换行符+当前缩进数量的空格+紧跟非空格字符
                    textBlock = true;
                    // 匹配到下一个line的indent = ${textBlockIndent} 缩进
                    // 查找第一行非空行作为baseTextIndent，每行的字符串内容为需要减去baseTextIndent个空格
                    int blockType = 0;
                    // 判断下一个字符
                    ch = source[++i];
                    if (ch == '+') {
                        blockType = 1;
                        i++;
                    } else if (ch == '-') {
                        blockType = 2;
                        i++;
                    }

                    char appendChar = replaceAsSpace ? SPACE_CHAR : '\n';

                    // 去除空白
                    while ((ch = source[i]) == SPACE_CHAR) {
                        i++;
                    }

                    // 第一行的注释非内容区和换行
//                    if (ch == '#') {
//                        // 注释处理
//                        i++;
//                        while (source[i++] != '\n') ;
//                        lineNum++;
//                    } else if (ch == '\n') {
//                        // 如果是换行符
//                        i++;
//                        lineNum++;
//                    } else {
//                        throw new RuntimeException("expected chomping or indentation indicators, but found " + ch + " in 'string', line " + lineNum + ", column n:");
//                    }
                    switch (ch) {
                        case '#': {
                            // 注释处理
                            i++;
                            while (source[i++] != '\n') ;
                            lineNum++;
                            break;
                        }
                        case '\r': {
                            i++;
                        }
                        case '\n': {
                            i++;
                            lineNum++;
                            break;
                        }
                        default:
                            throw new RuntimeException("expected chomping or indentation indicators, but found " + ch + " in 'string', line " + (lineNum + 1) + ", column n:");
                    }

                    // 添加到line
                    YamlLine blockYamlNode = addYamlNode(yamlNodes, indent, lineIndex, key, value, valueType, typeOfValue, leaf, textBlock, arrayToken, anchorKey, referenceKey);

                    // 解析紧跟的文本内容直到文本块结束
                    StringBuilder blockValue = new StringBuilder();

                    // 最小文本缩进
                    int minTextIndent = indent;
                    j = i;

                    // 去除文本块标记开始的空行，以便获取第一个非空行缩进信息
                    do {
                        // 防越界处理
                        if (i >= toIndex) {
                            blockYamlNode.value = getBlockValue(blockValue, blockType, appendChar);
                            blockYamlNode.blockType = blockType;
                            breakOutLoop = true;
                            break;
                        }
                        if ((ch = source[i]) != SPACE_CHAR && !isNewLineChar(ch)) {
                            break;
                        }
                        i++;
                        if (ch != SPACE_CHAR) {
                            // 如果是换行符（空行）
                            blockValue.append(appendChar);
                            j = i;
                        }
                    } while (true);

                    // 当前line的缩进空白数
                    int textIndent = i - j;
                    // 如果缩进数不大于minTextIndent则代表文本块结束
                    if (textIndent <= minTextIndent) {
                        blockYamlNode.value = getBlockValue(blockValue, blockType, appendChar);
                        blockYamlNode.blockType = blockType;
                        // 回滚当前行
                        i = j;
                        parseNodes(yamlNodes, i, toIndex);
                        breakOutLoop = true;
                        break;
                    }
                    // 第一行的缩进为基础缩进
                    int baseTextIndent = textIndent;

                    // 防越界
                    while (i < toIndex) {
                        i++;
                        // 遇到换行终止
                        // \r\n需要剔除\r
                        while (!isNewLineChar(ch = source[i])) {
                            i++;
                        }

                        int off = j + baseTextIndent;

                        blockValue.append(source, off, i - off);
                        blockValue.append(appendChar);

                        // 遇到\r处理
                        if(ch == '\r') {
                            i++;
                            lineNum++;
                        }

                        /** 如果读取到内容结束 */
                        if (i == toIndex - 1) {
                            blockYamlNode.value = getBlockValue(blockValue, blockType, appendChar);
                            blockYamlNode.blockType = blockType;
                            breakOutLoop = true;
                            break;
                        }

                        j = i + 1;
                        // 去除空行或者空白字符，遇到非空字符终止，计算缩进信息
                        while ((ch = source[++i]) == SPACE_CHAR || isNewLineChar(ch)) {
                            if (ch != SPACE_CHAR) {
                                // 空行
                                blockValue.append(appendChar);
                                if(ch == '\r') {
                                    i++;
                                    lineNum++;
                                }
                                j = i + 1;
                            }
                            if (i == toIndex - 1) {
                                blockYamlNode.value = getBlockValue(blockValue, blockType, appendChar);
                                blockYamlNode.blockType = blockType;
                                breakOutLoop = true;
                                break;
                            }
                        }

                        // 新的一行缩进
                        textIndent = i - j;

                        // 文本块的每行字符串内容的缩进必须大于indent（minTextIndent），且大于或者等于baseTextIndent
                        // 否则为文本块结束标记
                        if (textIndent <= minTextIndent) {
                            // 文本块结束
                            blockYamlNode.value = getBlockValue(blockValue, blockType, appendChar);
                            blockYamlNode.blockType = blockType;
                            i = j;
                            parseNodes(yamlNodes, i, toIndex);
                            breakOutLoop = true;
                            break;
                        } else {
                            if (textIndent < baseTextIndent) {
                                throw new YamlParseException("indent error, lineNum " + lineNum);
                            }
                        }
                    }

                    if (breakOutLoop) {
                        break;
                    }

                } else if (ch == '&') {
                    // 锚点支持紧跟'合法'字符(暂定不能为空格和换行符)
                    if (anchorKey == null) {
                        char anchorAppend = source[i + 1];
                        if (anchorAppend == SPACE_CHAR || isNewLineChar(anchorAppend)) {
                            throw new YamlParseException("anchor token '&' cannot be followed by ' ' and '\\n', line " + lineNum);
                        }
                        i++;
                        j = i;
                        while ((ch = source[++i]) != SPACE_CHAR && !isNewLineChar(ch)) ;
                        // 锚点key
                        anchorKey = new String(source, j, i - j).trim();
                    }
                } else if (ch == '*') {
                    // 锚点引用
                    char referenceAppend = source[i + 1];
                    if (referenceAppend == SPACE_CHAR || isNewLineChar(referenceAppend)) {
                        throw new YamlParseException("reference token '*' cannot be followed by ' ' and '\\n', line " + lineNum);
                    }
                    i++;
                    j = i;
                    while ((ch = source[++i]) != SPACE_CHAR && !isNewLineChar(ch)) ;
                    // 引用key
                    referenceKey = new String(source, j, i - j).trim();

                    // 如果后面是空格校验是否是注释或者换行，否则抛出异常
                    if (ch == SPACE_CHAR) {
                        while ((ch = source[++i]) == SPACE_CHAR) ;
                        if (ch != '#' && !isNewLineChar(ch)) {
                            throw new YamlParseException("reference token '*" + referenceKey + "' cannot be followed by any value character, line " + lineNum);
                        }
                        // 清除注释
                        if (ch == '#') {
                            // 注释内容
                            i++;
                            while (!isNewLineChar(source[i])) {
                                i++;
                            }
                        }
                    }
                } else if (ch == '!') {

                    if (valueType == 0) {
                        // 类型转化枚举
                        char typeAppend = source[i + 1];
                        if (typeAppend != '!') {
                            valueType = 1;
                            continue;
//                            throw new YamlParseException("'!' must be followed by '!' but found '" + typeAppend + "', line " + lineNum);
                        }
                        i++;
                        i++;
                        j = i;
                        while ((ch = source[++i]) != SPACE_CHAR && !isNewLineChar(ch)) ;

                        String typeName;
                        if (ch == SPACE_CHAR) {
                            typeName = new String(source, j, i - j).trim();
                            if (typeValues.containsKey(typeName)) {
                                valueType = typeValues.get(typeName);
                            } else {
                                throw new YamlParseException("Mandatory type '!!" + typeName + "' is not supported, line " + lineNum);
                            }
                        } else {
                            throw new YamlParseException("token '!!' must be followed by one type such as int, str, bool ...,  but empty, line " + lineNum);
                        }
                    }

                } else {

                    boolean json = ch == '{' || ch == '[';

                    char pv = '\0';
                    int commentIndex = -1;
                    while (!isNewLineChar(ch = source[i])) {
                        if (commentIndex == -1 && pv == SPACE_CHAR && ch == '#') {
                            commentIndex = i;
                        }
                        i++;
                        pv = ch;
                    }

                    value = new String(source, j, (commentIndex > -1 ? commentIndex : i) - j).trim();
                    leaf = value.length() > 0;

                    if (json) {
                        // 解析JSON数据
                        try {
                            typeOfValue = YamlJSON.parse(value);
                        } catch (YamlParseException exception) {
                            String message = exception.getMessage();
                            message += ", at lineNum " + (lineNum + 1);
                            throw new YamlParseException(message, exception);
                        }
                    }

                    break;
                }
            }

            if (breakOutLoop) {
                break;
            }

            if (!arrayToken && splitIndex == -1) {
                if (prevCh == SPLIT_CHAR) {
                    key = new String(source, j, i - j - 1).trim();
                    leaf = false;
                } else {
                    throw new YamlParseException("Separator ': ' not found in syntax, at line " + ++lineNum);
                }
            }

            // 空行
            if (isEmpty) continue;

            // 添加到list
            addYamlNode(yamlNodes, indent, lineIndex, key, value, valueType, typeOfValue, leaf, textBlock, arrayToken, anchorKey, referenceKey);
        }
    }

    /**
     * 根据块类型去除尾部换行或者空格
     */
    private String getBlockValue(StringBuilder blockValue, int blockType, char target) {
        if (blockType == 1) {
            // (|+ or >+)原样输出
            return blockValue.toString();
        }
        int len = blockValue.length();
        int i = len;
        while (i > 0 && blockValue.charAt(i - 1) == target) {
            i--;
        }
        // blockType == 0(| or >) 时保留一行空行（或者空格）
        // blockType == 2(|- or >-) 清除所有空行（或者空格）
        int newLen = (blockType == 2 || i == len) ? i : i + 1;
        blockValue.setLength(newLen);
        return blockValue.toString();
    }

    private YamlNode addYamlNode(List<YamlNode> yamlNodes, int indent, int lineNum, String key, String value, int valueType, Object typeOfValue, boolean leaf, boolean textBlock, boolean arrayToken, String anchorKey, String referenceKey) {
        YamlNode yamlNode = new YamlNode();
        yamlNode.indent = indent;
        yamlNode.lineNum = lineNum;
        yamlNode.textBlock = textBlock;
        yamlNode.key = key;
        yamlNode.value = value;
        yamlNode.valueType = valueType;
        yamlNode.typeOfValue = typeOfValue;
        yamlNode.leaf = leaf;
        yamlNode.arrayToken = arrayToken;
        yamlNode.anchorKey = anchorKey;
        yamlNode.referenceKey = referenceKey;
        yamlNodes.add(yamlNode);
        return yamlNode;
    }

    /**
     * 如果是最后一个字符判定为true
     */
    private boolean isNewLineChar(char ch) {
        if (ch == '\n') {
            lineNum++;
            return true;
        }
        return ch == '\r';
    }

}
