package io.github.wycst.wast.common.beans.geo;

public enum GeometryType {
    GEOMETRYCOLLECTION {
        @Override
        Geometry newInstance() {
            return new GeometryCollection();
        }
    },
    LINESTRING {
        @Override
        Geometry newInstance() {
            return new LineString();
        }
    },
    MULTILINESTRING {
        @Override
        Geometry newInstance() {
            return new MultiLineString();
        }
    },
    MULTIPOINT {
        @Override
        Geometry newInstance() {
            return new MultiPoint();
        }
    },
    MULTIPOLYGON {
        @Override
        Geometry newInstance() {
            return new MultiPolygon();
        }
    },
    POINT {
        @Override
        Geometry newInstance() {
            return new Point();
        }
    },
    POLYGON {
        @Override
        Geometry newInstance() {
            return new Polygon();
        }
    };

    Geometry newInstance() {
        throw new UnsupportedOperationException();
    }
}
