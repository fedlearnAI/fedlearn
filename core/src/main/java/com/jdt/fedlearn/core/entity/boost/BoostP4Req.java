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

public class BoostP4Req implements Message {
    private ClientInfo client;
    //特征索引
    private int kOpt;
    //分裂点索引
    private int vOpt;
    private final boolean accept;
    private int workerNum;

    public BoostP4Req(boolean accept) {
        this.accept = accept;
    }

    public BoostP4Req(ClientInfo client, int kOpt, int vOpt, boolean accept) {
        this.client = client;
        this.kOpt = kOpt;
        this.vOpt = vOpt;
        this.accept = accept;
    }

    public int getkOpt() {
        return kOpt;
    }

    public int getvOpt() {
        return vOpt;
    }

    public ClientInfo getClient() {
        return client;
    }

    public boolean isAccept() {
        return accept;
    }

    public int getWorkerNum() {
        return workerNum;
    }

    public void setWorkerNum(int workerNum) {
        this.workerNum = workerNum;
    }
}
