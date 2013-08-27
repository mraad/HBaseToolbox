package com.esri;

import ch.hsr.geohash.BoundingBox;
import ch.hsr.geohash.GeoHash;
import ch.hsr.geohash.queries.GeoHashBoundingBoxQuery;
import org.apache.hadoop.hbase.client.HTable;
import org.apache.hadoop.hbase.client.Result;
import org.apache.hadoop.hbase.client.ResultScanner;
import org.apache.hadoop.hbase.client.Scan;
import org.apache.hadoop.hbase.util.Bytes;
import org.apache.hadoop.io.IntWritable;
import org.apache.hadoop.io.LongWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Mapper;

import java.io.ByteArrayInputStream;
import java.io.DataInput;
import java.io.DataInputStream;
import java.io.IOException;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class FreqDistMapper extends Mapper<LongWritable, Text, LongWritable, IntWritable>
{
    public final static IntWritable ONE = new IntWritable(1);

    // private final Log m_log = LogFactory.getLog(FreqDistMapper.class);
    private final Pattern m_pattern = Pattern.compile("^.+\\t(-?\\d+\\.\\d+)\\t(-?\\d+\\.\\d+)$");
    private final LongWritable m_key = new LongWritable();
    private double m_offset;
    private HTable m_table;

    @Override
    protected void setup(final Context context) throws IOException, InterruptedException
    {
        m_offset = context.getConfiguration().getFloat("freqDistJob.offset", 5);
        m_table = new HTable(context.getConfiguration(), Const.LUT);
    }

    public void map(
            final LongWritable lineno,
            final Text line,
            final Context context
    ) throws IOException, InterruptedException
    {
        final Matcher matcher = m_pattern.matcher(line.toString());
        if (matcher.matches())
        {
            final double lon = Double.parseDouble(matcher.group(1));
            final double lat = Double.parseDouble(matcher.group(2));

            final BoundingBox boundingBox = new BoundingBox(
                    Math.max(-90, lat - m_offset), Math.min(90, lat + m_offset),
                    Math.max(-180, lon - m_offset), Math.min(180, lon + m_offset));
            final GeoHashBoundingBoxQuery geoHashBoundingBoxQuery = new GeoHashBoundingBoxQuery(boundingBox);
            final List<GeoHash> searchHashes = geoHashBoundingBoxQuery.getSearchHashes();
            double minDist = Double.POSITIVE_INFINITY;
            for (final GeoHash geoHash : searchHashes)
            {
                minDist = doScan(geoHash, lon, lat, boundingBox, minDist);
            }
            if (minDist < Double.POSITIVE_INFINITY)
            {
                m_key.set(Math.round(Math.sqrt(minDist) * 10.0)); // TODO - Make configurable
                context.write(m_key, ONE);
            }
        }
    }

    private double doScan(
            final GeoHash start,
            final double origLon,
            final double origLat,
            final BoundingBox boundingBox,
            double minDist) throws IOException
    {
        final Scan scan = new Scan();
        scan.setStartRow(Bytes.toBytes(start.longValue()));
        scan.setStopRow(Bytes.toBytes(start.next().longValue()));
        scan.setMaxVersions(1);
        scan.setCaching(50); // TODO - make configurable
        scan.setFilter(new BoundingBoxFilter(boundingBox));
        final ResultScanner scanner = m_table.getScanner(scan);
        try
        {
            for (final Result result : scanner)
            {
                final ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(result.getRow());
                final DataInput dataInput = new DataInputStream(byteArrayInputStream);
                final long bits = dataInput.readLong();
                final double resultLon = dataInput.readDouble();
                final double resultLat = dataInput.readDouble();
                // Dummy implementation of geo distance - should use http://en.wikipedia.org/wiki/Vincenty's_formulae
                final double deltaLon = resultLon - origLon;
                final double deltaLat = resultLat - origLat;
                minDist = Math.min(minDist, deltaLon * deltaLon + deltaLat * deltaLat);
            }
        }
        finally
        {
            scanner.close();
        }
        return minDist;
    }

    @Override
    protected void cleanup(final Context context) throws IOException, InterruptedException
    {
        if (m_table != null)
        {
            m_table.close();
        }
    }

}