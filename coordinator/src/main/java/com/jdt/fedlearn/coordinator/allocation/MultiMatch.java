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

import com.jdt.fedlearn.coordinator.dao.UniversalMapper;
import com.jdt.fedlearn.coordinator.dao.db.MatchMapper;
import com.jdt.fedlearn.coordinator.entity.table.PartnerProperty;
import com.jdt.fedlearn.coordinator.network.SendAndRecv;
import com.jdt.fedlearn.coordinator.service.prepare.MatchStartImpl;
import com.jdt.fedlearn.coordinator.util.ConfigUtil;
import com.jdt.fedlearn.core.entity.ClientInfo;
import com.jdt.fedlearn.core.entity.Message;
import com.jdt.fedlearn.core.entity.base.EmptyMessage;
import com.jdt.fedlearn.core.entity.common.CommonRequest;
import com.jdt.fedlearn.core.entity.common.CommonResponse;
import com.jdt.fedlearn.common.entity.uniqueId.MappingId;
import com.jdt.fedlearn.core.psi.CommonPrepare;
import com.jdt.fedlearn.core.psi.MappingReport;
import com.jdt.fedlearn.core.psi.MatchResult;
import com.jdt.fedlearn.core.psi.Prepare;
import com.jdt.fedlearn.core.type.MappingType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 *  @author fanmingjie
 */
public class MultiMatch implements Runnable {
    private final Logger logger = LoggerFactory.getLogger(this.getClass());
    private final String matchId;
    private final String userName;
    private static final int VALUE_GAP = 10;


    public MultiMatch(String matchId,String userName) {
        this.matchId = matchId;
        this.userName = userName;
    }

    /**
     * 单个线程的ID对齐；根据项目id获取对应参与的用户以及各自的客户端信息
     * @param matchId   对齐id
     * @return 对齐结果，包含一个报告和对齐数据
     */
    public MatchResult singleMatch(String matchId) throws ParseException {
        MappingId mappingId = new MappingId(matchId);
        String projectId= mappingId.getProjectId();
        MappingType mappingType = mappingId.getMappingType();
        // 获取各个客户端信息
        List<PartnerProperty> propertyList = UniversalMapper.read(projectId);
        List<ClientInfo> clientInfos = propertyList.stream().map(PartnerProperty::toClientInfo).collect(Collectors.toList());
        // 获取各个客户端数据集名称信息
        Map<ClientInfo, String> datasetMap = propertyList.stream().collect(Collectors.toMap(PartnerProperty::toClientInfo, PartnerProperty::getDataset));
        // TODO: id三方对齐
        Prepare prepare = CommonPrepare.construct(mappingType);
        // 为各个客户端创建initial requests
        List<CommonRequest> matchRequests = prepare.masterInit(clientInfos);
        List<CommonResponse> matchResponses = new ArrayList<>();
        while (prepare.isContinue()) {
            matchResponses = new ArrayList<>();
            for (CommonRequest request : matchRequests) {
                ClientInfo client = request.getClient();
                // 向客户端发送
                String response = SendAndRecv.send(client, matchId, datasetMap.get(client), request.getPhase(), mappingType.getType(), request.getBody());
                logger.info("before update progress percent");
                // 更新对齐进度
                updatePercent(matchId, MatchStartImpl.ID_MATCH_FLAG.get(matchId));
                logger.info("received from client: " + client.url());
                assert response != null;
//                logger.info("response : " + response);
                Message message = ResourceManager.serializer.deserialize(response);
                matchResponses.add(new CommonResponse(client, message));
                logger.info("matchResponses size : " + matchResponses.size());
            }
            logger.info("before prepare master");
            matchRequests = prepare.master(matchResponses);
            logger.info("after prepare master");
        }
        logger.info("out of while loop");
        MappingReport mappingReport = prepare.postMaster(matchResponses);
        return new MatchResult(matchId, mappingReport.getSize(), mappingReport);
    }


    @Override
    public void run() {
        try {
            MatchResult matchResult = singleMatch(matchId);
            matchResult.setMatchId(matchId);
            MatchStartImpl.SUM_DATA_MAP.put(matchId, matchResult);
            logger.info("SUM_DATA_MAP keys : " + MatchStartImpl.SUM_DATA_MAP.keySet() + " percent " + MatchStartImpl.ID_MATCH_FLAG.get(matchId));
            if (MatchStartImpl.ID_MATCH_FLAG.get(matchId) < 100) {
                MatchStartImpl.ID_MATCH_FLAG.put(matchId, 100);
            }
            logger.info("after ID_MATCH_FLAG " +MatchStartImpl.ID_MATCH_FLAG.get(matchId));
            MappingId mappingId = new MappingId(matchId);
            String projectId= mappingId.getProjectId();
            List<PartnerProperty> propertyList = UniversalMapper.read(projectId);
            List<ClientInfo> clientInfos = propertyList.stream().map(PartnerProperty::toClientInfo).collect(Collectors.toList());
            for (ClientInfo client : clientInfos) {
                String response = SendAndRecv.send(client, matchId, "", 999, "", new EmptyMessage());
                logger.info("response : " + response);
            }
            boolean flag = ConfigUtil.getJdChainAvailable();
            if (!flag) {
                MatchMapper.insertMatchInfo(matchResult, userName, "", matchResult.getMappingReport().getReport());
            }
            logger.info("after insert matchResult into db");
        } catch (ParseException e) {
            logger.error("ParseException " + e.getMessage());
        }
    }

    private void updatePercent(String matchToken, int value) {
        if (value < 100) {
            value = value + VALUE_GAP;
        }
        MatchStartImpl.ID_MATCH_FLAG.put(matchToken, value);
    }
}
