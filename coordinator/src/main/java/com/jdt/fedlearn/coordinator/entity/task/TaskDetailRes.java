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

package com.jdt.fedlearn.coordinator.entity.task;

import com.jdt.fedlearn.coordinator.entity.table.FeatureAnswer;
import com.jdt.fedlearn.coordinator.entity.table.PartnerProperty;
import com.jdt.fedlearn.coordinator.entity.table.TaskAnswer;

import java.util.List;

public class TaskDetailRes {
    private int taskId;
    private String taskName;
    private String owner;
    private String[] participants;
    private List<PartnerProperty> clientList;
    private List<FeatureAnswer> featureList;

    public TaskDetailRes(TaskAnswer taskAnswer, List<PartnerProperty> clientList, List<FeatureAnswer> featureList) {
        this.taskId = taskAnswer.getTaskId();
        this.taskName = taskAnswer.getTaskName();
        this.owner = taskAnswer.getOwner();
        this.participants = taskAnswer.getParticipants();
        this.clientList = clientList;
        this.featureList = featureList;
    }

    public TaskDetailRes(int taskId, String taskName, String owner, String[] participants, List<PartnerProperty> clientList, List<FeatureAnswer> featureList) {
        this.taskId = taskId;
        this.taskName = taskName;
        this.owner = owner;
        this.participants = participants;
        this.clientList = clientList;
        this.featureList = featureList;
    }

    public int getTaskId() {
        return taskId;
    }

    public void setTaskId(int taskId) {
        this.taskId = taskId;
    }

    public String getTaskName() {
        return taskName;
    }

    public void setTaskName(String taskName) {
        this.taskName = taskName;
    }

    public String getOwner() {
        return owner;
    }

    public void setOwner(String owner) {
        this.owner = owner;
    }

    public String[] getParticipants() {
        return participants;
    }

    public void setParticipants(String[] participants) {
        this.participants = participants;
    }

    public List<PartnerProperty> getClientList() {
        return clientList;
    }

    public void setClientList(List<PartnerProperty> clientList) {
        this.clientList = clientList;
    }

    public List<FeatureAnswer> getFeatureList() {
        return featureList;
    }

    public void setFeatureList(List<FeatureAnswer> featureList) {
        this.featureList = featureList;
    }
}
