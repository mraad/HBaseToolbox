package com.esri;

import com.esri.arcgis.geometry.IGeometry;
import com.esri.arcgis.geometry.Point;
import org.apache.hadoop.hbase.client.Put;
import org.apache.hadoop.hbase.util.Bytes;

import java.io.IOException;

/**
 */
public class PointWriterBytes implements ShapeWriterInterface
{
    @Override
    public void write(
            final Put put,
            final byte[] geomColFam,
            final IGeometry geometry) throws IOException
    {
        final Point point = (Point) geometry;
        put.add(geomColFam, Const.X, Bytes.toBytes(point.getX()));
        put.add(geomColFam, Const.Y, Bytes.toBytes(point.getY()));
    }

    private void close()
    {
    }
}
