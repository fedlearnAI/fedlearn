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

import java.util.Arrays;
import java.util.concurrent.ThreadLocalRandom;
import org.apache.commons.math3.distribution.BinomialDistribution;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class RandomDP {

    private static final Logger logger = LoggerFactory.getLogger(RandomDP.class);

    private final static double MIN_EPSILON = 1e-3;

    //随机响应机制
    //针对FederatedGB中的分桶操作，将排好序的数组一一定的概率进行轻微的打乱，使得有些数据被随机的分到别的桶之中
    public static double[][] randomizedResponseSoft(double probability, double[][] data) {
        double[][] res = data.clone();
        for (int i = 0; i < res.length; i++) {
            if (Math.random() < probability) {
                int randomIndex = (int) (Math.random() * res.length);
                double[] tmp = res[i];
                res[i] = res[randomIndex];
                res[randomIndex] = tmp;
            }
        }
        return res;
    }

    /**
     * 论文 https://arxiv.org/pdf/1606.05053.pdf 算法二的实现
     * @param data 需要扰动的数值型数据，定义域在[-1, 1]
     * @param epsilon 隐私预算
     * @return res
     */
    public static double[][] randomizedResponseForNumeric(double[][] data, double epsilon){
        if(data == null || data.length == 0 || data[0].length == 0){
            return new double[0][0];
        }
        double[][] res = new double[data.length][data[0].length];
        for(double[] v: res){
            Arrays.fill(v, 0);
        }
        if(epsilon < MIN_EPSILON){
            logger.error("The epsilon is too small. It will generate large noise");
            epsilon = MIN_EPSILON;
        }
        double expEpsilon = Math.exp(epsilon);
        int d = res[0].length;
        double candidateValue = (expEpsilon + 1) / (expEpsilon - 1) * d;
        for(int i = 0; i < res.length; i++){
            int j = ThreadLocalRandom.current().nextInt(d);
            double p = (data[i][j] * (expEpsilon - 1) + expEpsilon + 1) / (2 * expEpsilon + 2);
            BinomialDistribution binomialDistribution = new BinomialDistribution(1, p);
            int u = binomialDistribution.sample();
            if(u == 1){
                res[i][j] = candidateValue;
            }else{
                res[i][j] = -candidateValue;
            }
        }
        return res;
    }


}
