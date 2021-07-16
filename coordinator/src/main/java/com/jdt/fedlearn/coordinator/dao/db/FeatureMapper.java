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

package com.jdt.fedlearn.coordinator.dao.db;

import com.jdt.fedlearn.coordinator.entity.table.FeatureAnswer;
import com.jdt.fedlearn.coordinator.entity.table.PartnerProperty;
import com.jdt.fedlearn.coordinator.util.DbUtil;
import com.jdt.fedlearn.core.entity.feature.Features;
import com.jdt.fedlearn.core.entity.feature.SingleFeature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * 特征表，  is_index, 作为该字段特征是否是uid列的标记
 * 所有插入和查询数据均需要 考虑该新增字段
 */
public class FeatureMapper {
    private static final Logger logger = LoggerFactory.getLogger(FeatureMapper.class);

    //
    public static void insertFeature(int task_id, String username, String feature, String feature_type, String feature_describe, boolean isIndex, String dep_user, String dep_feature) {
        PreparedStatement ps = null;
        Connection conn = DbUtil.getConnection();
        try {
            String sql = "INSERT INTO feature_list(task_id, username, feature, feature_type, feature_describe, is_index, dep_user, dep_feature) VALUES (?,?,?,?,?,?,?,?)";
            if (conn != null) {
                ps = conn.prepareStatement(sql);
                ps.setInt(1, task_id);
                ps.setString(2, username);
                ps.setString(3, feature);
                ps.setString(4, feature_type);
                ps.setString(5, feature_describe);
                ps.setBoolean(6, isIndex);
                ps.setString(7, dep_user);
                ps.setString(8, dep_feature);
                ps.execute();
                ps.close();
                logger.info("insert click method: question=" + username + ",method=" + task_id);
            }
        } catch (SQLException e) {
            logger.error("insert click error with: question=" + username + ",method=" + task_id);
            logger.error(e.toString());
        } catch (Exception e) {
            logger.error("other exception:", e);
        } finally {
            // 关闭
            DbUtil.close(conn, ps, null);
        }
    }


    public static Features selectFeatureListByTaskIdAndCli(String taskId, PartnerProperty property) {
        List<SingleFeature> featureList = new ArrayList<>();
        String uidName = "uid";
        PreparedStatement ps = null;
        Connection conn = DbUtil.getConnection();
        ResultSet resultSet = null;
        try {
            String mysql = "select feature,feature_type,feature_describe,is_index from feature_list as f join client_info as c " +
                    "where client_ip=? and client_port = ? and protocol = ? and f.task_id=c.task_id and f.username=c.username and f.status = 0 and  c.status = 0 and  f.task_id = ? ";
            if (conn != null) {
                ps = conn.prepareStatement(mysql);
                ps.setString(1, property.getClientIp());
                ps.setInt(2, property.getPort());
                ps.setString(3, property.getProtocol());
                ps.setInt(4, Integer.parseInt(taskId));
                resultSet = ps.executeQuery();
                while (resultSet.next()) {
                    String feature_name = resultSet.getString(1);
                    String feature_type = resultSet.getString(2);
                    SingleFeature feature = new SingleFeature(feature_name, feature_type);
                    featureList.add(feature);
                    boolean isIndex = resultSet.getBoolean(4);
                    if (isIndex) {
                        uidName = feature_name;
                    }
                }

                ps.close();
                logger.info("select click method: messageId=" + taskId);
            }
        } catch (SQLException e) {
            logger.error("select features ByTaskIdAndCli with taskId:" + taskId, e);
        } catch (Exception e) {
            logger.error("other exception:", e);
        } finally {
            // 关闭
            DbUtil.close(conn, ps, resultSet);
        }
        return new Features(featureList, uidName, null);
    }

    /**
     * 根据taskId, 查询特征列表
     *
     * @param taskId
     * @return
     */
    public static List<FeatureAnswer> selectFeatureListByTaskId(Integer taskId) {
        List<FeatureAnswer> featureAnswers = new ArrayList<>();
        PreparedStatement ps = null;
        ResultSet resultSet = null;
        Connection conn = DbUtil.getConnection();
        try {
            String mysql = "select task_id,username,feature,feature_type,feature_describe,is_index,dep_user, dep_feature from feature_list where status = 0 and task_id = ?";
            if (conn != null) {
                ps = conn.prepareStatement(mysql);
                ps.setInt(1, taskId);
                resultSet = ps.executeQuery();
                while (resultSet.next()) {
                    String owner_name = resultSet.getString(2);
                    String feature = resultSet.getString(3);
                    String feature_type = resultSet.getString(4);
                    String describe = resultSet.getString(5);
                    boolean isIndex = resultSet.getBoolean(6);
                    FeatureAnswer answer = new FeatureAnswer(taskId, owner_name, feature, feature_type, describe, isIndex);
                    answer.setDep_user(resultSet.getString(7));
                    answer.setDep_feature(resultSet.getString(8));
                    featureAnswers.add(answer);
                }
                ps.close();
                logger.info("select click method: taskId=" + taskId);
            }
        } catch (SQLException e) {
            logger.error("select click error with: taskId=" + taskId);
            logger.error(e.toString());
        } catch (Exception e) {
            logger.error("other exception:", e);
        } finally {
            // 关闭
            DbUtil.close(conn, ps, resultSet);
        }
        return featureAnswers;
    }

}
