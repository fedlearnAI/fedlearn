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

package com.jdt.fedlearn.core.model.common;

import com.jdt.fedlearn.core.exception.NotImplementedException;
import com.jdt.fedlearn.core.model.*;
import com.jdt.fedlearn.core.model.mixLinear.LinearRegressionModel;
import com.jdt.fedlearn.core.model.serialize.FgbModelSerializer;
import com.jdt.fedlearn.core.model.serialize.MixGBSerializer;
import com.jdt.fedlearn.core.model.serialize.ModelSerializer;
import com.jdt.fedlearn.core.research.secureInference.DelphiInferenceServer;
import com.jdt.fedlearn.core.research.secureInference.TreeInferenceServer;
import com.jdt.fedlearn.core.type.AlgorithmType;

public class CommonModel {

    public static Model constructModel(AlgorithmType algorithm) {
        Model model = null;
        switch (algorithm) {
            case VerticalLinearRegression:
                model = new VerticalLinearModel();
                break;
            case RandomForest:
                model = new RandomForestModel();
                break;
            case DistributedRandomForest:
                model = new DistributedRandomForestModel();
                break;
            case FederatedGB:
                model = new FederatedGBModel();
                break;
            case DistributedFederatedGB:
                model = new DistributedFederatedGBModel();
                break;
            case MixGBoost:
                model = new MixGBModel();
                break;
            case LinearRegression:
                model = new LinearRegressionModel();
                break;
            case HorizontalFedAvg:
                model = new HorizontalFedAvgModel();
                break;
            case VerticalLR:
                model = new VerticalLRModel();
                break;
            case FederatedKernel:
                model = new FederatedKernelModel();
                break;
            case VerticalFDNN:
                model = new VerticalFDNNModel();
                break;
            case TreeInference:
                model = new TreeInferenceServer();
                break;
            case DelphiInference:
                model = new DelphiInferenceServer();
                break;
            default:
                throw new NotImplementedException("not implemented algorithm");
        }
        return model;
    }

    public static Model constructModel(AlgorithmType algorithm, ModelSerializer serializer) {
        Model model = null;
        switch (algorithm) {
            case VerticalLinearRegression:
                model = new VerticalLinearModel();
                break;
            case RandomForest:
                model = new RandomForestModel();
                break;
            case DistributedRandomForest:
                model = new DistributedRandomForestModel();
                break;
            case FederatedGB:
                FgbModelSerializer fgbModelSerializer = (FgbModelSerializer)serializer;
                model = new FederatedGBModel(fgbModelSerializer);
                break;
            case MixGBoost:
                MixGBSerializer mixGBSerializer = (MixGBSerializer) serializer;
                model = new MixGBModel(mixGBSerializer);
                break;
            case LinearRegression:
                model = new LinearRegressionModel();
                break;
            case HorizontalFedAvg:
                model = new HorizontalFedAvgModel();
                break;
            case VerticalLR:
                model = new VerticalLRModel();
                break;
            case FederatedKernel:
                model = new FederatedKernelModel();
                break;
            case VerticalFDNN:
                model = new VerticalFDNNModel();
                break;
            case TreeInference:
                model = new TreeInferenceServer();
                break;
            case DelphiInference:
                model = new DelphiInferenceServer();
                break;
            default:
                throw new NotImplementedException("not implemented algorithm");
        }
        return model;
    }
}
