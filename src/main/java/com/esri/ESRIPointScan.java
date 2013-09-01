package com.esri;

import com.esri.core.geometry.Geometry;
import com.esri.core.geometry.GeometryEngine;
import com.esri.core.geometry.Point;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.hbase.HBaseConfiguration;
import org.apache.hadoop.hbase.client.HTable;
import org.apache.hadoop.hbase.client.Result;
import org.apache.hadoop.hbase.client.ResultScanner;
import org.apache.hadoop.hbase.client.Scan;
import org.apache.hadoop.hbase.util.Bytes;

import java.io.IOException;

/**
 */
public class ESRIPointScan
{
    public static void main(final String[] args) throws IOException
    {
        final String tableName = args.length == 0 ? "CustPoi" : args[0];
        final Configuration configuration = HBaseConfiguration.create();
        final HTable table = new HTable(configuration, tableName);
        try
        {
            final Scan scan = new Scan();
            scan.setMaxVersions(1);
            scan.setCaching(1000);
            final ResultScanner scanner = table.getScanner(scan);
            try
            {
                for (final Result result : scanner)
                {
                    final int oid = Bytes.toInt(result.getRow());
                    final byte[] bytes = result.getValue(Const.GEOM, Const.SHAPE);
                    final Point point = (Point) GeometryEngine.geometryFromEsriShape(bytes, Geometry.Type.Point);
                    System.out.format("%d %.6f %.6f\n", oid, point.getX(), point.getY());
                }
            }
            finally
            {
                scanner.close();
            }
        }
        finally
        {
            table.close();
        }
    }
}
