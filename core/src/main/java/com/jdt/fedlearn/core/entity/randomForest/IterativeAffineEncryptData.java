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

import com.jdt.fedlearn.core.encryption.IterativeAffine.IterativeAffineCiphertext;
import com.jdt.fedlearn.core.encryption.IterativeAffine.IterativeAffineKey;
import com.jdt.fedlearn.core.encryption.IterativeAffine.IterativeAffineTool;
import com.jdt.fedlearn.grpc.federatedlearning.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

public class IterativeAffineEncryptData implements RandomForestEncryptData {

    // label
    private Vector yVec;
    private IterativeAffineVector yPvec;

    // encryptor
    private IterativeAffineKey key;


    // Todo 加入测试集

    // 主动方构造函数
    public IterativeAffineEncryptData(Vector yVec,
                                      int encryptionCertainty) {
        this.yVec = yVec;

        // 生成一个key
        key = IterativeAffineTool.generateKeyPair(encryptionCertainty);
        ArrayList<IterativeAffineCiphertext> yEnc = new ArrayList<>(Arrays.asList(DataUtils.IterativeAffineEncrypt(yVec, key)));
        yPvec = DataUtils.toIterativeAffineVector(yEnc);
    }

    // 被动方构造函数
    public IterativeAffineEncryptData() {
        this.yVec = null;
        this.key = null;
        this.yPvec = null;
    }

    // master 构造函数
    public IterativeAffineEncryptData(InputMessage encryptY) {
        this.yPvec = encryptY.getIterativeaffinevectors(0);
    }

    public void loadY(InputMessage Y) {
        yPvec = Y.getIterativeaffinevectors(0);
    }

    public InputMessage getEncryptedY() {
        return DataUtils.prepareIterativeAffineInputMessage(
                new Matrix[]{},
                new Vector[]{},
                new Double[]{},
                new IterativeAffineMatrix[]{},
                new IterativeAffineVector[]{yPvec},
                new IterativeAffineValue[]{});
    }

    public InputMessage getSubY(ArrayList<Integer> sampleId) {
        IterativeAffineVector.Builder subYBuilder = IterativeAffineVector.newBuilder();
        for (int i : sampleId) {
            subYBuilder.addValues(yPvec.getValues(i));
        }
        IterativeAffineVector subYPvec = subYBuilder.build();
        return DataUtils.prepareIterativeAffineInputMessage(
                new Matrix[]{},
                new Vector[]{},
                new Double[]{},
                new IterativeAffineMatrix[]{},
                new IterativeAffineVector[]{subYPvec},
                new IterativeAffineValue[]{});
    }

    public MultiInputMessage getSubY(ArrayList<Integer>[] sampleIds) {
        IterativeAffineVector[][] subYPvec = new IterativeAffineVector[sampleIds.length][1];
        for (int i = 0; i < sampleIds.length; i++) {
            IterativeAffineVector.Builder subYBuilder = IterativeAffineVector.newBuilder();
            for (int idx : sampleIds[i]) {
                subYBuilder.addValues(yPvec.getValues(idx));
            }
            subYPvec[i][0] = subYBuilder.build();
        }
        return DataUtils.prepareMultiIterativeAffineInputMessage(
                new Matrix[][]{},
                new Vector[][]{},
                new Double[][]{},
                new IterativeAffineMatrix[][]{},
                subYPvec,
                new IterativeAffineValue[][]{},
                sampleIds.length);
    }

    // single process
    public InputMessage prepareInputMessagePhase2Passive(Matrix X,
                                                         int numPercentiles,
                                                         InputMessage subY) {
        IterativeAffineVector subYPvec = subY.getIterativeaffinevectors(0);
        // safety check
        assert X.getRowsCount() == subYPvec.getValuesCount() : "X shape is different than y!";
        return DataUtils.prepareIterativeAffineInputMessage(
                new Matrix[]{X},
                new Vector[]{},
                new Double[]{(double) numPercentiles},
                new IterativeAffineMatrix[]{},
                new IterativeAffineVector[]{subYPvec},
                new IterativeAffineValue[]{});
    }

    // multi process
    public MultiInputMessage prepareInputMessagePhase2Passive(HashMap<Integer, ArrayList<Integer>> tidToSampleID,
                                                              ArrayList<Integer>[] featureIds,
                                                              Matrix XTrain,
                                                              int numPercentiles,
                                                              MultiInputMessage subYs) {
        int numTrees = tidToSampleID.size();
        Double[][] scalars = new Double[numTrees][1];
        Matrix[][] matrices = new Matrix[numTrees][1];
        IterativeAffineVector[][] iterativeAffineVectors = new IterativeAffineVector[numTrees][1];
        int i = 0;
        for (HashMap.Entry<Integer, ArrayList<Integer>> entry : tidToSampleID.entrySet()) {
            int treeIdi = entry.getKey().intValue();
            ArrayList<Integer> sampleIdi = entry.getValue();
            Matrix X = DataUtils.selectSubMatrix(XTrain, sampleIdi, featureIds[treeIdi]);
            IterativeAffineVector subYPvec = subYs.getMessages(i).getIterativeaffinevectors(0);
            matrices[i][0] = X;
            scalars[i][0] = (double) numPercentiles;
            iterativeAffineVectors[i][0] = subYPvec;
            i = i + 1;
        }
        return DataUtils.prepareMultiIterativeAffineInputMessage(
                matrices,
                new Vector[][]{},
                scalars,
                new IterativeAffineMatrix[][]{},
                iterativeAffineVectors,
                new IterativeAffineValue[][]{},
                numTrees);
    }

    public Matrix parsePassivePhase2(OutputMessage responsePhase2) {
        IterativeAffineMatrix Y1Enc = responsePhase2.getIterativeaffinematrices(0);
        return DataUtils.IterativeAffineDecrypt(Y1Enc, key);
    }

}
