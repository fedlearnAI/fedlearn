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

package com.jdt.fedlearn.core.loader.randomForest;


import com.jdt.fedlearn.core.loader.common.AbstractInferenceData;
import org.ejml.simple.SimpleMatrix;

import java.util.*;
import java.util.stream.Collectors;

public class RFInferenceData extends AbstractInferenceData {
//    private ArrayList<String> headers = new ArrayList<>();
//    private ArrayList<ArrayList<Double>> content = new ArrayList<>();

    public RFInferenceData(String[][] rawTable) {
        super.scan(rawTable);
    }

    public String[][] getUidFeature() {
        String[][] uidFeature = new String[datasetSize][featureDim + 1];
        for (int i = 0; i < datasetSize; i++) {
            String[] line = new String[featureDim + 1];
            line[0] = uid[i];
            for (int j = 0; j < featureDim; j++) {
                line[j + 1] = String.valueOf(sample[i][j]);
            }
            uidFeature[i] = line;
        }
        return uidFeature;
    }
}
