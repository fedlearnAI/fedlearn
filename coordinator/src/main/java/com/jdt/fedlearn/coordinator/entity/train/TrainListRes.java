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

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jdt.fedlearn.common.enums.RunningType;
import com.jdt.fedlearn.common.exception.DeserializeException;

import java.io.IOException;

/**
 *
 */
public class TrainListRes {
    private String taskId;
    private RunningType runningStatus;
    private String modelToken;

    public TrainListRes() {
    }

    public TrainListRes(String taskId,  RunningType runningStatus, String modelToken) {
        this.taskId = taskId;
        this.runningStatus = runningStatus;
        this.modelToken = modelToken;
    }

    public TrainListRes(String jsonStr) {
        parseJson(jsonStr);
    }


    public String getTaskId() {
        return this.taskId;
    }

    public RunningType getRunningStatus() {
        return runningStatus;
    }

    public String getModelToken() {
        return modelToken;
    }

    public void parseJson(String jsonStr) {
        ObjectMapper mapper = new ObjectMapper();
        TrainListRes p3r;
        try {
            p3r = mapper.readValue(jsonStr, TrainListRes.class);
            this.modelToken = p3r.getModelToken();
            this.taskId = p3r.getTaskId();
            this.runningStatus = p3r.runningStatus;
        } catch (IOException e) {
            throw new DeserializeException(e.getMessage());
        }
    }
}
