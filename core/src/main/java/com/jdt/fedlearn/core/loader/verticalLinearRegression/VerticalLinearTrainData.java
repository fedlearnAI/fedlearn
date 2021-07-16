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

package com.jdt.fedlearn.core.loader.verticalLinearRegression;

import com.jdt.fedlearn.core.loader.common.AbstractTrainData;
import com.jdt.fedlearn.core.preprocess.MissingValueFilling;
import com.jdt.fedlearn.core.preprocess.Scaling;
import com.jdt.fedlearn.core.entity.feature.Features;
import com.jdt.fedlearn.core.loader.common.TrainData;


import java.util.*;

public class VerticalLinearTrainData extends AbstractTrainData implements TrainData {
    private double[][] feature;
    private Scaling scaling = new Scaling();
    private double[] label;

    public VerticalLinearTrainData(String[][] rawTable, String[] idMap, Features features) {
        super.scan(rawTable, idMap, features);
        this.feature = super.sample;

        if (hasLabel) {
            this.label = super.label;
        }
        /**
         * TODO:
         *  1) add feature transformation
         *  2) fill empty data with NIL
         */
        MissingValueFilling filling = new MissingValueFilling(feature);
        if(feature.length > 0) {
            scaling.minMaxScaling(0, 1, feature);
            filling.avgFilling();
        }
    }

    public Scaling getScaling() {
        return scaling;
    }

    public double[][] getFeature() {
        return feature;
    }

    public double[] getLabel() {
        return label;
    }

}
