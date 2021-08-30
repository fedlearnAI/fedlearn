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

package com.jdt.fedlearn.core.entity.kernelLinearRegression;

import com.jdt.fedlearn.core.entity.ClientInfo;
import com.jdt.fedlearn.core.entity.Message;
import com.jdt.fedlearn.core.type.KernelDispatchJavaPhaseType;
import com.jdt.fedlearn.core.type.MetricType;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class InferenceReqAndRes implements Message {
    private ClientInfo client;
    private Map<String, Double> predict = new HashMap<>();
    private double[] predictA;
    private double[][] predicts;
    private List<Double> multiClassUniqueLabelList;
    private int numClassRound;
    private boolean isActive;
    private int numClass;
    private List<Integer> testUid;
    private KernelDispatchJavaPhaseType kernelDispatchJavaPhaseType;

    private Map<MetricType, List<Double>> metric;
    private Map<MetricType, List<Double[][]>> metricArr;
    private Map<MetricType, List<Double>> metricVali;
    private Map<MetricType, List<Double[][]>> metricArrVali;


    public InferenceReqAndRes(ClientInfo client) {
        this.client = client;
    }

    public InferenceReqAndRes(ClientInfo client, Map<String, Double> predict) {
        this.client = client;
        this.predict = predict;
    }

    public InferenceReqAndRes( double[][] predicts) {
        this.predicts = predicts;
    }

    public InferenceReqAndRes(ClientInfo client, double[] predictA, double[][] predicts, int numClassRound, boolean isActive,int numClass,List<Integer> testUid) {
        this.client = client;
        this.predictA = predictA;
        this.predicts = predicts;
        this.numClassRound = numClassRound;
        this.isActive = isActive;
        this.numClass=numClass;
        this.testUid =testUid;
    }

    public InferenceReqAndRes(ClientInfo client, double[] predictA, double[][] predicts, int numClassRound, boolean isActive,int numClass,List<Integer> testUid,KernelDispatchJavaPhaseType kernelDispatchJavaPhaseType) {
        this.client = client;
        this.predictA = predictA;
        this.predicts = predicts;
        this.numClassRound = numClassRound;
        this.isActive = isActive;
        this.numClass=numClass;
        this.testUid =testUid;
        this.kernelDispatchJavaPhaseType=kernelDispatchJavaPhaseType;
    }

    public InferenceReqAndRes(List<Double> multiClassUniqueLabelList, boolean isActive, int numClass,KernelDispatchJavaPhaseType kernelDispatchJavaPhaseType) {
        this.multiClassUniqueLabelList = multiClassUniqueLabelList;
        this.isActive = isActive;
        this.numClass = numClass;
        this.kernelDispatchJavaPhaseType=kernelDispatchJavaPhaseType;
    }

    public Map<String, Double> getPredict() {
        return predict;
    }

    public ClientInfo getClient() {
        return client;
    }

    public double[] getPredictA() {
        return predictA;
    }

    public double[][] getPredicts() {
        return predicts;
    }

    public List<Double> getMultiClassUniqueLabelList() {
        return multiClassUniqueLabelList;
    }

    public boolean isActive() {
        return isActive;
    }

    public int getNumClassRound() {
        return numClassRound;
    }

    public int getNumClass() {
        return numClass;
    }

    public List<Integer> getTestUid() {
        return testUid;
    }

    public KernelDispatchJavaPhaseType getKernelDispatchJavaPhaseType() {
        return kernelDispatchJavaPhaseType;
    }

    public Map<MetricType, List<Double>> getMetric() {
        return metric;
    }

    public void setMetric(Map<MetricType, List<Double>> metric) {
        this.metric = metric;
    }

    public Map<MetricType, List<Double[][]>> getMetricArr() {
        return metricArr;
    }

    public void setMetricArr(Map<MetricType, List<Double[][]>> metricArr) {
        this.metricArr = metricArr;
    }

    public Map<MetricType, List<Double>> getMetricVali() {
        return metricVali;
    }

    public void setMetricVali(Map<MetricType, List<Double>> metricVali) {
        this.metricVali = metricVali;
    }

    public Map<MetricType, List<Double[][]>> getMetricArrVali() {
        return metricArrVali;
    }

    public void setMetricArrVali(Map<MetricType, List<Double[][]>> metricArrVali) {
        this.metricArrVali = metricArrVali;
    }
}
