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

package com.jdt.fedlearn.core.model.common.loss;

import java.util.Arrays;
import static com.jdt.fedlearn.core.metrics.Metric.mean_square_error;

/**
 * MSE Loss function 类：
 * 用于MSE Loss相关的计算: 0.5 * (pred - label) ^ 2
 * 包含损失函数 residual 的一阶展开系数和二阶展开系数
 */

public class SquareLoss extends Loss {
    public double[] transform(double[] pred) {
        return pred;
    }

//    public double transform(double pred){
//        return pred;
//    }

    // getLoss 方法，计算预测值与真实值的 mean squared error
    public double getLoss(double[] pred, double[] label) {
        // mean square error 函数没有 1/2，在这里最后要除一个 2
        return mean_square_error(pred, label) / 2;
    }

    public double[] logTransform(double[] pred) {
        double[] logValue = new double[pred.length];
        for (int i = 0; i < pred.length; i++) {
            logValue[i] = pred[i] > 0 ? Math.log(pred[i]) : Math.log(1);
        }
        return logValue;
    }

    public double[] expTransform(double[] pred) {
        double[] expValue = new double[pred.length];
        for (int i = 0; i < pred.length; i++) {
            expValue[i] = Math.exp(pred[i]);
        }
        return expValue;
    }

    public double[] grad(double[] pred, double[] label) {
        double[] ret = new double[pred.length];
        for (int i = 0; i < ret.length; i++) {
            ret[i] = pred[i] - label[i];
        }
        return ret;
    }

    public double[] hess(double[] pred, double[] label) {
        double[] ret = new double[pred.length];
        Arrays.fill(ret, 1.0);
        return ret;
    }
}
