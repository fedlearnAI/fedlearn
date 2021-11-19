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

package com.jdt.fedlearn.core.entity.common;


import com.jdt.fedlearn.common.entity.core.Message;

import java.util.Map;

/**
 * 通用推理过程初始化请求
 * @author wangpeiqi
 * @since 0.7.1
 */
public class InferenceInit implements Message {
    private final String[] uid;
    private Map<String, Object> others;

    public InferenceInit(String[] uid) {
        this.uid = uid;
    }

    public InferenceInit(String[] uid, Map<String, Object> others) {
        this.uid = uid;
        this.others = others;
    }


    public String[] getUid() {
        return uid;
    }

    public Map<String, Object> getOthers() {
        return others;
    }


}
