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

package com.jdt.fedlearn.core.entity.boost;

import com.jdt.fedlearn.core.entity.Message;
import com.jdt.fedlearn.core.entity.common.MetricValue;

import java.util.Arrays;

/**
 * 左子树相关信息，包括客户端存储后返回的recordId，左子树的样本id等
 */
public class LeftTreeInfo implements Message {
    private final int recordId;
    private final int[] leftInstances;
    private MetricValue trainMetric;


    public LeftTreeInfo(int recordId, int[] leftIns) {
        this.recordId = recordId;
        this.leftInstances = leftIns;
    }

    public int getRecordId() {
        return recordId;
    }

    public int[] getLeftInstances() {
        return leftInstances;
    }

    public void setTrainMetric(MetricValue trainMetric) {
        this.trainMetric = trainMetric;
    }

    public MetricValue getTrainMetric() {
        return trainMetric;
    }

    @Override
    public String toString() {
        return "BoostP4Res{" +
                ", recordId=" + recordId +
                ", iLeft=" + Arrays.toString(leftInstances) +
                '}';
    }
}
