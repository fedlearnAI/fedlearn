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


import com.jdt.fedlearn.core.entity.Message;

import java.math.BigInteger;

public class DhMatchReq1 implements Message {
    private String[] cipherUid;
    private BigInteger g;
    private BigInteger n;
    private BigInteger random;

    public DhMatchReq1(String[] cipherUid, BigInteger g, BigInteger n) {
        this.cipherUid = cipherUid;
        this.g = g;
        this.n = n;
    }

    public DhMatchReq1(String[] cipherUid, BigInteger g, BigInteger n, BigInteger random) {
        this.cipherUid = cipherUid;
        this.g = g;
        this.n = n;
        this.random = random;
    }

    public BigInteger getRandom() {
        return random;
    }

    public String[] getCipherUid() {
        return cipherUid;
    }

    public BigInteger getG() {
        return g;
    }

    public BigInteger getN() {
        return n;
    }

}
