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

import com.jdt.fedlearn.common.entity.core.ClientInfo;
import com.jdt.fedlearn.common.entity.core.Message;
import com.jdt.fedlearn.core.type.KernelDispatchPhaseType;
import com.jdt.fedlearn.core.type.MetricType;

import java.util.List;
import java.util.Map;

public class TrainRes implements Message {
    private ClientInfo client;
    // 主被动标志
    private boolean isActive;
    private String body;
    private double[] vector;
    private double paraNorm;
    private double trainingloss;
    private Map<MetricType, List<Double>> metric;
    private double[][] vectors;
    private int numClassRound;
    private Map<MetricType, List<Double[][]>> metricArr;
    private Map<MetricType, List<Double>> metricVali;
    private Map<MetricType, List<Double[][]>> metricArrVali;
    private int round;
    private int clientInd;
    private List<ClientInfo> clientInfoList;
    private KernelDispatchPhaseType kernelDispatchPhaseType;


    public TrainRes(ClientInfo clientInfo, String body, boolean isActive) {
        this.client = clientInfo;
        this.body = body;
        this.isActive = isActive;
        this.paraNorm = 0.0;
        //TODO
        this.trainingloss = Double.POSITIVE_INFINITY;
    }

    public TrainRes(ClientInfo clientInfo, double[] vector, double paraNorm, boolean isActive) {
        this.client = clientInfo;
        this.vector = vector;
        this.paraNorm = paraNorm;
        this.isActive = isActive;
    }


    public TrainRes(ClientInfo clientInfo, int numClassRound, boolean isActive, Map<MetricType, List<Double>> metric, Map<MetricType, List<Double[][]>> metricArr, Map<MetricType, List<Double>> metricVali, Map<MetricType, List<Double[][]>> metricArrVali, KernelDispatchPhaseType kernelDispatchPhaseType) {
        this.client = clientInfo;
        this.numClassRound = numClassRound;
        this.isActive = isActive;
        this.metric = metric;
        this.metricArr = metricArr;
        this.metricVali = metricVali;
        this.metricArrVali = metricArrVali;
        this.kernelDispatchPhaseType = kernelDispatchPhaseType;
    }


    public TrainRes(ClientInfo clientInfo, int numClassRound, boolean isActive) {
        this.client = clientInfo;
        this.numClassRound = numClassRound;
        this.isActive = isActive;
    }

    public TrainRes(ClientInfo clientInfo, int numClassRound, boolean isActive, KernelDispatchPhaseType kernelDispatchPhaseType) {
        this.client = clientInfo;
        this.numClassRound = numClassRound;
        this.isActive = isActive;
        this.kernelDispatchPhaseType = kernelDispatchPhaseType;
    }

    public TrainRes(ClientInfo clientInfo, int round, KernelDispatchPhaseType kernelDispatchPhaseType) {
        this.client = clientInfo;
        this.round = round;
        this.kernelDispatchPhaseType = kernelDispatchPhaseType;
    }


    public TrainRes(ClientInfo clientInfo, double[][] vectors, double paraNorm, boolean isActive, int clientInd, int numClassRound, List<ClientInfo> clientInfoList, KernelDispatchPhaseType kernelDispatchPhaseType) {
        this.client = clientInfo;
        this.vectors = vectors;
        this.paraNorm = paraNorm;
        this.isActive = isActive;
        this.clientInd = clientInd;
        this.numClassRound = numClassRound;
        this.clientInfoList = clientInfoList;
        this.kernelDispatchPhaseType = kernelDispatchPhaseType;
    }

    public TrainRes(ClientInfo clientInfo, double[] vector, Map<MetricType, List<Double>> metric, double paraNorm, boolean isActive) {
        this.client = clientInfo;
        this.vector = vector;
        this.metric = metric;
        this.paraNorm = paraNorm;
        this.isActive = isActive;
    }

    public ClientInfo getClient() {
        return client;
    }

    public boolean getActive() {
        return isActive;
    }

    public String getBody() {
        return body;
    }

    public double getTrainingloss() {
        return trainingloss;
    }

    public double[] getVector() {
        return vector;
    }

    public double[][] getVectors() {
        return vectors;
    }

    public double getParaNorm() {
        return paraNorm;
    }

    public Map<MetricType, List<Double>> getMetric() {
        return metric;
    }

    public int getNumClassRound() {
        return numClassRound;
    }

    public Map<MetricType, List<Double[][]>> getMetricArr() {
        return metricArr;
    }

    public boolean isActive() {
        return isActive;
    }

    public Map<MetricType, List<Double>> getMetricVali() {
        return metricVali;
    }

    public Map<MetricType, List<Double[][]>> getMetricArrVali() {
        return metricArrVali;
    }

    public int getRound() {
        return round;
    }

    public int getClientInd() {
        return clientInd;
    }

    public List<ClientInfo> getClientInfoList() {
        return clientInfoList;
    }

    public KernelDispatchPhaseType getKernelDispatchJavaPhaseType() {
        return kernelDispatchPhaseType;
    }

    public void setMetricArr(Map<MetricType, List<Double[][]>> metricArr) {
        this.metricArr = metricArr;
    }

    public void setMetricVali(Map<MetricType, List<Double>> metricVali) {
        this.metricVali = metricVali;
    }

    public void setMetricArrVali(Map<MetricType, List<Double[][]>> metricArrVali) {
        this.metricArrVali = metricArrVali;
    }

    public void setMetric(Map<MetricType, List<Double>> metric) {
        this.metric = metric;
    }

}


