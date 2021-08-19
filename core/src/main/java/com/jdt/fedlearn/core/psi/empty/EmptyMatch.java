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

package com.jdt.fedlearn.core.psi.empty;

import com.jdt.fedlearn.core.entity.ClientInfo;
import com.jdt.fedlearn.core.entity.common.CommonRequest;
import com.jdt.fedlearn.core.entity.common.CommonResponse;
import com.jdt.fedlearn.core.psi.MatchResult;
import com.jdt.fedlearn.core.psi.Prepare;

import java.util.ArrayList;
import java.util.List;

public class EmptyMatch implements Prepare {

    public List<CommonRequest> masterInit(List<ClientInfo> clientInfos) {
        return new ArrayList<>();
    }


    public List<CommonRequest> master(List<CommonResponse> responses) {
        return new ArrayList<>();
    }

    /**
     * @param responses 各个返回结果
     * @return 预测结果
     */
    public MatchResult postMaster(List<CommonResponse> responses) {
        String report = "Match type is EMPTY!! \n ";
        return new MatchResult(0, report);

    }

    public boolean isContinue() {
        return false;
    }
}
