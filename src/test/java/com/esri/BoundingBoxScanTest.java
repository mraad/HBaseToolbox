package com.esri;

import ch.hsr.geohash.BoundingBox;
import ch.hsr.geohash.GeoHash;
import ch.hsr.geohash.WGS84Point;
import ch.hsr.geohash.queries.GeoHashBoundingBoxQuery;
import org.apache.hadoop.hbase.HBaseTestingUtility;
import org.apache.hadoop.hbase.client.HTable;
import org.apache.hadoop.hbase.client.HTableInterface;
import org.apache.hadoop.hbase.client.Put;
import org.apache.hadoop.hbase.client.Result;
import org.apache.hadoop.hbase.client.ResultScanner;
import org.apache.hadoop.hbase.client.Scan;
import org.apache.hadoop.hbase.util.Bytes;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInput;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 */
public class BoundingBoxScanTest
{
    protected final static byte[] TABLE_NAME = "test".getBytes();

    private final HBaseTestingUtility m_testUtil = new HBaseTestingUtility();

    @Before
    public void setUp() throws Exception
    {
        m_testUtil.startMiniCluster();
    }

    @Test
    public void testBoundingBoxScan() throws IOException
    {
        m_testUtil.createTable(TABLE_NAME, Const.ATTR);
        try
        {
            final HTableInterface table = new HTable(m_testUtil.getConfiguration(), TABLE_NAME);
            try
            {
                putLatLon(table);
                scanBox(table);
            }
            finally
            {
                table.close();
            }
        }
        finally
        {
            m_testUtil.deleteTable(TABLE_NAME);
        }
    }

    private void scanBox(final HTableInterface table) throws IOException
    {
        final List<WGS84Point> list = new ArrayList<WGS84Point>();
        final BoundingBox boundingBox = new BoundingBox(0.5, 1.5, 0.5, 1.5);
        final GeoHashBoundingBoxQuery geoHashBoundingBoxQuery = new GeoHashBoundingBoxQuery(boundingBox);
        final List<GeoHash> searchHashes = geoHashBoundingBoxQuery.getSearchHashes();
        for (final GeoHash geoHash : searchHashes)
        {
            scanGeoHash(table, geoHash, boundingBox, list);
        }
        Assert.assertEquals(1, list.size());
        final WGS84Point point = list.get(0);
        Assert.assertEquals(1.0, point.getLatitude(), 0.000001);
        Assert.assertEquals(1.0, point.getLongitude(), 0.000001);
    }

    private void scanGeoHash(
            final HTableInterface table,
            final GeoHash geoHash,
            final BoundingBox boundingBox,
            final List<WGS84Point> list) throws IOException
    {
        final Scan scan = new Scan();
        scan.setStartRow(Bytes.toBytes(geoHash.longValue()));
        scan.setStopRow(Bytes.toBytes(geoHash.next().longValue()));
        scan.setMaxVersions(1);
        scan.setCaching(50);
        scan.setFilter(new BoundingBoxFilter(boundingBox));
        final ResultScanner scanner = table.getScanner(scan);
        try
        {
            for (final Result result : scanner)
            {
                final ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(result.getRow());
                final DataInput dataInput = new DataInputStream(byteArrayInputStream);
                final long bits = dataInput.readLong();
                final double lon = dataInput.readDouble();
                final double lat = dataInput.readDouble();
                list.add(new WGS84Point(lat, lon));
            }
        }
        finally
        {
            scanner.close();
        }
    }

    private void putLatLon(final HTableInterface table) throws IOException
    {
        final ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream(64);
        final DataOutputStream dataOutputStream = new DataOutputStream(byteArrayOutputStream);
        int id = 0;
        for (double c = -10; c < 10; c++)
        {
            byteArrayOutputStream.reset();
            dataOutputStream.writeLong(Quad.encode(c, c));
            dataOutputStream.writeDouble(c);
            dataOutputStream.writeDouble(c);
            final Put put = new Put(byteArrayOutputStream.toByteArray());
            put.add(Const.ATTR,
                    Const.ID,
                    Bytes.toBytes(id++));
            table.put(put);
        }
    }

    @After
    public void tearDown() throws Exception
    {
        m_testUtil.shutdownMiniCluster();
    }

}
