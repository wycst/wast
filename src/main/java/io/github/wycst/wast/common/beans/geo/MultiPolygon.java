package io.github.wycst.wast.common.beans.geo;

import java.util.ArrayList;
import java.util.List;

/**
 * example data: MULTIPOLYGON(((103 35,104 35,104 36,103 36,103 35)),((103 36,104 36,104 37,103 36)))
 */
public class MultiPolygon extends AbstractMultiGeometry {

    List<Polygon> elements = new ArrayList<Polygon>();

    public MultiPolygon() {
        super(GeometryType.MULTIPOLYGON);
    }

    public void add(Polygon polygon) {
        elements.add(polygon);
    }

    public void addAll(Polygon... polygons) {
        for (Polygon polygon : polygons) {
            elements.add(polygon);
        }
    }

    public void addAll(List<Polygon> polygons) {
        elements.addAll(polygons);
    }

    public Polygon removeAt(int index) {
        return elements.remove(index);
    }

    public boolean remove(Polygon polygon) {
        return elements.remove(polygon);
    }

    public void clear() {
        elements.clear();
    }

    @Override
    void appendBody(StringBuilder builder) {
        builder.append("(");
        int deleteDotIndex = -1;
        for (Polygon polygon : elements) {
            polygon.appendBody(builder);
            builder.append(",");
            deleteDotIndex = builder.length() - 1;
        }
        if (deleteDotIndex > -1) {
            builder.deleteCharAt(deleteDotIndex);
        }
        builder.append(")");
    }

    @Override
    protected void readElement(char[] chars, GeometryContext geometryContext) {
        Polygon polygon = new Polygon();
        polygon.readBody(chars, geometryContext);
        add(polygon);
    }

    public List<Polygon> getElements() {
        return elements;
    }

    public void setElements(List<Polygon> elements) {
        this.elements = elements;
    }
}
