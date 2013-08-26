package com.esri;

import com.esri.arcgis.geodatabase.IFeature;
import org.apache.hadoop.hbase.util.Bytes;

import java.io.IOException;

/**
 */
public class RowKeyGeneratorOID implements RowKeyGeneratorInterface
{
    @Override
    public byte[] generateRowKey(final IFeature feature) throws IOException
    {
        return Bytes.toBytes(feature.getOID());
    }
}
