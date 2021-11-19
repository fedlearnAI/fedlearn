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

import java.math.BigInteger;
import java.util.Map;

public class DhMatchReq2 implements Message {
    private Map<ClientInfo, String[]> doubleCipherUid;
    private Map<ClientInfo, String[]> cipherUid;
    private BigInteger g;
    private BigInteger n;

    public DhMatchReq2(Map<ClientInfo, String[]> doubleCipherUid, Map<ClientInfo, String[]> cipherUid, BigInteger g, BigInteger n) {
        this.cipherUid = cipherUid;
        this.doubleCipherUid = doubleCipherUid;
        this.g = g;
        this.n = n;
    }

    public Map<ClientInfo, String[]> getCipherUid() {
        return cipherUid;
    }

    public Map<ClientInfo, String[]> getDoubleCipherUid() {
        return doubleCipherUid;
    }

    public BigInteger getG() {
        return g;
    }

    public BigInteger getN() {
        return n;
    }

}
