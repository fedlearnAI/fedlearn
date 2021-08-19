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

import com.jdt.fedlearn.core.encryption.differentialPrivacy.Laplace;
import com.jdt.fedlearn.core.encryption.distributedPaillier.DistributedPaillier;
import com.jdt.fedlearn.core.encryption.distributedPaillier.DistributedPaillierNative;
import com.jdt.fedlearn.core.encryption.distributedPaillier.HomoEncryptionUtil;
import com.jdt.fedlearn.core.entity.ClientInfo;
import com.jdt.fedlearn.core.entity.Message;
import com.jdt.fedlearn.core.entity.common.InferenceInit;
import com.jdt.fedlearn.core.entity.feature.Features;
import com.jdt.fedlearn.core.entity.kernelLinearRegression.DataUtils;
import com.jdt.fedlearn.core.entity.kernelLinearRegression.InferenceReqAndRes;
import com.jdt.fedlearn.core.entity.kernelLinearRegression.TrainReq;
import com.jdt.fedlearn.core.entity.kernelLinearRegression.TrainRes;
import com.jdt.fedlearn.core.entity.mixedLinearRegression.CypherMessage2D;
import com.jdt.fedlearn.core.entity.mixedLinearRegression.CypherMessage2DList;
import com.jdt.fedlearn.core.exception.DeserializeException;
import com.jdt.fedlearn.core.loader.common.CommonInferenceData;
import com.jdt.fedlearn.core.loader.common.InferenceData;
import com.jdt.fedlearn.core.loader.common.TrainData;
import com.jdt.fedlearn.core.loader.kernelLinearRegression.KernelLinearRegressionTrainData;
import com.jdt.fedlearn.core.math.MathExt;
import com.jdt.fedlearn.core.math.Normalizer;
import com.jdt.fedlearn.core.math.NormalizerOutPackage;
import com.jdt.fedlearn.core.metrics.Metric;
import com.jdt.fedlearn.core.model.serialize.KernelJavaSerializer;
import com.jdt.fedlearn.core.model.serialize.SerializerUtils;
import com.jdt.fedlearn.core.parameter.KernelLinearRegressionParameter;
import com.jdt.fedlearn.core.parameter.SuperParameter;
import com.jdt.fedlearn.core.preprocess.InferenceFilter;
import com.jdt.fedlearn.core.type.*;
import com.jdt.fedlearn.core.type.data.Tuple2;
import com.jdt.fedlearn.core.util.Tool;
import com.jdt.fedlearn.grpc.federatedlearning.Vector;
import org.ejml.simple.SimpleMatrix;
import org.jblas.DoubleMatrix;
import org.jblas.Solve;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;


public class KernelLinearRegressionJavaModel implements Model {
    private static final Logger logger = LoggerFactory.getLogger(KernelLinearRegressionJavaModel.class);
    //算法参数
    private KernelLinearRegressionParameter parameter;
    private double mapdim;
    private double scale;
    private int numSample;
    private int batchSize;
    private double kernelType;
    private NormalizationType normalizationType = NormalizationType.NONE;
    private int numClass;
    // 数据特征
    private boolean isInitTrain = false;
    private boolean hasFeature = true;
    private boolean isActive = false;
    private int clientInd = 0;
    private List<ClientInfo> clientInfoList;
    private List<Integer> sampleIndex;
    private List<Integer> testUid;
    private SimpleMatrix xsTrain = null;
    private SimpleMatrix xsTrainTrans = null;
    private SimpleMatrix xsTrainTransSub = null;
    private SimpleMatrix xsTest = null;
    private SimpleMatrix xsTestTrans = null;
    private double[] yTrain;
    private double[] yTrainSub;
    private double[][] yTrainSubs;
    private String[] testId;
    private String[][] validationData;
    private double[] yValiSub;
    private double[][] yValiSubs;
    private int tmpRound = 1;
    // 变换参数
    private Vector bias;
    private SimpleMatrix transMat = null;
    private double[] bias1;
    private double[][] transMetric;
    private final Map<Integer, Vector[]> modelParasRounds = new HashMap<>();
    private double[] normParams1;
    private double[] normParams2;
    private Vector[] modelParas;
    // 指标更新
    private String modelToken;
    private Map<MetricType, List<Double>> metricMap;
    private Map<MetricType, List<Double>> metricMapVali;
    private Map<MetricType, List<Double[][]>> metricMapArr;
    private Map<MetricType, List<Double[][]>> metricMapArrVali;
    private List<Double> multiClassUniqueLabelList = new ArrayList<>();
    private int numClassRound = 0;
    private double[][] predicts;
    // 安全推理
    private boolean useDistributedPillar = false;
    private boolean useFakeDec = false;
    private HomoEncryptionUtil pheKeys;
    private DistributedPaillierNative.signedByteArray[][] decPartialSum;
    private DistributedPaillierNative.signedByteArray[][] partialSum;


    public KernelLinearRegressionJavaModel() {
    }

    /**
     * 训练初始化
     *
     * @param rawData  原始数据
     * @param sp       算法参数
     * @param features 特征
     * @param others   其他参数
     * @return 训练数据
     */
    public KernelLinearRegressionTrainData trainInit(String[][] rawData, String[] uids, int[] testIndex, SuperParameter sp, Features features, Map<String, Object> others) {
        this.parameter = (KernelLinearRegressionParameter) sp;
        numClass = parameter.getNumClass();
        Tuple2<String[], String[]> trainTestUId = Tool.splitUid(uids, testIndex);
        KernelLinearRegressionTrainData trainData = new KernelLinearRegressionTrainData(rawData, trainTestUId._1(), features);
        extractedValidationData(rawData, testIndex, trainTestUId);
        normalization(trainData);
        kernelType = parameter.getKernelType();
        mapdim = parameter.getMapdim();
        scale = parameter.getScale();
        sampleIndex = (List<Integer>) others.get("sampleIndex");
        clientInd = (int) others.get("clientInd");
        clientInfoList = (List<ClientInfo>) others.get("clientInfoList");
        testUid = (List<Integer>) others.get("testUid");
        initTrainData(trainData);
        //TODO batchSize = numsample
        numSample = xsTrain.numRows();
        modelParas = new Vector[numClass];
        predicts = new double[numClass][numSample];
        yTrainSubs = new double[numClass][numSample];
        yValiSubs = new double[numClass][testId.length];
        //Math.min(numsample, parameter.getBatchSize());
        batchSize = numSample;
        logger.info("Finish initializing training data.");
        if (trainData.hasLabel) {
            isActive = true;
            yTrain = trainData.getLabel();
            IntStream.range(0, numClass).forEach(x -> yTrainSubs[x] = yTrain);
            if (numClass > 1) {
                multiLabelTransform();
            }
            yValiSub = Tool.str2double(MathExt.transpose(validationData)[validationData[0].length - 1]);
            IntStream.range(0, numClass).forEach(x -> yValiSubs[x] = yValiSub);
            validationData = loadFea(validationData);
            initMetrics();
        }
        return trainData;
    }


    /**
     * 初始化训练指标：包括训练，验证的一维/二维指标
     */
    private void initMetrics() {
        double initSumLoss = -Double.MAX_VALUE;
        Double[][] initSumLossArr = new Double[1][];
        initSumLossArr[0] = new Double[]{-Double.MAX_VALUE, -Double.MAX_VALUE};
        List<Double> tmpRoundMetric = new ArrayList<>();
        tmpRoundMetric.add(initSumLoss);
        //TODO metricType 常量化
        String[] arr = MetricType.getArrayMetrics();
        List<Double[][]> tmpRoundMetricArr = new ArrayList<>();
        tmpRoundMetricArr.add(initSumLossArr);
        metricMap = Arrays.stream(parameter.getMetricType()).filter(x -> !Arrays.asList(arr).contains(x.getMetric()))
                .collect(Collectors.toMap(metric -> metric, metric -> new ArrayList<>(tmpRoundMetric)));
        metricMapArr = Arrays.stream(parameter.getMetricType()).filter(x -> Arrays.asList(arr).contains(x.getMetric()))
                .collect(Collectors.toMap(metric -> metric, metric -> new ArrayList<>(tmpRoundMetricArr)));
        metricMapVali = Arrays.stream(parameter.getMetricType()).filter(x -> !Arrays.asList(arr).contains(x.getMetric()))
                .collect(Collectors.toMap(metric -> metric, metric -> new ArrayList<>(tmpRoundMetric)));
        metricMapArrVali = Arrays.stream(parameter.getMetricType()).filter(x -> Arrays.asList(arr).contains(x.getMetric()))
                .collect(Collectors.toMap(metric -> metric, metric -> new ArrayList<>(tmpRoundMetricArr)));
    }

    /**
     * 从全部数据提取验证数据
     *
     * @param rawData      全部二维数据
     * @param testIndex    测试uid的index
     * @param trainTestUId （训练uid，验证uid）
     */
    private void extractedValidationData(String[][] rawData, int[] testIndex, Tuple2<String[], String[]> trainTestUId) {
        if (testIndex.length == 0) {
            testId = trainTestUId._1();
        }
        testId = trainTestUId._2();
        validationData = new String[testId.length + 1][rawData[0].length];
        validationData[0] = rawData[0];
        for (int i = 0; i < testId.length; i++) {
            for (String[] rawDatum : rawData) {
                if (rawDatum[0].equals(testId[i])) {
                    validationData[i + 1] = rawDatum;
                }
            }
        }
    }


    /**
     * 获取部分训练数据标签
     *
     * @param labelset    全部标签
     * @param sampleIndex 需要获取的样本索引
     * @param batchSize   批次大小
     * @return 部分训练数据的标签
     */
    private double[] getSubTrainLabel(double[] labelset, List<Integer> sampleIndex, int batchSize) {
        yTrainSub = new double[batchSize];
        IntStream.range(0, batchSize).forEach(x -> yTrainSub[x] = labelset[sampleIndex.get(x)]);
        logger.info("Training label size " + labelset.length + ", sampleIndex size is " + sampleIndex.size());
        return yTrainSub;
    }


    /**
     * 获取部分训练数据的特征
     *
     * @param alldata     全部数据
     * @param sampleIndex 需要获取的样本索引
     * @param batchSize   批次大小
     * @return 部分训练数据的特征
     */
    private SimpleMatrix getSubTrainFeat(SimpleMatrix alldata, List<Integer> sampleIndex, int batchSize) {
        int feadim = alldata.numCols();
        xsTrainTransSub = new SimpleMatrix(batchSize, feadim);
        for (int i = 0; i < batchSize; i++) {
            int ind = sampleIndex.get(i);
            for (int j = 0; j < feadim; j++) {
                double value = alldata.get(ind, j);
                xsTrainTransSub.set(i, j, value);
            }
        }
        return xsTrainTransSub;
    }

    /**
     * 将部分数据的matrix的值赋给全部数据的matrix
     *
     * @param bigmat   全部数据matrix
     * @param smallmat 部分数据matrix
     * @param start    开始
     * @param end      结束
     * @return 转换之后的matrix
     */
    private SimpleMatrix setSmpMatValue(SimpleMatrix bigmat, SimpleMatrix smallmat, int start, int end) {
        int featdim = bigmat.numCols();
        for (int i = start; i < end; i++) {
            for (int j = 0; j < featdim; j++) {
                bigmat.set(i, j, smallmat.get(i - start, j));
            }
        }
        return bigmat;
    }

    /**
     * 多分类标签映射关系
     */
    private void multiLabelTransform() {
        multiClassUniqueLabelList = Arrays.stream(yTrain).distinct().boxed().collect(Collectors.toList());
        int missingLabelNum = parameter.getNumClass() - multiClassUniqueLabelList.size();
        double startUniqueValue = multiClassUniqueLabelList.stream().max(Double::compareTo).get() + 1;
        for (int i = 0; i < missingLabelNum; i++) {
            multiClassUniqueLabelList.add(startUniqueValue + i);
        }
    }

    /**
     * 多分类部分训练数据标签转换
     */
    private void transTrainSub() {
        //y是否等于numcloassround的类别，相等为1，否则为0
        this.yTrainSubs[numClassRound] = Arrays.stream(yTrainSub).map(l -> (l == multiClassUniqueLabelList.get(numClassRound)) ? 1 : 0).toArray();
    }

    /**
     * 加载特征
     *
     * @param rawTable 二维数据
     * @return 特征数据
     */
    private String[][] loadFea(String[][] rawTable) {
        String[][] res = new String[rawTable.length][rawTable[0].length - 1];
        for (int i = 0; i < rawTable.length; i++) {
            if (rawTable[0].length - 1 >= 0) {
                System.arraycopy(rawTable[i], 0, res[i], 0, rawTable[0].length - 1);
            }
        }
        return res;
    }

    /**
     * 训练数据初始化
     *
     * @param trainData 训练数据
     */
    private void initTrainData(KernelLinearRegressionTrainData trainData) {
        logger.info("Start initializing training data.");
        if (trainData.getFeatureDim() > 0) {
            xsTrain = new SimpleMatrix(trainData.getFeature());
        }
        //TODO feature维度为0时填的0，可以不计算直接返回
        else if (trainData.getFeatureDim() == 0) {
            xsTrain = DataUtils.toSmpMatrix(DataUtils.zeroMatrix(trainData.getDatasetSize(), 1));
            hasFeature = false;
        }
    }

    /**
     * 数据归一化并保存归一化系数
     *
     * @param trainData 训练数据
     */
    private void normalization(KernelLinearRegressionTrainData trainData) {
        this.normalizationType = parameter.getNormalizationType();
        NormalizerOutPackage tempNormalizedOut;
        boolean couldDoNormalization = trainData.getFeatureDim() > 0;
        if (couldDoNormalization) {
            switch (this.normalizationType) {
                case MINMAX:
                    tempNormalizedOut = Normalizer.MinMaxScaler(trainData.getFeature());
                    logger.info("Debug, after MinMaxScaler normalization, one row of getFeature: " + Arrays.toString(trainData.getFeature()[0]));
                    normParams1 = tempNormalizedOut.params1.clone();
                    normParams2 = tempNormalizedOut.params2.clone();
                    break;
                case STANDARD:
                    tempNormalizedOut = Normalizer.StandardScaler(trainData.getFeature());
                    logger.info("Debug, after StandardScaler normalization, one row of getFeature: " + Arrays.toString(trainData.getFeature()[0]));
                    normParams1 = tempNormalizedOut.params1.clone();
                    normParams2 = tempNormalizedOut.params2.clone();
                    break;
                default:
                    logger.info("In trainInit of KernelLinearRegressionModel, do NO normalization.");
                    logger.info("Debug, after normalization, one row of getFeature: " + Arrays.toString(trainData.getFeature()[0]));
                    normParams1 = new double[trainData.getFeatureDim()];
                    Arrays.fill(normParams1, -1.0);
                    normParams2 = new double[trainData.getFeatureDim()];
                    Arrays.fill(normParams2, -1.0);
                    break;
            }
        } else {
            logger.info("In trainInit of KernelLinearRegressionModel, feature_dim does not support for normalization.");
            normParams1 = new double[2];
            Arrays.fill(normParams1, -1.0);
            normParams2 = new double[2];
            Arrays.fill(normParams2, -1.0);
        }
    }

    /**
     * 核方法训练过程：7个步骤，分训练和验证两个部分；
     * 训练：步骤1：对数据进行变换，计算本地的w*x;
     * 步骤2：获取predict-y，求解w，并更新训练指标；
     * 验证：步骤3：验证初始化；
     * 步骤4：验证数据做变换；
     * 步骤6：各客户端计算本地的predict；
     * 步骤7：更新验证指标；
     *
     * @param phase    训练阶段
     * @param jsonData 训练中间结果
     * @param train    训练数据
     * @return 训练中间结果
     */
    public Message train(int phase, Message jsonData, TrainData train) {
        logger.info(String.format("Traing process, phase %s start", phase));
        if (jsonData == null) {
            return null;
        }
        switch (KernelJavaModelPhaseType.valueOf(phase)) {
            case COMPUTE_LOCAL_PREDICT:
                return computeLocalPredict((TrainReq) jsonData);
            case UPDATE_TRAIN_INFO:
                return trainPhase2((TrainReq) jsonData);
            case VALIDATE_INIT:
                return validInit(testId, validationData);
            case VALIDATE_NORMALIZATION:
                String[][] validationClone = validationData.clone();
                CommonInferenceData validationTest = new CommonInferenceData(validationClone, "uid", null);
                return inference1(phase, (InferenceInit) jsonData, validationTest);
            case VALIDATE_TRANS_DATA:
                inference2(xsTest.numRows(), xsTest.numRows(), 1);
                return null;
            case VALIDATE_RESULT:
                return validateResult(jsonData, xsTest.numRows(), xsTest.numRows(), 1);
            case VALIDATE_UPDATE_METRIC:
                return updateValidationMetric(jsonData);
            default:
                throw new UnsupportedOperationException("unsupported phase in kernel model");
        }
    }


    /**
     * 初始核近似：生成核近似变换矩阵，并完成转换
     *
     * @param featureTrain 训练数据特征
     * @return 转换之后的矩阵
     */
    public SimpleMatrix initKernelApproximation(SimpleMatrix featureTrain) {
        if (kernelType != 3) {
            return new SimpleMatrix(new double[0][]);
        }
        int num = featureTrain.numRows();
        int col = featureTrain.numCols();
        transMetric = MathExt.generateNormal(col, (int) mapdim, scale);
        bias1 = MathExt.generateUniform((int) mapdim);
        double[][] mulity = new double[num][transMetric[0].length];
        for (int i = 0; i < mulity.length; i++) {
            for (int j = 0; j < transMetric[0].length; j++) {
                for (int k = 0; k < col; k++) {
                    mulity[i][j] += featureTrain.get(i, k) * transMetric[k][j];
                }
                mulity[i][j] = Math.cos(mulity[i][j] + bias1[j]) * Math.sqrt(2) / Math.sqrt((int) mapdim);
            }
        }
        return new SimpleMatrix(mulity);
    }

    /**
     * 训练数据核近似转换
     *
     * @param simpleMatrix 原始特征
     * @param transMat     转换矩阵
     * @param bias         bias
     * @return 转换后的特征
     */
    public SimpleMatrix kernelApproximation(SimpleMatrix simpleMatrix, SimpleMatrix transMat, Vector bias) {
        double[][] multiRes = MathExt.matrixMul(simpleMatrix, transMat);
        double[][] res = new double[multiRes.length][multiRes[0].length];
        for (int i = 0; i < res.length; i++) {
            for (int j = 0; j < res[i].length; j++) {
                res[i][j] = (Math.sqrt(2.) / Math.sqrt(bias.getValuesCount())) * Math.cos(multiRes[i][j] + bias.getValues(j));
            }
        }
        return new SimpleMatrix(res);
    }


    /**
     * 训练第一步，计算本地预测值：分是否是第一次两种情况
     * 第一次：数据需要做核变换，然后计算本地预测值 w*x
     * 非第一次：计算本地预测值：w*x
     *
     * @param trainReq 训练请求
     * @return 计算结果
     */
    private Message computeLocalPredict(TrainReq trainReq) {
        Message res = null;
        ClientInfo client = trainReq.getClient();
//        clientInd = trainReq.getClientInd();
        //初始化
        if (!isInitTrain) {
            logger.info("Finish the initialization process.");
            if (hasFeature) {
                res = fromInitTrain(trainReq, client);
            } else {
                res = fromInitNoFea(trainReq, client);
            }
            isInitTrain = true;
        } else {
            if (trainReq.getBestRound() != 0) {
                int bestRound = trainReq.getBestRound();
                modelParas = modelParasRounds.get(bestRound);
                res = new TrainRes(client, parameter.getMaxIter() + 1, KernelDispatchJavaPhaseType.COMPUTE_LOSS);
                return res;
            }
//            List<Integer> sampleIndex = trainReq.getSampleIndex();
            if (hasFeature) {
                numClassRound = trainReq.getNumClassRound();
                xsTrainTransSub = getSubTrainFeat(xsTrainTrans, sampleIndex, batchSize);
                SimpleMatrix inner_prod = xsTrainTransSub.mult(DataUtils.toSmpMatrix(modelParas[numClassRound])).scale(-1);
                if (isActive) {
                    //todo trans y
                    yTrainSub = getSubTrainLabel(yTrain, sampleIndex, batchSize);
                    if (numClass > 1) {
                        transTrainSub();
                    }
                    inner_prod = inner_prod.plus(DataUtils.toSmpMatrix(DataUtils.arrayToVector(yTrainSubs[numClassRound])));
                }
                predicts[numClassRound] = DataUtils.vectorToArray(DataUtils.toVector(inner_prod)).clone();
                res = new TrainRes(client, predicts, 0.0, isActive, clientInd, numClassRound, clientInfoList, KernelDispatchJavaPhaseType.COMPUTE_LOSS);
                // todo erlystopping
            } else {
                modelParas[numClassRound] = DataUtils.allzeroVector((int) mapdim);
                SimpleMatrix inner_prod = DataUtils.toSmpMatrix(DataUtils.allzeroVector(batchSize));
                double para_norm = 0;
                if (isActive) {
                    //todo trans y
                    yTrainSub = getSubTrainLabel(yTrain, sampleIndex, batchSize);
                    if (numClass > 1) {
                        transTrainSub();
                    }
                    inner_prod = inner_prod.plus(DataUtils.toSmpMatrix(DataUtils.arrayToVector(yTrainSubs[numClassRound])));
                }
                predicts[numClassRound] = DataUtils.vectorToArray(DataUtils.toVector(inner_prod)).clone();
                res = new TrainRes(client, predicts, para_norm, isActive, clientInd, numClassRound, clientInfoList, KernelDispatchJavaPhaseType.COMPUTE_LOSS);
            }
        }
        logger.info("Phase 1 finish.");
        return res;
    }


    /**
     * 无特征时生成全零的矩阵
     *
     * @param req    训练请求
     * @param client 客户端信息
     * @return 客户端处理结果
     */
    private TrainRes fromInitNoFea(TrainReq req, ClientInfo client) {
        TrainRes res;
        int numSample = xsTrain.numRows();
        IntStream.range(0, numClass).forEach(x -> modelParas[x] = DataUtils.allzeroVector((int) mapdim));
        xsTrainTrans = DataUtils.toSmpMatrix(DataUtils.zeroMatrix(numSample, (int) mapdim));
        transMat = DataUtils.toSmpMatrix(DataUtils.zeroMatrix(numSample, (int) mapdim));
        bias = DataUtils.allzeroVector((int) mapdim);
//        List<Integer> sampleIndex = req.getSampleIndex();
        xsTrainTransSub = getSubTrainFeat(xsTrainTrans, sampleIndex, batchSize);
        SimpleMatrix innerProd = DataUtils.toSmpMatrix(DataUtils.allzeroVector(batchSize)).scale(-1);
        double para_norm = Math.pow(innerProd.normF(), 2);
        if (isActive) {
            yTrainSub = getSubTrainLabel(yTrain, sampleIndex, batchSize);
            if (numClass > 1) {
                transTrainSub();
            }
            innerProd = innerProd.plus(DataUtils.toSmpMatrix(DataUtils.arrayToVector(yTrainSubs[numClassRound])));
        }
        predicts[numClassRound] = DataUtils.vectorToArray(DataUtils.toVector(innerProd));
        res = new TrainRes(client, predicts, para_norm, isActive, clientInd, numClassRound, clientInfoList, KernelDispatchJavaPhaseType.COMPUTE_LOSS);
        return res;
    }

    /**
     * 有特征时处理
     *
     * @param req    训练请求
     * @param client 客户端信息
     * @return 客户端计算结果
     */
    private TrainRes fromInitTrain(TrainReq req, ClientInfo client) {
        TrainRes res;
        logger.info(String.format("Training data shape %s, %s", xsTrain.numCols(), xsTrain.numRows()));
        logger.info(String.format("Kernel type, map dim %s, %s, %s", kernelType, mapdim, scale));
        logger.info("Start kernel mapping.");
        int numsample = xsTrain.numRows();
        int batchSize = numsample;
        int numbatch = (int) Math.ceil((double) numsample / batchSize);
        xsTrainTrans = new SimpleMatrix(numsample, (int) mapdim);
        logger.info(String.format("Partitioning the training sample into %s blocks", numbatch));
        for (int i = 0; i < numbatch; i++) {
            int start = i * batchSize;
            int end = Math.min(numsample, (i + 1) * batchSize);
            SimpleMatrix temp = xsTrain.rows(start, end);
            if (i == 0) {
                xsTrainTransSub = initKernelApproximation(temp);
                transMat = new SimpleMatrix(transMetric);
                bias = DataUtils.arrayToVector(bias1);
            } else {
                xsTrainTransSub = kernelApproximation(temp, transMat, bias);
            }
            setSmpMatValue(xsTrainTrans, xsTrainTransSub, start, end);
        }
//        List<Integer> sampleIndex = req.getSampleIndex();
        logger.info("batchSize:" + batchSize);
        xsTrainTransSub = getSubTrainFeat(xsTrainTrans, sampleIndex, batchSize);
        IntStream.range(0, numClass).forEach(x -> modelParas[x] = DataUtils.alloneVector((int) mapdim, 0.1));
        SimpleMatrix innerProd = xsTrainTransSub.mult(DataUtils.toSmpMatrix(modelParas[numClassRound])).scale(-1);
        //TODO 差分隐私
        double noise = Laplace.laplaceMechanismNoise(Math.max(1, MathExt.max(DataUtils.vectorToArray(DataUtils.toVector(innerProd)))) / xsTrainTransSub.numRows(), parameter.getDifferentialPrivacy());
        double[] noiseArray = new double[xsTrainTransSub.numRows()];
        Arrays.fill(noiseArray, noise);
        innerProd = innerProd.plus(DataUtils.toSmpMatrix(DataUtils.arrayToVector(noiseArray)));
        double para_norm = Math.pow(innerProd.normF(), 2);
        if (isActive) {
            yTrainSub = getSubTrainLabel(yTrain, sampleIndex, batchSize);
            if (numClass > 1) {
                transTrainSub();
            }
            innerProd = innerProd.plus(DataUtils.toSmpMatrix(DataUtils.arrayToVector(yTrainSubs[numClassRound])));
        }
        double[] predArray = DataUtils.vectorToArray(DataUtils.toVector(innerProd));
        predicts[numClassRound] = predArray.clone();
        //TODO return double[numclass][samplenum] preds
        res = new TrainRes(client, predicts, para_norm, isActive, clientInd, numClassRound, clientInfoList, KernelDispatchJavaPhaseType.COMPUTE_LOSS);
        return res;
    }

    private static double clip(double val) {
        if (val < 0.00001) {
            return 0.0001;
        }
        return Math.min(val, 0.99999);
    }

    /**
     * 变换推理结果：
     * 类别为1时，直接返回；类别大于2时，将预测值映射到（0，1）区间，并使得各样本预测值之和为1
     *
     * @param data     数据
     * @param numClass 类别数据量
     * @return 变化之后的结果
     */
    public static double[][] predictTrans(double[][] data, int numClass) {
        double[][] res = new double[data.length][data[0].length];
        if (numClass == 1 || data[0].length == 1) {
            return data;
        }
        for (int i = 0; i < data.length; i++) {
            for (int j = 0; j < data[0].length; j++) {
                res[i][j] = clip(data[i][j]);
            }
        }
        Arrays.stream(res).forEach(predValues -> {
            double sum = Arrays.stream(predValues).sum();
            IntStream.range(0, numClass).forEach(i -> predValues[i] = predValues[i] / sum);
        });
        return res;
    }

    /**
     * @param predict 训练中间结果
     * @return 转换之后的结果
     */
    public double[] transform(double[][] predict) {
        double[][] values = MathExt.transpose(predict);
        Arrays.stream(values).forEach(predValues -> {
            double sum = Arrays.stream(predValues).sum();
            IntStream.range(0, numClass).forEach(i -> predValues[i] = predValues[i] / sum);
        });
        return Arrays.stream(MathExt.transpose(values)).flatMapToDouble(Arrays::stream).toArray();
    }

    /**
     * 训练第二步：主动方更新metric、更新的客户端更新w
     *
     * @param jsonData 请求数据
     * @return 更新后的指标
     */
    private TrainRes trainPhase2(TrainReq jsonData) {
        ClientInfo client = jsonData.getClient();
        clientInd = jsonData.getClientInd();
        TrainRes res;
        KernelDispatchJavaPhaseType kernelDispatchJavaPhaseType = KernelDispatchJavaPhaseType.UPDATE_METRIC;
        ClientInfo clientInfo = jsonData.getClient();
        boolean isUpdate = jsonData.isUpdate();
//        List<Integer> sampleIndex = jsonData.getSampleIndex();
        SimpleMatrix tmp = null;
        SimpleMatrix innerProd = null;
        if (isActive) {
            double[][] actualPredict = new double[numClass][yTrainSub.length];
            double[][] predictGaps = jsonData.getValuelists();
            for (int i = 0; i < numClass; i++) {
                int finalI = i;
                actualPredict[i] = IntStream.range(0, predictGaps[i].length).mapToDouble(p -> (predictGaps[finalI][p] - yTrainSubs[finalI][p]) * (-1)).toArray();
            }
            if (numClassRound + 1 == parameter.getNumClass()) {
                kernelDispatchJavaPhaseType = KernelDispatchJavaPhaseType.VALIDATION_INIT;
                double[] predictTrans = Arrays.stream(actualPredict).flatMapToDouble(Arrays::stream).toArray();
                if (numClass > 1) {
                    if (numClass == 2) {
                        predictTrans = actualPredict[1];
                    } else {
                        predictTrans = transform(actualPredict);
                    }
                    predictTrans = Arrays.stream(predictTrans).map(KernelLinearRegressionJavaModel::clip).toArray();
                    this.yTrainSub = Arrays.stream(yTrain).map(l -> multiClassUniqueLabelList.indexOf(l)).toArray();
                }
                updateMetric(predictTrans, yTrainSub);
                tmp = DataUtils.toSmpMatrix(DataUtils.arrayToVector(jsonData.getValuelists()[numClassRound]));
                innerProd = xsTrainTransSub.mult(DataUtils.toSmpMatrix(modelParas[numClassRound]));
            } else {
                tmp = DataUtils.toSmpMatrix(DataUtils.arrayToVector(jsonData.getValuelists()[numClassRound]));
                innerProd = xsTrainTransSub.mult(DataUtils.toSmpMatrix(modelParas[numClassRound]));
                numClassRound++;
            }
        }
        if (isUpdate) {
            if (!isActive) {
                tmp = DataUtils.toSmpMatrix(DataUtils.arrayToVector(jsonData.getValuelists()[numClassRound]));
                innerProd = xsTrainTransSub.mult(DataUtils.toSmpMatrix(modelParas[numClassRound]));
            }
            Vector y = DataUtils.toVector(innerProd.plus(tmp));
            logger.info(String.format("%s update model parameter!", clientInfo));
            logger.info(String.format("bath size is %s", batchSize));
            if (hasFeature) {
                xsTrainTransSub = getSubTrainFeat(xsTrainTrans, sampleIndex, batchSize);
                double[][] Xs_train_trans_sub_doub = DataUtils.smpmatrixToArray(xsTrainTransSub);
                double[] Xs_train_trans_sub_doub_trans = MathExt.trans2DtoArray(Xs_train_trans_sub_doub);
                DoubleMatrix doubleMatrix = new DoubleMatrix(Xs_train_trans_sub_doub.length, Xs_train_trans_sub_doub[0].length, Xs_train_trans_sub_doub_trans);
                double[] arrayY = DataUtils.vectorToArray(y);
                DoubleMatrix doubleMatrixY = new DoubleMatrix(arrayY.length, 1, arrayY);
                DoubleMatrix result = Solve.solveLeastSquares(doubleMatrix, doubleMatrixY);
                double[] resArray = result.toArray();
                if (isActive && numClassRound > 0) {
                    modelParas[numClassRound - 1] = DataUtils.arrayToVector(resArray);
                } else {
                    modelParas[numClassRound] = DataUtils.arrayToVector(resArray);
                }
            } else {
                if (isActive && numClassRound > 0) {
                    modelParas[numClassRound - 1] = DataUtils.allzeroVector((int) mapdim);
                } else if (isActive && numClassRound == 0) {
                    modelParas[numClass - 1] = DataUtils.allzeroVector((int) mapdim);
                } else {
                    modelParas[numClassRound] = DataUtils.allzeroVector((int) mapdim);
                }
            }
            res = new TrainRes(client, numClassRound, isActive, kernelDispatchJavaPhaseType);
            logger.info("Phase 2 finish.");
        } else {
            res = new TrainRes(client, numClassRound, isActive, kernelDispatchJavaPhaseType);
        }
        return res;
    }


    /**
     * 更新训练指标
     *
     * @param predict 预测值
     * @param label   真实值
     */
    private void updateMetric(double[] predict, double[] label) {
        if (!this.isActive || (metricMap == null && metricMapArr == null)) {
            return;
        }
        Map<MetricType, Double> trainMetric = new HashMap<>();
        Map<MetricType, Double[][]> trainMetricArr = new HashMap<>();
        MetricType[] evalMetric = parameter.getMetricType();
        String[] arr = MetricType.getArrayMetrics();
        for (MetricType t : evalMetric) {
            if (Arrays.asList(arr).contains(t.getMetric())) {
                trainMetricArr.put(t, Metric.calculateMetricArr(t, predict, label, multiClassUniqueLabelList));
            } else {
                trainMetric.put(t, Metric.calculateMetric(t, predict, label));
            }
        }
        trainMetric.forEach((key, value) -> metricMap.get(key).add(value));
        trainMetricArr.forEach((key, value) -> metricMapArr.get(key).add(value));
    }


    /**
     * 验证初始化
     *
     * @param uidList        需要验证的uid
     * @param validationData 验证数据
     * @return 过滤结果
     */
    public Message validInit(String[] uidList, String[][] validationData) {
        return InferenceFilter.filter(uidList, validationData);
    }


    /**
     * 验证阶段：对验证数据进行核变换并计算验证结果
     *
     * @param jsonData  中间数据
     * @param numSample 样本量
     * @param batchSize 批次大小
     * @param numBatch  批次
     * @return
     */
    private Message validateResult(Message jsonData, int numSample, int batchSize, int numBatch) {
        Message res = null;
        if (jsonData instanceof InferenceReqAndRes) {
            InferenceReqAndRes req = (InferenceReqAndRes) jsonData;
            ClientInfo client = req.getClient();
            //todo inference
            double[] predict = new double[numSample];
            double[][] results = new double[numSample][numClass];
            if (hasFeature) {
                for (int i = 0; i < numBatch; i++) {
                    int start = i * batchSize;
                    int end = Math.min(numSample, (i + 1) * batchSize);
                    SimpleMatrix temp = xsTestTrans.rows(start, end);
                    for (int m = 0; m < modelParas.length; m++) {
                        Vector innerProd = computePredict(temp, modelParas[m]);
                        for (int j = start; j < end; j++) {
                            results[j][m] = innerProd.getValues(j - start);
                            predict[j] = innerProd.getValues(j - start);
                        }
                    }
                }
            }
            logger.info("Inner product result");
            res = new InferenceReqAndRes(client, predict, results, numClass - 1, isActive, numClass, testUid, KernelDispatchJavaPhaseType.VALIDATION_RESULT);
        }
        return res;
    }

    /**
     * 更新验证指标
     *
     * @param jsonData 请求数据
     * @return 更新之后的指标
     */
    private TrainRes updateValidationMetric(Message jsonData) {
        TrainReq trainReq = (TrainReq) jsonData;
        TrainRes res;
        modelParasRounds.put(tmpRound, modelParas);
        tmpRound++;
        if (!isActive) {
            res = new TrainRes(trainReq.getClient(), numClassRound, isActive, KernelDispatchJavaPhaseType.UPDATE_METRIC);
        } else {
            double[][] predRes = trainReq.getPredictRes().getPredicts();
            double[][] predResTrans = MathExt.transpose(predRes);
            double[] predTrans = Arrays.stream(predResTrans).flatMapToDouble(Arrays::stream).toArray();
            if (numClass > 1) {
                if (numClass == 2) {
                    predTrans = predResTrans[1];
                } else {
                    predTrans = transform(predResTrans);
                    this.yValiSub = Arrays.stream(yValiSub).map(l -> multiClassUniqueLabelList.indexOf(l)).toArray();
                }
                predTrans = Arrays.stream(predTrans).map(KernelLinearRegressionJavaModel::clip).toArray();
            }
            updateMetricValidation(predTrans, yValiSub);
            numClassRound = 0;
            res = new TrainRes(trainReq.getClient(), numClassRound, isActive, metricMap, metricMapArr, metricMapVali, metricMapArrVali, KernelDispatchJavaPhaseType.UPDATE_METRIC);
            //TODO
        }
        return res;
    }

    /**
     * 更新指标
     *
     * @param predict 预测值
     * @param label   真实值
     */
    private void updateMetricValidation(double[] predict, double[] label) {
        if (!this.isActive || (metricMapVali == null && metricMapArrVali == null)) {
            return;
        }
        Map<MetricType, Double> trainMetric = new HashMap<>();
        Map<MetricType, Double[][]> trainMetricArr = new HashMap<>();
        MetricType[] evalMetric = parameter.getMetricType();
        String[] arr = MetricType.getArrayMetrics();
        for (MetricType t : evalMetric) {
            if (Arrays.asList(arr).contains(t.getMetric())) {
                trainMetricArr.put(t, Metric.calculateMetricArr(t, predict, label, multiClassUniqueLabelList));
            } else {
                trainMetric.put(t, Metric.calculateMetric(t, predict, label));
            }
        }
        trainMetric.forEach((key, value) -> metricMapVali.get(key).add(value));
        trainMetricArr.forEach((key, value) -> metricMapArrVali.get(key).add(value));
    }

    /**
     * 推理初始化
     *
     * @param uidList       需要推理的uid
     * @param inferenceData 推理数据
     * @param others        自定义参数，安全推理时包括安全推理的信息
     * @return 过滤结果，安全推理时完成安全推理秘钥初始化等
     */
    public Message inferenceInit(String[] uidList, String[][] inferenceData, Map<String, Object> others) {
        if (others.containsKey("pubKeyStr") && others.containsKey("privKeyStr")) {
            String pubKeyStr = others.get("pubKeyStr").toString();
            String privKeyStr = others.get("privKeyStr").toString();
            useDistributedPillar = true;
            int numP = (int) others.get("numP");
            int encBits = (int) others.get("ENC_BITS");
            // Standalone version, master generates Keys, clients receive.
            //是否时debug模式
            this.pheKeys = new HomoEncryptionUtil(numP, encBits, useFakeDec);
            int thisPartyID = (Integer) others.get("thisPartyID");
            DistributedPaillier.DistPaillierPubkey pubkey = new DistributedPaillier.DistPaillierPubkey();
            DistributedPaillier.DistPaillierPrivkey privkey = new DistributedPaillier.DistPaillierPrivkey();
            pubkey.parseJson(pubKeyStr);
            privkey.parseJson(privKeyStr);
            if (!useFakeDec) {
                this.pheKeys.setPk(pubkey);
                this.pheKeys.setSk(privkey);
                this.pheKeys.getSk().setRank(thisPartyID);
            }
        }
        return InferenceFilter.filter(uidList, inferenceData);
    }

    /**
     * 推理阶段：
     * 推理1：推理数据归一化；
     * 推理2：推理数据核变换；
     * 推理3：本地推理，安全推理时需完成加密，解密部分推理结果，解密最终推理结果。
     *
     * @param phase    阶段
     * @param jsonData 中间数据
     * @param data     推理数据
     * @return 各推理阶段计算结果
     */
    public Message inference(int phase, Message jsonData, InferenceData data) {
        logger.info(String.format("Inference process, phase %s start", phase));
        if (jsonData == null) {
            return null;
        }
        CommonInferenceData inferenceData = (CommonInferenceData) data;
        switch (KernelJavaModelPhaseType.valueOf(phase)) {
            case INFERENCE_INIT:
                return inference1(phase, (InferenceInit) jsonData, inferenceData);
            case INFERENCE_NORMALIZATION:
                inference2(xsTest.numRows(), xsTest.numRows(), 1);
                return null;
            case INFERENCE_RESULT:
                return inference3(jsonData, xsTest.numRows(), xsTest.numRows(), 1);
            default:
                throw new UnsupportedOperationException("unsupported phase in kernel model");
        }
    }

    /**
     * 推理数据归一化
     * phase为-1时，需要过滤数据，只保留需要预测的数据
     *
     * @param phase         阶段
     * @param jsonData      初始化结果
     * @param inferenceData 推理数据
     * @return 类别数量
     */
    private InferenceReqAndRes inference1(int phase, InferenceInit jsonData, CommonInferenceData inferenceData) {
        KernelDispatchJavaPhaseType kernelDispatchJavaPhaseType = KernelDispatchJavaPhaseType.EMPTY_REQUEST;
        if (phase == -1) {
            String[] newUidIndex = jsonData.getUid();
            inferenceData.filterOtherUid(newUidIndex);
            kernelDispatchJavaPhaseType = KernelDispatchJavaPhaseType.INFERENCE_EMPTY_REQUEST;
        }
        InferenceReqAndRes res;
        inferenceDataNormalization(inferenceData);
        logger.info(String.format("Data dimension: %s", inferenceData.getFeatureDim()));
        if (inferenceData.getFeatureDim() == 0) {
            xsTest = DataUtils.toSmpMatrix(DataUtils.zeroMatrix(inferenceData.getDatasetSize(), 1));
            hasFeature = false;
        }
        if (isActive) {
            res = new InferenceReqAndRes(multiClassUniqueLabelList, true, numClass, kernelDispatchJavaPhaseType);
        } else {
            res = new InferenceReqAndRes(new ArrayList<>(), false, numClass, kernelDispatchJavaPhaseType);
        }
        return res;
    }

    /**
     * 推理数据归一化
     *
     * @param inferenceData 推理数据
     */
    private void inferenceDataNormalization(CommonInferenceData inferenceData) {
        if (inferenceData.getFeatureDim() > 0) {
            // normalization
            double[][] mxSample;
            switch (this.normalizationType) {
                case MINMAX:
                    logger.info("In inference of KernelLinearRegressionModel, do minmax normalization.");
                    mxSample = inferenceData.getSample();
                    Normalizer.MinMaxScaler(mxSample, normParams1, normParams2);
                    xsTest = new SimpleMatrix(mxSample);
                    break;
                case STANDARD:
                    logger.info("In inference of KernelLinearRegressionModel, do standard normalization.");
                    mxSample = inferenceData.getSample().clone();
                    Normalizer.StandardScaler(mxSample, normParams1, normParams2);
                    xsTest = new SimpleMatrix(mxSample);
                    break;
                default:
                    logger.info("In inference of KernelLinearRegressionModel, do NO normalization.");
                    xsTest = new SimpleMatrix(inferenceData.getSample());
                    break;
            }
        }
    }


    /**
     * 推理数据核变换
     *
     * @param numSample 推理数量
     * @param batchSize 推理批次大小
     * @param numBatch  推理批次
     */
    private void inference2(int numSample, int batchSize, int numBatch) {
        if (!hasFeature) {
//            xsTestTrans = null;
            xsTestTrans = DataUtils.toSmpMatrix(DataUtils.zeroMatrix(numSample, 1));
        } else {
            xsTestTrans = new SimpleMatrix(numSample, (int) mapdim);
            for (int i = 0; i < numBatch; i++) {
                int start = i * batchSize;
                int end = Math.min(numSample, (i + 1) * batchSize);
                logger.info(String.format("Processing block %s, from sample %s to sample %s.", i, start, end));
                SimpleMatrix temp = xsTest.rows(start, end);
                SimpleMatrix xsTestTransSub = kernelApproximation(temp, transMat, bias);
                setSmpMatValue(xsTestTrans, xsTestTransSub, start, end);
            }
        }
    }


    /**
     * 推理阶段：
     * 正常模式：计算本地推理结果；
     * 安全推理：计算本地推理结果并加密；解密部分推理结果；解密最终结果；
     *
     * @param jsonData  推理请求
     * @param numSample 推理样本数
     * @param batchSize 推理批次
     * @param numBatch  批次数量
     * @return 推理结果
     */
    private Message inference3(Message jsonData, int numSample, int batchSize, int numBatch) {
        Message res = null;
        if (jsonData instanceof InferenceReqAndRes) {
            //todo inference
            double[][] results = new double[numSample][numClass];
            if (hasFeature) {
                for (int i = 0; i < numBatch; i++) {
                    int start = i * batchSize;
                    int end = Math.min(numSample, (i + 1) * batchSize);
                    SimpleMatrix temp = xsTestTrans.rows(start, end);
                    for (int m = 0; m < modelParas.length; m++) {
                        Vector innerProd = computePredict(temp, modelParas[m]);
                        for (int j = start; j < end; j++) {
                            results[j][m] = innerProd.getValues(j - start);
                        }
                    }
                }
            }
            logger.info("Inner product result");
            if (useDistributedPillar) {
                DistributedPaillierNative.signedByteArray[][] predEnc = new DistributedPaillierNative.signedByteArray[results.length][results[0].length];
                for (int i = 0; i < results.length; i++) {
                    for (int j = 0; j < results[0].length; j++) {
                        predEnc[i][j] = pheKeys.encryption(results[i][j], pheKeys.getPk());
                    }
                }
                return new CypherMessage2D(predEnc);
            }
            res = new InferenceReqAndRes(((InferenceReqAndRes) jsonData).getClient(), new double[0], results, numClass - 1, isActive, numClass, null);
        } else if (jsonData instanceof CypherMessage2D) {
            return decryptsPartialScores((CypherMessage2D) jsonData);
        } else if (jsonData instanceof CypherMessage2DList) {
            return decryptsFinalScores((CypherMessage2DList) jsonData);
        }
        return res;
    }


    /**
     * 客户端本地推理
     *
     * @param simpleMatrix 完成核变换的推理数据
     * @param vector       变换向量
     * @return 变换结果 客户端本地推理结果
     */
    public Vector computePredict(SimpleMatrix simpleMatrix, Vector vector) {
        int num = simpleMatrix.numRows();
        int col = simpleMatrix.numCols();
        double[] res = new double[num];
        for (int i = 0; i < num; i++) {
            for (int j = 0; j < col; j++) {
                res[i] += simpleMatrix.get(i, j) * vector.getValues(j);
            }
        }
        return DataUtils.arrayToVector(res);
    }

    /**
     * 数据转换
     *
     * @param mat 加密状态数据转换
     * @return 转换后的数据
     */
    public static DistributedPaillierNative.signedByteArray[][] transpose(DistributedPaillierNative.signedByteArray[][] mat) {
        DistributedPaillierNative.signedByteArray[][] res = new DistributedPaillierNative.signedByteArray[mat[0].length][mat.length];
        for (int i = 0; i < mat.length; i++) {
            for (int j = 0; j < mat[0].length; j++) {
                res[j][i] = mat[i][j];
            }
        }
        return res;
    }

    /**
     * 解密部分推理结果：客户端解密协调端发送过来的加密状态的推理结果
     *
     * @param message 请求数据
     * @return 部分解密结果
     */
    private Message decryptsPartialScores(CypherMessage2D message) {
        partialSum = message.getBody();
        decPartialSum = new DistributedPaillierNative.signedByteArray[partialSum.length][partialSum[0].length];
        for (int i = 0; i < partialSum.length; i++) {
            decPartialSum[i] = pheKeys.decryptPartial(partialSum[i], pheKeys.getSk());
        }
        List<DistributedPaillierNative.signedByteArray[][]> diffResultType = new ArrayList<>();
        diffResultType.add(decPartialSum);
        return new CypherMessage2DList(diffResultType);
    }

    /**
     * 解密最终推理结果：客户端解密最终的推理结果
     *
     * @param message 请求数据
     * @return 最终推理结果
     */
    private Message decryptsFinalScores(CypherMessage2DList message) {
        List<DistributedPaillierNative.signedByteArray[][]> othersDec = message.getBody();
        List<DistributedPaillierNative.signedByteArray[][]> othersDecTrans = new ArrayList<>();
        othersDec.stream().map(x -> othersDecTrans.add(transpose(x))).collect(Collectors.toList());
        DistributedPaillierNative.signedByteArray[][] partialSumTrans = transpose(partialSum);
        DistributedPaillierNative.signedByteArray[][] decPartialSumTrans = transpose(decPartialSum);
        double[][] finalSocres = new double[partialSum[0].length][partialSum.length];
        for (int i = 0; i < numClass; i++) {
            DistributedPaillierNative.signedByteArray[][] subPred = new DistributedPaillierNative.signedByteArray[othersDec.size() + 1][partialSum.length];
            subPred[0] = decPartialSumTrans[i];
            for (int j = 0; j < othersDec.size(); j++) {
                subPred[j + 1] = othersDecTrans.get(j)[i];
            }
            finalSocres[i] = pheKeys.decryptFinal(subPred, partialSumTrans[i], pheKeys.getSk());
        }
        return new InferenceReqAndRes(MathExt.transpose(finalSocres));
    }

    public void deserialize(String content) {
        KernelJavaSerializer kernelJavaSerializer = new KernelJavaSerializer();
        kernelJavaSerializer.parseJson(content);
        this.modelToken = kernelJavaSerializer.getModelToken();
        this.numClass = kernelJavaSerializer.getNumClass();
        this.mapdim = kernelJavaSerializer.getMapdim();
        this.modelParas = DataUtils.arraysToVectors(kernelJavaSerializer.getModelParas());
        this.transMat = DataUtils.arraysToSimpleMatrix(kernelJavaSerializer.getMatweight());
        this.normalizationType = kernelJavaSerializer.getNormalizationType();
        this.bias = DataUtils.arrayToVector(kernelJavaSerializer.getBias());
        this.normParams1 = kernelJavaSerializer.getNormParams1();
        this.normParams2 = kernelJavaSerializer.getNormParams2();
        this.isActive = kernelJavaSerializer.isActive();
        this.multiClassUniqueLabelList = kernelJavaSerializer.getMultiClassUniqueLabelList();
    }

    public void deserialize1(String content) {
        KernelJavaSerializer kernelJavaSerializer = null;
        try {
            kernelJavaSerializer = (KernelJavaSerializer) SerializerUtils.deserialize(content);
        } catch (IOException | ClassNotFoundException e) {
            throw new DeserializeException("kernelJava deserialize exception");
        }
        assert kernelJavaSerializer != null;
        this.modelToken = kernelJavaSerializer.getModelToken();
        this.numClass = kernelJavaSerializer.getNumClass();
        this.mapdim = kernelJavaSerializer.getMapdim();
        this.modelParas = DataUtils.arraysToVectors(kernelJavaSerializer.getModelParas());
        this.transMat = DataUtils.arraysToSimpleMatrix(kernelJavaSerializer.getMatweight());
        this.normalizationType = kernelJavaSerializer.getNormalizationType();
        this.bias = DataUtils.arrayToVector(kernelJavaSerializer.getBias());
        this.normParams1 = kernelJavaSerializer.getNormParams1();
        this.normParams2 = kernelJavaSerializer.getNormParams2();
        this.isActive = kernelJavaSerializer.isActive();
        this.multiClassUniqueLabelList = kernelJavaSerializer.getMultiClassUniqueLabelList();
    }


    public String serialize() {
        KernelJavaSerializer kernelJavaSerializer = new KernelJavaSerializer(modelToken, numClass, mapdim, DataUtils.vectorsToArrays(modelParas), DataUtils.smpmatrixToArray(transMat), DataUtils.vectorToArray(bias), normalizationType, normParams1, normParams2, isActive, multiClassUniqueLabelList);
        return kernelJavaSerializer.toJson();
    }


    public String serialize1() {
        KernelJavaSerializer kernelJavaSerializer = new KernelJavaSerializer(modelToken, numClass, mapdim, DataUtils.vectorsToArrays(modelParas), DataUtils.smpmatrixToArray(transMat), DataUtils.vectorToArray(bias), normalizationType, normParams1, normParams2, isActive, multiClassUniqueLabelList);
        try {
            return SerializerUtils.serialize(kernelJavaSerializer);
        } catch (IOException e) {
            logger.error("");
        }
        return null;
    }


    public AlgorithmType getModelType() {
        return AlgorithmType.KernelBinaryClassificationJava;
    }

    public void setForUnitTest(KernelLinearRegressionTrainData trainData, KernelLinearRegressionParameter
            parameter, Vector[] modelParas, List<Integer> sampleIndex, int numClass) {
        xsTrain = new SimpleMatrix(trainData.getFeature());
        this.mapdim = parameter.getMapdim();
        this.batchSize = xsTrain.numRows();
        this.numSample = xsTrain.numRows();
        this.kernelType = parameter.getKernelType();
        this.numClass = numClass;
        this.yTrainSubs = new double[numClass][numSample];
        this.predicts = new double[numClass][numSample];
        if (trainData.hasLabel) {
            isActive = true;
            yTrain = trainData.getLabel();
            yTrainSub = getSubTrainLabel(yTrain, sampleIndex, batchSize);
            IntStream.range(0, numClass).forEach(x -> yTrainSubs[x] = yTrain);
            double init = -Double.MAX_VALUE;
            List<Double> tmpRoundMetric = new ArrayList<>();
            tmpRoundMetric.add(init);
            metricMap = Arrays.stream(parameter.getMetricType()).collect(Collectors.toMap(metric -> metric, metric -> new ArrayList<>(tmpRoundMetric)));

        }
        this.parameter = parameter;
        this.modelParas = modelParas;
        this.xsTrainTransSub = initKernelApproximation(xsTrain);
        this.xsTrainTrans = setSmpMatValue(new SimpleMatrix(numSample, (int) mapdim), xsTrainTransSub, 0, xsTrain.numRows());
    }

    public void setForInferTest(InferenceData inferenceData, int numClass) {
        xsTest = new SimpleMatrix(inferenceData.getSample());
        this.numSample = xsTest.numRows();
        xsTestTrans = new SimpleMatrix(numSample, (int) mapdim);
        SimpleMatrix temp = xsTest.rows(0, inferenceData.getDatasetSize());
        SimpleMatrix Xs_test_trans_sub = kernelApproximation(temp, transMat, bias);
        setSmpMatValue(xsTestTrans, Xs_test_trans_sub, 0, inferenceData.getDatasetSize());
        this.numClass = numClass;
    }

    public KernelLinearRegressionJavaModel(String modelToken, SimpleMatrix transMat, Vector bias, int mapdim, Vector[] modelParas) {
        this.modelToken = modelToken;
        this.transMat = transMat;
        this.bias = bias;
        this.mapdim = mapdim;
        this.modelParas = modelParas;
    }

}