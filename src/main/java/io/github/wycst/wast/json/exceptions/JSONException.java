package io.github.wycst.wast.json.exceptions;

public class JSONException extends RuntimeException {
    private static final long serialVersionUID = 1L;

    public JSONException() {
    }

    public JSONException(String message) {
        super(message);
    }

    public JSONException(Throwable cause) {
        super(cause);
    }

    public JSONException(String message, Throwable cause) {
        super(message, cause);
    }
}
