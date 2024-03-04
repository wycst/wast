package io.github.wycst.wast.common.beans.geo;

/**
 * example data: MULTIPOINT(103 35, 104 34,105 35)
 */
public class MultiPoint extends AbstractPoints {

    public MultiPoint() {
        super(GeometryType.MULTIPOINT);
    }

}
