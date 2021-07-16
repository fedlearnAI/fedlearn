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

import org.ujmp.core.DenseMatrix;
import org.ujmp.core.Matrix;

public class WeightedLinRegLossPriv extends WeightedLinRegBFGSSolver {
    protected Matrix delt_w;
    protected Matrix delt_g;
    public double fullDim;
    public int [] lowDim2HighDimMap; // 0,1 组成的array. 从低维map到高维时，按照顺序把低维数据放在对应值为1的高维数组中
    protected double [] g_old;

    // for training
    public WeightedLinRegLossPriv(double[][] x, double[] w, double[] y, double fullDim, int[] lowDim2HighDimMap,
                                  double stepLength, double lambda) {
        super(x, w, y, stepLength, lambda);
        this.fullDim = fullDim;
        this.lowDim2HighDimMap = lowDim2HighDimMap.clone();
        for(int elem: lowDim2HighDimMap) {
            if (elem != 1 && elem != 0) {
                throw new IllegalStateException("lowDim2HighDimMap contains elements other than 0 and 1.");
            }
        }
        delt_w = DenseMatrix.Factory.zeros(numDim, 1);
        delt_g = DenseMatrix.Factory.zeros(numDim, 1);

        assert(this.fullDim >= w.length);
    }

    // for inference
    public WeightedLinRegLossPriv(double[][] x, double[] w) {
        super(x, w);
    }

    // for training: parties with empty private data
    public WeightedLinRegLossPriv(boolean xIsEmpyty) {
        super(xIsEmpyty);
    }

    public void updateWFinal(double[] w_new) {
        if(!xIsEmpyty) {
            Matrix new_w = DenseMatrix.Factory.linkToArray(w_new);
            Matrix old_w = DenseMatrix.Factory.linkToArray(w);
            delt_w = new_w.minus(old_w);
            w = w_new.clone();
        }
    }

    public void lowDim2HighDim1DArr(double [] lowDimArray, double [] highDimArray) {
        if(!xIsEmpyty) {
            assert (highDimArray.length == lowDim2HighDimMap.length + 1);
            int iter = 0;
            int i;
            for (i = 0; i < highDimArray.length - 1; i++) {
                if (lowDim2HighDimMap[i] == 1) {
                    highDimArray[i] = lowDimArray[iter];
                    iter += 1;
                } else {
                    highDimArray[i] = 0d;
                }
            }
            assert (lowDimArray.length == iter + 1);
            highDimArray[i] = lowDimArray[lowDimArray.length - 1];  // 加上常数项
        }
    }

    public void highDim2LowDim_1dArr(double [] highDimArray, double [] lowDimArray) {
        if(!xIsEmpyty) {
            int iter = 0;
            int i;
            for (i = 0; i < lowDim2HighDimMap.length; i++) {
                if (lowDim2HighDimMap[i] == 1) {
                    lowDimArray[iter] = highDimArray[i];
                    iter += 1;
                }
            }
            assert (lowDimArray.length == iter + 1);
            lowDimArray[iter] = highDimArray[highDimArray.length - 1];  // 加上常数项
        }
    }

    public void updateDeltG() {
        if(!xIsEmpyty) {
            g_old = g.clone();
            Matrix old_g = DenseMatrix.Factory.linkToArray(g_old);
            g = computeG().clone();
            Matrix new_g = DenseMatrix.Factory.linkToArray(g);
            this.delt_g = new_g.minus(old_g);
        }
    }

    // ==================================== 以下的代码函数有误, 待修改 ====================================

    // 每次迭代结束时，master返回新的g w, 更新
    @Deprecated
    public void update_g_w(double[] g_new, double[] w_new) {
        if(!xIsEmpyty) {
            Matrix new_g = DenseMatrix.Factory.linkToArray(g_new);
            Matrix new_w = DenseMatrix.Factory.linkToArray(w_new);
            Matrix old_g = DenseMatrix.Factory.linkToArray(g);
            Matrix old_w = DenseMatrix.Factory.linkToArray(w);
            delt_g = new_g.minus(old_g);
            delt_w = new_w.minus(old_w);
            g = g_new.clone();
            w = w_new.clone();
        }
    }

    @Deprecated
    public void updateHessInv() {
        if(!xIsEmpyty) {
            this.computeG();
            Matrix s = delt_w;
            Matrix y = delt_g;
            Matrix H_Inv = DenseMatrix.Factory.linkToArray(HessInv);

            Matrix I = DenseMatrix.Factory.eye(numDim, numDim);
            Matrix y_transpose = y.transpose();
            Matrix s_transpose = s.transpose();
            double rho = 1.0 / (y_transpose.mtimes(s).getAsDouble(0, 0) + 1E-80);
            Matrix D_left = I.minus(s.mtimes(y_transpose).times(rho));
            Matrix D_right = I.minus(y.mtimes(s_transpose).times(rho));
            Matrix D_add = s.mtimes(s_transpose).times(rho);
            H_Inv = D_left.mtimes(H_Inv).mtimes(D_right).plus(D_add);

            utils.matrixToarray(H_Inv, HessInv);
            hessInv_ready = true;
        }
    }
}
