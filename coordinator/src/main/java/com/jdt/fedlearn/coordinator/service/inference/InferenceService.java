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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jdt.fedlearn.common.util.TokenUtil;
import com.jdt.fedlearn.coordinator.allocation.ResourceManager;
import com.jdt.fedlearn.coordinator.constant.Constant;
import com.jdt.fedlearn.coordinator.constant.RequestConstant;
import com.jdt.fedlearn.coordinator.dao.db.InferenceLogMapper;
import com.jdt.fedlearn.coordinator.dao.db.PartnerMapper;
import com.jdt.fedlearn.coordinator.dao.db.TrainMapper;
import com.jdt.fedlearn.coordinator.dao.jdchain.ChainInferenceMapper;
import com.jdt.fedlearn.coordinator.dao.jdchain.ChainTrainMapper;
import com.jdt.fedlearn.coordinator.entity.jdchain.JdchainTrainInfo;
import com.jdt.fedlearn.coordinator.entity.common.BaseResp;
import com.jdt.fedlearn.coordinator.entity.table.TrainInfo;
import com.jdt.fedlearn.coordinator.entity.inference.PushRsultDTO;
import com.jdt.fedlearn.coordinator.entity.inference.QueryPredict;
import com.jdt.fedlearn.coordinator.entity.table.InferenceEntity;
import com.jdt.fedlearn.coordinator.entity.inference.InferenceFetchDTO;
import com.jdt.fedlearn.coordinator.entity.inference.InferenceRequest;
import com.jdt.fedlearn.coordinator.entity.inference.RemotePredict;
import com.jdt.fedlearn.coordinator.entity.table.PartnerProperty;
import com.jdt.fedlearn.coordinator.entity.train.SingleParameter;
import com.jdt.fedlearn.coordinator.exception.ForbiddenException;
import com.jdt.fedlearn.coordinator.exception.NotAcceptableException;
import com.jdt.fedlearn.coordinator.dao.jdchain.ChainFeaturePartnerMapper;
import com.jdt.fedlearn.coordinator.util.ConfigUtil;
import com.jdt.fedlearn.coordinator.network.SendAndRecv;
import com.jdt.fedlearn.coordinator.util.PacketUtil;
import com.jdt.fedlearn.core.dispatch.common.CommonControl;
import com.jdt.fedlearn.core.dispatch.common.Control;
import com.jdt.fedlearn.core.entity.ClientInfo;
import com.jdt.fedlearn.core.entity.common.CommonRequest;
import com.jdt.fedlearn.core.entity.common.CommonResponse;
import com.jdt.fedlearn.core.entity.common.PredictRes;
import com.jdt.fedlearn.core.parameter.SuperParameter;
import com.jdt.fedlearn.core.parameter.common.CommonParameter;
import com.jdt.fedlearn.core.type.AlgorithmType;
import org.apache.commons.lang3.StringUtils;
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
public class InferenceService {
    private static final Logger logger = LoggerFactory.getLogger(InferenceService.class);
    private static final int INFERENCE_BATCH_SIZE = 10000;
    public static final String INFERENCE_ID = "inferenceId";
    public static final String START_TIME = "startTime";
    public static final String PERCENT = "percent";
    public static final String PREDICT_INFO = "predictInfo";
    public static final String END_TIME = "endTime";
    public static final String TEN = "10";
    public static final String INFERENCE_SUCCESS = "推理完成";
    public static final String ONE_HUNDRED = "100";
    public static final String PREDICT = "predict";
    private static final String PRE = "inference_";
    public static final String UID = "uid";
    public static final String SCORE = "score";
    public static final String HEADER = "header";
    public static final String NAN = "NaN";

    /**
     * @param request 用户端发起的推理请求
     * @return 推理结果
     * @throws JsonProcessingException 1
     */
    public Map<String, Object> batchInference(InferenceRequest request) throws JsonProcessingException {
        String inferenceId = TokenUtil.generateInferenceToken(request.getModel());
        //进度map
        Map<String, Object> percentMap = new HashMap<>();
        // 推理开始，设置推理进度10%
        buildPercentStart(inferenceId, percentMap);

        // 调用推理
        // request里面包含uid list
        List<Map<String,Object>> predict = commonInference(request, percentMap);

        // 推理完成，推理进度100%
        buildPercentEnd(inferenceId, percentMap);
//                logger.info("推理流水【{}】训练进度=【{}】", inferenceId, (Map<String, Object>) CACHE.getValue(inferenceId));
        //将推理结果插入数据库
//        insertInferenceLog(request, inferenceId, startTime, "success");
        // 构造返回结果
        Map<String, Object> data = new HashMap<>();
        data.put(PREDICT, predict);
        return data;
    }

    /**
     * 构造推理结果进度数据
     *
     * @param inferenceId 推理唯一id
     * @param percentMap  推理进度
     */
    private void buildPercentEnd(String inferenceId, Map<String, Object> percentMap) {
        Date endTime = new Date();
        percentMap.put(PERCENT, ONE_HUNDRED);
        percentMap.put(PREDICT_INFO, INFERENCE_SUCCESS);
        percentMap.put(END_TIME, endTime.getTime());
        ResourceManager.CACHE.putValue(inferenceId, percentMap);
    }

    /**
     * 推理开始，构造推理结果进度
     *
     * @param inferenceId 推理唯一id
     * @param percentMap  推理进度
     */
    public void buildPercentStart(String inferenceId, Map<String, Object> percentMap) {
        Date startTime = new Date();
        percentMap.put(INFERENCE_ID, inferenceId);
        percentMap.put(START_TIME, startTime.getTime());
        percentMap.put(PERCENT, TEN);
        ResourceManager.CACHE.putValue(inferenceId, percentMap);
    }


    public void buildPercentProgress(String inferenceId, Map<String, Object> percentMap, int percent) {
        if (percent > 100 || percent < 0) {
            return;
        }
        Date currentTime = new Date();
        percentMap.put(INFERENCE_ID, inferenceId);
        percentMap.put(START_TIME, currentTime.getTime());
        percentMap.put(PERCENT, TEN);
        ResourceManager.CACHE.putValue(inferenceId, percentMap);
    }

    /**
     * 推理功能
     *
     * @param query 推理参数
     * @return 推理结果
     * @throws JsonProcessingException
     */
    public List<Map<String, Object>> commonInference(InferenceRequest query, Map<String, Object> percentMap) throws JsonProcessingException {
        String[] queryId = query.getUid();
        if (queryId == null || queryId.length == 0) {
            throw new ForbiddenException("queryId 数组不能为空");
        }
        // 查询推理相关信息
        TrainInfo model;
        // 查询客户端缓存
        List<ClientInfo> clientList;
        if (ConfigUtil.getJdChainAvailable()) {
            JdchainTrainInfo trainInfo = ChainTrainMapper.queryModelById(query.getModel());
            model = new TrainInfo(trainInfo.getModelToken(),AlgorithmType.valueOf(trainInfo.getAlgorithm()),trainInfo.getParameterFieldList(),trainInfo.getMetrics(),
                    trainInfo.getTrainStartTime().getTime(),trainInfo.getTrainEndTime().getTime(),trainInfo.getRunningType(),trainInfo.getPercent());
            clientList = ChainFeaturePartnerMapper.getClientInfos(trainInfo.getTaskId());
        } else {
            model = getModelToken(query); // model不可为空
            List<PartnerProperty> partnerProperties = getClientInfos(model);
            clientList = partnerProperties.stream().map(PartnerProperty::toClientInfo).collect(Collectors.toList());
        }
        AlgorithmType supportedAlgorithm = model.getAlgorithmType();

        // 数据传输分片
        List<String[]> dataList = PacketUtil.splitInference(queryId, INFERENCE_BATCH_SIZE);
        Map<String, Object> compoundRes = new HashMap<>();
        long s = System.currentTimeMillis();
        // 开始遍历分包数据
        logger.info("PacketUtil.splitInference: " + (System.currentTimeMillis() - s) + " ms");
        for (String[] subIds : dataList) {
            long ss = System.currentTimeMillis();
            logger.info("enter transeruid");
            String subInferenceId = TokenUtil.generateInferenceToken(query.getModel());
            // 开始预测
            Map<String, Object> subRes = transferUid(subIds, model, supportedAlgorithm, clientList, percentMap, subInferenceId);
            compoundRes.putAll(subRes);
            logger.info("transferUid: " + (System.currentTimeMillis() - ss) + " ms ");
        }

        List<Map<String, Object>> res = new ArrayList<>();
        Map<String, Object> header = new HashMap<>();
        header.put(UID, HEADER);
        header.put(SCORE, compoundRes.get(HEADER));
        res.add(header);
        for (int i = 0; i < queryId.length; i++) {
            String uid = queryId[i];
            Map<String, Object> singleResult = new HashMap<>();
            singleResult.put(UID, uid);
            singleResult.put(SCORE, NAN);
            // 有结果
            if (compoundRes.containsKey(uid)) {
                singleResult.put(SCORE, compoundRes.get(uid));
            }
            res.add(singleResult);
        }
        return res;
    }
    
    /**
     * 查询客户端信息
     *
     * @param model modelToken
     * @return 客户端信息
     */
    private List<PartnerProperty> getClientInfos(TrainInfo model) {
        String taskId = model.getModelToken().split("-")[0];
        final String taskIdKey = PRE + taskId;
        final Object clientValue = ResourceManager.CACHE.getValue(taskIdKey);
        if (!Objects.isNull(clientValue)) {
            return (List<PartnerProperty>) clientValue;
        }
        // 查询数据库
        List<PartnerProperty> propertyList = PartnerMapper.selectPartnerList(taskId);
        logger.info("propertyList " + propertyList.get(0));
        if (Objects.isNull(propertyList) || propertyList.size() == 0) {
            throw new ForbiddenException("推理的clientList不能为空");
        }
        // 设置缓存
        ResourceManager.CACHE.putValue(taskIdKey, propertyList);
        return propertyList;
    }

    /**
     * 查询推理结果
     *
     * @param query 推理请求
     * @return modelToken
     */
    private TrainInfo getModelToken(InferenceRequest query) {
        final String modelKey = PRE + query.getModel();
        final Object modelValue = ResourceManager.CACHE.getValue(modelKey);
        // 如果缓存存在
        if (!Objects.isNull(modelValue)) {
            return (TrainInfo) modelValue;
        }
        TrainInfo model = TrainMapper.getTrainInfoByToken(query.getModel());
        if (Objects.isNull(model)) {
            throw new ForbiddenException("推理的model不能为空");
        }
        ResourceManager.CACHE.putValue(modelKey, model);
        return model;
    }

    /**
     * @param queryId            请求id
     * @param model              modelToken
     * @param supportedAlgorithm 算法
     * @param clientList         客户端信息列表
     * @param percentMap         推理进度
     * @param inferenceId        推理唯一id
     * @return 推理结果
     * @throws JsonProcessingException
     */
    private Map<String, Object> transferUid(String[] queryId, TrainInfo model, AlgorithmType supportedAlgorithm, List<ClientInfo> clientList,
                                            Map<String, Object> percentMap, String inferenceId) throws JsonProcessingException {

        Control algorithm = algorithmSelection(model, supportedAlgorithm);
        assert algorithm != null;
        // step1 queryId 去重
        final Set<String> queryIdSet = Arrays.stream(queryId).collect(Collectors.toSet());
        // 获取到去重的uid数组
        final String[] distinctQueryId = queryIdSet.toArray(new String[]{});

        // 开始推理预测
        Map<String, Object> resultMap = doInference(distinctQueryId, model, supportedAlgorithm, clientList, percentMap, inferenceId, algorithm);

        // 删除缓存
        // deleteClientCache(clientList, inferenceId, model.getModelToken());
        return resultMap;
    }

    /**
     * 初始化算法
     *
     * @param model              modelToken
     * @param supportedAlgorithm 算法
     * @return 算法
     */
    private Control algorithmSelection(TrainInfo model, AlgorithmType supportedAlgorithm) {

        final List<SingleParameter> finishParameterFields = model.getHyperParameter();
        Map<String, Object> finishParameters = finishParameterFields.stream().collect(Collectors.toMap(SingleParameter::getField, SingleParameter::getValue));
        SuperParameter parameter = CommonParameter.parseParameter(finishParameters, supportedAlgorithm);
        logger.info("parameter : " + parameter);
        return CommonControl.dispatchConstruct(supportedAlgorithm, parameter);
    }

    /**
     * 开始预测
     *
     * @param existUidHasFiltered 需要推理的uid
     * @param model               modelToken
     * @param supportedAlgorithm  算法
     * @param clientList          客户端列表
     * @param percentMap          推理进度
     * @param inferenceId         推理唯一id
     * @param algorithm           算法
     */
    private Map<String, Object> doInference(String[] existUidHasFiltered, TrainInfo model, AlgorithmType supportedAlgorithm, List<ClientInfo> clientList, Map<String, Object> percentMap, String inferenceId, Control algorithm) {
        //正式的推理请求
//        int p = -1;
        List<CommonRequest> requests = algorithm.initInference(clientList, existUidHasFiltered);
        logger.info("requests 's size is : " + requests.size());
        int phase = requests.get(0).getPhase();
        logger.info("phase : " + phase);
        List<CommonResponse> responses = new ArrayList<>();
        while (algorithm.isInferenceContinue()) {
            responses = SendAndRecv.broadcastInference(requests, model.getModelToken(), supportedAlgorithm, inferenceId);
            logger.info("response size : " + responses.size());
            requests = algorithm.inferenceControl(responses);
            updateProgress(inferenceId, percentMap);
        }
        PredictRes predictRes = algorithm.postInferenceControl(responses);
        double[][] result = predictRes.getPredicts();
        String[] header = predictRes.getHeader();
        assert result.length == existUidHasFiltered.length;
        // 构造结果越uid的对应关系
        Map<String, Object> resultMap = new HashMap<>();
        resultMap.put(HEADER,header);
        for (int i = 0; i < existUidHasFiltered.length; i++) {
            resultMap.put(existUidHasFiltered[i], result[i]);
        }
        return resultMap;
    }

    private void insertInferenceLog(InferenceRequest query, String inferenceId, Date startTime, String referenceResult, int responseNum) {
        logger.info("userName: {}", query.getUsername());
        Date endTime = new Date();
        int requestNum = query.getUid() == null ? 0 : query.getUid().length;
        final InferenceEntity inferenceEntity =
                new InferenceEntity(query.getUsername(), query.getModel(), inferenceId, startTime, endTime, referenceResult, requestNum, responseNum);
        if (ConfigUtil.getJdChainAvailable()) {
            ChainInferenceMapper.insertInferenceLog(inferenceEntity);
        } else {
            InferenceLogMapper.insertInference(inferenceEntity);
        }
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

    private List<Map<String,Object>> doPredict(RemotePredict remotePredict, InferenceFetchDTO inferenceFetchDTO, String inferenceId, Date startTime) throws JsonProcessingException {
        InferenceRequest inferenceRequest = new InferenceRequest();
        inferenceRequest.setUid(inferenceFetchDTO.getUid());
        inferenceRequest.setModel(remotePredict.getModelToken());
        inferenceRequest.setUsername(remotePredict.getUsername());
        // 调用推理接口
        Map<String, Object> percentMap = (Map<String, Object>) ResourceManager.CACHE.getValue(inferenceId);
        final List<Map<String,Object>> predict = commonInference(inferenceRequest, percentMap);
        List<Map> collect = predict.stream().filter(m -> !m.get(SCORE).equals(NAN)).collect(Collectors.toList());
        insertInferenceLog(inferenceRequest, inferenceId, startTime, SUCCESS, collect.size());
//        logger.info("调用predict，resp:{}", mapper.writeValueAsString(predict));
        return predict;
    }


    /**
     * 为client 推送地址
     *
     * @param remotePredict
     * @return
     */
    public Object predictRemote(RemotePredict remotePredict) {
        String inferenceId = TokenUtil.generateInferenceToken(remotePredict.getModelToken());
        ObjectMapper mapper = new ObjectMapper();
        final PartnerProperty property;
        if (ConfigUtil.getJdChainAvailable()) {
            String taskId = TokenUtil.parseToken(remotePredict.getModelToken()).getTaskId();
            ClientInfo clientInfo = ChainFeaturePartnerMapper.getClientInfo(taskId, remotePredict.getUsername());
            //TODO getdataset
            property = new PartnerProperty("", clientInfo.getProtocol(), clientInfo.getIp(), clientInfo.getPort(), clientInfo.getUniqueId(), "clientInfo.getDataset()");
        } else {
            property = PartnerMapper.selectClientByToken(remotePredict.getModelToken(), remotePredict.getUsername());
        }
        // 调用获取uid的接口
        Map<String, Object> map = new HashMap<>();
        Date startTime = new Date();
        map.put(INFERENCE_ID, inferenceId);
        map.put(PERCENT, "0");
        map.put(PREDICT_INFO, "开始推理");
        map.put(START_TIME, startTime.getTime());
        ResourceManager.CACHE.putValue(inferenceId, map);
        if (null != property) {
            // 多线程
            ResourceManager.POOL.submit(() -> {
                try {
                    InferenceFetchDTO inferenceFetchDTO = null;
                    try {
                        inferenceFetchDTO = getUidList(remotePredict, mapper, property.toClientInfo());
                    } catch (NotAcceptableException nae) {
                        map.put(PERCENT, "-");
                        map.put(PREDICT_INFO, "推理异常，获取文件异常");
                        map.put(END_TIME, System.currentTimeMillis());
                        ResourceManager.CACHE.putValue(inferenceId, map);
                        logger.info("推理流水【{}】训练进度=【{}】", inferenceId, map);
                        return;
                    }
                    map.put(PERCENT, "10");
                    map.put(PREDICT_INFO, "已获取数据");
                    ResourceManager.CACHE.putValue(inferenceId, map);
//                        logger.info("推理流水【{}】训练进度=【{}】", inferenceId, map);
                    // 推送uidList 到client
                    List<Map<String,Object>> predict = null;
                    if (null != inferenceFetchDTO) {
                        predict = doPredict(remotePredict, inferenceFetchDTO, inferenceId, startTime);
                    } else {
                        map.put(PERCENT, "-");
                        map.put(PREDICT_INFO, "推理异常，获取文件异常");
                        map.put(END_TIME, System.currentTimeMillis());
                        ResourceManager.CACHE.putValue(inferenceId, map);
                        logger.info("推理流水【{}】训练进度=【{}】", inferenceId, map);
                        return;
                    }
                    // 推送推理结果到client;
                    if (null != predict) {
                        final PushRsultDTO pushRsultDTO = pushResultToClient(mapper, property.toClientInfo(), predict, (String) remotePredict.getPath());
                        map.put(Constant.PATH, pushRsultDTO.getPath());
                        map.put(PERCENT, "100");
                        map.put(PREDICT_INFO, INFERENCE_SUCCESS);
                        map.put(END_TIME, System.currentTimeMillis());
                        ResourceManager.CACHE.putValue(inferenceId, map);
//                            logger.info("推理流水【{}】训练进度=【{}】", inferenceId, map);
                        return;
                    }
                    map.put(PERCENT, "-");
                    map.put(PREDICT_INFO, "推理异常，推送文件异常");
                    map.put(END_TIME, System.currentTimeMillis());
                    ResourceManager.CACHE.putValue(inferenceId, map);
                    logger.info("推理流水【{}】训练进度=【{}】", inferenceId, map);
                    return;
                } catch (JsonProcessingException jpe) {
                    map.put(PERCENT, "-");
                    map.put(PREDICT_INFO, "推理异常，执行错误");
                    map.put(END_TIME, System.currentTimeMillis());
                    ResourceManager.CACHE.putValue(inferenceId, map);
                    logger.info("推理流水【{}】训练进度=【{}】, 异常详情：", inferenceId, map, jpe);
                }
            });
        } else {
            map.put(PERCENT, "-");
            map.put(PREDICT_INFO, "客户端信息不存在");
            ResourceManager.CACHE.putValue(inferenceId, map);
            logger.info("推理流水【{}】训练进度=【{}】", inferenceId, map);
        }
        return map;
    }


    /**
     * 获取uid List
     *
     * @param remotePredict path路径
     * @param mapper        mapper
     * @param clientInfo    客户端相关
     * @return inferenceFetchDTO
     * @throws JsonProcessingException
     */
    private InferenceFetchDTO getUidList(RemotePredict remotePredict, ObjectMapper mapper, ClientInfo clientInfo) throws JsonProcessingException {
        Map<String, Object> uidRequest = new HashMap<>();
        uidRequest.put(Constant.PATH, remotePredict.getPath());
        final String uidResult = SendAndRecv.send(clientInfo, RequestConstant.EXECUTE_INFERENCE_FETCH, Constant.HTTP_POST, uidRequest);
        return checkHttpResp(mapper, uidResult, InferenceFetchDTO.class, RequestConstant.EXECUTE_INFERENCE_FETCH);
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
     * 推送结果到client
     *
     * @param mapper
     * @param clientInfo
     * @param predict
     * @return
     * @throws JsonProcessingException
     */
    private PushRsultDTO pushResultToClient(ObjectMapper mapper, ClientInfo clientInfo, final List<Map<String,Object>> predict, String path) throws JsonProcessingException {
        HashMap<String, Object> data = new HashMap<>();
        data.put("predict", predict);
        data.put("path", path);
        final String pushResult = SendAndRecv.send(clientInfo, RequestConstant.EXECUTE_INFERENCE_PUSH, Constant.HTTP_POST, data);
        PushRsultDTO pushRsultDTO = checkHttpResp(mapper, pushResult, PushRsultDTO.class, RequestConstant.EXECUTE_INFERENCE_PUSH);
        return pushRsultDTO;
    }


    //核心的推理功能
    public double[][] inferenceCore(List<ClientInfo> clientInfos, AlgorithmType supportedAlgorithm, Control algorithm, String modelToken, String[] queryId, String inferenceId, Map<String, Object> percentMap) {
        List<CommonRequest> requests = algorithm.initInference(clientInfos, queryId);
        int p = -1;
        List<CommonResponse> responses = new ArrayList<>();
        while (algorithm.isInferenceContinue()) {
            responses = SendAndRecv.broadcastInference(requests, modelToken, supportedAlgorithm, inferenceId);
            requests = algorithm.inferenceControl(responses);

            updateProgress(inferenceId, percentMap);
        }
        return algorithm.postInferenceControl(responses).getPredicts();
    }


    //更新推理进度
    private void updateProgress(String inferenceId, Map<String, Object> percentMap) {
        int percent = Integer.parseInt(String.valueOf(percentMap.get(PERCENT)));//10
        percentMap.put(PERCENT, String.valueOf((percent <= 90) ? (++percent) : percent));
        percentMap.put(PREDICT_INFO, "推理中");
        ResourceManager.CACHE.putValue(inferenceId, percentMap);
        logger.info("推理流水【{}】训练进度=【{}】", inferenceId, ResourceManager.CACHE.getValue(inferenceId));
    }


    /**
     * 推理进度查询，包括三种，
     * 正在推理的，推理完成的，推理失败的
     *
     * @param queryPredict 推理进度查询请求体
     * @return 推理进度查询结果
     */
    public Map<String, Object> predictQuery(QueryPredict queryPredict) {
        Map<String, Object> map = null;
        if (ResourceManager.CACHE.constainsKey(queryPredict.getInferenceId())) {
            map = (Map<String, Object>) ResourceManager.CACHE.getValue(queryPredict.getInferenceId());
        } else {
            InferenceEntity inferenceLog = InferenceLogMapper.getInferenceLog(queryPredict.getInferenceId());
            map = new HashMap<>();
            if (inferenceLog.getInferenceId() == null) {
                map.put(INFERENCE_ID, queryPredict.getInferenceId());
                map.put(PERCENT, "-");
                map.put(PREDICT_INFO, "推理失败");
            } else {
                map.put(INFERENCE_ID, queryPredict.getInferenceId());
                map.put(START_TIME, inferenceLog.getStartTime().getTime());
                map.put(END_TIME, inferenceLog.getEndTime().getTime());
                map.put(PERCENT, "100");
                map.put(PREDICT_INFO, INFERENCE_SUCCESS);
            }
        }
        return map;
    }


}
