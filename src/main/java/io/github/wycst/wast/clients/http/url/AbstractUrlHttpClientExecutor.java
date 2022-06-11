package io.github.wycst.wast.clients.http.url;

import io.github.wycst.wast.clients.http.executor.HttpClientExecutor;
import io.github.wycst.wast.clients.http.ssl.X509TrustManagerImpl;

import javax.net.ssl.*;
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

}
