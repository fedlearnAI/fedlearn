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
import com.jdt.fedlearn.core.type.ParameterType;
import com.jdt.fedlearn.core.type.MetricType;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HorizontalFedAvgPara implements HyperParameter {
    private static final Logger logger = LoggerFactory.getLogger(HorizontalFedAvgPara.class);
    //fedAvg algo
    /*  e.g.,
        n_round: 100
        E: 5
        K: 100
        C: 0.3
        B: 50
     */
    private int numRound;           // training round, used by master
    private double fraction = 1;      // C, 0 < C <= 1, a C-fraction of clients are selected at each round, used by master
    private int numClients;         // K, number of clients
    private int batchSize;          // B, the local minibatch size, used by client
    private int localEpoch;         // E, the number of local epochs

    private String modelName;
    private MetricType[] eval_metric;         // 评价指标
    private String loss;                // 损失函数

    //not shown in UI
    private int datasetSize;

    //public HorizontalFedAvgPara() {}
    public HorizontalFedAvgPara() {
        this.numRound = 100;
        this.numClients = 3;
        this.fraction = 1.0;
        this.batchSize = 50;
        this.eval_metric = new MetricType[]{MetricType.RMSE};
        ;
        this.loss = "Regression:MSE";
        this.localEpoch = 5;
    }

    public HorizontalFedAvgPara(int numRound,
                                int numClients,
                                double fraction,
                                int batchSize,
                                int localEpoch,
                                MetricType[] eval_metric,
                                String loss) {
        this.numClients = numClients;
        this.fraction = fraction;
        this.batchSize = batchSize;
        this.eval_metric = eval_metric;
        this.loss = loss;
        this.numRound = numRound;
        this.localEpoch = localEpoch;
    }

    public List<ParameterField> obtainPara() {
        List<ParameterField> res = new ArrayList<>();
        res.add(new NumberParameter("numRound", "Server集成轮数", 100, new String[]{"1", "1000"}, ParameterType.NUMS));
        res.add(new NumberParameter("fraction", "Server每次集成需要的Client比例", 1, new String[]{"0", "1"}, ParameterType.NUMS));
        res.add(new NumberParameter("numClients", "Client个数", 3, new String[]{"1", "100"}, ParameterType.NUMS));
        res.add(new NumberParameter("batchSize", "Client的minibatch大小", 50, new String[]{"5", "10000"}, ParameterType.NUMS));
        res.add(new NumberParameter("localEpoch", "Client的localEpochs个数", 5, new String[]{"1", "500"}, ParameterType.NUMS));

        res.add(new MultiParameter("eval_metric", "eval_metric", "RMSE", new String[]{"RMSE", "AUC"}, ParameterType.MULTI));
        res.add(new CategoryParameter("loss", "loss", "Regression:MSE", new String[]{"Regression:MSE", "Classification:Cross entropy"}, ParameterType.STRING));
        res.add(new CategoryParameter("modelName", "模型名", "SGDRegressor", new String[]{"SGDRegressor", "SGDClassifier"}, ParameterType.STRING));
        //res.add(new ParameterField("datasetSize", "预留1", "2000", new String[]{"2000"}, ParameterType.NUMS));
        return res;
    }

    @Override
    public MetricType[] fetchMetric() {
        return eval_metric;
    }

    @Override
    public String serialize() {
        return null;
    }

    public int getNumClients() {
        return numClients;
    }

    public int getNumRound() {
        return numRound;
    }

    public int getBatchSize() {
        return batchSize;
    }

    public int getLocalEpoch() {
        return localEpoch;
    }

    public double getFraction() {
        return fraction;
    }

    public MetricType[] getEval_metric() {
        return eval_metric;
    }

    public String getLoss() {
        return loss;
    }

    public String getModelName() {
        return modelName;
    }

    public int getDatasetSize() {
        return datasetSize;
    }

    public void setDatasetSize(int datasetSize) {
        this.datasetSize = datasetSize;
    }

    public String toJson() {
        String jsonStr;
        ObjectMapper objectMapper = new ObjectMapper();
        try {
            jsonStr = objectMapper.writeValueAsString(this);
        } catch (Exception e) {
            System.out.println("error");
            jsonStr = null;
        }
        return jsonStr;
    }

    public void parseJson(String jsonStr) {
        ObjectMapper mapper = new ObjectMapper();
        HorizontalFedAvgPara tmp = null;
        try {
            tmp = mapper.readValue(jsonStr, HorizontalFedAvgPara.class);
            this.numRound = tmp.numRound;
            this.numClients = tmp.numClients;
            this.eval_metric = tmp.eval_metric;
            this.loss = tmp.loss;
            this.fraction = tmp.fraction;
            this.batchSize = tmp.batchSize;
            this.localEpoch = tmp.localEpoch;
            this.modelName = tmp.modelName;
        } catch (IOException e) {
            System.out.println("parse error");
            logger.error("parse error", e);
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        HorizontalFedAvgPara that = (HorizontalFedAvgPara) o;
        return numRound == that.numRound && Double.compare(that.fraction, fraction) == 0 && numClients == that.numClients && batchSize == that.batchSize && localEpoch == that.localEpoch && datasetSize == that.datasetSize && Objects.equals(modelName, that.modelName) && Arrays.equals(eval_metric, that.eval_metric) && Objects.equals(loss, that.loss);
    }

    @Override
    public int hashCode() {
        int result = Objects.hash(numRound, fraction, numClients, batchSize, localEpoch, modelName, loss, datasetSize);
        result = 31 * result + Arrays.hashCode(eval_metric);
        return result;
    }
}

