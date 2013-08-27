package com.esri;

import org.apache.hadoop.io.IntWritable;
import org.apache.hadoop.io.LongWritable;
import org.apache.hadoop.mapreduce.Reducer;

import java.io.IOException;

public final class FreqDistReducer extends Reducer<LongWritable, IntWritable, LongWritable, IntWritable>
{
    @Override
    public void reduce(
            final LongWritable key,
            final Iterable<IntWritable> values,
            final Context context)
            throws IOException, InterruptedException
    {
        int sum = 0;
        for (final IntWritable val : values)
        {
            sum += val.get();
        }
        context.write(key, new IntWritable(sum));
    }
}
