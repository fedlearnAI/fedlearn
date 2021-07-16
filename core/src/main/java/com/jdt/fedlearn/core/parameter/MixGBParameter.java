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

package com.jdt.fedlearn.core.parameter;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.jdt.fedlearn.core.parameter.common.CategoryParameter;
import com.jdt.fedlearn.core.parameter.common.MultiParameter;
import com.jdt.fedlearn.core.parameter.common.NumberParameter;
import com.jdt.fedlearn.core.parameter.common.ParameterField;
import com.jdt.fedlearn.core.type.ObjectiveType;
import com.jdt.fedlearn.core.type.ParameterType;
import com.jdt.fedlearn.core.type.MetricType;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

/**
 * @author zhangwenxi
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class MixGBParameter implements SuperParameter {

    private final int trainingEpoch;
    private final int maxTreeNum;
    private final double verticalFeatureSampling;
    private final int maxBinNum;
    private final int minSampleSplit;
    private final double lambda;
    private final double gamma;
    private final MetricType[] evalMetric;
    private final int maxDepth;
    private final double eta;
    private final double evalDataRatio;
    private boolean evalIdRemoval;
    private final double horizontalFeaturesRatio;
    private double needVerticalSplitRatio;
    private ObjectiveType objective;
    private int numClass;
    public final String catFeatures;
    private final int bitLength;

    public MixGBParameter() {
        trainingEpoch = 1;
        maxTreeNum = 30;
        verticalFeatureSampling = 0.8;
        maxBinNum = 32;
        minSampleSplit = 10;
        lambda = 1;
        gamma = 0.0;
        evalMetric = new MetricType[]{MetricType.RMSE};
        maxDepth = 10;
        eta = 0.1;
        evalDataRatio = 0.6;
        evalIdRemoval = false;
        horizontalFeaturesRatio = 0.8;
        needVerticalSplitRatio = 0.98;
        objective = ObjectiveType.regSquare;
        numClass = 1;
        catFeatures = "";
        bitLength = 1024;
    }

    public MixGBParameter(double verticalFeatureSampling, double horizontalFeaturesRatio, int minSampleSplit, double lambda, double gamma, ObjectiveType objective, MetricType[] evalMetric, int maxDepth, int maxTreeNum, int trainingEpoch, double eta, int maxBinNum, double evalDataRatio, String catFeatures, int bitLength, double needVerticalSplitRatio) {
        this.verticalFeatureSampling = verticalFeatureSampling;
        this.minSampleSplit = minSampleSplit;
        this.lambda = lambda;
        this.gamma = gamma;
        this.evalMetric = evalMetric;
        this.maxDepth = maxDepth;
        this.maxTreeNum = maxTreeNum;
        this.trainingEpoch = trainingEpoch;
        this.eta = eta;
        this.maxBinNum = maxBinNum;
        this.evalDataRatio = evalDataRatio;
        this.horizontalFeaturesRatio = horizontalFeaturesRatio;
        this.catFeatures = catFeatures;
        this.objective = objective;
        this.bitLength = bitLength;
        this.needVerticalSplitRatio = needVerticalSplitRatio;
    }

    public MixGBParameter(double verticalFeatureSampling, double horizontalFeaturesRatio, int minSampleSplit, double lambda, double gamma, ObjectiveType objective, MetricType[] evalMetric, int maxDepth, int maxTreeNum, int trainingEpoch, double eta, int maxBinNum, double evalDataRatio, String catFeatures, int bitLength) {
        this.verticalFeatureSampling = verticalFeatureSampling;
        this.minSampleSplit = minSampleSplit;
        this.lambda = lambda;
        this.gamma = gamma;
        this.evalMetric = evalMetric;
        this.maxDepth = maxDepth;
        this.maxTreeNum = maxTreeNum;
        this.trainingEpoch = trainingEpoch;
        this.eta = eta;
        this.maxBinNum = maxBinNum;
        this.evalDataRatio = evalDataRatio;
        this.horizontalFeaturesRatio = horizontalFeaturesRatio;
        this.catFeatures = catFeatures;
        this.objective = objective;
        this.bitLength = bitLength;
    }

    @Override
    public MetricType[] fetchMetric() {
        return evalMetric;
    }

    @Override
    public String serialize() {
        return "MixGBParameter{" +
                "trainingEpoch=" + trainingEpoch +
                ", maxTreeNum=" + maxTreeNum +
                ", verticalFeatureSampling=" + verticalFeatureSampling  +
                ", maxBinNum=" + maxBinNum  +
                ", minSampleSplit=" + minSampleSplit  +
                ", lambda=" + lambda +
                ", gamma=" + gamma +
                ", evalMetric=" + Arrays.toString(evalMetric) +
                ", maxDepth=" + maxDepth +
                ", eta=" + eta +
                ", objective='" + objective + '\'' +
                ", evalDataRatio=" + evalDataRatio  +
                ", evalIdRemoval=" + evalIdRemoval  +
                ", horizontalFeaturesRatio=" + horizontalFeaturesRatio  +
                ", needVerticalSplitRatio=" + needVerticalSplitRatio  +
                ", numClass=" + numClass  +
                ", bitLength=" + bitLength +
                ", catFeatures='" + catFeatures + '\'' +
                '}';
    }

    @Override
    public List<ParameterField> obtainPara() {
        List<ParameterField> res = new ArrayList<>();
        res.add(new NumberParameter("maxTreeNum", "树的个数", 50, new String[]{"1", "100"}, ParameterType.NUMS));
        res.add(new NumberParameter("verticalFeatureSampling", "特征抽样比例", 0.8, new String[]{"0", "1"}, ParameterType.NUMS));
        res.add(new NumberParameter("maxBinNum", "特征分桶个数", 32, new String[]{"10", "50"}, ParameterType.NUMS));
        res.add(new NumberParameter("minSampleSplit", "节点最小样本数", 10, new String[]{"1", "30"}, ParameterType.NUMS));
        res.add(new NumberParameter("lambda", "lambda", 1, new String[]{"1", "20"}, ParameterType.NUMS));
        res.add(new NumberParameter("gamma", "gamma", 0, new String[]{"0", "1"}, ParameterType.NUMS));
        res.add(new MultiParameter("evalMetric", "eval_metric", "MAPE", new String[]{"RMSE", "MAPE", "MAAPE", "MSE", "F1", "ACC", "AUC", "RECALL", "PRECISION"}, ParameterType.MULTI));
        res.add(new NumberParameter("maxDepth", "max_depth", 5, new String[]{"2", "20"}, ParameterType.NUMS));
        res.add(new NumberParameter("eta", "learning rate", 0.3, new String[]{"0.001", "1.0"}, ParameterType.NUMS));
        res.add(new NumberParameter("evalDataRatio", "evalDataRatio", 0.6, new String[]{"0", "1"}, ParameterType.NUMS));
        res.add(new CategoryParameter("evalIdRemoval", "evalIdRemoval", "false", new String[]{"true", "false"}, ParameterType.STRING));
        res.add(new NumberParameter("horizontalFeaturesRatio", "horizontalFeaturesRatio", 0.8, new String[]{"0", "1"}, ParameterType.NUMS));
        res.add(new NumberParameter("needVerticalSplitRatio", "needVerticalSplitRatio", 0.98, new String[]{"0.6", "1"}, ParameterType.NUMS));
        res.add(new CategoryParameter("objective", "objective", "regSquare", new String[]{"regLogistic", "regSquare", "binaryLogistic"}, ParameterType.STRING));
        res.add(new NumberParameter("numClass", "(仅多分类问题)类别数量", 1, new String[]{"1", "100"}, ParameterType.NUMS));
        res.add(new CategoryParameter("bitLength", "同态加密比特数", "1024", new String[]{"512", "1024", "2048"}, ParameterType.STRING));
        return res;
    }

    public int getMaxTreeNum() {
        return maxTreeNum;
    }

    public int getTrainingEpoch() {
        return trainingEpoch;
    }

    public int getMaxBinNum() {
        return maxBinNum;
    }

    public int getMinSampleSplit() {
        return minSampleSplit;
    }

    public int getMaxDepth() {
        return maxDepth;
    }

    public double getVerticalFeatureSampling() {
        return verticalFeatureSampling;
    }

    public double getLambda() {
        return lambda;
    }

    public double getGamma() {
        return gamma;
    }

    public double getEta() {
        return eta;
    }

    public double getEvalDataRatio() {
        return evalDataRatio;
    }

    public double getHorizontalFeaturesRatio() {
        return horizontalFeaturesRatio;
    }

    public double getNeedVerticalSplitRatio() {
        return needVerticalSplitRatio;
    }

    public MetricType[] getEvalMetric() {
        return evalMetric;
    }

    public boolean getEvalIdRemoval() {
        return evalIdRemoval;
    }

    public ObjectiveType getObjective() {
        return objective;
    }

    public int getNumClass() {
        return numClass;
    }

    public int getBitLength() {
        return bitLength;
    }

    @Override
    public String toString() {
        return "MixGBParameter{" +
                "trainingEpoch=" + trainingEpoch +
                ", maxTreeNum=" + maxTreeNum +
                ", verticalFeatureSampling=" + verticalFeatureSampling +
                ", maxBinNum=" + maxBinNum +
                ", minSampleSplit=" + minSampleSplit +
                ", lambda=" + lambda +
                ", gamma=" + gamma +
                ", evalMetric=" + Arrays.toString(evalMetric) +
                ", maxDepth=" + maxDepth +
                ", eta=" + eta +
                ", evalDataRatio=" + evalDataRatio +
                ", evalIdRemoval=" + evalIdRemoval +
                ", horizontalFeaturesRatio=" + horizontalFeaturesRatio +
                ", needVerticalSplitRatio=" + needVerticalSplitRatio +
                ", objective=" + objective +
                ", numClass=" + numClass +
                ", catFeatures='" + catFeatures + '\'' +
                ", bitLength=" + bitLength +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        MixGBParameter that = (MixGBParameter) o;
        return trainingEpoch == that.trainingEpoch && maxTreeNum == that.maxTreeNum && Double.compare(that.verticalFeatureSampling, verticalFeatureSampling) == 0 && maxBinNum == that.maxBinNum && minSampleSplit == that.minSampleSplit && Double.compare(that.lambda, lambda) == 0 && Double.compare(that.gamma, gamma) == 0 && maxDepth == that.maxDepth && Double.compare(that.eta, eta) == 0 && Double.compare(that.evalDataRatio, evalDataRatio) == 0 && evalIdRemoval == that.evalIdRemoval && Double.compare(that.horizontalFeaturesRatio, horizontalFeaturesRatio) == 0 && Double.compare(that.needVerticalSplitRatio, needVerticalSplitRatio) == 0 && numClass == that.numClass && bitLength == that.bitLength && Arrays.equals(evalMetric, that.evalMetric) && Objects.equals(objective, that.objective) && Objects.equals(catFeatures, that.catFeatures);
    }

    @Override
    public int hashCode() {
        int result = Objects.hash(trainingEpoch, maxTreeNum, verticalFeatureSampling, maxBinNum, minSampleSplit, lambda, gamma, maxDepth, eta, evalDataRatio, evalIdRemoval, horizontalFeaturesRatio, needVerticalSplitRatio, objective, numClass, catFeatures, bitLength);
        result = 31 * result + Arrays.hashCode(evalMetric);
        return result;
    }
}
