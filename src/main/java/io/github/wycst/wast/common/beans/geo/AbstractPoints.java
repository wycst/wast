package io.github.wycst.wast.common.beans.geo;

import java.util.ArrayList;
import java.util.List;

/**
 * @Author wangyunchao
 * @Date 2023/5/15 22:10
 */
abstract class AbstractPoints extends Geometry {

    public AbstractPoints(GeometryType geometryType) {
        super(geometryType);
    }

    protected List<Point> points = new ArrayList<Point>();

    public List<Point> getPoints() {
        return points;
    }

    public void setPoints(List<Point> points) {
        this.points = points;
    }

    public void add(Point point) {
        points.add(point);
    }

    public void addAll(Point... pointList) {
        for (Point point : pointList) {
            points.add(point);
        }
    }

    public void addAll(List<Point> pointList) {
        points.addAll(pointList);
    }

    public Point removeAt(int index) {
        return points.remove(index);
    }

    public boolean remove(Point point) {
        return points.remove(point);
    }

    public void clear() {
        points.clear();
    }

    void appendBody(StringBuilder builder) {
        builder.append("(");
        int deleteDotIndex = -1;
        for (Point point : points) {
            builder.append(point.x).append(" ").append(point.y);
            builder.append(",");
            deleteDotIndex = builder.length() - 1;
        }
        if (deleteDotIndex > -1) {
            builder.deleteCharAt(deleteDotIndex);
        }
        builder.append(")");
    }

    @Override
    final void readBody(char[] chars, GeometryContext geometryContext) {
        setPoints(readPoints(chars, geometryContext));
    }
}
