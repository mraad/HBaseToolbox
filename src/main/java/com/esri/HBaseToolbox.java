package com.esri;

import com.esri.arcgis.geodatabase.IEnumGPName;
import com.esri.arcgis.geodatabase.IGPName;
import com.esri.arcgis.geoprocessing.EnumGPName;
import com.esri.arcgis.geoprocessing.GPFunctionName;
import com.esri.arcgis.geoprocessing.IEnumGPEnvironment;
import com.esri.arcgis.geoprocessing.IGPFunction;
import com.esri.arcgis.geoprocessing.IGPFunctionFactory;
import com.esri.arcgis.interop.AutomationException;
import com.esri.arcgis.interop.extn.ArcGISCategories;
import com.esri.arcgis.interop.extn.ArcGISExtension;
import com.esri.arcgis.system.IUID;
import com.esri.arcgis.system.UID;

import java.io.IOException;
import java.util.UUID;

/**
 */
@ArcGISExtension(categories = {ArcGISCategories.GPFunctionFactories})
public final class HBaseToolbox implements IGPFunctionFactory
{
    private static final long serialVersionUID = 2385676665599961990L;

    private static final String NAME = HBaseToolbox.class.getSimpleName();

    public IUID getCLSID() throws IOException, AutomationException
    {
        final UID uid = new UID();
        uid.setValue("{" + UUID.nameUUIDFromBytes(this.getClass().getName().getBytes()) + "}");
        return uid;
    }

    public String getName() throws IOException, AutomationException
    {
        return NAME;
    }

    public String getAlias() throws IOException, AutomationException
    {
        return NAME;
    }

    public IGPFunction getFunction(final String s) throws IOException, AutomationException
    {
        if (ExportToHBaseTool.NAME.equalsIgnoreCase(s))
        {
            return new ExportToHBaseTool();
        }
        if (CreateHTableTool.NAME.equalsIgnoreCase(s))
        {
            return new CreateHTableTool();
        }
        return null;
    }

    public IGPName getFunctionName(final String s) throws IOException, AutomationException
    {
        if (ExportToHBaseTool.NAME.equalsIgnoreCase(s))
        {
            final GPFunctionName functionName = new GPFunctionName();
            functionName.setCategory(ExportToHBaseTool.NAME);
            functionName.setDescription(ExportToHBaseTool.NAME);
            functionName.setDisplayName(ExportToHBaseTool.NAME);
            functionName.setName(ExportToHBaseTool.NAME);
            functionName.setFactoryByRef(this);
            return functionName;
        }
        if (CreateHTableTool.NAME.equalsIgnoreCase(s))
        {
            final GPFunctionName functionName = new GPFunctionName();
            functionName.setCategory(CreateHTableTool.NAME);
            functionName.setDescription(CreateHTableTool.NAME);
            functionName.setDisplayName(CreateHTableTool.NAME);
            functionName.setName(CreateHTableTool.NAME);
            functionName.setFactoryByRef(this);
            return functionName;
        }
        return null;
    }

    public IEnumGPName getFunctionNames() throws IOException, AutomationException
    {
        final EnumGPName nameArray = new EnumGPName();
        nameArray.add(getFunctionName(ExportToHBaseTool.NAME));
        nameArray.add(getFunctionName(CreateHTableTool.NAME));
        return nameArray;
    }

    public IEnumGPEnvironment getFunctionEnvironments() throws IOException, AutomationException
    {
        return null;
    }
}