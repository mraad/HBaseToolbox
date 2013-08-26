package com.esri;

import com.esri.arcgis.geometry.IGeometry;
import com.esri.arcgis.geometry.Point;
import org.apache.hadoop.hbase.client.Put;
import org.apache.hadoop.hbase.util.Bytes;

import java.io.IOException;

/**
 */
public class PointWriterGeoJSON implements ShapeWriterInterface
{
    private final byte[] pointQual = Bytes.toBytes("point");
    private final StringBuilder m_stringBuilder = new StringBuilder();

    @Override
    public void write(
            final Put put,
            final byte[] geomColFam,
            final IGeometry geometry) throws IOException
    {
        final Point point = (Point) geometry;
        m_stringBuilder.setLength(0);
        m_stringBuilder.append("{\"type\":\"Point\",\"coordinates\":[").
                append(point.getX()).
                append(",").
                append(point.getY()).
                append(")]}");
        put.add(geomColFam, pointQual, Bytes.toBytes(m_stringBuilder.toString()));
    }

    private void close()
    {
    }
}
