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

package com.jdt.fedlearn.core.loader.common;

import com.jdt.fedlearn.core.entity.verticalFDNN.VFDNNInferenceData;
import com.jdt.fedlearn.core.exception.NotImplementedException;
import com.jdt.fedlearn.core.loader.linearRegression.LinearInferenceData;
import com.jdt.fedlearn.core.loader.mixGBoost.MixGBInferenceData;
import com.jdt.fedlearn.core.loader.randomForest.RFInferenceData;
import com.jdt.fedlearn.core.loader.secureInference.DelphiInferenceData;
import com.jdt.fedlearn.core.loader.secureInference.TreeInferenceData;
import com.jdt.fedlearn.core.type.AlgorithmType;

public class CommonLoad {

    public static InferenceData constructInference(AlgorithmType algorithm, String[][] rawData) {
        InferenceData inferenceData;
        switch (algorithm) {
            case VerticalLinearRegression:
            case VerticalLR:
            case FederatedKernel:
            case FederatedGB:
            case DistributedFederatedGB:
            case HorizontalFedAvg:
                inferenceData = new CommonInferenceData(rawData, "uid", null);
                break;
            case LinearRegression: {
                inferenceData = new LinearInferenceData(rawData, null);
                break;
            }
            case RandomForest:
            case DistributedRandomForest: {
                inferenceData = new RFInferenceData(rawData);
                break;
            }
            case MixGBoost: {
                inferenceData = new MixGBInferenceData(rawData);
                break;
            }
            case VerticalFDNN: {
                inferenceData = new VFDNNInferenceData(rawData);
                break;
            }
            case TreeInference: {
                inferenceData = new TreeInferenceData(rawData);
                break;
            }
            case DelphiInference: {
                inferenceData = new DelphiInferenceData(rawData);
                break;
            }
            default: {
                throw new NotImplementedException("not implemented algorithm in inference");
            }
        }
        return inferenceData;
    }

}
