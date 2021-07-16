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

import com.jdt.fedlearn.grpc.federatedlearning.InputMessage;
import com.jdt.fedlearn.grpc.federatedlearning.Matrix;
import com.jdt.fedlearn.grpc.federatedlearning.MultiInputMessage;
import com.jdt.fedlearn.grpc.federatedlearning.OutputMessage;
import org.ejml.simple.SimpleMatrix;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public interface RandomForestEncryptData {
    // 随机森林数据接口

    void loadY(InputMessage Y);

    InputMessage getEncryptedY();

    InputMessage getSubY(ArrayList<Integer> sampleId);

    MultiInputMessage getSubY(ArrayList<Integer>[] sampleIds);

    InputMessage prepareInputMessagePhase2Passive(Matrix X,
                                                  int numPercentiles,
                                                  InputMessage subY);

    MultiInputMessage prepareInputMessagePhase2Passive(HashMap<Integer, ArrayList<Integer>> tidToSampleID,
                                                      ArrayList<Integer>[] featureIds,
                                                      Matrix XTrain,
                                                      int numPercentiles,
                                                      MultiInputMessage subYs);

    Matrix parsePassivePhase2(OutputMessage responsePhase2);
}
