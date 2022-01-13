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
import com.jdt.fedlearn.core.type.EncryptionType;
import com.jdt.fedlearn.core.type.ParameterType;
import com.jdt.fedlearn.core.type.MetricType;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RandomForestParameter implements HyperParameter {

    private static final Logger logger = LoggerFactory.getLogger(RandomForestParameter.class);
    private int numTrees;               // 树的数量
    private int maxDepth;               // 树的深度
    private int maxTreeSamples;         // 单棵树最多包含的样本数
    private int maxSampledFeatures;     // 最大采样特征数
    private double maxSampledRatio;     // 最大采样特征 ratio
    private int numPercentiles;         // 分位点采样个数
    private double boostRatio = 1.;          // loss 降低比率
    private int minSamplesSplit;        // minSamplesSplit
    private String localModel = "Null";          // localModel

    private MetricType[] eval_metric;         // 评价指标
    private String loss = "Regression:MSE";                // 损失函数
    //todo cat_features String[]
    private String cat_features;
    private EncryptionType encryptionType;      // 加密方式
    private String encryptionKeyPath = "null";        // paillier key path
    private int encryptionCertainty = 1024;        // paillier certainty
    private int randomSeed = 666;

    // 差分隐私epsilon
    private double dpEpsilon;
    // 是否使用差分隐私
    private String useDP;

    public RandomForestParameter() {
        this.numTrees = 10;
        this.maxDepth = 20;
        this.maxTreeSamples = 50000;
        this.maxSampledFeatures = 30;
        this.maxSampledRatio = .8;
        this.numPercentiles = 50;
        this.minSamplesSplit = 10;
        this.localModel = "Null";
        this.eval_metric = new MetricType[]{MetricType.RMSE};
        ;
        this.loss = "Regression:MSE";
        this.cat_features = "";
        this.boostRatio = 0.;
        this.encryptionType = EncryptionType.Javallier;
        this.encryptionKeyPath = "/export/Data/paillier/";
        this.encryptionCertainty = 1024;
    }

    public RandomForestParameter(int numTrees,
                                 int maxDepth,
                                 int maxTreeSamples,
                                 int maxSampledFeatures,
                                 double maxSampledRatio,
                                 int numPercentiles,
                                 int minSamplesSplit,
                                 String localModel,
                                 int nJobs,
                                 EncryptionType encryptionType,
                                 MetricType[] eval_metric,
                                 String loss,
                                 int randomSeed) {
        this.numTrees = numTrees;
        this.maxDepth = maxDepth;
        this.maxSampledFeatures = maxSampledFeatures;
        this.maxSampledRatio = maxSampledRatio;
        this.numPercentiles = numPercentiles;
        this.minSamplesSplit = minSamplesSplit;
        this.localModel = "Null";
        this.eval_metric = eval_metric;
        this.loss = "Regression:MSE";
        this.maxTreeSamples = maxTreeSamples;
        this.cat_features = "";
        this.boostRatio = 0.;
//        this.nJobs = nJobs;
        this.encryptionType = encryptionType;
        this.encryptionKeyPath = "/export/Data/paillier/";
        this.encryptionCertainty = 1024;
        this.randomSeed = randomSeed;
    }

    public RandomForestParameter(int numTrees,
                                 int maxDepth,
                                 int maxTreeSamples,
                                 int maxSampledFeatures,
                                 double maxSampledRatio,
                                 int numPercentiles,
                                 int minSamplesSplit,
                                 String localModel,
                                 int nJobs,
                                 EncryptionType encryptionType,
                                 MetricType[] eval_metric,
                                 String loss,
                                 int randomSeed,
                                 String useDP,
                                 double dpEpsilon) {
        this.numTrees = numTrees;
        this.maxDepth = maxDepth;
        this.maxSampledFeatures = maxSampledFeatures;
        this.maxSampledRatio = maxSampledRatio;
        this.numPercentiles = numPercentiles;
        this.minSamplesSplit = minSamplesSplit;
        this.localModel = "Null";
        this.eval_metric = eval_metric;
        this.loss = "Regression:MSE";
        this.maxTreeSamples = maxTreeSamples;
        this.cat_features = "";
        this.boostRatio = 0.;
//        this.nJobs = nJobs;
        this.encryptionType = encryptionType;
        this.encryptionKeyPath = "/export/Data/paillier/";
        this.encryptionCertainty = 1024;
        this.randomSeed = randomSeed;
        this.useDP = useDP;
        this.dpEpsilon = dpEpsilon;
    }


    public List<ParameterField> obtainPara() {
        List<ParameterField> res = new ArrayList<>();
        res.add(new NumberParameter("numTrees", "树的个数", 2, new String[]{"1", "100"}, ParameterType.NUMS));
        res.add(new NumberParameter("maxDepth", "maxDepth", 3, new String[]{"3", "100"}, ParameterType.NUMS));
        res.add(new NumberParameter("maxTreeSamples", "一棵树最多sample样本数，使用全部样本请输入-1", 300, new String[]{"-1", "100000"}, ParameterType.NUMS));
        res.add(new NumberParameter("maxSampledFeatures", "最多sample特征数", 25, new String[]{"0", "100"}, ParameterType.NUMS));
        res.add(new NumberParameter("maxSampledRatio", "最多sample比例", 0.6, new String[]{"0", "1"}, ParameterType.NUMS));
        res.add(new NumberParameter("numPercentiles", "numPercentiles", 30, new String[]{"3", "100"}, ParameterType.NUMS));
        res.add(new NumberParameter("minSamplesSplit", "最少分裂样本数", 30, new String[]{"1", "100"}, ParameterType.NUMS));
        // TODO: localModel暂仅算法内部调试使用，用户无选择意义。
//        res.add(new CategoryParameter("localModel", "localModel形式", "Null", new String[]{"Null"}, ParameterType.STRING));//, "LinearModel"
//        res.add(new NumberParameter("nJobs", "并行数", 10, new String[]{"1", "50"}, ParameterType.NUMS));
        res.add(new MultiParameter("eval_metric", "eval_metric", "RMSE", new String[]{"RMSE", "AUC", "MAPE", "KS", "F1", "ACC", "RECALL","RAE","R2","RRSE","MSE", "PRECISION", "CONFUSION", "ROCCURVE", "KSCURVE", "TPR", "FPR"}, ParameterType.MULTI));
        // TODO: loss暂仅支持Regression:MSE，故暂无需用户选择。
//        res.add(new CategoryParameter("loss", "loss", "Regression:MSE", new String[]{"Regression:MSE", "Classification:Cross entropy"}, ParameterType.STRING));
        res.add(new CategoryParameter("cat_features", "cat_features", "null", new String[]{}, ParameterType.STRING));
//        res.add(new NumberParameter("boostRatio", "boostRatio(无需改动)", 1, new String[]{"1", "100"}, ParameterType.NUMS));
        res.add(new CategoryParameter("encryptionType", "加密方式", "Javallier", new String[]{"Javallier"}, ParameterType.STRING));
        res.add(new NumberParameter("randomSeed", "随机种子", 666, new String[]{"1", "1000"}, ParameterType.NUMS));
        res.add(new NumberParameter("dpEpsilon", "dpEpsilon", 3.2, new String[]{"1.0", "3.2"}, ParameterType.NUMS));
        res.add(new CategoryParameter("useDP", "useDP", "false", new String[]{"true", "false"}, ParameterType.STRING));
        return res;
    }

    @Override
    public String serialize() {
        return null;
    }

    @Override
    public MetricType[] fetchMetric() {
        return eval_metric;
    }

    public int getNumTrees() {
        return numTrees;
    }

    public int getMaxDepth() {
        return maxDepth;
    }

    public int getMaxSampledFeatures() {
        return maxSampledFeatures;
    }

    public int getMaxTreeSamples() {
        return maxTreeSamples;
    }

    public int getNumPercentiles() {
        return numPercentiles;
    }

    public double getBoostRatio() {
        return boostRatio;
    }

    public int getMinSamplesSplit() {
        return minSamplesSplit;
    }

    public String getLocalModel() {
        return localModel;
    }

    public double getMaxSampledRatio() {
        return maxSampledRatio;
    }

    public MetricType[] getEval_metric() {
        return eval_metric;
    }

    public String getLoss() {
        return loss;
    }

    public boolean isUseDP(){
        return "true".equals(this.useDP);
    }

    public double getDpEpsilon(){
        return this.dpEpsilon;
    }

    public void setDpEpsilon(double epsilon){
        this.dpEpsilon = epsilon;
    }

    public void averageEpsilon(){
        this.dpEpsilon = this.dpEpsilon / (this.numTrees * this.maxDepth * (this.maxDepth + 1));
    }

    public String getCat_features() {
        return cat_features;
    }

    public int getRandomSeed() {
        return randomSeed;
    }

    public String getEncryptionKeyPath() {
        return encryptionKeyPath;
    }

    public int getEncryptionCertainty() {
        return encryptionCertainty;
    }

    public EncryptionType getEncryptionType() {
        return encryptionType;
    }

    public void parseJson(String jsonStr) {
        ObjectMapper mapper = new ObjectMapper();
        RandomForestParameter tmp = null;
        try {
            tmp = mapper.readValue(jsonStr, RandomForestParameter.class);
            this.numTrees = tmp.numTrees;
            this.maxDepth = tmp.maxDepth;
            this.maxSampledFeatures = tmp.maxSampledFeatures;
            this.numPercentiles = tmp.numPercentiles;
            this.minSamplesSplit = tmp.minSamplesSplit;
//            this.localModel = tmp.localModel;
            this.eval_metric = tmp.eval_metric;
//            this.loss = tmp.loss;
            this.cat_features = tmp.cat_features;
//            this.boostRatio = tmp.boostRatio;
            this.encryptionType = tmp.encryptionType;
            this.randomSeed = tmp.randomSeed;
        } catch (IOException e) {
            System.out.println("parse error");
            logger.error("parse error", e);
        }
    }

}

