package io.github.wycst.wast.json;

import io.github.wycst.wast.common.beans.AsciiStringSource;
import io.github.wycst.wast.common.beans.CharSource;
import io.github.wycst.wast.common.beans.UTF16ByteArraySource;
import io.github.wycst.wast.common.reflect.GenericParameterizedType;
import io.github.wycst.wast.common.reflect.UnsafeHelper;
import io.github.wycst.wast.common.utils.EnvUtils;
import io.github.wycst.wast.json.exceptions.JSONException;
import io.github.wycst.wast.json.options.ReadOption;

/**
 * pojo Deserializer
 *
 * @Author wangyunchao
 * @Date 2022/6/28 13:02
 */
public abstract class JSONPojoDeserializer<T> extends JSONTypeDeserializer {

    // private final Class<? extends T> pojoClass;
    protected final JSONPojoStructure pojoStructureWrapper;

    public JSONPojoDeserializer(Class<T> pojoClass) {
        pojoClass.getClass();
        // this.pojoClass = pojoClass;
        pojoStructureWrapper = JSONPojoStructure.get(pojoClass);
        if (pojoStructureWrapper == null) {
            throw new UnsupportedOperationException("type not support for " + pojoClass);
        }
    }

    JSONPojoDeserializer(JSONPojoStructure pojoStructureWrapper) {
        this.pojoStructureWrapper = pojoStructureWrapper;
    }

    protected final T deserialize(CharSource charSource, char[] buf, int fromIndex, int toIndex, GenericParameterizedType parameterizedType, Object entity, char endToken, JSONParseContext jsonParseContext) throws Exception {
        char beginChar = buf[fromIndex];

        if(beginChar == '{') {
            return (T) deserializePojo(charSource, buf, fromIndex, toIndex, parameterizedType, entity, endToken, jsonParseContext);
        } else if(beginChar == 'n') {
            NULL.deserialize(null, buf, fromIndex, toIndex, null, null, jsonParseContext);
            return null;
        } else {
            if(jsonParseContext.unMatchedEmptyAsNull && beginChar == '"' && buf[fromIndex + 1] == '"') {
                jsonParseContext.endIndex = fromIndex + 1;
                return null;
            }
            // not support or custom handle ?
            String errorContextTextAt = createErrorContextText(buf, fromIndex);
            throw new JSONException("Syntax error, at pos " + fromIndex + ", context text by '" + errorContextTextAt + "', unexpected '" + beginChar + "' for Object Type, expected '{' ");
        }
    }

    protected final T deserialize(CharSource charSource, byte[] buf, int fromIndex, int toIndex, GenericParameterizedType parameterizedType, Object entity, byte endToken, JSONParseContext jsonParseContext) throws Exception {
        byte beginByte = buf[fromIndex];
        switch (beginByte) {
            case '{':
                return (T) deserializePojo(charSource, buf, fromIndex, toIndex, parameterizedType, entity, beginByte, jsonParseContext);
            case 'n':
                parseNull(buf, fromIndex, toIndex, jsonParseContext);
                return null;
            default: {
                if(jsonParseContext.unMatchedEmptyAsNull && beginByte == DOUBLE_QUOTATION && buf[fromIndex + 1] == DOUBLE_QUOTATION) {
                    jsonParseContext.endIndex = fromIndex + 1;
                    return null;
                }
                // not support or custom handle ?
                String errorContextTextAt = createErrorContextText(buf, fromIndex);
                throw new JSONException("Syntax error, at pos " + fromIndex + ", context text by '" + errorContextTextAt + "', unexpected '" + (char) beginByte + "' for Object Type, expected '{' ");
            }
        }
    }

    protected Object pojo(Object value) {
        return value;
    }

    final Object deserializePojo(CharSource charSource, char[] buf, int fromIndex, int toIndex, GenericParameterizedType parameterizedType, Object entity, char endToken, JSONParseContext jsonParseContext) throws Exception {
        if (entity == null) {
            entity = createPojo();
        }
        boolean empty = true;
        char ch;
        final boolean allowComment = jsonParseContext.allowComment;
        int i = fromIndex;
        for (;;) {
            while ((ch = buf[++i]) <= ' ');
            if (allowComment) {
                if (ch == '/') {
                    ch = buf[i = clearCommentAndWhiteSpaces(buf, i + 1, toIndex, jsonParseContext)];
                }
            }
            int fieldKeyFrom = i;
            JSONPojoFieldDeserializer fieldDeserializer = null;
            if (ch == '"') {
                if((ch = buf[++i]) != '"') {
                    long hashValue = ch;
                    char ch1;
                    if ((ch = buf[++i]) != '"' && (ch1 = buf[++i]) != '"') {
                        hashValue = pojoStructureWrapper.hashChar(hashValue, ch, ch1);
                        if ((ch = buf[++i]) != '"' && (ch1 = buf[++i]) != '"') {
                            hashValue = pojoStructureWrapper.hashChar(hashValue, ch, ch1);
                            while ((ch = buf[++i]) != '"' && (ch1 = buf[++i]) != '"') {
                                hashValue = pojoStructureWrapper.hashChar(hashValue, ch, ch1);
                            }
                        }
                    }
                    if(ch != '"') {
                        hashValue = pojoStructureWrapper.hashChar(hashValue, ch);
                    }
                    fieldDeserializer = getFieldDeserializer(buf, ++fieldKeyFrom, i, hashValue);
                    if(fieldDeserializer == null) {
                        int j = i - 1;
                        if(buf[j] == '\\') {
                            boolean isPrevEscape = true;
                            while (buf[--j] == '\\') {
                                isPrevEscape = !isPrevEscape;
                            }
                            if(isPrevEscape) {
                                // skip
                                char prev = 0;
                                while (((ch = buf[++i]) != '"' || prev == '\\')) {
                                    prev = ch;
                                }
                            }
                        }
                    }
                }
                empty = false;
                while ((ch = buf[++i]) <= ' ');
            } else {
                if (ch == '}') {
                    if (!empty && !jsonParseContext.allowLastEndComma) {
                        String errorContextTextAt = createErrorContextText(buf, i);
                        throw new JSONException("Syntax error, at pos " + i + ", context text by '" + errorContextTextAt + "' the closing symbol '}' is not allowed here.");
                    }
                    jsonParseContext.endIndex = i;
                    return pojo(entity);
                }
                if (ch == '\'') {
                    if (jsonParseContext.allowSingleQuotes) {
                        long hashValue = 0;
                        while (i + 1 < toIndex && (ch = buf[++i]) != '\'') {
                            hashValue = pojoStructureWrapper.hashChar(hashValue, ch);
                        }
                        fieldDeserializer = getFieldDeserializer(buf, ++fieldKeyFrom, i, hashValue);
                        empty = false;
                        while ((ch = buf[++i]) <= ' ');
                    } else {
                        String errorContextTextAt = createErrorContextText(buf, i);
                        throw new JSONException("Syntax error, at pos " + i + ", context text by '" + errorContextTextAt + "' the single quote symbol ' is not allowed here.");
                    }
                } else {
                    if (jsonParseContext.allowUnquotedFieldNames) {
                        long hashValue = 0;
                        while (i + 1 < toIndex && (ch = buf[++i]) != ':') {
                            if (ch > ' ') {
                                hashValue = pojoStructureWrapper.hashChar(hashValue, ch);
                            }
                        }
                        fieldDeserializer = getFieldDeserializer(buf, fieldKeyFrom, i, hashValue);
                        empty = false;
                    } else {
                        String errorContextTextAt = createErrorContextText(buf, i);
                        throw new JSONException("Syntax error, at pos " + i + ", context text by '" + errorContextTextAt + "'\"' is required.");
                    }
                }
            }
            if (allowComment) {
                if (ch == '/') {
                    ch = buf[i = clearCommentAndWhiteSpaces(buf, i + 1, toIndex, jsonParseContext)];
                }
            }
            if (ch == ':') {
                // buf fieldKeyFrom fieldKeyTo
                while ((buf[++i]) <= ' ') ;
                if (allowComment) {
                    ch = buf[i];
                    if (ch == '/') {
                        i = clearCommentAndWhiteSpaces(buf, i + 1, toIndex, jsonParseContext);
                    }
                }
                // FieldDeserializer fieldDeserializer = getFieldDeserializer(buf, fieldKeyFrom, fieldKeyTo, isUnquotedFieldName, hashValue);
                boolean isDeserialize = fieldDeserializer != null;
                Object defaultFieldValue = null;
                GenericParameterizedType valueType = null;
                JSONTypeDeserializer deserializer = null;
                if (isDeserialize) {
                    valueType = fieldDeserializer.getGenericParameterizedType();
                    deserializer = fieldDeserializer.getDeserializer();
                    if (deserializer == null) {
                        Class implClass;
                        if ((implClass = fieldDeserializer.getImplClass()) != null) {
                            valueType = valueType.copyAndReplaceActualType(implClass);
                            deserializer = getTypeDeserializer(implClass);
                        } else {
                            ch = buf[i];
                            if (ch == '{') {
                                // object
                                // read first field if key is '@c' and value as the implClass
                                String className = parseObjectClassName(charSource, buf, i, toIndex, jsonParseContext);
                                if (className == null) {
                                    // use DefaultFieldInstance
                                    if (jsonParseContext.useDefaultFieldInstance) {
                                        defaultFieldValue = fieldDeserializer.getDefaultFieldValue(entity);
                                        if (defaultFieldValue != null) {
                                            implClass = defaultFieldValue.getClass();
                                            valueType = valueType.copyAndReplaceActualType(implClass);
                                            deserializer = getTypeDeserializer(implClass);
                                        }
                                    } else {
                                        isDeserialize = false;
                                    }
                                } else {
                                    try {
                                        Class<?> cls = getClassByName(className);
                                        // check if isAssignableFrom cls
                                        if (fieldDeserializer.isAvailableImpl(cls)) {
                                            valueType = valueType.copyAndReplaceActualType(cls);
                                            deserializer = getTypeDeserializer(cls);
                                            JSONPojoDeserializer fieldPojoDeserializer = (JSONPojoDeserializer) deserializer;
                                            i = jsonParseContext.endIndex;
                                            while ((ch = buf[++i]) <= ' ') ;
                                            Object value;
                                            if (ch == ',') {
                                                value = fieldPojoDeserializer.deserializePojo(charSource, buf, i, toIndex, valueType, null, '}', jsonParseContext);
                                                i = jsonParseContext.endIndex;
                                            } else if (ch == '}') {
                                                value = pojo(fieldPojoDeserializer.createPojo());
                                            } else {
                                                String errorContextTextAt = createErrorContextText(buf, i);
                                                throw new JSONException("Syntax error, at pos " + i + ", context text by '" + errorContextTextAt + "', unexpected '" + ch + "', expected ',' or '}'");
                                            }
                                            setFieldValue((T) entity, fieldDeserializer, value);
                                            while ((ch = buf[++i]) <= ' ') ;
                                            if (ch == ',') {
                                                continue;
                                            }
                                            if (ch == '}') {
                                                jsonParseContext.endIndex = i;
                                                return pojo(entity);
                                            }
                                            String errorContextTextAt = createErrorContextText(buf, i);
                                            throw new JSONException("Syntax error, at pos " + i + ", context text by '" + errorContextTextAt + "', unexpected '" + ch + "', expected ',' or '}'");
                                        } else {
                                            isDeserialize = false;
                                        }
                                    } catch (Throwable throwable) {
                                        throw new JSONException(throwable.getMessage(), throwable);
                                    }
                                }
                            } else {
                                Object value = ANY.deserialize(charSource, buf, i, toIndex, null, null, '}', jsonParseContext);
                                if (fieldDeserializer.isInstance(value)) {
                                    setFieldValue((T) entity, fieldDeserializer, value);
                                    i = jsonParseContext.endIndex;
                                    while ((ch = buf[++i]) <= ' ') ;
                                    if (ch == ',') {
                                        continue;
                                    }
                                    if (ch == '}') {
                                        jsonParseContext.endIndex = i;
                                        return pojo(entity);
                                    }
                                    String errorContextTextAt = createErrorContextText(buf, i);
                                    throw new JSONException("Syntax error, at pos " + i + ", context text by '" + errorContextTextAt + "', unexpected '" + ch + "', expected ',' or '}'");
                                }
                            }
                        }
                    } else {
                        boolean camouflage = valueType.isCamouflage();
                        if (camouflage) {
                            valueType = getGenericValueType(parameterizedType, valueType);
                            if (!fieldDeserializer.isCustomDeserialize()) {
                                deserializer = getTypeDeserializer(valueType.getActualType());
                            }
                        }
                    }
                }

                if (isDeserialize) {
                    Object value = deserializer.deserialize(charSource, buf, i, toIndex, valueType, defaultFieldValue, '}', jsonParseContext);
                    setFieldValue((T) entity, fieldDeserializer, value);
                } else {
                    JSONTypeDeserializer.ANY.skip(charSource, buf, i, toIndex, '}', jsonParseContext);
                }
                i = jsonParseContext.endIndex;
                int endChar = jsonParseContext.endChar;
                if(endChar == 0) {
                    while ((ch = buf[++i]) <= ' ') ;
                } else {
                    ch = (char) endChar;
                    jsonParseContext.endChar = 0;
                }
                if (ch == ',') {
                    continue;
                }
                if (ch == '}') {
                    jsonParseContext.endIndex = i;
                    return pojo(entity);
                }
                String errorContextTextAt = createErrorContextText(buf, i);
                throw new JSONException("Syntax error, at pos " + i + ", context text by '" + errorContextTextAt + "', unexpected '" + ch + "', expected ',' or '}'");
            } else {
                String errorContextTextAt = createErrorContextText(buf, i);
                throw new JSONException("Syntax error, at pos " + i + ", context text by '" + errorContextTextAt + "', unexpected '" + ch + "', token character ':' is expected.");
            }
        }
    }

    final Object deserializePojo(CharSource charSource, byte[] buf, int fromIndex, int toIndex, GenericParameterizedType parameterizedType, Object entity, byte endToken, JSONParseContext jsonParseContext) throws Exception {
        if (entity == null) {
            entity = createPojo();
        }
        boolean empty = true;
        byte b;
        final boolean allowComment = jsonParseContext.allowComment;
        int i = fromIndex;
        for (;;) {
            while ((b = buf[++i]) <= WHITE_SPACE);
            if (allowComment) {
                if (b == '/') {
                    b = buf[i = clearCommentAndWhiteSpaces(buf, i + 1, toIndex, jsonParseContext)];
                }
            }
            int fieldKeyFrom = i;
            JSONPojoFieldDeserializer fieldDeserializer = null;
            if (b == DOUBLE_QUOTATION) {
                if((b = buf[++i]) != DOUBLE_QUOTATION) {
                    long hashValue = b;
                    byte b1;
                    if ((b = buf[++i]) != DOUBLE_QUOTATION && (b1 = buf[++i]) != DOUBLE_QUOTATION) {
                        hashValue = pojoStructureWrapper.hashChar(hashValue, b, b1);
                        if ((b = buf[++i]) != DOUBLE_QUOTATION && (b1 = buf[++i]) != DOUBLE_QUOTATION) {
                            hashValue = pojoStructureWrapper.hashChar(hashValue, b, b1);
                            while ((b = buf[++i]) != DOUBLE_QUOTATION && (b1 = buf[++i]) != DOUBLE_QUOTATION) {
                                hashValue = pojoStructureWrapper.hashChar(hashValue, b, b1);
                            }
                        }
                    }
                    if(b != DOUBLE_QUOTATION) {
                        hashValue = pojoStructureWrapper.hashChar(hashValue, b);
                    }
                    fieldDeserializer = getFieldDeserializer(buf, ++fieldKeyFrom, i, hashValue);
                    if(fieldDeserializer == null) {
                        int j = i - 1;
                        if(buf[j] == ESCAPE) {
                            boolean isPrevEscape = true;
                            while (buf[--j] == ESCAPE) {
                                isPrevEscape = !isPrevEscape;
                            }
                            if(isPrevEscape) {
                                byte prev = 0;
                                // skip
                                while (((b = buf[++i]) != DOUBLE_QUOTATION || prev == ESCAPE)) {
                                    prev = b;
                                }
                            }
                        }
                    }
                }
                empty = false;
                ++i;
            } else {
                if (b == END_OBJECT) {
                    if (!empty && !jsonParseContext.allowLastEndComma) {
                        String errorContextTextAt = createErrorContextText(buf, i);
                        throw new JSONException("Syntax error, at pos " + i + ", context text by '" + errorContextTextAt + "' the closing symbol '}' is not allowed here.");
                    }
                    jsonParseContext.endIndex = i;
                    return pojo(entity);
                }
                if (b == '\'') {
                    if (jsonParseContext.allowSingleQuotes) {
                        long hashValue = 0;
                        while (i + 1 < toIndex && (b = buf[++i]) != '\'') {
                            hashValue = pojoStructureWrapper.hashChar(hashValue, b);
                        }
                        fieldDeserializer = getFieldDeserializer(buf, ++fieldKeyFrom, i, hashValue);
                        empty = false;
                        ++i;
                    } else {
                        String errorContextTextAt = createErrorContextText(buf, i);
                        throw new JSONException("Syntax error, at pos " + i + ", context text by '" + errorContextTextAt + "' the single quote symbol ' is not allowed here.");
                    }
                } else {
                    if (jsonParseContext.allowUnquotedFieldNames) {
                        long hashValue = 0;
                        while (i + 1 < toIndex && buf[++i] != ':') {
                            if (b > ' ') {
                                hashValue = pojoStructureWrapper.hashChar(hashValue, b);
                            }
                        }
                        fieldDeserializer = getFieldDeserializer(buf, fieldKeyFrom, i, hashValue);
                        empty = false;
                    }
                }
            }
            while ((b = buf[i]) <= WHITE_SPACE) {
                ++i;
            }
            if (allowComment) {
                if (b == '/') {
                    b = buf[i = clearCommentAndWhiteSpaces(buf, i + 1, toIndex, jsonParseContext)];
                }
            }

            if (b == COLON_SIGN) {
                // buf fieldKeyFrom fieldKeyTo
                while ((buf[++i]) <= WHITE_SPACE) ;
                if (allowComment) {
                    b = buf[i];
                    if (b == '/') {
                        i = clearCommentAndWhiteSpaces(buf, i + 1, toIndex, jsonParseContext);
                    }
                }
                // FieldDeserializer fieldDeserializer = getFieldDeserializer(buf, fieldKeyFrom, fieldKeyTo, isUnquotedFieldName, hashValue);
                boolean isDeserialize = fieldDeserializer != null;
                Object defaultFieldValue = null;
                GenericParameterizedType valueType = null;
                JSONTypeDeserializer deserializer = null;
                if (isDeserialize) {
                    valueType = fieldDeserializer.getGenericParameterizedType();
                    deserializer = fieldDeserializer.getDeserializer();
                    if (deserializer == null) {
                        Class implClass;
                        if ((implClass = fieldDeserializer.getImplClass()) != null) {
                            valueType = valueType.copyAndReplaceActualType(implClass);
                            deserializer = getTypeDeserializer(implClass);
                        } else {
                            b = buf[i];
                            if (b == '{') {
                                // object
                                // read first field if key is '@c' and value as the implClass
                                String className = parseObjectClassName(charSource, buf, i, toIndex, jsonParseContext);
                                if (className == null) {
                                    // use DefaultFieldInstance
                                    if (jsonParseContext.useDefaultFieldInstance) {
                                        defaultFieldValue = fieldDeserializer.getDefaultFieldValue(entity);
                                        if (defaultFieldValue != null) {
                                            implClass = defaultFieldValue.getClass();
                                            valueType = valueType.copyAndReplaceActualType(implClass);
                                            deserializer = getTypeDeserializer(implClass);
                                        }
                                    } else {
                                        isDeserialize = false;
                                    }
                                } else {
                                    try {
                                        Class<?> cls = getClassByName(className);
                                        // check if isAssignableFrom cls
                                        if (fieldDeserializer.isAvailableImpl(cls)) {
                                            valueType = valueType.copyAndReplaceActualType(cls);
                                            deserializer = getTypeDeserializer(cls);
                                            JSONPojoDeserializer fieldPojoDeserializer = (JSONPojoDeserializer) deserializer;
                                            i = jsonParseContext.endIndex;
                                            while ((b = buf[++i]) <= ' ') ;
                                            Object value;
                                            if (b == ',') {
                                                value = fieldPojoDeserializer.deserializePojo(charSource, buf, i, toIndex, valueType, null, (byte)'}', jsonParseContext);
                                                i = jsonParseContext.endIndex;
                                            } else if (b == '}') {
                                                value = pojo(fieldPojoDeserializer.createPojo());
                                            } else {
                                                String errorContextTextAt = createErrorContextText(buf, i);
                                                throw new JSONException("Syntax error, at pos " + i + ", context text by '" + errorContextTextAt + "', unexpected '" + (char)b + "', expected ',' or '}'");
                                            }
                                            setFieldValue((T) entity, fieldDeserializer, value);
                                            while ((b = buf[++i]) <= ' ') ;
                                            if (b == ',') {
                                                continue;
                                            }
                                            if (b == '}') {
                                                jsonParseContext.endIndex = i;
                                                return pojo(entity);
                                            }
                                            String errorContextTextAt = createErrorContextText(buf, i);
                                            throw new JSONException("Syntax error, at pos " + i + ", context text by '" + errorContextTextAt + "', unexpected '" + (char) b + "', expected ',' or '}'");
                                        } else {
                                            isDeserialize = false;
                                        }
                                    } catch (Throwable throwable) {
                                        throw new JSONException(throwable.getMessage(), throwable);
                                    }
                                }
                            } else {
                                Object value = ANY.deserialize(charSource, buf, i, toIndex, null, null, (byte) '}', jsonParseContext);
                                if (fieldDeserializer.isInstance(value)) {
                                    setFieldValue((T) entity, fieldDeserializer, value);
                                    i = jsonParseContext.endIndex;
                                    while ((b = buf[++i]) <= ' ') ;
                                    if (b == ',') {
                                        continue;
                                    }
                                    if (b == '}') {
                                        jsonParseContext.endIndex = i;
                                        return pojo(entity);
                                    }
                                    String errorContextTextAt = createErrorContextText(buf, i);
                                    throw new JSONException("Syntax error, at pos " + i + ", context text by '" + errorContextTextAt + "', unexpected '" + (char) b + "', expected ',' or '}'");
                                }
                            }
                        }
                    } else {
                        boolean camouflage = valueType.isCamouflage();
                        if (camouflage) {
                            valueType = getGenericValueType(parameterizedType, valueType);
                            if (!fieldDeserializer.isCustomDeserialize()) {
                                deserializer = getTypeDeserializer(valueType.getActualType());
                            }
                        }
                    }
                }

                if (isDeserialize) {
                    Object value = deserializer.deserialize(charSource, buf, i, toIndex, valueType, defaultFieldValue, END_OBJECT, jsonParseContext);
                    setFieldValue((T) entity, fieldDeserializer, value);
                } else {
                    JSONTypeDeserializer.ANY.skip(charSource, buf, i, toIndex, END_OBJECT, jsonParseContext);
                }
                i = jsonParseContext.endIndex;
                int endChar = jsonParseContext.endChar;
                if(endChar == 0) {
                    while ((b = buf[++i]) <= WHITE_SPACE) ;
                } else {
                    b = (byte) endChar;
                    jsonParseContext.endChar = 0;
                }
                if (b == COMMA) {
                    continue;
                }
                if (b == END_OBJECT) {
                    jsonParseContext.endIndex = i;
                    return pojo(entity);
                }
                String errorContextTextAt = createErrorContextText(buf, i);
                throw new JSONException("Syntax error, at pos " + i + ", context text by '" + errorContextTextAt + "', unexpected '" + (char) b + "', expected ',' or '}'");
            } else {
                String errorContextTextAt = createErrorContextText(buf, i);
                throw new JSONException("Syntax error, at pos " + i + ", context text by '" + errorContextTextAt + "', unexpected '" + (char) b + "', token character ':' is expected.");
            }
        }
    }

    /**
     * 根据hash值和偏移位置获取属性的反序列化器
     *
     * @param buf<char[]>
     * @param offset
     * @param endIndex
     * @param hashValue
     * @return
     */
    protected JSONPojoFieldDeserializer getFieldDeserializer(char[] buf, int offset, int endIndex, long hashValue) {
        JSONPojoFieldDeserializer fieldDeserializer = pojoStructureWrapper.getFieldDeserializer(buf, offset, endIndex, hashValue);
        return fieldDeserializer;
    }

    /**
     * 根据hash值和偏移位置获取属性的反序列化器
     *
     * @param buf<byte[]>
     * @param offset
     * @param endIndex
     * @param hashValue
     * @return
     */
    protected JSONPojoFieldDeserializer getFieldDeserializer(byte[] buf, int offset, int endIndex, long hashValue) {
        JSONPojoFieldDeserializer fieldDeserializer = pojoStructureWrapper.getFieldDeserializer(buf, offset, endIndex, hashValue);
        return fieldDeserializer;
    }

    private GenericParameterizedType getGenericValueType(GenericParameterizedType parameterizedType, GenericParameterizedType valueType) {
        if (parameterizedType != null) {
            Class<?> actualType = parameterizedType.getGenericClass(valueType.getGenericName());
            valueType = GenericParameterizedType.actualType(actualType);
        }
        return valueType;
    }

    public final T deserialize(String json, ReadOption... options) {
        if (EnvUtils.JDK_9_PLUS) {
            byte coder = UnsafeHelper.getStringCoder(json);
            if (coder == 0) {
                CharSource charSource = AsciiStringSource.of(json);
                return deserialize(charSource, charSource.byteArray(), options);
            } else {
                char[] chars = getChars(json);
                return deserialize(UTF16ByteArraySource.of(json), chars, options);
            }
        }
        return deserialize(getChars(json), options);
    }

    public final T deserialize(char[] buf, ReadOption... options) {
        return deserialize(null, buf, options);
    }

    private T deserialize(CharSource charSource, char[] buf, ReadOption... options) {
        int fromIndex = 0;
        int toIndex = buf.length;
        char beginChar = '\0';
        while ((fromIndex < toIndex) && (beginChar = buf[fromIndex]) <= ' ') {
            fromIndex++;
        }
        while ((toIndex > fromIndex) && buf[toIndex - 1] <= ' ') {
            toIndex--;
        }
        if (beginChar != '{') {
            throw new JSONException("The first non empty character is not '{'");
        }
        JSONParseContext jsonParseContext = new JSONNodeContext();
        JSONOptions.readOptions(options, jsonParseContext);
        T entity;
        try {
            entity = createPojo();
            deserializePojo(charSource, buf, fromIndex, toIndex, getGenericParameterizedType(), entity, '}', jsonParseContext);
        } catch (Throwable throwable) {
            handleCatchException(throwable, buf, toIndex);
            throw new JSONException(throwable.getMessage(), throwable);
        }

        return entity;
    }

    public final T deserialize(byte[] buf, ReadOption... options) {
        return deserialize(null, buf, options);
    }

    private T deserialize(CharSource charSource, byte[] buf, ReadOption... options) {
        int fromIndex = 0;
        int toIndex = buf.length;
        byte beginByte = '\0';
        while ((fromIndex < toIndex) && (beginByte = buf[fromIndex]) <= ' ') {
            fromIndex++;
        }
        while ((toIndex > fromIndex) && buf[toIndex - 1] <= ' ') {
            toIndex--;
        }
        if (beginByte != '{') {
            throw new JSONException("The first non empty character is not '{'");
        }
        JSONParseContext jsonParseContext = new JSONNodeContext();
        JSONOptions.readOptions(options, jsonParseContext);
        T entity;
        try {
            entity = createPojo();
            deserializePojo(charSource, buf, fromIndex, toIndex, getGenericParameterizedType(), entity, END_OBJECT, jsonParseContext);
        } catch (Throwable throwable) {
            handleCatchException(throwable, buf, toIndex);
            throw new JSONException(throwable.getMessage(), throwable);
        }

        return entity;
    }

    protected void setFieldValue(T entity, JSONPojoFieldDeserializer fieldDeserializer, Object value) {
        fieldDeserializer.invoke(entity, value);
    }

    protected T createPojo() throws Exception {
        return (T) pojoStructureWrapper.newInstance();
    }

    protected final GenericParameterizedType getGenericParameterizedType() {
        return pojoStructureWrapper.getGenericType();
    }

    /**
     * Note: Ensure that the hash does not collide, otherwise do not use it
     * call isCollision() to check if collide
     *
     * @param hashValue
     * @return
     */
    protected final JSONPojoFieldDeserializer getFieldDeserializer(long hashValue) {
        return pojoStructureWrapper.getFieldDeserializer(hashValue);
    }

    protected final boolean isCollision() {
        return pojoStructureWrapper.isCollision();
    }

    // record supported
    public static class JSONRecordDeserializer<T> extends JSONPojoDeserializer {

        JSONRecordDeserializer(JSONPojoStructure objectStructureWrapper) {
            super(objectStructureWrapper);
        }

        protected Object createPojo() throws Exception {
            return pojoStructureWrapper.createConstructorArgs();
        }

        protected final void setFieldValue(Object entity, JSONPojoFieldDeserializer fieldDeserializer, Object value) {
            Object[] argValues = (Object[]) entity;
            argValues[fieldDeserializer.getIndex()] = value;
        }

        @Override
        protected JSONPojoFieldDeserializer getFieldDeserializer(char[] buf, int offset, int endIndex, long hashValue) {
            if (!isCollision()) {
                return getFieldDeserializer(hashValue);
            }
            return super.getFieldDeserializer(buf, offset, endIndex, hashValue);
        }

        @Override
        protected JSONPojoFieldDeserializer getFieldDeserializer(byte[] buf, int offset, int endIndex, long hashValue) {
            if (!isCollision()) {
                return getFieldDeserializer(hashValue);
            }
            return super.getFieldDeserializer(buf, offset, endIndex, hashValue);
        }

        protected final Object pojo(Object value) {
            try {
                return pojoStructureWrapper.newInstance((Object[]) value);
            } catch (Exception e) {
                throw new JSONException(e.getMessage(), e);
            }
        }
    }
}
