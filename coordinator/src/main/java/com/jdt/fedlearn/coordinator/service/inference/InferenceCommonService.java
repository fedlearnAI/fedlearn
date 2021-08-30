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
import com.jdt.fedlearn.common.util.FileUtil;
import com.jdt.fedlearn.common.util.TokenUtil;
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
import com.jdt.fedlearn.core.entity.ClientInfo;
import com.jdt.fedlearn.core.entity.common.CommonRequest;
import com.jdt.fedlearn.core.entity.common.CommonResponse;
import com.jdt.fedlearn.core.entity.common.PredictRes;
import com.jdt.fedlearn.core.parameter.SuperParameter;
import com.jdt.fedlearn.core.parameter.common.CommonParameter;
import com.jdt.fedlearn.core.type.AlgorithmType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 推理服务实现
 * 分为2种，
 * 1.远端推理
 * 2.批量推理，包括用户上传文件，调用接口等推理
 * 其中，远端推理为异步，提交后查询进度更新，完成后去读取数据
 * 批量推理为同步多线程推理，每个请求实时返回结果，可提交多个请求
 * 3.客户端推理
 * <p>
 * 核心的推理过程分为以下步骤：
 * 0.id去重和记录
 * 1.对去重后的id进行推理
 * 2.推理完成后删除客户端缓存
 * 3.对0中去重的id进行补全，保证返回的结果在长度和顺序上与原始输入保持一致
 */
public class InferenceCommonService {
    private static final Logger logger = LoggerFactory.getLogger(InferenceCommonService.class);
    private static final String PRE = "inference_";

    /**
     * 推理功能
     *
     * @return 推理结果
     */
    public static InferenceRes commonInference(String modelId, String[] queryId, List<PartnerInfoNew> partnerInfoNews, boolean secureMode) {
        if (queryId == null || queryId.length == 0) {
            throw new ForbiddenException("queryId 数组不能为空");
        }
        // 去重
        final Set<String> queryIdSet = Arrays.stream(queryId).collect(Collectors.toSet());
        final String[] distinctQueryId = queryIdSet.toArray(new String[]{});

        //参数准备
        TrainInfo model = getModelInfoById(modelId);
        AlgorithmType algorithmType = model.getAlgorithmType(); // 获取模型使用算法
        Control algorithm = algorithmSelection(model.getHyperParameter(), algorithmType);
        List<ClientInfo> clientList = partnerInfoNews.stream().map(PartnerInfoNew::toClientInfo).collect(Collectors.toList());
        String subInferenceId = TokenUtil.generateInferenceId(modelId);

        //开始推理
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
        // 删除客户端缓存
        // deleteClientCache(clientList, inferenceId, model.getModelToken());

        double[][] result = predictRes.getPredicts();
        // 构造结果越uid的对应关系
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
            // 如果缓存存在
            if (!Objects.isNull(modelValue)) {
                return (TrainInfo) modelValue;
            }
            model = TrainMapper.getTrainInfoByToken(modelId);
            if (Objects.isNull(model)) {
                throw new ForbiddenException("推理的model不能为空");
            }
            //缓存
            ResourceManager.CACHE.putValue(modelKey, model);
        }
        return model;
    }

    /**
     * 初始化算法
     *
     * @param parameterFieldList 参数列表
     * @param supportedAlgorithm 算法
     * @return 算法
     */
    private static Control algorithmSelection(List<SingleParameter> parameterFieldList, AlgorithmType supportedAlgorithm) {
        Map<String, Object> finishParameters = parameterFieldList.stream().collect(Collectors.toMap(SingleParameter::getField, SingleParameter::getValue));
        SuperParameter parameter = CommonParameter.parseParameter(finishParameters, supportedAlgorithm);
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
