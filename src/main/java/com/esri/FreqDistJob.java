package com.esri;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.conf.Configured;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.hbase.HBaseConfiguration;
import org.apache.hadoop.io.IntWritable;
import org.apache.hadoop.io.LongWritable;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.lib.input.FileInputFormat;
import org.apache.hadoop.mapreduce.lib.input.TextInputFormat;
import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat;
import org.apache.hadoop.mapreduce.lib.output.TextOutputFormat;
import org.apache.hadoop.util.GenericOptionsParser;
import org.apache.hadoop.util.Tool;
import org.apache.hadoop.util.ToolRunner;

import java.io.IOException;

/**
 * mvn -P job clean package
 * hadoop fs -rm -R -skipTrash output
 * hadoop jar target/HBaseToolbox-1.0-SNAPSHOT-job.jar /user/cloudera/points.txt /user/cloudera/output
 */
public class FreqDistJob extends Configured implements Tool
{
    public static void main(final String[] args) throws Exception
    {
        System.exit(ToolRunner.run(new FreqDistJob(), args));
    }

    public static Job createSubmittableJob(
            final Configuration configuration,
            final String[] args
    ) throws IOException
    {
        final Job job = Job.getInstance(configuration, FreqDistJob.class.getSimpleName());

        job.setJarByClass(FreqDistJob.class);

        FileInputFormat.addInputPath(job, new Path(args[0]));
        FileOutputFormat.setOutputPath(job, new Path(args[1]));

        job.setInputFormatClass(TextInputFormat.class);

        job.setMapperClass(FreqDistMapper.class);
        job.setMapOutputKeyClass(LongWritable.class);
        job.setMapOutputValueClass(IntWritable.class);

        job.setCombinerClass(FreqDistReducer.class);

        job.setReducerClass(FreqDistReducer.class);
        job.setOutputKeyClass(LongWritable.class);
        job.setOutputValueClass(IntWritable.class);

        job.setOutputFormatClass(TextOutputFormat.class);

        return job;
    }

    @Override
    public int run(final String[] args) throws Exception
    {
        setConf(HBaseConfiguration.create(getConf()));
        final String[] remainingArgs = new GenericOptionsParser(getConf(), args).getRemainingArgs();
        final int rc;
        if (remainingArgs.length != 2)
        {
            System.err.println("Arguments format: input-path output-path");
            ToolRunner.printGenericCommandUsage(System.err);
            rc = -1;
        }
        else
        {
            rc = createSubmittableJob(getConf(), remainingArgs).waitForCompletion(true) ? 0 : 1;
        }
        return rc;
    }
}
