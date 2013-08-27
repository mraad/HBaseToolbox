package com.esri;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.hbase.HBaseConfiguration;
import org.apache.hadoop.hbase.HColumnDescriptor;
import org.apache.hadoop.hbase.HTableDescriptor;
import org.apache.hadoop.hbase.client.HBaseAdmin;
import org.apache.hadoop.hbase.client.HTable;
import org.apache.hadoop.hbase.client.HTableInterface;
import org.apache.hadoop.hbase.client.Put;

import java.io.ByteArrayOutputStream;
import java.io.DataOutput;
import java.io.DataOutputStream;
import java.io.IOException;

/**
 * mvn exec:java -q -Dexec.mainClass=com.esri.CreatePutLUT -Dexec.args="1000"
 */
public final class CreatePutLUT
{
    public static void main(final String[] args) throws IOException
    {
        final int count = args.length == 0 ? 1000000 : Integer.parseInt(args[0]);

        final Configuration configuration = HBaseConfiguration.create();

        createLUT(configuration);
        putLUT(configuration, count);
    }

    private static void putLUT(
            final Configuration configuration,
            final int count
    ) throws IOException
    {
        final HTableInterface table = new HTable(configuration, Const.LUT);
        try
        {
            final ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream(128);
            final DataOutput dataOutput = new DataOutputStream(byteArrayOutputStream);

            for (int c = 0; c < count; c++)
            {
                final double lat = -90.0 + 180.0 * Math.random();
                final double lon = -180.0 + 360.0 * Math.random();

                byteArrayOutputStream.reset();
                dataOutput.writeLong(Quad.encode(lon, lat));
                dataOutput.writeDouble(lon);
                dataOutput.writeDouble(lat);
                dataOutput.writeInt(c); // To make it "really" unique

                final Put put = new Put(byteArrayOutputStream.toByteArray());
                put.add(Const.ATTR,
                        Const.ID,
                        Integer.toString(c).getBytes());
                table.put(put);
            }
        }
        finally
        {
            table.close();
        }
    }

    private static void createLUT(final Configuration configuration) throws IOException
    {
        final HBaseAdmin admin = new HBaseAdmin(configuration);
        try
        {
            if (admin.tableExists(Const.LUT))
            {
                admin.disableTable(Const.LUT);
                admin.deleteTable(Const.LUT);
            }
            final HTableDescriptor tableDescriptor = new HTableDescriptor(Const.LUT);
            final HColumnDescriptor columnDescriptor = new HColumnDescriptor(Const.ATTR);
            columnDescriptor.setMaxVersions(1);
            tableDescriptor.addFamily(columnDescriptor);
            admin.createTable(tableDescriptor);
        }
        finally
        {
            admin.close();
        }
    }
}
