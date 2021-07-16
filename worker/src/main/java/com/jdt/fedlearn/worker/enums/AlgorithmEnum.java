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
package com.jdt.fedlearn.worker.enums;

import com.jdt.fedlearn.worker.service.RandomForestAlgorithmService;
import com.jdt.fedlearn.core.type.AlgorithmType;
import org.apache.commons.lang3.StringUtils;

/**
 * @Author: liuzhaojun10
 * @Date: 2020/9/7 14:55
 * @Description:
 */
public enum AlgorithmEnum {
    DEMO(AlgorithmType.DistributedRandomForest.getAlgorithm(), new RandomForestAlgorithmService());

    private final String supportedAlgorithm;
    private final RandomForestAlgorithmService algorithmService;

    AlgorithmEnum(String supportedAlgorithm, RandomForestAlgorithmService algorithmService) {
        this.supportedAlgorithm = supportedAlgorithm;
        this.algorithmService = algorithmService;
    }

    public String getSupportedAlgorithm() {
        return supportedAlgorithm;
    }

    public RandomForestAlgorithmService getAlgorithmService() {
        return algorithmService;
    }

    public static AlgorithmEnum findEnum(String supportedAlgorithm) {
        for (AlgorithmEnum algorithmEnum : AlgorithmEnum.values()) {
            if (StringUtils.equals(supportedAlgorithm, algorithmEnum.getSupportedAlgorithm())) {
                return algorithmEnum;
            }
        }
        return DEMO;
    }

    public static void main(String[] args) {
        System.out.println(AlgorithmType.RandomForestJava.getAlgorithm());
    }
}
