package com.esri;

import com.esri.arcgis.geometry.IGeometry;
import org.apache.hadoop.hbase.client.Put;

import java.io.IOException;

/**
 */
public interface ShapeWriterInterface
{
    public void write(
            final Put put,
            final byte[] geomColFam,
            final IGeometry geometry) throws IOException;

}
