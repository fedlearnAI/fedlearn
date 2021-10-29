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

package com.jdt.fedlearn.core.entity.randomForest;

import com.jdt.fedlearn.core.entity.Message;

import java.util.List;
import java.util.Map;

public class RandomForestInferMessage implements Message {

    private String modelString;
    private String[] inferenceUid;
    private double[] localPredict;
    private String type;
    private Map<Integer, Map<Integer, List<String>>> treeInfo;

    public RandomForestInferMessage(String modelString, String[] inferenceUid, double[] localPredict, String type) {
        this.modelString = modelString;
        this.inferenceUid = inferenceUid;
        this.localPredict = localPredict;
        this.type = type;
    }

    public RandomForestInferMessage(String[] inferenceUid, double[] localPredict, String type, Map<Integer, Map<Integer, List<String>>> treeInfo) {
        this.inferenceUid = inferenceUid;
        this.localPredict = localPredict;
        this.type = type;
        this.treeInfo = treeInfo;
    }

    public RandomForestInferMessage(String[] inferenceUid, Map<Integer, Map<Integer, List<String>>> treeInfo) {
        this.inferenceUid = inferenceUid;
        this.treeInfo = treeInfo;
    }

    public RandomForestInferMessage(double[] localPredict, String type) {
        this.localPredict = localPredict;
        this.type = type;
    }

    public String getModelString() {
        return modelString;
    }

    public String[] getInferenceUid() {
        return inferenceUid;
    }

    public double[] getLocalPredict() {
        return localPredict;
    }

    public String getType() {
        return type;
    }

    public Map<Integer, Map<Integer, List<String>>> getTreeInfo() {
        return treeInfo;
    }
}
