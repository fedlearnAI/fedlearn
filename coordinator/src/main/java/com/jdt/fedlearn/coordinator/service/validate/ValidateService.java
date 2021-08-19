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

package com.jdt.fedlearn.coordinator.service.validate;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jdt.fedlearn.common.entity.project.PartnerInfoNew;
import com.jdt.fedlearn.common.util.TokenUtil;
import com.jdt.fedlearn.coordinator.allocation.ResourceManager;
import com.jdt.fedlearn.coordinator.constant.Constant;
import com.jdt.fedlearn.coordinator.constant.RequestConstant;
import com.jdt.fedlearn.coordinator.dao.db.TrainMapper;
import com.jdt.fedlearn.coordinator.dao.jdchain.ChainTrainMapper;
import com.jdt.fedlearn.common.entity.SingleParameter;
import com.jdt.fedlearn.coordinator.entity.validate.ValidateRequest;
import com.jdt.fedlearn.coordinator.entity.jdchain.JdchainTrainInfo;
import com.jdt.fedlearn.coordinator.entity.common.BaseResp;
import com.jdt.fedlearn.coordinator.entity.table.TrainInfo;
import com.jdt.fedlearn.coordinator.exception.ForbiddenException;
import com.jdt.fedlearn.coordinator.exception.NotAcceptableException;
import com.jdt.fedlearn.coordinator.service.inference.InferenceCommonService;
import com.jdt.fedlearn.coordinator.service.train.TrainCommonServiceImpl;
import com.jdt.fedlearn.coordinator.util.ConfigUtil;
import com.jdt.fedlearn.coordinator.network.SendAndRecv;
import com.jdt.fedlearn.coordinator.util.PacketUtil;
import com.jdt.fedlearn.core.dispatch.common.DispatcherFactory;
import com.jdt.fedlearn.core.dispatch.common.Control;
import com.jdt.fedlearn.core.entity.ClientInfo;
import com.jdt.fedlearn.core.entity.common.CommonRequest;
import com.jdt.fedlearn.core.entity.common.CommonResponse;
import com.jdt.fedlearn.core.math.MathExt;
import com.jdt.fedlearn.core.parameter.SuperParameter;
import com.jdt.fedlearn.core.parameter.common.CommonParameter;
import com.jdt.fedlearn.core.type.AlgorithmType;
import com.jdt.fedlearn.core.type.data.Tuple2;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 验证服务的实现类，包含验证服务需要的方法；与推理服务结构类；包含
 * <p>{@code buildPercentStart}方法： 验证开始，设置验证进度10%</p>
 * <p>{@code commonValidate}方法： 调用验证开始</p>
 * <p>{@code buildPercentEnd}方法：验证完成，验证进度100%</p>
 * @see InferenceCommonService
 */
public class ValidateService {
    private static final Logger logger = LoggerFactory.getLogger(ValidateService.class);
    private static final TrainCommonServiceImpl service = new TrainCommonServiceImpl();
    private static final int INFERENCE_BATCH_SIZE = 10000;
    public static final String INFERENCE_ID = "inferenceId";
    public static final String START_TIME = "startTime";
    public static final String PERCENT = "percent";
    public static final String PREDICT_INFO = "predictInfo";
    public static final String END_TIME = "endTime";
    public static final String TEN = "10";
    public static final String INFERENCE_SUCCESS = "验证完成";
    public static final String ONE_HUNDRED = "100";
    public static final String PREDICT = "predict";
    public static final String METRICS = "metrics";
    public static final String LABEL = "label";
    private static final String PRE = "inference_";
    public static final String UID = "uid";
    public static final String SCORE = "score";
    public static final String NAN = "NaN";
    public static final String FILTER_LIST = "filterList";

    /**
     * @param request 用户端发起的验证请求
     * @return 验证结果
     * @throws JsonProcessingException 1
     */
    public Map<String, Object> batchValidate(ValidateRequest request) throws JsonProcessingException {
        String inferenceId = TokenUtil.generateInferenceId(request.getModel());
        //进度map
        Map<String, Object> percentMap = new HashMap<>();
        // 验证开始，设置验证进度10%
        buildPercentStart(inferenceId, percentMap);

        // 调用验证
        Tuple2<String, List<Map<String, Object>>> metricPredict = commonValidate(request, percentMap);
        String validateMetric = metricPredict._1();
        List<Map<String,Object>> predict = metricPredict._2();

        // 验证完成，验证进度100%
        buildPercentEnd(inferenceId, percentMap);
//                logger.info("验证流水【{}】训练进度=【{}】", inferenceId, (Map<String, Object>) CACHE.getValue(inferenceId));
        //将验证结果插入数据库
//        insertInferenceLog(request, inferenceId, startTime, "success");
        // 构造返回结果
        Map<String, Object> data = new HashMap<>();
        data.put(METRICS, validateMetric);
        data.put(PREDICT, predict);
        return data;
    }

    /**
     * 构造验证结果进度数据
     *
     * @param inferenceId 验证唯一id
     * @param percentMap  验证进度
     */
    private void buildPercentEnd(String inferenceId, Map<String, Object> percentMap) {
        Date endTime = new Date();
        percentMap.put(PERCENT, ONE_HUNDRED);
        percentMap.put(PREDICT_INFO, INFERENCE_SUCCESS);
        percentMap.put(END_TIME, endTime.getTime());
        ResourceManager.CACHE.putValue(inferenceId, percentMap);
    }

    /**
     * 验证开始，构造验证结果进度
     *
     * @param inferenceId 验证唯一id
     * @param percentMap  验证进度
     */
    private void buildPercentStart(String inferenceId, Map<String, Object> percentMap) {
        Date startTime = new Date();
        percentMap.put(INFERENCE_ID, inferenceId);
        percentMap.put(START_TIME, startTime.getTime());
        percentMap.put(PERCENT, TEN);
        ResourceManager.CACHE.putValue(inferenceId, percentMap);
    }
    

    /**
     * 验证功能
     *
     * @param query 验证参数
     * @return 验证结果
     * @throws JsonProcessingException
     */
    public Tuple2<String, List<Map<String, Object>>> commonValidate(ValidateRequest query, Map<String, Object> percentMap) throws JsonProcessingException {

        // 查询验证相关信息
        TrainInfo model;
        // 查询客户端缓存
        List<ClientInfo> clientList = query.getClientList().stream().map(PartnerInfoNew::toClientInfo).collect(Collectors.toList());
        if (ConfigUtil.getJdChainAvailable()) {
            JdchainTrainInfo trainInfo = ChainTrainMapper.queryModelById(query.getModel());
            model = new TrainInfo(trainInfo.getModelToken(),AlgorithmType.valueOf(trainInfo.getAlgorithm()),trainInfo.getParameterFieldList(),trainInfo.getMetrics(),
                    trainInfo.getTrainStartTime().getTime(),trainInfo.getTrainEndTime().getTime(),trainInfo.getRunningType(),trainInfo.getPercent());
        } else {
            model = getModelToken(query);
        }
        AlgorithmType supportedAlgorithm = model.getAlgorithmType();

        String[] queryId = query.getUid();
        if (queryId == null || queryId.length == 0) {
            throw new ForbiddenException("queryId 数组不能为空");
        }
        // 数据传输分片
        List<String[]> dataList = PacketUtil.splitInference(queryId, INFERENCE_BATCH_SIZE);
        Map<String, Double> compoundRes = new HashMap<>();
        long s = System.currentTimeMillis();
        // 开始遍历分包数据
        logger.info("PacketUtil.splitInference: " + (System.currentTimeMillis() - s) + " ms");
        for (String[] subIds : dataList) {
            long ss = System.currentTimeMillis();
            logger.info("enter transeruid");
            String subInferenceId = TokenUtil.generateInferenceId(query.getModel());
            // 开始预测
            Map<String, Double> subRes = transferValidUid(subIds, model, supportedAlgorithm, clientList, percentMap, subInferenceId, query.getLabelName());
            compoundRes.putAll(subRes);
            logger.info("transferUid: " + (System.currentTimeMillis() - ss) + " ms ");
        }

        List<Map<String, Object>> res = new ArrayList<>();

        for (String uid : queryId) {
            Map<String, Object> singleResult = new HashMap<>();
            singleResult.put(UID, uid);
            singleResult.put(SCORE, NAN);
            // 有结果
            if (compoundRes.containsKey(uid)) {
                singleResult.put(SCORE, compoundRes.get(uid));
            }
            res.add(singleResult);
        }

        //验证指标计算
        logger.info("testUid size is: " + queryId.length);
        String validateMetric = "";
        List<Map<String, Object>> testRes = res;
        HashMap<String, Object> data = new HashMap<>();
        data.put("testRes", testRes);
        data.put("labelName", query.getLabelName());
        // TODO 增加传入metrictypes
        data.put("metric", query.getMetricType());
        for (ClientInfo clientInfo : clientList) {
            String response = SendAndRecv.send(clientInfo, RequestConstant.VALIDATION_METRIC, data);
            ObjectMapper mapper = new ObjectMapper();
            Map json = mapper.readValue(response, Map.class);
            String metric = (String) json.get("metric");
            logger.info("metric is : " + metric);
            if (!"no_label".equals(metric)) {
                validateMetric = metric;
            }
        }
        logger.info("validateMetric:" + validateMetric);
        return new Tuple2<>(validateMetric, res);
    }

    /**
     * 查询验证结果
     *
     * @param query 验证请求
     * @return modelToken
     */
    private TrainInfo getModelToken(ValidateRequest query) {
        final String modelKey = PRE + query.getModel();
        final Object modelValue = ResourceManager.CACHE.getValue(modelKey);
        // 如果缓存存在
        if (!Objects.isNull(modelValue)) {
            return (TrainInfo) modelValue;
        }
        TrainInfo model = TrainMapper.getTrainInfoByToken(query.getModel());
        if (Objects.isNull(model)) {
            throw new ForbiddenException("验证的model不能为空");
        }
        ResourceManager.CACHE.putValue(modelKey, model);
        return model;
    }
    
    /**
     * @param queryId            请求id
     * @param model              modelToken
     * @param supportedAlgorithm 算法
     * @param clientList         客户端信息列表
     * @param percentMap         验证进度
     * @param inferenceId        验证唯一id
     * @return 验证结果
     * @throws JsonProcessingException
     */
    private Map<String, Double> transferValidUid(String[] queryId, TrainInfo model, AlgorithmType supportedAlgorithm, List<ClientInfo> clientList,
                                                 Map<String, Object> percentMap, String inferenceId, String labelName) throws JsonProcessingException {
        
        Control algorithm = algorithmSelection(model, supportedAlgorithm);
        assert algorithm != null;
        // step1 queryId 去重
        final Set<String> queryIdSet = Arrays.stream(queryId).collect(Collectors.toSet());
        // 获取到去重的uid数组
        final String[] distinctQueryId = queryIdSet.toArray(new String[]{});

        // 开始验证预测
        Map<String, Double> resultMap = doValidate(distinctQueryId, model, supportedAlgorithm, clientList, percentMap, inferenceId, algorithm, labelName);

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
     * @throws JsonProcessingException
     */
    private Control algorithmSelection(TrainInfo model, AlgorithmType supportedAlgorithm) throws JsonProcessingException {
        final List<SingleParameter> finishParameterFields = model.getHyperParameter();
        Map<String, Object> finishParameters = finishParameterFields.stream().collect(Collectors.toMap(SingleParameter::getField, SingleParameter::getValue));
        SuperParameter parameter = CommonParameter.parseParameter(finishParameters, supportedAlgorithm);
        logger.info("parameter : " + parameter);
        return DispatcherFactory.getDispatcher(supportedAlgorithm, parameter);
    }
    

    /**
     * 开始预测
     *
     * @param existUidHasFiltered 需要验证的uid
     * @param model               modelToken
     * @param supportedAlgorithm  算法
     * @param clientList          客户端列表
     * @param percentMap          验证进度
     * @param inferenceId         验证唯一id
     * @param algorithm           算法
     */
    public Map<String, Double> doValidate(String[] existUidHasFiltered, TrainInfo model, AlgorithmType supportedAlgorithm, List<ClientInfo> clientList, Map<String, Object> percentMap, String inferenceId, Control algorithm, String labelName) {
        //正式的验证请求
//        int p = -1;
        // todo secureMode为false
        Map<String,Object> others = new HashMap<>();
        List<CommonRequest> requests = algorithm.initInference(clientList, existUidHasFiltered,others);
        logger.info("requests 's size is : " + requests.size());
        int phase = requests.get(0).getPhase();
        logger.info("phase : " + phase);
        List<CommonResponse> responses = new ArrayList<>();
        while (algorithm.isInferenceContinue()) {
            responses = SendAndRecv.broadcastValidate(requests, model.getModelToken(), supportedAlgorithm, inferenceId, labelName);
            logger.info("response size : " + responses.size());
            requests = algorithm.inferenceControl(responses);
            updateProgress(inferenceId, percentMap);
        }
        double[] result = MathExt.transpose(algorithm.postInferenceControl(responses).getPredicts())[0];
        assert result.length == existUidHasFiltered.length;
        // 构造结果越uid的对应关系
        Map<String, Double> resultMap = new HashMap<>();
        for (int i = 0; i < existUidHasFiltered.length; i++) {
            resultMap.put(existUidHasFiltered[i], result[i]);
        }
        return resultMap;
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
    

    //更新验证进度
    private void updateProgress(String inferenceId, Map<String, Object> percentMap) {
        int percent = Integer.parseInt(String.valueOf(percentMap.get(PERCENT)));//10
        percentMap.put(PERCENT, String.valueOf((percent <= 90) ? (++percent) : percent));
        percentMap.put(PREDICT_INFO, "验证中");
        ResourceManager.CACHE.putValue(inferenceId, percentMap);
        logger.info("验证流水【{}】训练进度=【{}】", inferenceId, ResourceManager.CACHE.getValue(inferenceId));
    }
    
}
