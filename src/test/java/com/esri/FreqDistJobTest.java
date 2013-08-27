package com.esri;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.hadoop.fs.FSDataOutputStream;
import org.apache.hadoop.fs.FileStatus;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.hbase.HBaseTestingUtility;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import java.io.IOException;

/**
 */
public class FreqDistJobTest
{
    private final Log m_log = LogFactory.getLog(getClass());
    private final HBaseTestingUtility m_testUtil = new HBaseTestingUtility();

    @Before
    public void setUp() throws Exception
    {
        m_testUtil.startMiniCluster();
        m_testUtil.startMiniMapReduceCluster();
    }

    @Ignore // TODO - not working as expected
    @Test
    public void testFreqDistJob()
    {
        try
        {
            m_testUtil.createTable(Const.LUT, Const.ATTR);
            try
            {
                writeInput();

                final String[] args = {"points.txt", "output"};
                m_log.info("Submitting....");
                FreqDistJob.createSubmittableJob(m_testUtil.getConfiguration(), args).waitForCompletion(true);
                m_log.info("Completed !");

                readOutput();
            }
            finally
            {
                m_testUtil.deleteTable(Const.LUT);
            }
        }
        catch (Throwable t)
        {
            m_log.error(t.toString(), t);
        }
    }

    private void readOutput() throws IOException
    {
        m_log.info("Read output...");

        final FileSystem fileSystem = FileSystem.get(m_testUtil.getConfiguration());
        final Path path = new Path("output");
        if (fileSystem.exists(path))
        {
            for (final FileStatus fileStatus : fileSystem.listStatus(path))
            {
                m_log.info(fileStatus.getPath().getName());
            }
        }
        else
        {
            m_log.info("Folder 'output' does not exist !");
        }
    }

    private void writeInput() throws IOException
    {
        m_log.info("Write input...");

        final FileSystem fileSystem = FileSystem.get(m_testUtil.getConfiguration());
        final FSDataOutputStream fsDataOutputStream = fileSystem.create(new Path("points.txt"), true);
        try
        {
            fsDataOutputStream.writeChars("0\t0.0\t0.0\n");
        }
        finally
        {
            fsDataOutputStream.close();
        }
    }

    @After
    public void tearDown() throws Exception
    {
        m_testUtil.shutdownMiniMapReduceCluster();
        m_testUtil.shutdownMiniCluster();
    }

}
