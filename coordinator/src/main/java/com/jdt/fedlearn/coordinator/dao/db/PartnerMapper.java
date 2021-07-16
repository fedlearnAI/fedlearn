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

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jdt.fedlearn.coordinator.entity.table.PartnerProperty;
import com.jdt.fedlearn.coordinator.util.DbUtil;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * 参与方信息持久化和读写
 */
public class PartnerMapper {
    private static final Logger logger = LoggerFactory.getLogger(PartnerMapper.class);

    public static void insertPartner(PartnerProperty cp) {
        PreparedStatement ps = null;
        String sql = "INSERT INTO client_info (task_id,username,client_ip,client_port,protocol,token,dataset) VALUES (?,?,?,?,?,?,?)";
        try (Connection conn = DbUtil.getConnection()) {
            if (conn != null) {
                ps = conn.prepareStatement(sql);
                ps.setInt(1, cp.getTaskId());
                ps.setString(2, cp.getUsername());
                ps.setString(3, cp.getClientIp());
                ps.setInt(4, cp.getPort());
                ps.setString(5, cp.getProtocol());
                ps.setInt(6, cp.getToken());
                ps.setString(7, cp.getDataset());
                ps.execute();
                ps.close();
            }
        } catch (SQLException e) {
            logger.error("insert client sql error with: " + cp.toString(), e);
        } catch (Exception e) {
            logger.error("normal error:", e);
        } finally {
            DbUtil.close(ps, null);
        }
    }


    public static List<PartnerProperty> selectPartnerList(String taskId) {
        return selectPartnerList(taskId, "");
    }

    /**
     * @param taskId   任务的唯一id
     * @param username 用户名
     * @return
     */
    public static List<PartnerProperty> selectPartnerList(String taskId, String username) {
        List<PartnerProperty> partnerProperties = new ArrayList<>();
        PreparedStatement ps = null;
        Connection conn = DbUtil.getConnection();
        ResultSet resultSet = null;
        try {
            StringBuilder mysql = new StringBuilder("select client_ip,client_port,protocol,token,dataset from client_info where status = 0 and task_id = ?");
            if (StringUtils.isNotEmpty(username)) {
                mysql.append(" and username = ?");
            }
            if (conn != null) {
                ps = conn.prepareStatement(mysql.toString());
                ps.setInt(1, Integer.parseInt(taskId));
                if (StringUtils.isNotEmpty(username)) {
                    ps.setString(2, username);
                }
                resultSet = ps.executeQuery();
                while (resultSet.next()) {
                    String ip = resultSet.getString(1);
                    int port = resultSet.getInt(2);
                    String protocol = resultSet.getString(3);
                    int token = resultSet.getInt(4);
                    String dataset = resultSet.getString(5);
                    logger.info("dataset: " + dataset);
                    PartnerProperty property = new PartnerProperty(Integer.parseInt(taskId), username, protocol, ip, port, token, dataset);
                    ObjectMapper mapper = new ObjectMapper();
                    logger.info("property: " + mapper.writeValueAsString(property));
                    partnerProperties.add(property);
                }
                ps.close();
                logger.info("select click method: messageId=" + taskId);
            }
        } catch (SQLException e) {
            logger.error("select click error with: messageId=" + taskId, e);
        } catch (Exception e) {
            logger.error("other error", e);
        } finally {
            // 关闭
            DbUtil.close(conn, ps, resultSet);
        }
        return partnerProperties;
    }

    /**
     * 根据token和username查询对应的client
     *
     * @param modelToken
     * @param username
     * @return
     */
    public static PartnerProperty selectClientByToken(String modelToken, String username) {
        PartnerProperty partnerProperty = null;
        Connection conn = DbUtil.getConnection();
        PreparedStatement ps = null;
        ResultSet resultSet = null;
        try {
            String sql = "SELECT client_ip,  client_port, protocol,token,dataset FROM client_info cli INNER JOIN model_table model ON cli.task_id = model.task_id where cli.username = ? and model.model_token = ? and cli.`status` = '0' and model.`status`='0'";
            if (conn != null) {
                ps = conn.prepareStatement(sql);
                ps.setString(1, username);
                ps.setString(2, modelToken);
                resultSet = ps.executeQuery();
                while (resultSet.next()) {
                    String ip = resultSet.getString(1);
                    int port = resultSet.getInt(2);
                    String protocol = resultSet.getString(3);
                    int token = resultSet.getInt(4);
                    String dataset = resultSet.getString(5);
                    partnerProperty = new PartnerProperty(username, protocol, ip, port, token, dataset);
                }
            }
        } catch (SQLException e) {
            logger.error("sql执行异常异常", e);
        } finally {
            // 关闭
            DbUtil.close(conn, ps, resultSet);
        }
        return partnerProperty;
    }
}
