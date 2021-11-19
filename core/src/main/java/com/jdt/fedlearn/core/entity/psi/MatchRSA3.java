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


import com.jdt.fedlearn.common.entity.core.ClientInfo;
import com.jdt.fedlearn.common.entity.core.Message;

/** RSA 仅支持两方进行对齐
 * @author zhangwenxi
 */
public class MatchRSA3 implements Message {

    private String[] signId;
    private String[] svId;
    private ClientInfo clientInfo;


    public MatchRSA3(String[] signId, String[] svId, ClientInfo clientInfo) {
        this.signId = signId;
        this.svId = svId;
        this.clientInfo = clientInfo;
    }

    public String[] getSignId() {
        return signId;
    }

    public String[] getSvId() {
        return svId;
    }
}
