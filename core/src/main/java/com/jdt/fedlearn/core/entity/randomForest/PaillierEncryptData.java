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

import com.jdt.fedlearn.core.encryption.*;
import com.jdt.fedlearn.grpc.federatedlearning.*;
import com.n1analytics.paillier.EncryptedNumber;
import com.n1analytics.paillier.PaillierPrivateKey;
import com.n1analytics.paillier.PaillierPublicKey;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

public class PaillierEncryptData implements RandomForestEncryptData {

    // label
    private Vector yVec;
    private PaillierVector yPvec;

    private Encryptor encryptor;
    private Decryptor decryptor;

    private PaillierKeyPublic keyPublic = null;

    private PaillierKeyPublic[] keyPublics;


    // Todo 加入测试集

    // 主动方构造函数
    public PaillierEncryptData(Vector yVec,
                               int encryptionCertainty) {
        this.yVec = yVec;
        PaillierPrivateKey privateKey = PaillierPrivateKey.create(encryptionCertainty);
        PaillierPublicKey publicKey = privateKey.getPublicKey();
//        PaillierUtil.privateKeyToFile(privateKey, PRIVATE_KEY_FILE);
//        PaillierUtil.publicKeyToFile(publicKey, PUBLIC_KEY_FILE);
        // 生成一个key
        encryptor = new Encryptor(publicKey);
        decryptor = new Decryptor(privateKey);
        keyPublic = DataUtils.paillierPublicKeyToRpcProto(encryptor.getPaillierPublicKey());
        ArrayList<EncryptedNumber> yEnc = new ArrayList<>(Arrays.asList(DataUtils.PaillierEncrypt(yVec, encryptor, false)));
        yPvec = DataUtils.toPaillierVector(yEnc);
    }

    // 被动方构造函数
    public PaillierEncryptData() {
        this.yVec = null;
        this.keyPublic = null;
        this.yPvec = null;
    }

    // master 构造函数
    public PaillierEncryptData(InputMessage encryptY) {
        this.yPvec = encryptY.getPailliervectors(0);
        this.keyPublic = encryptY.getPaillierkeypublic();
    }

    public void loadY(InputMessage Y) {
        yPvec = Y.getPailliervectors(0);
        keyPublic = Y.getPaillierkeypublic();
    }

    public InputMessage getEncryptedY() {
        return DataUtils.prepareInputMessage(
                new Matrix[]{},
                new Vector[]{},
                new Double[]{},
                new PaillierMatrix[]{},
                new PaillierVector[]{yPvec},
                new PaillierValue[]{},
                keyPublic);
    }

    public InputMessage getSubY(ArrayList<Integer> sampleId) {
        PaillierVector.Builder subYBuilder = PaillierVector.newBuilder();
        for (int i : sampleId) {
            subYBuilder.addValues(yPvec.getValues(i));
        }
        PaillierVector subYPvec = subYBuilder.build();
        return DataUtils.prepareInputMessage(
                new Matrix[]{},
                new Vector[]{},
                new Double[]{},
                new PaillierMatrix[]{},
                new PaillierVector[]{subYPvec},
                new PaillierValue[]{},
                keyPublic);
    }

    public MultiInputMessage getSubY(ArrayList<Integer>[] sampleIds) {
        PaillierVector[][] subYPvec = new PaillierVector[sampleIds.length][1];
//        PaillierKeyPublic[] keyPublics = new PaillierKeyPublic[sampleIds.length];
        for (int i = 0; i < sampleIds.length; i++) {
            PaillierVector.Builder subYBuilder = PaillierVector.newBuilder();
            for (int idx : sampleIds[i]) {
                subYBuilder.addValues(yPvec.getValues(idx));
            }
            subYPvec[i][0] = subYBuilder.build();
        }
        keyPublics = new PaillierKeyPublic[sampleIds.length];
        Arrays.fill(keyPublics, keyPublic);
        PaillierKeyPublic[] localkeyPublics = new PaillierKeyPublic[sampleIds.length];
        Arrays.fill(localkeyPublics, keyPublic);
        return DataUtils.prepareMultiInputMessage(
                new Matrix[][]{},
                new Vector[][]{},
                new Double[][]{},
                new PaillierMatrix[][]{},
                subYPvec,
                new PaillierValue[][]{},
                localkeyPublics,
                sampleIds.length);
    }

    // single process
    public InputMessage prepareInputMessagePhase2Passive(Matrix X,
                                                         int numPercentiles,
                                                         InputMessage subY) {
        PaillierVector subYPvec = subY.getPailliervectors(0);
        keyPublic = subY.getPaillierkeypublic();
        // safety check
        assert X.getRowsCount() == subYPvec.getValuesCount() : "X shape is different than y!";
        return DataUtils.prepareInputMessage(
                new Matrix[]{X},
                new Vector[]{},
                new Double[]{(double) numPercentiles},
                new PaillierMatrix[]{},
                new PaillierVector[]{subYPvec},
                new PaillierValue[]{},
                keyPublic);
    }

    // multi process
    public MultiInputMessage prepareInputMessagePhase2Passive(HashMap<Integer, ArrayList<Integer>> tidToSampleId,
                                                              ArrayList<Integer>[] featureIds,
                                                              Matrix XTrain,
                                                              int numPercentiles,
                                                              MultiInputMessage subYs) {
        int num_trees = tidToSampleId.size();
        Double[][] scalars = new Double[num_trees][1];
        Matrix[][] matrices = new Matrix[num_trees][1];

        PaillierVector[][] paillierVectors = new PaillierVector[num_trees][1];
        PaillierKeyPublic[] keyPublics = new PaillierKeyPublic[num_trees];
        // 第一次收到加密的Y的时候需要收集 publicKey
        if (keyPublic == null) {
            keyPublic = subYs.getMessages(0).getPaillierkeypublic();
        }
//        for (int i = 0; i<num_trees; i++) {
//            int treeIdi = Integer.valueOf(treeIds[i]);
//            ArrayList<Integer> sampleIdi = new ArrayList<>(Arrays.stream(sampleIds[i].substring(1, sampleIds[i].length()-1).split(", "))
//                    .map(Integer::valueOf).collect(Collectors.toList()));
        int i = 0;
        for (HashMap.Entry<Integer, ArrayList<Integer>> entry : tidToSampleId.entrySet()) {
            int treeIdi = entry.getKey().intValue();
            ArrayList<Integer> sampleIdi = entry.getValue();
            Matrix X = DataUtils.selectSubMatrix(XTrain, sampleIdi, featureIds[treeIdi]);
            PaillierVector subYPvec = subYs.getMessages(i).getPailliervectors(0);
            matrices[i][0] = X;
            scalars[i][0] = (double) numPercentiles;
            paillierVectors[i][0] = subYPvec;
            keyPublics[i] = subYs.getMessages(i).getPaillierkeypublic();
            i++;
        }
        return DataUtils.prepareMultiInputMessage(
                matrices,
                new Vector[][]{},
                scalars,
                new PaillierMatrix[][]{},
                paillierVectors,
                new PaillierValue[][]{},
                keyPublics,
                num_trees);
    }

    public Matrix parsePassivePhase2(OutputMessage responsePhase2) {
        PaillierMatrix Y1Enc = responsePhase2.getPailliermatrices(0);
        return DataUtils.PaillierDecryptParallel(Y1Enc, decryptor, keyPublic);
    }

    public PaillierKeyPublic getKeyPublic() {
        return keyPublic;
    }

    public void setKeyPublic(PaillierKeyPublic keyPublic) {
        this.keyPublic = keyPublic;
    }

}
