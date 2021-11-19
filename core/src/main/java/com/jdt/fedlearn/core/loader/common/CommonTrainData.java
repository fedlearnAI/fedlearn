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

import com.jdt.fedlearn.common.entity.core.feature.Features;

import java.util.List;

/**
 *
 */
public class CommonTrainData extends AbstractTrainData implements TrainData {

    /**
     * @param rawTable  表
     * @param commonIds id对齐结果，公共ID
     * @param features  特征
     */
    public CommonTrainData(String[][] rawTable, String[] commonIds, Features features) {
        super.scan(rawTable, commonIds, features);
    }

    /**
     * @param rawTable  数据
     * @param commonIds id对齐结果，公共ID
     * @param features  特征
     * @param expressions 特征处理表达式
     */
    public CommonTrainData(String[][] rawTable, String[] commonIds, Features features, List<String> expressions) {
        super.scan(rawTable, commonIds, features);
        if (expressions != null && expressions.size() != 0) {
            super.featureProcessing(expressions);
        }
    }
}
