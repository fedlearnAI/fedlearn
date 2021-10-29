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

package com.jdt.fedlearn.coordinator.allocation;

import com.jdt.fedlearn.common.entity.project.MatchPartnerInfo;
import com.jdt.fedlearn.common.enums.RunningType;
import com.jdt.fedlearn.common.util.LogUtil;
import com.jdt.fedlearn.coordinator.constant.RequestConstant;
import com.jdt.fedlearn.coordinator.dao.db.MatchMapper;
import com.jdt.fedlearn.coordinator.entity.prepare.MatchStartReq;
import com.jdt.fedlearn.coordinator.entity.table.MatchEntity;
import com.jdt.fedlearn.coordinator.network.SendAndRecv;
import com.jdt.fedlearn.coordinator.service.prepare.MatchStartImpl;
import com.jdt.fedlearn.coordinator.util.ConfigUtil;
import com.jdt.fedlearn.core.entity.ClientInfo;
import com.jdt.fedlearn.core.entity.Message;
import com.jdt.fedlearn.core.entity.base.EmptyMessage;
import com.jdt.fedlearn.core.entity.common.CommonRequest;
import com.jdt.fedlearn.core.entity.common.CommonResponse;
import com.jdt.fedlearn.common.entity.uniqueId.MappingId;
import com.jdt.fedlearn.core.exception.DeserializeException;
import com.jdt.fedlearn.core.psi.CommonPrepare;
import com.jdt.fedlearn.core.psi.MatchResult;
import com.jdt.fedlearn.core.psi.Prepare;
import com.jdt.fedlearn.core.type.MappingType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * @author fanmingjie
 */
public class MultiMatch implements Runnable {
    private final Logger logger = LoggerFactory.getLogger(this.getClass());
    private final String matchId;
    private final MatchStartReq matchStartReq;
    private static final int VALUE_GAP = 10;


    public MultiMatch(String matchId, MatchStartReq matchStartReq) {
        this.matchId = matchId;
        this.matchStartReq = matchStartReq;
    }

    /**
     * 单个线程的ID对齐；根据项目id获取对应参与的用户以及各自的客户端信息
     *
     * @param matchId 对齐id
     * @return 对齐结果，包含一个报告和对齐数据
     */
    public MatchResult singleMatch(String matchId, List<MatchPartnerInfo> partnerInfos) throws ParseException, DeserializeException {
        MappingId mappingId = new MappingId(matchId);
        MappingType mappingType = mappingId.getMappingType();
        // TODO: id三方对齐
        Prepare prepare = CommonPrepare.construct(mappingType);
        // 为各个客户端创建initial requests
        List<ClientInfo> clientInfos = partnerInfos.stream().map(MatchPartnerInfo::toClientInfo).collect(Collectors.toList());
        List<CommonRequest> matchRequests = prepare.masterInit(clientInfos);
        List<CommonResponse> matchResponses = new ArrayList<>();
        while (prepare.isContinue()) {
            matchResponses = new ArrayList<>();
            for (int i = 0; i < matchRequests.size(); i++) {
                CommonRequest request = matchRequests.get(i);
                ClientInfo client = request.getClient();
                // 向客户端发送
                Map<String, Object> context = new HashMap<>();
                context.put("dataset", partnerInfos.get(i).getDataset());
                context.put("index", partnerInfos.get(i).getIndex());
                context.put("matchType", mappingType.getType());
                context.put("phase", request.getPhase());
                context.put("matchToken", matchId);
//                String response = SendAndRecv.send(client, matchId, partnerInfos.get(i).getDataset(), partnerInfos.get(i).getIndex(), request.getPhase(), mappingType.getType(), request.getBody());
                String response = SendAndRecv.send(client, RequestConstant.TRAIN_MATCH, context, request.getBody());
                // 更新对齐进度
//                updatePercent(matchId, MatchStartImpl.ID_MATCH_FLAG.get(matchId));
                logger.info("received from client: " + client.url());
                Message message = ResourceManager.serializer.deserialize(response);
                matchResponses.add(new CommonResponse(client, message));
                logger.info("matchResponses size : " + matchResponses.size());
            }
            matchRequests = prepare.master(matchResponses);
        }
        logger.info("out of while loop");
        return prepare.postMaster(matchResponses);
    }


    @Override
    public void run() {
        try {
            List<MatchPartnerInfo> partnerInfos = matchStartReq.getClientList();
            MatchResult matchResult = singleMatch(matchId, partnerInfos);
            matchResult.setMatchId(matchId);
            logger.info("SUM_DATA_MAP keys : " + MatchStartImpl.matchEntityMap.keySet());
            for (MatchPartnerInfo client : partnerInfos) {

                Map<String, Object> context = new HashMap<>();
                context.put("dataset", "");
                context.put("index", "");
                context.put("matchType", "");
                context.put("phase", 999);
                context.put("matchToken", matchId);
                String response = SendAndRecv.send(client.toClientInfo(), RequestConstant.TRAIN_MATCH, context, new EmptyMessage());
                logger.info("response : " + response);
            }
            // 更新缓存和数据库的对齐信息
            updateMatchEntity(matchResult);
            logger.info("after insert matchResult into db");
        } catch (ParseException | DeserializeException e) {
            MatchEntity matchEntity = MatchStartImpl.matchEntityMap.get(matchId);
            matchEntity.setRunningType(RunningType.FAIL);
            matchEntity.setMatchReport(LogUtil.logLine(e.getMessage()));
            if (!ConfigUtil.getJdChainAvailable()) {
                MatchMapper.updateMatchInfo(matchEntity);
            }
            MatchStartImpl.matchEntityMap.put(matchId, matchEntity);
            logger.error("ParseException " + e.getMessage());
        }
    }


    private void updateMatchEntity(MatchResult matchResult) {
        MatchEntity matchEntity = MatchStartImpl.matchEntityMap.get(matchResult.getMatchId());
        matchEntity.setRunningType(RunningType.COMPLETE);
        matchEntity.setMatchReport(matchResult.getReport());
        matchEntity.setLength(matchResult.getLength());
        if (!ConfigUtil.getJdChainAvailable()) {
            MatchMapper.updateMatchInfo(matchEntity);
        }
    }
}
