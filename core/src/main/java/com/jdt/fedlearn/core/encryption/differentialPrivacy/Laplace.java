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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Laplace {

    private static final Logger logger = LoggerFactory.getLogger(Laplace.class);

    private final static double MIN_LEVEL = 1e-10;
    private final static double MIN_EPSILON = 1e-5;
    /**
     * 拉普拉斯机制，通过生成基于拉普拉斯分布的噪声来提供差分隐私保护
     * @param deltaF 参数为全局敏感度，代表了邻近数据集在函数F下的最大差值
     * @param level epsilon的倒数，代表了提供的差分隐私保护级别，为零时不提供差分隐私保护
     * @return
     */
    public static double laplaceMechanismNoise(double deltaF, double level) {
        if (level <= MIN_LEVEL) {
            return 0;
        }
        double epsilon = 1 / level;
        LaplaceDistribution ld = new LaplaceDistribution(0, Math.abs(deltaF) / epsilon);
        return ld.sample();
    }

    public static double laplaceMechanismNoiseV1(double deltaF, double epsilon){
        if(epsilon < MIN_EPSILON){
            logger.error("The epsilon is too small. It will generate large noise");
            epsilon = MIN_EPSILON;
        }
        if(deltaF <= 0){
            logger.error("The delta must be positive. 0 is returned as noise");
            return 0;
        }
        LaplaceDistribution ld = new LaplaceDistribution(0, Math.abs(deltaF) / epsilon);
        return ld.sample();
    }

}
