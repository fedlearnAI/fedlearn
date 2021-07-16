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

/**
 * 各个客户端查询表的条目
 */
public class QueryEntry {
    //自定义，记录id
    private int recordId;
    //特征索引
    private int featureIndex;
    //分裂值
    private double splitValue;

    //for jackson parse TODO  remove
    public QueryEntry() {
    }

    public QueryEntry(int recordId, int featureIndex, double splitValue) {
        this.recordId = recordId;
        this.featureIndex = featureIndex;
        this.splitValue = splitValue;
    }

    public int getRecordId() {
        return recordId;
    }

    public int getFeatureIndex() {
        return featureIndex;
    }

    public double getSplitValue() {
        return splitValue;
    }

}
