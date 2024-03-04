package io.github.wycst.wast.common.beans.geo;

/**
 * example data: POINT(103 35)
 */
public class Point extends Geometry {

    public Point() {
        this(0, 0);
    }

    Number x;
    Number y;

    public Point(Number x, Number y) {
        super(GeometryType.POINT);
        this.x = x;
        this.y = y;
    }

    public static Point of(Number x, Number y) {
        return new Point(x, y);
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

    @Override
    void readBody(char[] chars, GeometryContext geometryContext) {
        int offset = geometryContext.offset;
        // trim()
        while ((chars[++offset]) == ' ');
        geometryContext.offset = offset;
        readPoint(chars, geometryContext);
        offset = geometryContext.offset;
        if(chars[offset] != ')') {
            throw new IllegalArgumentException("Geometry syntax error, offset " + offset + " , expected ')', actual '" + chars[offset] + "'");
        }
        this.x = geometryContext.x;
        this.y = geometryContext.y;
    }

    @Override
    void appendBody(StringBuilder builder) {
        builder.append("(").append(x).append(" ").append(y).append(")");
    }
}
