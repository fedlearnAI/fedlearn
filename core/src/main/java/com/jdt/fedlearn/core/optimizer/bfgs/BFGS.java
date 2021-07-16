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

/**
 * Performing BFGS on Master
 */
public class BFGS {

    private double[] g;
    private double[] g_old;
    private double[] w;
    private final double[][] H_Inv;
    private Matrix delt_w;
    private Matrix delt_g;
    private final int numDim;
    private final double stepLength;

    public BFGS(double[] w, double stepLength, int numDim) {
        this.w = w.clone();
        this.g = new double[numDim]; // 各方 non_priv data 算出的总的 g
        this.g_old = new double[numDim];
        this.H_Inv = DenseMatrix.Factory.eye(numDim, numDim).toDoubleArray();
        this.delt_g = DenseMatrix.Factory.zeros(numDim, 1);
        this.delt_w = DenseMatrix.Factory.zeros(numDim, 1);
        this.numDim = numDim;
        this.stepLength = stepLength;
    }

    public void update_g_glob(double[] g_new) {
        this.g_old = g.clone();
        this.g = g_new.clone();
    }

    public double[][] update_HessInv() {
        Matrix s = delt_w;
        Matrix y = delt_g;

        Matrix H_Inv = Matrix.Factory.linkToArray(this.H_Inv);
        Matrix I = DenseMatrix.Factory.eye(numDim, numDim);
        Matrix y_transpose = y.transpose();
        Matrix s_transpose = s.transpose();
        double rho = 1.0 / (y_transpose.mtimes(s).getAsDouble(0, 0) + 1E-80);
        Matrix D_left = I.minus(s.mtimes(y_transpose).times(rho));
        Matrix D_right = I.minus(y.mtimes(s_transpose).times(rho));
        Matrix D_add = s.mtimes(s_transpose).times(rho);
        H_Inv = D_left.mtimes(H_Inv).mtimes(D_right).plus(D_add);

        utils.matrixToarray(H_Inv, this.H_Inv);
        return this.H_Inv;
    }

    public void updateFinalW() {
        _updateWRet w_delt_w = _updateW(Matrix.Factory.linkToArray(H_Inv),
                Matrix.Factory.linkToArray(g),
                Matrix.Factory.linkToArray(w),
                w,
                stepLength);
        delt_w = w_delt_w.delt_w;
        w = w_delt_w.new_w;
        Matrix g_new = Matrix.Factory.linkToArray(g);
        Matrix g_old = Matrix.Factory.linkToArray(this.g_old);
        delt_g = g_new.minus(g_old);
    }

    private _updateWRet _updateW(Matrix H_last, Matrix g_mat, Matrix w_mat, double[] new_W, double stepLen) {
        // 计算全局 W 的增量
        Matrix delt_w = H_last.mtimes(g_mat).times(-1 * stepLen);

        // TODO: only for debugging
//        delt_w =  DenseMatrix.Factory.eye(numDim, numDim).mtimes(g_mat).times(-1 * stepLength);
        w_mat = w_mat.plus(delt_w);
        utils.matrixToarray(w_mat, new_W);
        return new BFGS._updateWRet(new_W, delt_w);
    }

    private static class _updateWRet {
        double[] new_w;
        Matrix delt_w;

        _updateWRet(double[] new_w, Matrix delt_w) {
            this.new_w = new_w;
            this.delt_w = delt_w;
        }
    }

    public double[] getFinalW() {
        return w;
    }

    public double[] get_delt_w_final() {
        double[] ret = new double[numDim];
        utils.matrixToarray(delt_w, ret);
        return ret;
    }

    public double[] get_delt_w_glob() {
        double[] ret = new double[numDim];
        utils.matrixToarray(delt_w, ret);
        return ret;
    }
}
