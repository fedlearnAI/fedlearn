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

package com.jdt.fedlearn.core.psi.mixMd5;

import com.jdt.fedlearn.core.entity.base.EmptyMessage;
import com.jdt.fedlearn.core.entity.Message;
import com.jdt.fedlearn.core.entity.psi.MatchInitRes;
import com.jdt.fedlearn.core.psi.PrepareClient;

import java.util.Map;

/**
 * @author zhangwenxi
 */
public class MixMd5MatchClient implements PrepareClient {
    private String[] commonIds;
    @Override
    public String[] getCommonIds() {
        return commonIds;
    }

    public void setCommonIds(String[] commonIds) {
        this.commonIds = commonIds;
    }

    @Override
    public Message init(String[] uid, Map<String,Object> others) {
        return new MatchInitRes(null, uid);
    }

    public Message client(int phase, Message parameterData, String[] data) {
       return EmptyMessage.message();
    }
}
