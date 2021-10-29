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
import com.jdt.fedlearn.core.type.DifferentialPrivacyType;
import com.jdt.fedlearn.core.type.MetricType;
import com.jdt.fedlearn.core.type.NormalizationType;
import com.jdt.fedlearn.core.type.ParameterType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class FederatedKernelParameter implements HyperParameter {

    private static final Logger logger = LoggerFactory.getLogger(FederatedKernelParameter.class);
    private double kernelType;
    private long seed;
    private double mapdim; //the dimension of kernel mapping feature for each party
    private double scale;
    private int maxIter;
    private int batchSize;
    private MetricType[] metricType;
    private NormalizationType normalizationType; // 0: no training normalization; 1: minmax; 2: standard.
    private int numClass;
    private int earlyStoppingRounds = 10;

    // 差分隐私系数lambda
    private double dpLambda = 1.0;
    // 差分隐私epsilon
    private double dpEpsilon;
    // 差分隐私delta
    private double dpDelta = 1e-8;
    // 差分隐私类型
    private DifferentialPrivacyType dpType;
    // 是否使用差分隐私
    private String useDP = "false";


    public FederatedKernelParameter() {
        this.kernelType = 3; //rbf kernel by default
        this.seed = 100;
        this.mapdim = 400;
        this.maxIter = 1000;
        this.scale = 0.005;
        this.batchSize = 100000;
        this.metricType = new MetricType[]{MetricType.TRAINLOSS};
        this.normalizationType = NormalizationType.NONE;
        this.numClass = 2;
        this.earlyStoppingRounds = 5;
        this.dpEpsilon = 0.4;
        this.dpType = DifferentialPrivacyType.OUTPUT_PERTURB;
        this.dpDelta = 1e-8;
        this.useDP = "true";
        this.dpLambda = 0.001;
    }

    public FederatedKernelParameter(Builder builder) {
        this.numClass = builder.numClass;
        this.metricType = builder.metricType;

        this.kernelType = builder.kernelType;
        this.seed = builder.seed;
        this.mapdim = builder.mapdim;
        this.maxIter = builder.maxIter;
        this.scale = builder.scale;
        this.batchSize = builder.batchSize;
        this.normalizationType = builder.normalizationType;
        ;
        this.earlyStoppingRounds = builder.earlyStoppingRounds;
    }

    public static class Builder {
        private double kernelType;
        private int seed;
        private double mapdim; //the dimension of kernel mapping feature for each party
        private double scale;
        private int maxIter;
        private int batchSize;
        private MetricType[] metricType;
        private NormalizationType normalizationType; // 0: no training normalization; 1: minmax; 2: standard.
        private int numClass;
        private int earlyStoppingRounds = 10;

        private String useDP = "false";
        // 差分隐私epsilon
        private double dpEpsilon;
        // 差分隐私delta
        private double dpDelta;
        // 差分隐私类型
        private String dpType;

        public Builder(MetricType[] metricType, int numClass) {
            this.metricType = metricType;
            this.numClass = numClass;
        }

        public Builder kernelType(double kernelType) {
            this.kernelType = kernelType;
            return this;
        }

        public Builder seed(int seed) {
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

        public Builder earlyStoppingRounds(int earlyStoppingRounds) {
            this.earlyStoppingRounds = earlyStoppingRounds;
            return this;
        }

        public Builder useDp(String useDP) {
            this.useDP = useDP;
            return this;
        }

        public Builder dpEpsilon(double dpEpsilon) {
            this.dpEpsilon = dpEpsilon;
            return this;
        }

        public Builder dpDelta(double dpDelta) {
            this.dpDelta = dpDelta;
            return this;
        }

        public Builder dpType(String dpType) {
            this.dpType = dpType;
            return this;
        }


        public FederatedKernelParameter build() {
            return new FederatedKernelParameter(this);
        }
    }

    public FederatedKernelParameter(double kernelType, int seed, double mapdim, int maxIter, double scale, int batchSize, MetricType[] metricType, NormalizationType normalizationType) {
        this.kernelType = kernelType; //rbf kernel by default
        this.seed = seed;
        this.mapdim = mapdim;
        this.maxIter = maxIter;
        this.scale = scale;
        this.batchSize = batchSize;
        this.metricType = metricType;
        this.normalizationType = normalizationType;
    }

    public FederatedKernelParameter(double kernelType, int seed, double mapdim, int maxIter, double scale, int batchSize, MetricType[] metricType, NormalizationType normalizationType, int numClass) {
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

    public FederatedKernelParameter(double kernelType, int seed, double mapdim, int maxIter, double scale, int batchSize, MetricType[] metricType, NormalizationType normalizationType, int numClass, int earlyStoppingRounds) {
        this.kernelType = kernelType; //rbf kernel by default
        this.seed = seed;
        this.mapdim = mapdim;
        this.maxIter = maxIter;
        this.scale = scale;
        this.batchSize = batchSize;
        this.metricType = metricType;
        this.normalizationType = normalizationType;
        this.numClass = numClass;
        this.earlyStoppingRounds = earlyStoppingRounds;
    }

    public FederatedKernelParameter(double kernelType, int seed, double mapdim, int maxIter, double scale, int batchSize, MetricType[] metricType, NormalizationType normalizationType, int numClass, int earlyStoppingRounds, String useDP, DifferentialPrivacyType dpType, double dpEpsilon, double dpDelta, double dpLambda) {
        this.kernelType = kernelType; //rbf kernel by default
        this.seed = seed;
        this.mapdim = mapdim;
        this.maxIter = maxIter;
        this.scale = scale;
        this.batchSize = batchSize;
        this.metricType = metricType;
        this.normalizationType = normalizationType;
        this.numClass = numClass;
        this.earlyStoppingRounds = earlyStoppingRounds;
        this.useDP = useDP;
        this.dpType = dpType;
        this.dpEpsilon = dpEpsilon;
        this.dpDelta = dpDelta;
        this.dpLambda = dpLambda;
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
        res.add(new NumberParameter("earlyStoppingRounds", "早停轮数", 1, new String[]{"1", "100"}, ParameterType.NUMS));
        res.add(new NumberParameter("dpEpsilon", "dpEpsilon", 1.6, new String[]{"0.05", "1.6"}, ParameterType.NUMS));
        res.add(new CategoryParameter("useDP", "useDP", "false", new String[]{"true", "false"}, ParameterType.STRING));
        res.add(new CategoryParameter("dpType", "dpType", "OUTPUT_PERTURB", new String[]{"OUTPUT_PERTURB", "OBJECTIVE_PERTURB"}, ParameterType.STRING));
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

    public long getSeed() {
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

    public boolean isUseDP() {
        return "true".equals(this.useDP);
    }

    public double getDpDelta() {
        return this.dpDelta;
    }

    public double getDpLambda() {
        return this.dpLambda;
    }

    public double getDpEpsilon() {
        return this.dpEpsilon;
    }

    public DifferentialPrivacyType getDpType() {
        return this.dpType;
    }

    public int getEarlyStoppingRounds() {
        return earlyStoppingRounds;
    }

    public void parseJson(String jsonStr) {
        ObjectMapper mapper = new ObjectMapper();
        FederatedKernelParameter tmp;
        try {
            tmp = mapper.readValue(jsonStr, FederatedKernelParameter.class);
            this.kernelType = tmp.kernelType;
            this.seed = tmp.seed;
            this.mapdim = tmp.mapdim;
            this.maxIter = tmp.maxIter;
            this.metricType = tmp.metricType;
            this.batchSize = tmp.batchSize;
            this.scale = tmp.scale;
            this.normalizationType = tmp.normalizationType;
            this.numClass = tmp.numClass;
            this.dpDelta = tmp.dpDelta;
            this.dpEpsilon = tmp.dpEpsilon;
            this.dpType = tmp.dpType;
            this.useDP = tmp.useDP;
        } catch (IOException e) {
            System.out.println("parse error");
            logger.error("parse error", e);
        }
    }

}
