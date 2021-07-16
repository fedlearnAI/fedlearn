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
import com.jdt.fedlearn.core.entity.ClientInfo;
import com.jdt.fedlearn.core.entity.Message;
import com.jdt.fedlearn.core.entity.common.InferenceInit;
import com.jdt.fedlearn.core.entity.feature.Features;
import com.jdt.fedlearn.core.entity.kernelLinearRegression.*;
import com.jdt.fedlearn.core.loader.common.CommonInferenceData;
import com.jdt.fedlearn.core.metrics.Metric;
import com.jdt.fedlearn.core.preprocess.InferenceFilter;

import com.jdt.fedlearn.core.type.AlgorithmType;
//import com.jdt.fedlearn.core.type.KernelModelJavaPhaseType;
import com.jdt.fedlearn.core.type.MetricType;
import com.jdt.fedlearn.core.type.NormalizationType;
import com.jdt.fedlearn.core.type.data.Tuple2;
import com.jdt.fedlearn.core.util.Tool;
import com.jdt.fedlearn.grpc.federatedlearning.Vector;
import com.jdt.fedlearn.core.loader.common.InferenceData;
import com.jdt.fedlearn.core.loader.common.TrainData;
import com.jdt.fedlearn.core.loader.kernelLinearRegression.*;
import com.jdt.fedlearn.core.math.MathExt;
import com.jdt.fedlearn.core.math.Normalizer;
import com.jdt.fedlearn.core.math.NormalizerOutPackage;
import com.jdt.fedlearn.core.parameter.KernelLinearRegressionParameter;
import com.jdt.fedlearn.core.parameter.SuperParameter;

import org.ejml.simple.SimpleMatrix;
import org.jblas.*;
import org.jblas.Solve;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class KernelLinearRegressionJavaModel implements Model {
    private static final Logger logger = LoggerFactory.getLogger(KernelLinearRegressionJavaModel.class);
    private boolean isInitTrain = false;
    private boolean hasFeature = true;
    boolean isActive = false;
    private SimpleMatrix xsTrain = null;
    private SimpleMatrix xsTrainTrans = null;
    private SimpleMatrix xsTrainTransSub = null;
    private SimpleMatrix xsTest = null;
    private SimpleMatrix xsTestTrans = null;
    private SimpleMatrix transMat = null;
    private Vector bias;
    private double[] yTrain;
    private double[] yTrainSub;
    private Vector[] modelParas;
    private String splitLine = "========================================================";
    private String modelToken;
    private double kernelType;
    private double mapdim;
    private double scale;
    private int numsample;
    private int batchSize;
    private NormalizationType normalizationType = NormalizationType.NONE;
    private KernelLinearRegressionParameter parameter;
    private double[][] transMetric;
    private double[] bias1;
    private double[] normParams1;
    private double[] normParams2;
    private Map<MetricType, List<Double>> metricMap;
    private Map<MetricType, List<Double>> metricMapVali;
    private List<Double> multiClassUniqueLabelList = new ArrayList<>();
    private int numClassRound = 0;
    private double[][] preds;
    private int numClass;
    private double[][] yTrainSubs;
    private Map<MetricType, List<Double[][]>> metricMapArr;
    private Map<MetricType, List<Double[][]>> metricMapArrVali;
    private CommonInferenceData validationTest;
    private double[] yValiSub;
    private double[][] yValiSubs;
    private String[] testId;
    private String[][] valiadationData;
    private Map<Integer, Vector[]> modelParasRounds = new HashMap<>();
    private int bestRound;
    private int tmpRound = 1;

    public KernelLinearRegressionJavaModel() {
    }

    public KernelLinearRegressionJavaModel(String modelToken, SimpleMatrix transMat, Vector bias, int mapdim, Vector[] modelParas) {
        this.modelToken = modelToken;
        this.transMat = transMat;
        this.bias = bias;
        this.mapdim = mapdim;
        this.modelParas = modelParas;
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
        logger.info("Training label size " + labelset.length + ", sampleIndex size is " + sampleIndex.size());
        //TODO
        for (int i = 0; i < batchSize; i++) {
            int ind = sampleIndex.get(i);
            yTrainSub[i] = labelset[ind];
        }
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
    public void multiLabelTransform() {
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
    public void transTrainSub() {
        //y是否等于numcloassround的类别，相等为1，否则为0
        this.yTrainSubs[numClassRound] = Arrays.stream(yTrainSub).map(l -> (l == multiClassUniqueLabelList.get(numClassRound)) ? 1 : 0).toArray();
    }

    private String[][] loadFea(String[][] rawTable) {
        String[][] res = new String[rawTable.length][rawTable[0].length - 1];
        for (int i = 0; i < rawTable.length; i++) {
            for (int j = 0; j < rawTable[0].length - 1; j++) {
                res[i][j] = rawTable[i][j];
            }
        }
        return res;
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
        if (testIndex.length == 0) {
            testId = trainTestUId._1();
        }
        testId = trainTestUId._2();
        valiadationData = new String[testId.length + 1][rawData[0].length];
        valiadationData[0] = rawData[0];
        for (int i = 0; i < testId.length; i++) {
            for (int m = 0; m < rawData.length; m++) {
                if (rawData[m][0].equals(testId[i])) {
                    valiadationData[i + 1] = rawData[m];
                }
            }
        }
        normalization(trainData);
        kernelType = parameter.getKernelType();
        mapdim = parameter.getMapdim();
        scale = parameter.getScale();
        initTrainData(trainData);
        //TODO batchSize = numsample
        numsample = xsTrain.numRows();
        modelParas = new Vector[numClass];
        preds = new double[numClass][numsample];
        yTrainSubs = new double[numClass][numsample];
        yValiSubs = new double[numClass][testId.length];
        //Math.min(numsample, parameter.getBatchSize());
        batchSize = numsample;
        logger.info("Finish initializing training data.");
        if (trainData.hasLabel) {
            isActive = true;
            yTrain = trainData.getLabel();
            IntStream.range(0, numClass).forEach(x -> yTrainSubs[x] = yTrain);
            if (numClass > 1) {
                multiLabelTransform();
            }
            yValiSub = Tool.str2double(MathExt.transpose(valiadationData)[valiadationData[0].length - 1]);
            IntStream.range(0, numClass).forEach(x -> yValiSubs[x] = yValiSub);
            valiadationData = loadFea(valiadationData);

        }
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
        return trainData;
    }

    /**
     * 训练数据初始化
     *
     * @param trainData 训练数据
     */
    private void initTrainData(KernelLinearRegressionTrainData trainData) {
        logger.info("Start initializing training data.");
        if (trainData.featureDim > 0) {
            xsTrain = new SimpleMatrix(trainData.getFeature());
        }
        //TODO feature维度为0时填的0，可以不计算直接返回
        else if (trainData.featureDim == 0) {
            xsTrain = DataUtils.toSmpMatrix(DataUtils.zeroMatrix(trainData.datasetSize, 1));
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
        boolean couldDoNormalization = false;
        if (trainData.featureDim > 0) {
            couldDoNormalization = true;
        }
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
     * 生成符合高斯分布的数组
     *
     * @param m 生成数据的列数
     * @param n 生成数据的维数
     * @return 符合高斯分布的数组
     */
    public double[][] generateNormal(int m, int n) {
        double[][] res = new double[m][n];
        Random r = new Random(7);
        for (int i = 0; i < res.length; i++) {
            for (int j = 0; j < res[0].length; j++) {
                res[i][j] = r.nextGaussian() * Math.sqrt(2 * scale);
            }
        }
        return res;
    }

    /**
     * 生成符合均匀分布的数组
     *
     * @param m 维数
     * @return 符合均匀分布的数组
     */
    public double[] generateUniform(int m) {
        //TODO    range
        double[] res = new double[m];
        Random random = new Random(7);
        for (int i = 0; i < res.length; i++) {
            res[i] = random.nextDouble() * 2 * Math.PI;
        }
        return res;
    }

    /**
     * 训练数据特征
     *
     * @param featureTrain 训练数据特征
     * @return 转换之后的矩阵
     */
    public SimpleMatrix KernelApproximationTrain(SimpleMatrix featureTrain) {
        if (kernelType != 3) {
            return new SimpleMatrix(new double[0][]);
        }
        int num = featureTrain.numRows();
        int col = featureTrain.numCols();
        transMetric = generateNormal(col, (int) mapdim);
        bias1 = generateUniform((int) mapdim);
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
     * @param origionFeature 原始特征
     * @param transMatric    转换矩阵
     * @param bias           bias
     * @return 转换后的特征
     */
    public SimpleMatrix kernelApproximationPhase2(SimpleMatrix origionFeature, SimpleMatrix transMatric, Vector bias) {
        double[][] mutiRes = MathExt.matrixMul(origionFeature, transMatric);
        double[][] res = new double[mutiRes.length][mutiRes[0].length];
        for (int i = 0; i < res.length; i++) {
            for (int j = 0; j < res[i].length; j++) {
                res[i][j] = (Math.sqrt(2.) / Math.sqrt(bias.getValuesCount())) * Math.cos(mutiRes[i][j] + bias.getValues(j));
            }
        }
        return new SimpleMatrix(res);
    }


    public Message train(int phase, Message jsonData, TrainData train) {
        logger.info(String.format("Traing process, phase %s start", phase) + splitLine);
        Message res = null;
        if (jsonData == null) {
            return null;
        }
        if (phase == 1) {
            TrainReq req = (TrainReq) jsonData;
            ClientInfo client = req.getClient();
            //初始化
            if (!isInitTrain) {
                client = req.getClient();
                logger.info("Finish the initialization process.");
                if (hasFeature) {
                    res = fromInitTrain(req, client);
                }
                if (!hasFeature) {
                    res = fromInitNoFea(req, client);
                }
                isInitTrain = true;
            } else {
                if (req.getBestRound() != 0) {
                    bestRound = req.getBestRound();
                    modelParas = modelParasRounds.get(bestRound);
                    res = new TrainRes(client, parameter.getMaxIter() + 1);
                    return res;
                }
                if (hasFeature) {
                    List<Integer> sampleIndex = req.getSampleIndex();
                    numClassRound = req.getNumClassRound();
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
                    preds[numClassRound] = DataUtils.vectorToArray(DataUtils.toVector(inner_prod)).clone();
                    res = new TrainRes(client, preds, 0.0, isActive);
                    // todo erlystopping

                } else {
                    List<Integer> sampleIndex = req.getSampleIndex();
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
                    preds[numClassRound] = DataUtils.vectorToArray(DataUtils.toVector(inner_prod)).clone();
                    res = new TrainRes(client, preds, para_norm, isActive);
                }
            }
            logger.info("Phase 1 finish.");
            return res;
        }
        if (phase == 2) {
            TrainReq req = (TrainReq) jsonData;
            ClientInfo client = req.getClient();
            res = trainPhase2(req, client);
            return res;
        }
        if (phase == 3) {
            res = inferenceInit(testId, valiadationData, null);
        } else {
            String[][] validationClone = valiadationData.clone();
            validationTest = new CommonInferenceData(validationClone, "uid", null);
            res = validation(phase, jsonData, validationTest);
        }
        return res;
    }


    private TrainRes fromInitNoFea(TrainReq req, ClientInfo client) {
        TrainRes res;
        int numsample = xsTrain.numRows();
        IntStream.range(0, numClass).forEach(x -> modelParas[x] = DataUtils.allzeroVector((int) mapdim));
//                    modelParas[numClassRound] = DataUtils.allzeroVector((int) mapdim);
        xsTrainTrans = DataUtils.toSmpMatrix(DataUtils.zeroMatrix(numsample, (int) mapdim));
        List<Integer> sampleIndex = req.getSampleIndex();
        xsTrainTransSub = getSubTrainFeat(xsTrainTrans, sampleIndex, batchSize);
        SimpleMatrix inner_prod = DataUtils.toSmpMatrix(DataUtils.allzeroVector(batchSize)).scale(-1);
        double para_norm = Math.pow(inner_prod.normF(), 2);
        if (isActive) {
            yTrainSub = getSubTrainLabel(yTrain, sampleIndex, batchSize);
            if (numClass > 1) {
                transTrainSub();
            }
            inner_prod = inner_prod.plus(DataUtils.toSmpMatrix(DataUtils.arrayToVector(yTrainSubs[numClassRound])));
        }
        preds[numClassRound] = DataUtils.vectorToArray(DataUtils.toVector(inner_prod));
        res = new TrainRes(client, preds, para_norm, isActive);
        return res;
    }

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
                xsTrainTransSub = KernelApproximationTrain(temp);
                transMat = new SimpleMatrix(transMetric);
                bias = DataUtils.arrayToVector(bias1);
                xsTrainTrans = setSmpMatValue(xsTrainTrans, xsTrainTransSub, start, end);
            } else {
                xsTrainTransSub = kernelApproximationPhase2(temp, transMat, bias);
                xsTrainTrans = setSmpMatValue(xsTrainTrans, xsTrainTransSub, start, end);
            }
        }
        List<Integer> sampleIndex = req.getSampleIndex();
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
        preds[numClassRound] = predArray.clone();
        //TODO return double[numclass][samplenum] preds
        res = new TrainRes(client, preds, para_norm, isActive);
        return res;
    }

    private static double clip(double val) {
        if (val < 0.00001) {
            return 0.0001;
        }
        if (val > 0.99999) {
            return 0.99999;
        }
        return val;
    }

    public static double[][] predTrans(double[][] data, int numClass) {
        double[][] res = new double[data.length][data[0].length];
        if (numClass == 1) {
            res = data;
            return res;
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

    public double[] transform(double[][] pred) {
        double[][] values = MathExt.transpose(pred);
        Arrays.stream(values).forEach(predValues -> {
            double sum = Arrays.stream(predValues).sum();
            IntStream.range(0, numClass).forEach(i -> predValues[i] = predValues[i] / sum);
        });
        // return flatten nClass * dataSize
        return Arrays.stream(MathExt.transpose(values)).flatMapToDouble(Arrays::stream).toArray();
    }

    private TrainRes trainPhase2(TrainReq req, ClientInfo client) {
        TrainRes res = null;
        ClientInfo clientInfo = req.getClient();
        boolean isUpdate = req.isUpdate();
        List<Integer> sampleIndex = req.getSampleIndex();
        SimpleMatrix tmp = null;
        SimpleMatrix innerProd = null;
        if (isActive) {
            double[][] predActuals = new double[numClass][yTrainSub.length];
            double[][] predGaps = req.getValuelists();
            for (int i = 0; i < numClass; i++) {
                int finalI = i;
                predActuals[i] = IntStream.range(0, predGaps[i].length).mapToDouble(p -> (predGaps[finalI][p] - yTrainSubs[finalI][p]) * (-1)).toArray();
            }
            if (numClassRound + 1 == parameter.getNumClass()) {
                double[] predTrans = Arrays.stream(predActuals).flatMapToDouble(Arrays::stream).toArray();
                if (numClass > 1) {
                    if (numClass == 2) {
                        predTrans = predActuals[1];
                    } else {
                        predTrans = transform(predActuals);
                    }
                    predTrans = Arrays.stream(predTrans).map(p -> clip(p)).toArray();
                    this.yTrainSub = Arrays.stream(yTrain).map(l -> multiClassUniqueLabelList.indexOf(l)).toArray();
                }
                updateMetric(predTrans, yTrainSub);
                tmp = DataUtils.toSmpMatrix(DataUtils.arrayToVector(req.getValuelists()[numClassRound]));
                innerProd = xsTrainTransSub.mult(DataUtils.toSmpMatrix(modelParas[numClassRound]));
            } else {
                tmp = DataUtils.toSmpMatrix(DataUtils.arrayToVector(req.getValuelists()[numClassRound]));
                innerProd = xsTrainTransSub.mult(DataUtils.toSmpMatrix(modelParas[numClassRound]));
                numClassRound++;
            }
        }
        if (isUpdate) {
            if (!isActive) {
                tmp = DataUtils.toSmpMatrix(DataUtils.arrayToVector(req.getValuelists()[numClassRound]));
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
            res = new TrainRes(client, numClassRound, isActive);
            logger.info("Phase 2 finish.");
        } else {
            res = new TrainRes(client, numClassRound, isActive);
        }
        return res;
    }


    private void updateMetric(double[] pred, double[] label) {
        if (!this.isActive || (metricMap == null && metricMapArr == null)) {
            return;
        }
        Map<MetricType, Double> trainMetric = new HashMap<>();
        Map<MetricType, Double[][]> trainMetricArr = new HashMap<>();
        MetricType[] evalMetric = parameter.getMetricType();
        String[] arr = {"confusion", "RocCurve", "KSCurve", "mAuc", "tpr", "fpr"};
        for (MetricType t : evalMetric) {
            if (Arrays.asList(arr).contains(t.getMetric())) {
                trainMetricArr.put(t, Metric.calculateMetricArr(t, pred, label, multiClassUniqueLabelList));
            } else {
                trainMetric.put(t, Metric.calculateMetric(t, pred, label));
            }
        }
        trainMetric.forEach((key, value) -> metricMap.get(key).add(value));
        trainMetricArr.forEach((key, value) -> metricMapArr.get(key).add(value));
    }

    private void updateMetricVali(double[] pred, double[] label) {
        if (!this.isActive || (metricMapVali == null && metricMapArrVali == null)) {
            return;
        }
        Map<MetricType, Double> trainMetric = new HashMap<>();
        Map<MetricType, Double[][]> trainMetricArr = new HashMap<>();
        MetricType[] evalMetric = parameter.getMetricType();
        String[] arr = {"confusion", "RocCurve", "KSCurve", "mAuc", "tpr", "fpr"};
        for (MetricType t : evalMetric) {
            if (Arrays.asList(arr).contains(t.getMetric())) {
                trainMetricArr.put(t, Metric.calculateMetricArr(t, pred, label, multiClassUniqueLabelList));
            } else {
                trainMetric.put(t, Metric.calculateMetric(t, pred, label));
            }
        }
        trainMetric.forEach((key, value) -> metricMapVali.get(key).add(value));
        trainMetricArr.forEach((key, value) -> metricMapArrVali.get(key).add(value));
    }

    // 推理初始化，
    public Message inferenceInit(String[] uidList, String[][] inferenceCacheFile, Map<String, Object> others) {
        return InferenceFilter.filter(uidList, inferenceCacheFile);
    }

    public Message inference(int phase, Message jsonData, InferenceData Data) {
        logger.info(String.format("Inference process, phase %s start", phase) + splitLine);
        if (jsonData == null) {
            return null;
        }
        CommonInferenceData inferenceData = (CommonInferenceData) Data;
        InferenceReqAndRes res = null;
        if (phase == -1) {
            res = inference1((InferenceInit) jsonData, inferenceData);
        }
        if (phase == -2) {
            inference2(xsTest.numRows(), xsTest.numRows(), 1);
        }
        if (phase == -3) {
            res = inference3((InferenceReqAndRes) jsonData, xsTest.numRows(), xsTest.numRows(), 1);
        }
        logger.info(String.format("Inference process, phase %s end", phase) + splitLine);
        return res;
    }

    public Message validation(int phase, Message jsonData, InferenceData Data) {
        logger.info(String.format("Inference process, phase %s start", phase) + splitLine);
        if (jsonData == null) {
            return null;
        }
        CommonInferenceData inferenceData = (CommonInferenceData) Data;
        Message res = null;
        if (phase == 4) {
            res = inference1((InferenceInit) jsonData, inferenceData);
        }
        if (phase == 5) {
            inference2(xsTest.numRows(), xsTest.numRows(), 1);
        }
        if (phase == 6) {
            res = inference3((InferenceReqAndRes) jsonData, xsTest.numRows(), xsTest.numRows(), 1);
        }
        if (phase == 7) {
            res = valid(jsonData);
        }
        logger.info(String.format("Inference process, phase %s end", phase) + splitLine);
        return res;
    }


    private InferenceReqAndRes inference3(InferenceReqAndRes jsonData, int numsample, int batchSize, int numbatch) {
        InferenceReqAndRes res;
        InferenceReqAndRes req = jsonData;
        ClientInfo client = req.getClient();
        //todo inference
        double[] predict = new double[numsample];
        double[][] results = new double[numsample][numClass];
        if (hasFeature) {
            for (int i = 0; i < numbatch; i++) {
                int start = i * batchSize;
                int end = Math.min(numsample, (i + 1) * batchSize);
                SimpleMatrix temp = xsTestTrans.rows(start, end);
                for (int m = 0; m < modelParas.length; m++) {
                    Vector innerProd = kernelLinearRegressionInferencePhase1(temp, modelParas[m]);
                    for (int j = start; j < end; j++) {
                        if (hasFeature) {
                            results[j][m] = innerProd.getValues(j - start);
                            predict[j] = innerProd.getValues(j - start);
                        } else {
                            results[j][m] = 0;
                            predict[j] = 0;
                        }
                    }
                }
            }
        }
        logger.info("Inner product result");
        res = new InferenceReqAndRes(client, predict, results, numClass - 1, isActive, numClass);
        return res;
    }

    private TrainRes valid(Message jsonData) {
        TrainReq trainReq = (TrainReq) jsonData;
        TrainRes res;
        modelParasRounds.put(tmpRound, modelParas);
        tmpRound++;
        if (!isActive) {
            res = new TrainRes(trainReq.getClient(), numClassRound, isActive);
            return res;
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
                predTrans = Arrays.stream(predTrans).map(p -> clip(p)).toArray();
            }
            updateMetricVali(predTrans, yValiSub);
            numClassRound = 0;
            res = new TrainRes(trainReq.getClient(), numClassRound, isActive, metricMap, metricMapArr, metricMapVali, metricMapArrVali);
            //TODO
            return res;
        }
    }


    private void inference2(int numsample, int batchSize, int numbatch) {
        int i;
        if (hasFeature) {
            xsTestTrans = new SimpleMatrix(numsample, (int) mapdim);
            for (i = 0; i < numbatch; i++) {
                int start = i * batchSize;
                int end = Math.min(numsample, (i + 1) * batchSize);
                logger.info(String.format("Processing block %s, from sample %s to sample %s.", i, start, end));
                SimpleMatrix temp = xsTest.rows(start, end);
                SimpleMatrix xsTestTransSub = kernelApproximationPhase2(temp, transMat, bias);
                xsTestTrans = setSmpMatValue(xsTestTrans, xsTestTransSub, start, end);
            }
        } else {
            xsTestTrans = null;
        }
    }

    private InferenceReqAndRes inference1(InferenceInit jsonData, CommonInferenceData inferenceData) {
        InferenceReqAndRes res = null;
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
        logger.info(String.format("Data dimension: %s", inferenceData.getFeatureDim()));
        if (inferenceData.getFeatureDim() == 0) {
            xsTest = DataUtils.toSmpMatrix(DataUtils.zeroMatrix(inferenceData.getDatasetSize(), 1));
            hasFeature = false;
        }
        if (isActive) {
            res = new InferenceReqAndRes(multiClassUniqueLabelList, isActive, numClass);
        } else {
            res = new InferenceReqAndRes(new ArrayList<>(), isActive, numClass);
        }
        return res;
    }

    public Vector kernelLinearRegressionInferencePhase1(SimpleMatrix simpleMatrix, Vector vector) {
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

    public void deserialize(String content) {
        String[] lines = content.split("\n");
        int lineNum = 0;
        String modelToken = lines[lineNum].split("=")[1];
        this.modelToken = modelToken;
        lineNum++;
        this.numClass = Integer.parseInt(lines[lineNum].split("=")[1]);
        modelParas = new Vector[numClass];
        lineNum++;
        String csvWeight = lines[lineNum].split("=")[1];
        String[] csvWeights = csvWeight.split(", \\|\\| ");
        for (int n = 0; n < numClass; n++) {
            String[] tmp = csvWeights[n].replace(" ||", "").split(",");
            double[] modelweight = new double[tmp.length];
            int i, j;
            logger.info("parsing model weight");
            for (i = 0; i < tmp.length; i++) {
                modelweight[i] = Double.parseDouble(tmp[i]);
            }
            modelParas[n] = DataUtils.arrayToVector(modelweight);
        }
        lineNum++;
        String matSize = lines[lineNum].split("=")[1];
        lineNum++;
        int numRow = Integer.parseInt(matSize.split(",")[0]);
        int numCol = Integer.parseInt(matSize.split(",")[1]);
        if (numRow != 0 && numCol != 0) {
            String matWeight = lines[lineNum].split("=")[1];
            lineNum++;
            String[] tmp = matWeight.split(",");
            double[][] matweight = new double[numRow][numCol];
            int k = 0;
            logger.info("Parsing weight matrix");
            for (int i = 0; i < numRow; i++) {
                for (int j = 0; j < numCol; j++) {
                    matweight[i][j] = Double.parseDouble(tmp[k]);
                    k += 1;
                }
            }
            this.transMat = new SimpleMatrix(matweight);
            this.mapdim = numCol;
            String biasWeight = lines[lineNum].split("=")[1];
            lineNum++;
            tmp = biasWeight.split(",");
            double[] biasweight = new double[tmp.length];
            logger.info("Parsing bias weight");
            for (int i = 0; i < tmp.length; i++) {
                biasweight[i] = Double.parseDouble(tmp[i]);
            }
            this.bias = DataUtils.arrayToVector(biasweight);
        }
        // load normalization parameters
        normalizationType = NormalizationType.valueOf(lines[lineNum].split("=")[1]);
        lineNum++;
        String strNormParams1 = lines[lineNum].split("=")[1];
        String[] tmp = strNormParams1.split(",");
        normParams1 = new double[tmp.length];
        for (int i = 0; i < tmp.length; i++) {
            normParams1[i] = Double.parseDouble(tmp[i]);
        }
        lineNum++;
        String strNormParams2 = lines[lineNum].split("=")[1];
        tmp = strNormParams2.split(",");
        normParams2 = new double[tmp.length];
        for (int i = 0; i < tmp.length; i++) {
            normParams2[i] = Double.parseDouble(tmp[i]);
        }
        lineNum++;
        String isActive = lines[lineNum].split("=")[1];
        this.isActive = Boolean.parseBoolean(isActive);
        lineNum++;
        if (this.isActive && numClass > 1) {
            String labelList = lines[lineNum].split("=")[1];
            tmp = labelList.split(",");
            multiClassUniqueLabelList = new ArrayList<>();
            for (int i = 0; i < tmp.length; i++) {
                multiClassUniqueLabelList.add(Double.parseDouble(tmp[i]));
            }
        }
    }

    public String serialize() {
        StringBuilder sb = new StringBuilder();
        sb.append("modelToken=").append(modelToken).append("\n");
        StringBuilder classNum = new StringBuilder();
        classNum.append(numClass);
        sb.append("numClass=").append(classNum).append("\n");
        StringBuilder csvWeight = new StringBuilder();
        for (int i = 0; i < numClass; i++) {
            for (double w : DataUtils.vectorToArray(modelParas[i])) {
                csvWeight.append(w).append(",");
            }
            csvWeight.append(" || ");
        }
        csvWeight = new StringBuilder(csvWeight.substring(0, csvWeight.length() - 1));
        sb.append("weight=").append(csvWeight).append("\n");
        int numRow, numCol;
        if (transMat != null) {
            numRow = transMat.numRows();
            numCol = transMat.numCols();
        } else {
            numRow = 0;
            numCol = 0;
        }
        sb.append("matsize=").append(numRow).append(',').append(numCol).append("\n");
        if (numRow != 0 && numCol != 0) {
            StringBuilder matWeight = new StringBuilder();
            for (int i = 0; i < numRow; i++) {
                for (int j = 0; j < numCol; j++) {
                    matWeight.append(transMat.get(i, j)).append(',');
                }
            }
            matWeight = new StringBuilder(matWeight.substring(0, matWeight.length() - 1));
            sb.append("matweight=").append(matWeight);
            sb.append("\n");
            StringBuilder biasWeight = new StringBuilder();
            for (double w : DataUtils.vectorToArray(bias)) {
                biasWeight.append(w).append(',');
            }
            biasWeight = new StringBuilder(biasWeight.substring(0, biasWeight.length() - 1));
            sb.append("biasweight=").append(biasWeight);
            sb.append("\n");
        }
        sb.append("norm_type=").append(normalizationType.toString()).append("\n");
        StringBuilder strNormParams1 = new StringBuilder("");
        for (int i = 0; i < normParams1.length; i++) {
            strNormParams1.append(normParams1[i]).append(',');
        }
        StringBuilder strNormParams2 = new StringBuilder("");
        for (int i = 0; i < normParams2.length; i++) {
            strNormParams2.append(normParams2[i]).append(',');
        }
        if (strNormParams1.length() > 0) {
            strNormParams1 = new StringBuilder(strNormParams1.substring(0, strNormParams1.length() - 1));
            strNormParams2 = new StringBuilder(strNormParams2.substring(0, strNormParams2.length() - 1));
        }
        sb.append("norm_params_1=").append(strNormParams1);
        sb.append("\n");
        sb.append("norm_params_2=").append(strNormParams2);
        sb.append("\n");
        StringBuilder isActive = new StringBuilder();
        isActive.append(this.isActive);
        sb.append("isActive=").append(isActive).append("\n");
        StringBuilder labelList = new StringBuilder();
        for (int i = 0; i < multiClassUniqueLabelList.size(); i++) {
            labelList.append(multiClassUniqueLabelList.get(i)).append(",");
        }
        sb.append("multiClassUniqueLabelList=").append(labelList);
        sb.append("\n");
        return sb.toString();
    }

    public AlgorithmType getModelType() {
        return AlgorithmType.KernelBinaryClassificationJava;
    }

    public void setForUnitTest(KernelLinearRegressionTrainData trainData, KernelLinearRegressionParameter
            parameter, Vector[] modelParas, List<Integer> sampleIndex, int numClass) {
        xsTrain = new SimpleMatrix(trainData.getFeature());
        this.mapdim = parameter.getMapdim();
        this.batchSize = xsTrain.numRows();
        this.numsample = xsTrain.numRows();
        this.kernelType = parameter.getKernelType();
        this.numClass = numClass;
        this.yTrainSubs = new double[numClass][numsample];
        this.preds = new double[numClass][numsample];
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
        this.xsTrainTransSub = KernelApproximationTrain(xsTrain);
        this.xsTrainTrans = setSmpMatValue(new SimpleMatrix(numsample, (int) mapdim), xsTrainTransSub, 0, xsTrain.numRows());
    }

    public void setForInferTest(InferenceData inferenceData, int numClass) {
        xsTest = new SimpleMatrix(inferenceData.getSample());
        this.numsample = xsTest.numRows();
        xsTestTrans = new SimpleMatrix(numsample, (int) mapdim);
        SimpleMatrix temp = xsTest.rows(0, inferenceData.getDatasetSize());
        SimpleMatrix Xs_test_trans_sub = kernelApproximationPhase2(temp, transMat, bias);
        xsTestTrans = setSmpMatValue(xsTestTrans, Xs_test_trans_sub, 0, inferenceData.getDatasetSize());
        this.numClass = numClass;
    }

}