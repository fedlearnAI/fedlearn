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

public class Random {
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
}
