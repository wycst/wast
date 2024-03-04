package io.github.wycst.wast.common.beans.geo;

import java.util.List;

/**
 * @Author wangyunchao
 * @Date 2023/5/15 23:43
 */
public class PolygonPlane extends AbstractPoints {
    public PolygonPlane() {
        super(GeometryType.POLYGON);
    }

    PolygonPlane(List<Point> points) {
        super(GeometryType.POLYGON);
        this.points = points;
    }

}
