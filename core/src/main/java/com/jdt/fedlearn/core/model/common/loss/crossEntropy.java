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

import com.jdt.fedlearn.core.math.MathExt;
import com.jdt.fedlearn.core.util.Tool;

import java.util.Arrays;
import java.util.stream.IntStream;

public class crossEntropy extends Loss {
    private int numClass = 0;

    public crossEntropy() {
    }

    public crossEntropy(int num) {
        numClass = num;
    }

    public void setNumClass(int num) {
        numClass = num;
    }

    private double clip(double val) {
        if (val < 0.00001) {
            return 0.00001;
        }
        if (val > 0.99999) {
            return 0.99999;
        }
        return val;
    }

    // Softmax
    @Override
    public double[] transform(double[] pred) {
        double[] expPred = Arrays.stream(pred).map(p -> clip(Math.exp(p))).toArray();
        // Pred : flatten nClass * dataSize
        // values: dataSize * nClass
        double[][] values = MathExt.transpose(Tool.reshape(expPred, numClass));
        Arrays.stream(values).forEach(predValues -> {
            double sum = Arrays.stream(predValues).sum();
            IntStream.range(0, numClass).forEach(i -> predValues[i] = predValues[i] / sum);
        });
        // return flatten nClass * dataSize
        return Arrays.stream(MathExt.transpose(values)).flatMapToDouble(Arrays::stream).toArray();
    }

    public double[] postTransform(double[] pred) {
        double[] expPred = Arrays.stream(pred).map(p -> clip(Math.exp(p))).toArray();
        // Pred : flatten dataSize * nClass
        // values: dataSize * nClass
        double[][] values = Tool.reshape(expPred, (int) pred.length / numClass);
        Arrays.stream(values).forEach(predValues -> {
            double sum = Arrays.stream(predValues).sum();
            IntStream.range(0, numClass).forEach(i -> predValues[i] = predValues[i] / sum);
        });
        // return flatten dataSize * nClass
        return Arrays.stream(values).flatMapToDouble(Arrays::stream).toArray();
    }

    // TODO: 合并 grad 和 hess 计算. 用一个结构体保存两者
    @Override
    public double[] grad(double[] pred, double[] label) {
        // flatten nClass * dataSize
        double[] pred1 = transform(pred);
        // if pk == label, gk = pk - 1
        final int dataSize = label.length;
        IntStream.range(0, dataSize).parallel().forEach(i -> pred1[dataSize * (int) label[i] + i] = pred1[dataSize * (int) label[i] + i] - 1);
        return pred1;
    }

    @Override
    public double[] hess(double[] pred, double[] label) {
        // flatten nClass * dataSize
        double[] pred1 = transform(pred);
        return Arrays.stream(pred1).map(p -> p * (1 - p)).toArray();
    }
}
