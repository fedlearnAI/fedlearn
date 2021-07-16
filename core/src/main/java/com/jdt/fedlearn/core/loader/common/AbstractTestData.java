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

package com.jdt.fedlearn.core.loader.common;

import java.util.Arrays;

public abstract class AbstractTestData implements TestData {
    protected double[][] sample;
    protected String[] featureName;
    protected String[] uid;
    private double[] label;
    protected int featureDim;
    protected int datasetSize;

    public void scan(String[][] rawTable, String labelName) {
        datasetSize = rawTable.length - 1;
        featureDim = rawTable[0].length - 2;

        featureName = new String[featureDim];
        uid = new String[datasetSize];
        sample = new double[datasetSize][featureDim];

        featureName = Arrays.stream(rawTable[0]).filter(x -> (!"uid".equals(x) && !x.equals(labelName))).toArray(String[]::new);
        for (int row = 0; row < datasetSize; row++) {
            String[] strs = rawTable[row + 1];
            uid[row] = strs[0];
            for (int col = 1; col < featureDim; col++) {
                if ("".equals(strs[col])) {
                    sample[row][col] = NULL;
                } else {
                    sample[row][col] = Double.parseDouble(strs[col]);
                }
            }
        }
    }

    public double[][] getSample() {
        return this.sample;
    }

    public String[] getFeatureName() {
        return featureName;
    }

    public String[] getUid() {
        return uid;
    }

    public double[] getLabel() {
        return label;
    }

    public void setLabel(double[] label) {
        this.label = label;
    }

    public int getFeatureDim() {
        return featureDim;
    }

    public int getDatasetSize() {
        return datasetSize;
    }

}
