package io.github.wycst.wast.clients.http.provider;

import java.io.InputStream;
import java.util.Properties;

public interface FetchPropertiesCallback {
    void loadProperties(InputStream is);

    void loadProperties(Properties properties);
}
