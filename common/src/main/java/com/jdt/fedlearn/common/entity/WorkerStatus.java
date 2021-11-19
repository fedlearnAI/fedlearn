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
package com.jdt.fedlearn.common.entity;

import com.jdt.fedlearn.common.enums.TaskTypeEnum;
import com.jdt.fedlearn.common.entity.core.type.AlgorithmType;

import java.io.Serializable;

/**
 * @className: WorkerStatus
 * @description: 用于计算任务占用资源情况
 * @author: geyan29
 * @createTime: 2021/10/13 4:16 下午
 */
public class WorkerStatus implements Serializable {

    private TaskTypeEnum taskType;

    private String dataSet;

    private AlgorithmType algorithm;

    private String modelToken;

    private int phase;

    public WorkerStatus() {
    }

    public WorkerStatus(TaskTypeEnum taskType, String dataSet, AlgorithmType algorithm, String modelToken, int phase) {
        this.taskType = taskType;
        this.dataSet = dataSet;
        this.algorithm = algorithm;
        this.modelToken = modelToken;
        this.phase = phase;
    }

    public TaskTypeEnum getTaskType() {
        return taskType;
    }

    public void setTaskType(TaskTypeEnum taskType) {
        this.taskType = taskType;
    }

    public String getDataSet() {
        return dataSet;
    }

    public void setDataSet(String dataSet) {
        this.dataSet = dataSet;
    }

    public AlgorithmType getAlgorithm() {
        return algorithm;
    }

    public void setAlgorithm(AlgorithmType algorithm) {
        this.algorithm = algorithm;
    }

    public String getModelToken() {
        return modelToken;
    }

    public void setModelToken(String modelToken) {
        this.modelToken = modelToken;
    }

    public int getPhase() {
        return phase;
    }

    public void setPhase(int phase) {
        this.phase = phase;
    }
}
