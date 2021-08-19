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

import com.jdt.fedlearn.core.parameter.common.CategoryParameter;
import com.jdt.fedlearn.core.parameter.common.MultiParameter;
import com.jdt.fedlearn.core.parameter.common.NumberParameter;
import com.jdt.fedlearn.core.parameter.common.ParameterField;
import com.jdt.fedlearn.core.type.ParameterType;
import com.jdt.fedlearn.core.type.BitLengthType;
import com.jdt.fedlearn.core.type.FirstPredictType;
import com.jdt.fedlearn.core.type.MetricType;
import com.jdt.fedlearn.core.type.ObjectiveType;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

public class FgbParameter implements SuperParameter {
    private final int numBoostRound;   //轮数，树的个数
    private final FirstPredictType firstRoundPred;  //初始化预测值
    private final int earlyStoppingRound; //早停轮数
    private final double minChildWeight;
    private final int minSampleSplit;
    private final double lambda;
    private final double gamma;
    private final MetricType[] evalMetric;
    private final int maxDepth;
    private final double eta;
    private final ObjectiveType objective;
    private final int numBin;  //特征分桶个数
    private final boolean maximize;
    private final double rowSample;   //（行）样本抽样比例
    private final double colSample;   //列（特征）抽样比例
    private final double scalePosWeight;
    private final BitLengthType bitLength;  //paillier 同态加密 模
    private int numClass;
    //todo catFeatures String[]
    private final String catFeatures;
    private final double randomizedResponseProbability;
    private final double differentialPrivacyParameter;

    public FgbParameter() {
        this.numBoostRound = 50;
        this.firstRoundPred = FirstPredictType.AVG;
        this.maximize = true;
        this.rowSample = 0.8;
        this.colSample = 0.8;
        this.earlyStoppingRound = 10;
        this.minChildWeight = 1;
        this.minSampleSplit = 10;
        this.lambda = 1;
        this.gamma = 0;
        this.scalePosWeight = 1;
        this.numBin = 33;
        this.evalMetric = new MetricType[]{MetricType.RMSE};
        this.maxDepth = 7;
        this.eta = 0.1;
        this.objective = ObjectiveType.regSquare;
        this.catFeatures = "";
        this.bitLength = BitLengthType.bit1024;
        this.numClass = 1;
        this.differentialPrivacyParameter = 0;
        this.randomizedResponseProbability = 0;
    }

    public FgbParameter(Builder builder) {
        this.numBoostRound = builder.numRound;
        this.firstRoundPred = builder.firstRoundPred;
        this.evalMetric = builder.evalMetric;

        this.maximize = builder.maximize;
        this.rowSample = builder.rowSample;
        this.colSample = builder.colSample;
        this.earlyStoppingRound = builder.earlyStoppingRound;
        this.minChildWeight = builder.minChildWeight;
        this.minSampleSplit = builder.minSampleSplit;
        this.lambda = builder.lambda;
        this.gamma = builder.gamma;
        this.bitLength = builder.bitLength;
        this.scalePosWeight = builder.scalePosWeight;
        this.numBin = builder.numBin;
        this.maxDepth = builder.maxDepth;
        this.eta = builder.eta;
        this.objective = builder.objective;
        this.numClass = builder.numClass;
        this.catFeatures = builder.catFeatures;
        this.randomizedResponseProbability = builder.randomizedResponseProbability;
        this.differentialPrivacyParameter = builder.differentialPrivacyParameter;
    }

    public static class Builder {
        //required parameters
        private final int numRound;
        private final MetricType[] evalMetric;
        private final ObjectiveType objective;

        //
        private FirstPredictType firstRoundPred = FirstPredictType.AVG;
        private double rowSample = 1.0;
        private double colSample = 1.0;
        private boolean maximize = true;
        private int earlyStoppingRound = 10;
        private double minChildWeight = 1.0;
        private int minSampleSplit = 10;
        private double lambda = 1.0;
        private double gamma = 0.0;
        private BitLengthType bitLength = BitLengthType.bit1024;
        private double scalePosWeight = 1.0;
        private int numBin = 33;
        private int maxDepth = 7;
        private double eta = 0.3;
        private int numClass = 1;
        private String catFeatures = "";
        private double randomizedResponseProbability = 0;
        private double differentialPrivacyParameter = 0;

        public Builder(int numRound, MetricType[] evalMetric, ObjectiveType objective) {
            this.numRound = numRound;
            this.evalMetric = evalMetric;
            this.objective = objective;
        }

        public Builder firstRoundPred(FirstPredictType firstRoundPred) {
            this.firstRoundPred = firstRoundPred;
            return this;
        }

        public Builder rowSample(double rowSample) {
            this.rowSample = rowSample;
            return this;
        }

        public Builder colSample(double colSample) {
            this.colSample = colSample;
            return this;
        }

        public Builder maximize(boolean maximize) {
            this.maximize = maximize;
            return this;
        }

        public Builder earlyStoppingRound(int earlyStoppingRound) {
            this.earlyStoppingRound = earlyStoppingRound;
            return this;
        }

        public Builder minChildWeight(double minChildWeight) {
            this.minChildWeight = minChildWeight;
            return this;
        }

        public Builder minSampleSplit(int minSampleSplit) {
            this.minSampleSplit = minSampleSplit;
            return this;
        }

        public Builder lambda(double lambda) {
            this.lambda = lambda;
            return this;
        }

        public Builder gamma(double gamma) {
            this.gamma = gamma;
            return this;
        }

        public Builder bitLength(BitLengthType bitLength) {
            this.bitLength = bitLength;
            return this;
        }

        public Builder scalePosWeight(double scalePosWeight) {
            this.scalePosWeight = scalePosWeight;
            return this;
        }

        public Builder numBin(int numBin) {
            this.numBin = numBin;
            return this;
        }

        public Builder maxDepth(int maxDepth) {
            this.maxDepth = maxDepth;
            return this;
        }

        public Builder eta(double eta) {
            this.eta = eta;
            return this;
        }

        public Builder numClass(int numClass) {
            this.numClass = numClass;
            return this;
        }

        public Builder catFeatures(String catFeatures) {
            this.catFeatures = catFeatures;
            return this;
        }

        public Builder randomizedResponseProbability(double randomizedResponseProbability) {
            this.randomizedResponseProbability = randomizedResponseProbability;
            return this;
        }

        public Builder differentialPrivacyParameter(double differentialPrivacyParameter) {
            this.differentialPrivacyParameter = differentialPrivacyParameter;
            return this;
        }

        public FgbParameter build() {
            return new FgbParameter(this);
        }
    }

    public List<ParameterField> obtainPara() {
        List<ParameterField> res = new ArrayList<>();
        res.add(new NumberParameter("numBoostRound", "树的个数", 50, new String[]{"1", "100"}, ParameterType.NUMS));
        res.add(new CategoryParameter("firstRoundPred", "初始化预测值", "AVG", new String[]{"ZERO", "AVG", "RANDOM"}, ParameterType.STRING));
        res.add(new CategoryParameter("maximize", "maximize", "true", new String[]{"true", "false"}, ParameterType.STRING));
        res.add(new NumberParameter("rowSample", "样本抽样比例", 1.0, new String[]{"0.1", "1.0"}, ParameterType.NUMS));
        res.add(new NumberParameter("colSample", "列抽样比例", 1.0, new String[]{"0.1", "1.0"}, ParameterType.NUMS));
        res.add(new NumberParameter("earlyStoppingRound", "早停轮数", 10, new String[]{"1", "20"}, ParameterType.NUMS));
        res.add(new NumberParameter("minChildWeight", "minChildWeight", 1, new String[]{"1", "10"}, ParameterType.NUMS));
        res.add(new NumberParameter("minSampleSplit", "minSampleSplit", 10, new String[]{"1", "20"}, ParameterType.NUMS));
        res.add(new NumberParameter("lambda", "lambda", 1, new String[]{"1", "20"}, ParameterType.NUMS));
        res.add(new NumberParameter("gamma", "gamma", 0, new String[]{"0", "1"}, ParameterType.NUMS));
        res.add(new NumberParameter("scalePosWeight", "scalePosWeight", 1, new String[]{"0", "1"}, ParameterType.NUMS));
        res.add(new NumberParameter("numBin", "特征分桶个数", 33, new String[]{"33", "50"}, ParameterType.NUMS));
        res.add(new MultiParameter("evalMetric", "evalMetric", "MAPE", new String[]{"RMSE", "MAPE", "MSE", "F1", "ACC", "AUC", "RECALL", "PRECISION", "MACC", "MERROR"}, ParameterType.MULTI));
        res.add(new NumberParameter("maxDepth", "maxDepth", 7, new String[]{"2", "20"}, ParameterType.NUMS));
        res.add(new NumberParameter("eta", "learning rate", 0.3, new String[]{"0.01", "1"}, ParameterType.NUMS));
        res.add(new CategoryParameter("objective", "objective", "regSquare", new String[]{"regLogistic", "regSquare", "countPoisson", "binaryLogistic", "multiSoftmax", "multiSoftProb"}, ParameterType.STRING));
        res.add(new NumberParameter("numClass", "(仅多分类问题)类别数量", 1, new String[]{"1", "100"}, ParameterType.NUMS));
        res.add(new CategoryParameter("bitLength", "同态加密比特数", "bit1024", new String[]{"bit512", "bit1024", "bit2048"}, ParameterType.STRING));
        res.add(new CategoryParameter("catFeatures", "catFeatures", "", new String[]{}, ParameterType.STRING));
        res.add(new NumberParameter("randomizedResponseProbability", "randomizedResponseProbability", 0, new String[]{"0", "1"}, ParameterType.NUMS));
        res.add(new NumberParameter("differentialPrivacyParameter", "differentialPrivacyParameter", 0, new String[]{"0", "1"}, ParameterType.NUMS));
        return res;
    }

    public int getNumBoostRound() {
        return numBoostRound;
    }

    public FirstPredictType getFirstRoundPred() {
        return firstRoundPred;
    }

    public boolean isMaximize() {
        return maximize;
    }

    public double getRowSample() {
        return rowSample;
    }

    public double getColSample() {
        return colSample;
    }

    public int getEarlyStoppingRound() {
        return earlyStoppingRound;
    }

    public double getMinChildWeight() {
        return minChildWeight;
    }

    public int getMinSampleSplit() {
        return minSampleSplit;
    }

    public double getLambda() {
        return lambda;
    }

    public double getGamma() {
        return gamma;
    }

    public double getScalePosWeight() {
        return scalePosWeight;
    }

    public MetricType[] getEvalMetric() {
        return evalMetric;
    }

    public int getMaxDepth() {
        return maxDepth;
    }

    public double getEta() {
        return eta;
    }

    public ObjectiveType getObjective() {
        return objective;
    }

    public String getCatFeatures() {
        return catFeatures;
    }

    public BitLengthType getBitLength() {
        return bitLength;
    }

    public int getNumBin() {
        return numBin;
    }

    public double getRandomizedResponseProbability() {
        return randomizedResponseProbability;
    }

    public double getDifferentialPrivacyParameter() {
        return differentialPrivacyParameter;
    }

    public int getNumClass() {
        return numClass;
    }

    public void setNumClass(int value) {
        this.numClass = value;
    }

    public String serialize() {
        return this.toString();
    }

    public MetricType[] fetchMetric() {
        return evalMetric;
    }

    public String toString() {
        return "FgbParameter{" +
                "numBoostRound=" + numBoostRound +
                ", firstRoundPred=" + firstRoundPred +
                ", earlyStoppingRound=" + earlyStoppingRound +
                ", minChildWeight=" + minChildWeight +
                ", minSampleSplit=" + minSampleSplit +
                ", lambda=" + lambda +
                ", gamma=" + gamma +
                ", evalMetric=" + Arrays.toString(evalMetric) +
                ", maxDepth=" + maxDepth +
                ", eta=" + eta +
                ", objective='" + objective + '\'' +
                ", numBin=" + numBin +
                ", maximize=" + maximize +
                ", rowSample=" + rowSample +
                ", colSample=" + colSample +
                ", scalePosWeight=" + scalePosWeight +
                ", bitLength=" + bitLength +
                ", numClass=" + numClass +
                ", catFeatures='" + catFeatures + '\'' +
                ", randomizedResponseProbability=" + randomizedResponseProbability +
                ", differentialPrivacyParameter=" + differentialPrivacyParameter +
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
        FgbParameter parameter = (FgbParameter) o;
        return numBoostRound == parameter.numBoostRound &&
                earlyStoppingRound == parameter.earlyStoppingRound &&
                Double.compare(parameter.minChildWeight, minChildWeight) == 0 &&
                minSampleSplit == parameter.minSampleSplit &&
                Double.compare(parameter.lambda, lambda) == 0 &&
                Double.compare(parameter.gamma, gamma) == 0 &&
                maxDepth == parameter.maxDepth &&
                Double.compare(parameter.eta, eta) == 0 &&
                numBin == parameter.numBin &&
                maximize == parameter.maximize &&
                Double.compare(parameter.rowSample, rowSample) == 0 &&
                Double.compare(parameter.colSample, colSample) == 0 &&
                Double.compare(parameter.scalePosWeight, scalePosWeight) == 0 &&
                bitLength == parameter.bitLength &&
                numClass == parameter.numClass &&
                Double.compare(parameter.randomizedResponseProbability, randomizedResponseProbability) == 0 &&
                Double.compare(parameter.differentialPrivacyParameter, differentialPrivacyParameter) == 0 &&
                firstRoundPred == parameter.firstRoundPred &&
                Arrays.equals(evalMetric, parameter.evalMetric) &&
                Objects.equals(objective, parameter.objective) &&
                Objects.equals(catFeatures, parameter.catFeatures);
    }

    @Override
    public int hashCode() {
        int result = Objects.hash(numBoostRound, firstRoundPred, earlyStoppingRound, minChildWeight, minSampleSplit, lambda, gamma, maxDepth, eta, objective, numBin, maximize, rowSample, colSample, scalePosWeight, bitLength, numClass, catFeatures, randomizedResponseProbability, differentialPrivacyParameter);
        result = 31 * result + Arrays.hashCode(evalMetric);
        return result;
    }

}

