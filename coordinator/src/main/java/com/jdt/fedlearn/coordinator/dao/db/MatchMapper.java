package com.jdt.fedlearn.coordinator.dao.db;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jdt.fedlearn.common.entity.TokenDTO;
import com.jdt.fedlearn.common.entity.project.MatchPartnerInfo;
import com.jdt.fedlearn.common.enums.RunningType;
import com.jdt.fedlearn.coordinator.entity.table.MatchEntity;
import com.jdt.fedlearn.coordinator.util.DbUtil;
import com.jdt.fedlearn.tools.TokenUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import scala.Tuple2;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * ID对齐相关信息在coordinator端储存于数据库中，储存信息包括：task_id，match_token，match_type，match_report，length，running_type，datasets
 *
 * @author fanmingjie
 */
public class MatchMapper {
    private static final Logger logger = LoggerFactory.getLogger(MatchMapper.class);


    public static void insertMatchInfo(String matchId, String username, RunningType runningType, String datasets) {
        PreparedStatement ps = null;
        String sql = "INSERT INTO match_info (task_id,username,match_token,match_type,running_type,datasets) VALUES (?,?,?,?,?,?)";
        try (Connection conn = DbUtil.getConnection()) {
            if (conn != null) {
                ps = conn.prepareStatement(sql);
                TokenDTO tokenDTO = TokenUtil.parseToken(matchId);
                String taskId = tokenDTO.getTaskId();
                ps.setString(1, taskId);
                ps.setString(2, username);
                ps.setString(3, matchId);
                ps.setString(4, matchId.split("-")[1]);
                ps.setString(5, runningType.toString());
                ps.setString(6, datasets);
                ps.execute();
                ps.close();
            }
        } catch (SQLException e) {
            logger.error("insert client sql error with: " + matchId, e);
        } catch (Exception e) {
            logger.error("normal error:", e);
        } finally {
            DbUtil.close(ps, null);
        }
    }

    public static void updateMatchInfo(MatchEntity matchEntity) {
        PreparedStatement ps = null;
        Connection conn = DbUtil.getConnection();
        try {
            String sql = "update match_info set match_report=?,length=?,running_type=? where match_token=?";
            if (conn != null) {
                ps = conn.prepareStatement(sql);
                ps.setString(1, matchEntity.getMatchReport());
                ps.setInt(2, matchEntity.getLength());
                ps.setString(3, matchEntity.getRunningType().toString());
                ps.setString(4, matchEntity.getMatchId());
                ps.execute();
                ps.close();
            }
        } catch (SQLException e) {
            logger.error("update match info: matchId=" + matchEntity.getMatchId());
            logger.error(e.toString());
        } catch (Exception e) {
            logger.error("update sql error:{} ", e.getMessage());
        } finally {
            DbUtil.close(conn, ps, null);
        }
    }


    public static MatchEntity getMatchEntityByToken(String token) {
        MatchEntity matchEntity = new MatchEntity();
        PreparedStatement ps = null;
        Connection conn = DbUtil.getConnection();
        ObjectMapper objectMapper = new ObjectMapper();
        ResultSet resultSet = null;
        try {
            String sql = "SELECT task_id,match_token,length,match_report,running_type,datasets FROM match_info where status = 0 and match_token=?";
            if (conn != null) {
                ps = conn.prepareStatement(sql);
                ps.setString(1, token);
                resultSet = ps.executeQuery();
                if (resultSet.next()) {
                    String taskId = resultSet.getString(1);
                    String matchToken = resultSet.getString(2);
                    int length = resultSet.getInt(3);
                    String matchReport = resultSet.getString(4);
                    String runningType = resultSet.getString(5);
                    String datasets = resultSet.getString(6);
                    final List<MatchPartnerInfo> partnerInfos = objectMapper.readValue(datasets, new TypeReference<List<MatchPartnerInfo>>() {
                    });
                    matchEntity = new MatchEntity(taskId, matchToken, length, matchReport, RunningType.valueOf(runningType), partnerInfos);
                }
                ps.close();
            }
        } catch (SQLException e) {
            logger.error("other exception:", e);
        } catch (JsonMappingException e) {
            logger.error("datasets to MatchPartnerInfo error ", e);
        } catch (JsonProcessingException e) {
            logger.error("datasets JsonProcessingException error ", e);
        } finally {
            DbUtil.close(conn, ps, resultSet);
        }
        return matchEntity;
    }


    /**
     * @param taskId 任务id
     * @return 模型id列表
     */
    public static List<Tuple2<String, RunningType>> getMatchIdsByTaskId(String taskId) {
        PreparedStatement ps = null;
        ResultSet resultSet = null;
        List<Tuple2<String, RunningType>> matchList = new ArrayList<>();
        Connection conn = DbUtil.getConnection();
        try {
            String sql = "SELECT match_token,running_type FROM match_info where status = 0 and task_id=?";
            if (conn != null) {
                ps = conn.prepareStatement(sql);
                ps.setString(1, taskId);
                resultSet = ps.executeQuery();
                while (resultSet.next()) {
                    String token = resultSet.getString(1);
                    String runningTypeString = resultSet.getString(2);
                    RunningType runningType = RunningType.valueOf(runningTypeString);
                    matchList.add(new Tuple2<>(token, runningType));
                }
                ps.close();
                logger.info("match list size is:" + matchList.size());
            }
        } catch (SQLException e) {
            logger.error("match list sql error :{}", e.toString());
        } catch (Exception e) {
            logger.error("match list error:{}", e.getMessage());
        } finally {
            // 关闭
            DbUtil.close(conn, ps, resultSet);
        }
        return matchList;
    }


    public static List<Tuple2<String, RunningType>> getMatchIdsByTaskIdRunningType(String taskId, String type) {
        PreparedStatement ps = null;
        ResultSet resultSet = null;
        List<Tuple2<String, RunningType>> matchList = new ArrayList<>();
        Connection conn = DbUtil.getConnection();
        try {
            String sql = "SELECT match_token,running_type FROM match_info where status = 0 and task_id=? and running_type=?";
            if (conn != null) {
                ps = conn.prepareStatement(sql);
                ps.setString(1, taskId);
                ps.setString(2, type);
                resultSet = ps.executeQuery();
                while (resultSet.next()) {
                    String token = resultSet.getString(1);
                    String runningTypeString = resultSet.getString(2);
                    RunningType runningType = RunningType.valueOf(runningTypeString);
                    matchList.add(new Tuple2<>(token, runningType));
                }
                ps.close();
                logger.info("match list size is:" + matchList.size());
            }
        } catch (SQLException e) {
            logger.error("match list sql error :{}", e.toString());
        } catch (Exception e) {
            logger.error("match list error:{}", e.getMessage());
        } finally {
            // 关闭
            DbUtil.close(conn, ps, resultSet);
        }
        return matchList;
    }

    /**
     * @param token
     * @return
     */
    public static boolean isContainMatchModel(String token) {
        PreparedStatement ps = null;
        Connection conn = DbUtil.getConnection();
        ResultSet resultSet = null;
        try {
            String sql = "SELECT task_id,match_token,match_type,match_report,length FROM match_info where status = 0 and match_token=?";
            if (conn != null) {
                ps = conn.prepareStatement(sql);
                ps.setString(1, token);
                resultSet = ps.executeQuery();
                if (resultSet.next()) {
                    return true;
                }
                ps.close();
            }
        } catch (SQLException e) {
            logger.error("other exception:{}", e.getMessage());
            return false;
        } catch (Exception e) {
            logger.error("isContainMatchModel error:{}", e.getMessage());
            return false;
        } finally {
            DbUtil.close(conn, ps, resultSet);
        }
        return false;
    }


    public static String isContainMatch(String tasKId, String matchType) {
        String matchToken = null;
        PreparedStatement ps = null;
        Connection conn = DbUtil.getConnection();
        ResultSet resultSet = null;
        try {
            String sql = "SELECT match_token FROM match_info where status = 0 and task_id=? and match_type=?";
            if (conn != null) {
                ps = conn.prepareStatement(sql);
                ps.setString(1, tasKId);
                ps.setString(2, matchType);
                resultSet = ps.executeQuery();
                if (resultSet.next()) {
                    matchToken = resultSet.getString(1);

                }
                ps.close();
            }
        } catch (SQLException e) {
            logger.error("other exception:", e);
        } catch (Exception e) {
            logger.error("isContainMatch error:{}", e.getMessage());
        } finally {
            DbUtil.close(conn, ps, resultSet);
        }
        return matchToken;
    }

    public static boolean deleteMatch(String matchId) {
        PreparedStatement ps = null;
        Connection conn = DbUtil.getConnection();
        try {
            String sql = "update match_info set status=? where status = 0 and match_token=?";
            if (conn != null) {
                ps = conn.prepareStatement(sql);
                ps.setInt(1, 1);
                ps.setString(2, matchId);
                ps.execute();
                ps.close();
            }
        } catch (SQLException e) {
            logger.error("update match info: matchId=" + matchId);
            logger.error(e.toString());
        } catch (Exception e) {
            logger.error("update sql error:{} ", e.getMessage());
        } finally {
            DbUtil.close(conn, ps, null);
        }
        return true;
    }

}
