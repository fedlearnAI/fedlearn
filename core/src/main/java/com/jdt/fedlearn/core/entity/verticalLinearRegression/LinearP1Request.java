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

package com.jdt.fedlearn.core.entity.verticalLinearRegression;

import com.jdt.fedlearn.core.entity.ClientInfo;
import com.jdt.fedlearn.core.entity.Message;

import java.util.List;


public class LinearP1Request implements Message {
    private String pubKey;
    private List<Long> batchSamples;
    private boolean newIter;
    private ClientInfo client;

    public LinearP1Request(ClientInfo client, boolean newIter, String pubKey) {
        this.client = client;
        this.pubKey = pubKey;
        this.newIter = newIter;
    }

    public boolean isNewIter() {
        return newIter;
    }

    public String getPubKey() {
        return pubKey;
    }

//    public List<Long> getBatchSamples() {
//        return batchSamples;
//    }

    public ClientInfo getClient() {
        return client;
    }

}
