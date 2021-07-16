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

/**
 *
 */
public interface FeatureProcess {
    /**
     * Transform a feature vector.
     *
     * @param vector a feature vector.
     * @return the transformed feature value.
     */
    double[] transform(double[] vector);

    /**
     * Transform a data frame.
     *
     * @param matrix 2 dim matrix.
     * @return the transformed data frame.
     */
    default double[][] transform(double[][] matrix) {
        int n = matrix.length;
        double[][] y = new double[n][];
        for (int i = 0; i < n; i++) {
            y[i] = transform(matrix[i]);
        }
        return y;
    }


}
