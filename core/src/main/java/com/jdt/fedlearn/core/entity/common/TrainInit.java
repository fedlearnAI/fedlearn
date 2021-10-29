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

package com.jdt.fedlearn.core.entity.common;

import com.jdt.fedlearn.core.entity.Message;
import com.jdt.fedlearn.core.entity.feature.Features;
import com.jdt.fedlearn.core.parameter.HyperParameter;

import java.util.Map;

/**
 * 初始化过程为所有算法共用，请勿改动；master端使用
 * @author wangpeiqi
 */
public class TrainInit implements Message {
    private final HyperParameter parameter;
    private final Features featureList;
    private final int[] testIndex;
    private final String matchId;
    private final Map<String, Object> others;

    public TrainInit(HyperParameter parameter, Features featureList, int[] testIndex, String matchId, Map<String, Object> others) {
        this.parameter = parameter;
        this.featureList = featureList;
        this.testIndex = testIndex;
        this.matchId = matchId;
        this.others = others;
    }

    public TrainInit(HyperParameter parameter, Features featureList, String matchId, Map<String, Object> others) {
        this.parameter = parameter;
        this.featureList = featureList;
        this.matchId = matchId;
        this.others = others;
        this.testIndex = new int[]{};
    }
    public TrainInit(HyperParameter parameter, Features featureList, String matchId, Map<String, Object> others, int[] testIndex) {
        this.parameter = parameter;
        this.featureList = featureList;
        this.matchId = matchId;
        this.others = others;
        this.testIndex = testIndex;
    }

    public HyperParameter getParameter() {
        return parameter;
    }

    public int[] getTestIndex() {
        return testIndex;
    }

    public String getMatchId() {
        return matchId;
    }

    public Features getFeatureList() {
        return featureList;
    }

    public Map<String, Object> getOthers() {
        return others;
    }
}
