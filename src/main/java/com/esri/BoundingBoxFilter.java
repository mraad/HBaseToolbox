package com.esri;

import ch.hsr.geohash.BoundingBox;
import ch.hsr.geohash.WGS84Point;
import org.apache.hadoop.hbase.filter.FilterBase;

import java.io.ByteArrayInputStream;
import java.io.DataInput;
import java.io.DataInputStream;
import java.io.DataOutput;
import java.io.IOException;

/**
 */
public final class BoundingBoxFilter extends FilterBase
{
    private BoundingBox m_boundingBox;

    public BoundingBoxFilter()
    {
    }

    public BoundingBoxFilter(
            final BoundingBox boundingBox
    )
    {
        m_boundingBox = boundingBox;
    }

    @Override
    public boolean filterRowKey(
            final byte[] buffer,
            final int offset,
            final int length)
    {
        boolean filter;
        final ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(buffer, offset, length);
        final DataInputStream dataInputStream = new DataInputStream(byteArrayInputStream);
        try
        {
            final long bits = dataInputStream.readLong();
            final double lon = dataInputStream.readDouble();
            final double lat = dataInputStream.readDouble();
            filter = !m_boundingBox.contains(new WGS84Point(lat, lon));
        }
        catch (IOException e)
        {
            filter = true;
        }
        return filter;
    }

    @Override
    public void write(final DataOutput dataOutput) throws IOException
    {
        dataOutput.writeDouble(m_boundingBox.getMinLat());
        dataOutput.writeDouble(m_boundingBox.getMaxLat());
        dataOutput.writeDouble(m_boundingBox.getMinLon());
        dataOutput.writeDouble(m_boundingBox.getMaxLon());
    }

    @Override
    public void readFields(final DataInput dataInput) throws IOException
    {

        final double ymin = dataInput.readDouble();
        final double ymax = dataInput.readDouble();
        final double xmin = dataInput.readDouble();
        final double xmax = dataInput.readDouble();
        m_boundingBox = new BoundingBox(ymin, ymax, xmin, xmax);
    }
}
