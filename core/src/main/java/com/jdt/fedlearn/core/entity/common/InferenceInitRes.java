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

import com.jdt.fedlearn.core.entity.Message;

import java.util.List;

/**
 * 标记使用白名单还是黑名单，当isAllowList为true时返回可用的uid index 列表，为false时返回不可用uid index列表
 */
public class InferenceInitRes implements Message {
    private final boolean isAllowList;
    private final int[] uid;

    public InferenceInitRes(boolean isAllowList, int[] uid) {
        this.isAllowList = isAllowList;
        this.uid = uid;
    }

    public InferenceInitRes(boolean isAllowList, List<Integer> uid) {
        this.isAllowList = isAllowList;
        this.uid = uid.stream().mapToInt(x->x).toArray();
    }

    public boolean isAllowList() {
        return isAllowList;
    }

    public int[] getUid() {
        return uid;
    }
}
