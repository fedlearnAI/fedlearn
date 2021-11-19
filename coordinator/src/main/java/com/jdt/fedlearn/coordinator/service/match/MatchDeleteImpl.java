package com.jdt.fedlearn.coordinator.service.match;

import com.jdt.fedlearn.coordinator.dao.db.MatchMapper;
import com.jdt.fedlearn.coordinator.entity.prepare.MatchQueryReq;
import com.jdt.fedlearn.coordinator.service.TrainService;
import com.jdt.fedlearn.tools.internel.ResponseHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

public class MatchDeleteImpl implements TrainService {
    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    @Override
    public Map<String, Object> service(String content) {
        try {
            MatchQueryReq req = MatchQueryReq.parseJson(content);
            if (deleteMatch(req)) {
                return ResponseHandler.success();
            } else {
                return ResponseHandler.error(-1, "delete fail");
            }
        } catch (Exception e) {
            logger.error("exception occur", e);
            return ResponseHandler.error(-5, e.getMessage());
        }
    }

    private boolean deleteMatch(MatchQueryReq queryReq) {
        return MatchMapper.deleteMatch(queryReq.getMatchId());
    }
}
