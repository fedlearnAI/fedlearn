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

package com.jdt.fedlearn.core.psi.md5;

import com.jdt.fedlearn.core.entity.Message;
import com.jdt.fedlearn.core.entity.base.EmptyMessage;
import com.jdt.fedlearn.core.entity.psi.MatchInitRes;
import com.jdt.fedlearn.core.entity.psi.MatchTransit;
import com.jdt.fedlearn.core.psi.PrepareClient;

import java.util.Map;

/**
 * Vertical_MD5客户端的信息传输类
 */
public class Md5MatchClient implements PrepareClient {
    private String[] commonIds = null;
    @Override
    public String[] getCommonIds() {
        return commonIds;
    }


    @Override
    public Message init(String[] uid, Map<String,Object> others) {
        // TODO uid加密传输
        return new MatchInitRes(null, uid);
    }

    public Message client(int phase, Message parameterData, String[] data) {
        if (phase == 1) {
            if (!(parameterData instanceof MatchTransit)) {
                throw new UnsupportedOperationException("Vertical-MD5 Phase 1 should be instance of MatchTransit");
            }
            // 结束了之后收取由master发过来的commonIds并赋值全局变量
            commonIds = ((MatchTransit)parameterData).getIds().values().toArray(new String[0]);
        }
        return EmptyMessage.message();
    }

}
