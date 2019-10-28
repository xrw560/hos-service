package com.runisys.web;

import com.runisys.HosConfiguration;
import com.runisys.server.HdfsServiceImpl;
import com.runisys.server.HosStoreImpl;
import com.runisys.server.IHosStore;
import org.apache.hadoop.hbase.HBaseConfiguration;
import org.apache.hadoop.hbase.client.Connection;
import org.apache.hadoop.hbase.client.ConnectionFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.IOException;

@Configuration
public class HosServerBeanConfiguration {


    //获取HBase connection 注入bean
    @Bean
    public Connection getConnection() throws IOException{
        org.apache.hadoop.conf.Configuration configuration = HBaseConfiguration.create();
        HosConfiguration hosConfiguration = HosConfiguration.getConfiguration();
        configuration.set("hbase.zookeeper.quorum",hosConfiguration.getString("hbase.zookeeper.quorum"));
        configuration.set("hbase.zookeeper.property.clientPort",hosConfiguration.getString("hbase.zookeeper.property.clientPort"));
        return ConnectionFactory.createConnection(configuration);
    }

    //实例化一个HosStore实例
    @Bean("hosStore")
    public IHosStore getHosStore(@Autowired Connection connection) throws Exception {
        HosConfiguration hosConfiguration = HosConfiguration.getConfiguration();
        String zkHosts = hosConfiguration.getString("hbase.zooeeper.quorum");
        HosStoreImpl hosStore = new HosStoreImpl(connection, new HdfsServiceImpl(), zkHosts);
        return hosStore;
    }

}
