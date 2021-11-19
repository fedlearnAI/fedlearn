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

package com.jdt.fedlearn.coordinator.entity.jdchain;
import com.jdt.fedlearn.coordinator.entity.train.TrainContext;
import com.jdt.fedlearn.common.entity.core.Message;

/**
 * @className: JdChainTaskStatus
 * @description: 包装区块链版本的训练信息
 * @author: geyan29
 * @createTime: 2021/1/28 5:37 下午
 */
public class JdChainTaskStatus implements Message {
    private String startTime;
    private String modifyTime;
    private TrainContext trainContext;
    private String percent;

    public JdChainTaskStatus() {
    }

    public JdChainTaskStatus(String startTime, String modifyTime, TrainContext trainContext) {
        this.startTime = startTime;
        this.modifyTime = modifyTime;
        this.trainContext = trainContext;
    }

    public String getStartTime() {
        return startTime;
    }

    public void setStartTime(String startTime) {
        this.startTime = startTime;
    }

    public String getModifyTime() {
        return modifyTime;
    }

    public void setModifyTime(String modifyTime) {
        this.modifyTime = modifyTime;
    }

    public TrainContext getTrainContext() {
        return trainContext;
    }

    public void setTrainContext(TrainContext trainContext) {
        this.trainContext = trainContext;
    }

    public String getPercent() {
        return percent;
    }

    public void setPercent(String percent) {
        this.percent = percent;
    }
}
