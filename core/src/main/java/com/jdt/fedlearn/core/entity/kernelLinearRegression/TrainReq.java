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
import com.jdt.fedlearn.core.entity.common.PredictRes;

import java.util.List;


public class TrainReq implements Message {
    private ClientInfo client;
    private boolean isUpdate = false;
    private double[] valuelist;
    private double[][] valuelists;
    private List<Integer> sampleIndex;
    private int numClassRound;
    private PredictRes predictRes;
    private int bestRound;
    private int clientInd;

    public TrainReq(ClientInfo clientInfo, List<Integer> sampleIndex) {
        this.client = clientInfo;
        this.sampleIndex = sampleIndex;
    }

    public TrainReq(ClientInfo clientInfo, int numClassRound, int bestRound) {
        this.client = clientInfo;
        this.numClassRound = numClassRound;
        this.bestRound = bestRound;
    }

    public TrainReq(ClientInfo clientInfo, PredictRes predictRes) {
        this.client = clientInfo;
        this.predictRes = predictRes;
    }

    public TrainReq(ClientInfo clientInfo, double[] valuelist, List<Integer> sampleIndex, boolean isUpdate) {
        this.client = clientInfo;
        this.valuelist = valuelist;
        this.sampleIndex = sampleIndex;
        this.isUpdate = isUpdate;
    }

    public TrainReq(ClientInfo clientInfo, double[][] valuelists, boolean isUpdate) {
        this.client = clientInfo;
        this.valuelists = valuelists;
        this.isUpdate = isUpdate;
    }

    public ClientInfo getClient() {
        return client;
    }

    public List<Integer> getSampleIndex() {
        return sampleIndex;
    }


    public double[] getValueList() {
        return valuelist;
    }

    public double[][] getValuelists() {
        return valuelists;
    }

    public boolean isUpdate() {
        return isUpdate;
    }

    public int getNumClassRound() {
        return numClassRound;
    }

    public PredictRes getPredictRes() {
        return predictRes;
    }

    public int getBestRound() {
        return bestRound;
    }

    public int getClientInd() {
        return clientInd;
    }

    public void setClientInd(int clientInd) {
        this.clientInd = clientInd;
    }
}