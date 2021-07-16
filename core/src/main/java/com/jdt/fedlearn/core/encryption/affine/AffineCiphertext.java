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
package com.jdt.fedlearn.core.encryption.affine;

import com.jdt.fedlearn.core.encryption.common.Ciphertext;
import com.jdt.fedlearn.core.exception.NotMatchException;

import java.math.BigInteger;

/**
 * AffineCiphertext
 */
public class AffineCiphertext implements Ciphertext {
    //y = a * x + b, times is a, b = bias * large number
    public BigInteger times;
    public BigInteger bias;
    private final int scale;

    public AffineCiphertext(BigInteger times, BigInteger bias, int scale) {
        this.times = times;
        this.bias = bias;
        this.scale = scale;
    }

    public AffineCiphertext(BigInteger times, int bias, int scale) {
        this.times = times;
        this.bias = BigInteger.valueOf(bias);
        this.scale = scale;
    }

    public BigInteger getTimes() {
        return times;
    }

    public BigInteger getBias() {
        return bias;
    }

    public int getScale() {
        return scale;
    }

    @Override
    public String serialize() {
        return times.toString() + ":" + bias.toString() + ":" + scale;
    }
}
