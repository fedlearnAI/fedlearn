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

import com.jdt.fedlearn.common.util.TimeUtil;
import com.jdt.fedlearn.coordinator.entity.inference.InferenceDto;
import com.jdt.fedlearn.coordinator.entity.table.InferenceEntity;
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


public class InferenceLogMapper {

    private static final Logger logger = LoggerFactory.getLogger(InferenceLogMapper.class);


    public static Boolean insertInference(InferenceEntity inferenceEntity) {
        PreparedStatement ps = null;
        Connection conn = DbUtil.getConnection();
        try {
            String sql = "INSERT INTO inference_log (inference_id,model_token,username,start_time,end_time,inference_result,request_num,response_num) VALUES (?,?,?,?,?,?,?,?)";
            if (conn != null) {
                ps = conn.prepareStatement(sql);
                ps.setString(1, inferenceEntity.getInferenceId());
                ps.setString(2, inferenceEntity.getModelToken());
                ps.setString(3, inferenceEntity.getUserName());
                ps.setString(4, TimeUtil.defaultFormat(inferenceEntity.getStartTime()));
                ps.setString(5, TimeUtil.defaultFormat(inferenceEntity.getEndTime()));
                ps.setString(6, inferenceEntity.getInferenceResult());
                ps.setInt(7, inferenceEntity.getRequestNum());
                ps.setInt(8, inferenceEntity.getResponseNum());
                ps.execute();
                ps.close();
                logger.info("insert inference_log success");
            }
        } catch (SQLException e) {
            logger.error("insert inference_log fail");
            logger.error(e.toString());
        } finally {
            // 关闭
            DbUtil.close(conn, ps, null);
        }
        return true;
    }

    public static List<InferenceEntity> getInferenceList(InferenceDto inferenceDto) {
        final int pageIndex = Integer.parseInt(inferenceDto.getPageIndex());
        final int pageSize = Integer.parseInt(inferenceDto.getPageSize());
        PreparedStatement ps = null;
        ResultSet resultSet = null;
        Connection conn = DbUtil.getConnection();
        List<InferenceEntity> inferenceEntityList = new ArrayList<>();
        try {
            StringBuilder sql = new StringBuilder("SELECT inference_id,model_token,username,start_time,end_time,inference_result,request_num,response_num FROM inference_log where ");
            sql.append("model_token = ? ");
            if (StringUtils.isNotBlank(inferenceDto.getCaller())) {
                sql.append("and username = ? ");
            }
            sql.append("and start_time between ? and ? and status = 0  order by id desc limit ?, ?");
            if (conn != null) {
                ps = conn.prepareStatement(sql.toString());
                ps.setString(1, inferenceDto.getModelToken());
                if (StringUtils.isNotBlank(inferenceDto.getCaller())) {
                    ps.setString(2, inferenceDto.getCaller());
                    ps.setString(3, inferenceDto.getStartTime());
                    ps.setString(4, inferenceDto.getEndTime());
                    ps.setInt(5, (pageIndex - 1) * pageSize);
                    ps.setInt(6, pageSize);
                } else {
                    ps.setString(2, inferenceDto.getStartTime());
                    ps.setString(3, inferenceDto.getEndTime());
                    ps.setInt(4, (pageIndex - 1) * pageSize);
                    ps.setInt(5, pageSize);
                }
                resultSet = ps.executeQuery();
                while (resultSet.next()) {
                    final InferenceEntity inferenceEntity = new InferenceEntity();
                    inferenceEntity.setInferenceId(resultSet.getString(1));
                    inferenceEntity.setModelToken(resultSet.getString(2));
                    inferenceEntity.setCaller(resultSet.getString(3));
                    inferenceEntity.setStartTime(TimeUtil.parseStrToData(resultSet.getString(4)));
                    inferenceEntity.setEndTime(TimeUtil.parseStrToData(resultSet.getString(5)));
                    inferenceEntity.setInferenceResult(resultSet.getString(6));
                    inferenceEntity.setRequestNum(resultSet.getInt(7));
                    inferenceEntity.setResponseNum(resultSet.getInt(8));
                    inferenceEntityList.add(inferenceEntity);
                }
                ps.close();
                logger.info("inference list size is:" + inferenceEntityList.size());
            }
        } catch (SQLException e) {
            logger.error("query inference_log fail");
            logger.error(e.toString());
        } finally {
            // 关闭
            DbUtil.close(conn, ps, resultSet);
        }
        return inferenceEntityList;
    }

    public static InferenceEntity getInferenceLog(String inferenceId) {
        final InferenceEntity inferenceEntity = new InferenceEntity();
        PreparedStatement ps = null;
        ResultSet resultSet = null;
        Connection conn = DbUtil.getConnection();
        try {
            StringBuilder sql = new StringBuilder("SELECT inference_id,model_token,username,start_time,end_time,inference_result,request_num,response_num FROM inference_log where inference_id = ? limit 1");
            if (conn != null) {
                ps = conn.prepareStatement(sql.toString());
                ps.setString(1, inferenceId);
                resultSet = ps.executeQuery();
                while (resultSet.next()) {
                    inferenceEntity.setInferenceId(resultSet.getString(1));
                    inferenceEntity.setModelToken(resultSet.getString(2));
                    inferenceEntity.setCaller(resultSet.getString(3));
                    inferenceEntity.setStartTime(TimeUtil.parseStrToData(resultSet.getString(4)));
                    inferenceEntity.setEndTime(TimeUtil.parseStrToData(resultSet.getString(5)));
                    inferenceEntity.setInferenceResult(resultSet.getString(6));
                    inferenceEntity.setRequestNum(resultSet.getInt(7));
                    inferenceEntity.setResponseNum(resultSet.getInt(8));
                }
            }
        } catch (SQLException e) {
            logger.error("query inference_log by inference_id=【{}】 fail，异常详情：", inferenceId, e);
        } finally {
            DbUtil.close(conn, ps, resultSet);
        }
        return inferenceEntity;
    }

    public static Integer getInferenceCount(InferenceDto inferenceDto) {
        PreparedStatement ps = null;
        Integer num = 0;
        ResultSet resultSet = null;
        Connection conn = DbUtil.getConnection();
        try {
            StringBuilder sql = new StringBuilder("SELECT count(1) num FROM inference_log where ");
            sql.append("model_token = ? ");
            if (StringUtils.isNotBlank(inferenceDto.getCaller())) {
                sql.append("and username = ? ");
            }
            sql.append("and start_time between ? and ? and status = 0");
            if (conn != null) {
                ps = conn.prepareStatement(sql.toString());
                ps.setString(1, inferenceDto.getModelToken());
                if (StringUtils.isNotBlank(inferenceDto.getCaller())) {
                    ps.setString(2, inferenceDto.getCaller());
                    ps.setString(3, inferenceDto.getStartTime());
                    ps.setString(4, inferenceDto.getEndTime());
                } else {
                    ps.setString(2, inferenceDto.getStartTime());
                    ps.setString(3, inferenceDto.getEndTime());
                }
                resultSet = ps.executeQuery();
                if (resultSet.next()) {
                    num = resultSet.getInt(1);
                }
                ps.close();
                logger.info("query inference count is:" + num);
            }
        } catch (SQLException e) {
            logger.error("qquery inference count fail");
            logger.error(e.toString());
        } finally {
            // 关闭
            DbUtil.close(conn, ps, resultSet);
        }
        return num;
    }
}
