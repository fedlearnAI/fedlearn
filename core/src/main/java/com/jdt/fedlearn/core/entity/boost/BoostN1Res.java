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

import com.jdt.fedlearn.core.model.common.tree.Tree;
import com.jdt.fedlearn.common.entity.core.Message;

import java.util.List;

public class BoostN1Res implements Message {
    private final List<Tree> trees;
    private final double firstRoundPred;
    private final List<Double> multiClassUniqueLabelList;

    public BoostN1Res() {
        this.trees = null;
        this.firstRoundPred = 0.0;
        this.multiClassUniqueLabelList = null;
    }

    public BoostN1Res(List<Tree> trees, double firstRoundPred, List<Double> multiClassUniqueLabelList) {
        this.trees = trees;
        this.firstRoundPred = firstRoundPred;
        this.multiClassUniqueLabelList = multiClassUniqueLabelList;
    }

    public List<Tree> getTrees() {
        return trees;
    }

    public List<Double> getMultiClassUniqueLabelList() {
        return multiClassUniqueLabelList;
    }

    public double getFirstRoundPred() {
        return firstRoundPred;
    }
}
