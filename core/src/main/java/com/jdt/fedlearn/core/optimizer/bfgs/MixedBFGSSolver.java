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

public abstract class MixedBFGSSolver {

    protected double[] w;
    protected final double[][] x;   // x.shape = [instance数量][维度]
    protected final double[] y;
    protected double [] y_hat;
    protected double [][] HessInv;
    protected double [] g;
    protected final int numInstance;
    protected final int numDim;
    protected double loss;
    protected double lambda;

    // BFGS 方法变量
    protected double stepLength;

    //每次更新w时候更新此参数(置为false)，防止重复/漏算
    protected boolean y_hat_ready;
    protected boolean hessInv_ready;
    protected boolean g_ready;

    // 处理X为空时的情形，用于空的 private data
    protected final boolean xIsEmpyty;


    MixedBFGSSolver(boolean xIsEmpyty) {
        this.xIsEmpyty = true;
        this.numDim = 0;
        this.numInstance = 0;
        this.x = null;
        this.y = null;
        this.y_hat = null;
    }
    // 用于训练
    MixedBFGSSolver(double[][] x, double[] w, double[] y, double stepLength, double lambda) {
        assert (x.length > 0 && x[0].length > 0 && w.length > 0
                && x.length == y.length && x[0].length == w.length);

        this.xIsEmpyty = false;
        this.w = w.clone();
        this.x = x;
        this.y = y;
        this.numInstance = x.length;
        this.numDim = this.w.length;
        this.loss = Double.NaN;
        this.y_hat_ready = false;
        this.stepLength = stepLength;
        this.lambda = lambda;

        // TODO: 初始化 HessInv，初始化的方法可能需要更改
        this.HessInv = DenseMatrix.Factory.eye(numDim, numDim).toDoubleArray();
        this.hessInv_ready = true;

        // TODO: 初始化 g=0，初始化的方法可能需要更改
        this.g = new double[numDim];
        this.g_ready = true;
    }

    // 用于预测
    MixedBFGSSolver(double[][] x, double[] w) {
        assert (x.length > 0 && x[0].length > 0 && w.length > 0
                && x[0].length == w.length);

        this.xIsEmpyty = false;
        this.w = w.clone();
        this.x = x;
        this.numInstance = x.length;
        this.numDim = w.length;
        this.loss = Double.NaN;
        this.y_hat_ready = false;
        this.y = null;
    }

    public double getLoss() {
        return loss;
    }
    public double[] getG() {
        return g;
    }
    public double[] getW() {
        return w;
    }

    public double[] getYHat() {
        if(!y_hat_ready) {
            forward();
        }
        return y_hat;
    }

    public abstract void forward();

    public abstract double updateLoss();

    public abstract double[] computeG();

    public double[] getY() {
        return y;
    }

    public double[][] getX() {
        return x;
    }

    public void updateW(double[] w_new) {
        assert w_new.length == this.w.length;
        this.w = w_new.clone();
    }

    public double getAlpha() {
        return w[w.length-1];
    }
}


class utils {
    public static double _MSE(double [] y_hat, double[] y) {
        return _SE(y_hat, y)/y.length;
    }

    public static double _SE(double [] y_hat, double[] y) {
        assert (y_hat.length == y.length);
        assert (y_hat.length != 0);
        double se = 0d;
        for(int i = 0; i < y.length; i++) {
            double t =  y_hat[i] - y[i];
            se += t * t;
        }
        return se;
    }

    public static void matrixToarray(Matrix mat, double[][] arr){
        for (int i = 0;i < arr.length;i++){
            for (int j = 0;j < arr[i].length;j++){
                arr[i][j] = mat.getAsDouble(i, j);
            }
        }
    }

    public static void matrixToarray(Matrix mat, double[] arr){
        for (int i = 0;i < arr.length;i++){
            arr[i] = mat.getAsDouble(i, 0);
        }
    }

}
