# HBaseToolbox

ArcObject toolbox to work with an [HBase](http://hbase.apache.org) instance.

## Assumptions

This toolbox assumes that you already have [ArcGIS for Desktop](http://www.esri.com/software/arcgis/arcgis-for-desktop), and an HBase instance.
For the latter, and for local testing purposes, I've been using the [Cloudera Quick Start VM](http://www.cloudera.com/content/support/en/downloads/download-components/download-products.html?productID=F6mO278Rvo).

In addition, this maven project depends on the file *arcobjects.jar* (mine is at C:\Program Files (x86)\ArcGIS\Desktop10.1\java\lib).
Install it in your local maven repo as follows:

    $ mvn install:install-file \
     -Dfile=arcobjects.jar\
     -DgroupId=com.esri\
     -DartifactId=arcobjects\
     -Dversion=10.1\
     -Dpackaging=jar\
     -DgeneratePom=true

### Dependencies

    $ git clone https://github.com/kungfoo/geohash-java.git
    $ mvn install

## Build and package

    $ mvn avro:schema
    $ mvn package

Copy from the **target** folder the **SNAPSHOT.jar** file and the **libs** folder into the C:\Program Files (x86)\ArcGIS\Desktop10.1\java\lib\ext folder.

Before starting ArcMap, you have to adjust the ArcGIS JVM Heap values. Run as **administrator** JavaConfigTool located in C:\Program Files (x86)\ArcGIS\Desktop10.1\bin

![JavaConfigTool](https://dl.dropboxusercontent.com/u/2193160/JavaConfigTool.png)

Check out [this documentation](http://help.arcgis.com/en/arcgisdesktop/10.0/help/index.html#/A_quick_tour_of_managing_tools_and_toolboxes/003q00000001000000/) to see how to add a Toolbox and a Tool to ArcMap.

Start ArcMap. Create a toolbox named *HBaseToolbox*, and add to it the *ExportToHBaseTool* and the *CreateHTableTool*.

## CreateHTableTool

![CreateHTableTool](https://dl.dropboxusercontent.com/u/2193160/CreateHTableTool.png)

Tool to create a table in HBase with two column families:

* geom - CF to hold the feature geometry
* attr - CF to hold the feature attributes

Here is a sample _hadoop.properties_ content:

    fs.default.name=hdfs\://localhost\:8020
    fs.defaultFS=hdfs\://localhost\:8020
    dfs.client.use.legacy.blockreader=true
    dfs.replication=1

## ExportToHBaseTool

![ExportToHBaseTool](https://dl.dropboxusercontent.com/u/2193160/ExportToHBaseTool.png)

Tool to export a feature class into an HBase table.

## Hive Integration

Once a feature class is exported into an HBase table, it can be [mapped into a Hive table](https://cwiki.apache.org/confluence/display/Hive/HBaseIntegration) for SQL-like queries.
Please note the suffixes _#b_ and _#s_ in each column mapping to indicate a binary or string implementation.

Here is a sample hive session:

    $ hive
    hive> add jar /usr/lib/hive/lib/hbase.jar;
    hive> add jar /usr/lib/hive/lib/zookeeper.jar;
    hive> add jar /usr/lib/hive/lib/guava-11.0.2.jar;
    hive> add jar /usr/lib/hive/lib/hive-hbase-handler-0.10.0-cdh4.3.0.jar;

    hive> DROP TABLE IF EXISTS worldlabels;
    hive> CREATE EXTERNAL TABLE IF NOT EXISTS worldlabels (key int, x double, y double, name string)
    STORED BY 'org.apache.hadoop.hive.hbase.HBaseStorageHandler'
    WITH SERDEPROPERTIES (
    "hbase.columns.mapping" = ":key#b,geom:x#b,geom:y#b,attr:NAME#s"
    ) TBLPROPERTIES ("hbase.table.name" = "worldlabels");

    hive> select * from worldlabels limit 10;
    hive> select x,y from worldlabels where name='Lebanon';

## Impala Integration

Once an HBase table is defined in Hive, it can be also consumed from [Impala](http://www.cloudera.com/content/cloudera-content/cloudera-docs/Impala/latest/Installing-and-Using-Impala/ciiu_impala_hbase.html).

    $ impala-shell

    [localhost:21000] > invalidate metadata;
    [localhost:21000] > select avg(x),avg(y) from worldlabels where key between 100 and 200;
    Query: select avg(x),avg(y) from worldlabels where key between 100 and 200
    Query finished, fetching results ...
    +-------------------+-------------------+
    | _c0               | _c1               |
    +-------------------+-------------------+
    | 24.97849666542185 | 14.59178906611481 |
    +-------------------+-------------------+

## Geometry Formats

The HBase tables are made up of two column families: _geom_ and _attr_.
This is a naive implementation of a feature separation into column families, as it is assuming that the geometry is accessed most of the time without its attributes.
Nevertheless, The attribute column family is populated by the feature class non-geometry field and are qualified by their field names.
When it came down to qualifying and populating the geometry column family, I wanted to experiment with different storage format.

There exists a configuration property named _exportToHBase.shapeWriterType_ with the following values that enables me to switch storage formats:

* bytes - write each column qualifier value as a byte array.
* geojson - write each column qualifier value in [GeoJSON](http://www.geojson.org/geojson-spec.html) format.
* avro - Write each column qualifier value in [Avro](http://avro.apache.org/docs/current/) format.

### Avro Format

I defined 3 Avro [schemas](http://avro.apache.org/docs/current/spec.html) to represent points, polylines and polygons.
This project relies on the specific implementation of these schemas.
So, to convert the schemas into concrete classes, execute the following command:

    $ mvn avro:schema

## Spatial Query

/opt/cloudera/parcels/CDH-4.3.1-1.cdh4.3.1.p0.110/lib/hbase/lib