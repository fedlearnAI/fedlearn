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

import com.jdt.fedlearn.core.entity.ClientInfo;

import java.util.*;

/**
 * id对齐结果，包括对齐后的数据和对齐报告,
 * <p>
 * 对齐后的数据分为两种，发给各方的数据相同和不同
 * n个client的mapping结果在部分算法是一样的，可以优化存储
 */
public class MappingOutput {
    private final boolean isSame;
    // clientInfos 与 result中的元素一一对应,或者为多对一关系
    private final List<ClientInfo> clientInfos;
    private final List<MappingResult> mappingResults;
    private final MappingReport report;

    public MappingOutput(Map<ClientInfo, MappingResult> result, MappingReport report) {
        this.isSame = false;
        this.clientInfos = new ArrayList<>();
        this.mappingResults = new ArrayList<>();
        for (Map.Entry<ClientInfo, MappingResult> entry : result.entrySet()) {
            clientInfos.add(entry.getKey());
            mappingResults.add(entry.getValue());
        }
        this.report = report;
    }

    public MappingOutput(List<ClientInfo> clientInfoList, MappingResult result, MappingReport report) {
        this.isSame = true;
        this.clientInfos = clientInfoList;
        this.mappingResults = Collections.singletonList(result);
        this.report = report;
    }


    public MappingResult getAnyResult() {
        return mappingResults.get(0);
    }

    public MappingResult getResultByClient(ClientInfo client) {
        if (!clientInfos.contains(client)) {
            return null;
        }
        if (isSame) {
            return mappingResults.get(0);
        } else {
            int index = clientInfos.indexOf(client);
            return mappingResults.get(index);
        }
    }

    public MappingReport getReport() {
        return report;
    }

    public List<ClientInfo> getClientInfos() {
        return clientInfos;
    }
}
