package io.github.wycst.wast.jdbc.exception;

@SuppressWarnings({"serial"})
public class ParameterException extends RuntimeException {

    public ParameterException(String message) {
        super(message);
    }

    public ParameterException(String message, Throwable cause) {
        super(message, cause);
    }

}
