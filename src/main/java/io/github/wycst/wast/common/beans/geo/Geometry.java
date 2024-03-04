package io.github.wycst.wast.common.beans.geo;

import io.github.wycst.wast.common.reflect.UnsafeHelper;

import java.util.ArrayList;
import java.util.List;

public abstract class Geometry {

    private final GeometryType type;

    Geometry(GeometryType type) {
        this.type = type;
    }

    /**
     * POLYGON((100 20,200 40,140 60,140 60,100 20),(100 20,200 40,140 60,140 60,100 20))
     * LINESTRING(100 20,200 40,140 60,140 60,100 20)
     * MULTILINESTRING((100 20,200 40,140 60,140 60,100 20),(100 20,200 40,140 60,140 60,100 20),(100 20,200 40,140 60,140 60,100 20))
     * MULTIPOLYGON(((100 20,200 40,140 60,140 60,100 20),(100 20,200 40,140 60,140 60,100 20)),((100 20,200 40,140 60,140 60,100 20),(100 20,200 40,140 60,140 60,100 20)),((100 20,200 40,140 60,140 60,100 20),(100 20,200 40,140 60,140 60,100 20)))
     * POINT(12 33)
     * MULTIPOINT(12 33,12 33,12 33)
     * GEOMETRYCOLLECTION(POINT(12 33),MULTIPOLYGON(((100 20,200 40,140 60,140 60,100 20),(100 20,200 40,140 60,140 60,100 20)),((100 20,200 40,140 60,140 60,100 20),(100 20,200 40,140 60,140 60,100 20)),((100 20,200 40,140 60,140 60,100 20),(100 20,200 40,140 60,140 60,100 20))),LINESTRING(100 20,200 40,140 60,140 60,100 20))
     *
     * @param source
     * @return
     */
    public static Geometry from(String source) {
        return from(source, false);
    }

    /**
     * 转化为Geometry实例
     *
     * @param source 空间字符串来源
     * @param strict 严格模式
     * @return
     */
    public static Geometry from(String source, boolean strict) {
        if (source == null) return null;
        source = source.trim();
        GeometryContext geometryContext = new GeometryContext();
        geometryContext.strict = strict;
        return from(source, geometryContext);
    }

    static Geometry from(String source, GeometryContext geometryContext) {
        char[] chars = UnsafeHelper.getChars(source.toUpperCase());
        GeometryType geometryType = readGeometryType(chars, geometryContext);
        Geometry geometry = geometryType.newInstance();
        try {
            geometry.readBody(chars, geometryContext);
        } catch (RuntimeException runtimeException) {
            throw new IllegalArgumentException("Error Geometry source: " + source, runtimeException);
        }
        if (geometryContext.offset != chars.length - 1) {
            throw new IllegalArgumentException("Error Geometry source: " + source + ", extra characters found '" + new String(source.substring(geometryContext.offset + 1)) + "'");
        }
        return geometry;
    }

    void readBody(char[] chars, GeometryContext geometryContext) {
    }

    /**
     * 读取类型
     *
     * @param chars
     * @param geometryContext
     * @return
     */
    static GeometryType readGeometryType(char[] chars, GeometryContext geometryContext) {
        int offset = geometryContext.offset;
        int beginIndex = offset;
        while (chars[++offset] != '(') ;
        geometryContext.offset = offset;
        String type = new String(chars, beginIndex, offset - beginIndex).trim();
        return GeometryType.valueOf(type);
    }

    /**
     * 读取点数据（x, y坐标）
     * 需要提前trim,确保offset为第一个有效字符,读到','或者')'结束
     *
     * @param chars
     * @param geometryContext
     */
    static void readPoint(char[] chars, GeometryContext geometryContext) {
        int offset = geometryContext.offset;
        int beginIndex = offset;
        boolean decimalPoint = false;
        char ch;
        while ((ch = chars[++offset]) != ' ') {
            if (ch == '.') {
                decimalPoint = true;
            }
        }
        String numberStr = new String(chars, beginIndex, offset - beginIndex);
        Number doubleVal = Double.parseDouble(numberStr);
        geometryContext.x = decimalPoint ? doubleVal : (Number) doubleVal.longValue();

        while ((chars[++offset]) == ' ') ;
        beginIndex = offset;
        decimalPoint = false;
        while ((ch = chars[++offset]) != ')' && ch != ',') {
            if (ch == '.') {
                decimalPoint = true;
            }
        }
        numberStr = new String(chars, beginIndex, offset - beginIndex).trim();
        doubleVal = Double.parseDouble(numberStr);
        geometryContext.setOffset(offset);
        geometryContext.y = decimalPoint ? doubleVal : (Number) doubleVal.longValue();
    }

    /**
     * ps: 由(...)包围的一组point列表, 调用前确保开始token(chars[offset])为'(', 返回前确保结束token为')'
     *
     * @param chars
     * @param geometryContext
     * @return
     */
    static List<Point> readPoints(char[] chars, GeometryContext geometryContext) {
        int offset = geometryContext.offset;
        List<Point> points = new ArrayList<Point>();
        char ch;
        while ((ch = chars[++offset]) == ' ') ;
        boolean useBracket = ch == '(';
        while (true) {
            if (useBracket) {
                // trim()
                while (chars[++offset] == ' ') ;
            }
            // read point
            geometryContext.offset = offset;
            readPoint(chars, geometryContext);
            Point point = Point.of(geometryContext.x, geometryContext.y);
            points.add(point);
            offset = geometryContext.offset;
            // check end token
            ch = chars[offset];
            if (useBracket) {
                if (ch != ')') {
                    throw new IllegalArgumentException("Geometry syntax error, offset " + offset + " , expected ')', actual '" + ch + "'");
                }
                // trim
                while ((ch = chars[++offset]) == ' ') ;
                geometryContext.offset = offset;
                if (ch == ')') {
                    // break
                    break;
                } else {
                    if (ch != ',') {
                        throw new IllegalArgumentException("Geometry syntax error, offset " + offset + " , expected ',', actual '" + ch + "'");
                    }
                }
            } else {
                if (ch == ')') {
                    // break
                    break;
                } else {
                    if (ch != ',') {
                        throw new IllegalArgumentException("Geometry syntax error, offset " + offset + " , expected ',', actual '" + ch + "'");
                    }
                }
            }
            // 当前offset位置为字符逗号(,), 进行trim()去除可能存在的空格，确保第一个字符为非空字符
            // trim()
            while ((ch = chars[++offset]) == ' ') ;
            if (useBracket && ch != '(') {
                throw new IllegalArgumentException("Geometry syntax error, offset " + offset + " , expected '(', actual '" + ch + "'");
            }
        }
        return points;
    }

    abstract void appendBody(StringBuilder builder);

    final void appendTo(StringBuilder builder) {
        builder.append(type);
        appendBody(builder);
    }

    public String toString() {
        StringBuilder builder = new StringBuilder();
        appendTo(builder);
        return builder.toString();
    }

    public final String toGeometryString() {
        return toString();
    }

    public GeometryType getType() {
        return type;
    }
}
