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

import com.jdt.fedlearn.core.encryption.common.Ciphertext;
import com.jdt.fedlearn.core.encryption.common.EncryptionTool;
import com.jdt.fedlearn.core.encryption.common.PublicKey;
import com.jdt.fedlearn.core.encryption.differentialPrivacy.DifferentialPrivacyFactory;
import com.jdt.fedlearn.core.encryption.differentialPrivacy.IDifferentialPrivacy;
import com.jdt.fedlearn.core.encryption.paillier.PaillierTool;
import com.jdt.fedlearn.common.entity.core.ClientInfo;
import com.jdt.fedlearn.common.entity.core.Message;
import com.jdt.fedlearn.core.entity.base.DoubleArray;
import com.jdt.fedlearn.core.entity.base.StringArray;
import com.jdt.fedlearn.common.entity.core.feature.Features;
import com.jdt.fedlearn.core.loader.common.CommonInferenceData;
import com.jdt.fedlearn.core.loader.common.InferenceData;
import com.jdt.fedlearn.core.loader.verticalLinearRegression.VerticalLinearTrainData;
import com.jdt.fedlearn.core.parameter.HyperParameter;
import com.jdt.fedlearn.core.model.common.Regularization;
import com.jdt.fedlearn.core.loader.common.TrainData;
import com.jdt.fedlearn.core.parameter.VerticalLinearParameter;
import com.jdt.fedlearn.core.math.MathExt;
import com.jdt.fedlearn.core.entity.verticalLinearRegression.*;
import com.jdt.fedlearn.core.preprocess.InferenceFilter;
import com.jdt.fedlearn.core.preprocess.Scaling;
import com.jdt.fedlearn.core.model.serialize.LinearModelSerializer;
import com.jdt.fedlearn.common.entity.core.type.AlgorithmType;
import com.jdt.fedlearn.core.type.DifferentialPrivacyType;
import com.jdt.fedlearn.core.type.VerLinModelPhaseType;
import com.jdt.fedlearn.core.util.Tool;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.stream.IntStream;

/**
 * 纵向线性回归模型
 */
public class VerticalLinearModel implements Model {
    private static final Logger logger = LoggerFactory.getLogger(VerticalLinearModel.class);
    private String modelToken;
    private double[] weight; // weight of the last index is b
    private boolean isInit = true;
    private PublicKey pubKey;
    private VerticalLinearParameter parameter;
    private double maxGradient = 100;
    private Scaling scaling = new Scaling();
    private double[] random;
    private IDifferentialPrivacy differentialPrivacy;
    private EncryptionTool encryptionTool = new PaillierTool();// new FakeTool();//new PaillierTool();//new JavallierTool();
    private List<String> expressions = new ArrayList<>();

    public VerticalLinearModel() {

    }

    public VerticalLinearModel(String modelToken, double[] weight, Scaling scaling) {
        this.modelToken = modelToken;
        this.weight = weight;
        this.scaling = scaling;
    }

    public VerticalLinearModel(boolean isInit, VerticalLinearParameter parameter, EncryptionTool encryptionTool,
                               double[] weight, Scaling scaling) {
        this.isInit = isInit;
        this.parameter = parameter;
        this.encryptionTool = encryptionTool;
        this.weight = weight;
        this.scaling = scaling;
    }

    public VerticalLinearModel(boolean isInit, VerticalLinearParameter parameter, EncryptionTool encryptionTool,
                               double[] weight, Scaling scaling, PublicKey pubKey) {
        this.isInit = isInit;
        this.parameter = parameter;
        this.encryptionTool = encryptionTool;
        this.weight = weight;
        this.scaling = scaling;
        this.pubKey = pubKey;
    }

    public VerticalLinearModel(boolean isInit, VerticalLinearParameter parameter, EncryptionTool encryptionTool,
                               double[] weight, Scaling scaling, PublicKey pubKey, double[] random) {
        this.isInit = isInit;
        this.parameter = parameter;
        this.encryptionTool = encryptionTool;
        this.weight = weight;
        this.scaling = scaling;
        this.pubKey = pubKey;
        this.random = random;
    }


    /**
     * 初始化参数
     *
     * @param rawData        原始数据
     * @param hyperParameter 超参数
     * @param uids           用户id对照表
     * @param testIndex      验证id的index
     * @param features       特征
     * @param others         其他参数
     * @return
     */
    @Override
    public VerticalLinearTrainData trainInit(String[][] rawData, String[] uids, int[] testIndex, HyperParameter hyperParameter, Features features, Map<String, Object> others) {
        parameter = (VerticalLinearParameter) hyperParameter;

        VerticalLinearTrainData trainData = new VerticalLinearTrainData(rawData, uids, features, parameter.isUseDP());
        this.expressions = trainData.getExpressions();
        //初始化预测值和gradient hessian
        logger.info("actual received features:" + features.getFeatureList().toString());
        logger.info("client data dim:" + trainData.getDatasetSize() + "," + trainData.getFeatureDim());
        int datasetSize = trainData.getDatasetSize();
        logger.info("client parameter init");
        // 对于每一个feature和intercept生成初始化参数=0
        weight = Tool.initWeight1(trainData.getFeatureDim());
        scaling = trainData.getScaling(); // new VerticalLinearTrainData做过minMaxScaling
        if (this.parameter.isUseDP()) {
            this.differentialPrivacy = DifferentialPrivacyFactory.createDifferentialPrivacy(this.parameter.getDpType());
            this.differentialPrivacy.init(this.weight.length, this.parameter.getMaxEpoch(), datasetSize, this.parameter.getDpEpsilon(),
                    this.parameter.getDpDelta(), this.parameter.getDpLambda(), this.parameter.getEta(), this.parameter.getSeed());
            this.differentialPrivacy.generateNoises();
        }
        return trainData;
    }


    @Override
    public Message train(int phase, Message masterRetMsg, TrainData rawData) {
        VerticalLinearTrainData trainData = (VerticalLinearTrainData) rawData;
        switch (VerLinModelPhaseType.valueOf(phase)) {
            case PASSIVE_LOCAL_PREDICT:
                return trainPhase1(masterRetMsg, trainData);
            case COMPUTE_DIFFERENT:
                return trainPhase2(masterRetMsg, trainData);
            case COMPUTE_GRADIENTS:
                return trainPhase3(masterRetMsg, trainData);
            case UPDATE_WEIGHTS:
                return trainPhase4(masterRetMsg);
            default:
                throw new UnsupportedOperationException();
        }
    }

    /**
     * 使用全局变量 isInit, encryptionTool, weight
     * 更新全局变量 pubKey, isInit,
     *
     * @param data      generated from controlPhase1
     * @param trainData generated from trainInit
     * @return
     */
    private LinearP1Response trainPhase1(Message data, VerticalLinearTrainData trainData) {
        LinearP1Request linearP1Request = (LinearP1Request) data;
        ClientInfo clientInfo = linearP1Request.getClient();
        //首次请求初始化参数
        // first call, init parameter
        if (isInit) {
            String stringPubKey = linearP1Request.getPubKey();
            pubKey = encryptionTool.restorePublicKey(stringPubKey);
//            pubKey = new FakePubKey().deserialize(stringPubKey);
            isInit = false;
        }
        //第一轮，有label的数据无需处理
        if (trainData.hasLabel) {
            return new LinearP1Response(clientInfo, new String[0][], "");
        }
        //u is local y_predict
        //todo 共有特征权重 除以系数
        double[] u = MathExt.forward1(trainData.getFeature(), weight); // W_B * X_B
        // datasize x 2: the 1st col is index, the 2nd is encrypted prediction
        String[][] compoundU = new String[u.length][2];
        for (int i = 0; i < u.length; i++) {
            double partU = u[i];
            String secureU = encryptionTool.encrypt(partU, pubKey).serialize();
            compoundU[i][0] = i + "";
            compoundU[i][1] = secureU;
        }
        //因为没有label，所以计算的是local loss，不是真的loss
        // 当期那不包含lambda*WA2/2+lambda*WAB2/4
        double localLoss = 0;
        for (double v : u) {
            localLoss += v * v;
        }
        double lossSquare = localLoss * localLoss;
        String cipherLoss = encryptionTool.encrypt(lossSquare, pubKey).serialize();
        LinearP1Response res = new LinearP1Response(clientInfo, compoundU, cipherLoss);
        return res;
    }

    //返回loss和d
    private LossGradients trainPhase2(Message data, VerticalLinearTrainData trainData) {
        // get all trainPhase1 res from all clients
        LinearP2Request linearP2Request = (LinearP2Request) data;
        // TODO 此处需优化，当client较多时，会出现性能问题
        if (!trainData.hasLabel) {
            return new LossGradients(new ClientInfo(), new String[0], new String[0]);
        }
        double[] label = trainData.getLabel();
        // labeled u
        double[] localU = new double[label.length];
        if (trainData.getFeature().length != 0 && trainData.getFeature() != null) {
            localU = MathExt.forward1(trainData.getFeature(), weight); // wx+b
        }
        ClientInfo thisClient = null;
        if (linearP2Request.getClient() != null) {
            thisClient = linearP2Request.getClient();
        }
        Ciphertext[] encDiff = computeDiff(linearP2Request, thisClient, localU, label);
        //d是公式求导的中间结果，
        Ciphertext[] d1 = computeD(trainData, linearP2Request, thisClient, localU, label);
        // LB=sum用户(uB-label)2+lambda*WB2/2+lambda*WAB2/4; 当前仅包含用户(uB-label)
        String[] globalEncUStr = IntStream.range(0, encDiff.length).boxed().map(i -> encDiff[i].serialize()).toArray(String[]::new);
        //[[uB-label]] + [[uA]]
        String[] d1Str = IntStream.range(0, d1.length).boxed().map(i -> d1[i].serialize()).toArray(String[]::new);
        return new LossGradients(thisClient, globalEncUStr, d1Str);
    }

    /**
     * 在Active方计算label和pred的差距之后，将各个被动方的加密后的pred都加上，即得到全局的label和pred的diff
     *
     * @param linearP2Request 每一个client的encrypted local_pred和loss
     * @param thisClient
     * @param localU          wBxB + b
     * @param label
     * @return
     */
    private Ciphertext[] computeDiff(LinearP2Request linearP2Request, ClientInfo thisClient, double[] localU, double[] label) {
        // local端的encrypted diff [[uB-label]]
        Ciphertext[] encLocalDiff = IntStream.range(0, label.length).mapToObj(i -> encryptionTool.encrypt(localU[i] - label[i], pubKey)).toArray(Ciphertext[]::new);
        // global的encrypted diff
        Ciphertext[] globalEncU = new Ciphertext[encLocalDiff.length];
        System.arraycopy(encLocalDiff, 0, globalEncU, 0, encLocalDiff.length);
        for (LinearP1Response d : linearP2Request.getBodies()) {
            if (!d.getClient().equals(thisClient)) {
                String[][] otherU = d.getU();
                Ciphertext[] finalGlobalEncU = globalEncU;
                // [[uB-label]] + [[uA]]
                globalEncU = IntStream.range(0, localU.length).boxed().map(i -> encryptionTool.add(finalGlobalEncU[i], encryptionTool.restoreCiphertext(otherU[i][1]), pubKey)).toArray(Ciphertext[]::new);
            }
        }
        return globalEncU;
    }

    /**
     * @param trainData
     * @param linearP2Request 每一个client的encrypted local_pred和loss
     * @param thisClient
     * @param localU          wx + b
     * @param label
     * @return
     */
    private Ciphertext[] computeD(VerticalLinearTrainData trainData, LinearP2Request linearP2Request, ClientInfo thisClient, double[] localU, double[] label) {
        //数组默认初始化为0
        Ciphertext[] d1 = new Ciphertext[trainData.getDatasetSize()];
        for (int i = 0; i < d1.length; i++) {
            d1[i] = encryptionTool.encrypt(0, pubKey);
        }
        for (int j = 0; j < trainData.getDatasetSize(); j++) {
            Ciphertext sumU = encryptionTool.encrypt(0, pubKey);
            for (LinearP1Response d : linearP2Request.getBodies()) {
                String[][] u = d.getU();
                if (d.getClient().equals(thisClient)) {
                    Ciphertext tmp = encryptionTool.encrypt(localU[j], pubKey);
                    sumU = encryptionTool.add(sumU, tmp, pubKey);
                } else {
                    sumU = encryptionTool.add(sumU, encryptionTool.restoreCiphertext(u[j][1]), pubKey);
                }
            }
            Ciphertext cipherLabel = encryptionTool.encrypt(-label[j], pubKey);
            Ciphertext cipherError = encryptionTool.add(sumU, cipherLabel, pubKey); // 单个样本的global_pred - label
            d1[j] = encryptionTool.add(d1[j], cipherError, pubKey);
        }
        return d1;
    }

    //计算同态加密状态的gradient; 每个client分别计算各自的gradient
    private LossGradients trainPhase3(Message data, VerticalLinearTrainData trainData) {

        LossGradients lossgradients = (LossGradients) data;
        String[] d = lossgradients.getGradient();
        // 计算 gradient //数组默认初始化为0 //初始化为随机变量;
        Ciphertext[] gradient0 = new Ciphertext[trainData.getFeatureDim()]; // feat's gradient
        for (int i = 0; i < gradient0.length; i++) {
            gradient0[i] = encryptionTool.encrypt(0, pubKey);
        }
        Ciphertext gradient1 = encryptionTool.encrypt(0, pubKey); // b's gradient
        if (trainData.getDatasetSize() != 0 && trainData.getFeature().length != 0 && trainData.getFeature() != null) {
            for (int j = 0; j < trainData.getDatasetSize(); j++) {
                for (int k = 0; k < trainData.getFeature()[j].length; k++) {
                    // 第j个样本第k个特征/样本数量
                    double tmp = trainData.getFeature()[j][k] / trainData.getDatasetSize();
                    // 第j个样本的uB-label乘第j个样本第k个特征/样本数量
                    Ciphertext g = encryptionTool.multiply(encryptionTool.restoreCiphertext(d[j]), tmp, pubKey);
                    gradient0[k] = encryptionTool.add(gradient0[k], g, pubKey);
                }
                gradient1 = encryptionTool.add(gradient1, encryptionTool.restoreCiphertext(d[j]), pubKey);
            }
            gradient1 = encryptionTool.multiply(gradient1, 1.0 / trainData.getDatasetSize(), pubKey);
        }
        Ciphertext[] gradients = Tool.arrayAppend(gradient0, gradient1);
        double[] reg = Regularization.regularization(weight, parameter.getRegularization(), parameter.getLambda());
        random = Tool.generateRandom(gradients.length, 0, 1);
        for (int i = 0; i < gradients.length; i++) {
            if (trainData.getDatasetSize() != 0) {
                //加入随机数与差分隐私
                Ciphertext l1 = encryptionTool.encrypt(reg[i], pubKey);
                Ciphertext encarpRandom = encryptionTool.encrypt(random[i], pubKey);
                // 加上regularization term
                gradients[i] = encryptionTool.add(gradients[i], l1, pubKey);
//                gradients[i] = encryptionTool.add(gradients[i], homoNoise, pubKey);
                // 加上随机数
                gradients[i] = encryptionTool.add(gradients[i], encarpRandom, pubKey);
            }
        }
        String[] gradientsS = IntStream.range(0, gradients.length).boxed().map(i -> gradients[i].serialize()).toArray(String[]::new);
        LossGradients res = new LossGradients(lossgradients.getClient(), lossgradients.getLoss(), gradientsS);
        return res;
    }

    //
    private GradientsMetric trainPhase4(Message data) {

        GradientsMetric GradientsMetric = (GradientsMetric) data;
        double[] gradients = GradientsMetric.getGradients();
        // 目标扰动，求解时根据目标函数对梯度进行噪声添加
        if (this.parameter.isUseDP() && DifferentialPrivacyType.OBJECTIVE_PERTURB.equals(this.parameter.getDpType())) {
            this.differentialPrivacy.addNoises(gradients, weight);
        }
        //去除phase3 gradients增加的随机数
        if (weight.length > 1) {
            for (int i = 0; i < weight.length; i++) {
                weight[i] = weight[i] + gradients[i] + parameter.getEta() * random[i];
            }
        }
        //根据传入的metric类型 计算 metric
        GradientsMetric res = new GradientsMetric(GradientsMetric.getMetric());
//        res.setMetric(GradientsMetric.getMetric());
        double tmpMaxGradient = -Double.MAX_VALUE;
        for (double gradient : gradients) {
            if (tmpMaxGradient < gradient) {
                tmpMaxGradient = gradient;
            }
        }
        maxGradient = Math.min(maxGradient, tmpMaxGradient);
        return res;
    }

    // 推理初始化，判断推理id是否存在推理数据库中
    public Message inferenceInit(String[] uidList, String[][] inferenceCacheFile, Map<String, Object> others) {
        return InferenceFilter.filter(uidList, inferenceCacheFile);
    }

    @Override
    public Message inference(int phase, Message masterRetMsg, InferenceData inferenceData) {
        CommonInferenceData data = (CommonInferenceData) inferenceData;
        Message res;
        switch (VerLinModelPhaseType.valueOf(phase)) {
            case DO_inferencePhase:
                res = inferencePhase1(masterRetMsg, data);
                break;
            default:
                throw new UnsupportedOperationException();
        }
        return res;
    }


    private DoubleArray inferencePhase1(Message message, CommonInferenceData data) {
        String[] newUidIndex = ((StringArray) message).getData();
        data.filterOtherUid(newUidIndex);
        if (scaling != null) {
            scaling.inferenceMinMaxScaling(data.getSample());
        }
        double[] res = MathExt.forward1(data.getSample(), weight);
        DoubleArray response = new DoubleArray(res);
//        response.setU(res);
        return response;
    }


    @Override
    public String serialize() {
        // 输出扰动需要在保存模型之前对模型的参数进行加噪声
        if (this.parameter != null && this.parameter.isUseDP() && DifferentialPrivacyType.OUTPUT_PERTURB.equals(this.parameter.getDpType())) {
            this.differentialPrivacy.addNoises(this.weight, this.weight);
        }
//        return LinearModelSerializer.saveModelVrticalLinear(modelToken, weight, scaling);
        return Tool.addExpressions(LinearModelSerializer.saveModelVrticalLinear(modelToken, weight, scaling), this.expressions);
    }


    public void deserialize(String content) {
        String[] contents = Tool.splitExpressionsAndModel(content);
        this.expressions = Tool.splitExpressions(contents[0]);
        VerticalLinearModel model = LinearModelSerializer.loadVerticalLinearModel(contents[1]);
        this.modelToken = model.modelToken;
        this.weight = model.weight;
        this.scaling = model.scaling;
    }

    public AlgorithmType getModelType() {
        return AlgorithmType.VerticalLinearRegression;
    }

    public IDifferentialPrivacy getDifferentialPrivacy(){
        return this.differentialPrivacy;
    }

    public List<String> getExpressions() {
        return expressions;
    }

}
