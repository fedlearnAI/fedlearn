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

package com.jdt.fedlearn.core.loader.linearRegression;

import com.jdt.fedlearn.common.entity.core.feature.Features;
import com.jdt.fedlearn.core.loader.common.AbstractTrainData;
import com.jdt.fedlearn.core.math.MathExt;
import com.jdt.fedlearn.core.preprocess.Scaling;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;


public class LinearTrainData extends AbstractTrainData{
    private double[][] feature;
    public static final double NIL = Double.NEGATIVE_INFINITY;
    private ArrayList<Integer> missingCount = new ArrayList<>();
    public ArrayList<Integer> catFeaturesCols = new ArrayList<>();
    private int feature_dim, dataset_size;

    public LinearTrainData(String[][] rawTable, Features features) {

        Map<Long, String> fakeIdMap = makeFakeIDMap(rawTable, features);
        super.scan(rawTable, fakeIdMap.values().toArray(new String[0]), features);

        //特征维度
        feature_dim = super.featureDim;
        dataset_size = super.datasetSize;
        feature = super.sample;

        // TODO: Missing value filling not implemented.
//        MissingValueFilling filling = new MissingValueFilling(rawTable);
//        filling.avgFilling();

        Scaling scaling = new Scaling(feature);
        scaling.minMaxScaling(0, 1); // 由于当前的精度为0.01，最好将数值归一化到0~100

    }

    private Map<Long, String> makeFakeIDMap(String[][] rawTable, Features features) {
        Map<Long, String> idMap = new HashMap<>();
        String[][] transTable = MathExt.transpose(rawTable);
        for (String[] line : transTable) {
            if (features.isIndex(line[0])) {
                for (long i = 1; i < line.length; i++) {
                        idMap.put(i-1, line[(int) i]);
                }
                break;
            }
        }
        return idMap;
    }

    public double[][] getFeature() {
        return feature;
    }

    public String[] getFeatureName() {
        return featureName;
    }

    @Override
    public double[] getLabel() {
        return label;
    }

    @Override
    public String[] getUid() {
        return uid;
    }

    @Override
    public int getFeatureDim() {
        return featureDim;
    }

    @Override
    public int getDatasetSize() {
        return datasetSize;
    }

    public boolean isHasLabel() {
        return hasLabel;
    }

    public static double getNIL() {
        return NIL;
    }

    public ArrayList<Integer> getMissingCount() {
        return missingCount;
    }

    public ArrayList<Integer> getCatFeaturesCols() {
        return catFeaturesCols;
    }

    public int getFeature_dim() {
        return feature_dim;
    }

    public int getDataset_size() {
        return dataset_size;
    }

}
