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
import com.jdt.fedlearn.core.encryption.javallier.JavallierTool;
import com.jdt.fedlearn.common.entity.core.ClientInfo;
import com.jdt.fedlearn.common.entity.core.Message;
import com.jdt.fedlearn.core.entity.base.DoubleArray;
import com.jdt.fedlearn.core.entity.base.StringArray;
import com.jdt.fedlearn.common.entity.core.feature.Features;
import com.jdt.fedlearn.core.entity.verticalLinearRegression.*;
import com.jdt.fedlearn.core.loader.common.CommonInferenceData;
import com.jdt.fedlearn.core.loader.common.InferenceData;
import com.jdt.fedlearn.core.loader.common.TrainData;
import com.jdt.fedlearn.core.loader.verticalLinearRegression.VerticalLinearTrainData;
import com.jdt.fedlearn.core.math.MathExt;
import com.jdt.fedlearn.core.model.common.loss.LogisticLoss;
import com.jdt.fedlearn.core.parameter.HyperParameter;
import com.jdt.fedlearn.core.parameter.VerticalLRParameter;
import com.jdt.fedlearn.core.preprocess.InferenceFilter;
import com.jdt.fedlearn.core.preprocess.Scaling;
import com.jdt.fedlearn.core.model.serialize.LinearModelSerializer;
import com.jdt.fedlearn.common.entity.core.type.AlgorithmType;
import com.jdt.fedlearn.core.util.Tool;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.IntStream;

import com.jdt.fedlearn.core.type.*;

/**
 * 纵向线性回归模型
 */
public class VerticalLRModel implements Model {
    private static final Logger logger = LoggerFactory.getLogger(VerticalLRModel.class);
    private String modelToken;
    private double[] weight;
    private boolean isInit = true;
    private PublicKey pubKey;
    private VerticalLRParameter parameter;
    private double maxGradient = 100;
    private double[] label;
    private Scaling scaling;
    private LogisticLoss logisticLoss;
    private IDifferentialPrivacy differentialPrivacy;
    EncryptionTool encryptionTool = new JavallierTool();
    private List<String> expressions = new ArrayList<>();

    public VerticalLRModel() {

    }

    public VerticalLRModel(String modelToken, double[] weight, Scaling scaling) {
        this.modelToken = modelToken;
        this.weight = weight;
        this.scaling = scaling;
    }

    @Override
    public VerticalLinearTrainData trainInit(String[][] rawData, String[] uids, int[] testIndex,
                                             HyperParameter hyperParameter, Features features, Map<String, Object> others) {
        parameter = (VerticalLRParameter) hyperParameter;
        VerticalLinearTrainData trainData = new VerticalLinearTrainData(rawData, uids, features, this.parameter.isUseDP());
        this.expressions = trainData.getExpressions();
        //初始化预测值和gradient hessian
        logger.info("actual received features:" + features.getFeatureList().toString());
        logger.info("client data dim:" + trainData.getDatasetSize() + "," + trainData.getFeatureDim());
        this.label = trainData.getLabel();
        int datasetSize = trainData.getDatasetSize();
        logger.info("client parameter init");
        weight = Tool.initWeight1(trainData.getFeatureDim());
        scaling = trainData.getScaling();
        logisticLoss = new LogisticLoss();
        // 如果是目标或者输出扰动的话，则提前生成高斯噪声
        if (this.parameter.isUseDP()) {
            this.differentialPrivacy = DifferentialPrivacyFactory.createDifferentialPrivacy(this.parameter.getDpType());
            this.differentialPrivacy.init(this.weight.length, this.parameter.getMaxEpoch(), datasetSize, this.parameter.getDpEpsilon(),
                    this.parameter.getDpDelta(), this.parameter.getLamba(), this.parameter.getEta(), this.parameter.getSeed());
            this.differentialPrivacy.generateNoises();
        }
        return trainData;
    }


    @Override
    public Message train(int phase, Message masterRetMsg, TrainData rawData) {
        VerticalLinearTrainData trainData = (VerticalLinearTrainData) rawData;
        switch (VerLRModelPhaseType.valueOf(phase)) {
            case PASSIVE_LOCAL_PREDICT:
                //广播表头等属性，客户端根据表头，以及特征是否完整，是否有label 计算u和loss，并返回
                return trainPhase1(masterRetMsg, trainData);
            case COMPUTE_DIFFERENT:
                //接受其他client的计算结果，综合计算, 返回综合loss和偏导数
                return trainPhase2(masterRetMsg, trainData);
            case COMPUTE_GRADIENTS:
                //根据计算出的偏导数更新权值
                return trainPhase3(masterRetMsg, trainData);
            case UPDATE_WEIGHTS:
                return trainPhase4(masterRetMsg);
            default:
                throw new UnsupportedOperationException();
        }
    }


    private LinearP1Response trainPhase1(Message data, VerticalLinearTrainData trainData) {

        //首次请求初始化参数
        // first call, init parameter
        ClientInfo clientInfo = null;
        if (isInit) {
            LinearP1Request linearP1Request = (LinearP1Request) data;
            clientInfo = linearP1Request.getClient();
            pubKey = encryptionTool.restorePublicKey(linearP1Request.getPubKey());
            isInit = false;
        }

        //第一轮，有label的数据无需处理
        if (trainData.hasLabel) {
            return new LinearP1Response(new ClientInfo(), new String[0][], "");
        }

        // 无标签方计算 wx
        double[] u = MathExt.forward1(trainData.getFeature(), weight);
        String[][] compoundU = new String[u.length][2];
        for (int i = 0; i < u.length; i++) {
            double partU = u[i];
            String secureU = encryptionTool.encrypt(partU, pubKey).serialize();
            compoundU[i][0] = i + "";
            compoundU[i][1] = secureU;
        }
        // T因为没有label，所以计算的是local loss，不是真的loss
        double localLoss = 0;
        for (double v : u) {
            localLoss += v * v;
        }
        double lossSquare = localLoss * localLoss;
        String cipherLoss = encryptionTool.encrypt(lossSquare, pubKey).serialize(); // TODO: this field is never used ??
        LinearP1Response res = new LinearP1Response(clientInfo, compoundU, cipherLoss);
        return res;
    }

    //返回loss和d
    private LossGradients trainPhase2(Message data, VerticalLinearTrainData trainData) {
        // get all trainPhase1 res from all clients
        LinearP2Request linearP2Request = (LinearP2Request) data;
        ClientInfo thisClient = null;
        if (linearP2Request.getClient() != null) {
            thisClient = linearP2Request.getClient();
        }
        // TODO 此处需优化，当client较多时，会出现性能问题
        if (!trainData.hasLabel) {
            return new LossGradients(new ClientInfo(), new String[0], new String[0]);
        }
        // labeled u
        double[] localU = new double[label.length];
        if (trainData.getFeature().length != 0 && trainData.getFeature() != null) {
            localU = MathExt.forward1(trainData.getFeature(), weight);
        }
        double[] label = trainData.getLabel();
        Ciphertext[] lossXi = new Ciphertext[label.length];
        Ciphertext[] sigmoidWxMinusY = computeSigmoidWxMinusY(trainData, linearP2Request, thisClient, localU, label, lossXi);
        String[] yMinusSigmoidWxStr = IntStream.range(0, sigmoidWxMinusY.length).boxed()
                .map(i -> sigmoidWxMinusY[i].serialize()).toArray(String[]::new);
        String[] lossXiStr = IntStream.range(0, lossXi.length).boxed().map(i -> lossXi[i].serialize()).toArray(String[]::new);
        return new LossGradients(thisClient, lossXiStr, yMinusSigmoidWxStr);
    }


    /**
     * 在有label的一方计算 sigmoid(wx) - y
     *
     * @return sigmoidWxMinusY: sigmoid(wx) - y returned as return value
     * lossXi: returned as passed reference
     */
    private Ciphertext[] computeSigmoidWxMinusY(VerticalLinearTrainData trainData, LinearP2Request linearP2Request,
                                                ClientInfo thisClient, double[] localU, double[] label,
                                                Ciphertext[] lossXi) {
        //数组默认初始化为0
        Ciphertext[] sigmoidWxMinusY = new Ciphertext[trainData.getDatasetSize()];
        for (int i = 0; i < sigmoidWxMinusY.length; i++) {
            sigmoidWxMinusY[i] = encryptionTool.encrypt(0, pubKey);
        }
        for (int j = 0; j < trainData.getDatasetSize(); j++) {
            Ciphertext sumU = encryptionTool.encrypt(0, pubKey);
            for (LinearP1Response d : linearP2Request.getBodies()) {
                String[][] u = d.getU();

                // adding up all local wx_i, i.e. wx_i^p
                // localU.length>0: 这里对含有label的一方进行判断:若此方只有label(localU.length==0),则直接将其他方的localU 相加.
                if (localU != null && localU.length > 0) { // add lcoalU of this party
                    Ciphertext tmp = encryptionTool.encrypt(localU[j], pubKey);
                    sumU = encryptionTool.add(sumU, tmp, pubKey);
                } else {
                    if (u != null) {
                        sumU = encryptionTool.add(sumU, encryptionTool.restoreCiphertext(u[j][1]), pubKey);
                    }
                }
            }
            // compute sigmoid(wx) - y
            Ciphertext sigmoidWx = logisticLoss.sigmoidApproxEnc(sumU, pubKey);
            Ciphertext minusLabel = encryptionTool.encrypt(-label[j], pubKey);
            sigmoidWxMinusY[j] = encryptionTool.add(sigmoidWx, minusLabel, pubKey);

            // compute sigmoid(wx_i) when y==1 and (1 - sigmoid(wx_i)) when y==0
            // then we mockSend it to Master, decrypting, taking log and computing loss
            if (label[j] == 1) {
                lossXi[j] = sigmoidWx;
            } else if (label[j] == 0) {
                lossXi[j] = encryptionTool.add(
                        encryptionTool.encrypt(1d, pubKey),
                        encryptionTool.multiply(sigmoidWx, -1, pubKey),
                        pubKey
                );
            } else {
                throw new UnsupportedOperationException("label is not 0 or 1 ! ");
            }
        }
        return sigmoidWxMinusY;
    }

    //计算 g = sum((sigmoid(wx)-y )*x_i) / |X|
    private LossGradients trainPhase3(Message data, VerticalLinearTrainData trainData) {
//        LinearP3Response res = new LinearP3Response();
        LossGradients lossgradients = (LossGradients) data;
        String[] d = lossgradients.getGradient(); // sigmoid(wx)-y
        // 计算 gradient //数组默认初始化为0
        Ciphertext[] gradient0 = new Ciphertext[trainData.getFeatureDim()];
        for (int i = 0; i < gradient0.length; i++) {
            gradient0[i] = encryptionTool.encrypt(0, pubKey);
        }
        Ciphertext gradient1 = encryptionTool.encrypt(0, pubKey);

        if (trainData.getDatasetSize() != 0 && trainData.getFeature().length != 0 && trainData.getFeature() != null) { // for client HAS ONLY LABEL, set gradient to 0
            for (int j = 0; j < trainData.getDatasetSize(); j++) {
                for (int k = 0; k < trainData.getFeature()[j].length; k++) {
                    double tmp = trainData.getFeature()[j][k] / trainData.getDatasetSize(); // xi/|X|
                    Ciphertext g = encryptionTool.multiply(encryptionTool.restoreCiphertext(d[j]), tmp, pubKey); // sigmoid(wx_i)-y_i * (x_i/|X|)
                    gradient0[k] = encryptionTool.add(gradient0[k], g, pubKey);
                }
                gradient1 = encryptionTool.add(gradient1, encryptionTool.restoreCiphertext(d[j]), pubKey); // gradient1 is the gradient of W0 or b or the intercept.
            }
            gradient1 = encryptionTool.multiply(gradient1, 1.0 / trainData.getDatasetSize(), pubKey);
        }
        Ciphertext[] gradients = Tool.arrayAppend(gradient0, gradient1);
        logger.info("gradients:" + Arrays.toString(gradients));
        String[] gradientsS = IntStream.range(0, gradients.length).boxed().map(i -> gradients[i].serialize()).toArray(String[]::new);
        LossGradients res = new LossGradients(lossgradients.getClient(), lossgradients.getLoss(), gradientsS);
//        res.setLoss(LossGradients.getLoss());
//        res.setGradients(gradientsS);
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
        if (weight.length > 1) {
            for (int i = 0; i < weight.length; i++) {
                weight[i] = weight[i] + gradients[i];
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

    // 推理初始化，
    public Message inferenceInit(String[] uidList, String[][] inferenceCacheFile, Map<String, Object> others) {
        return InferenceFilter.filter(uidList, inferenceCacheFile);
    }

    @Override
    public Message inference(int phase, Message jsonData, InferenceData inferenceData) {
        CommonInferenceData data = (CommonInferenceData) inferenceData;
        Message res;
        if (phase == -2) {
            res = inferencePhase1(jsonData, data);
        } else {
            throw new UnsupportedOperationException();
        }
        return res;
    }


    public DoubleArray inferencePhase1(Message message, CommonInferenceData data) {
        String[] newUidIndex = ((StringArray) (message)).getData();
        data.filterOtherUid(newUidIndex);
        if (scaling != null) {
            scaling.inferenceMinMaxScaling(data.getSample());
        }
        double[] res = MathExt.forward1(data.getSample(), weight);
        DoubleArray response = new DoubleArray(res);
//        response.setU(res);
        return response;
    }

    public double[] getWeight() {
        return weight;
    }

    public IDifferentialPrivacy getDifferentialPrivacy() {
        return this.differentialPrivacy;
    }

    @Override
    public String serialize() {
        // 输出扰动需要在保存模型之前对模型的参数进行加噪声
        if (this.parameter.isUseDP() && DifferentialPrivacyType.OUTPUT_PERTURB.equals(this.parameter.getDpType())) {
            this.differentialPrivacy.addNoises(this.weight, this.weight);
        }
        return Tool.addExpressions(LinearModelSerializer.saveModelVrticalLinear(modelToken, weight, scaling), this.expressions);
    }


    public void deserialize(String content) {
        String[] contents = Tool.splitExpressionsAndModel(content);
        this.expressions = Tool.splitExpressions(contents[0]);
        VerticalLRModel model = LinearModelSerializer.loadVerticalLRModel(contents[1]);
        this.modelToken = model.modelToken;
        this.weight = model.weight;
        this.scaling = model.scaling;
    }

    public AlgorithmType getModelType() {
        return AlgorithmType.VerticalLR;
    }

    public List<String> getExpressions() {
        return expressions;
    }

}
