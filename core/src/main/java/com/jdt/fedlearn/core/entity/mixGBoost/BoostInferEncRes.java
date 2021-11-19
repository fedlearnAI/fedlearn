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

package com.jdt.fedlearn.core.entity.mixGBoost;

import com.jdt.fedlearn.core.encryption.distributedPaillier.DistributedPaillierNative;
import com.jdt.fedlearn.common.entity.core.Message;


/**
 * @author zhangwenxi
 */
public class BoostInferEncRes implements Message {
    private final DistributedPaillierNative.signedByteArray[] body;
    private String pkStr;

    public BoostInferEncRes () {
        body = new DistributedPaillierNative.signedByteArray[0];
    }

    public BoostInferEncRes (DistributedPaillierNative.signedByteArray[] in, String pkStr) {
        this.body = in;
        this.pkStr = pkStr;
    }

    public DistributedPaillierNative.signedByteArray[] getBody() {
        return body;
    }

    public Boolean isEmpty(){
        return body.length == 0;
    }

    public String getPkStr() {
        return pkStr;
    }
}
