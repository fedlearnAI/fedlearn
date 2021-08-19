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

package com.jdt.fedlearn.coordinator.entity.train;


import com.jdt.fedlearn.common.entity.SingleParameter;
import com.jdt.fedlearn.common.enums.RunningType;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 查询模型训练参数的结果实体，包含任务名称、任务ID、模型token、算法参数、交叉验证任务参数、训练开始和结束时间、训练流程、训练进度、算法类型、状态几方面信息
 * @see TrainParameterQuery
 * @since 0.6.7
 */

public class TrainParameterRes {
    /**
     * 任务名称
     */
    private String taskName;
    /**
     * 任务id
     */
    private String taskId;
    /**
     * modelToken
     */
    private String modelToken;
    /**
     * 训练参数
     */
    private List<Map<String, Object>> algorithmParams;
    /**
     * 交叉验证任务参数
     */
    private List<Map<String, Object>> crosspParams;
    /**
     * 训练开始时间
     */
    private String trainStartTime;
    /**
     * 训练结束时间
     */
    private String trainEndTime;
    /**
     * 训练流程
     */
    private List<String> trainInfo;
    /**
     * 训练进度
     */
    private int percent;
    /**
     * 算法类型
     */
    private String model;
    /**
     * 状态
     */
    private RunningType runningStatus;

    public TrainParameterRes(String taskId, String modelToken, List<SingleParameter> algorithmParams, List<SingleParameter> crosspParams, String trainStartTime, String trainEndTime,
                             List<String> trainInfo, double percent, String model, RunningType runningStatus) {
        this.taskId = taskId;
        this.modelToken = modelToken;
        for (SingleParameter parameter : algorithmParams) {
            Map<String, Object> single = new HashMap<>();
            single.put("field", parameter.getField());
            single.put("value", parameter.getValue());
            this.algorithmParams.add(single);
        }

        for (SingleParameter parameter : crosspParams) {
            Map<String, Object> single = new HashMap<>();
            single.put("field", parameter.getField());
            single.put("value", parameter.getValue());
            this.crosspParams.add(single);
        }
        this.trainStartTime = trainStartTime;
        this.trainEndTime = trainEndTime;
        this.trainInfo = trainInfo;
        this.percent = (int) percent;
        this.model = model;
        this.runningStatus = runningStatus;
    }


    public TrainParameterRes(String taskId, String modelToken, List<Map<String, Object>> algorithmParams, String trainStartTime, String trainEndTime,int percent, String model, RunningType runningStatus) {
        this.taskId = taskId;
        this.modelToken = modelToken;
        this.algorithmParams = algorithmParams;
        this.trainStartTime = trainStartTime;
        this.trainEndTime = trainEndTime;
        this.percent = percent;
        this.model = model;
        this.runningStatus = runningStatus;
    }


    public String getTaskName() {
        return taskName;
    }

    public void setTaskName(String taskName) {
        this.taskName = taskName;
    }

    public String getTaskId() {
        return taskId;
    }

    public void setTaskId(String taskId) {
        this.taskId = taskId;
    }

    public String getModelToken() {
        return modelToken;
    }

    public void setModelToken(String modelToken) {
        this.modelToken = modelToken;
    }

    public List<String> getTrainInfo() {
        return trainInfo;
    }

    public void setTrainInfo(List<String> trainInfo) {
        this.trainInfo = trainInfo;
    }

    public int getPercent() {
        return percent;
    }

    public void setPercent(int percent) {
        this.percent = percent;
    }

    public String getModel() {
        return model;
    }

    public void setModel(String model) {
        this.model = model;
    }

    public String getTrainStartTime() {
        return trainStartTime;
    }

    public void setTrainStartTime(String trainStartTime) {
        this.trainStartTime = trainStartTime;
    }

    public String getTrainEndTime() {
        return trainEndTime;
    }

    public void setTrainEndTime(String trainEndTime) {
        this.trainEndTime = trainEndTime;
    }

    public RunningType getRunningStatus() {
        return runningStatus;
    }

    public void setRunningStatus(RunningType runningStatus) {
        this.runningStatus = runningStatus;
    }

    public List<Map<String, Object>> getAlgorithmParams() {
        return algorithmParams;
    }

    public void setAlgorithmParams(List<Map<String, Object>> algorithmParams) {
        this.algorithmParams = algorithmParams;
    }

    public List<Map<String, Object>> getCrosspParams() {
        return crosspParams;
    }

    public void setCrosspParams(List<Map<String, Object>> crosspParams) {
        this.crosspParams = crosspParams;
    }

}
