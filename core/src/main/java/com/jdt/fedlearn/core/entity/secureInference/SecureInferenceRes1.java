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

package com.jdt.fedlearn.core.entity.secureInference;

import com.jdt.fedlearn.core.entity.Message;

import java.math.BigInteger;

/**
 *
 * @author zhangwenxi
 */
public class SecureInferenceRes1 implements Message {
    private String[] nodePath;
    private String[][] otInitData;

    public SecureInferenceRes1() {
    }

    public SecureInferenceRes1(String[] nodePath) {
        this.nodePath = nodePath;
    }

    public SecureInferenceRes1(String[] nodePath, String[][] otInitData) {
        this.nodePath = nodePath;
        this.otInitData = otInitData;
    }

    public String[] getNodePath() {
        return nodePath;
    }

    public String[][] getOtInitData() {
        return otInitData;
    }
}
