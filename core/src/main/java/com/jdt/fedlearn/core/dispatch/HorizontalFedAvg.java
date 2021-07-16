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

package com.jdt.fedlearn.core.dispatch;

import com.google.protobuf.ByteString;
import com.jdt.fedlearn.core.dispatch.common.Control;
import com.jdt.fedlearn.core.entity.ClientInfo;
import com.jdt.fedlearn.core.entity.base.EmptyMessage;
import com.jdt.fedlearn.core.entity.common.*;
import com.jdt.fedlearn.core.psi.MatchResult;
import com.jdt.fedlearn.core.type.*;
import com.jdt.fedlearn.grpc.federatedlearning.*;

import com.jdt.fedlearn.core.entity.feature.Features;

import com.jdt.fedlearn.core.entity.horizontalZoo.HorizontalZooDataUtils;
import com.jdt.fedlearn.core.entity.horizontalZoo.HorizontalZooMsgStream;
import com.jdt.fedlearn.core.type.HorizontalZooMsgType;
import com.jdt.fedlearn.core.parameter.HorizontalFedAvgPara;
import com.jdt.fedlearn.core.type.AlgorithmType;
import com.jdt.fedlearn.core.type.MetricType;

import com.jdt.fedlearn.core.type.data.Pair;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.stream.Collectors;

public class HorizontalFedAvg implements Control {
    private static final Logger logger = LoggerFactory.getLogger(HorizontalFedAvg.class);

    private static final AlgorithmType algorithmType = AlgorithmType.HorizontalFedAvg;
    private CalculateGrpc.CalculateBlockingStub stub = null;
    private String stubAddress = "";

    public List<ClientInfo> clientInfoList;
    public Features localFeature;
    private int round = 0;
    //private int[] datasetSizes;
    Random random = new Random();
    //TODO 是否需要 modelToken
    private String modelToken = String.valueOf(random.nextInt());

    private HorizontalFedAvgPara parameter;
    private String modelName;
    private int phase = 0;
    private int inferencePhase = -1;

    private Map<MetricType, List<Pair<Integer, Double>>> metricMap = new HashMap<>();

    //inference
    private String[] inferenceDataUid;
    private Map<String, Integer> mapInferenceOrder = new HashMap<>();
    private boolean inferStop = false;

    public HorizontalFedAvg(HorizontalFedAvgPara parameter,
                            String modelName) {
        this.parameter = parameter;
        this.modelName = modelName;
    }

    public HorizontalFedAvg(HorizontalFedAvgPara parameter) {
        this.parameter = parameter;
        modelName = parameter.getModelName();
    }


    @Override
    public List<CommonRequest> initControl(List<ClientInfo> clientInfos,
                                           MatchResult idMap,
                                           Map<ClientInfo, Features> features,
                                           Map<String, Object> other) {
        logger.info("clientInfos: (" + clientInfos.size() + ")");
        logger.info("Init control");
        this.clientInfoList = clientInfos;

        List<CommonRequest> res = new ArrayList<>();
        for (ClientInfo clientInfo : clientInfos) {
            Features localFeature = features.get(clientInfo);
            // TrainInit(SuperParameter parameter, Features featureList, int[] testIndex, String matchId, Map<String, Object> others) {
            TrainInit nested_req = new TrainInit(parameter, localFeature, idMap.getMatchId(), other);
            CommonRequest request = CommonRequest.buildTrainInitial(clientInfo, nested_req);
            res.add(request);
        }
        return res;
    }


    @Override
    public List<CommonRequest> control(List<CommonResponse> responses) {
        phase = getNextPhase(phase, responses);
        String trainId = 1 + getAlgorithmType().toString();
        switch (HorDispatchPhaseType.valueOf(phase)) {
            case createNullRequest:
                logger.info("HorizontalFedAvg train init...");
                return createNullRequest(responses, trainId);
            case transferModels:
                byte[] gobalModelString = initModel();
                return transferModels(gobalModelString, HorizontalZooMsgType.TransferGlobalModelParaAndInit);

            default:
                if (round <= parameter.getNumRound()) {
                    return updateGlobalWeights(trainId, responses);
                } else {
                    logger.info("HorizontalFedAvg train finished...");
                    return createNullRequest(responses, trainId);
                }
        }
    }


    private byte[] initModel() {
        // 设置 metricMap
        //round = this.parameter.getNumRound();
        for (MetricType metricType : parameter.getEval_metric()) {
            if (metricMap.containsKey(metricType)) {
                metricMap.get(metricType).add(new Pair<>(round, -1.));
            } else {
                List<Pair<Integer, Double>> tmpRoundMetric = new ArrayList<>();
                tmpRoundMetric.add(new Pair<>(round, -1.));
                metricMap.put(metricType, tmpRoundMetric);
            }
        }
        printMetricMap();

        // master 方建立 stubby server
        if ("".equals(stubAddress)) {
            stubAddress = "127.0.0.1:8891"; //8891
        }

        //master 建立 stubby server
        logger.info("master setup channel with grpc server(" + this.stubAddress + ")");
        ManagedChannel channel = ManagedChannelBuilder.forTarget(this.stubAddress)
                .usePlaintext().maxInboundMessageSize(Integer.MAX_VALUE).build();
        this.stub = CalculateGrpc.newBlockingStub(channel);

        //创建global model实例： 发送命令给grpc
        logger.info("[Master=>GRPC]: GlobalModelInit, (" + this.stubAddress + ")初始化全局模型. ");
        String modelHyperPara = this.parameter.toJson();
        String cmdName = HorizontalZooMsgType.GlobalModelInit.getMsgType();
        HFLModelMessage req = HorizontalZooDataUtils.prepareHFLModelMessage(
                this.modelName,
                "", //modelPara.toJson(),
                modelHyperPara,
                cmdName);
        HFLModelMessage res = stub.hFLModelHandler(req);
        String resCommandMsg = HorizontalZooDataUtils.parseCommandMsg(res);
        logger.info("[GRPC=>Master]: " + resCommandMsg + ", (" + this.stubAddress + ")发起水平联邦训练");
        ByteString modelString = HorizontalZooDataUtils.parseModelString(res);
        return modelString.toByteArray();
    }

    private List<CommonRequest> transferModels(byte[] gobalModelString, HorizontalZooMsgType cmdMsg) {
        //Master透传全局模型给客户端
        List<CommonRequest> res = new ArrayList<>();

        for (ClientInfo client : this.clientInfoList) {
            logger.info("[Master=>Client]: TransferGlobalModelPara, 全局模型参数透传至(" + client.getIp() + ":" + client.getPort() + ").");
            HorizontalZooMsgStream req = new HorizontalZooMsgStream(modelToken, client, cmdMsg, parameter,
                    this.modelName,
                    gobalModelString);

            CommonRequest commonRequest = new CommonRequest(client, EmptyMessage.message(), phase);
            commonRequest.setBody(req);
            res.add(commonRequest);
        }
        return res;
    }

    private byte[] aggregateModel(Map<ClientInfo, byte[]> models, Map<ClientInfo, Integer> datasetSizes) {
        //logger.info("集成更新globalModel, 轮数加1");

        //给grpc发送所有的模型
        String cmdName = HorizontalZooMsgType.UpdateLocalModelPara.getMsgType();
        for (Map.Entry<ClientInfo, byte[]> client : models.entrySet()) {
            byte[] model = client.getValue();
            int datasetSize = datasetSizes.get(client.getKey());
            logger.info("[Master=>GRPC]: UpdateLocalModelPara, 更新(" + client.getKey().getIp() + ":" + client.getKey().getPort() + ")的本地模型.");
            this.parameter.setDatasetSize(datasetSize);
            HFLModelMessage req = HorizontalZooDataUtils.prepareHFLModelMessage(this.modelName,
                    this.parameter.toJson(),
                    "", //modelPara.toJson(),
                    cmdName,
                    client.getKey().getIp() + ":" + client.getKey().getPort(),
                    ByteString.copyFrom(model));
            HFLModelMessage res = stub.hFLModelHandler(req);
            String commandMsg = HorizontalZooDataUtils.parseCommandMsg(res);
            logger.info("[GRPC=>Master]: " + commandMsg + ", (" + client.getKey().getIp() + ":" + client.getKey().getPort() + ")本地模型更新完成.");
        }

        //开始aggregation
        logger.info("[Master=>GRPC]: AggregateModelPara, 集成(" + this.stubAddress + ")的全局模型.");
        HFLModelMessage req = HorizontalZooDataUtils.prepareHFLModelMessage(this.modelName,
                this.parameter.toJson(),
                "", //modelPara.toJson(),
                HorizontalZooMsgType.AggregateModelPara.getMsgType());
        HFLModelMessage res = stub.hFLModelHandler(req);
        String commandMsg = HorizontalZooDataUtils.parseCommandMsg(res);
        logger.info("[GRPC=>Master]: " + commandMsg + ", (" + this.stubAddress + ")本地模型更新完成.");
        ByteString modelString = HorizontalZooDataUtils.parseModelString(res);
        return modelString.toByteArray();
    }

    private List<CommonRequest> updateGlobalWeights(String trainId, List<CommonResponse> responses) {
        logger.debug("收到localModel后，集成更新globalModel");
        Map<ClientInfo, byte[]> modelMap = new HashMap<>();
        Map<ClientInfo, Integer> datasetMap = new HashMap<>();
        double gMetricsSum = 0.0;
        double lMetricsSum = 0.0;
        for (CommonResponse response_i : responses) {
            ClientInfo client = response_i.getClient();
            HorizontalZooMsgStream res_i = (HorizontalZooMsgStream) response_i.getBody();
            byte[] model = res_i.getModelString();
            int datasetSize = res_i.getDatasetSize();
            modelMap.put(client, model);
            datasetMap.put(client, datasetSize);

            double gMetric = res_i.getGMetric();
            double lMetric = res_i.getLMetric();
            gMetricsSum += gMetric;
            lMetricsSum += lMetric;

            logger.info("gMetric=" + gMetric);
            logger.info("lMetric=" + lMetric);
        }

        for (MetricType metricType : parameter.getEval_metric()) {
            if (metricMap.containsKey(metricType)) {
                //metricMap.get(metricType).set(metricMap.get(metricType).size()-1, new Pair<>(round, loss.get(metricType.getMetric())));
                metricMap.get(metricType).set(metricMap.get(metricType).size() - 1, new Pair<>(round, gMetricsSum / responses.size()));
            }
        }
        printMetricMap();

        //轮数加1
        round++;

        if (round <= parameter.getNumRound()) {
            //集成模型
            byte[] gobalModelString = aggregateModel(modelMap, datasetMap);

            if (round == parameter.getNumRound()) {
                return transferModels(gobalModelString, HorizontalZooMsgType.TransferGlobalModelParaAndEnd);
            } else {
                return transferModels(gobalModelString, HorizontalZooMsgType.TransferGlobalModelPara);
            }
        } else {
            return createNullRequest(responses, trainId);
        }

    }

    private List<CommonRequest> createNullRequest(List<CommonResponse> response, String trainId) {
        List<CommonRequest> req = new ArrayList<>();
        for (CommonResponse res_i : response) {
            CommonRequest req_i = new CommonRequest(res_i.getClient(), null, 1);
            req.add(req_i);
        }
        return req;
    }

    private void printMetricMap() {
        String mapAsString = metricMap.keySet().stream()
                .map(key -> key + "=" + metricMap.get(key))
                .collect(Collectors.joining(", ", "{", "}"));
        logger.info("metricMap: " + mapAsString);
    }

    @Override
    public boolean isContinue() {
        logger.info("round = " + round);
        if (round == parameter.getNumRound() + 1) {
            logger.info("Master stop, due to round = " + round);
            return false;
        }
        return true;
    }

    @Override
    public List<CommonRequest> initInference(List<ClientInfo> clientInfos, String[] predictUid) {
        inferenceDataUid = predictUid;
        for (int i = 0; i < inferenceDataUid.length; i++) {
            mapInferenceOrder.put(inferenceDataUid[i], i);
        }

        clientInfoList = clientInfos;
        List<CommonRequest> res = new ArrayList<>();
        for (ClientInfo client : clientInfoList) {
            logger.info("[Master=>Client]: TransferInferenceInfo, 推理信息传至(" + client.getIp() + ":" + client.getPort() + ").");

            InferenceInit init = new InferenceInit(predictUid);
            CommonRequest request = CommonRequest.buildInferenceInitial(client, init);
            res.add(request);
        }
        return res;
    }

    private List<CommonRequest> predict(String trainId, List<CommonResponse> responses) {
        List<CommonRequest> reqs = new ArrayList<>();
        for (CommonResponse resi : responses) {
            ClientInfo client = resi.getClient();
            logger.info("[Master=>Client]: TransferPredictInfo, 透传推理信息至(" + client.getIp() + ":" + client.getPort() + ").");
            HorizontalZooMsgStream req = new HorizontalZooMsgStream(
                    modelToken, client,
                    HorizontalZooMsgType.TransferInferenceInfo,
                    this.modelName,
                    inferenceDataUid);

            CommonRequest request = new CommonRequest(client, EmptyMessage.message());
            request.setBody(req);
            reqs.add(request);
        }
        return reqs;
    }

    private List<CommonRequest> getPredictResults(String trainId, List<CommonResponse> responses) {
        List<CommonRequest> reqs = new ArrayList<>();
        for (CommonResponse resi : responses) {
            ClientInfo client = resi.getClient();
            logger.info("[Master]: 解析推理结果(" + client.getIp() + ":" + client.getPort() + ").");
            HorizontalZooMsgStream res_i = (HorizontalZooMsgStream) resi.getBody();
//            double gMetric = res_i.getGMetric();
            double lMetric = res_i.getLMetric();
            //logger.info("gMetric="+gMetric);
            logger.info("解析推理结果指标 PredictResults=" + lMetric);

            HorizontalZooMsgStream req = new HorizontalZooMsgStream(
                    modelToken, client,
                    HorizontalZooMsgType.TransferInferenceInfo,
                    this.modelName,
                    inferenceDataUid);
            CommonRequest request = new CommonRequest(client, EmptyMessage.message());
            request.setBody(req);
            reqs.add(request);
        }
        return reqs;
    }

    @Override
    public List<CommonRequest> inferenceControl(List<CommonResponse> responses) {
        logger.info("Master: inferenceControl, " + ", phase=" + inferencePhase);
        inferencePhase = getNextPhase(inferencePhase, responses);
        String trainId = 1 + "_" + getAlgorithmType().getAlgorithm(); //TODO
        switch (HorDispatchPhaseType.valueOf(phase)) {
            case predict:
                return predict(trainId, responses);
            case getPredictResults:
                return getPredictResults(trainId, responses);
            default:
                throw new UnsupportedOperationException();
        }
    }


    @Override
    public PredictRes postInferenceControl(List<CommonResponse> responses) {
        logger.info("Post Inference Control...");
        double[] res = new double[inferenceDataUid.length];
        for (int i = 0; i < res.length; i++) {
            // use inferenceDataUid get correct order
            //res[i] = pred.get(i);
            //res[i] = pred.get(mapInferenceOrder.get(inferenceDataUid[i]));
            res[i] = mapInferenceOrder.get(inferenceDataUid[i]);
            //res[i] = 0.6;
        }
        return new PredictRes(new String[]{"label"}, res);
    }

    @Override
    public boolean isInferenceContinue() {
        logger.info(String.format("inferStop: %s", inferStop));
        return !inferStop;
    }

    public int getNextPhase(int old, List<CommonResponse> responses) {
        if (old < 0) {
            //inference 阶段
            if (old == -1) {
                return -2;
            } else if (old == -2) {
                inferStop = true;
                return -3;
            } else {
                inferStop = true;
                return -3;
            }
        } else {
            return old + 1;
        }
    }

    @Override
    public MetricValue readMetrics() {
        return new MetricValue(metricMap);
    }

    public AlgorithmType getAlgorithmType() {
        return algorithmType;
    }

}
