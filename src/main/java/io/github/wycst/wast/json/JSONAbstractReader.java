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

import io.github.wycst.wast.common.reflect.ClassStructureWrapper;
import io.github.wycst.wast.common.reflect.GenericParameterizedType;
import io.github.wycst.wast.common.reflect.ReflectConsts;
import io.github.wycst.wast.common.reflect.SetterInfo;
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
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * @author wangyunchao
 * @see ReaderCallback
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
    ReaderCallback callback;
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
    volatile Object lock = new Object();
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
        read(new ReaderCallback(), async);
    }

    public Object readAsResult(Class<?> actualType) {
        return readAsResult(GenericParameterizedType.actualType(actualType));
    }

    public <T> T readAsResult(GenericParameterizedType<T> genericType) {
        this.genericType = genericType;
        this.executeReadStream();
        return (T) result;
    }

    public void read(ReaderCallback callback) {
        read(callback, false);
    }

    /**
     * @param callback
     * @param async
     */
    public void read(ReaderCallback callback, boolean async) {
        if (reading) return;
        checkReadState();
        this.callback = callback;
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
        synchronized (lock) {
            lock.notify();
        }
    }

    private void await(long timeout) {
        synchronized (lock) {
            try {
                lock.wait(timeout);
            } catch (InterruptedException e) {
                throw new JSONException(e);
            }
        }
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
        if (callback != null) {
            callback.complete(result);
        }
    }

    private void checkAutoGenericCollectionType() {
        if (this.genericType == null) {
            this.genericType = GenericParameterizedType.collectionType(ArrayList.class, LinkedHashMap.class);
        } else {
            Class<?> actualType = genericType.getActualType();
            if (!Collection.class.isAssignableFrom(actualType)) {
                if(!actualType.isArray()) {
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
                if (!empty) {
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
        if(useBigDecimal) {
            writer.setLength(0);
            writer.append((char) current);
            int ch;
            while ((ch = readNext()) != ',' && ch != endSyntax) {
                writer.append((char) ch);
            }
            String text = writer.toString();
            try {
                return new BigDecimal(text.trim());
            } catch (NumberFormatException  numberFormatException) {
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
//            double doubleVal = value;
//            expValue = expNegative ? -expValue - decimalCount : expValue - decimalCount;
//            if (expValue > 0) {
//                double powValue = getDecimalPowerValue(expValue); // Math.pow(radix, expValue);
//                doubleVal *= powValue;
//            } else if (expValue < 0) {
//                double powValue = getDecimalPowerValue(-expValue);// Math.pow(radix, -expValue);
//                doubleVal /= powValue;
//            }
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
                if (elementIndex > 0) {
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
        Map mapInstane = null;
        boolean assignableFromMap = true;
        ClassStructureWrapper classStructureWrapper = null;
        boolean externalImpl = isExternalImpl();
        GenericParameterizedType ofValueType = null;
        if (!externalImpl) {
            if (genericType != null) {
                Class<?> actualType = genericType.getActualType();
                ReflectConsts.ClassCategory classCategory = genericType.getActualClassCategory();
                if (classCategory == ReflectConsts.ClassCategory.MapCategory || classCategory == ReflectConsts.ClassCategory.ANY) {
                    assignableFromMap = true;
                    instance = mapInstane = createMapInstance(genericType);
                    ofValueType = genericType.getValueType();
                } else if (classCategory == ReflectConsts.ClassCategory.ObjectCategory) {
                    assignableFromMap = false;
                    classStructureWrapper = ClassStructureWrapper.get(actualType);
                    if (classStructureWrapper == null) {
                        throw new UnsupportedOperationException("Class " + actualType + " is not supported ");
                    }
                    instance = classStructureWrapper.newInstance();
                } else {
                    throw new UnsupportedOperationException("Class " + actualType + " is not supported ");
                }
            } else {
                instance = mapInstane = new LinkedHashMap();
            }
        } else {
            instance = callback.created(path, 1);
        }

        boolean empty = true;
        for (; ; ) {
            clearWhitespaces();
            if (current == '}') {
                if (!empty) {
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
                        setterInfo = classStructureWrapper.getSetterInfo(key);
                        isSkipValue = setterInfo == null;
                        if (!isSkipValue) {
                            jsonProperty = (JsonProperty) setterInfo.getAnnotation(JsonProperty.class);
                            valueType = setterInfo.getGenericParameterizedType();
                        }
                    }
                    if (isSkipValue) {
                        this.skipValue('}');
                    } else {
                        String nextPath = externalImpl ? path + "/" + key : null;
                        switch (current) {
                            case '{':
                                value = this.readObject(nextPath, valueType);
                                invokeValueOfObject(key, value, nextPath, externalImpl, assignableFromMap, mapInstane, instance, setterInfo);
                                break;
                            case '[':
                                value = this.readArray(nextPath, valueType);
                                invokeValueOfObject(key, value, nextPath, externalImpl, assignableFromMap, mapInstane, instance, setterInfo);
                                break;
                            case '"':
                                value = parseStringTo(this.readString(), valueType, jsonProperty);
                                invokeValueOfObject(key, value, nextPath, externalImpl, assignableFromMap, mapInstane, instance, setterInfo);
                                break;
                            case 'n':
                                readNull();
                                invokeValueOfObject(key, null, nextPath, externalImpl, assignableFromMap, mapInstane, instance, setterInfo);
                                break;
                            case 't':
                                readTrue();
                                value = toBoolType(true, valueType);
                                invokeValueOfObject(key, value, nextPath, externalImpl, assignableFromMap, mapInstane, instance, setterInfo);
                                break;
                            case 'f':
                                readFalse();
                                value = toBoolType(false, valueType);
                                invokeValueOfObject(key, value, nextPath, externalImpl, assignableFromMap, mapInstane, instance, setterInfo);
                                break;
                            default:
                                // number
                                value = parseNumberTo(this.readNumber('}'), valueType);
                                toBreakOrContinue = true;
                                invokeValueOfObject(key, value, nextPath, externalImpl, assignableFromMap, mapInstane, instance, setterInfo);
                                break;
                        }

                        // if aborted
                        if (isAborted()) {
                            return instance;
                        }
                        if (callback != null) {
                            if (callback.isAbored()) {
                                abortRead();
                                return instance;
                            }
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
                if (!empty) {
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
                if (elementIndex > 0) {
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
        if (actualType.isInstance(simpleValue)) {
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

    private void invokeValueOfObject(String key, Object value, String nextPath, boolean externalImpl, boolean assignableFromMap, Map mapInstane, Object instance, SetterInfo setterInfo) throws Exception {
        if (!externalImpl) {
            if (assignableFromMap) {
                mapInstane.put(key, value);
            } else {
                if (setterInfo != null) {
                    setterInfo.invoke(instance, value);
                }
            }
        } else {
            callback.parseValue(key, value, instance, -1, nextPath);
        }
    }

    private void parseCollectionElement(boolean externalImpl, Object value, Collection arrInstance, Object instance, int elementIndex, String nextPath) throws Exception {
        if (!externalImpl) {
            arrInstance.add(value);
        } else {
            callback.parseValue(null, value, instance, elementIndex, nextPath);
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
        Collection arrInstance = null;
        Class<?> collectionCls = null;
        GenericParameterizedType valueType = null;
        Class actualType = null;
        boolean isArrayCls = false;
        if (genericType != null) {
            collectionCls = genericType.getActualType();
            valueType = genericType.getValueType();
            actualType = valueType == null ? null : valueType.getActualType();
        }
        boolean externalImpl = isExternalImpl();
        if (!isExternalImpl()) {
            if (collectionCls == null || collectionCls == ArrayList.class) {
                arrInstance = new ArrayList<Object>();
            } else {
                isArrayCls = collectionCls.isArray();
                if (isArrayCls) {
                    // arr用list先封装数据再转化为数组
                    arrInstance = new ArrayList<Object>();
                    actualType = collectionCls.getComponentType();
                    if (valueType == null) {
                        valueType = GenericParameterizedType.actualType(actualType);
                    }
                } else {
                    arrInstance = createCollectionInstance(collectionCls);
                }
            }
            instance = arrInstance;
        } else {
            instance = callback.created(path, 2);
        }

        int elementIndex = 0;
        for (; ; ) {
            clearWhitespaces();
            if (current == ']') {
                if (elementIndex > 0) {
                    throw new JSONException("Syntax error, not allowed ',' followed by ']', pos " + pos);
                }
                return isArrayCls ? collectionToArray(arrInstance, actualType == null ? Object.class : actualType) : arrInstance;
            }

            boolean toBreakOrContinue = false;
            String nextPath = externalImpl ? path + "/[" + elementIndex + "]" : null;

            switch (current) {
                case '{': {
                    Object value = this.readObject(nextPath, valueType);
                    this.parseCollectionElement(externalImpl, value, arrInstance, instance, elementIndex, nextPath);
                    break;
                }
                case '[': {
                    // 2 [ array
                    Object value = this.readArray(nextPath, valueType);
                    this.parseCollectionElement(externalImpl, value, arrInstance, instance, elementIndex, nextPath);
                    break;
                }
                case '"': {
                    // 3 string
                    Object value = parseStringTo(this.readString(), valueType, null);
                    this.parseCollectionElement(externalImpl, value, arrInstance, instance, elementIndex, nextPath);
                    break;
                }
                case 'n':
                    readNull();
                    this.parseCollectionElement(externalImpl, null, arrInstance, instance, elementIndex, nextPath);
                    break;
                case 't': {
                    readTrue();
                    Object value = toBoolType(true, valueType);
                    this.parseCollectionElement(externalImpl, value, arrInstance, instance, elementIndex, nextPath);
                    break;
                }
                case 'f': {
                    readFalse();
                    Object value = toBoolType(false, valueType);
                    this.parseCollectionElement(externalImpl, value, arrInstance, instance, elementIndex, nextPath);
                    break;
                }
                default: {
                    // null, boolean, number
                    Object value = parseNumberTo(this.readNumber(']'), valueType);
                    toBreakOrContinue = true;
                    this.parseCollectionElement(externalImpl, value, arrInstance, instance, elementIndex, nextPath);
                    break;
                }
            }

            // if aborted
            if (isAborted()) {
                return isArrayCls ? collectionToArray(arrInstance, actualType == null ? Object.class : actualType) : arrInstance;
            }

            // supported abort
            if (callback != null) {
                if (callback.isAbored()) {
                    abortRead();
                    return isArrayCls ? collectionToArray(arrInstance, actualType == null ? Object.class : actualType) : arrInstance;
                }
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

        return isArrayCls ? collectionToArray(arrInstance, actualType == null ? Object.class : actualType) : arrInstance;
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
     * External implementation
     */
    private boolean isExternalImpl() {
        return this.callback != null && this.callback.readParseMode == ReadParseMode.ExternalImpl;
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
            // <p> The getResult () that calls the reader in the callback mode can be blocked.
            if (threadId != currentThreadId) {
                return result;
            }
            await(timeout);
        }
        return result;
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

    public enum ReadParseMode {

        /**
         * Built in parsing
         */
        BuiltParse,

        /**
         * External implementation
         */
        ExternalImpl
    }

    /***
     * Response parsing process through callback (subscription) mode
     * Hook mode, non asynchronous call
     */
    public static class ReaderCallback {

        // Read parsing mode
        private final ReadParseMode readParseMode;
        private boolean abored;

        /**
         * Default internal parsing mode
         * After reading the end of the stream, it is returned to the user
         */
        public ReaderCallback() {
            this(ReadParseMode.BuiltParse);
        }

        public ReaderCallback(ReadParseMode readParseMode) {
            this.readParseMode = readParseMode;
        }

        /**
         * Give the initiative to build the object to the caller. If the type is 1, create a map or object. If the type is 2, create a collection object
         *
         * @param path JSON PATH
         * @param type 1. Object type; 2 Collection type
         * @return
         * @throws Exception
         */
        public Object created(String path, int type) throws Exception {
            return null;
        }

        /**
         * Assign property settings to the caller
         *
         * @param key          the key if object({}), otherwise null
         * @param value        map/collection/string/number
         * @param host         object or collection
         * @param elementIndex the index if collection([]), otherwise -1
         * @param path         JSON PATH
         * @throws Exception
         */
        public void parseValue(String key, Object value, Object host, int elementIndex, String path) throws Exception {
        }

        /**
         * Parse completed callback
         *
         * @param result
         */
        void complete(Object result) {
        }

        /**
         * terminate read operation
         */
        final void abort() {
            this.abored = true;
        }

        final boolean isAbored() {
            return abored;
        }
    }
}
