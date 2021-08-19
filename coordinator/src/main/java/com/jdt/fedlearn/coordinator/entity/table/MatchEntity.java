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

package com.jdt.fedlearn.coordinator.entity.table;

import com.jdt.fedlearn.common.entity.project.MatchPartnerInfo;
import com.jdt.fedlearn.common.entity.project.PartnerInfoNew;
import com.jdt.fedlearn.common.enums.RunningType;

import java.util.List;

/**
 * 记录id对齐信息的实体，与数据库表结构对应，记录信息包括对齐id、对齐进度、对齐状态、对齐结果
 */
public class MatchEntity {
    private String taskId;
    private String matchId;
    private int length;
    private String matchReport;
    private RunningType runningType;
    private List<MatchPartnerInfo> datasets;

    public MatchEntity(){}

    public MatchEntity(String taskId, String matchId, int length, String matchReport, RunningType runningType, List<MatchPartnerInfo> datasets) {
        this.taskId = taskId;
        this.matchId = matchId;
        this.length = length;
        this.matchReport = matchReport;
        this.runningType = runningType;
        this.datasets = datasets;
    }

    public String getTaskId() {
        return taskId;
    }

    public void setTaskId(String taskId) {
        this.taskId = taskId;
    }

    public String getMatchId() {
        return matchId;
    }

    public void setMatchId(String matchId) {
        this.matchId = matchId;
    }

    public int getLength() {
        return length;
    }

    public void setLength(int length) {
        this.length = length;
    }

    public String getMatchReport() {
        return matchReport;
    }

    public void setMatchReport(String matchReport) {
        this.matchReport = matchReport;
    }

    public RunningType getRunningType() {
        return runningType;
    }

    public void setRunningType(RunningType runningType) {
        this.runningType = runningType;
    }

    public List<MatchPartnerInfo> getDatasets() {
        return datasets;
    }

    public void setDatasets(List<MatchPartnerInfo> datasets) {
        this.datasets = datasets;
    }
}
