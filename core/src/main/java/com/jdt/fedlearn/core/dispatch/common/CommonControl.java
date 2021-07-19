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

package com.jdt.fedlearn.core.dispatch.common;

import com.jdt.fedlearn.core.dispatch.*;
import com.jdt.fedlearn.core.dispatch.mixLinear.LinearRegression;
import com.jdt.fedlearn.core.exception.NotImplementedException;
import com.jdt.fedlearn.core.parameter.*;
import com.jdt.fedlearn.core.research.secureInference.TreeInferenceClient;
import com.jdt.fedlearn.core.type.AlgorithmType;

public class CommonControl {

    /**
     * @param algorithmType 算法枚举类型
     * @param parameter     超参数
     * @return 构造的算法
     */
    public static Control dispatchConstruct(AlgorithmType algorithmType, SuperParameter parameter) {
        Control algorithm;

        switch (algorithmType) {
            case VerticalLinearRegression: {
                VerticalLinearParameter verticalLinearParameter = (VerticalLinearParameter) parameter;
                algorithm = new VerticalLinearRegression(verticalLinearParameter);
                break;
            }
            case RandomForest: {
                RandomForestParameter rfParameter = (RandomForestParameter) parameter;
                algorithm = new RandomForest(rfParameter);
                break;
            }
            case FederatedGB: {
                FgbParameter fgbParameter = (FgbParameter) parameter;
                algorithm = new FederatedGB(fgbParameter);
                break;
            }
            case LinearRegression: {
                LinearParameter linearParameter = (LinearParameter) parameter;
                algorithm = new LinearRegression(linearParameter);
                break;
            }
            case KernelBinaryClassification:{
                KernelLinearRegressionParameter kernelLinearRegressionParameter = (KernelLinearRegressionParameter) parameter;
                algorithm = new KernelLinearRegression(kernelLinearRegressionParameter);
                break;
            }
            case KernelBinaryClassificationJava: {
                KernelLinearRegressionParameter kernelLinearRegressionParameter = (KernelLinearRegressionParameter) parameter;
                algorithm = new KernelLinearRegressionJava(kernelLinearRegressionParameter);
                break;
            }
            case HorizontalFedAvg: {
                HorizontalFedAvgPara HFLParameter = (HorizontalFedAvgPara) parameter;
                algorithm = new HorizontalFedAvg(HFLParameter);
                break;
            }
            case RandomForestJava: {
                RandomForestParameter rfParameter = (RandomForestParameter) parameter;
                algorithm = new RandomForestJava(rfParameter);
                break;
            }
            case DistributedRandomForest: {
                RandomForestParameter rfParameter = (RandomForestParameter) parameter;
                algorithm = new DistributedRandomForest(rfParameter);
                break;
            }
            case VerticalLR: {
                VerticalLRParameter verticalLRParameter = (VerticalLRParameter) parameter;
                algorithm = new VerticalLogisticRegression(verticalLRParameter);
                break;
            }
            case VerticalFDNN: {
                VerticalFDNNParameter verticalFDNNParameter = (VerticalFDNNParameter) parameter;
                algorithm = new VerticalFDNN(verticalFDNNParameter);
                break;
            }
            case TreeInference: {
                algorithm = new TreeInferenceClient();
                break;
            }
            default:
                throw new NotImplementedException("not implemented algorithm:" + algorithmType.toString());
        }
        return algorithm;
    }


    /**
     * @param supportedAlgorithmStr 算法枚举类型
     * @param parameter             超参数
     * @return 构造的算法
     */
    public static Control dispatchConstruct(String supportedAlgorithmStr, SuperParameter parameter) {
        AlgorithmType algorithmType = AlgorithmType.valueOf(supportedAlgorithmStr);
        return dispatchConstruct(algorithmType, parameter);
    }

}
