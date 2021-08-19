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


package com.jdt.fedlearn.coordinator.service.train;


import com.jdt.fedlearn.common.entity.project.FeatureDTO;
import com.jdt.fedlearn.common.entity.project.MatchPartnerInfo;
import com.jdt.fedlearn.common.entity.project.PartnerInfoNew;
import com.jdt.fedlearn.common.enums.RunningType;
import com.jdt.fedlearn.common.util.TokenUtil;
import com.jdt.fedlearn.coordinator.entity.table.MatchEntity;
import com.jdt.fedlearn.coordinator.entity.train.StartTrain;
import com.jdt.fedlearn.coordinator.entity.train.StartValues;
import com.jdt.fedlearn.common.entity.SingleParameter;
import com.jdt.fedlearn.coordinator.entity.train.TrainContext;
import com.jdt.fedlearn.coordinator.service.prepare.MatchStartImpl;
import com.jdt.fedlearn.core.entity.ClientInfo;
import com.jdt.fedlearn.core.entity.feature.Features;
import com.jdt.fedlearn.core.entity.feature.SingleFeature;
import com.jdt.fedlearn.core.psi.MatchResult;
import com.jdt.fedlearn.core.type.AlgorithmType;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * 训练任务的通用类
 * <p>{@code isExist}方法用于判断某个{@code modelToken}是否存在在全局变量{@code trainContextMap}中</p>
 * <p>{@code startPrepare}方法用于准备好模型训练前的所需各类参数、特征、id对齐结果等方面的信息并返回</p>
 *
 * @author lijingxi
 */

public class TrainCommonServiceImpl {
    private static final Logger logger = LoggerFactory.getLogger(TrainCommonServiceImpl.class);
    public static Map<String, TrainContext> trainContextMap = new ConcurrentHashMap<>();

    public static Boolean isExist(String modelToken) {
        return trainContextMap.containsKey(modelToken);
    }

    //准备工作
    public static StartValues startPrepare(StartTrain req) {
        // TaskID
        String taskId = req.getTaskId();
        // 处理特征相关
        final String value = req.getAlgorithmParams().stream().filter(a -> StringUtils.equals("label", a.getField()))
                .findAny().get().getValue().toString();
        logger.info("label的result:" + value);
        // 从clientList获取features信息
        List<FeatureDTO> featuresList = req.getClientList().stream().map(PartnerInfoNew::getFeatures).collect(Collectors.toList());
        List<Features> featuresMap = getFeatureMap(featuresList, value);
        List<PartnerInfoNew> partnerInfos = req.getClientList();
        String mappingType = TokenUtil.parseToken(req.getMatchId()).getAlgorithm();
        String matchTaskId = TokenUtil.parseToken(req.getMatchId()).getTaskId();
        MatchEntity matchEntity = MatchStartImpl.getMatchEntity(matchTaskId, mappingType);
        if(matchEntity == null){
            logger.error("输入的id对齐信息有误，请对齐");
        }
        assert matchEntity != null;
        List<MatchPartnerInfo> matchPartnerInfo = matchEntity.getDatasets();
        // TODO clientList 与features合并到一个实体
        List<MatchPartnerInfo> trainPartnerInfo = new ArrayList<>();
        for (int i = 0; i < partnerInfos.size(); i++) {
            PartnerInfoNew partnerInfoNew = partnerInfos.get(i);
            trainPartnerInfo.add(new MatchPartnerInfo(partnerInfoNew.getUrl(), partnerInfoNew.getDataset(), featuresList.get(i).getIndex()));
        }
//        MatchStartImpl.SUM_DATA_MAP.remove(taskId);
        if(!checkContinue(matchPartnerInfo,trainPartnerInfo)){
            logger.error("id对齐和训练的客户端信息或数据集等信息不一致，请核对信息！！");
            //todo return error info
            return null;
        }
        List<ClientInfo> clientInfos = partnerInfos.stream().map(PartnerInfoNew::toClientInfo).collect(Collectors.toList());
        List<String> dataset = partnerInfos.stream().map(PartnerInfoNew::getDataset).collect(Collectors.toList());
        // 整体集成
        final List<SingleParameter> algorithmParams = req.getAlgorithmParams();
        AlgorithmType supportedAlgorithm = AlgorithmType.valueOf(req.getModel());
        MatchResult matchResult = new MatchResult(matchEntity.getMatchId(), matchEntity.getLength(), matchEntity.getMatchReport());
        StartValues values = new StartValues(taskId, clientInfos, featuresMap, matchResult, dataset);
        values.setParameter(algorithmParams);
        values.setSupportedAlgorithm(supportedAlgorithm);
        return values;
    }


    private static boolean checkContinue(List<MatchPartnerInfo> matchPartnerInfos, List<MatchPartnerInfo> trainParterInfos) {
        return matchPartnerInfos.size() == trainParterInfos.size() && matchPartnerInfos.containsAll(trainParterInfos);
    }

    public static boolean updateRunningType(String id, RunningType runningType) {
        //TODO status check
        trainContextMap.get(id).setRunningType(runningType);
        return true;
    }



    private static List<Features> getFeatureMap(List<FeatureDTO> featuresMap, String value) {
        List<Features> features = new ArrayList<>();
        for (int i = 0; i < featuresMap.size(); i++) {
            Features featureInfo = featuresMap.get(i).toFeatures();
            if (featureInfo.getFeatureList().stream().map(SingleFeature::getName).anyMatch(x -> x.equals(value))) {
                featureInfo.setLabel(value);
            } else {
                featureInfo.setLabel(null);
            }
            features.add(featureInfo);
        }
        return features;
    }
}