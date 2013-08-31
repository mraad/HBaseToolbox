package com.esri;

import com.esri.arcgis.geometry.IESRIShape2;
import com.esri.arcgis.geometry.IESRIShape2Proxy;
import com.esri.arcgis.geometry.IGeometry;
import com.esri.arcgis.geometry.esriShapeExportFlags;
import com.esri.arcgis.interop.Cleaner;
import org.apache.hadoop.hbase.client.Put;

import java.io.IOException;

/**
 */
public class ShapeWriterEsri implements ShapeWriterInterface
{
    private final byte[] m_qual = "shape".getBytes();

    @Override
    public void write(
            final Put put,
            final byte[] family,
            final IGeometry geometry) throws IOException
    {
        final IESRIShape2 esriShape = new IESRIShape2Proxy(geometry);
        try
        {
            final int size = esriShape.getESRIShapeSizeEx2(esriShapeExportFlags.esriShapeExportDefaults);
            final int[] counts = new int[size];
            final byte[] bytes = new byte[size];
            esriShape.exportToESRIShapeEx2(esriShapeExportFlags.esriShapeExportDefaults, counts, bytes);
            put.add(family, m_qual, bytes);
        }
        finally
        {
            Cleaner.release(esriShape);
        }
    }
}
