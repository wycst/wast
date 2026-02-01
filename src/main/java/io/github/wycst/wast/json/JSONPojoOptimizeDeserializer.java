package io.github.wycst.wast.json;

import io.github.wycst.wast.common.reflect.GenericParameterizedType;

/**
 * branch optimization, unpacking, and JIT optimization for commonly used entity classes (JDK9+ has a slightly noticeable effect +5%~20%)
 * currently still using reflection, will consider using bytecode for JIT optimization in the future.
 * <p>
 * all fields in POJO are encoded in ASCII.
 * if there are Chinese fields, it will increase the coding complexity and optimization is not currently supported
 *
 * @param <T>
 */
public abstract class JSONPojoOptimizeDeserializer<T> extends JSONPojoDeserializer {

    protected final JSONPojoStructure pojoStructure;
    final JSONKeyValueMap<JSONPojoFieldDeserializer> fieldDeserializerMap;
    final GenericParameterizedType<?> genericType;
    final JSONKeyValueMap.EntryNode<JSONPojoFieldDeserializer>[] valueEntryNodes;
    final int mask;

    JSONPojoOptimizeDeserializer(JSONPojoStructure pojoStructure) {
        this.pojoStructure = pojoStructure;
        this.genericType = pojoStructure.getGenericType();
        this.fieldDeserializerMap = pojoStructure.fieldDeserializerMatcher.valueMapForChars;
        this.valueEntryNodes = fieldDeserializerMap.valueEntryNodes;
        this.mask = fieldDeserializerMap.mask;
    }

    protected final Object createPojo() throws Exception {
        return pojoStructure.newInstance();
    }

    @Override
    protected final Object pojo(Object value) {
        return value;
    }

    @Override
    protected final boolean checkIfSupportedStartsWith(int c) {
        // null
        return c == '{' || c == 'n';
    }

    abstract static class GenericImpl extends JSONPojoOptimizeDeserializer {

        GenericImpl(JSONPojoStructure pojoStructure) {
            super(pojoStructure);
        }

        final JSONPojoFieldDeserializer matchDeserializer(char[] buf, int offset, int endToken, JSONParseContext parseContext) {
            char c;
            if ((c = buf[offset]) != endToken) {
                long hashValue = readPojoFieldHash(c, buf, offset, endToken, parseContext);
                JSONKeyValueMap.EntryNode<JSONPojoFieldDeserializer> entryNode = valueEntryNodes[(int) (hashValue & mask)];
                if (entryNode != null && (!parseContext.strictMode ? entryNode.hash == hashValue : entryNode.equals(buf, offset, parseContext.endIndex - offset))) {
                    return entryNode.value;
                }
                return skipIfNotMatch(buf, endToken, parseContext);
            }
            parseContext.endIndex = offset;
            return null;
        }

        final JSONPojoFieldDeserializer matchDeserializer(byte[] buf, int offset, int endToken, JSONParseContext parseContext) {
            byte c;
            if ((c = buf[offset]) != endToken) {
                long hashValue = readPojoFieldHash(c, buf, offset, endToken, parseContext);
                JSONKeyValueMap.EntryNode<JSONPojoFieldDeserializer> entryNode = valueEntryNodes[(int) (hashValue & mask)];
                if (entryNode != null && (!parseContext.strictMode ? entryNode.hash == hashValue : entryNode.equals(buf, offset, parseContext.endIndex - offset))) {
                    return entryNode.value;
                }
                return skipIfNotMatch(buf, endToken, parseContext);
            }
            parseContext.endIndex = offset;
            return null;
        }
    }

    @Override
    final JSONTypeDeserializer ensureInitialized() {
        pojoStructure.ensureInitializedFieldDeserializers();
        return this;
    }

    protected final T deserialize(CharSource charSource, char[] buf, int fromIndex, GenericParameterizedType<?> parameterizedType, Object entity, int endToken, JSONParseContext parseContext) throws Exception {
        char c = buf[fromIndex];
        if (c == '{') {
            return (T) deserializePojo(charSource, buf, fromIndex, parameterizedType, entity, parseContext);
        } else if (c == 'n') {
            parseNull(buf, fromIndex, parseContext);
            return null;
        } else {
            if (parseContext.unMatchedEmptyAsNull && (c == '"' || c == '\'') && buf[fromIndex + 1] == c) {
                parseContext.endIndex = fromIndex + 1;
                return null;
            }
            // not support or custom handle ?
            return throwUnexpectedException(buf, fromIndex, c, '{');
        }
    }

    protected final T deserialize(CharSource charSource, byte[] buf, int fromIndex, GenericParameterizedType<?> parameterizedType, Object entity, int endToken, JSONParseContext parseContext) throws Exception {
        byte c = buf[fromIndex];
        if (c == '{') {
            return (T) deserializePojo(charSource, buf, fromIndex, parameterizedType, entity, parseContext);
        } else if (c == 'n') {
            parseNull(buf, fromIndex, parseContext);
            return null;
        } else {
            if (parseContext.unMatchedEmptyAsNull && (c == DOUBLE_QUOTATION || c == '\'') && buf[fromIndex + 1] == c) {
                parseContext.endIndex = fromIndex + 1;
                return null;
            }
            // not support or custom handle ?
            return throwUnexpectedException(buf, fromIndex, c, '{');
        }
    }

    long readPojoFieldHash(long initValue, char[] buf, int offset, int endToken, JSONParseContext parseContext) {
        return 0L;
    }

    static final <E> E skipIfNotMatch(char[] buf, int endToken, JSONParseContext parseContext) {
        int offset = parseContext.endIndex;
        char prev = buf[offset - 1];
        while (prev == ESCAPE_BACKSLASH) {
            boolean isPrevEscape = true;
            int j = offset - 1;
            while (buf[--j] == ESCAPE_BACKSLASH) {
                isPrevEscape = !isPrevEscape;
            }
            if (isPrevEscape) {
                while (buf[++offset] != endToken) ;
                prev = buf[offset - 1];
            } else {
                break;
            }
        }
        parseContext.endIndex = offset;
        return null;
    }

    abstract JSONPojoFieldDeserializer matchDeserializer(char[] buf, int offset, int endToken, JSONParseContext parseContext);

    final JSONPojoFieldDeserializer matchUnquoted(char[] buf, int offset, int beginByte, JSONParseContext parseContext) {
        if (parseContext.allowUnquotedFieldNames) {
            final int from = offset;
            long hashValue = beginByte;
            char c;
            while ((c = buf[++offset]) != ':') {
                if (c > ' ') {
                    hashValue = fieldDeserializerMap.hash(hashValue, c);
                }
            }
            parseContext.endIndex = offset - 1;
            return fieldDeserializerMap.getValue(buf, from, offset, hashValue);
        } else {
            throwUnexpectedException(buf, offset, beginByte, '"', '\'');
            return null;
        }
    }

    final Object deserializePojo(CharSource charSource, char[] buf, int fromIndex, GenericParameterizedType<?> parameterizedType, Object entity, JSONParseContext parseContext) throws Exception {
        if (entity == null) {
            entity = pojoStructure.newInstance();
        }
        final boolean allowComment = parseContext.allowComment, allowLastEndComma = parseContext.allowLastEndComma;
        int i = skipWhiteSpacesOrComment(buf, fromIndex, allowComment, parseContext);
        char c = buf[i];
        if (c == END_OBJECT) {
            parseContext.endIndex = i;
            return entity;
        }

        boolean isComma;
        JSONPojoFieldDeserializer fieldDeserializer;
        for (; ; ) {
            fieldDeserializer = c == DOUBLE_QUOTATION || c == '\'' ? matchDeserializer(buf, i + 1, c, parseContext) : matchUnquoted(buf, i, c, parseContext);
            c = buf[i = skipWhiteSpacesOrComment(buf, parseContext.endIndex, allowComment, parseContext)];
            if (c == COLON_SIGN) {
                i = skipWhiteSpacesOrComment(buf, i, allowComment, parseContext);
                if (fieldDeserializer != null) {
                    Object value = fieldDeserializer.deserializer.deserialize(charSource, buf, i, fieldDeserializer.genericParameterizedType, null, '}', parseContext);
                    JSON_SECURE_TRUSTED_ACCESS.set(fieldDeserializer.setterInfo, entity, value);
                } else {
                    JSONTypeDeserializer.ANY.skip(charSource, buf, i, '}', parseContext);
                }
                c = buf[i = skipWhiteSpacesOrComment(buf, parseContext.endIndex, allowComment, parseContext)];
                if (c == END_OBJECT || ((isComma = c == COMMA) && ((c = buf[i = skipWhiteSpacesOrComment(buf, i, allowComment, parseContext)]) == END_OBJECT) && allowLastEndComma)) {
                    parseContext.endIndex = i;
                    return entity;
                }
                if (!isComma) {
                    throwUnexpectedException(buf, i, c, ',', '}');
                }
            } else {
                throwUnexpectedException(buf, i, c, ':');
            }
        }
    }

    long readPojoFieldHash(long initValue, byte[] buf, int offset, int endToken, JSONParseContext parseContext) {
        return 0L;
    }

    static final <E> E skipIfNotMatch(byte[] buf, int endToken, JSONParseContext parseContext) {
        int offset = parseContext.endIndex;
        byte prev = buf[offset - 1];
        while (prev == ESCAPE_BACKSLASH) {
            boolean isPrevEscape = true;
            int j = offset - 1;
            while (buf[--j] == ESCAPE_BACKSLASH) {
                isPrevEscape = !isPrevEscape;
            }
            if (isPrevEscape) {
                while (buf[++offset] != endToken) ;
                prev = buf[offset - 1];
            } else {
                break;
            }
        }
        parseContext.endIndex = offset;
        return null;
    }

    abstract JSONPojoFieldDeserializer matchDeserializer(byte[] buf, int offset, int endToken, JSONParseContext parseContext);

    final JSONPojoFieldDeserializer matchUnquoted(byte[] buf, int offset, int beginByte, JSONParseContext parseContext) {
        if (parseContext.allowUnquotedFieldNames) {
            final int from = offset;
            long hashValue = beginByte;
            byte c;
            while ((c = buf[++offset]) != ':') {
                if (c > ' ') {
                    hashValue = fieldDeserializerMap.hash(hashValue, c);
                }
            }
            parseContext.endIndex = offset - 1;
            return fieldDeserializerMap.getValue(buf, from, offset, hashValue);
        } else {
            throwUnexpectedException(buf, offset, beginByte, '"', '\'');
            return null;
        }
    }

    final Object deserializePojo(CharSource charSource, byte[] buf, int fromIndex, GenericParameterizedType<?> parameterizedType, Object entity, JSONParseContext parseContext) throws Exception {
        if (entity == null) {
            entity = pojoStructure.newInstance();
        }
        final boolean allowComment = parseContext.allowComment, allowLastEndComma = parseContext.allowLastEndComma;
        int i = skipWhiteSpacesOrComment(buf, fromIndex, allowComment, parseContext);
        byte c = buf[i];
        if (c == END_OBJECT) {
            parseContext.endIndex = i;
            return entity;
        }

        boolean isComma;
        JSONPojoFieldDeserializer fieldDeserializer;

        fieldDeserializer = c == DOUBLE_QUOTATION || c == '\'' ? matchDeserializer(buf, i + 1, c, parseContext) : matchUnquoted(buf, i, c, parseContext);
        c = buf[i = skipWhiteSpacesOrComment(buf, parseContext.endIndex, allowComment, parseContext)];
        if (c == COLON_SIGN) {
            i = skipWhiteSpacesOrComment(buf, i, allowComment, parseContext);
            if (fieldDeserializer != null) {
                Object value = fieldDeserializer.deserializer.deserialize(charSource, buf, i, fieldDeserializer.genericParameterizedType, null, END_OBJECT, parseContext);
                JSON_SECURE_TRUSTED_ACCESS.set(fieldDeserializer.setterInfo, entity, value);
            } else {
                JSONTypeDeserializer.ANY.skip(charSource, buf, i, END_OBJECT, parseContext);
            }
            c = buf[i = skipWhiteSpacesOrComment(buf, parseContext.endIndex, allowComment, parseContext)];
            if (c == END_OBJECT || ((isComma = c == COMMA) && ((c = buf[i = skipWhiteSpacesOrComment(buf, i, allowComment, parseContext)]) == END_OBJECT) && allowLastEndComma)) {
                parseContext.endIndex = i;
                return entity;
            }
            if (!isComma) {
                throwUnexpectedException(buf, i, c, ',', '}');
            }
        } else {
            throwUnexpectedException(buf, i, c, ':');
        }

        fieldDeserializer = c == DOUBLE_QUOTATION || c == '\'' ? matchDeserializer(buf, i + 1, c, parseContext) : matchUnquoted(buf, i, c, parseContext);
        c = buf[i = skipWhiteSpacesOrComment(buf, parseContext.endIndex, allowComment, parseContext)];
        if (c == COLON_SIGN) {
            i = skipWhiteSpacesOrComment(buf, i, allowComment, parseContext);
            if (fieldDeserializer != null) {
                Object value = fieldDeserializer.deserializer.deserialize(charSource, buf, i, fieldDeserializer.genericParameterizedType, null, END_OBJECT, parseContext);
                JSON_SECURE_TRUSTED_ACCESS.set(fieldDeserializer.setterInfo, entity, value);
            } else {
                JSONTypeDeserializer.ANY.skip(charSource, buf, i, END_OBJECT, parseContext);
            }
            c = buf[i = skipWhiteSpacesOrComment(buf, parseContext.endIndex, allowComment, parseContext)];
            if (c == END_OBJECT || ((isComma = c == COMMA) && ((c = buf[i = skipWhiteSpacesOrComment(buf, i, allowComment, parseContext)]) == END_OBJECT) && allowLastEndComma)) {
                parseContext.endIndex = i;
                return entity;
            }
            if (!isComma) {
                throwUnexpectedException(buf, i, c, ',', '}');
            }
        } else {
            throwUnexpectedException(buf, i, c, ':');
        }

        fieldDeserializer = c == DOUBLE_QUOTATION || c == '\'' ? matchDeserializer(buf, i + 1, c, parseContext) : matchUnquoted(buf, i, c, parseContext);
        c = buf[i = skipWhiteSpacesOrComment(buf, parseContext.endIndex, allowComment, parseContext)];
        if (c == COLON_SIGN) {
            i = skipWhiteSpacesOrComment(buf, i, allowComment, parseContext);
            if (fieldDeserializer != null) {
                Object value = fieldDeserializer.deserializer.deserialize(charSource, buf, i, fieldDeserializer.genericParameterizedType, null, END_OBJECT, parseContext);
                JSON_SECURE_TRUSTED_ACCESS.set(fieldDeserializer.setterInfo, entity, value);
            } else {
                JSONTypeDeserializer.ANY.skip(charSource, buf, i, END_OBJECT, parseContext);
            }
            c = buf[i = skipWhiteSpacesOrComment(buf, parseContext.endIndex, allowComment, parseContext)];
            if (c == END_OBJECT || ((isComma = c == COMMA) && ((c = buf[i = skipWhiteSpacesOrComment(buf, i, allowComment, parseContext)]) == END_OBJECT) && allowLastEndComma)) {
                parseContext.endIndex = i;
                return entity;
            }
            if (!isComma) {
                throwUnexpectedException(buf, i, c, ',', '}');
            }
        } else {
            throwUnexpectedException(buf, i, c, ':');
        }

        fieldDeserializer = c == DOUBLE_QUOTATION || c == '\'' ? matchDeserializer(buf, i + 1, c, parseContext) : matchUnquoted(buf, i, c, parseContext);
        c = buf[i = skipWhiteSpacesOrComment(buf, parseContext.endIndex, allowComment, parseContext)];
        if (c == COLON_SIGN) {
            i = skipWhiteSpacesOrComment(buf, i, allowComment, parseContext);
            if (fieldDeserializer != null) {
                Object value = fieldDeserializer.deserializer.deserialize(charSource, buf, i, fieldDeserializer.genericParameterizedType, null, END_OBJECT, parseContext);
                JSON_SECURE_TRUSTED_ACCESS.set(fieldDeserializer.setterInfo, entity, value);
            } else {
                JSONTypeDeserializer.ANY.skip(charSource, buf, i, END_OBJECT, parseContext);
            }
            c = buf[i = skipWhiteSpacesOrComment(buf, parseContext.endIndex, allowComment, parseContext)];
            if (c == END_OBJECT || ((isComma = c == COMMA) && ((c = buf[i = skipWhiteSpacesOrComment(buf, i, allowComment, parseContext)]) == END_OBJECT) && allowLastEndComma)) {
                parseContext.endIndex = i;
                return entity;
            }
            if (!isComma) {
                throwUnexpectedException(buf, i, c, ',', '}');
            }
        } else {
            throwUnexpectedException(buf, i, c, ':');
        }

        for (; ; ) {
            fieldDeserializer = c == DOUBLE_QUOTATION || c == '\'' ? matchDeserializer(buf, i + 1, c, parseContext) : matchUnquoted(buf, i, c, parseContext);
            c = buf[i = skipWhiteSpacesOrComment(buf, parseContext.endIndex, allowComment, parseContext)];
            if (c == COLON_SIGN) {
                i = skipWhiteSpacesOrComment(buf, i, allowComment, parseContext);
                if (fieldDeserializer != null) {
                    Object value = fieldDeserializer.deserializer.deserialize(charSource, buf, i, fieldDeserializer.genericParameterizedType, null, END_OBJECT, parseContext);
                    JSON_SECURE_TRUSTED_ACCESS.set(fieldDeserializer.setterInfo, entity, value);
                } else {
                    JSONTypeDeserializer.ANY.skip(charSource, buf, i, END_OBJECT, parseContext);
                }
                c = buf[i = skipWhiteSpacesOrComment(buf, parseContext.endIndex, allowComment, parseContext)];
                if (c == END_OBJECT || ((isComma = c == COMMA) && ((c = buf[i = skipWhiteSpacesOrComment(buf, i, allowComment, parseContext)]) == END_OBJECT) && allowLastEndComma)) {
                    parseContext.endIndex = i;
                    return entity;
                }
                if (!isComma) {
                    throwUnexpectedException(buf, i, c, ',', '}');
                }
            } else {
                throwUnexpectedException(buf, i, c, ':');
            }
        }
    }

    protected final <T> GenericParameterizedType<?> getGenericParameterizedType(Class<T> actualType) {
        return genericType;
    }

    static final int readPojoFieldHashPlhv(int hashValue, byte[] buf, int offset, int endToken, JSONParseContext parseContext) {
        int c, c1;
        if ((c = buf[++offset]) != endToken && (c1 = buf[++offset]) != endToken) {
            hashValue += c + c1;
            if ((c = buf[++offset]) != endToken && (c1 = buf[++offset]) != endToken) {
                hashValue += c + c1;
                while ((c = buf[++offset]) != endToken && (c1 = buf[++offset]) != endToken) {
                    hashValue += c + c1;
                }
            }
        }
        if (c != endToken) {
            hashValue += c;
        }
        parseContext.endIndex = offset;
        return hashValue;
    }

    static final int readPojoFieldHashPlhv(int hashValue, char[] buf, int offset, int endToken, JSONParseContext parseContext) {
        int c, c1;
        if ((c = buf[++offset]) != endToken && (c1 = buf[++offset]) != endToken) {
            hashValue += c + c1;
            if ((c = buf[++offset]) != endToken && (c1 = buf[++offset]) != endToken) {
                hashValue += c + c1;
                while ((c = buf[++offset]) != endToken && (c1 = buf[++offset]) != endToken) {
                    hashValue += c + c1;
                }
            }
        }
        if (c != endToken) {
            hashValue += c;
        }
        parseContext.endIndex = offset;
        return hashValue;
    }

    static JSONPojoOptimizeDeserializer<?> optimize(JSONPojoStructure pojoStructure) {

        if (pojoStructure.fieldDeserializerMatcher.isPlhv()) {
            final JSONKeyValueMap<JSONPojoFieldDeserializer> fieldDeserializerMap = pojoStructure.fieldDeserializerMatcher.valueMapForChars;

            final int size = pojoStructure.fieldDeserializers.size();
            if (size == 1) {
                final JSONKeyValueMap.EntryNode<JSONPojoFieldDeserializer> one = fieldDeserializerMap.first();
                final JSONPojoFieldDeserializer value = one.value;
                final int hv = (int) one.hash;
                return new JSONPojoOptimizeDeserializer(pojoStructure) {
                    JSONPojoFieldDeserializer matchDeserializer(char[] buf, int offset, int endToken, JSONParseContext parseContext) {
                        int c;
                        if ((c = buf[offset]) != endToken) {
                            int hashValue = readPojoFieldHashPlhv(c, buf, offset, endToken, parseContext);
                            if ((!parseContext.strictMode ? hv == hashValue : one.equals(buf, offset, parseContext.endIndex - offset))) {
                                return value;
                            }
                            return skipIfNotMatch(buf, endToken, parseContext);
                        }
                        parseContext.endIndex = offset;
                        return null;
                    }

                    JSONPojoFieldDeserializer matchDeserializer(byte[] buf, int offset, int endToken, JSONParseContext parseContext) {
                        int c;
                        if ((c = buf[offset]) != endToken) {
                            int hashValue = readPojoFieldHashPlhv(c, buf, offset, endToken, parseContext);
                            if ((!parseContext.strictMode ? hv == hashValue : one.equals(buf, offset, parseContext.endIndex - offset))) {
                                return value;
                            }
                            return skipIfNotMatch(buf, endToken, parseContext);
                        }
                        parseContext.endIndex = offset;
                        return null;
                    }
                };
            }

//            else if(size == 2) {
//                final JSONKeyValueMap.EntryNode<JSONPojoFieldDeserializer> two = fieldDeserializerMap.last();
//                return new JSONPojoOptimizeDeserializer(pojoStructure) {
//                    JSONPojoFieldDeserializer matchDeserializer(char[] buf, int offset, int endToken, JSONParseContext parseContext) {
//                        char c;
//                        if ((c = buf[offset]) != endToken) {
//                            long hashValue = readPojoFieldHashPlhv(c, buf, offset, endToken, parseContext);
//                            if(hashValue == one.hash) {
//                                if (!parseContext.strictMode || one.equals(buf, offset, parseContext.endIndex - offset)) {
//                                    return one.value;
//                                }
//                            } else if(hashValue == two.hash) {
//                                if (!parseContext.strictMode || two.equals(buf, offset, parseContext.endIndex - offset)) {
//                                    return two.value;
//                                }
//                            }
//                        }
//                        return skipIfNotMatch(buf, endToken, parseContext);
//                    }
//
//                    JSONPojoFieldDeserializer matchDeserializer(byte[] buf, int offset, int endToken, JSONParseContext parseContext) {
//                        byte c;
//                        if ((c = buf[offset]) != endToken) {
//                            long hashValue = readPojoFieldHashPlhv(c, buf, offset, endToken, parseContext);
//                            if(hashValue == one.hash) {
//                                if (!parseContext.strictMode || one.equals(buf, offset, parseContext.endIndex - offset)) {
//                                    return one.value;
//                                }
//                            } else if(hashValue == two.hash) {
//                                if (!parseContext.strictMode || two.equals(buf, offset, parseContext.endIndex - offset)) {
//                                    return two.value;
//                                }
//                            }
//                        }
//                        return skipIfNotMatch(buf, endToken, parseContext);
//                    }
//                };
//            }

            return new JSONPojoOptimizeDeserializer(pojoStructure) {
                JSONPojoFieldDeserializer matchDeserializer(char[] buf, int offset, int endToken, JSONParseContext parseContext) {
                    int c;
                    if ((c = buf[offset]) != endToken) {
                        int hashValue = readPojoFieldHashPlhv(c, buf, offset, endToken, parseContext);
                        JSONKeyValueMap.EntryNode<JSONPojoFieldDeserializer> entryNode = valueEntryNodes[hashValue & mask];
                        if (entryNode != null && (!parseContext.strictMode ? entryNode.hash == hashValue : entryNode.equals(buf, offset, parseContext.endIndex - offset))) {
                            return entryNode.value;
                        }
                        return skipIfNotMatch(buf, endToken, parseContext);
                    }
                    parseContext.endIndex = offset;
                    return null;
                }

                JSONPojoFieldDeserializer matchDeserializer(byte[] buf, int offset, int endToken, JSONParseContext parseContext) {
                    int c;
                    if ((c = buf[offset]) != endToken) {
                        int hashValue = readPojoFieldHashPlhv(c, buf, offset, endToken, parseContext);
                        JSONKeyValueMap.EntryNode<JSONPojoFieldDeserializer> entryNode = valueEntryNodes[hashValue & mask];
                        if (entryNode != null && (!parseContext.strictMode ? entryNode.hash == hashValue : entryNode.equals(buf, offset, parseContext.endIndex - offset))) {
                            return entryNode.value;
                        }
                        return skipIfNotMatch(buf, endToken, parseContext);
                    }
                    parseContext.endIndex = offset;
                    return null;
                }
            };
        } else if (pojoStructure.fieldDeserializerMatcher.isPrhv()) {
            final long primeValue = pojoStructure.fieldDeserializerMatcher.valueMapForChars.primeValue;
            final long primeSquare = primeValue * primeValue;
            return new GenericImpl(pojoStructure) {
                long readPojoFieldHash(long initValue, byte[] buf, int offset, int endToken, JSONParseContext parseContext) {
                    long hashValue = initValue;
                    int c, c1;
                    if ((c = buf[++offset]) != endToken && (c1 = buf[++offset]) != endToken) {
                        hashValue = hashValue * primeSquare + c * primeValue + c1;
                        if ((c = buf[++offset]) != endToken && (c1 = buf[++offset]) != endToken) {
                            hashValue = hashValue * primeSquare + c * primeValue + c1;
                            while ((c = buf[++offset]) != endToken && (c1 = buf[++offset]) != endToken) {
                                hashValue = hashValue * primeSquare + c * primeValue + c1;
                            }
                        }
                    }
                    if (c != endToken) {
                        hashValue = hashValue * primeValue + c;
                    }
                    parseContext.endIndex = offset;
                    return hashValue;
                }

                long readPojoFieldHash(long initValue, char[] buf, int offset, int endToken, JSONParseContext parseContext) {
                    long hashValue = initValue;
                    int c, c1;
                    if ((c = buf[++offset]) != endToken && (c1 = buf[++offset]) != endToken) {
                        hashValue = hashValue * primeSquare + c * primeValue + c1;
                        if ((c = buf[++offset]) != endToken && (c1 = buf[++offset]) != endToken) {
                            hashValue = hashValue * primeSquare + c * primeValue + c1;
                            while ((c = buf[++offset]) != endToken && (c1 = buf[++offset]) != endToken) {
                                hashValue = hashValue * primeSquare + c * primeValue + c1;
                            }
                        }
                    }
                    if (c != endToken) {
                        hashValue = hashValue * primeValue + c;
                    }
                    parseContext.endIndex = offset;
                    return hashValue;
                }
            };
        } else if (pojoStructure.fieldDeserializerMatcher.isBihv()) {
            final int bits = pojoStructure.fieldDeserializerMatcher.valueMapForChars.getBits();
            final int bitsTwice = bits << 1;
            return new GenericImpl(pojoStructure) {
                long readPojoFieldHash(long initValue, byte[] buf, int offset, int endToken, JSONParseContext parseContext) {
                    long hashValue = initValue;
                    int c, c1;
                    if ((c = buf[++offset]) != endToken && (c1 = buf[++offset]) != endToken) {
                        hashValue = (hashValue << bitsTwice) + (c << bits) + c1;
                        if ((c = buf[++offset]) != endToken && (c1 = buf[++offset]) != endToken) {
                            hashValue = (hashValue << bitsTwice) + (c << bits) + c1;
                            while ((c = buf[++offset]) != endToken && (c1 = buf[++offset]) != endToken) {
                                hashValue = (hashValue << bitsTwice) + (c << bits) + c1;
                            }
                        }
                    }
                    if (c != endToken) {
                        hashValue = (hashValue << bits) + c;
                    }
                    parseContext.endIndex = offset;
                    return hashValue;
                }

                long readPojoFieldHash(long initValue, char[] buf, int offset, int endToken, JSONParseContext parseContext) {
                    long hashValue = initValue;
                    int c, c1;
                    if ((c = buf[++offset]) != endToken && (c1 = buf[++offset]) != endToken) {
                        hashValue = (hashValue << bitsTwice) + (c << bits) + c1;
                        if ((c = buf[++offset]) != endToken && (c1 = buf[++offset]) != endToken) {
                            hashValue = (hashValue << bitsTwice) + (c << bits) + c1;
                            while ((c = buf[++offset]) != endToken && (c1 = buf[++offset]) != endToken) {
                                hashValue = (hashValue << bitsTwice) + (c << bits) + c1;
                            }
                        }
                    }
                    if (c != endToken) {
                        hashValue = (hashValue << bits) + c;
                    }
                    parseContext.endIndex = offset;
                    return hashValue;
                }
            };
        } else {
            // return new JSONPojoOptimizeDeserializer(pojoStructure);
            throw new UnsupportedOperationException("optimize not supported " + pojoStructure.getSourceClass());
        }
    }
}
