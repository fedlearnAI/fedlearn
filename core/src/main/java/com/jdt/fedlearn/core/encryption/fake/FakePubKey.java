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

package com.jdt.fedlearn.core.encryption.fake;


import com.jdt.fedlearn.core.encryption.common.PublicKey;

public class FakePubKey implements PublicKey {
    private final int pubKey;

    public FakePubKey(String pubKey) {
        this.pubKey = Integer.parseInt(pubKey);
    }

    public FakePubKey(int pubKey) {
        this.pubKey = pubKey;
    }

    @Override
    public String serialize() {
        return String.valueOf(this.pubKey);
    }

}
