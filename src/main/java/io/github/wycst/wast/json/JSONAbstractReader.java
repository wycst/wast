/*
 * Copyright [2020-2026] [wangyunchao]
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0

 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */
package io.github.wycst.wast.json;

import io.github.wycst.wast.common.reflect.ClassStrucWrap;
import io.github.wycst.wast.common.reflect.GenericParameterizedType;
import io.github.wycst.wast.common.reflect.ReflectConsts;
import io.github.wycst.wast.common.reflect.SetterInfo;
import io.github.wycst.wast.common.utils.CollectionUtils;
import io.github.wycst.wast.common.utils.NumberUtils;
import io.github.wycst.wast.common.utils.ObjectUtils;
import io.github.wycst.wast.json.annotations.JsonProperty;
import io.github.wycst.wast.json.exceptions.JSONException;
import io.github.wycst.wast.json.options.ReadOption;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * @author wangyunchao
 * @see JSONReaderHook
 * @see JSON
 * @see JSONNode
 * @see JSONCharArrayWriter
 */
abstract class JSONAbstractReader extends JSONGeneral {

    /**
     * Current pointer position in the entire stream (absolute position)
     */
    int pos;

    /**
     * capacity
     */
    int bufferSize = DIRECT_READ_BUFFER_SIZE;

    /**
     * The actual readable length of the buffer character array
     */
    int count;

    /**
     * current read offset
     */
    int offset;

    /**
     * current character
     */
    int current;
    JSONReaderHook readerHook;
    final JSONParseContext parseContext = new JSONParseContext();
    final StringBuilder writer = new StringBuilder();
    ReadOption[] readOptions = new ReadOption[0];
    Object result;
    boolean reading;
    boolean closed;
    boolean aborted;
    boolean completed;
    int readingOffset = -1;
    volatile CountDownLatch countDownLatch = new CountDownLatch(1);
    boolean async;
    long timeout = 60000;
    long currentThreadId;
    boolean multiple;

    public void setOptions(ReadOption... readOptions) {
        JSONOptions.readOptions(this.readOptions = readOptions, parseContext);
    }

    public void setTimeout(long timeout) {
        this.timeout = timeout;
    }

    /**
     * Does it support reading multiple JSON
     *
     * @param multiple
     */
    public void setMultiple(boolean multiple) {
        this.multiple = multiple;
    }

    public abstract Object read();

    public void read(boolean async) {
        read(null, async);
    }

    public <T> T readAsResult(Class<T> actualType) {
        return readAsResult(GenericParameterizedType.actualType(actualType));
    }

    public <T> T readAsResult(GenericParameterizedType<T> genericType) {
        this.executeReadStream(genericType);
        return (T) result;
    }

    public void read(JSONReaderHook readerHook) {
        read(readerHook, false);
    }

    /**
     * @param readerHook
     * @param async
     */
    public void read(JSONReaderHook readerHook, boolean async) {
        if (reading) return;
        checkReadState();
        this.readerHook = readerHook;
        this.reading = true;
        if (!async) {
            // sync
            this.executeReadStream(null);
        } else {
            // async
            this.async = true;
            this.currentThreadId = Thread.currentThread().getId();
            new Thread(new Runnable() {
                public void run() {
                    executeReadStream(null);
                }
            }).start();
        }
    }

    /**
     * Skip Next JSON
     */
    public final void skipNext() {
        try {
            if (current == 0) {
                readBuffer();
            }
            clearWhitespaces();
            switch (current) {
                case '{':
                    skipObject();
                    break;
                case '[':
                    skipArray();
                    break;
                case -1: {
                    this.complete();
                    break;
                }
                default:
                    throw new JSONException("character stream start character error for '" + (char) current + "'. the start token is only supported  '{' or '[' on multiple mode");
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Skip the number of JSONs
     *
     * @param count
     */
    public final void skipNext(int count) {
        while (count-- > 0) {
            skipNext();
        }
    }

    private void complete() {
        this.result = null;
        this.completed = true;
    }

    private void checkReadState() {
        if (this.closed || this.aborted) {
            throw new UnsupportedOperationException("reader is closed or aborted");
        }
    }

    private void executeReadStream(GenericParameterizedType<?> genericType) {
        try {
            this.readBuffer();
            this.beginReadWithType(genericType);
        } catch (Exception e) {
            throw new JSONException(e);
        } finally {
            tryCloseReader();
            this.reading = false;
            this.closed = true;
            unlock();
        }
    }

    abstract void readBuffer() throws IOException;

    private void unlock() {
        countDownLatch.countDown();
    }

    final void defaultRead() throws Exception {
        clearWhitespaces();
        switch (current) {
            case '{':
                this.result = this.readObject();
                break;
            case '[':
                this.result = this.readArray();
                break;
            case -1: {
                this.complete();
                return;
            }
            default:
                throw new JSONException("character stream start character error for '" + (char) current + "'. the start token is only supported  '{' or '[' on multiple mode");
        }
        // clear white space characters
        if (!multiple) {
            clearWhitespaces();
            if (current > -1) {
                throw new JSONException("Syntax error, extra characters found, '" + (char) current + "', pos " + pos);
            }
        }
    }

    private void beginReadWithType(GenericParameterizedType genericType) throws Exception {
        clearWhitespaces();
        switch (current) {
            case '{':
                this.result = this.readObject("", genericType);
                break;
            case '[':
                this.result = this.readArray("", genericType);
                break;
            case -1: {
                this.complete();
                return;
            }
            default:
                throw new UnsupportedOperationException("Character stream start character error. Only object({) or array([) parsing is supported");
        }
        // clear white space characters
        if (isAborted()) return;
        if (!multiple) {
            clearWhitespaces();
            if (current > -1) {
                throw new JSONException("Syntax error, extra characters found, '" + (char) current + "', pos " + pos);
            }
        }
        if (readerHook != null) {
            readerHook.onCompleted(result);
        }
    }

    private Object readObject() throws Exception {
        Map instance = new LinkedHashMap();
        for (; ; ) {
            clearWhitespaces();
            if (current == '}') {
                // always supported last ','
                return instance;
            }
            String key;
            if (current == '"' || current == '\'') {
                final int token = current;
                // find next "
                this.beginReading(0);
                // todo The reading logic is not rigorous enough and needs to be escaped
                while (readNext() > -1 && current != token) ;
                key = endReadingAsString(-1);
                clearWhitespaces();
                if (current == ':') {
                    clearWhitespaces();
                    switch (current) {
                        case '{':
                            instance.put(key, readObject());
                            break;
                        case '[':
                            instance.put(key, readArray());
                            break;
                        case '"':
                            instance.put(key, readString());
                            break;
                        case 'n':
                            this.readNull();
                            instance.put(key, null);
                            break;
                        case 't':
                            this.readTrue();
                            instance.put(key, true);
                            break;
                        case 'f':
                            this.readFalse();
                            instance.put(key, false);
                            break;
                        default:
                            instance.put(key, this.readNumber('}'));
                            if (current == '}') {
                                return instance;
                            } else {
                                continue;
                            }
                    }
                    clearWhitespaces();
                    if (current == ',') {
                        continue;
                    }
                    if (current == '}') {
                        return instance;
                    }
                    if (current == -1) {
                        throw new JSONException("Syntax error, the closing symbol '}' is not found, end pos: " + pos);
                    }
                    throwUnexpectedException();
                } else {
                    throwUnexpectedException();
                }
            } else {
                throwUnexpectedException();
            }
        }
    }

    private void readTrue() throws Exception {
        // true
        if (readNext(true) == 'r'
                && readNext(true) == 'u'
                && readNext(true) == 'e') {
            return;
        }
        throwUnexpectedException();
    }

    private void readFalse() throws Exception {
        // false
        if (readNext(true) == 'a'
                && readNext(true) == 'l'
                && readNext(true) == 's'
                && readNext(true) == 'e') {
            return;
        }
        throwUnexpectedException();
    }

    private void readNull() throws Exception {
        if (readNext(true) == 'u'
                && readNext(true) == 'l'
                && readNext(true) == 'l') {
            return;
        }
        throwUnexpectedException();
    }

    private Number readNumber(char endSyntax) throws Exception {
        final boolean useBigDecimal = parseContext.useBigDecimalAsDefault;
        if (useBigDecimal) {
            writer.setLength(0);
            writer.append((char) current);
            int ch;
            while ((ch = readNext()) != ',' && ch != endSyntax) {
                writer.append((char) ch);
            }
            String text = writer.toString();
            try {
                return new BigDecimal(text.trim());
            } catch (NumberFormatException numberFormatException) {
                throw new NumberFormatException("offset " + offset + ", error input " + text);
            }
        }
        boolean negative = false;
        char beginChar = (char) current;
        if (beginChar == '-') {
            // is negative
            negative = true;
            readNext();
        } else if (beginChar == '+') {
            readNext();
        }
        long value = 0;
        int decimalCount = 0;
        int expValue = 0;
        boolean expNegative = false;
        int mode = 0;
        int specifySuffix = 0;
        do {
            while (NumberUtils.isDigit(current)) {
                value = (value << 3) + (value << 1) + current - 48;
                readNext();
            }
            if (current == '.') {
                mode = 1;
                // direct scan numbers
                while (NumberUtils.isDigit(readNext())) {
                    value = (value << 3) + (value << 1) + current - 48;
                    ++decimalCount;
                }
            }
            if (current <= ' ') {
                while (readNext() <= ' ') ;
            }
            if (current == ',' || current == endSyntax) {
                break;
            }
            if (current == 'E' || current == 'e') {
                // 科学计数法(浮点模式)
                mode = 2;
                if ((expNegative = readNext() == '-') || current == '+') {
                    readNext();
                }
                if (NumberUtils.isDigit(current)) {
                    expValue = current - 48;
                    while (NumberUtils.isDigit(readNext())) {
                        expValue = (expValue << 3) + (expValue << 1) + current - 48;
                    }
                }
                if (current == ',' || current == endSyntax) {
                    break;
                }
            }
            switch (current) {
                case 'l':
                case 'L': {
                    if (specifySuffix == 0) {
                        specifySuffix = 1;
                        while (readNext() <= ' ') ;
                        if (current == ',' || current == endSyntax) {
                            break;
                        }
                    }
                    throwUnexpectedException();
                    return value;
                }
                case 'f':
                case 'F': {
                    if (specifySuffix == 0) {
                        specifySuffix = 2;
                        if (current == ',' || current == endSyntax) {
                            break;
                        }
                    }
                    throwUnexpectedException();
                    return value;
                }
                case 'd':
                case 'D': {
                    if (specifySuffix == 0) {
                        specifySuffix = 3;
                        if (current == ',' || current == endSyntax) {
                            break;
                        }
                    }
                    throwUnexpectedException();
                    return value;
                }
                default: {
                    throwUnexpectedException();
                }
            }
        } while (false);

        if (mode == 0) {
            value = negative ? -value : value;
            if (specifySuffix > 0) {
                switch (specifySuffix) {
                    case 1:
                        return value;
                    case 2:
                        return (float) value;
                }
                return value;
            }
            if (value <= Integer.MAX_VALUE && value >= Integer.MIN_VALUE) {
                return (int) value;
            }
            return value;
        } else {
            double dv = NumberUtils.scientificToIEEEDouble(value, expNegative ? expValue + decimalCount : decimalCount - expValue);
            double doubleVal = negative ? -dv : dv;
            if (specifySuffix > 0) {
                switch (specifySuffix) {
                    case 1:
                        return (long) doubleVal;
                    case 2:
                        return (float) doubleVal;
                }
                return doubleVal;
            }
            return doubleVal;
        }
    }

    private Object readArray() throws Exception {
        Collection arrayList = new ArrayList();
//        int elementIndex = 0;
        for (; ; ) {
            clearWhitespaces();
            if (current == ']') {
                // always supported  ',]'
//                if (elementIndex > 0 && !parseContext.allowLastEndComma) {
//                    throw new JSONException("Syntax error, not allowed ',' followed by ']', pos " + pos);
//                }
                return arrayList;
            }
            switch (current) {
                case '{': {
                    Object value = this.readObject();
                    arrayList.add(value);
                    break;
                }
                case '[': {
                    // 2 [ array
                    Object value = this.readArray();
                    arrayList.add(value);
                    break;
                }
                case '"': {
                    String value = this.readString();
                    arrayList.add(value);
                    break;
                }
                case 'n':
                    this.readNull();
                    arrayList.add(null);
                    break;
                case 't':
                    this.readTrue();
                    arrayList.add(true);
                    break;
                case 'f':
                    this.readFalse();
                    arrayList.add(false);
                    break;
                default: {
                    Number value = readNumber(']');
                    arrayList.add(value);
                    if (current == ']') {
                        return arrayList;
                    } else {
                        continue;
                    }
                }
            }
            // elementIndex++;
            clearWhitespaces();
            // , or ]
            if (current == ',') {
                continue;
            }
            if (current == ']') {
                return arrayList;
            }
            if (current == -1) {
                throw new JSONException("Syntax error, the closing symbol ']' is not found, end pos: " + pos);
            }
            throwUnexpectedException();
        }
    }

    /**
     * When the end of the stream is read or the end of the} character is encountered
     *
     * @throws Exception
     */
    Object readObject(String path, GenericParameterizedType<?> genericType) throws Exception {
        Object instance;
        Map map = null;
        ClassStrucWrap classStrucWrap = null;
        final boolean useHook = readerHook != null;
        GenericParameterizedType<?> valueType = null;
        boolean useHookValueParse = useHook && genericType == null;
        do {
            if (genericType == null) {
                if (useHook) {
                    if (!readerHook.filter(path, 1)) {
                        skipObject();
                        return null;
                    }
                    genericType = readerHook.getParameterizedType(path);
                    if (genericType == null) {
                        instance = readerHook.createdMap(path);
                        break;
                    }
                    useHookValueParse = false;
                } else {
                    instance = map = new LinkedHashMap();
                    break;
                }
            }
            Class<?> actualType = genericType.getActualType();
            ReflectConsts.ClassCategory classCategory = genericType.getActualClassCategory();
            if (classCategory == ReflectConsts.ClassCategory.ObjectCategory) {
                classStrucWrap = ClassStrucWrap.get(actualType);
                if (classStrucWrap == null) {
                    throw new UnsupportedOperationException(actualType + " is not supported the path: " + path);
                }
                instance = classStrucWrap.newInstance();
            } else if (classCategory == ReflectConsts.ClassCategory.MapCategory) {
                instance = map = createMapInstance(genericType);
                valueType = genericType.getValueType();
                if (valueType == null) {
                    valueType = GenericParameterizedType.AnyType;
                }
            } else if (classCategory == ReflectConsts.ClassCategory.ANY) {
                instance = map = new LinkedHashMap();
                valueType = GenericParameterizedType.AnyType;
            } else {
                throw new UnsupportedOperationException(actualType + " is not supported the path: " + path);
            }
        } while (false);
        for (; ; ) {
            clearWhitespaces();
            if (current == '}') {
                // always supported last ','
                return instance;
            }
            String key;
            if (current == '"' || current == '\'') {
                final int token = current;
                this.beginReading(0);
                while (readNext() > -1 && current != token) ;
                key = endReadingAsString(-1);
                clearWhitespaces();
                if (current == ':') {
                    clearWhitespaces();
                    Object value;
                    boolean toBreakOrContinue = false;
                    SetterInfo setterInfo = null;
                    JsonProperty jsonProperty = null;
                    // if skip value
                    boolean isSkipValue = false;
                    if (classStrucWrap != null) {
                        setterInfo = classStrucWrap.getSetterInfo(key);
                        isSkipValue = setterInfo == null;
                        if (!isSkipValue) {
                            jsonProperty = (JsonProperty) setterInfo.getAnnotation(JsonProperty.class);
                            valueType = setterInfo.getGenericParameterizedType();
                        }
                    }
                    if (isSkipValue) {
                        this.skipValue('}');
                    } else {
                        String nextPath = useHook ? path + "/" + key : null;
                        // boolean useHookValueParse = useHook && setterInfo == null;
                        switch (current) {
                            case '{':
                                value = this.readObject(nextPath, valueType);
                                invokeValueOfObject(useHookValueParse, key, value, map, instance, setterInfo, nextPath, JSONNode.OBJECT);
                                break;
                            case '[':
                                value = this.readArray(nextPath, valueType);
                                invokeValueOfObject(useHookValueParse, key, value, map, instance, setterInfo, nextPath, JSONNode.ARRAY);
                                break;
                            case '"':
                                value = parseStringTo(this.readString(), valueType, jsonProperty == null ? null : JSONPropertyDefinition.of(jsonProperty));
                                invokeValueOfObject(useHookValueParse, key, value, map, instance, setterInfo, nextPath, JSONNode.STRING);
                                break;
                            case 'n':
                                readNull();
                                invokeValueOfObject(useHookValueParse, key, null, map, instance, setterInfo, nextPath, JSONNode.NULL);
                                break;
                            case 't':
                                readTrue();
                                value = toBoolType(true, valueType);
                                invokeValueOfObject(useHookValueParse, key, value, map, instance, setterInfo, nextPath, JSONNode.BOOLEAN);
                                break;
                            case 'f':
                                readFalse();
                                value = toBoolType(false, valueType);
                                invokeValueOfObject(useHookValueParse, key, value, map, instance, setterInfo, nextPath, JSONNode.BOOLEAN);
                                break;
                            default:
                                // number
                                value = parseNumberTo(this.readNumber('}'), valueType);
                                toBreakOrContinue = true;
                                invokeValueOfObject(useHookValueParse, key, value, map, instance, setterInfo, nextPath, JSONNode.NUMBER);
                                break;
                        }

                        // if aborted
                        if (isAborted()) {
                            return instance;
                        }
                        if (readerHook != null && readerHook.isAbored()) {
                            abortRead();
                            return instance;
                        }
                        if (!toBreakOrContinue) {
                            clearWhitespaces();
                        }
                    }
                    // , or }
                    if (current == '}') {
                        break;
                    }
                    if (current == ',') {
                        continue;
                    }
                    if (current == -1) {
                        throw new JSONException("Syntax error, the closing symbol '}' is not found, end pos: " + pos);
                    }
                    throwUnexpectedException();
                } else {
                    throwUnexpectedException();
                }
            } else {
                throwUnexpectedException();
            }
        }
        if (readerHook != null && readerHook.isAboredOnParsed(instance, path, 1)) {
            abortRead();
        }
        return instance;
    }

    final Object readArray(String path, GenericParameterizedType<?> genericType) throws Exception {

        Object instance;
        Collection<?> collection;
        Class<?> collectionCls = null;
        GenericParameterizedType<?> valueType = null;
        Class<?> actualType = null;
        boolean isArrayCls = false, elementPrimitive = false;
        boolean useHook = readerHook != null;
        boolean useHookValueParse = useHook && genericType == null;
        do {
            if (genericType == null) {
                if (useHook) {
                    if (!readerHook.filter(path, 2)) {
                        skipArray();
                        return null;
                    }
                    genericType = readerHook.getParameterizedType(path);
                    if (genericType == null) {
                        collection = readerHook.createdCollection(path);
                        instance = collection;
                        break;
                    }
                    useHookValueParse = false;
                } else {
                    if (collectionCls == null || collectionCls == ArrayList.class) {
                        collection = new ArrayList<Object>();
                    } else {
                        isArrayCls = collectionCls.isArray();
                        if (isArrayCls) {
                            // arr用list先封装数据再转化为数组
                            collection = new ArrayList<Object>();
                            actualType = collectionCls.getComponentType();
                            if (valueType == null) {
                                valueType = GenericParameterizedType.actualType(actualType);
                            }
                        } else {
                            collection = createCollectionInstance(collectionCls);
                        }
                    }
                    instance = collection;
                    break;
                }
            }

            collectionCls = genericType.getActualType();
            isArrayCls = collectionCls.isArray();
            valueType = genericType.getValueType();
            if (valueType == null) {
                valueType = GenericParameterizedType.AnyType;
            }
            actualType = valueType.getActualType();
            elementPrimitive = actualType.isPrimitive();

            instance = collection = isArrayCls ? new ArrayList<Object>() : createCollectionInstance(collectionCls);
        } while (false);

        int elementIndex = 0;
        for (; ; ) {
            clearWhitespaces();
            if (current == ']') {
                // always supported ','
                return isArrayCls ? CollectionUtils.toArray(collection, actualType == null ? Object.class : actualType) : collection;
            }
            boolean toBreakOrContinue = false;
            String nextPath = useHook ? path + "/" + elementIndex : null;
            // boolean useHookParse = useHook && genericType == null;
            switch (current) {
                case '{': {
                    Object value = this.readObject(nextPath, valueType);
                    this.parseCollectionElement(useHookValueParse, value, collection, instance, elementIndex, nextPath, JSONNode.OBJECT);
                    break;
                }
                case '[': {
                    // 2 [ array
                    Object value = this.readArray(nextPath, valueType);
                    this.parseCollectionElement(useHookValueParse, value, collection, instance, elementIndex, nextPath, JSONNode.ARRAY);
                    break;
                }
                case '"': {
                    // 3 string
                    Object value = parseStringTo(this.readString(), valueType, null);
                    this.parseCollectionElement(useHookValueParse, value, collection, instance, elementIndex, nextPath, JSONNode.STRING);
                    break;
                }
                case 'n':
                    readNull();
                    this.parseCollectionElement(useHookValueParse, elementPrimitive ? ObjectUtils.defaulValue(actualType) : null, collection, instance, elementIndex, nextPath, JSONNode.NULL);
                    break;
                case 't': {
                    readTrue();
                    Object value = toBoolType(true, valueType);
                    this.parseCollectionElement(useHookValueParse, value, collection, instance, elementIndex, nextPath, JSONNode.BOOLEAN);
                    break;
                }
                case 'f': {
                    readFalse();
                    Object value = toBoolType(false, valueType);
                    this.parseCollectionElement(useHookValueParse, value, collection, instance, elementIndex, nextPath, JSONNode.BOOLEAN);
                    break;
                }
                default: {
                    // null, boolean, number
                    Object value = parseNumberTo(this.readNumber(']'), valueType);
                    toBreakOrContinue = true;
                    this.parseCollectionElement(useHookValueParse, value, collection, instance, elementIndex, nextPath, JSONNode.NUMBER);
                    break;
                }
            }
            // if aborted
            if (isAborted()) {
                return isArrayCls ? CollectionUtils.toArray(collection, actualType == null ? Object.class : actualType) : collection;
            }
            // supported abort
            if (readerHook != null && readerHook.isAbored()) {
                abortRead();
                return isArrayCls ? CollectionUtils.toArray(collection, actualType == null ? Object.class : actualType) : collection;
            }
            elementIndex++;
            if (!toBreakOrContinue) {
                clearWhitespaces();
            }
            // , or ]
            if (current == ']') {
                break;
            }
            if (current == ',') {
                continue;
            }
            if (current == -1) {
                throw new JSONException("Syntax error, the closing symbol ']' is not found, end pos: " + pos);
            }
            throwUnexpectedException();
        }
        if (readerHook != null && readerHook.isAboredOnParsed(instance, path, 2)) {
            abortRead();
        }
        return isArrayCls ? CollectionUtils.toArray(collection, actualType == null ? Object.class : actualType) : collection;
    }

    private Object toBoolType(boolean b, GenericParameterizedType<?> valueType) {
        if (valueType == null || valueType == GenericParameterizedType.AnyType || valueType.getActualClassCategory() == ReflectConsts.ClassCategory.BoolCategory) {
            return b;
        }
        Class<?> actualType = valueType.getActualType();
        if (actualType == AtomicBoolean.class) {
            return new AtomicBoolean(b);
        }
        throw new JSONException("boolean value " + b + " is mismatch " + actualType);
    }

    private void skipValue(char endChar) throws Exception {
        switch (current) {
            case '{':
                this.skipObject();
                this.clearWhitespaces();
                break;
            case '[':
                this.skipArray();
                this.clearWhitespaces();
                break;
            case '"':
                // string
                this.skipString();
                this.clearWhitespaces();
                break;
            default:
                // null, boolean, number
                this.skipSimple(endChar);
                break;
        }
    }

    private void skipObject() throws Exception {
        for (; ; ) {
            clearWhitespaces();
            if (current == '}') {
                // always supported ','
                return;
            }
            if (current == '"') {
                while (readNext() > -1 && current != '"') ;
                clearWhitespaces();
                if (current == ':') {
                    clearWhitespaces();
                    this.skipValue('}');
                    // , or }
                    if (current == '}') {
                        return;
                    }
                    if (current == ',') {
                        continue;
                    }
                    if (current == -1) {
                        throw new JSONException("Syntax error, the closing symbol '}' is not found, end pos: " + pos);
                    }
                    throwUnexpectedException();
                } else {
                    throwUnexpectedException();
                }
            } else {
                throwUnexpectedException();
            }
        }
    }

    private void skipArray() throws Exception {
        for (; ; ) {
            clearWhitespaces();
            if (current == ']') {
                // always supported ','
                return;
            }
            this.skipValue(']');
            // , or ]
            if (current == ']') {
                return;
            }
            if (current == ',') {
                continue;
            }
            if (current == -1) {
                throw new JSONException("Syntax error, the closing symbol ']' is not found, end pos: " + pos);
            }
            throwUnexpectedException();
        }
    }

    private void skipString() throws Exception {
        char prev = '\0';
        while (readNext() > -1) {
            // todo The logic here is not rigorous enough
            if (current == '"' && prev != '\\') {
                return;
            }
            prev = (char) current;
        }
        // maybe throw an exception
        throwUnexpectedException();
    }

    private void skipSimple(char endChar) throws Exception {
        while (readNext() > -1) {
            if (current == ',' || current == endChar) {
                return;
            }
        }
        throwUnexpectedException();
    }

    void beginReading(int n) {
        writer.setLength(0);
        this.readingOffset = offset + n;
    }

    abstract String endReadingAsString(int n);

    void endReading(int n) {
        endReading(n, -1);
    }

    /**
     * @param n         End offset correction position
     * @param newOffset
     */
    abstract void endReading(int n, int newOffset);

    private Object parseNumberTo(Object simpleValue, GenericParameterizedType valueType) {
        if (simpleValue == null) return null;
        ReflectConsts.ClassCategory classCategory;
        if (valueType == null || (classCategory = valueType.getActualClassCategory()) == ReflectConsts.ClassCategory.ANY) {
            return simpleValue;
        }
        Class<?> actualType = valueType.getActualType();
        if (ObjectUtils.isInstance(actualType, simpleValue)) {
            return simpleValue;
        }
        Number numValue = (Number) simpleValue;
        if (classCategory == ReflectConsts.ClassCategory.NumberCategory) {
            return ObjectUtils.toTypeNumber(numValue, actualType);
        } else if (classCategory == ReflectConsts.ClassCategory.EnumCategory) {
            int ordinal = numValue.intValue();
            Enum[] values = (Enum[]) actualType.getEnumConstants();
            if (values != null && ordinal < values.length)
                return values[ordinal];
            throw new JSONException("value " + numValue + " is mismatch enum " + actualType);
        }
        throw new JSONException("read simple value " + numValue + " is mismatch " + actualType);
    }

    private Object parseStringTo(String value, GenericParameterizedType<?> valueType, JSONPropertyDefinition propertyDefinition) throws Exception {
        if (value == null) return null;
        if (valueType == null || valueType == GenericParameterizedType.AnyType) {
            return value;
        }
        Class<?> actualType = valueType.getActualType();
        if (actualType == String.class || actualType == CharSequence.class) {
            return value;
        }
        JSONTypeDeserializer deserializer = JSONStore.INSTANCE.getFieldDeserializer(valueType, propertyDefinition);
        return deserializer.valueOf(value, actualType);
    }

    private void invokeValueOfObject(boolean useHookParse, String key, Object value, Map map, Object instance, SetterInfo setterInfo, String nextPath, int type) throws Exception {
        if (useHookParse) {
            readerHook.parseValue(key, value, instance, -1, nextPath, type);
        } else {
            if (setterInfo != null) {
                JSON_SECURE_TRUSTED_ACCESS.set(setterInfo, instance, value);
            } else {
                if (map != null) {
                    map.put(key, value);
                }
            }
        }
    }

    private void parseCollectionElement(boolean useHookParse, Object value, Collection collection, Object instance, int elementIndex, String nextPath, int type) throws Exception {
        if (useHookParse) {
            readerHook.parseValue(null, value, instance, elementIndex, nextPath, type);
        } else {
            collection.add(value);
        }
    }

    final void abortRead() {
        this.aborted = true;
    }

    public final boolean isAborted() {
        return aborted;
    }

    /**
     * throw unexpected exception
     */
    final void throwUnexpectedException() {
        throw new JSONException("Syntax error, unexpected '" + (char) current + "', position " + pos);
    }

    final String readString() throws Exception {
        // reset StringBuilder
        this.beginReading(0);
        char prev = '\0';
        while (readNext() > -1) {
            if (prev == '\\') {
                // Due to batch reading, if the last character in the current batch is the escape character \ \,
                // the escape character will be written to the writer and needs to be cleared when readNext() is used
                if (offset == 1) {
                    // remove \\
                    int bufferLen = writer.length();
                    writer.setLength(bufferLen - 1);
                }
                switch (current) {
                    case '"':
                        // Skip \\ and current
                        this.endReading(-2, offset);
                        writer.append('\"');
                        break;
                    case 'n':
                        // Skip \\ and current
                        this.endReading(-2, offset);
                        writer.append('\n');
                        break;
                    case 'r':
                        // Skip \\ and current
                        this.endReading(-2, offset);
                        writer.append('\r');
                        break;
                    case 't':
                        // Skip \\ and current
                        this.endReading(-2, offset);
                        writer.append('\t');
                        break;
                    case 'b':
                        // Skip \\ and current
                        this.endReading(-2, offset);
                        writer.append('\b');
                        break;
                    case 'f':
                        // Skip \\ and current
                        this.endReading(-2, offset);
                        writer.append('\f');
                        break;
                    case 'u':
                        // Skip \\ and current
                        this.endReading(-2, offset);
                        // stop reading buffer
                        this.readingOffset = -1;
                        int c1 = readNext(true);
                        int c2 = readNext(true);
                        int c3 = readNext(true);
                        int c4 = readNext(true);
                        long c64;
                        try {
                            c64 = hex4ToLong(c1, c2, c3, c4);
                            if (c64 < 0) {
                                throw new JSONException("Syntax error, from pos " + offset + ", invalid unicode " + new String(new char[]{(char) c1, (char) c2, (char) c3, (char) c4}));
                            }
                        } catch (Throwable throwable) {
                            throw new JSONException("Syntax error, from pos " + offset + ", invalid unicode " + new String(new char[]{(char) c1, (char) c2, (char) c3, (char) c4}));
                        }
                        char c = (char) c64;
                        // begin reading and locate to offset
                        this.readingOffset = offset;
                        writer.append(c);
                        break;
                    case '\\':
                        // Skip \\ and current
                        this.endReading(-2, offset);
                        writer.append('\\');
                        break;
                    default: {
                        // other case delete char '\\'
                        this.endReading(-2, offset);
                        writer.append((char) current);
                        break;
                    }
                }
                prev = '\0';
                continue;
            }
            if (current == '"') {
                return endReadingAsString(-1);
            }
            prev = (char) current;
        }
        throwUnexpectedException();
        return null;
    }

    abstract int readNext() throws Exception;

    final int readNext(boolean check) throws Exception {
        readNext();
        if (check && current == -1) {
            tryCloseReader();
            throw new JSONException("Unexpected error, stream is end ");
        }
        return current;
    }

    /**
     * Clear white space characters until non empty characters
     *
     * @throws IOException
     */
    private void clearWhitespaces() throws Exception {
        while (readNext() > -1 && current <= ' ') ;
    }

    /**
     * whether the reading completed
     *
     * @return
     */
    abstract boolean isCompleted();

    /**
     * return the result of parsing
     *
     * @return
     */
    public Object getResult() {
        return getResult(timeout);
    }

    /**
     * return the result of parsing
     *
     * @return
     */
    public Object getResult(long timeout) {
        if (async) {
            long threadId = Thread.currentThread().getId();
            // <p> The getResult () that calls the reader in the readerHook mode can be blocked.
            if (threadId != currentThreadId) {
                return result;
            }
            await(timeout);
        }
        return result;
    }

    private void await(long timeout) {
        try {
            countDownLatch.await(timeout, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * try to close stream
     */
    void tryCloseReader() {
        writer.setLength(0);
        if (!multiple) {
            close();
        }
    }

    /**
     * close stream
     */
    public abstract void close();

    public JSONAbstractReader multiple(boolean multiple) {
        this.multiple = multiple;
        return this;
    }

    public JSONAbstractReader options(ReadOption[] readOptions) {
        this.readOptions = readOptions;
        return this;
    }
}
