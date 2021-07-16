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

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jdt.fedlearn.core.entity.ClientInfo;
import com.jdt.fedlearn.core.entity.Message;
import com.jdt.fedlearn.core.entity.base.EmptyMessage;
import com.jdt.fedlearn.core.entity.randomForest.RandomforestMessage;
import com.jdt.fedlearn.core.entity.common.InferenceInit;
import com.jdt.fedlearn.core.entity.feature.Features;
import com.jdt.fedlearn.core.entity.localModel.LocalLinearModel;
import com.jdt.fedlearn.core.entity.localModel.LocalModel;
import com.jdt.fedlearn.core.entity.localModel.LocalNullModel;
import com.jdt.fedlearn.core.entity.randomForest.*;
import com.jdt.fedlearn.core.preprocess.InferenceFilter;
import com.jdt.fedlearn.core.type.AlgorithmType;
import com.jdt.fedlearn.core.type.MemoryUnitsType;
import com.jdt.fedlearn.core.type.RFModelPhaseType;
import com.jdt.fedlearn.core.type.data.Tuple2;
import com.jdt.fedlearn.core.util.Tool;
import com.jdt.fedlearn.grpc.federatedlearning.Vector;
import com.jdt.fedlearn.grpc.federatedlearning.*;
import com.jdt.fedlearn.core.loader.common.InferenceData;
import com.jdt.fedlearn.core.loader.common.TrainData;
import com.jdt.fedlearn.core.loader.randomForest.DataFrame;
import com.jdt.fedlearn.core.loader.randomForest.RFInferenceData;
import com.jdt.fedlearn.core.metrics.Metric;
import com.jdt.fedlearn.core.parameter.RandomForestParameter;
import com.jdt.fedlearn.core.parameter.SuperParameter;
import com.jdt.fedlearn.core.type.MetricType;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import org.ejml.simple.SimpleMatrix;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class RandomForestModel implements Model {
    // log
    private static final Logger logger = LoggerFactory.getLogger(RandomForestModel.class);
    // 是否是主动方
    boolean isActive = false;
    double[] yPredBagging, yLabel, yLocal;
    // inference uid
    String[] inferenceUid;
    // 损失函数
    RandomForestLoss loss;
    // 判断是否并行建树
    boolean isParallel = false;
    // local model
    LocalModel localModel;
    double magicDefault = 1.61803398;
    // 日志分隔
    private final String splitLine = "========================================================";
    // 单元测试用 1 active 1 passive
    // stubby server 变量
    // TODO：将stubby server 拉出单独做成connector，因为后续可能需要支持spark
    //  stubby server connector跟spark connect应该是一样的level
    private CalculateGrpc.CalculateBlockingStub stub = null;
    private String stubAddress = "";
    // 该次训练任务的模型id：modelString
    private String modelString;
    private Map<String, String> serializedModel;
    // flag：是否初始化
    private boolean isInitTrain = false;
    private boolean hasReceivedY = false;
    // 随机森林参数
    private RandomForestParameter parameter = new RandomForestParameter();
    // encryption data
    private RandomForestEncryptData encryptData;
    // 该参与方挂载的训练集，测试集，以及加密后的label
    private ArrayList<Integer>[] featureIds;
    private Matrix XTrain;
    private SimpleMatrix yTrain;
    private SimpleMatrix XTest;
    // 主动方的 label，以及预测值
    private double[][] yPredValues;
    // 以下为尚未梳理的变量
    private double lossType = -1;
    private Random rand = new Random(666);


    // 构造函数
    public RandomForestModel() {
    }

    public RandomForestModel(String stubAddress) {
        this.stubAddress = stubAddress;
    }


    /**
     * client init 完成文件加载和初始化等 初始化完成TrainData类的完整初始化 超参数的解析和赋值 密钥生成等
     *
     * @param rawData    原始数据
     * @param parameter  从master传入的超参数
     * @param features   特征
     * @param others     其他参数
     * @return 解析完成的训练数据
     */
    public DataFrame trainInit(String[][] rawData, String[] uids, int[] testIndex, SuperParameter parameter, Features features, Map<String, Object> others) {
        Tuple2<String[],String[]> trainTestUid = Tool.splitUid(uids,testIndex);
        DataFrame trainData = new DataFrame(rawData, trainTestUid._1(), features);
        // 第一次请求里 body 是 parameter, extendJson 是 idMap + featureList
        logger.info("Init train start");
        this.parameter = (RandomForestParameter) parameter;
        rand = new Random(this.parameter.getRandomSeed());
        // 是否并行建树
        isParallel = this.parameter.getnJobs() > 1;
        // 初始化 train data
        logger.info("HasLabel: " + String.valueOf(trainData.hasLabel));
        trainData.fillna(0);
        DataFrame df = trainData;
        logger.info(String.format("Dataframe: %s rows, %s columns", df.numRows(), df.numCols()));
        int numTrees = this.parameter.getNumTrees();
//        Xs_train = new SimpleMatrix[numTrees];
        featureIds = new ArrayList[numTrees];
        ArrayList<String> headers = df.getHeaders();
        logger.info(String.format("Dataframe header: %s", String.join(" ", headers)));
        SimpleMatrix X = df.toSmpMatrix(0, df.numRows(), 0, df.numCols());
        XTrain = DataUtils.toMatrix(X);
        logger.info(String.format("Train matrix: %s rows, %s columns", X.numRows(), X.numCols()));
        // Sample some feature ids and get the corresponding local feature ids
        int[] numSampleFeature = Arrays.stream(String.valueOf(others.get("featureAllocation")).split(","))
                .mapToInt(Integer::parseInt).toArray();
        for (int i = 0; i < numTrees; i++) {
            ArrayList<Integer> sampleFeatures = DataUtils.choice(numSampleFeature[i], df.numCols(), rand);
            Collections.sort(sampleFeatures);
            featureIds[i] = new ArrayList<>();
            for (int fi : sampleFeatures) {
                featureIds[i].add(fi);
            }
            logger.info(String.format("Sampled features: %s", sampleFeatures.toString()));

//            logger.info(String.format("Train matrix, %s rows, %s columns", Xs_train[i].numRows(), Xs_train[i].numCols()));
        }
        // 如果有label，加载Y，作为active方
        if (trainData.hasLabel) {
            isActive = true;

            // 初始化 loss
            loss = new RandomForestLoss(this.parameter.getLoss());
            // get loss type
            this.lossType = loss.getLossTypeId();

            // active 方建立 stubby server
            if ("".equals(stubAddress)) {
                stubAddress = "127.0.0.1:8891";
            }
            ManagedChannel channel = ManagedChannelBuilder.forTarget(stubAddress)
                    .usePlaintext().maxInboundMessageSize(Integer.MAX_VALUE).build();
            stub = CalculateGrpc.newBlockingStub(channel);

            // 处理label
            yLabel = trainData.getLabel();
//            logger.info(String.format("yLabel: %s", Arrays.toString(yLabel)));
            Vector.Builder vectorOrBuilder = Vector.newBuilder();
            for (int j = 0; j < yLabel.length; j++) {
                vectorOrBuilder.addValues(yLabel[j]);
            }
            Vector y_vec = vectorOrBuilder.build();
            yTrain = DataUtils.toSmpMatrix(y_vec);
            // TODO: fit local model
            RandomForestParameter param = (RandomForestParameter) parameter;
            if ("Null".equals(param.getLocalModel())) {
                localModel = new LocalNullModel();
            } else if ("LinearModel".equals(param.getLocalModel())) {
                localModel = new LocalLinearModel();
            }
            localModel.train(X, yTrain);
            yLocal = localModel.batchPredict(X);
            // adjust yTrain
            IntStream.range(0, yLocal.length).forEach(idx -> {
                yTrain.set(idx, yTrain.get(idx) - yLocal[idx]);
            });
            yPredValues = new double[this.parameter.getNumTrees()][trainData.getDatasetSize()];
            yPredBagging = new double[yPredValues[0].length];
            for (double[] yi : yPredValues) {
                Arrays.fill(yi, magicDefault);
            }
            Arrays.fill(yPredBagging, magicDefault);
            // 根据 sample 到的 sampleId fill mean
            List<Integer> sampleId = DataUtils.stringToSampleId(String.valueOf(others.get("sampleId")));
            int count = sampleId.size();
            double sum = IntStream.range(0, count).mapToDouble(idx -> yLabel[sampleId.get(idx)]).sum();
            double filled_mean = sum / count;
            sampleId.stream().forEach(idx -> {
                yPredBagging[idx] = filled_mean;
            });

            switch (this.parameter.getEncryptionType()) {
                case Paillier:
                    encryptData = new PaillierEncryptData(y_vec, this.parameter.getEncryptionCertainty());
                    break;
                case IterativeAffine:
                    encryptData = new IterativeAffineEncryptData(y_vec, this.parameter.getEncryptionCertainty());
                    break;
                default:
                    throw new IllegalArgumentException("Unsupported encryption type!");
            }
        } else {
            // passive 方建立 stubby server
            if ("".equals(stubAddress)) {
                stubAddress = "127.0.0.1:8891";
            }
            ManagedChannel channel = ManagedChannelBuilder.forTarget(stubAddress)
                    .usePlaintext().maxInboundMessageSize(Integer.MAX_VALUE).build();
            stub = CalculateGrpc.newBlockingStub(channel);

            switch (this.parameter.getEncryptionType()) {
                case Paillier:
                    encryptData = new PaillierEncryptData();
                    break;
                case IterativeAffine:
                    encryptData = new IterativeAffineEncryptData();
                    break;
                default:
                    throw new IllegalArgumentException("Unsupported encryption type!");
            }
        }
        logger.info("Init train end");
        return trainData;
    }

    /**
     * 进行模型训练，客户端的整体控制流程
     *
     * @param phase    当前步骤
     * @param request 训练迭代参数
     * @param train    训练数据
     * @return 客户端的响应结果
     */
    @Override
    public Message train(int phase, Message request, TrainData train) {
        logger.info(String.format("Training process, phase %s start", phase) + splitLine);
        // TODO 控制流程优化，去掉下面这个判断
        if (request==null) {
            return null;
        }
        Message response = null;
        // parse request

        // 根据 phase 编号处理对应phase
        switch (RFModelPhaseType.valueOf(phase)) {
            case GET_PREDICT:
                response = trainPhase1(request);
                break;
            case SORT_BY_FEATURES:
                if (isActive) {
                    response = trainPhase2Active(request);
                } else {
                    response = trainPhase2Passive(request);
                }
                break;
            case CALCULATE_SPLIT_POINTS:
                if (isActive) {
                    response = trainPhase3Active(request);
                } else {
                    response = trainPhase3Passive(request);
                }
                break;
            case SPLIT_DATA:
                response = trainPhase4(request);
                break;
            case PASS:
                break;
            // model 什么都不用做
            case FINISH_TRAIN:
                // 保存模型
                response = trainPhase99(request);
                break;
            default:
                logger.info("phase number error in client!!!!!!!!");
        }
        logger.info(String.format("Training process, phase %s end", phase) + splitLine);
//        logger.debug("Max Memory: {} ", DataUtils.checkMaxMemory());
//        logger.debug("Memory used: {} MB", DataUtils.checkUsedMemory("MB"));
        logger.info("Memory used: {} MB", DataUtils.checkUsedMemory(MemoryUnitsType.MB));
        return response;
    }


    /**
     * 第一次初始化：主动方回传加密的label, publickey, 特征采样信息和样本数；被动方回传特征采样信息和样本数
     * 主动方非初始化：调用trainPhase1Active函数
     *
     * @param request  服务端发送的JSON格式请求
     * @return 客户端的响应结果
     */
    private Message trainPhase1(Message request) {
        String responseStr = "";
        if (!isInitTrain) {
            // 初始化： 已经移到 TrainInit中
            //trainInit(trainData, jsonData);
            isInitTrain = true;
            if (isActive) {
                // active 端回传加密的Y 和 publickey
                InputMessage encryptY = encryptData.getEncryptedY();
                responseStr = DataUtils.inputMessage2json(encryptY);
            }
            // 同时将feature id map 打包
            String allFeatureIdMap = Arrays.stream(featureIds).map(
                    x -> x.stream().map(String::valueOf).collect(Collectors.joining(",")))
                    .collect(Collectors.joining(";"));
            if (!("".equals(allFeatureIdMap))) {
                responseStr = responseStr + "||" + allFeatureIdMap;
            } else {
                responseStr = responseStr + "||" + "";
            }
            // 回传样本数
//            String numSamples = String.valueOf(Xs_train[0].numRows());
            String numSamples = String.valueOf(XTrain.getRowsCount());
            responseStr = responseStr + "||" + numSamples;
        } else {
            // 非初始化过程 active 端 返回预测值
            if (isActive) {
                RandomForestReq req = (RandomForestReq)request;
                responseStr = trainPhase1Active(req);
            } else {
                responseStr = "";
            }
        }
        return new RandomforestMessage(responseStr);
    }

    /**
     * 执行phase99操作，序列化模型，计算最终性能指标
     *
     * @param request  服务端发送的请求
     * @return 客户端的响应结果
     */
    public Message trainPhase99(Message request) {
        String responseStr;
        // phase 99 接受模型
        //logger.info("Header of jsonData:" + jsonData.substring(0, 100));
        // TODO: fix large string bug
        String jsonData = ((RandomforestMessage)request).getResponseStr();
        jsonData = jsonData.split("\003")[0];
//        Map<String, String> map = new HashMap<>();
        ObjectMapper mapper = new ObjectMapper();
        mapper.configure(JsonParser.Feature.ALLOW_UNQUOTED_FIELD_NAMES, true);
        TypeReference<HashMap<String, String>> typeRef
                = new TypeReference<HashMap<String, String>>() {
        };
        try {
            serializedModel = mapper.readValue(jsonData, typeRef);
        } catch (IOException e) {
            logger.error("parse error", e);
        }
        responseStr = "finish";
        // give final metrics
        // TODO: Add acc, F1 and others for cross entropy

        return new RandomforestMessage(responseStr);
    }


    public String trainPhase1Active(RandomForestReq req) {
        String responseStr = "";
        // multi process

        // 对应 algorithm 侧的 extraMessage， treeIds||sampleIds
        String[] extraMessage = new String[2];
        extraMessage[0] = req.getTidToSampleID().keySet().stream().map(Object::toString)
                .collect(Collectors.joining("|"));
        logger.info("TidToSampleId: " + req.getTidToSampleID().values().toString());
        extraMessage[1] = req.getTidToSampleID().values().stream().map(Object::toString).collect(Collectors.joining("|"));

//        String[] extraMessage = req.getExtraInfo().split("\\|\\|");
//        Integer[] treeIds = Arrays.stream(extraMessage[0].substring(1, extraMessage[0].length()-1).split("\\|"))
//                .map(Integer::valueOf).toArray(Integer[]::new);
        Integer[] treeIds = Arrays.stream(extraMessage[0].split("\\|"))
                .map(Integer::valueOf).toArray(Integer[]::new);
        String[] sampleIds = extraMessage[1].split("\\|");
        String[] yBaggings = new String[treeIds.length];
        for (int i = 0; i < treeIds.length; i++) {
            logger.info(String.format("SampleId %s: %s", i, sampleIds[i]));
            if ("[]".equals(sampleIds[i])) {
                yBaggings[i] = String.valueOf(magicDefault);
                continue;
            }
            Integer[] sampleIdi = Arrays.stream(sampleIds[i].substring(1, sampleIds[i].length()-1).split(", "))
                    .map(Integer::valueOf).toArray(Integer[]::new);

            double singlePred = loss.bagging(DataUtils.selecRows(yTrain, sampleIdi));
            // 更新预测值到 y_pred, 并更新RMSE
            for (int idx : sampleIdi) {
                // 更新预测值
                yPredValues[treeIds[i]][idx] = singlePred + yLocal[idx];
                // 找出参与预测的树计算mean
                yPredBagging[idx] = Arrays.stream(yPredValues).filter(x -> x[idx] != magicDefault)
                        .mapToDouble(xx -> xx[idx]).sum();
                double countTmp = Arrays.stream(yPredValues).filter(x -> x[idx] != magicDefault).count();
                yPredBagging[idx] = yPredBagging[idx] / countTmp;
                yBaggings[i] = String.valueOf(singlePred);
            }
        }
        // 更新 Loss
        double[][] tmp = parseValidY();
        double[] yPredMeanValid = tmp[0];
        double[] yLabelValid = tmp[1];
        if (sameValueCheck(yLabelValid)) {
            logger.warn("Label has same value, some metric might not be correct for example AUC.");
        }
        Map<String, Double[][]> metricArrMap = new HashMap<>();
        Map<String, Double> metricMap = new HashMap<>();
         String[] arr = MetricType.getArrayMetrics();
        for (MetricType t : parameter.getEval_metric()) {
            if (Arrays.asList(arr).contains(t.getMetric())) {
                metricArrMap.put(t.getMetric(), Metric.calculateMetricArr(t,yPredMeanValid, yLabelValid,new ArrayList<>()));
            } else {
                metricMap.put(t.getMetric(), Metric.calculateMetric(t, yPredMeanValid, yLabelValid));
            }
        }

        String metricArrRes = Tool.getMetricArrString(metricArrMap);

        // 序列化结果
        String metricString = metricMap.keySet().stream()
                .map(key -> key + "=" + metricMap.get(key))
                .collect(Collectors.joining(", ", "{", "}"));
        responseStr = String.join(",", yBaggings) + "||" + metricString + "||" + metricArrRes;
        return responseStr;
    }

    /**
     * 执行主动方phase2操作
     * 根据sampleIds收集label和特征
     * 根据每个特征的大小对相应的label进行排序
     * 根据parameter对label计算 bin sum
     *
     * @param request  服务端发送的请求
     * @return 客户端的响应结果
     */
    public RandomForestRes trainPhase2Active(Message request) {
        RandomForestReq req = (RandomForestReq) request;
        req.sampleToTid();
        HashMap<Integer, ArrayList<Integer>> tidToSampleId = req.getTidToSampleID();
        int numTrees = req.getTidToXsampleId().size();
        Double[][] scalars = new Double[numTrees][1];
        Matrix[][] matrices = new Matrix[numTrees][1];
        Vector[][] vectors = new Vector[numTrees][1];
        int idx = 0;
        for( HashMap.Entry<Integer, ArrayList<Integer>> entry :  tidToSampleId.entrySet()) {
            int treeIdi = entry.getKey().intValue();
            ArrayList<Integer> sampleIdi = entry.getValue();
            Matrix X = DataUtils.selectSubMatrix(XTrain, sampleIdi, featureIds[treeIdi]);
            SimpleMatrix y = DataUtils.selecRows(yTrain, sampleIdi);
            matrices[idx][0] = X;
            vectors[idx][0] = DataUtils.toVector(y);
            scalars[idx][0] = (double) parameter.getNumPercentiles();
            idx = idx + 1;
        }
        // 调用 active Phase 2
        MultiInputMessage requestPhase2 = DataUtils.prepareMultiInputMessage(
                matrices,
                vectors,
                scalars,
                numTrees);
        // TODO: change stubby server call to connector
        MultiOutputMessage responsePhase2 = stub.mPRandomForestPhase2(requestPhase2);
        RandomForestRes res = new RandomForestRes(req.getClient(),
                DataUtils.outputMessage2json(responsePhase2),
                isActive, null, -1, req.getExtraInfo(), req.getTidToSampleID());
        return res;
    }

    /**
     * 执行被动方phase2操作
     * 根据sampleIds收集加密后的label和特征
     * 根据每个特征的大小对相应的加密后的label进行排序
     * 根据parameter对加密后的label计算 bin sum
     *
     * @param request  服务端发送的请求
     * @return 客户端的响应结果
     */
    public RandomForestRes trainPhase2Passive(Message request) {
        RandomForestReq req = (RandomForestReq) request;
        if (req.isSkip()) {
            // skip 什么都不用做
            ClientInfo client = req.getClient();
            return new RandomForestRes(client, "", isActive);
        }

        req.sampleToTid();
        HashMap<Integer, ArrayList<Integer>> tidToSampleId = req.getTidToSampleID();
        int numTrees = tidToSampleId.size();
        ArrayList<Integer>[] sampleIds1 = new ArrayList[numTrees];
        int idx = 0;
        for( HashMap.Entry<Integer, ArrayList<Integer>> entry :  tidToSampleId.entrySet()) {
            sampleIds1[idx] = entry.getValue();
            idx = idx + 1;
        }

        if (!hasReceivedY) {
            // get all Y from active
            InputMessage Y = DataUtils.json2inputMessage(req.getBody());
            encryptData.loadY(Y);
            hasReceivedY = true;
        }
        MultiInputMessage requestPhase2 = encryptData.prepareInputMessagePhase2Passive(tidToSampleId,
                featureIds,
                XTrain,
                parameter.getNumPercentiles(),
                encryptData.getSubY(sampleIds1));
        MultiOutputMessage responsePhase2 = stub.mPRandomForestPhase2(requestPhase2);
        RandomForestRes res = new RandomForestRes(req.getClient(), DataUtils.outputMessage2json(responsePhase2), isActive);
        return res;
    }

    /**
     * 执行被动方phase3操作
     * 构造响应结果并回传
     *
     * @param request  服务端发送的请求
     * @return 客户端的响应结果
     */
    public RandomForestRes trainPhase3Passive(Message request) {
        RandomForestReq req = (RandomForestReq) request;
        ClientInfo client = null;
        Integer treeId = -1;
        ArrayList<Integer> sampleId = req.getSampleId();
        client = req.getClient();
        treeId = req.getTreeId();
        RandomForestRes res = new RandomForestRes(client, "", isActive, sampleId, treeId, req.getExtraInfo());
        return res;
    }

    /**
     * 执行主动方phase3操作
     * 主动方解析Phase2各个Client回传的信息，并将percentile bin sum解密。
     * enumerate各个Y的 bin sum，获取最大的 weighted square of sum （S）和对应的特征id
     * 如果 S 大于 (sum Y)^2，则该特征的分位点为分裂点，否则分裂失败，置为叶节点
     *
     * @param request  服务端发送的请求
     * @return 客户端的响应结果
     */
    public RandomForestRes trainPhase3Active(Message request) {
        RandomForestReq req = (RandomForestReq) request;
        ClientInfo client = null;
        String[] jsonStr = req.getBody().split("\\|\\|\\|");
        req.sampleToTid();
        // 先处理 Phase 2 返回结果
        Matrix[][] Y1s = new Matrix[0][0];
        int[][] sampleId = null;
        for (int i = 0; i < jsonStr.length; i++) {
            RandomForestRes tmp = new RandomForestRes();
            tmp.parseJson(jsonStr[i]);
            MultiOutputMessage responsePhase2 = DataUtils.json2MultiOutputMessage(tmp.getBody());
            if (Y1s.length == 0) {
                Y1s = new Matrix[responsePhase2.getMessagesCount()][jsonStr.length];
            }
            if (tmp.getIsActive()) {
                client = tmp.getClient();
                sampleId = new int[Y1s.length][];
                int [] tid_arr = tmp.getTidToSampleId().keySet().stream().mapToInt(Integer::intValue).toArray();
                for (int ii = 0; ii < Y1s.length; ii++) {
                    Y1s[ii][i] = responsePhase2.getMessages(ii).getMatrices(0);
                    sampleId[ii] = tmp.getTidToSampleId().get(tid_arr[ii]).stream().mapToInt(Integer::intValue).toArray();
                }
            } else {
                for (int ii = 0; ii < Y1s.length; ii++) {
                    Y1s[ii][i] = encryptData.parsePassivePhase2(responsePhase2.getMessages(ii));
                }
            }
        }
        // 处理 Phase 3
        logger.info("Start phase 3 ...");
        Double[][] yMean = new Double[Y1s.length][2];
        for (int i = 0; i < Y1s.length; i++) {
            assert sampleId != null;
            SimpleMatrix y = DataUtils.selecRows(yTrain, sampleId[i]);
            yMean[i][0] = y.elementSum() / y.numRows();
            yMean[i][1] = lossType;
        }
        MultiInputMessage requestPhase3 = DataUtils.prepareMultiInputMessage(
                Y1s,
                new Vector[][]{},
                yMean,
                Y1s.length);
        MultiOutputMessage responsePhase3 = stub.mPRandomForestPhase3(requestPhase3);
        RandomForestRes res = new RandomForestRes(client, DataUtils.outputMessage2json(responsePhase3), isActive,
                null, -1, req.getExtraInfo(), req.getTidToSampleID());
        return res;
    }


    /**
     * 执行phase4操作，主动方和被动方操作相同
     * 若为分裂方：根据特征id和percentile给出特征的分裂阈值value opt，
     * 将样本id根据分裂阈值分为左子树节点和右子树节点
     *
     * @param request  服务端发送的请求
     * @return 客户端的响应结果
     */
    public RandomForestRes trainPhase4(Message request) {
        RandomForestReq req = (RandomForestReq) request;
        RandomForestRes res;
        req.sampleToTid();
        if ("".equals(req.getBody())) {
            // skip 什么都不用做
            res = new RandomForestRes(req.getClient(), "", isActive, req.getSampleId(), req.getTreeId());
        } else {
            Map<Integer, int[]> treeSampleMap = new HashMap<>();
//            String[] exInfo = req.getExtraInfo().split("\\|\\|");
            String[] exInfo = new String[2];
            exInfo[0] = req.getTidToSampleID().keySet().stream().map(Object::toString)
                    .collect(Collectors.joining("|"));
            exInfo[1] = req.getTidToSampleID().values().stream().map(Object::toString).collect(Collectors.joining("|"));

            String[] treeIds = exInfo[0].split("\\|");
            String[] sampleIds = exInfo[1].split("\\|");
            for (int i = 0; i < treeIds.length; i++) {
                int[] sampleIdi = Arrays.stream(sampleIds[i].substring(1, sampleIds[i].length() - 1).split(","))
                        .map(String::trim).mapToInt(Integer::parseInt).toArray();
                treeSampleMap.put(Integer.parseInt(treeIds[i]), sampleIdi);
            }
            String[] s = req.getBody().split("\\|\\|");
            Matrix[][] matrices = new Matrix[s.length][1];
            Double[][] scalars = new Double[s.length][2];
            treeIds = new String[s.length];
            sampleIds = new String[s.length];
            for (int i = 0; i < s.length; i++) {
                String si = s[i];
                ObjectMapper mapper = new ObjectMapper();
                try {
                    Map<String, Double> tmp = mapper.readValue(si, Map.class);
                    int idx = (int) Math.round(tmp.get("treeId"));
                    treeIds[i] = String.valueOf(idx);
                    sampleIds[i] = Arrays.toString(treeSampleMap.get(idx));
                    matrices[i][0] = DataUtils.selectSubMatrix(XTrain,
                            treeSampleMap.get(idx),
                            featureIds[Integer.valueOf(treeIds[i])]);
                    scalars[i][0] = tmp.get("featureId");
                    scalars[i][1] = tmp.get("percentile");
                } catch (JsonProcessingException e) {
                    logger.error("JsonProcessingException: ", e);
                }
            }
            MultiInputMessage requestPhase4 = DataUtils.prepareMultiInputMessage(
                    matrices,
                    new Vector[][]{},
                    scalars,
                    matrices.length);
            MultiOutputMessage responsePhase4 = stub.mPRandomForestPhase4(requestPhase4);
            exInfo = new String[2];
            exInfo[0] = String.join("|", treeIds);
            exInfo[1] = String.join("|", sampleIds);
            res = new RandomForestRes(req.getClient(),
                    DataUtils.outputMessage2json(responsePhase4),
                    isActive,
                    null,
                    -1,
                    String.join("||", exInfo),
                    req.getTidToSampleID());
        }
        return res;
    }

    // 推理初始化，
    public Message inferenceInit(String[] uidList, String[][] inferenceCacheFile, Map<String, Object> others) {
        logger.info("Init inference...");
        return InferenceFilter.filter(uidList, inferenceCacheFile);
    }

    /**
     * 客户端推理
     *
     * @param phase     当前步骤
     * @param jsonData  服务端请求
     * @param data      推理数据集
     * @return 序列化后的返回结果
     */
    @Override
    public Message inference(int phase, Message jsonData, InferenceData data) {
        logger.info(String.format("Inference process, phase %s start", phase) + splitLine);
        if (phase == -1) {
            RFInferenceData inferenceData = (RFInferenceData) data;
            String[] subUid = inferencePhase1(inferenceData, jsonData);
            if (subUid.length == 0) {
                return null;
            }
            XTest = inferenceData.selectToSmpMatrix(subUid);
            inferenceUid = subUid;
        }
        return inferenceOneShot(phase, jsonData);

    }

    /**
     * 推理初始化，处理推理数据集
     *
     * @param inferenceData 推理数据集
     * @param request      服务端请求
     */
    public String[] inferencePhase1(RFInferenceData inferenceData, Message request) {
//        String[][] rawTable = inferenceData.getUidFeature();
        // TODO 确定 inference 到底带不带 header
        inferenceData.init();
        inferenceData.fillna(0);
        // 接受 jsonData 里的 InferenceInit 中的 uid
        InferenceInit init = (InferenceInit) request;
        return init.getUid();
    }

    /**
     * 快速推理
     *
     * @param phase     当前步骤
     * @param request  服务端请求
     * @return 序列化后的返回结果
     */
    public Message inferenceOneShot(int phase, Message request) {
        if (phase == -1) {
            Map<Integer, Map<Integer, String>> treeInfo = parseModel(modelString);
            Map<Integer, Map<Integer, List<String>>> res = new HashMap<>();
            IntStream.range(0, treeInfo.size()).parallel().forEach(id -> {
                Integer treeId = id;
                Map<Integer, List<String>> tmp = new HashMap<>();
                Map<Integer, String> tree = treeInfo.get(treeId);
                for (Map.Entry<Integer, String> nodeId : tree.entrySet()) {
                    String[] s = nodeId.getValue().split(" ");
                    List<String> vals = new ArrayList<>();
                    if (s.length > 1) {
                        // split
                        int feature = Integer.parseInt(s[0]);
                        double threshold = Double.parseDouble(s[1]);
                        for (int i = 0; i < XTest.numRows(); i++) {
                            if (XTest.get(i, feature) < threshold) {
                                vals.add("L");
                            } else {
                                vals.add("R");
                            }
                        }
                    } else {
                        // prediction
                        for (int i = 0; i < XTest.numRows(); i++) {
                            vals.add(s[0]);
                        }
                    }
                    tmp.put(nodeId.getKey(), vals);
                }
                res.put(treeId, tmp);
            });
            String jsonResult = "";
            ObjectMapper mapper = new ObjectMapper();
            try {
                jsonResult = mapper.writerWithDefaultPrettyPrinter()
                        .writeValueAsString(res);
            } catch (JsonProcessingException e) {
                logger.error("JsonProcessingException: ", e);
            }
            // check if is active
            if (modelString.contains("localModel")) {
                // do local prediction
                double[] localPredict = localModel.batchPredict(XTest);
                return new Randomforestinfer2Message(jsonResult, inferenceUid, localPredict, "one-shot");
            } else {
                return new Randomforestinfer2Message(jsonResult, inferenceUid, null, "");
            }
        } else if (phase == -2) {
            // phase -1 model 侧什么都不需要做
            return new EmptyMessage();
        } else if (phase == -3) {
            // pahse -2 model 侧什么都不需要做
            return new EmptyMessage();
        } else if (phase == -4) {
            // pahse -3 model 侧什么都不需要做
            return new EmptyMessage();
        } else {
            return new EmptyMessage();
        }
    }

    /**
     * 解析模型
     *
     * @param modelString  序列化的模型
     * @return 模型解析后的Map结构
     */
    private Map<Integer, Map<Integer, String>> parseModel(String modelString) {
        Map<String, String> map = new HashMap<>();
        Map<Integer, Map<Integer, String>> treeInfo = new HashMap<>();
        String[] s = modelString.substring(1, modelString.length() - 1).split(", ");
        for (String si : s) {
            String[] tmp = si.split("=");
            map.put(tmp[0], tmp[1]);
        }
        int numTrees = Integer.parseInt(map.get("numTrees"));
        for (int i = 0; i < numTrees; i++) {
            String singleTreeString = map.get(String.format("Tree%s", i));
            ObjectMapper mapper = new ObjectMapper();
            TypeReference<HashMap<String, Map<String, String>>> typeRef
                    = new TypeReference<HashMap<String, Map<String, String>>>() {
            };
            try {
                Map<String, Map<String, String>> singleTreeMap = mapper.readValue(singleTreeString, typeRef);
                treeInfo.put(i, parseTreeString(singleTreeMap));
            } catch (JsonProcessingException e) {
                logger.error("JsonProcessingException: ", e);
            }

        }
        return treeInfo;
    }

    /**
     * 模型
     *
     * @param map  模型解析的中间结果（序列化的树）
     * @return 序列化的树解析后的Map结构
     */
    private Map<Integer, String> parseTreeString(Map<String, Map<String, String>> map) {
        // parse model string, then extract the info of this client
        //ClientInfo
        Map<Integer, String> map1 = new HashMap<>();
        for (Map.Entry<String, Map<String, String>> keyi : map.entrySet()) {
            Map<String, String> tmp = keyi.getValue();
            if ("1".equals(tmp.get("isLeaf"))) {
                map1.put(Integer.parseInt(keyi.getKey()), tmp.get("prediction"));
            } else {
                ObjectMapper mapper = new ObjectMapper();
                TypeReference<HashMap<String, String>> typeRef
                        = new TypeReference<HashMap<String, String>>() {
                };
                try {
                    Map<String, String> tmp1 = mapper.readValue(tmp.get("referenceJson"), typeRef);
                    map1.put(Integer.parseInt(keyi.getKey()), tmp1.get("feature_opt") + " " + tmp1.get("value_opt"));
                } catch (JsonProcessingException e) {
                    logger.error("JsonProcessingException: ", e);
                }
            }
        }
        return map1;
    }


    /**
     * 模型反序列化
     *
     * @param input 序列化的模型，
     */
    public void deserialize(String input) {
        Map<String, String> strTrees;
        logger.info("Model deserialize...");
        modelString = input;
        // check if model string is start with {
        if ("{".equals(modelString.substring(0, 1))) {
            // deserialize string
            strTrees = Arrays.stream(modelString.substring(1, modelString.length() - 1).split(", "))
                    .map(entry -> entry.split("="))
                    .collect(Collectors.toMap(entry -> entry[0], entry -> entry[1]));
        } else {
            // deserialize string
            strTrees = Arrays.stream(modelString.split(", "))
                    .map(entry -> entry.split("="))
                    .collect(Collectors.toMap(entry -> entry[0], entry -> entry[1]));
        }
        serializedModel = strTrees;
        // TODO finish inference on local linear model
        // load local model
        if (strTrees.containsKey("localModel")) {
            String modelType = strTrees.get("localModelType");
            if ("Null".equals(modelType)) {
                localModel = new LocalNullModel();
            } else if ("LinearModel".equals(modelType)) {
                localModel = new LocalLinearModel();
            }
            localModel = localModel.deserialize(strTrees.get("localModel"));
        }
    }

    /**
     * 模型序列化具体实现
     *
     * @return 模型序列化为树的序列化结构
     */
    public Map<String, String> getModel() {
        Map<String, String> strTrees = serializedModel;
        int numTrees = Integer.parseInt(strTrees.get("numTrees"));
        strTrees.put("numTrees", Integer.toString(numTrees));
        // serialize local model
        if (isActive) {
            strTrees.put("localModelType", localModel.getModelType());
            strTrees.put("localModel", localModel.serialize());
        }
        return strTrees;
    }

    /**
     * 供client端调用，模型序列化
     *
     * @return 模型序列化后的结果
     */
    public String serialize() {
        if (serializedModel == null) {
            return "";
        }
        modelString = getModel().toString();
        return modelString;
    }


    /**
     * 解析ValidY
     *
     * @return 解析后的ValidY
     */
    private double[][] parseValidY() {
        int count = (int) Arrays.stream(yPredBagging).filter(x -> x != magicDefault).count();
        double[][] validY = new double[2][count];
        validY[0] = Arrays.stream(yPredBagging).filter(x -> x != magicDefault).toArray();
        validY[1] = IntStream.range(0, yPredBagging.length).filter(idx -> yPredBagging[idx] != magicDefault)
                .mapToDouble(i -> yLabel[i]).toArray();
        return validY;
    }

    /**
     * 判断是否有重复值
     *
     * @param   value   待判断的数组
     * @return 是否存在重复值
     */
    private boolean sameValueCheck(double[] value) {
        HashSet<Double> tmp = new HashSet<>();
        for (double vi : value) {
            tmp.add(vi);
        }
        return tmp.size() == 1;
    }

    public AlgorithmType getModelType(){
        return AlgorithmType.RandomForest;
    }

}
