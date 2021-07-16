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
import com.jdt.fedlearn.core.loader.common.CommonLoad;
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
import com.jdt.fedlearn.core.math.MathExt;
import com.jdt.fedlearn.core.metrics.Metric;
import com.jdt.fedlearn.core.parameter.RandomForestParameter;
import com.jdt.fedlearn.core.parameter.SuperParameter;
import com.jdt.fedlearn.core.type.MetricType;
import com.n1analytics.paillier.EncryptedNumber;
import com.n1analytics.paillier.PaillierPrivateKey;
import com.n1analytics.paillier.PaillierPublicKey;
import com.n1analytics.paillier.cli.PrivateKeyJsonSerialiser;
import org.ejml.simple.SimpleMatrix;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.Serializable;
import java.math.BigInteger;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static com.jdt.fedlearn.core.encryption.Decryptor.String2PaillierPrivateKey;

public class DistributedRandomForestModel implements Model, Serializable {
    // log
    private static final Logger logger = LoggerFactory.getLogger(DistributedRandomForestModel.class);
    // 日志分隔
    private final String splitLine = "========================================================";
    private Random rand = new Random(666);
    // 是否是主动方
    private boolean isActive = false;
    private double[] yPredBagging, yLabel, yLocal;
    // inference uid
    private String[] inferenceUid;
    // 损失函数
    private RandomForestLoss loss;
    // local model
    private LocalModel localModel;
    private double magicDefault = 1.61803398;
    // 该次训练任务的模型id：modelString
    private String modelString;
    private Map<String, String> serializedModel;
    // flag：是否初始化
    private boolean isInitTrain = false;
    // 随机森林参数
    private RandomForestParameter parameter = new RandomForestParameter();
    // 该参与方挂载的训练集，测试集，以及加密后的label
    private SimpleMatrix[] XsTrain;
    private SimpleMatrix yTrain;
    private SimpleMatrix XTest;
    // 密钥
    private PaillierKeyPublic keyPublic = null;
    String privateKeyString;
    // 分布式每个worker读取的数据和总数据集的行映射关系
    private Map<Integer, Integer>[] sampleMap;
    // 主动方的 label，以及预测值
    private double[][] yPredValues;
    // 采样特征id
    private ArrayList<Integer>[] featureIds;
    // 是否采用分布式
    private boolean isDistributed = false;
    // 损失类型
    private double lossType = -1;
    // 验证相关
    private String[][] validationData;
    private double[] validateLabel;
    private String labelName;
    private Map<Integer, Map<String, String>> serializedModels = new HashMap<>();
    // 早停相关
    private int tmpRound = 1;
    private int bestRound;
    private String[] testId;
    private final double EPSILON = 1E-8;
    private Map<String, Double> localTree = new HashMap<>();
    // 构造函数
    public DistributedRandomForestModel() {
    }

    /**
     * client init 完成文件加载和初始化等 初始化完成TrainData类的完整初始化 超参数的解析和赋值 密钥生成等
     *
     * @param rawData    原始数据
     * @param uids       用户全量训练和验证id信息
     * @param testIndex  验证集的id索引值
     * @param parameter  从master传入的超参数
     * @param features   特征
     * @param others     其他参数
     * @return 解析完成的训练数据
     */
    public DataFrame trainInit(String[][] rawData, String[] uids, int[] testIndex, SuperParameter parameter, Features features, Map<String, Object> others) {
        labelName = features.getLabel();
        Tuple2<String[],String[]> trainTestUid = Tool.splitUid(uids,testIndex);
        testId = trainTestUid._2();
        validationData = new String[testId.length + 1][rawData[0].length];
        validationData[0] = rawData[0];
        for (int i = 0; i < testId.length; i++) {
            for (int m = 0; m < rawData.length; m++) {
                if (rawData[m][0].equals(testId[i])) {
                    validationData[i + 1] = rawData[m];
                }
            }
        }
        DataFrame trainData = new DataFrame(rawData, trainTestUid._1(), features);
        if ((others.containsKey("isDistributed")) && (others.get("isDistributed").equals("true"))) {
            this.isDistributed =true;
        }
        // 第一次请求里 body 是 parameter, extendJson 是 idMap + featureList
        logger.info("Init train start");
        this.parameter = (RandomForestParameter) parameter;
        rand = new Random(this.parameter.getRandomSeed());
        // 初始化 train data
        logger.info("HasLabel: " + trainData.hasLabel);
        trainData.fillna(0);
        logger.info(String.format("Dataframe: %s rows, %s columns", trainData.numRows(), trainData.numCols()));
        int numTrees = this.parameter.getNumTrees();
        XsTrain = new SimpleMatrix[numTrees];
        featureIds = new ArrayList[numTrees];
        ArrayList<String> headers = trainData.getHeaders();
        logger.info(String.format("Dataframe header: %s", String.join(" ", headers)));
        SimpleMatrix X = trainData.toSmpMatrix(0, trainData.numRows(), 0, trainData.numCols());
        logger.info(String.format("Train matrix: %s rows, %s columns", X.numRows(), X.numCols()));
        // Sample some feature ids and get the corresponding local feature ids
        //int numSampleFeature = (int) Math.min(df.numCows() * this.parameter.getMaxSampledRatio(), this.parameter.getMaxSampledFeatures());
        int[] numSampleFeature = Arrays.stream(String.valueOf(others.get("featureAllocation")).split(","))
                .mapToInt(Integer::parseInt).toArray();
        for (int i = 0; i < numTrees; i++) {
            ArrayList<Integer> sampleFeatures = DataUtils.choice(numSampleFeature[i], trainData.numCols(), rand);
            Collections.sort(sampleFeatures);
            featureIds[i] = new ArrayList<>();
            for (int fi : sampleFeatures) {
                featureIds[i].add(fi);
            }
            logger.info(String.format("Sampled features: %s", sampleFeatures.toString()));
            XsTrain[i] = DataUtils.selectCols(X, sampleFeatures);
            // add noise
            if ((XsTrain[i].numRows() > 0) && (XsTrain[i].numCols() > 0)) {
                SimpleMatrix noiseMult = DataUtils.randGaussSmpMatrix(XsTrain[i].numRows(), XsTrain[i].numCols(), rand).scale(1e-6).plus(1.);
                SimpleMatrix noiseAdd = DataUtils.randGaussSmpMatrix(XsTrain[i].numRows(), XsTrain[i].numCols(), rand).scale(1e-6);
                XsTrain[i] = XsTrain[i].elementMult(noiseMult).plus(noiseAdd);
            }
            logger.info(String.format("Train matrix, %s rows, %s columns", XsTrain[i].numRows(), XsTrain[i].numCols()));
        }
        // 如果有label，加载Y，作为active方
        if (trainData.hasLabel) {
            isActive = true;
            // 初始化 loss
            loss = new RandomForestLoss(this.parameter.getLoss());
            // get loss type
            this.lossType = loss.getLossTypeId();
            // 处理label
            yLabel = trainData.getLabel();
//            logger.info(String.format("yLabel: %s", Arrays.toString(yLabel)));
            Vector.Builder vectorOrBuilder = Vector.newBuilder();
            for (double v : yLabel) {
                vectorOrBuilder.addValues(v);
            }
            Vector yVec = vectorOrBuilder.build();
            yTrain = DataUtils.toSmpMatrix(yVec);
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
            IntStream.range(0, yLocal.length).forEach(idx -> yTrain.set(idx, yTrain.get(idx) - yLocal[idx]));
            yPredValues = new double[this.parameter.getNumTrees()][trainData.getDatasetSize()];
            PaillierPrivateKey privateKey = PaillierPrivateKey.create(this.parameter.getEncryptionCertainty());
            keyPublic = DataUtils.paillierPublicKeyToRpcProto(privateKey.getPublicKey());
            PrivateKeyJsonSerialiser serialiser = new PrivateKeyJsonSerialiser("");
            privateKey.serialize(serialiser);
            privateKeyString = serialiser.toString();
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
            case UPDATE_MODEL:
            case FINISH_TRAIN:
                // 保存模型
                response = updateModel(request);
                break;
            case VALIDATE:
                response = doValidation(request);
                break;
            case CALCULATE_VALIDATION_METRIC:
                response = calculateValidationMetric(request);
                break;
            case GET_BESTROUND_MODLE:
                response = getBestRound(request);
                break;
            default:
                logger.info("phase number error in client!!!!!!!!");
        }
        logger.info(String.format("Training process, phase %s end", phase) + splitLine);
        logger.debug("Max Memory: {} ", DataUtils.checkMaxMemory());
        logger.debug("Memory used: {} MB", DataUtils.checkUsedMemory(MemoryUnitsType.MB));
        return response;
    }


    /**
     * 第一次初始化：主动方回传加密的label, publickey, 特征采样信息和样本数；被动方回传特征采样信息和样本数
     * 主动方非初始化：调用multiProcessPhase1Active函数
     * 如果为初始化且分布式调用，计算每个worker加载数据和总数据集的行映射关系
     *
     * @param request  服务端发送的JSON格式请求
     * @return 客户端的响应结果
     */
    private Message trainPhase1(Message request) {
        String responseStr = "";
        if (!isInitTrain) {
            // 第一次执行phase1时,进行初始化
            if (isDistributed) {
                // 如果是分布式调用
                // 计算每个worker加载数据和总数据集的行映射关系
                DistributedRandomForestReq req = (DistributedRandomForestReq)request;
                String[] extraMessage = req.getExtraInfo().split("\\|\\|");
                Integer[] treeIds = Arrays.stream(extraMessage[0].split("\\|"))
                        .map(Integer::valueOf).toArray(Integer[]::new);
                String[] sampleIds = extraMessage[1].split("\\|");
                sampleMap = new Map[treeIds.length];
                for (int i = 0; i < treeIds.length; i++) {
                    Integer[] sampleIdi = Arrays.stream(sampleIds[i].split(","))
                            .map(Integer::valueOf).toArray(Integer[]::new);
                    sampleMap[i] = new HashMap<>();
                    for (int j = 0; j < sampleIdi.length; j++) {
                        sampleMap[i].put(sampleIdi[j], new Integer(j));
                    }
                }
            }
            if (isActive) {
                // 主动方计算对label进行加密
                Vector.Builder vectorOrBuilder = Vector.newBuilder();
                for (double v : yLabel) {
                    vectorOrBuilder.addValues(v);
                }
                Vector yVec = vectorOrBuilder.build();
                yTrain = DataUtils.toSmpMatrix(yVec);
                yPredValues = new double[parameter.getNumTrees()][yLabel.length];
                yPredBagging = new double[yPredValues[0].length];
                for (double[] yi : yPredValues) {
                    Arrays.fill(yi, magicDefault);
                }
                Arrays.fill(yPredBagging, magicDefault);
                PaillierPrivateKey privateKey = String2PaillierPrivateKey(privateKeyString);
                assert privateKey != null;
                ArrayList<EncryptedNumber> yEnc = new ArrayList<>(Arrays.asList(DataUtils.PaillierEncrypt(yVec, privateKey.getPublicKey(), false)));
                PaillierVector yPvec = DataUtils.toPaillierVector(yEnc);
                // active 端回传加密的Y 和 publickey
                InputMessage encryptY = DataUtils.prepareInputMessage(
                        new Matrix[]{},
                        new Vector[]{},
                        new Double[]{},
                        new PaillierMatrix[]{},
                        new PaillierVector[]{yPvec},
                        new PaillierValue[]{},
                        keyPublic);
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
            String numSamples = String.valueOf(XsTrain[0].numRows());
            responseStr = responseStr + "||" + numSamples;
        } else {
            // 非初始化过程 active 端 返回预测值
            if (isActive) {
                DistributedRandomForestReq req = (DistributedRandomForestReq)request;
                // TODO: change req.getTreeId to something like isMulti
                responseStr = trainPhase1Active(req);
            } else {
                responseStr = "";
            }
        }
        isInitTrain = true;
        return new RandomforestMessage(responseStr);
    }

    /**
     * 执行主动方非初始化phase1操作，计算模型预测值并回传
     *
     * @param req  服务端发送的请求
     * @return 客户端的响应结果
     */
    public String trainPhase1Active(DistributedRandomForestReq req) {
        String responseStr;
        // 下列参数对应 algorithm 侧的 extraMessage， treeIds||sampleIds
        String[] extraMessage = req.getExtraInfo().split("\\|\\|");
        Integer[] treeIds = Arrays.stream(extraMessage[0].split("\\|"))
                .map(Integer::valueOf).toArray(Integer[]::new);
        String[] sampleIds = extraMessage[1].split("\\|");
        String[] yBaggings = new String[treeIds.length];
        for (int i = 0; i < treeIds.length; i++) {
            Integer[] sampleIdi;
            if (isDistributed) {
                Integer[] sampleIdiOri = Arrays.stream(sampleIds[i].split(","))
                        .map(Integer::valueOf).toArray(Integer[]::new);
                sampleIdi = new Integer[sampleIdiOri.length];
                for (int j = 0; j < sampleIdiOri.length; j++) {
                    sampleIdi[j] = sampleMap[treeIds[i]].get(sampleIdiOri[j]);
                }
            } else {
                sampleIdi = Arrays.stream(sampleIds[i].split(","))
                        .map(Integer::valueOf).toArray(Integer[]::new);
            }
            double singlePred = loss.bagging(DataUtils.selecRows(yTrain, sampleIdi));
            // 更新预测值到 y_pred, 并更新RMSE
            for (int idx : sampleIdi) {
                // 更新预测值
                yPredValues[treeIds[i]][idx] = singlePred;
                // 找出参与预测的树计算mean
                yPredBagging[idx] = Arrays.stream(yPredValues).filter(x -> Math.abs(x[idx] - magicDefault) > EPSILON)
                        .mapToDouble(xx -> xx[idx]).sum();
                double countTmp = Arrays.stream(yPredValues).filter(x -> Math.abs(x[idx] - magicDefault) > EPSILON).count();
                yPredBagging[idx] = yPredBagging[idx] / (countTmp + Double.MIN_VALUE);
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
    public DistributedRandomForestRes trainPhase2Active(Message request) {
        DistributedRandomForestReq req = (DistributedRandomForestReq) request;
        String[] exInfo = req.getExtraInfo().split("\\|\\|");
        String[] treeIds = exInfo[0].split("\\|");
        String[] sampleIds = exInfo[1].split("\\|");
        Matrix[][] matrices = new Matrix[treeIds.length][1];
        Matrix[][] resmatrices = new Matrix[treeIds.length][1];
        Vector[][] vectors = new Vector[treeIds.length][1];

        IntStream.range(0, treeIds.length).parallel().forEach(i -> {
            int treeIdi = Integer.parseInt(treeIds[i]);
            Integer[] sampleIdi;
            if (isDistributed) {
                Integer[] sampleIdiOri = Arrays.stream(sampleIds[i].substring(1, sampleIds[i].length() - 1).split(", "))
                        .map(Integer::valueOf).toArray(Integer[]::new);
                //根据行映射关系,将总数据集的sampleId转化为worker加载的部分数据集的sampleId
                sampleIdi = new Integer[sampleIdiOri.length];
                for (int j=0;j<sampleIdiOri.length;j++) {
                    sampleIdi[j] = sampleMap[Integer.parseInt(treeIds[i])].get(sampleIdiOri[j]);
                }
            } else {
                sampleIdi = Arrays.stream(sampleIds[i].substring(1, sampleIds[i].length() - 1).split(", "))
                        .map(Integer::valueOf).toArray(Integer[]::new);
            }
            SimpleMatrix X = DataUtils.selecRows(XsTrain[treeIdi], sampleIdi);
            SimpleMatrix y = DataUtils.selecRows(yTrain, sampleIdi);
            matrices[i][0] = DataUtils.toMatrix(X);
            vectors[i][0] = DataUtils.toVector(y);
            double[][] matrix = new double[sampleIdi.length][XsTrain[treeIdi].numCols() + 1];
            for (int j = 0; j < sampleIdi.length; j++) {
                int k;
                for (k = 0; k < XsTrain[treeIdi].numCols(); k++) {
                    matrix[j][k] = matrices[i][0].getRows(j).getValues(k);
                }
                matrix[j][k] = vectors[i][0].getValues(j);
            }
            int[] bins = new int[parameter.getNumPercentiles() + 1];
            for (int j = 0; j <= parameter.getNumPercentiles(); j++) {
                bins[j] = j * sampleIdi.length / parameter.getNumPercentiles();
            }
            SimpleMatrix mat = new SimpleMatrix(XsTrain[treeIdi].numCols(), parameter.getNumPercentiles());
            for (int j = 0; j < XsTrain[treeIdi].numCols(); j++) {
                int finalJ = j;
                Arrays.sort(matrix, Comparator.comparingDouble(a -> a[finalJ]));
                double[][] transp = MathExt.transpose(matrix);
                int finalJ1 = j;
                IntStream.range(0, bins.length - 1).parallel().forEach(k -> {
                    double[] tmp = Arrays.copyOfRange(transp[matrix[0].length - 1], bins[k], bins[k + 1]);
                    double res = MathExt.average(tmp);
                    mat.set(finalJ1, k, res);
                });
            }
            resmatrices[i][0] = DataUtils.toMatrix(mat);
        });
        // 调用 active Phase 2
        // TODO: change stubby server call to connector
        MultiOutputMessage prepareMultiOutputMessages = DataUtils.prepareMultiOutputMessage(
                resmatrices,
                treeIds.length
        );
        DistributedRandomForestRes res = new DistributedRandomForestRes(req.getClient(),
                DataUtils.outputMessage2json(prepareMultiOutputMessages),
                isActive, null, -1, req.getExtraInfo());
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
    public DistributedRandomForestRes trainPhase2Passive(Message request) {
        DistributedRandomForestReq req = (DistributedRandomForestReq) request;
        String[] exInfo = req.getExtraInfo().split("\\|\\|");
        String[] treeIds = exInfo[0].split("\\|");
        String[] sampleIds = exInfo[1].split("\\|");
        Matrix[][] matrices = new Matrix[treeIds.length][1];
        MultiInputMessage distributedSubYs = null;
        PaillierMatrix[][] resmatrices = new PaillierMatrix[treeIds.length][1];
        PaillierPublicKey[] resKeyPublic = new PaillierPublicKey[treeIds.length];
        // 第一次收到加密的Y的时候需要收集 publicKey
        distributedSubYs = DataUtils.json2MultiInputMessage(req.getBody());
        if (keyPublic == null) {
            keyPublic = distributedSubYs.getMessages(0).getPaillierkeypublic();
        }

        MultiInputMessage finalDistributedSubYs = distributedSubYs;
        IntStream.range(0, treeIds.length).parallel().forEach(i -> {
            int treeIdi = Integer.parseInt(treeIds[i]);
            Integer[] sampleIdi;
            PaillierVector subYPvec;

            if (isDistributed) {
                //根据行映射关系,将总数据集的sampleId转化为worker加载的部分数据集的sampleId
                Integer[] sampleIdiOri = Arrays.stream(sampleIds[i].substring(1, sampleIds[i].length() - 1).split(", "))
                        .map(Integer::valueOf).toArray(Integer[]::new);
                sampleIdi = new Integer[sampleIdiOri.length];
                for (int j = 0; j < sampleIdiOri.length; j++) {
                    sampleIdi[j] = sampleMap[Integer.parseInt(treeIds[i])].get(sampleIdiOri[j]);
                }
            } else {
                sampleIdi = Arrays.stream(sampleIds[i].substring(1, sampleIds[i].length() - 1).split(", "))
                        .map(Integer::valueOf).toArray(Integer[]::new);
            }
            subYPvec = finalDistributedSubYs.getMessages(i).getPailliervectors(0);


            SimpleMatrix X = DataUtils.selecRows(XsTrain[treeIdi], sampleIdi);
            matrices[i][0] = DataUtils.toMatrix(X);

            // python
            resKeyPublic[i] = new PaillierPublicKey(new BigInteger(keyPublic.getN()));
            EncryptedNumber[] encryptedNumber = new EncryptedNumber[subYPvec.getValuesCount()];
            for (int j = 0; j < subYPvec.getValuesCount(); j++) {
                encryptedNumber[j] = new EncryptedNumber(resKeyPublic[i].createSignedContext(), new BigInteger(subYPvec.getValues(j).getCiphertext()), Integer.parseInt(subYPvec.getValues(j).getExponent()));
            }
            // 调用 active Phase 2
            double[][] matrix = new double[sampleIdi.length][XsTrain[treeIdi].numCols() + 1];
            for (int j = 0; j < sampleIdi.length; j++) {
                int k;
                for (k = 0; k < XsTrain[treeIdi].numCols(); k++) {
                    matrix[j][k] = (matrices[i][0].getRows(j).getValues(k));
                }
                matrix[j][k] = j;
            }
            int[] bins = new int[parameter.getNumPercentiles() + 1];
            for (int j = 0; j <= parameter.getNumPercentiles(); j++) {
                bins[j] = j * sampleIdi.length / parameter.getNumPercentiles();
            }

            EncryptedNumber[][] resvec = new EncryptedNumber[XsTrain[treeIdi].numCols()][bins.length - 1];
            for (int j = 0; j < XsTrain[treeIdi].numCols(); j++) {
                int finalJ = j;
                Arrays.sort(matrix, Comparator.comparing(a -> a[finalJ]));
                EncryptedNumber[] encryptedNumberi = new EncryptedNumber[subYPvec.getValuesCount()];
                for (int k = 0; k < subYPvec.getValuesCount(); k++) {
                    encryptedNumberi[k] = encryptedNumber[(int) matrix[k][matrix[0].length - 1]];
                }
                for (int k = 0; k < bins.length - 1; k++) {
                    resvec[j][k] = MathExt.average(encryptedNumberi, bins[k], bins[k + 1]);
                }
            }
            resmatrices[i][0] = DataUtils.toPaillierMatrix(resvec);
        });
        MultiOutputMessage responsePhase2 = DataUtils.prepareMultiOutputMessage(
                resmatrices,
                resKeyPublic,
                treeIds.length
        );
        DistributedRandomForestRes res = new DistributedRandomForestRes(req.getClient(), DataUtils.outputMessage2json(responsePhase2), isActive);
        return res;
    }

    /**
     * 执行被动方phase3操作
     * 构造响应结果并回传
     *
     * @param request  服务端发送的请求
     * @return 客户端的响应结果
     */
    public DistributedRandomForestRes trainPhase3Passive(Message request) {
        DistributedRandomForestReq req = (DistributedRandomForestReq) request;
        ClientInfo client;
        int treeId;
        ArrayList<Integer> sampleId = req.getSampleId();
        client = req.getClient();
        treeId = req.getTreeId();
        DistributedRandomForestRes res = new DistributedRandomForestRes(client, "", isActive, sampleId, treeId, req.getExtraInfo());
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
    public DistributedRandomForestRes trainPhase3Active(Message request) {
        DistributedRandomForestReq req = (DistributedRandomForestReq) request;
        ClientInfo client = null;
        String[] jsonStr = req.getBody().split("\\|\\|\\|");
        // 先处理 Phase 2 返回结果
        Matrix[][] Y1s = new Matrix[0][0];
        int[][] sampleId = null;
        PaillierPrivateKey privateKey = String2PaillierPrivateKey(privateKeyString);
        for (int i = 0; i < jsonStr.length; i++) {
            DistributedRandomForestRes tmp = new DistributedRandomForestRes();
            tmp.parseJson(jsonStr[i]);
            MultiOutputMessage responsePhase2 = DataUtils.json2MultiOutputMessage(tmp.getBody());
            if (Y1s.length == 0) {
                Y1s = new Matrix[responsePhase2.getMessagesCount()][jsonStr.length];
            }
            if (tmp.getIsActive()) {
                client = tmp.getClient();
                sampleId = new int[Y1s.length][];
                String[] strSampleId = tmp.getExtraInfo().split("\\|\\|")[1].split("\\|");
                String[] treeIds = tmp.getExtraInfo().split("\\|\\|")[0].split("\\|");
                for (int ii = 0; ii < Y1s.length; ii++) {
                    Y1s[ii][i] = responsePhase2.getMessages(ii).getMatrices(0);
                    if (isDistributed) {
                        int[] sampleIdOri = Arrays.stream(strSampleId[ii].substring(1, strSampleId[ii].length() - 1)
                                .split(", ")).mapToInt(Integer::parseInt).toArray();
                        //根据行映射关系,将总数据集的sampleId转化为worker加载的部分数据集的sampleId
                        sampleId[ii] = new int[sampleIdOri.length];
                        for (int j = 0; j < sampleIdOri.length; j++) {
                            sampleId[ii][j] = sampleMap[Integer.parseInt(treeIds[ii])].get(sampleIdOri[j]);
                        }
                    } else {
                        sampleId[ii] = Arrays.stream(strSampleId[ii].substring(1, strSampleId[ii].length() - 1)
                                .split(", ")).mapToInt(Integer::parseInt).toArray();
                    }
                }
            } else {
                for (int j = 0; j < Y1s.length; j++) {
                    PaillierMatrix Y1Enc = responsePhase2.getMessages(j).getPailliermatrices(0);
                    Y1s[j][i] = DataUtils.PaillierDecryptParallel(Y1Enc, privateKey, keyPublic, parameter.getEncryptionCertainty());
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
        Double[][] values = new Double[Y1s.length][4];
        String[] isLeafs = new String[Y1s.length];
        Matrix[][] finalY1s = Y1s;
        IntStream.range(0, Y1s.length).parallel().forEach(n -> {
            double boosting = 1 + 1E-6;
            double squareyMean = Math.pow(yMean[n][0], 2);
            double scoreOpt = Double.NEGATIVE_INFINITY;
            double partyOpt = 0;
            double featureOpt = 0;
            double percentileOpt = 0;
            double[][] Ys = new double[0][0];
            for (int i = 0; i < finalY1s[n].length; i++) {
                if (finalY1s[n][i].getRowsCount() > 0) {
                    // to double[][]
                    for (int j = 0; j < finalY1s[n][i].getRowsCount(); j++) {
                        if (finalY1s[n][i].getRows(j).getValuesCount() == 0) {
                            continue;
                        }
                        Ys = new double[finalY1s[n][i].getRowsCount()][finalY1s[n][i].getRows(j).getValuesCount()];
                    }
//                    double[][] Ys = new double[finalY1s[n][i].getRowsCount()][finalY1s[n][0].getRows(0).getValuesCount()];
                    for (int j = 0; j < finalY1s[n][i].getRowsCount(); j++) {
                        for (int k = 0; k < finalY1s[n][i].getRows(j).getValuesCount(); k++) {
                            Ys[j][k] = finalY1s[n][i].getRows(j).getValues(k);
                        }
                    }
                    // get cumsum
                    int maxEnum = Ys[0].length - 1;
                    double[][] cumsum = new double[Ys.length][Ys[0].length];
                    for (int j = 0; j < Ys.length; j++) {
                        cumsum[j][0] = Ys[j][0];
                        for (int k = 1; k < Ys[0].length; k++) {
                            cumsum[j][k] = cumsum[j][k - 1] + Ys[j][k];
                        }
                    }
                    for (int k = 0; k < maxEnum; k++) {
                        double[] eZL = new double[cumsum.length];
                        double[] eZR = new double[cumsum.length];
                        double[] scorei = new double[cumsum.length];
                        for (int m = 0; m < eZL.length; m++) {
                            eZL[m] = cumsum[m][k] / (k + 1);
                            eZR[m] = (cumsum[m][maxEnum] - cumsum[m][k]) / (maxEnum - k);
                            scorei[m] = (Math.pow(eZL[m], 2) * (k + 1) + Math.pow(eZR[m], 2) * (maxEnum - k)) / maxEnum;
                        }
                        int featureOpti = MathExt.maxIndex(scorei);
                        if (scorei[featureOpti] > scoreOpt * (1 + 1E-8)) {
                            partyOpt = i;
                            featureOpt = featureOpti;
                            percentileOpt = k;
                            scoreOpt = scorei[featureOpti];
                        }
                    }
                }
            }
            String isLeaf;
            if (scoreOpt > (squareyMean / Ys[0].length) * boosting) {
                isLeaf = "{\"is_leaf\": 0}";
            } else {
                partyOpt = -1;
                featureOpt = 0;
                percentileOpt = 0;
                scoreOpt = 0;
                isLeaf = "{\"is_leaf\": 1}";
            }
            values[n] = new Double[]{partyOpt, featureOpt, percentileOpt, scoreOpt};
            isLeafs[n] = isLeaf;
        });
        MultiOutputMessage responsePhase3 = DataUtils.prepareMultiOutputMessage(
                values,
                isLeafs,
                Y1s.length);
        DistributedRandomForestRes res = new DistributedRandomForestRes(client, DataUtils.outputMessage2json(responsePhase3), isActive,
                null, -1, req.getExtraInfo());
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
    public DistributedRandomForestRes trainPhase4(Message request) {
        DistributedRandomForestReq req = (DistributedRandomForestReq) request;
        DistributedRandomForestRes res;
        if ("".equals(req.getBody())) {
            // skip 什么都不用做
            res = new DistributedRandomForestRes(req.getClient(), "", isActive, req.getSampleId(), req.getTreeId());
        } else {
            Map<Integer, int[]> treeSampleMap = new HashMap<>();
            String[] exInfo = req.getExtraInfo().split("\\|\\|");
            String[] treeIds = exInfo[0].split("\\|");
            String[] sampleIds = exInfo[1].split("\\|");
            for (int i = 0; i < treeIds.length; i++) {
                int[] sampleIdi;
                if (isDistributed) {
                    int[] sampleIdiOri = Arrays.stream(sampleIds[i].substring(1, sampleIds[i].length() - 1).split(","))
                            .map(String::trim).mapToInt(Integer::parseInt).toArray();
                    //根据行映射关系,将总数据集的sampleId转化为worker加载的部分数据集的sampleId
                    sampleIdi = new int[sampleIdiOri.length];
                    for (int j = 0; j < sampleIdiOri.length; j++) {
                        sampleIdi[j] = sampleMap[Integer.parseInt(treeIds[i])].get(sampleIdiOri[j]);
                    }
                } else {
                    sampleIdi = Arrays.stream(sampleIds[i].substring(1, sampleIds[i].length() - 1).split(","))
                            .map(String::trim).mapToInt(Integer::parseInt).toArray();
                }
                treeSampleMap.put(Integer.parseInt(treeIds[i]), sampleIdi);
            }
            String[] body = req.getBody().split("\\|\\|");
            Matrix[][] matrices = new Matrix[body.length][1];
            Vector[][] vector = new Vector[body.length][1];
            treeIds = new String[body.length];
            sampleIds = new String[body.length];
            String[] mess = new String[body.length];
            String[] finalTreeIds = treeIds;
            String[] finalSampleIds = sampleIds;
            IntStream.range(0, body.length).parallel().forEach(i -> {
                String si = body[i];
                ObjectMapper mapper = new ObjectMapper();
                try {
                    Map<String, Double> tmp = mapper.readValue(si, Map.class);
                    int idx = (int) Math.round(tmp.get("treeId"));
                    finalTreeIds[i] = String.valueOf(idx);
                    finalSampleIds[i] = Arrays.toString(treeSampleMap.get(idx));
                    matrices[i][0] = DataUtils.toMatrix(DataUtils.selecRows(XsTrain[idx],
                            treeSampleMap.get(idx)));
                    //python内操作
                    double[][] matrix = new double[treeSampleMap.get(idx).length][XsTrain[idx].numCols()];
                    for (int j = 0; j < treeSampleMap.get(idx).length; j++) {
                        for (int k = 0; k < XsTrain[idx].numCols(); k++) {
                            matrix[j][k] = matrices[i][0].getRows(j).getValues(k);
                        }
                    }
                    double[][] originMatrix = matrix.clone();
                    Arrays.sort(matrix, Comparator.comparingDouble(a -> a[tmp.get("featureId").intValue()]));
                    double p1 = matrix.length * tmp.get("percentile") / 100.0;
                    int midFloor = (int) Math.floor(p1);
                    if (midFloor == 0) {
                        midFloor += 1;
                    }
                    if (midFloor == matrix.length - 1) {
                        midFloor -= 1;
                    }
                    double midValue = matrix[midFloor][tmp.get("featureId").intValue()];
                    SimpleMatrix mat1 = new SimpleMatrix(midFloor, matrix[0].length);
                    SimpleMatrix mat2 = new SimpleMatrix(matrix.length - midFloor, matrix[0].length);
                    SimpleMatrix vec = new SimpleMatrix(matrix.length, 1);
                    int mat1row = 0;
                    int mat2row = 0;
                    for (int j = 0; j < matrix.length; j++) {
                        double nowValue = originMatrix[j][tmp.get("featureId").intValue()];
                        if (nowValue <= midValue && mat1row < mat1.numRows()) {
                            for (int k = 0; k < matrix[0].length; k++) {
                                double val = originMatrix[j][k];
                                mat1.set(mat1row, k, val);
                            }
                            mat1row++;
                            vec.set(j, 0, 1);
                        } else {
                            for (int k = 0; k < matrix[0].length; k++) {
                                double val = originMatrix[j][k];
                                mat2.set(mat2row, k, val);
                            }
                            mat2row++;
                            vec.set(j, 0, 0);
                        }
                    }
                    vector[i][0] = DataUtils.toVector(vec);
                    int realFeatureId = featureIds[idx].get(tmp.get("featureId").intValue());
                    localTree.put("" + idx + "," + tmp.get("nodeId").intValue() + "," + realFeatureId , midValue);
                    mess[i] = "{\"is_leaf\": 0, \"feature_opt\": " + tmp.get("featureId").intValue() + ", \"value_opt\": " + "0" + "}";
//                    mess[i] = "{\"is_leaf\": 0, \"feature_opt\": " + tmp.get("featureId").intValue() + ", \"value_opt\": " + midValue + "}";
                } catch (JsonProcessingException e) {
                    logger.error("JsonProcessingException: ", e);
                }
            });

            // make response
            MultiOutputMessage responsePhase4 = DataUtils.prepareMultiOutputMessage(
                    vector,
                    mess,
                    body.length
            );
            exInfo = new String[2];
            exInfo[0] = String.join("|", treeIds);
            exInfo[1] = String.join("|", sampleIds);
            res = new DistributedRandomForestRes(req.getClient(),
                    DataUtils.outputMessage2json(responsePhase4),
                    isActive,
                    null,
                    -1,
                    String.join("||", exInfo));
        }
        return res;
    }

    public Message doValidation(Message request) {
        serialize();
        InferenceData data = CommonLoad.constructInference(AlgorithmType.RandomForestJava, validationData);
        RFInferenceData inferenceData = (RFInferenceData) data;
        inferenceData.init();
        inferenceData.fillna(0);
//        String[] subUid = inferencePhase1(inferenceData, request);
        String[] subUid = testId;
        if (subUid.length == 0) {
            return null;
        }
        if (labelName == null) {
            XTest = inferenceData.selectToSmpMatrix(subUid);
            inferenceUid = subUid;
        } else {
            XTest = inferenceData.selectToSmpMatrix(subUid,labelName);
            inferenceUid = subUid;
            validateLabel = inferenceData.getLabel(subUid,labelName);
        }

        return inferenceOneShot(-1, null);
    }

    public Message calculateValidationMetric(Message request) {
        if (request == null) {
            return new RandomforestMessage("");
        }
        RandomforestValidateReq req = (RandomforestValidateReq) request;


        double[] yPredMeanValid = req.getPred();
        double[] yLabelValid = validateLabel;

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
        String metricString = metricMap.keySet().parallelStream()
                .map(key -> key + "=" + metricMap.get(key))
                .collect(Collectors.joining(", ", "{", "}"));
        String responseStr =  metricString + "||" + metricArrRes;
        return new RandomforestMessage(responseStr);
    }

    public Message getBestRound(Message request) {
        DistributedRandomForestReq req = (DistributedRandomForestReq) request;
        if (request == null) {
            return new DistributedRandomForestRes(null, "", isActive);
        }
        if (req.getBestRound() != 0) {
            bestRound = req.getBestRound();
            serializedModel = serializedModels.get(bestRound);
            return new DistributedRandomForestRes(req.getClient(), "early stopping success", isActive);
        }
        return new DistributedRandomForestRes(req.getClient(), "", isActive);
    }

    /**
     * 执行phase99操作，序列化模型，计算最终性能指标
     *
     * @param request  服务端发送的请求
     * @return 客户端的响应结果
     */
    public Message updateModel(Message request) {
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

        Map<String, String> strTrees = serializedModel;
        int numTrees = Integer.parseInt(strTrees.get("numTrees"));
        for (int i = 0; i < numTrees; i++) {
            String singleTreeString = strTrees.get(String.format("Tree%s", i));
            ObjectMapper objectMapper = new ObjectMapper();
            TypeReference<HashMap<String, Map<String, String>>> typeReference
                    = new TypeReference<HashMap<String, Map<String, String>>>() {
            };
            try {
                Map<String, Map<String, String>> singleTreeMap = objectMapper.readValue(singleTreeString, typeReference);
                for (Map.Entry<String, Map<String, String>> keyi : singleTreeMap.entrySet()) {
                    Map<String, String> tmp = keyi.getValue();
                    if (!"1".equals(tmp.get("isLeaf"))) {
                        ObjectMapper mapperTemp = new ObjectMapper();
                        TypeReference<HashMap<String, String>> typeRefTemp
                                = new TypeReference<HashMap<String, String>>() {
                        };
                        try {
                            Map<String, String> tmp1 = mapperTemp.readValue(tmp.get("referenceJson"), typeRefTemp);
                            String splitStr = "" + i + "," + tmp.get("nodeId") + "," + tmp1.get("feature_opt");
                            tmp1.put("value_opt", localTree.get(splitStr).toString());
                            String resJson = mapperTemp.writeValueAsString(tmp1);
                            tmp.put("referenceJson", resJson);
                            keyi.setValue(tmp);
                        } catch (JsonProcessingException e) {
                            logger.error("JsonProcessingException: ", e);
                        }
                    }
                }
                String resTreeStr =  mapper.writeValueAsString(singleTreeMap);
                strTrees.put(String.format("Tree%s", i), resTreeStr);
            } catch (JsonProcessingException e) {
                logger.error("JsonProcessingException: ", e);
            }
        }
        serializedModel = strTrees;

        serializedModels.put(tmpRound, serializedModel);
        tmpRound++;
        logger.info("tmpround : " + tmpRound + " modelStrings " + serializedModels.size());
        responseStr = "finish";
        // give final metrics
        // TODO: Add acc, F1 and others for cross entropy

        return new RandomforestMessage(responseStr);
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
        if (bestRound != 0) {
            serializedModel = serializedModels.get(bestRound);
        }
        Map<String, String> strTrees = serializedModel;
        int numTrees = Integer.parseInt(strTrees.get("numTrees"));
//        int numTrees = Integer.parseInt(strTrees.get("numTrees"));
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
        int count = (int) Arrays.stream(yPredBagging).filter(x -> Math.abs(x - magicDefault) > EPSILON).count();
        double[][] validY = new double[2][count];
        validY[0] = Arrays.stream(yPredBagging).filter(x -> Math.abs(x - magicDefault) > EPSILON).toArray();
        validY[1] = IntStream.range(0, yPredBagging.length).filter(idx -> Math.abs(yPredBagging[idx] - magicDefault) > EPSILON)
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
        return AlgorithmType.DistributedRandomForest;
    }


    public PaillierKeyPublic getKeyPublic() {
        return keyPublic;
    }

    public void setKeyPublic(PaillierKeyPublic keyPublic) {
        this.keyPublic = keyPublic;
    }

    public String getPrivateKeyString() {
        return privateKeyString;
    }

    public void setValidationData(String[][] validationData) {
        this.validationData = validationData;
    }

    public void setPrivateKeyString(String privateKeyString) {
        this.privateKeyString = privateKeyString;
    }

    public void setTestId(String[] testId) {
        this.testId = testId;
    }

    public Map<String, Double> getLocalTree() {
        return localTree;
    }

    public void setLocalTree(Map<String, Double> localTree) {
        this.localTree = localTree;
    }
}

