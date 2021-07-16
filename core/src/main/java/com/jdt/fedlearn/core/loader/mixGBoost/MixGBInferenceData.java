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

package com.jdt.fedlearn.core.loader.mixGBoost;

import com.jdt.fedlearn.core.loader.common.AbstractInferenceData;
import com.jdt.fedlearn.core.util.Tool;

import java.util.*;
import java.util.stream.Collectors;

public class MixGBInferenceData extends AbstractInferenceData {
    private final Map<String, Integer> idIndexMap;

    public MixGBInferenceData(String[][] rawTable) {
        super.scan(rawTable);
        idIndexMap = new HashMap<>();
        for (int i = 0; i < datasetSize; i++) {
            if (idIndexMap.containsKey(uid[i])) {
                continue;
            }
            idIndexMap.put(uid[i], i);
        }
    }

    public void computeUidIndex(String[] partUid) {
        fakeIdIndex = Arrays.stream(partUid).parallel().mapToInt(id -> idIndexMap.getOrDefault(id, -1)).toArray();
    }

    public double getInstanceFeatureValue(String instId, String fname) {
        if (!idIndexMap.containsKey(instId)) {
            return Double.MAX_VALUE;
        }
        int columnIndex = 0;
        while (columnIndex < featureName.length && !(fname.equals(featureName[columnIndex]))) {
            columnIndex++;
        }
        if (columnIndex == featureName.length) {
            return Double.MAX_VALUE;
        }
        return sample[idIndexMap.get(instId)][columnIndex];
    }

    public double[] getInstanceFeatureValue(String[] instIds, String fname) {
        int columnIndex = 0;
        while (columnIndex < featureName.length && !(fname.equals(featureName[columnIndex]))) {
            columnIndex++;
        }
        if (columnIndex == featureName.length) {
            double[] res = new double[instIds.length];
            Arrays.fill(res, Double.MAX_VALUE);
            return res;
        }
        int finalColumnIndex = columnIndex;
        return Arrays.stream(instIds).parallel().mapToDouble(instId -> {
            if (idIndexMap.containsKey(instId)) {
                return sample[idIndexMap.get(instId)][finalColumnIndex];
            } else {
                return Double.MAX_VALUE;
            }
        }).toArray();
    }

    public Set<String> getLeftInstance(Set<String> instIds, String fname, double value) {
        int columnIndex = 0;
        while (columnIndex < featureName.length && !(fname.equals(featureName[columnIndex]))) {
            columnIndex++;
        }
        if (columnIndex == featureName.length) {
            return new HashSet<>();
        }
        int finalColumnIndex = columnIndex;
        return instIds.parallelStream().filter(instId ->
                idIndexMap.containsKey(instId) && Tool.compareDoubleValue(sample[idIndexMap.get(instId)][finalColumnIndex], value) <= 0)
                .collect(Collectors.toSet());
    }

    public Map<String, Integer> getIdIndexMap() {
        return idIndexMap;
    }
}
