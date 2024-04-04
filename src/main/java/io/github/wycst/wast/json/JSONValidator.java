package io.github.wycst.wast.json;

import io.github.wycst.wast.json.exceptions.JSONException;
import io.github.wycst.wast.json.options.ReadOption;

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
        JSONOptions.readOptions(readOptions, parseContext);
        this.parseContext = parseContext;
        this.showMessage = showMessage;
        try {
            boolean allowComment = parseContext.allowComment;
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
                    setValidateMessage("Syntax error, at pos " + offset + " extra characters found, '" + new String(buf, offset + 1, wordNum) + " ...'");
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

    private void validateJSONArray(int fromIndex) throws Exception {
        int beginIndex = fromIndex + 1;
        char ch = '\0';
        int size = 0;
        for (int i = beginIndex; i < toIndex; ++i) {
            // clear white space characters
            while ((ch = buf[i]) <= ' ') {
                ++i;
            }
            if (ch == ']') {
                if (size > 0) {
                    result = false;
                    if (showMessage) {
                        setValidateMessage("Syntax error, at pos " + i + ", the closing symbol ']' is not allowed here.");
                    }
                    return;
                }
                offset = i;
                return;
            }
            size++;
            this.validateValue(ch, i, ']');
            i = offset;
            if (!result) return;
            // clear white space characters
            while ((ch = buf[++i]) <= ' ') ;
            boolean isEnd = ch == ']';
            if (ch == ',' || isEnd) {
                if (isEnd) {
                    offset = i;
                    return;
                }
            } else {
                this.result = false;
                if (showMessage) {
                    setValidateMessage("Syntax error, at pos " + i + ", unexpected '" + ch + "', expected ',' or ']'");
                }
                return;
            }
        }
        this.result = false;
        if (showMessage) {
            setValidateMessage("Syntax error, cannot find closing symbol ']' matching '['");
        }
    }

    private void validateJSONObject(int fromIndex) throws Exception {

        int beginIndex = fromIndex + 1;
        char ch;
        boolean empty = true;

        // for loop to parse
        for (int i = beginIndex; i < toIndex; ++i) {
            // clear white space characters
            while ((ch = buf[i]) <= ' ') {
                ++i;
            }
            int splitIndex, simpleToIndex = -1;
            // Standard JSON field name with "
            if (ch == '"') {
                while (i + 1 < toIndex && (ch = buf[++i]) != '"') ;
                if(ch == '"') {
                    char prev = buf[i - 1];
                    while (prev == '\\') {
                        boolean prevEscapeFlag = true;
                        int j = i - 1;
                        while (buf[--j] == '\\') {
                            prevEscapeFlag = !prevEscapeFlag;
                        }
                        if(prevEscapeFlag) {
                            while (i + 1 < toIndex && buf[++i] != '"');
                            prev = buf[i - 1];
                        } else {
                            break;
                        }
                    }
                    ch = buf[i];
                }
                if (ch != '"') {
                    result = false;
                    if (showMessage) {
                        String errorContextTextAt = createErrorContextText(buf, i);
                        setValidateMessage("Syntax error, util pos " + i + ", context text by '" + errorContextTextAt + "' the closing symbol '\"' is not found ");
                    }
                    return;
                }
                empty = false;
                ++i;
            } else {
                if (ch == '}') {
                    if (!empty) {
                        result = false;
                        if (showMessage) {
                            String errorContextTextAt = createErrorContextText(buf, i);
                            setValidateMessage("Syntax error, at pos " + i + ", context text by '" + errorContextTextAt + "' the closing symbol '}' is not allowed here.");
                        }
                        return;
                    }
                    offset = i;
                    return;
                }

                if (ch == '\'') {
                    if (parseContext.allowSingleQuotes) {
                        while (i + 1 < toIndex && buf[++i] != '\'') ;
                        empty = false;
                        ++i;
                    } else {
                        this.result = false;
                        if (showMessage) {
                            String errorContextTextAt = createErrorContextText(buf, i);
                            setValidateMessage("Syntax error, at pos " + i + ", context text by '" + errorContextTextAt + "', the single quote symbol ' is not allowed here.");
                        }
                        return;
                    }
                } else {
                    if (parseContext.allowUnquotedFieldNames) {
                        // 无引号key处理
                        // 直接锁定冒号（:）位置
                        while (i + 1 < toIndex && buf[++i] != ':') ;
                        empty = false;
                    }
                }
            }

            // clear white space characters
            while ((ch = buf[i]) <= ' ') {
                ++i;
            }
            if (ch == ':') {
                while ((ch = buf[++i]) <= ' ') ;
                this.validateValue(ch, i, '}');
                i = offset;
                if (!result) return;
                // clear white space characters
                while ((ch = buf[++i]) <= ' ') ;

                // Check whether the next character is a comma or end symbol '}'. If yes, continue or break. If not, throw an exception
                boolean isClosingSymbol = ch == '}';
                if (ch == ',' || isClosingSymbol) {
                    if (isClosingSymbol) {
                        offset = i;
                        return;
                    }
                } else {
                    this.result = false;
                    if (showMessage) {
                        String errorContextTextAt = createErrorContextText(buf, i);
                        setValidateMessage("Syntax error, at pos " + i + ", context text by '" + errorContextTextAt + "', unexpected '" + ch + "', expected ',' or '}'");
                    }
                    return;
                }
            } else {
                this.result = false;
                if (showMessage) {
                    String errorContextTextAt = createErrorContextText(buf, i);
                    setValidateMessage("Syntax error, at pos " + i + ", context text by '" + errorContextTextAt + "', unexpected '" + ch + "', token ':' is expected.");
                }
                return;
            }
        }

        this.result = false;
        this.message = "Syntax error, the closing symbol '}' is not found ";
    }

    private void validateValue(char ch, int i, char endChar) throws Exception {
        switch (ch) {
            case '{': {
                validateJSONObject(i);
                break;
            }
            case '[': {
                validateJSONArray(i);
                break;
            }
            case '"':
            case '\'': {
                validateJSONString(i, ch);
                break;
            }
            case 'n': {
                validateNULL(i);
                break;
            }
            case 't': {
                validateTrue(i);
                break;
            }
            case 'f': {
                validateFalse(i);
                break;
            }
            default: {
                validateNumber(i, '}');
                break;
            }
        }
    }

    private void validateJSONString(int from, char endCh) {
        int beginIndex = from + 1;
        char ch = '\0';
        int i = beginIndex;
        while (i < toIndex && (ch = buf[i]) != endCh) {
            ++i;
        }
        if(ch == endCh) {
            char prev = buf[i - 1];
            while (prev == '\\') {
                boolean prevEscapeFlag = true;
                int j = i - 1;
                while (buf[--j] == '\\') {
                    prevEscapeFlag = !prevEscapeFlag;
                }
                if(prevEscapeFlag) {
                    while (i < toIndex && buf[++i] != endCh);
                    prev = buf[i - 1];
                } else {
                    break;
                }
            }
        }
        offset = i;
        if (i == toIndex) {
            result = false;
            if (showMessage) {
                setValidateMessage("Syntax error, the closing symbol '" + endCh + "' is not found ");
            }
        }
    }

    private void validateNULL(int fromIndex) {
        int beginIndex = fromIndex + 1;
        if (fromIndex + 3 < toIndex && buf[beginIndex++] == 'u'
                && buf[beginIndex++] == 'l'
                && buf[beginIndex] == 'l') {
            offset = beginIndex;
            return;
        } else {
            result = false;
            if (showMessage) {
                int len = Math.min(4, toIndex - fromIndex);
                setValidateMessage("Syntax error, at pos " + fromIndex + ", expected 'null' because it starts with 'n', but found text '" + new String(buf, fromIndex, len) + "'");
            }
            return;
        }
    }

    private void validateTrue(int fromIndex) {
        int beginIndex = fromIndex + 1;
        if (fromIndex + 3 < toIndex && buf[beginIndex++] == 'r'
                && buf[beginIndex++] == 'u'
                && buf[beginIndex] == 'e') {
            offset = beginIndex;
            return;
        } else {
            result = false;
            if (showMessage) {
                int len = Math.min(4, toIndex - fromIndex);
                setValidateMessage("Syntax error, at pos " + fromIndex + ", expected 'true' because it starts with 't', but found text '" + new String(buf, fromIndex, len) + "'");
            }
            return;
        }
    }

    private void validateFalse(int fromIndex) {
        int beginIndex = fromIndex + 1;
        if (fromIndex + 4 < toIndex && buf[beginIndex++] == 'a'
                && buf[beginIndex++] == 'l'
                && buf[beginIndex++] == 's'
                && buf[beginIndex] == 'e') {
            offset = beginIndex;
            return;
        } else {
            result = false;
            if (showMessage) {
                int len = Math.min(4, toIndex - fromIndex);
                setValidateMessage("Syntax error, at pos " + fromIndex + ", expected 'false' because it starts with 'f', but found text '" + new String(buf, fromIndex, len) + "'");
            }
            return;
        }
    }

    private void validateNumber(int fromIndex, char endChar) throws Exception {
        try {
            JSONTypeDeserializer.NUMBER.deserializeDefault(buf, fromIndex, toIndex, endChar, parseContext);
            offset = parseContext.endIndex;
        } catch (Throwable throwable) {
            result = false;
            if (showMessage) {
                setValidateMessage(throwable.getMessage());
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
}