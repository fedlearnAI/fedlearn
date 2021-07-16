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

package com.jdt.fedlearn.core.optimizer;

import org.ujmp.core.DenseMatrix;
import org.ujmp.core.Matrix;

import java.util.ArrayList;
import java.util.Collection;

public class Newton implements Optimizer {
    private int it = 0;
    Matrix weightsUpdate;
    Matrix grediantUpdate;
    private int numOfFeature = -1;
    Matrix G;

    public void setNewton(int numOfFeature) {
        this.numOfFeature = numOfFeature;
        G = DenseMatrix.Factory.eye(numOfFeature, numOfFeature);
    }


    public Matrix bfgs(Matrix G, Matrix s, Matrix y) {
        Matrix I = DenseMatrix.Factory.eye(numOfFeature, numOfFeature);
        Matrix D_left = I.minus(s.mtimes(y.transpose()).times(1.0 / y.transpose().mtimes(s).getAsDouble(0, 0)));
        Matrix D_right = I.minus(y.mtimes(s.transpose()).times(1.0 / y.transpose().mtimes(s).getAsDouble(0, 0)));
        Matrix D_add = s.mtimes(s.transpose()).times(1.0 / y.transpose().mtimes(s).getAsDouble(0, 0));
        return D_left.mtimes(G).mtimes(D_right).plus(D_add);
    }

    public double[] getGlobalUpdate(double[] gredients) {
        if (weightsUpdate == null || grediantUpdate == null) {
            int numOfGrediant = gredients.length;
            setNewton(numOfGrediant);
            weightsUpdate = arrayToMatrix(gredients);
            grediantUpdate = arrayToMatrix(gredients);
            for (int i = 0; i < gredients.length; i++) {
                gredients[i] = -gredients[i];
            }
            return gredients;
        }
        Matrix g = arrayToMatrix(gredients);
        Matrix y = g.minus(grediantUpdate);
        Matrix s = weightsUpdate;
        G = bfgs(G, s, y);
        weightsUpdate = G.mtimes(g).times(-1);//.times(Math.pow(sigma, m));
        grediantUpdate = g;
        int index = 0;
        for (int i = 0; i < gredients.length; i++) {
            gredients[i] = weightsUpdate.getAsDouble(index, 0);
            index++;
        }
        it++;
        return gredients;
    }

    public double[][] getGlobalUpdate(double[][] gredients) {
        if (weightsUpdate == null || grediantUpdate == null) {
            int numOfGrediant = 0;
            for (int i = 0; i < gredients.length; i++) {
                numOfGrediant += gredients[i].length;
            }
            setNewton(numOfGrediant);
            weightsUpdate = arrayToMatrix(gredients);
            grediantUpdate = arrayToMatrix(gredients);
            for (int i = 0; i < gredients.length; i++) {
                for (int j = 0; j < gredients[i].length; j++) {
                    gredients[i][j] = -gredients[i][j];
                }
            }
            return gredients;
        }
        Matrix g = arrayToMatrix(gredients);
        Matrix y = g.minus(grediantUpdate);
        Matrix s = weightsUpdate;
        G = bfgs(G, s, y);
        //g = g.times(Math.pow(sigma, m));
        weightsUpdate = G.mtimes(g).times(-1);//.times(Math.pow(sigma, m));
        grediantUpdate = g;
        int index = 0;
        for (int i = 0; i < gredients.length; i++) {
            for (int j = 0; j < gredients[i].length; j++) {
                gredients[i][j] = weightsUpdate.getAsDouble(index, 0);
                index++;
            }
        }
        it++;
        return gredients;
    }

    public Matrix arrayToMatrix(double[][] array) {

        Matrix result = DenseMatrix.Factory.zeros(numOfFeature, 1);
        int index = 0;
        for (int i = 0; i < array.length; i++) {
            for (int j = 0; j < array[i].length; j++) {
                result.setAsDouble(array[i][j], index++, 0);
            }
        }
        return result;
    }

    public Matrix arrayToMatrix(double[] array) {

        Matrix result = DenseMatrix.Factory.zeros(numOfFeature, 1);
        int index = 0;
        for (int i = 0; i < array.length; i++) {
            result.setAsDouble(array[i], index++, 0);
        }
        return result;
    }

    public Collection<Long> randomChoose(Collection<Long> samples) {
        return new ArrayList<>();
    }
}
