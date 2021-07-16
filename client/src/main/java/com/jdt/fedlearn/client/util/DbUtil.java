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

package com.jdt.fedlearn.client.util;

import com.jdt.fedlearn.client.entity.source.DbConfig;
import com.jdt.fedlearn.client.entity.source.DataSourceConfig;
import com.jdt.fedlearn.client.entity.source.DbSourceConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;


/**
 * mysql and sqlite jdbc 数据库连接示例
 */
public class DbUtil {
    private static final Logger logger = LoggerFactory.getLogger(DbUtil.class);

    /**
     * 私有化构造器，防止用户重复创建对象，造成资源浪费。
     */
    private DbUtil() {

    }

    public static Connection getConnection(DataSourceConfig config) {
        String username;
        String password;
        String url;
        String driver;
        Connection con = null;
        try {
            if (config == null) {
                DbConfig dbconfig = ConfigUtil.getInferenceDbProperties();
                username = dbconfig.getUsername();
                password = dbconfig.getPassword();
                url = dbconfig.getUrl();
                driver = "com.mysql.jdbc.Driver";
            } else {
                DbSourceConfig paramMap = getConf(config);
                username = paramMap.getUsername();
                password = paramMap.getPassword();
                url = paramMap.getUrl();
                driver = paramMap.getDriver();
            }
            Class.forName(driver);
            con = DriverManager.getConnection(url, username, password);
        } catch (SQLException e) {
            logger.error("db 连接失败", e);
        } catch (ClassNotFoundException e) {
            logger.error("ClassNotFoundException ", e);
        }
        return con;
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

    private static DbSourceConfig getConf(DataSourceConfig config) {
        return (DbSourceConfig) config;
    }

}
