package io.github.wycst.wast.clients.http.impl;

import io.github.wycst.wast.clients.http.definition.HttpClientResponse;
import io.github.wycst.wast.common.utils.IOUtils;

import java.io.IOException;
import java.io.InputStream;

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

    public AbstractHttpClientResponse(int code, String reasonPhrase, InputStream is, int contentLength) {
        this.status = code;
        this.reasonPhrase = reasonPhrase;
        this.contentLength = contentLength;
        writeStreamToContent(is);
    }

    public AbstractHttpClientResponse(int code, String reasonPhrase, byte[] content) {
        this.status = code;
        this.reasonPhrase = reasonPhrase;
        this.content = content;
        this.contentLength = content == null ? -1 : content.length;
    }

    private void writeStreamToContent(InputStream is) {
        if (is != null) {
            boolean isClosed = false;
            try {
                if (contentLength > 0) {
                    byte[] buf = new byte[contentLength];
                    int offset = 0, len = buf.length, count;
                    // Read until -1 or offset = contentLength
                    while ((count = is.read(buf, offset, len)) != -1) {
                        offset += count;
                        if (count == len) break;
                        len -= count;
                    }
                    if(offset != contentLength) {
                        // The actual length of the read content is less than the content length declared in the header
                        // whether to throw an error
                    }
                    content = buf;
                } else {
                    this.content = IOUtils.readBytes(is);
                    isClosed = true;
                }
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                try {
                    if (!isClosed) {
                        is.close();
                    }
                } catch (IOException e) {
                }
            }
        }
    }

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
}
