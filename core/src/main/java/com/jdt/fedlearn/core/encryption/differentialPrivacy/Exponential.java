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
import org.apache.commons.math3.distribution.ExponentialDistribution;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Exponential {

    private static final Logger logger = LoggerFactory.getLogger(Exponential.class);

    /**
     * 指数机制，对于非连续值，或者在多个值选择时使用，用类似Softmax的方法输出概率，并根据概率选取相应的值
     * @param value 输入数值
     * @param deltaF 全局敏感度
     * @param epsilon 代表了提供的差分隐私保护预算
     * @return
     */
    public static int exponentialMechanismIndex(double[] value, double deltaF, double epsilon) {
        if(value == null || value.length == 0){
            return -1;
        }
        double[] expValues = new double[value.length];
        for (int i = 0; i < value.length; i++) {
            expValues[i] = Math.exp(value[i] * epsilon / (2 * deltaF));
            if(Double.isInfinite(expValues[i]) || Double.isNaN(expValues[i])){
                logger.error("The exp value is overflow. Just return the max value");
                return MathExt.maxIndex(value);
            }
        }
        //根据概率随机采样
        double[] weights = new double[value.length];
        weights[0] = expValues[0];
        for (int i = 1; i < weights.length; i++) {
            weights[i] = expValues[i] + weights[i - 1];
        }
        int index = 0;
        double choice = Math.random() * weights[weights.length - 1];
        for (int i = 0; i < weights.length; i++) {
            index = i;
            if (choice < weights[i]) {
                break;
            }
        }
        return index;
    }

    /**
     * 获取指数分布的噪声向量
     * @param shape 噪声向量的size
     * @param lambda 指数分布的均值
     * @return noises
     */
    public static double[] getExponentialMechanismNoise(int shape, double lambda){
        double[] noises = new double[shape];
        ExponentialDistribution eD = new ExponentialDistribution(lambda);
        for(int i = 0; i < shape; i++){
            noises[i] = eD.sample();
        }
        return noises;
    }
}
