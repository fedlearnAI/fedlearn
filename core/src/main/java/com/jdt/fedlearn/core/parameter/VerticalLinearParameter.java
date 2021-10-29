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
import com.jdt.fedlearn.core.type.DifferentialPrivacyType;
import com.jdt.fedlearn.core.type.MetricType;
import com.jdt.fedlearn.core.type.OptimizerType;
import com.jdt.fedlearn.core.type.ParameterType;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class VerticalLinearParameter implements HyperParameter {
    //终止损失 //0.1--100
    private double minLoss;
    //learning rate  //0.01--0.1
    private double eta;
    //loss 类型 MSE MAE RMSE MAPE 等
    private MetricType[] metricType;
    //优化器类型 梯度下降，牛顿法 等
    private OptimizerType optimizer;
    //       //100-10000
    private int batchSize;
    //     //100-10000
    private int maxEpoch;

    private String regularization;

    private double lambda = 0.001;

    // 差分隐私系数lambda
    private double dpLambda = 1.0;
    // 差分隐私epsilon
    private double dpEpsilon;
    // 差分隐私delta
    private double dpDelta = 1e-8;
    // 差分隐私类型
    private DifferentialPrivacyType dpType;
    // 是否使用差分隐私
    private String useDP;

    private int seed = 666;

    public VerticalLinearParameter() {
        this.minLoss = 0.02;
        this.eta = 0.1;
        this.metricType = new MetricType[]{MetricType.RMSE};
        this.batchSize = 1000;
        this.maxEpoch = 300;
        this.optimizer = OptimizerType.BatchGD;
        this.regularization = "L2";
        this.dpEpsilon = 0.4;
        this.dpType = DifferentialPrivacyType.OUTPUT_PERTURB;
        this.dpDelta = 1e-8;
        this.useDP = "true";
        this.dpLambda = 0.001;
    }

    public VerticalLinearParameter(double minLoss, double eta, MetricType[] loss, OptimizerType optimizer, int batchSize, int maxEpoch, String regularization, double lambda, double differentialPrivacyParameter) {
        this.minLoss = minLoss;
        this.eta = eta;
        this.metricType = loss;
        this.batchSize = batchSize;
        this.maxEpoch = maxEpoch;
        this.optimizer = optimizer;
        this.regularization = regularization;
        this.lambda = lambda;
    }

    public VerticalLinearParameter(double minLoss, double eta, MetricType[] loss, OptimizerType optimizer, int batchSize, int maxEpoch, String regularization, double lambda, String useDp, DifferentialPrivacyType dpType, double dpEpsilon, double dpDelta, double dpLambda) {
        this.minLoss = minLoss;
        this.eta = eta;
        this.metricType = loss;
        this.batchSize = batchSize;
        this.maxEpoch = maxEpoch;
        this.optimizer = optimizer;
        this.regularization = regularization;
        this.lambda = lambda;
        this.dpEpsilon = dpEpsilon;
        this.dpType = dpType;
        this.dpDelta = dpDelta;
        this.useDP = useDp;
        this.dpLambda = dpLambda;
    }

    public double getMinLoss() {
        return minLoss;
    }

    public double getEta() {
        return eta;
    }

    public void setEta(double eta) {
        this.eta = eta;
    }

    public MetricType[] getMetricType() {
        return metricType;
    }

    public OptimizerType getOptimizer() {
        return optimizer;
    }

    public int getBatchSize() {
        return batchSize;
    }

    public int getMaxEpoch() {
        return maxEpoch;
    }

    public void setMaxEpoch(int maxEpoch) {
        this.maxEpoch = maxEpoch;
    }

    public String getRegularization() {
        return regularization;
    }

    public void setLambda(double lambda) {
        this.lambda = lambda;
    }

    public double getLambda() {
        return lambda;
    }

    public MetricType[] fetchMetric() {
        return metricType;
    }

    public boolean isUseDP(){
        return "true".equals(this.useDP);
    }

    public double getDpDelta(){
        return this.dpDelta;
    }

    public double getDpLambda(){
        return this.dpLambda;
    }

    public double getDpEpsilon(){
        return this.dpEpsilon;
    }

    public DifferentialPrivacyType getDpType(){
        return this.dpType;
    }

    public int getSeed(){
        return this.seed;
    }

    @Override
    public String toString() {
        return "LinearParameter{" +
                "minLoss=" + minLoss +
                ", eta=" + eta +
                ", metricType=" + Arrays.toString(metricType) +
                ", batchSize=" + batchSize +
                ", maxEpoch=" + maxEpoch +
                ", optimizer" + optimizer +
                ", regularization=" + regularization +
                ", lambda=" + lambda +
                '}';
    }

    public String serialize() {
        return this.toString();
    }

    public List<ParameterField> obtainPara() {
        List<ParameterField> res = new ArrayList<>();
        res.add(new NumberParameter("maxEpoch", "maxEpoch", 30, new String[]{"1", "1000"}, ParameterType.NUMS));
        res.add(new NumberParameter("minLoss", "minLoss", 0.02, new String[]{"0.0", "1.0"}, ParameterType.NUMS));
        res.add(new NumberParameter("eta", "eta", 0.02, new String[]{"0", "1"}, ParameterType.NUMS));
        res.add(new MultiParameter("metricType", "metricType", "RMSE", new String[]{"RMSE", "MAE", "MSE"}, ParameterType.MULTI));
        res.add(new CategoryParameter("optimizer", "optimizer", "BatchGD", new String[]{"BatchGD", "NEWTON"}, ParameterType.STRING));
        res.add(new CategoryParameter("regularization","regularization","L1",new String[]{"L1", "L2"}, ParameterType.STRING));
        res.add(new NumberParameter("lambda","lambda",0.001,new String[]{"0","1"}, ParameterType.NUMS));
        res.add(new NumberParameter("dpEpsilon", "dpEpsilon", 1.6, new String[]{"0.05", "1.6"}, ParameterType.NUMS));
        res.add(new CategoryParameter("useDP", "useDP", "false", new String[]{"true", "false"}, ParameterType.STRING));
        res.add(new CategoryParameter("dpType", "dpType", "OUTPUT_PERTURB", new String[]{"OUTPUT_PERTURB", "OBJECTIVE_PERTURB"}, ParameterType.STRING));
        return res;
    }
}
