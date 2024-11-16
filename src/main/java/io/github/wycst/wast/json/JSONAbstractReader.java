/*
 * Copyright [2020-2024] [wangyunchao]
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
    GenericParameterizedType genericType;
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
        this.genericType = genericType;
        this.executeReadStream();
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
            this.executeReadStream();
        } else {
            // async
            this.async = true;
            this.currentThreadId = Thread.currentThread().getId();
            new Thread(new Runnable() {
                public void run() {
                    executeReadStream();
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
                    throw new UnsupportedOperationException("Character stream start character error. Only object({) or array([) parsing is supported");
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
        if (this.closed) {
            throw new UnsupportedOperationException("is closed");
        }
        if (this.aborted) {
            throw new UnsupportedOperationException("is aborted");
        }
    }

    private void executeReadStream() {
        try {
            this.readBuffer();
            this.beginReadWithType();
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
                throw new UnsupportedOperationException("Character stream start character error. Only object({) or array([) parsing is supported");
        }
        // clear white space characters
        if (!multiple) {
            clearWhitespaces();
            if (current > -1) {
                throw new JSONException("Syntax error, extra characters found, '" + (char) current + "', pos " + pos);
            }
        }
    }

    private void beginReadWithType() throws Exception {
        clearWhitespaces();
        switch (current) {
            case '{':
                this.checkAutoGenericObjectType();
                this.result = this.readObject("", genericType);
                break;
            case '[':
                this.checkAutoGenericCollectionType();
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

    private void checkAutoGenericCollectionType() {
        if (this.genericType == null) {
            this.genericType = GenericParameterizedType.collectionType(ArrayList.class, LinkedHashMap.class);
        } else {
            Class<?> actualType = genericType.getActualType();
            if (!Collection.class.isAssignableFrom(actualType)) {
                if (!actualType.isArray()) {
                    this.genericType = GenericParameterizedType.collectionType(ArrayList.class, actualType);
                }
            }
        }
    }

    private void checkAutoGenericObjectType() {
        if (this.genericType == null) {
            this.genericType = GenericParameterizedType.actualType(LinkedHashMap.class);
        }
    }

    private Object readObject() throws Exception {
        Map instance = new LinkedHashMap();
        boolean empty = true;
        for (; ; ) {
            clearWhitespaces();
            if (current == '}') {
                if (!empty && !parseContext.allowLastEndComma) {
                    throw new JSONException("Syntax error, not allowed ',' followed by '}', pos " + pos);
                }
                return instance;
            }
            String key;
            if (current == '"') {
                empty = false;
                // find next "
                this.beginReading(0);
                // todo The reading logic is not rigorous enough and needs to be escaped
                while (readNext() > -1 && current != '"') ;
                key = endReadingAsString(-1);
                clearWhitespaces();
                if (current == ':') {
                    clearWhitespaces();
                    Object value;
                    switch (current) {
                        case '{':
                            value = this.readObject();
                            instance.put(key, value);
                            break;
                        case '[':
                            value = this.readArray();
                            instance.put(key, value);
                            break;
                        case '"':
                            value = this.readString();
                            instance.put(key, value);
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
                            value = this.readNumber('}');
                            instance.put(key, value);
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
            while (isDigit(current)) {
                value = (value << 3) + (value << 1) + current - 48;
                readNext();
            }
            if (current == '.') {
                mode = 1;
                // direct scan numbers
                while (isDigit(readNext())) {
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
                if (isDigit(current)) {
                    expValue = current - 48;
                    while (isDigit(readNext())) {
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
        Collection arrInstance = new ArrayList();
        int elementIndex = 0;
        for (; ; ) {
            clearWhitespaces();
            if (current == ']') {
                if (elementIndex > 0 && !parseContext.allowLastEndComma) {
                    throw new JSONException("Syntax error, not allowed ',' followed by ']', pos " + pos);
                }
                return arrInstance;
            }
            switch (current) {
                case '{': {
                    Object value = this.readObject();
                    arrInstance.add(value);
                    break;
                }
                case '[': {
                    // 2 [ array
                    Object value = this.readArray();
                    arrInstance.add(value);
                    break;
                }
                case '"': {
                    String value = this.readString();
                    arrInstance.add(value);
                    break;
                }
                case 'n':
                    this.readNull();
                    arrInstance.add(null);
                    break;
                case 't':
                    this.readTrue();
                    arrInstance.add(true);
                    break;
                case 'f':
                    this.readFalse();
                    arrInstance.add(false);
                    break;
                default: {
                    Number value = readNumber(']');
                    arrInstance.add(value);
                    if (current == ']') {
                        return arrInstance;
                    } else {
                        continue;
                    }
                }
            }

            elementIndex++;
            clearWhitespaces();
            // , or ]
            if (current == ',') {
                continue;
            }
            if (current == ']') {
                return arrInstance;
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
    Object readObject(String path, GenericParameterizedType genericType) throws Exception {

        Object instance;
        Map map = null;
        boolean assignableFromMap = true;
        ClassStrucWrap classStrucWrap = null;
        boolean externalImpl = readerHook != null;
        GenericParameterizedType ofValueType = null;
        if (!externalImpl) {
            if (genericType != null) {
                Class<?> actualType = genericType.getActualType();
                ReflectConsts.ClassCategory classCategory = genericType.getActualClassCategory();
                if (classCategory == ReflectConsts.ClassCategory.MapCategory || classCategory == ReflectConsts.ClassCategory.ANY) {
                    assignableFromMap = true;
                    instance = map = createMapInstance(genericType);
                    ofValueType = genericType.getValueType();
                } else if (classCategory == ReflectConsts.ClassCategory.ObjectCategory) {
                    assignableFromMap = false;
                    classStrucWrap = ClassStrucWrap.get(actualType);
                    if (classStrucWrap == null) {
                        throw new UnsupportedOperationException(actualType + " is not supported ");
                    }
                    instance = classStrucWrap.newInstance();
                } else {
                    throw new UnsupportedOperationException(actualType + " is not supported ");
                }
            } else {
                instance = map = new LinkedHashMap();
            }
        } else {
            if (!readerHook.filter(path, 1)) {
                skipObject();
                return null;
            }
            instance = readerHook.created(path, 1);
        }

        boolean empty = true;
        for (; ; ) {
            clearWhitespaces();
            if (current == '}') {
                if (!empty && !parseContext.allowLastEndComma) {
                    throw new JSONException("Syntax error, not allowed ',' followed by '}', pos " + pos);
                }
                return instance;
            }
            String key;
            if (current == '"') {
                empty = false;
                // find next "
                this.beginReading(0);
                while (readNext() > -1 && current != '"') ;
                key = endReadingAsString(-1);
                // 解析value
                clearWhitespaces();
                if (current == ':') {
                    clearWhitespaces();
                    Object value;
                    boolean toBreakOrContinue = false;

                    GenericParameterizedType valueType = ofValueType == null ? null : ofValueType;
                    SetterInfo setterInfo = null;
                    JsonProperty jsonProperty = null;
                    // if skip value
                    boolean isSkipValue = false;
                    if (!externalImpl && !assignableFromMap) {
                        setterInfo = classStrucWrap.getSetterInfo(key);
                        isSkipValue = setterInfo == null;
                        if (!isSkipValue) {
                            jsonProperty = (JsonProperty) setterInfo.getAnnotation(JsonProperty.class);
                            valueType = setterInfo.getGenericParameterizedType();
                        }
                    } else {

                    }
                    if (isSkipValue) {
                        this.skipValue('}');
                    } else {
                        String nextPath = externalImpl ? path + "/" + key : null;
                        switch (current) {
                            case '{':
                                value = this.readObject(nextPath, valueType);
                                invokeValueOfObject(key, value, nextPath, externalImpl, assignableFromMap, map, instance, setterInfo, JSONNode.OBJECT);
                                break;
                            case '[':
                                value = this.readArray(nextPath, valueType);
                                invokeValueOfObject(key, value, nextPath, externalImpl, assignableFromMap, map, instance, setterInfo, JSONNode.ARRAY);
                                break;
                            case '"':
                                value = parseStringTo(this.readString(), valueType, jsonProperty);
                                invokeValueOfObject(key, value, nextPath, externalImpl, assignableFromMap, map, instance, setterInfo, JSONNode.STRING);
                                break;
                            case 'n':
                                readNull();
                                invokeValueOfObject(key, null, nextPath, externalImpl, assignableFromMap, map, instance, setterInfo, JSONNode.NULL);
                                break;
                            case 't':
                                readTrue();
                                value = toBoolType(true, valueType);
                                invokeValueOfObject(key, value, nextPath, externalImpl, assignableFromMap, map, instance, setterInfo, JSONNode.BOOLEAN);
                                break;
                            case 'f':
                                readFalse();
                                value = toBoolType(false, valueType);
                                invokeValueOfObject(key, value, nextPath, externalImpl, assignableFromMap, map, instance, setterInfo, JSONNode.BOOLEAN);
                                break;
                            default:
                                // number
                                value = parseNumberTo(this.readNumber('}'), valueType);
                                toBreakOrContinue = true;
                                invokeValueOfObject(key, value, nextPath, externalImpl, assignableFromMap, map, instance, setterInfo, JSONNode.NUMBER);
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

    private Object toBoolType(boolean b, GenericParameterizedType valueType) {
        if (valueType == null) return b;
        if (valueType.getActualClassCategory() == ReflectConsts.ClassCategory.BoolCategory) {
            return b;
        }
        Class actualType = valueType.getActualType();
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
        boolean empty = true;
        for (; ; ) {
            clearWhitespaces();
            if (current == '}') {
                if (!empty && !parseContext.allowLastEndComma) {
                    throw new JSONException("Syntax error, not allowed ',' followed by '}', pos " + pos);
                }
                return;
            }
            if (current == '"') {
                empty = false;
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
        int elementIndex = 0;
        for (; ; ) {
            clearWhitespaces();
            if (current == ']') {
                if (elementIndex > 0 && !parseContext.allowLastEndComma) {
                    throw new JSONException("Syntax error, not allowed ',' followed by ']', pos " + pos);
                }
                return;
            }
            this.skipValue(']');
            elementIndex++;
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
        // maybe throw an exception
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

    private Object parseStringTo(String value, GenericParameterizedType valueType, JsonProperty jsonProperty) throws Exception {
        if (value == null) return null;
        if (valueType == null || valueType == GenericParameterizedType.AnyType) {
            return value;
        }
        Class<?> actualType = valueType.getActualType();
        if (actualType == String.class || actualType == CharSequence.class) {
            return value;
        }
        JSONTypeDeserializer deserializer = JSONTypeDeserializer.getFieldDeserializer(valueType, jsonProperty);
        return deserializer.valueOf(value, actualType);
    }

    private void invokeValueOfObject(String key, Object value, String nextPath, boolean externalImpl, boolean assignableFromMap, Map mapInstane, Object instance, SetterInfo setterInfo, int type) throws Exception {
        if (!externalImpl) {
            if (assignableFromMap) {
                mapInstane.put(key, value);
            } else {
                if (setterInfo != null) {
                    JSON_SECURE_TRUSTED_ACCESS.set(setterInfo, instance, value); // setterInfo.invoke(instance, value);
                }
            }
        } else {
            readerHook.parseValue(key, value, instance, -1, nextPath, type);
        }
    }

    private void parseCollectionElement(boolean externalImpl, Object value, Collection arrInstance, Object instance, int elementIndex, String nextPath, int type) throws Exception {
        if (!externalImpl) {
            arrInstance.add(value);
        } else {
            readerHook.parseValue(null, value, instance, elementIndex, nextPath, type);
        }
    }

    final void abortRead() {
        this.aborted = true;
    }

    public final boolean isAborted() {
        return aborted;
    }

    final Object readArray(String path, GenericParameterizedType genericType) throws Exception {

        Object instance;
        Collection collection = null;
        Class<?> collectionCls = null;
        GenericParameterizedType valueType = null;
        Class actualType = null;
        boolean isArrayCls = false;
        if (genericType != null) {
            collectionCls = genericType.getActualType();
            valueType = genericType.getValueType();
            actualType = valueType == null ? null : valueType.getActualType();
        }
        boolean externalImpl = readerHook != null;
        if (!externalImpl) {
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
        } else {
            if (!readerHook.filter(path, 2)) {
                skipArray();
                return null;
            }
            instance = readerHook.created(path, 2);
            collection = (Collection) instance;
        }

        int elementIndex = 0;
        for (; ; ) {
            clearWhitespaces();
            if (current == ']') {
                if (elementIndex > 0 && !parseContext.allowLastEndComma) {
                    throw new JSONException("Syntax error, not allowed ',' followed by ']', pos " + pos);
                }
                return isArrayCls ? CollectionUtils.toArray(collection, actualType == null ? Object.class : actualType) : collection;
            }

            boolean toBreakOrContinue = false;
            String nextPath = externalImpl ? path + "/" + elementIndex : null;

            switch (current) {
                case '{': {
                    Object value = this.readObject(nextPath, valueType);
                    this.parseCollectionElement(externalImpl, value, collection, instance, elementIndex, nextPath, JSONNode.OBJECT);
                    break;
                }
                case '[': {
                    // 2 [ array
                    Object value = this.readArray(nextPath, valueType);
                    this.parseCollectionElement(externalImpl, value, collection, instance, elementIndex, nextPath, JSONNode.ARRAY);
                    break;
                }
                case '"': {
                    // 3 string
                    Object value = parseStringTo(this.readString(), valueType, null);
                    this.parseCollectionElement(externalImpl, value, collection, instance, elementIndex, nextPath, JSONNode.STRING);
                    break;
                }
                case 'n':
                    readNull();
                    this.parseCollectionElement(externalImpl, null, collection, instance, elementIndex, nextPath, JSONNode.NULL);
                    break;
                case 't': {
                    readTrue();
                    Object value = toBoolType(true, valueType);
                    this.parseCollectionElement(externalImpl, value, collection, instance, elementIndex, nextPath, JSONNode.BOOLEAN);
                    break;
                }
                case 'f': {
                    readFalse();
                    Object value = toBoolType(false, valueType);
                    this.parseCollectionElement(externalImpl, value, collection, instance, elementIndex, nextPath, JSONNode.BOOLEAN);
                    break;
                }
                default: {
                    // null, boolean, number
                    Object value = parseNumberTo(this.readNumber(']'), valueType);
                    toBreakOrContinue = true;
                    this.parseCollectionElement(externalImpl, value, collection, instance, elementIndex, nextPath, JSONNode.NUMBER);
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

    /**
     * throw unexpected exception
     */
    final void throwUnexpectedException() {
        throw new JSONException("Syntax error, unexpected '" + (char) current + "', position " + pos);
    }

//    /**
//     * Starting from the current character (including the current character)
//     */
//    void beginCurrent() {
//        // Every time a character is read, the offset will be+1, and the actual position of the current is offset -1
//        this.beginReading(-1);
//    }

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
                        int c = hex4(c1, c2, c3, c4);

                        // begin reading and locate to offset
                        this.readingOffset = offset;
                        writer.append((char) c);
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

}
