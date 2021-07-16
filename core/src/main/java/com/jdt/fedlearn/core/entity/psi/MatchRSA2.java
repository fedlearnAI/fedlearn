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

package com.jdt.fedlearn.core.entity.psi;


import com.jdt.fedlearn.core.entity.ClientInfo;
import com.jdt.fedlearn.core.entity.Message;


/**
 * @author zhangwenxi
 */
public class MatchRSA2 implements Message {

    /**
     * RSA server ids after two hash
     */
    private String[] encId;
    private ClientInfo clientInfo = new ClientInfo();

    public MatchRSA2(String[] encId) {
        this.encId = encId;
    }

    public ClientInfo getClientInfo() {
        return clientInfo;
    }

    public String[] getEncId() {
        return encId;
    }

    public void setClientInfo(ClientInfo clientInfo) {
        this.clientInfo = clientInfo;
    }
}
