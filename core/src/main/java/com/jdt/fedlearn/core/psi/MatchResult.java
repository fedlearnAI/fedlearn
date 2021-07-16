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

package com.jdt.fedlearn.core.psi;

import com.jdt.fedlearn.core.entity.Message;


/**
 * Master端ID对齐结果client端储存后在master端缓存的实体类；记录任务ID，对齐Token，对齐结果report
 */
public class MatchResult implements Message {
    private String matchId;
    private int length;
    private MappingReport mappingReport;

    public MatchResult() {
    }

    public MatchResult(int length) {
        this.length = length;
    }

    public MatchResult(String matchId, int length, MappingReport mappingReport) {
        this.matchId = matchId;
        this.length = length;
        this.mappingReport = mappingReport;
    }

    public MappingReport getMappingReport() {
        return mappingReport;
    }

    public void setMappingReport(MappingReport mappingReport) {
        this.mappingReport = mappingReport;
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

}

