package com.esri;

import com.esri.arcgis.geometry.IGeometry;
import org.apache.hadoop.hbase.client.Put;

/**
 */
public class ShapeWriterNoop implements ShapeWriterInterface
{
    @Override
    public void write(
            final Put put,
            final byte[] geomColFam,
            final IGeometry geometry)
    {
    }

    private void close()
    {
    }
}
