package io.github.wycst.wast.jdbc.transform;

import io.github.wycst.wast.common.beans.geo.Geometry;

public class GeometryTypeTransformer extends TypeTransformer<Geometry> {

    @Override
    public Object fromJavaField(Geometry value) {
        return value == null ? null : value.toGeometryString();
    }

    @Override
    public Geometry toJavaField(Object value) {
        return value == null ? null : Geometry.from(value.toString());
    }
}
