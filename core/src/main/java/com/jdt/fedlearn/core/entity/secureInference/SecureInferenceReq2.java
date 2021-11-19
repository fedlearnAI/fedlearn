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

import com.jdt.fedlearn.common.entity.core.Message;

/**
 * @author zhangwenxi
 */
public class SecureInferenceReq2 implements Message {
    private int[] leaves;
    private String[][] cipher0;
    private String[][] cipher1;
    private byte[][] wArray;

    public SecureInferenceReq2() {
    }

    public SecureInferenceReq2(int[] leaves) {
        this.leaves = leaves;
    }

    public SecureInferenceReq2(int[] leaves, String[][] cipher0, String[][] cipher1) {
        this.leaves = leaves;
        this.cipher0 = cipher0;
        this.cipher1 = cipher1;
    }

    public SecureInferenceReq2(String[][] cipher0, String[][] cipher1, byte[][] wArray) {
        this.cipher0 = cipher0;
        this.cipher1 = cipher1;
        this.wArray = wArray;
    }

    public int[] getLeaves() {
        return leaves;
    }

    public void setLeaves(int[] leaves) {
        this.leaves = leaves;
    }

    public String[][] getCipher0() {
        return cipher0;
    }

    public byte[][] getwArray() {
        return wArray;
    }

    public void setwArray(byte[][] wArray) {
        this.wArray = wArray;
    }

    public String[][] getCipher1() {
        return cipher1;
    }
}
