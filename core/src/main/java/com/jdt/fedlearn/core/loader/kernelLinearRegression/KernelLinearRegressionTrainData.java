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

package com.jdt.fedlearn.core.loader.kernelLinearRegression;


import com.jdt.fedlearn.common.entity.core.feature.Features;
import com.jdt.fedlearn.core.loader.common.AbstractTrainData;
import com.jdt.fedlearn.core.loader.common.TrainData;
import com.jdt.fedlearn.core.preprocess.MissingValueFilling;

import java.util.List;


public class KernelLinearRegressionTrainData extends AbstractTrainData implements TrainData {
    private final double[][] feature;

    public KernelLinearRegressionTrainData(String[][] rawTable, String[] idMap, Features features) {
        super.scan(rawTable, idMap, features);
        if (expressions != null && expressions.size() != 0) {
            super.featureProcessing(expressions);
        }
        this.feature = super.sample;
        MissingValueFilling filling = new MissingValueFilling(feature);
        if (feature.length > 0) {
            filling.avgFilling();
        }
    }

    public double[][] getFeature() {
        return feature;
    }

}
