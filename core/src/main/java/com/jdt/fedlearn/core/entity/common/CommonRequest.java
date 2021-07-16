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

import com.jdt.fedlearn.core.entity.ClientInfo;
import com.jdt.fedlearn.core.entity.Message;

public class CommonRequest {
    public static final int trainInitialPhase = 0;
    public static final int inferenceInitialPhase = -255;
    private ClientInfo client;
    private Message body;
    private int phase;
    private boolean isSync;  //是否采用同步请求，对于请求体小且速度快的请求可以使用

    public CommonRequest(ClientInfo client, Message body) {
        this.client = client;
        this.body = body;
        this.isSync = false;
    }

    public CommonRequest(ClientInfo client, Message body, int phase) {
        this.client = client;
        this.body = body;
        this.isSync = false;
        this.phase = phase;
    }

    public CommonRequest(ClientInfo client, Message body , int phase, boolean isSync) {
        this.client = client;
        this.body = body;
        this.phase = phase;
        this.isSync = isSync;
    }

    public static CommonRequest buildTrainInitial(ClientInfo client, Message body){
        return new CommonRequest(client,  body, trainInitialPhase);
    }

    public static CommonRequest buildInferenceInitial(ClientInfo client, Message body){
        return new CommonRequest(client, body, inferenceInitialPhase);
    }

    public ClientInfo getClient() {
        return client;
    }

    public void setClient(ClientInfo client) {
        this.client = client;
    }

    public Message getBody() {
        return body;
    }

    public void setBody(Message body) {
        this.body = body;
    }

    public boolean isSync() {
        return isSync;
    }

    public void setSync(boolean sync) {
        isSync = sync;
    }

    public int getPhase() {
        return phase;
    }

    public void setPhase(int phase) {
        this.phase = phase;
    }
}
