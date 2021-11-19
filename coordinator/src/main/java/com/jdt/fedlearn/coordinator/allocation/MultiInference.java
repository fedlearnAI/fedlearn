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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jdt.fedlearn.common.entity.project.PartnerInfoNew;
import com.jdt.fedlearn.coordinator.constant.Constant;
import com.jdt.fedlearn.coordinator.constant.RequestConstant;
import com.jdt.fedlearn.coordinator.entity.common.BaseResp;
import com.jdt.fedlearn.coordinator.entity.inference.*;
import com.jdt.fedlearn.coordinator.exception.NotAcceptableException;
import com.jdt.fedlearn.coordinator.network.SendAndRecv;
import com.jdt.fedlearn.coordinator.service.inference.InferenceCommonService;
import com.jdt.fedlearn.common.entity.core.ClientInfo;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MultiInference implements Runnable {
    private static final Logger logger = LoggerFactory.getLogger(MultiInference.class);

    private static final String PERCENT = "percent";
    private static final String PREDICT_INFO = "predictInfo";
    private static final String END_TIME = "endTime";
    private static final String INFERENCE_SUCCESS = "推理完成";

    private static final String TEN = "10";
    private static final String ONE_HUNDRED = "100";
    private static final int INFERENCE_BATCH_SIZE = 10000;
    ObjectMapper mapper = new ObjectMapper();

    private final String inferenceId;
    private final ClientInfo clientAddress;
    private final String remotePath;
    private final List<PartnerInfoNew> clientList;
    private final String modelId;


    public MultiInference(String inferenceId, RemotePredict remotePredict) {
        this.inferenceId = inferenceId;
        ClientInfo property = ClientInfo.parseUrl(remotePredict.getUserAddress());
        this.clientAddress = property;
        this.remotePath = remotePredict.getPath();
        this.clientList = remotePredict.getClientList();
        this.modelId = remotePredict.getModelToken();
    }

    @Override
    public void run() {
        try {
            InferenceFetchDTO inferenceFetchDTO = null;
            try {
                inferenceFetchDTO = getUidList(remotePath, clientAddress);
            } catch (NotAcceptableException nae) {
                Map<String, Object> map = new HashMap<>();
                map.put(PERCENT, "-");
                map.put(PREDICT_INFO, "推理异常，获取文件异常");
                map.put(END_TIME, System.currentTimeMillis());
                ResourceManager.CACHE.putValue(inferenceId, map);
                logger.info("推理流水【{}】训练进度=【{}】", inferenceId, map);
                return;
            }
            updateCacheInfo(inferenceId, 10, "已获取数据");
            // 推理
            List<SingleInferenceRes> predict = doPredict(clientList, modelId, inferenceFetchDTO);
            // 推送推理结果到client
            final PushRsultDTO pushRsultDTO = pushResultToClient(mapper, clientAddress, predict, remotePath);

            buildPercentEnd(inferenceId, pushRsultDTO.getPath());
//                            logger.info("推理流水【{}】训练进度=【{}】", inferenceId, map);
        } catch (JsonProcessingException jpe) {
            Map<String, Object> map = new HashMap<>();
            map.put(PERCENT, "-");
            map.put(PREDICT_INFO, "推理异常，执行错误");
            map.put(END_TIME, System.currentTimeMillis());
            ResourceManager.CACHE.putValue(inferenceId, map);
            logger.info("推理流水【{}】训练进度=【{}】, 异常详情：", inferenceId, map, jpe);
        }
    }

    /**
     * 获取uid List
     *
     * @param remotePath path路径
     * @param clientInfo 客户端相关
     * @return inferenceFetchDTO
     * @throws JsonProcessingException 1
     */
    //TODO 返回总数据量和本次数据量，分批次推理
    private InferenceFetchDTO getUidList(String remotePath, ClientInfo clientInfo) throws JsonProcessingException {
        Map<String, Object> uidRequest = new HashMap<>();
        uidRequest.put(Constant.PATH, remotePath);
        final String uidResult = SendAndRecv.send(clientInfo, RequestConstant.INFERENCE_FETCH, uidRequest);
        return checkHttpResp(mapper, uidResult, InferenceFetchDTO.class, RequestConstant.INFERENCE_FETCH);
    }

    /**
     * http 返回结果校验接口
     *
     * @param mapper
     * @param result
     * @param t
     * @param inter
     * @param <T>
     * @return
     * @throws JsonProcessingException
     */
    private <T extends BaseResp> T checkHttpResp(ObjectMapper mapper, String result, Class<T> t, String inter) throws JsonProcessingException {
        if (StringUtils.isEmpty(result)) {
            throw new NotAcceptableException("调用" + inter + "失败");
        }
        final T resp = mapper.readValue(result, t);
        if (resp.getCode() != Constant.CODE) {
            throw new NotAcceptableException("调用" + inter + "失败");
        }
        return resp;
    }


    /**
     * 根据uid做预测
     *
     * @param remotePredict
     * @param inferenceFetchDTO
     * @return 结果
     * @throws JsonProcessingException
     */
    private static final String SUCCESS = "success";

    private List<SingleInferenceRes> doPredict(List<PartnerInfoNew> clientList, String modelId, InferenceFetchDTO dto) throws JsonProcessingException {
        // 调用推理接口
        Map<String, Object> percentMap = (Map<String, Object>) ResourceManager.CACHE.getValue(inferenceId);
        final InferenceRes inferenceRes = InferenceCommonService.commonInference(modelId, dto.getUid(), clientList, false);
        updateProgress(inferenceId, percentMap);
        List<SingleInferenceRes> predict = inferenceRes.getInferenceResList();
        int validateSize = (int) predict.stream().filter(m -> !(m.getScore().length == 0)).count();

        InferenceRequest inferenceRequest = new InferenceRequest();
        inferenceRequest.setUid(dto.getUid());
        inferenceRequest.setModelToken(modelId);
        inferenceRequest.setClientList(clientList);
        long startTime = (long) percentMap.get("startTime");

        InferenceCommonService.insertInferenceLog(inferenceRequest, inferenceId, startTime, SUCCESS, validateSize);
//        logger.info("调用predict，resp:{}", mapper.writeValueAsString(predict));
        return predict;
    }


    /**
     * 推送结果到client
     *
     * @param mapper
     * @param clientInfo
     * @param predict
     * @return
     * @throws JsonProcessingException
     */
    private PushRsultDTO pushResultToClient(ObjectMapper mapper, ClientInfo clientInfo, final List<SingleInferenceRes> predict, String path) throws JsonProcessingException {
        HashMap<String, Object> data = new HashMap<>();
        data.put("predict", predict);
        data.put("path", path);
        final String pushResult = SendAndRecv.send(clientInfo, RequestConstant.INFERENCE_PUSH, data);
        PushRsultDTO pushRsultDTO = checkHttpResp(mapper, pushResult, PushRsultDTO.class, RequestConstant.INFERENCE_PUSH);
        return pushRsultDTO;
    }

    /**
     * 构造推理结果进度数据
     *
     * @param inferenceId 推理唯一id
     * @param path        结果文件路径
     */
    private void buildPercentEnd(String inferenceId, String path) {
        Map<String, Object> percentMap = new HashMap<>();
        percentMap.put("path", path);
        percentMap.put(PERCENT, ONE_HUNDRED);
        percentMap.put(PREDICT_INFO, INFERENCE_SUCCESS);
        percentMap.put(END_TIME, System.currentTimeMillis());
        ResourceManager.CACHE.putValue(inferenceId, percentMap);
    }



    public void updateCacheInfo(String inferenceId, int percent, String desc) {
        if (percent > 100 || percent < 0) {
            return;
        }
        Map<String, Object> percentMap = (Map<String, Object>) ResourceManager.CACHE.getValue(inferenceId);
        percentMap.put(PERCENT, percent);
        percentMap.put(PREDICT_INFO, desc);
        ResourceManager.CACHE.putValue(inferenceId, percentMap);
    }

    //更新推理进度
    private void updateProgress(String inferenceId, Map<String, Object> percentMap) {
        int percent = Integer.parseInt(String.valueOf(percentMap.get(PERCENT)));//10
        percentMap.put(PERCENT, String.valueOf((percent <= 90) ? (++percent) : percent));
        percentMap.put(PREDICT_INFO, "推理中");
        ResourceManager.CACHE.putValue(inferenceId, percentMap);
        logger.info("推理流水【{}】训练进度=【{}】", inferenceId, ResourceManager.CACHE.getValue(inferenceId));
    }
}
