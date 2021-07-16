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

package com.jdt.fedlearn.core.model;

import com.google.protobuf.ByteString;
import com.jdt.fedlearn.core.entity.ClientInfo;
import com.jdt.fedlearn.core.entity.base.EmptyMessage;
import com.jdt.fedlearn.core.entity.Message;
import com.jdt.fedlearn.core.entity.base.SingleElement;
import com.jdt.fedlearn.core.entity.feature.Features;
import com.jdt.fedlearn.core.entity.horizontalZoo.HorizontalZooMsgStream;
import com.jdt.fedlearn.core.psi.MappingResult;
import com.jdt.fedlearn.core.type.HorizontalZooMsgType;
import com.jdt.fedlearn.core.entity.horizontalZoo.HorizontalZooDataUtils;

import com.jdt.fedlearn.core.preprocess.InferenceFilter;
import com.jdt.fedlearn.core.type.AlgorithmType;
import com.jdt.fedlearn.grpc.federatedlearning.*;
import com.jdt.fedlearn.grpc.federatedlearning.Vector;
import com.jdt.fedlearn.core.loader.randomForest.RFInferenceData;

import com.jdt.fedlearn.core.loader.common.InferenceData;
import com.jdt.fedlearn.core.loader.common.TrainData;
import com.jdt.fedlearn.core.loader.horizontalZoo.HorizontalDataFrame;

import com.jdt.fedlearn.core.loader.randomForest.DataFrame;
import com.jdt.fedlearn.core.parameter.SuperParameter;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import org.ejml.simple.SimpleMatrix;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

import com.jdt.fedlearn.core.parameter.HorizontalFedAvgPara;
import com.jdt.fedlearn.core.type.*;

public class HorizontalFedAvgModel implements Model {
    private static final Logger logger = LoggerFactory.getLogger(HorizontalFedAvgModel.class);

    private CalculateGrpc.CalculateBlockingStub stub = null;

    private boolean isInitTrain = false;
    private boolean isInitInfer = false;

    private String modelToken;

    private Matrix X_train;
    private Vector y_train;
    private Matrix X_test;
    private Vector y_test;
    private int datasetSize;
    private double[][] y_preds;
    double[] y_pred_mean, y_label;
    PaillierVector y_pvec;

    private ArrayList<Integer>[] featureIds;

    private String stubAddress = "";
    private String modelName = "";
    private HorizontalFedAvgPara parameter = new HorizontalFedAvgPara();

    // inference uid
    int[] inferenceUid;

    public HorizontalFedAvgModel() {
    }

    public HorizontalFedAvgModel(String stubAddress) {
        this.stubAddress = stubAddress;
    }

    public HorizontalFedAvgModel(String stubAddress, String modelName) {
        this.stubAddress = stubAddress;
        this.modelName = modelName;
    }

    @Override
    public TrainData trainInit(String[][] rawData, String[] uids, int[] testIndex,
                               SuperParameter superParameter,
                               Features features, Map<String, Object> others) {
        HorizontalDataFrame trainData = new HorizontalDataFrame(rawData);
        if ("".equals(stubAddress)) {
            stubAddress = "127.0.0.1:8891";//8891
        }
        logger.debug("Init train");
        //client 建立 stubby server
        ManagedChannel channel = ManagedChannelBuilder.forTarget(this.stubAddress)
                .usePlaintext().maxInboundMessageSize(Integer.MAX_VALUE).build();
        this.stub = CalculateGrpc.newBlockingStub(channel);

        logger.info("trainInit: labelName=" + trainData.labelName + ", (" + this.stubAddress + ")");

        if (trainData.labelName != null) {
            trainData.init(trainData.labelName);
        } else {
            trainData.init();
        }

        trainData.fillna(0);

        X_train = HorizontalZooDataUtils.toMatrix(trainData.X);
        y_train = HorizontalZooDataUtils.toVector(trainData.y);

        this.datasetSize = X_train.getRowsCount();

        DataFrame trainData2 = new DataFrame(rawData, uids, features);

        return trainData2;
    }

    /**
     * @param phase        阶段
     * @param jsonData     训练迭代参数
     * @param trainDataOld 训练数据，由每个用户保有
     * @return 序列化后的训练中间状态
     */

    @Override
    public Message train(int phase, Message jsonData, TrainData trainDataOld) {
        logger.info("train:" + " phase" + phase + ".");

        if (HorModelPhaseType.valueOf(phase) == HorModelPhaseType.Null_1) {
            return null;
        } else if (HorModelPhaseType.valueOf(phase) == HorModelPhaseType.loadTrainUpdate) {
            return loadTrainUpdate(
                    (DataFrame) trainDataOld,
                    jsonData,
                    HorizontalZooMsgType.SynGlobalModelParaAndInit.getMsgType(),
                    HorizontalZooMsgType.TransferLocalModelPara);
        } else if (HorModelPhaseType.valueOf(phase) == HorModelPhaseType.loadTrainUpdate_1) {
            HorizontalZooMsgStream req = (HorizontalZooMsgStream) jsonData;
            if (req.getMsgType() == HorizontalZooMsgType.TransferGlobalModelPara) {
                return loadTrainUpdate((DataFrame) trainDataOld, jsonData,
                        HorizontalZooMsgType.SynGlobalModelPara.getMsgType(),
                        HorizontalZooMsgType.TransferLocalModelPara);
            } else if (req.getMsgType() == HorizontalZooMsgType.TransferGlobalModelParaAndEnd) {
                return loadTrainUpdate((DataFrame) trainDataOld, jsonData,
                        HorizontalZooMsgType.SynGlobalModelParaAndEnd.getMsgType(),
                        HorizontalZooMsgType.SaveGlobalModel2LocalFinish);
            }
        } else {
            return null;
        }
        return null;
    }

    private Message loadTrainUpdate(DataFrame trainData, Message jsonData,
                                    String cmdMsg, HorizontalZooMsgType toMasterCmdMsg) {
        logger.debug("Init train in clients");
        HorizontalZooMsgStream req = (HorizontalZooMsgStream) jsonData;

        //String reqCommandMsg = HorizontalZooMsgType.SynGlobalModelPara.getMsgType();
        logger.info("[Client=>GRPC]: " + cmdMsg + ", (" + this.stubAddress + ")同步全局模型参数并进行本地模型训练.");
        ByteString globalModel = ByteString.copyFrom(req.getModelString());
        HFLModelMessage requestToGrpc = HorizontalZooDataUtils.prepareHFLModelMessage(
                req.getModelName(),
                req.getParameter().toJson(), //FedAvg parameters
                "",
                cmdMsg,
                this.stubAddress,
                globalModel,
                X_train,
                y_train,
                modelToken);
        HFLModelMessage responseOfGrpc = stub.hFLModelHandler(requestToGrpc);
        String resCommandMsg = HorizontalZooDataUtils.parseCommandMsg(responseOfGrpc);
        logger.info("[GRPC=>Client]: " + resCommandMsg + ", (" + this.stubAddress + ")更新本地模型.");
        ByteString modelString = HorizontalZooDataUtils.parseModelString(responseOfGrpc);
        logger.info("[GRPC=>Client](" + this.stubAddress + "): globalMetric=" + HorizontalZooDataUtils.parseGlobalModelMetric(responseOfGrpc));
        logger.info("[GRPC=>Client](" + this.stubAddress + "): LocalMetric=" + HorizontalZooDataUtils.parseLocalModelMetric(responseOfGrpc));

        //client透传消息给master
        ClientInfo client = req.getClient();
        logger.info("[Client=>Master]: " + toMasterCmdMsg.getMsgType() + ", (" + client.getIp() + ":" + client.getPort() + ")透传本地模型参数.");
        HorizontalZooMsgStream res = new HorizontalZooMsgStream(req.getModelToken(), req.getClient(), toMasterCmdMsg, req.getParameter(),
                req.getModelName(),
                modelString.toByteArray(),
                this.datasetSize,
                HorizontalZooDataUtils.parseGlobalModelMetric(responseOfGrpc),
                HorizontalZooDataUtils.parseLocalModelMetric(responseOfGrpc));
        return res;
    }

    // 推理初始化，
    public Message inferenceInit(String[] uidList, String[][] inferenceCacheFile, Map<String, Object> others) {
        return InferenceFilter.filter(uidList, inferenceCacheFile);
    }

    @Override
    public Message inference(int phase, Message jsonData, InferenceData data) {
        logger.info(String.format("Inference process, phase=%s", phase));
        logger.info(String.format("Inference process, jsonData=%s", jsonData));
        RFInferenceData inferenceData = (RFInferenceData) data;
        if (HorModelPhaseType.valueOf(phase) == HorModelPhaseType.inferenceInit) {
            inferenceInit(inferenceData, jsonData);
                /*return predict(data, jsonData,
                        HorizontalZooMsgType.PredictInClient.getMsgType(),
                        HorizontalZooMsgType.PredictInClientFinish);*/
            return new SingleElement(Arrays.toString(inferenceUid));
        } else if (HorModelPhaseType.valueOf(phase) == HorModelPhaseType.predictByUidList) {
            //            HorizontalZooMsgStream req = new HorizontalZooMsgStream(jsonData);
            //inferenceInit(inferenceData, jsonData);
            return predictByUidList(data, jsonData,
                    HorizontalZooMsgType.PredictInClient.getMsgType(),
                    HorizontalZooMsgType.PredictInClientFinish);
        } else {
            return new EmptyMessage();
        }
    }


    private void inferenceInit(RFInferenceData inferenceData, Message jsonData) {
        //client 建立 stubby server
        if ("".equals(stubAddress)) {
            stubAddress = "127.0.0.1:8891";//8891
        }
        logger.info("inferenceInit: (" + this.stubAddress + "), setup stubby server.");
        ManagedChannel channel = ManagedChannelBuilder.forTarget(this.stubAddress)
                .usePlaintext().maxInboundMessageSize(Integer.MAX_VALUE).build();
        this.stub = CalculateGrpc.newBlockingStub(channel);

        //初始化模型变量
        modelToken = "";//TODO
        //String[] pred_uids = req.getUid();
        //String[][] rawTable = inferenceData.getUidFeature(pred_uids);
        String[][] rawTable = inferenceData.getUidFeature();
        logger.info("Init inference...");

        HorizontalDataFrame df = new HorizontalDataFrame(rawTable);
        df.initAll();
        df.fillna(0);
        SimpleMatrix X_test_matrix = df.toSmpMatrix(0, df.numRows(), 1, df.numCows() - 1);
        SimpleMatrix y_test_matrix = df.toSmpMatrix(0, df.numRows(), df.numCows() - 1, df.numCows());

        X_test = HorizontalZooDataUtils.toMatrix(X_test_matrix);
        y_test = HorizontalZooDataUtils.toVector(y_test_matrix);

        //logger.info("Get inference data: " + Arrays.toString(inferenceUid));
    }

    private Message predictByUidList(InferenceData trainData, Message jsonData,
                                     String cmdMsg, HorizontalZooMsgType toMasterCmdMsg) {
        logger.debug("predictByUidList in clients");
        HorizontalZooMsgStream req = (HorizontalZooMsgStream) jsonData;
        modelName = req.getModelName();

        logger.info("[Client=>GRPC]: " + cmdMsg + ", (" + this.stubAddress + ")使用本地模型预测列表中Uid.");
        HFLModelMessage requestToGrpc = HorizontalZooDataUtils.prepareHFLModelMessage(
                req.getModelName(),
                "", //FedAvg parameters
                "",
                cmdMsg,
                stubAddress,
                ByteString.EMPTY,
                X_test,
                y_test,
                modelToken); //TODO assume the labels are known.
        HFLModelMessage responseOfGrpc = stub.hFLModelHandler(requestToGrpc);
        String resCommandMsg = HorizontalZooDataUtils.parseCommandMsg(responseOfGrpc);
        logger.info("[GRPC=>Client]: " + resCommandMsg + ", (" + this.stubAddress + ")本地模型预测结束.");
        ByteString modelString = HorizontalZooDataUtils.parseModelString(responseOfGrpc);
        logger.info("[GRPC=>Client](" + this.stubAddress + "): globalMetric=" + HorizontalZooDataUtils.parseGlobalModelMetric(responseOfGrpc));
        logger.info("[GRPC=>Client](" + this.stubAddress + "): LocalMetric=" + HorizontalZooDataUtils.parseLocalModelMetric(responseOfGrpc));

        //client透传消息给master
        ClientInfo client = req.getClient();
        logger.info("[Client=>Master]: " + toMasterCmdMsg.getMsgType() + ", (" + client.getIp() + ":" + client.getPort() + ")透传推理结果.");
        HorizontalZooMsgStream res = new HorizontalZooMsgStream(req.getModelToken(), req.getClient(), toMasterCmdMsg, req.getParameter(),
                req.getModelName(),
                modelString.toByteArray(),
                0,
                HorizontalZooDataUtils.parseGlobalModelMetric(responseOfGrpc),
                HorizontalZooDataUtils.parseLocalModelMetric(responseOfGrpc));
        return res;
    }

    private void predict(InferenceData trainData, String jsonData,
                         String cmdMsg, HorizontalZooMsgType toMasterCmdMsg) {
        logger.debug("Predict in clients");
        /*HorizontalZooMsgStream req = new HorizontalZooMsgStream(jsonData);
        modelName = req.getModelName();*/

        logger.info("[Client=>GRPC]: " + cmdMsg + ", (" + this.stubAddress + ")使用本地模型预测所有来自文件的Uid.");
        HFLModelMessage requestToGrpc = HorizontalZooDataUtils.prepareHFLModelMessage(
                modelName,
                "", //FedAvg parameters
                "",
                cmdMsg,
                stubAddress,
                ByteString.EMPTY,
                X_test,
                y_test,
                modelToken); //TODO assume the labels are known.
        HFLModelMessage responseOfGrpc = stub.hFLModelHandler(requestToGrpc);
        String resCommandMsg = HorizontalZooDataUtils.parseCommandMsg(responseOfGrpc);
        logger.info("[GRPC=>Client]: " + resCommandMsg + ", (" + this.stubAddress + ")本地模型预测结束.");
//        ByteString modelString = HorizontalZooDataUtils.parseModelString(responseOfGrpc);
        logger.info("[GRPC=>Client](" + this.stubAddress + "): globalMetric=" + HorizontalZooDataUtils.parseGlobalModelMetric(responseOfGrpc));
        logger.info("[GRPC=>Client](" + this.stubAddress + "): LocalMetric=" + HorizontalZooDataUtils.parseLocalModelMetric(responseOfGrpc));

    }

    @Override
    public void deserialize(String input) {
        logger.info("Model deserialize...");
    }

    @Override
    public String serialize() {
        logger.info("Model serialize...");
        return "";
        //return ""getModel().toString()"";
    }

    private Map<String, String> getModel() {
        Map<String, String> strNN = new HashMap<>();
        return strNN;
    }

    public void setModelName(String modelName) {
        this.modelName = modelName;
    }

    public AlgorithmType getModelType() {
        return AlgorithmType.HorizontalFedAvg;
    }
}
