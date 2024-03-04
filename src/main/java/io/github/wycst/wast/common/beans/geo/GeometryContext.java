package io.github.wycst.wast.common.beans.geo;

public class GeometryContext {

    int offset;
    boolean strict;
    Number x;
    Number y;

    public int getOffset() {
        return offset;
    }

    public void setOffset(int offset) {
        this.offset = offset;
    }

    public boolean isStrict() {
        return strict;
    }

    public void setStrict(boolean strict) {
        this.strict = strict;
    }

    public Number getX() {
        return x;
    }

    public void setX(Number x) {
        this.x = x;
    }

    public Number getY() {
        return y;
    }

    public void setY(Number y) {
        this.y = y;
    }
}
