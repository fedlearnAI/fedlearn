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
import org.ejml.data.SingularMatrixException;
import org.ejml.equation.Equation;
import org.ejml.simple.SimpleMatrix;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.IntStream;

public class LocalLinearModel implements LocalModel {

    /**
     * Local linear model: Y = X * beta + alpha
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
    public LocalLinearModel() {
        this.lambda = 1e-6;
    }

    public LocalLinearModel(double lambda) {
        this.lambda = lambda;
    }

    @Override
    public String getModelType() {
        return "LinearModel";
    }

    @Override
    public void train(SimpleMatrix X, SimpleMatrix y) {
        // compute (X.T * X + lambda * I) \ (X.T * y)
        SimpleMatrix y1 = y;
        if (y1.numRows() == 1) {
            y1 = y1.transpose();
        }
        Equation eq = new Equation();
        int numRows = X.numRows();
        eq.alias(X, "X", numRows, "n");
        eq.process("X1 = [X,ones(n,1)]");
        SimpleMatrix X1 = new SimpleMatrix(eq.lookupSimple("X1"));
        SimpleMatrix eye = SimpleMatrix.identity(X.numCols() + 1);
        SimpleMatrix X2 = X1.transpose().mult(X1).plus(eye.scale(lambda));
        try {
            SimpleMatrix res = X2.solve(X1.transpose().mult(y1));
            // set beta
            res.print();
            beta = res.extractMatrix(0, res.numRows() - 1, 0, 1);
            alpha = res.get(res.numRows() - 1, 0);
        } catch (SingularMatrixException e) {
            throw new IllegalArgumentException("Singular matrix");
        }
    }

    @Override
    public double predict(SimpleMatrix X) {
        return X.dot(beta) + alpha;
    }

    @Override
    public double[] batchPredict(SimpleMatrix X) {
        if (X.numCols() == beta.numRows()) {
            // X * beta
            return doBatchPredict(X);
        } else if (X.numRows() == beta.numRows()) {
            // X.T * beta
            return doBatchPredict(X.transpose());
        } else {
            throw new IllegalArgumentException("Matrix dimensions of X and beta are not compatible");
        }
    }

    private double[] doBatchPredict(SimpleMatrix X) {
        SimpleMatrix y = X.mult(beta).plus(alpha);
        return DataUtils.toArray(y);
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
        map.put("beta", DataUtils.doubleArrayToString(DataUtils.toArray(beta)));
        map.put("alpha", String.valueOf(alpha));
        return DataUtils.mapToString(map);
    }

    @Override
    public LocalLinearModel deserialize(String s) {
        Map<String, String> map = DataUtils.stringToMap(s, true, true);
        double[] tmp = DataUtils.stringToDoubleArray(map.get("beta"));
        beta = new SimpleMatrix(tmp.length, 1);
        for (int i = 0; i < beta.numRows(); i++) {
            beta.set(i, 0, tmp[i]);
        }
        alpha = Double.parseDouble(map.get("alpha"));
        return this;
    }


}
