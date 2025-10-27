package io.github.wycst.wast.json;

public class JSONSchemaResult {

    public static final JSONSchemaResult SUCCESS = new JSONSchemaResult(true, null);
    public static final JSONSchemaResult SUCCESS_SKIP = new JSONSchemaResult(true, "skip");
    public static final JSONSchemaResult FAILURE = new JSONSchemaResult(false, "failure");
    public static final JSONSchemaResult TYPE_NOT_MATCH = new JSONSchemaResult(false, "type not match");

    private final boolean success;
    private final String message;
    private final String path;

    public JSONSchemaResult(boolean success, String message) {
        this(success, message, null);
    }

    public JSONSchemaResult(boolean success, String message, String path) {
        this.success = success;
        this.message = message;
        this.path = path;
    }

    public static JSONSchemaResult fail(String message) {
        return new JSONSchemaResult(false, message);
    }

    public static JSONSchemaResult fail(String message, String path) {
        return new JSONSchemaResult(false, message, path);
    }

    public boolean isSuccess() {
        return success;
    }

    public String getMessage() {
        return message;
    }

    public String getPath() {
        return path;
    }

    @Override
    public String toString() {
        if (success) {
            return "JSONSchemaResult{success=true}";
        }
        if (path == null) {
            return "JSONSchemaResult{" +
                    "success=" + success +
                    ", message='" + message + '\'' +
                    '}';
        }
        return "JSONSchemaResult{" +
                "success=" + success +
                ", message='" + message + '\'' +
                ", path='" + path + '\'' +
                '}';
    }
}
