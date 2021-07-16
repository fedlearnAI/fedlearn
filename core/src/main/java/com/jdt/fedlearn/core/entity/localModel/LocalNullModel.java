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

package com.jdt.fedlearn.core.entity.localModel;

import com.jdt.fedlearn.core.model.common.loss.SquareLoss;
import com.jdt.fedlearn.core.entity.randomForest.DataUtils;
import org.ejml.simple.SimpleMatrix;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.IntStream;

public class LocalNullModel implements LocalModel {

    /**
     * Local null model
     */

    // column vector
    SimpleMatrix beta;
    // scalar
    double alpha;
    // ridge regression parameter
    double lambda;
    // 线性模型默认拟合MSE
    SquareLoss loss = new SquareLoss();

    // 构造函数
    public LocalNullModel() {
        this.lambda = 0;
    }

    @Override
    public String getModelType() {
        return "Null";
    }

    @Override
    public void train(SimpleMatrix X, SimpleMatrix y) {
        beta = new SimpleMatrix(X.numCols(), 1);
        alpha = 0;
    }

    @Override
    public double predict(SimpleMatrix X) {
        return 0;
    }

    @Override
    public double[] batchPredict(SimpleMatrix X) {
        double[] res = new double[X.numRows()];
        Arrays.fill(res, 0);
        return res;
    }

    @Override
    public double[] getResidual(SimpleMatrix X, SimpleMatrix y) {
        double[] pred = batchPredict(X);
        return IntStream.range(0, pred.length).mapToDouble(i -> pred[i] - y.get(i)).toArray();
    }

    @Override
    public double[] getGradient(SimpleMatrix X, SimpleMatrix y) {
        double[] pred = batchPredict(X);
        return loss.grad(pred, DataUtils.toArray(y));
    }

    @Override
    public double[] getHessian(SimpleMatrix X, SimpleMatrix y) {
        double[] pred = batchPredict(X);
        return loss.hess(pred, DataUtils.toArray(y));
    }

    @Override
    public double[] getPseudoLabel(SimpleMatrix X, SimpleMatrix y) {
        double[] gradient = getGradient(X, y);
        double[] hessian = getHessian(X, y);
        // formula: (sqrt(h) * f - (-g) / sqrt(h))^2
        // MSE loss 的话 hessian 是 1
        return IntStream.range(0, gradient.length).mapToDouble(i -> -gradient[i] / Math.sqrt(hessian[i])).toArray();
    }


    @Override
    public String serialize() {
        Map<String, String> map = new HashMap<>();
        if (beta.numRows() == 0) {
            map.put("beta", "null");
        } else {
            map.put("beta", DataUtils.doubleArrayToString(DataUtils.toArray(beta)));
        }
        map.put("alpha", String.valueOf(alpha));
        return DataUtils.mapToString(map);
    }

    @Override
    public LocalNullModel deserialize(String s) {
        Map<String, String> map = DataUtils.stringToMap(s, true, true);
        if (!("null".equals(map.get("beta")))) {
            double[] tmp = DataUtils.stringToDoubleArray(map.get("beta"));
            beta = new SimpleMatrix(tmp.length, 1);
            for (int i = 0; i < beta.numRows(); i++) {
                beta.set(i, 0, tmp[i]);
            }
        } else {
            beta = new SimpleMatrix(0, 1);
        }
        alpha = Double.parseDouble(map.get("alpha"));
        return this;
    }


}
