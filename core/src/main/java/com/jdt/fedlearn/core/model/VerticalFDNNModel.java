package com.jdt.fedlearn.core.model;

import com.google.protobuf.ByteString;
import com.jdt.fedlearn.core.entity.Message;
import com.jdt.fedlearn.core.entity.common.InferenceInit;
import com.jdt.fedlearn.core.entity.feature.Features;
import com.jdt.fedlearn.core.entity.randomForest.DataUtils;
import com.jdt.fedlearn.core.entity.verticalFDNN.VFDNNInferenceData;
import com.jdt.fedlearn.core.entity.verticalFDNN.VFDNNMessage;
import com.jdt.fedlearn.core.entity.verticalFDNN.VerticalFDNNUtils;
import com.jdt.fedlearn.core.loader.common.InferenceData;
import com.jdt.fedlearn.core.loader.common.TrainData;
import com.jdt.fedlearn.core.loader.randomForest.RFTrainData;
import com.jdt.fedlearn.core.parameter.HyperParameter;
import com.jdt.fedlearn.core.parameter.VerticalFDNNParameter;
import com.jdt.fedlearn.core.preprocess.InferenceFilter;
import com.jdt.fedlearn.core.type.AlgorithmType;
import com.jdt.fedlearn.grpc.federatedlearning.CalculateGrpc;
import com.jdt.fedlearn.grpc.federatedlearning.Matrix;
import com.jdt.fedlearn.grpc.federatedlearning.Vector;
import com.jdt.fedlearn.grpc.federatedlearning.VerticalFDNNMessage;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import org.ejml.simple.SimpleMatrix;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Collectors;

import com.jdt.fedlearn.core.type.*;

public class VerticalFDNNModel implements Model {
    // log
    private static final Logger logger = LoggerFactory.getLogger(VerticalFDNNModel.class);
    // 模型 token
    private String modelToken;
    // stubby server config
    private CalculateGrpc.CalculateBlockingStub stub = null;
    private String stubAddress = "";
    // active or passive
    private boolean isActive;
    // vertical-FDNN parameter
    private VerticalFDNNParameter parameter;
    // 损失函数
    // TODO: add loss
    private String loss;
    private Matrix XTrain;
    private Vector yTrain;

    // inference
    private String[] inferenceUid;
    private SimpleMatrix XTest;

    // 加这个 splitLine 完全是因为log太多不加看不清……
    private final String splitLine = "========================================================";

    // constructor
    public VerticalFDNNModel() {
    }

    public VerticalFDNNModel(String stubAddress) {
        this.stubAddress = stubAddress;
    }

    @Override
    public TrainData trainInit(String[][] rawData, String[] uids, int[] testIndex, HyperParameter parameter, Features features, Map<String, Object> others) {
        modelToken = "_MLP";
        RFTrainData trainData = new RFTrainData(rawData, uids, features, false);
        logger.info("Init train start");
        this.parameter = (VerticalFDNNParameter) parameter;
        // 初始化 train data
        logger.info("HasLabel: " + trainData.hasLabel);
        trainData.fillna(0);
        logger.info(String.format("Dataframe: %s rows, %s columns", trainData.numRows(), trainData.numCols()));
        SimpleMatrix X = trainData.toSmpMatrix(0, trainData.numRows(), 0, trainData.numCols());
        XTrain = DataUtils.toMatrix(X);
        // 如果有label，加载Y，作为active方
        if (trainData.hasLabel) {
            isActive = true;
            // TODO: add loss
            loss = null;
            double[] yLabel = trainData.getLabel();
            Vector.Builder vectorOrBuilder = Vector.newBuilder();
            for (int j = 0; j < yLabel.length; j++) {
                vectorOrBuilder.addValues(yLabel[j]);
            }
            yTrain = vectorOrBuilder.build();
        } else {
            isActive = false;
        }
        createStubServer();
        logger.info("Init train end");
        return trainData;
    }

    @Override
    public Message train(int phase, Message request, TrainData trainData) {
        logger.info(String.format("Training process, phase %s start", phase) + splitLine);
        Message response = null;
        //        DataFrame data = (DataFrame) trainData;
        // 根据 phase 编号处理对应phase
        switch (VerFDNNModelPhaseType.valueOf(phase)) {
            case trainPhase0Passive:
                // Phase 0: passive party 初始化
                if (!isActive) {
                    return trainPhase0Passive(request, trainData);
                } else {
                    return makeNullMessage();
                }
            case trainPhase0Passive1:
                // Phase 1: active party 初始化
                if (isActive) {
                    return trainPhase0Active(request, trainData);
                } else {
                    return makeNullMessage();
                }
            case trainPhase2:
                return trainPhase2(request, trainData);
            case trainPhase3:
                return trainPhase3(request, trainData);
            case trainPhase4:
                return trainPhase4(request, trainData);
            case trainPhase99:
                return trainPhase4(request, trainData);
            default:
                throw new UnsupportedOperationException();
        }
    }


    @Override
    public Message inferenceInit(String[] uid, String[][] inferenceData, Map<String, Object> others) {
        return InferenceFilter.filter(uid, inferenceData);
    }


    public Message inferenceInit(VFDNNInferenceData inferenceData, Message request) {
        logger.info("Init inference...");
        // TODO 确定 inference 到底带不带 header
        inferenceData.init();
        inferenceData.fillna(0);
        // 接受 jsonData 里的 InferenceInit 中的 uid
        InferenceInit init = (InferenceInit) request;
        String[] subUid = init.getUid();
        XTest = inferenceData.selectToSmpMatrix(subUid);
        // get uid
        inferenceUid = init.getUid();
        logger.info("InferenceUid: " + Arrays.toString(inferenceUid));
        createStubServer();
        return new VFDNNMessage(modelToken,
                new ArrayList<>(),
                new ArrayList<>(),
                isActive);
    }

    @Override
    public Message inference(int phase, Message masterRequest, InferenceData data) {
        logger.info(String.format("Inference process, phase %s start", phase) + splitLine);
        VFDNNInferenceData inferenceData = (VFDNNInferenceData) data;
        switch (VerFDNNModelPhaseType.valueOf(phase)) {
            case inferenceInit:
                return inferenceInit(inferenceData, masterRequest);
            case inferenceInit1:
                return inferencePhase1((VFDNNMessage) masterRequest, inferenceData);
            case inferenceInit2:
                return inferencePhase2((VFDNNMessage) masterRequest, inferenceData);
            default:
                return null;
        }

    }

    @Override
    public String serialize() {
        Map<String, String> map = new HashMap<>();
        map.put("modelToken", modelToken);
        if (isActive) {
            map.put("isActive", "1");
        } else {
            map.put("isActive", "0");
        }
        return DataUtils.mapToString(map);
    }

    @Override
    public void deserialize(String modelContent) {
        Map<String, String> map = DataUtils.stringToMap(modelContent, true, true);
        isActive = "1".equals(map.get("isActive"));
        modelToken = map.get("modelToken");
    }

    private Message trainPhase0Passive(Message parameterData, TrainData trainData) {
        // Phase 0 init
        List<ByteString> modelBytes = new ArrayList<>();
        List<Double> modelParameters = new ArrayList<>();
        // convert train data to bytes and mockSend to grpc
        List<List<Double>> X = DataUtils.MatrixToList(XTrain);
//            modelBytes.add(ByteString.copyFrom(X.toString(), "unicode"));
        modelBytes.add(ByteString.copyFrom(X.toString().getBytes(StandardCharsets.UTF_8)));
        modelParameters.add((double) parameter.getBatchSize());
        modelParameters.add((double) parameter.getNumEpochs());
        modelParameters.add(parameter.getIsTest() ? 1. : 0.);
        VerticalFDNNMessage message = VerticalFDNNUtils.prepareInputMessage(modelToken,
                modelBytes, modelParameters);
        VerticalFDNNMessage message1 = stub.verticalFDNNTrainPhase0Passive(message);
        // new ArrayList: because list in protobuf are lazy list and cannot be serialized
        VFDNNMessage response = new VFDNNMessage(modelToken,
                new ArrayList<>(message1.getModelParametersList()),
                new ArrayList<>(message1.getModelBytesList()),
                isActive);
        logger.info("DEBUG: passive phase 0");
        logger.info(response.toString());
        return response;
    }

    private Message trainPhase0Active(Message masterMessage, TrainData trainData) {
        // Phase 0 init
        VFDNNMessage request = (VFDNNMessage) masterMessage;
        List<ByteString> modelBytes = request.getModelBytes();
        for (ByteString si : modelBytes) {
            logger.info(si.toString());
        }
        List<List<Double>> X = DataUtils.MatrixToList(XTrain);
        List<Double> y = yTrain.getValuesList();
        modelBytes.add(ByteString.copyFrom(X.toString().getBytes(StandardCharsets.UTF_8)));
        modelBytes.add(ByteString.copyFrom(y.toString().getBytes(StandardCharsets.UTF_8)));
        VerticalFDNNMessage message = VerticalFDNNUtils.prepareInputMessage(request.getModelToken(),
                modelBytes, request.getModelParameters());
        VerticalFDNNMessage resMessage = stub.verticalFDNNTrainPhase0Active(message);

        VFDNNMessage response = new VFDNNMessage(modelToken,
                new ArrayList<>(resMessage.getModelParametersList()),
                resMessage.getModelBytesList(),
                isActive);
        logger.info("DEBUG: active phase 0");
        logger.info(response.toString());
        return response;
    }

    private Message trainPhase2(Message parameterData, TrainData trainData) {
        // Phase1 input message
        List<ByteString> modelBytes = new ArrayList<>();
        List<Double> modelParameters = new ArrayList<>();
        String s = "";
        //            modelBytes.add(ByteString.copyFrom("", "unicode"));
        modelBytes.add(ByteString.copyFrom(s.getBytes(StandardCharsets.UTF_8)));
        modelParameters.add((double) parameter.getBatchSize());
        modelParameters.add((double) parameter.getNumEpochs());
        modelParameters.add(parameter.getIsTest() ? 1. : 0.);
        VerticalFDNNMessage message = VerticalFDNNUtils.prepareInputMessage(modelToken,
                modelBytes, modelParameters);
        VerticalFDNNMessage message1 = stub.verticalFDNNTrainPhase1(message);
        VFDNNMessage response = new VFDNNMessage(modelToken,
                new ArrayList<>(message1.getModelParametersList()),
                new ArrayList<>(message1.getModelBytesList()),
                isActive);
        return response;
    }

    private Message trainPhase3(Message masterMessage, TrainData trainData) {
        VFDNNMessage request = (VFDNNMessage) masterMessage;
        VerticalFDNNMessage message = VerticalFDNNUtils.prepareInputMessage(request.getModelToken(),
                request.getModelBytes(), request.getModelParameters());
        VerticalFDNNMessage message1 = stub.verticalFDNNTrainPhase2(message);
        VFDNNMessage response = new VFDNNMessage(modelToken,
                new ArrayList<>(message1.getModelParametersList()),
                new ArrayList<>(message1.getModelBytesList()),
                isActive);
        return response;
    }

    private Message trainPhase4(Message masterMessage, TrainData trainData) {
        VFDNNMessage request = (VFDNNMessage) masterMessage;
        VerticalFDNNMessage message = VerticalFDNNUtils.prepareInputMessage(request.getModelToken(),
                request.getModelBytes(), request.getModelParameters());
        VerticalFDNNMessage message1 = stub.verticalFDNNTrainPhase3(message);
        VFDNNMessage response = new VFDNNMessage(modelToken,
                new ArrayList<>(message1.getModelParametersList()),
                new ArrayList<>(message1.getModelBytesList()),
                isActive);
        return response;
    }

    private Message trainPhase99(Message masterMessage, TrainData trainData) {
        VFDNNMessage request = (VFDNNMessage) masterMessage;
        VerticalFDNNMessage message = VerticalFDNNUtils.prepareInputMessage(request.getModelToken(),
                request.getModelBytes(), request.getModelParameters());
        VerticalFDNNMessage message1 = stub.verticalFDNNTrainPhase99(message);
        VFDNNMessage response = new VFDNNMessage(modelToken,
                new ArrayList<>(message1.getModelParametersList()),
                new ArrayList<>(message1.getModelBytesList()),
                isActive);
        return response;
    }

    private Message inferencePhase1(VFDNNMessage masterRequest, InferenceData data) {
        // passive part
        if (isActive) {
            return new VFDNNMessage(modelToken,
                    new ArrayList<>(),
                    new ArrayList<>(),
                    isActive);
        } else {
            List<ByteString> modelBytes = new ArrayList<>();
            // convert input
            List<List<Double>> X = DataUtils.MatrixToList(DataUtils.toMatrix(XTest));
//            modelBytes.add(ByteString.copyFrom(X.toString(), "unicode"));
            modelBytes.add(ByteString.copyFrom(X.toString().getBytes(StandardCharsets.UTF_8)));
            List<Double> modelParameters = new ArrayList<>();
            VerticalFDNNMessage message = VerticalFDNNUtils.prepareInputMessage(
                    modelToken, modelBytes, modelParameters);
            VerticalFDNNMessage res = stub.verticalFDNNInferencePhase1(message);
            return new VFDNNMessage(modelToken,
                    new ArrayList<>(res.getModelParametersList()),
                    new ArrayList<>(res.getModelBytesList()),
                    isActive);
        }
    }

    private Message inferencePhase2(VFDNNMessage masterRequest, InferenceData data) {
        // active part
        if (isActive) {
            List<ByteString> modelBytes = masterRequest.getModelBytes();
            // convert input
            List<List<Double>> X = DataUtils.MatrixToList(DataUtils.toMatrix(XTest));
//            modelBytes.add(ByteString.copyFrom(X.toString(), "unicode"));
            modelBytes.add(ByteString.copyFrom(X.toString().getBytes(StandardCharsets.UTF_8)));
            List<Double> modelParameters = new ArrayList<>();
            VerticalFDNNMessage message = VerticalFDNNUtils.prepareInputMessage(
                    modelToken, modelBytes, modelParameters);
            VerticalFDNNMessage tmp = stub.verticalFDNNInferencePhase2(message);
            List<ByteString> res = tmp.getModelBytesList();
            String resStr = res.get(0).toStringUtf8();
            List<List<Double>> resDouble = DataUtils.stringToListListDouble(resStr);
            List<Double> resDouble1 = resDouble.stream().flatMap(List::stream).collect(Collectors.toList());
            List<ByteString> resModelBytes = new ArrayList<>();
            //            resModelBytes.add(ByteString.copyFrom(inferenceUid.toString(), "unicode"));
//            resModelBytes.add(ByteString.copyFrom(resDouble1.toString(), "unicode"));
            resModelBytes.add(ByteString.copyFrom(Arrays.toString(inferenceUid).getBytes(StandardCharsets.UTF_8)));
            resModelBytes.add(ByteString.copyFrom(resDouble1.toString().getBytes(StandardCharsets.UTF_8)));
            logger.info("resModelBytes: " + resModelBytes.toString());
            return new VFDNNMessage(
                    modelToken,
                    new ArrayList<>(),
                    resModelBytes,
                    isActive
            );
        } else {
            return new VFDNNMessage(modelToken,
                    new ArrayList<>(),
                    new ArrayList<>(),
                    isActive);
        }

    }


    private VFDNNMessage makeNullMessage() {
        return new VFDNNMessage(modelToken,
                new ArrayList<>(),
                new ArrayList<>(),
                isActive);
    }

    private void createStubServer() {
        // 建立 stubby server
        if ("".equals(stubAddress)) {
            stubAddress = "127.0.0.1:8891";
        }
        ManagedChannel channel = ManagedChannelBuilder.forTarget(stubAddress)
                .usePlaintext().maxInboundMessageSize(Integer.MAX_VALUE).build();
        stub = CalculateGrpc.newBlockingStub(channel);
    }

    public AlgorithmType getModelType() {
        return AlgorithmType.VerticalFDNN;
    }

}
