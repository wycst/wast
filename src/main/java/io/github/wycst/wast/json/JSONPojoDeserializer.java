package io.github.wycst.wast.json;

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
public class JSONPojoDeserializer<T> extends JSONTypeDeserializer {

    protected final JSONPojoStructure pojoStructure;
    final JSONValueMatcher<JSONPojoFieldDeserializer> fieldDeserializerMatcher;
    final GenericParameterizedType genericType;

    public JSONPojoDeserializer(Class<T> pojoClass) {
        this(checkPojoStructure(pojoClass));
        initialize();
    }
    static <T> JSONPojoStructure checkPojoStructure(Class<T> pojoClass) {
        JSONPojoStructure pojoStructure = JSONPojoStructure.get(pojoClass);
        if (pojoStructure == null) {
            throw new UnsupportedOperationException("not support for " + pojoClass);
        }
        return pojoStructure;
    }

    JSONPojoDeserializer(JSONPojoStructure pojoStructure) {
        this.pojoStructure = pojoStructure;
        this.genericType = pojoStructure.getGenericType();
        this.fieldDeserializerMatcher = pojoStructure.fieldDeserializerMatcher;
    }

    @Override
    final void initialize() {
        pojoStructure.ensureInitializedFieldDeserializers();
    }

    protected final T deserialize(CharSource charSource, char[] buf, int fromIndex, int toIndex, GenericParameterizedType parameterizedType, Object entity, char endToken, JSONParseContext jsonParseContext) throws Exception {
        char beginChar = buf[fromIndex];
        if (beginChar == '{') {
            return (T) deserializePojo(charSource, buf, fromIndex, toIndex, parameterizedType, entity, endToken, jsonParseContext);
        } else if (beginChar == 'n') {
            NULL.deserialize(null, buf, fromIndex, toIndex, null, null, jsonParseContext);
            return null;
        } else {
            if (jsonParseContext.unMatchedEmptyAsNull && beginChar == '"' && buf[fromIndex + 1] == '"') {
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
        if(beginByte == '{') {
            return (T) deserializePojo(charSource, buf, fromIndex, toIndex, parameterizedType, entity, beginByte, jsonParseContext);
        } else if(beginByte == 'n') {
            parseNull(buf, fromIndex, toIndex, jsonParseContext);
            return null;
        } else {
            if (jsonParseContext.unMatchedEmptyAsNull && beginByte == DOUBLE_QUOTATION && buf[fromIndex + 1] == DOUBLE_QUOTATION) {
                jsonParseContext.endIndex = fromIndex + 1;
                return null;
            }
            // not support or custom handle ?
            String errorContextTextAt = createErrorContextText(buf, fromIndex);
            throw new JSONException("Syntax error, at pos " + fromIndex + ", context text by '" + errorContextTextAt + "', unexpected '" + (char) beginByte + "' for Object Type, expected '{' ");
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
        for (; ; ) {
            while ((ch = buf[++i]) <= ' ') ;
            if (allowComment) {
                if (ch == '/') {
                    ch = buf[i = clearCommentAndWhiteSpaces(buf, i + 1, toIndex, jsonParseContext)];
                }
            }
            int fieldKeyFrom = i;
            JSONPojoFieldDeserializer fieldDeserializer;
            if (ch == '"') {
                fieldDeserializer = fieldDeserializerMatcher.matchValue(charSource, buf, i + 1, '"', jsonParseContext);
                i = jsonParseContext.endIndex;
                if (fieldDeserializer == null) {
                    char prev = buf[i - 1];
                    while (prev == '\\') {
                        boolean isPrevEscape = true;
                        int j = i - 1;
                        while (buf[--j] == '\\') {
                            isPrevEscape = !isPrevEscape;
                        }
                        if (isPrevEscape) {
                            // skip
                            while (buf[++i] != '"') ;
                            prev = buf[i - 1];
                        } else {
                            break;
                        }
                    }
                }
                empty = false;
                while ((ch = buf[++i]) <= ' ') ;
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
                        fieldDeserializer = fieldDeserializerMatcher.matchValue(charSource, buf, ++i, '\'', jsonParseContext);
                        i = jsonParseContext.endIndex;
                        if (fieldDeserializer == null) {
                            char prev = buf[i - 1];
                            while (prev == '\\') {
                                boolean isPrevEscape = true;
                                int j = i - 1;
                                while (buf[--j] == '\\') {
                                    isPrevEscape = !isPrevEscape;
                                }
                                if (isPrevEscape) {
                                    // skip
                                    while (buf[++i] != '\'') ;
                                    prev = buf[i - 1];
                                } else {
                                    break;
                                }
                            }
                        }
                        empty = false;
                        while ((ch = buf[++i]) <= ' ') ;
                    } else {
                        String errorContextTextAt = createErrorContextText(buf, i);
                        throw new JSONException("Syntax error, at pos " + i + ", context text by '" + errorContextTextAt + "' the single quote symbol ' is not allowed here.");
                    }
                } else {
                    if (jsonParseContext.allowUnquotedFieldNames) {
                        long hashValue = 0;
                        while (i + 1 < toIndex && (ch = buf[++i]) != ':') {
                            if (ch > ' ') {
                                hashValue = fieldDeserializerMatcher.hash(hashValue, ch);
                            }
                        }
                        fieldDeserializer = fieldDeserializerMatcher.getValue(buf, fieldKeyFrom, i, hashValue);
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
                    valueType = fieldDeserializer.genericParameterizedType;
                    deserializer = fieldDeserializer.deserializer;
                    if (deserializer != null) {
                        boolean camouflage = valueType.isCamouflage();
                        if (camouflage) {
                            valueType = getGenericValueType(parameterizedType, valueType);
                            if (!fieldDeserializer.isCustomDeserialize()) {
                                deserializer = getTypeDeserializer(valueType.getActualType());
                            }
                        }
                    } else {
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
                    }
                }

                if (isDeserialize) {
                    Object value = deserializer.deserialize(charSource, buf, i, toIndex, valueType, defaultFieldValue, '}', jsonParseContext);
                    setFieldValue((T) entity, fieldDeserializer, value);
                } else {
                    JSONTypeDeserializer.ANY.skip(charSource, buf, i, toIndex, '}', jsonParseContext);
                }
                i = jsonParseContext.endIndex;
                int endChar = jsonParseContext.endToken;
                if (endChar == 0) {
                    while ((ch = buf[++i]) <= ' ') ;
                    if (allowComment) {
                        if (ch == '/') {
                            ch = buf[i = clearCommentAndWhiteSpaces(buf, i + 1, toIndex, jsonParseContext)];
                        }
                    }
                } else {
                    ch = (char) endChar;
                    jsonParseContext.endToken = 0;
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
        for (; ; ) {
            while ((b = buf[++i]) <= WHITE_SPACE) ;
            if (allowComment) {
                if (b == '/') {
                    b = buf[i = clearCommentAndWhiteSpaces(buf, i + 1, toIndex, jsonParseContext)];
                }
            }
            int fieldKeyFrom = i;
            JSONPojoFieldDeserializer fieldDeserializer = null;
            if (b == DOUBLE_QUOTATION) {
                fieldDeserializer = fieldDeserializerMatcher.matchValue(charSource, buf, ++i, DOUBLE_QUOTATION, jsonParseContext);
                i = jsonParseContext.endIndex;
                if (fieldDeserializer == null) {
                    byte prev = buf[i - 1];
                    while (prev == ESCAPE) {
                        boolean isPrevEscape = true;
                        int j = i - 1;
                        while (buf[--j] == ESCAPE) {
                            isPrevEscape = !isPrevEscape;
                        }
                        if (isPrevEscape) {
                            // skip
                            while (buf[++i] != DOUBLE_QUOTATION) ;
                            prev = buf[i - 1];
                        } else {
                            break;
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
                        fieldDeserializer = fieldDeserializerMatcher.matchValue(charSource, buf, ++i, '\'', jsonParseContext);
                        i = jsonParseContext.endIndex;
                        if (fieldDeserializer == null) {
                            byte prev = buf[i - 1];
                            while (prev == ESCAPE) {
                                boolean isPrevEscape = true;
                                int j = i - 1;
                                while (buf[--j] == ESCAPE) {
                                    isPrevEscape = !isPrevEscape;
                                }
                                if (isPrevEscape) {
                                    // skip
                                    while (buf[++i] != '\'') ;
                                    prev = buf[i - 1];
                                } else {
                                    break;
                                }
                            }
                        }
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
                                hashValue = fieldDeserializerMatcher.hash(hashValue, b);
                            }
                        }
                        fieldDeserializer = fieldDeserializerMatcher.getValue(buf, fieldKeyFrom, i, hashValue);
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
                    valueType = fieldDeserializer.genericParameterizedType;
                    deserializer = fieldDeserializer.deserializer;
                    if (deserializer != null) {
                        if (valueType.isCamouflage()) {
                            valueType = getGenericValueType(parameterizedType, valueType);
                            if (!fieldDeserializer.isCustomDeserialize()) {
                                deserializer = getTypeDeserializer(valueType.getActualType());
                            }
                        }
                    } else {
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
                                                value = fieldPojoDeserializer.deserializePojo(charSource, buf, i, toIndex, valueType, null, (byte) '}', jsonParseContext);
                                                i = jsonParseContext.endIndex;
                                            } else if (b == '}') {
                                                value = pojo(fieldPojoDeserializer.createPojo());
                                            } else {
                                                String errorContextTextAt = createErrorContextText(buf, i);
                                                throw new JSONException("Syntax error, at pos " + i + ", context text by '" + errorContextTextAt + "', unexpected '" + (char) b + "', expected ',' or '}'");
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
                    }
                }
                if (isDeserialize) {
                    Object value = deserializer.deserialize(charSource, buf, i, toIndex, valueType, defaultFieldValue, END_OBJECT, jsonParseContext);
                    setFieldValue((T) entity, fieldDeserializer, value);
                } else {
                    JSONTypeDeserializer.ANY.skip(charSource, buf, i, toIndex, END_OBJECT, jsonParseContext);
                }
                i = jsonParseContext.endIndex;
                int endChar = jsonParseContext.endToken;
                if (endChar == 0) {
                    while ((b = buf[++i]) <= WHITE_SPACE);
                    if (allowComment) {
                        if (b == '/') {
                            b = buf[i = clearCommentAndWhiteSpaces(buf, i + 1, toIndex, jsonParseContext)];
                        }
                    }
                } else {
                    b = (byte) endChar;
                    jsonParseContext.endToken = 0;
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
            deserializePojo(charSource, buf, fromIndex, toIndex, genericType, entity, '}', jsonParseContext);
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
            deserializePojo(charSource, buf, fromIndex, toIndex, genericType, entity, END_OBJECT, jsonParseContext);
        } catch (Throwable throwable) {
            handleCatchException(throwable, buf, toIndex);
            throw new JSONException(throwable.getMessage(), throwable);
        }

        return entity;
    }

    protected void setFieldValue(T entity, JSONPojoFieldDeserializer fieldDeserializer, Object value) {
        JSON_SECURE_TRUSTED_ACCESS.set(fieldDeserializer.setterInfo, entity, value); // fieldDeserializer.setterInfo.invoke(entity, value);
    }

    protected T createPojo() throws Exception {
        return (T) pojoStructure.newInstance();
    }

    protected final <T> GenericParameterizedType getGenericParameterizedType(Class<T> actualType) {
        return genericType;
    }

    // record supported
    public static class RecordImpl<T> extends JSONPojoDeserializer {

        RecordImpl(JSONPojoStructure pojoStructure) {
            super(pojoStructure);
        }

        protected Object createPojo() throws Exception {
            return pojoStructure.createConstructorArgs();
        }

        protected final void setFieldValue(Object entity, JSONPojoFieldDeserializer fieldDeserializer, Object value) {
            Object[] argValues = (Object[]) entity;
            argValues[fieldDeserializer.fieldIndex] = value;
        }

        protected final Object pojo(Object value) {
            try {
                return pojoStructure.newInstance((Object[]) value);
            } catch (Exception e) {
                throw new JSONException(e.getMessage(), e);
            }
        }
    }
}
