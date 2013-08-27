package com.esri;

import org.apache.hadoop.hbase.util.Bytes;

/**
 */
public final class Const
{
    final static byte[] GEOM = "geom".getBytes();
    final static byte[] ATTR = "attr".getBytes();
    final static byte[] LUT = "lut".getBytes();
    final static byte[] ID = Bytes.toBytes("id");
    final static byte[] X = Bytes.toBytes("x");
    final static byte[] Y = Bytes.toBytes("y");

    private Const()
    {
    }
}
