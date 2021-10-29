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

package com.jdt.fedlearn.core.preprocess;

import com.jdt.fedlearn.core.math.MathExt;

import java.io.Serializable;
import java.util.stream.IntStream;

/**
 * feature scaling
 */
public class Scaling implements Serializable {
    private double[][] table;
    //每列一个scale
    private double[] scales;
    private double[] X_min;
    private double[] X_max;

    // label的scale
    private double yMin = 0;
    private double yMax = 1;
    private double yScale = 1;

    public Scaling() {

    }

    public Scaling(double[][] table) {
        this.table = table;
        this.scales = new double[table[0].length];
        X_max = new double[table[0].length];
        X_min = new double[table[0].length];
    }

    /*
    The transformation is calculated as::

        X_scaled = scale * X + min - X.min(axis=0) * scale
        where scale = (max - min) / (X.max(axis=0) - X.min(axis=0))

    This transformation is often used as an alternative to zero mean,
    unit variance scaling.
     */
    public void minMaxScaling(double min, double max) {
        this.scales = new double[table[0].length];
        this.X_max = new double[table[0].length];
        this.X_min = new double[table[0].length];
        double[][] trans = MathExt.transpose(table);
        for (int i = 0; i < trans.length; i++) {
            double[] column = trans[i];
            double x_max = MathExt.max(column);
            double x_min = MathExt.min(column);
            double scale = 1;
            if (x_max != x_min) {
                scale = (max - min) / (x_max - x_min);
            }
            X_max[i] = x_max;
            X_min[i] = x_min;
            scales[i] = scale;
        }
        for (int i = 0; i < table.length; i++) {
            for (int j = 0; j < table[0].length; j++) {
                double tmp = table[i][j];
                table[i][j] = scales[j] * (tmp - X_min[j]) + min;
            }
        }
    }

    public void minMaxScaling(double min, double max, double[][] table) {
        this.scales = new double[table[0].length];
        this.X_max = new double[table[0].length];
        this.X_min = new double[table[0].length];
        double[][] trans = MathExt.transpose(table);
        for (int i = 0; i < trans.length; i++) {
            double[] column = trans[i];
            double x_max = MathExt.max(column);
            double x_min = MathExt.min(column);
            double scale = 1;
            if (x_max != x_min) {
                scale = (max - min) / (x_max - x_min);
            }
            X_max[i] = x_max;
            X_min[i] = x_min;
            scales[i] = scale;
        }
        for (int i = 0; i < table.length; i++) {
            for (int j = 0; j < table[0].length; j++) {
                double tmp = table[i][j];
                table[i][j] = scales[j] * (tmp - X_min[j]) + min;
            }
        }
    }

    // 放缩标签值，针对与使用差分隐私的回归任务
    public void minMaxScalingLabel(double min, double max, double[] label){
        yMax = MathExt.max(label);
        yMin = MathExt.min(label);
        if(yMax != yMin){
            yScale = (max - min) / (yMax - yMin);
        }
        IntStream.range(0, label.length).parallel().forEach(idx -> {
            double tmp = label[idx];
            label[idx] = yScale * (tmp - yMin) + min;
        });
    }

    public void inferenceMinMaxScaling(double[][] table) {
        for (int i = 0; i < table.length; i++) {
            for (int j = 0; j < table[0].length; j++) {
                double tmp = table[i][j];
                table[i][j] = scales[j] * (tmp - X_min[j]);
            }
        }
    }

    public double inferenceMinMaxYScaling(double y){
        return y / yScale + yMin;
    }

    /*
      """Standardize features by removing the mean and scaling to unit variance

    The standard score of a sample `x` is calculated as:

        z = (x - u) / s

    where `u` is the mean of the training samples or zero if `with_mean=False`,
    and `s` is the standard deviation of the training samples or one if
    `with_std=False`.
     */
    public void stdScaling() {

    }

    public double[] getScales() {
        return scales;
    }

    public double[] getX_min() {
        return X_min;
    }

    public double[] getX_max() {
        return X_max;
    }

    public void setScales(double[] scales) {
        this.scales = scales;
    }

    public void setX_min(double[] x_min) {
        X_min = x_min;
    }

    public void setX_max(double[] x_max) {
        X_max = x_max;
    }

    public double getYScale(){
        return this.yScale;
    }

    public double getYMin(){
        return this.yMin;
    }

    public double getYMax(){
        return this.yMax;
    }
}

