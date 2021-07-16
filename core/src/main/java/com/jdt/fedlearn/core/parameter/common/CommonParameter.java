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

package com.jdt.fedlearn.core.parameter.common;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jdt.fedlearn.core.parameter.*;
import com.jdt.fedlearn.core.type.AlgorithmType;

import java.util.List;
import java.util.Map;

public class CommonParameter {

    public static List<ParameterField> constructList(AlgorithmType algorithm) {
        List<ParameterField> parameterFields = null;
        switch (algorithm) {
            case FederatedGB: {
                parameterFields = new FgbParameter().obtainPara();
                break;
            }
            case RandomForest:
            case DistributedRandomForest:
            case RandomForestJava: {
                parameterFields = new RandomForestParameter().obtainPara();
                break;
            }
            case HorizontalFedAvg: {
                parameterFields = new HorizontalFedAvgPara().obtainPara();
                break;
            }
            case VerticalLinearRegression: {
                parameterFields = new VerticalLinearParameter().obtainPara();
                break;
            }
            case MixGBoost: {
                parameterFields = new MixGBParameter().obtainPara();
                break;
            }
            case KernelBinaryClassification:
            case KernelBinaryClassificationJava:{
                parameterFields = new KernelLinearRegressionParameter().obtainPara();
                break;
            }
            case LinearRegression: {
                parameterFields = new LinearParameter().obtainPara();
                break;
            }
            case VerticalLR: {
                parameterFields = new VerticalLRParameter().obtainPara();
                break;
            }
            case VerticalFDNN: {
                parameterFields = new VerticalFDNNParameter().obtainPara();
                break;
            }
            case TreeInference: {
                parameterFields = new TreeInferenceParameter().obtainPara();
                break;
            }
            default: {
                break;
            }
        }
        return parameterFields;
    }


    public static List<ParameterField> constructList(String algorithmStr) {
        AlgorithmType algorithm = AlgorithmType.valueOf(algorithmStr);
        return constructList(algorithm);
    }

    /**
     * 讲入参的ListAlgorithmParam 转为一个对象
     *
     * @param algorithmParams ParameterFields
     * @return SuperParameter
     */
    private static <T extends SuperParameter> T convertListToSuperParameter(Map<String, Object> algorithmParams, Class<T> t) {
        algorithmParams.put("@clazz", t);
        ObjectMapper objectMapper = new ObjectMapper();
        // 设置序列化多了其他属性不报错
        objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        return objectMapper.convertValue(algorithmParams, t);
    }

    public static SuperParameter parseParameter(Map<String, Object> parameterFields, AlgorithmType algorithmType) {
        SuperParameter parameter;
        if (AlgorithmType.VerticalLinearRegression == algorithmType) {
            parameter = convertListToSuperParameter(parameterFields, VerticalLinearParameter.class);
        } else if (AlgorithmType.RandomForest == algorithmType) {
            parameter = convertListToSuperParameter(parameterFields, RandomForestParameter.class);
        } else if (AlgorithmType.RandomForestJava == algorithmType) {
            parameter = convertListToSuperParameter(parameterFields, RandomForestParameter.class);
        } else if (AlgorithmType.DistributedRandomForest == algorithmType) {
            parameter = convertListToSuperParameter(parameterFields, RandomForestParameter.class);
        } else if (AlgorithmType.FederatedGB == algorithmType) {
            parameter = convertListToSuperParameter(parameterFields, FgbParameter.class);
        } else if (AlgorithmType.MixGBoost == algorithmType) {
            parameter = convertListToSuperParameter(parameterFields, MixGBParameter.class);
        } else if (AlgorithmType.LinearRegression == algorithmType) {
            parameter = convertListToSuperParameter(parameterFields, LinearParameter.class);
        } else if (AlgorithmType.KernelBinaryClassification == algorithmType) {
            parameter = convertListToSuperParameter(parameterFields, KernelLinearRegressionParameter.class);
        } else if (AlgorithmType.KernelBinaryClassificationJava == algorithmType) {
            parameter = convertListToSuperParameter(parameterFields, KernelLinearRegressionParameter.class);
        } else if (AlgorithmType.HorizontalFedAvg == algorithmType) {
            parameter = convertListToSuperParameter(parameterFields, HorizontalFedAvgPara.class);
        } else if (AlgorithmType.VerticalLR == algorithmType) {
            parameter = convertListToSuperParameter(parameterFields, VerticalLRParameter.class);
        } else if (AlgorithmType.VerticalFDNN == algorithmType) {
            parameter = convertListToSuperParameter(parameterFields, VerticalFDNNParameter.class);
        } else if (AlgorithmType.TreeInference == algorithmType) {
            parameter = convertListToSuperParameter(parameterFields, TreeInferenceParameter.class);
        } else {
            throw new UnsupportedOperationException();
        }
        return parameter;
    }

    public static SuperParameter parseParameter(Map<String, Object> parameterFields, String algorithmStr) {
        AlgorithmType algorithm = AlgorithmType.valueOf(algorithmStr);
        return parseParameter(parameterFields, algorithm);
    }

}
