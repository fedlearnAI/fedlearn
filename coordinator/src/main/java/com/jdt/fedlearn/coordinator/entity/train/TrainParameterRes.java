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


import com.jdt.fedlearn.common.util.TimeUtil;
import com.jdt.fedlearn.core.type.AlgorithmType;

import java.util.List;
import java.util.Map;

/**
 * 查询模型训练参数的结果实体，包含任务ID、模型token、算法参数、交叉验证任务参数、训练开始和结束时间、训练进度、算法类型、状态几方面信息
 *
 * @see TrainParameterQuery
 * @since 0.6.7
 */

public class TrainParameterRes {
    private final String taskId;
    private final String matchId;
    private final AlgorithmType algorithmType;
    private final String startTime;
    //对于未结束的训练 endTime 为 null
    private final String endTime;
    // 训练参数
    private final List<Map<String, Object>> algorithmParams;


    public TrainParameterRes(String taskId, AlgorithmType model, List<Map<String, Object>> algorithmParams, String startTime, String endTime) {
        this.taskId = taskId;
        this.algorithmParams = algorithmParams;
        this.startTime = startTime;
        this.endTime = endTime;
        this.algorithmType = model;
        this.matchId = "";
    }

    public TrainParameterRes(String taskId, String matchId, AlgorithmType model, List<Map<String, Object>> algorithmParams, long startTime, long endTime) {
        this.taskId = taskId;
        this.matchId = matchId;
        this.algorithmParams = algorithmParams;
        this.startTime = TimeUtil.parseLongtoStr(startTime);
        if (endTime == 0) {
            this.endTime = null;
        } else {
            this.endTime = TimeUtil.parseLongtoStr(endTime);
        }
        this.algorithmType = model;
    }


    public String getTaskId() {
        return taskId;
    }


    public String getMatchId() {
        return matchId;
    }

    public AlgorithmType getAlgorithmType() {
        return algorithmType;
    }

    public String getStartTime() {
        return startTime;
    }


    public String getEndTime() {
        return endTime;
    }

    public List<Map<String, Object>> getAlgorithmParams() {
        return algorithmParams;
    }

}
