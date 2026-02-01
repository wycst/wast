package io.github.wycst.wast.json;

import io.github.wycst.wast.json.annotations.JsonProperty;
import io.github.wycst.wast.json.options.ReadOption;

import java.util.regex.Pattern;

abstract class JSONSchemaBase {

    public static final Pattern PATTERN_URL = Pattern.compile("^(https?|ftp|file)://[-a-zA-Z0-9+&@#/%?=~_|!:,.;]*[-a-zA-Z0-9+&@#/%=~_|]");
    public static final Pattern PATTERN_EMAIL = Pattern.compile("^[a-zA-Z0-9_]+@[a-zA-Z0-9_]+(\\.[a-zA-Z0-9_]+)+$");
    // 解析时支持注释，单引号，双引号，末尾逗号
    static final ReadOption[] PERFECT_READ_OPTIONS = new ReadOption[]
            {
                    ReadOption.AllowComment,
                    ReadOption.AllowUnquotedFieldNames,
                    ReadOption.AllowSingleQuotes,
                    ReadOption.AllowLastEndComma,
            };

    // JSONSchema root
    @JsonProperty(deserialize = false, serialize = false)
    protected JSONSchema root;

    @JsonProperty(deserialize = false, serialize = false)
    private Boolean __formatUrl;
    @JsonProperty(deserialize = false, serialize = false)
    private Boolean __formatEmail;
    @JsonProperty(deserialize = false, serialize = false)
    private Boolean __formatDate;

    public boolean formatUrl() {
        String format = getFormat();
        if (this.__formatUrl != null) {
            return this.__formatUrl;
        }
        return __formatUrl = "url".equals(format);
    }

    public boolean formatDate() {
        String format = getFormat();
        if (this.__formatDate != null) {
            return this.__formatDate;
        }
        return __formatDate = "date".equals(format);
    }

    public boolean formatEmail() {
        String format = getFormat();
        if (this.__formatEmail != null) {
            return this.__formatEmail;
        }
        return __formatEmail = "email".equals(format);
    }

    public abstract String getFormat();

    public JSONSchema root() {
        return root;
    }
}
