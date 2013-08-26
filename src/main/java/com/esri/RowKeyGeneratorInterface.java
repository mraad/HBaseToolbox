package com.esri;

import com.esri.arcgis.geodatabase.Feature;
import com.esri.arcgis.geodatabase.IFeature;

import java.io.IOException;

/**
 */
public interface RowKeyGeneratorInterface
{
    public byte[] generateRowKey(final IFeature feature) throws IOException;
}
