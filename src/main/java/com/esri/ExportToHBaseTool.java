package com.esri;

import com.esri.arcgis.geodatabase.FeatureClass;
import com.esri.arcgis.geodatabase.IFeature;
import com.esri.arcgis.geodatabase.IFeatureClass;
import com.esri.arcgis.geodatabase.IFeatureClassProxy;
import com.esri.arcgis.geodatabase.IFeatureCursor;
import com.esri.arcgis.geodatabase.IField;
import com.esri.arcgis.geodatabase.IFields;
import com.esri.arcgis.geodatabase.IGPMessages;
import com.esri.arcgis.geodatabase.IGPValue;
import com.esri.arcgis.geodatabase.esriFieldType;
import com.esri.arcgis.geometry.esriShapeType;
import com.esri.arcgis.geoprocessing.IGPEnvironmentManager;
import com.esri.arcgis.interop.AutomationException;
import com.esri.arcgis.interop.Cleaner;
import com.esri.arcgis.system.Array;
import com.esri.arcgis.system.IArray;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.hbase.HBaseConfiguration;
import org.apache.hadoop.hbase.HColumnDescriptor;
import org.apache.hadoop.hbase.HTableDescriptor;
import org.apache.hadoop.hbase.client.HBaseAdmin;
import org.apache.hadoop.hbase.client.HTable;
import org.apache.hadoop.hbase.client.HTableInterface;
import org.apache.hadoop.hbase.client.Put;
import org.apache.hadoop.hbase.util.Bytes;
import org.apache.hadoop.security.UserGroupInformation;

import java.io.File;
import java.io.IOException;
import java.security.PrivilegedExceptionAction;

/**
 */
public final class ExportToHBaseTool extends AbstractTool
{
    private static final long serialVersionUID = -7466263751504116156L;

    public static final String NAME = ExportToHBaseTool.class.getSimpleName();

    @Override
    protected void doExecute(
            final IArray parameters,
            final IGPMessages messages,
            final IGPEnvironmentManager environmentManager
    ) throws Exception
    {
        final IGPValue hadoopUserValue = gpUtilities.unpackGPValue(parameters.getElement(1));

        final UserGroupInformation ugi = UserGroupInformation.createRemoteUser(hadoopUserValue.getAsText());
        final int count = ugi.doAs(new PrivilegedExceptionAction<Integer>()
        {
            public Integer run() throws Exception
            {
                return doExport(parameters, messages);
            }
        });
        messages.addMessage(String.format("Exported %d features.", count));
    }

    private int doExport(
            final IArray parameters,
            final IGPMessages messages
    ) throws Exception
    {
        final IGPValue hadoopConfValue = gpUtilities.unpackGPValue(parameters.getElement(0));
        final IGPValue featureClassValue = gpUtilities.unpackGPValue(parameters.getElement(2));

        int count = 0;
        final IFeatureClass[] featureClasses = new IFeatureClass[]{new IFeatureClassProxy()};
        gpUtilities.decodeFeatureLayer(featureClassValue, featureClasses, null);
        final FeatureClass featureClass = new FeatureClass(featureClasses[0]);
        try
        {
            final Configuration configuration = HBaseConfiguration.create(createConfiguration(hadoopConfValue.getAsText()));
            createIfDoesNotExist(configuration, featureClass.getName());
            final HTableInterface table = new HTable(configuration, featureClass.getName());
            try
            {
                table.setAutoFlush(configuration.getBoolean("exportToHBaseTool.autoFlush", false));
                messages.addMessage("autoFlush is " + (table.isAutoFlush() ? "true" : "false"));
                table.setWriteBufferSize(configuration.getInt("exportToHBaseTool.writeBufferSize", 1024 * 1024));
                messages.addMessage(String.format("writeBufferSize = %d", table.getWriteBufferSize()));
                count = doExport(configuration, messages, featureClass, table);
                if (!table.isAutoFlush())
                {
                    table.flushCommits();
                }
            }
            finally
            {
                table.close();
            }
        }
        finally
        {
            Cleaner.release(featureClass);
        }
        return count;
    }

    private void createIfDoesNotExist(
            final Configuration configuration,
            final String tableName) throws IOException
    {
        final HBaseAdmin admin = new HBaseAdmin(configuration);
        try
        {
            if (!admin.tableExists(tableName))
            {
                final int maxVersions = configuration.getInt("createHTableTool.maxVersions", 1);

                final HTableDescriptor tableDescriptor = new HTableDescriptor(tableName);

                final HColumnDescriptor geomDescriptor = new HColumnDescriptor(Const.GEOM);
                geomDescriptor.setMaxVersions(maxVersions);
                tableDescriptor.addFamily(geomDescriptor);

                final HColumnDescriptor attrDescriptor = new HColumnDescriptor(Const.ATTR);
                attrDescriptor.setMaxVersions(maxVersions);
                tableDescriptor.addFamily(attrDescriptor);

                admin.createTable(tableDescriptor);
            }
        }
        finally
        {
            admin.close();
        }
    }

    private int doExport(
            final Configuration configuration,
            final IGPMessages messages,
            final FeatureClass featureClass,
            final HTableInterface table) throws IOException
    {
        int count = 0;
        final String rowKeyGenerator = configuration.get("exportToHBaseTool.rowKeyGenerator", "oid");
        messages.addMessage(String.format("rowKeyGenerator = %s, shapeType = %d", rowKeyGenerator, featureClass.getShapeType()));
        final RowKeyGeneratorInterface rowKeyGeneratorInterface;
        switch (featureClass.getShapeType())
        {
            case esriShapeType.esriShapePoint:
                if ("geohash".equalsIgnoreCase(rowKeyGenerator))
                {
                    rowKeyGeneratorInterface = new RowKeyGeneratorQuadPoint();
                }
                else
                {
                    rowKeyGeneratorInterface = new RowKeyGeneratorOID();
                }
                break;
            default: // TODO - handle geohash polyline and polygons
                rowKeyGeneratorInterface = new RowKeyGeneratorOID();
        }
        final boolean writeToWAL = configuration.getBoolean("exportToHBaseTool.writeToWAL", true);
        messages.addMessage("writeToWAL is " + (writeToWAL ? "true" : "false"));
        final ShapeWriterInterface shapeWriter = toShapeWriter(configuration, featureClass, rowKeyGenerator, messages);
        final IFields fields = featureClass.getFields();
        try
        {
            addMessage(messages, fields);
            final IFeatureCursor cursor = featureClass.search(null, false);
            try
            {
                IFeature feature = cursor.nextFeature();
                while (feature != null)
                {
                    final Put put = new Put(rowKeyGeneratorInterface.generateRowKey(feature));
                    put.setWriteToWAL(writeToWAL);

                    shapeWriter.write(put, Const.GEOM, feature.getShape());

                    final int fieldCount = fields.getFieldCount();
                    for (int f = 0; f < fieldCount; f++)
                    {
                        final IField field = fields.getField(f);
                        try
                        {
                            final byte[] name = Bytes.toBytes(field.getName());
                            final Object value = feature.getValue(f);
                            // TODO - Handle date and other types
                            switch (field.getType())
                            {
                                case esriFieldType.esriFieldTypeString:
                                    put.add(Const.ATTR, name, Bytes.toBytes((String) value));
                                    break;
                                case esriFieldType.esriFieldTypeSingle:
                                    put.add(Const.ATTR, name, Bytes.toBytes((Float) value));
                                    break;
                                case esriFieldType.esriFieldTypeDouble:
                                    put.add(Const.ATTR, name, Bytes.toBytes((Double) value));
                                    break;
                                case esriFieldType.esriFieldTypeInteger:
                                    put.add(Const.ATTR, name, Bytes.toBytes((Integer) value));
                                    break;
                                case esriFieldType.esriFieldTypeSmallInteger:
                                    if (value instanceof Short)
                                    {
                                        put.add(Const.ATTR, name, Bytes.toBytes((Short) value));
                                    }
                                    else if (value instanceof Byte)
                                    {
                                        put.add(Const.ATTR, name, Bytes.toBytes((Byte) value));
                                    }
                                    break;
                            }
                        }
                        finally
                        {
                            Cleaner.release(field);
                        }
                    }

                    table.put(put);
                    count++;

                    feature = cursor.nextFeature();
                }
            }
            finally
            {
                Cleaner.release(cursor);
            }
        }
        finally
        {
            Cleaner.release(fields);
        }
        return count;
    }

    private ShapeWriterInterface toShapeWriter(
            final Configuration configuration,
            final FeatureClass featureClass,
            final String rowKeyGenerator,
            final IGPMessages messages) throws IOException
    {
        final ShapeWriterInterface shapeWriter;
        final String shapeWriterType = configuration.get("exportToHBaseTool.shapeWriterType", "bytes");
        messages.addMessage("shapeWriterType = " + shapeWriterType);
        switch (featureClass.getShapeType())
        {
            case esriShapeType.esriShapePoint:
                if ("geohash".equalsIgnoreCase(rowKeyGenerator))
                {
                    shapeWriter = new ShapeWriterNoop();
                }
                else if ("geojson".equalsIgnoreCase(shapeWriterType))
                {
                    shapeWriter = new PointWriterGeoJSON();
                }
                else if ("avro".equalsIgnoreCase(shapeWriterType))
                {
                    shapeWriter = new PointWriterAvro(getWkid(featureClass));
                }
                else if ("bytes".equalsIgnoreCase(shapeWriterType))
                {
                    shapeWriter = new PointWriterBytes();
                }
                else if ("esri".equalsIgnoreCase(shapeWriterType))
                {
                    shapeWriter = new ShapeWriterEsri();
                }
                else // noop
                {
                    shapeWriter = new ShapeWriterNoop();
                }
                break;
            default:
                if ("esri".equalsIgnoreCase(shapeWriterType))
                {
                    shapeWriter = new ShapeWriterEsri();
                }
                else
                {
                    // TODO - Polyline and Polygon !
                    shapeWriter = new ShapeWriterNoop();
                }
        }
        return shapeWriter;
    }

    private void addMessage(
            final IGPMessages messages,
            final IFields fields) throws IOException
    {
        final int count = fields.getFieldCount();
        for (int c = 0; c < count; c++)
        {
            final IField field = fields.getField(c);
            try
            {
                messages.addMessage(String.format("%s %d", field.getName(), field.getType()));
            }
            finally
            {
                Cleaner.release(field);
            }
        }
    }

    @Override
    public IArray getParameterInfo() throws IOException, AutomationException
    {
        final String username = System.getProperty("user.name");
        final String userhome = System.getProperty("user.home") + File.separator;

        final IArray parameters = new Array();

        addParamFile(parameters, "Hadoop properties file", "in_hadoop_prop", userhome + "hadoop.properties");
        addParamString(parameters, "Hadoop user", "in_hadoop_user", username);
        addParamFeatureLayer(parameters, "Input features", "in_features");

        return parameters;
    }

    @Override
    public String getName() throws IOException, AutomationException
    {
        return NAME;
    }

    @Override
    public String getDisplayName() throws IOException, AutomationException
    {
        return NAME;
    }
}
