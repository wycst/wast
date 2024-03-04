package io.github.wycst.wast.common.beans.geo;

import java.util.ArrayList;
import java.util.List;

/**
 * example data: MULTILINESTRING((103 35, 104 35), (105 36, 105 37))
 */
public class MultiLineString extends AbstractMultiGeometry {

    public MultiLineString() {
        super(GeometryType.MULTILINESTRING);
    }

    private List<LineString> elements = new ArrayList<LineString>();

    public void add(LineString lineString) {
        elements.add(lineString);
    }

    public void addAll(LineString...lineStringList) {
        for (LineString lineString : lineStringList) {
            elements.add(lineString);
        }
    }

    public void addAll(List<LineString> lineStringList) {
        elements.addAll(lineStringList);
    }

    public LineString removeAt(int index) {
        return elements.remove(index);
    }

    public boolean remove(LineString lineString) {
        return elements.remove(lineString);
    }

    public void clear() {
        elements.clear();
    }

    @Override
    void appendBody(StringBuilder builder) {
        builder.append("(");
        int deleteDotIndex = -1;
        for (LineString lineString : elements) {
            lineString.appendBody(builder);
            builder.append(",");
            deleteDotIndex = builder.length() - 1;
        }
        if (deleteDotIndex > -1) {
            builder.deleteCharAt(deleteDotIndex);
        }
        builder.append(")");
    }

    protected void readElement(char[] chars, GeometryContext geometryContext) {
        elements.add(new LineString(readPoints(chars, geometryContext)));
    }

    public List<LineString> getElements() {
        return elements;
    }

    public void setElements(List<LineString> elements) {
        this.elements = elements;
    }
}
