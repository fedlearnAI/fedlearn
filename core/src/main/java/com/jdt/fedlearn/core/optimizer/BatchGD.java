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

package com.jdt.fedlearn.core.optimizer;

import java.util.ArrayList;
import java.util.Collection;

public class BatchGD implements Optimizer {
    private double learning_rate;

    BatchGD() {

    }

    public BatchGD(double lr) {
        this.learning_rate = lr;
    }

    public double[][] getGlobalUpdate(double[][] gredients) {
        for (int i = 0; i < gredients.length; i++) {
            for (int j = 0; j < gredients[i].length; j++) {
                gredients[i][j] = -learning_rate * gredients[i][j];
            }
        }
        return gredients;
    }

    public double[] getGlobalUpdate(double[] gredients) {
        for (int i = 0; i < gredients.length; i++) {
            gredients[i] = -learning_rate * gredients[i];
        }
        return gredients;
    }


    public void setLearning_rate(double learning_rate) {
        this.learning_rate = learning_rate;
    }

    public double getLearning_rate() {
        return learning_rate;
    }

    public Collection<Long> randomChoose(Collection<Long> samples) {
        return new ArrayList<>();
    }
}
