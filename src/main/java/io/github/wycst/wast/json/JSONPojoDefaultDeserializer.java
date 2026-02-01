package io.github.wycst.wast.json;

import io.github.wycst.wast.common.reflect.GenericParameterizedType;
import io.github.wycst.wast.json.exceptions.JSONException;

/**
 * pojo Deserializer
 *
 * @Author wangyunchao
 * @Date 2022/6/28 13:02
 */
public class JSONPojoDefaultDeserializer<T> extends JSONPojoDeserializer {

    protected final JSONPojoStructure pojoStructure;
    final JSONValueMatcher<JSONPojoFieldDeserializer> fieldDeserializerMatcher;
    final GenericParameterizedType<?> genericType;

//    public JSONPojoDefaultDeserializer(Class<T> pojoClass) {
//        this(checkPojoStructure(pojoClass));
//        ensureInitialized();
//    }
//
//    static <T> JSONPojoStructure checkPojoStructure(Class<T> pojoClass) {
//        JSONPojoStructure pojoStructure = JSONStore.DEFAULT_STORE.getPojoStruc(pojoClass);
//        if (pojoStructure == null) {
//            throw new UnsupportedOperationException("not support for " + pojoClass);
//        }
//        return pojoStructure;
//    }

    @Override
    protected final boolean checkIfSupportedStartsWith(int c) {
        // null
        return c == '{' || c == 'n';
    }

    JSONPojoDefaultDeserializer(JSONPojoStructure pojoStructure) {
        this.pojoStructure = pojoStructure;
        this.genericType = pojoStructure.getGenericType();
        this.fieldDeserializerMatcher = pojoStructure.fieldDeserializerMatcher;
    }

    @Override
    final JSONTypeDeserializer ensureInitialized() {
        pojoStructure.ensureInitializedFieldDeserializers();
        return this;
    }

    protected final T deserialize(CharSource charSource, char[] buf, int fromIndex, GenericParameterizedType<?> parameterizedType, Object entity, int endToken, JSONParseContext jsonParseContext) throws Exception {
        char beginChar = buf[fromIndex];
        if (beginChar == '{') {
            return (T) deserializePojo(charSource, buf, fromIndex, parameterizedType, entity, jsonParseContext);
        } else if (beginChar == 'n') {
            parseNull(buf, fromIndex, jsonParseContext);
            return null;
        } else {
            if (jsonParseContext.unMatchedEmptyAsNull && (beginChar == '"' || beginChar == '\'') && buf[fromIndex + 1] == beginChar) {
                jsonParseContext.endIndex = fromIndex + 1;
                return null;
            }
            // not support or custom handle ?
            String errorContextTextAt = createErrorContextText(buf, fromIndex);
            throw new JSONException("Syntax error, at pos " + fromIndex + ", context text by '" + errorContextTextAt + "', unexpected '" + beginChar + "' , expected '{' ");
        }
    }

    protected final T deserialize(CharSource charSource, byte[] buf, int fromIndex, GenericParameterizedType<?> parameterizedType, Object entity, int endToken, JSONParseContext jsonParseContext) throws Exception {
        byte beginByte = buf[fromIndex];
        if (beginByte == '{') {
            return (T) deserializePojo(charSource, buf, fromIndex, parameterizedType, entity, jsonParseContext);
        } else if (beginByte == 'n') {
            parseNull(buf, fromIndex, jsonParseContext);
            return null;
        } else {
            if (jsonParseContext.unMatchedEmptyAsNull && (beginByte == DOUBLE_QUOTATION || beginByte == '\'') && buf[fromIndex + 1] == beginByte) {
                jsonParseContext.endIndex = fromIndex + 1;
                return null;
            }
            // not support or custom handle ?
            String errorContextTextAt = createErrorContextText(buf, fromIndex);
            throw new JSONException("Syntax error, at pos " + fromIndex + ", context text by '" + errorContextTextAt + "', unexpected '" + (char) beginByte + "' , expected '{' ");
        }
    }

    protected Object pojo(Object value) {
        return value;
    }

    JSONPojoFieldDeserializer matchFieldDeserializer(CharSource charSource, char[] buf, int offset, int endToken, JSONParseContext parseContext) {
        return fieldDeserializerMatcher.matchValue(charSource, buf, offset, endToken, parseContext);
    }

    final Object deserializePojo(CharSource charSource, char[] buf, int fromIndex, GenericParameterizedType<?> parameterizedType, Object entity, JSONParseContext parseContext) throws Exception {
        if (entity == null) {
            entity = createPojo();
        }
        boolean empty = true;
        char c;
        final boolean allowComment = parseContext.allowComment;
        int i = fromIndex;
        for (; ; ) {
            if ((c = buf[++i]) <= ' ') {
                c = buf[i = skipWhiteSpaces(buf, i + 1)];
            }
            if (allowComment) {
                if (c == '/') {
                    c = buf[i = clearCommentAndWhiteSpaces(buf, i + 1, parseContext)];
                }
            }
            JSONPojoFieldDeserializer fieldDeserializer;
            if (c == '"' || c == '\'') {
                fieldDeserializer = matchFieldDeserializer(charSource, buf, i + 1, c, parseContext); // fieldDeserializerMatcher.matchValue(charSource, buf, i + 1, c, parseContext);
                i = parseContext.endIndex;
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
                            while (buf[++i] != c) ;
                            prev = buf[i - 1];
                        } else {
                            break;
                        }
                    }
                }
                empty = false;
                while ((c = buf[++i]) <= ' ') ;
            } else {
                if (c == '}') {
                    if (!empty && !parseContext.allowLastEndComma) {
                        String errorContextTextAt = createErrorContextText(buf, i);
                        throw new JSONException("Syntax error, at pos " + i + ", context text by '" + errorContextTextAt + "' the closing symbol '}' is not allowed here.");
                    }
                    parseContext.endIndex = i;
                    return pojo(entity);
                }
                if (parseContext.allowUnquotedFieldNames) {
                    final int from = i;
                    long hashValue = 0;
                    while ((c = buf[++i]) != ':') {
                        if (c > ' ') {
                            hashValue = fieldDeserializerMatcher.hash(hashValue, c);
                        }
                    }
                    fieldDeserializer = fieldDeserializerMatcher.getValue(buf, from, i, hashValue);
                    empty = false;
                } else {
                    return throwUnexpectedException(buf, i, c, '"', '\'');
                }
            }
            if (allowComment) {
                if (c == '/') {
                    c = buf[i = clearCommentAndWhiteSpaces(buf, i + 1, parseContext)];
                }
            }
            if (c == ':') {
                if (buf[++i] <= ' ') {
                    i = skipWhiteSpaces(buf, i + 1);
                }
                if (allowComment) {
                    c = buf[i];
                    if (c == '/') {
                        i = clearCommentAndWhiteSpaces(buf, i + 1, parseContext);
                    }
                }
                boolean isDeserialize = fieldDeserializer != null;
                Object defaultFieldValue = null;
                GenericParameterizedType<?> valueType = null;
                JSONTypeDeserializer deserializer = null;
                if (isDeserialize) {
                    valueType = fieldDeserializer.genericParameterizedType;
                    deserializer = fieldDeserializer.deserializer;
                    if (deserializer != null) {
                        boolean camouflage = valueType.isCamouflage();
                        if (camouflage) {
                            valueType = getGenericValueType(parameterizedType, valueType);
                            if (!fieldDeserializer.isCustomDeserialize()) {
                                deserializer = pojoStructure.store.getTypeDeserializer(valueType.getActualType());
                            }
                        }
                    } else {
                        Class implClass;
                        if ((implClass = fieldDeserializer.getImplClass()) != null) {
                            valueType = valueType.copyAndReplaceActualType(implClass);
                            deserializer = pojoStructure.store.getTypeDeserializer(implClass);
                        } else {
                            c = buf[i];
                            if (c == '{') {
                                // object
                                // read first field if key is '@c' and value as the implClass
                                String className = parseObjectClassName(charSource, buf, i, parseContext);
                                if (className == null) {
                                    // use DefaultFieldInstance
                                    if (parseContext.useDefaultFieldInstance) {
                                        defaultFieldValue = fieldDeserializer.getDefaultFieldValue(entity);
                                        if (defaultFieldValue != null) {
                                            implClass = defaultFieldValue.getClass();
                                            valueType = valueType.copyAndReplaceActualType(implClass);
                                            deserializer = pojoStructure.store.getTypeDeserializer(implClass);
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
                                            deserializer = pojoStructure.store.getTypeDeserializer(cls);
                                            JSONPojoDeserializer fieldPojoDeserializer = (JSONPojoDeserializer) deserializer;
                                            i = parseContext.endIndex;
                                            while ((c = buf[++i]) <= ' ') ;
                                            Object value;
                                            if (c == ',') {
                                                value = fieldPojoDeserializer.deserializePojo(charSource, buf, i, valueType, null, parseContext);
                                                i = parseContext.endIndex;
                                            } else if (c == '}') {
                                                value = pojo(fieldPojoDeserializer.createPojo());
                                            } else {
                                                return throwUnexpectedException(buf, i, c, ',', '}');
                                            }
                                            setFieldValue((T) entity, fieldDeserializer, value);
                                            while ((c = buf[++i]) <= ' ') ;
                                            if (c == ',') {
                                                continue;
                                            }
                                            if (c == '}') {
                                                parseContext.endIndex = i;
                                                return pojo(entity);
                                            }
                                            throwUnexpectedException(buf, i, c, ',', '}');
                                        } else {
                                            isDeserialize = false;
                                        }
                                    } catch (Throwable throwable) {
                                        throw new JSONException(throwable.getMessage(), throwable);
                                    }
                                }
                            } else {
                                Object value = ANY.deserialize(charSource, buf, i, null, null, '}', parseContext);
                                if (fieldDeserializer.isInstance(value)) {
                                    setFieldValue((T) entity, fieldDeserializer, value);
                                    i = parseContext.endIndex;
                                    while ((c = buf[++i]) <= ' ') ;
                                    if (c == ',') {
                                        continue;
                                    }
                                    if (c == '}') {
                                        parseContext.endIndex = i;
                                        return pojo(entity);
                                    }
                                    throwUnexpectedException(buf, i, c, ',', '}');
                                }
                            }
                        }
                    }
                }

                if (isDeserialize) {
                    Object value = deserializer.deserialize(charSource, buf, i, valueType, defaultFieldValue, '}', parseContext);
                    setFieldValue((T) entity, fieldDeserializer, value);
                } else {
                    JSONTypeDeserializer.ANY.skip(charSource, buf, i, '}', parseContext);
                }
                i = parseContext.endIndex;
                while ((c = buf[++i]) <= ' ') ;
                if (allowComment) {
                    if (c == '/') {
                        c = buf[i = clearCommentAndWhiteSpaces(buf, i + 1, parseContext)];
                    }
                }
                if (c == ',') {
                    continue;
                }
                if (c == '}') {
                    parseContext.endIndex = i;
                    return pojo(entity);
                }
                throwUnexpectedException(buf, i, c, ',', '}');
            } else {
                throwUnexpectedException(buf, i, c, ':');
            }
        }
    }

    JSONPojoFieldDeserializer matchFieldDeserializer(CharSource charSource, byte[] buf, int offset, int endToken, JSONParseContext parseContext) {
        return fieldDeserializerMatcher.matchValue(charSource, buf, offset, endToken, parseContext);
    }

    final Object deserializePojo(CharSource charSource, byte[] buf, int fromIndex, GenericParameterizedType<?> parameterizedType, Object entity, JSONParseContext parseContext) throws Exception {
        if (entity == null) {
            entity = createPojo();
        }
        boolean empty = true;
        byte c;
        final boolean allowComment = parseContext.allowComment;
        int i = fromIndex;
        for (; ; ) {
            c = buf[i = skipWhiteSpacesOrComment(buf, i, allowComment, parseContext)];
            JSONPojoFieldDeserializer fieldDeserializer;
            if (c == DOUBLE_QUOTATION || c == '\'') {
//                fieldDeserializer = fieldDeserializerMatcher.matchValue(charSource, buf, i + 1, c, parseContext);
                fieldDeserializer = matchFieldDeserializer(charSource, buf, i + 1, c, parseContext);
                i = parseContext.endIndex;
                if (fieldDeserializer == null) {
                    byte prev = buf[i - 1];
                    while (prev == ESCAPE_BACKSLASH) {
                        boolean isPrevEscape = true;
                        int j = i - 1;
                        while (buf[--j] == ESCAPE_BACKSLASH) {
                            isPrevEscape = !isPrevEscape;
                        }
                        if (isPrevEscape) {
                            // skip
                            while (buf[++i] != c) ;
                            prev = buf[i - 1];
                        } else {
                            break;
                        }
                    }
                }
                empty = false;
            } else {
                if (c == END_OBJECT) {
                    if (!empty && !parseContext.allowLastEndComma) {
                        String errorContextTextAt = createErrorContextText(buf, i);
                        throw new JSONException("Syntax error, at pos " + i + ", context text by '" + errorContextTextAt + "' the closing symbol '}' is not allowed here.");
                    }
                    parseContext.endIndex = i;
                    return pojo(entity);
                }
                if (parseContext.allowUnquotedFieldNames) {
                    final int from = i;
                    long hashValue = c;
                    while ((c = buf[++i]) != ':') {
                        if (c > ' ') {
                            hashValue = fieldDeserializerMatcher.hash(hashValue, c);
                        }
                    }
                    fieldDeserializer = fieldDeserializerMatcher.getValue(buf, from, i, hashValue);
                    empty = false;
                    --i;
                } else {
                    return throwUnexpectedException(buf, i, c, '"', '\'');
                }
            }
            c = buf[i = skipWhiteSpacesOrComment(buf, i, allowComment, parseContext)];
            if (c == COLON_SIGN) {
                i = skipWhiteSpacesOrComment(buf, i, allowComment, parseContext);
                boolean isDeserialize = fieldDeserializer != null;
                Object defaultFieldValue = null;
                GenericParameterizedType<?> valueType = null;
                JSONTypeDeserializer deserializer = null;
                if (isDeserialize) {
                    valueType = fieldDeserializer.genericParameterizedType;
                    deserializer = fieldDeserializer.deserializer;
                    if (deserializer != null) {
                        if (valueType.isCamouflage()) {
                            valueType = getGenericValueType(parameterizedType, valueType);
                            if (!fieldDeserializer.isCustomDeserialize()) {
                                deserializer = pojoStructure.store.getTypeDeserializer(valueType.getActualType());
                            }
                        }
                    } else {
                        Class implClass;
                        if ((implClass = fieldDeserializer.getImplClass()) != null) {
                            valueType = valueType.copyAndReplaceActualType(implClass);
                            deserializer = pojoStructure.store.getTypeDeserializer(implClass);
                        } else {
                            c = buf[i];
                            if (c == '{') {
                                // object
                                // read first field if key is '@c' and value as the implClass
                                String className = parseObjectClassName(charSource, buf, i, parseContext);
                                if (className == null) {
                                    // use DefaultFieldInstance
                                    if (parseContext.useDefaultFieldInstance) {
                                        defaultFieldValue = fieldDeserializer.getDefaultFieldValue(entity);
                                        if (defaultFieldValue != null) {
                                            implClass = defaultFieldValue.getClass();
                                            valueType = valueType.copyAndReplaceActualType(implClass);
                                            deserializer = pojoStructure.store.getTypeDeserializer(implClass);
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
                                            deserializer = pojoStructure.store.getTypeDeserializer(cls);
                                            JSONPojoDeserializer fieldPojoDeserializer = (JSONPojoDeserializer) deserializer;
                                            i = parseContext.endIndex;
                                            while ((c = buf[++i]) <= ' ') ;
                                            Object value;
                                            if (c == ',') {
                                                value = fieldPojoDeserializer.deserializePojo(charSource, buf, i, valueType, null, parseContext);
                                                i = parseContext.endIndex;
                                            } else if (c == '}') {
                                                value = pojo(fieldPojoDeserializer.createPojo());
                                            } else {
                                                return throwUnexpectedException(buf, i, c, ',', '}');
                                            }
                                            setFieldValue((T) entity, fieldDeserializer, value);
                                            while ((c = buf[++i]) <= ' ') ;
                                            if (c == ',') {
                                                continue;
                                            }
                                            if (c == '}') {
                                                parseContext.endIndex = i;
                                                return pojo(entity);
                                            }
                                            throwUnexpectedException(buf, i, c, ',', '}');
                                        } else {
                                            isDeserialize = false;
                                        }
                                    } catch (Throwable throwable) {
                                        throw new JSONException(throwable.getMessage(), throwable);
                                    }
                                }
                            } else {
                                Object value = ANY.deserialize(charSource, buf, i, null, null, (byte) '}', parseContext);
                                if (fieldDeserializer.isInstance(value)) {
                                    setFieldValue((T) entity, fieldDeserializer, value);
                                    i = parseContext.endIndex;
                                    while ((c = buf[++i]) <= ' ') ;
                                    if (c == ',') {
                                        continue;
                                    }
                                    if (c == '}') {
                                        parseContext.endIndex = i;
                                        return pojo(entity);
                                    }
                                    throwUnexpectedException(buf, i, c, ',', '}');
                                }
                            }
                        }
                    }
                }
                if (isDeserialize) {
                    Object value = deserializer.deserialize(charSource, buf, i, valueType, defaultFieldValue, END_OBJECT, parseContext);
                    setFieldValue((T) entity, fieldDeserializer, value);
                } else {
                    JSONTypeDeserializer.ANY.skip(charSource, buf, i, END_OBJECT, parseContext);
                }
                c = buf[i = skipWhiteSpacesOrComment(buf, parseContext.endIndex, allowComment, parseContext)];
                if (c == COMMA) {
                    continue;
                }
                if (c == END_OBJECT) {
                    parseContext.endIndex = i;
                    return pojo(entity);
                }
                throwUnexpectedException(buf, i, c, ',', '}');
            } else {
                throwUnexpectedException(buf, i, c, ':');
            }
        }
    }

    private GenericParameterizedType<?> getGenericValueType(GenericParameterizedType<?> parameterizedType, GenericParameterizedType<?> valueType) {
        if (parameterizedType != null) {
            Class<?> actualType = parameterizedType.getGenericClass(valueType.getGenericName());
            valueType = GenericParameterizedType.actualType(actualType);
        }
        return valueType;
    }

    static JSONPojoDefaultDeserializer create(JSONPojoStructure pojoStructure) {
        if (pojoStructure.fieldDeserializerMatcher.isPlhv()) {
            final JSONKeyValueMap<JSONPojoFieldDeserializer> valueMap = pojoStructure.fieldDeserializerMatcher.valueMapForChars;
            final JSONKeyValueMap.EntryNode<JSONPojoFieldDeserializer>[] valueEntryNodes = valueMap.valueEntryNodes;
            final int mask = valueMap.mask;
            return new JSONPojoDefaultDeserializer(pojoStructure) {
                @Override
                JSONPojoFieldDeserializer matchFieldDeserializer(CharSource charSource, byte[] buf, int offset, int endToken, JSONParseContext parseContext) {
                    byte c;
                    int i = offset;
                    if ((c = buf[i]) != endToken) {
                        long hashValue = c;
                        byte c1;
                        if ((c = buf[++i]) != endToken && (c1 = buf[++i]) != endToken) {
                            hashValue += c + c1;
                            if ((c = buf[++i]) != endToken && (c1 = buf[++i]) != endToken) {
                                hashValue += c + c1;
                                while ((c = buf[++i]) != endToken && (c1 = buf[++i]) != endToken) {
                                    hashValue += c + c1;
                                }
                            }
                        }
                        if (c != endToken) {
                            hashValue += c;
                        }
                        parseContext.endIndex = i;
                        if (!parseContext.strictMode) {
                            JSONKeyValueMap.EntryNode<JSONPojoFieldDeserializer> entryNode = valueEntryNodes[(int) (hashValue & mask)];
                            if (entryNode != null && entryNode.hash == hashValue) {
                                return entryNode.value;
                            }
                        } else {
                            return valueMap.getValue(buf, offset, i, hashValue);
                        }
                    }
                    parseContext.endIndex = i;
                    return null;
                }

                @Override
                JSONPojoFieldDeserializer matchFieldDeserializer(CharSource charSource, char[] buf, int offset, int endToken, JSONParseContext parseContext) {
                    char c;
                    int i = offset;
                    if ((c = buf[i]) != endToken) {
                        long hashValue = c;
                        char c1;
                        if ((c = buf[++i]) != endToken && (c1 = buf[++i]) != endToken) {
                            hashValue += c + c1;
                            if ((c = buf[++i]) != endToken && (c1 = buf[++i]) != endToken) {
                                hashValue += c + c1;
                                while ((c = buf[++i]) != endToken && (c1 = buf[++i]) != endToken) {
                                    hashValue += c + c1;
                                }
                            }
                        }
                        if (c != endToken) {
                            hashValue += c;
                        }
                        parseContext.endIndex = i;
                        if (!parseContext.strictMode) {
                            JSONKeyValueMap.EntryNode<JSONPojoFieldDeserializer> entryNode = valueEntryNodes[(int) (hashValue & mask)];
                            if (entryNode != null && entryNode.hash == hashValue) {
                                return entryNode.value;
                            }
                        } else {
                            return valueMap.getValue(buf, offset, i, hashValue);
                        }
                    }
                    parseContext.endIndex = i;
                    return null;
                }
            };
        } else {
            return new JSONPojoDefaultDeserializer(pojoStructure);
        }
    }

    protected void setFieldValue(T entity, JSONPojoFieldDeserializer fieldDeserializer, Object value) {
        JSON_SECURE_TRUSTED_ACCESS.set(fieldDeserializer.setterInfo, entity, value); // fieldDeserializer.setterInfo.invoke(entity, value);
    }

    protected Object createPojo() throws Exception {
        return pojoStructure.newInstance();
    }

    protected final <E> GenericParameterizedType<?> getGenericParameterizedType(Class<E> actualType) {
        return genericType;
    }

    // record supported
    public static class RecordImpl<T> extends JSONPojoDefaultDeserializer {

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
