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

import io.github.wycst.wast.json.exceptions.JSONException;

import java.io.*;

/**
 * 1,JSON parsing based on stream (character stream):
 * <p>
 *  Large file JSON file parsing (unlimited file size reading), no need to read stream content into memory for parsing. <br>
 *  Can be terminated as needed. <br>
 *  Supports asynchronous. <br>
 * </p>
 * <br>
 * 2, The streaming content needs to strictly adhere to the JSON specification.
 *
 * <br>
 * <br>
 * Example:
 * <pre>
 * final JSONReader reader = JSONReader.from(new File("/tmp/text.json"));
 * </pre>
 * <p> 1、Read complete stream
 * <pre>
 *     reader.read();
 *     Object result = reader.getResult(); (map or list)
 * </pre>
 * 2、On demand read stream
 * <p> Specify pattern when constructing ReaderCallback
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
 * <p> Calling abort() can terminate stream read at any time
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
public class JSONReader extends JSONAbstractReader {

    /**
     * Character stream reader
     */
    final Reader reader;

    /**
     * Buffered character array
     */
    private char[] buf;

    /**
     * Building a JSON stream reader from a file object
     *
     * @param file
     */
    private JSONReader(File file) throws FileNotFoundException {
        this(new FileReader(file));
    }

    /**
     * Building a JSON stream reader from a file object
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
     * Building a JSON stream reader from a stream object
     *
     * @param inputStream
     * @return
     */
    public static JSONReader from(InputStream inputStream) {
        return new JSONReader(inputStream);
    }

    /**
     * Build through strings
     *
     * @param json
     * @return
     */
    public static JSONReader from(String json) {
        return new JSONReader(getChars(json));
    }

    /**
     * Build through character arrays
     *
     * @param source
     * @return
     */
    public static JSONReader from(char[] source) {
        return new JSONReader(source);
    }

    /**
     * Build through character arrays
     *
     * @param buf
     */
    public JSONReader(char[] buf) {
        this.buf = buf;
        this.count = buf.length;
        this.reader = null;
    }

    /**
     * Building a JSON stream reader from a stream object
     *
     * @param inputStream
     */
    public JSONReader(InputStream inputStream) {
        this.reader = new InputStreamReader(inputStream);
    }

    /**
     * Building a JSON stream reader from a stream object
     *
     * @param inputStream
     * @param buffSize
     */
    public JSONReader(InputStream inputStream, int buffSize) {
        this.reader = new InputStreamReader(inputStream);
        this.bufferSize = buffSize;
    }


    /**
     * Building a JSON stream reader from a stream object
     *
     * @param inputStream
     * @param charsetName
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
     * Build directly through the reader
     *
     * @param reader
     */
    public JSONReader(Reader reader) {
        if (reader == null) {
            throw new UnsupportedOperationException("reader is null");
        }
        this.reader = reader;
    }

    public Object read() {
        try {
            this.readBuffer();
            if (!multiple && this.isCompleted()) {
                return JSONDefaultParser.parseInternal(null, buf, 0, count, null, readOptions);
            }
            this.defaultRead();
        } catch (Exception e) {
            throw new JSONException(e);
        } finally {
            this.tryCloseReader();
        }
        return result;
    }

    protected void readBuffer() throws IOException {
        if (reader == null) return;
        if (buf == null) {
            buf = new char[bufferSize];
        }
        if (this.readingOffset > -1) {
            // put all the remaining unread content in the builder
            if (bufferSize > this.readingOffset) {
                this.writer.append(buf, this.readingOffset, bufferSize - this.readingOffset);
            }
            // reset
            this.readingOffset = 0;
        }
        if (offset >= count) {
            count = reader.read(buf);
            offset = 0;
        }
    }

    protected String endReadingAsString(int n) {
        if (writer.length() > 0) {
            endReading(n);
            return writer.toString();
        } else {
            int endIndex = offset + n;
            String result = new String(buf, this.readingOffset, endIndex - this.readingOffset);
            this.readingOffset = -1;
            return result;
        }
    }

    /**
     * @param n         End offset correction position
     * @param newOffset
     */
    protected void endReading(int n, int newOffset) {
        int endIndex = offset + n;
        if (endIndex > this.readingOffset) {
            this.writer.append(buf, this.readingOffset, endIndex - this.readingOffset);
        }
        this.readingOffset = newOffset;
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
            // synchronous reading, when the count is less than bufferSize, it must be read completely
            return current = -1;
        }
    }

    /**
     * whether the reading completed
     *
     * @return
     */
    protected boolean isCompleted() {
        return reader == null || count < bufferSize || completed;
    }

    /**
     * close stream
     */
    public void close() {
        try {
            if (reader != null) {
                reader.close();
            }
        } catch (IOException e) {
        } finally {
            this.closed = true;
        }
    }
}
