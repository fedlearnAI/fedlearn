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

package com.jdt.fedlearn.core.loader.boost;

import com.jdt.fedlearn.core.model.common.tree.sampling.ColSampler;
import com.jdt.fedlearn.core.model.common.tree.sampling.RowSampler;
import com.jdt.fedlearn.core.entity.feature.Features;
import com.jdt.fedlearn.core.loader.common.AbstractTrainData;
import com.jdt.fedlearn.core.loader.common.TrainData;

import java.util.*;

/**
 * 数据的加载，和加载后的简单处理
 * 除了构建阶段
 * 用户数据的所有内容均只读
 * 原始数据分为两种类型，数值型和类别型
 */
public class BoostTrainData extends AbstractTrainData implements TrainData {
    //原始数据，列式存储,即每行一个特征，每列一个样本
    private final double[][] column_sample;
    //非数值型列名和非数值型列id
    private final List<String> catFeaturesNames;
    private RowSampler rowSampler;
    private ColSampler colSampler;

    public BoostTrainData(String[][] rawTable, String[] idMap, Features features, List<String> categoricalFeatures) {
        this.catFeaturesNames = categoricalFeatures;

        super.scan(rawTable, idMap, features);
        //特征维度
        column_sample = columnTrans(sample);
        rowSampler = new RowSampler(0,0);
        colSampler = new ColSampler(0,0);
    }

    //instances 样本实例id，col 列索引，传入的col 从
    public double[][] getFeature(int[] instances, int col) {
        double[][] res = new double[instances.length][2];
        //TODO 后续每个feature均包含id列，然后从id列开始计数
        //column_samples 竖排样本，
        double[] column = column_sample[col - 1];
        for (int j = 0; j < instances.length; j++) {
            res[j][0] = instances[j];
            res[j][1] = column[instances[j]];
        }
        return res;
    }


    public RowSampler getRowSampler() {
        return rowSampler;
    }

    public ColSampler getColSampler() {
        return colSampler;
    }
}
