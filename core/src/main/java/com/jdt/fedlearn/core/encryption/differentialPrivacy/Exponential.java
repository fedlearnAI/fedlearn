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

package com.jdt.fedlearn.core.encryption.differentialPrivacy;

import com.jdt.fedlearn.core.math.MathExt;

public class Exponential {
    /**
     * 指数机制，对于非连续值，或者在多个值选择时使用，用类似Softmax的方法输出概率，并根据概率选取相应的值
     * @param value 输入数值
     * @param deltaF 全局敏感度
     * @param protectLevel 代表了提供的差分隐私保护级别，为零时不提供差分隐私保护
     * @return
     */
    public static int exponentialMechanismIndex(double[] value, double deltaF, double protectLevel) {
        double max = -Double.MAX_VALUE;
        int index = MathExt.maxIndex(value);
        if (protectLevel <= 1e-10) {
            return index;
        }
        double epsilon = 1 / protectLevel;
        max = max * epsilon / (2 * deltaF + 1e-5);
        //防止double溢出
        for (int i = 0; i < value.length; i++) {
            value[i] = value[i] * epsilon / (2 * deltaF + 1e-5);
            value[i] = value[i] - max;
            value[i] = Math.exp(value[i]);
        }

        //根据概率随机采样
        double[] weights = new double[value.length];
        weights[0] = value[0];
        for (int i = 1; i < weights.length; i++) {
            weights[i] = value[i] + weights[i - 1];
        }
        double choice = Math.random() * weights[weights.length - 1];
        for (int i = 0; i < weights.length; i++) {
            if (choice < weights[i]) {
                index = i;
                break;
            }
            index = i;
        }
        return index;
    }
}
