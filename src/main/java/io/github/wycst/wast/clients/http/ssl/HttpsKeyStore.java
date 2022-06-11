package io.github.wycst.wast.clients.http.ssl;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
 
public class HttpsKeyStore {
 
    public static InputStream getKeyStoreStream() {
        InputStream inStream = null;
        try {
            inStream = new FileInputStream(Arguments.keystorePath);
        } catch (FileNotFoundException e) {
            System.out.println("读取密钥文件失败 "+ e);
        }
        return inStream;
    }
 
    public static char[] getCertificatePassword() {
        return Arguments.certificatePassword.toCharArray();
    }
 
    public static char[] getKeyStorePassword() {
        return Arguments.keystorePassword.toCharArray();
    }
}