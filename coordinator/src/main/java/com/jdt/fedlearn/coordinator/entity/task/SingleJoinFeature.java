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

import java.io.Serializable;

/**
 * 特征对齐实体，用于对齐不同参与方相同的特征信息
 *
 * @author lijingxi
 */
public class SingleJoinFeature extends SingleCreateFeature implements Serializable {
    // 特征名称
    private String name;
    // 特征类型
    private String dtype;
    // 特征描述
    private String describe;
    // 对齐信息
    private Alignment alignment;

    public SingleJoinFeature() {
    }

    public SingleJoinFeature(String name, String dtype, String describe, Alignment alignment) {
        this.name = name;
        this.dtype = dtype;
        this.describe = describe;
        this.alignment = alignment;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDtype() {
        return dtype;
    }

    public void setDtype(String dtype) {
        this.dtype = dtype;
    }

    public String getDescribe() {
        return describe;
    }

    public void setDescribe(String describe) {
        this.describe = describe;
    }

    public Alignment getAlignment() {
        return alignment;
    }

    public void setAlignment(Alignment alignment) {
        this.alignment = alignment;
    }
}
