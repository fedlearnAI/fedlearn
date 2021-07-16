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

package com.jdt.fedlearn.core.entity.horizontalZoo;

import com.google.protobuf.ByteString;
import com.jdt.fedlearn.grpc.federatedlearning.*;
import com.jdt.fedlearn.grpc.federatedlearning.Vector;
import org.ejml.simple.SimpleMatrix;

import java.util.*;

public class HorizontalZooDataUtils {
    public static HFLModelMessage prepareHFLModelMessage(String modelName,
                                                         String modelHyperPara,
                                                         String modelPara,
                                                         String cmd) {
        HFLModelMessage.Builder myMsg = HFLModelMessage.newBuilder();
        myMsg.setCommandMsg(cmd);
        myMsg.setModelName(modelName);
        myMsg.setModelHyperPara(modelHyperPara);
        myMsg.setModelParameter(modelPara);
        return myMsg.build();
    }

    public static HFLModelMessage prepareHFLModelMessage(String modelName,
                                                         String modelHyperPara,
                                                         String modelPara,
                                                         String cmd,
                                                         ByteString modelString) {
        HFLModelMessage.Builder myMsg = HFLModelMessage.newBuilder();
        myMsg.setCommandMsg(cmd);
        myMsg.setModelName(modelName);
        myMsg.setModelHyperPara(modelHyperPara);
        myMsg.setModelParameter(modelPara);
        myMsg.setModelBytes(modelString);
        return myMsg.build();
    }

    public static HFLModelMessage prepareHFLModelMessage(String modelName,
                                                         String modelHyperPara,
                                                         String modelPara,
                                                         String cmd,
                                                         String client,
                                                         ByteString modelString) {
        HFLModelMessage.Builder myMsg = HFLModelMessage.newBuilder();
        myMsg.setCommandMsg(cmd);
        myMsg.setModelName(modelName);
        myMsg.setModelHyperPara(modelHyperPara);
        myMsg.setModelParameter(modelPara);
        myMsg.setModelBytes(modelString);
        myMsg.setClientInfo(client);
        return myMsg.build();
    }

    public static HFLModelMessage prepareHFLModelMessage(String modelName,
                                                         String modelHyperPara,
                                                         String modelPara,
                                                         String cmd,
                                                         String client,
                                                         ByteString modelString,
                                                         Matrix X,
                                                         Vector y,
                                                         String taskID) {
        HFLModelMessage.Builder myMsg = HFLModelMessage.newBuilder();
        myMsg.setCommandMsg(cmd);
        myMsg.setModelName(modelName);
        myMsg.setModelHyperPara(modelHyperPara);
        myMsg.setModelParameter(modelPara);
        myMsg.setModelBytes(modelString);
        myMsg.setClientInfo(client);
        myMsg.setMatrices(X);
        myMsg.setVectors(y);
        myMsg.setTaskID(taskID);
        return myMsg.build();
    }

    public static ByteString parseModelString(HFLModelMessage msg) {
        return msg.getModelBytes();
    }

    public static String parseCommandMsg(HFLModelMessage msg) {
        return msg.getCommandMsg();
    }

    public static double parseGlobalModelMetric(HFLModelMessage msg) {
        return msg.getGlobalMetric();
    }

    public static double parseLocalModelMetric(HFLModelMessage msg) {
        return msg.getLocalMetric();
    }

    public static String HFLModelMessage2json(HFLModelMessage msg) {
        byte[] bytes = msg.toByteArray();
        String jsonData = Base64.getEncoder().encodeToString(bytes);
        return jsonData;
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
        //assert(rows > 0 && cols > 0);
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

    // Sample without replacement
    public static ArrayList<Integer> choice(int sampleSize, int totalSize, Random rand) {
        ArrayList<Integer> allIndices = new ArrayList<>(totalSize);
        for (int i = 0; i < totalSize; i++) {
            allIndices.add(i);
        }
        Collections.shuffle(allIndices, rand);
        if (sampleSize < totalSize) {
            ArrayList<Integer> sampledIndices = new ArrayList<>(allIndices.subList(0, sampleSize));
            return sampledIndices;
        } else {
            return allIndices;
        }
    }

    //  Given a matrix and a list of column indices, return a sub-matrix
    public static SimpleMatrix selectCols(SimpleMatrix mat, ArrayList<Integer> colIds) {
        int numRows = mat.numRows();
        int numCols = colIds.size();
        SimpleMatrix submat = new SimpleMatrix(numRows, numCols);
        for (int i = 0; i < numRows; i++) {
            for (int j = 0; j < numCols; j++) {
                submat.set(i, j, mat.get(i, colIds.get(j)));
            }
        }
        return submat;
    }
}
