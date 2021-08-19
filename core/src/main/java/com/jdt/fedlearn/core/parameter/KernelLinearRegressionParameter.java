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

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jdt.fedlearn.core.parameter.common.CategoryParameter;
import com.jdt.fedlearn.core.parameter.common.MultiParameter;
import com.jdt.fedlearn.core.parameter.common.NumberParameter;
import com.jdt.fedlearn.core.parameter.common.ParameterField;
import com.jdt.fedlearn.core.type.NormalizationType;
import com.jdt.fedlearn.core.type.ParameterType;
import com.jdt.fedlearn.core.type.MetricType;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class KernelLinearRegressionParameter implements SuperParameter {

    private static final Logger logger = LoggerFactory.getLogger(KernelLinearRegressionParameter.class);
    private double kernelType;
    private double seed;
    private double mapdim; //the dimension of kernel mapping feature for each party
    private double scale;
    private int maxIter;
    private int batchSize;
    private MetricType[] metricType;
    private NormalizationType normalizationType; // 0: no training normalization; 1: minmax; 2: standard.
    private int numClass;
    private double differentialPrivacy = 0;
    private int earlyStoppingRounds = 10;

    public KernelLinearRegressionParameter() {
        this.kernelType = 3; //rbf kernel by default
        this.seed = 100;
        this.mapdim = 400;
        this.maxIter = 1000;
        this.scale = 0.005;
        this.batchSize = 100000;
        this.metricType = new MetricType[]{MetricType.TRAINLOSS};
        this.normalizationType = NormalizationType.NONE;
        this.numClass = 1;
        this.differentialPrivacy = 0;
        this.earlyStoppingRounds = 5;
    }

    public KernelLinearRegressionParameter(Builder builder) {
        this.numClass = builder.numClass;
        this.metricType = builder.metricType;

        this.kernelType = builder.kernelType;
        this.seed = builder.seed;
        this.mapdim = builder.mapdim;
        this.maxIter = builder.maxIter;
        this.scale = builder.scale;
        this.batchSize = builder.batchSize;
        this.normalizationType = builder.normalizationType;
        this.differentialPrivacy = builder.differentialPrivacy;
        this.earlyStoppingRounds = builder.earlyStoppingRounds;
    }

    public static class Builder {
        private double kernelType;
        private double seed;
        private double mapdim; //the dimension of kernel mapping feature for each party
        private double scale;
        private int maxIter;
        private int batchSize;
        private MetricType[] metricType;
        private NormalizationType normalizationType; // 0: no training normalization; 1: minmax; 2: standard.
        private int numClass;
        private double differentialPrivacy = 0;
        private int earlyStoppingRounds = 10;

        public Builder(MetricType[] metricType, int numClass) {
            this.metricType = metricType;
            this.numClass = numClass;
        }

        public Builder kernelType(double kernelType) {
            this.kernelType = kernelType;
            return this;
        }

        public Builder seed(double seed) {
            this.seed = seed;
            return this;
        }

        public Builder mapdim(double mapdim) {
            this.mapdim = mapdim;
            return this;
        }

        public Builder scale(double scale) {
            this.scale = scale;
            return this;
        }

        public Builder maxIter(int maxIter) {
            this.maxIter = maxIter;
            return this;
        }

        public Builder batchSize(int batchSize) {
            this.batchSize = batchSize;
            return this;
        }

        public Builder normalizationType(NormalizationType normalizationType) {
            this.normalizationType = normalizationType;
            return this;

        }

        public Builder differentialPrivacy(double differentialPrivacy) {
            this.differentialPrivacy = differentialPrivacy;
            return this;
        }

        public Builder earlyStoppingRounds(int earlyStoppingRounds) {
            this.earlyStoppingRounds = earlyStoppingRounds;
            return this;
        }

        public KernelLinearRegressionParameter build(){
            return new KernelLinearRegressionParameter(this);
        }
    }

    public KernelLinearRegressionParameter(double kernelType, double seed, double mapdim, int maxIter, double scale, int batchSize, MetricType[] metricType, NormalizationType normalizationType) {
        this.kernelType = kernelType; //rbf kernel by default
        this.seed = seed;
        this.mapdim = mapdim;
        this.maxIter = maxIter;
        this.scale = scale;
        this.batchSize = batchSize;
        this.metricType = metricType;
        this.normalizationType = normalizationType;
    }

    public KernelLinearRegressionParameter(double kernelType, double seed, double mapdim, int maxIter, double scale, int batchSize, MetricType[] metricType, NormalizationType normalizationType, int numClass) {
        this.kernelType = kernelType; //rbf kernel by default
        this.seed = seed;
        this.mapdim = mapdim;
        this.maxIter = maxIter;
        this.scale = scale;
        this.batchSize = batchSize;
        this.metricType = metricType;
        this.normalizationType = normalizationType;
        this.numClass = numClass;
    }

    public KernelLinearRegressionParameter(double kernelType, double seed, double mapdim, int maxIter, double scale, int batchSize, MetricType[] metricType, NormalizationType normalizationType, int numClass, double differentialPrivacy) {
        this.kernelType = kernelType; //rbf kernel by default
        this.seed = seed;
        this.mapdim = mapdim;
        this.maxIter = maxIter;
        this.scale = scale;
        this.batchSize = batchSize;
        this.metricType = metricType;
        this.normalizationType = normalizationType;
        this.numClass = numClass;
        this.differentialPrivacy = differentialPrivacy;
    }

    public KernelLinearRegressionParameter(double kernelType, double seed, double mapdim, int maxIter, double scale, int batchSize, MetricType[] metricType, NormalizationType normalizationType, int numClass, double differentialPrivacy, int earlyStoppingRounds) {
        this.kernelType = kernelType; //rbf kernel by default
        this.seed = seed;
        this.mapdim = mapdim;
        this.maxIter = maxIter;
        this.scale = scale;
        this.batchSize = batchSize;
        this.metricType = metricType;
        this.normalizationType = normalizationType;
        this.numClass = numClass;
        this.differentialPrivacy = differentialPrivacy;
        this.earlyStoppingRounds = earlyStoppingRounds;
    }

    public List<ParameterField> obtainPara() {
        List<ParameterField> res = new ArrayList<>();
        res.add(new NumberParameter("batchSize", "随机样本采样个数", 100000, new String[]{"1000", "5000000"}, ParameterType.NUMS));
        res.add(new NumberParameter("scale", "核变换尺度参数", 0.005, new String[]{"0.000001", "1000"}, ParameterType.NUMS));
        res.add(new NumberParameter("seed", "种子点", 100, new String[]{"0", "2000"}, ParameterType.NUMS));
        res.add(new NumberParameter("mapdim", "核函数映射维数", 400, new String[]{"0", "1000"}, ParameterType.NUMS));
        res.add(new NumberParameter("maxIter", "训练迭代次数", 100, new String[]{"10", "1000"}, ParameterType.NUMS));
        res.add(new MultiParameter("metricType", "metricType", "TRAINLOSS", new String[]{"TRAINLOSS", "RMSE", "MAE", "MAPE", "MAAPE", "F1", "ACC", "AUC", "RECALL", "PRECISION", "MACC", "MERROR", "MAUC", "MF1"}, ParameterType.MULTI));
        res.add(new CategoryParameter("normalizationType", "归一化类型", "NONE", new String[]{"NONE", "MINMAX", "STANDARD"}, ParameterType.STRING));
        res.add(new NumberParameter("numClass", "(仅多分类问题)类别数量", 1, new String[]{"1", "100"}, ParameterType.NUMS));
        res.add(new NumberParameter("differentialPrivacy", "differentialPrivacy", 0, new String[]{"0.0", "1.0"}, ParameterType.NUMS));
        res.add(new NumberParameter("earlyStoppingRounds", "早停轮数", 1, new String[]{"1", "100"}, ParameterType.NUMS));
        return res;
    }

    @Override
    public String serialize() {
        return null;
    }

    @Override
    public MetricType[] fetchMetric() {
        return metricType;
    }

    public double getKernelType() {
        return kernelType;
    }

    public double getMapdim() {
        return mapdim;
    }

    public int getBatchSize() {
        return batchSize;
    }

    public int getMaxIter() {
        return maxIter;
    }

    public double getSeed() {
        return seed;
    }

    public double getScale() {
        return scale;
    }

    public NormalizationType getNormalizationType() {
        return normalizationType;
    }

    public MetricType[] getMetricType() {
        return metricType;
    }

    public int getNumClass() {
        return numClass;
    }

    public double getDifferentialPrivacy() {
        return differentialPrivacy;
    }

    public int getEarlyStoppingRounds() {
        return earlyStoppingRounds;
    }

    public void parseJson(String jsonStr) {
        ObjectMapper mapper = new ObjectMapper();
        KernelLinearRegressionParameter tmp;
        try {
            tmp = mapper.readValue(jsonStr, KernelLinearRegressionParameter.class);
            this.kernelType = tmp.kernelType;
            this.seed = tmp.seed;
            this.mapdim = tmp.mapdim;
            this.maxIter = tmp.maxIter;
            this.metricType = tmp.metricType;
            this.batchSize = tmp.batchSize;
            this.scale = tmp.scale;
            this.normalizationType = tmp.normalizationType;
            this.numClass = tmp.numClass;
            this.differentialPrivacy = tmp.differentialPrivacy;
        } catch (IOException e) {
            System.out.println("parse error");
            logger.error("parse error", e);
        }
    }

}
