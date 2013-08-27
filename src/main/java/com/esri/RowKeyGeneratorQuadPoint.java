package com.esri;

import com.esri.arcgis.geodatabase.IFeature;
import com.esri.arcgis.geometry.IEnvelope;
import com.esri.arcgis.interop.Cleaner;

import java.io.ByteArrayOutputStream;
import java.io.DataOutput;
import java.io.DataOutputStream;
import java.io.IOException;

/**
 */
public class RowKeyGeneratorQuadPoint implements RowKeyGeneratorInterface
{
    private ByteArrayOutputStream m_byteArrayOutputStream = new ByteArrayOutputStream(128);
    private DataOutput m_dataOutput = new DataOutputStream(m_byteArrayOutputStream);

    @Override
    public byte[] generateRowKey(final IFeature feature) throws IOException
    {
        final double x, y;
        final IEnvelope extent = feature.getExtent();
        try
        {
            x = extent.getXMin();
            y = extent.getYMin();
        }
        finally
        {
            Cleaner.release(extent);
        }
        m_byteArrayOutputStream.reset();
        m_dataOutput.writeLong(Quad.encode(x, y));
        m_dataOutput.writeDouble(x);
        m_dataOutput.writeDouble(y);
        m_dataOutput.writeInt(feature.getOID()); // to "really" make it unique !!
        return m_byteArrayOutputStream.toByteArray();
    }
}
