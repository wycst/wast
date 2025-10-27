package io.github.wycst.wast.clients.http.url;

import io.github.wycst.wast.clients.http.executor.HttpClientExecutor;
import io.github.wycst.wast.clients.http.ssl.X509TrustManagerImpl;
import io.github.wycst.wast.common.utils.IOUtils;

import javax.net.ssl.*;
import java.io.IOException;
import java.io.InputStream;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;

/**
 * @Author: wangy
 * @Date: 2020/7/7 19:16
 * @Description:
 */
abstract class AbstractUrlHttpClientExecutor extends HttpClientExecutor {

    static {
        // -Dhttps.protocols=TLSv1,SSLv3
        if (System.getProperty("https.protocols") == null) {
            System.setProperty("https.protocols", "TLSv1,TLSv1.2,SSLv3");
        }
        try {
            SSLContext sslcontext = SSLContext.getInstance("SSL", "SunJSSE");
            TrustManager[] tm = {new X509TrustManagerImpl()};
            sslcontext.init(null, tm, new SecureRandom());
            HostnameVerifier ignoreHostnameVerifier = new HostnameVerifier() {
                public boolean verify(String arg0, SSLSession arg1) {
                    return true;
                }
            };
            HttpsURLConnection
                    .setDefaultHostnameVerifier(ignoreHostnameVerifier);
            HttpsURLConnection.setDefaultSSLSocketFactory(sslcontext
                    .getSocketFactory());

        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    protected static byte[] readInputStream(InputStream is, int contentLength) {
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
                    if (offset != contentLength) {
                        // The actual length of the read content is less than the content length declared in the header
                        // whether to throw an error
                    }
                    return buf;
                } else {
                    return IOUtils.readBytes(is);
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
        throw new RuntimeException("InputStream Empty error");
    }

}
