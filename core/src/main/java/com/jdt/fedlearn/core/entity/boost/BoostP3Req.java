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

package com.jdt.fedlearn.core.entity.boost;

import com.jdt.fedlearn.common.entity.core.ClientInfo;
import com.jdt.fedlearn.common.entity.core.Message;

import java.util.ArrayList;
import java.util.List;

public class BoostP3Req implements Message {
    //要请求的客户端信息
    private final ClientInfo client;
    // 收集到的每个客户端以及对应的 Gradient 和 hessian 数据
    private final List<List<FeatureLeftGH>> dataList;
    private int workerNum;

    public BoostP3Req(ClientInfo client, List<BoostP2Res> res) {
        this.client = client;
        dataList = new ArrayList<>();
        for (BoostP2Res r : res) {
            List<FeatureLeftGH> subBodies = new ArrayList<>();
            for (FeatureLeftGH b : r.getFeatureGL()) {
                FeatureLeftGH boostP3ReqSubBody = new FeatureLeftGH(b.getClient(), b.getFeature(), b.getGhLeft());
                subBodies.add(boostP3ReqSubBody);
            }
            dataList.add(subBodies);
        }
    }

    public ClientInfo getClient() {
        return client;
    }

    public List<List<FeatureLeftGH>> getDataList() {
        return dataList;
    }

    public int getWorkerNum() {
        return workerNum;
    }

    public void setWorkerNum(int workerNum) {
        this.workerNum = workerNum;
    }


}
