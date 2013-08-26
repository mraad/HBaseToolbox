package com.esri;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.hbase.HBaseConfiguration;
import org.apache.hadoop.hbase.HColumnDescriptor;
import org.apache.hadoop.hbase.HTableDescriptor;
import org.apache.hadoop.hbase.MiniHBaseCluster;
import org.apache.hadoop.hbase.client.HBaseAdmin;
import org.apache.hadoop.hbase.client.HTableInterface;
import org.apache.hadoop.hbase.client.HTablePool;
import org.apache.hadoop.hbase.zookeeper.MiniZooKeeperCluster;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.net.ServerSocket;

/**
 * Borrowed heavily from the Hive/HBaseHandler !
 */
public class ZKHBaseTest
{
    private final static String TAB_NAME = "features";

    private Configuration m_configuration;
    private MiniZooKeeperCluster m_zooKeeperCluster;
    private MiniHBaseCluster m_hbaseCluster;

    @Before
    public void setUp() throws IOException, InterruptedException
    {
        m_configuration = HBaseConfiguration.create();

        final File file = new File("/tmp/hbase");
        m_configuration.set("hbase.rootdir", file.toURI().toURL().toString());
        m_configuration.setInt("hbase.master.port", findFreePort());
        m_configuration.setInt("hbase.master.info.port", -1);
        m_configuration.setInt("hbase.regionserver.port", findFreePort());
        m_configuration.setInt("hbase.regionserver.info.port", -1);

        if (m_zooKeeperCluster == null)
        {
            m_zooKeeperCluster = new MiniZooKeeperCluster();
            final int zooKeeperPort = m_zooKeeperCluster.startup(new File("/tmp", "zookeeper"));
            m_configuration.setInt("hbase.zookeeper.property.clientPort", zooKeeperPort);
        }

        if (m_hbaseCluster == null)
        {
            m_hbaseCluster = new MiniHBaseCluster(m_configuration, 1);
            m_configuration.set("hbase.master", m_hbaseCluster.getMaster().getServerName().getHostAndPort());
        }

        createTable();
    }

    private void createTable() throws IOException
    {
        final HBaseAdmin admin = new HBaseAdmin(m_configuration);
        try
        {
            if (admin.tableExists(TAB_NAME))
            {
                admin.disableTable(TAB_NAME);
                admin.deleteTable(TAB_NAME);
            }
            final HTableDescriptor tableDescriptor = new HTableDescriptor(TAB_NAME);
            final HColumnDescriptor columnDescriptor = new HColumnDescriptor(Const.GEOM);
            columnDescriptor.setMaxVersions(1);
            tableDescriptor.addFamily(columnDescriptor);
            admin.createTable(tableDescriptor);
        }
        finally
        {
            admin.close();
        }
    }

    private int findFreePort() throws IOException
    {
        final ServerSocket server = new ServerSocket(0);
        final int port;
        try
        {
            port = server.getLocalPort();
        }
        finally
        {
            server.close();
        }
        return port;
    }

    @Ignore
    @Test
    public void testCreate() throws IOException
    {
        final HTablePool tablePool = new HTablePool(m_configuration, 2);
        try
        {
            final HTableInterface table = tablePool.getTable(TAB_NAME);
            try
            {
            }
            finally
            {
                table.close();
            }
        }
        finally
        {
            tablePool.close();
        }
    }

    @After
    public void tearDown() throws IOException
    {
        if (m_hbaseCluster != null)
        {
            m_hbaseCluster.shutdown();
            m_hbaseCluster.waitUntilShutDown();
            m_hbaseCluster = null;
        }
        if (m_zooKeeperCluster != null)
        {
            m_zooKeeperCluster.shutdown();
            m_zooKeeperCluster = null;
        }
    }
}
