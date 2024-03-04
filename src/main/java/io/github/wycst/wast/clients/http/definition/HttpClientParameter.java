package io.github.wycst.wast.clients.http.definition;

import io.github.wycst.wast.common.utils.IOUtils;

import java.io.*;

/**
 * @Author: wangy
 * @Date: 2020/7/2 16:18
 * @Description:
 */
public class HttpClientParameter {

    private final String name;
    private final String value;

    private String contentType;
    private long contentLength;
    private File file;
    private byte[] bytes;
    private boolean fileUpload = false;

    HttpClientParameter(String name, String value) {
        this.name = name;
        this.value = value == null ? "" : value;
        // 注意这里指字节长度不是字符长度
        this.contentLength = this.value.getBytes().length;
        this.contentType = "text/plain; charset=UTF-8";
    }

    HttpClientParameter(String name, File file, String contentType) {
        this.name = name;
        this.value = file.getName();
        this.contentType = contentType;
        this.file = file;
        this.contentLength = file.length();
        this.fileUpload = true;
    }

    HttpClientParameter(String name, String fileName, byte[] bytes, String contentType) {
        this.name = name;
        this.value = fileName;
        this.contentType = contentType;
        this.bytes = bytes;
        this.contentLength = bytes.length;
        this.fileUpload = true;
    }

    public String getName() {
        return name;
    }

    public String getValue() {
        return value;
    }

    public String getContentType() {
        return contentType;
    }

    public long getContentLength() {
        return contentLength;
    }

    public boolean isFileUpload() {
        return fileUpload;
    }


    public void writeContentTo(OutputStream os) throws IOException {
        if (fileUpload) {
            if(file != null) {
                // 文件流
                InputStream is = new FileInputStream(file);
                bytes = IOUtils.readBytes(is);
                file = null;
                os.write(bytes);
//                byte[] buf = new byte[1024];
//                int size;
//                while ((size = is.read(buf)) > -1) {
//                    os.write(buf, 0, size);
//                }
//                is.close();
            } else {
                os.write(bytes);
            }
        } else {
            os.write(value.getBytes());
        }
    }
}
