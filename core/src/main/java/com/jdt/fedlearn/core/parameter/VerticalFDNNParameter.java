package com.jdt.fedlearn.core.parameter;

import com.jdt.fedlearn.core.parameter.common.NumberParameter;
import com.jdt.fedlearn.core.parameter.common.ParameterField;
import com.jdt.fedlearn.core.type.MetricType;
import com.jdt.fedlearn.core.type.ParameterType;

import java.util.ArrayList;
import java.util.List;

public class VerticalFDNNParameter implements SuperParameter {
    private int batchSize; // bach size
    private int numEpochs; // num epochs
    private boolean isTest; // for integrate test

    public VerticalFDNNParameter() {
        this.batchSize = 32;
        this.numEpochs = 5;
    }

    public VerticalFDNNParameter(int batchSize, int numEpochs) {
        this.batchSize = batchSize;
        this.numEpochs = numEpochs;
    }

    public VerticalFDNNParameter(int batchSize, int numEpochs, boolean isTest) {
        this.batchSize = batchSize;
        this.numEpochs = numEpochs;
        this.isTest = isTest;

    }


    @Override
    public String serialize() {
        return null;
    }

    @Override
    public List<ParameterField> obtainPara() {
        List<ParameterField> res = new ArrayList<>();
        res.add(new NumberParameter("batch_size", "batch size", 32, new String[]{"16", "10000"}, ParameterType.NUMS));
        res.add(new NumberParameter("num_epochs", "num epochs", 5, new String[]{"1", "100"}, ParameterType.NUMS));
        res.add(new NumberParameter("isTest", "是否固定初始化值（测试用）", 0, new String[]{"true", "false"}, ParameterType.STRING));
//        res.add(new MultiParameter("eval_metric", "eval_metric", "RMSE", new String[]{"AUC", "KS"}, ParameterType.MULTI));
        return res;
    }

    @Override
    public MetricType[] fetchMetric() {
        return new MetricType[0];
    }

    public int getBatchSize() {
        return batchSize;
    }

    public int getNumEpochs() {
        return numEpochs;
    }

    public boolean getIsTest() {
        return isTest;
    }
}
