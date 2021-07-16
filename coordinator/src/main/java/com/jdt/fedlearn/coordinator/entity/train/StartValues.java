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

import com.jdt.fedlearn.core.entity.ClientInfo;
import com.jdt.fedlearn.core.entity.Message;
import com.jdt.fedlearn.core.entity.feature.Features;
import com.jdt.fedlearn.core.type.AlgorithmType;
import com.jdt.fedlearn.core.psi.MatchResult;


import java.util.List;

/**
 * <p>任务训练中的参数</p>
 * <p>包含TaskID，数据集信息，超参数，算法类型，客户端信息，id对齐信息，特征对齐信息</p>
 *
 * @author lijingxi
 */

public class StartValues implements Message {
    /**
     * 任务id
     */
    private String taskId;

    /**
     * 超参数，与算法对应
     */
    private List<SingleParameter> parameter;
    /**
     * 枚举类型的算法，与算法类对应
     */
    private AlgorithmType supportedAlgorithm;
    /**
     * 客户端信息
     */
    private List<ClientInfo> clientInfos;
    /**
     * 特征对齐信息，与clientInfo 排序一一对应
     */
    private List<Features> feature;
    /**
     * 数据集，与clientInfo 排序一一对应
     */
    private List<String> dataset;
    /**
     * id对齐信息
     */
    private MatchResult idMap;


    public StartValues() {
    }

    public StartValues(String taskId, List<ClientInfo> clientInfos, List<Features> feature, MatchResult idMap, List<String> dataset) {
        this.taskId = taskId;
        this.clientInfos = clientInfos;
        this.feature = feature;
        this.idMap = idMap;
        this.dataset = dataset;
    }

    public String getTaskId() {
        return this.taskId;
    }

    public List<SingleParameter> getParameter() {
        return this.parameter;
    }

    public AlgorithmType getSupportedAlgorithm() {
        return supportedAlgorithm;
    }

    public List<ClientInfo> getClientInfos() {
        return this.clientInfos;
    }


    public List<Features> getFeature() {
        return this.feature;
    }

    public void setSupportedAlgorithm(AlgorithmType supportedAlgorithm) {
        this.supportedAlgorithm = supportedAlgorithm;
    }

    public List<String> getDataset() {
        return dataset;
    }

    public void setParameter(List<SingleParameter>  parameter) {
        this.parameter = parameter;
    }

    public MatchResult getIdMap() {
        return this.idMap;
    }

    @Override
    public String toString() {
        return "StartValues{" +
                "taskId='" + taskId + '\'' +
                ", dataset='" + dataset + '\'' +
                ", parameter=" + parameter +
                ", supportedAlgorithm=" + supportedAlgorithm +
                ", clientInfos=" + clientInfos +
                ", feature=" + feature +
                ", idMap=" + idMap +
                '}';
    }
}
