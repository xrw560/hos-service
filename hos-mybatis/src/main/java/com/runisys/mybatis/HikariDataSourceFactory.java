package com.runisys.mybatis;

import com.zaxxer.hikari.HikariDataSource;
import org.apache.ibatis.datasource.unpooled.UnpooledDataSourceFactory;

public class HikariDataSSourceFactory extends UnpooledDataSourceFactory {

    public HikariDataSSourceFactory() {
        this.dataSource = new HikariDataSource();
    }

}
