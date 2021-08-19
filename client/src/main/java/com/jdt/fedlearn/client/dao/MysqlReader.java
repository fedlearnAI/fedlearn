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

package com.jdt.fedlearn.client.dao;

import com.jdt.fedlearn.client.entity.source.DataSourceConfig;
import com.jdt.fedlearn.client.entity.source.DbSourceConfig;
import com.jdt.fedlearn.client.util.DbUtil;
import com.jdt.fedlearn.common.util.JsonUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class MysqlReader implements DataReader {
    private static final Logger logger = LoggerFactory.getLogger(MysqlReader.class);

    public String[][] loadTrain(DataSourceConfig config) {
        logger.info("enter mysql load train data");
        String trainTableName = getConfig(config);
        String[] headers = getColumnNames(trainTableName, config).toArray(new String[0]);

        List<String[]> featureAnswers = new ArrayList<>();
        String[] columns = Arrays.copyOfRange(headers, 1, headers.length);
        logger.info("columns :" + Arrays.toString(columns));
//        featureAnswers.add(new String[]{"uid", "col0", "col1", "col2", "col3", "col4", "col5", "col6", "col7", "col8", "col9", "col10", "col11", "col12", "col13"});
        featureAnswers.add(columns);
        PreparedStatement ps = null;
        ResultSet resultSet = null;
        DbSourceConfig dbSourceConfig = (DbSourceConfig)config;
        Connection conn = DbUtil.getConnection(dbSourceConfig);
        try {
//            String mysql = "select uid,col0,col1,col2,col3,col4,col5,col6,col7,col8,col9,col10,col11,col12,col13 from " + trainTableName;
            String column = String.join(",", columns);
            String mysql = "select " + column + " from " + trainTableName;
            logger.info("mysql: " + mysql);
            if (conn != null) {
                ps = conn.prepareStatement(mysql);
                resultSet = ps.executeQuery();

                while (resultSet.next()) {
                    String[] answer = new String[columns.length];
                    for (int i = 0; i < answer.length; i++) {
                        answer[i] = resultSet.getString(i + 1);
                    }
                    logger.info("ans: " + Arrays.toString(answer));
                    featureAnswers.add(answer);
                }
                ps.close();
                logger.info("select click method: messageId=" + trainTableName);
            }
        } catch (SQLException e) {
            logger.error("select click error with: messageId=" + trainTableName);
            logger.error("select error: ", e);
        } finally {
            // 关闭
            DbUtil.close(conn, ps, resultSet);
        }
        String[][] data = featureAnswers.toArray(new String[0][]);
        logger.info("featureAnswers.toArray(new String[0][]): " + data.length);
        for (int i = 0; i < 20; i++) {
            logger.info("featureAnswers.toArray top20 : " + Arrays.toString(data[i]));
        }
        for (int i = data.length - 20; i < data.length; i++) {
            logger.info("featureAnswers.toArray last20 : " + Arrays.toString(data[i]));
        }
//        logger.info("featureAnswers.toArray : " + Arrays.toString(data[1]));
        return data;
    }


    public String[][] loadInference(DataSourceConfig config, String[] uid) {
        List<String[]> featureAnswers = new ArrayList<>();
        DbSourceConfig sourceConfig = (DbSourceConfig)config;
        String tableName = sourceConfig.getTable();
        String[] headers = getColumnNames(tableName, null).toArray(new String[0]);
        String[] columns = Arrays.copyOfRange(headers, 1, headers.length);
        String column = String.join(",", columns);
        String condition = "where uid in (";
        StringBuilder queryBuilder = new StringBuilder(condition);
        String[] conditions = new String[uid.length];
        for (int i = 0; i < uid.length; i++) {
            conditions[i] = " ? ";
        }
        queryBuilder.append(String.join(",", conditions)).append(")");
        String mysql = "select " + column + " from " + tableName + "  " + queryBuilder.toString();
        logger.info("inference load query:" + mysql);
        PreparedStatement ps = null;
        ResultSet resultSet = null;
        DbSourceConfig dbSourceConfig = (DbSourceConfig)config;
        Connection conn = DbUtil.getConnection(dbSourceConfig);
        try {
            if (conn != null) {
                ps = conn.prepareStatement(mysql);
//                ps.setString(1, inferenceTableName);
                for (int i = 0; i < uid.length; i++) {
                    ps.setString(i + 1, uid[i]);
                }
                resultSet = ps.executeQuery();
                featureAnswers.add(columns);
                while (resultSet.next()) {
                    String[] answer = new String[columns.length];
                    for (int i = 0; i < answer.length; i++) {
                        answer[i] = resultSet.getString(i + 1);
                    }
                    featureAnswers.add(answer);
                }
                ps.close();
                logger.info("select click method: messageId={},数据=【{}】", tableName, JsonUtil.object2json(featureAnswers));
//                logger.info("select click method: messageId=" + trainTableName);
            }
        } catch (SQLException e) {
            logger.error("select click error with: messageId=" + tableName);
            logger.error(e.toString());
        } catch (Exception e) {
            logger.error("select error: ", e);
        } finally {
            // 关闭
            DbUtil.close(conn, ps, resultSet);
        }
        return featureAnswers.toArray(new String[0][]);
    }

    public String[][] loadValidate(DataSourceConfig config, String[] uid) {
        List<String[]> featureAnswers = new ArrayList<>();
        DbSourceConfig sourceConfig = (DbSourceConfig)config;
        String tableName = sourceConfig.getTable();
        String[] headers = getColumnNames(tableName, null).toArray(new String[0]);
        String[] columns = Arrays.copyOfRange(headers, 1, headers.length);
        String column = String.join(",", columns);
        String condition = "where uid in (";
        StringBuilder queryBuilder = new StringBuilder(condition);
        String[] conditions = new String[uid.length];
        for (int i = 0; i < uid.length; i++) {
            conditions[i] = " ? ";
        }
        queryBuilder.append(String.join(",", conditions)).append(")");
        String mysql = "select " + column + " from " + tableName + "  " + queryBuilder.toString();
        logger.info("inference load query:" + mysql);
        PreparedStatement ps = null;
        ResultSet resultSet = null;
        Connection conn = DbUtil.getConnection(null);
        try {
            if (conn != null) {
                ps = conn.prepareStatement(mysql);
//                ps.setString(1, inferenceTableName);
                for (int i = 0; i < uid.length; i++) {
                    ps.setString(i + 1, uid[i]);
                }
                resultSet = ps.executeQuery();
                featureAnswers.add(columns);
                while (resultSet.next()) {
                    String[] answer = new String[columns.length];
                    for (int i = 0; i < answer.length; i++) {
                        answer[i] = resultSet.getString(i + 1);
                    }
                    featureAnswers.add(answer);
                }
                ps.close();
                logger.info("select click method: messageId={},数据=【{}】", tableName, JsonUtil.object2json(featureAnswers));
//                logger.info("select click method: messageId=" + trainTableName);
            }
        } catch (SQLException e) {
            logger.error("select click error with: messageId=" + tableName);
            logger.error(e.toString());
        } catch (Exception e) {
            logger.error("select error: ", e);
        } finally {
            // 关闭
            DbUtil.close(conn, ps, resultSet);
        }
        return featureAnswers.toArray(new String[0][]);
    }


    public String[] loadHeader(DataSourceConfig config) {
        DbSourceConfig dbConfig = (DbSourceConfig) config;
        List<String> dbheader = getColumnNames(dbConfig.getTable(), dbConfig);
        String[] header = dbheader.toArray(new String[dbheader.size()]);
        String[] columns = Arrays.copyOfRange(header, 1, header.length);
        return columns;
    }

    /**
     * 获取表中所有字段名称
     *
     * @param tableName 表名
     * @return
     */
    public static List<String> getColumnNames(String tableName, DataSourceConfig config) {
        List<String> columnNames = new ArrayList<>();
        //与数据库的连接
        PreparedStatement pStemt = null;
        DbSourceConfig dbSourceConfig = (DbSourceConfig)config;
        Connection conn = DbUtil.getConnection(dbSourceConfig);
        String tableSql = "SELECT * FROM " + tableName + " LIMIT 1";
        try {
            pStemt = conn.prepareStatement(tableSql);
            //结果集元数据
            ResultSetMetaData rsmd = pStemt.getMetaData();
            //表列数
            int size = rsmd.getColumnCount();
            for (int i = 0; i < size; i++) {
                columnNames.add(rsmd.getColumnName(i + 1));
            }
        } catch (SQLException e) {
            logger.error("getColumnNames failure", e);
        } finally {
            DbUtil.close(conn, pStemt, null);
        }
        return columnNames;
    }

    private static String getConfig(DataSourceConfig config) {
        try {
            DbSourceConfig dbConfig = (DbSourceConfig) config;
            String trainTableName = (dbConfig).getTable();
            return trainTableName;
        } catch (Exception e) {
            logger.error("类加载失败", e);
            return null;
        }
    }
}