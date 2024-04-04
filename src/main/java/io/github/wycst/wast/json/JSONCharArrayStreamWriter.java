package io.github.wycst.wast.json;

import io.github.wycst.wast.common.reflect.UnsafeHelper;
import io.github.wycst.wast.common.utils.EnvUtils;
import io.github.wycst.wast.common.utils.IOUtils;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.Charset;

/**
 * supported os
 *
 * @Date 2024/3/10 11:52
 * @Created by wangyc
 */
class JSONCharArrayStreamWriter extends JSONCharArrayWriter {

    protected final Charset charset;

    JSONCharArrayStreamWriter(Charset charset) {
        // outputStream is cannot be null
        this.charset = charset == null ? Charset.defaultCharset() : charset;
    }

    @Override
    protected void toOutputStream(OutputStream os) throws IOException {
        boolean isByteArrayOs = os.getClass() == ByteArrayOutputStream.class;
        boolean emptyByteArrayOs = false;
        if (isByteArrayOs) {
            ByteArrayOutputStream byteArrayOutputStream = (ByteArrayOutputStream) os;
            emptyByteArrayOs = byteArrayOutputStream.size() == 0;
        }
        String source = new String(buf, 0, count);
        byte[] bytes = (byte[]) UnsafeHelper.getStringValue(source);
        if (bytes.length == count) {
            if (emptyByteArrayOs) {
                UnsafeHelper.getUnsafe().putObject(os, UnsafeHelper.BAO_BUF_OFFSET, bytes);
                UnsafeHelper.getUnsafe().putInt(os, UnsafeHelper.BAO_COUNT_OFFSET, count);
            } else {
                os.write(bytes, 0, count);
            }
        } else {
            if (charset == EnvUtils.CHARSET_UTF_8) {
                byte[] output = new byte[count * 3];
                int length = IOUtils.encodeUTF8(buf, 0, count, output);
                if (emptyByteArrayOs) {
                    UnsafeHelper.getUnsafe().putObject(os, UnsafeHelper.BAO_BUF_OFFSET, output);
                    UnsafeHelper.getUnsafe().putInt(os, UnsafeHelper.BAO_COUNT_OFFSET, length);
                } else {
                    os.write(output, 0, length);
                }
            } else {
                bytes = source.getBytes(charset);
                if (emptyByteArrayOs) {
                    UnsafeHelper.getUnsafe().putObject(os, UnsafeHelper.BAO_BUF_OFFSET, bytes);
                    UnsafeHelper.getUnsafe().putInt(os, UnsafeHelper.BAO_COUNT_OFFSET, bytes.length);
                } else {
                    os.write(bytes);
                }
            }
        }
        os.flush();
    }
}
