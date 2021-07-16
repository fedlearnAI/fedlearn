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
package com.jdt.fedlearn.worker.service;


import com.jdt.fedlearn.common.exception.NotAcceptableException;
import com.jdt.fedlearn.worker.cache.TrainDataCache;
import com.jdt.fedlearn.worker.constant.Constant;
import com.jdt.fedlearn.worker.dao.IdMatchProcessor;
import com.jdt.fedlearn.worker.entity.prepare.MatchRequest;
import com.jdt.fedlearn.common.util.PacketUtil;
import com.jdt.fedlearn.core.entity.psi.MatchInit;
import com.jdt.fedlearn.core.psi.CommonPrepare;
import com.jdt.fedlearn.core.psi.PrepareClient;
import com.jdt.fedlearn.common.constant.ResponseConstant;
import com.jdt.fedlearn.core.type.MappingType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.jdt.fedlearn.core.entity.Message;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static com.jdt.fedlearn.common.util.PacketUtil.msgMap;

/**
 * 预处理模块包括 id 对齐， 交叉验证等功能
 */
public class PrepareService {
    private static final Logger logger = LoggerFactory.getLogger(PrepareService.class);
    Map<String, PrepareClient> clientMap = new ConcurrentHashMap<>();

    public Map<String, Object> match(MatchRequest request) {
        Map<String, Object> modelMap = new HashMap<>();
        String strBody = "";
        try {
            String matchToken = request.getMatchToken();
            if (request.getPhase() == 999) {
                // TODO 目前只该了Vertical-MD5，其他的还没有改，会报错
                String[] matchIds = clientMap.get(matchToken).getCommonIds();
                // 储存id对齐结果
                Boolean success = IdMatchProcessor.saveResult(matchToken, matchIds);
                // 清楚client端缓存
                clientMap.remove(matchToken);
                logger.info("Match Result saved successfully: ", success);
                modelMap.put("data", strBody);
                modelMap.put(ResponseConstant.CODE, 0);
                modelMap.put(ResponseConstant.STATUS, "success");
                return modelMap;
            }

            String dataset = request.getDataset();
            logger.info("dataset: " + dataset);
            MappingType type = MappingType.valueOf(request.getMatchType());
            Message restoreMessage = Constant.serializer.deserialize(request.getBody());
            String[] tmp = new String[0];
            if (!clientMap.containsKey(matchToken)) {
                PrepareClient uidMatchClient = CommonPrepare.constructClient(type);
                clientMap.put(matchToken, uidMatchClient);
                if (!(restoreMessage instanceof MatchInit)) {
                    logger.info("error: unexpected message from initial phase of ID match");
                    throw new NotAcceptableException("unexpected message from initial phase of ID match");
                }
                MatchInit matchInit = (MatchInit)restoreMessage;
                String[][] uidList = TrainDataCache.readFullTrainData(dataset);
                tmp = TrainDataCache.getFirstColumnUid(uidList);
                Message trainData = uidMatchClient.init(tmp, matchInit.getOthers());
                strBody = Constant.serializer.serialize(trainData);
            } else {
                PrepareClient uidMatchClient = clientMap.get(matchToken);
                Message body = uidMatchClient.client(request.getPhase(), restoreMessage, tmp);
                strBody = Constant.serializer.serialize(body);
            }
            modelMap.put("data", strBody);
            modelMap.put(ResponseConstant.CODE, 0);
            modelMap.put(ResponseConstant.STATUS, "success");
        } catch (Exception e) {
            modelMap.put(ResponseConstant.CODE, -1);
            modelMap.put(ResponseConstant.STATUS, "fail");
            logger.error("Match error", e);
        }
        return modelMap;
    }


    public Map<String, Object> getSplitData(Map content) {
        Map<String, Object> modelMap = new HashMap<>();
        try {
            String msgid = (String) content.get("msgid");
            int dataSize = (int) content.get("dataSize");
            int dataIndex = (int) content.get("dataIndex");
            List<String> dataList = PacketUtil.msgMap.get(msgid);
            String data = dataList.get(dataIndex);
            modelMap.put("data", data);
            modelMap.put(ResponseConstant.CODE, 0);
            modelMap.put(ResponseConstant.STATUS, "success");
            if (dataIndex == dataSize - 1) {
                msgMap.remove(msgid);
            }
        } catch (Exception e) {
            modelMap.put(ResponseConstant.CODE, -1);
            modelMap.put(ResponseConstant.STATUS, "fail");
            logger.error("get split data error: ", e);
        }
        return modelMap;
    }
}
