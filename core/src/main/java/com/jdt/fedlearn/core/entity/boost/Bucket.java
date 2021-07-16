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

package com.jdt.fedlearn.core.entity.boost;


import com.jdt.fedlearn.core.math.MathExt;

import java.util.Arrays;

/**
 * bucket 为内部对象，不用于对外传输
 */
public class Bucket {
    //id 和value 一一对应
    private final double[] ids;
    private final double[] values;
    //split value 尝试了多种值，包括value的平均值，中位数，最大值等
    private final double splitValue;

    public Bucket(double[] ids, double[] values) {
        this.ids = ids;
        this.values = values;
        this.splitValue = MathExt.max(values);
    }

    public Bucket(double[][] mat) {
        ids = new double[mat.length];
        values = new double[mat.length];
        for (int i = 0; i < mat.length; i++) {
            ids[i] = mat[i][0];
            values[i] = mat[i][1];
        }
        this.splitValue = MathExt.max(values);
    }

    public double[] getIds() {
        return ids;
    }

    public double[] getValues() {
        return values;
    }

    public double getSplitValue() {
        return splitValue;
    }

    @Override
    public String toString() {
        return "Bucket{" +
                "ids=" + Arrays.toString(ids) +
                ", values=" + Arrays.toString(values) +
                ", splitValue=" + splitValue +
                '}';
    }
}
