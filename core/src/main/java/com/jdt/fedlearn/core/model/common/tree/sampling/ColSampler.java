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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ColSampler {

    private List<Integer> cols = new ArrayList<>();
    private List<Integer> colSelected;
    private int nSelected;

    public ColSampler(int n, double sampling_rate) {
        for (int i = 0; i < n; i++) {
            cols.add(i);
        }
        nSelected = (int) (n * sampling_rate);
        colSelected = cols.subList(0, nSelected);
    }

    public void shuffle() {
        Collections.shuffle(cols);
        colSelected = cols.subList(0, nSelected);
    }

    public List<Integer> getCols() {
        return cols;
    }

    public void setCols(List<Integer> cols) {
        this.cols = cols;
    }

    public List<Integer> getColSelected() {
        return colSelected;
    }

    public void setColSelected(List<Integer> colSelected) {
        this.colSelected = colSelected;
    }

    public int getnSelected() {
        return nSelected;
    }

    public void setnSelected(int nSelected) {
        this.nSelected = nSelected;
    }
}