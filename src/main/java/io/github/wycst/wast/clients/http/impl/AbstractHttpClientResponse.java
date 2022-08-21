package io.github.wycst.wast.clients.http.impl;

import io.github.wycst.wast.clients.http.definition.HttpClientResponse;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;

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
            try {
                if (contentLength > 0) {
                    // read max length(${contentLength})
                    byte[] buf = new byte[contentLength];
                    int len = is.read(buf);
                    if (len < contentLength) {
                        content = Arrays.copyOf(buf, len);
                    } else {
                        content = buf;
                    }
                } else {
                    // read full is
                    ByteArrayOutputStream os = new ByteArrayOutputStream();
                    byte[] buf = new byte[1024];
                    int len;
                    while ((len = is.read(buf)) > 0) {
                        os.write(buf, 0, len);
                    }
                    this.content = os.toByteArray();
                }
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                try {
                    is.close();
                } catch (IOException e) {
                }
            }
        }
    }

    private boolean checkIfRead(ByteArrayOutputStream os) {
        if (this.contentLength == -1) {
            return true;
        }
        return os.size() < this.contentLength;
    }

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
