/*
 * Copyright [2020-2022] [wangyunchao]
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
package org.framework.light.json;

import org.framework.light.common.reflect.ClassStructureWrapper;
import org.framework.light.common.reflect.GenericParameterizedType;
import org.framework.light.common.reflect.SetterInfo;
import org.framework.light.common.tools.Base64;
import org.framework.light.json.exceptions.JSONException;
import org.framework.light.json.options.JSONParseContext;
import org.framework.light.json.options.Options;
import org.framework.light.json.options.ReadOption;

import java.io.*;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.*;

/**
 * 1,基于流的JSON解析:
 * <p> 超大文件json文件解析（文件大小读取无限制）,无需将流内容读取到内存中再解析
 * <p> 检测当前json类型是对象还是数组，其他类型处理无意义
 * <p> 线性解析模式（无回溯）
 * <p> 随时终止
 * <p> 支持异步
 * <br>
 * <p>
 * 2,Only strict JSON mode is supported:
 * <p> Attributes use double quotation marks ("), and key values use colons (:) for declarations
 * <p> Use commas (,) to split tokens
 *
 * <p>
 * for example:
 * <p>
 * final JSONReader reader = JSONReader.from(new File("/tmp/text.json"));
 * <p>
 * 1、Read complete stream
 * reader.read();
 * Object result = reader.getResult(); (map或者list)
 * <p>
 * 2、On demand read stream
 * <p>
 * 构造StreamReaderCallback时指定模式： ReadParseMode.ExternalImpl
 * <pre>
 *         reader.read(new JSONReader.StreamReaderCallback(JSONReader.ReadParseMode.ExternalImpl) {
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
 * Call abort() to terminate the reading of the stream at any time
 *
 * @author wangyunchao
 * @see StreamReaderCallback
 * @see JSONReader#JSONReader(InputStream)
 * @see JSONReader#JSONReader(InputStream, String)
 * @see JSONReader#JSONReader(BufferedReader)
 * @see JSON
 * @see JSONNode
 * @see JSONWriter
 */
public final class JSONReader extends JSONGeneral {

    /**
     * 字符流读取器
     */
    private final BufferedReader reader;

    /**
     * 整个流中当前指针位置（绝对位置）
     */
    private int pos;

    /**
     * 缓冲字符数组
     */
    private char[] buf;

    /**
     * 缓冲字符数组实际可读取的长度
     */
    private int count;

    /**
     * 缓冲字符数组当前读取位置
     */
    private int offset;

    /**
     * 当前字符
     */
    private int current;

    // 回调句柄
    private StreamReaderCallback callback;

    /**
     * 解析配置上下文
     */
    private final JSONParseContext parseContext = new JSONParseContext();

    /**
     * 反射类型，如果没有指定，解析时动态指定
     */
    private GenericParameterizedType genericType;

    // 解析结果
    private Object result;

    // 读取中状态
    private boolean reading;

    // 是否结束
    private boolean closed;

    // 是否终止
    private boolean aborted;

    // 临时
    private final StringBuilder templatWriter = new StringBuilder();

    // 锁(也可以使用CountDownLatch)
    private Object lock = new Object();
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
        this(new FileInputStream(file));
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

    public JSONReader(char[] buf) {
        this.buf = buf;
        this.count = buf.length;
        this.reader = null;
    }

    /**
     * 通过流对象构建json流读取器 (Building a JSON stream reader from a stream object)
     *
     * @param inputStream
     */
    public JSONReader(InputStream inputStream) {
        this.reader = new BufferedReader(new InputStreamReader(inputStream));
    }

    /**
     * 通过流对象构建json流读取器 (Building a JSON stream reader from a stream object)
     *
     * @param inputStream
     * @param charsetName 指定编码
     */
    public JSONReader(InputStream inputStream, String charsetName) {
        BufferedReader reader;
        try {
            reader = new BufferedReader(new InputStreamReader(inputStream, charsetName));
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
    public JSONReader(BufferedReader reader) {
        this.reader = reader;
    }

    /***
     * 设置解析配置项
     *
     * @param readOptions
     */
    public void setOptions(ReadOption... readOptions) {
        Options.readOptions(readOptions, parseContext);
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
        read(false);
        return result;
    }

    /**
     * 以默认模式读取 (default read)
     * <p> 读取完成可以调用getResult获取结果
     *
     * @param async 是否异步
     */
    public void read(boolean async) {
        read(new StreamReaderCallback(), async);
    }

    /**
     * 以默认模式读取 (default read)
     * <p> 返回result
     */
    public Object readAsResult(Class<?> actualType) {
        this.genericType = GenericParameterizedType.actualType(actualType);
        this.executeReadStream();
        return result;
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
    public void read(StreamReaderCallback callback) {
        read(callback, false);
    }

    /**
     * 读取
     *
     * @param callback 回调句柄
     * @param async    是否异步
     */
    public void read(StreamReaderCallback callback, boolean async) {
        if (this.closed) {
            throw new UnsupportedOperationException("Stream has been closed");
        }
        if (this.aborted) {
            throw new UnsupportedOperationException("Reader has been aborted");
        }
        if (this.reading) {
            throw new UnsupportedOperationException("Stream is being read");
        }
        this.callback = callback;
        this.reading = true;
        if (!async) {
            // 以阻塞模式下读取
            this.executeReadStream();
        } else {
            // 异步处理
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

    private void executeReadStream() {
        try {
            this.readBuffer();
            this.beginRead();
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
        if(reader == null) return;
        if (buf == null) {
            buf = new char[DIRECT_READ_BUFFER_SIZE];
        }
        count = reader.read(buf);
        offset = 0;
    }

    private void unlock() {
        synchronized (lock) {
            lock.notify();
        }
    }

    private void await() {
        await(timeout);
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
    private void beginRead() throws Exception {
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
        if (!externalImpl) {
            if (genericType != null) {
                Class<?> actualType = genericType.getActualType();
                if (actualType == null || actualType == Object.class
                        || actualType == Map.class || actualType == LinkedHashMap.class) {
                    instance = mapInstane = new LinkedHashMap<String, Object>();
                } else if (actualType == HashMap.class) {
                    instance = mapInstane = new HashMap<String, Object>();
                } else {
                    classStructureWrapper = ClassStructureWrapper.get(actualType);
                    if (classStructureWrapper == null) {
                        throw new UnsupportedOperationException("Class " + actualType + " is not supported ");
                    }
                    assignableFromMap = classStructureWrapper == null ? false : classStructureWrapper.isAssignableFromMap();
                    if (!assignableFromMap) {
                        instance = classStructureWrapper.newInstance();
                    } else {
                        instance = mapInstane = (Map) actualType.newInstance();
                    }
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
                // next "
                templatWriter.setLength(0);
                while (readNext() > -1) {
                    if (current != '"') {
                        templatWriter.append((char) current);
                    } else {
                        break;
                    }
                }
                key = templatWriter.toString();
                templatWriter.setLength(0);
                String nextPath = externalImpl ? path + "/" + key : null;
//                callback.isParse();

                // 解析value
                clearWhitespaces();
                if (current == ':') {
                    clearWhitespaces();
                    Object value;
                    boolean toBreakOrContinue = false;

                    GenericParameterizedType valueType = null;
                    SetterInfo setterInfo = null;
                    if (!externalImpl && !assignableFromMap) {
                        setterInfo = classStructureWrapper.getSetterInfo(key);
                        valueType = setterInfo.getGenericParameterizedType();
                    }

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
                            value = parseStringTo(this.readString(), valueType);
                            invokeValueOfObject(key, value, nextPath, externalImpl, assignableFromMap, mapInstane, instance, setterInfo);
                            break;
                        default:
                            // null, boolean, number
                            value = parseSimpleValueTo(this.readSimpleValue('}'), valueType);
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

    private Object parseSimpleValueTo(Object simpleValue, GenericParameterizedType valueType) {
        if (simpleValue == null) return null;
        Class<?> actualType = null;
        if (valueType == null || (actualType = valueType.getActualType()) == Object.class) {
            return simpleValue;
        }
        if (actualType.isInstance(simpleValue)) {
            return simpleValue;
        }
        if (Number.class.isAssignableFrom(actualType)) {
            Number numValue = (Number) simpleValue;
            if (actualType == int.class || actualType == Integer.class) {
                return numValue.intValue();
            } else if (actualType == int.class || actualType == Integer.class) {
                return numValue.intValue();
            } else if (actualType == float.class || actualType == Float.class) {
                return numValue.floatValue();
            } else if (actualType == long.class || actualType == Long.class) {
                return numValue.longValue();
            } else if (actualType == double.class || actualType == Double.class) {
                return numValue.doubleValue();
            } else if (actualType == byte.class || actualType == Byte.class) {
                return numValue.byteValue();
            } else if (actualType == BigDecimal.class) {
                return new BigDecimal(String.valueOf(numValue));
            } else if (actualType == BigInteger.class) {
                return new BigInteger(String.valueOf(numValue));
            }
        }
        return simpleValue;
    }

    private Object parseStringTo(String value, GenericParameterizedType valueType) throws Exception {
        if (value == null) return null;
        Class<?> actualType = null;
        if (valueType == null || (actualType = valueType.getActualType()) == String.class || actualType == Object.class) {
            return value;
        }
        char[] chars = getChars(value);
        if (actualType == CharSequence.class) {
            return value;
        } else if (Date.class.isAssignableFrom(actualType)) {
            char[] dateChars = new char[chars.length + 2];
            dateChars[0] = '"';
            System.arraycopy(chars, 0, dateChars, 1, chars.length);
            dateChars[chars.length + 1] = '"';
            String pattern = valueType.getDatePattern();
            String timezone = valueType.getDateTimezone();
            return parseDateValue(0, dateChars.length, dateChars, pattern, timezone, (Class<? extends Date>) actualType);
        } else if (actualType == byte[].class) {
            if (parseContext.isByteArrayFromHexString()) {
                return hexString2Bytes(chars, 0, chars.length);
            } else {
                return Base64.getDecoder().decode(value);
            }
        } else if (actualType == char[].class) {
            return chars;
        } else if (Enum.class.isAssignableFrom(actualType)) {
            try {
                Class enumCls = actualType;
                return Enum.valueOf(enumCls, value);
            } catch (RuntimeException exception) {
                if (parseContext.isUnknownEnumAsNull()) {
                    return null;
                } else {
                    throw exception;
                }
            }
        } else if (actualType == StringBuffer.class) {
            StringBuffer buffer = new StringBuffer();
            buffer.append(chars);
            return buffer;
        } else if (actualType == StringBuilder.class) {
            StringBuilder builder = new StringBuilder();
            builder.append(chars);
            return builder;
        } else if (actualType == Class.class) {
            return Class.forName(value);
        } else if (actualType == Character.class) {
            return chars[0];
        }
        return null;
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

            if (current == '{') {
                Object value = this.readObject(nextPath, valueType);
                this.parseCollectionElement(externalImpl, value, arrInstance, instance, elementIndex, nextPath);
            } else if (current == '[') {
                // 2 [ array
                Object value = this.readArray(nextPath, valueType);
                this.parseCollectionElement(externalImpl, value, arrInstance, instance, elementIndex, nextPath);
            } else if (current == '"') {
                // 3 string
                Object value = parseStringTo(this.readString(), valueType);
                this.parseCollectionElement(externalImpl, value, arrInstance, instance, elementIndex, nextPath);
            } else {
                // null, boolean, number
                Object value = parseSimpleValueTo(this.readSimpleValue(']'), valueType);
                toBreakOrContinue = true;
                this.parseCollectionElement(externalImpl, value, arrInstance, instance, elementIndex, nextPath);
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

    private void throwUnexpectedException() {
        throw new JSONException("Syntax error, unexpected token character '" + (char) current + "', position " + pos);
    }

    private Object readSimpleValue(char endSyntax) throws Exception {
        templatWriter.setLength(0);
        templatWriter.append((char) current);
        Object value = null;
        boolean matchEnd = false;
        while (readNext() > -1) {
            if (current == ',' || current == endSyntax) {
                String stringValue = templatWriter.toString().trim();
                boolean ifTrue;
                if (stringValue.equals("null")) {
                    value = null;
                } else if ((ifTrue = stringValue.equals("true")) || stringValue.equals("false")) {
                    value = ifTrue;
                } else {
                    int len = stringValue.length();
                    char[] digits = new char[len];
                    templatWriter.getChars(0, len, digits, 0);
                    value = parseNumber(digits, 0, len, parseContext.isUseBigDecimalAsDefault());
                }
                matchEnd = true;
                break;
            } else {
                templatWriter.append((char) current);
            }
        }
        if (!matchEnd) {
            throw new JSONException("Syntax error, the closing symbol '" + endSyntax + "' is not found, end pos: " + pos);
        }
        return value;
    }

    private String readString() throws Exception {
        // reset
        templatWriter.setLength(0);
        char prev = '\0';
        while (readNext() > -1) {
            if (prev == '\\') {
                switch (current) {
                    case '"':
                        templatWriter.append('\"');
                        break;
                    case 'n':
                        templatWriter.append('\n');
                        break;
                    case 'r':
                        templatWriter.append('\r');
                        break;
                    case 't':
                        templatWriter.append('\t');
                        break;
                    case 'b':
                        templatWriter.append('\b');
                        break;
                    case 'f':
                        templatWriter.append('\f');
                        break;
                    case 'u':
                        // find next 4 character
                        char[] hexDigits = new char[4];
                        for (int i = 0; i < 4; i++) {
                            hexDigits[i] = (char) readNext(true);
                        }
                        int c = parseInt(hexDigits, 0, 4, 16);
                        templatWriter.append((char) c);
                        break;
                    case '\\':
                        templatWriter.append('\\');
                        break;
                }
                prev = '\0';
                continue;
            }
            if (current == '"') {
                return templatWriter.toString();
            }
            templatWriter.append((char) current);
            prev = (char) current;
        }

        // maybe throw an exception
        throwUnexpectedException();
        return null/*templatWriter.toString()*/;
    }

    private int readNext() throws Exception {
        pos++;
        if (offset < count) return current = buf[offset++];

        if (reader == null) {
            return -1;
        }

        if (count == DIRECT_READ_BUFFER_SIZE) {
            readBuffer();
            if (count == -1) return current = -1;
            return current = buf[offset++];
        } else {
            return current = -1;
        }
//        return current = reader.read();
    }

    private int readNext(boolean check) throws Exception {
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
            templatWriter.setLength(0);
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
    public static class StreamReaderCallback {

        // 读取解析模式 (Read parsing mode)
        private final ReadParseMode readParseMode;
        private boolean abored;

        /**
         * 默认内部解析模式
         * 即读取到流结束后返回给使用者
         */
        public StreamReaderCallback() {
            this(ReadParseMode.BuiltParse);
        }

        public StreamReaderCallback(ReadParseMode readParseMode) {
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
