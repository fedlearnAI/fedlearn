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

import com.jdt.fedlearn.core.preprocess.Scaling;
import com.jdt.fedlearn.core.loader.common.AbstractInferenceData;

import java.util.Arrays;


public class LinearInferenceData extends AbstractInferenceData {
    private String[][] rawTable;
    private double[] groudtruth;
    private boolean hasGroundTruth;
    protected double[] groundTruth;

    public LinearInferenceData(String[][] rawTable, String labelName) {
        this.rawTable = rawTable;

        this.hasGroundTruth = labelName != null;
        if(hasGroundTruth) {
            this.scan(rawTable, labelName);
        } else {
            super.scan(rawTable);
        }
//        sample = new double[0][];
        if(sample != null && sample.length!=0) {
            Scaling scaling = new Scaling(sample);
            scaling.minMaxScaling(0, 1);
        }

        this.uid = super.getUid();
        if(hasGroundTruth) {
            groudtruth = this.groundTruth;
        }
    }

    /*
    Linreg infer数据加载。
    由于在测试时需要groundtruth， 添加labelName。默认uid 在测试数据的第一列，groundtruth在测试数据的最后一列。
    */
    public void scan(String[][] rawTable, String labelName) {
        datasetSize = rawTable.length - 1;
        featureDim = rawTable[0].length - 2; // feature 去除第一列和最后一列

        featureName = new String[featureDim];
        uid = new String[datasetSize];
        groundTruth = new double[datasetSize];
        sample = new double[datasetSize][featureDim];

        featureName = Arrays.stream(rawTable[0]).filter(x -> !("uid".equals(x) || labelName.equals(x)) ).toArray(String[]::new);
        for (int row = 0; row < datasetSize; row++) {
            String[] strs = rawTable[row + 1];
            uid[row] = strs[0];
            for (int col = 0; col < featureDim; col++) {
                if (null == strs[col + 1] || strs[col + 1].isEmpty()) {
                    sample[row][col] = NULL;
                } else {
                    sample[row][col] = Double.parseDouble(strs[col + 1]);
                }
            }
            groundTruth[row] = Double.parseDouble(strs[featureDim + 1]);
        }
    }

    public String[][] getRawTable() {
        return rawTable;
    }

    public void setRawTable(String[][] rawTable) {
        this.rawTable = rawTable;
    }

    public double[][] getX_inference() {
        return sample;
    }

    public void setX_inference(double[][] sample) {
        this.sample = sample;
    }

    public double[] getGroudtruth() {
        return groudtruth;
    }

    public boolean isHasGroundTruth() {
        return hasGroundTruth;
    }

    public void setHasGroundTruth(boolean hasGroundTruth) {
        this.hasGroundTruth = hasGroundTruth;
    }
}
