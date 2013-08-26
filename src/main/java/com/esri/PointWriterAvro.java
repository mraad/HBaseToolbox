package com.esri;

import com.esri.arcgis.geometry.IGeometry;
import com.esri.arcgis.geometry.Point;
import org.apache.avro.file.DataFileWriter;
import org.apache.avro.io.DatumWriter;
import org.apache.avro.specific.SpecificDatumWriter;
import org.apache.hadoop.hbase.client.Put;
import org.apache.hadoop.hbase.util.Bytes;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

/**
 */
public class PointWriterAvro extends ByteArrayOutputStream implements ShapeWriterInterface
{
    private AvroSpatialReference m_spatialReference;
    private DataFileWriter<AvroPoint> m_dataFileWriter;

    private final byte[] pointQual = Bytes.toBytes("point");

    public PointWriterAvro(final int wkid)
    {
        final DatumWriter<AvroPoint> datumWriter = new SpecificDatumWriter<AvroPoint>(AvroPoint.class);
        m_dataFileWriter = new DataFileWriter<AvroPoint>(datumWriter);
        m_spatialReference = AvroSpatialReference.newBuilder().setWkid(wkid).build();
    }

    @Override
    public void write(
            final Put put,
            final byte[] geomColFam,
            final IGeometry geometry) throws IOException
    {
        final Point point = (Point) geometry;

        m_dataFileWriter.create(AvroPoint.getClassSchema(), this);
        final AvroCoord coord = AvroCoord.newBuilder().
                setX(point.getX()).
                setY(point.getY()).
                build();
        m_dataFileWriter.append(AvroPoint.newBuilder().
                setSpatialReference(m_spatialReference).
                setCoord(coord).
                build());
        m_dataFileWriter.close();

        put.add(geomColFam, pointQual, this.toByteArray());

        this.reset();
    }

    @Override
    public void close()
    {
    }
}
