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

package com.jdt.fedlearn.core.model.common.tree.sampling;


import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;


public class RowSampler implements Serializable {
    public ArrayList<Double> row_mask = new ArrayList<>();

    public RowSampler(int n, double sampling_rate) {
        for (int i = 0; i < n; i++) {
            this.row_mask.add(Math.random() <= sampling_rate ? 1.0 : 0.0);
        }
    }

    public void shuffle() {
        Collections.shuffle(this.row_mask);
    }

    public ArrayList<Double> getRow_mask() {
        return row_mask;
    }

    public void setRow_mask(ArrayList<Double> row_mask) {
        this.row_mask = row_mask;
    }
}
