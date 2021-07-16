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

package com.jdt.fedlearn.core.entity.randomForest;

import com.google.protobuf.InvalidProtocolBufferException;
import com.jdt.fedlearn.core.encryption.*;
import com.jdt.fedlearn.core.encryption.IterativeAffine.IterativeAffineCiphertext;
import com.jdt.fedlearn.core.encryption.IterativeAffine.IterativeAffineKey;
import com.jdt.fedlearn.core.encryption.javallier.JavallierTool;
import com.jdt.fedlearn.core.type.MemoryUnitsType;
import com.jdt.fedlearn.grpc.federatedlearning.*;
import com.jdt.fedlearn.grpc.federatedlearning.Vector;
import com.n1analytics.paillier.EncryptedNumber;
import com.n1analytics.paillier.PaillierContext;
import com.n1analytics.paillier.PaillierPrivateKey;
import com.n1analytics.paillier.PaillierPublicKey;

import java.io.*;
import java.math.BigInteger;

import org.ejml.simple.SimpleMatrix;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.nio.ByteBuffer;

import org.apache.commons.lang3.StringUtils;

import java.nio.charset.StandardCharsets;

public class DataUtils {


    public static InputMessage prepareInputMessage(Matrix[] matrices, Vector[] vectors, Double[] scalars) {
        InputMessage.Builder inputmessageOrBuilder = InputMessage.newBuilder();
        for (Matrix mat : matrices) {
            inputmessageOrBuilder.addMatrices(mat);
        }
        for (Vector vec : vectors) {
            inputmessageOrBuilder.addVectors(vec);
        }
        for (Double val : scalars) {
            inputmessageOrBuilder.addValues(val);
        }
        return inputmessageOrBuilder.build();
    }

    public static InputMessage prepareInputMessage(Matrix[] matrices,
                                                   Vector[] vectors,
                                                   Double[] scalars,
                                                   PaillierMatrix[] matricesEnc,
                                                   PaillierVector[] vectorsEnc,
                                                   PaillierValue[] scalarsEnc,
                                                   PaillierKeyPublic keyPublic) {

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
        if (matricesEnc != null) {
            for (PaillierMatrix mat : matricesEnc) {
                inputmessageOrBuilder.addPailliermatrices(mat);
            }
        }
        if (vectorsEnc != null) {
            for (PaillierVector vec : vectorsEnc) {
                inputmessageOrBuilder.addPailliervectors(vec);
            }
        }
        if (scalarsEnc != null) {
            for (PaillierValue val : scalarsEnc) {
                inputmessageOrBuilder.addPailliervalues(val);
            }
        }
        if (keyPublic != null) {
            inputmessageOrBuilder.setPaillierkeypublic(keyPublic);
        }
        // add param
        inputmessageOrBuilder.setParams(setRandomForestEncryptionScheme("paillier"));
        return inputmessageOrBuilder.build();
    }


    public static MultiInputMessage prepareMultiInputMessage(Matrix[][] matrices,
                                                             Vector[][] vectors,
                                                             Double[][] scalars,
                                                             int numMessages) {
        MultiInputMessage.Builder multInputMessageOrBuilder = MultiInputMessage.newBuilder();

        // check length
        if (matrices.length > 0) {
            assert (matrices.length == numMessages) : "Matrices length must align with numMessages";
        }
        if (vectors.length > 0) {
            assert (vectors.length == numMessages) : "Vectors length must align with numMessages";
        }
        if (scalars.length > 0) {
            assert (scalars.length == numMessages) : "Scalars length must align with numMessages";
        }
        for (int i = 0; i < numMessages; i++) {
            InputMessage.Builder inputmessageOrBuilder = InputMessage.newBuilder();
            if (matrices.length > 0) {
                Matrix[] matrixi = matrices[i];
                for (Matrix mat : matrixi) {
                    inputmessageOrBuilder.addMatrices(mat);
                }
            }
            if (vectors.length > 0) {
                Vector[] vectori = vectors[i];
                for (Vector vec : vectori) {
                    inputmessageOrBuilder.addVectors(vec);
                }
            }
            if (scalars.length > 0) {
                Double[] scalari = scalars[i];
                for (Double val : scalari) {
                    inputmessageOrBuilder.addValues(val);
                }
            }

            multInputMessageOrBuilder.addMessages(inputmessageOrBuilder.build());
        }
        return multInputMessageOrBuilder.build();
    }

    public static MultiInputMessage prepareMultiInputMessage(Matrix[][] matrices,
                                                             Vector[][] vectors,
                                                             Double[][] scalars,
                                                             PaillierMatrix[][] matricesEnc,
                                                             PaillierVector[][] vectorsEnc,
                                                             PaillierValue[][] scalarsEnc,
                                                             PaillierKeyPublic[] keyPublic,
                                                             int numMessages) {
        MultiInputMessage.Builder multInputMessageOrBuilder = MultiInputMessage.newBuilder();
        if (matrices.length > 0) {
            assert (matrices.length == numMessages) : "Matrices length must align with numMessages";
        }
        if (vectors.length > 0) {
            assert (vectors.length == numMessages) : "Vectors length must align with numMessages";
        }
        if (scalars.length > 0) {
            assert (scalars.length == numMessages) : "Scalars length must align with numMessages";
        }
        if (matricesEnc.length > 0) {
            assert (matricesEnc.length == numMessages) : "PaillierMatrices length must align with numMessages";
        }
        if (vectorsEnc.length > 0) {
            assert (vectorsEnc.length == numMessages) : "PaillierVectors length must align with numMessages";
        }
        if (scalarsEnc.length > 0) {
            assert (scalarsEnc.length == numMessages) : "PaillierScalars length must align with numMessages";
        }
        assert (keyPublic.length == numMessages) : "Public key length must align with numMessages";
        for (int i = 0; i < numMessages; i++) {
            InputMessage.Builder inputmessageOrBuilder = InputMessage.newBuilder();
            if (matrices.length > 0) {
                Matrix[] matrixi = matrices[i];
                for (Matrix mat : matrixi) {
                    inputmessageOrBuilder.addMatrices(mat);
                }
            }
            if (vectors.length > 0) {
                Vector[] vectori = vectors[i];
                for (Vector vec : vectori) {
                    inputmessageOrBuilder.addVectors(vec);
                }
            }
            if (scalars.length > 0) {
                Double[] scalari = scalars[i];
                for (Double val : scalari) {
                    inputmessageOrBuilder.addValues(val);
                }
            }
            if (matricesEnc.length > 0) {
                PaillierMatrix[] matrixEnci = matricesEnc[i];
                for (PaillierMatrix mat : matrixEnci) {
                    inputmessageOrBuilder.addPailliermatrices(mat);
                }
            }
            if (vectorsEnc.length > 0) {
                PaillierVector[] vectorEnci = vectorsEnc[i];
                for (PaillierVector vec : vectorEnci) {
                    inputmessageOrBuilder.addPailliervectors(vec);
                }
            }
            if (scalarsEnc.length > 0) {
                PaillierValue[] scalarEnci = scalarsEnc[i];
                for (PaillierValue val : scalarEnci) {
                    inputmessageOrBuilder.addPailliervalues(val);
                }
            }
            if (keyPublic.length > 0) {
                PaillierKeyPublic keyi = keyPublic[i];
                inputmessageOrBuilder.setPaillierkeypublic(keyi);
            }
            // add param
            inputmessageOrBuilder.setParams(setRandomForestEncryptionScheme("paillier"));
            multInputMessageOrBuilder.addMessages(inputmessageOrBuilder.build());
        }
        return multInputMessageOrBuilder.build();
    }

    public static OutputMessage prepareOutputMessage(Matrix[] matrices, Vector[] vectors, Double[] scalars) {
        OutputMessage.Builder outputmessageOrBuilder = OutputMessage.newBuilder();
        for (Matrix mat : matrices) {
            outputmessageOrBuilder.addMatrices(mat);
        }
        for (Vector vec : vectors) {
            outputmessageOrBuilder.addVectors(vec);
        }
        for (Double val : scalars) {
            outputmessageOrBuilder.addValues(val);
        }
        return outputmessageOrBuilder.build();
    }

    public static OutputMessage prepareOutputMessage(Matrix[] matrices,
                                                     Vector[] vectors,
                                                     Double[] scalars,
                                                     PaillierMatrix[] matricesEnc,
                                                     PaillierVector[] vectorsEnc,
                                                     PaillierValue[] scalarsEnc,
                                                     PaillierKeyPublic keyPublic) {

        OutputMessage.Builder outputmessageOrBuilder = OutputMessage.newBuilder();
        if (matrices != null) {
            for (Matrix mat : matrices) {
                outputmessageOrBuilder.addMatrices(mat);
            }
        }
        if (vectors != null) {
            for (Vector vec : vectors) {
                outputmessageOrBuilder.addVectors(vec);
            }
        }
        if (scalars != null) {
            for (Double val : scalars) {
                outputmessageOrBuilder.addValues(val);
            }
        }
        if (matricesEnc != null) {
            for (PaillierMatrix mat : matricesEnc) {
                outputmessageOrBuilder.addPailliermatrices(mat);
            }
        }
        if (vectorsEnc != null) {
            for (PaillierVector vec : vectorsEnc) {
                outputmessageOrBuilder.addPailliervectors(vec);
            }
        }
        if (scalarsEnc != null) {
            for (PaillierValue val : scalarsEnc) {
                outputmessageOrBuilder.addPailliervalues(val);
            }
        }
        if (keyPublic != null) {
            outputmessageOrBuilder.setPaillierkeypublic(keyPublic);
        }
        // TODO set param
        return outputmessageOrBuilder.build();
    }

    public static MultiOutputMessage prepareMultiOutputMessage(Matrix[][] matrices,
                                                               int numMessages) {
        MultiOutputMessage.Builder multOutputMessageOrBuilder = MultiOutputMessage.newBuilder();

        // check length
        if (matrices.length > 0) {
            assert (matrices.length == numMessages) : "Matrices length must align with numMessages";
        }
        for (int i = 0; i < numMessages; i++) {
            OutputMessage.Builder outputmessageOrBuilder = OutputMessage.newBuilder();
            if (matrices.length > 0) {
                Matrix[] matrixi = matrices[i];
                for (Matrix mat : matrixi) {
                    outputmessageOrBuilder.addMatrices(mat);
                }
            }
            multOutputMessageOrBuilder.addMessages(outputmessageOrBuilder.build());
        }
        return multOutputMessageOrBuilder.build();
    }

    public static MultiOutputMessage prepareMultiOutputMessage(Vector[][] vectors,
                                                               String[] messages,
                                                               int numMessages) {
        MultiOutputMessage.Builder multOutputMessageOrBuilder = MultiOutputMessage.newBuilder();

        // check length
        if (vectors.length > 0) {
            assert (vectors.length == numMessages) : "Vectors length must align with numMessages";
        }
        if (messages.length > 0) {
            assert (messages.length == numMessages) : "Vectors length must align with numMessages";
        }
        for (int i = 0; i < numMessages; i++) {
            OutputMessage.Builder outputmessageOrBuilder = OutputMessage.newBuilder();
            if (vectors.length > 0) {
                Vector[] vectori = vectors[i];
                for (Vector vec : vectori) {
                    outputmessageOrBuilder.addVectors(vec);
                }
            }
            if (messages.length > 0) {
                String messagei = messages[i];
                {
                    outputmessageOrBuilder.setMessage(messagei);
                }
            }
            multOutputMessageOrBuilder.addMessages(outputmessageOrBuilder.build());
        }
        return multOutputMessageOrBuilder.build();
    }

    public static MultiOutputMessage prepareMultiOutputMessage(Double[][] values,
                                                               String[] messages,
                                                               int numMessages) {
        MultiOutputMessage.Builder multOutputMessageOrBuilder = MultiOutputMessage.newBuilder();

        // check length
        if (values.length > 0) {
            assert (values.length == numMessages) : "Matrices length must align with numMessages";
        }
        if (messages.length > 0) {
            assert (messages.length == numMessages) : "Vectors length must align with numMessages";
        }
        for (int i = 0; i < numMessages; i++) {
            OutputMessage.Builder outputmessageOrBuilder = OutputMessage.newBuilder();
            if (values.length > 0) {
                Double[] matrixi = values[i];
                for (Double mat : matrixi) {
                    outputmessageOrBuilder.addValues(mat);
                }
            }
            if (messages.length > 0) {
                String messagei = messages[i];
                {
                    outputmessageOrBuilder.setMessage(messagei);
                }
            }
            multOutputMessageOrBuilder.addMessages(outputmessageOrBuilder.build());
        }
        return multOutputMessageOrBuilder.build();
    }

    public static MultiOutputMessage prepareMultiOutputMessage(PaillierMatrix[][] matricesEnc,
                                                               PaillierPublicKey[] keyPublic,
                                                               int numMessages) {
        MultiOutputMessage.Builder multOutputMessageOrBuilder = MultiOutputMessage.newBuilder();
        if (matricesEnc.length > 0) {
            assert (matricesEnc.length == numMessages) : "PaillierMatrices length must align with numMessages";
        }
        assert (keyPublic.length == numMessages) : "Public key length must align with numMessages";
        for (int i = 0; i < numMessages; i++) {
            OutputMessage.Builder outputmessageOrBuilder = OutputMessage.newBuilder();
            if (matricesEnc.length > 0) {
                PaillierMatrix[] matrixEnci = matricesEnc[i];
                for (PaillierMatrix mat : matrixEnci) {
                    outputmessageOrBuilder.addPailliermatrices(mat);
                }
            }
            if (keyPublic.length > 0) {
                PaillierKeyPublic keyi = paillierPublicKeyToRpcProto(keyPublic[i]);
                outputmessageOrBuilder.setPaillierkeypublic(keyi);
            }
            multOutputMessageOrBuilder.addMessages(outputmessageOrBuilder.build());
        }
        return multOutputMessageOrBuilder.build();
    }

    // TODO: merge two encryption scheme
    public static InputMessage prepareIterativeAffineInputMessage(Matrix[] matrices,
                                                                  Vector[] vectors,
                                                                  Double[] scalars,
                                                                  IterativeAffineMatrix[] matricesEnc,
                                                                  IterativeAffineVector[] vectorsEnc,
                                                                  IterativeAffineValue[] scalarsEnc) {

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
        if (matricesEnc != null) {
            for (IterativeAffineMatrix mat : matricesEnc) {
                inputmessageOrBuilder.addIterativeaffinematrices(mat);
            }
        }
        if (vectorsEnc != null) {
            for (IterativeAffineVector vec : vectorsEnc) {
                inputmessageOrBuilder.addIterativeaffinevectors(vec);
            }
        }
        if (scalarsEnc != null) {
            for (IterativeAffineValue val : scalarsEnc) {
                inputmessageOrBuilder.addIterativeaffinevalues(val);
            }
        }
        inputmessageOrBuilder.setParams(setRandomForestEncryptionScheme("iterativeAffine"));
        return inputmessageOrBuilder.build();
    }

    // TODO: merge two encryption scheme
    public static MultiInputMessage prepareMultiIterativeAffineInputMessage(Matrix[][] matrices,
                                                                            Vector[][] vectors,
                                                                            Double[][] scalars,
                                                                            IterativeAffineMatrix[][] matricesEnc,
                                                                            IterativeAffineVector[][] vectorsEnc,
                                                                            IterativeAffineValue[][] scalarsEnc,
                                                                            int numMessages) {
        MultiInputMessage.Builder multInputMessageOrBuilder = MultiInputMessage.newBuilder();
        if (matrices.length > 0) {
            assert (matrices.length == numMessages) : "Matrices length must align with numMessages";
        }
        if (vectors.length > 0) {
            assert (vectors.length == numMessages) : "Vectors length must align with numMessages";
        }
        if (scalars.length > 0) {
            assert (scalars.length == numMessages) : "Scalars length must align with numMessages";
        }
        if (matricesEnc.length > 0) {
            assert (matricesEnc.length == numMessages) : "PaillierMatrices length must align with numMessages";
        }
        if (vectorsEnc.length > 0) {
            assert (vectorsEnc.length == numMessages) : "PaillierVectors length must align with numMessages";
        }
        if (scalarsEnc.length > 0) {
            assert (scalarsEnc.length == numMessages) : "PaillierScalars length must align with numMessages";
        }
        for (int i = 0; i < numMessages; i++) {
            InputMessage.Builder inputmessageOrBuilder = InputMessage.newBuilder();
            if (matrices.length > 0) {
                Matrix[] matrixi = matrices[i];
                for (Matrix mat : matrixi) {
                    inputmessageOrBuilder.addMatrices(mat);
                }
            }
            if (vectors.length > 0) {
                Vector[] vectori = vectors[i];
                for (Vector vec : vectori) {
                    inputmessageOrBuilder.addVectors(vec);
                }
            }
            if (scalars.length > 0) {
                Double[] scalari = scalars[i];
                for (Double val : scalari) {
                    inputmessageOrBuilder.addValues(val);
                }
            }
            if (matricesEnc.length > 0) {
                IterativeAffineMatrix[] matrixEnci = matricesEnc[i];
                for (IterativeAffineMatrix mat : matrixEnci) {
                    inputmessageOrBuilder.addIterativeaffinematrices(mat);
                }
            }
            if (vectorsEnc.length > 0) {
                IterativeAffineVector[] vectorEnci = vectorsEnc[i];
                for (IterativeAffineVector vec : vectorEnci) {
                    inputmessageOrBuilder.addIterativeaffinevectors(vec);
                }
            }
            if (scalarsEnc.length > 0) {
                IterativeAffineValue[] scalarEnci = scalarsEnc[i];
                for (IterativeAffineValue val : scalarEnci) {
                    inputmessageOrBuilder.addIterativeaffinevalues(val);
                }
            }
            inputmessageOrBuilder.setParams(setRandomForestEncryptionScheme("iterativeAffine"));
            multInputMessageOrBuilder.addMessages(inputmessageOrBuilder.build());
        }
        return multInputMessageOrBuilder.build();
    }

    // TODO: merge two encryption scheme
    public static AlgorithmParam setRandomForestEncryptionScheme(String encryptionScheme) {
        RandomForestParam.Builder rfParamBuilder = RandomForestParam.newBuilder();
        rfParamBuilder.setEncryptionScheme(encryptionScheme);
        AlgorithmParam.Builder algoParamBuilder = AlgorithmParam.newBuilder();
        algoParamBuilder.setRfParams(rfParamBuilder);
        return algoParamBuilder.build();
    }


    public static Vector randGaussVector(int length, Random rand) {
        Vector.Builder vectorOrBuilder = Vector.newBuilder();
        for (int j = 0; j < length; j++) {
            vectorOrBuilder.addValues(rand.nextGaussian());
        }
        return vectorOrBuilder.build();
    }

    public static Matrix randGaussMatrix(int numRows, int numCols, Random rand) {
        Matrix.Builder matrixOrBuilder = Matrix.newBuilder();
        for (int i = 0; i < numRows; i++) {
            Vector.Builder vectorOrBuilder = Vector.newBuilder();
            for (int j = 0; j < numCols; j++) {
                vectorOrBuilder.addValues(rand.nextGaussian());
            }
            matrixOrBuilder.addRows(vectorOrBuilder.build());
        }
        return matrixOrBuilder.build();
    }


    public static SimpleMatrix randGaussSmpMatrix(int numRows, int numCols, Random rand) {
        return toSmpMatrix(randGaussMatrix(numRows, numCols, rand));
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
        //assert(cols>0): "Matrix has zero column!";
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

    public static double[] toArray(SimpleMatrix smpMat) {
        // convert vector to double array
        if (smpMat.numCols() == 1) {
            return doToArray(smpMat);
        } else if (smpMat.numRows() == 1) {
            return doToArray(smpMat.transpose());
        } else {
            throw new IllegalArgumentException("Need a vector!");
        }
    }

    private static double[] doToArray(SimpleMatrix smpMat) {
        double[] res = new double[smpMat.numRows()];
        for (int i = 0; i < smpMat.numRows(); i++) {
            res[i] = smpMat.get(i, 0);
        }
        return res;
    }

    public static double mean(SimpleMatrix smpMat) {
        final int N = smpMat.getNumElements();
        double total = 0.;
        for (int i = 0; i < N; i++) {
            total += smpMat.get(i);
        }
        return total / N;
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

    public static ArrayList<Integer> choice(int sampleSize, List<Integer> array, Random rand) {
        List<Integer> idx = choice(sampleSize, array.size(), rand);
        ArrayList<Integer> selected = new ArrayList<>();
        for (int i : idx) {
            selected.add(array.get(i));
        }
        return selected;
    }

    //  Given a matrix and a list of column indices, return a sub-matrix
    public static SimpleMatrix selectCols(SimpleMatrix mat, List<Integer> colIds) {
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

    //  Given a matrix and a list of row indices, return a sub-matrix
    public static SimpleMatrix selecRows(SimpleMatrix mat, List<Integer> rowIds) {
        int numRows = rowIds.size();
        int numCols = mat.numCols();
        SimpleMatrix submat = new SimpleMatrix(numRows, numCols);
        for (int i = 0; i < numRows; i++) {
            for (int j = 0; j < numCols; j++) {
                submat.set(i, j, mat.get(rowIds.get(i), j));
            }
        }
        return submat;
    }

    public static SimpleMatrix selecRows(SimpleMatrix mat, Integer[] rowIds) {
        int numRows = rowIds.length;
        int numCols = mat.numCols();
        SimpleMatrix submat = new SimpleMatrix(numRows, numCols);
        for (int i = 0; i < numRows; i++) {
            for (int j = 0; j < numCols; j++) {
                submat.set(i, j, mat.get(rowIds[i], j));
            }
        }
        return submat;
    }

    public static SimpleMatrix selecRows(SimpleMatrix mat, int[] rowIds) {
        int numRows = rowIds.length;
        int numCols = mat.numCols();
        SimpleMatrix submat = new SimpleMatrix(numRows, numCols);
        for (int i = 0; i < numRows; i++) {
            for (int j = 0; j < numCols; j++) {
                submat.set(i, j, mat.get(rowIds[i], j));
            }
        }
        return submat;
    }

    public static PaillierValue paillierEncryptedNumberToRpcProto(EncryptedNumber en) {
        PaillierValue.Builder paillierValueOrBuilder = PaillierValue.newBuilder();
        paillierValueOrBuilder.setCiphertext(en.calculateCiphertext().toString());
        paillierValueOrBuilder.setExponent(Integer.toString(en.getExponent()));
        return paillierValueOrBuilder.build();
    }

    public static PaillierKeyPublic paillierPublicKeyToRpcProto(PaillierPublicKey pkp) {
        PaillierKeyPublic.Builder paillierKeyPublicOrBuilder = PaillierKeyPublic.newBuilder();
        paillierKeyPublicOrBuilder.setN(pkp.getModulus().toString());
        paillierKeyPublicOrBuilder.setNsquare(pkp.getModulusSquared().toString());
        paillierKeyPublicOrBuilder.setG(pkp.getGenerator().toString());
        return paillierKeyPublicOrBuilder.build();
    }

    public static IterativeAffineValue iterativeAffineCiphertextToRpcProto(IterativeAffineCiphertext val) {
        IterativeAffineValue.Builder iterativeAffineValueOrBuilder = IterativeAffineValue.newBuilder();
        iterativeAffineValueOrBuilder.setCipher1(val.getCipher1().toString());
        iterativeAffineValueOrBuilder.setCipher2(val.getCipher2().toString());
        iterativeAffineValueOrBuilder.setNfinal(val.getN_final().toString());
        iterativeAffineValueOrBuilder.setMulttimes(String.valueOf(val.getMult_times()));
        return iterativeAffineValueOrBuilder.build();
    }

    public static EncryptedNumber rpcProtoToPaillierEncryptedNumber(PaillierKeyPublic pkp, PaillierValue pv) {
        PaillierPublicKey paillierPublicKey = new PaillierPublicKey(new BigInteger(pkp.getN()));
        // Default signed = True so we are not adding signed to Proto buffer
        // Default precision = 1024 so we are not adding precision to Proto buffer
        // Default base = 16 so we are not adding base to Proto buffer
        PaillierContext paillierContext = new PaillierContext(paillierPublicKey, true, 1024, 16);
        // Default isSafe = true so we are not adding base to Proto buffer
        return new EncryptedNumber(paillierContext, new BigInteger(pv.getCiphertext()), Integer.parseInt(pv.getExponent()), true);
    }

    public static EncryptedNumber rpcProtoToPaillierEncryptedNumber(PaillierKeyPublic pkp, PaillierValue pv, int encryptionCertainty) {
        PaillierPublicKey paillierPublicKey = new PaillierPublicKey(new BigInteger(pkp.getN()));
        // Default signed = True so we are not adding signed to Proto buffer
        // Default precision = 1024 so we are not adding precision to Proto buffer
        // Default base = 16 so we are not adding base to Proto buffer
        PaillierContext paillierContext = new PaillierContext(paillierPublicKey, true, encryptionCertainty, 16);
        // Default isSafe = true so we are not adding base to Proto buffer
        return new EncryptedNumber(paillierContext, new BigInteger(pv.getCiphertext()), Integer.parseInt(pv.getExponent()), true);
    }

    public static IterativeAffineCiphertext rpcProtoToPaillierIterativeAffineCiphertext(IterativeAffineValue val) {
        BigInteger cipher1 = new BigInteger(val.getCipher1());
        BigInteger cipher2 = new BigInteger(val.getCipher2());
        BigInteger nFinal = new BigInteger(val.getNfinal());
        int multTimes = Integer.parseInt(val.getMulttimes());
        long multiple = (long) Math.pow(2, 50);
        IterativeAffineCiphertext cipher = new IterativeAffineCiphertext(cipher1, cipher2, nFinal, multiple, multTimes);
        return cipher;
    }

    public static PaillierVector toPaillierVector(EncryptedNumber[] vec) {
        PaillierVector.Builder paillierVectorOrBuilder = PaillierVector.newBuilder();
        for (EncryptedNumber val : vec) {
            paillierVectorOrBuilder.addValues(paillierEncryptedNumberToRpcProto(val));
        }
        return paillierVectorOrBuilder.build();
    }

    public static PaillierVector toPaillierVector(PaillierValue[] vec) {
        PaillierVector.Builder paillierVectorOrBuilder = PaillierVector.newBuilder();
        for (PaillierValue val : vec) {
            paillierVectorOrBuilder.addValues(val);
        }
        return paillierVectorOrBuilder.build();
    }

    public static PaillierVector toPaillierVector(ArrayList<EncryptedNumber> vec) {
        PaillierVector.Builder paillierVectorOrBuilder = PaillierVector.newBuilder();
        for (EncryptedNumber val : vec) {
            paillierVectorOrBuilder.addValues(paillierEncryptedNumberToRpcProto(val));
        }
        return paillierVectorOrBuilder.build();
    }

    public static PaillierMatrix toPaillierMatrix(EncryptedNumber[][] mat) {
        PaillierMatrix.Builder paillierMatrixOrBuilder = PaillierMatrix.newBuilder();
        for (EncryptedNumber[] vec : mat) {
            PaillierVector paillierVec = toPaillierVector(vec);
            paillierMatrixOrBuilder.addRows(paillierVec);
        }
        return paillierMatrixOrBuilder.build();
    }

    public static PaillierMatrix toPaillierMatrix(PaillierValue[][] mat) {
        PaillierMatrix.Builder paillierMatrixOrBuilder = PaillierMatrix.newBuilder();
        for (PaillierValue[] row : mat) {
            PaillierVector paillierVec = toPaillierVector(row);
            paillierMatrixOrBuilder.addRows(paillierVec);
        }
        return paillierMatrixOrBuilder.build();
    }

    public static EncryptedNumber[] PaillierEncrypt(Vector vec, Encryptor encryptor, boolean issafe) {
        int n = vec.getValuesCount();
        EncryptedNumber[] result = new EncryptedNumber[n];
        IntStream.range(0, n).parallel()
                .forEach(index -> {
                    result[index] = encryptor.encrypt(vec.getValues(index), issafe);
                });
        return result;
    }

    public static EncryptedNumber[] PaillierEncrypt(Vector vec, PaillierPublicKey encryptor, boolean issafe) {
        int n = vec.getValuesCount();
        EncryptedNumber[] result = new EncryptedNumber[n];
        IntStream.range(0, n).parallel()
                .forEach(index -> {
                    result[index] = JavallierTool.encryptionInner(vec.getValues(index), encryptor, issafe);
                });
        return result;
    }

    public static IterativeAffineVector toIterativeAffineVector(ArrayList<IterativeAffineCiphertext> vec) {
        IterativeAffineVector.Builder iterativeAffineVectorOrBuilder = IterativeAffineVector.newBuilder();
        for (IterativeAffineCiphertext val : vec) {
            iterativeAffineVectorOrBuilder.addValues(iterativeAffineCiphertextToRpcProto(val));
        }
        return iterativeAffineVectorOrBuilder.build();
    }

    public static IterativeAffineCiphertext[] IterativeAffineEncrypt(Vector vec, IterativeAffineKey key) {
        int n = vec.getValuesCount();
        IterativeAffineCiphertext[] result = new IterativeAffineCiphertext[n];
        IntStream.range(0, n).parallel()
                .forEach(index -> {
                    result[index] = key.encrypt(vec.getValues(index));
                });
        return result;
    }


    public static Matrix PaillierDecryptParallel(PaillierMatrix mat, Decryptor decryptor, PaillierKeyPublic keyPublic) {
        int numRows = mat.getRowsCount();
        if (numRows == 0) {
            // Null matrix
            Matrix.Builder matrixOrBuilder = Matrix.newBuilder();
            return matrixOrBuilder.build();
        }
        int numCols = mat.getRows(0).getValuesCount();
        Matrix.Builder matrixOrBuilder = Matrix.newBuilder();
        for (int i = 0; i < numRows; i++) {
            PaillierVector pvec = mat.getRows(i);
            assert (pvec.getValuesCount() == numCols);
            List<PaillierValue> v1 = pvec.getValuesList();
            List<Double> vals = v1.parallelStream().map(x -> decryptor.decrypt(rpcProtoToPaillierEncryptedNumber(keyPublic, x)))
                    .collect(Collectors.toList());
            Vector.Builder vectorOrBuilder = Vector.newBuilder();
            for (int j = 0; j < numCols; j++) {
                vectorOrBuilder.addValues(vals.get(j));
            }
            matrixOrBuilder.addRows(vectorOrBuilder.build());
        }
        return matrixOrBuilder.build();
    }


    public static Matrix PaillierDecryptParallel(PaillierMatrix mat, PaillierPrivateKey privateKey, PaillierKeyPublic keyPublic, int encryptionCertainty) {
        int numRows = mat.getRowsCount();
        if (numRows == 0) {
            // Null matrix
            Matrix.Builder matrixOrBuilder = Matrix.newBuilder();
            return matrixOrBuilder.build();
        }
        int numCols = mat.getRows(0).getValuesCount();
        Matrix.Builder matrixOrBuilder = Matrix.newBuilder();
        for (int i = 0; i < numRows; i++) {
            PaillierVector pvec = mat.getRows(i);
            assert (pvec.getValuesCount() == numCols);
            List<PaillierValue> v1 = pvec.getValuesList();
            List<Double> vals = v1.parallelStream()
                    .map(x -> JavallierTool.decryptionInner(rpcProtoToPaillierEncryptedNumber(keyPublic, x, encryptionCertainty), privateKey))
                    .collect(Collectors.toList());
            Vector.Builder vectorOrBuilder = Vector.newBuilder();
            for (int j = 0; j < numCols; j++) {
                vectorOrBuilder.addValues(vals.get(j));
            }
            matrixOrBuilder.addRows(vectorOrBuilder.build());
        }
        return matrixOrBuilder.build();
    }

    public static Vector PaillierDecryptParallel(PaillierVector pvec, Decryptor decryptor, PaillierKeyPublic keyPublic) {
        int numCols = pvec.getValuesCount();
        Vector.Builder vectorOrBuilder = Vector.newBuilder();
        if (numCols == 0) {
            // Null vector
            return vectorOrBuilder.build();
        }
        List<PaillierValue> v1 = pvec.getValuesList();
        List<Double> vals = v1.parallelStream().map(x -> decryptor.decrypt(rpcProtoToPaillierEncryptedNumber(keyPublic, x)))
                .collect(Collectors.toList());
        for (int j = 0; j < numCols; j++) {
            vectorOrBuilder.addValues(vals.get(j));
        }
        return vectorOrBuilder.build();
    }

    public static Matrix IterativeAffineDecrypt(IterativeAffineMatrix mat,
                                                IterativeAffineKey key) {
        int numRows = mat.getRowsCount();
        int numCols = 0;
        if (numRows > 0) {
            numCols = mat.getRows(0).getValuesCount();
        }
        Matrix.Builder matrixOrBuilder = Matrix.newBuilder();
        for (int i = 0; i < numRows; i++) {
            IterativeAffineVector pvec = mat.getRows(i);
            assert (pvec.getValuesCount() == numCols);
            List<IterativeAffineValue> v1 = pvec.getValuesList();
            List<Double> vals = v1.parallelStream().map(
                    x -> key.decrypt(rpcProtoToPaillierIterativeAffineCiphertext(x)))
                    .collect(Collectors.toList());
            Vector.Builder vectorOrBuilder = Vector.newBuilder();
            for (int j = 0; j < numCols; j++) {
                vectorOrBuilder.addValues(vals.get(j));
            }
            matrixOrBuilder.addRows(vectorOrBuilder.build());
        }
        return matrixOrBuilder.build();
    }

    public static String inputMessage2json(InputMessage msg) {
        byte[] bytes = msg.toByteArray();
        String jsonData = Base64.getEncoder().encodeToString(bytes);
        return jsonData;
    }

    public static String inputMessage2json(MultiInputMessage msg) {
        byte[] bytes = msg.toByteArray();
        String jsonData = Base64.getEncoder().encodeToString(bytes);
        return jsonData;
    }

    public static InputMessage json2inputMessage(String jsonData) {
        byte[] bytes = Base64.getDecoder().decode(jsonData);
        try {
            InputMessage msg = InputMessage.parseFrom(bytes);
            return msg;
        } catch (InvalidProtocolBufferException e) {
        }
        InputMessage.Builder builder = InputMessage.newBuilder();
        return builder.build();
    }

    public static MultiInputMessage json2MultiInputMessage(String jsonData) {
        byte[] bytes = Base64.getDecoder().decode(jsonData);
        try {
            MultiInputMessage msg = MultiInputMessage.parseFrom(bytes);
            return msg;
        } catch (InvalidProtocolBufferException e) {
        }
        MultiInputMessage.Builder builder = MultiInputMessage.newBuilder();
        return builder.build();
    }

    public static String outputMessage2json(OutputMessage msg) {
        byte[] bytes = msg.toByteArray();
        String jsonData = Base64.getEncoder().encodeToString(bytes);
        return jsonData;
    }

    public static String outputMessage2json(MultiOutputMessage msg) {
        byte[] bytes = msg.toByteArray();
        String jsonData = Base64.getEncoder().encodeToString(bytes);
        return jsonData;
    }

    public static OutputMessage json2OutputMessage(String jsonData) {
        byte[] bytes = Base64.getDecoder().decode(jsonData);
        try {
            OutputMessage msg = OutputMessage.parseFrom(bytes);
            return msg;
        } catch (InvalidProtocolBufferException e) {
        }
        OutputMessage.Builder builder = OutputMessage.newBuilder();
        return builder.build();
    }

    public static MultiOutputMessage json2MultiOutputMessage(String jsonData) {
        byte[] bytes = Base64.getDecoder().decode(jsonData);
        try {
            MultiOutputMessage msg = MultiOutputMessage.parseFrom(bytes);
            return msg;
        } catch (InvalidProtocolBufferException e) {
        }
        MultiOutputMessage.Builder builder = MultiOutputMessage.newBuilder();
        return builder.build();
    }

    public static double checkUsedMemory(MemoryUnitsType format) {
        double usedMemory = 0.;
        switch (format) {
            case B:
                usedMemory = (double) (Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory());
                break;
            case KB:
                usedMemory = (Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()) / 1024.;
                break;
            case MB:
                usedMemory = (Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()) / 1024. / 1024.;
                break;
            case GB:
                usedMemory = (Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()) / 1024. / 1024. / 1024.;
                break;
            case TB:
                usedMemory = (Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()) / 1024. / 1024. / 1024. / 1024.;
                break;
            default:
        }
        return usedMemory;
    }

    public static double checkMaxMemory() {
        return Runtime.getRuntime().maxMemory();
    }

    public static String mapToString(Map<String, ?> map) {
        String mapAsString = map.keySet().stream()
                .map(key -> key + "\003" + map.get(key))
                .collect(Collectors.joining("\002", "{", "}"));
        return mapAsString;
    }

    public static Map<String, String> stringToMap(String mapAsString, boolean hasPrefix, boolean hasSuffix) {
        int start = hasPrefix ? 1 : 0;
        int end = hasSuffix ? mapAsString.length() - 1 : mapAsString.length();
        Map<String, String> map = Arrays.stream(mapAsString.substring(start, end).split("\002"))
                .map(entry -> entry.split("\003"))
                .collect(Collectors.toMap(entry -> entry[0], entry -> entry[1]));
        return map;
    }

    public static String doubleArrayToString(double[] array, String delimiter) {
        String[] betaString = Arrays.stream(array).mapToObj(Double::toString).toArray(String[]::new);
        return String.join(delimiter, betaString);
    }

    public static String doubleArrayToString(double[] array) {
        return doubleArrayToString(array, "\004");
    }

    public static double[] stringToDoubleArray(String s, String delimiter) {
        // inverse function of Arrays.toString(double[])
        // check if string is singleton
        if (s.contains(delimiter)) {
            return Arrays.stream(s.substring(1, s.length() - 1).split(delimiter))
                    .mapToDouble(Double::parseDouble)
                    .toArray();
        } else {
            // singleton
            double[] res = new double[1];
            res[0] = Double.parseDouble(s);
            return res;
        }
    }

    public static double[] stringToDoubleArray(String s) {
        return stringToDoubleArray(s, "\004");
    }

    public static <T extends Comparable<? super T>> List<T> asSortedList(Collection<T> c) {
        List<T> list = new ArrayList<T>(c);
        java.util.Collections.sort(list);
        return list;
    }

    public static Matrix selectSubMatrix(Matrix matrix, List<Integer> sampleId, List<Integer> featureId) {
        Matrix.Builder matrixOrBuilder = Matrix.newBuilder();
        for (int id : sampleId) {
            Vector vec = matrix.getRows(id);
            Vector.Builder vectorOrBuilder = Vector.newBuilder();
            for (int fid : featureId) {
                vectorOrBuilder.addValues(vec.getValues(fid));
            }
            matrixOrBuilder.addRows(vectorOrBuilder);
        }
        return matrixOrBuilder.build();
    }

    public static Matrix selectSubMatrix(Matrix matrix, int[] sampleId, ArrayList<Integer> featureId) {
        Matrix.Builder matrixOrBuilder = Matrix.newBuilder();
        for (int id : sampleId) {
            Vector vec = matrix.getRows(id);
            Vector.Builder vectorOrBuilder = Vector.newBuilder();
            for (int fid : featureId) {
                vectorOrBuilder.addValues(vec.getValues(fid));
            }
            matrixOrBuilder.addRows(vectorOrBuilder);
        }
        return matrixOrBuilder.build();
    }


    public static String sampleIdToString(List<Integer> sampleId, Integer datasetSize) {
        double sampleRate = sampleId.size() / (double) datasetSize;
        if (sampleRate > 0.13) {
            char head = 0x01;
            return head + sampleIdToStringV1(sampleId);
        } else {
            char head = 0x03;
            return head + sampleIdToStringV3(sampleId);
        }
    }

    public static List<Integer> stringToSampleId(String str) throws NullPointerException {
        String head = str.substring(0, 1);
        String content = str.substring(1);
        if (head.charAt(0) == 0x01) {
            return stringToSampleIdV1(content);
        } else if (head.charAt(0) == 0x03) {
            return stringToSampleIdV3(content);
        } else {
            return new ArrayList<>();
        }
    }


    // new sample id serialization with less memory usage
    public static String sampleIdToStringV1(List<Integer> indicesArr) {
        // use string(), getBytes() to convert between byte[] and String
        // ref: https://stackoverflow.com/questions/88838/how-to-convert-strings-to-and-from-utf8-byte-arrays-in-java
        if (indicesArr == null) {
            return null;
        }

        int maxIdx = Collections.max(indicesArr);
        int messageSizeInByte = (int) Math.ceil((double) (maxIdx + 7) / 8);
        byte[] byteArr = new byte[messageSizeInByte];

        int indicesArrIdx = 0;
        for (int byteIdx = 0; byteIdx != messageSizeInByte; byteIdx++) {
            int value = 0;
            for (int bitIdx = 0; bitIdx < 8; bitIdx++) {
                // bit shift can only be done on int type in Java...
                // ref: https://stackoverflow.com/questions/3312853/how-does-bitshifting-work-in-java
                value = value << 1;
                if (indicesArrIdx < indicesArr.size()) {
                    int idxInBitStr = byteIdx * 8 + bitIdx;
                    int sampleidInIndices = indicesArr.get(indicesArrIdx);
                    if (idxInBitStr == sampleidInIndices) {
                        value = value | 1;
                        indicesArrIdx++;
                    }
                }
            }
            // force converting int type to byte by dropping the leading 3 bytes,
            // this does not affect the result as the leading 3 bytes should all be zeros.
            byteArr[byteIdx] = (byte) value;
        }
        //4，6 -> X 0 0 0 0 1 0 1 (5)   7, 8 -> X 1 1 0 0 0 0 0 (96）
        String encodedStr = new String(byteArr, StandardCharsets.ISO_8859_1); // this works only for positives
        return encodedStr;
    }

    // new sample id deserialization with less memory usage
    public static ArrayList<Integer> stringToSampleIdV1(String message) {
        ArrayList<Integer> indices = new ArrayList<>();
        if (message == null) {
            return indices;
        }

        byte[] byteArr = message.getBytes(StandardCharsets.ISO_8859_1);
        int[] base = {128, 64, 32, 16, 8, 4, 2, 1}; // Java reserves the first bit in byte for sign, which will be skipped here
        for (int byteIdx = 0; byteIdx != byteArr.length; byteIdx++) {
            int v = (int) byteArr[byteIdx];
            for (int bitIdx = 0; bitIdx != 8; bitIdx++) {
                int flg = v & base[bitIdx];
                if (flg != 0) {
                    indices.add(bitIdx + byteIdx * 8);
                }
            }
        }
        return indices;
    }

    public static String sampleIdToStringV3(List<Integer> indicesArr) {
        // This stores how many 0s are there before hit another 1
        // skip_msg = <skip_0><skip_1><skip_2>...<skip_k>, where each <skip> can take multiple bytes
        // inorder to separate them apart, we use a string of 001100 (<sep>) to represent.
        // further more, we will need to have <head> to store store the length of <sep> so that <sep> and <skip> can be separated
        // number of bits in <sep> < samples, thus we can set <head> to be 4 bytes. This supports up to 2^32 = 4 billion dataset size
        // the final encoded message is composed of: <head><sep><skip_0><skip_1><skip_2>...

        if (indicesArr == null) {
            return null;
        }
        Collections.sort(indicesArr);

        ArrayList<Integer> skipArr = new ArrayList<>();
        int prevIdx = -1;
        for (Integer curIdx : indicesArr) {
            skipArr.add(curIdx - prevIdx - 1);
            prevIdx = curIdx;
        }
        ArrayList<Byte> skipStrArr = new ArrayList<>();
        StringBuilder bitStr = new StringBuilder("");
        char flg = '0';

        for (Integer skip : skipArr) {
            int skipNum = (int) skip; // it could take more than one byte to store a single skip, but not likely to exceed max_int
            int numBytes = 0;
            if (skip == 0) {
                skipStrArr.add((byte) 0);
                numBytes++;
            } else {
                byte[] bb = ByteBuffer.allocate(4).putInt(skipNum).array(); //remove empty leading bytes
                boolean leadingZeroRemoved = false;
                for (int i = 0; i < 4; i++) {
                    if (bb[i] != 0 | leadingZeroRemoved) {
                        skipStrArr.add(bb[i]);
                        numBytes++;
                        leadingZeroRemoved = true;
                    }
                }
            }

            bitStr.append(StringUtils.repeat(flg, numBytes));
            if (flg == '0') {
                flg = '1';
            } else {
                flg = '0';
            }
        }

        int paddingSize = 8 - bitStr.length() % 8; // padding will be applied right most, less than 8 which can be stored in a byte
        bitStr.append(StringUtils.repeat('0', paddingSize));
        int bitCountInByte = bitStr.length() / 8;
        byte[] sepBytes = new byte[bitCountInByte];
        for (int i = 0; i < bitCountInByte; i++) {
            String bitsForByte = bitStr.substring(8 * i, 8 * (i + 1));
            sepBytes[i] = (byte) Integer.parseInt(bitsForByte, 2);
        }

        byte[] headBytes = ByteBuffer.allocate(4).putInt(bitCountInByte).array();
        String headStr = new String(headBytes, StandardCharsets.ISO_8859_1);
        byte paddingBytes = ByteBuffer.allocate(4).putInt(paddingSize).array()[3];
        String paddingStr = String.valueOf(paddingBytes); //TODO
        String sepStr = new String(sepBytes, StandardCharsets.ISO_8859_1);
        byte[] myArray = new byte[skipStrArr.size()];
        int j = 0;
        for (Byte b : skipStrArr) {
            myArray[j++] = b.byteValue();
        }
        String skipStr = new String(myArray, StandardCharsets.ISO_8859_1);//TODO

        String encodedStr = headStr + paddingStr + sepStr + skipStr;
//        ArrayList<Integer> s = stringToSampleIdV3(encodedStr);
        return encodedStr;
    }

    public static ArrayList<Integer> stringToSampleIdV3(String encodedStr) {
        ArrayList<Integer> indices = new ArrayList<>();
        if (encodedStr == null) {
            return indices;
        }

        // decode head
        String headStr = encodedStr.substring(0, 4);
        byte[] headArr = headStr.getBytes(StandardCharsets.ISO_8859_1);
        ByteBuffer headBytes = ByteBuffer.wrap(headArr);
        int sepLen = headBytes.getInt();

        // decode padding
        String paddingStr = encodedStr.substring(4, 5);
        int padding = (int) Integer.parseInt(paddingStr);
        int mask = 0;
        for (int i = 8; i > 0; i--) {
            mask = mask << 1;
            if (i > padding) {
                mask = mask | 1;
            }
        }

        // decode sep
        String sepStr = encodedStr.substring(5, 5 + sepLen);
        byte[] sepArr = sepStr.getBytes(StandardCharsets.ISO_8859_1);
        StringBuilder sepBuilder = new StringBuilder("");
        for (int i = 0; i < sepArr.length; i++) {
            int val;
            val = (sepArr[i] & 0xFF) + 0x100;
            if (i == sepArr.length - 1) { // remove paddings
                val = val & mask;
            }
            String s = Integer.toBinaryString(val & 0xff | 0x100).substring(1);
            sepBuilder.append(s);
        }
        String sep = sepBuilder.toString();
        sep = sep.substring(0, sep.length() - padding);
//        sep += StringUtils.repeat("0",sepBuilder.length() - sep.length());

        // decode skip
        int i = 0;
        ArrayList<Integer> bytesInSkipArr = new ArrayList<>();
        while (i < sep.length() - 1) {
            if (sep.charAt(i + 1) != sep.charAt(i)) {
                bytesInSkipArr.add(i + 1);
            }
            i++;
        }
        bytesInSkipArr.add(sep.length());

        String skipStr = encodedStr.substring(5 + sepLen);
        int prevIdx = 0;
        for (int idx = 0; idx < bytesInSkipArr.size(); idx++) {
            int curIdx = bytesInSkipArr.get(idx);
            String s = skipStr.substring(prevIdx, curIdx);
            int skipIdx = 0;
            for (int j = 0; j < s.length(); j++) {
                int v = (byte) s.charAt(j) & 0xFF; // convert signed byte to unsigned int8
                skipIdx = skipIdx << 8 | v;
            }

            Integer prev;
            if (indices.size() == 0) {
                prev = -1;
            } else {
                prev = indices.get(indices.size() - 1);
            }
            indices.add(skipIdx + prev + 1);
            prevIdx = curIdx;
        }

        return indices;
    }

    // convert SimpleMatrix to list of list
    public static List<List<Double>> SimpleMatrixToList(SimpleMatrix matrix) {
        List<List<Double>> res = new ArrayList();
        for (int rowNum = 0; rowNum < matrix.numRows(); rowNum++) {
            List<Double> row = new ArrayList<>();
            for (int colNum = 0; colNum < matrix.numCols(); colNum++) {
                row.add(matrix.get(rowNum, colNum));
            }
            res.add(row);
        }
        return res;
    }

    // convert matrix proto to list of list
    public static List<List<Double>> MatrixToList(Matrix matrix) {
        List<List<Double>> res = new ArrayList();
        for (int rowNum = 0; rowNum < matrix.getRowsCount(); rowNum++) {
//            List<Double> row = new ArrayList<>();
            Vector vec = matrix.getRows(rowNum);
            res.add(vec.getValuesList());
        }
        return res;
    }

    public static List<List<Double>> stringToListListDouble(String s) {
        String s1 = s.substring(2, s.length()-2);
        String[] s2 = s1.split("\\], \\[");
        List<List<Double>> res = new ArrayList<>();
        for (String si: s2) {
            List<Double> row = new ArrayList<>();
            for (String sk: si.split(", ")) {
                row.add(Double.parseDouble(sk));
            }
            res.add(row);
        }
        return res;
    }


}
