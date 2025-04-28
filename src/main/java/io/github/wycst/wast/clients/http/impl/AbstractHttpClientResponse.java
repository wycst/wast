package io.github.wycst.wast.clients.http.impl;

import io.github.wycst.wast.clients.http.definition.HttpClientResponse;

import java.io.InputStream;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * @Author: wangy
 * @Date: 2020/7/7 19:40
 * @Description:
 */
abstract class AbstractHttpClientResponse implements HttpClientResponse {

    private final int status;
    private final String reasonPhrase;
    private final int contentLength;

    private byte[] content;
    private InputStream inputStream;

    private String contentType;
    private Map<String, List<String>> headers;

    public AbstractHttpClientResponse(int code, String reasonPhrase, InputStream is, int contentLength, String contentType, Map<String, List<String>> headers) {
        this.status = code;
        this.reasonPhrase = reasonPhrase;
        this.contentLength = contentLength;
        this.inputStream = is;
        this.contentType = contentType;
        this.headers = headers;
    }

    public AbstractHttpClientResponse(int code, String reasonPhrase, byte[] content, int contentLength, String contentType, Map<String, List<String>> headers) {
        this.status = code;
        this.reasonPhrase = reasonPhrase;
        this.contentLength = contentLength;
        this.content = content;
        this.contentType = contentType;
        this.headers = headers;
    }

    @Override
    public void setHeaders(Map<String, List<String>> responseHeaders) {
        this.headers = responseHeaders;
    }

    public final String getHeader(String name) {
        if (name == null) return null;
        Set<Map.Entry<String, List<String>>> entrySet = headers.entrySet();
        for (Map.Entry<String, List<String>> entry : entrySet) {
            if (name.equalsIgnoreCase(entry.getKey())) {
                List<String> values = entry.getValue();
                if (values != null && !values.isEmpty()) {
                    return values.get(0);
                } else {
                    return null;
                }
            }
        }
        return null;
    }

    public List<String> getHeaders(String name) {
        return headers.get(name);
    }

    public Set<String> getHeaderNames() {
        return headers.keySet();
    }

    @Override
    public void setContentType(String contentType) {
        this.contentType = contentType;
    }

    public String getContentType() {
        return contentType;
    }

//    public AbstractHttpClientResponse(int code, String reasonPhrase, byte[] content) {
//        this.status = code;
//        this.reasonPhrase = reasonPhrase;
//        this.content = content;
//        this.contentLength = content == null ? -1 : content.length;
//    }

//    protected void writeStreamToContent(InputStream is) {
//        if (is != null) {
//            boolean isClosed = false;
//            try {
//                if (contentLength > 0) {
//                    byte[] buf = new byte[contentLength];
//                    int offset = 0, len = buf.length, count;
//                    // Read until -1 or offset = contentLength
//                    while ((count = is.read(buf, offset, len)) != -1) {
//                        offset += count;
//                        if (count == len) break;
//                        len -= count;
//                    }
//                    if(offset != contentLength) {
//                        // The actual length of the read content is less than the content length declared in the header
//                        // whether to throw an error
//                    }
//                    content = buf;
//                } else {
//                    this.content = IOUtils.readBytes(is);
//                    isClosed = true;
//                }
//            } catch (IOException e) {
//                e.printStackTrace();
//            } finally {
//                try {
//                    if (!isClosed) {
//                        is.close();
//                    }
//                } catch (IOException e) {
//                }
//            }
//        }
//    }

//    private boolean checkIfRead(ByteArrayOutputStream os) {
//        if (this.contentLength == -1) {
//            return true;
//        }
//        return os.size() < this.contentLength;
//    }

    public int status() {
        return status;
    }

    public String reasonPhrase() {
        return reasonPhrase;
    }

    public byte[] content() {
        return content;
    }

    public int length() {
        return content == null ? -1 : content.length;
    }

    @Override
    public long readContentLength() {
        if (headers == null) return contentLength;
        try {
            String value = getHeader("content-length");
            return Long.parseLong(value);
        } catch (Throwable throwable) {
            return contentLength;
        }
    }

    public InputStream inputStream() {
        return inputStream;
    }
}
