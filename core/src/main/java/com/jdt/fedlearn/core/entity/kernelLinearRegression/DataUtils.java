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

package com.jdt.fedlearn.core.entity.kernelLinearRegression;

import com.jdt.fedlearn.core.math.MathExt;
import com.jdt.fedlearn.grpc.federatedlearning.*;
import com.jdt.fedlearn.grpc.federatedlearning.Vector;
import org.ejml.simple.SimpleMatrix;

import java.util.ArrayList;

public class DataUtils {


    public static InputMessage prepareInputMessage(Matrix[] matrices, Vector[] vectors, Double[] scalars) {
        InputMessage.Builder inputmessageOrBuilder = InputMessage.newBuilder();
        if (matrices != null) {
            for (Matrix mat : matrices) {
                inputmessageOrBuilder.addMatrices(mat);
            }
        }
        if (vectors != null) {
            for (Vector vec : vectors) {
                inputmessageOrBuilder.addVectors(vec);
            }
        }
        if (scalars != null) {
            for (Double val : scalars) {
                inputmessageOrBuilder.addValues(val);
            }
        }
        return inputmessageOrBuilder.build();
    }


    public static Vector allzeroVector(int length) {
        Vector.Builder vectorOrBuilder = Vector.newBuilder();
        for (int j = 0; j < length; j++) {
            vectorOrBuilder.addValues(0.0);
        }
        return vectorOrBuilder.build();
    }

    public static Vector alloneVector(int length, double scale) {
        Vector.Builder vectorOrBuilder = Vector.newBuilder();
        for (int j = 0; j < length; j++) {
            vectorOrBuilder.addValues(scale);
        }
        return vectorOrBuilder.build();
    }

    public static double[] vectorToArray(Vector vec) {
        assert (vec.getValuesCount() > 0) : "vector length should be larger than 0";
        int num = vec.getValuesCount();
        double[] res = new double[num];
        for (int i = 0; i < num; i++) {
            res[i] = vec.getValues(i);
        }
        return res;
    }


    public static ArrayList<Double> vectorToList(Vector vector) {
        assert (vector.getValuesCount() > 0) : "vector size should be larger than 0";
        int i = 0;
        ArrayList<Double> list = new ArrayList<Double>();
        for (i = 0; i < vector.getValuesCount(); i++) {
            list.add(i, vector.getValues(i));
        }
        return list;
    }

    public static Vector arrayToVector(double[] array) {
        assert (array.length > 0) : "array length should be larger than 1";
        Vector.Builder vector = Vector.newBuilder();
        for (int j = 0; j < array.length; j++) {
            vector.addValues(array[j]);
        }
        return vector.build();
    }

    public static Matrix zeroMatrix(int numRows, int numCols) {
        Matrix.Builder matrixOrBuilder = Matrix.newBuilder();
        for (int i = 0; i < numRows; i++) {
            Vector.Builder vectorOrBuilder = Vector.newBuilder();
            for (int j = 0; j < numCols; j++) {
                vectorOrBuilder.addValues(0.0);
            }
            matrixOrBuilder.addRows(vectorOrBuilder.build());
        }
        return matrixOrBuilder.build();
    }

    public static SimpleMatrix toSmpMatrix(Vector vec) {
        int rows = vec.getValuesCount();
        assert (rows > 0) : "Vector has zero element!";
        SimpleMatrix smpMat = new SimpleMatrix(rows, 1);
        for (int i = 0; i < rows; i++) {
            smpMat.set(i, 0, vec.getValues(i));
        }
        return smpMat;
    }

    public static SimpleMatrix toSmpMatrix(Matrix mat) {
        int rows = mat.getRowsCount();
        assert (rows > 0) : "Matrix has zero row!";
        int cols = mat.getRows(0).getValuesCount();
        assert (cols > 0) : "Matrix has zero column!";
        SimpleMatrix smpMat = new SimpleMatrix(rows, cols);
        for (int i = 0; i < rows; i++) {
            Vector row = mat.getRows(i);
            for (int j = 0; j < cols; j++) {
                smpMat.set(i, j, row.getValues(j));
            }
        }
        return smpMat;
    }

    public static Vector toVector(SimpleMatrix smpMat) {
        int rows = smpMat.numRows();
        int cols = smpMat.numCols();
        assert (cols == 1);
        Vector.Builder vectorOrBuilder = Vector.newBuilder();
        for (int i = 0; i < rows; i++) {
            vectorOrBuilder.addValues(smpMat.get(i, 0));
        }
        return vectorOrBuilder.build();
    }

    public static Matrix toMatrix(SimpleMatrix smpMat) {
        int rows = smpMat.numRows();
        int cols = smpMat.numCols();
        assert (rows > 0 && cols > 0);
        Matrix.Builder matrixOrBuilder = Matrix.newBuilder();
        for (int i = 0; i < rows; i++) {
            Vector.Builder vectorOrBuilder = Vector.newBuilder();
            for (int j = 0; j < cols; j++) {
                vectorOrBuilder.addValues(smpMat.get(i, j));
            }
            matrixOrBuilder.addRows(vectorOrBuilder.build());
        }
        return matrixOrBuilder.build();
    }

    public static double[][] smpmatrixToArray(SimpleMatrix simpleMatrix) {
        int rows = simpleMatrix.numRows();
        int cols = simpleMatrix.numCols();
        double[][] res = new double[rows][cols];
        for (int i = 0; i < rows; i++) {
            for (int j = 0; j < cols; j++) {
                res[i][j] = simpleMatrix.get(i, j);
            }
        }
        return res;
    }

    public static SimpleMatrix arraysToSimpleMatrix(double[][] data){
        int rows = data.length;
        int cols =data[0].length;
        SimpleMatrix simpleMatrix = new SimpleMatrix(rows,cols);
        for (int i = 0; i < rows; i++) {
            for (int j = 0; j < cols; j++) {
                double value = data[i][j];
                simpleMatrix.set(i, j, value);
            }
        }
        return simpleMatrix;
    }

    public static Vector[] arraysToVectors(double[][] data){
        int cols = data[0].length;
        Vector[] vectors = new Vector[cols];
        double[][] transData = MathExt.transpose(data);
        for(int i=0;i<transData.length;i++){
            vectors[i]=arrayToVector(transData[i]);
        }
        return vectors;
    }

    public static double[][] vectorsToArrays(Vector[] vectors){
        double[][] res = new double[vectors.length][vectors[0].getValuesCount()];
        for(int i=0;i<vectors.length;i++){
            res[i] = vectorToArray(vectors[i]);
        }
        return MathExt.transpose(res);
    }

}
