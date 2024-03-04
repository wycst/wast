package io.github.wycst.wast.json;

import java.io.Writer;

public abstract class JSONStringWriter extends Writer {

    public abstract String toString();

    abstract StringBuffer toStringBuffer();

    abstract StringBuilder toStringBuilder();

    public void reset() {
    }

    public void clear() {
    }
}
