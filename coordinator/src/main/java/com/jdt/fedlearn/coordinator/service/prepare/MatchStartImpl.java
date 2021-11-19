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

import com.jdt.fedlearn.common.enums.RunningType;
import com.jdt.fedlearn.tools.LogUtil;
import com.jdt.fedlearn.tools.TokenUtil;
import com.jdt.fedlearn.tools.internel.ResponseHandler;
import com.jdt.fedlearn.tools.serializer.JsonUtil;
import com.jdt.fedlearn.coordinator.allocation.ResourceManager;
import com.jdt.fedlearn.coordinator.constant.Constant;
import com.jdt.fedlearn.coordinator.dao.db.MatchMapper;
import com.jdt.fedlearn.coordinator.entity.prepare.MatchStartReq;
import com.jdt.fedlearn.coordinator.entity.table.MatchEntity;
import com.jdt.fedlearn.coordinator.service.CommonService;
import com.jdt.fedlearn.coordinator.service.TrainService;
import com.jdt.fedlearn.coordinator.entity.uniqueId.MappingId;
import com.jdt.fedlearn.coordinator.util.ConfigUtil;
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
    private static final Logger logger = LoggerFactory.getLogger(MatchStartImpl.class);
    public static Map<String, MatchEntity> matchEntityMap = new ConcurrentHashMap<>();

    @Override
    public Map<String, Object> service(String content) {
        try {
            final MatchStartReq query = new MatchStartReq(content);
            final Map<String, Object> resultMap = match(query);
            return ResponseHandler.successResponse(resultMap);
        } catch (Exception e){
            logger.error(String.format("MatchStartImpl Exception :%s ", LogUtil.logLine(e.getMessage())));
            return CommonService.exceptionProcess(e, new HashMap<>());
        }
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
        MatchEntity matchEntity = getMatchEntity(taskId, matchAlgorithm);
        String matchId = null;
        if(matchEntity !=null){
            matchId = matchEntity.getMatchId();
//            MatchStartImpl.ID_MATCH_FLAG.put(matchId, 100);
            matchEntityMap.put(matchId,matchEntity);
            dataMap.put(Constant.MATCH_ID, matchId);
            return dataMap;
        }
        // 获取对齐算法
        MappingType mappingType = MappingType.valueOf(matchAlgorithm);
        // 获取mappingId
        MappingId mappingId = new MappingId(taskId, mappingType);
        matchId = mappingId.getMappingId();
        logger.info("matchId : " + matchId + " matchAlgorithm : " + matchAlgorithm);
        matchEntity = new MatchEntity();
        matchEntity.setMatchId(matchId);
        matchEntity.setTaskId(TokenUtil.parseToken(matchId).getTaskId());
        matchEntity.setRunningType(RunningType.RUNNING);
        matchEntity.setDatasets(query.getClientList());
        boolean flag = ConfigUtil.getJdChainAvailable();
        if(!flag) {
            String datasetsString = JsonUtil.object2json(query.getClientList());
            MatchMapper.insertMatchInfo(matchId, "defaultUser", RunningType.RUNNING,datasetsString);
        }
        // 记录对齐进度为10%
//        MatchStartImpl.ID_MATCH_FLAG.put(matchId, 10);
        MatchStartImpl.matchEntityMap.put(matchId, matchEntity);
        ResourceManager.submitMatch(matchId, query);
        dataMap.put(Constant.MATCH_ID, matchId);
        return dataMap;
    }



    public static MatchEntity getMatchEntity(String taskId, String mappingType) {
        Set<Map.Entry<String, MatchEntity>> entries = matchEntityMap.entrySet();
        logger.info("MatchStartImpl.SUM_DATA_MAP:" + matchEntityMap.keySet());
        Iterator<Map.Entry<String, MatchEntity>> iterator = entries.iterator();
        MatchEntity matchEntity = null;
        while (iterator.hasNext()) {
            Map.Entry<String, MatchEntity> e = iterator.next();
            if (TokenUtil.parseToken(e.getKey()).getTaskId().equals(taskId) && mappingType.equals(TokenUtil.parseToken(e.getKey()).getAlgorithm())) {
                logger.info("get match id from cache " + e.getKey());
                matchEntity = e.getValue();
                break;
            }
        }
        if (matchEntity == null) {
            logger.info("mapping type is " + mappingType);
            String matchIdStr = MatchMapper.isContainMatch(taskId, mappingType);
            logger.info("get match id from db " + matchIdStr);
            if (StringUtils.isNotBlank(matchIdStr)) {
                matchEntity = MatchMapper.getMatchEntityByToken(matchIdStr);
            }
        }
        return matchEntity;
    }


}
