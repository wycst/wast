package io.github.wycst.wast.common.beans.geo;

import java.util.List;

/**
 * example data: LINESTRING(103 35,103 36,104 36,105 37)
 */
public class LineString extends AbstractPoints {

    public LineString() {
        super(GeometryType.LINESTRING);
    }

    LineString(List<Point> points) {
        super(GeometryType.POLYGON);
        this.points = points;
    }


}
