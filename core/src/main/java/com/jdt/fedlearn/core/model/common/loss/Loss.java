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


import com.jdt.fedlearn.core.exception.NotImplementedException;

import java.io.Serializable;

public class Loss implements Serializable {

    public double[] grad(double[] pred, double[] label) {
        throw new NotImplementedException();
    }

    public double[] hess(double[] pred, double[] label) {
        throw new NotImplementedException();
    }

    public double[] transform(double[] pred) {
        throw new NotImplementedException();
    }

    public double[] postTransform(double[] pred) {
        throw new NotImplementedException();
    }

    public double transform(double pred) {
        throw new NotImplementedException();
    }

    public double[] logTransform(double[] pred) {
        throw new NotImplementedException();
    }

    public double[] expTransform(double[] pred) {
        throw new NotImplementedException();
    }

    public static double crossEntropy() {
        return 0;
    }


    public static double mseLoss(double[] predict, double[] label) {
        double loss = 0;
        for (int i = 0; i < label.length; i++) {
            double error = predict[i] - label[i];
            loss += Math.pow(error, 2);
        }
        loss = loss / label.length;
        return loss;
    }

    public static double logLoss(double[] predict, double[] label) {
        double loss = 0;
        for (int i = 0; i < label.length; i++) {
            loss += label[i] * Math.log(predict[i]) - (1 - label[i]) * Math.log(1 - predict[i]);
        }
        loss = -loss / label.length;
        return loss;
    }

    public static double sigmoid(double z) {
        return 1 / (1 + Math.pow(Math.E, -z));
    }

}
