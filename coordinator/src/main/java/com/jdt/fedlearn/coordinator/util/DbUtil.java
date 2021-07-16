/* Copyright 2020 The FedLearn Authors. All Rights Reserved.

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at
http://www.apache.org/licenses/LICENSE-2.0
Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
*/

package com.jdt.fedlearn.coordinator.util;

import com.jdt.fedlearn.coordinator.entity.DbConfig;
import com.jdt.fedlearn.coordinator.type.DbType;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;


/**
 * mysql and sqlite jdbc 数据库连接示例
 */
public class DbUtil {
    private static final Logger logger = LoggerFactory.getLogger(DbUtil.class);

    private static volatile HikariDataSource hds;
    private static HikariConfig hikariConfig;
    private static DbType type;

    static {
        //创建获取配置文件对象。
        DbConfig config = ConfigUtil.getDbProperties();
        hikariConfig = new HikariConfig();
        hikariConfig.setJdbcUrl(config.getUrl());
        hikariConfig.setDriverClassName(config.getDriver());
        hikariConfig.setUsername(config.getUsername());
        hikariConfig.setPassword(config.getPassword());
        hikariConfig.setMaximumPoolSize(Integer.parseInt(config.getMaxPoolSize()));
        hikariConfig.setMinimumIdle(Integer.parseInt(config.getMinIdle()));
        hikariConfig.setLeakDetectionThreshold(Long.parseLong(config.getLeakDetectionThreshold()));
        type = config.getDbType();
    }

    /**
     * 私有化构造器，防止用户重复创建对象，造成资源浪费。
     */
    private DbUtil() {

    }

    //创建数据库连接对象
    public static Connection getConnection() {
        try {
            if (hds == null) {
                synchronized (HikariDataSource.class) {
                    if (hds == null) {
                        hds = new HikariDataSource(hikariConfig);
                    }
                }
            }
            return hds.getConnection();
        } catch (SQLException e) {
            logger.error("获取数据库连接异常}", e);
        }
        return null;
    }

    //关闭对应数据流
    public static void close(Connection con, PreparedStatement ps, ResultSet rs) {
        if (con != null) {
            try {
                con.close();
            } catch (SQLException e) {
                logger.error("conn关闭异常", e);
            }
        }
        if (ps != null) {
            try {
                ps.close();
            } catch (SQLException e) {
                logger.error("ps关闭异常", e);
            }
        }
        if (rs != null) {
            try {
                rs.close();
            } catch (SQLException e) {
                logger.error("rs关闭异常", e);
            }
        }
    }


    public static void close(PreparedStatement ps, ResultSet rs) {
        if (ps != null) {
            try {
                ps.close();
            } catch (SQLException e) {
                logger.error("ps关闭异常", e);
            }
        }
        if (rs != null) {
            try {
                rs.close();
            } catch (SQLException e) {
                logger.error("rs关闭异常", e);
            }
        }
    }

    public static DbType getDbType() {
        return type;
    }

}
