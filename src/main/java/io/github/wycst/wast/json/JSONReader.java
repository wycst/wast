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
import io.github.wycst.wast.common.utils.ObjectUtils;
import io.github.wycst.wast.json.annotations.JsonProperty;
import io.github.wycst.wast.json.exceptions.JSONException;
import io.github.wycst.wast.json.options.ReadOption;

import java.io.*;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 1,基于流(字符流)的JSON解析:
 * <p> - 超大文件json文件解析（文件大小读取无限制）,无需将流内容读取到内存中再解析
 * <p> - 检测当前json类型是对象还是数组，其他类型处理无意义
 * <p> - 可按需终止
 * <p> - 支持异步
 * <br>
 * 2, 流内容需要严格遵守JSON规范.
 *
 * <br>
 * <br>
 * 例子:
 * <pre>
 * final JSONReader reader = JSONReader.from(new File("/tmp/text.json"));
 * </pre>
 * <p> 1、Read complete stream
 * <pre>
 *     reader.read();
 *     Object result = reader.getResult(); (map或者list)
 * </pre>
 * 2、On demand read stream
 * <p> 构造ReaderCallback时指定模式： ReadParseMode.ExternalImpl
 * <pre>
 *         reader.read(new JSONReader.ReaderCallback(JSONReader.ReadParseMode.ExternalImpl) {
 *
 *             public void parseValue(String key, Object value, Object host, int elementIndex, String path) throws Exception {
 *                 if(path.equals("/features/[100000]/properties/STREET")) {
 *                     System.out.println(value);
 *                     abort();
 *                 }
 *             }
 *         }, true);
 * </pre>
 * <p>
 * <p> 调用 abort() 可以随时终止流读取
 *
 * @author wangyunchao
 * @see ReaderCallback
 * @see JSONReader#JSONReader(InputStream)
 * @see JSONReader#JSONReader(InputStream, String)
 * @see JSONReader#JSONReader(Reader)
 * @see JSON
 * @see JSONNode
 * @see JSONCharArrayWriter
 */
public class JSONReader extends JSONGeneral {

    /**
     * 字符流读取器
     */
    private final Reader reader;

    /**
     * 整个流中当前指针位置（绝对位置）
     */
    protected int pos;

    /**
     * 缓冲字符数组
     */
    private char[] buf;

    /**
     * 缓冲容量
     */
    protected int bufferSize = DIRECT_READ_BUFFER_SIZE;

    /**
     * 缓冲字符数组实际可读取的长度
     */
    protected int count;

    /**
     * 缓冲字符数组当前读取位置(相对位置)
     */
    protected int offset;

    /**
     * 当前字符
     */
    protected int current;

    // 回调句柄
    private ReaderCallback callback;

    /**
     * 解析配置上下文
     */
    private final JSONParseContext parseContext = new JSONParseContext();
    private ReadOption[] readOptions = new ReadOption[0];

    /**
     * 反射类型，如果没有指定，解析时动态指定
     */
    private GenericParameterizedType genericType;

    // 解析结果
    private Object result;

    // 读取中状态
    private boolean reading;

    // 是否已关闭（流）
    private boolean closed;

    // 是否终止
    private boolean aborted;

    // 临时字符串构建器
    protected final StringBuilder bufferWriter = new StringBuilder();
    protected int readingOffset = -1;

    // 锁(也可以使用CountDownLatch)
    private volatile Object lock = new Object();
    // 是否异步标识
    private boolean async;
    // 默认超时60s
    private long timeout = 60000;
    // 当前运行的线程id
    private long currentThreadId;

    /**
     * 通过文件对象构建json流读取器
     *
     * @param file
     */
    private JSONReader(File file) throws FileNotFoundException {
        this(new FileReader(file));
    }

    /**
     * 通过文件源构建读取器 (Building a JSON stream reader from a file object)
     *
     * @param file
     * @return
     */
    public static JSONReader from(File file) {
        try {
            return new JSONReader(file);
        } catch (FileNotFoundException e) {
            throw new JSONException(e);
        }
    }

    /**
     * 通过流对象构建json流读取器 (Building a JSON stream reader from a stream object)
     *
     * @param inputStream
     * @return
     */
    public static JSONReader from(InputStream inputStream) {
        return new JSONReader(inputStream);
    }

    /**
     * 通过字符串构建
     *
     * @param json
     * @return
     */
    public static JSONReader from(String json) {
        return new JSONReader(getChars(json));
    }

    /**
     * 通过字符数组构建
     *
     * @param source
     * @return
     */
    public static JSONReader from(char[] source) {
        return new JSONReader(source);
    }

    /**
     * 通过字符数组构建
     *
     * @param buf
     */
    public JSONReader(char[] buf) {
        this.buf = buf;
        this.count = buf.length;
        this.reader = null;
    }

    JSONReader() {
        this.reader = null;
    }

    /**
     * 通过流对象构建json流读取器 (Building a JSON stream reader from a stream object)
     *
     * @param inputStream
     */
    public JSONReader(InputStream inputStream) {
        this.reader = new InputStreamReader(inputStream);
    }

    /**
     * 通过流对象构建json流读取器 (Building a JSON stream reader from a stream object)
     *
     * @param inputStream
     * @param buffSize    缓冲大小
     */
    public JSONReader(InputStream inputStream, int buffSize) {
        this.reader = new InputStreamReader(inputStream);
        this.bufferSize = buffSize;
    }


    /**
     * 通过流对象构建json流读取器 (Building a JSON stream reader from a stream object)
     *
     * @param inputStream
     * @param charsetName 指定编码
     */
    public JSONReader(InputStream inputStream, String charsetName) {
        Reader reader;
        try {
            reader = new InputStreamReader(inputStream, charsetName);
        } catch (UnsupportedEncodingException e) {
            throw new UnsupportedOperationException(e);
        }
        this.reader = reader;
    }

    /**
     * 直接通过reader构建
     *
     * @param reader
     */
    public JSONReader(Reader reader) {
        if (reader == null) {
            throw new UnsupportedOperationException("reader is null");
        }
        this.reader = reader;
    }

    /***
     * 设置解析配置项
     *
     * @param readOptions
     */
    public void setOptions(ReadOption... readOptions) {
        JSONOptions.readOptions(this.readOptions = readOptions, parseContext);
    }

    /**
     * 指定超时时间单位毫秒 (timeout)
     *
     * @param timeout
     */
    public void setTimeout(long timeout) {
        this.timeout = timeout;
    }

    /**
     * 以默认模式读取 (default read)
     * <p> 返回result
     */
    public Object read() {
        try {
            this.readBuffer();
            if (this.isCompleted()) {
                return JSONDefaultParser.parse(null, buf, 0, count, null, readOptions);
            }
            this.defaultRead();
        } catch (Exception e) {
            throw new JSONException(e);
        } finally {
            this.close();
        }
        return result;
    }

    /**
     * 以默认模式读取 (default read)
     * <p> 读取完成可以调用getResult获取结果
     *
     * @param async 是否异步
     */
    public void read(boolean async) {
        read(new ReaderCallback(), async);
    }

    /**
     * 以默认模式读取 (default read)
     * <p> 返回result
     */
    public Object readAsResult(Class<?> actualType) {
        return readAsResult(GenericParameterizedType.actualType(actualType));
    }

    /**
     * 以默认模式读取 (default read)
     * <p> 返回result
     */
    public <T> T readAsResult(GenericParameterizedType<T> genericType) {
        this.genericType = genericType;
        this.executeReadStream();
        return (T) result;
    }

    /**
     * 读取
     *
     * @param callback 回调句柄
     */
    public void read(ReaderCallback callback) {
        read(callback, false);
    }

    /**
     * 读取
     *
     * @param callback 回调句柄
     * @param async    是否异步
     */
    public void read(ReaderCallback callback, boolean async) {
        checkReadState();
        this.callback = callback;
        this.reading = true;
        if (!async) {
            // 以阻塞模式下读取
            this.executeReadStream();
        } else {
            // 如果需要频繁的调用,可以使用Executors.newCachedThreadPool()在外部自行实现异步处理
            this.async = true;
            // 记录当前线程id
            this.currentThreadId = Thread.currentThread().getId();
            new Thread(new Runnable() {
                public void run() {
                    executeReadStream();
                }
            }).start();
        }
    }

    private void checkReadState() {
        if (this.closed) {
            throw new UnsupportedOperationException("Stream has been closed");
        }
        if (this.aborted) {
            throw new UnsupportedOperationException("Reader has been aborted");
        }
        if (this.reading) {
            throw new UnsupportedOperationException("Stream is being read");
        }
    }

    private void executeReadStream() {
        try {
            this.readBuffer();
            this.beginReadWithType();
        } catch (Exception e) {
            throw new JSONException(e);
        } finally {
            close();
            this.reading = false;
            this.closed = true;
            unlock();
        }
    }

    private void readBuffer() throws IOException {
        if (reader == null) return;
        if (buf == null) {
            buf = new char[bufferSize];
        }
        if (this.readingOffset > -1) {
            if (bufferSize > this.readingOffset) {
                this.bufferWriter.append(buf, this.readingOffset, bufferSize - this.readingOffset);
            }
            // reset
            this.readingOffset = 0;
        }
        count = reader.read(buf);
        offset = 0;
    }

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

    /**
     * 开始读取
     */
    private void defaultRead() throws Exception {
        clearWhitespaces();
        switch (current) {
            case '{':
                this.result = this.readObject();
                break;
            case '[':
                this.result = this.readArray();
                break;
            default:
                throw new UnsupportedOperationException("Character stream start character error. Only object({) or array([) parsing is supported");
        }
        // clear white space characters
        clearWhitespaces();
        if (current > -1) {
            throw new JSONException("Syntax error, extra characters found, '" + (char) current + "', pos " + pos);
        }
    }

    /**
     * 开始读取
     */
    private void beginReadWithType() throws Exception {
        clearWhitespaces();
        switch (current) {
            case '{':
                // 对象解析
                this.checkAutoGenericObjectType();
                this.result = this.readObject("", genericType);
                break;
            case '[':
                // 数组解析
                this.checkAutoGenericCollectionType();
                this.result = this.readArray("", genericType);
                break;
            default:
                throw new UnsupportedOperationException("Character stream start character error. Only object({) or array([) parsing is supported");
        }
        // clear white space characters

        if (isAborted()) return;

        clearWhitespaces();
        if (current > -1) {
            throw new JSONException("Syntax error, extra characters found, '" + (char) current + "', pos " + pos);
        }

        // 执行回调
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
                this.genericType = GenericParameterizedType.collectionType(ArrayList.class, actualType);
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
                // 暂且不考虑key值中存在转义字符\
                while (readNext() > -1 && current != '"') ;
                // 去掉当前字符（结束 "）
                key = endReadingAsString(-1);
                // 解析value
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
                            // 将字符串转化为指定类型
                            value = this.readString();
                            instance.put(key, value);
                            break;
                        case 'n':
                            // 读取null
                            this.readNull();
                            instance.put(key, null);
                            break;
                        case 't':
                            // 读取null
                            this.readTrue();
                            instance.put(key, true);
                            break;
                        case 'f':
                            // 读取null
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
                    // 是否为逗号或者}
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
        if (parseContext.useBigDecimalAsDefault) {
            this.beginCurrent();
            while (readNext() > -1) {
                if (current == ',' || current == endSyntax) {
                    this.endReading(-1);
                    // 前置空白已清除
                    int len = bufferWriter.length();
                    // 去除后置空白
                    while (bufferWriter.charAt(len - 1) <= ' ') {
                        len--;
                    }
                    char[] digits = new char[len];
                    bufferWriter.getChars(0, len, digits, 0);
                    return new BigDecimal(digits, 0, digits.length);
                }
            }
            throw new JSONException("Syntax error, the closing symbol '" + endSyntax + "' is not found, end pos: " + pos);
        } else {
            // append current
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
            // init integer type
            int mode = 0;
            // number suffix
            int specifySuffix = 0;

            do {
                while (isDigit(current)) {
                    value = (value << 3) + (value << 1) + current - 48;
                    readNext();
                }
                if (current == '.') {
                    // 小数点模式
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
                double doubleVal = value;
                expValue = expNegative ? -expValue - decimalCount : expValue - decimalCount;
                if (expValue > 0) {
                    double powValue = getDecimalPowerValue(expValue); // Math.pow(radix, expValue);
                    doubleVal *= powValue;
                } else if (expValue < 0) {
                    double powValue = getDecimalPowerValue(-expValue);// Math.pow(radix, -expValue);
                    doubleVal /= powValue;
                }
                doubleVal = negative ? -doubleVal : doubleVal;
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
                    // 读取null
                    this.readNull();
                    arrInstance.add(null);
                    break;
                case 't':
                    // 读取null
                    this.readTrue();
                    arrInstance.add(true);
                    break;
                case 'f':
                    // 读取null
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
     * 读取对象 （read object）
     * 当读取到流结束或者遇到}字符结束 (When the end of the stream is read or the end of the} character is encountered)
     *
     * @throws Exception
     */
    private Object readObject(String path, GenericParameterizedType genericType) throws Exception {

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
                    Class<? extends Map> mapCls = (Class<? extends Map>) actualType;
                    assignableFromMap = true;
                    instance = mapInstane = JSONDefaultParser.createMapInstance(mapCls);
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
                                // 将字符串转化为指定类型
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
                    // 是否为逗号或者}
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
                // 将字符串转化为指定类型
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
                    // 是否为逗号或者}
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
            // 是否为逗号或者]
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

    protected void beginReading(int n) {
        bufferWriter.setLength(0);
        this.readingOffset = offset + n;
    }

    protected String endReadingAsString(int n) {
        if (bufferWriter.length() > 0) {
            endReading(n);
            return bufferWriter.toString();
        } else {
            int endIndex = offset + n;
            String result = new String(buf, this.readingOffset, endIndex - this.readingOffset);
            this.readingOffset = -1;
            return result;
        }
    }

    private void endReading(int n) {
        endReading(n, -1);
    }

    /**
     * @param n         End offset correction position
     * @param newOffset
     */
    protected void endReading(int n, int newOffset) {
        int endIndex = offset + n;
        if (endIndex > this.readingOffset) {
            this.bufferWriter.append(buf, this.readingOffset, endIndex - this.readingOffset);
        }
        this.readingOffset = newOffset;
    }

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

    private void abortRead() {
        this.aborted = true;
    }

    public boolean isAborted() {
        return aborted;
    }

    private Object readArray(String path, GenericParameterizedType genericType) throws Exception {

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
            // 是否为逗号或者]
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
    protected final void throwUnexpectedException() {
        throw new JSONException("Syntax error, unexpected '" + (char) current + "', position " + pos);
    }

    /**
     * 从当前字符开始（包含当前字符）
     */
    protected void beginCurrent() {
        // 每次读完字符，offset会+1, current的实际位置是offset - 1
        this.beginReading(-1);
    }

    protected String readString() throws Exception {
        // reset StringBuilder
        this.beginReading(0);
        char prev = '\0';
        while (readNext() > -1) {
            if (prev == '\\') {
                // buf因为分批读取的原因，如果当前批次最后一个字符为转义符\\，readNext()时转义符会被写到writer中需要清掉
                if (offset == 1) {
                    // remove \\
                    int bufferLen = bufferWriter.length();
                    bufferWriter.setLength(bufferLen - 1);
                }
                switch (current) {
                    case '"':
                        // Skip \\ and current
                        this.endReading(-2, offset);
                        bufferWriter.append('\"');
                        break;
                    case 'n':
                        // Skip \\ and current
                        this.endReading(-2, offset);
                        bufferWriter.append('\n');
                        break;
                    case 'r':
                        // Skip \\ and current
                        this.endReading(-2, offset);
                        bufferWriter.append('\r');
                        break;
                    case 't':
                        // Skip \\ and current
                        this.endReading(-2, offset);
                        bufferWriter.append('\t');
                        break;
                    case 'b':
                        // Skip \\ and current
                        this.endReading(-2, offset);
                        bufferWriter.append('\b');
                        break;
                    case 'f':
                        // Skip \\ and current
                        this.endReading(-2, offset);
                        bufferWriter.append('\f');
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
                        bufferWriter.append((char) c);
                        break;
                    case '\\':
                        // Skip \\ and current
                        this.endReading(-2, offset);
                        bufferWriter.append('\\');
                        break;
                    default: {
                        // other case delete char '\\'
                        this.endReading(-2, offset);
                        bufferWriter.append((char) current);
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

    protected int readNext() throws Exception {
        pos++;
        if (offset < count) return current = buf[offset++];
        if (reader == null) {
            return current = -1;
        }
        if (count == bufferSize) {
            readBuffer();
            if (count == -1) return current = -1;
            return current = buf[offset++];
        } else {
            return current = -1;
        }
    }

    protected final int readNext(boolean check) throws Exception {
        readNext();
        if (check && current == -1) {
            close();
            throw new JSONException("Unexpected error, stream is end ");
        }
        return current;
    }

    /**
     * 清除空白字符直到读取到非空字符 （Clear white space characters until non empty characters）
     *
     * @throws IOException
     */
    private void clearWhitespaces() throws Exception {
        while (readNext() > -1 && current <= ' ') ;
    }

    /**
     * 外部实现
     */
    private boolean isExternalImpl() {
        return this.callback != null && this.callback.readParseMode == ReadParseMode.ExternalImpl;
    }

    /**
     * 是否读取完成
     *
     * @return
     */
    protected boolean isCompleted() {
        return reader == null || count < bufferSize;
    }

    /**
     * 返回解析的结果
     *
     * @return
     */
    public Object getResult() {
        return getResult(timeout);
    }

    /**
     * 返回解析的结果
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
     * 关闭流
     */
    public void close() {
        try {
            if (reader != null) {
                reader.close();
                this.closed = true;
            }
            bufferWriter.setLength(0);
        } catch (IOException e) {
            throw new UnsupportedOperationException(e);
        }
    }

    public enum ReadParseMode {
        /**
         * 内置解析
         * <p> 解析结果为Map或者List
         */
        BuiltParse,

        /***
         * 外部实现
         *
         * <p> 将内容抛给实现者自定义处理
         *
         */
        ExternalImpl
    }

    /***
     * 通过回调(订阅)模式响应解析过程 (Response parsing process through callback (subscription) mode)
     * 钩子模式，非异步调用 （Hook mode, non asynchronous call）
     *
     */
    public static class ReaderCallback {

        // 读取解析模式 (Read parsing mode)
        private final ReadParseMode readParseMode;
        private boolean abored;

        /**
         * 默认内部解析模式
         * 即读取到流结束后返回给使用者
         */
        public ReaderCallback() {
            this(ReadParseMode.BuiltParse);
        }

        public ReaderCallback(ReadParseMode readParseMode) {
            this.readParseMode = readParseMode;
        }

        /**
         * 触发场景: {} 和 []结构内容开启时被调用
         *
         * <p> 解析子路径时将作为宿主传入
         * <p> 返回值在上一级解析时作为value传入
         *
         * @param path json路径
         * @param type 1 {}; 2 []
         * @return 实例对象
         * @throws Exception
         */
        public Object created(String path, int type) throws Exception {
            return null;
        }

        /**
         * 提供给实现者解析
         * <p> 当readMode为ReadParseMode.ExternalImpl时有效
         *
         * @param key          当解析对象时为对象的key值，否则为null
         * @param value        对象/数组/字符串/number
         * @param host         宿主对象
         * @param elementIndex 当数组集合时标记当前的位置，否则-1
         * @param path         json路径
         * @throws Exception
         */
        public void parseValue(String key, Object value, Object host, int elementIndex, String path) throws Exception {
        }

        /**
         * 解析完成回调
         *
         * @param result
         */
        protected void complete(Object result) {
        }

        /**
         * 终止读取操作
         */
        protected final void abort() {
            this.abored = true;
        }

        final boolean isAbored() {
            return abored;
        }
    }
}
