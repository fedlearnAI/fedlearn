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

package com.jdt.fedlearn.coordinator.service.inference;

import com.jdt.fedlearn.common.entity.project.PartnerInfoNew;
import com.jdt.fedlearn.coordinator.allocation.ResourceManager;
import com.jdt.fedlearn.coordinator.constant.Constant;
import com.jdt.fedlearn.coordinator.dao.UniversalMapper;
import com.jdt.fedlearn.coordinator.dao.db.TrainMapper;
import com.jdt.fedlearn.coordinator.dao.jdchain.ChainTrainMapper;
import com.jdt.fedlearn.coordinator.entity.inference.InferenceRes;
import com.jdt.fedlearn.coordinator.entity.inference.SingleInferenceRes;
import com.jdt.fedlearn.coordinator.entity.jdchain.JdchainTrainInfo;
import com.jdt.fedlearn.coordinator.entity.table.TrainInfo;
import com.jdt.fedlearn.coordinator.entity.table.InferenceEntity;
import com.jdt.fedlearn.coordinator.entity.inference.InferenceRequest;
import com.jdt.fedlearn.common.entity.SingleParameter;
import com.jdt.fedlearn.coordinator.exception.ForbiddenException;
import com.jdt.fedlearn.coordinator.util.ConfigUtil;
import com.jdt.fedlearn.coordinator.network.SendAndRecv;
import com.jdt.fedlearn.core.dispatch.common.DispatcherFactory;
import com.jdt.fedlearn.core.dispatch.common.Control;
import com.jdt.fedlearn.common.entity.core.ClientInfo;
import com.jdt.fedlearn.core.entity.common.CommonRequest;
import com.jdt.fedlearn.core.entity.common.CommonResponse;
import com.jdt.fedlearn.core.entity.common.PredictRes;
import com.jdt.fedlearn.core.parameter.HyperParameter;
import com.jdt.fedlearn.core.parameter.common.CommonParameter;
import com.jdt.fedlearn.common.entity.core.type.AlgorithmType;
import com.jdt.fedlearn.tools.FileUtil;
import com.jdt.fedlearn.tools.TokenUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.stream.Collectors;

/**
 * ??????????????????
 * ??????2??????
 * 1.????????????
 * 2.???????????????????????????????????????????????????????????????
 * ???????????????????????????????????????????????????????????????????????????????????????
 * ?????????????????????????????????????????????????????????????????????????????????????????????
 * 3.???????????????
 * <p>
 * ??????????????????????????????????????????
 * 0.id???????????????
 * 1.???????????????id????????????
 * 2.????????????????????????????????????
 * 3.???0????????????id????????????????????????????????????????????????????????????????????????????????????
 */
public class InferenceCommonService {
    private static final Logger logger = LoggerFactory.getLogger(InferenceCommonService.class);
    private static final String PRE = "inference_";

    /**
     * ????????????
     *
     * @return ????????????
     */
    public static InferenceRes commonInference(String modelId, String[] queryId, List<PartnerInfoNew> partnerInfoNews, boolean secureMode) {
        if (queryId == null || queryId.length == 0) {
            throw new ForbiddenException("queryId ??????????????????");
        }
        // ??????
        final Set<String> queryIdSet = Arrays.stream(queryId).collect(Collectors.toSet());
        final String[] distinctQueryId = queryIdSet.toArray(new String[]{});

        //????????????
        TrainInfo model = getModelInfoById(modelId);
        AlgorithmType algorithmType = model.getAlgorithmType(); // ????????????????????????
        Control algorithm = algorithmSelection(model.getHyperParameter(), algorithmType);
        List<ClientInfo> clientList = partnerInfoNews.stream().map(PartnerInfoNew::toClientInfo).collect(Collectors.toList());
        String subInferenceId = TokenUtil.generateInferenceId(modelId);

        //????????????
        long ss = System.currentTimeMillis();
        Map<String, Object> others = new HashMap<>();
        if (secureMode) {
            String pubPath = ConfigUtil.getPubKeyDir() + Constant.PUB_KEY;
            String content = FileUtil.loadClassFromFile(pubPath);
            others.put("pubKeyStr", content);
        }
        List<CommonRequest> requests = algorithm.initInference(clientList, distinctQueryId, others);
        List<CommonResponse> responses = new ArrayList<>();
        while (algorithm.isInferenceContinue()) {

            responses = SendAndRecv.broadcastInference(requests, modelId, algorithmType, subInferenceId, partnerInfoNews);
            logger.info("response size : " + responses.size());
            requests = algorithm.inferenceControl(responses);
        }
        PredictRes predictRes = algorithm.postInferenceControl(responses);
        logger.info("inference time consume: " + (System.currentTimeMillis() - ss) + " ms ");
        // ?????????????????????
        // deleteClientCache(clientList, inferenceId, model.getModelToken());

        double[][] result = predictRes.getPredicts();
        // ???????????????uid???????????????
        Map<String, double[]> resultMap = new HashMap<>();
        for (int i = 0; i < distinctQueryId.length; i++) {
            resultMap.put(distinctQueryId[i], result[i]);
        }

        List<SingleInferenceRes> res = new ArrayList<>();
        for (String uid : queryId) {
            double[] score = new double[0];
            if (resultMap.containsKey(uid)) {
                score = resultMap.get(uid);
            }
            SingleInferenceRes inferenceRes = new SingleInferenceRes(uid, score);
            res.add(inferenceRes);
        }
        return new InferenceRes("uid", predictRes.getHeader(), res);
    }


    private static TrainInfo getModelInfoById(String modelId) {
        TrainInfo model;
        if (ConfigUtil.getJdChainAvailable()) {
            JdchainTrainInfo trainInfo = ChainTrainMapper.queryModelById(modelId);
            model = new TrainInfo(trainInfo.getModelToken(), AlgorithmType.valueOf(trainInfo.getAlgorithm()), trainInfo.getParameterFieldList(), trainInfo.getMetrics(),
                    trainInfo.getTrainStartTime().getTime(), trainInfo.getTrainEndTime().getTime(), trainInfo.getRunningType(), trainInfo.getPercent());
        } else {
            final String modelKey = PRE + modelId;
            final Object modelValue = ResourceManager.CACHE.getValue(modelKey);
            // ??????????????????
            if (!Objects.isNull(modelValue)) {
                return (TrainInfo) modelValue;
            }
            model = TrainMapper.getTrainInfoByToken(modelId);
            if (Objects.isNull(model)) {
                throw new ForbiddenException("?????????model????????????");
            }
            //??????
            ResourceManager.CACHE.putValue(modelKey, model);
        }
        return model;
    }

    /**
     * ???????????????
     *
     * @param parameterFieldList ????????????
     * @param supportedAlgorithm ??????
     * @return ??????
     */
    private static Control algorithmSelection(List<SingleParameter> parameterFieldList, AlgorithmType supportedAlgorithm) {
        Map<String, Object> finishParameters = parameterFieldList.stream().collect(Collectors.toMap(SingleParameter::getField, SingleParameter::getValue));
        HyperParameter parameter = CommonParameter.parseParameter(finishParameters, supportedAlgorithm);
        logger.info("parameter : " + parameter);
        return DispatcherFactory.getDispatcher(supportedAlgorithm, parameter);
    }


    public static void insertInferenceLog(InferenceRequest query, String inferenceId, long startTimestamp, String referenceResult, int responseNum) {
        Date startTime = new Date();
        startTime.setTime(startTimestamp);
        Date endTime = new Date();
        int requestNum = query.getUid() == null ? 0 : query.getUid().length;
        InferenceEntity entity = new InferenceEntity("fedlearn", query.getModelToken(), inferenceId,
                startTime, endTime, referenceResult, requestNum, responseNum);
        UniversalMapper.insertInference(entity);
    }
}
