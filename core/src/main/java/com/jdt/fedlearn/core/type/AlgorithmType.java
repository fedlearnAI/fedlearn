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

package com.jdt.fedlearn.core.type;

import java.util.Arrays;

/**
 * 算法类型
 */

public enum AlgorithmType {
    VerticalLinearRegression("VerticalLinearRegression"),
    LinearRegression("LinearRegression"),
    FederatedGB("FederatedGB"),
    DistributedFederatedGB("DistributedFederatedGB"),
    MixGBoost("MixGBoost"),
//    RandomForest("RandomForest"),
    RandomForest("RandomForest"),
    //KernelLinearRegression("KernelLinearRegression"),
//    KernelBinaryClassification("KernelBinaryClassification"),
    FederatedKernel("FederatedKernel"),
    HorizontalFedAvg("HorizontalFedAvg"),
    VerticalLR("VerticalLR"),
    DistributedRandomForest("DistributedRandomForest"),
    VerticalFDNN("VerticalFDNN"),
    TreeInference("TreeInference"),
    DelphiInference("DelphiInference")
    ;

    private final String type;

    AlgorithmType(String type) {
        this.type = type;
    }

    public String getAlgorithm() {
        return type;
    }

    public static AlgorithmType[] getAlgorithmTypes() {
        return AlgorithmType.values();
    }


    public static String[] getAlgorithms() {
        return Arrays.stream(AlgorithmType.values()).map(AlgorithmType::getAlgorithm).toArray(String[]::new);
    }

    public static AlgorithmType[] getAlgorithmTypesWithResearch() {
        return AlgorithmType.values();
    }


    public static String[] getAlgorithmsWithResearch() {
        return Arrays.stream(AlgorithmType.values()).map(AlgorithmType::getAlgorithm).toArray(String[]::new);
    }


}
