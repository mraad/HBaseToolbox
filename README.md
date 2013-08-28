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

* bytes - Write each column qualifier value as a byte array.
* geojson - Write a column qualifier value in [GeoJSON](http://www.geojson.org/geojson-spec.html) format.
* avro - Write a column qualifier value in [Avro](http://avro.apache.org/docs/current/) format.
* noop - Do not write any values.

### Avro Format

I defined 3 Avro [schemas](http://avro.apache.org/docs/current/spec.html) to represent points, polylines and polygons.
This project relies on the specific implementation of these schemas.
So, to convert the schemas into concrete classes, execute the following command:

    $ mvn avro:schema

## RowKey Generation

The most important factor in building an HBase table is the design of the RowKey as it heavily affects _Scan_ operations.
To create spatial locality, I decided to use [Geohash](http://en.wikipedia.org/wiki/Geohash) like in the book [Hbase In Action](http://www.manning.com/dimidukkhurana/).
I extended the author's implementation, and created a compound key made up of the binary representation of the geohash, followed by the explicit location and a unique identifier.

    m_dataOutput.writeLong(Quad.encode(x, y));
    m_dataOutput.writeDouble(x);
    m_dataOutput.writeDouble(y);
    m_dataOutput.writeInt(feature.getOID());

I like this implementation as it gives me spatial locality, range and uniqueness.
Range because the geohash code is in the front. Uniqueness because multiple points can gave the same geohash code, thus
the writing of the x and y, and to make it further unique (think bunch of customer locations in the same tall building :-), I write the ObjectID.
In addition, it has one extra bit, where in the case of point features, I do not have to store the geometry in a column family and when it comes to filtered scans, a row can be passed
through or not by the first filter method [filterRowKey](http://my.safaribooksonline.com/book/databases/database-design/9781449314682/filters/id4460220#X2ludGVybmFsX0h0bWxWaWV3P3htbGlkPTk3ODE0NDkzMTQ2ODIlMkZpZDMyOTQwNzgmcXVlcnk9)

I've been using the Cloudera Manager in [Cloudera VM](http://www.cloudera.com/content/support/en/downloads/download-components/download-products.html?productID=F6mO278Rvo) for all my experiments.
And per my understanding (and could be wrong), a jar containing the filter code can be placed in a "static" location and referenced using the *HBASE_CLASSPATH* in the *hbase-env.sh*.
This solution never worked for me. I had to explicitly copy the jars to the parcel location and restart the HBase services.

    sudo cp target/geohash-1.0.8.jar /opt/cloudera/parcels/CDH-4.3.1-1.cdh4.3.1.p0.110/lib/hbase/lib
    sudo cp target/HBaseToolbox-1.0-SNAPSHOT.jar /opt/cloudera/parcels/CDH-4.3.1-1.cdh4.3.1.p0.110/lib/hbase/lib

## MapReduce Spatial Joins Operations

Typically, when you want to perform a spatial join between two datasets in a MapReduce implementation, the smaller set is
loaded into an in memory spatial index and you stream through the bigger set in the map or reduce phase while performing
spatial searches on the index. Now this is fine is the set can fit in the mapper or reducer memory space. what if it cannot ?
this is where HBase can come to the rescue with the above rowkey implementation. Remember, Hbase is a fast massive in-memory
key-value lookup engine. So we can use the range scan and the filter to quickly locate items in say a bounding box.
See bounding boxes are nice as they translate to geohash ranges.  The book talks about this in more detail.

Actually, I used this technique to solve a business problem where I was given two huge sets of data points and was asked
to find the distribution of the frequency of the distances of the two closest points from each set within a specified distance.
Basically, give a point from the first set, find the closest point in the second set and keep a tally of that rounded distance.

The following is a mock implementation as I cannot share the real data.
So, I wrote a standalone program to load the lookup table (LUT) data into HBase:

    $ mvn exec:java -q -Dexec.mainClass=com.esri.CreatePutLUT -Dexec.args="1000"

Here is an [AWK](http://en.wikipedia.org/wiki/AWK) script to simulate the other data set in HDFS:

    $ awk -f points.awk | hadoop fs -put - points.txt

And finally, a MapReduce Job that will perform the spatial search:

    $ mvn -P job clean package
    $ hadoop fs -rm -R -skipTrash output
    $ hadoop jar target/HBaseToolbox-1.0-SNAPSHOT-job.jar /user/cloudera/points.txt /user/cloudera/output
    $ hadoop fs -cat output/part-r-00000

## Future Experiments

* Handle polyline and polygons RowKey generation by embedding bounding box
* Expand Quad code implementation to handle bounding boxes
* Try different quad code implementation such as [Morton](http://en.wikipedia.org/wiki/Z-order_curve).
* Dynamic Generation of Spatial Index
* Geo-Enrichment application
