package io.github.wycst.wast.common.beans.geo;

import java.util.ArrayList;
import java.util.List;

/**
 * example data: GEOMETRYCOLLECTION(POINT(103 35), LINESTRING(103 35, 103 37))
 */
public class GeometryCollection extends AbstractMultiGeometry {

    public GeometryCollection() {
        super(GeometryType.GEOMETRYCOLLECTION);
    }

    private List<Geometry> elements = new ArrayList<Geometry>();

    public void add(Geometry child) {
        elements.add(child);
    }

    public void addAll(Geometry...childList) {
        for (Geometry child : childList) {
            elements.add(child);
        }
    }

    public void addAll(List<Geometry> childList) {
        elements.addAll(childList);
    }

    public Geometry removeAt(int index) {
        return elements.remove(index);
    }

    public boolean remove(Geometry geometry) {
        return elements.remove(geometry);
    }

    public void clear() {
        elements.clear();
    }

    @Override
    void appendBody(StringBuilder builder) {
        builder.append("(");
        int deleteDotIndex = -1;
        for (Geometry geometry : elements) {
            geometry.appendTo(builder);
            builder.append(",");
            deleteDotIndex = builder.length() - 1;
        }
        if (deleteDotIndex > -1) {
            builder.deleteCharAt(deleteDotIndex);
        }
        builder.append(")");
    }

    @Override
    protected void checkElementPrefix(char ch, int offset) {
    }

    @Override
    protected void readElement(char[] chars, GeometryContext geometryContext) {
        GeometryType geometryType = readGeometryType(chars, geometryContext);
        Geometry geometry = geometryType.newInstance();
        geometry.readBody(chars, geometryContext);
        add(geometry);
    }

    public List<Geometry> getElements() {
        return elements;
    }

    public void setElements(List<Geometry> elements) {
        this.elements = elements;
    }
}
