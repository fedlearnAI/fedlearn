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

package com.jdt.fedlearn.coordinator.service.prepare;

import com.jdt.fedlearn.common.util.TokenUtil;
import com.jdt.fedlearn.coordinator.allocation.ResourceManager;
import com.jdt.fedlearn.coordinator.dao.db.MatchMapper;
import com.jdt.fedlearn.coordinator.entity.prepare.MatchStartReq;
import com.jdt.fedlearn.coordinator.service.AbstractDispatchService;
import com.jdt.fedlearn.coordinator.service.TrainService;
import com.jdt.fedlearn.coordinator.util.ConfigUtil;
import com.jdt.fedlearn.common.entity.uniqueId.MappingId;
import com.jdt.fedlearn.core.psi.*;
import com.jdt.fedlearn.core.type.MappingType;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * {@code TrainService}接口的ID对齐实现类，实现模型训练前多线程的ID对齐预处理过程。
 *
 * @author lijingxi
 * @author fanmingjie
 * @since 0.8.0
 */
public class MatchStartImpl implements TrainService {
    private final Logger logger = LoggerFactory.getLogger(this.getClass());
    public static Map<String, MatchResult> SUM_DATA_MAP = new ConcurrentHashMap<>();
    //ID_MATCH_FLAG 各matchToken ID对齐的进度条
    public static Map<String, Integer> ID_MATCH_FLAG = new ConcurrentHashMap<>();

    @Override
    public Map<String, Object> service(String content) {
        final MatchStartReq query = new MatchStartReq(content);
        final Map resultMap = match(query);
        return new AbstractDispatchService() {
            @Override
            public Map dealService() {
                return resultMap;
            }
        }.doProcess(true);
    }


    /**
     * 多线程ID对齐
     *
     * @param query ID对齐请求实体
     * @return 对齐结果
     */
    public Map<String, Object> match(MatchStartReq query) {
        Map<String, Object> dataMap = new HashMap<>();
        // 获取taskId
        String taskId = query.getTaskId();
        // 获取对齐算法
        String matchAlgorithm = query.getMatchAlgorithm();
        // 如果本算法和taskId已经对齐过一次
        Set<String> matchIds = SUM_DATA_MAP.keySet();
        for (String matchId : matchIds) {
            if (matchId.contains(taskId) && matchAlgorithm.equals(TokenUtil.parseToken(matchId).getAlgorithm())) {
                return processAlreadyMatched(matchId, dataMap);
            }
        }
        String matchId = null;
        if (!ConfigUtil.getJdChainAvailable()) {
            String matchIdDB = MatchMapper.isContainMatch(taskId, matchAlgorithm);
            logger.info("get match id from db " + matchIdDB);
            if (StringUtils.isNotBlank(matchIdDB)) {
                matchId = matchIdDB;
                dataMap.put("matchToken", matchId);
                return dataMap;
            }
        }
        // 获取对齐算法
        MappingType mappingType = MappingType.valueOf(matchAlgorithm);
        // 获取mappingId
        MappingId mappingId = new MappingId(taskId, mappingType);
        matchId = mappingId.getMappingId();
        logger.info("matchId : " + matchId + " matchAlgorithm : " + matchAlgorithm);
        // 记录对齐进度为10%
        MatchStartImpl.ID_MATCH_FLAG.put(matchId, 10);
        ResourceManager.submitMatch(matchId, query.getUsername());
        dataMap.put("matchToken", matchId);
        return dataMap;
    }

    private Map<String, Object> processAlreadyMatched(String matchId, Map<String, Object> dataMap) {
        MatchStartImpl.ID_MATCH_FLAG.put(matchId, 100);
        dataMap.put("matchToken", matchId);
        return dataMap;
    }

}
