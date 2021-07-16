package com.jdt.fedlearn.coordinator.dao.db;

import com.jdt.fedlearn.common.entity.TokenDTO;
import com.jdt.fedlearn.common.util.TokenUtil;
import com.jdt.fedlearn.coordinator.util.DbUtil;
import com.jdt.fedlearn.core.psi.MappingReport;
import com.jdt.fedlearn.core.psi.MatchResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * ID对齐相关信息在master端储存于数据库中，储存信息包括：Task ID，Match ID，对齐类型，对齐后长度，对齐report
 */
public class MatchMapper {
    private static final Logger logger = LoggerFactory.getLogger(MatchMapper.class);

    // TODO 储存对齐token,taskId,MappingType, ClientInfo； 确认什么样的数据需要被储存下来
    public static void insertMatchInfo(MatchResult matchResult,String username,String dataset,String matchReport) {
        PreparedStatement ps = null;
        String sql = "INSERT INTO match_info (task_id,username,match_token,match_type,match_report,length) VALUES (?,?,?,?,?,?)";
        try (Connection conn = DbUtil.getConnection()) {
            if (conn != null) {
                ps = conn.prepareStatement(sql);
                TokenDTO tokenDTO = TokenUtil.parseToken(matchResult.getMatchId());
                String taskId = tokenDTO.getTaskId();
                ps.setString(1, taskId);
                ps.setString(2,username);
                ps.setString(3, matchResult.getMatchId());
                ps.setString(4,matchResult.getMatchId().split("-")[1]);
                ps.setString(5,matchReport);
                ps.setInt(6,matchResult.getLength());
                ps.execute();
                ps.close();
            }
        } catch (SQLException e) {
            logger.error("insert client sql error with: " + matchResult.toString(), e);
        } catch (Exception e) {
            logger.error("normal error:", e);
        } finally {
            DbUtil.close(ps, null);
        }
    }


    public static MatchResult getMatchInfoByToken(String token) {
        MatchResult matchResult = new MatchResult();
        PreparedStatement ps = null;
        Connection conn = DbUtil.getConnection();
        ResultSet resultSet = null;
        try {
            String sql = "SELECT task_id,match_token,length,match_report FROM match_info where match_token=?";
            if (conn != null) {
                ps = conn.prepareStatement(sql);
                ps.setString(1, token);
                resultSet = ps.executeQuery();
                if (resultSet.next()) {
                    String matchToken = resultSet.getString(2);
                    int length = resultSet.getInt(3);
                    String matchReport = resultSet.getString(4);
                    matchResult = new MatchResult(matchToken, length, new MappingReport(matchReport, length));
                }
                ps.close();
            }
        } catch (SQLException e) {
            logger.error("other exception:", e);
        }  finally {
            DbUtil.close(conn, ps, resultSet);
        }
        return matchResult;
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
            String sql = "SELECT task_id,match_token,match_type,match_report,length FROM match_info where match_token=?";
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
            logger.error("other exception:", e);
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
            String sql = "SELECT match_token FROM match_info where task_id=? and match_type=?";
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
        } finally {
            DbUtil.close(conn, ps, resultSet);
        }
        return matchToken;
    }

}
