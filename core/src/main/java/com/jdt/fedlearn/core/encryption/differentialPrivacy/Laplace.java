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

import org.apache.commons.math3.distribution.LaplaceDistribution;

public class Laplace {
    /**
     * 拉普拉斯机制，通过生成基于拉普拉斯分布的噪声来提供差分隐私保护
     * @param deltaF 参数为全局敏感度，代表了邻近数据集在函数F下的最大差值
     * @param level epsilon的倒数，代表了提供的差分隐私保护级别，为零时不提供差分隐私保护
     * @return
     */
    public static double laplaceMechanismNoise(double deltaF, double level) {
        if (level <= 1e-10) {
            return 0;
        }
        double epsilon = 1 / level;
        LaplaceDistribution ld = new LaplaceDistribution(0, Math.abs(deltaF) / epsilon);
        return ld.sample();
    }

}
