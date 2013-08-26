package com.esri;

import com.esri.arcgis.geodatabase.FeatureClass;
import com.esri.arcgis.geodatabase.IFeatureClass;
import com.esri.arcgis.geodatabase.IFeatureClassProxy;
import com.esri.arcgis.geodatabase.IGPMessages;
import com.esri.arcgis.geodatabase.IGPValue;
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
import org.apache.hadoop.security.UserGroupInformation;

import java.io.File;
import java.io.IOException;
import java.security.PrivilegedExceptionAction;

/**
 */
public final class CreateHTableTool extends AbstractTool
{
    private static final long serialVersionUID = -2916674390536249579L;

    public static final String NAME = CreateHTableTool.class.getSimpleName();

    @Override
    protected void doExecute(
            final IArray parameters,
            final IGPMessages messages,
            final IGPEnvironmentManager environmentManager
    ) throws Exception
    {
        final IGPValue hadoopUserValue = gpUtilities.unpackGPValue(parameters.getElement(1));

        final UserGroupInformation ugi = UserGroupInformation.createRemoteUser(hadoopUserValue.getAsText());
        ugi.doAs(new PrivilegedExceptionAction<Void>()
        {
            public Void run() throws Exception
            {
                return doExport(parameters, messages);
            }
        });
    }

    private Void doExport(
            final IArray parameters,
            final IGPMessages messages
    ) throws Exception
    {
        final IGPValue hadoopConfValue = gpUtilities.unpackGPValue(parameters.getElement(0));
        final IGPValue featureClassValue = gpUtilities.unpackGPValue(parameters.getElement(2));

        final IFeatureClass[] featureClasses = new IFeatureClass[]{new IFeatureClassProxy()};
        gpUtilities.decodeFeatureLayer(featureClassValue, featureClasses, null);
        final FeatureClass featureClass = new FeatureClass(featureClasses[0]);
        try
        {
            final Configuration configuration = HBaseConfiguration.create(createConfiguration(hadoopConfValue.getAsText()));
            final HBaseAdmin admin = new HBaseAdmin(configuration);
            try
            {
                if (admin.tableExists(featureClass.getName()))
                {
                    admin.disableTable(featureClass.getName());
                    admin.deleteTable(featureClass.getName());
                }
                messages.addMessage("Creating HTable '" + featureClass.getName() + "'");

                final int maxVersions = configuration.getInt("createHTableTool.maxVersions", 1);

                final HTableDescriptor tableDescriptor = new HTableDescriptor(featureClass.getName());

                final HColumnDescriptor geomDescriptor = new HColumnDescriptor(Const.GEOM);
                geomDescriptor.setMaxVersions(maxVersions);
                tableDescriptor.addFamily(geomDescriptor);

                final HColumnDescriptor attrDescriptor = new HColumnDescriptor(Const.ATTR);
                attrDescriptor.setMaxVersions(maxVersions);
                tableDescriptor.addFamily(attrDescriptor);

                admin.createTable(tableDescriptor);
            }
            finally
            {
                admin.close();
            }
        }
        finally
        {
            Cleaner.release(featureClass);
        }
        return null;
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
