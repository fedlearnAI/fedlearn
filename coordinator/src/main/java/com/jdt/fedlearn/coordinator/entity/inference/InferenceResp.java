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

package com.jdt.fedlearn.coordinator.entity.inference;

import java.util.List;

/**
 * @Name: InferenceResp
 */
public class InferenceResp {
    /**
     * 日志结果
     */
    private List<InferenceInfoDto> inferenceList;

    /**
     * 总页数
     */
    Integer inferenceCount;

    public List<InferenceInfoDto> getInferenceList() {
        return inferenceList;
    }

    public void setInferenceList(List<InferenceInfoDto> inferenceList) {
        this.inferenceList = inferenceList;
    }

    public Integer getInferenceCount() {
        return inferenceCount;
    }

    public void setInferenceCount(Integer inferenceCount) {
        this.inferenceCount = inferenceCount;
    }
}
