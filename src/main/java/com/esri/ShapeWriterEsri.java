package com.esri;

import com.esri.arcgis.geometry.IESRIShape2;
import com.esri.arcgis.geometry.IGeometry;
import org.apache.hadoop.hbase.client.Put;

import java.io.IOException;

/**
 */
public class ShapeWriterEsri implements ShapeWriterInterface
{
    private final byte[] qual = "shape".getBytes();

    @Override
    public void write(
            final Put put,
            final byte[] family,
            final IGeometry geometry) throws IOException
    {
        if (geometry instanceof IESRIShape2)
        {
            final IESRIShape2 shape = (IESRIShape2) geometry;
            final int size = shape.getESRIShapeSizeEx2(0);
            final int[] counts = new int[size];
            final byte[] bytes = new byte[size];
            shape.exportToESRIShapeEx2(0, counts, bytes);
            put.add(family, qual, bytes);
        }
    }
}
