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


    public InferenceReqAndRes(ClientInfo client) {
        this.client = client;
    }

    public InferenceReqAndRes(ClientInfo client, Map<String, Double> predict) {
        this.client = client;
        this.predict = predict;
    }

    public InferenceReqAndRes(ClientInfo client, double[] predictA, double[][] predicts, int numClassRound, boolean isActive,int numClass) {
        this.client = client;
        this.predictA = predictA;
        this.predicts = predicts;
        this.numClassRound = numClassRound;
        this.isActive = isActive;
        this.numClass=numClass;
    }

    public InferenceReqAndRes(List<Double> multiClassUniqueLabelList, boolean isActive, int numClass) {
        this.multiClassUniqueLabelList = multiClassUniqueLabelList;
        this.isActive = isActive;
        this.numClass = numClass;
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

}
