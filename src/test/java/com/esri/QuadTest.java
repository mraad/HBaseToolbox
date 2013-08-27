package com.esri;

import ch.hsr.geohash.WGS84Point;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 */
public final class QuadTest
{
    @Test
    public void testEncode()
    {
        assertEquals(0L, Quad.encode(0, 0, 0));

        assertEquals((0L << 63) | (0L << 62), Quad.encode(-1, -1, 1));
        assertEquals((0L << 63) | (1L << 62), Quad.encode(-1, 1, 1));
        assertEquals((1L << 63) | (0L << 62), Quad.encode(1, -1, 1));
        assertEquals((1L << 63) | (1L << 62), Quad.encode(1, 1, 1));

        assertEquals((1L << 63) | (1L << 62), Quad.encode(0, 0, 1));
    }

    @Test
    public void testEncodeDecode() throws Exception
    {
        for (int i = 0; i < 100; i++)
        {
            final double lon = -180 + 360 * Math.random();
            final double lat = -90 + 180 * Math.random();
            final WGS84Point point = Quad.decode(Quad.encode(lon, lat)).getCenterPoint();
            assertEquals(lat, point.getLatitude(), 0.000001);
            assertEquals(lon, point.getLongitude(), 0.000001);
        }
    }

}
