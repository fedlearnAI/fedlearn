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

package com.jdt.fedlearn.core.preprocess;


import com.jdt.fedlearn.core.loader.common.Data;
import com.jdt.fedlearn.core.math.MathExt;

import java.util.Arrays;

/**
 * 缺失值填充，提供多种填充方法
 */
public class MissingValueFilling {
    private double[][] table;

    public MissingValueFilling(double[][] table) {
        this.table = table;
    }

    //全部按照平均值填充缺失值
    public void avgFilling() {
        int size = table.length;
        int width = table[0].length;
        double[] avg = new double[width];
        double[][] trans = MathExt.transpose(table);
        for (int i = 0; i < trans.length; i++) {
            trans[i] = Arrays.stream(trans[i]).filter(x -> !Double.isNaN(x)).toArray();
            avg[i] = MathExt.average(trans[i]);
        }
        //求每列的均值
//        String linRNilString = Data.NULL_STRING;
        for (int i = 0; i < size; i++) {
            for (int j = 0; j < width; j++) {
                if (Double.isNaN(table[i][j])) {
                    table[i][j] = (avg[j]);
                }
            }
        }
    }


}
