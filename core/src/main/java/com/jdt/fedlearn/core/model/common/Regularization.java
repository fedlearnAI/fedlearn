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

package com.jdt.fedlearn.core.model.common;

public class Regularization {


    public static double[] regularization(double[] weight, String regType, double lambda) {

        if ("L1".equals(regType.toUpperCase())) {
            return L1(weight, lambda);
        } else if ("L2".equals(regType.toUpperCase())) {
            return L2(weight, lambda);
        } else {
            return new double[weight.length];
        }
    }

    public static double[] L1(double[] weights, double lambda) {
        double[] L1 = new double[weights.length];
        for (int i = 0; i < weights.length; i++) {
            if (weights[i] >= 0) {
                L1[i] = lambda;
            } else {
                L1[i] = -lambda;
            }
        }
        return L1;
    }

    public static double[] L2(double[] weights, double lambda) {
        double[] L2 = new double[weights.length];
        for (int i = 0; i < weights.length; i++) {
            L2[i] = 2 * lambda * weights[i];
        }
        return L2;
    }

}
