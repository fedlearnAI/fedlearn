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


import com.jdt.fedlearn.common.util.TokenUtil;
import com.jdt.fedlearn.coordinator.dao.UniversalMapper;
import com.jdt.fedlearn.coordinator.dao.db.MatchMapper;
import com.jdt.fedlearn.coordinator.entity.train.StartTrain;
import com.jdt.fedlearn.coordinator.entity.train.StartValues;
import com.jdt.fedlearn.coordinator.entity.table.PartnerProperty;
import com.jdt.fedlearn.coordinator.entity.train.SingleParameter;
import com.jdt.fedlearn.coordinator.entity.train.TrainContext;
import com.jdt.fedlearn.coordinator.service.prepare.MatchStartImpl;
import com.jdt.fedlearn.core.entity.ClientInfo;
import com.jdt.fedlearn.core.entity.feature.Features;
import com.jdt.fedlearn.core.entity.feature.SingleFeature;
import com.jdt.fedlearn.core.psi.MatchResult;
import com.jdt.fedlearn.core.type.AlgorithmType;
import com.jdt.fedlearn.coordinator.type.RunningType;
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
        // 参与方信息
        List<PartnerProperty> partnerProperties = UniversalMapper.read(taskId);
        // 客户端信息
        List<ClientInfo> clientInfos = partnerProperties.stream().map(PartnerProperty::toClientInfo).collect(Collectors.toList());
        // 客户端数据集信息，与clientInfos顺序一致
        List<String> dataset = partnerProperties.stream().map(PartnerProperty::getDataset).collect(Collectors.toList());
        // 处理特征相关
        final String value = req.getAlgorithmParams().stream().filter(a -> StringUtils.equals("label", a.getField()))
                .findAny().get().getValue().toString();
        logger.info("label的result:" + value);
        List<Features> featuresMap = getFeatureMap(taskId, partnerProperties, value);
        String mappingType = (String) req.getCommonParams().get(0).getValue();
        Set<Map.Entry<String, MatchResult>> entries = MatchStartImpl.SUM_DATA_MAP.entrySet();
        Iterator<Map.Entry<String, MatchResult>> iterator = entries.iterator();
        MatchResult matchResult = null;
        while (iterator.hasNext()){
            Map.Entry<String, MatchResult> e = iterator.next();
            if(e.getKey().contains(taskId) && mappingType.equals(TokenUtil.parseToken(e.getKey()).getAlgorithm())){
                matchResult = e.getValue();
                break;
            }
        }
        if (matchResult == null) {
            logger.info("mapping type is " + mappingType);
            String matchIdStr = MatchMapper.isContainMatch(taskId, mappingType);
            logger.info("match token is " + matchIdStr);
            matchResult = MatchMapper.getMatchInfoByToken(matchIdStr);
        }
//        MatchStartImpl.SUM_DATA_MAP.remove(taskId);
        logger.info("MatchStartImpl.SUM_DATA_MAP:" + MatchStartImpl.SUM_DATA_MAP.keySet());

        // 整体集成
        final List<SingleParameter> algorithmParams = req.getAlgorithmParams();
        AlgorithmType supportedAlgorithm = AlgorithmType.valueOf(req.getModel());

        StartValues values = new StartValues(taskId, clientInfos, featuresMap, matchResult, dataset);

        values.setParameter(algorithmParams);
        values.setSupportedAlgorithm(supportedAlgorithm);
        return values;
    }

    public static boolean updateRunningType(String id, RunningType runningType) {
        //TODO status check
        trainContextMap.get(id).setRunningType(runningType);
        return true;
    }


    /**
     * @param taskId
     * @param partnerProperties
     * @param value
     * @description: 通过client获取client的feature
     * @return: List
     * @author: geyan29
     * @date: 2021/1/28 3:41 下午
     */
    private static List<Features> getFeatureMap(String taskId, List<PartnerProperty> partnerProperties, String value) {
        List<Features> featuresMap = new ArrayList<>();
        for (PartnerProperty partnerProperty : partnerProperties) {
            Features feature = UniversalMapper.readFeatures(taskId, partnerProperty);
            if (feature.getFeatureList().stream().map(SingleFeature::getName).anyMatch(x -> x.equals(value))) {
                feature.setLabel(value);
            } else {
                feature.setLabel(null);
            }
            featuresMap.add(feature);
            logger.info("clientInfo:" + partnerProperty.toString());
            logger.info("feature info:" + feature.toString() + "-----");
        }
        return featuresMap;
    }
}