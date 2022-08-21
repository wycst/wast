package io.github.wycst.wast.json;

import io.github.wycst.wast.common.reflect.GenericParameterizedType;
import io.github.wycst.wast.common.reflect.ReflectConsts;
import io.github.wycst.wast.json.exceptions.JSONException;
import io.github.wycst.wast.json.options.JSONNodeContext;
import io.github.wycst.wast.json.options.JSONParseContext;
import io.github.wycst.wast.json.options.Options;
import io.github.wycst.wast.json.options.ReadOption;
import io.github.wycst.wast.json.reflect.FieldDeserializer;
import io.github.wycst.wast.json.reflect.ObjectStructureWrapper;

/**
 * pojo Deserializer
 *
 * @Author wangyunchao
 * @Date 2022/6/28 13:02
 */
public abstract class JSONPojoDeserializer<T> extends JSONTypeDeserializer {

    private final Class<? extends T> pojoClass;
    protected final ObjectStructureWrapper pojoStructureWrapper;

    public JSONPojoDeserializer(Class<? extends T> pojoClass) {
        pojoClass.getClass();
        ReflectConsts.ClassCategory classCategory = ReflectConsts.getClassCategory(pojoClass);
        if (classCategory != ReflectConsts.ClassCategory.ObjectCategory) {
            throw new UnsupportedOperationException("type not support for " + pojoClass);
        }

        this.pojoClass = pojoClass;
        pojoStructureWrapper = ObjectStructureWrapper.get(pojoClass);
        if (pojoStructureWrapper == null) {
            throw new UnsupportedOperationException("type not support for " + pojoClass);
        }
    }

    protected final Object deserialize(char[] buf, int fromIndex, int toIndex, GenericParameterizedType parameterizedType, Object entity, char endToken, JSONParseContext jsonParseContext) throws Exception {
        char beginChar = buf[fromIndex];
        switch (beginChar) {
            case '{':
                return deserializePojo(buf, fromIndex, toIndex, parameterizedType, entity, endToken, jsonParseContext);
            case 'n':
                return NULL.deserialize(buf, fromIndex, toIndex, null, null, jsonParseContext);
            default: {
                // not support or custom handle ?
                String errorContextTextAt = createErrorContextText(buf, fromIndex);
                throw new JSONException("Syntax error, at pos " + fromIndex + ", context text by '" + errorContextTextAt + "', unexpected token character '" + beginChar + "' for Object Type, expected '{' ");
            }
        }
    }

    protected Object pojo(Object value) {
        return value;
    }

    private final Object deserializePojo(char[] buf, int fromIndex, int toIndex, GenericParameterizedType parameterizedType, Object entity, char endToken, JSONParseContext jsonParseContext) throws Exception {
        if (entity == null) {
            entity = createPojo();
        }
        boolean empty = true;
        char ch;
        boolean allowComment = jsonParseContext.isAllowComment();
        for (int i = fromIndex + 1; /*i < toIndex*/ ; ++i) {
            while ((ch = buf[i]) <= ' ') {
                ++i;
            }
            if (allowComment) {
                if (ch == '/') {
                    ch = buf[i = clearCommentAndWhiteSpaces(buf, i + 1, toIndex, jsonParseContext)];
                }
            }

            int fieldKeyFrom = i, fieldKeyTo;
            boolean isUnquotedFieldName = false;
            int hashValue = 0;
            if (ch == '"') {
                while (/*i + 1 < toIndex &&*/ ((ch = buf[++i]) != '"' || buf[i - 1] == '\\')) {
                    hashValue = hashValue * 31 + ch;
                }
                empty = false;
//                if (ch != '"') {
//                    String errorContextTextAt = createErrorContextText(buf, i);
//                    throw new JSONException("Syntax error, util pos " + i + ", context text by '" + errorContextTextAt + "' the closing symbol '\"' is not found ");
//                }
                ++i;
            } else {
                if (ch == '}') {
                    if (!empty) {
                        String errorContextTextAt = createErrorContextText(buf, i);
                        throw new JSONException("Syntax error, at pos " + i + ", context text by '" + errorContextTextAt + "' the closing symbol '}' is not allowed here.");
                    }
                    jsonParseContext.setEndIndex(i);
                    return pojo(entity);
                }
                if (ch == '\'') {
                    if (jsonParseContext.isAllowSingleQuotes()) {
                        while (i + 1 < toIndex && (ch = buf[++i]) != '\'') {
                            hashValue = hashValue * 31 + ch;
                        }
                        empty = false;
                        ++i;
                    } else {
                        String errorContextTextAt = createErrorContextText(buf, i);
                        throw new JSONException("Syntax error, at pos " + i + ", context text by '" + errorContextTextAt + "' the single quote symbol ' is not allowed here.");
                    }
                } else {
                    if (jsonParseContext.isAllowUnquotedFieldNames()) {
                        // 无引号key处理
                        // 直接锁定冒号（:）位置
                        // 需要处理后置空白（冒号前面的空白字符）
                        while (i + 1 < toIndex && buf[++i] != ':') {
                            if (ch > ' ') {
                                hashValue = hashValue * 31 + ch;
                            }
                        }
                        empty = false;
                        isUnquotedFieldName = true;
                    }
                }
            }

            fieldKeyTo = i;
            while ((ch = buf[i]) <= ' ') {
                ++i;
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

                FieldDeserializer fieldDeserializer = getFieldDeserializer(buf, fieldKeyFrom, fieldKeyTo, isUnquotedFieldName, hashValue);
                boolean isDeserialize = fieldDeserializer != null;
                Object defaultFieldValue = null;
                GenericParameterizedType valueType = null;
                JSONTypeDeserializer deserializer = null;
                if (isDeserialize) {
                    valueType = fieldDeserializer.getGenericParameterizedType();
                    deserializer = fieldDeserializer.getDeserializer();
                    if (deserializer == null) {
                        Class implClass = null;
                        if ((implClass = fieldDeserializer.getImplClass()) != null) {
                            valueType = valueType.copyAndReplaceActualType(implClass);
                            deserializer = getTypeDeserializer(implClass);
                        } else {
                            ch = buf[i];
                            if (ch == '{') {
                                // object
                                // read first field if key is '@c' and value as the implClass
                                String className = parseObjectClassName(buf, i, toIndex, jsonParseContext);
                                if (className == null) {
                                    // use DefaultFieldInstance
                                    if (jsonParseContext.isUseDefaultFieldInstance()) {
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
                                            i = jsonParseContext.getEndIndex();
                                            while ((ch = buf[++i]) <= ' ') ;
                                            Object value = null;
                                            if (ch == ',') {
                                                value = fieldPojoDeserializer.deserializePojo(buf, i, toIndex, valueType, null, '}', jsonParseContext);
                                                i = jsonParseContext.getEndIndex();
                                            } else if (ch == '}') {
                                                value = pojo(fieldPojoDeserializer.createPojo());
                                            } else {
                                                String errorContextTextAt = createErrorContextText(buf, i);
                                                throw new JSONException("Syntax error, at pos " + i + ", context text by '" + errorContextTextAt + "', unexpected token character '" + ch + "', expected ',' or '}'");
                                            }
                                            setFieldValue((T) entity, fieldDeserializer, value);
                                            while ((ch = buf[++i]) <= ' ') ;
                                            if (ch == ',') {
                                                continue;
                                            }
                                            if (ch == '}') {
                                                jsonParseContext.setEndIndex(i);
                                                return pojo(entity);
                                            }
                                            String errorContextTextAt = createErrorContextText(buf, i);
                                            throw new JSONException("Syntax error, at pos " + i + ", context text by '" + errorContextTextAt + "', unexpected token character '" + ch + "', expected ',' or '}'");
                                        } else {
                                            isDeserialize = false;
                                        }
                                    } catch (Throwable throwable) {
                                        throw new JSONException(throwable.getMessage(), throwable);
                                    }
                                }
                            } else {
                                // Non object type
                                // Other interface types use any to resolve and then judge whether the types match
                                Object value = ANY.deserialize(buf, i, toIndex, null, null, '}', jsonParseContext);
                                if (fieldDeserializer.isInstance(value)) {
                                    setFieldValue((T) entity, fieldDeserializer, value);
                                    i = jsonParseContext.getEndIndex();
                                    while ((ch = buf[++i]) <= ' ') ;
                                    if (ch == ',') {
                                        continue;
                                    }
                                    if (ch == '}') {
                                        jsonParseContext.setEndIndex(i);
                                        return pojo(entity);
                                    }
                                    String errorContextTextAt = createErrorContextText(buf, i);
                                    throw new JSONException("Syntax error, at pos " + i + ", context text by '" + errorContextTextAt + "', unexpected token character '" + ch + "', expected ',' or '}'");
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
                    Object value = deserializer.deserialize(buf, i, toIndex, valueType, defaultFieldValue, '}', jsonParseContext);
                    // 设置value值
                    setFieldValue((T) entity, fieldDeserializer, value);
                } else {
                    JSONTypeDeserializer.ANY.skip(buf, i, toIndex, '}', jsonParseContext);
                }
                i = jsonParseContext.getEndIndex();
                while ((ch = buf[++i]) <= ' ') ;
                if (ch == ',') {
                    continue;
                }
                if (ch == '}') {
                    jsonParseContext.setEndIndex(i);
                    return pojo(entity);
                }
                String errorContextTextAt = createErrorContextText(buf, i);
                throw new JSONException("Syntax error, at pos " + i + ", context text by '" + errorContextTextAt + "', unexpected token character '" + ch + "', expected ',' or '}'");
            } else {
                String errorContextTextAt = createErrorContextText(buf, i);
                throw new JSONException("Syntax error, at pos " + i + ", context text by '" + errorContextTextAt + "', unexpected token character '" + ch + "', token character ':' is expected.");
            }
        }

//        throw new JSONException("Syntax error, the closing symbol '}' is not found ");
    }

    /**
     * 根据hash值和偏移位置获取属性的反序列化器
     *
     * @param buf
     * @param from
     * @param to
     * @param isUnquotedFieldName 是否没有双引号
     * @param hashValue
     * @return
     */
    private FieldDeserializer getFieldDeserializer(char[] buf, int from, int to, boolean isUnquotedFieldName, int hashValue) {
        if (isUnquotedFieldName) {
            return getFieldDeserializer(buf, from, to, hashValue);
        }
        return getFieldDeserializer(buf, from + 1, to - 1, hashValue);
    }

    /**
     * 根据hash值和偏移位置获取属性的反序列化器
     *
     * @param buf
     * @param offset
     * @param endIndex
     * @param hashValue
     * @return
     */
    protected FieldDeserializer getFieldDeserializer(char[] buf, int offset, int endIndex, int hashValue) {
        FieldDeserializer fieldDeserializer = pojoStructureWrapper.getFieldDeserializer(buf, offset, endIndex, hashValue);
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
        return deserialize(getChars(json), options);
    }

    public final T deserialize(char[] buf, ReadOption... options) {
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
        Options.readOptions(options, jsonParseContext);
        T entity;
        try {
            entity = createPojo();
            deserializePojo(buf, fromIndex, toIndex, getGenericParameterizedType(), entity, '}', jsonParseContext);
        } catch (Throwable throwable) {
            if (throwable instanceof JSONException) {
                throw (JSONException) throwable;
            }

            // There is only one possibility to control out of bounds exceptions when indexing toindex
            if (throwable instanceof IndexOutOfBoundsException) {
                String errorContextTextAt = createErrorContextText(buf, toIndex);
                throw new JSONException("Syntax error, context text by '" + errorContextTextAt + "', JSON format error, and the end token may be missing, such as '\"' or ', ' or '}' or ']'.");
            }

            throw new JSONException(throwable.getMessage(), throwable);
        }

        return entity;
    }

    protected void setFieldValue(T entity, FieldDeserializer fieldDeserializer, Object value) {
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
    protected final FieldDeserializer getFieldDeserializer(int hashValue) {
        return pojoStructureWrapper.getFieldDeserializer(hashValue);
    }

    protected final boolean isCollision() {
        return pojoStructureWrapper.isCollision();
    }

    // record supported
    public static class JSONRecordDeserializer<T> extends JSONPojoDeserializer {

        public JSONRecordDeserializer(Class<? extends T> recordClass) {
            super(recordClass);
        }

        protected Object createPojo() throws Exception {
            return pojoStructureWrapper.createConstructorArgs();
        }

        protected final void setFieldValue(Object entity, FieldDeserializer fieldDeserializer, Object value) {
            Object[] argValues = (Object[]) entity;
            argValues[fieldDeserializer.getIndex()] = value;
        }

        @Override
        protected FieldDeserializer getFieldDeserializer(char[] buf, int offset, int endIndex, int hashValue) {
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
