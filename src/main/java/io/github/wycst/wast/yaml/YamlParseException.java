package io.github.wycst.wast.yaml;

/**
 * @Author wangyunchao
 * @Date 2022/5/20 15:14
 */
public class YamlParseException extends RuntimeException {

    public YamlParseException(String message) {
        super(message);
    }

    public YamlParseException(String message, Throwable cause) {
        super(message, cause);
    }

    public YamlParseException(Throwable cause) {
        super(cause);
    }
}
