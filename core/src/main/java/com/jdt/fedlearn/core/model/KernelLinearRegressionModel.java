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

import com.jdt.fedlearn.core.dispatch.KernelLinearRegression;
import com.jdt.fedlearn.core.entity.ClientInfo;
import com.jdt.fedlearn.core.entity.Message;
import com.jdt.fedlearn.core.entity.common.InferenceInit;
import com.jdt.fedlearn.core.entity.feature.Features;
import com.jdt.fedlearn.core.entity.kernelLinearRegression.*;
import com.jdt.fedlearn.core.loader.common.CommonInferenceData;
import com.jdt.fedlearn.core.preprocess.InferenceFilter;
import com.jdt.fedlearn.core.type.AlgorithmType;
import com.jdt.fedlearn.core.type.NormalizationType;
import com.jdt.fedlearn.grpc.federatedlearning.*;
import com.jdt.fedlearn.grpc.federatedlearning.Vector;
import com.jdt.fedlearn.core.loader.common.InferenceData;
import com.jdt.fedlearn.core.loader.common.TrainData;
import com.jdt.fedlearn.core.loader.kernelLinearRegression.KernelLinearRegressionTrainData;
import com.jdt.fedlearn.core.parameter.KernelLinearRegressionParameter;

import com.jdt.fedlearn.core.math.Normalizer;
import com.jdt.fedlearn.core.math.NormalizerOutPackage;
import com.jdt.fedlearn.core.parameter.SuperParameter;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import org.ejml.simple.SimpleMatrix;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

import java.util.ArrayList;

public class KernelLinearRegressionModel implements Model {
    private static final Logger logger = LoggerFactory.getLogger(KernelLinearRegression.class);

    // 单元测试用 1 active 1 passive
    private CalculateGrpc.CalculateBlockingStub stub = null;
    private String stubAddress = "";
    private boolean isInitTrain = false;
    private boolean hasFeature = true;
    boolean isActive = false;

    private SimpleMatrix Xs_train = null;
    private SimpleMatrix Xs_train_trans = null;
    private SimpleMatrix Xs_train_trans_sub = null;
    private SimpleMatrix Xs_test = null;
    private SimpleMatrix Xs_test_trans = null;
    private SimpleMatrix Trans_mat = null;
    private Vector bias;
    private String[] uId;
    private double[] y_train;
    private double[] y_train_sub;
    private Vector modelPara;

    private String splitLine = "========================================================";
    private String modelToken;
    private double kernelType;
    private double mapdim;
    private double scale;
    private int numsample;
    private int batchSize;
    private NormalizationType normalizationType = NormalizationType.NONE; // 0: no training normalization; 1: minmax; 2: standard.
    private KernelLinearRegressionParameter parameter;

    // Temporary arrays to store two kinds of intermediate parameters in normalization.
    private double[] norm_params_1;
    private double[] norm_params_2;

    public KernelLinearRegressionModel() {
    }

    public KernelLinearRegressionModel(String modelToken, SimpleMatrix trans_mat, Vector bias, int mapdim, Vector modelPara) {
        this.modelToken = modelToken;
        this.Trans_mat = trans_mat;
        this.bias = bias;
        this.mapdim = mapdim;
        this.modelPara = modelPara;
    }

    @Deprecated
    public KernelLinearRegressionModel(String stubAddress) {
        this.stubAddress = stubAddress;
    }


    private double[] getSubTrainLabel(double[] labelset, List<Integer> sampleIndex, int batchSize) {
        y_train_sub = new double[batchSize];
        logger.info(String.format("Training label size %s", labelset.length));
        logger.info(String.format("sampleIndex size is %s", sampleIndex.size()));
        for (int i = 0; i < batchSize; i++) {
            int ind = sampleIndex.get(i);
            y_train_sub[i] = labelset[ind];
        }
        return y_train_sub;
    }

    private SimpleMatrix getSubTrainFeat(SimpleMatrix alldata, List<Integer> sampleIndex, int batchSize) {
        int feadim = alldata.numCols();
        Xs_train_trans_sub = new SimpleMatrix(batchSize, feadim);
        for (int i = 0; i < batchSize; i++) {
            int ind = sampleIndex.get(i);
            for (int j = 0; j < feadim; j++) {
                double value = alldata.get(ind, j);
                Xs_train_trans_sub.set(i, j, value);
            }
        }
        return Xs_train_trans_sub;
    }

    private SimpleMatrix setSmpMatValue(SimpleMatrix bigmat, SimpleMatrix smallmat, int start, int end) {
        int featdim = bigmat.numCols();
        for (int i = start; i < end; i++) {
            for (int j = 0; j < featdim; j++) {
                bigmat.set(i, j, smallmat.get(i - start, j));
            }
        }
        return bigmat;
    }

    public KernelLinearRegressionTrainData trainInit(String[][] rawData, String[] uids, int[] testIndex, SuperParameter sp, Features features, Map<String, Object> others) {
        this.parameter = (KernelLinearRegressionParameter) sp;
        KernelLinearRegressionTrainData trainData = new KernelLinearRegressionTrainData(rawData, uids, features);
        // do normalization?
//        for(int i=0; i<haha.length; i++) {
//            logger.info("norm type " + haha[i].getNormalizationType());
//        }

        //
        this.normalizationType = parameter.getNormalizationType();

        //
        NormalizerOutPackage temp_normalized_out;
        boolean could_do_normalization = false;
        if (trainData.hasLabel) {
            if (trainData.getFeatureDim() - 1 > 0) {
                could_do_normalization = true;
            }
        } else {
            if (trainData.getFeatureDim() > 0) {
                could_do_normalization = true;
            }
        }
        // debug, 显示归一化之前的内容
        double[][] debug_temp_features;
        String debug_oneRow;
        if (could_do_normalization) {
            debug_temp_features = trainData.getFeature();
            debug_oneRow = Arrays.toString(debug_temp_features[0]);
            logger.info("Debug, before normalization, one row of getFeature");
            logger.info(debug_oneRow);
        }
        //
        if (could_do_normalization) {
            switch (this.normalizationType) {
                case MINMAX:
                    logger.info("In trainInit of KernelLinearRegressionModel, do minmax normalization.");
                    temp_normalized_out = Normalizer.MinMaxScaler(trainData.getFeature());
                    // debug, 显示归一化之后的内容
                    debug_temp_features = trainData.getFeature();
                    debug_oneRow = Arrays.toString(debug_temp_features[0]);
                    logger.info("Debug, after normalization, one row of getFeature");
                    logger.info(debug_oneRow);
                    //
                    norm_params_1 = temp_normalized_out.params1.clone();
                    norm_params_2 = temp_normalized_out.params2.clone();
                    break;
                case STANDARD:
                    logger.info("In trainInit of KernelLinearRegressionModel, do standard normalization.");
                    temp_normalized_out = Normalizer.StandardScaler(trainData.getFeature());
                    // debug, 显示归一化之后的内容
                    debug_temp_features = trainData.getFeature();
                    debug_oneRow = Arrays.toString(debug_temp_features[0]);
                    logger.info("Debug, after normalization, one row of getFeature");
                    logger.info(debug_oneRow);
                    //
                    norm_params_1 = temp_normalized_out.params1.clone();
                    norm_params_2 = temp_normalized_out.params2.clone();
                    break;
                default:
                    logger.info("In trainInit of KernelLinearRegressionModel, do NO normalization.");
                    // debug, 显示归一化之后的内容
                    debug_temp_features = trainData.getFeature();
                    debug_oneRow = Arrays.toString(debug_temp_features[0]);
                    logger.info("Debug, after NO normalization, one row of getFeature");
                    logger.info(debug_oneRow);
                    //
                    norm_params_1 = new double[8];
                    Arrays.fill(norm_params_1, -1.0);
                    norm_params_2 = new double[8];
                    Arrays.fill(norm_params_2, -1.0);
                    break;
            }
        } else {
            logger.info("In trainInit of KernelLinearRegressionModel, feature_dim does not support for normalization.");
            norm_params_1 = new double[8];
            Arrays.fill(norm_params_1, -1.0);
            norm_params_2 = new double[8];
            Arrays.fill(norm_params_2, -1.0);
        }
        //
        kernelType = parameter.getKernelType();
        mapdim = parameter.getMapdim();
        scale = parameter.getScale();
        logger.info("Start initializing training data.");
        if (trainData.getFeatureDim() > 0) {
            Xs_train = new SimpleMatrix(trainData.getFeature());
        } else if (trainData.getFeatureDim() == 0) {
            Xs_train = DataUtils.toSmpMatrix(DataUtils.zeroMatrix(trainData.getDatasetSize(), 1));
            hasFeature = false;
        }
        numsample = Xs_train.numRows();
        batchSize = numsample;//Math.min(numsample, parameter.getBatchSize());
        logger.info("Finish initializing training data.");
        logger.info("Start connecting with grpc service.");
        if (trainData.hasLabel) {
            isActive = true;
            y_train = trainData.getLabel();
            // active 方建立 stubby server
            //String target = "127.0.0.1:8891";
            if ("".equals(stubAddress)) {
                stubAddress = "127.0.0.1:8891";
            }
            ManagedChannel channel = ManagedChannelBuilder.forTarget(stubAddress).usePlaintext().maxInboundMessageSize(Integer.MAX_VALUE).build();
            stub = CalculateGrpc.newBlockingStub(channel);
        } else {
            //String target = "127.0.0.1:8891";
            if ("".equals(stubAddress)) {
                stubAddress = "127.0.0.1:8891";
            }
            ManagedChannel channel = ManagedChannelBuilder.forTarget(stubAddress).usePlaintext().maxInboundMessageSize(Integer.MAX_VALUE).build();
            stub = CalculateGrpc.newBlockingStub(channel);
        }
        logger.info("Finish connecting with grpc service!");
//        isInitTrain = true;
        return trainData;
    }


    //private double[] modelPara;

    // 一共 5 个 Phase
    // Phase 1：
    //    第一次循环，完成初始化和样本核变换
    //    之后的循环  计算X*w 传输给master
    //    master 完成加法
    // Phase 2:
    //    每次选择一方调用grpc服务，求解一个回归问题
    public Message train(int phase, Message jsonData, TrainData train) {
        logger.info(String.format("Traing process, phase %s start", phase) + splitLine);
        String responseStr = "";
        TrainRes res = null;
        if (jsonData == null) {
            return null;
        }
        TrainReq req = (TrainReq) jsonData;
        ClientInfo client = req.getClient();
//        KernelLinearRegressionTrainData trainData = (KernelLinearRegressionTrainData) train;
        int i;
        if (phase == 1) {
            //初始化
            if (!isInitTrain) {
//                KernelLinearRegressionParameter parameter = req.getParameter();
                client = req.getClient();
//                trainInit(trainData, req);
                logger.info("Finish the initialization process.");
                if (hasFeature == true) {
                    logger.info(String.format("Training data shape %s, %s", Xs_train.numCols(), Xs_train.numRows()));
                    logger.info(String.format("Kernel type, map dim %s, %s, %s", kernelType, mapdim, scale));
                    logger.info("Start kernel mapping.");
                    int numsample = Xs_train.numRows();
                    int batchSize = numsample; //Math.min(parameter.getBatchSize(), numsample);
                    int numbatch = (int) Math.ceil((double) numsample / batchSize);
                    Xs_train_trans = new SimpleMatrix(numsample, (int) mapdim);
                    logger.info(String.format("Partitioning the training sample into %s blocks", numbatch));
                    for (i = 0; i < numbatch; i++) {
                        int start = i * batchSize;
                        int end = Math.min(numsample, (i + 1) * batchSize);
                        logger.info(String.format("Processing block %s, from sample %s to sample %s.", i, start, end));
                        SimpleMatrix temp = Xs_train.rows(start, end);
                        if (i == 0) {
                            InputMessage msg = DataUtils.prepareInputMessage(new Matrix[]{DataUtils.toMatrix(temp)},
                                    new Vector[]{}, new Double[]{kernelType, mapdim, scale});
                            OutputMessage responsePhase0 = stub.kernelApproximationPhase1(msg);
                            Xs_train_trans_sub = DataUtils.toSmpMatrix(responsePhase0.getMatrices(0));
                            Trans_mat = DataUtils.toSmpMatrix(responsePhase0.getMatrices(1));
                            bias = responsePhase0.getVectors(0);
                            Xs_train_trans = setSmpMatValue(Xs_train_trans, Xs_train_trans_sub, start, end);
                        } else {
                            InputMessage msg = DataUtils.prepareInputMessage(new Matrix[]{DataUtils.toMatrix(temp),
                                    DataUtils.toMatrix(Trans_mat)}, new Vector[]{bias}, new Double[]{});
                            OutputMessage responsePhase0 = stub.kernelApproximationPhase2(msg);
                            Xs_train_trans_sub = DataUtils.toSmpMatrix(responsePhase0.getMatrices(0));
                            Xs_train_trans = setSmpMatValue(Xs_train_trans, Xs_train_trans_sub, start, end);
                        }
                        logger.info(String.format("Finish processing block %s, from sample %s to sample %s.", i, start, end));
                    }
                    List<Integer> sampleIndex = req.getSampleIndex();
                    logger.info("batchSize:" + batchSize);
                    logger.info("sampleIndex" + Arrays.asList(sampleIndex));
                    Xs_train_trans_sub = getSubTrainFeat(Xs_train_trans, sampleIndex, batchSize);
                    modelPara = DataUtils.alloneVector((int) mapdim, 0.1);//toSmpMatrix(responsePhase1.getVectors(0));
                    SimpleMatrix inner_prod = Xs_train_trans_sub.mult(DataUtils.toSmpMatrix(modelPara)).scale(-1);
                    double para_norm = Math.pow(inner_prod.normF(), 2);
                    if (isActive == true) {
                        y_train_sub = getSubTrainLabel(y_train, sampleIndex, batchSize);
                        inner_prod = inner_prod.plus(DataUtils.toSmpMatrix(DataUtils.arrayToVector(y_train_sub)));
                    }
                    res = new TrainRes(client, DataUtils.vectorToArray(DataUtils.toVector(inner_prod)), para_norm, isActive);
                }
                if (hasFeature == false) {
                    int numsample = Xs_train.numRows();
                    modelPara = DataUtils.allzeroVector((int) mapdim);
                    Xs_train_trans = DataUtils.toSmpMatrix(DataUtils.zeroMatrix(numsample, (int) mapdim));
                    List<Integer> sampleIndex = req.getSampleIndex();
                    Xs_train_trans_sub = getSubTrainFeat(Xs_train_trans, sampleIndex, batchSize);
                    y_train_sub = getSubTrainLabel(y_train, sampleIndex, batchSize);
                    SimpleMatrix inner_prod = DataUtils.toSmpMatrix(DataUtils.allzeroVector(batchSize)).scale(-1);
                    double para_norm = Math.pow(inner_prod.normF(), 2);
                    if (isActive == true) {
                        inner_prod = inner_prod.plus(DataUtils.toSmpMatrix(DataUtils.arrayToVector(y_train_sub)));
                    }
                    res = new TrainRes(client, DataUtils.vectorToArray(DataUtils.toVector(inner_prod)), para_norm, isActive);

                }
                isInitTrain = true;
            } else {
                if (hasFeature == true) {
                    List<Integer> sampleIndex = req.getSampleIndex();
                    Xs_train_trans_sub = getSubTrainFeat(Xs_train_trans, sampleIndex, batchSize);
                    SimpleMatrix inner_prod = Xs_train_trans_sub.mult(DataUtils.toSmpMatrix(modelPara)).scale(-1);
                    if (isActive == true) {
                        y_train_sub = getSubTrainLabel(y_train, sampleIndex, batchSize);
                        inner_prod = inner_prod.plus(DataUtils.toSmpMatrix(DataUtils.arrayToVector(y_train_sub)));
                    }
                    res = new TrainRes(client, DataUtils.vectorToArray(DataUtils.toVector(inner_prod)), 0.0, isActive);
                } else {
                    List<Integer> sampleIndex = req.getSampleIndex();
                    modelPara = DataUtils.allzeroVector((int) mapdim);
                    SimpleMatrix inner_prod = DataUtils.toSmpMatrix(DataUtils.allzeroVector(batchSize));
                    double para_norm = 0;
                    if (isActive == true) {
                        y_train_sub = getSubTrainLabel(y_train, sampleIndex, batchSize);
                        inner_prod = inner_prod.plus(DataUtils.toSmpMatrix(DataUtils.arrayToVector(y_train_sub)));
                    }
                    res = new TrainRes(client, DataUtils.vectorToArray(DataUtils.toVector(inner_prod)), para_norm, isActive);
                }
            }
            logger.info("Phase 1 finish.");
        }
        if (phase == 2) {
            ClientInfo clientInfo = req.getClient();
            boolean isUpdate = req.isUpdate();
            List<Integer> sampleIndex = req.getSampleIndex();
            SimpleMatrix tmp = DataUtils.toSmpMatrix(DataUtils.arrayToVector(req.getValueList()));
            SimpleMatrix inner_prod = Xs_train_trans_sub.mult(DataUtils.toSmpMatrix(modelPara));
            Vector y = DataUtils.toVector(inner_prod.plus(tmp));
            if (isUpdate == true) {
                logger.info(String.format("%s update model parameter!", clientInfo));
                logger.info(String.format("bath size is %s", batchSize));
                if (hasFeature == true) {
                    Xs_train_trans_sub = getSubTrainFeat(Xs_train_trans, sampleIndex, batchSize);
                    InputMessage msg = DataUtils.prepareInputMessage(new Matrix[]{DataUtils.toMatrix(Xs_train_trans_sub)},
                            new Vector[]{y}, new Double[]{});
                    OutputMessage responsePhase1 = stub.ceKernelLinearRegressionPhase1(msg);
                    modelPara = responsePhase1.getVectors(0);
                } else {
                    modelPara = DataUtils.allzeroVector((int) mapdim);
                }
                String message = "Phase 2 finish!";
                res = new TrainRes(client, message, isActive);
                logger.info("Phase 2 finish.");
            } else {
                String message = "Phase 2 finish!";
                res = new TrainRes(client, message, isActive);
            }
        }
        return res;
    }


    public void inferenceInit() {
        if ("".equals(stubAddress)) {
            stubAddress = "127.0.0.1:8891";
        }
        //String target = "127.0.0.1:8891";
        ManagedChannel channel = ManagedChannelBuilder.forTarget(stubAddress).usePlaintext().maxInboundMessageSize(Integer.MAX_VALUE).build();
        stub = CalculateGrpc.newBlockingStub(channel);
    }

    // 推理初始化，
    public Message inferenceInit(String[] uidList, String[][] inferenceCacheFile, Map<String, Object> others) {
        return InferenceFilter.filter(uidList, inferenceCacheFile);
    }

    public Message inference(int phase, Message jsonData, InferenceData Data) {
        logger.info(String.format("Inference process, phase %s start", phase) + splitLine);
        String responseStr = "";
        if (jsonData == null || Data.getDatasetSize() == 0) {
            return null;
        }
        InferenceReqAndRes res = null;
        CommonInferenceData inferenceData = (CommonInferenceData) Data;
        if (phase == -1) {
            String[] newUid = ((InferenceInit) jsonData).getUid();
            inferenceData.filterOtherUid(newUid);
            if (inferenceData.getFeatureDim() > 0) {
                // normalization
                double[][] mxSample;
                switch (this.normalizationType) {
                    case MINMAX:
                        logger.info("In inference of KernelLinearRegressionModel, do minmax normalization.");
                        mxSample = inferenceData.getSample();
                        Normalizer.MinMaxScaler(mxSample, norm_params_1, norm_params_2);
                        Xs_test = new SimpleMatrix(mxSample);
                        break;
                    case STANDARD:
                        logger.info("In inference of KernelLinearRegressionModel, do standard normalization.");
                        mxSample = inferenceData.getSample();
                        Normalizer.StandardScaler(mxSample, norm_params_1, norm_params_2);
                        Xs_test = new SimpleMatrix(mxSample);
                        break;
                    default:
                        logger.info("In inference of KernelLinearRegressionModel, do NO normalization.");
                        Xs_test = new SimpleMatrix(inferenceData.getSample());
                        break;
                }
                // Xs_test = new SimpleMatrix(inferenceData.getSample());
            } else {
                logger.info("In inference of KernelLinearRegressionModel, feature_dim does not support for normalization.");
            }
            //
            logger.info(String.format("Data dimension: %s", inferenceData.getFeatureDim()));
            if (inferenceData.getFeatureDim() == 0) {
                Xs_test = DataUtils.toSmpMatrix(DataUtils.zeroMatrix(inferenceData.getDatasetSize(), 1));
                hasFeature = false;
            }
        }


        int i, j;
        int numsample = 0;
        int batchSize = 0;
        int numbatch = 0;
        if (Xs_test != null) {
            numsample = Xs_test.numRows();
            //TODO batchSize取值暂时修改
            //batchSize = Math.min(parameter.getBatchSize(), numsample);
            batchSize = numsample;
            numbatch = (int) Math.ceil((double) numsample / batchSize);
        }
        if (phase == -2) {
            //KernelLinearRegressionInferInitReq req = new KernelLinearRegressionInferInitReq();
            //req.parseJson(jsonData);
            //modelPara = req.getModel();
            inferenceInit();
            if (hasFeature == true) {
                Xs_test_trans = new SimpleMatrix(numsample, (int) mapdim);
                for (i = 0; i < numbatch; i++) {
                    int start = i * batchSize;
                    int end = Math.min(numsample, (i + 1) * batchSize);
                    logger.info(String.format("Processing block %s, from sample %s to sample %s.", i, start, end));
                    SimpleMatrix temp = Xs_test.rows(start, end);
                    InputMessage msg = DataUtils.prepareInputMessage(new Matrix[]{DataUtils.toMatrix(temp),
                            DataUtils.toMatrix(Trans_mat)}, new Vector[]{bias}, new Double[]{});
                    OutputMessage responsePhase0 = stub.kernelApproximationPhase2(msg);
                    SimpleMatrix Xs_test_trans_sub = DataUtils.toSmpMatrix(responsePhase0.getMatrices(0));
                    Xs_test_trans = setSmpMatValue(Xs_test_trans, Xs_test_trans_sub, start, end);
                }
            } else {
                Xs_test_trans = null;
            }
        }
        if (phase == -3) {
            InferenceReqAndRes req = (InferenceReqAndRes) jsonData;
            ClientInfo client = req.getClient();
            ArrayList<Double> result = new ArrayList<Double>();
            if (hasFeature == true) {
                for (i = 0; i < numbatch; i++) {
                    int start = i * batchSize;
                    int end = Math.min(numsample, (i + 1) * batchSize);
                    logger.info(String.format("Processing block %s, from sample %s to sample %s.", i, start, end));
                    SimpleMatrix temp = Xs_test_trans.rows(start, end);
                    InputMessage msg = DataUtils.prepareInputMessage(new Matrix[]{DataUtils.toMatrix(temp)}, new Vector[]{modelPara}, new Double[]{});
                    OutputMessage responsePhase1 = stub.kernelLinearRegressionInferencePhase1(msg);
                    Vector inner_prod = responsePhase1.getVectors(0);
                    for (j = start; j < end; j++) {
                        if (hasFeature == true) {
                            result.add(j, inner_prod.getValues(j - start));
                        } else {
                            result.add(j, 0.0);
                        }
                    }
                }
            } else {
                for (i = 0; i < numsample; i++) {
                    result.add(i, 0.0);
                }
            }
            logger.info("Inner product result");
//            for (i = 0; i < numsample; i++) {
//                logger.info(String.format("Entry %s value is %s", i, result.get(i)));
//            }
            uId = inferenceData.getUid();
            Map<String, Double> predict = new HashMap<>();
            if (uId.length == result.size()) {
                for (int k = 0; k < uId.length; k++) {
                    predict.put(uId[k], result.get(k));
                }
                res = new InferenceReqAndRes(client, predict);
            } else {
                logger.error("Inference Data shape doesn't match!");
            }
        }
        logger.info(String.format("Inference process, phase %s end", phase) + splitLine);
        return res;
    }

    public void deserialize(String content) {
        String[] lines = content.split("\n");
        int line_num = 0;
        String modelToken = lines[line_num].split("=")[1];
        line_num++; // 1
        String csvWeight = lines[line_num].split("=")[1];
        line_num++; // 2
        String[] tmp = csvWeight.split(",");
        double[] modelweight = new double[tmp.length];
        int i, j;
        logger.info("parsing model weight");
        for (i = 0; i < tmp.length; i++) {
            modelweight[i] = Double.parseDouble(tmp[i]);
            logger.info(String.format("Entry %s, value is %s", i, modelweight[i]));
        }
        this.modelToken = modelToken;
        this.modelPara = DataUtils.arrayToVector(modelweight);

        String matSize = lines[line_num].split("=")[1];
        line_num++; // 3
        int numRow = Integer.parseInt(matSize.split(",")[0]);
        int numCol = Integer.parseInt(matSize.split(",")[1]);
        if (numRow != 0 && numCol != 0) {
            String matWeight = lines[line_num].split("=")[1];
            line_num++; // 4 or not do
            tmp = matWeight.split(",");
            double[][] matweight = new double[numRow][numCol];
            int k = 0;
            logger.info("Parsing weight matrix");
            for (i = 0; i < numRow; i++) {
                for (j = 0; j < numCol; j++) {
                    matweight[i][j] = Double.parseDouble(tmp[k]);
                    logger.info(String.format("Entry at position %s, %s, value is %s", i, j, matweight[i][j]));
                    k += 1;
                }
            }

            this.Trans_mat = new SimpleMatrix(matweight);
            this.mapdim = numCol;
            String biasWeight = lines[line_num].split("=")[1];
            line_num++; // 5 or not do
            tmp = biasWeight.split(",");
            double[] biasweight = new double[tmp.length];
            logger.info("Parsing bias weight");
            for (i = 0; i < tmp.length; i++) {
                biasweight[i] = Double.parseDouble(tmp[i]);
                logger.info(String.format("Entry %s, value is %s", i, biasweight[i]));
            }
            this.bias = DataUtils.arrayToVector(biasweight);
        }
        // load normalization parameters
        normalizationType = normalizationType.valueOf(lines[line_num].split("=")[1]);
        line_num++; // 3 or 5
        String str_norm_params_1 = lines[line_num].split("=")[1];
        line_num++; // 4 or 6
        tmp = str_norm_params_1.split(",");
        norm_params_1 = new double[tmp.length];
        for (i = 0; i < tmp.length; i++) {
            norm_params_1[i] = Double.parseDouble(tmp[i]);
            logger.info(String.format("norm_params_1 %s, value is %s", i, norm_params_1[i]));
        }
        String str_norm_params_2 = lines[line_num].split("=")[1];
        tmp = str_norm_params_2.split(",");
        norm_params_2 = new double[tmp.length];
        for (i = 0; i < tmp.length; i++) {
            norm_params_2[i] = Double.parseDouble(tmp[i]);
            logger.info(String.format("norm_params_2 %s, value is %s", i, norm_params_2[i]));
        }
    }

    public String serialize() {
        StringBuilder sb = new StringBuilder();
        sb.append("modelToken=").append(modelToken).append("\n");
        StringBuilder csvWeight = new StringBuilder();
        for (double w : DataUtils.vectorToArray(modelPara)) {
            csvWeight.append(w).append(",");
        }
        csvWeight = new StringBuilder(csvWeight.substring(0, csvWeight.length() - 1));
        sb.append("weight=").append(csvWeight).append("\n");
        int numRow, numCol;
        if (Trans_mat != null) {
            numRow = Trans_mat.numRows();
            numCol = Trans_mat.numCols();
        } else {
            numRow = 0;
            numCol = 0;
        }
        sb.append("matsize=").append(numRow).append(',').append(numCol).append("\n");
        if (numRow != 0 && numCol != 0) {
            StringBuilder matWeight = new StringBuilder();
            for (int i = 0; i < numRow; i++) {
                for (int j = 0; j < numCol; j++) {
                    matWeight.append(Trans_mat.get(i, j)).append(',');
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
        // save normalization parameters
        // sb.append("\n");
        sb.append("norm_type=").append(normalizationType.toString()).append("\n");
        StringBuilder str_norm_params_1 = new StringBuilder();
        for (int i = 0; i < norm_params_1.length; i++) {
            str_norm_params_1.append(norm_params_1[i]).append(',');
        }
        str_norm_params_1 = new StringBuilder(str_norm_params_1.substring(0, str_norm_params_1.length() - 1));
        sb.append("norm_params_1=").append(str_norm_params_1);
        sb.append("\n");
        StringBuilder str_norm_params_2 = new StringBuilder();
        for (int i = 0; i < norm_params_2.length; i++) {
            str_norm_params_2.append(norm_params_2[i]).append(',');
        }
        str_norm_params_2 = new StringBuilder(str_norm_params_2.substring(0, str_norm_params_2.length() - 1));
        sb.append("norm_params_2=").append(str_norm_params_2);
        sb.append("\n");
        //
        return sb.toString();
    }

    public AlgorithmType getModelType() {
        return AlgorithmType.KernelBinaryClassification;
    }

}