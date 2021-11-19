package com.jdt.fedlearn.coordinator.service.match;

import com.jdt.fedlearn.common.entity.TokenDTO;
import com.jdt.fedlearn.coordinator.entity.prepare.MatchDetailRes;
import com.jdt.fedlearn.coordinator.entity.prepare.MatchQueryReq;
import com.jdt.fedlearn.coordinator.service.TrainService;
import com.jdt.fedlearn.tools.TokenUtil;
import com.jdt.fedlearn.tools.internel.ResponseHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

public class MatchDetailImpl implements TrainService {
    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    @Override
    public Map<String, Object> service(String content) {
        try {
            MatchQueryReq req = MatchQueryReq.parseJson(content);
            MatchDetailRes detailRes = detail(req);
            return ResponseHandler.successResponse(detailRes);
        } catch (Exception e) {
            logger.error("exception occur", e);
            return ResponseHandler.error(-5, e.getMessage());
        }
    }

    private MatchDetailRes detail(MatchQueryReq queryReq) {
        String matchId = queryReq.getMatchId();
        TokenDTO tokenDTO = TokenUtil.parseToken(matchId);
        String taskId = tokenDTO.getTaskId();
        String matchType = tokenDTO.getAlgorithm();
        return new MatchDetailRes(matchType, taskId);
    }
}
