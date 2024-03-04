package io.github.wycst.wast.common.beans.geo;

/**
 * 几何集合类
 *
 */
abstract class AbstractMultiGeometry extends Geometry {

    public AbstractMultiGeometry(GeometryType geometryType) {
        super(geometryType);
    }

    @Override
    final void readBody(char[] chars, GeometryContext geometryContext) {
        int offset = geometryContext.offset;
        char ch;
        // 读取points集合
        while (true) {
            // trim()
            while ((ch = chars[++offset]) == ' ');
            checkElementPrefix(ch, offset);
            geometryContext.offset = offset;
            readElement(chars, geometryContext);
            offset = geometryContext.offset;
            while ((ch = chars[++offset]) == ' ');
            if(ch == ')') {
                // 结束标记
                geometryContext.offset = offset;
                return;
            } else {
                if(ch != ',') {
                    throw new IllegalArgumentException("Geometry syntax error, offset " + offset + " , expected ',', actual '" + ch + "'");
                }
            }
        }
    }

    /**
     * 通常以(开始，只有GeometryCollection例外
     *
     * @param ch
     * @param offset
     */
    protected void checkElementPrefix(char ch, int offset) {
        if(ch != '(') {
            throw new IllegalArgumentException("Geometry syntax error, offset " + offset + " , expected '(', actual '" + ch + "'");
        }
    }

    protected abstract void readElement(char[] chars, GeometryContext geometryContext);

}
