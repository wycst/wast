package io.github.wycst.wast.clients.http.ssl;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLEngine;
import java.security.KeyStore;
import java.security.Security;
 
public class HttpSslContextFactory {
//    private static final String PROTOCOL = "SSLv2";
    private static final String PROTOCOL = "SSLv3";//客户端可以指明为SSLv3或者TLSv1.2
    /**针对于服务器端配置*/
    private static SSLContext sslContext = null;
    static {
        String algorithm = Security
                .getProperty("ssl.KeyManagerFactory.algorithm");
        if (algorithm == null) {
            algorithm = "SunX509";
        }
        SSLContext serverContext = null;
        try {
            KeyStore ks = KeyStore.getInstance("JKS");
            ks.load(HttpsKeyStore.getKeyStoreStream(), HttpsKeyStore.getKeyStorePassword());
            KeyManagerFactory kmf = KeyManagerFactory.getInstance(algorithm);
            kmf.init(ks, HttpsKeyStore.getCertificatePassword());
            serverContext = SSLContext.getInstance(PROTOCOL);
            serverContext.init(kmf.getKeyManagers(), null, null);
        } catch (Exception e) {
            System.out.println("初始化server SSL失败 "+ e);
            throw new Error("Failed to initialize the server SSLContext", e);
        }
        sslContext = serverContext;
    }
    public static SSLEngine createSSLEngine() {
        SSLEngine sslEngine = sslContext.createSSLEngine();
        sslEngine.setUseClientMode(false);
        sslEngine.setNeedClientAuth(false);
        return sslEngine ;
    }
}