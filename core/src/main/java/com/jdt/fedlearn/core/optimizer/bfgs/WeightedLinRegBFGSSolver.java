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

package com.jdt.fedlearn.core.optimizer.bfgs;

import com.jdt.fedlearn.core.math.MathExt;

public class WeightedLinRegBFGSSolver extends MixedBFGSSolver {

    /**
     * Function f(x,w) = w^T x
     * Loss(f, t) = MSE( f(x,w), t)
     */
    public WeightedLinRegBFGSSolver(double[][] x, double[] w, double[] y,
                                    double stepLength, double lambda) {
        super(x, w, y, stepLength, lambda);
    }
    // 对于 private 数据为空
    public WeightedLinRegBFGSSolver(boolean xIsEmpyty) {
        super(xIsEmpyty);
    }

    public WeightedLinRegBFGSSolver(double[][] x, double[] w) {
        super(x, w);
    }

    @Override
    public void forward() {
        if(!xIsEmpyty) {
            y_hat = new double[numInstance];
            for (int i = 0; i < numInstance; i++) {
                y_hat[i] = MathExt.dotMultiply(w, x[i]);  // alpha 是每个party的权重
            }
            y_hat_ready = true;
        }
    }

    @Override
    public double updateLoss() {
        if(!xIsEmpyty) {
            if (y_hat_ready) {
                loss = utils._SE(y_hat, y);
            } else {
                this.forward();
                loss = utils._SE(this.getYHat(), y);
            }
            return loss;
        } else {
            return 0d;
        }
    }

    /**
     * loss(w, x) 对 w 的导数
     * Loss = MSE(y-y_hat) + lambda * ||W||^2
     * @return loss 对 w_i 的导数值
     */
    @Override
    public double[] computeG() {
        if(!xIsEmpyty) {
            this.forward();
            double[] sum_d = new double[numDim];
            for (int i = 0; i < numInstance; i++) {
                sum_d = MathExt.add(
                        sum_d,
                        MathExt.dotMultiply(x[i], 2 * (y_hat[i] - y[i]))
                );
            }
            return MathExt.add(
                    MathExt.dotMultiply(sum_d, 1d / numInstance),
                    MathExt.dotMultiply(w, lambda)
            );
        } else {
            return new double[numDim];
        }
    }
}
