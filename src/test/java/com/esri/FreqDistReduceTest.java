package com.esri;

import org.apache.hadoop.io.IntWritable;
import org.apache.hadoop.io.LongWritable;
import org.apache.hadoop.mrunit.mapreduce.ReduceDriver;
import org.junit.Test;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 */
public final class FreqDistReduceTest
{
    @Test
    public void testFreqDistReducer() throws IOException
    {
        final ReduceDriver<
                LongWritable, IntWritable,
                LongWritable, IntWritable
                > reduceDriver = ReduceDriver.newReduceDriver(new FreqDistReducer());

        final List<IntWritable> list = new ArrayList<IntWritable>();
        list.add(FreqDistMapper.ONE);
        list.add(FreqDistMapper.ONE);
        list.add(FreqDistMapper.ONE);

        final LongWritable key = new LongWritable();
        reduceDriver.withInput(key, list);
        reduceDriver.withOutput(key, new IntWritable(3));
        reduceDriver.runTest();
    }

}
