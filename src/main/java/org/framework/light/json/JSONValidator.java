package org.framework.light.json;

import org.framework.light.json.exceptions.JSONException;
import org.framework.light.json.options.JSONParseContext;
import org.framework.light.json.options.Options;
import org.framework.light.json.options.ReadOption;

/**
 * json校验
 * （通常情况下只要JSON格式错误，解析一定会抛出异常，但通过异常来判定影响性能，拷贝的解析的代码然后做了修改）
 *
 * @Author: wangy
 * @Date: 2022/6/3 20:01
 * @Description:
 */
public class JSONValidator extends JSONGeneral {

    private char[] buf;
    private int fromIndex;
    private int toIndex;
    private int offset;
    private char current;
    private boolean result = true;

    private boolean showMessage;
    private String message;
    private JSONParseContext parseContext;


    public JSONValidator(String json) {
        this(getChars(json));
    }

    public JSONValidator(char[] buf) {
        this.buf = buf;
    }

    public final boolean validate(boolean showMessage, ReadOption... readOptions) {
        if (buf == null || buf.length == 0) {
            if (this.showMessage) {
                setValidateMessage("Empty json to validate ");
            }
            return false;
        }
        this.init();
        JSONParseContext parseContext = new JSONParseContext();
        Options.readOptions(readOptions, parseContext);
        this.parseContext = parseContext;
        this.showMessage = showMessage;
        try {
            boolean allowComment = parseContext.isAllowComment();
            if (allowComment && current == '/') {
                /** 去除声明在头部的注释*/
                fromIndex = clearCommentAndWhiteSpaces(buf, fromIndex + 1, toIndex, parseContext);
                current = buf[fromIndex];
            }
            switch (current) {
                case '{':
                    validateJSONObject(fromIndex);
                    break;
                case '[':
                    validateJSONArray(fromIndex);
                    break;
                case '"':
                    validateJSONString(fromIndex, current);
                    break;
                default:
                    if (showMessage) {
                        setValidateMessage("Unsupported for begin character with '" + current + "' pos 0 ");
                    }
                    return false;
            }

            if (!result) {
                return false;
            }

            if (allowComment) {
                /** 去除声明在尾部的注释*/
                if (offset < toIndex - 1) {
                    char commentStart = '\0';
                    while (offset + 1 < toIndex && (commentStart = buf[++offset]) <= ' ') ;
                    if (commentStart == '/') {
                        offset = clearCommentAndWhiteSpaces(buf, offset + 1, toIndex, parseContext);
                    }
                }
            }

            if (offset != toIndex - 1) {
                int wordNum = Math.min(50, buf.length - offset - 1);
                if (showMessage) {
                    setValidateMessage("Syntax error, extra characters found, '" + new String(buf, offset + 1, wordNum) + "', at pos " + offset);
                }
                return false;
            }

        } catch (Exception ex) {
            if (ex instanceof JSONException) {
                throw (JSONException) ex;
            }
            throw new JSONException("Error: " + ex.getMessage(), ex);
        } finally {
            parseContext.clear();
        }

        return result;
    }

    /**
     * 校验json格式是否正确
     *
     * @param readOptions
     * @return
     */
    public final boolean validate(ReadOption... readOptions) {
        return validate(false, readOptions);
    }

    private void init() {
        this.fromIndex = 0;
        this.toIndex = buf.length;
        while ((fromIndex < toIndex) && (current = buf[fromIndex]) <= ' ') {
            fromIndex++;
        }
        while ((toIndex > fromIndex) && buf[toIndex - 1] <= ' ') {
            toIndex--;
        }
        this.result = true;
        this.message = null;
    }

    private void validateJSONArray(int fromIndex) {

        int beginIndex = fromIndex + 1;
        char ch = '\0';

        int size = 0;
        // 集合数组核心token是逗号（The core token of the collection array is a comma）
        // for loop
        for (int i = beginIndex; i < toIndex; i++) {
            // clear white space characters
            while ((ch = buf[i]) <= ' ') {
                i++;
            }
            // for simple element parse
            int simpleFromIndex = i, simpleToIndex = -1;

            // 如果提前遇到字符']'，说明是空集合
            //  （If the character ']' is encountered in advance, it indicates that it is an empty set）
            // 另一种可能性(语法错误): 非空集合，发生在空逗号后接结束字符']'直接抛出异常
            //  (Another possibility(Syntax error): whether the empty comma followed by the closing character ']' throws an exception)
            if (ch == ']') {
                if (size > 0) {
                    result = false;
                    if (showMessage) {
                        setValidateMessage("Syntax error, not allowed ',' followed by ']', pos " + i);
                    }
                    return;
                }
                offset = i;
                return;
            }

            size++;

            // 是否简单元素如null, true or false or numeric or string
            // (is simple elements such as null, true or false or numeric or string)
            boolean isSimpleElement = false;
            if (ch == '{') {
                validateJSONObject(i);
                i = offset;
            } else if (ch == '[') {
                // 2 [ array
                validateJSONArray(i);
                i = offset;
            } else if (ch == '"' || ch == '\'') {
                // 3 string
                // When there are escape characters, the escape character needs to be parsed
                validateJSONString(i, ch);
                i = offset;
            } else {
                // 简单元素（Simple element）
                isSimpleElement = true;
                while (i + 1 < toIndex) {
                    ch = buf[i + 1];
                    if (ch == ',' || ch == ']') {
                        break;
                    }
                    i++;
                }
            }

            if (!result) return;

            // 清除空白字符（clear white space characters）
            while ((ch = buf[++i]) <= ' ') ;
            if (simpleToIndex == -1) {
                simpleToIndex = i;
            }

            // 检查下一个字符是逗号还是结束符号。如果是，继续或者终止。如果不是，则抛出异常
            // （Check whether the next character is a comma or end symbol. If yes, continue or return . If not, throw an exception）
            boolean isEnd = ch == ']';
            if (ch == ',' || isEnd) {
                if (isSimpleElement) {
                    validateSimpleValue(simpleFromIndex, simpleToIndex);
                    if (!result) return;
                }
                if (isEnd) {
                    offset = i;
                    return;
                }
            } else {
                this.result = false;
                if (showMessage) {
                    setValidateMessage("Syntax error, unexpected token character '" + ch + "', position " + i + ", Missing ',' or '}'");
                }
                return;
            }
        }
        this.result = false;
        if (showMessage) {
            setValidateMessage("Syntax error, the closing symbol ']' is not found ");
        }
    }

    private void validateJSONObject(int fromIndex) {

        int beginIndex = fromIndex + 1;
        char ch;
        String key = null;

        boolean empty = true;

        // for loop to parse
        for (int i = beginIndex; i < toIndex; i++) {
            // clear white space characters
            while ((ch = buf[i]) <= ' ') {
                i++;
            }
            int fieldKeyFrom = i, fieldKeyTo, splitIndex, simpleToIndex = -1;
            // Standard JSON field name with "
            if (ch == '"') {
                while (i + 1 < toIndex && (buf[++i] != '"' || buf[i - 1] == '\\')) ;
                empty = false;
                i++;
            } else {
                if (ch == '}') {
                    if (!empty) {
                        result = false;
                        if (showMessage) {
                            setValidateMessage("Syntax error, the closing symbol '}' is not allowed at pos " + i);
                        }
                        return;
                    }
                    offset = i;
                    // 空对象提前结束查找
                    return;
                }

                if (ch == '\'') {
                    if (parseContext.isAllowSingleQuotes()) {
                        while (i + 1 < toIndex && buf[++i] != '\'') ;
                        empty = false;
                        i++;
                    } else {
                        this.result = false;
                        if (showMessage) {
                            setValidateMessage("Syntax error, the single quote symbol ' is not allowed at pos " + i);
                        }
                        return;
                    }
                } else {
                    if (parseContext.isAllowUnquotedFieldNames()) {
                        // 无引号key处理
                        // 直接锁定冒号（:）位置
                        while (i + 1 < toIndex && buf[++i] != ':') ;
                        empty = false;
                    }
                }
            }

            // clear white space characters
            while ((ch = buf[i]) <= ' ') {
                i++;
            }
            // 清除注释前记录属性字段的token结束位置
            fieldKeyTo = i;

            // Standard JSON rules:
            // 1 if matched, it can only be followed by a colon, and all other symbols are wrong
            // 2 after the attribute value is parsed, it can only be followed by a comma(,) or end, and other symbols are wrong
            if (ch == ':') {
                // Resolve key value pairs
//                validateFieldKey(fieldKeyFrom, fieldKeyTo);
                // 清除空白字符（clear white space characters）
                while ((ch = buf[++i]) <= ' ') ;

                // 分割符位置,SimpleMode
                splitIndex = i - 1;

                // 空，布尔值，数字，或者字符串 （ null, true or false or numeric or string）
                boolean isSimpleValue = false;

                switch (ch) {
                    case '{': {
                        validateJSONObject(i);
                        i = offset;
                        break;
                    }
                    case '[': {
                        // 2 [ array
                        // 解析集合或者数组 （Parse a collection or array）
                        validateJSONArray(i);
                        i = offset;
                        break;
                    }
                    case '"':
                    case '\'': {
                        validateJSONString(i, ch);
                        i = offset;
                        break;
                    }
                    default: {
                        isSimpleValue = true;
                        // 4 null, true or false or numeric
                        // Find comma(,) or closing symbol(})
                        while (i + 1 < toIndex) {
                            ch = buf[i + 1];
                            if (ch == ',' || ch == '}') {
                                break;
                            }
                            i++;
                        }
                        // Check whether post comments are appended
                    }
                }

                if (!result) return;

                // clear white space characters
                while ((ch = buf[++i]) <= ' ') ;
                if (simpleToIndex == -1) {
                    simpleToIndex = i;
                }

                // Check whether the next character is a comma or end symbol '}'. If yes, continue or break. If not, throw an exception
                boolean isClosingSymbol = ch == '}';
                if (ch == ',' || isClosingSymbol) {
                    if (isSimpleValue) {
                        validateSimpleValue(splitIndex + 1, simpleToIndex);
                    }
                    if (isClosingSymbol) {
                        offset = i;
                        return;
                    }
                } else {
                    this.result = false;
                    if (showMessage) {
                        setValidateMessage("Syntax error, unexpected token character '" + ch + "', position " + i);
                    }
                    return;
                }
            } else {
                this.result = false;
                if (showMessage) {
                    setValidateMessage("Syntax error, unexpected token character '" + ch + "', position " + i);
                }
                return;
            }
        }

        this.result = false;
        this.message = "Syntax error, the closing symbol '}' is not found ";
    }

    private void validateJSONString(int from, char endCh) {
        int beginIndex = from + 1;
        char ch = '\0';
        int i = beginIndex;
        char prev = '\0';
        while (i < toIndex && (ch = buf[i]) != '"' || prev == '\\') {
            i++;
            prev = ch;
        }
        offset = i;
        if (i == toIndex) {
            result = false;
            if (showMessage) {
                setValidateMessage("Syntax error, the closing symbol '" + endCh + "' is not found ");
            }
        }
    }

    // 数字， boolean， null
    private void validateSimpleValue(int fromIndex, int toIndex) {
        // 初始位置
        char beginChar = '\0';
        char endChar = '\0';

        while ((fromIndex < toIndex) && ((beginChar = buf[fromIndex]) <= ' ')) {
            fromIndex++;
        }
        while ((toIndex > fromIndex) && ((endChar = buf[toIndex - 1]) <= ' ')) {
            toIndex--;
        }

        int len = toIndex - fromIndex;
        switch (beginChar) {
            case 't':
                if (buf[fromIndex + 1] == 'r'
                        && buf[fromIndex + 2] == 'u'
                        && endChar == 'e') {
                    return;
                } else {
                    result = false;
                    if (showMessage) {
                        setValidateMessage("Syntax error, offset pos " + fromIndex + ", 't' is unexpected");
                    }
                    return;
                }
            case 'f':
                if (buf[fromIndex + 1] == 'a'
                        && buf[fromIndex + 2] == 'l'
                        && buf[fromIndex + 3] == 's'
                        && endChar == 'e') {
                    return;
                } else {
                    result = false;
                    if (showMessage) {
                        setValidateMessage("Syntax error, offset pos " + fromIndex + ", 'f' is unexpected");
                    }
                    return;
                }
            case 'n':
                if (buf[fromIndex + 1] == 'u'
                        && buf[fromIndex + 2] == 'l'
                        && endChar == 'l') {
                    return;
                } else {
                    result = false;
                    if (showMessage) {
                        setValidateMessage("Syntax error, offset pos " + fromIndex + ", 'n' is unexpected");
                    }
                    return;
                }
            default:
                // 判断是否数字
                // +-开头,最多一个小数点,最多一个E, 结尾DL
                int i = fromIndex;
                if (beginChar == '+' || beginChar == '-') {
                    i++;
                }
                int dotNum = 0;
                boolean existUnDigitChar = false;
                for (; i < toIndex; i++) {
                    char ch = buf[i];
                    if (ch >= '0' || ch <= '9') continue;
                    if (ch == '.') {
                        dotNum++;
                    } else {
                        existUnDigitChar = true;
                    }
                }
                if (!existUnDigitChar && dotNum < 2) {
                    return;
                }
                try {
                    parseDouble(buf, fromIndex, toIndex);
                } catch (Throwable throwable) {
                    result = false;
                    if (showMessage) {
                        setValidateMessage("Syntax error, offset pos " + fromIndex + " number error .");
                    }
                }
        }
    }

    void setValidateMessage(String text) {
        this.result = false;
        this.message = text;
    }

    public String getValidateMessage() {
        return this.message;
    }

//    /**
//     * @param from
//     * @param to
//     * @return
//     */
//    private boolean validateFieldKey(int from, int to) {
//        char start = '"';
//        while ((from < to) && ((start = buf[from]) <= ' ')) {
//            from++;
//        }
//        char end = '"';
//        while ((to > from) && ((end = buf[to - 1]) <= ' ')) {
//            to--;
//        }
//        if (start == '"' && end == '"' || (start == '\'' && end == '\'')) {
//            int len = to - from - 2;
//            return
//        }
//        return new String(buf, from, to - from);
//    }


}