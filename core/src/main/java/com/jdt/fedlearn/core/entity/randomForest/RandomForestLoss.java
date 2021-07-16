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

import org.ejml.simple.SimpleMatrix;

import java.io.Serializable;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * 随机森林 loss 类
 * 暂时将所有的loss对应的bagging y的方法放到一起
 */
public class RandomForestLoss implements Serializable {

    // "Regression:MSE" or "Classification:Cross entropy"

    private static final String[] supportLossTypeList = new String[]{
            "Regression:MSE", "Classification:Cross entropy"};
    public static final Set<String> supportLossType = new HashSet<>(Arrays.asList(supportLossTypeList));

    public RandomForestLoss(String lossType) {
        if (!supportLossType.contains(lossType)) {
            throw new IllegalArgumentException("Unsupported loss type!");
        }
    }

    public double bagging(SimpleMatrix y) {
        double yBagging = 0;
        yBagging = DataUtils.mean(y);
        return yBagging;
    }

    // get loss type id for random forest phase 3
    public double getLossTypeId() {
        return 1;
    }
}
