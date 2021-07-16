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

import com.jdt.fedlearn.coordinator.entity.task.CreateQuery;
import com.jdt.fedlearn.coordinator.entity.table.TaskAnswer;
import com.jdt.fedlearn.coordinator.type.DbType;
import com.jdt.fedlearn.coordinator.util.DbUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;


public class TaskMapper {
    private static final Logger logger = LoggerFactory.getLogger(TaskMapper.class);

    public static int insertTask(CreateQuery createQuery) {
        int taskId = -1;
        PreparedStatement psOne = null;
        PreparedStatement psTow = null;
        ResultSet resultSet = null;
        Connection conn = DbUtil.getConnection();
        try {
            String sql = "INSERT INTO task_table (task_name,task_owner,has_pwd,task_pwd,mer_code,visible,visible_mercode,inference_flag,partners) " +
                    "VALUES (?,?,?,?,?,?,?,?,'')";
            if (conn != null) {
                psOne = conn.prepareStatement(sql);
                psOne.setString(1, createQuery.getTaskName());
                psOne.setString(2, createQuery.getUsername());
                psOne.setString(3, createQuery.getHasPwd());
                psOne.setString(4, createQuery.getTaskPwd());
                psOne.setString(5, createQuery.getMerCode());
                psOne.setString(6, createQuery.getVisible());
                psOne.setString(7, createQuery.getVisibleMerCode());
                psOne.setString(8, createQuery.getInferenceFlag());
                psOne.execute();

                String sql2 = "SELECT LAST_INSERT_ID()";
                if (DbUtil.getDbType().equals(DbType.sqlite)) {
                    sql2 = "select last_insert_rowid() ";
                }
                psTow = conn.prepareStatement(sql2);
                resultSet = psTow.executeQuery();
                if (resultSet.next()) {
                    taskId = resultSet.getInt(1);
                }
                logger.info("insert task: username=" + createQuery.getUsername() + ",taskName=" + createQuery.getTaskName());
            }
        } catch (SQLException e) {
            logger.error("insert task error with: username=" + createQuery.getUsername() + ",taskName=" + createQuery.getTaskName());
            logger.error(e.toString());
        } catch (Exception e) {
            logger.error("other exception:", e);
        } finally {
            // 关闭
            DbUtil.close(conn, psOne, resultSet);
            DbUtil.close(null, psTow, null);
        }
        return taskId;
    }

    //查询用户自己创建的任务
    public static List<TaskAnswer> selectCreatedTask(String username) {
        List<TaskAnswer> createdTaskList = new ArrayList<>();
        PreparedStatement ps = null;
        ResultSet resultSet = null;
        Connection conn = DbUtil.getConnection();
        try {
            String mysql = "select id, task_name, task_owner, partners ,has_pwd, mer_code, visible, visible_mercode, inference_flag from task_table where status = 0 and task_owner = ? order by modified_time desc";
            if (conn != null) {
                ps = conn.prepareStatement(mysql);
                ps.setString(1, username);
                resultSet = ps.executeQuery();
                while (resultSet.next()) {
                    int id = resultSet.getInt(1);
                    String taskName = resultSet.getString(2);
                    String owner = resultSet.getString(3);
                    String partners = resultSet.getString(4);
                    String hasPwd = resultSet.getString(5);
                    String merCode = resultSet.getString(6);
                    String visible = resultSet.getString(7);
                    String visibleMerCode = resultSet.getString(8);
                    String inferenceFlag = resultSet.getString(9);
                    TaskAnswer answer = new TaskAnswer(id, taskName, owner, partners, hasPwd, merCode, visible, visibleMerCode, inferenceFlag);
                    createdTaskList.add(answer);
                }
                ps.close();
//                logger.info("select created task: " + createdTaskList.toString());
            }
        } catch (SQLException e) {
            logger.error("select created task error with: username=" + username);
            logger.error(e.toString());
        } catch (Exception e) {
            logger.error("other exception:", e);
        } finally {
            // 关闭
            DbUtil.close(conn, ps, resultSet);
        }
        return createdTaskList;
    }

    public static String selectTaskPartner(int taskId) {
        String res = null;
        PreparedStatement ps = null;
        ResultSet resultSet = null;
        Connection conn = DbUtil.getConnection();
        try {
            String sql = "select id,partners from task_table where status = 0 and id = ?";
            if (conn != null) {
                ps = conn.prepareStatement(sql);
                ps.setInt(1, taskId);
                resultSet = ps.executeQuery();
                if (resultSet.next()) {
//                    int id = resultSet.getInt(1);
                    res = resultSet.getString(2);
                }
                ps.close();
                logger.info("select Task Partner with: taskId=" + taskId);
            }
        } catch (SQLException e) {
            logger.error("select Task Partner with: taskId=" + taskId);
            logger.error(e.toString());
        } catch (Exception e) {
            logger.error("other exception:", e);
        } finally {
            // 关闭
            DbUtil.close(conn, ps, resultSet);
        }
        return res;
    }

    public static TaskAnswer selectTaskById(Integer taskId) {
        TaskAnswer answer = new TaskAnswer();
        PreparedStatement ps = null;
        ResultSet resultSet = null;
        Connection conn = DbUtil.getConnection();
        try {
            String sql = "select id,task_name,partners,task_owner,task_pwd from task_table where status = 0 and id = ?";
            if (conn != null) {
                ps = conn.prepareStatement(sql);
                ps.setInt(1, taskId);
                resultSet = ps.executeQuery();
                if (resultSet.next()) {
                    answer.setTaskId(resultSet.getInt(1));
                    answer.setTaskName(resultSet.getString(2));
                    answer.setParticipants(resultSet.getString(3));
                    answer.setOwner(resultSet.getString(4));
                    answer.setTaskPwd(resultSet.getString(5));
                }
                ps.close();
                logger.info("select task with taskId=" + taskId);
            }
        } catch (SQLException e) {
            logger.error("select task error with: taskId=" + taskId);
            logger.error(e.toString());
        } catch (Exception e) {
            logger.error("other exception:", e);
        } finally {
            // 关闭
            DbUtil.close(conn, ps, resultSet);
        }
        return answer;
    }

    public static void updateTaskPartner(int taskId, String partners) {
        PreparedStatement ps = null;
        Connection conn = DbUtil.getConnection();
        try {
            String sql = "UPDATE task_table set partners = ? where id = ? ";
            if (conn != null) {
                ps = conn.prepareStatement(sql);
                ps.setString(1, partners);
                ps.setInt(2, taskId);
                ps.execute();
                ps.close();
                logger.info("insert click method: question=" + taskId + ",method=" + partners);
            }
        } catch (SQLException e) {
            logger.error("insert click error with: question=" + taskId + ",method=" + partners);
            logger.error(e.toString());
        } catch (Exception e) {
            logger.error("other exception:", e);
        } finally {
            // 关闭
            DbUtil.close(conn, ps, null);
        }
    }

    //查询非用户创建的任务，用于后续区分用户参与和未参与
    public static List<TaskAnswer> selectNotOwnTask(String username) {
        List<TaskAnswer> taskAnswers = new ArrayList<>();
        PreparedStatement ps = null;
        ResultSet resultSet = null;
        Connection conn = DbUtil.getConnection();
        try {
            String sql = "select id, task_name, task_owner, partners ,has_pwd, mer_code, visible, visible_mercode, inference_flag from task_table where status = 0 and task_owner != ? order by modified_time desc";
            if (conn != null) {
                ps = conn.prepareStatement(sql);
                ps.setString(1, username);
                resultSet = ps.executeQuery();
                while (resultSet.next()) {
                    int id = resultSet.getInt(1);
                    String taskName = resultSet.getString(2);
                    String owner = resultSet.getString(3);
                    String partner = resultSet.getString(4);
                    String hasPwd = resultSet.getString(5);
                    String merCode = resultSet.getString(6);
                    String visible = resultSet.getString(7);
                    String visibleMerCode = resultSet.getString(8);
                    String inferenceFlag = resultSet.getString(9);
                    TaskAnswer taskAnswer = new TaskAnswer(id, taskName, owner, partner,hasPwd,merCode,visible,visibleMerCode, inferenceFlag);
                    taskAnswers.add(taskAnswer);
                }
                ps.close();
                logger.info("select Not Own task : username=" + username);
            }
        } catch (SQLException e) {
            logger.error("select Not Own task error with username=" + username);
            logger.error(e.toString());
        } catch (Exception e) {
            logger.error("other exception:", e);
        } finally {
            // 关闭
            DbUtil.close(conn, ps, resultSet);
        }
        return taskAnswers;
    }
}
