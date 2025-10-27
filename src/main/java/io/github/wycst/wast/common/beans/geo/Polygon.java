package io.github.wycst.wast.common.beans.geo;

import java.util.ArrayList;
import java.util.List;

/**
 * example data: POLYGON((103 35,103 36,104 36,105 37,105 37,105 37,103 35))
 * <p>
 * note: Generally, it is a closed path, and there are no restrictions here
 */
public class Polygon extends AbstractMultiGeometry {

    List<PolygonPlane> elements = new ArrayList<PolygonPlane>();

    public Polygon() {
        super(GeometryType.POLYGON);
    }

    public void add(PolygonPlane plane) {
        elements.add(plane);
    }

    public void addAll(PolygonPlane... elements) {
        for (PolygonPlane plane : elements) {
            this.elements.add(plane);
        }
    }

    public void addAll(List<PolygonPlane> elements) {
        this.elements.addAll(elements);
    }

    public PolygonPlane removeAt(int index) {
        return elements.remove(index);
    }

    public boolean remove(PolygonPlane plane) {
        return elements.remove(plane);
    }

    public void clear() {
        elements.clear();
    }

    @Override
    void appendBody(StringBuilder builder) {
        builder.append("(");
        int deleteDotIndex = -1;
        for (PolygonPlane plane : elements) {
            plane.appendBody(builder);
            builder.append(",");
            deleteDotIndex = builder.length() - 1;
        }
        if (deleteDotIndex > -1) {
            builder.deleteCharAt(deleteDotIndex);
        }
        builder.append(")");
    }

    protected void readElement(char[] chars, GeometryContext geometryContext) {
        elements.add(new PolygonPlane(readPoints(chars, geometryContext)));
    }

    public List<PolygonPlane> getElements() {
        return elements;
    }

    public void setElements(List<PolygonPlane> elements) {
        this.elements = elements;
    }
}
