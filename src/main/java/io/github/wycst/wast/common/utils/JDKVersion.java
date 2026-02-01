package io.github.wycst.wast.common.utils;

public final class JDKVersion {
    public static final float VERSION;

    static {
        float jdkVersion = 1.8f;
        try {
            // 规范版本号
            String version = System.getProperty("java.specification.version");
            if (version != null) {
                jdkVersion = Float.parseFloat(version);
            }
        } catch (Throwable throwable) {
        }
        VERSION = jdkVersion;
    }
}
